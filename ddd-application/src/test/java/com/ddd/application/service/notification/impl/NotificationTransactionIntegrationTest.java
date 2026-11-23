package com.ddd.application.service.notification.impl;

import com.ddd.application.dto.notification.CreateNotificationDto;
import com.ddd.application.dto.notification.NotificationInfoDto;
import com.ddd.application.mapper.NotificationDtoMapper;
import com.ddd.application.service.notification.NotificationCommandService;
import com.ddd.application.service.notification.NotificationDispatchAfterCommit;
import com.ddd.domain.enums.NotificationStatusEnum;
import com.ddd.domain.enums.NotificationTypeEnum;
import com.ddd.domain.model.Notification;
import com.ddd.domain.model.port.notification.NotificationPushPort;
import com.ddd.domain.model.port.security.SecurityContextPort;
import com.ddd.domain.repository.FanpageMemberRepository;
import com.ddd.domain.repository.NotificationRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.sql.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class NotificationTransactionIntegrationTest {

    private static String jdbcUrl;
    private static String dbUser;
    private static String dbPassword;

    private AnnotationConfigApplicationContext context;
    private String schema;
    private RecordingPushAdapter recordingPushAdapter;
    private NotificationCommandService commandService;
    private OuterCallerService outerCallerService;

    @BeforeEach
    void setUp() throws SQLException {
        jdbcUrl = getPropertyOrEnv("NOTIFICATION_MIGRATION_TEST_JDBC_URL");
        dbUser = getPropertyOrEnv("NOTIFICATION_MIGRATION_TEST_DB_USER");
        dbPassword = getPropertyOrEnv("NOTIFICATION_MIGRATION_TEST_DB_PASSWORD");

        if (jdbcUrl == null || dbUser == null || dbPassword == null) {
            fail("PostgreSQL test database configuration missing. Please provide " +
                    "NOTIFICATION_MIGRATION_TEST_JDBC_URL, NOTIFICATION_MIGRATION_TEST_DB_USER, and NOTIFICATION_MIGRATION_TEST_DB_PASSWORD.");
        }

        schema = "test_tx_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        try (Connection conn = DriverManager.getConnection(jdbcUrl, dbUser, dbPassword);
             Statement stmt = conn.createStatement()) {
            stmt.execute(String.format("CREATE SCHEMA %s", schema));
            stmt.execute(String.format(
                    "CREATE TABLE %s.notifications (" +
                            "id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY, " +
                            "type VARCHAR(50), " +
                            "message TEXT, " +
                            "status VARCHAR(50) NOT NULL DEFAULT 'UNREAD', " +
                            "user_id BIGINT, " +
                            "fanpage_id BIGINT, " +
                            "is_active BOOLEAN NOT NULL DEFAULT TRUE, " +
                            "created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(), " +
                            "updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(), " +
                            "CONSTRAINT notifications_type_check CHECK (type IN (" +
                            "'FANPAGE_APPROVED', 'FANPAGE_REJECTED', 'FANPAGE_MEMBER_INVITED', " +
                            "'FANPAGE_INVITATION_ACCEPTED', 'FANPAGE_INVITATION_REJECTED', 'FANPAGE_ADMIN_APPOINTED', " +
                            "'EVENT_CREATED', 'EVENT_CHECKOUT_AWARDED', 'EVENT_ABSENCE_ALERT', 'SYSTEM'))" +
                            ")",
                    schema
            ));
        }

        context = new AnnotationConfigApplicationContext();
        context.registerBean("testSchema", String.class, () -> schema);
        context.register(TestConfig.class);
        context.refresh();

        recordingPushAdapter = context.getBean(RecordingPushAdapter.class);
        commandService = context.getBean(NotificationCommandService.class);
        outerCallerService = context.getBean(OuterCallerService.class);
    }

    @AfterEach
    void tearDown() throws SQLException {
        if (context != null) {
            context.close();
        }
        if (schema != null && jdbcUrl != null) {
            try (Connection conn = DriverManager.getConnection(jdbcUrl, dbUser, dbPassword);
                 Statement stmt = conn.createStatement()) {
                stmt.execute(String.format("DROP SCHEMA IF EXISTS %s CASCADE", schema));
            }
        }
    }

    private static String getPropertyOrEnv(String name) {
        String val = System.getProperty(name);
        return (val != null && !val.isBlank()) ? val : System.getenv(name);
    }

    @Test
    @DisplayName("Single notification pushes only after commit and can be verified via independent connection at push time")
    void singleNotificationPushesOnlyAfterCommit() throws SQLException {
        CreateNotificationDto dto = new CreateNotificationDto(
                10L, NotificationTypeEnum.EVENT_CREATED, "Event Created", "icon.png", "/events/1"
        );

        NotificationInfoDto created = commandService.createNotification(dto);
        assertNotNull(created);

        // Verify push occurred
        assertEquals(1, recordingPushAdapter.recordedUserPushes.size());
        RecordedPush push = recordingPushAdapter.recordedUserPushes.get(0);
        assertEquals(10L, push.targetId);
        assertTrue(push.committedAtPushTime, "Row must already be visible in PostgreSQL at the moment of push!");
    }

    @Test
    @DisplayName("Batch notification pushes each item after commit, preferring user destination")
    void batchNotificationPushesAfterCommit() throws SQLException {
        CreateNotificationDto dto1 = new CreateNotificationDto(
                10L, NotificationTypeEnum.EVENT_CREATED, "Msg 1", null, null
        );
        CreateNotificationDto dto2 = CreateNotificationDto.forFanpage(
                20L, NotificationTypeEnum.FANPAGE_APPROVED, "Msg 2", null, null
        );

        commandService.createNotifications(List.of(dto1, dto2));

        assertEquals(1, recordingPushAdapter.recordedUserPushes.size());
        assertEquals(1, recordingPushAdapter.recordedFanpagePushes.size());
        assertTrue(recordingPushAdapter.recordedUserPushes.get(0).committedAtPushTime);
        assertTrue(recordingPushAdapter.recordedFanpagePushes.get(0).committedAtPushTime);
    }

    @Test
    @DisplayName("Insert failure in batch rolls back transaction and sends zero pushes")
    void failedBatchLeavesNoRowsOrPushes() throws SQLException {
        CreateNotificationDto validDto = new CreateNotificationDto(
                10L, NotificationTypeEnum.EVENT_CREATED, "Valid", null, null
        );
        // Will fail due to PostgreSQL NOT NULL status constraint
        CreateNotificationDto invalidDto = new CreateNotificationDto(
                11L, NotificationTypeEnum.SYSTEM, "CAUSE_DB_ERROR", null, null
        );

        assertThrows(Exception.class, () -> commandService.createNotifications(List.of(validDto, invalidDto)));

        // No pushes delivered
        assertEquals(0, recordingPushAdapter.recordedUserPushes.size());
        assertEquals(0, recordingPushAdapter.recordedFanpagePushes.size());

        // Zero rows committed
        try (Connection conn = DriverManager.getConnection(jdbcUrl, dbUser, dbPassword);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(String.format("SELECT count(*) FROM %s.notifications", schema))) {
            assertTrue(rs.next());
            assertEquals(0, rs.getInt(1));
        }
    }

    @Test
    @DisplayName("Push failure does not roll back committed rows")
    void pushFailurePreservesCommittedData() throws SQLException {
        recordingPushAdapter.throwOnPush.set(true);

        CreateNotificationDto dto = new CreateNotificationDto(
                10L, NotificationTypeEnum.SYSTEM, "Test", null, null
        );

        // Method completes without throwing push error
        NotificationInfoDto created = commandService.createNotification(dto);
        assertNotNull(created);

        // Row was committed and persisted
        try (Connection conn = DriverManager.getConnection(jdbcUrl, dbUser, dbPassword);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(String.format("SELECT count(*) FROM %s.notifications WHERE id = %d", schema, created.id()))) {
            assertTrue(rs.next());
            assertEquals(1, rs.getInt(1));
        }
    }

    @Test
    @DisplayName("Outer transaction rollback does not undo successfully committed REQUIRES_NEW notification")
    void outerRollbackDoesNotUndoCommittedNotification() throws SQLException {
        CreateNotificationDto dto = new CreateNotificationDto(
                10L, NotificationTypeEnum.EVENT_CREATED, "Independent tx", null, null
        );

        assertThrows(RuntimeException.class, () -> outerCallerService.createNotificationThenRollback(dto));

        // Notification push was sent
        assertEquals(1, recordingPushAdapter.recordedUserPushes.size());

        // Notification row exists in DB
        try (Connection conn = DriverManager.getConnection(jdbcUrl, dbUser, dbPassword);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(String.format("SELECT count(*) FROM %s.notifications", schema))) {
            assertTrue(rs.next());
            assertEquals(1, rs.getInt(1));
        }
    }

    // --- Test Infrastructure & Spring Beans ---

    record RecordedPush(Long targetId, Long notificationId, boolean committedAtPushTime) {}

    static class RecordingPushAdapter implements NotificationPushPort {
        private final DataSource dataSource;
        private final String schema;
        final List<RecordedPush> recordedUserPushes = new ArrayList<>();
        final List<RecordedPush> recordedFanpagePushes = new ArrayList<>();
        final AtomicBoolean throwOnPush = new AtomicBoolean(false);

        RecordingPushAdapter(DataSource dataSource, String schema) {
            this.dataSource = dataSource;
            this.schema = schema;
        }

        private boolean verifyCommittedThroughIndependentConnection(Long id) {
            try (Connection conn = DriverManager.getConnection(jdbcUrl, dbUser, dbPassword);
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(String.format("SELECT 1 FROM %s.notifications WHERE id = %d", schema, id))) {
                return rs.next();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void pushToUser(Long userId, Notification notification) {
            boolean visible = verifyCommittedThroughIndependentConnection(notification.getId());
            recordedUserPushes.add(new RecordedPush(userId, notification.getId(), visible));
            if (throwOnPush.get()) {
                throw new RuntimeException("Simulated SSE network failure");
            }
        }

        @Override
        public void pushToFanpage(Long fanpageId, Notification notification) {
            boolean visible = verifyCommittedThroughIndependentConnection(notification.getId());
            recordedFanpagePushes.add(new RecordedPush(fanpageId, notification.getId(), visible));
            if (throwOnPush.get()) {
                throw new RuntimeException("Simulated SSE network failure");
            }
        }
    }

    static class JdbcNotificationRepository implements NotificationRepository {
        private final DataSource dataSource;
        private final String schema;

        JdbcNotificationRepository(DataSource dataSource, String schema) {
            this.dataSource = dataSource;
            this.schema = schema;
        }

        @Override
        public Notification save(Notification notification) {
            Connection conn = DataSourceUtils.getConnection(dataSource);
            try (PreparedStatement ps = conn.prepareStatement(
                         String.format("INSERT INTO %s.notifications (type, message, status, user_id, fanpage_id) " +
                                 "VALUES (?, ?, ?, ?, ?) RETURNING id, created_at, updated_at", schema))) {
                if (notification.getType() == null) {
                    ps.setNull(1, Types.VARCHAR);
                } else {
                    ps.setString(1, notification.getType().name());
                }
                ps.setString(2, notification.getMessage());
                if ("CAUSE_DB_ERROR".equals(notification.getMessage())) {
                    ps.setNull(3, Types.VARCHAR); // Triggers real PostgreSQL NOT NULL violation
                } else {
                    ps.setString(3, notification.getStatus() != null ? notification.getStatus().name() : "UNREAD");
                }
                if (notification.getUserId() != null) ps.setLong(4, notification.getUserId()); else ps.setNull(4, Types.BIGINT);
                if (notification.getFanpageId() != null) ps.setLong(5, notification.getFanpageId()); else ps.setNull(5, Types.BIGINT);

                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        notification.setId(rs.getLong("id"));
                    }
                }
                return notification;
            } catch (SQLException e) {
                throw new RuntimeException(e);
            } finally {
                DataSourceUtils.releaseConnection(conn, dataSource);
            }
        }

        @Override
        public List<Notification> saveAll(List<Notification> notifications) {
            List<Notification> saved = new ArrayList<>();
            for (Notification n : notifications) {
                saved.add(save(n));
            }
            return saved;
        }

        @Override public Optional<Notification> findById(Long id) { return Optional.empty(); }
        @Override public Page<Notification> findAllByUserId(Long userId, Pageable pageable) { return Page.empty(); }
        @Override public Page<Notification> findAllByUserIdAndStatus(Long userId, NotificationStatusEnum status, Pageable pageable) { return Page.empty(); }
        @Override public long countUnreadByUserId(Long userId) { return 0; }
        @Override public void markAsRead(Long id, Long userId) {}
        @Override public void markAllAsRead(Long userId) {}
        @Override public void deleteByIdAndUserId(Long id, Long userId) {}
        @Override public Page<Notification> findAllByFanpageId(Long fanpageId, Pageable pageable) { return Page.empty(); }
        @Override public Page<Notification> findAllByFanpageIdAndStatus(Long fanpageId, NotificationStatusEnum status, Pageable pageable) { return Page.empty(); }
        @Override public long countUnreadByFanpageId(Long fanpageId) { return 0; }
        @Override public void markAsReadByFanpage(Long id, Long fanpageId) {}
        @Override public void markAllAsReadByFanpage(Long fanpageId) {}
        @Override public void deleteByIdAndFanpageId(Long id, Long fanpageId) {}
    }

    @Service
    static class OuterCallerService {
        private final NotificationCommandService notificationCommandService;

        OuterCallerService(NotificationCommandService notificationCommandService) {
            this.notificationCommandService = notificationCommandService;
        }

        @Transactional
        public void createNotificationThenRollback(CreateNotificationDto dto) {
            notificationCommandService.createNotification(dto);
            throw new RuntimeException("Deliberate outer transaction rollback");
        }
    }

    @Configuration
    @EnableTransactionManagement
    static class TestConfig {
        @Bean
        public DataSource dataSource() {
            DriverManagerDataSource ds = new DriverManagerDataSource();
            ds.setDriverClassName("org.postgresql.Driver");
            ds.setUrl(jdbcUrl);
            ds.setUsername(dbUser);
            ds.setPassword(dbPassword);
            return ds;
        }

        @Bean
        public PlatformTransactionManager transactionManager(DataSource dataSource) {
            return new DataSourceTransactionManager(dataSource);
        }

        @Bean
        public RecordingPushAdapter recordingPushAdapter(DataSource dataSource, String testSchema) {
            return new RecordingPushAdapter(dataSource, testSchema);
        }

        @Bean
        public NotificationDispatchAfterCommit notificationDispatchAfterCommit(RecordingPushAdapter recordingPushAdapter) {
            return new NotificationDispatchAfterCommit(recordingPushAdapter);
        }

        @Bean
        public NotificationRepository notificationRepository(DataSource dataSource, String testSchema) {
            return new JdbcNotificationRepository(dataSource, testSchema);
        }

        @Bean
        public NotificationDtoMapper notificationDtoMapper() {
            NotificationDtoMapper mapper = mock(NotificationDtoMapper.class);
            when(mapper.toModel(any(CreateNotificationDto.class))).thenAnswer(inv -> {
                CreateNotificationDto dto = inv.getArgument(0);
                Notification n = new Notification();
                n.setType(dto.type());
                n.setMessage(dto.message());
                n.setIconUrl(dto.iconUrl());
                n.setUrl(dto.url());
                n.setUserId(dto.userId());
                n.setFanpageId(dto.fanpageId());
                n.setStatus(NotificationStatusEnum.UNREAD);
                return n;
            });
            when(mapper.toInfoDto(any(Notification.class))).thenAnswer(inv -> {
                Notification n = inv.getArgument(0);
                return new NotificationInfoDto(
                        n.getId(), n.getType(), n.getMessage(), n.getStatus(),
                        n.getIconUrl(), n.getUrl(), n.getUserId(), n.getFanpageId(), null
                );
            });
            return mapper;
        }

        @Bean
        public SecurityContextPort securityContextPort() {
            return mock(SecurityContextPort.class);
        }

        @Bean
        public FanpageMemberRepository fanpageMemberRepository() {
            return mock(FanpageMemberRepository.class);
        }

        @Bean
        public NotificationCommandService notificationCommandService(
                NotificationRepository notificationRepository,
                NotificationDispatchAfterCommit notificationDispatchAfterCommit,
                NotificationDtoMapper notificationDtoMapper,
                SecurityContextPort securityContextPort,
                FanpageMemberRepository fanpageMemberRepository
        ) {
            return new NotificationCommandServiceImpl(
                    notificationRepository,
                    notificationDispatchAfterCommit,
                    notificationDtoMapper,
                    securityContextPort,
                    fanpageMemberRepository
            );
        }

        @Bean
        public OuterCallerService outerCallerService(NotificationCommandService notificationCommandService) {
            return new OuterCallerService(notificationCommandService);
        }
    }
}
