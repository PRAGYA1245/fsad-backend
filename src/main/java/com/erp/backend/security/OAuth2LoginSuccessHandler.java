package com.erp.backend.security;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import com.erp.backend.config.ApplicationProperties;
import com.erp.backend.entity.AppUser;
import com.erp.backend.service.UserService;

@Component
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final UserService userService;
    private final JwtService jwtService;
    private final ApplicationProperties applicationProperties;

    public OAuth2LoginSuccessHandler(
            UserService userService,
            JwtService jwtService,
            ApplicationProperties applicationProperties
    ) {
        this.userService = userService;
        this.jwtService = jwtService;
        this.applicationProperties = applicationProperties;
    }

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException, ServletException {
        OAuth2User oauth2User = (OAuth2User) authentication.getPrincipal();
        AppUser user = userService.upsertOAuthUser(oauth2User);
        String jwt = jwtService.generateToken(user);

        String redirectUrl = applicationProperties.getFrontendBaseUrl()
                + "/?token=" + URLEncoder.encode(jwt, StandardCharsets.UTF_8)
                + "&route=" + URLEncoder.encode(user.getRole().getRoute(), StandardCharsets.UTF_8)
                + "&oauth=true";

        response.sendRedirect(redirectUrl);
    }
}
