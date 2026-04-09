package com.erp.backend.dto;

import com.erp.backend.entity.EnrollmentStatus;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record EnrollmentUpdateRequest(
        @NotNull(message = "Enrollment status is required.")
        EnrollmentStatus status,
        @DecimalMin(value = "0.0", message = "Attendance cannot be negative.")
        @DecimalMax(value = "100.0", message = "Attendance cannot exceed 100.")
        Double attendancePercentage,
        @Size(max = 20, message = "Grade must not exceed 20 characters.")
        String grade
) {
}
