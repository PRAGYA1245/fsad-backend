package com.erp.backend.service;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.erp.backend.common.ApiException;
import com.erp.backend.dto.UserRequest;
import com.erp.backend.dto.UserResponse;
import com.erp.backend.entity.AppUser;
import com.erp.backend.entity.StudentProfile;
import com.erp.backend.entity.TeacherProfile;
import com.erp.backend.portal.Role;
import com.erp.backend.repository.AppUserRepository;

@Service
@Transactional
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final AppUserRepository appUserRepository;
    private final DepartmentService departmentService;
    private final PasswordEncoder passwordEncoder;

    public UserService(
            AppUserRepository appUserRepository,
            DepartmentService departmentService,
            PasswordEncoder passwordEncoder
    ) {
        this.appUserRepository = appUserRepository;
        this.departmentService = departmentService;
        this.passwordEncoder = passwordEncoder;
    }

    public List<UserResponse> getAllUsers() {
        return appUserRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    public AppUser getUserEntity(Long id) {
        return appUserRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found."));
    }

    public Optional<AppUser> findByIdentifier(String identifier) {
        return appUserRepository.findByUsernameIgnoreCaseOrEmailIgnoreCase(identifier, identifier);
    }

    public AppUser getByEmail(String email) {
        return appUserRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found."));
    }

    public UserResponse getUser(Long id) {
        return toResponse(getUserEntity(id));
    }

    public UserResponse createUser(UserRequest request) {
        validateUniqueUser(request.username(), request.email(), null);
        if (request.password() == null || request.password().isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Password is required.");
        }

        AppUser user = new AppUser();
        applyCommonUserFields(user, request, true);
        AppUser savedUser = appUserRepository.save(user);
        log.info("Created user {} with role {}", savedUser.getUsername(), savedUser.getRole());
        return toResponse(savedUser);
    }

    public UserResponse updateUser(Long id, UserRequest request) {
        AppUser user = getUserEntity(id);
        validateUniqueUser(request.username(), request.email(), id);
        applyCommonUserFields(user, request, false);
        AppUser savedUser = appUserRepository.save(user);
        log.info("Updated user {} with role {}", savedUser.getUsername(), savedUser.getRole());
        return toResponse(savedUser);
    }

    public void deleteUser(Long id) {
        AppUser user = getUserEntity(id);
        appUserRepository.delete(user);
        log.info("Deleted user {}", user.getUsername());
    }

    public AppUser createUserFromRegistration(String username, String fullName, String email, String password, Role role, Long departmentId) {
        validateUniqueUser(username, email, null);
        AppUser user = new AppUser();
        UserRequest request = new UserRequest(
                username,
                email,
                fullName,
                password,
                role,
                departmentId,
                role == Role.STUDENT ? "STU-" + UUID.randomUUID().toString().substring(0, 8) : "EMP-" + UUID.randomUUID().toString().substring(0, 8),
                role == Role.STUDENT ? "B.Tech" : "Faculty",
                role == Role.STUDENT ? 1 : 1,
                role == Role.STUDENT ? 8.0 : 0.0,
                null
        );
        applyCommonUserFields(user, request, true);
        user.setEmailVerified(false);
        return appUserRepository.save(user);
    }

    public AppUser upsertOAuthUser(OAuth2User oauth2User) {
        String email = Objects.toString(oauth2User.getAttribute("email"), null);
        if (email == null || email.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "OAuth provider did not return an email.");
        }

        return appUserRepository.findByEmailIgnoreCase(email)
                .map(existing -> {
                    existing.setOauthAccount(true);
                    existing.setAuthProvider("OAUTH2");
                    existing.setEmailVerified(true);
                    return appUserRepository.save(existing);
                })
                .orElseGet(() -> {
                    AppUser user = new AppUser();
                    user.setUsername(email.split("@")[0]);
                    user.setEmail(email);
                    user.setFullName(Objects.toString(oauth2User.getAttribute("name"), user.getUsername()));
                    user.setPasswordHash(passwordEncoder.encode(UUID.randomUUID().toString()));
                    user.setRole(Role.STUDENT);
                    user.setEmailVerified(true);
                    user.setOauthAccount(true);
                    user.setAuthProvider("OAUTH2");

                    StudentProfile profile = new StudentProfile();
                    profile.setStudentId("SSO-" + UUID.randomUUID().toString().substring(0, 8));
                    profile.setProgram("OAuth Imported");
                    profile.setSemester(1);
                    profile.setCgpa(0.0);
                    profile.setAttendancePercentage(0.0);
                    profile.setUser(user);
                    user.setStudentProfile(profile);

                    AppUser savedUser = appUserRepository.save(user);
                    log.info("Provisioned OAuth user {}", savedUser.getEmail());
                    return savedUser;
                });
    }

    public UserResponse toResponse(AppUser user) {
        String profileId = null;
        String primaryInfo = user.getRole().getLabel();

        if (user.getStudentProfile() != null) {
            profileId = user.getStudentProfile().getStudentId();
            primaryInfo = user.getStudentProfile().getProgram() + " | Semester " + user.getStudentProfile().getSemester();
        } else if (user.getTeacherProfile() != null) {
            profileId = user.getTeacherProfile().getEmployeeId();
            primaryInfo = user.getTeacherProfile().getDesignation() + " | " + user.getTeacherProfile().getExperienceYears() + " yrs";
        }

        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getFullName(),
                user.getRole(),
                user.isEmailVerified(),
                user.getDepartment() != null ? user.getDepartment().getName() : null,
                profileId,
                primaryInfo
        );
    }

    private void applyCommonUserFields(AppUser user, UserRequest request, boolean encodePassword) {
        user.setUsername(request.username().trim());
        user.setEmail(request.email().trim().toLowerCase());
        user.setFullName(request.fullName().trim());
        user.setRole(request.role());
        user.setOauthAccount(false);
        user.setAuthProvider("LOCAL");
        user.setEnabled(true);

        if (request.departmentId() != null) {
            user.setDepartment(departmentService.findDepartmentEntity(request.departmentId()));
        } else {
            user.setDepartment(null);
        }

        if (encodePassword || (request.password() != null && !request.password().isBlank())) {
            user.setPasswordHash(passwordEncoder.encode(request.password()));
        }

        if (request.role() == Role.STUDENT) {
            StudentProfile profile = user.getStudentProfile();
            if (profile == null) {
                profile = new StudentProfile();
            }
            profile.setStudentId(defaultIfBlank(request.profileId(), "STU-AUTO-" + UUID.randomUUID().toString().substring(0, 6)));
            profile.setProgram(defaultIfBlank(request.designationOrProgram(), "B.Tech"));
            profile.setSemester(request.semesterOrExperience() != null ? request.semesterOrExperience() : 1);
            profile.setCgpa(request.cgpaOrAttendance() != null ? request.cgpaOrAttendance() : 0.0);
            profile.setAttendancePercentage(0.0);
            profile.setPhone(request.phone());
            profile.setUser(user);
            user.setStudentProfile(profile);
            user.setTeacherProfile(null);
        } else if (request.role() == Role.TEACHER || request.role() == Role.ADMIN) {
            TeacherProfile profile = user.getTeacherProfile();
            if (profile == null) {
                profile = new TeacherProfile();
            }
            profile.setEmployeeId(defaultIfBlank(request.profileId(), "EMP-AUTO-" + UUID.randomUUID().toString().substring(0, 6)));
            profile.setDesignation(defaultIfBlank(request.designationOrProgram(), request.role() == Role.ADMIN ? "Administrator" : "Faculty"));
            profile.setExperienceYears(request.semesterOrExperience() != null ? request.semesterOrExperience() : 1);
            profile.setPhone(request.phone());
            profile.setSpecialization(request.role() == Role.ADMIN ? "Operations" : "Teaching");
            profile.setOfficeHours("Mon-Fri, 10:00 AM - 4:00 PM");
            profile.setUser(user);
            user.setTeacherProfile(profile);
            user.setStudentProfile(null);
        }
    }

    private void validateUniqueUser(String username, String email, Long currentUserId) {
        appUserRepository.findByUsernameIgnoreCase(username)
                .filter(existing -> !existing.getId().equals(currentUserId))
                .ifPresent(existing -> {
                    throw new ApiException(HttpStatus.CONFLICT, "Username already exists.");
                });

        appUserRepository.findByEmailIgnoreCase(email)
                .filter(existing -> !existing.getId().equals(currentUserId))
                .ifPresent(existing -> {
                    throw new ApiException(HttpStatus.CONFLICT, "Email already exists.");
                });
    }

    private String defaultIfBlank(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }
}
