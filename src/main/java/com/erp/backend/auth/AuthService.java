package com.erp.backend.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.erp.backend.common.ApiException;
import com.erp.backend.common.ApiMessageResponse;
import com.erp.backend.entity.AppUser;
import com.erp.backend.portal.Role;
import com.erp.backend.repository.AppUserRepository;
import com.erp.backend.security.JwtService;
import com.erp.backend.service.CurrentUserService;
import com.erp.backend.service.UserService;
import com.erp.backend.service.VerificationService;

@Service
@Transactional
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserService userService;
    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final VerificationService verificationService;
    private final CurrentUserService currentUserService;

    public AuthService(
            UserService userService,
            AppUserRepository appUserRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            VerificationService verificationService,
            CurrentUserService currentUserService
    ) {
        this.userService = userService;
        this.appUserRepository = appUserRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.verificationService = verificationService;
        this.currentUserService = currentUserService;
    }

    public LoginResponse login(LoginRequest request) {
        String rawPassword = request.password().trim();

        AppUser user = userService.findByIdentifier(request.userIdOrEmail().trim())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Invalid credentials."));

        if (!isPasswordValid(user, rawPassword)) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid credentials.");
        }

        if (!user.isEnabled()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "User account is disabled.");
        }

        String token = jwtService.generateToken(user);
        log.info("User {} authenticated successfully", user.getUsername());
        return new LoginResponse(
                token,
                user.getRole().name(),
                user.getRole().getRoute(),
                user.getFullName(),
                buildUserMeta(user),
                user.isEmailVerified()
        );
    }

    private boolean isPasswordValid(AppUser user, String rawPassword) {
        String storedPassword = user.getPasswordHash();

        if (storedPassword == null || storedPassword.isBlank()) {
            return false;
        }

        if (looksLikeBcryptHash(storedPassword)) {
            try {
                if (passwordEncoder.matches(rawPassword, storedPassword)) {
                    return true;
                }
            } catch (IllegalArgumentException exception) {
                log.warn("Stored password hash for user {} is not a valid BCrypt value", user.getUsername());
            }
        }

        if (rawPassword.equals(storedPassword)) {
            user.setPasswordHash(passwordEncoder.encode(rawPassword));
            appUserRepository.save(user);
            log.info("Migrated legacy password storage for {}", user.getUsername());
            return true;
        }

        return false;
    }

    private boolean looksLikeBcryptHash(String passwordHash) {
        return passwordHash.startsWith("$2a$")
                || passwordHash.startsWith("$2b$")
                || passwordHash.startsWith("$2y$");
    }

    public ApiMessageResponse register(RegisterRequest request) {
        if (request.role() == Role.ADMIN) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Administrator accounts cannot be self-registered.");
        }

        AppUser user = userService.createUserFromRegistration(
                request.username(),
                request.fullName(),
                request.email(),
                request.password(),
                request.role(),
                request.departmentId()
        );
        String verificationToken = verificationService.createAndSendVerification(user);
        log.info("Registered new user {}", user.getUsername());
        return new ApiMessageResponse("Registration successful. Verification token: " + verificationToken);
    }

    public ApiMessageResponse forgotPassword(ForgotPasswordRequest request) {
        AppUser user = userService.findByIdentifier(request.userIdOrEmail().trim())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found."));
        log.info("Password reset requested for {}", user.getUsername());
        return new ApiMessageResponse("Password reset flow placeholder created for " + user.getEmail() + ".");
    }

    public ApiMessageResponse verifyEmail(VerifyEmailRequest request) {
        verificationService.verifyEmail(request.token());
        return new ApiMessageResponse("Email verified successfully.");
    }

    public ApiMessageResponse resendVerification(ResendVerificationRequest request) {
        verificationService.resendVerification(request.email());
        return new ApiMessageResponse("Verification email sent.");
    }

    public CurrentUserResponse currentUser(Jwt jwt) {
        AppUser user = currentUserService.getCurrentUser(jwt);
        return new CurrentUserResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getFullName(),
                user.getRole(),
                user.isEmailVerified(),
                user.getRole().getRoute()
        );
    }

    private String buildUserMeta(AppUser user) {
        if (user.getStudentProfile() != null) {
            return user.getStudentProfile().getProgram() + " | Semester " + user.getStudentProfile().getSemester();
        }
        if (user.getTeacherProfile() != null) {
            return user.getTeacherProfile().getDesignation();
        }
        return user.getRole().getLabel();
    }
}
