package com.erp.backend.service;

import java.time.LocalDateTime;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.erp.backend.common.ApiException;
import com.erp.backend.entity.AppUser;
import com.erp.backend.entity.EmailVerificationToken;
import com.erp.backend.repository.AppUserRepository;
import com.erp.backend.repository.EmailVerificationTokenRepository;

@Service
@Transactional
public class VerificationService {

    private static final Logger log = LoggerFactory.getLogger(VerificationService.class);

    private final EmailVerificationTokenRepository tokenRepository;
    private final AppUserRepository appUserRepository;
    private final MailService mailService;

    public VerificationService(
            EmailVerificationTokenRepository tokenRepository,
            AppUserRepository appUserRepository,
            MailService mailService
    ) {
        this.tokenRepository = tokenRepository;
        this.appUserRepository = appUserRepository;
        this.mailService = mailService;
    }

    public String createAndSendVerification(AppUser user) {
        EmailVerificationToken token = new EmailVerificationToken();
        token.setToken(UUID.randomUUID().toString());
        token.setExpiresAt(LocalDateTime.now().plusHours(24));
        token.setUser(user);
        EmailVerificationToken savedToken = tokenRepository.save(token);

        String verificationMessage = "Verify your ERP account using this token: " + savedToken.getToken();
        mailService.sendEmail(user.getEmail(), "Verify your ERP account", verificationMessage);
        log.info("Created email verification token for {}", user.getEmail());
        return savedToken.getToken();
    }

    public void verifyEmail(String tokenValue) {
        EmailVerificationToken token = tokenRepository.findByToken(tokenValue)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Verification token not found."));

        if (token.isAlreadyUsed()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Verification token has already been used.");
        }
        if (token.isExpired()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Verification token has expired.");
        }

        token.setVerifiedAt(LocalDateTime.now());
        AppUser user = token.getUser();
        user.setEmailVerified(true);
        appUserRepository.save(user);
        tokenRepository.save(token);
        log.info("Verified email for {}", user.getEmail());
    }

    public void resendVerification(String email) {
        AppUser user = appUserRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found."));
        if (user.isEmailVerified()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Email is already verified.");
        }
        createAndSendVerification(user);
    }
}
