package com.ddd.application.service.fanpage;

public interface FanpageMemberInvitationCommandService {
    void acceptInvitation(Long invitationId, String userEmail);

    void declineInvitation(Long invitationId, String userEmail);

    void cancelInvitation(Long invitationId, String adminEmail);
}
