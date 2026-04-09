package com.erp.backend.controller;

import java.util.List;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.erp.backend.dto.EnrollmentRequest;
import com.erp.backend.dto.EnrollmentResponse;
import com.erp.backend.dto.EnrollmentUpdateRequest;
import com.erp.backend.service.EnrollmentService;

@RestController
@RequestMapping("/api/enrollments")
@Tag(name = "Enrollments", description = "Enrollment CRUD and tracking APIs")
public class EnrollmentController {

    private final EnrollmentService enrollmentService;

    public EnrollmentController(EnrollmentService enrollmentService) {
        this.enrollmentService = enrollmentService;
    }

    @GetMapping
    @Operation(summary = "List enrollments for the current user or all when privileged")
    public List<EnrollmentResponse> getEnrollments(@AuthenticationPrincipal Jwt jwt) {
        return enrollmentService.getMyEnrollments(jwt);
    }

    @PostMapping
    @Operation(summary = "Create a new enrollment")
    public EnrollmentResponse createEnrollment(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody EnrollmentRequest request
    ) {
        return enrollmentService.enroll(jwt, request);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update enrollment status, attendance, and grade")
    public EnrollmentResponse updateEnrollment(
            @PathVariable Long id,
            @Valid @RequestBody EnrollmentUpdateRequest request
    ) {
        return enrollmentService.updateEnrollment(id, request);
    }
}
