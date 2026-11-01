package com.ddd.application.service.media.impl;

import com.ddd.application.dto.media.PresignedUploadDto;
import com.ddd.application.dto.media.PresignedUploadRequestDto;
import com.ddd.application.service.media.MediaUploadService;
import com.ddd.domain.enums.MediaCategoryEnum;
import com.ddd.domain.exception.InvalidFileException;
import com.ddd.domain.model.port.FileStoragePort;
import com.ddd.domain.model.port.security.SecurityContextPort;
import com.ddd.domain.model.storage.PresignedUploadParam;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class MediaUploadServiceImpl implements MediaUploadService {
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg","jpeg","png","webp");
    private final FileStoragePort fileStoragePort;
    private final SecurityContextPort securityContextPort;


    @Override
    public PresignedUploadDto createPresignedUpload(PresignedUploadRequestDto request) {
        MediaCategoryEnum category = request.category() != null ? request.category() : MediaCategoryEnum.GENERAL;
        Long currentUserId = securityContextPort.getCurrentUserId();

        if (request.fileExtension() != null && !request.fileExtension().isBlank()){
            String ext = request.fileExtension().toLowerCase().replace(".","");
            if(!ALLOWED_EXTENSIONS.contains(ext)){
                throw new InvalidFileException("Unsupported file extension: " + ext);
            }
        }

        String customPublicId = String.format("u_%s_%s", currentUserId != null ? currentUserId : "anon",
                UUID.randomUUID().toString().replace("-","").substring(0,16));

        PresignedUploadParam param = fileStoragePort.generatePresignUpload(category.getFolder(), customPublicId, null);
        return new PresignedUploadDto(
                param.uploadUrl(),
                param.apiKey(),
                param.timestamp(),
                param.signature(),
                param.folder(),
                param.assetFolder(),
                param.publicId(),
                param.cloudName()
        );
    }

    @Override
    public void deleteImage( String publicId) {
        Long  currentUserId = securityContextPort.getCurrentUserId();
        boolean isAdmin = securityContextPort.hasRole("ADMIN");
        if (!isAdmin && currentUserId != null){
            String userPrefix = "u_" + currentUserId + "_";
            if (!publicId.contains(userPrefix)){
                throw new InvalidFileException("You are not authorized to delete this asset");
            }
        }

        fileStoragePort.deleteFile(publicId);
    }


}
