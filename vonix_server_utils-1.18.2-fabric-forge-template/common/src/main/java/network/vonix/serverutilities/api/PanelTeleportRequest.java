package network.vonix.serverutilities.api;

import java.util.Objects;
import java.util.UUID;

/**
 * Request to teleport a player to a {@link PanelTeleportTarget}.
 *
 * <p>Factory methods {@link #spawn(UUID)} and {@link #home(UUID, String)} set
 * actor and target to the same player id (self-teleport).
 *
 * <p>Part of the {@code network.vonix.serverutilities.api} published SPI — SemVer stable.
 */
public record PanelTeleportRequest(UUID actorPlayerId, UUID targetPlayerId, PanelTeleportTarget target) {
    public PanelTeleportRequest {
        Objects.requireNonNull(actorPlayerId, "actorPlayerId");
        Objects.requireNonNull(targetPlayerId, "targetPlayerId");
        Objects.requireNonNull(target, "target");
    }

    public static PanelTeleportRequest spawn(UUID playerId) {
        return new PanelTeleportRequest(playerId, playerId, new PanelTeleportTarget.Spawn());
    }

    public static PanelTeleportRequest home(UUID playerId, String homeName) {
        return new PanelTeleportRequest(playerId, playerId, new PanelTeleportTarget.Home(homeName));
    }
}
