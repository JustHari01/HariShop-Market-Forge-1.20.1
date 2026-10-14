package hari.harishop.forge;

import com.mojang.brigadier.ParseResults;
import hari.harishop.HariShopMod;
import hari.harishop.v5.AdminCommandPolicy;
import hari.harishop.v5.ExactStackIdentity;
import hari.harishop.v5.ProvenanceMarker;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.CommandEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.item.ItemTossEvent;
import net.minecraftforge.event.entity.player.AnvilRepairEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Forge 1.20.1 provenance boundary.
 *
 * <p>The NeoForge port receives explicit hooks for command deltas, creative
 * inventory changes, crafting, smelting, anvil output and tosses. Forge has
 * the same semantic events under different packages, so this adapter keeps a
 * bounded pre/post snapshot and marks only the changed stack. Existing
 * survival inventory is never touched merely because its owner is an OP.
 * Marked stacks remain usable in-game but are rejected by HariShop and
 * Numismatic's {@code EconomicItemGuard}.</p>
 */
@Mod.EventBusSubscriber(modid = HariShopMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ForgeProvenanceEvents {
    private static final int MAX_TRACKED_PLAYERS = 256;
    private static final int SNAPSHOT_INTERVAL = 5;
    private static final Map<UUID, Map<Integer, ItemStack>> CREATIVE_BASELINES = new HashMap<>();

    private ForgeProvenanceEvents() {}

    /**
     * Forge fires CommandEvent before Brigadier executes the command. Capture
     * all online inventories, then compare on the server executor after the
     * command has mutated them. This covers /give, /item, /loot, /replaceitem,
     * /summon and wrapper commands such as /execute ... run give without
     * trusting a client target or command argument.
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onCommand(CommandEvent event) {
        if (event == null) return;
        try {
            ParseResults<CommandSourceStack> parsed = event.getParseResults();
            if (parsed == null || parsed.getContext() == null) return;
            CommandSourceStack source = parsed.getContext().getSource();
            if (source == null || !source.hasPermission(2) || !AdminCommandPolicy.isInjectionCommand(parsed.getReader().getString())) {
                return;
            }
            MinecraftServer server = source.getServer();
            if (server == null) return;
            Map<UUID, Map<Integer, ItemStack>> before = captureAll(server);
            server.execute(() -> {
                for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                    Map<Integer, ItemStack> snapshot = before.get(player.getUUID());
                    if (snapshot != null) markDelta(player, snapshot, "admin-command:" + player.getUUID());
                }
            });
        } catch (Throwable failure) {
            // A failure here does not grant value: the ordinary calculator
            // accepts only an explicitly certified source and the optional
            // Numi guard rejects any marker it can see.
            HariShopMod.LOGGER.warn("Unable to record Forge admin command provenance", failure);
        }
    }

    /** Detect Creative-tab additions without marking pre-existing survival items. */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event == null || event.phase != TickEvent.Phase.END || event.player.level().isClientSide
                || !(event.player instanceof ServerPlayer player) || !player.isCreative()
                || player.tickCount % SNAPSHOT_INTERVAL != 0) return;
        try {
            if (CREATIVE_BASELINES.size() >= MAX_TRACKED_PLAYERS && !CREATIVE_BASELINES.containsKey(player.getUUID())) {
                CREATIVE_BASELINES.clear();
            }
            Map<Integer, ItemStack> current = capture(player.getInventory());
            Map<Integer, ItemStack> before = CREATIVE_BASELINES.put(player.getUUID(), current);
            if (before != null) markDelta(player, before, "creative-mode:" + player.getUUID());
        } catch (Throwable failure) {
            HariShopMod.LOGGER.warn("Unable to record Forge creative provenance", failure);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onCrafted(PlayerEvent.ItemCraftedEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        ItemStack result = event.getCrafting();
        if (result == null || result.isEmpty()) return;
        if (player.isCreative() || player.hasPermissions(2)) {
            ProvenanceMarker.markDerived(result, "admin-craft:" + player.getUUID());
            return;
        }
        Container matrix = event.getInventory();
        if (matrix != null) {
            for (int slot = 0; slot < matrix.getContainerSize(); slot++) {
                if (ProvenanceMarker.has(matrix.getItem(slot))) {
                    ProvenanceMarker.propagate(result, matrix.getItem(slot));
                    return;
                }
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onSmelted(PlayerEvent.ItemSmeltedEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        ItemStack result = event.getSmelting();
        if ((player.isCreative() || player.hasPermissions(2)) && result != null && !result.isEmpty()) {
            ProvenanceMarker.markDerived(result, "admin-smelt:" + player.getUUID());
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onAnvil(AnvilRepairEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !player.hasPermissions(2)) return;
        ItemStack output = event.getOutput();
        if (output != null && !output.isEmpty()) ProvenanceMarker.markDerived(output, "admin-anvil:" + player.getUUID());
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onToss(ItemTossEvent event) {
        if (event == null || !(event.getPlayer() instanceof ServerPlayer player) || !player.isCreative()) return;
        ItemStack stack = event.getEntity() == null ? ItemStack.EMPTY : event.getEntity().getItem();
        if (stack != null && !stack.isEmpty()) ProvenanceMarker.markAdmin(stack, "admin-toss:" + player.getUUID());
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event != null && event.getEntity() != null) CREATIVE_BASELINES.remove(event.getEntity().getUUID());
    }

    private static Map<UUID, Map<Integer, ItemStack>> captureAll(MinecraftServer server) {
        Map<UUID, Map<Integer, ItemStack>> result = new HashMap<>();
        List<ServerPlayer> players = server.getPlayerList().getPlayers();
        int start = Math.max(0, players.size() - MAX_TRACKED_PLAYERS);
        for (int i = start; i < players.size(); i++) {
            ServerPlayer player = players.get(i);
            result.put(player.getUUID(), capture(player.getInventory()));
        }
        return result;
    }

    private static Map<Integer, ItemStack> capture(Inventory inventory) {
        Map<Integer, ItemStack> result = new HashMap<>();
        if (inventory == null) return result;
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            result.put(slot, stack == null ? ItemStack.EMPTY : stack.copy());
        }
        return result;
    }

    private static void markDelta(ServerPlayer player, Map<Integer, ItemStack> before, String reason) {
        if (player == null || before == null) return;
        Inventory inventory = player.getInventory();
        boolean changed = false;
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack now = inventory.getItem(slot);
            if (now == null || now.isEmpty() || ProvenanceMarker.has(now)) continue;
            ItemStack old = before.getOrDefault(slot, ItemStack.EMPTY);
            boolean identityChanged = old.isEmpty() || !sameIdentity(old, now);
            boolean countIncreased = !old.isEmpty() && now.getCount() > old.getCount();
            if (identityChanged || countIncreased) {
                ProvenanceMarker.markAdmin(now, reason + ":slot=" + slot);
                changed = true;
            }
        }
        if (changed) inventory.setChanged();
    }

    private static boolean sameIdentity(ItemStack first, ItemStack second) {
        try { return ExactStackIdentity.encode(first).sha256().equals(ExactStackIdentity.encode(second).sha256()); }
        catch (Throwable invalid) { return false; }
    }
}
