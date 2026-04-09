package com.erp.backend.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SwaggerUiController {

    @GetMapping({"/swagger-ui", "/swagger-ui/", "/swagger-ui.html"})
    public String swaggerUi() {
        return "redirect:/swagger-ui/index.html";
    }
}
