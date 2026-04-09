package com.erp.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DepartmentRequest(
        @NotBlank(message = "Department code is required.")
        @Size(max = 30, message = "Department code must not exceed 30 characters.")
        String code,
        @NotBlank(message = "Department name is required.")
        @Size(max = 100, message = "Department name must not exceed 100 characters.")
        String name,
        @Size(max = 500, message = "Description must not exceed 500 characters.")
        String description
) {
}
