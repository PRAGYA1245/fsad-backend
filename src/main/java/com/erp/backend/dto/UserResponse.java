package com.erp.backend.dto;

import com.erp.backend.portal.Role;

public record UserResponse(
        Long id,
        String username,
        String email,
        String fullName,
        Role role,
        boolean emailVerified,
        String departmentName,
        String profileId,
        String primaryInfo
) {
}
