package com.ddd.api.mapper;

import com.ddd.api.dto.media.req.PresignedRequestDto;
import com.ddd.api.dto.media.res.MediaResponseDto;
import com.ddd.api.dto.media.res.PresignedResponseDto;
import com.ddd.application.dto.media.MediaInfoDto;
import com.ddd.application.dto.media.PresignedUploadDto;
import com.ddd.application.dto.media.PresignedUploadRequestDto;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface MediaApiMapper {
    MediaResponseDto toResponseDto(MediaInfoDto mediaInfoDto);
    PresignedUploadRequestDto toApplicationDto(PresignedRequestDto presignedRequestDto);
    PresignedResponseDto  toResponseDto(PresignedUploadDto presignedUploadDto);
}
