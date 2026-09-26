package com.ddd.api.controller;

import com.ddd.api.common.BaseResponse;
import com.ddd.api.dto.major.req.CreateMajorRequestDto;
import com.ddd.api.dto.major.req.UpdateMajorRequestDto;
import com.ddd.api.dto.major.res.MajorResponseDto;
import com.ddd.api.mapper.MajorApiMapper;
import com.ddd.application.dto.school.MajorDto;
import com.ddd.application.service.major.MajorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/majors")
@RequiredArgsConstructor
public class MajorController {

    private final MajorService majorService;
    private final MajorApiMapper majorApiMapper;

    @GetMapping
    public BaseResponse<List<MajorResponseDto>> getMajors() {
        List<MajorResponseDto> response = majorService.findAll().stream()
                .map(majorApiMapper::toMajorResponseDto)
                .toList();
        return BaseResponse.of(response);
    }

    @GetMapping("/{id}")
    public BaseResponse<MajorResponseDto> getMajorById(@PathVariable Long id) {
        MajorDto major = majorService.findById(id);
        return BaseResponse.of(majorApiMapper.toMajorResponseDto(major));
    }

    @PostMapping("/admin")
    public BaseResponse<MajorResponseDto> createMajor(@Valid @RequestBody CreateMajorRequestDto req) {
        MajorDto dto = majorApiMapper.toMajorDto(req);
        MajorDto created = majorService.createMajor(dto);
        return BaseResponse.of(majorApiMapper.toMajorResponseDto(created));
    }

    @PutMapping("/admin/{id}")
    public BaseResponse<MajorResponseDto> updateMajor(@PathVariable Long id, @Valid @RequestBody UpdateMajorRequestDto req) {
        MajorDto dto = majorApiMapper.toMajorDto(req);
        MajorDto updated = majorService.updateMajor(id, dto);
        return BaseResponse.of(majorApiMapper.toMajorResponseDto(updated));
    }

    @DeleteMapping("/admin/{id}")
    public void deleteMajor(@PathVariable Long id) {
        majorService.deleteMajor(id);
    }
}

