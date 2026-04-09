package com.erp.backend.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CourseRequest(
        @NotBlank(message = "Course code is required.")
        @Size(max = 30, message = "Course code must not exceed 30 characters.")
        String code,
        @NotBlank(message = "Course title is required.")
        @Size(max = 120, message = "Course title must not exceed 120 characters.")
        String title,
        @Size(max = 500, message = "Description must not exceed 500 characters.")
        String description,
        @NotNull(message = "Credits are required.")
        @Min(value = 1, message = "Credits must be at least 1.")
        @Max(value = 10, message = "Credits must not exceed 10.")
        Integer credits,
        @NotNull(message = "Department is required.")
        Long departmentId,
        Long teacherId
) {
}
