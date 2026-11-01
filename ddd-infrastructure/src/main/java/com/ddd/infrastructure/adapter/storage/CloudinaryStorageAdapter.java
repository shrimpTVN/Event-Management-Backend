package com.ddd.infrastructure.adapter.storage;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.ddd.domain.exception.FileStorageException;
import com.ddd.domain.exception.InvalidFileException;
import com.ddd.domain.model.port.FileStoragePort;
import com.ddd.domain.model.storage.PresignedUploadParam;
import com.ddd.infrastructure.config.properties.CloudinaryProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import com.ddd.domain.enums.MediaCategoryEnum;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class CloudinaryStorageAdapter implements FileStoragePort {

    private final Cloudinary cloudinary;
    private final CloudinaryProperties cloudinaryProperties;

    @Override
    public PresignedUploadParam generatePresignUpload(String subFolder, String customPublicId, Map<String, Object> additionalParams) {
        long timestamp = Instant.now().getEpochSecond();
        String folder = resolveTargetFolder(subFolder);

        Map<String, Object> paramsToSign = new HashMap<>();
        paramsToSign.put("timestamp", timestamp);
        paramsToSign.put("folder", folder);
        paramsToSign.put("asset_folder", folder);

        if (customPublicId != null && !customPublicId.isBlank()){
            paramsToSign.put("public_id", customPublicId);
        }

        if (additionalParams != null){
            paramsToSign.putAll(additionalParams);
        }

        try{
            String signature = cloudinary.apiSignRequest(paramsToSign, cloudinaryProperties.apiSecret(), cloudinary.config.signatureVersion);
            String uploadUrl = String.format("https://api.cloudinary.com/v1_1/%s/image/upload", cloudinaryProperties.cloudName());

            return new PresignedUploadParam(
                    uploadUrl,
                    cloudinaryProperties.apiKey(),
                    timestamp,
                    signature,
                    folder,
                    folder,
                    customPublicId,
                    cloudinaryProperties.cloudName()
            );
        } catch(Exception e){
            log.error("Failed to generate presigned upload signature: {}", e.getMessage(),e);
            throw new FileStorageException("Failed to generate presigned upload signature");
        }

    }

    @Override
    public void deleteFile(String publicId) {
        if (publicId == null || publicId.isBlank()){
            throw new InvalidFileException("Public ID is null or blank");
        }

        String prefix = cloudinaryProperties.folderPrefix();
        if (prefix != null && !prefix.isBlank() && !publicId.startsWith(prefix + "/")){
            log.warn("Security alert: Attempted deletion outside configured prefix '{}': {}", prefix, publicId);
            throw new InvalidFileException("Unauthorized file path deletion request");
        }

        try {
            @SuppressWarnings("unchecked")
                    Map<String, Object> result = (Map<String, Object>) cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
            log.info("Cloudinary delete result for {}: {}", publicId, result.get("result"));
        } catch(Exception e){
            log.error("Failed to delete asset {}: {}",publicId, e.getMessage(), e);
            throw new FileStorageException("Failed to delete file from storage: " + e.getMessage(), e);
        }
    }

    private String resolveTargetFolder(String subFolder) {
        String prefix = cloudinaryProperties.folderPrefix();
        if (prefix == null || prefix.isEmpty()){
            return subFolder != null && !subFolder.isBlank() ? subFolder.trim() : MediaCategoryEnum.GENERAL.getFolder();
        }

        if (subFolder == null || subFolder.isBlank()){
            return prefix.trim();
        }

        return prefix.trim() + "/" + subFolder.trim();
    }
}
