package hari.harishop.v5;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

/** NBT representation of the server-side provenance boundary on 1.20.1. */
public final class ProvenanceMarker {
    public static final String ROOT = "harishop_provenance";
    public static final String CLASS = "origin_class";
    public static final String REASON = "reason";
    public static final String MARKED_AT = "marked_at";

    private ProvenanceMarker() {}

    public static boolean has(ItemStack stack) {
        return stack != null && !stack.isEmpty() && stack.hasTag()
                && stack.getTag().contains(ROOT, CompoundTag.TAG_COMPOUND);
    }

    public static void mark(ItemStack stack, ProvenanceLedger.OriginClass origin, String reason, long nowMillis) {
        if (stack == null || stack.isEmpty() || origin == null) return;
        CompoundTag marker = new CompoundTag();
        marker.putString(CLASS, origin.name());
        if (reason != null && !reason.isBlank()) marker.putString(REASON, reason.substring(0, Math.min(128, reason.length())));
        marker.putLong(MARKED_AT, Math.max(0L, nowMillis));
        stack.getOrCreateTag().put(ROOT, marker);
    }

    public static void markAdmin(ItemStack stack, String reason) {
        if (!has(stack)) mark(stack, ProvenanceLedger.OriginClass.ADMIN_INJECTED, reason, System.currentTimeMillis());
    }

    public static void markDerived(ItemStack stack, String reason) {
        if (!has(stack)) mark(stack, ProvenanceLedger.OriginClass.ADMIN_DERIVED, reason, System.currentTimeMillis());
    }

    /** Copies an existing quarantine marker to a transformed output. */
    public static void propagate(ItemStack output, ItemStack... inputs) {
        if (output == null || output.isEmpty() || inputs == null || has(output)) return;
        for (ItemStack input : inputs) {
            if (has(input)) {
                output.getOrCreateTag().put(ROOT, input.getTag().getCompound(ROOT).copy());
                return;
            }
        }
    }

    public static ProvenanceLedger.OriginClass origin(ItemStack stack) {
        if (!has(stack)) return ProvenanceLedger.OriginClass.UNKNOWN;
        String value = stack.getTag().getCompound(ROOT).getString(CLASS);
        try { return ProvenanceLedger.OriginClass.valueOf(value); }
        catch (IllegalArgumentException ignored) { return ProvenanceLedger.OriginClass.UNKNOWN; }
    }
}
