package network.vonix.serverutilities.api;

import java.util.Objects;

/**
 * Immutable snapshot of server identity and player counts.
 *
 * <p>Part of the {@code network.vonix.serverutilities.api} published SPI — SemVer stable.
 */
public record ServerSnapshot(String vsuVersion, String minecraftVersion, int onlinePlayers, int maxPlayers) {
    public ServerSnapshot {
        Objects.requireNonNull(vsuVersion, "vsuVersion");
        Objects.requireNonNull(minecraftVersion, "minecraftVersion");
        if (onlinePlayers < 0) {
            throw new IllegalArgumentException("onlinePlayers");
        }
        if (maxPlayers < 0) {
            throw new IllegalArgumentException("maxPlayers");
        }
    }
}
