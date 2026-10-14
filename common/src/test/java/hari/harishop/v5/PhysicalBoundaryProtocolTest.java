package hari.harishop.v5;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PhysicalBoundaryProtocolTest {
    @Test
    void everyForgeReceiptKillPointHasSafeRecovery() {
        PhysicalBoundaryProtocol.Report report = PhysicalBoundaryProtocol.runMatrix();
        assertTrue(report.total() >= 18, report.summary());
        assertTrue(report.pass(), report.summary());
    }

    @Test
    void lostExternalWriteNeverCreatesFreeMoney() {
        PhysicalBoundaryProtocol.Report report = PhysicalBoundaryProtocol.runMatrix();
        assertTrue(report.cases().stream().filter(c -> c.fault() == PhysicalBoundaryProtocol.FaultPoint.AFTER_EXTERNAL_WRITE)
                .allMatch(PhysicalBoundaryProtocol.CaseResult::safe), report.summary());
    }
}
