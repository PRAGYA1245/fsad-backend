package com.erp.backend.dto;

import java.time.LocalDateTime;

import com.erp.backend.entity.NoticeLevel;
import com.erp.backend.portal.Role;

public record NoticeResponse(
        Long id,
        String title,
        String message,
        NoticeLevel level,
        Role audienceRole,
        String authorName,
        LocalDateTime createdAt
) {
}
