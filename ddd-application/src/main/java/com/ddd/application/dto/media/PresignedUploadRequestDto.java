package com.ddd.application.dto.media;
import com.ddd.domain.enums.MediaCategoryEnum;

public record PresignedUploadRequestDto(
        MediaCategoryEnum category,
        String fileExtension
) {
}
