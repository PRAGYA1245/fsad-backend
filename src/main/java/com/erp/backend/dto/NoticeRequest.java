package com.erp.backend.dto;

import com.erp.backend.entity.NoticeLevel;
import com.erp.backend.portal.Role;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record NoticeRequest(
        @NotBlank(message = "Notice title is required.")
        @Size(max = 150, message = "Title must not exceed 150 characters.")
        String title,
        @NotBlank(message = "Notice message is required.")
        @Size(max = 1000, message = "Message must not exceed 1000 characters.")
        String message,
        @NotNull(message = "Notice level is required.")
        NoticeLevel level,
        Role audienceRole
) {
}
