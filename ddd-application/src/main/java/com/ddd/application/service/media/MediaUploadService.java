package com.ddd.application.service.media;

import com.ddd.application.dto.media.MediaInfoDto;
import com.ddd.application.dto.media.PresignedUploadDto;
import com.ddd.application.dto.media.PresignedUploadRequestDto;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface MediaUploadService {
      PresignedUploadDto createPresignedUpload(PresignedUploadRequestDto appRequest);

    void deleteImage(String publicId);
}
