package com.ddd.api.controller;

import com.ddd.api.common.BaseResponse;
import com.ddd.api.dto.media.res.MediaResponseDto;
import com.ddd.api.mapper.MediaApiMapper;
import com.ddd.application.dto.media.MediaInfoDto;
import com.ddd.application.enums.MediaCategory;
import com.ddd.application.service.media.MediaUploadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/media")
@RequiredArgsConstructor
public class MediaController {

    private final MediaUploadService mediaUploadService;
    private final MediaApiMapper mediaApiMapper;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public BaseResponse<MediaResponseDto> uploadImage(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "category", required = false, defaultValue = "GENERAL") MediaCategory category) {
        log.info("API: Upload single image with category: {}", category);
        MediaInfoDto mediaInfo = mediaUploadService.uploadImage(file, category);
        return BaseResponse.of(mediaApiMapper.toResponseDto(mediaInfo));
    }

    @PostMapping(value = "/upload/multiple", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public BaseResponse<List<MediaResponseDto>> uploadImages(
            @RequestParam("files") List<MultipartFile> files,
            @RequestParam(value = "category", required = false, defaultValue = "GENERAL") MediaCategory category) {
        log.info("API: Upload {} images with category: {}", files != null ? files.size() : 0, category);
        List<MediaInfoDto> mediaInfoList = mediaUploadService.uploadImages(files, category);
        List<MediaResponseDto> responseList = mediaInfoList.stream()
                .map(mediaApiMapper::toResponseDto)
                .toList();
        return BaseResponse.of(responseList);
    }

    @DeleteMapping
    public BaseResponse<Void> deleteImage(@RequestParam("publicId") String publicId) {
        log.info("API: Delete image with publicId: {}", publicId);
        mediaUploadService.deleteImage(publicId);
        return BaseResponse.ok();
    }
}
