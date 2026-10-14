package hari.harishop.v5;

import java.util.ArrayList;
import java.util.List;

/**
 * Explicit release gate for the closed-world economy certificate.
 *
 * <p>A fingerprint by itself is not a safety certificate.  The gate keeps
 * every prerequisite visible and fail-closed: if a route, persistence check,
 * migration or sink hook is unknown, the market remains locked.  This class is
 * deliberately pure so the same policy can be exercised by unit tests and by
 * the NeoForge runtime without depending on Minecraft classes.</p>
 */
public final class CertificationGate {
    public record Inputs(boolean fingerprintComplete,
                          boolean kernelReady,
                          boolean indexingComplete,
                          boolean catalogPresent,
                          boolean routeGraphPresent,
                          boolean numismaticApiReady,
                          boolean allEconomicSinksHooked,
                          boolean exactVerifierPassed,
                          boolean persistenceIntegrityPassed,
                          boolean migrationComplete,
                          boolean noP0Quarantine,
                          int unsupportedRouteTypes,
                          int untrustedOutputs,
                          boolean runtimeManifestComplete,
                          boolean stoppedWorldCensusComplete,
                          boolean certificateManifestComplete,
                          boolean unsupportedRoutesFailClosed,
                          boolean testEvidenceComplete,
                          /** True when no external surface is unclassified; exact pinned quarantine adapters count. */
                          boolean noExternalEconomicSurfaces,
                          /** Crash matrix covers playerdata/chunk/receipt boundaries, not only SQLite. */
                          boolean physicalBoundaryEvidenceComplete,
                          /** Escrow/receipt/read-back/quarantine protocol proves no free value across external file commits. */
                          boolean crossResourceProtocolEvidenceComplete,
                          /** Native client screenshots/pixel assertions passed for the release matrix. */
                          boolean nativePixelEvidenceComplete,
                          /** JFR measurements were checked against the documented budgets. */
                          boolean jfrBudgetEvidenceComplete,
                          /** A production full-pack restart of the same world reached Done and stopped cleanly. */
                          boolean fullPackRestartEvidenceComplete,
                          /** The evidence is bound to the complete closed-world inventory fingerprint. */
                          boolean closedWorldInventoryEvidenceComplete) {
        /** Compatibility constructor for the original pure-gate tests/API. */
        public Inputs(boolean fingerprintComplete, boolean kernelReady, boolean indexingComplete,
                      boolean catalogPresent, boolean routeGraphPresent, boolean numismaticApiReady,
                      boolean allEconomicSinksHooked, boolean exactVerifierPassed,
                      boolean persistenceIntegrityPassed, boolean migrationComplete,
                      boolean noP0Quarantine, int unsupportedRouteTypes, int untrustedOutputs) {
            this(fingerprintComplete, kernelReady, indexingComplete, catalogPresent, routeGraphPresent,
                    numismaticApiReady, allEconomicSinksHooked, exactVerifierPassed,
                    persistenceIntegrityPassed, migrationComplete, noP0Quarantine,
                    unsupportedRouteTypes, untrustedOutputs, true, true, true, false, true, true,
                    false, false, false, false, false, false);
        }
        public Inputs {
            if (unsupportedRouteTypes < 0 || untrustedOutputs < 0) {
                throw new IllegalArgumentException("negative coverage count");
            }
        }

        /**
         * Compatibility constructor for callers that already supplied the
         * runtime/certificate flags but predate the physical release gates.
         * New gates default to false deliberately: old callers cannot widen
         * the certificate by omitting the stronger evidence.
         */
        public Inputs(boolean fingerprintComplete, boolean kernelReady, boolean indexingComplete,
                      boolean catalogPresent, boolean routeGraphPresent, boolean numismaticApiReady,
                      boolean allEconomicSinksHooked, boolean exactVerifierPassed,
                      boolean persistenceIntegrityPassed, boolean migrationComplete,
                      boolean noP0Quarantine, int unsupportedRouteTypes, int untrustedOutputs,
                      boolean runtimeManifestComplete, boolean stoppedWorldCensusComplete,
                      boolean certificateManifestComplete, boolean unsupportedRoutesFailClosed,
                      boolean testEvidenceComplete, boolean noExternalEconomicSurfaces) {
            this(fingerprintComplete, kernelReady, indexingComplete, catalogPresent, routeGraphPresent,
                    numismaticApiReady, allEconomicSinksHooked, exactVerifierPassed,
                    persistenceIntegrityPassed, migrationComplete, noP0Quarantine,
                    unsupportedRouteTypes, untrustedOutputs, runtimeManifestComplete,
                    stoppedWorldCensusComplete, certificateManifestComplete,
                    unsupportedRoutesFailClosed, testEvidenceComplete, noExternalEconomicSurfaces,
                    false, false, false, false, false, false);
        }
    }

