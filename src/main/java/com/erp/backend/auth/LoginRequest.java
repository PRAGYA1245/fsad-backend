package com.erp.backend.auth;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "User ID/email is required.")
        String userIdOrEmail,
        @NotBlank(message = "Password is required.")
        String password
) {
}
