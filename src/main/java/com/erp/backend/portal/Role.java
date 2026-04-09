package com.erp.backend.portal;

import org.springframework.http.HttpStatus;

import com.erp.backend.common.ApiException;

public enum Role {
    ADMIN("/admin", "Administrator"),
    TEACHER("/teacher", "Teacher"),
    STUDENT("/student", "Student");

    private final String route;
    private final String label;

    Role(String route, String label) {
        this.route = route;
        this.label = label;
    }

    public String getRoute() {
        return route;
    }

    public String getLabel() {
        return label;
    }

    public String getAuthority() {
        return "ROLE_" + name();
    }

    public static Role fromPath(String value) {
        try {
            return Role.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Unsupported portal role.");
        }
    }
}
