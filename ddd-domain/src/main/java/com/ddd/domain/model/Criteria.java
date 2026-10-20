package com.ddd.domain.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Criteria extends BaseModel {
    private String name;
    private String note;
    private String scope;
    private Integer level = 0;
    private Integer childrenOrder = 1;
    private Integer requiredChildren;
    private Long parentId;

    public void setLevel(Integer level) {
        if (level != null && level < 0) {
            throw new IllegalArgumentException("Level cannot be negative");
        }
        this.level = level;
    }

    public void setChildrenOrder(Integer childrenOrder) {
        if (childrenOrder != null && childrenOrder < 0) {
            throw new IllegalArgumentException("Children order must be non-negative");
        }
        this.childrenOrder = childrenOrder;
    }

    public void setRequiredChildren(Integer requiredChildren) {
        if (requiredChildren != null && requiredChildren <= 0) {
            throw new IllegalArgumentException("Required children must be positive");
        }
        this.requiredChildren = requiredChildren;
    }

    // Deprecated alias for backwards-compatibility during refactoring
    @Deprecated
    public String getDescription() {
        return note;
    }

    @Deprecated
    public void setDescription(String description) {
        this.note = description;
    }
}
