package com.ddd.application.service.userprofile.impl;

import com.ddd.application.dto.user.FanpageAdminProfileDto;
import com.ddd.application.mapper.FanpageAdminProfileDtoMapper;
import com.ddd.application.service.userprofile.FanpageAdminProfileCommandService;
import com.ddd.domain.exception.DuplicateResourceException;
import com.ddd.domain.model.FanpageAdminProfile;
import com.ddd.domain.repository.FanpageAdminProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FanpageAdminProfileCommandServiceImpl implements FanpageAdminProfileCommandService {
    private final FanpageAdminProfileRepository fanpageAdminProfileRepository;
    private final FanpageAdminProfileDtoMapper fanpageAdminProfileDtoMapper;
    @Override
    public void createFanpageAdminProfile(FanpageAdminProfileDto fanpageAdminProfileDto, Long userId) {
        if (fanpageAdminProfileRepository.existsByStaffId(fanpageAdminProfileDto.staffId())) {
            throw new DuplicateResourceException("Staff ID already exists");
        }

        FanpageAdminProfile fanpageAdminProfile = fanpageAdminProfileDtoMapper.toEntity(fanpageAdminProfileDto);
        fanpageAdminProfileRepository.createFanpageAdminProfile(fanpageAdminProfile, userId);
    }
}
