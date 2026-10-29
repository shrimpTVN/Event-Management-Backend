package com.ddd.domain.repository;

import com.ddd.domain.model.StudentSemesterPoint;

import java.util.ArrayList;
import java.util.List;

public interface StudentSemesterPointRepository {
    StudentSemesterPoint findByUserIdAndSemesterIdAndPointCategoryId(Long userId, Long semesterId, Long pointCategoryId);

    List<StudentSemesterPoint> findAllIn(List<Long> studentUserIds, Long semesterId, List<Long> pointCategoryIds);

    void saveAll(ArrayList<StudentSemesterPoint> studentSemesterPoints);

    void save(StudentSemesterPoint studentSemesterPoint);
}
