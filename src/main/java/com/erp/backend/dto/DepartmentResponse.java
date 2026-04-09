package com.erp.backend.dto;

public record DepartmentResponse(
        Long id,
        String code,
        String name,
        String description,
        long userCount,
        long courseCount
) {
}
