package com.ddd.domain.model;

import com.ddd.domain.enums.CriteriaCompletionSourceEnum;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StudentCriteriaProgressTest {

    @Test
    void completeFromEvent_setsCompletionMetadata() {
        StudentCriteriaProgress progress = new StudentCriteriaProgress(1L, 10L, 2L);
        Instant completedAt = Instant.parse("2026-10-20T10:00:00Z");

        progress.complete(CriteriaCompletionSourceEnum.EVENT_CHECKOUT, 25L, completedAt);

        assertTrue(progress.isCompleted());
        assertEquals(CriteriaCompletionSourceEnum.EVENT_CHECKOUT, progress.getCompletionSource());
        assertEquals(25L, progress.getSourceEventId());
        assertEquals(completedAt, progress.getCompletedAt());
    }

    @Test
    void completeTwice_preservesOriginalCompletionTime() {
        StudentCriteriaProgress progress = new StudentCriteriaProgress(1L, 10L, 2L);
        Instant firstCompletion = Instant.parse("2026-10-20T10:00:00Z");
        Instant laterCompletion = Instant.parse("2026-10-21T10:00:00Z");

        progress.complete(CriteriaCompletionSourceEnum.MANUAL, null, firstCompletion);
        progress.complete(CriteriaCompletionSourceEnum.EVENT_CHECKOUT, 25L, laterCompletion);

        assertEquals(firstCompletion, progress.getCompletedAt());
        assertEquals(CriteriaCompletionSourceEnum.EVENT_CHECKOUT, progress.getCompletionSource());
    }

    @Test
    void uncomplete_clearsCompletionMetadata() {
        StudentCriteriaProgress progress = new StudentCriteriaProgress(1L, 10L, 2L);
        progress.complete(CriteriaCompletionSourceEnum.MANUAL, null, Instant.now());

        progress.uncomplete();

        assertFalse(progress.isCompleted());
        assertEquals(CriteriaCompletionSourceEnum.MANUAL, progress.getCompletionSource());
        assertNull(progress.getSourceEventId());
        assertNull(progress.getCompletedAt());
    }
}
