/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.nbt.CompoundTag
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.world.item.Item
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.level.ItemLike
 *  net.minecraftforge.registries.ForgeRegistries
 */
package hari.harishop.shop;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.core.registries.BuiltInRegistries;
import hari.harishop.v5.MoneyMath;

public class ShopTrade {
    private final Item item;
    private final ItemStack itemStack;
    private final long priceInBronze;
    private int stock;
    private final int maxStock;

    public ShopTrade(Item item, long priceInBronze, int stock) {
        this(requireItemStack(item), priceInBronze, stock);
    }

    private static ItemStack requireItemStack(Item item) {
        if (item == null) {
            throw new IllegalArgumentException("Shop trade item cannot be null");
        }
        return new ItemStack((ItemLike)item);
    }

    /** Variant-aware trade constructor.  The stack is copied and normalised
     * to one item so client packets can never change its identity or count. */
    public ShopTrade(ItemStack stack, long priceInBronze, int stock) {
        if (stack == null || stack.isEmpty()) {
            throw new IllegalArgumentException("Shop trade item stack cannot be empty");
        }
        this.itemStack = stack.copy();
        this.itemStack.setCount(1);
        Item item = this.itemStack.getItem();
        if (item == null) {
            throw new IllegalArgumentException("Shop trade item cannot be null");
        }
        MoneyMath.positive(priceInBronze, "Shop trade price");
        if (stock < -1) {
            throw new IllegalArgumentException("Shop trade stock must be -1 (unlimited) or non-negative");
        }
        this.item = item;
        this.priceInBronze = priceInBronze;
        this.stock = stock;
        this.maxStock = stock;
    }

    public Item getItem() {
        return this.item;
    }

    public ItemStack getItemStack() {
        return this.itemStack.copy();
    }

    public long getPriceInBronze() {
        return this.priceInBronze;
    }

    public int getStock() {
        return this.stock;
    }

    public int getMaxStock() {
        return this.maxStock;
    }

    public boolean isAvailable() {
        return this.stock < 0 || this.stock > 0;
    }

    public void purchase() {
        if (this.stock > 0) {
            --this.stock;
        }
    }

    public boolean matchesSearch(String query) {
        if (query == null || query.isEmpty()) {
            return true;
        }
        String lowerQuery = query.toLowerCase();
        String itemName = this.itemStack.getHoverName().getString().toLowerCase();
        if (itemName.contains(lowerQuery)) {
            return true;
        }
        ResourceLocation key = BuiltInRegistries.ITEM.getKey(this.item);
        return key != null && key.toString().toLowerCase().contains(lowerQuery);
    }

    public CompoundTag toNbt() {
        CompoundTag tag = new CompoundTag();
        ResourceLocation key = BuiltInRegistries.ITEM.getKey(this.item);
        if (key != null) {
            tag.putString("item", key.toString());
        }
        CompoundTag stackTag = new CompoundTag();
        this.itemStack.save(stackTag);
        tag.put("stack", stackTag);
        tag.putLong("price", this.priceInBronze);
        tag.putInt("stock", this.stock);
        tag.putInt("maxStock", this.maxStock);
        return tag;
    }

    public static ShopTrade fromNbt(CompoundTag tag) {
        if (tag == null) {
            return null;
        }
        ItemStack stack = ItemStack.EMPTY;
        if (tag.contains("stack", 10)) {
            try { stack = ItemStack.of(tag.getCompound("stack")); }
            catch (RuntimeException ignored) { stack = ItemStack.EMPTY; }
        }
        // Backward compatibility for old trades that only stored an item id.
        if (stack.isEmpty() && tag.contains("item")) {
            String itemId = tag.getString("item");
            if (itemId == null || itemId.isEmpty()) return null;
            Item item = (Item)BuiltInRegistries.ITEM.get(new ResourceLocation(itemId));
            if (item == null) return null;
            stack = new ItemStack((ItemLike)item);
        }
        if (stack.isEmpty()) return null;
        long price = tag.getLong("price");
        int stock = tag.getInt("stock");
        if (price <= 0L || price > MoneyMath.MAX_BALANCE || stock < -1) {
            return null;
        }
        try {
            return new ShopTrade(stack, price, stock);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }
}

