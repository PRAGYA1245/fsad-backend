package com.erp.backend.dto;

import jakarta.validation.constraints.NotNull;

public record EnrollmentRequest(
        @NotNull(message = "Course ID is required.")
        Long courseId,
        Long studentId
) {
}
