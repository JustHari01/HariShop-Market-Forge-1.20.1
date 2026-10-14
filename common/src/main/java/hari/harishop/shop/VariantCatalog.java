package hari.harishop.shop;

import hari.harishop.HariShopMod;
import hari.harishop.config.ShopConfig;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.item.alchemy.Potion;

/**
 * Deterministic catalog of vanilla variants which are not represented by a
 * plain creative-tab {@link ItemStack}.  Forge 1.20.1 stores these variants in
 * NBT, so they can be generated without relying on a client/JER integration.
 *
 * <p>The catalog is deliberately bounded and fail-closed: a generated stack
 * is only admitted after the same variant-aware calculator used by buy/sell
 * packets returns a positive price.  Unsupported mod-specific components are
 * therefore never turned into a guessed economic item.</p>
 */
public final class VariantCatalog {
    /** Hard upper bound against a mod registering an unreasonably large registry. */
    public static final int MAX_GENERATED_VARIANTS = 4096;

    private VariantCatalog() {}

    /** Returns all safe generated variants in stable registry order. */
    public static List<ItemStack> generate() {
        return generate(ShopConfig.get());
    }

    /**
     * Test/platform hook with an explicit configuration.  Keeping the
     * configuration as an argument makes the generator deterministic in a
     * headless contract test while the production overload still requires the
     * real server config before admitting any variant.
     */
    static List<ItemStack> generate(ShopConfig config) {
        List<ItemStack> result = new ArrayList<>();
        if (config == null) return result;
        Set<String> seen = new HashSet<>();
        addEnchantedBooks(result, seen, config);
        addPotions(result, seen, config);
        return result;
    }

    private static void addEnchantedBooks(List<ItemStack> out, Set<String> seen, ShopConfig config) {
        List<EnchantmentEntry> enchantments = new ArrayList<>();
        for (Enchantment enchantment : BuiltInRegistries.ENCHANTMENT) {
            ResourceLocation id = BuiltInRegistries.ENCHANTMENT.getKey(enchantment);
            if (id != null) enchantments.add(new EnchantmentEntry(id, enchantment));
        }
        enchantments.sort(Comparator.comparing(entry -> entry.id.toString()));
        for (EnchantmentEntry entry : enchantments) {
            int max = Math.max(1, Math.min(255, entry.enchantment.getMaxLevel()));
            for (int level = 1; level <= max && out.size() < MAX_GENERATED_VARIANTS; level++) {
                ItemStack stack;
                try {
                    stack = EnchantedBookItem.createForEnchantment(
                            new EnchantmentInstance(entry.enchantment, level));
                } catch (RuntimeException invalid) {
                    continue;
                }
                addSafe(out, seen, stack, entry.id.toString(), config);
            }
            if (out.size() >= MAX_GENERATED_VARIANTS) return;
        }
    }

    private static void addPotions(List<ItemStack> out, Set<String> seen, ShopConfig config) {
        List<PotionEntry> potions = new ArrayList<>();
        for (Potion potion : BuiltInRegistries.POTION) {
            ResourceLocation id = BuiltInRegistries.POTION.getKey(potion);
            if (id != null) potions.add(new PotionEntry(id, potion));
        }
        potions.sort(Comparator.comparing(entry -> entry.id.toString()));
        for (PotionEntry entry : potions) {
            ItemStack normal = PotionUtils.setPotion(new ItemStack(Items.POTION), entry.potion);
            addSafe(out, seen, normal, "potion:" + entry.id, config);
            if (out.size() >= MAX_GENERATED_VARIANTS) return;
            ItemStack splash = PotionUtils.setPotion(new ItemStack(Items.SPLASH_POTION), entry.potion);
            addSafe(out, seen, splash, "splash_potion:" + entry.id, config);
            if (out.size() >= MAX_GENERATED_VARIANTS) return;
            ItemStack lingering = PotionUtils.setPotion(new ItemStack(Items.LINGERING_POTION), entry.potion);
            addSafe(out, seen, lingering, "lingering_potion:" + entry.id, config);
            if (out.size() >= MAX_GENERATED_VARIANTS) return;
            ItemStack tippedArrow = PotionUtils.setPotion(new ItemStack(Items.TIPPED_ARROW), entry.potion);
            addSafe(out, seen, tippedArrow, "tipped_arrow:" + entry.id, config);
        }
    }

    private static void addSafe(List<ItemStack> out, Set<String> seen, ItemStack stack, String source, ShopConfig config) {
        if (stack == null || stack.isEmpty() || out.size() >= MAX_GENERATED_VARIANTS) return;
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (itemId == null) return;
        if (config.isModBlacklisted(itemId.getNamespace()) || config.isItemBlacklisted(itemId.toString())) return;
        long price;
        try {
            price = ItemValueCalculator.calculatePrice(stack);
        } catch (RuntimeException invalid) {
            return;
        }
        if (price <= 0L) return;
        String identity = itemId + "|" + String.valueOf(stack.getTag());
        if (!seen.add(identity)) return;
        out.add(stack.copyWithCount(1));
    }

    private record EnchantmentEntry(ResourceLocation id, Enchantment enchantment) {}
    private record PotionEntry(ResourceLocation id, Potion potion) {}
}
