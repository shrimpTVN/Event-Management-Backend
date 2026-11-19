package com.ddd.application.service.fanpage.impl;

import com.ddd.application.dto.fanpage.FanpageMemberDto;
import com.ddd.application.mapper.FanpageDtoMapper;
import com.ddd.application.mapper.FanpageMemberDtoMapper;
import com.ddd.domain.model.Fanpage;
import com.ddd.domain.model.port.security.SecurityContextPort;
import com.ddd.domain.repository.FanpageMemberRepository;
import com.ddd.domain.repository.FanpageRepository;
import com.ddd.domain.repository.FollowingRepository;
import com.ddd.infrastructure.entity.FanpageMemberJpaEntity;
import com.ddd.infrastructure.repository.jpaRepository.FanpageJpaRepository;
import com.ddd.infrastructure.repository.jpaRepository.FanpageMemberJpaRepository;
import com.ddd.infrastructure.repository.jpaRepository.FollowingJpaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FanpageQueryServiceImplTest {

    @Mock private FanpageMemberJpaRepository fanpageMemberJpaRepository;
    @Mock private FanpageMemberDtoMapper fanpageMemberDtoMapper;
    @Mock private FanpageJpaRepository fanpageJpaRepository;
    @Mock private FanpageDtoMapper fanpageDtoMapper;
    @Mock private FanpageRepository fanpageRepository;
    @Mock private FanpageMemberRepository fanpageMemberRepository;
    @Mock private FollowingRepository followingRepository;
    @Mock private FollowingJpaRepository followingJpaRepository;
    @Mock private SecurityContextPort securityContextPort;

    @InjectMocks private FanpageQueryServiceImpl fanpageQueryService;

    @Test
    void getMembersOfFanpage_whenAdmin_shouldFetchEvenIfFanpageInactive() {
        when(securityContextPort.hasRole("ADMIN")).thenReturn(true);
        Fanpage fanpage = new Fanpage();
        fanpage.setId(99L);
        fanpage.setActive(false);
        when(fanpageRepository.findById(99L)).thenReturn(Optional.of(fanpage));

        FanpageMemberJpaEntity memberEntity = new FanpageMemberJpaEntity();
        when(fanpageMemberJpaRepository.findAllByFanpageId(99L)).thenReturn(List.of(memberEntity));
        FanpageMemberDto memberDto = new FanpageMemberDto(1L, "admin@test.com", "Admin", "User", null, "ADMIN", "ST01", "0901234567");
        when(fanpageMemberDtoMapper.toDto(memberEntity)).thenReturn(memberDto);

        List<FanpageMemberDto> result = fanpageQueryService.getMembersOfFanpage(99L);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("ST01", result.get(0).staffId());
        assertEquals("0901234567", result.get(0).phoneNumber());
        verify(fanpageRepository, never()).findByIdAndIsActive(anyLong(), anyBoolean());
    }
}
