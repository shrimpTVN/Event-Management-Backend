package com.ddd.infrastructure.service.storage.impl;

import com.cloudinary.Cloudinary;
import com.cloudinary.Uploader;
import com.ddd.domain.exception.FileStorageException;
import com.ddd.domain.exception.InvalidFileException;
import com.ddd.infrastructure.config.properties.CloudinaryProperties;
import com.ddd.infrastructure.dto.storage.CloudinaryUploadResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CloudinaryServiceImplTest {

    @Mock
    private Cloudinary cloudinary;

    @Mock
    private Uploader uploader;

    private CloudinaryProperties cloudinaryProperties;
    private CloudinaryServiceImpl cloudinaryService;

    @BeforeEach
    void setUp() {
        cloudinaryProperties = new CloudinaryProperties("test-cloud", "test-key", "test-secret", "event-management", "upload-EventManagemen");
        cloudinaryService = new CloudinaryServiceImpl(cloudinary, cloudinaryProperties);
    }

    @Test
    void uploadImage_withEmptyFile_throwsInvalidFileException() {
        MockMultipartFile emptyFile = new MockMultipartFile("file", "test.jpg", "image/jpeg", new byte[0]);

        InvalidFileException exception = assertThrows(InvalidFileException.class, () ->
                cloudinaryService.uploadImage(emptyFile, "avatars")
        );
        assertEquals("File must not be empty", exception.getMessage());
    }

    @Test
    void uploadImage_withInvalidMimeType_throwsInvalidFileException() {
        MockMultipartFile pdfFile = new MockMultipartFile("file", "test.pdf", "application/pdf", new byte[]{1, 2, 3});

        InvalidFileException exception = assertThrows(InvalidFileException.class, () ->
                cloudinaryService.uploadImage(pdfFile, "avatars")
        );
        assertTrue(exception.getMessage().contains("Invalid file format"));
    }

    @Test
    void uploadImage_success_returnsCloudinaryUploadResult() throws Exception {
        MockMultipartFile validFile = new MockMultipartFile("file", "avatar.png", "image/png", new byte[]{1, 2, 3, 4});

        org.mockito.ArgumentCaptor<Map> paramsCaptor = org.mockito.ArgumentCaptor.forClass(Map.class);
        when(cloudinary.uploader()).thenReturn(uploader);
        when(uploader.upload(any(byte[].class), paramsCaptor.capture())).thenReturn(Map.of(
                "public_id", "event-management/avatars/sample123",
                "url", "http://res.cloudinary.com/test-cloud/image/upload/v1/sample123.png",
                "secure_url", "https://res.cloudinary.com/test-cloud/image/upload/v1/sample123.png",
                "format", "png",
                "resource_type", "image",
                "bytes", 1024L,
                "width", 400,
                "height", 400,
                "created_at", "2026-10-22T00:00:00Z"
        ));

        CloudinaryUploadResult result = cloudinaryService.uploadImage(validFile, "avatars");

        assertNotNull(result);
        assertEquals("event-management/avatars/sample123", result.publicId());
        assertEquals("https://res.cloudinary.com/test-cloud/image/upload/v1/sample123.png", result.secureUrl());
        assertEquals("png", result.format());
        assertEquals(400, result.width());
        assertEquals(400, result.height());

        Map<String, Object> capturedParams = paramsCaptor.getValue();
        assertEquals("event-management/avatars", capturedParams.get("folder"));
        assertEquals("event-management/avatars", capturedParams.get("asset_folder"));
        assertEquals("upload-EventManagemen", capturedParams.get("upload_preset"));
    }

    @Test
    void uploadImage_cloudinaryFails_throwsFileStorageException() throws Exception {
        MockMultipartFile validFile = new MockMultipartFile("file", "avatar.png", "image/png", new byte[]{1, 2, 3, 4});

        when(cloudinary.uploader()).thenReturn(uploader);
        when(uploader.upload(any(byte[].class), anyMap())).thenThrow(new RuntimeException("Cloudinary API unavailable"));

        assertThrows(FileStorageException.class, () ->
                cloudinaryService.uploadImage(validFile, "avatars")
        );
    }

    @Test
    void deleteImage_withBlankPublicId_throwsInvalidFileException() {
        assertThrows(InvalidFileException.class, () ->
                cloudinaryService.deleteImage("   ")
        );
    }

    @Test
    void deleteImage_success_callsDestroy() throws Exception {
        when(cloudinary.uploader()).thenReturn(uploader);
        when(uploader.destroy(eq("sample-id"), anyMap())).thenReturn(Map.of("result", "ok"));

        assertDoesNotThrow(() -> cloudinaryService.deleteImage("sample-id"));
        verify(uploader).destroy(eq("sample-id"), anyMap());
    }
}
