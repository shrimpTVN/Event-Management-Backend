package com.ddd.domain.repository;

import com.ddd.domain.model.FanpageAdminProfile;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public interface FanpageAdminProfileRepository {

    void createFanpageAdminProfile(FanpageAdminProfile fanpageAdminProfile, Long userId);

    boolean existsByStaffId(@NotNull @NotBlank String s);
}
