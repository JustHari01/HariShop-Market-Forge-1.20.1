package hari.harishop.v5;

import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Explicit audit of every economic mutation boundary owned by HariShop.
 *
 * <p>This is intentionally a small pure object rather than a boolean hidden in
 * the certification path.  A new mutation boundary must be added to the enum
 * and observed by the runtime reflection audit before the market can certify.
 * Hook failures are sticky for the lifetime of the server and therefore cannot
 * be cleared by a later retry or a client packet.</p>
 */
public final class EconomicSinkCoverage {
    public enum Sink {
        MONEY_DEBIT,
        MONEY_CREDIT,
        SHOP_BUY,
        SHOP_SELL,
        NIGHT_MARKET,
        PAWN,
        CLAIM_REAL_ESTATE,
        COMMAND_PROVENANCE,
        CRAFT_PROVENANCE,
        SMELT_PROVENANCE,
        BLOCK_DROP_PROVENANCE,
        MOB_DROP_PROVENANCE,
        FISH_DROP_PROVENANCE,
        PLACEMENT_PROVENANCE,
        PISTON_PROVENANCE,
        ITEM_ENTITY_INGRESS,
        ITEM_TOSS_PROVENANCE,
        CONTAINER_LOOT_PROVENANCE
    }

    private final Set<Sink> observed;
    private final Set<String> failures;

    private EconomicSinkCoverage(Set<Sink> observed, Set<String> failures) {
        this.observed = observed.isEmpty()
                ? Set.of() : Collections.unmodifiableSet(EnumSet.copyOf(observed));
        this.failures = Collections.unmodifiableSet(new LinkedHashSet<>(failures));
    }

    public static EconomicSinkCoverage audit(Set<Sink> observed, Set<String> failures) {
        EnumSet<Sink> safeObserved = observed == null || observed.isEmpty()
                ? EnumSet.noneOf(Sink.class) : EnumSet.copyOf(observed);
        LinkedHashSet<String> safeFailures = new LinkedHashSet<>();
        if (failures != null) safeFailures.addAll(failures);
        return new EconomicSinkCoverage(safeObserved, safeFailures);
    }

    public static Set<Sink> required() {
        return Collections.unmodifiableSet(EnumSet.allOf(Sink.class));
    }

    public boolean complete() {
        return failures.isEmpty() && observed.containsAll(required());
    }

    public Set<Sink> observed() { return observed; }

    public Set<String> failures() { return failures; }

    public Set<Sink> missing() {
        EnumSet<Sink> missing = EnumSet.allOf(Sink.class);
        missing.removeAll(observed);
        return Collections.unmodifiableSet(missing);
    }

    public String summary() {
        if (complete()) return "COMPLETE";
        return "missing=" + missing() + "; failures=" + failures;
    }
}


