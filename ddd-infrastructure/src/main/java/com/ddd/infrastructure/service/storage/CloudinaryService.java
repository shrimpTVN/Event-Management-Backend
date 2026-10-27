package com.ddd.infrastructure.service.storage;

import com.ddd.infrastructure.dto.storage.CloudinaryUploadResult;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface CloudinaryService {
    CloudinaryUploadResult uploadImage(MultipartFile file, String subFolder);
    List<CloudinaryUploadResult> uploadImages(List<MultipartFile> files, String subFolder);
    void deleteImage(String publicId);
}
