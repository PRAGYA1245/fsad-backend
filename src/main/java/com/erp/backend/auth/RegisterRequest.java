package com.erp.backend.auth;

import com.erp.backend.portal.Role;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "Username is required.")
        @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters.")
        String username,
        @NotBlank(message = "Full name is required.")
        @Size(max = 120, message = "Full name must not exceed 120 characters.")
        String fullName,
        @Email(message = "Email must be valid.")
        @NotBlank(message = "Email is required.")
        String email,
        @NotBlank(message = "Password is required.")
        @Size(min = 6, max = 100, message = "Password must be between 6 and 100 characters.")
        String password,
        @NotNull(message = "Role is required.")
        Role role,
        Long departmentId
) {
}
