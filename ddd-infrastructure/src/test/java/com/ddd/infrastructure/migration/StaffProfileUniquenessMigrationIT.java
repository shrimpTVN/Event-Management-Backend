package com.ddd.infrastructure.migration;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

public class StaffProfileUniquenessMigrationIT {

    private static final String MIGRATION_SCRIPT = "database/migration/staff-profile-uniqueness-migration.sql";

    private Connection connection;
    private final List<String> createdSchemas = new ArrayList<>();
    private String jdbcUrl;
    private String dbUser;
    private String dbPassword;

    @BeforeEach
    void setUp() throws SQLException {
        jdbcUrl = getPropertyOrEnv("STAFF_PROFILE_MIGRATION_TEST_JDBC_URL");
        if (jdbcUrl == null) {
            jdbcUrl = getPropertyOrEnv("NOTIFICATION_MIGRATION_TEST_JDBC_URL");
        }
        dbUser = getPropertyOrEnv("STAFF_PROFILE_MIGRATION_TEST_DB_USER");
        if (dbUser == null) {
            dbUser = getPropertyOrEnv("NOTIFICATION_MIGRATION_TEST_DB_USER");
        }
        dbPassword = getPropertyOrEnv("STAFF_PROFILE_MIGRATION_TEST_DB_PASSWORD");
        if (dbPassword == null) {
            dbPassword = getPropertyOrEnv("NOTIFICATION_MIGRATION_TEST_DB_PASSWORD");
        }

        if (jdbcUrl == null || dbUser == null || dbPassword == null) {
            fail("PostgreSQL test database configuration missing. " +
                    "Please set NOTIFICATION_MIGRATION_TEST_JDBC_URL, " +
                    "NOTIFICATION_MIGRATION_TEST_DB_USER, and NOTIFICATION_MIGRATION_TEST_DB_PASSWORD.");
        }

        connection = DriverManager.getConnection(jdbcUrl, dbUser, dbPassword);
        connection.setAutoCommit(true);
    }

    @AfterEach
    void tearDown() throws SQLException {
        if (connection != null && !connection.isClosed()) {
            try (Statement stmt = connection.createStatement()) {
                stmt.execute("ROLLBACK");
            } catch (SQLException ignored) {
            }
            try (Statement stmt = connection.createStatement()) {
                for (String schema : createdSchemas) {
                    stmt.execute(String.format("DROP SCHEMA IF EXISTS %s CASCADE", schema));
                }
            } finally {
                connection.close();
            }
        }
    }

    private String getPropertyOrEnv(String name) {
        String value = System.getProperty(name);
        if (value == null || value.isBlank()) {
            value = System.getenv(name);
        }
        return value;
    }

