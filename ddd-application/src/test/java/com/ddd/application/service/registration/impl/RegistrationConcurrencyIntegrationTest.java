package com.ddd.application.service.registration.impl;

import com.ddd.application.dto.event.EventUpdateDto;
import com.ddd.application.service.event.EventCommandService;
import com.ddd.application.service.registration.RegistrationCommandService;
import com.ddd.domain.enums.EventStatusEnum;
import com.ddd.domain.event.EventCheckoutAwardedEvent;
import com.ddd.domain.exception.*;
import com.ddd.domain.repository.EventRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.context.PayloadApplicationEvent;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.locks.LockSupport;
import java.util.stream.IntStream;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RegistrationConcurrencyIntegrationTest {
    private RegistrationPostgresFixtures fixtures;
    private AnnotationConfigApplicationContext context;
    private RegistrationCommandService registrations;
    private EventCommandService commands;
    private EventRepository events;
    private TransactionTemplate tx;
    private RegistrationPostgresTestConfig.TestIdentity identity;
    private final ExecutorService workers = Executors.newFixedThreadPool(10);
    private final List<EventCheckoutAwardedEvent> notifications = new CopyOnWriteArrayList<>();

    @BeforeAll
    void start() throws Exception {
        fixtures = new RegistrationPostgresFixtures();
        context = new AnnotationConfigApplicationContext();
        context.registerBean("dataSource", DataSource.class, () -> fixtures.pool);
        context.register(RegistrationPostgresTestConfig.class);
        context.addApplicationListener(event -> {
            if (event instanceof PayloadApplicationEvent<?> wrapper
                    && wrapper.getPayload() instanceof EventCheckoutAwardedEvent completed) {
                notifications.add(completed);
            }
        });
        context.refresh();
        registrations = context.getBean(RegistrationCommandService.class);
        commands = context.getBean(EventCommandService.class);
        events = context.getBean(EventRepository.class);
        tx = new TransactionTemplate(context.getBean(PlatformTransactionManager.class));
        identity = context.getBean(RegistrationPostgresTestConfig.TestIdentity.class);
    }

    @AfterAll
    void stop() {
        try {
            workers.shutdownNow();
            if (context != null) context.close();
        } finally {
            if (fixtures != null) fixtures.close();
        }
    }

    private void register(long event, int user) {
        identity.as(user, () -> registrations.registerEvent(event));
    }

    @Test
    void tenStudentsCompeteForFinalPlace() throws Exception {
        long event = fixtures.event(2);
        fixtures.seed(event, 11, "REGISTERED");
        var outcomes = race(IntStream.rangeClosed(1, 10)
                .mapToObj(i -> (Runnable) () -> register(event, i)).toList());
        assertEquals(1, outcomes.stream().filter(java.util.Objects::isNull).count());
        assertEquals(9, outcomes.stream().filter(EventFullException.class::isInstance).count(), outcomes::toString);
        assertEquals(2, fixtures.occupied(event));
        assertEquals(2, capacity(event));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false,true})
    void simultaneousDuplicateAndReactivation(boolean cancelled) throws Exception {
        long event = fixtures.event(1);
        if (cancelled) fixtures.seed(event, 1, "CANCELLED");
        var outcomes = race(List.of(() -> register(event, 1), () -> register(event, 1)));
        assertEquals(1, outcomes.stream().filter(java.util.Objects::isNull).count());
        assertEquals(1, outcomes.stream().filter(DuplicateResourceException.class::isInstance).count(), outcomes::toString);
        assertEquals(1, fixtures.occupied(event));
        assertEquals("REGISTERED", fixtures.status(event, 1));
    }

    private List<RuntimeException> race(List<Runnable> actions) throws Exception {
        CyclicBarrier gate = new CyclicBarrier(actions.size());
        List<Future<RuntimeException>> pending = new ArrayList<>();
        for (Runnable action : actions) {
            pending.add(workers.submit(() -> {
                gate.await(8, SECONDS);
                return outcome(action);
            }));
        }
        List<RuntimeException> results = new ArrayList<>();
        for (var future : pending) results.add(future.get(8, SECONDS));
        return results;
    }

    private static RuntimeException outcome(Runnable operation) {
        try {
            operation.run();
            return null;
        } catch (RuntimeException error) {
            return error;
        }
    }

    // B reports its backend inside its own transaction. Only release A when PostgreSQL
    // confirms B is waiting on A, so these cases exercise a real blocking lock order.
    private RuntimeException ordered(long event, Runnable first, Runnable second, boolean rollback) throws Exception {
        CompletableFuture<Integer> backend = new CompletableFuture<>();
        CompletableFuture<Future<RuntimeException>> pending = new CompletableFuture<>();
        tx.executeWithoutResult(holder -> {
            events.findByIdForUpdate(event);
            int holderPid = fixtures.db.queryForObject("select pg_backend_pid()", Integer.class);
            pending.complete(workers.submit(() -> outcome(() -> tx.executeWithoutResult(contender -> {
                backend.complete(fixtures.db.queryForObject("select pg_backend_pid()", Integer.class));
                second.run();
            }))));
            int contenderPid = backend.orTimeout(5, SECONDS).join();
            long deadline = System.nanoTime() + SECONDS.toNanos(2);
            while (!fixtures.db.queryForObject("select ? = any(pg_blocking_pids(?))", Boolean.class, holderPid, contenderPid)) {
                assertTrue(System.nanoTime() < deadline, "contender never blocked on the holder");
                Thread.onSpinWait();
            }
            first.run();
            if (rollback) holder.setRollbackOnly();
        });
        return pending.join().get(8, SECONDS);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false,true})
    void winnerCommitsOrRollsBack(boolean rollback) throws Exception {
        long event = fixtures.event(1);
        var second = ordered(event, () -> register(event, 1), () -> register(event, 2), rollback);
        if (rollback) assertNull(second);
        else assertInstanceOf(EventFullException.class, second);
        assertEquals(1, fixtures.occupied(event));
        assertEquals("REGISTERED", fixtures.status(event, rollback ? 2 : 1));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false,true})
    void cancellationAndRegistration(boolean cancelFirst) throws Exception {
        long event = fixtures.event(1);
        fixtures.seed(event, 1, "REGISTERED");
        Runnable cancel = () -> identity.as(1, () -> registrations.cancelRegistration(event, "schedule"));
        Runnable join = () -> register(event, 2);
        if (cancelFirst) {
            assertNull(ordered(event, cancel, join, false));
        } else {
            // The failed registration rolls back A before B can cancel.
            assertNull(ordered(event, () -> assertThrows(EventFullException.class, join::run), cancel, true));
            join.run();
        }
        assertEquals("CANCELLED", fixtures.status(event, 1));
        assertEquals("REGISTERED", fixtures.status(event, 2));
        assertEquals(1, fixtures.occupied(event));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false,true})
    void capacityReductionAndRegistration(boolean reduceFirst) throws Exception {
        long event = fixtures.event(2);
        fixtures.seed(event, 1, "REGISTERED");
        Runnable reduce = () -> {
            // Updates validate the registration opening time as future. Wait until
            // the edited opening time before releasing the lock to the contender.
            Instant open = Instant.now().plusMillis(500);
            commands.updateEvent(event, new EventUpdateDto("Updated","Description","Hall",open,
                    open.plusSeconds(3600),open.plusSeconds(7200),1,0,0,"banner",1L,null,1L,null,true,List.of()),
                    RegistrationPostgresFixtures.email(12));
            waitUntil(open);
        };
        Runnable join = () -> register(event, 2);
        if (reduceFirst) {
            assertInstanceOf(EventFullException.class, ordered(event, reduce, join, false));
            assertEquals(1, capacity(event));
        } else {
            var error = ordered(event, join, reduce, false);
            assertInstanceOf(BusinessException.class, error);
            assertEquals("Event capacity cannot be lower than current registrations.", error.getMessage());
            assertEquals(2, capacity(event));
        }
        assertEquals(capacity(event), fixtures.occupied(event));
    }

    private int capacity(long event) {
        return fixtures.db.queryForObject("select capacity from events where id=?", Integer.class, event);
    }

    private static void waitUntil(Instant time) {
        while (Instant.now().isBefore(time)) {
            LockSupport.parkNanos(java.time.Duration.between(Instant.now(), time).toNanos());
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"closed","inactive","expired"})
    void eligibilityRecheckedAfterWaiting(String transition) throws Exception {
        long event = fixtures.event(1);
        Runnable change = switch (transition) {
            case "closed" -> () -> commands.changeEventStatus(event, EventStatusEnum.CLOSED, RegistrationPostgresFixtures.email(12));
            case "inactive" -> () -> commands.deleteEvent(event, RegistrationPostgresFixtures.email(12));
            default -> () -> {
                Instant close = Instant.now().plusMillis(350);
                fixtures.db.update("update events set date_close=? where id=?", Timestamp.from(close), event);
                waitUntil(close.plusMillis(1));
            };
        };
        var error = ordered(event, change, () -> register(event, 1), false);
        assertInstanceOf(EventRegistrationException.class, error);
        assertFalse(error instanceof EventFullException);
        assertEquals(0, fixtures.occupied(event));
    }

    @Test
    void unrelatedRegistrationAndOrdinaryReadProceedWhileEventLocked() {
        long locked = fixtures.event(1);
        long other = fixtures.event(1);
        tx.executeWithoutResult(holder -> {
            events.findByIdForUpdate(locked);
            var independent = workers.submit(() -> {
                tx.executeWithoutResult(reader -> assertEquals(locked, events.findById(locked).getId()));
                register(other, 2);
            });
            assertDoesNotThrow(() -> independent.get(2, SECONDS));
        });
        assertEquals(1, fixtures.occupied(other));
    }

    @Test
    void lockTimeoutIsTranslatedAndRetrySucceeds() {
        long event = fixtures.event(1);
        tx.executeWithoutResult(holder -> {
            events.findByIdForUpdate(event);
            long start = System.nanoTime();
            var contender = workers.submit(() -> outcome(() -> register(event, 1)));
            var error = assertDoesNotThrow(() -> contender.get(8, SECONDS));
            assertInstanceOf(PessimisticLockingFailureException.class, error);
            assertTrue(System.nanoTime() - start >= TimeUnit.MILLISECONDS.toNanos(2800));
            assertEquals(0, fixtures.occupied(event));
            holder.setRollbackOnly();
        });
        register(event, 1);
        assertEquals(1, fixtures.occupied(event));
    }

    private long attendanceEvent() {
        long event = fixtures.event(2);
        fixtures.db.update("update events set date_happen=now()-interval '1 hour' where id=?", event);
        fixtures.seed(event, 1, "CONFIRMED");
        fixtures.seed(event, 2, "CANCELLED");
        fixtures.seed(event, 3, "RECORDED");
        fixtures.db.update("insert into event_points(event_id,point_category_id,point) values (?,1,5)", event);
        return event;
    }

    @Test
    void concurrentCompletionPersistsStatusAndAwardsExactlyOnce() throws Exception {
        long event = attendanceEvent();
        Runnable complete = () -> identity.as(12, () -> registrations.fanpageMemberMarkEventAsCompleted(event));
        assertNull(ordered(event, complete, complete, false));
        assertEquals("COMPLETED", eventStatus(event));
        assertEquals("RECORDED", fixtures.status(event, 1));
        assertEquals("CANCELLED", fixtures.status(event, 2));
        assertEquals("RECORDED", fixtures.status(event, 3));
        assertEquals(List.of(1L), fixtures.db.queryForList("select user_id from point_transactions where event_id=? and point_category_id=1", Long.class, event));
        assertEquals(List.of(List.of(1L)), notifications.stream().filter(n -> n.eventId() == event).map(EventCheckoutAwardedEvent::userIds).toList());
    }

    @Test
    void failedCompletionRollsBackEventAttendanceAndRewards() {
        long event = attendanceEvent();
        class InjectedFailure extends RuntimeException {}
        assertThrows(InjectedFailure.class, () -> tx.executeWithoutResult(holder -> {
            identity.as(12, () -> registrations.fanpageMemberMarkEventAsCompleted(event));
            throw new InjectedFailure();
        }));
        assertEquals("PUBLISHED", eventStatus(event));
        assertEquals("CONFIRMED", fixtures.status(event, 1));
        assertEquals("CANCELLED", fixtures.status(event, 2));
        assertEquals("RECORDED", fixtures.status(event, 3));
        assertEquals(0, fixtures.db.queryForObject("select count(*) from point_transactions where event_id=?", Integer.class, event));
    }

    private String eventStatus(long event) {
        return fixtures.db.queryForObject("select status from events where id=?", String.class, event);
    }

    @Test
    void concurrentMaleStudentsCompeteForFinalMaleSlot() throws Exception {
        long event = fixtures.event(4, 2, 2, true);
        fixtures.setUserGender(11, "MALE");
        fixtures.seed(event, 11, "REGISTERED");
        for (int i = 1; i <= 5; i++) {
            fixtures.setUserGender(i, "MALE");
        }
        fixtures.setUserGender(6, "FEMALE");

        var outcomes = race(IntStream.rangeClosed(1, 5)
                .mapToObj(i -> (Runnable) () -> register(event, i)).toList());
        assertEquals(1, outcomes.stream().filter(java.util.Objects::isNull).count());
        assertEquals(4, outcomes.stream().filter(EventGenderFullException.class::isInstance).count(), outcomes::toString);
        assertEquals(2, fixtures.occupiedByGender(event, "MALE"));

        // Female quota still has room and total capacity (4) is not exhausted
        assertDoesNotThrow(() -> register(event, 6));
        assertEquals(1, fixtures.occupiedByGender(event, "FEMALE"));
        assertEquals(3, fixtures.occupied(event));
    }

    @Test
    void genderConstraintRejectsOtherAndNullGender() {
        long event = fixtures.event(4, 2, 2, true);
        fixtures.setUserGender(1, "OTHER");
        fixtures.setUserGender(2, null);

        var otherErr = assertThrows(EventRegistrationException.class, () -> register(event, 1));
        var nullErr = assertThrows(EventRegistrationException.class, () -> register(event, 2));
        assertFalse(otherErr instanceof EventGenderFullException);
        assertFalse(nullErr instanceof EventGenderFullException);
        assertEquals(0, fixtures.occupied(event));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void concurrentGenderQuotaReductionVsRegistration(boolean reduceFirst) throws Exception {
        long event = fixtures.event(4, 2, 2, true);
        fixtures.setUserGender(11, "MALE");
        fixtures.seed(event, 11, "REGISTERED");
        fixtures.setUserGender(1, "MALE");

        Runnable reduceMaleToOne = () -> {
            Instant open = Instant.now().plusMillis(500);
            commands.updateEvent(event, new EventUpdateDto("Updated", "Description", "Hall", open,
                    open.plusSeconds(3600), open.plusSeconds(7200), 4, 1, 2, true, "banner", 1L, null, 1L, null, true, List.of()),
                    RegistrationPostgresFixtures.email(12));
            waitUntil(open);
        };
        Runnable joinMale = () -> register(event, 1);

        if (reduceFirst) {
            assertInstanceOf(EventGenderFullException.class, ordered(event, reduceMaleToOne, joinMale, false));
            assertEquals(1, fixtures.occupiedByGender(event, "MALE"));
        } else {
            var error = ordered(event, joinMale, reduceMaleToOne, false);
            assertInstanceOf(BusinessException.class, error);
            assertEquals("Male quantity cannot be lower than current male registrations.", error.getMessage());
            assertEquals(2, fixtures.occupiedByGender(event, "MALE"));
        }
    }

    @Test
    void cancelledReRegistrationConsumesOrRejectsGenderQuota() {
        long event = fixtures.event(4, 1, 2, true);
        fixtures.setUserGender(1, "MALE");
        fixtures.setUserGender(2, "MALE");
        fixtures.seed(event, 1, "CANCELLED");
        fixtures.seed(event, 2, "REGISTERED");

        // Male quota is full (1/1) -> cancelled male cannot re-register
        assertThrows(EventGenderFullException.class, () -> register(event, 1));
        assertEquals("CANCELLED", fixtures.status(event, 1));

        // After user 2 cancels, user 1 can re-register and occupy the 1 male slot
        identity.as(2, () -> registrations.cancelRegistration(event, "schedule"));
        assertDoesNotThrow(() -> register(event, 1));
        assertEquals("REGISTERED", fixtures.status(event, 1));
        assertEquals(1, fixtures.occupiedByGender(event, "MALE"));
    }
}
