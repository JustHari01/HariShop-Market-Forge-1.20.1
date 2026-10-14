/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.Gson
 *  com.google.gson.GsonBuilder
 *  com.google.gson.JsonArray
 *  com.google.gson.JsonElement
 *  com.google.gson.JsonObject
 *  com.google.gson.JsonParser
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.world.item.Item
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.level.ItemLike
 *  net.minecraftforge.fml.loading.FMLPaths
 *  net.minecraftforge.registries.ForgeRegistries
 */
package hari.harishop.shop;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import hari.harishop.HariShopMod;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import dev.architectury.platform.Platform;
import net.minecraft.core.registries.BuiltInRegistries;

public class ShopEditHistory {
    private static final int MAX_HISTORY_PER_CATEGORY = Integer.MAX_VALUE;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Map<String, List<DeletedItemRecord>> historyByCategory = new ConcurrentHashMap<String, List<DeletedItemRecord>>();
    private static Path historyFile;

    public static void init() {
        Path configDir = Platform.getConfigFolder().resolve("harishop");
        try {
            Files.createDirectories(configDir, new FileAttribute[0]);
        }
        catch (IOException e) {
            HariShopMod.LOGGER.error("Failed to create config directory", (Throwable)e);
        }
        historyFile = configDir.resolve("edit_history.json");
        ShopEditHistory.loadFromFile();
        HariShopMod.LOGGER.info("ShopEditHistory initialized with {} categories", (Object)historyByCategory.size());
    }

    public static void recordDeletion(String category, ItemStack stack, long price, String deletedBy) {
        if (stack.isEmpty() || category == null) {
            return;
        }
        String itemId = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        DeletedItemRecord record = new DeletedItemRecord(System.currentTimeMillis(), category, itemId, price, deletedBy != null ? deletedBy : "Unknown", stack.getCount());
        List categoryHistory = historyByCategory.computeIfAbsent(category, k -> new ArrayList());
        categoryHistory.add(0, record);
        while (categoryHistory.size() > Integer.MAX_VALUE) {
            categoryHistory.remove(categoryHistory.size() - 1);
        }
        ShopEditHistory.saveToFile();
        HariShopMod.LOGGER.info("Recorded deletion: {} from {} by {}", (Object)itemId, (Object)category, (Object)deletedBy);
    }

    public static List<DeletedItemRecord> getHistory(String category) {
        return historyByCategory.getOrDefault(category, Collections.emptyList());
    }

    public static List<DeletedItemRecord> getAllHistory() {
        ArrayList<DeletedItemRecord> all = new ArrayList<DeletedItemRecord>();
        for (List<DeletedItemRecord> list : historyByCategory.values()) {
            all.addAll(list);
        }
        all.sort((a, b) -> Long.compare(b.timestamp, a.timestamp));
        return all;
    }

    public static DeletedItemRecord restoreItem(String category, int index) {
        List<DeletedItemRecord> categoryHistory = historyByCategory.get(category);
        if (categoryHistory == null || index < 0 || index >= categoryHistory.size()) {
            return null;
        }
        DeletedItemRecord record = categoryHistory.remove(index);
        ShopEditHistory.saveToFile();
        HariShopMod.LOGGER.info("Restored: {} from {}", (Object)record.itemId, (Object)category);
        return record;
    }

    public static void clearHistory(String category) {
        historyByCategory.remove(category);
        ShopEditHistory.saveToFile();
    }

    public static void clearAllHistory() {
        historyByCategory.clear();
        ShopEditHistory.saveToFile();
    }

    private static void saveToFile() {
        if (historyFile == null) {
            return;
        }
        try {
            JsonObject root = new JsonObject();
            for (Map.Entry<String, List<DeletedItemRecord>> entry : historyByCategory.entrySet()) {
                JsonArray arr = new JsonArray();
                for (DeletedItemRecord record : entry.getValue()) {
                    arr.add((JsonElement)record.toJson());
                }
                root.add(entry.getKey(), (JsonElement)arr);
            }
            try (FileWriter writer = new FileWriter(historyFile.toFile());){
                GSON.toJson((JsonElement)root, (Appendable)writer);
            }
        }
        catch (IOException e) {
            HariShopMod.LOGGER.error("Failed to save edit history", (Throwable)e);
        }
    }

    private static void loadFromFile() {
        if (historyFile == null || !Files.exists(historyFile, new LinkOption[0])) {
            return;
        }
        try (FileReader reader = new FileReader(historyFile.toFile());){
            JsonObject root = JsonParser.parseReader((Reader)reader).getAsJsonObject();
            historyByCategory.clear();
            for (Map.Entry entry : root.entrySet()) {
                String category = (String)entry.getKey();
                JsonArray arr = ((JsonElement)entry.getValue()).getAsJsonArray();
                ArrayList<DeletedItemRecord> list = new ArrayList<DeletedItemRecord>();
                for (JsonElement elem : arr) {
                    try {
                        list.add(DeletedItemRecord.fromJson(elem.getAsJsonObject()));
                    }
                    catch (Exception e) {
                        HariShopMod.LOGGER.warn("Failed to parse history record", (Throwable)e);
                    }
                }
                historyByCategory.put(category, list);
            }
            HariShopMod.LOGGER.info("Loaded {} categories from edit history", (Object)historyByCategory.size());
        }
        catch (Exception e) {
            HariShopMod.LOGGER.error("Failed to load edit history", (Throwable)e);
        }
    }

    public static int getTotalHistoryCount() {
        int count = 0;
        for (List<DeletedItemRecord> list : historyByCategory.values()) {
            count += list.size();
        }
        return count;
    }

    public static class DeletedItemRecord {
        public final long timestamp;
        public final String category;
        public final String itemId;
        public final long price;
        public final String deletedBy;
        public final int count;

        public DeletedItemRecord(long timestamp, String category, String itemId, long price, String deletedBy, int count) {
            this.timestamp = timestamp;
            this.category = category;
            this.itemId = itemId;
            this.price = price;
            this.deletedBy = deletedBy;
            this.count = count;
        }

        public String getFormattedTime() {
            return DateTimeFormatter.ofPattern("MM/dd HH:mm").withZone(ZoneId.systemDefault()).format(Instant.ofEpochMilli(this.timestamp));
        }

        public ItemStack toItemStack() {
            Item item = (Item)BuiltInRegistries.ITEM.get(new ResourceLocation(this.itemId));
            if (item != null) {
                return new ItemStack((ItemLike)item, this.count);
            }
            return ItemStack.EMPTY;
        }

        public JsonObject toJson() {
            JsonObject obj = new JsonObject();
            obj.addProperty("timestamp", (Number)this.timestamp);
            obj.addProperty("category", this.category);
            obj.addProperty("itemId", this.itemId);
            obj.addProperty("price", (Number)this.price);
            obj.addProperty("deletedBy", this.deletedBy);
            obj.addProperty("count", (Number)this.count);
            return obj;
        }

        public static DeletedItemRecord fromJson(JsonObject obj) {
            return new DeletedItemRecord(obj.get("timestamp").getAsLong(), obj.get("category").getAsString(), obj.get("itemId").getAsString(), obj.get("price").getAsLong(), obj.get("deletedBy").getAsString(), obj.has("count") ? obj.get("count").getAsInt() : 1);
        }
    }
}

