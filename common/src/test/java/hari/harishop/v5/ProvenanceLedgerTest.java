package hari.harishop.v5;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ProvenanceLedgerTest {
    @Test
    void forgedOrRevokedLotCannotBecomeEconomic() {
        ProvenanceLedger ledger = new ProvenanceLedger(new byte[32], "test");
        ProvenanceLedger.Ref lot = ledger.issue(ProvenanceLedger.OriginClass.SURVIVAL_CERTIFIED, 4L, "", 1L);
        assertTrue(ledger.economicallyEligible(lot));
        ProvenanceLedger.Ref forged = new ProvenanceLedger.Ref(lot.lotId(), lot.origin(), lot.parentDigest(),
                lot.committedQuantity() + 1L, lot.issuedAtMillis(), lot.keyId(), lot.signature());
        assertFalse(ledger.verify(forged));
        ledger.revoke(lot.lotId());
        assertFalse(ledger.economicallyEligible(ledger.resolve(lot.lotId())));
    }

    @Test
    void adminInputTaintsDerivedLot() {
        ProvenanceLedger ledger = new ProvenanceLedger(new byte[32], "test");
        ProvenanceLedger.Ref admin = ledger.issue(ProvenanceLedger.OriginClass.ADMIN_INJECTED, 1L, "", 1L);
        ProvenanceLedger.Ref derived = ledger.derive(List.of(admin), 1L, 2L);
        assertEquals(ProvenanceLedger.OriginClass.ADMIN_DERIVED, derived.origin());
        assertFalse(ledger.economicallyEligible(derived));
    }
}
