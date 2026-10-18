package com.ddd.application.service.semesterpoint.impl;

import com.ddd.application.service.semesterpoint.StudentSemesterPointCommandService;
import com.ddd.domain.repository.StudentSemesterPointRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class StudentSemesterPointCommandServiceImpl implements StudentSemesterPointCommandService {
    private final StudentSemesterPointRepository studentSemesterPointRepository;

}
