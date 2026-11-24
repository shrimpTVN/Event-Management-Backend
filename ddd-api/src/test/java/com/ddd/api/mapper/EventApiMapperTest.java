package com.ddd.api.mapper;

import com.ddd.application.dto.event.EventInfoDto;
import com.ddd.application.dto.recommendation.EventRecommendationDto;
import com.ddd.application.dto.recommendation.PointCategoryGainDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringJUnitConfig({EventApiMapperImpl.class, SemesterApiMapperImpl.class})
class EventApiMapperTest {
    @Autowired
    private EventApiMapper mapper;

    @Test
    void recommendationIncludesFullEventAndKeepsRecommendationFieldsAtTopLevel() {
        var recommendation = new EventRecommendationDto(
                event().withRegistrationInfo(12, true, true, true), 78, "Earn useful points",
                0, 3, true, List.of(new PointCategoryGainDto(5L, "Service", 3, 2)),
                true, 3, 1, true, true, true, 83, 0, 5, 78);

        var response = mapper.toRecommendationResponseDto(recommendation);
        var json = new ObjectMapper().findAndRegisterModules().valueToTree(response);

        assertEquals(42, json.path("event").path("id").asLong());
        assertEquals("Campus hall", json.path("event").path("location").asText());
        assertEquals(12, json.path("event").path("registeredCount").asLong());
        assertTrue(json.path("event").path("isFollowFanpage").asBoolean());
        assertTrue(json.path("event").path("isFull").asBoolean());
        assertTrue(json.path("event").path("registrationClosed").asBoolean());
        assertEquals("Earn useful points", json.path("explain").asText());
        assertTrue(json.path("viewed").asBoolean());
        assertEquals(78, json.path("effectiveScore").asDouble());
        assertEquals(1, json.path("pointGains").size());
        assertFalse(json.has("aiExplanation"));
        assertFalse(json.has("isFollowedFanpage"));
        assertFalse(json.has("full"));
        assertFalse(json.has("registrationClosed"));
        assertFalse(json.has("registeredCount"));
    }

    @Test
    void sharedEventMapperUsesRegistrationDataWithoutRecommendationContext() {
        var response = mapper.toResponseDto(event().withRegistrationInfo(12, false, true, true));
        assertEquals(12L, response.registeredCount());
        assertTrue(response.isFull());
        assertTrue(response.registrationClosed());
        assertFalse(response.isFollowFanpage());
    }

    private EventInfoDto event() {
        var closed = Instant.parse("2020-01-01T00:00:00Z");
        return new EventInfoDto(42L, "Volunteering", "Help out", "Campus hall",
                closed.minusSeconds(3600), closed, closed.plusSeconds(3600), 12, 6, 6,
                "banner.png", "PUBLISHED", 10L, "Volunteer", 11L, "Service",
                "Campus volunteers", 14L, true, true, null, closed, List.of(), 12L);
    }
}
