package hari.harishop.v5;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.channels.FileChannel;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Properties;

/**
 * Deterministic byte-level model of the two-resource receipt protocol for
 * the Forge backport. It is intentionally Minecraft-independent, so it can
 * run in the ordinary Java test JVM on Java 17. A release must quarantine an
 * uncertain external boundary rather than guessing whether an item or money
 * mutation happened.
 */
public final class PhysicalBoundaryProtocol {
    private static final String PAYLOAD = "tx=physical-matrix|asset=minecraft:stone|quantity=1|amount=17";
    private static final String PAYLOAD_DIGEST = digest(PAYLOAD.getBytes(StandardCharsets.UTF_8));

    private PhysicalBoundaryProtocol() {}

    public enum Kind { BUY, SELL }

    public enum FaultPoint {
        BEFORE_PREPARED,
        AFTER_PREPARED,
        AFTER_MONEY_APPLIED,
        AFTER_EXTERNAL_STARTED,
        AFTER_EXTERNAL_WRITE,
        AFTER_EXTERNAL_FORCE,
        AFTER_READBACK,
        AFTER_COMMITTED
    }

    public record CaseResult(Kind kind, FaultPoint fault, boolean receiptPresent,
                             CrashRecoveryPolicy.Action action, boolean safe,
                             String detail) {}

    public record Report(List<CaseResult> cases) {
        public Report { cases = List.copyOf(cases); }
        public int total() { return cases.size(); }
        public long safeCount() { return cases.stream().filter(CaseResult::safe).count(); }
        public boolean pass() { return !cases.isEmpty() && safeCount() == total(); }
        public String summary() {
            return "cases=" + total() + ";safe=" + safeCount() + ";status=" + (pass() ? "PASS" : "INCOMPLETE");
        }
    }

    /** Runs both trade directions at every boundary, including a lost write. */
    public static Report runMatrix() {
        ArrayList<CaseResult> results = new ArrayList<>();
        for (Kind kind : Kind.values()) {
            for (FaultPoint point : FaultPoint.values()) {
                if (point == FaultPoint.AFTER_EXTERNAL_WRITE) {
                    results.add(run(kind, point, true));
                    results.add(run(kind, point, false));
                } else {
                    results.add(run(kind, point, true));
                }
            }
        }
        return new Report(results);
    }

    private static CaseResult run(Kind kind, FaultPoint fault, boolean writeVisible) {
        Path root = null;
        try {
            root = Files.createTempDirectory("harishop-forge-physical-boundary-");
            Path intent = root.resolve("intent.properties");
            Path external = root.resolve("playerdata.receipt");
            boolean moneyApplied = false;
            boolean externalStarted = false;
            boolean receipt = false;

            if (fault == FaultPoint.BEFORE_PREPARED) {
                return result(kind, fault, false, CrashRecoveryPolicy.Action.CANCEL, true, "no durable intent");
            }
            writeIntent(intent, "PREPARED", false, false);
            if (fault == FaultPoint.AFTER_PREPARED) return recover(kind, fault, intent, external, false, false, false);

            if (kind == Kind.BUY) {
                moneyApplied = true;
                writeIntent(intent, "MONEY_APPLIED", true, false);
                if (fault == FaultPoint.AFTER_MONEY_APPLIED) return recover(kind, fault, intent, external, false, true, false);
            }

            externalStarted = true;
            writeIntent(intent, "EXTERNAL_STARTED", moneyApplied, true);
            if (fault == FaultPoint.AFTER_EXTERNAL_STARTED) return recover(kind, fault, intent, external, false, moneyApplied, true);

            writeExternal(external);
            if (fault == FaultPoint.AFTER_EXTERNAL_WRITE && !writeVisible) Files.deleteIfExists(external);
            if (fault == FaultPoint.AFTER_EXTERNAL_WRITE) return recover(kind, fault, intent, external, false, moneyApplied, true);

            force(external);
            if (fault == FaultPoint.AFTER_EXTERNAL_FORCE) return recover(kind, fault, intent, external, false, moneyApplied, true);

            receipt = readReceipt(external);
            writeIntent(intent, "INVENTORY_SAVED", moneyApplied, true);
            if (fault == FaultPoint.AFTER_READBACK) return recover(kind, fault, intent, external, receipt, moneyApplied, true);

            if (kind == Kind.SELL) {
                moneyApplied = true;
                writeIntent(intent, "MONEY_APPLIED", true, true);
                if (fault == FaultPoint.AFTER_MONEY_APPLIED) return recover(kind, fault, intent, external, receipt, true, true);
            }
            writeIntent(intent, "COMMITTED", moneyApplied, true);
            if (fault == FaultPoint.AFTER_COMMITTED) return recover(kind, fault, intent, external, receipt, moneyApplied, true);
            return result(kind, fault, receipt,
                    kind == Kind.SELL ? CrashRecoveryPolicy.Action.COMPLETE_SELL : CrashRecoveryPolicy.Action.COMPLETE_BUY,
                    true, "completed before injected kill");
        } catch (Throwable failure) {
            return result(kind, fault, false, CrashRecoveryPolicy.Action.QUARANTINE, false,
                    failure.getClass().getSimpleName() + ":" + failure.getMessage());
        } finally { deleteTree(root); }
    }

