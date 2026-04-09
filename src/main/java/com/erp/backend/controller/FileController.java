package com.erp.backend.controller;

import java.util.List;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.erp.backend.dto.FileUploadResponse;
import com.erp.backend.entity.FileDocument;
import com.erp.backend.service.FileStorageService;

@RestController
@RequestMapping("/api/files")
@Tag(name = "Files", description = "Multipart upload and file metadata APIs")
public class FileController {

    private final FileStorageService fileStorageService;

    public FileController(FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
    }

    @PostMapping("/upload")
    @Operation(summary = "Upload a file using multipart/form-data")
    public FileUploadResponse uploadFile(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "courseId", required = false) Long courseId
    ) {
        return fileStorageService.upload(jwt, file, courseId);
    }

    @GetMapping
    @Operation(summary = "List uploaded file metadata")
    public List<FileUploadResponse> listFiles() {
        return fileStorageService.listFiles();
    }

    @GetMapping("/{id}/download")
    @Operation(summary = "Download a stored file")
    public ResponseEntity<Resource> downloadFile(@PathVariable Long id) {
        FileDocument metadata = fileStorageService.getMetadata(id);
        Resource resource = fileStorageService.loadAsResource(id);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(metadata.getContentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(metadata.getOriginalFileName())
                        .build()
                        .toString())
                .body(resource);
    }
}
