package com.ddd.application.service.notification;

import com.ddd.domain.enums.NotificationStatusEnum;
import com.ddd.domain.enums.NotificationTypeEnum;
import com.ddd.domain.model.Notification;
import com.ddd.domain.model.port.notification.NotificationPushPort;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationDispatchAfterCommitTest {

    @Mock
    private NotificationPushPort notificationPushPort;

    @InjectMocks
    private NotificationDispatchAfterCommit dispatcher;

    @BeforeEach
    void setUp() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @AfterEach
    void tearDown() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    private void triggerAfterCommit() {
        List<TransactionSynchronization> synchronizations = TransactionSynchronizationManager.getSynchronizations();
        for (TransactionSynchronization sync : synchronizations) {
            sync.afterCommit();
        }
    }

    @Test
    @DisplayName("dispatchSingleAfterCommit throws IllegalStateException when synchronization is inactive")
    void dispatchSingleAfterCommit_outsideTransaction_throwsIllegalStateException() {
        Notification notification = new Notification();
        notification.setId(1L);

        assertThrows(IllegalStateException.class, () -> dispatcher.dispatchSingleAfterCommit(notification));
        verifyNoInteractions(notificationPushPort);
    }

    @Test
    @DisplayName("dispatchAfterCommit throws IllegalStateException when synchronization is inactive")
    void dispatchAfterCommit_outsideTransaction_throwsIllegalStateException() {
        Notification notification = new Notification();
        notification.setId(1L);

        assertThrows(IllegalStateException.class, () -> dispatcher.dispatchAfterCommit(List.of(notification)));
        verifyNoInteractions(notificationPushPort);
    }

    @Test
    @DisplayName("dispatchAfterCommit with empty or null list is a no-op")
    void dispatchAfterCommit_emptyOrNull_isNoOp() {
        dispatcher.dispatchAfterCommit(null);
        dispatcher.dispatchAfterCommit(List.of());
        verifyNoInteractions(notificationPushPort);
    }

    @Test
    @DisplayName("dispatchSingleAfterCommit with null notification is a no-op")
    void dispatchSingleAfterCommit_null_isNoOp() {
        TransactionSynchronizationManager.initSynchronization();
        dispatcher.dispatchSingleAfterCommit(null);
        triggerAfterCommit();
        verifyNoInteractions(notificationPushPort);
    }

    @Test
    @DisplayName("Single notification pushes to both user and fanpage destinations if both are present, only after commit")
    void dispatchSingleAfterCommit_bothDestinations_pushesAfterCommit() {
        TransactionSynchronizationManager.initSynchronization();

        Notification notification = new Notification();
        notification.setId(10L);
        notification.setUserId(1L);
        notification.setFanpageId(20L);
        notification.setType(NotificationTypeEnum.EVENT_CREATED);
        notification.setMessage("Event created");

        dispatcher.dispatchSingleAfterCommit(notification);

        // Before commit: no pushes
        verifyNoInteractions(notificationPushPort);

        // Trigger commit
        triggerAfterCommit();

        // After commit: both user and fanpage received push
        verify(notificationPushPort, times(1)).pushToUser(eq(1L), any(Notification.class));
        verify(notificationPushPort, times(1)).pushToFanpage(eq(20L), any(Notification.class));
    }

    @Test
    @DisplayName("Batch notification prefers user destination and otherwise uses fanpage destination")
    void dispatchAfterCommit_batchRouting_prefersUserOverFanpage() {
        TransactionSynchronizationManager.initSynchronization();

        Notification userAndFanpage = new Notification();
        userAndFanpage.setId(1L);
        userAndFanpage.setUserId(100L);
        userAndFanpage.setFanpageId(200L);

        Notification fanpageOnly = new Notification();
        fanpageOnly.setId(2L);
        fanpageOnly.setFanpageId(300L);

        Notification userOnly = new Notification();
        userOnly.setId(3L);
        userOnly.setUserId(400L);

        dispatcher.dispatchAfterCommit(List.of(userAndFanpage, fanpageOnly, userOnly));

        // Before commit
        verifyNoInteractions(notificationPushPort);

        // Commit
        triggerAfterCommit();

        // UserAndFanpage should ONLY push to user (prefers user in batch)
        verify(notificationPushPort).pushToUser(eq(100L), any(Notification.class));
        verify(notificationPushPort, never()).pushToFanpage(eq(200L), any());

        // FanpageOnly pushes to fanpage
        verify(notificationPushPort).pushToFanpage(eq(300L), any(Notification.class));

        // UserOnly pushes to user
        verify(notificationPushPort).pushToUser(eq(400L), any(Notification.class));
    }

    @Test
    @DisplayName("Mutation of original model after scheduling does not affect the detached snapshot")
    void dispatchSingleAfterCommit_snapshotNotAffectedByMutation() {
        TransactionSynchronizationManager.initSynchronization();

        Notification original = new Notification();
        original.setId(5L);
        original.setUserId(50L);
        original.setMessage("Original Message");
        original.setStatus(NotificationStatusEnum.UNREAD);

        dispatcher.dispatchSingleAfterCommit(original);

        // Mutate original object
        original.setMessage("Mutated Message");
        original.setStatus(NotificationStatusEnum.READ);
        original.setUserId(999L);

        triggerAfterCommit();

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationPushPort).pushToUser(eq(50L), captor.capture());

        Notification captured = captor.getValue();
        assertEquals("Original Message", captured.getMessage());
        assertEquals(NotificationStatusEnum.UNREAD, captured.getStatus());
        assertEquals(50L, captured.getUserId());
    }

    @Test
    @DisplayName("Delivery failure on one destination is isolated and does not stop remaining deliveries")
    void dispatchAfterCommit_isolatesDeliveryFailures() {
        TransactionSynchronizationManager.initSynchronization();

        Notification notif1 = new Notification();
        notif1.setId(1L);
        notif1.setUserId(10L);

        Notification notif2 = new Notification();
        notif2.setId(2L);
        notif2.setUserId(20L);

        doThrow(new RuntimeException("SSE network error")).when(notificationPushPort).pushToUser(eq(10L), any());

        dispatcher.dispatchAfterCommit(List.of(notif1, notif2));

        // Trigger commit - should not throw exception
        assertDoesNotThrow(this::triggerAfterCommit);

        // Delivery for notif2 still proceeded
        verify(notificationPushPort).pushToUser(eq(20L), any());
    }

    @Test
    @DisplayName("Rollback never triggers afterCommit callback")
    void noPushOnRollback() {
        TransactionSynchronizationManager.initSynchronization();

        Notification notif = new Notification();
        notif.setId(1L);
        notif.setUserId(10L);

        dispatcher.dispatchSingleAfterCommit(notif);

        // Simulate rollback: afterCompletion with STATUS_ROLLED_BACK instead of afterCommit
        List<TransactionSynchronization> synchronizations = TransactionSynchronizationManager.getSynchronizations();
        for (TransactionSynchronization sync : synchronizations) {
            sync.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK);
        }

        verifyNoInteractions(notificationPushPort);
    }
}
