package network.vonix.serverutilities.api;

import java.util.Optional;

/**
 * Static accessor for the currently bound {@link VonixPanel}.
 *
 * <p>Returns {@link Optional#empty()} when no panel is bound. Binding is
 * performed by VSU core internals, not by this SPI type.
 *
 * <p>Part of the {@code network.vonix.serverutilities.api} published SPI — SemVer stable.
 */
public final class VonixPanels {
    private VonixPanels() {}

    public static Optional<VonixPanel> current() {
        return Optional.empty();
    }
}
