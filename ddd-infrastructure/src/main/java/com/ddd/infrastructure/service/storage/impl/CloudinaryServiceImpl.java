package com.ddd.infrastructure.service.storage.impl;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.ddd.domain.exception.FileStorageException;
import com.ddd.domain.exception.InvalidFileException;
import com.ddd.infrastructure.config.properties.CloudinaryProperties;
import com.ddd.infrastructure.dto.storage.CloudinaryUploadResult;
import com.ddd.infrastructure.service.storage.CloudinaryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class CloudinaryServiceImpl implements CloudinaryService {

    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp",
            "image/gif",
            "image/jpg"
    );

    private final Cloudinary cloudinary;
    private final CloudinaryProperties cloudinaryProperties;

    @Override
    public CloudinaryUploadResult uploadImage(MultipartFile file, String subFolder) {
        validateImageFile(file);

        String targetFolder = resolveTargetFolder(subFolder);

        try {
            Map<String, Object> uploadParams = new HashMap<>();
            uploadParams.put("folder", targetFolder);
            uploadParams.put("asset_folder", targetFolder);
            uploadParams.put("resource_type", "image");
            if (cloudinaryProperties.uploadPreset() != null && !cloudinaryProperties.uploadPreset().isBlank()) {
                uploadParams.put("upload_preset", cloudinaryProperties.uploadPreset());
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> uploadResult = (Map<String, Object>) cloudinary.uploader()
                    .upload(file.getBytes(), uploadParams);

            log.info("Successfully uploaded image to Cloudinary with publicId: {}", uploadResult.get("public_id"));

            return mapToUploadResult(uploadResult);
        } catch (IOException e) {
            log.error("Failed to read image bytes during upload: {}", e.getMessage(), e);
            throw new FileStorageException("Failed to read image content", e);
        } catch (Exception e) {
            log.error("Failed to upload image to Cloudinary: {}", e.getMessage(), e);
            throw new FileStorageException("Failed to upload image to Cloudinary: " + e.getMessage(), e);
        }
    }

    @Override
    public List<CloudinaryUploadResult> uploadImages(List<MultipartFile> files, String subFolder) {
        if (files == null || files.isEmpty()) {
            throw new InvalidFileException("File list must not be empty");
        }

        List<CloudinaryUploadResult> results = new ArrayList<>();
        for (MultipartFile file : files) {
            results.add(uploadImage(file, subFolder));
        }
        return results;
    }

    @Override
    public void deleteImage(String publicId) {
        if (publicId == null || publicId.isBlank()) {
            throw new InvalidFileException("Public ID must not be blank for image deletion");
        }

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> destroyResult = (Map<String, Object>) cloudinary.uploader()
                    .destroy(publicId, ObjectUtils.emptyMap());

            String result = (String) destroyResult.get("result");
            if (!"ok".equalsIgnoreCase(result)) {
                log.warn("Cloudinary delete result for publicId {}: {}", publicId, result);
            } else {
                log.info("Successfully deleted image from Cloudinary with publicId: {}", publicId);
            }
        } catch (Exception e) {
            log.error("Failed to delete image from Cloudinary with publicId {}: {}", publicId, e.getMessage(), e);
            throw new FileStorageException("Failed to delete image from Cloudinary: " + e.getMessage(), e);
        }
    }

    private void validateImageFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidFileException("File must not be empty");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_IMAGE_TYPES.contains(contentType.toLowerCase())) {
            throw new InvalidFileException("Invalid file format. Allowed formats: JPEG, PNG, WEBP, GIF");
        }
    }

    private String resolveTargetFolder(String subFolder) {
        String prefix = cloudinaryProperties.folderPrefix();
        if (prefix == null || prefix.isBlank()) {
            return subFolder != null && !subFolder.isBlank() ? subFolder.trim() : "general";
        }
        if (subFolder == null || subFolder.isBlank()) {
            return prefix.trim();
        }
        return prefix.trim() + "/" + subFolder.trim();
    }

    private CloudinaryUploadResult mapToUploadResult(Map<String, Object> uploadResult) {
        String publicId = (String) uploadResult.get("public_id");
        String url = (String) uploadResult.get("url");
        String secureUrl = (String) uploadResult.get("secure_url");
        String format = (String) uploadResult.get("format");
        String resourceType = (String) uploadResult.get("resource_type");

        long bytes = uploadResult.get("bytes") instanceof Number num ? num.longValue() : 0L;
        int width = uploadResult.get("width") instanceof Number num ? num.intValue() : 0;
        int height = uploadResult.get("height") instanceof Number num ? num.intValue() : 0;
        String createdAt = uploadResult.get("created_at") != null ? uploadResult.get("created_at").toString() : null;

        return new CloudinaryUploadResult(
                publicId,
                url,
                secureUrl,
                format,
                resourceType,
                bytes,
                width,
                height,
                createdAt
        );
    }
}
