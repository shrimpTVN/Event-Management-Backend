package com.ddd.application.service.fanpage;

import com.ddd.application.dto.statistic.AlertAbsentStudentsDto;

public interface FanpageStatisticCommandService {
    void alertAbsentStudents(Long fanpageId, AlertAbsentStudentsDto dto);
}
