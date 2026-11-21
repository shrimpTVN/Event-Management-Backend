package com.ddd.api.dto.statistic.res.admin;

import java.util.List;

public record AdminDemographicsResponseDto(
        long totalRegistrations,
        List<AdminDemographicItemResponseDto> cohorts,
        List<AdminDemographicItemResponseDto> schools,
        List<AdminDemographicItemResponseDto> majors
) {}
