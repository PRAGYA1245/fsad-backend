package com.erp.backend.common;

import java.time.Instant;

public record ApiErrorResponse(
        Instant timestamp,
        int status,
        String message
) {
}
