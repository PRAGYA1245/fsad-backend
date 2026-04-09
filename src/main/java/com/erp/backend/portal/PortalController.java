package com.erp.backend.portal;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.erp.backend.entity.AppUser;
import com.erp.backend.service.CurrentUserService;

@RestController
@RequestMapping("/api")
public class PortalController {

    private final PortalContentService portalContentService;
    private final CurrentUserService currentUserService;

    public PortalController(PortalContentService portalContentService, CurrentUserService currentUserService) {
        this.portalContentService = portalContentService;
        this.currentUserService = currentUserService;
    }

    @GetMapping("/admin/dashboard")
    public AdminDashboardResponse getAdminDashboard(@AuthenticationPrincipal Jwt jwt) {
        AppUser user = currentUserService.requireRole(jwt, Role.ADMIN);
        return portalContentService.getAdminDashboard(user);
    }

    @GetMapping("/teacher/dashboard")
    public TeacherDashboardResponse getTeacherDashboard(@AuthenticationPrincipal Jwt jwt) {
        AppUser user = currentUserService.requireRole(jwt, Role.TEACHER);
        return portalContentService.getTeacherDashboard(user);
    }

    @GetMapping("/student/dashboard")
    public StudentDashboardResponse getStudentDashboard(@AuthenticationPrincipal Jwt jwt) {
        AppUser user = currentUserService.requireRole(jwt, Role.STUDENT);
        return portalContentService.getStudentDashboard(user);
    }

    @GetMapping("/{role}/profile")
    public ProfileResponse getProfile(@PathVariable String role, @AuthenticationPrincipal Jwt jwt) {
        Role portalRole = Role.fromPath(role);
        AppUser user = currentUserService.requireRole(jwt, portalRole);
        return portalContentService.getProfile(portalRole, user);
    }

    @GetMapping("/{role}/sections/{slug}")
    public SectionResponse getSection(
            @PathVariable String role,
            @PathVariable String slug,
            @AuthenticationPrincipal Jwt jwt
    ) {
        Role portalRole = Role.fromPath(role);
        AppUser user = currentUserService.requireRole(jwt, portalRole);
        return portalContentService.getSection(portalRole, slug, user);
    }
}
