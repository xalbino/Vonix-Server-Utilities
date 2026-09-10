package network.vonix.serverutilities.api;

/**
 * Callback for {@link PanelEvent} delivery.
 *
 * <p>Part of the {@code network.vonix.serverutilities.api} published SPI — SemVer stable.
 */
@FunctionalInterface
public interface PanelListener {
    void onPanelEvent(PanelEvent event);
}
