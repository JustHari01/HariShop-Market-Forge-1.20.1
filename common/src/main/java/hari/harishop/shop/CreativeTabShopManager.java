/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.resources.ResourceKey
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.world.item.CreativeModeTab
 *  net.minecraft.world.item.CreativeModeTabs
 *  net.minecraft.world.item.Item
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.level.ItemLike
 *  net.minecraftforge.registries.ForgeRegistries
 */
package hari.harishop.shop;

import hari.harishop.HariShopMod;
import hari.harishop.config.ShopConfig;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;

public class CreativeTabShopManager {
    private static final Map<ResourceLocation, List<ItemStack>> TAB_ITEMS = new ConcurrentHashMap<ResourceLocation, List<ItemStack>>();
    private static final Map<ResourceLocation, String> TAB_NAMES = new ConcurrentHashMap<ResourceLocation, String>();
    private static final List<ResourceLocation> AVAILABLE_TABS = new ArrayList<ResourceLocation>();
    private static boolean initialized = false;
    private static final Set<ResourceKey<CreativeModeTab>> BLACKLISTED_TABS = Set.of(
        ResourceKey.create(Registries.CREATIVE_MODE_TAB, new ResourceLocation("hotbar")),
        ResourceKey.create(Registries.CREATIVE_MODE_TAB, new ResourceLocation("inventory")),
        ResourceKey.create(Registries.CREATIVE_MODE_TAB, new ResourceLocation("search")),
        ResourceKey.create(Registries.CREATIVE_MODE_TAB, new ResourceLocation("op_blocks"))
    );
    private static final Map<String, String> VANILLA_CATEGORIES = new LinkedHashMap<String, String>();

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;
        HariShopMod.LOGGER.info("Initializing Creative Tab Shop Manager...");
        TAB_ITEMS.clear();
        TAB_NAMES.clear();
        AVAILABLE_TABS.clear();
        ShopConfig config = ShopConfig.get();
        LinkedHashMap<String, List> byMod = new LinkedHashMap<String, List>();
        for (Map.Entry entry : BuiltInRegistries.ITEM.entrySet()) {
            ItemStack stack;
            ResourceLocation itemId = ((ResourceKey)entry.getKey()).location();
            Item item = (Item)entry.getValue();
            String modId = itemId.getNamespace();
            if (config.isModBlacklisted(modId) || config.isItemBlacklisted(itemId.toString()) || (stack = new ItemStack((ItemLike)item)).isEmpty()) continue;
            byMod.computeIfAbsent(modId, k -> new ArrayList()).add(stack);
        }
        if (byMod.containsKey("minecraft")) {
            CreativeTabShopManager.categorizeMinecraftItems((List)byMod.remove("minecraft"));
        }
        for (Map.Entry entry : byMod.entrySet()) {
            String modId = (String)entry.getKey();
            List items = (List)entry.getValue();
            if (items.isEmpty()) continue;
            String displayName = CreativeTabShopManager.formatModName(modId);
            ResourceLocation tabId = new ResourceLocation(modId, "items");
            TAB_ITEMS.put(tabId, items);
            TAB_NAMES.put(tabId, displayName);
            AVAILABLE_TABS.add(tabId);
        }

