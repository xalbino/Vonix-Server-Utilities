package network.vonix.serverutilities.api;

import java.util.Locale;
import java.util.Objects;

/**
 * Sealed teleport destination. Only spawn and a named home are permitted;
 * arbitrary coordinates and commands are rejected by {@link PanelTeleportRules}.
 *
 * <p>Part of the {@code network.vonix.serverutilities.api} published SPI — SemVer stable.
 */
public sealed interface PanelTeleportTarget permits PanelTeleportTarget.Home, PanelTeleportTarget.Spawn {
    record Home(String name) implements PanelTeleportTarget {
        public Home {
            Objects.requireNonNull(name, "name");
            name = name.trim().toLowerCase(Locale.ROOT);
            if (name.isBlank()) {
                throw new IllegalArgumentException("home name must not be blank");
            }
        }
    }

    record Spawn() implements PanelTeleportTarget {}
}
