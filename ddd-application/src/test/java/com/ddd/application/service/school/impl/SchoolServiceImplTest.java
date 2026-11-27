package com.ddd.application.service.school.impl;

import com.ddd.application.dto.school.SchoolDetailDto;
import com.ddd.application.mapper.SchoolDtoMapper;
import com.ddd.domain.model.Major;
import com.ddd.domain.model.School;
import com.ddd.domain.repository.MajorRepository;
import com.ddd.domain.repository.SchoolRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SchoolServiceImplTest {

    @Mock
    private SchoolRepository schoolRepository;

    @Mock
    private MajorRepository majorRepository;

    private SchoolDtoMapper schoolDtoMapper;
    private SchoolServiceImpl schoolService;

    @BeforeEach
    void setUp() {
        schoolDtoMapper = Mappers.getMapper(SchoolDtoMapper.class);
        schoolService = new SchoolServiceImpl(schoolRepository, majorRepository, schoolDtoMapper);
    }

    @Test
    @DisplayName("findAllWithMajors groups majors per school in 2 queries and preserves major isActive state")
    void findAllWithMajors_groupsMajorsAndPreservesIsActive() {
        School ict = new School();
        ict.setId(1L);
        ict.setName("Trường CNTT&TT");
        ict.setActive(true);

        School econ = new School();
        econ.setId(2L);
        econ.setName("Khoa Kinh tế");
        econ.setActive(true);

        Major cntt = new Major();
        cntt.setId(10L);
        cntt.setCode("CNTT");
        cntt.setName("Công nghệ thông tin");
        cntt.setSchoolId(1L);
        cntt.setActive(true);

        Major ktpm = new Major();
        ktpm.setId(11L);
        ktpm.setCode("KTPM");
        ktpm.setName("Kỹ thuật phần mềm");
        ktpm.setSchoolId(1L);
        ktpm.setActive(false);

        when(schoolRepository.findAll()).thenReturn(List.of(ict, econ));
        when(majorRepository.findAll()).thenReturn(List.of(cntt, ktpm));

        List<SchoolDetailDto> result = schoolService.findAllWithMajors();

        assertEquals(2, result.size());

        SchoolDetailDto ictDto = result.get(0);
        assertEquals(1L, ictDto.id());
        assertTrue(ictDto.isActive());
        assertEquals(2, ictDto.majors().size());
        assertTrue(ictDto.majors().get(0).isActive(), "Active major must map isActive=true");
        assertFalse(ictDto.majors().get(1).isActive(), "Inactive major must map isActive=false");

        SchoolDetailDto econDto = result.get(1);
        assertEquals(2L, econDto.id());
        assertTrue(econDto.majors().isEmpty());

        verify(schoolRepository, times(1)).findAll();
        verify(majorRepository, times(1)).findAll();
        verifyNoMoreInteractions(schoolRepository, majorRepository);
    }
}
