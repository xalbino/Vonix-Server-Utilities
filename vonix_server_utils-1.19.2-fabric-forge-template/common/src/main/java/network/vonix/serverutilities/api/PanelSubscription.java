package network.vonix.serverutilities.api;

/**
 * Handle for a registered {@link PanelListener}. Closing unregisters the listener.
 *
 * <p>Part of the {@code network.vonix.serverutilities.api} published SPI — SemVer stable.
 */
public interface PanelSubscription extends AutoCloseable {
    boolean isActive();

    @Override
    void close();
}
