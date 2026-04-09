package com.erp.backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class SwaggerUiWebConfig implements WebMvcConfigurer {

    private static final String SWAGGER_UI_WEBJAR_LOCATION =
            "classpath:/META-INF/resources/webjars/swagger-ui/5.32.0/";

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/swagger-ui/**")
                .addResourceLocations(
                        "classpath:/static/swagger-ui/",
                        SWAGGER_UI_WEBJAR_LOCATION
                );
    }
}
