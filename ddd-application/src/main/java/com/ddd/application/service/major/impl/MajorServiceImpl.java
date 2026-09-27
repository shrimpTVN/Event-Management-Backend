package com.ddd.application.service.major.impl;

import com.ddd.application.dto.school.MajorDto;
import com.ddd.application.mapper.MajorDtoMapper;
import com.ddd.application.service.major.MajorService;
import com.ddd.domain.model.Major;
import com.ddd.domain.repository.MajorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MajorServiceImpl implements MajorService {
    private final MajorRepository majorRepository;
    private final MajorDtoMapper majorDtoMapper;

    @Override
    public List<MajorDto> findAll(){
        return majorRepository.findAll()
                .stream()
                .map(majorDtoMapper::toMajorDto)
                .toList();
    }

    @Override
    public MajorDto findById(Long id){
        Major major = majorRepository.findById(id);
        return majorDtoMapper.toMajorDto(major);
    }

    @Override
    @Transactional
    public MajorDto createMajor(MajorDto majorDto){
        Major major = majorDtoMapper.toMajorEntity(majorDto);
        return majorDtoMapper.toMajorDto(majorRepository.saveMajor(major));
    }

    @Override
    @Transactional
    public MajorDto updateMajor(Long id, MajorDto majorDto){
        Major major = majorRepository.findById(id);
        majorDtoMapper.updateMajorEntityFromDto(major, majorDto);
        return majorDtoMapper.toMajorDto(majorRepository.saveMajor(major));
    }

    @Override
    @Transactional
    public void deleteMajor(Long id){
        majorRepository.deleteMajor(id);
    }


}
