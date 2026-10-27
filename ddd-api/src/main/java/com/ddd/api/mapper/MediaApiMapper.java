package com.ddd.api.mapper;

import com.ddd.api.dto.media.res.MediaResponseDto;
import com.ddd.application.dto.media.MediaInfoDto;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface MediaApiMapper {
    MediaResponseDto toResponseDto(MediaInfoDto mediaInfoDto);
}
