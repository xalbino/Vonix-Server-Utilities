package network.vonix.serverutilities.api;

/**
 * Result of attempting to enqueue a {@link PanelEvent} for delivery.
 *
 * <p>Part of the {@code network.vonix.serverutilities.api} published SPI — SemVer stable.
 */
public enum PanelDelivery {
    ACCEPTED,
    DROPPED_OLDEST,
    DISABLED,
    REJECTED
}
