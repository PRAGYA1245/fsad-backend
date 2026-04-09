package com.erp.backend.controller;

import java.util.List;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.erp.backend.dto.NoticeRequest;
import com.erp.backend.dto.NoticeResponse;
import com.erp.backend.service.NoticeService;

@RestController
@RequestMapping("/api/notices")
@Tag(name = "Notices", description = "Announcements and broadcast APIs")
public class NoticeController {

    private final NoticeService noticeService;

    public NoticeController(NoticeService noticeService) {
        this.noticeService = noticeService;
    }

    @GetMapping
    @Operation(summary = "List notices visible to the current role")
    public List<NoticeResponse> getNotices(@AuthenticationPrincipal Jwt jwt) {
        return noticeService.getVisibleNotices(jwt);
    }

    @PostMapping
    @Operation(summary = "Create a notice")
    public NoticeResponse createNotice(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody NoticeRequest request) {
        return noticeService.createNotice(jwt, request);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a notice")
    public NoticeResponse updateNotice(@PathVariable Long id, @Valid @RequestBody NoticeRequest request) {
        return noticeService.updateNotice(id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a notice")
    public void deleteNotice(@PathVariable Long id) {
        noticeService.deleteNotice(id);
    }
}
