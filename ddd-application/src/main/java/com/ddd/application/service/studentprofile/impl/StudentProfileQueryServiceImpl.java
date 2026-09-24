package com.ddd.application.service.studentprofile.impl;

import com.ddd.application.service.studentprofile.StudentProfileQueryService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class StudentProfileQueryServiceImpl implements StudentProfileQueryService {
}
