package com.ddd.domain.repository;

import com.ddd.domain.model.StudentSemesterPoint;

import java.util.ArrayList;
import java.util.List;

public interface StudentSemesterPointRepository {
    StudentSemesterPoint findByUserIdAndSemesterIdAndPointCategoryId(Long userId, Long semesterId, Long pointCategoryId);

    List<StudentSemesterPoint> findAllByUserIdAndSemesterId(Long userId, Long semesterId);

    List<StudentSemesterPoint> findAllByUserIdsAndSemesterId(List<Long> userIds, Long semesterId);

    List<StudentSemesterPoint> findAllIn(List<Long> studentUserIds, Long semesterId, List<Long> pointCategoryIds);

    void saveAll(List<StudentSemesterPoint> studentSemesterPoints);

    void save(StudentSemesterPoint studentSemesterPoint);
}
