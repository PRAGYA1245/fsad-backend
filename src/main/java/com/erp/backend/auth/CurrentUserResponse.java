package com.erp.backend.auth;

import com.erp.backend.portal.Role;

public record CurrentUserResponse(
        Long id,
        String username,
        String email,
        String fullName,
        Role role,
        boolean emailVerified,
        String route
) {
}
