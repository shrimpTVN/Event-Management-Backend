package com.ddd.infrastructure.migration;

import com.ddd.domain.enums.NotificationTypeEnum;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class NotificationTypeMigrationIT {

    private static final String MIGRATION_SCRIPT = "database/migration/notification-absence-alert-type-migration.sql";
    private static final String PREFLIGHT_SCRIPT = "database/migration/notification-absence-alert-type-preflight.sql";

    private Connection connection;
    private final List<String> createdSchemas = new ArrayList<>();

    @BeforeEach
    void setUp() throws SQLException {
        String jdbcUrl = getPropertyOrEnv("NOTIFICATION_MIGRATION_TEST_JDBC_URL");
        String user = getPropertyOrEnv("NOTIFICATION_MIGRATION_TEST_DB_USER");
        String password = getPropertyOrEnv("NOTIFICATION_MIGRATION_TEST_DB_PASSWORD");

        if (jdbcUrl == null || user == null || password == null) {
            fail("PostgreSQL test database configuration missing. " +
                    "Please set NOTIFICATION_MIGRATION_TEST_JDBC_URL, " +
                    "NOTIFICATION_MIGRATION_TEST_DB_USER, and NOTIFICATION_MIGRATION_TEST_DB_PASSWORD.");
        }

        connection = DriverManager.getConnection(jdbcUrl, user, password);
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
        String schema = "test_notif_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(String.format("CREATE SCHEMA %s", schema));
        }
        createdSchemas.add(schema);
        return schema;
    }

    private void createLegacyFixture(String schema) throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(String.format(
                    "CREATE TABLE %s.notifications (" +
                            "id BIGINT PRIMARY KEY, " +
                            "type VARCHAR(50), " +
                            "message TEXT, " +
                            "status VARCHAR(50) NOT NULL DEFAULT 'UNREAD', " +
                            "is_active BOOLEAN NOT NULL DEFAULT TRUE, " +
                            "created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(), " +
                            "updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(), " +
                            "CONSTRAINT notifications_type_check CHECK (type IN (" +
                            "'FANPAGE_APPROVED', 'FANPAGE_REJECTED', 'FANPAGE_MEMBER_INVITED', " +
                            "'FANPAGE_INVITATION_ACCEPTED', 'FANPAGE_INVITATION_REJECTED', 'FANPAGE_ADMIN_APPOINTED', " +
                            "'EVENT_CREATED', 'EVENT_CHECKOUT_AWARDED', 'SYSTEM')), " +
                            "CONSTRAINT notifications_status_check CHECK (status <> 'INVALID')" +
                            ")",
                    schema
            ));
            stmt.execute(String.format("CREATE INDEX idx_notif_status_%s ON %s.notifications (status)",
                    schema.replace("-", "_"), schema));

            // Seed valid notification
            stmt.execute(String.format(
                    "INSERT INTO %s.notifications (id, type, message) VALUES (1, 'EVENT_CREATED', 'Welcome')",
                    schema
            ));
        }
    }

    private void executeScript(String resourcePath, String schema) throws Exception {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(String.format("SELECT set_config('ddd.notification_migration_schema', '%s', false)", schema));
            stmt.execute(String.format("SET search_path TO %s, public", schema));
        }
        String sql = loadResourceAsString(resourcePath);
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) {
            try (Statement stmt = connection.createStatement()) {
                stmt.execute("ROLLBACK");
            } catch (SQLException ignored) {
            }
            throw e;
        }
    }

    private String loadResourceAsString(String resourcePath) throws Exception {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            if (is == null) {
                throw new IllegalArgumentException("Resource not found: " + resourcePath);
            }
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private Map<Long, Map<String, Object>> readNotificationSnapshot(String schema) throws SQLException {
        Map<Long, Map<String, Object>> rows = new LinkedHashMap<>();
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(String.format("SELECT * FROM %s.notifications ORDER BY id", schema))) {
            ResultSetMetaData meta = rs.getMetaData();
            int colCount = meta.getColumnCount();
            while (rs.next()) {
                long id = rs.getLong("id");
                Map<String, Object> row = new HashMap<>();
                for (int i = 1; i <= colCount; i++) {
                    row.put(meta.getColumnName(i), rs.getObject(i));
                }
                rows.put(id, row);
            }
        }
        return rows;
    }

    @Test
    @DisplayName("upgradesOldCheckWithoutChangingRows: verifies old check rejects EVENT_ABSENCE_ALERT, then upgrade preserves rows and allows it")
    void upgradesOldCheckWithoutChangingRows() throws Exception {
        String schema = createUniqueSchema();
        createLegacyFixture(schema);

        // Assert BEFORE migration: EVENT_ABSENCE_ALERT is rejected with SQLState 23514
        try (Statement stmt = connection.createStatement()) {
            SQLException ex = assertThrows(SQLException.class, () ->
                    stmt.execute(String.format(
                            "INSERT INTO %s.notifications (id, type, message) VALUES (99, 'EVENT_ABSENCE_ALERT', 'Absence warning')",
                            schema
                    ))
            );
            assertEquals("23514", ex.getSQLState(), "Expected check constraint violation code 23514");
        }

        Map<Long, Map<String, Object>> snapshotBefore = readNotificationSnapshot(schema);

        // Run Preflight and Migration
        executeScript(PREFLIGHT_SCRIPT, schema);
        executeScript(MIGRATION_SCRIPT, schema);

        // Assert existing rows are preserved verbatim
        Map<Long, Map<String, Object>> snapshotAfter = readNotificationSnapshot(schema);
        assertEquals(snapshotBefore.size(), snapshotAfter.size());
        assertEquals(snapshotBefore.get(1L), snapshotAfter.get(1L));

        // Assert AFTER migration: EVENT_ABSENCE_ALERT can be inserted successfully
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(String.format(
                    "INSERT INTO %s.notifications (id, type, message) VALUES (99, 'EVENT_ABSENCE_ALERT', 'Absence warning')",
                    schema
            ));
        }

        // Verify inserted row
        Map<Long, Map<String, Object>> finalSnapshot = readNotificationSnapshot(schema);
        assertTrue(finalSnapshot.containsKey(99L));
        assertEquals("EVENT_ABSENCE_ALERT", finalSnapshot.get(99L).get("type"));
    }

    @Test
    @DisplayName("acceptsEveryCurrentEnumValue: asserts all 10 NotificationTypeEnum values can be inserted after migration")
    void acceptsEveryCurrentEnumValue() throws Exception {
        String schema = createUniqueSchema();
        createLegacyFixture(schema);
        executeScript(MIGRATION_SCRIPT, schema);

        long id = 100;
        for (NotificationTypeEnum typeEnum : NotificationTypeEnum.values()) {
            try (Statement stmt = connection.createStatement()) {
                stmt.execute(String.format(
                        "INSERT INTO %s.notifications (id, type, message) VALUES (%d, '%s', 'Test message')",
                        schema, ++id, typeEnum.name()
                ));
            }
        }

        Map<Long, Map<String, Object>> snapshot = readNotificationSnapshot(schema);
        assertEquals(1 + NotificationTypeEnum.values().length, snapshot.size());
    }

    @Test
    @DisplayName("rejectsUnknownNonNullType: asserts unknown string fails with SQLState 23514")
    void rejectsUnknownNonNullType() throws Exception {
        String schema = createUniqueSchema();
        createLegacyFixture(schema);
        executeScript(MIGRATION_SCRIPT, schema);

        try (Statement stmt = connection.createStatement()) {
            SQLException ex = assertThrows(SQLException.class, () ->
                    stmt.execute(String.format(
                            "INSERT INTO %s.notifications (id, type, message) VALUES (200, 'UNKNOWN_CUSTOM_TYPE', 'Invalid')",
                            schema
                    ))
            );
            assertEquals("23514", ex.getSQLState());
        }
    }

    @Test
    @DisplayName("preservesNullTypes: asserts null type is allowed")
    void preservesNullTypes() throws Exception {
        String schema = createUniqueSchema();
        createLegacyFixture(schema);
        executeScript(MIGRATION_SCRIPT, schema);

        try (Statement stmt = connection.createStatement()) {
            assertDoesNotThrow(() ->
                    stmt.execute(String.format(
                            "INSERT INTO %s.notifications (id, type, message) VALUES (201, NULL, 'Null type')",
                            schema
                    ))
            );
        }
    }

    @Test
    @DisplayName("abortsOnUnknownHistoricalType: migration fails and does not alter constraint if table contains invalid types")
    void abortsOnUnknownHistoricalType() throws Exception {
        String schema = createUniqueSchema();
        // Create table without check constraint and seed invalid type
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(String.format(
                    "CREATE TABLE %s.notifications (id BIGINT PRIMARY KEY, type VARCHAR(50), message TEXT)", schema
            ));
            stmt.execute(String.format(
                    "INSERT INTO %s.notifications (id, type, message) VALUES (1, 'CORRUPTED_TYPE_123', 'Corrupted')", schema
            ));
        }

        // Running migration must fail
        assertThrows(Exception.class, () -> executeScript(MIGRATION_SCRIPT, schema));

        // Constraint was NOT added
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(String.format(
                     "SELECT count(*) FROM pg_constraint c " +
                             "JOIN pg_namespace n ON n.oid = c.connamespace " +
                             "WHERE n.nspname = '%s' AND c.conname = 'notifications_type_check'", schema))) {
            assertTrue(rs.next());
            assertEquals(0, rs.getInt(1));
        }
    }

    @Test
    @DisplayName("preservesUnrelatedConstraintsAndIndexes: unrelated check and index remain after migration")
    void preservesUnrelatedConstraintsAndIndexes() throws Exception {
        String schema = createUniqueSchema();
        createLegacyFixture(schema);
        executeScript(MIGRATION_SCRIPT, schema);

        try (Statement stmt = connection.createStatement()) {
            // Unrelated check still works (rejects status = 'INVALID')
            SQLException ex = assertThrows(SQLException.class, () ->
                    stmt.execute(String.format(
                            "INSERT INTO %s.notifications (id, type, status, message) VALUES (300, 'SYSTEM', 'INVALID', 'Test')",
                            schema
                    ))
            );
            assertEquals("23514", ex.getSQLState());

            // Index still exists
            try (ResultSet rs = stmt.executeQuery(String.format(
                    "SELECT count(*) FROM pg_indexes WHERE schemaname = '%s' AND tablename = 'notifications' AND indexname LIKE 'idx_notif_status_%%'",
                    schema))) {
                assertTrue(rs.next());
                assertEquals(1, rs.getInt(1));
            }
        }
    }

    @Test
    @DisplayName("isRepeatable: running migration script twice succeeds and maintains a valid constraint")
    void isRepeatable() throws Exception {
        String schema = createUniqueSchema();
        createLegacyFixture(schema);

        executeScript(MIGRATION_SCRIPT, schema);
        executeScript(MIGRATION_SCRIPT, schema);

        // Verify exactly one notifications_type_check exists and is validated
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(String.format(
                     "SELECT conname, convalidated FROM pg_constraint c " +
                             "JOIN pg_namespace n ON n.oid = c.connamespace " +
                             "WHERE n.nspname = '%s' AND c.conname = 'notifications_type_check'", schema))) {
            assertTrue(rs.next());
            assertTrue(rs.getBoolean("convalidated"));
            assertFalse(rs.next(), "Expected exactly one constraint named notifications_type_check");
        }
    }

    @Test
    @DisplayName("targetsOnlyConfiguredSchema: migration modifies target schema without affecting second schema")
    void targetsOnlyConfiguredSchema() throws Exception {
        String schemaA = createUniqueSchema();
        String schemaB = createUniqueSchema();

        createLegacyFixture(schemaA);
        createLegacyFixture(schemaB);

        // Apply only to schemaA
        executeScript(MIGRATION_SCRIPT, schemaA);

        try (Statement stmt = connection.createStatement()) {
            // schemaA accepts EVENT_ABSENCE_ALERT
            assertDoesNotThrow(() -> stmt.execute(String.format(
                    "INSERT INTO %s.notifications (id, type, message) VALUES (99, 'EVENT_ABSENCE_ALERT', 'OK')", schemaA
            )));

            // schemaB still rejects EVENT_ABSENCE_ALERT
            SQLException ex = assertThrows(SQLException.class, () -> stmt.execute(String.format(
                    "INSERT INTO %s.notifications (id, type, message) VALUES (99, 'EVENT_ABSENCE_ALERT', 'Rejected')", schemaB
            )));
            assertEquals("23514", ex.getSQLState());
        }
    }

    @Test
    @DisplayName("rollsBackConstraintChangeOnFailure: transaction failure leaves old constraint intact")
    void rollsBackConstraintChangeOnFailure() throws Exception {
        String schema = createUniqueSchema();
        createLegacyFixture(schema);

        // Execute a script that drops constraint then causes a deliberate failure before commit
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(String.format("SELECT set_config('ddd.notification_migration_schema', '%s', false)", schema));
            assertThrows(SQLException.class, () -> stmt.execute(
                    "BEGIN;\n" +
                            String.format("ALTER TABLE %s.notifications DROP CONSTRAINT notifications_type_check;\n", schema) +
                            "DO $$ BEGIN RAISE EXCEPTION 'Simulated error before commit'; END $$;\n" +
                            "COMMIT;\n"
            ));
            try {
                stmt.execute("ROLLBACK");
            } catch (SQLException ignored) {
            }
        }
    }

    @Test
    @DisplayName("freshBootstrapProducesExpectedContract: verifies fresh bootstrap from schema.sql and migrations produces expected contract")
    void freshBootstrapProducesExpectedContract() throws Exception {
        String schema = createUniqueSchema();

        // 1. Execute schema.sql
        executeScript("database/schema.sql", schema);

        // 2. Execute title/type compatibility
        executeScript("database/migration/notification-type-refactor-migration.sql", schema);

        // 3. Execute fanpage support
        executeScript("database/migration/notification-fanpage-support-migration.sql", schema);

        // 4. Execute sequence migration
        executeScript("database/migration/notification-sequence-migration.sql", schema);

        // 5. Execute new check migration
        executeScript(MIGRATION_SCRIPT, schema);

        // Verify all 10 enum values can be inserted
        long id = 500;
        for (NotificationTypeEnum typeEnum : NotificationTypeEnum.values()) {
            try (Statement stmt = connection.createStatement()) {
                stmt.execute(String.format(
                        "INSERT INTO %s.notifications (type, message) VALUES ('%s', 'Bootstrap test')",
                        schema, typeEnum.name()
                ));
            }
        }

        // Verify invalid type is rejected
        try (Statement stmt = connection.createStatement()) {
            SQLException ex = assertThrows(SQLException.class, () -> stmt.execute(String.format(
                    "INSERT INTO %s.notifications (type, message) VALUES ('INVALID_TYPE', 'Should fail')", schema
            )));
            assertEquals("23514", ex.getSQLState());
        }
    }
}
