package com.ddd.application.service.media.impl;

import com.ddd.application.dto.media.MediaInfoDto;
import com.ddd.application.enums.MediaCategory;
import com.ddd.application.service.media.MediaUploadService;
import com.ddd.infrastructure.dto.storage.CloudinaryUploadResult;
import com.ddd.infrastructure.service.storage.CloudinaryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MediaUploadServiceImpl implements MediaUploadService {

    private final CloudinaryService cloudinaryService;

    @Override
    public MediaInfoDto uploadImage(MultipartFile file, MediaCategory category) {
        String subFolder = resolveFolder(category);
        log.info("Uploading image to category: {} (subFolder: {})", category, subFolder);

        CloudinaryUploadResult result = cloudinaryService.uploadImage(file, subFolder);
        return mapToMediaInfoDto(result);
    }

    @Override
    public List<MediaInfoDto> uploadImages(List<MultipartFile> files, MediaCategory category) {
        String subFolder = resolveFolder(category);
        log.info("Uploading {} images to category: {} (subFolder: {})", files != null ? files.size() : 0, category, subFolder);

        List<CloudinaryUploadResult> results = cloudinaryService.uploadImages(files, subFolder);
        return results.stream().map(this::mapToMediaInfoDto).toList();
    }

    @Override
    public void deleteImage(String publicId) {
        log.info("Deleting image with publicId: {}", publicId);
        cloudinaryService.deleteImage(publicId);
    }

    private String resolveFolder(MediaCategory category) {
        if (category == null) {
            return MediaCategory.GENERAL.getFolder();
        }
        return category.getFolder();
    }

    private MediaInfoDto mapToMediaInfoDto(CloudinaryUploadResult result) {
        return new MediaInfoDto(
                result.publicId(),
                result.url(),
                result.secureUrl(),
                result.format(),
                result.resourceType(),
                result.bytes(),
                result.width(),
                result.height(),
                result.createdAt()
        );
    }
}