    public record Result(boolean sealed, List<String> failures) {
        public Result {
            failures = List.copyOf(failures == null ? List.of() : failures);
        }

        public String summary() {
            return sealed ? "SEALED" : String.join("; ", failures);
        }
    }

    private CertificationGate() {}

    public static Result evaluate(Inputs input) {
        if (input == null) return new Result(false, List.of("thiếu đầu vào certification"));
        List<String> failures = new ArrayList<>();
        if (!input.fingerprintComplete()) failures.add("fingerprint chưa hoàn tất");
        if (!input.kernelReady()) failures.add("economy kernel chưa sẵn sàng");
        if (!input.indexingComplete()) failures.add("catalog/index vẫn đang chạy");
        if (!input.catalogPresent()) failures.add("catalog rỗng");
        if (!input.routeGraphPresent()) failures.add("acquisition graph rỗng");
        if (!input.numismaticApiReady()) failures.add("Numismatic secure API chưa sẵn sàng");
        if (!input.allEconomicSinksHooked()) failures.add("chưa hook đủ economic sink");
        if (!input.exactVerifierPassed()) failures.add("exact verifier chưa pass");
        if (!input.persistenceIntegrityPassed()) failures.add("integrity persistence chưa pass");
        if (!input.migrationComplete()) failures.add("migration chưa hoàn tất");
        if (!input.noP0Quarantine()) failures.add("còn quarantine P0");
        if (!input.runtimeManifestComplete()) failures.add("runtime mod manifest chưa hoàn tất");
        if (!input.stoppedWorldCensusComplete()) failures.add("stopped-world census chưa hoàn tất");
        if (!input.certificateManifestComplete()) failures.add("certificate manifest chưa đầy đủ");
        if (!input.testEvidenceComplete()) failures.add("test evidence chưa được chứng minh");
        if (!input.noExternalEconomicSurfaces()) {
            failures.add("đang nạp economic surface bên ngoài chưa được adapter chứng nhận");
        }
        if (!input.physicalBoundaryEvidenceComplete()) {
            failures.add("physical boundary crash matrix chưa chứng minh");
        }
        if (!input.crossResourceProtocolEvidenceComplete()) {
            failures.add("cross-resource escrow/receipt protocol chưa chứng minh");
        }
        if (!input.nativePixelEvidenceComplete()) {
            failures.add("native client pixel acceptance chưa chứng minh");
        }
        if (!input.jfrBudgetEvidenceComplete()) {
            failures.add("JFR budget tick/DB/memory chưa chứng minh");
        }
        if (!input.fullPackRestartEvidenceComplete()) {
            failures.add("full-pack same-world restart chưa chứng minh");
        }
        if (!input.closedWorldInventoryEvidenceComplete()) {
            failures.add("closed-world inventory evidence chưa bind đúng fingerprint");
        }
        if (input.unsupportedRouteTypes() > 0 && !input.unsupportedRoutesFailClosed()) {
            failures.add("còn " + input.unsupportedRouteTypes() + " loại route chưa có adapter");
        }
        if (input.untrustedOutputs() > 0) {
            failures.add("còn " + input.untrustedOutputs() + " output chưa chứng minh provenance");
        }
        return new Result(failures.isEmpty(), failures);
    }
}


