package com.ddd.domain.model;

import com.ddd.domain.exception.BusinessException;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Fanpage extends BaseModel {
    private String name;
    private String description;
    private String avatarUrl;
    private String status;
    private String orgType;
    private String orgName;
    private String parentOrg;
    private String helpContact;
    private String helpPhone;
    private String rejectionReason;
    private String evidenceUrl;

    public void updateByFanpageAdmin(String description, String avatarUrl, String helpContact, String helpPhone) {
        if (description != null) this.description = description.trim();
        if (avatarUrl != null) this.avatarUrl = avatarUrl.trim();
        if (helpContact != null) this.helpContact = helpContact.trim();
        if (helpPhone != null) this.helpPhone = helpPhone.trim();
    }

    public void updateByAdmin(String name, String description, String avatarUrl, String orgType,
                              String orgName, String parentOrg, String helpContact, String helpPhone, String status) {
        if (name != null && !name.isBlank()) this.name = name.trim();
        if (description != null) this.description = description.trim();
        if (avatarUrl != null) this.avatarUrl = avatarUrl.trim();
        if (orgType != null && !orgType.isBlank()) this.orgType = orgType.trim();
        if (orgName != null && !orgName.isBlank()) this.orgName = orgName.trim();
        if (parentOrg != null && !parentOrg.isBlank()) this.parentOrg = parentOrg.trim();
        if (helpContact != null) this.helpContact = helpContact.trim();
        if (helpPhone != null) this.helpPhone = helpPhone.trim();
        if (status != null && !status.isBlank()) this.status = status.trim();
    }

    public void reject(String reason) {
        if (reason == null || reason.trim().isEmpty()) {
            throw BusinessException.withField("reason", "Rejection reason is required");
        }
        this.status = "REJECTED";
        this.rejectionReason = reason.trim();
        this.setActive(false);
    }

    public void approve() {
        this.status = "ACTIVE";
        this.rejectionReason = null;
        this.setActive(true);
    }

    public void unban() {
        this.status = "ACTIVE";
        this.rejectionReason = null;
        this.setActive(true);
    }

    public void deactivate() {
        this.setActive(false);
    }
}
