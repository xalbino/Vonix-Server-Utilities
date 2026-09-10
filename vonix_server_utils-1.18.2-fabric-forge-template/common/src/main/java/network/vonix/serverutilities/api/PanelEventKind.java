package network.vonix.serverutilities.api;

/**
 * Kind of a {@link PanelEvent}.
 *
 * <p>Part of the {@code network.vonix.serverutilities.api} published SPI — SemVer stable.
 */
public enum PanelEventKind {
    PLAYER_JOIN,
    PLAYER_QUIT,
    PLAYER_DEATH,
    TELEPORT_COMPLETED
}
