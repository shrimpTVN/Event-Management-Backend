package com.ddd.application.service;

import com.ddd.application.dto.auth.SemesterDto;

import java.util.List;

public interface SemesterService {
    List<SemesterDto> findAll();
}
