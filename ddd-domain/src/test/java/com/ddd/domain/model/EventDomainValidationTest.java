package com.ddd.domain.model;

import com.ddd.domain.exception.BusinessException;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.*;

class EventDomainValidationTest {

    private Event validEvent(Instant open, Instant close, Instant happen) {
        Event event = new Event();
        event.setDateOpen(open);
        event.setDateClose(close);
        event.setDateHappen(happen);
        event.setCapacity(100);
        event.setMaleQuantity(50);
        event.setFemaleQuantity(50);
        return event;
    }

    @Test
    void validateCapacityAndQuantities_rejectsMalePlusFemaleExceedingCapacityWithFormattedMessage() {
        Event event = validEvent(Instant.now(), Instant.now(), Instant.now());
        event.setCapacity(60);
        event.setMaleQuantity(40);
        event.setFemaleQuantity(30);

        BusinessException ex = assertThrows(BusinessException.class, event::validateCapacityAndQuantities);

        assertEquals("Total male and female quantities (70) cannot exceed event capacity (60)", ex.getMessage());
        assertEquals(ex.getMessage(), ex.getFieldErrors().get("maleQuantity"));
        assertEquals(ex.getMessage(), ex.getFieldErrors().get("femaleQuantity"));
    }

    @Test
    void validateCapacityAndQuantities_rejectsNullOrNonPositiveCapacity() {
        Event nullCapacity = validEvent(Instant.now(), Instant.now(), Instant.now());
        nullCapacity.setCapacity(null);

        BusinessException nullEx = assertThrows(BusinessException.class, nullCapacity::validateCapacityAndQuantities);
        assertEquals("Event capacity must be greater than 0", nullEx.getFieldErrors().get("capacity"));

        Event zeroCapacity = validEvent(Instant.now(), Instant.now(), Instant.now());
        zeroCapacity.setCapacity(0);

        BusinessException zeroEx = assertThrows(BusinessException.class, zeroCapacity::validateCapacityAndQuantities);
        assertEquals("Event capacity must be greater than 0", zeroEx.getFieldErrors().get("capacity"));
    }

    @Test
    void validateCapacityAndQuantities_rejectsNullOrNegativeGenderQuantities() {
        Event nullMale = validEvent(Instant.now(), Instant.now(), Instant.now());
        nullMale.setMaleQuantity(null);

        BusinessException maleEx = assertThrows(BusinessException.class, nullMale::validateCapacityAndQuantities);
        assertEquals("Male and female quantities must be non-negative", maleEx.getFieldErrors().get("maleQuantity"));

        Event negativeFemale = validEvent(Instant.now(), Instant.now(), Instant.now());
        negativeFemale.setFemaleQuantity(-1);

        BusinessException femaleEx = assertThrows(BusinessException.class, negativeFemale::validateCapacityAndQuantities);
        assertEquals("Male and female quantities must be non-negative", femaleEx.getFieldErrors().get("femaleQuantity"));
    }

    @Test
    void validateEventDatesForUpdate_allowsMinutePrecisionUnchangedPastDateOpen() {
        Instant previousOpen = Instant.now().minus(2, ChronoUnit.DAYS).truncatedTo(ChronoUnit.MINUTES).plusSeconds(42);
        Instant incomingOpen = previousOpen.truncatedTo(ChronoUnit.MINUTES);
        Instant close = Instant.now().plus(1, ChronoUnit.DAYS);
        Instant happen = Instant.now().plus(2, ChronoUnit.DAYS);

        Event event = validEvent(incomingOpen, close, happen);
        assertDoesNotThrow(() -> event.validateEventDatesForUpdate(previousOpen));
    }

    @Test
    void validateEventDatesForUpdate_rejectsDateOpenAfterDateClose_andDateCloseAfterDateHappen() {
        Instant open = Instant.now().plus(3, ChronoUnit.DAYS);
        Instant close = Instant.now().plus(2, ChronoUnit.DAYS);
        Instant happen = Instant.now().plus(4, ChronoUnit.DAYS);

        Event openAfterClose = validEvent(open, close, happen);
        BusinessException openEx = assertThrows(
                BusinessException.class,
                () -> openAfterClose.validateEventDatesForUpdate(open)
        );
        assertEquals(
                "Registration open date must be before or equal to registration close date",
                openEx.getFieldErrors().get("dateOpen")
        );

        Event closeAfterHappen = validEvent(
                Instant.now().plus(1, ChronoUnit.DAYS),
                Instant.now().plus(5, ChronoUnit.DAYS),
                happen
        );
        BusinessException closeEx = assertThrows(
                BusinessException.class,
                () -> closeAfterHappen.validateEventDatesForUpdate(closeAfterHappen.getDateOpen())
        );
        assertEquals(
                "Registration close date must be before or equal to event happen date",
                closeEx.getFieldErrors().get("dateClose")
        );
    }
}
