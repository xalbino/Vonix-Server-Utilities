package network.vonix.serverutilities.api;

import java.util.Objects;

/**
 * Immutable snapshot of a single inventory slot.
 *
 * <p>Part of the {@code network.vonix.serverutilities.api} published SPI — SemVer stable.
 */
public record InventorySlotSnapshot(int index, String itemId, int count) {
    public InventorySlotSnapshot {
        Objects.requireNonNull(itemId, "itemId");
        if (index < 0) {
            throw new IllegalArgumentException("slot index must be non-negative");
        }
        if (count < 0) {
            throw new IllegalArgumentException("count must be non-negative");
        }
        if (itemId.isBlank()) {
            throw new IllegalArgumentException("item id must not be blank");
        }
    }
}
