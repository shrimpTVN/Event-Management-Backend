package com.ddd.api.controller;

import com.ddd.api.common.BaseResponse;
import com.ddd.api.dto.semester.SemesterRequestDto;
import com.ddd.api.dto.semester.SemesterResponseDto;
import com.ddd.api.mapper.SemesterApiMapper;
import com.ddd.application.dto.SemesterDto;
import com.ddd.application.service.SemesterService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/semesters")
@RequiredArgsConstructor
public class SemesterController {
    private final SemesterService semesterService;
    private final SemesterApiMapper semesterApiMapper;

    @GetMapping({"","/"})
    public BaseResponse<List<SemesterResponseDto>> getAllActiveSemester() {
        List<SemesterDto> semesterDtos = semesterService.findAllActive();
        return BaseResponse.of(semesterDtos.stream().map(semesterApiMapper::toResponseDto).toList());
    }

    @GetMapping({"/admin","/admin/"})
    public BaseResponse<List<SemesterResponseDto>> getAllSemester() {
        List<SemesterDto> semesterDtos = semesterService.findAll();
        return BaseResponse.of(semesterDtos.stream().map(semesterApiMapper::toResponseDto).toList());
    }

    @GetMapping("/{id}")
    public BaseResponse<SemesterResponseDto> getSemesterById(@PathVariable Long id){
        SemesterDto semesterDto = semesterService.findById(id);
        return BaseResponse.of(semesterApiMapper.toResponseDto(semesterDto));
    }

    @GetMapping("/current")
    public BaseResponse<SemesterResponseDto> getSemesterCurrentSemester(){
        SemesterDto semesterDto = semesterService.getCurrent();
        return BaseResponse.of(semesterApiMapper.toResponseDto(semesterDto));
    }

    @PostMapping({"","/"})
    public BaseResponse<SemesterResponseDto> createSemester(@RequestBody @Valid SemesterRequestDto requestDto) {
        SemesterDto semesterDto = semesterApiMapper.toDto(requestDto);
        SemesterDto createdSemester = semesterService.createSemester(semesterDto);
        return BaseResponse.of(semesterApiMapper.toResponseDto(createdSemester));
    }

    @PutMapping("/{id}")
    public BaseResponse<SemesterResponseDto> updateSemester(@PathVariable Long id, @RequestBody @Valid SemesterRequestDto requestDto) {
        SemesterDto semesterDto = semesterApiMapper.toDto(requestDto);
        SemesterDto updatedSemester = semesterService.updateSemester(id, semesterDto);
        return BaseResponse.of(semesterApiMapper.toResponseDto(updatedSemester));
    }

    @DeleteMapping("/{id}")
    public BaseResponse<Object> deleteSemester(@PathVariable Long id) {
        semesterService.deleteSemester(id);
        return BaseResponse.ok();
    }
}
