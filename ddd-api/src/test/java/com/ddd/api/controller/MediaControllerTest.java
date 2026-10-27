package com.ddd.api.controller;

import com.ddd.api.dto.media.res.MediaResponseDto;
import com.ddd.api.mapper.MediaApiMapper;
import com.ddd.application.dto.media.MediaInfoDto;
import com.ddd.application.enums.MediaCategory;
import com.ddd.application.service.media.MediaUploadService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class MediaControllerTest {

    private MockMvc mockMvc;

    @Mock
    private MediaUploadService mediaUploadService;

    @Mock
    private MediaApiMapper mediaApiMapper;

    @InjectMocks
    private MediaController mediaController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(mediaController).build();
    }

    @Test
    void uploadImage_success_returns200AndData() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "test.jpg", "image/jpeg", new byte[]{1, 2, 3});
        MediaInfoDto mediaInfo = new MediaInfoDto("id1", "url", "secure_url", "jpg", "image", 100L, 200, 200, "now");
        MediaResponseDto responseDto = new MediaResponseDto("id1", "url", "secure_url", "jpg", "image", 100L, 200, 200, "now");

        when(mediaUploadService.uploadImage(any(), eq(MediaCategory.AVATAR))).thenReturn(mediaInfo);
        when(mediaApiMapper.toResponseDto(mediaInfo)).thenReturn(responseDto);

        mockMvc.perform(multipart("/media/upload")
                        .file(file)
                        .param("category", "AVATAR")
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Success"))
                .andExpect(jsonPath("$.data.publicId").value("id1"))
                .andExpect(jsonPath("$.data.secureUrl").value("secure_url"));
    }

    @Test
    void uploadImages_multiple_returns200AndList() throws Exception {
        MockMultipartFile file1 = new MockMultipartFile("files", "test1.jpg", "image/jpeg", new byte[]{1});
        MockMultipartFile file2 = new MockMultipartFile("files", "test2.jpg", "image/jpeg", new byte[]{2});
        MediaInfoDto info1 = new MediaInfoDto("id1", "url1", "secure1", "jpg", "image", 100L, 200, 200, "now");
        MediaResponseDto res1 = new MediaResponseDto("id1", "url1", "secure1", "jpg", "image", 100L, 200, 200, "now");

        when(mediaUploadService.uploadImages(any(), eq(MediaCategory.EVENT))).thenReturn(List.of(info1));
        when(mediaApiMapper.toResponseDto(info1)).thenReturn(res1);

        mockMvc.perform(multipart("/media/upload/multiple")
                        .file(file1)
                        .file(file2)
                        .param("category", "EVENT")
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Success"))
                .andExpect(jsonPath("$.data[0].publicId").value("id1"));
    }

    @Test
    void deleteImage_success_returns200() throws Exception {
        doNothing().when(mediaUploadService).deleteImage("event-management/avatars/sample");

        mockMvc.perform(delete("/media")
                        .param("publicId", "event-management/avatars/sample"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Success"));

        verify(mediaUploadService).deleteImage("event-management/avatars/sample");
    }
}
