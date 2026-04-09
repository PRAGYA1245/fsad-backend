package com.erp.backend.config;

import java.net.URI;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig {

    private final ApplicationProperties applicationProperties;

    public CorsConfig(ApplicationProperties applicationProperties) {
        this.applicationProperties = applicationProperties;
    }

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        String configuredFrontendOrigin = extractOrigin(applicationProperties.getFrontendBaseUrl());

        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/api/**")
                        .allowedOriginPatterns(
                                configuredFrontendOrigin,
                                "http://localhost:*",
                                "http://127.0.0.1:*"
                        )
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
                        .allowedHeaders("*")
                        .exposedHeaders("Authorization");
            }
        };
    }

    private String extractOrigin(String frontendBaseUrl) {
        try {
            URI uri = URI.create(frontendBaseUrl);
            String scheme = uri.getScheme() == null ? "http" : uri.getScheme();
            String host = uri.getHost();

            if (host == null || host.isBlank()) {
                return "http://localhost:*";
            }

            if (uri.getPort() > 0) {
                return scheme + "://" + host + ":" + uri.getPort();
            }

            return scheme + "://" + host;
        } catch (IllegalArgumentException exception) {
            return "http://localhost:*";
        }
    }
}
