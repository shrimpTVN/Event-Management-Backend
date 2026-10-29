package com.ddd.domain.model;

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

    public void deactivate() {
        this.setActive(false);
    }
}
