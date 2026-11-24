package com.ddd.application.dto.statistic.admin;

import java.util.List;

public record AdminDemographicsDto(
        long totalRegistrations,
        List<AdminDemographicItemDto> cohorts,
        List<AdminDemographicItemDto> schools,
        List<AdminDemographicItemDto> majors
) {}
