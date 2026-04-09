package com.erp.backend.auth;

import jakarta.validation.constraints.NotBlank;

public record ForgotPasswordRequest(
        @NotBlank(message = "User ID/email is required.")
        String userIdOrEmail
) {
}
