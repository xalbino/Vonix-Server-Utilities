package network.vonix.serverutilities.api;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Immutable panel event delivered to {@link PanelListener} subscribers.
 *
 * <p>Part of the {@code network.vonix.serverutilities.api} published SPI — SemVer stable.
 */
public record PanelEvent(
        PanelEventKind kind,
        UUID playerId,
        String username,
        long occurredAtEpochMs,
        Optional<String> detail
) {
    public PanelEvent {
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(playerId, "playerId");
        if (username == null) username = "";
        if (detail == null) detail = Optional.empty();
    }
}
