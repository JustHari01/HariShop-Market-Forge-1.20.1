package hari.harishop.v5;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Closed-world contract for a recipe/process serializer.
 *
 * <p>The adapter is intentionally small and Minecraft-independent.  The
 * NeoForge layer is responsible for extracting inputs/outputs, while this
 * contract is the allow-list boundary: a process type may enter the economic
 * graph only when a versioned adapter claims it and its self-test passes.
 * Unknown types are never guessed.</p>
 */
public interface ProcessAdapter {
    /** Semantic proof metadata required before a process can enter the graph. */
    record SemanticContract(
            boolean enumeratesInputs,
            boolean enumeratesOutputs,
            boolean enumeratesByproducts,
            boolean enumeratesCatalysts,
            boolean capturesProbabilities,
            boolean capturesResources,
            boolean deterministicOracle,
            boolean provenancePropagation,
            String codecVersion,
            String oracleVersion) {
        public SemanticContract {
            codecVersion = normalize(codecVersion);
            oracleVersion = normalize(oracleVersion);
        }

        public boolean complete() {
            return enumeratesInputs && enumeratesOutputs && enumeratesByproducts
                    && enumeratesCatalysts && capturesProbabilities && capturesResources
                    && deterministicOracle && provenancePropagation
                    && !codecVersion.isBlank() && !oracleVersion.isBlank();
        }

        public List<String> missing() {
            ArrayList<String> missing = new ArrayList<>();
            if (!enumeratesInputs) missing.add("inputs");
            if (!enumeratesOutputs) missing.add("outputs");
            if (!enumeratesByproducts) missing.add("byproducts");
            if (!enumeratesCatalysts) missing.add("catalysts");
            if (!capturesProbabilities) missing.add("probabilities");
            if (!capturesResources) missing.add("resources");
            if (!deterministicOracle) missing.add("deterministic-oracle");
            if (!provenancePropagation) missing.add("provenance-propagation");
            if (codecVersion.isBlank()) missing.add("codec-version");
            if (oracleVersion.isBlank()) missing.add("oracle-version");
            return List.copyOf(missing);
        }

        /** Stable representation included in the graph/certificate digest. */
        public String canonical() {
            return Boolean.toString(enumeratesInputs) + '|' + enumeratesOutputs + '|'
                    + enumeratesByproducts + '|' + enumeratesCatalysts + '|'
                    + capturesProbabilities + '|' + capturesResources + '|'
                    + deterministicOracle + '|' + provenancePropagation + '|'
                    + codecVersion + '|' + oracleVersion;
        }

        public static SemanticContract unsupported() {
            return new SemanticContract(false, false, false, false, false, false,
                    false, false, "", "");
        }

        private static String normalize(String value) {
            return value == null ? "" : value.trim();
        }
    }

    /** Stable identifier included in the certificate and graph fingerprint. */
    String id();

    /** Exact serializer/type ids handled by this adapter. */
    Set<String> typeIds();

    /** Adapter implementation/version bound to the current semantic contract. */
    String version();

    /** Pure startup self-test; a false result disables the adapter. */
    boolean selfTest();

    /** Old adapters are unsupported until they declare complete semantics. */
    default SemanticContract semanticContract() {
        return SemanticContract.unsupported();
    }

    /** Code self-test and semantic proof must both pass. */
    default boolean semanticSelfTest() {
        SemanticContract contract = semanticContract();
        return selfTest() && contract != null && contract.complete();
    }

    default boolean supports(String typeId) {
        return typeId != null && typeIds().contains(typeId);
    }
}


