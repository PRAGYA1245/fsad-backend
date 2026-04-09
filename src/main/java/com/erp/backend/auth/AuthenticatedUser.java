package com.erp.backend.auth;

import com.erp.backend.portal.Role;

public record AuthenticatedUser(
        String token,
        String userId,
        Role role,
        String route,
        String userName,
        String userMeta
) {
}
