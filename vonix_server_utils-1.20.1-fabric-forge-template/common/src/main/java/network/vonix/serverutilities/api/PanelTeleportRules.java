package network.vonix.serverutilities.api;

import java.util.Objects;

/**
 * Pure evaluation of a {@link PanelTeleportRequest} against capability and
 * runtime flags. Does not perform a teleport.
 *
 * <p>Part of the {@code network.vonix.serverutilities.api} published SPI — SemVer stable.
 */
public final class PanelTeleportRules {
    private PanelTeleportRules() {}

    public static PanelTeleportResult evaluate(
            PanelTeleportRequest request,
            boolean teleportActionsEnabled,
            boolean playerOnline,
            boolean spawnEnabled,
            boolean homesEnabled,
            boolean homeExists,
            boolean hasPermission,
            boolean onCooldown) {
        Objects.requireNonNull(request, "request");
        if (!teleportActionsEnabled) {
            return PanelTeleportResult.of(PanelTeleportStatus.DISABLED, "outside-mod teleport actions are disabled");
        }
        if (!request.actorPlayerId().equals(request.targetPlayerId())) {
            return PanelTeleportResult.of(PanelTeleportStatus.WRONG_IDENTITY, "actor must be the target player");
        }
        if (!playerOnline) {
            return PanelTeleportResult.of(PanelTeleportStatus.PLAYER_OFFLINE, "player is not online");
        }
        if (!hasPermission) {
            return PanelTeleportResult.of(PanelTeleportStatus.PERMISSION_DENIED, "missing spawn or home permission");
        }
        if (onCooldown) {
            return PanelTeleportResult.of(PanelTeleportStatus.COOLDOWN, "teleport request is on cooldown");
        }
        if (request.target() instanceof PanelTeleportTarget.Spawn) {
            if (!spawnEnabled) {
                return PanelTeleportResult.of(PanelTeleportStatus.FEATURE_DISABLED, "spawn feature is disabled");
            }
            return PanelTeleportResult.of(PanelTeleportStatus.ACCEPTED, "spawn");
        }
        if (request.target() instanceof PanelTeleportTarget.Home) {
            if (!homesEnabled) {
                return PanelTeleportResult.of(PanelTeleportStatus.FEATURE_DISABLED, "homes feature is disabled");
            }
            if (!homeExists) {
                return PanelTeleportResult.of(PanelTeleportStatus.HOME_MISSING, "named home does not exist");
            }
            return PanelTeleportResult.of(PanelTeleportStatus.ACCEPTED, "home");
        }
        return rejectUnsupportedTarget();
    }

    /**
     * Convenience overload that assumes permission is granted and the request
     * is not on cooldown.
     */
    public static PanelTeleportResult evaluate(
            PanelTeleportRequest request,
            boolean teleportActionsEnabled,
            boolean playerOnline,
            boolean spawnEnabled,
            boolean homesEnabled,
            boolean homeExists) {
        return evaluate(request, teleportActionsEnabled, playerOnline, spawnEnabled, homesEnabled, homeExists, true, false);
    }

    public static PanelTeleportResult rejectArbitraryCoordinates() {
        return PanelTeleportResult.of(PanelTeleportStatus.REJECTED, "arbitrary coordinates are not a panel teleport target");
    }

    public static PanelTeleportResult rejectArbitraryCommand() {
        return PanelTeleportResult.of(PanelTeleportStatus.REJECTED, "arbitrary commands are not a panel teleport target");
    }

    public static PanelTeleportResult rejectUnsupportedTarget() {
        return PanelTeleportResult.of(PanelTeleportStatus.REJECTED, "unsupported panel teleport target");
    }
}
