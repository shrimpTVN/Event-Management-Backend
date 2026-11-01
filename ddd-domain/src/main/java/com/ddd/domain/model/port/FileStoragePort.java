package com.ddd.domain.model.port;

import com.ddd.domain.model.storage.PresignedUploadParam;

import java.util.Map;

public interface FileStoragePort {
    PresignedUploadParam generatePresignUpload(String subFolder, String customPublicId, Map<String, Object> additionalParams);
    void deleteFile(String publicId);
}
