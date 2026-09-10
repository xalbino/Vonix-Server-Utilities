package network.vonix.serverutilities.api;

import java.util.Objects;

/**
 * Outcome of evaluating or executing a {@link PanelTeleportRequest}.
 *
 * <p>{@link #accepted()} is true for {@link PanelTeleportStatus#ACCEPTED} and
 * {@link PanelTeleportStatus#QUEUED}.
 *
 * <p>Part of the {@code network.vonix.serverutilities.api} published SPI — SemVer stable.
 */
public record PanelTeleportResult(PanelTeleportStatus status, String message) {
    public PanelTeleportResult {
        Objects.requireNonNull(status, "status");
        if (message == null) message = "";
    }

    public boolean accepted() {
        return status == PanelTeleportStatus.ACCEPTED || status == PanelTeleportStatus.QUEUED;
    }

    public static PanelTeleportResult of(PanelTeleportStatus status, String message) {
        return new PanelTeleportResult(status, message);
    }
}
