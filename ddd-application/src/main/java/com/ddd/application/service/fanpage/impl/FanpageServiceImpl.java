package com.ddd.application.service.fanpage.impl;

import com.ddd.application.dto.fanpage.FanpageRegisterDto;
import com.ddd.application.mapper.FanpageDtoMapper;
import com.ddd.application.service.fanpage.FanpageService;
import com.ddd.domain.enums.FanpageRoleEnum;
import com.ddd.domain.enums.FanpageStatusEnum;
import com.ddd.domain.exception.BusinessException;
import com.ddd.domain.exception.DuplicateResourceException;
import com.ddd.domain.exception.ResourceNotFoundException;
import com.ddd.domain.model.Fanpage;
import com.ddd.domain.model.FanpageMember;
import com.ddd.domain.model.User;
import com.ddd.domain.repository.FanpageMemberRepository;
import com.ddd.domain.repository.FanpageRepository;
import com.ddd.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class FanpageServiceImpl implements FanpageService {
    private final FanpageRepository fanpageRepository;
    private final FanpageMemberRepository fanpageMemberRepository;
    private final FanpageDtoMapper fanpageDtoMapper;
    private final UserRepository userRepository;


    @Override
    public void createFanpage(FanpageRegisterDto registerDto, String email) {
        //check duplicate  fanpage name
        if (fanpageRepository.existsByName(registerDto.name())) {
            throw new DuplicateResourceException("Fanpage name already exists");
        }

        User user = userRepository.findByEmail(email);

        //check whether fanpage admin already has a fanpage
        if (fanpageMemberRepository.isAlreadyHasFanpage(user.getId())) {
            throw new DuplicateResourceException("Fanpage admin already has a fanpage");
        }

        Fanpage fanpage = fanpageDtoMapper.toFanpage(registerDto);
        fanpage.setStatus(FanpageStatusEnum.PENDING.name());
        Fanpage savedFanpage = fanpageRepository.save(fanpage);

        FanpageMember fanpageMember = new FanpageMember(savedFanpage.getId(), user.getId(), FanpageRoleEnum.ADMIN.name());
        fanpageMemberRepository.save(fanpageMember);

        log.info("Fanpage created successfully: {}", savedFanpage.getName());
    }

    @Override
    public void acceptFanpage(Long fanpageId) {
        Fanpage fanpage = fanpageRepository.findById(fanpageId).orElseThrow(() -> new ResourceNotFoundException("Fanpage not found"));
        fanpage.setStatus(FanpageStatusEnum.ACTIVE.name());
        fanpageRepository.save(fanpage);
        log.info("Fanpage accepted successfully: {}", fanpage.getName());
    }

    @Override
    public void banFanpage(Long fanpageId) {
        Fanpage fanpage = fanpageRepository.findById(fanpageId).orElseThrow(() -> new ResourceNotFoundException("Fanpage not found"));
        fanpage.setStatus(FanpageStatusEnum.BANNED.name());
        fanpageRepository.save(fanpage);
        log.info("Fanpage banned successfully: {}", fanpage.getName());
    }

    @Override
    public void addMemberToFanpage(Long fanpageId, Long userId, String email) {
        User member = userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User not found"));

        User fanpageAdmin = userRepository.findByEmail(email);

        if (fanpageMemberRepository.isAdminOfFanpage(fanpageAdmin.getId(), userId)) {
            throw new BusinessException("User email: " + email + " is not an admin of the fanpage");
        }

        FanpageMember fanpageMember = new FanpageMember(fanpageId, member.getId(), FanpageRoleEnum.MEMBER.name());
        fanpageMemberRepository.save(fanpageMember);
        log.info("Member added to fanpage successfully: {}", member.getEmail());
    }

    @Override
    public void removeMemberFromFanpage(Long fanpageId, Long userId, String email) {
        User member = userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User not found"));

        User fanpageAdmin = userRepository.findByEmail(email);

        if (fanpageMemberRepository.isAdminOfFanpage(fanpageAdmin.getId(), userId)) {
            throw new BusinessException("User email: " + email + " is not an admin of the fanpage");
        }

        fanpageMemberRepository.removeFanpageMember(fanpageId, userId);
        log.info("Member removed from fanpage successfully: {}", member.getEmail());
    }
}
