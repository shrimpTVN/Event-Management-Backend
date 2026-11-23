package com.ddd.api.mapper;

import com.ddd.api.dto.user.res.StudentProfileDetailsResponseDto;
import com.ddd.api.dto.user.res.UserInfoResponseDto;
import com.ddd.application.dto.user.StudentProfileDetailsDto;
import com.ddd.application.dto.user.UserInfoDto;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", uses = {StaffProfileApiMapper.class})
public interface UserInfoApiMapper {

    UserInfoResponseDto toUserInfoResponseDto(UserInfoDto summary);

    StudentProfileDetailsResponseDto toStudentProfileDetailsResponseDto(StudentProfileDetailsDto details);
}
