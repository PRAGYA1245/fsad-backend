package com.erp.backend.auth;

public record LoginResponse(
        String token,
        String role,
        String route,
        String userName,
        String userMeta,
        boolean emailVerified
) {
}
