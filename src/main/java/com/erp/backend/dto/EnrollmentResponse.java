package com.erp.backend.dto;

import java.time.LocalDateTime;

import com.erp.backend.entity.EnrollmentStatus;

public record EnrollmentResponse(
        Long id,
        Long studentId,
        String studentName,
        Long courseId,
        String courseCode,
        String courseTitle,
        EnrollmentStatus status,
        double attendancePercentage,
        String grade,
        LocalDateTime enrolledAt
) {
}
