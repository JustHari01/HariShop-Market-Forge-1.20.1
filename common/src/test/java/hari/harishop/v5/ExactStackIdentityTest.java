package hari.harishop.v5;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ExactStackIdentityTest {
    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void sameNbtHasSameDigestAndDifferentNbtDoesNot() {
        ItemStack first = new ItemStack(Items.STONE);
        ItemStack second = new ItemStack(Items.STONE);
        assertEquals(ExactStackIdentity.encode(first).sha256(), ExactStackIdentity.encode(second).sha256());
        second.getOrCreateTag().putInt("Damage", 1);
        assertNotEquals(ExactStackIdentity.encode(first).sha256(), ExactStackIdentity.encode(second).sha256());
    }

    @Test
    void emptyStackIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> ExactStackIdentity.encode(ItemStack.EMPTY));
    }
}
