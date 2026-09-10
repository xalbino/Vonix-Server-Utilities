package network.vonix.serverutilities.kits;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * SQLite persistence for kit claims. Cooldowns and one-time flags are enforced
 * per {@code claim_group}: a player may hold at most one active claim inside a
 * group, while kits in different groups stay independent.
 *
 * {@link #migrateSchema(Connection)} is idempotent and safe on both fresh
 * four-column tables and legacy {@code (uuid, kit_name, last_used)} databases.
 * It never drops the table or rewrites rows that already have a group.
 */
public final class KitCooldownStore {
    public static final String TABLE = "vsu_kit_cooldowns";

    /** In-process mutex so two threads cannot both pass a check-then-insert. */
    private static final Object CLAIM_LOCK = new Object();

    public enum ClaimStatus { SUCCESS, ON_COOLDOWN, ALREADY_CLAIMED }

    public record ClaimOutcome(ClaimStatus status, int remainingSeconds) {
        public static ClaimOutcome success() {
            return new ClaimOutcome(ClaimStatus.SUCCESS, 0);
        }
        public static ClaimOutcome alreadyClaimed() {
            return new ClaimOutcome(ClaimStatus.ALREADY_CLAIMED, 0);
        }
        public static ClaimOutcome onCooldown(int seconds) {
            return new ClaimOutcome(ClaimStatus.ON_COOLDOWN, seconds);
        }
    }

    private KitCooldownStore() {}

    /** Create the kit-cooldown table if needed, then apply the group-column migration. */
    public static void createTable(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("""
                CREATE TABLE IF NOT EXISTS vsu_kit_cooldowns (
                    uuid        TEXT NOT NULL,
                    kit_name    TEXT NOT NULL,
                    claim_group TEXT NOT NULL,
                    last_used   INTEGER NOT NULL,
                    PRIMARY KEY (uuid, kit_name)
                )""");
            statement.execute("PRAGMA busy_timeout=5000");
        }
        migrateSchema(connection);
    }

    /**
     * Add {@code claim_group} when the legacy three-column layout is present,
     * backfill only empty groups from {@code kit_name}, and ensure the group
     * lookup index exists. Safe to rerun. Does not delete rows or change
     * {@code last_used} / {@code kit_name} / {@code uuid}.
     */
    public static void migrateSchema(Connection connection) throws SQLException {
        Set<String> columns = columnsOf(connection, TABLE);
        if (columns.isEmpty()) {
            return;
        }
        if (!columns.contains("claim_group")) {
            try (Statement statement = connection.createStatement()) {
                statement.execute(
                        "ALTER TABLE vsu_kit_cooldowns ADD COLUMN claim_group TEXT NOT NULL DEFAULT ''");
            }
        }
        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA busy_timeout=5000");
            statement.execute("""
                UPDATE vsu_kit_cooldowns
                   SET claim_group = lower(kit_name)
                 WHERE claim_group IS NULL OR trim(claim_group) = ''
                """);
            statement.execute(
                    "CREATE INDEX IF NOT EXISTS idx_vsu_kit_cooldowns_group ON vsu_kit_cooldowns (uuid, claim_group)");
        }
    }

    /**
     * Atomically check group eligibility and record the claim.
     * A process-wide lock plus {@code BEGIN IMMEDIATE} keep two callers from
     * both observing an empty group window and both succeeding.
     */
    public static ClaimOutcome claim(
            Connection connection,
            UUID uuid,
            String kitName,
            String group,
            boolean oneTime,
            int cooldownSeconds,
            long nowSeconds) throws SQLException {
        synchronized (CLAIM_LOCK) {
            SQLException last = null;
            for (int attempt = 0; attempt < 8; attempt++) {
                try {
                    return claimOnce(connection, uuid, kitName, group, oneTime, cooldownSeconds, nowSeconds);
                } catch (SQLException e) {
                    last = e;
                    if (!isRetryableLock(e)) throw e;
                    try {
                        Thread.sleep(15L * (attempt + 1));
                    } catch (InterruptedException interrupted) {
                        Thread.currentThread().interrupt();
                        throw e;
                    }
                }
            }
            throw last;
        }
    }

    private static ClaimOutcome claimOnce(
            Connection connection,
            UUID uuid,
            String kitName,
            String group,
            boolean oneTime,
            int cooldownSeconds,
            long nowSeconds) throws SQLException {
        String kit = kitName == null ? "" : kitName.trim().toLowerCase(Locale.ROOT);
        String claimGroup = KitGroupRules.normalizeGroup(group, kit);
        if (kit.isEmpty() || claimGroup == null) {
            throw new SQLException("kit name/group required");
        }

        boolean startedTransaction = false;
        try {
            if (connection.getAutoCommit()) {
                try (Statement statement = connection.createStatement()) {
                    statement.execute("BEGIN IMMEDIATE");
                }
                startedTransaction = true;
            }
            long lastUsed = lastGroupUsed(connection, uuid, claimGroup);
            if (lastUsed > 0) {
                if (oneTime) {
                    commitIfStarted(connection, startedTransaction);
                    return ClaimOutcome.alreadyClaimed();
                }
                long remaining = (lastUsed + (long) cooldownSeconds) - nowSeconds;
                if (remaining > 0) {
                    commitIfStarted(connection, startedTransaction);
                    int seconds = remaining > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) remaining;
                    return ClaimOutcome.onCooldown(seconds);
                }
            }
            try (PreparedStatement statement = connection.prepareStatement(
                    "INSERT OR REPLACE INTO vsu_kit_cooldowns (uuid, kit_name, claim_group, last_used) VALUES(?,?,?,?)")) {
                statement.setString(1, uuid.toString());
                statement.setString(2, kit);
                statement.setString(3, claimGroup);
                statement.setLong(4, nowSeconds);
                statement.executeUpdate();
            }
            commitIfStarted(connection, startedTransaction);
            return ClaimOutcome.success();
        } catch (SQLException e) {
            if (startedTransaction) {
                try (Statement statement = connection.createStatement()) {
                    statement.execute("ROLLBACK");
                } catch (SQLException ignore) {
                    // keep original
                }
            }
            throw e;
        }
    }

    public static long lastGroupUsed(Connection connection, UUID uuid, String group) throws SQLException {
        String claimGroup = group == null ? "" : group.trim().toLowerCase(Locale.ROOT);
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT MAX(last_used) FROM vsu_kit_cooldowns WHERE uuid=? AND claim_group=?")) {
            statement.setString(1, uuid.toString());
            statement.setString(2, claimGroup);
            ResultSet rs = statement.executeQuery();
            if (rs.next()) return rs.getLong(1);
        }
        return 0;
    }

    static Set<String> columnsOf(Connection connection, String table) throws SQLException {
        Set<String> columns = new HashSet<>();
        try (Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery("PRAGMA table_info(" + table + ")")) {
            while (rs.next()) {
                String name = rs.getString("name");
                if (name != null) columns.add(name.toLowerCase(Locale.ROOT));
            }
        }
        return columns;
    }

    private static void commitIfStarted(Connection connection, boolean startedTransaction) throws SQLException {
        if (!startedTransaction) return;
        try (Statement statement = connection.createStatement()) {
            statement.execute("COMMIT");
        }
    }

    private static boolean isRetryableLock(SQLException e) {
        String message = e.getMessage();
        if (message == null) return false;
        String upper = message.toUpperCase(Locale.ROOT);
        return upper.contains("BUSY") || upper.contains("LOCKED");
    }
}
