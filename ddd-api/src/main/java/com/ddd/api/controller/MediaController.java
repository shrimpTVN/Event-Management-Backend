package com.ddd.api.controller;

import com.ddd.api.common.BaseResponse;
import com.ddd.api.dto.media.req.PresignedRequestDto;
import com.ddd.api.dto.media.res.PresignedResponseDto;
import com.ddd.api.mapper.MediaApiMapper;
import com.ddd.application.dto.media.PresignedUploadDto;
import com.ddd.application.dto.media.PresignedUploadRequestDto;
import com.ddd.application.service.media.MediaUploadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Objects;

@Slf4j
@RestController
@RequestMapping("/media")
@RequiredArgsConstructor
public class MediaController {

    private final MediaUploadService mediaUploadService;
    private final MediaApiMapper mediaApiMapper;

    @PostMapping("/presign")
    public BaseResponse<PresignedResponseDto> getPresignedUpload(
            @RequestBody(required = false) PresignedRequestDto requestDto) {
        PresignedUploadRequestDto appRequest = mediaApiMapper.toApplicationDto(requestDto);
        PresignedUploadDto result = mediaUploadService.createPresignedUpload(appRequest);
        return BaseResponse.of(mediaApiMapper.toResponseDto(result));
    }

    @DeleteMapping
    public BaseResponse<Void> deleteImage(
            @RequestParam("publicId") String publicId
    ) {
        mediaUploadService.deleteImage(publicId);
        return BaseResponse.ok();
    }


}
