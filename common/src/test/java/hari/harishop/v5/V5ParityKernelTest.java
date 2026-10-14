package hari.harishop.v5;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Forge backport coverage for the pure V5 contracts.  These tests deliberately
 * avoid a client/server launch: they prove the deterministic economic rules
 * that are shared by every platform adapter.
 */
class V5ParityKernelTest {
    @Test
    void quoteDirectionsAndPawnFloorCannotCreateLoop() {
        PricePolicy.Quote quote = PricePolicy.quote(101L, BigDecimal.ONE,
                BigDecimal.ONE, new BigDecimal("0.4"), true);
        assertEquals(101L, quote.buy());
        assertEquals(40L, quote.sell());
        assertTrue(quote.retailEnabled());
        assertTrue(quote.pawnEnabled());
        assertEquals(0L, PricePolicy.quote(1L, BigDecimal.ONE, BigDecimal.ONE,
                BigDecimal.ONE, true).sell());
    }

    @Test
    void variantPolicySeparatesCosmeticAndUnknownComponents() {
        VariantPolicy policy = new VariantPolicy()
                .understand("minecraft:damage")
                .cosmetic("minecraft:custom_name");
        VariantPolicy.Decision auto = policy.classify("minecraft:iron_sword", List.of(
                new VariantPolicy.ComponentValue("minecraft:damage", "3"),
                new VariantPolicy.ComponentValue("minecraft:custom_name", "demo")));
        assertEquals(VariantPolicy.Status.AUTO, auto.status());
        VariantPolicy.Decision manual = policy.classify("example:thing", List.of(
                new VariantPolicy.ComponentValue("example:unknown", "payload")));
        assertEquals(VariantPolicy.Status.MANUAL, manual.status());
    }

    @Test
    void repeatedSupplyScanReplacesSnapshot() {
        SupplyLedger ledger = new SupplyLedger(Duration.ofMinutes(5));
        Instant now = Instant.now();
        ledger.replace("inventory", new SupplyLedger.Snapshot(Map.of("minecraft:stone", 12L),
                now, SupplyLedger.Coverage.COMPLETE, 1.0));
        assertEquals(12L, ledger.observe("minecraft:stone", now).count());
        ledger.replace("inventory", new SupplyLedger.Snapshot(Map.of("minecraft:stone", 3L),
                now, SupplyLedger.Coverage.COMPLETE, 1.0));
        assertEquals(3L, ledger.observe("minecraft:stone", now).count());
    }

    @Test
    void acquisitionGraphLocksGuaranteedArbitrageAndDetectsCycle() {
        AcquisitionGraph graph = new AcquisitionGraph();
        graph.source("ore", 10L);
        graph.route(new AcquisitionGraph.Route("smelt", Map.of("ore", 1L), "ingot", 1L, 0L));
        graph.route(new AcquisitionGraph.Route("loop", Map.of("ingot", 1L), "ore", 2L, 0L));
        AcquisitionGraph.Result result = graph.solve(Map.of("ore", 100L, "ingot", 100L));
        assertTrue(result.cyclicItems().contains("ore"));
        assertTrue(result.lockedRoutes().contains("loop"));
    }

    @Test
    void persistenceHasChecksumAndBackupRecovery() throws Exception {
        Path root = Files.createTempDirectory("harishop-v5-store");
        Path state = root.resolve("market.properties");
        var first = new java.util.Properties();
        first.setProperty("revision", "1");
        AtomicStateStore.save(state, first);
        var second = new java.util.Properties();
        second.setProperty("revision", "2");
        AtomicStateStore.save(state, second);
        Files.writeString(state, "corrupted");
        assertEquals("1", AtomicStateStore.load(state).getProperty("revision"));
    }
}
