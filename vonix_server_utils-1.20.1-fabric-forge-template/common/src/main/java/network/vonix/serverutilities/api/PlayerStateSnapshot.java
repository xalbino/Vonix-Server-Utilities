package network.vonix.serverutilities.api;

import java.util.Objects;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.UUID;

/**
 * Immutable snapshot of a player's online state and optional vitals.
 *
 * <p>Part of the {@code network.vonix.serverutilities.api} published SPI — SemVer stable.
 */
public record PlayerStateSnapshot(
        UUID playerId,
        String username,
        boolean online,
        Optional<String> dimension,
        OptionalDouble health,
        OptionalDouble maxHealth,
        OptionalInt hunger,
        OptionalInt armor,
        OptionalInt xpLevel,
        OptionalLong playtimeTicks,
        OptionalLong lastSeenEpochMs
) {
    public PlayerStateSnapshot {
        Objects.requireNonNull(playerId, "playerId");
        if (username == null) username = "";
        if (dimension == null) dimension = Optional.empty();
        if (health == null) health = OptionalDouble.empty();
        if (maxHealth == null) maxHealth = OptionalDouble.empty();
        if (hunger == null) hunger = OptionalInt.empty();
        if (armor == null) armor = OptionalInt.empty();
        if (xpLevel == null) xpLevel = OptionalInt.empty();
        if (playtimeTicks == null) playtimeTicks = OptionalLong.empty();
        if (lastSeenEpochMs == null) lastSeenEpochMs = OptionalLong.empty();
    }

    public static PlayerStateSnapshot unknown(UUID playerId) {
        return new PlayerStateSnapshot(
                playerId,
                "",
                false,
                Optional.empty(),
                OptionalDouble.empty(),
                OptionalDouble.empty(),
                OptionalInt.empty(),
                OptionalInt.empty(),
                OptionalInt.empty(),
                OptionalLong.empty(),
                OptionalLong.empty());
    }
}
