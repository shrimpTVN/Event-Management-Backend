package com.ddd.domain.repository;

public interface FanpageStatisticRepository {
    long countEventsByFanpageId(Long fanpageId);
    long countTotalRegistrationsByFanpageId(Long fanpageId);
    long countTotalAttendeesByFanpageId(Long fanpageId);
    long countActiveMembersByFanpageId(Long fanpageId);
}
