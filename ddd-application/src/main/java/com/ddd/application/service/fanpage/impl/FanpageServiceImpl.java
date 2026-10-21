package com.ddd.application.service.fanpage.impl;

import com.ddd.application.dto.fanpage.FanpageAdminUpdateDto;
import com.ddd.application.dto.fanpage.FanpageRegisterDto;
import com.ddd.application.dto.fanpage.FanpageSystemAdminUpdateDto;
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
import com.ddd.domain.model.Following;
import com.ddd.domain.repository.FanpageMemberRepository;
import com.ddd.domain.repository.FanpageRepository;
import com.ddd.domain.repository.FollowingRepository;
import com.ddd.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class FanpageServiceImpl implements FanpageService {
    private final FanpageRepository fanpageRepository;
    private final FanpageMemberRepository fanpageMemberRepository;
    private final FanpageDtoMapper fanpageDtoMapper;
    private final UserRepository userRepository;
    private final FollowingRepository followingRepository;


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

        if (!fanpageMemberRepository.isAdminOfFanpage(fanpageId, fanpageAdmin.getId())) {
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

        if (!fanpageMemberRepository.isAdminOfFanpage(fanpageId, fanpageAdmin.getId())) {
            throw new BusinessException("User email: " + email + " is not an admin of the fanpage");
        }

        fanpageMemberRepository.removeFanpageMember(fanpageId, userId);
        log.info("Member removed from fanpage successfully: {}", member.getEmail());
    }

    @Override
    public void unbanFanpage(Long fanpageId) {
        Fanpage fanpage = fanpageRepository.findById(fanpageId).orElseThrow(() -> new ResourceNotFoundException("Fanpage not found"));
        fanpage.setStatus(FanpageStatusEnum.ACTIVE.name());
        fanpageRepository.save(fanpage);
        log.info("Fanpage unbanned successfully: {}", fanpage.getName());
    }

    @Override
    public void updateFanpageByFanpageAdmin(Long fanpageId, FanpageAdminUpdateDto updateDto, String email) {
        User user = userRepository.findByEmail(email);
        if (user == null) {
            throw new ResourceNotFoundException("User not found");
        }
        if (!fanpageMemberRepository.isAdminOfFanpage(fanpageId, user.getId())) {
            throw new BusinessException("User email: " + email + " is not an admin of the fanpage");
        }

        Fanpage fanpage = fanpageRepository.findByIdAndIsActive(fanpageId, true)
                .orElseThrow(() -> new ResourceNotFoundException("Fanpage not found with id: " + fanpageId));

        fanpageDtoMapper.updateFanpageFromAdminDto(updateDto, fanpage);

        fanpageRepository.save(fanpage);
        log.info("Fanpage id: {} updated by fanpage admin: {}", fanpageId, email);
    }

    @Override
    public void updateFanpageByAdmin(Long fanpageId, FanpageSystemAdminUpdateDto updateDto) {
        Fanpage fanpage = fanpageRepository.findById(fanpageId)
                .orElseThrow(() -> new ResourceNotFoundException("Fanpage not found with id: " + fanpageId));

        if (updateDto.name() != null && !updateDto.name().trim().equalsIgnoreCase(fanpage.getName())) {
            if (fanpageRepository.existsByNameAndIdNot(updateDto.name().trim(), fanpageId)) {
                throw new DuplicateResourceException("Fanpage name already exists: " + updateDto.name());
            }
        }

        fanpageDtoMapper.updateFanpageFromSystemAdminDto(updateDto, fanpage);

        fanpageRepository.save(fanpage);
        log.info("Fanpage id: {} updated by system admin", fanpageId);
    }

    @Override
    public void deleteFanpage(Long fanpageId) {
        Fanpage fanpage = fanpageRepository.findById(fanpageId)
                .orElseThrow(() -> new ResourceNotFoundException("Fanpage not found with id: " + fanpageId));

        fanpage.deactivate();
        fanpageRepository.save(fanpage);

        // Deactivate all members of this fanpage
        fanpageMemberRepository.deactivateAllMembersByFanpageId(fanpageId);
        log.info("Fanpage id: {} soft deleted by admin", fanpageId);
    }

    @Override
    public void followFanpage(Long fanpageId, String email) {
        User user = userRepository.findByEmail(email);
        if (user == null) {
            throw new ResourceNotFoundException("User not found");
        }

        Fanpage fanpage = fanpageRepository.findByIdAndIsActive(fanpageId, true)
                .orElseThrow(() -> new ResourceNotFoundException("Fanpage not found with id: " + fanpageId));

        if (!FanpageStatusEnum.ACTIVE.name().equalsIgnoreCase(fanpage.getStatus())) {
            throw new BusinessException("Just can follow fanpage has status (ACTIVE)");
        }

        Optional<Following> existingOpt = followingRepository.findByFanpageIdAndUserId(fanpageId, user.getId());
        if (existingOpt.isPresent()) {
            Following existing = existingOpt.get();
            if (existing.isFollowing()) {
                log.info("User {} is already following fanpage {}", user.getId(), fanpageId);
                return;
            }
            existing.follow();
            existing.setActive(true);
            followingRepository.save(existing);
        } else {
            Following newFollowing = new Following(fanpageId, user.getId());
            followingRepository.save(newFollowing);
        }
        log.info("User {} followed fanpage {}", user.getId(), fanpageId);
    }

    @Override
    public void unfollowFanpage(Long fanpageId, String email) {
        User user = userRepository.findByEmail(email);
        if (user == null) {
            throw new ResourceNotFoundException("User not found");
        }

        if (fanpageRepository.findByIdAndIsActive(fanpageId, true).isEmpty()) {
            throw new ResourceNotFoundException("Fanpage not found with id: " + fanpageId);
        }

        Following following = followingRepository.findByFanpageIdAndUserId(fanpageId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("You not have followed this fanpage"));

        if (!following.isFollowing()) {
            throw new ResourceNotFoundException("You not have followed this fanpage");
        }

        following.unfollow();
        followingRepository.save(following);
        log.info("User {} unfollowed fanpage {}", user.getId(), fanpageId);
    }
}
