package network.vonix.serverutilities.api;

/**
 * Outcome of a panel teleport request.
 *
 * <p>Part of the {@code network.vonix.serverutilities.api} published SPI — SemVer stable.
 */
public enum PanelTeleportStatus {
    ACCEPTED,
    QUEUED,
    DISABLED,
    WRONG_IDENTITY,
    PLAYER_OFFLINE,
    HOME_MISSING,
    FEATURE_DISABLED,
    WORLD_UNAVAILABLE,
    PERMISSION_DENIED,
    NOT_ON_SERVER_THREAD,
    COOLDOWN,
    REJECTED
}
