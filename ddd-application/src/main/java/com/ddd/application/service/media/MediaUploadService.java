package com.ddd.application.service.media;

import com.ddd.application.dto.media.MediaInfoDto;
import com.ddd.application.enums.MediaCategory;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface MediaUploadService {
    MediaInfoDto uploadImage(MultipartFile file, MediaCategory category);
    List<MediaInfoDto> uploadImages(List<MultipartFile> files, MediaCategory category);
    void deleteImage(String publicId);
}
