package network.vonix.serverutilities.api;

import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;

/**
 * Immutable snapshot of a player's last death location and optional metadata.
 *
 * <p>Part of the {@code network.vonix.serverutilities.api} published SPI — SemVer stable.
 */
public record LastDeathSnapshot(
        String world,
        double x,
        double y,
        double z,
        float yaw,
        float pitch,
        Optional<String> cause,
        Optional<String> killer,
        OptionalLong occurredAtEpochMs
) {
    public LastDeathSnapshot {
        Objects.requireNonNull(world, "world");
        if (cause == null) cause = Optional.empty();
        if (killer == null) killer = Optional.empty();
        if (occurredAtEpochMs == null) occurredAtEpochMs = OptionalLong.empty();
        if (world.isBlank()) {
            throw new IllegalArgumentException("death world must not be blank");
        }
    }

    public static LastDeathSnapshot locationOnly(
            String world, double x, double y, double z, float yaw, float pitch, long occurredAtEpochMs) {
        return new LastDeathSnapshot(
                world, x, y, z, yaw, pitch, Optional.empty(), Optional.empty(), OptionalLong.of(occurredAtEpochMs));
    }
}
