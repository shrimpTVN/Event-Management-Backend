package com.ddd.application.service.media.impl;

import com.ddd.application.dto.media.PresignedUploadDto;
import com.ddd.application.dto.media.PresignedUploadRequestDto;
import com.ddd.domain.enums.MediaCategoryEnum;
import com.ddd.domain.exception.InvalidFileException;
import com.ddd.domain.model.port.FileStoragePort;
import com.ddd.domain.model.port.security.SecurityContextPort;
import com.ddd.domain.model.storage.PresignedUploadParam;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MediaUploadServiceImplTest {

    @Mock
    private FileStoragePort fileStoragePort;

    @Mock
    private SecurityContextPort securityContextPort;

    @InjectMocks
    private MediaUploadServiceImpl mediaUploadService;

    @Test
    @DisplayName("shouldAllowPdfUploadForEvidenceCategory")
    void shouldAllowPdfUploadForEvidenceCategory() {
        PresignedUploadRequestDto request = new PresignedUploadRequestDto(MediaCategoryEnum.EVIDENCE, "pdf");
        when(securityContextPort.getCurrentUserId()).thenReturn(10L);

        PresignedUploadParam mockParam = new PresignedUploadParam(
                "https://api.cloudinary.com/v1_1/test/auto/upload",
                "key123",
                123456789L,
                "sig123",
                "evidence",
                "evidence",
                "u_10_abc",
                "test"
        );
        when(fileStoragePort.generatePresignUpload(eq("evidence"), anyString(), isNull())).thenReturn(mockParam);

        PresignedUploadDto result = mediaUploadService.createPresignedUpload(request);

        assertNotNull(result);
        assertEquals("https://api.cloudinary.com/v1_1/test/auto/upload", result.uploadUrl());
        verify(fileStoragePort).generatePresignUpload(eq("evidence"), anyString(), isNull());
    }

    @Test
    @DisplayName("shouldAllowImageUploadForEvidenceCategory")
    void shouldAllowImageUploadForEvidenceCategory() {
        PresignedUploadRequestDto request = new PresignedUploadRequestDto(MediaCategoryEnum.EVIDENCE, ".png");
        when(securityContextPort.getCurrentUserId()).thenReturn(10L);

        PresignedUploadParam mockParam = new PresignedUploadParam(
                "https://api.cloudinary.com/v1_1/test/auto/upload",
                "key123",
                123456789L,
                "sig123",
                "evidence",
                "evidence",
                "u_10_abc",
                "test"
        );
        when(fileStoragePort.generatePresignUpload(eq("evidence"), anyString(), isNull())).thenReturn(mockParam);

        PresignedUploadDto result = mediaUploadService.createPresignedUpload(request);

        assertNotNull(result);
        assertEquals("https://api.cloudinary.com/v1_1/test/auto/upload", result.uploadUrl());
    }

    @Test
    @DisplayName("shouldRejectPdfUploadForAvatarCategory")
    void shouldRejectPdfUploadForAvatarCategory() {
        PresignedUploadRequestDto request = new PresignedUploadRequestDto(MediaCategoryEnum.AVATAR, "pdf");
        when(securityContextPort.getCurrentUserId()).thenReturn(10L);

        assertThrows(InvalidFileException.class, () -> mediaUploadService.createPresignedUpload(request));
    }

    @Test
    @DisplayName("shouldAllowImageUploadForAvatarCategory")
    void shouldAllowImageUploadForAvatarCategory() {
        PresignedUploadRequestDto request = new PresignedUploadRequestDto(MediaCategoryEnum.AVATAR, "webp");
        when(securityContextPort.getCurrentUserId()).thenReturn(10L);

        PresignedUploadParam mockParam = new PresignedUploadParam(
                "https://api.cloudinary.com/v1_1/test/auto/upload",
                "key123",
                123456789L,
                "sig123",
                "avatars",
                "avatars",
                "u_10_abc",
                "test"
        );
        when(fileStoragePort.generatePresignUpload(eq("avatars"), anyString(), isNull())).thenReturn(mockParam);

        PresignedUploadDto result = mediaUploadService.createPresignedUpload(request);

        assertNotNull(result);
        assertEquals("avatars", result.folder());
    }

    @Test
    @DisplayName("shouldRejectUnsupportedFileExtension")
    void shouldRejectUnsupportedFileExtension() {
        PresignedUploadRequestDto request = new PresignedUploadRequestDto(MediaCategoryEnum.EVIDENCE, "exe");
        when(securityContextPort.getCurrentUserId()).thenReturn(10L);

        assertThrows(InvalidFileException.class, () -> mediaUploadService.createPresignedUpload(request));
    }
}
