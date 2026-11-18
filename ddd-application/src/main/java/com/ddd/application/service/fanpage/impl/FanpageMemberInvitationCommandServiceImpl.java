package com.ddd.application.service.fanpage.impl;

import com.ddd.application.service.fanpage.FanpageMemberInvitationCommandService;
import com.ddd.domain.enums.InvatitationStatusEnum;
import com.ddd.domain.event.FanpageInvitationAcceptedEvent;
import com.ddd.domain.event.FanpageInvitationRejectedEvent;
import com.ddd.domain.exception.AuthException;
import com.ddd.domain.exception.BusinessException;
import com.ddd.domain.exception.ResourceNotFoundException;
import com.ddd.domain.model.FanpageMember;
import com.ddd.domain.model.FanpageMemberInvitation;
import com.ddd.domain.model.User;
import com.ddd.domain.model.port.security.SecurityContextPort;
import com.ddd.domain.repository.FanpageMemberInvitationRepository;
import com.ddd.domain.repository.FanpageMemberRepository;
import com.ddd.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class FanpageMemberInvitationCommandServiceImpl implements FanpageMemberInvitationCommandService {

    private final FanpageMemberInvitationRepository invitationRepository;
    private final UserRepository userRepository;
    private final FanpageMemberRepository fanpageMemberRepository;
    private final SecurityContextPort securityContextPort;
    private final ApplicationEventPublisher applicationEventPublisher;

    @Override
    public void acceptInvitation(Long invitationId, String userEmail) {
        FanpageMemberInvitation invitation = getValidPendingInvitation(invitationId);

        Long userId = securityContextPort.getCurrentUserId();

        if (!invitation.getUserId().equals(userId)) {
            throw new AuthException("You are not the recipient of this invitation");
        }

        invitation.setStatus(InvatitationStatusEnum.ACCEPTED);
        invitationRepository.save(invitation);

        Long fanpageId = invitation.getFanpageId();

        if (!fanpageMemberRepository.isActiveMember( fanpageId, userId)) {
            FanpageMember newMember = new FanpageMember(fanpageId, userId, "MEMBER");
            fanpageMemberRepository.save(newMember);
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        applicationEventPublisher.publishEvent(new FanpageInvitationAcceptedEvent(
                fanpageId,
                user.getId(),
                user.getFullName(),
                user.getAvatarUrl()
        ));

        log.info("Invitation {} accepted by user {}", invitationId, userEmail);
    }

    @Override
    public void declineInvitation(Long invitationId, String userEmail) {
        FanpageMemberInvitation invitation = getValidPendingInvitation(invitationId);

        User user = userRepository.findByEmail(userEmail);
        if (user == null) {
            throw new ResourceNotFoundException("User not found with email: " + userEmail);
        }

        if (!invitation.getUserId().equals(user.getId())) {
            throw new AuthException("You are not the recipient of this invitation");
        }

        invitation.setStatus(InvatitationStatusEnum.REJECTED);
        invitationRepository.save(invitation);

        applicationEventPublisher.publishEvent(new FanpageInvitationRejectedEvent(
                invitation.getFanpageId(),
                user.getId(),
                user.getFullName(),
                user.getAvatarUrl()
        ));

        log.info("Invitation {} declined by user {}", invitationId, userEmail);
    }

    @Override
    public void cancelInvitation(Long invitationId, String adminEmail) {
        Long adminId = securityContextPort.getCurrentUserId();

        FanpageMemberInvitation invitation = getValidPendingInvitation(invitationId);
        Long fanpageId = invitation.getFanpageId();
        if (!fanpageMemberRepository.isAdminOfFanpage(fanpageId, adminId)) {
            throw new AuthException("You are not an admin of this fanpage");
        }

        invitation.setStatus(InvatitationStatusEnum.CANCELED);
        invitationRepository.save(invitation);

        log.info("Invitation {} canceled by admin {}", invitationId, adminEmail);
    }

    private FanpageMemberInvitation getValidPendingInvitation(Long invitationId) {
        FanpageMemberInvitation invitation = invitationRepository.findById(invitationId)
                .orElseThrow(() -> new ResourceNotFoundException("Invitation not found with id: " + invitationId));

        if (invitation.getStatus() != InvatitationStatusEnum.PENDING) {
            throw new BusinessException("Invitation is no longer pending (current status: " + invitation.getStatus() + ")");
        }

        return invitation;
    }
}
