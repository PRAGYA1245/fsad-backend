package com.erp.backend.service;

import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import com.erp.backend.common.ApiException;
import com.erp.backend.entity.AppUser;
import com.erp.backend.portal.Role;
import com.erp.backend.repository.AppUserRepository;

@Service
public class CurrentUserService {

    private final AppUserRepository appUserRepository;

    public CurrentUserService(AppUserRepository appUserRepository) {
        this.appUserRepository = appUserRepository;
    }

    public AppUser getCurrentUser(Jwt jwt) {
        if (jwt == null || jwt.getSubject() == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Missing authenticated user context.");
        }

        return appUserRepository.findByEmailIgnoreCase(jwt.getSubject())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Authenticated user no longer exists."));
    }

    public AppUser requireRole(Jwt jwt, Role role) {
        AppUser user = getCurrentUser(jwt);
        if (user.getRole() != role) {
            throw new ApiException(HttpStatus.FORBIDDEN, "You do not have access to this portal.");
        }
        return user;
    }
}
