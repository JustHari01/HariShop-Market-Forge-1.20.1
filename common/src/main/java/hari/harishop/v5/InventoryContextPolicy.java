package hari.harishop.v5;

import java.util.List;

/** Pure NBT traversal policy; kept separate from Minecraft classes for tests. */
final class InventoryContextPolicy {
    private static final List<String> KEYS = List.of("Inventory", "EnderItems", "Items", "item", "Item");

    private InventoryContextPolicy() {}

    static boolean child(boolean parentInInventory, String key) {
        return parentInInventory || (key != null && KEYS.contains(key));
    }

    static List<String> keys() { return KEYS; }
}


