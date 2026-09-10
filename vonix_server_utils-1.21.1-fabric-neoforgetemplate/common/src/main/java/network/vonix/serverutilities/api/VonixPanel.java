package network.vonix.serverutilities.api;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Main panel SPI facade. Implementations are bound by VSU internals and
 * obtained via {@link VonixPanels#current()}.
 *
 * <p>Part of the {@code network.vonix.serverutilities.api} published SPI — SemVer stable.
 */
public interface VonixPanel {
    PanelCapabilities capabilities();

    ServerSnapshot server();

    List<HomeSnapshot> listHomes(UUID playerId);

    Optional<HomeSnapshot> getHome(UUID playerId, String name);

    Optional<LastDeathSnapshot> lastDeath(UUID playerId);

    Optional<PlayerStateSnapshot> playerState(UUID playerId);

    List<InventorySnapshot> inventory(UUID playerId);

    PanelTeleportResult requestTeleport(PanelTeleportRequest request);

    Optional<PanelSubscription> registerListener(PanelListener listener);
}
