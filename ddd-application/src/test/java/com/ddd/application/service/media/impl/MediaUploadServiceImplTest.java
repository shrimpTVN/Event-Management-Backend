package com.ddd.application.service.media.impl;

import com.ddd.application.dto.media.MediaInfoDto;
import com.ddd.application.enums.MediaCategory;
import com.ddd.infrastructure.dto.storage.CloudinaryUploadResult;
import com.ddd.infrastructure.service.storage.CloudinaryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MediaUploadServiceImplTest {

    @Mock
    private CloudinaryService cloudinaryService;

    private MediaUploadServiceImpl mediaUploadService;

    @BeforeEach
    void setUp() {
        mediaUploadService = new MediaUploadServiceImpl(cloudinaryService);
    }

    @Test
    void uploadImage_withAvatarCategory_resolvesAvatarsFolder() {
        MockMultipartFile file = new MockMultipartFile("file", "avatar.jpg", "image/jpeg", new byte[]{1, 2});
        CloudinaryUploadResult mockResult = new CloudinaryUploadResult(
                "event-management/avatars/abc",
                "http://example.com/abc.jpg",
                "https://example.com/abc.jpg",
                "jpg",
                "image",
                1024L,
                200,
                200,
                "2026-10-22"
        );

        when(cloudinaryService.uploadImage(file, "avatars")).thenReturn(mockResult);

        MediaInfoDto result = mediaUploadService.uploadImage(file, MediaCategory.AVATAR);

        assertNotNull(result);
        assertEquals("event-management/avatars/abc", result.publicId());
        assertEquals("https://example.com/abc.jpg", result.secureUrl());
        verify(cloudinaryService).uploadImage(file, "avatars");
    }

    @Test
    void uploadImage_withNullCategory_defaultsToGeneralFolder() {
        MockMultipartFile file = new MockMultipartFile("file", "image.png", "image/png", new byte[]{1, 2});
        CloudinaryUploadResult mockResult = new CloudinaryUploadResult(
                "event-management/general/xyz",
                "http://example.com/xyz.png",
                "https://example.com/xyz.png",
                "png",
                "image",
                2048L,
                400,
                300,
                "2026-10-22"
        );

        when(cloudinaryService.uploadImage(file, "general")).thenReturn(mockResult);

        MediaInfoDto result = mediaUploadService.uploadImage(file, null);

        assertNotNull(result);
        assertEquals("event-management/general/xyz", result.publicId());
        verify(cloudinaryService).uploadImage(file, "general");
    }

    @Test
    void uploadImages_multipleFiles_returnsListOfMediaInfo() {
        MultipartFile file1 = new MockMultipartFile("files", "img1.png", "image/png", new byte[]{1});
        MultipartFile file2 = new MockMultipartFile("files", "img2.png", "image/png", new byte[]{2});
        List<MultipartFile> files = List.of(file1, file2);

        CloudinaryUploadResult res1 = new CloudinaryUploadResult("id1", "u1", "su1", "png", "image", 100, 10, 10, "2026-10-22");
        CloudinaryUploadResult res2 = new CloudinaryUploadResult("id2", "u2", "su2", "png", "image", 200, 20, 20, "2026-10-22");

        when(cloudinaryService.uploadImages(files, "events")).thenReturn(List.of(res1, res2));

        List<MediaInfoDto> results = mediaUploadService.uploadImages(files, MediaCategory.EVENT);

        assertEquals(2, results.size());
        assertEquals("id1", results.get(0).publicId());
        assertEquals("id2", results.get(1).publicId());
        verify(cloudinaryService).uploadImages(files, "events");
    }

    @Test
    void deleteImage_delegatesToCloudinaryService() {
        doNothing().when(cloudinaryService).deleteImage("event-management/avatars/abc");

        assertDoesNotThrow(() -> mediaUploadService.deleteImage("event-management/avatars/abc"));
        verify(cloudinaryService).deleteImage("event-management/avatars/abc");
    }
}
