package hari.harishop.v5;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Version-pinned quarantine adapters for economic mods that are present in the
 * current pack but do not expose HariShop's transaction API.
 *
 * <p>The adapter does not make a third-party shop a money issuer.  Its policy
 * is the opposite: every item/currency ingress from that surface is treated
 * as UNKNOWN by HariShop's ingress fence, and therefore cannot become an
 * eligible pawn lot or a Numismatic physical lot.  The exact JAR digest is
 * part of the certificate boundary; updating a surface without adding a
 * reviewed adapter locks certification again.</p>
 */
public final class ExternalEconomicSurfaceQuarantine {
    public record Surface(String modId, String jarSha256, String policyVersion) {
        public Surface {
            if (modId == null || modId.isBlank() || jarSha256 == null || jarSha256.length() != 64
                    || policyVersion == null || policyVersion.isBlank()) {
                throw new IllegalArgumentException("invalid external surface");
            }
            modId = modId.trim().toLowerCase(java.util.Locale.ROOT);
            jarSha256 = jarSha256.trim().toUpperCase(java.util.Locale.ROOT);
            if (!jarSha256.matches("[0-9A-F]{64}")) throw new IllegalArgumentException("invalid surface digest");
            policyVersion = policyVersion.trim();
        }
    }

    private static final Map<String, Surface> KNOWN = known();

    private ExternalEconomicSurfaceQuarantine() {}

    private static Map<String, Surface> known() {
        Map<String, Surface> values = new LinkedHashMap<>();
        values.put("bountiful", new Surface("bountiful",
                "DCA5E0AF724E3076B3D950B7EC88CF63C509435CF41371A6E1C7C306CB6A7E51", "quarantine-v1"));
        values.put("numismatic_reforged_bounties", new Surface("numismatic_reforged_bounties",
                "8F72F54787AB3A38062541F1EF71382FCCB216199FCC8C05FDA5C283B9138467", "quarantine-v1"));
        values.put("shoppy", new Surface("shoppy",
                "949788DE5EA6EE7EA94F6BC8401FA5BC0DF8D5430A428DD524997EAAC4684640", "quarantine-v1"));
        values.put("spudaciousshops", new Surface("spudaciousshops",
                "5C0F74A5DB2FCE03945362A185ACFA940D3B58C786F0FD8D365666488AD317E6", "quarantine-v1"));
        values.put("village_bounties", new Surface("village_bounties",
                "502D8A4EF2BD2A4B1278483112F9A14D122BA78274092FB66B3AB674CAAB1778", "quarantine-v1"));
        return Map.copyOf(values);
    }

    public static Set<String> knownIds() { return KNOWN.keySet(); }

    public static Surface known(String modId) {
        return modId == null ? null : KNOWN.get(modId.trim().toLowerCase(java.util.Locale.ROOT));
    }

    /** Returns true only when every loaded surface is pinned to this adapter. */
    public static boolean certified(Map<String, String> loadedDigests) {
        if (loadedDigests == null) return false;
        for (Map.Entry<String, String> entry : loadedDigests.entrySet()) {
            Surface surface = known(entry.getKey());
            if (surface == null || entry.getValue() == null
                    || !surface.jarSha256().equalsIgnoreCase(entry.getValue())) return false;
        }
        return true;
    }

    /** This adapter intentionally never authorizes a positive external payout. */
    public static boolean failClosedIngress() { return true; }
}


