package com.ddd.api.mapper;

import com.ddd.api.dto.registration.res.AttendeeResponseDto;
import com.ddd.api.dto.registration.res.RegistrationArrangedResponseDto;
import com.ddd.api.dto.registration.res.RegistrationResponseDto;
import com.ddd.api.dto.registration.res.StudentProfileBasicResponseDto;
import com.ddd.api.dto.registration.res.StudentRegistrationResponseDto;
import com.ddd.application.dto.fanpage.AttendeeInfoDto;
import com.ddd.application.dto.registration.RegistrationArrangedInfoDto;
import com.ddd.application.dto.registration.RegistrationDto;
import com.ddd.application.dto.registration.RegistrationInfoDto;
import com.ddd.application.dto.registration.StudentProfileBasicInfoDto;
import com.ddd.application.dto.registration.StudentRegistrationInfoDto;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", uses = {EventApiMapper.class})
public interface RegistrationApiMapper {
    RegistrationResponseDto toResponseDto(RegistrationInfoDto registrationInfoDto);
    AttendeeResponseDto toAttendeeResponseDto(AttendeeInfoDto attendeeInfoDto);
    RegistrationArrangedResponseDto toArrangedResponseDto(RegistrationArrangedInfoDto registrationArrangedInfoDto);
    StudentProfileBasicResponseDto toStudentProfileBasicResponseDto(StudentProfileBasicInfoDto infoDto);
    StudentRegistrationResponseDto toStudentRegistrationResponseDto(StudentRegistrationInfoDto infoDto);
}
