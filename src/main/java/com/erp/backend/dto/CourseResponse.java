package com.erp.backend.dto;

public record CourseResponse(
        Long id,
        String code,
        String title,
        String description,
        int credits,
        String departmentName,
        String teacherName,
        long enrolledStudents
) {
}
