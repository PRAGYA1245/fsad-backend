package com.erp.backend.dto;

import java.time.LocalDateTime;

public record FileUploadResponse(
        Long id,
        String originalFileName,
        String contentType,
        long size,
        String uploader,
        String courseCode,
        LocalDateTime uploadedAt
) {
}
