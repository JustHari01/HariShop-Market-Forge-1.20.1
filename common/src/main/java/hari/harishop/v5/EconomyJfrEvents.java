package hari.harishop.v5;

import jdk.jfr.Category;
import jdk.jfr.Event;
import jdk.jfr.Label;
import jdk.jfr.Name;
import jdk.jfr.StackTrace;

/**
 * Low-overhead telemetry for the release performance gates.  Events are
 * emitted only at economic boundaries; no inventory or world object is kept
 * in an event, so a recording cannot retain a player or chunk accidentally.
 */
public final class EconomyJfrEvents {
    private EconomyJfrEvents() {}

    @Name("HariShop.Quote")
    @Label("HariShop quote")
    @Category({"HariShop", "Economy"})
    @StackTrace(false)
    public static final class Quote extends Event {
        @Label("Action") public String action;
        @Label("Outcome") public String outcome;
        @Label("Revision") public long revision;
        @Label("Price") public long price;
    }

    @Name("HariShop.Transaction")
    @Label("HariShop transaction")
    @Category({"HariShop", "Economy"})
    @StackTrace(false)
    public static final class Transaction extends Event {
        @Label("Operation") public String operation;
        @Label("Outcome") public String outcome;
        @Label("Amount") public long amount;
        @Label("Revision") public long revision;
    }

    @Name("HariShop.AdapterMiss")
    @Label("HariShop adapter miss")
    @Category({"HariShop", "Economy"})
    @StackTrace(false)
    public static final class AdapterMiss extends Event {
        @Label("Component") public String component;
        @Label("Reason") public String reason;
    }

    @Name("HariShop.SupplyDrift")
    @Label("HariShop supply drift")
    @Category({"HariShop", "Economy"})
    @StackTrace(false)
    public static final class SupplyDrift extends Event {
        @Label("Asset") public String asset;
        @Label("Observed") public long observed;
        @Label("Previous") public long previous;
        @Label("Coverage") public String coverage;
    }

    @Name("HariShop.DatabaseLatency")
    @Label("HariShop database latency")
    @Category({"HariShop", "Economy"})
    @StackTrace(false)
    public static final class DatabaseLatency extends Event {
        @Label("Operation") public String operation;
        @Label("Micros") public long micros;
        @Label("Committed") public boolean committed;
        @Label("Phase") public String phase;
    }

    @Name("HariShop.BackgroundWork")
    @Label("HariShop background work")
    @Category({"HariShop", "Economy", "Performance"})
    @StackTrace(false)
    public static final class BackgroundWork extends Event {
        @Label("Operation") public String operation;
        @Label("Micros") public long micros;
        @Label("Queue depth") public long queueDepth;
        @Label("Succeeded") public boolean succeeded;
        @Label("Counts against tick budget") public boolean countsAgainstTickBudget;
    }

    @Name("HariShop.Reconciliation")
    @Label("HariShop reconciliation")
    @Category({"HariShop", "Economy", "Performance"})
    @StackTrace(false)
    public static final class Reconciliation extends Event {
        @Label("Operation") public String operation;
        @Label("Micros") public long micros;
        @Label("Loaded units") public long loadedUnits;
        @Label("Complete") public boolean complete;
        @Label("Counts against tick budget") public boolean countsAgainstTickBudget;
    }

    @Name("HariShop.MixinMiss")
    @Label("HariShop mixin target miss")
    @Category({"HariShop", "Economy"})
    @StackTrace(false)
    public static final class MixinMiss extends Event {
        @Label("Target") public String target;
        @Label("Reason") public String reason;
    }

    public static void transaction(String operation, String outcome, long amount, long revision) {
        Transaction event = new Transaction();
        if (!event.isEnabled()) return;
        event.operation = safe(operation);
        event.outcome = safe(outcome);
        event.amount = amount;
        event.revision = revision;
        event.commit();
    }

    public static void database(String operation, long startedNanos, boolean committed) {
        DatabaseLatency event = new DatabaseLatency();
        if (!event.isEnabled()) return;
        event.operation = safe(operation);
        event.micros = Math.max(0L, (System.nanoTime() - startedNanos) / 1_000L);
        event.committed = committed;
        event.phase = committed ? "COMMIT" : "ROLLBACK_OR_READ";
        event.commit();
    }

    public static void background(String operation, long startedNanos, long queueDepth, boolean succeeded) {
        background(operation, startedNanos, queueDepth, succeeded, true);
    }

    /** Startup/offline compiler work is not a tick task and is excluded from
     * the per-tick budget while remaining visible in the recording. */
    public static void background(String operation, long startedNanos, long queueDepth,
                                  boolean succeeded, boolean countsAgainstTickBudget) {
        BackgroundWork event = new BackgroundWork();
        if (!event.isEnabled()) return;
        event.operation = safe(operation);
        event.micros = Math.max(0L, (System.nanoTime() - startedNanos) / 1_000L);
        event.queueDepth = Math.max(0L, queueDepth);
        event.succeeded = succeeded;
        event.countsAgainstTickBudget = countsAgainstTickBudget;
        event.commit();
    }

    public static void reconciliation(String operation, long startedNanos, long loadedUnits, boolean complete) {
        reconciliation(operation, startedNanos, loadedUnits, complete, true);
    }

    public static void reconciliation(String operation, long startedNanos, long loadedUnits,
                                      boolean complete, boolean countsAgainstTickBudget) {
        Reconciliation event = new Reconciliation();
        if (!event.isEnabled()) return;
        event.operation = safe(operation);
        event.micros = Math.max(0L, (System.nanoTime() - startedNanos) / 1_000L);
        event.loadedUnits = Math.max(0L, loadedUnits);
        event.complete = complete;
        event.countsAgainstTickBudget = countsAgainstTickBudget;
        event.commit();
    }

    public static void adapterMiss(String component, String reason) {
        AdapterMiss event = new AdapterMiss();
        if (!event.isEnabled()) return;
        event.component = safe(component);
        event.reason = safe(reason);
        event.commit();
    }

    public static void supply(String asset, long observed, long previous, String coverage) {
        SupplyDrift event = new SupplyDrift();
        if (!event.isEnabled()) return;
        event.asset = safe(asset);
        event.observed = observed;
        event.previous = previous;
        event.coverage = safe(coverage);
        event.commit();
    }

    public static void mixinMiss(String target, String reason) {
        MixinMiss event = new MixinMiss();
        if (!event.isEnabled()) return;
        event.target = safe(target);
        event.reason = safe(reason);
        event.commit();
    }

    private static String safe(String value) {
        if (value == null) return "";
        return value.length() <= 256 ? value : value.substring(0, 256);
    }
}


