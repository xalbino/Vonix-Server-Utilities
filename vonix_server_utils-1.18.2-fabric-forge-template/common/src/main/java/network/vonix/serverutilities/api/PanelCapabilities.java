package network.vonix.serverutilities.api;

import java.util.Objects;

/**
 * Declared panel feature surface for the currently bound VSU instance.
 *
 * <p>{@link #lastDeathIsHistory()} is always {@code false} in this SPI revision;
 * last-death data is a single snapshot, not a history list.
 *
 * <p>Part of the {@code network.vonix.serverutilities.api} published SPI — SemVer stable.
 */
public record PanelCapabilities(
        boolean eventsEnabled,
        boolean statsEnabled,
        boolean lastDeathEnabled,
        boolean inventoryReadEnabled,
        boolean teleportActionsEnabled,
        boolean homesEnabled,
        boolean spawnEnabled,
        boolean lastDeathIsHistory,
        String vsuVersion,
        String minecraftVersion
) {
    public PanelCapabilities {
        Objects.requireNonNull(vsuVersion, "vsuVersion");
        Objects.requireNonNull(minecraftVersion, "minecraftVersion");
        lastDeathIsHistory = false;
    }

    public boolean featureEnabled(String featureKey) {
        if (PanelFeatureKeys.EVENTS.equals(featureKey)) return eventsEnabled;
        if (PanelFeatureKeys.STATS.equals(featureKey)) return statsEnabled;
        if (PanelFeatureKeys.DEATH_HISTORY.equals(featureKey)) return lastDeathEnabled;
        if (PanelFeatureKeys.INVENTORY_READ.equals(featureKey)) return inventoryReadEnabled;
        if (PanelFeatureKeys.TELEPORT_ACTIONS.equals(featureKey)) return teleportActionsEnabled;
        if ("homes".equals(featureKey)) return homesEnabled;
        if ("spawn".equals(featureKey)) return spawnEnabled;
        return false;
    }
}
