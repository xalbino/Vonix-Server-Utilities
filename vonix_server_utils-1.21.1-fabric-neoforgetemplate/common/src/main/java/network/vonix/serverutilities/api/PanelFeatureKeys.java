package network.vonix.serverutilities.api;

/**
 * Stable string keys for panel feature flags. Used by
 * {@link PanelCapabilities#featureEnabled(String)}.
 *
 * <p>Part of the {@code network.vonix.serverutilities.api} published SPI — SemVer stable.
 */
public final class PanelFeatureKeys {
    public static final String EVENTS = "panel_events_enabled";
    public static final String STATS = "panel_stats_enabled";
    public static final String DEATH_HISTORY = "panel_death_history_enabled";
    public static final String INVENTORY_READ = "panel_inventory_read_enabled";
    public static final String TELEPORT_ACTIONS = "panel_teleport_actions_enabled";

    private PanelFeatureKeys() {}
}