        // A creative tab only exposes one default ItemStack per item.  That
        // loses the economic identity of enchanted books and potion effects.
        // Add a bounded, server-generated tab of exact variants so Cửa Hàng
        // and Chợ Đêm can sell the same concrete stacks that the calculator
        // and packet validator understand.  VariantCatalog is deterministic
        // and admits only positive, fail-closed prices.
        List<ItemStack> generatedVariants = VariantCatalog.generate();
        if (!generatedVariants.isEmpty()) {
            ResourceLocation variantsTab = new ResourceLocation("harishop", "variants");
            TAB_ITEMS.put(variantsTab, generatedVariants);
            TAB_NAMES.put(variantsTab, "Enchanted & Potion Variants");
            AVAILABLE_TABS.add(variantsTab);
            HariShopMod.LOGGER.info("Generated {} safe enchanted/potion variants for retail shops",
                    generatedVariants.size());
        }
        HariShopMod.LOGGER.info("Creative Tab Shop Manager initialized: {} categories, {} total items", (Object)AVAILABLE_TABS.size(), (Object)CreativeTabShopManager.getTotalItemCount());
    }

    private static void categorizeMinecraftItems(List<ItemStack> items) {
        LinkedHashMap<String, List> categories = new LinkedHashMap<String, List>();
        for (ItemStack itemStack : items) {
            String category = CreativeTabShopManager.getCategoryForItem(itemStack);
            categories.computeIfAbsent(category, k -> new ArrayList()).add(itemStack);
        }
        for (Map.Entry entry : categories.entrySet()) {
            String catKey = (String)entry.getKey();
            List catItems = (List)entry.getValue();
            if (catItems.isEmpty()) continue;
            String displayName = VANILLA_CATEGORIES.getOrDefault(catKey, catKey.substring(0, 1).toUpperCase() + catKey.substring(1).replace("_", " "));
            ResourceLocation tabId = new ResourceLocation("minecraft", catKey);
            TAB_ITEMS.put(tabId, catItems);
            TAB_NAMES.put(tabId, displayName);
            AVAILABLE_TABS.add(tabId);
        }
    }

    private static String getCategoryForItem(ItemStack stack) {
        Item item = stack.getItem();
        String itemName = BuiltInRegistries.ITEM.getKey(item).getPath().toLowerCase();
        if (itemName.contains("brick") || itemName.contains("stone") || itemName.contains("cobble") || itemName.contains("planks") || itemName.contains("slab") || itemName.contains("stairs") || itemName.contains("wall") || itemName.contains("fence") || itemName.contains("glass") || itemName.contains("concrete") || itemName.contains("terracotta") || itemName.contains("quartz") || itemName.contains("deepslate") || itemName.contains("copper") || itemName.endsWith("_block")) {
            return "building_blocks";
        }
        if (itemName.contains("wool") || itemName.contains("carpet") || itemName.contains("bed") || itemName.contains("banner") || itemName.contains("candle") || itemName.contains("stained") || itemName.contains("dye") || itemName.contains("glazed")) {
            return "colored_blocks";
        }
        if (itemName.contains("log") || itemName.contains("leaves") || itemName.contains("sapling") || itemName.contains("dirt") || itemName.contains("grass") || itemName.contains("sand") || itemName.contains("gravel") || itemName.contains("ore") || itemName.contains("flower") || itemName.contains("mushroom") || itemName.contains("coral") || itemName.contains("kelp") || itemName.contains("moss") || itemName.contains("azalea")) {
            return "natural_blocks";
        }
        if (itemName.contains("chest") || itemName.contains("furnace") || itemName.contains("crafting") || itemName.contains("anvil") || itemName.contains("enchanting") || itemName.contains("brewing") || itemName.contains("beacon") || itemName.contains("shulker") || itemName.contains("barrel") || itemName.contains("hopper") || itemName.contains("dispenser") || itemName.contains("dropper") || itemName.contains("cauldron") || itemName.contains("campfire") || itemName.contains("lantern") || itemName.contains("torch") || itemName.contains("sign") || itemName.contains("door") || itemName.contains("trapdoor") || itemName.contains("button") || itemName.contains("lever")) {
            return "functional_blocks";
        }
        if (itemName.contains("redstone") || itemName.contains("piston") || itemName.contains("observer") || itemName.contains("comparator") || itemName.contains("repeater") || itemName.contains("target") || itemName.contains("tripwire") || itemName.contains("pressure_plate") || itemName.contains("daylight")) {
            return "redstone_blocks";
        }
        if (itemName.contains("sword") || itemName.contains("axe") || itemName.contains("bow") || itemName.contains("crossbow") || itemName.contains("trident") || itemName.contains("arrow") || itemName.contains("helmet") || itemName.contains("chestplate") || itemName.contains("leggings") || itemName.contains("boots") || itemName.contains("shield") || itemName.contains("totem") || itemName.contains("armor")) {
            return "combat";
        }
        if (itemName.contains("pickaxe") || itemName.contains("shovel") || itemName.contains("hoe") || itemName.contains("fishing_rod") || itemName.contains("shears") || itemName.contains("flint") || itemName.contains("bucket") || itemName.contains("compass") || itemName.contains("clock") || itemName.contains("map") || itemName.contains("lead") || itemName.contains("name_tag") || itemName.contains("spyglass") || itemName.contains("brush")) {
            return "tools_and_utilities";
        }
        if (item.isEdible() || itemName.contains("food") || itemName.contains("apple") || itemName.contains("bread") || itemName.contains("steak") || itemName.contains("porkchop") || itemName.contains("chicken") || itemName.contains("fish") || itemName.contains("carrot") || itemName.contains("potato") || itemName.contains("beetroot") || itemName.contains("melon") || itemName.contains("berry") || itemName.contains("cake") || itemName.contains("cookie") || itemName.contains("pie") || itemName.contains("stew") || itemName.contains("soup") || itemName.contains("honey") || itemName.contains("potion") || itemName.contains("milk")) {
            return "food_and_drinks";
        }
        if (itemName.contains("spawn_egg")) {
            return "spawn_eggs";
        }
        return "ingredients";
    }

    private static String formatModName(String modId) {
        String name = modId.replace("_", " ").replace("-", " ");
        StringBuilder result = new StringBuilder();
        boolean capitalize = true;
        for (char c : name.toCharArray()) {
            if (c == ' ') {
                capitalize = true;
                result.append(c);
                continue;
            }
            if (capitalize) {
                result.append(Character.toUpperCase(c));
                capitalize = false;
                continue;
            }
            result.append(c);
        }
        return result.toString();
    }

    public static void reload() {
        initialized = false;
        CreativeTabShopManager.init();
    }

    public static List<ResourceLocation> getAvailableTabs() {
        if (!initialized) {
            CreativeTabShopManager.init();
        }
        return new ArrayList<ResourceLocation>(AVAILABLE_TABS);
    }

    public static String getTabName(ResourceLocation tabId) {
        if (!initialized) {
            CreativeTabShopManager.init();
        }
        return TAB_NAMES.getOrDefault(tabId, tabId.getPath());
    }

    public static List<ItemStack> getItemsForTab(ResourceLocation tabId) {
        if (!initialized) {
            CreativeTabShopManager.init();
        }
        return new ArrayList<ItemStack>(TAB_ITEMS.getOrDefault(tabId, Collections.emptyList()));
    }

    public static List<ItemStack> getAllItems() {
        if (!initialized) {
            CreativeTabShopManager.init();
        }
        ArrayList<ItemStack> allItems = new ArrayList<ItemStack>();
        for (List<ItemStack> tabItems : TAB_ITEMS.values()) {
            allItems.addAll(tabItems);
        }
        return allItems;
    }

    public static int getTotalItemCount() {
        if (!initialized) {
            CreativeTabShopManager.init();
        }
        return TAB_ITEMS.values().stream().mapToInt(List::size).sum();
    }

    public static int getTabCount() {
        if (!initialized) {
            CreativeTabShopManager.init();
        }
        return AVAILABLE_TABS.size();
    }

    public static void addItemToTab(ResourceLocation tabId, Item item, long price) {
        List<ItemStack> items;
        if (!initialized) {
            CreativeTabShopManager.init();
        }
        if ((items = TAB_ITEMS.get(tabId)) == null) {
            items = new ArrayList<ItemStack>();
            TAB_ITEMS.put(tabId, items);
            if (!AVAILABLE_TABS.contains(tabId)) {
                AVAILABLE_TABS.add(tabId);
            }
        }
        for (ItemStack existing : items) {
            if (existing.getItem() != item) continue;
            HariShopMod.LOGGER.info("Item {} already exists in tab {}", (Object)BuiltInRegistries.ITEM.getKey(item), (Object)tabId);
            return;
        }
        items.add(new ItemStack((ItemLike)item));
        HariShopMod.LOGGER.info("Restored item {} to tab {} with price {}", (Object)BuiltInRegistries.ITEM.getKey(item), (Object)tabId, (Object)price);
    }

    static {
        VANILLA_CATEGORIES.put("building_blocks", "Building Blocks");
        VANILLA_CATEGORIES.put("colored_blocks", "Colored Blocks");
        VANILLA_CATEGORIES.put("natural_blocks", "Natural Blocks");
        VANILLA_CATEGORIES.put("functional_blocks", "Functional Blocks");
        VANILLA_CATEGORIES.put("redstone_blocks", "Redstone Blocks");
        VANILLA_CATEGORIES.put("tools_and_utilities", "Tools & Utilities");
        VANILLA_CATEGORIES.put("combat", "Combat");
        VANILLA_CATEGORIES.put("food_and_drinks", "Food & Drinks");
        VANILLA_CATEGORIES.put("ingredients", "Ingredients");
        VANILLA_CATEGORIES.put("spawn_eggs", "Spawn Eggs");
    }
}

