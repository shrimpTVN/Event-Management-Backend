package com.ddd.application.service.semesterpoint;

import java.util.List;

public interface StudentSemesterPointCommandService {
    void rebuildSemesterPoints(Long userId, Long semesterId);
    void rebuildSemesterPoints(List<Long> userIds, Long semesterId);
}
