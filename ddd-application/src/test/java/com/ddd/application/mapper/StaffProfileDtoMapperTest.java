package com.ddd.application.mapper;

import com.ddd.application.dto.user.StaffProfileDetailsDto;
import com.ddd.application.dto.user.StaffProfileSummaryDto;
import com.ddd.infrastructure.entity.FanpageAdminProfileJpaEntity;
import com.ddd.infrastructure.entity.RoleJpaEntity;
import com.ddd.infrastructure.entity.UserJpaEntity;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.time.LocalDate;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class StaffProfileDtoMapperTest {

    private final StaffProfileDtoMapper mapper = Mappers.getMapper(StaffProfileDtoMapper.class);

    @Test
    void toStaffProfileSummaryDto_shouldMapFields_whenStaffProfilePresent() {
        RoleJpaEntity role = new RoleJpaEntity();
        role.setName("FANPAGE_ADMIN");

        UserJpaEntity user = new UserJpaEntity();
        user.setId(10L);
        user.setFirstName("An");
        user.setLastName("Nguyen");
        user.setEmail("an.nguyen@example.com");
        user.setGender("MALE");
        user.setAvatarUrl("https://example.com/avatar.png");
        user.setRoles(Set.of(role));

        FanpageAdminProfileJpaEntity staffProfile = new FanpageAdminProfileJpaEntity();
        staffProfile.setStaffId("STAFF001");
        staffProfile.setOrgName("Youth Department");
        staffProfile.setTitle("Lecturer");
        staffProfile.setDob(LocalDate.of(1995, 10, 15));
        staffProfile.setPhoneNumber("0901234567");

        StaffProfileSummaryDto result = mapper.toStaffProfileSummaryDto(user, staffProfile);

        assertNotNull(result);
        assertEquals(10L, result.userId());
        assertEquals("An", result.firstName());
        assertEquals("Nguyen", result.lastName());
        assertEquals("an.nguyen@example.com", result.email());
        assertEquals("MALE", result.gender());
        assertEquals("https://example.com/avatar.png", result.avatarUrl());
        assertTrue(result.roles().contains("FANPAGE_ADMIN"));

        StaffProfileDetailsDto details = result.staffProfile();
        assertNotNull(details);
        assertEquals("STAFF001", details.staffId());
        assertEquals("Youth Department", details.orgName());
        assertEquals("Lecturer", details.title());
        assertEquals(LocalDate.of(1995, 10, 15), details.dateOfBirth());
        assertEquals("0901234567", details.phoneNumber());
    }

    @Test
    void toStaffProfileSummaryDto_shouldReturnNullStaffProfile_whenStaffProfileAbsent() {
        UserJpaEntity user = new UserJpaEntity();
        user.setId(20L);
        user.setFirstName("Binh");
        user.setLastName("Tran");
        user.setEmail("binh.tran@example.com");

        StaffProfileSummaryDto result = mapper.toStaffProfileSummaryDto(user, null);

        assertNotNull(result);
        assertEquals(20L, result.userId());
        assertEquals("Binh", result.firstName());
        assertNull(result.staffProfile());
    }
}