    private String createUniqueSchema() throws SQLException {
        String schema = "test_staff_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(String.format("CREATE SCHEMA %s", schema));
        }
        createdSchemas.add(schema);
        return schema;
    }

    private void createTableFixture(String schema) throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(String.format(
                    "CREATE TABLE %s.fanpage_admin_profiles (" +
                            "id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY, " +
                            "staff_id VARCHAR(20) NOT NULL, " +
                            "org_name VARCHAR(255), " +
                            "title VARCHAR(100), " +
                            "dob DATE, " +
                            "phone_number VARCHAR(20), " +
                            "user_id BIGINT NOT NULL, " +
                            "is_active BOOLEAN NOT NULL DEFAULT TRUE, " +
                            "created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(), " +
                            "created_by BIGINT, " +
                            "updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(), " +
                            "updated_by BIGINT" +
                            ")",
                    schema
            ));
        }
    }

    private void executeMigration(String schema) throws Exception {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        try (InputStream is = cl.getResourceAsStream(MIGRATION_SCRIPT)) {
            assertNotNull(is, "Migration script not found: " + MIGRATION_SCRIPT);
            String sql = new String(is.readAllBytes(), StandardCharsets.UTF_8);

            try (Statement stmt = connection.createStatement()) {
                stmt.execute(String.format("SELECT set_config('ddd.staff_profile_migration_schema', '%s', false)", schema));
                stmt.execute(sql);
            }
        }
    }

    @Test
    @DisplayName("migrationAppliesSuccessfullyOnValidData")
    void migrationAppliesSuccessfullyOnValidData() throws Exception {
        String schema = createUniqueSchema();
        createTableFixture(schema);

        try (Statement stmt = connection.createStatement()) {
            stmt.execute(String.format(
                    "INSERT INTO %s.fanpage_admin_profiles (staff_id, org_name, title, user_id) VALUES " +
                            "('STAFF001', 'School of CS', 'Lecturer', 10), " +
                            "('STAFF002', 'School of Law', 'Dean', 20)",
                    schema
            ));
        }

        executeMigration(schema);

        // Verify constraints exist
        try (Statement stmt = connection.createStatement()) {
            ResultSet rs = stmt.executeQuery(String.format(
                    "SELECT conname FROM pg_constraint c " +
                            "JOIN pg_namespace n ON n.oid = c.connamespace " +
                            "JOIN pg_class cl ON cl.oid = c.conrelid " +
                            "WHERE n.nspname = '%s' AND cl.relname = 'fanpage_admin_profiles' " +
                            "AND c.conname IN ('uk_staff_profile_user_id', 'uk_staff_profile_staff_id')",
                    schema
            ));

            Set<String> constraints = new HashSet<>();
            while (rs.next()) {
                constraints.add(rs.getString("conname"));
            }

            assertTrue(constraints.contains("uk_staff_profile_user_id"), "Missing uk_staff_profile_user_id");
            assertTrue(constraints.contains("uk_staff_profile_staff_id"), "Missing uk_staff_profile_staff_id");

            // Verify rows preserved
            ResultSet rsRows = stmt.executeQuery(String.format("SELECT count(*) FROM %s.fanpage_admin_profiles", schema));
            assertTrue(rsRows.next());
            assertEquals(2, rsRows.getInt(1));
        }
    }

    @Test
    @DisplayName("migrationAbortsOnDuplicateUserId")
    void migrationAbortsOnDuplicateUserId() throws Exception {
        String schema = createUniqueSchema();
        createTableFixture(schema);

        try (Statement stmt = connection.createStatement()) {
            stmt.execute(String.format(
                    "INSERT INTO %s.fanpage_admin_profiles (staff_id, org_name, title, user_id) VALUES " +
                            "('STAFF001', 'Org A', 'Lecturer', 10), " +
                            "('STAFF002', 'Org B', 'Lecturer', 10)",
                    schema
            ));
        }

        SQLException ex = assertThrows(SQLException.class, () -> executeMigration(schema));
        assertTrue(ex.getMessage().contains("duplicate user_id"));

        // Verify table was not altered
        try (Statement stmt = connection.createStatement()) {
            ResultSet rs = stmt.executeQuery(String.format(
                    "SELECT count(*) FROM pg_constraint c " +
                            "JOIN pg_namespace n ON n.oid = c.connamespace " +
                            "WHERE n.nspname = '%s' AND c.conname = 'uk_staff_profile_user_id'",
                    schema
            ));
            assertTrue(rs.next());
            assertEquals(0, rs.getInt(1));
        }
    }

    @Test
    @DisplayName("migrationAbortsOnDuplicateStaffId")
    void migrationAbortsOnDuplicateStaffId() throws Exception {
        String schema = createUniqueSchema();
        createTableFixture(schema);

        try (Statement stmt = connection.createStatement()) {
            stmt.execute(String.format(
                    "INSERT INTO %s.fanpage_admin_profiles (staff_id, org_name, title, user_id) VALUES " +
                            "('STAFF001', 'Org A', 'Lecturer', 10), " +
                            "('STAFF001', 'Org B', 'Dean', 20)",
                    schema
            ));
        }

        SQLException ex = assertThrows(SQLException.class, () -> executeMigration(schema));
        assertTrue(ex.getMessage().contains("duplicate staff_id"));
    }

    @Test
    @DisplayName("migrationAbortsOnTrimCollision")
    void migrationAbortsOnTrimCollision() throws Exception {
        String schema = createUniqueSchema();
        createTableFixture(schema);

        try (Statement stmt = connection.createStatement()) {
            stmt.execute(String.format(
                    "INSERT INTO %s.fanpage_admin_profiles (staff_id, org_name, title, user_id) VALUES " +
                            "('STAFF001 ', 'Org A', 'Lecturer', 10), " +
                            "(' STAFF001', 'Org B', 'Dean', 20)",
                    schema
            ));
        }

        SQLException ex = assertThrows(SQLException.class, () -> executeMigration(schema));
        assertTrue(ex.getMessage().contains("after trimming"));
    }

    @Test
    @DisplayName("migrationAbortsOnBlankStaffId")
    void migrationAbortsOnBlankStaffId() throws Exception {
        String schema = createUniqueSchema();
        createTableFixture(schema);

        try (Statement stmt = connection.createStatement()) {
            stmt.execute(String.format(
                    "INSERT INTO %s.fanpage_admin_profiles (staff_id, org_name, title, user_id) VALUES " +
                            "('   ', 'Org A', 'Lecturer', 10)",
                    schema
            ));
        }

        SQLException ex = assertThrows(SQLException.class, () -> executeMigration(schema));
        assertTrue(ex.getMessage().contains("null or blank staff_id"));
    }

    @Test
    @DisplayName("migrationNormalizesWhitespaceSafely")
    void migrationNormalizesWhitespaceSafely() throws Exception {
        String schema = createUniqueSchema();
        createTableFixture(schema);

        try (Statement stmt = connection.createStatement()) {
            stmt.execute(String.format(
                    "INSERT INTO %s.fanpage_admin_profiles (staff_id, org_name, title, user_id) VALUES " +
                            "('  STAFF001  ', 'Org A', 'Lecturer', 10)",
                    schema
            ));
        }

        executeMigration(schema);

        try (Statement stmt = connection.createStatement()) {
            ResultSet rs = stmt.executeQuery(String.format(
                    "SELECT staff_id FROM %s.fanpage_admin_profiles WHERE user_id = 10", schema));
            assertTrue(rs.next());
            assertEquals("STAFF001", rs.getString("staff_id"));
        }
    }

    @Test
    @DisplayName("migrationIsIdempotent")
    void migrationIsIdempotent() throws Exception {
        String schema = createUniqueSchema();
        createTableFixture(schema);

        try (Statement stmt = connection.createStatement()) {
            stmt.execute(String.format(
                    "INSERT INTO %s.fanpage_admin_profiles (staff_id, org_name, title, user_id) VALUES " +
                            "('STAFF001', 'Org A', 'Lecturer', 10)",
                    schema
            ));
        }

        // Run 1st time
        executeMigration(schema);
        // Run 2nd time
        executeMigration(schema);

        try (Statement stmt = connection.createStatement()) {
            ResultSet rs = stmt.executeQuery(String.format(
                    "SELECT count(*) FROM pg_constraint c " +
                            "JOIN pg_namespace n ON n.oid = c.connamespace " +
                            "JOIN pg_class cl ON cl.oid = c.conrelid " +
                            "WHERE n.nspname = '%s' AND cl.relname = 'fanpage_admin_profiles' " +
                            "AND c.conname = 'uk_staff_profile_staff_id'",
                    schema
            ));
            assertTrue(rs.next());
            assertEquals(1, rs.getInt(1), "Exactly one constraint must exist");
        }
    }

    @Test
    @DisplayName("uniqueConstraintsEnforceRejection")
    void uniqueConstraintsEnforceRejection() throws Exception {
        String schema = createUniqueSchema();
        createTableFixture(schema);
        executeMigration(schema);

        try (Statement stmt = connection.createStatement()) {
            stmt.execute(String.format(
                    "INSERT INTO %s.fanpage_admin_profiles (staff_id, user_id) VALUES ('STAFF001', 10)", schema));

            // Duplicate user_id
            SQLException exUser = assertThrows(SQLException.class, () ->
                    stmt.execute(String.format(
                            "INSERT INTO %s.fanpage_admin_profiles (staff_id, user_id) VALUES ('STAFF002', 10)", schema)));
            assertEquals("23505", exUser.getSQLState(), "Expected unique_violation (23505)");

            // Duplicate staff_id
            SQLException exStaff = assertThrows(SQLException.class, () ->
                    stmt.execute(String.format(
                            "INSERT INTO %s.fanpage_admin_profiles (staff_id, user_id) VALUES ('STAFF001', 20)", schema)));
            assertEquals("23505", exStaff.getSQLState(), "Expected unique_violation (23505)");
        }
    }

    @Test
    @DisplayName("concurrentInsertRaceCondition")
    void concurrentInsertRaceCondition() throws Exception {
        String schema = createUniqueSchema();
        createTableFixture(schema);
        executeMigration(schema);

        int numThreads = 6;
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(numThreads);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger duplicateCount = new AtomicInteger(0);

        for (int i = 0; i < numThreads; i++) {
            final int userId = 100 + i;
            executor.submit(() -> {
                try {
                    startLatch.await();
                    try (Connection threadConn = DriverManager.getConnection(jdbcUrl, dbUser, dbPassword);
                         Statement stmt = threadConn.createStatement()) {
                        stmt.execute(String.format(
                                "INSERT INTO %s.fanpage_admin_profiles (staff_id, user_id) VALUES ('SAME_STAFF_ID', %d)",
                                schema, userId
                        ));
                        successCount.incrementAndGet();
                    } catch (SQLException e) {
                        if ("23505".equals(e.getSQLState())) {
                            duplicateCount.incrementAndGet();
                        }
                    }
                } catch (Exception ignored) {
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        assertTrue(doneLatch.await(10, TimeUnit.SECONDS));
        executor.shutdown();

        assertEquals(1, successCount.get(), "Exactly one concurrent insert must succeed");
        assertEquals(numThreads - 1, duplicateCount.get(), "All other concurrent inserts must fail with 23505");
    }
}
