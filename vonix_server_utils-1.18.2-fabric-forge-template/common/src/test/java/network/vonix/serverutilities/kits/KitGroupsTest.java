package network.vonix.serverutilities.kits;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Deterministic Kit Groups checks: JSON defaulting, claim semantics,
 * legacy schema migration, idempotent rerun, data preservation, and
 * concurrent same-group claims.
 */
public final class KitGroupsTest {
    private KitGroupsTest() {}

    public static void main(String[] args) throws Exception {
        Class.forName("org.sqlite.JDBC");
        testJsonGroupParsingAndDefaulting();
        testMalformedGroupNormalisation();
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:")) {
            KitCooldownStore.createTable(connection);
            testSameGroupBlocking(connection);
            testDifferentGroupIndependence(connection);
            testOneTimeBehavior(connection);
        }
        testLegacySchemaMigrationAndIdempotentRerun();
        testSourceDataPreservation();
        testConcurrentClaimSafety();
        System.out.println("KitGroupsTest: PASS");
    }

    private static void testJsonGroupParsingAndDefaulting() {
        List<KitGroupRules.ParsedKit> kits = KitGroupRules.parseKitsJson("""
                {
                  "kits": [
                    {"name": "Warrior", "group": "Class", "cooldown_seconds": 60, "one_time": true,
                     "items": [{"item": "minecraft:iron_sword", "count": 1}]},
                    {"name": "mage", "cooldown_seconds": 60, "one_time": true},
                    {"name": "food", "group": "", "cooldown_seconds": 10, "one_time": false}
                  ]
                }
                """);
        require(kits.size() == 3, "expected 3 parsed kits, got " + kits.size());
        require(kits.get(0).name().equals("warrior"), "name must be lower-cased");
        require(kits.get(0).group().equals("class"), "explicit group must be lower-cased");
        require(kits.get(0).oneTime(), "warrior is one-time");
        require(kits.get(1).group().equals("mage"), "missing group defaults to kit name");
        require(kits.get(2).group().equals("food"), "blank group defaults to kit name");
        require(KitGroupRules.normalizeGroup(null, "Starter").equals("starter"), "null group defaults to name");
        require(KitGroupRules.normalizeGroup("   ", "Tools").equals("tools"), "whitespace group defaults to name");
        require(KitGroupRules.normalizeName("  ") == null, "blank name rejected");
        require(KitGroupRules.parseKitsJson("{\"kits\":[{\"cooldown_seconds\":1}]}").isEmpty(),
                "nameless entries are skipped");
    }

    private static void testMalformedGroupNormalisation() {
        List<KitGroupRules.ParsedKit> kits = KitGroupRules.parseKitsJson("""
                {
                  "kits": [
                    {"name": "a", "group": null},
                    {"name": "b", "group": {"nested": true}},
                    {"name": "c", "group": ["x"]},
                    {"name": "d", "group": 12},
                    {"name": "e", "group": "  Class-A  "}
                  ]
                }
                """);
        require(kits.size() == 5, "malformed entries still produce kits when named");
        require(kits.get(0).group().equals("a"), "JSON null group defaults to name");
        require(kits.get(1).group().equals("b"), "object group defaults to name");
        require(kits.get(2).group().equals("c"), "array group defaults to name");
        require(kits.get(3).group().equals("12"), "numeric group uses deterministic string form");
        require(kits.get(4).group().equals("class-a"), "valid group is trimmed and lower-cased");
    }

    private static void testSameGroupBlocking(Connection connection) throws Exception {
        UUID player = UUID.fromString("00000000-0000-0000-0000-000000000001");
        KitCooldownStore.ClaimOutcome first = KitCooldownStore.claim(
                connection, player, "warrior", "class", false, 3600, 1_000L);
        require(first.status() == KitCooldownStore.ClaimStatus.SUCCESS, "first class kit must succeed");
        KitCooldownStore.ClaimOutcome second = KitCooldownStore.claim(
                connection, player, "mage", "class", false, 3600, 1_010L);
        require(second.status() == KitCooldownStore.ClaimStatus.ON_COOLDOWN,
                "second kit in the same group must be blocked");
        require(second.remainingSeconds() == 3590, "remaining cooldown is against the group last-used");
        KitCooldownStore.ClaimOutcome after = KitCooldownStore.claim(
                connection, player, "mage", "class", false, 3600, 1_000L + 3600);
        require(after.status() == KitCooldownStore.ClaimStatus.SUCCESS,
                "same group is claimable again after cooldown");
    }

    private static void testDifferentGroupIndependence(Connection connection) throws Exception {
        UUID player = UUID.fromString("00000000-0000-0000-0000-000000000002");
        KitCooldownStore.ClaimOutcome classKit = KitCooldownStore.claim(
                connection, player, "warrior", "class", false, 3600, 2_000L);
        KitCooldownStore.ClaimOutcome food = KitCooldownStore.claim(
                connection, player, "food", "food", false, 1800, 2_001L);
        require(classKit.status() == KitCooldownStore.ClaimStatus.SUCCESS, "class kit claim");
        require(food.status() == KitCooldownStore.ClaimStatus.SUCCESS,
                "kits in a different group remain independently claimable");
    }

    private static void testOneTimeBehavior(Connection connection) throws Exception {
        UUID player = UUID.fromString("00000000-0000-0000-0000-000000000003");
        KitCooldownStore.ClaimOutcome first = KitCooldownStore.claim(
                connection, player, "warrior", "class", true, 60, 3_000L);
        require(first.status() == KitCooldownStore.ClaimStatus.SUCCESS, "first one-time group claim");
        KitCooldownStore.ClaimOutcome same = KitCooldownStore.claim(
                connection, player, "warrior", "class", true, 60, 9_000L);
        require(same.status() == KitCooldownStore.ClaimStatus.ALREADY_CLAIMED,
                "one-time kit cannot be reclaimed after cooldown would have expired");
        KitCooldownStore.ClaimOutcome other = KitCooldownStore.claim(
                connection, player, "mage", "class", true, 60, 9_001L);
        require(other.status() == KitCooldownStore.ClaimStatus.ALREADY_CLAIMED,
                "one-time group blocks the other kit in the group forever");
        KitCooldownStore.ClaimOutcome independent = KitCooldownStore.claim(
                connection, player, "starter", "starter", true, 60, 9_002L);
        require(independent.status() == KitCooldownStore.ClaimStatus.SUCCESS,
                "one-time kits in another group stay independent");
    }

    private static void testLegacySchemaMigrationAndIdempotentRerun() throws Exception {
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:")) {
            try (Statement statement = connection.createStatement()) {
                statement.execute("""
                    CREATE TABLE vsu_kit_cooldowns (
                        uuid TEXT NOT NULL,
                        kit_name TEXT NOT NULL,
                        last_used INTEGER NOT NULL,
                        PRIMARY KEY (uuid, kit_name)
                    )""");
                statement.execute("INSERT INTO vsu_kit_cooldowns VALUES('u-legacy','Starter',111)");
                statement.execute("INSERT INTO vsu_kit_cooldowns VALUES('u-legacy','tools',222)");
            }
            require(!KitCooldownStore.columnsOf(connection, "vsu_kit_cooldowns").contains("claim_group"),
                    "fixture is the legacy three-column layout");

            KitCooldownStore.migrateSchema(connection);
            assertLegacyRows(connection, 111, 222);

            KitCooldownStore.migrateSchema(connection);
            assertLegacyRows(connection, 111, 222);
            require(countRows(connection, "vsu_kit_cooldowns") == 2, "rerun must not duplicate rows");
        }
    }

    private static void testSourceDataPreservation() throws Exception {
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:")) {
            try (Statement statement = connection.createStatement()) {
                statement.execute("""
                    CREATE TABLE vsu_kit_cooldowns (
                        uuid TEXT NOT NULL,
                        kit_name TEXT NOT NULL,
                        last_used INTEGER NOT NULL,
                        PRIMARY KEY (uuid, kit_name)
                    )""");
                statement.execute("""
                    CREATE TABLE vsu_homes (
                        uuid TEXT NOT NULL,
                        name TEXT NOT NULL,
                        world TEXT NOT NULL,
                        PRIMARY KEY (uuid, name)
                    )""");
                statement.execute("INSERT INTO vsu_kit_cooldowns VALUES('u-keep','food',333)");
                statement.execute("INSERT INTO vsu_homes VALUES('u-keep','base','minecraft:overworld')");
            }
            KitCooldownStore.migrateSchema(connection);
            try (Statement statement = connection.createStatement();
                 ResultSet homes = statement.executeQuery("SELECT uuid,name,world FROM vsu_homes")) {
                require(homes.next(), "unrelated homes row must survive");
                require(homes.getString(1).equals("u-keep")
                        && homes.getString(2).equals("base")
                        && homes.getString(3).equals("minecraft:overworld"),
                        "homes row must be unchanged");
                require(!homes.next(), "no extra homes rows");
            }
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT kit_name, claim_group, last_used FROM vsu_kit_cooldowns WHERE uuid=?")) {
                statement.setString(1, "u-keep");
                ResultSet rs = statement.executeQuery();
                require(rs.next(), "kit row preserved");
                require(rs.getString(1).equals("food"), "kit_name must not be rewritten");
                require(rs.getString(2).equals("food"), "empty group backfills from kit_name");
                require(rs.getLong(3) == 333, "last_used must not be reset");
            }
        }
    }

    private static void testConcurrentClaimSafety() throws Exception {
        Path dbFile = Files.createTempFile("vsu-kitgroups-", ".db");
        dbFile.toFile().deleteOnExit();
        String url = "jdbc:sqlite:" + dbFile.toAbsolutePath();
        UUID player = UUID.fromString("00000000-0000-0000-0000-000000000099");
        try (Connection setup = DriverManager.getConnection(url)) {
            setup.createStatement().execute("PRAGMA busy_timeout=5000");
            KitCooldownStore.createTable(setup);
        }

        AtomicInteger successes = new AtomicInteger();
        AtomicInteger blocked = new AtomicInteger();
        AtomicReference<Exception> failure = new AtomicReference<>();
        CyclicBarrier start = new CyclicBarrier(2);
        CountDownLatch done = new CountDownLatch(2);
        String[] kits = {"warrior", "mage"};

        for (int i = 0; i < 2; i++) {
            final String kit = kits[i];
            Thread thread = new Thread(() -> {
                try (Connection connection = DriverManager.getConnection(url)) {
                    connection.createStatement().execute("PRAGMA busy_timeout=5000");
                    start.await();
                    KitCooldownStore.ClaimOutcome outcome = KitCooldownStore.claim(
                            connection, player, kit, "class", false, 3600, 50_000L);
                    if (outcome.status() == KitCooldownStore.ClaimStatus.SUCCESS) {
                        successes.incrementAndGet();
                    } else {
                        blocked.incrementAndGet();
                    }
                } catch (Exception e) {
                    failure.compareAndSet(null, e);
                } finally {
                    done.countDown();
                }
            }, "kit-claim-" + kit);
            thread.start();
        }
        done.await();
        if (failure.get() != null) throw failure.get();
        require(successes.get() == 1, "exactly one concurrent same-group claim may succeed, got " + successes.get());
        require(blocked.get() == 1, "the other concurrent same-group claim must be rejected, got " + blocked.get());
    }

    private static void assertLegacyRows(Connection connection, long starterUsed, long toolsUsed) throws Exception {
        require(KitCooldownStore.columnsOf(connection, "vsu_kit_cooldowns").contains("claim_group"),
                "claim_group column must exist after migration");
        try (Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(
                     "SELECT kit_name, claim_group, last_used FROM vsu_kit_cooldowns ORDER BY kit_name")) {
            require(rs.next(), "starter row");
            require(rs.getString(1).equals("Starter"), "legacy kit_name preserved");
            require(rs.getString(2).equals("starter"), "claim_group backfilled from lower(kit_name)");
            require(rs.getLong(3) == starterUsed, "starter last_used preserved");
            require(rs.next(), "tools row");
            require(rs.getString(1).equals("tools"), "tools kit_name preserved");
            require(rs.getString(2).equals("tools"), "tools claim_group backfilled");
            require(rs.getLong(3) == toolsUsed, "tools last_used preserved");
            require(!rs.next(), "no extra kit rows");
        }
    }

    private static int countRows(Connection connection, String table) throws Exception {
        try (Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery("SELECT COUNT(*) FROM " + table)) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
