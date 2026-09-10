package network.vonix.serverutilities.api;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Immutable snapshot of a player's inventory from one source.
 *
 * <p>Part of the {@code network.vonix.serverutilities.api} published SPI — SemVer stable.
 */
public record InventorySnapshot(UUID playerId, String sourceId, String title, List<InventorySlotSnapshot> slots) {
    public InventorySnapshot {
        Objects.requireNonNull(playerId, "playerId");
        Objects.requireNonNull(sourceId, "sourceId");
        Objects.requireNonNull(title, "title");
        Objects.requireNonNull(slots, "slots");
        slots = List.copyOf(slots);
        if (sourceId.isBlank()) {
            throw new IllegalArgumentException("source id must not be blank");
        }
    }
}
