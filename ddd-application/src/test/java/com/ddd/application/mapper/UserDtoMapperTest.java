package com.ddd.application.mapper;

import com.ddd.application.dto.user.UserSummaryDto;
import com.ddd.infrastructure.entity.UserJpaEntity;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class UserDtoMapperTest {
    private final UserDtoMapper mapper = Mappers.getMapper(UserDtoMapper.class);

    @Test
    void toUserSummaryDto_shouldMapIdToUserId() {
        UserJpaEntity entity = new UserJpaEntity();
        entity.setId(105L);
        entity.setEmail("admin@example.edu.vn");

        UserSummaryDto dto = mapper.toUserSummaryDto(entity);

        assertNotNull(dto);
        assertEquals(105L, dto.userId());
        assertEquals("admin@example.edu.vn", dto.email());
    }
}