    private static CaseResult recover(Kind kind, FaultPoint fault, Path intent, Path external,
                                      boolean receiptFromCaller, boolean moneyApplied,
                                      boolean externalStarted) throws IOException {
        Properties state = new Properties();
        try (var input = Files.newInputStream(intent)) { state.load(input); }
        boolean persistedMoney = Boolean.parseBoolean(state.getProperty("moneyApplied", "false"));
        boolean persistedStarted = Boolean.parseBoolean(state.getProperty("externalStarted", "false"));
        boolean receipt = receiptFromCaller || readReceipt(external);
        CrashRecoveryPolicy.MoneyStatus primary = (moneyApplied || persistedMoney)
                ? CrashRecoveryPolicy.MoneyStatus.APPLIED : CrashRecoveryPolicy.MoneyStatus.MISSING;
        CrashRecoveryPolicy.Action action = CrashRecoveryPolicy.decide(kind == Kind.SELL, receipt,
                externalStarted || persistedStarted, primary, CrashRecoveryPolicy.MoneyStatus.MISSING);
        return result(kind, fault, receipt, action, safeOutcome(kind, action, receipt, primary),
                state.getProperty("stage", "unknown"));
    }

    private static boolean safeOutcome(Kind kind, CrashRecoveryPolicy.Action action,
                                       boolean receipt, CrashRecoveryPolicy.MoneyStatus primary) {
        if (action == CrashRecoveryPolicy.Action.QUARANTINE) return true;
        if (kind == Kind.BUY) {
            return switch (action) {
                case CANCEL -> primary != CrashRecoveryPolicy.MoneyStatus.APPLIED && !receipt;
                case REFUND_BUY -> primary == CrashRecoveryPolicy.MoneyStatus.APPLIED && !receipt;
                case COMPLETE_BUY -> primary == CrashRecoveryPolicy.MoneyStatus.APPLIED && receipt;
                default -> false;
            };
        }
        return switch (action) {
            case CANCEL -> primary != CrashRecoveryPolicy.MoneyStatus.APPLIED && !receipt;
            case PAY_SELL, COMPLETE_SELL -> receipt;
            default -> false;
        };
    }

    private static CaseResult result(Kind kind, FaultPoint fault, boolean receipt,
                                     CrashRecoveryPolicy.Action action, boolean safe, String detail) {
        return new CaseResult(kind, fault, receipt, action, safe, detail == null ? "" : detail);
    }

    private static void writeIntent(Path path, String stage, boolean money, boolean started) throws IOException {
        Properties properties = new Properties();
        properties.setProperty("stage", stage);
        properties.setProperty("moneyApplied", Boolean.toString(money));
        properties.setProperty("externalStarted", Boolean.toString(started));
        properties.setProperty("payloadDigest", PAYLOAD_DIGEST);
        Path tmp = path.resolveSibling(path.getFileName() + ".tmp");
        try (var output = Files.newOutputStream(tmp, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)) {
            properties.store(output, "HariShop Forge physical boundary matrix");
        }
        force(tmp);
        try { Files.move(tmp, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE); }
        catch (java.nio.file.AtomicMoveNotSupportedException unsupported) { Files.move(tmp, path, StandardCopyOption.REPLACE_EXISTING); }
    }

    private static void writeExternal(Path path) throws IOException {
        Path tmp = path.resolveSibling(path.getFileName() + ".tmp");
        Files.writeString(tmp, "receipt=" + PAYLOAD_DIGEST + "\n", StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
        force(tmp);
        try { Files.move(tmp, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE); }
        catch (java.nio.file.AtomicMoveNotSupportedException unsupported) { Files.move(tmp, path, StandardCopyOption.REPLACE_EXISTING); }
    }

    private static boolean readReceipt(Path path) throws IOException {
        if (!Files.isRegularFile(path)) return false;
        return Files.readString(path, StandardCharsets.UTF_8).trim().equals("receipt=" + PAYLOAD_DIGEST);
    }

    private static void force(Path path) throws IOException {
        try (FileChannel channel = FileChannel.open(path, StandardOpenOption.READ, StandardOpenOption.WRITE)) { channel.force(true); }
    }

    private static String digest(byte[] bytes) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)); }
        catch (Exception failure) { throw new IllegalStateException("SHA-256 unavailable", failure); }
    }

    private static void deleteTree(Path root) {
        if (root == null) return;
        try (var stream = Files.walk(root)) {
            stream.sorted((a, b) -> b.compareTo(a)).forEach(path -> { try { Files.deleteIfExists(path); } catch (IOException ignored) {} });
        } catch (IOException ignored) {}
    }
}
