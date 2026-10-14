package hari.harishop.v5;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ShopRequestJournalTest {
    @AfterEach
    void clear() {
        ShopRequestJournal.clearForTests();
    }

    @Test
    void rejectsReplayAndConflictingReuse() {
        assertEquals(ShopRequestJournal.Decision.NEW,
                ShopRequestJournal.begin("tx-1", "buy:p:1:1"));
        assertEquals(ShopRequestJournal.Decision.IN_FLIGHT,
                ShopRequestJournal.begin("tx-1", "buy:p:1:1"));
        ShopRequestJournal.complete("tx-1", "buy:p:1:1");
        assertEquals(ShopRequestJournal.Decision.COMPLETE,
                ShopRequestJournal.begin("tx-1", "buy:p:1:1"));
        assertEquals(ShopRequestJournal.Decision.CONFLICT,
                ShopRequestJournal.begin("tx-1", "sell:p:1:1"));
    }

    @Test
    void abortAllowsRetryAfterFailedMutation() {
        assertEquals(ShopRequestJournal.Decision.NEW,
                ShopRequestJournal.begin("tx-2", "sell:p:2:4"));
        ShopRequestJournal.abort("tx-2", "sell:p:2:4");
        assertEquals(ShopRequestJournal.Decision.NEW,
                ShopRequestJournal.begin("tx-2", "sell:p:2:4"));
    }
}
