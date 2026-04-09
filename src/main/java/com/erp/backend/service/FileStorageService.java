package com.erp.backend.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import com.erp.backend.common.ApiException;
import com.erp.backend.config.ApplicationProperties;
import com.erp.backend.dto.FileUploadResponse;
import com.erp.backend.entity.AppUser;
import com.erp.backend.entity.Course;
import com.erp.backend.entity.FileDocument;
import com.erp.backend.repository.FileDocumentRepository;

@Service
public class FileStorageService {

    private static final Logger log = LoggerFactory.getLogger(FileStorageService.class);

    private final FileDocumentRepository fileDocumentRepository;
    private final CurrentUserService currentUserService;
    private final CourseService courseService;
    private final Path uploadPath;

    public FileStorageService(
            FileDocumentRepository fileDocumentRepository,
            CurrentUserService currentUserService,
            CourseService courseService,
            ApplicationProperties applicationProperties
    ) {
        this.fileDocumentRepository = fileDocumentRepository;
        this.currentUserService = currentUserService;
        this.courseService = courseService;
        this.uploadPath = Path.of(applicationProperties.getFile().getUploadDir()).toAbsolutePath().normalize();

        try {
            Files.createDirectories(uploadPath);
        } catch (IOException exception) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not initialize upload storage.");
        }
    }

    public FileUploadResponse upload(Jwt jwt, MultipartFile file, Long courseId) {
        if (file == null || file.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Please choose a file to upload.");
        }

        AppUser uploader = currentUserService.getCurrentUser(jwt);
        Course course = courseId != null ? courseService.findCourseEntity(courseId) : null;

        String originalFileName = file.getOriginalFilename() != null
                ? StringUtils.cleanPath(file.getOriginalFilename())
                : "uploaded-file";
        if (originalFileName.contains("..")) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid file name.");
        }
        String storedFileName = UUID.randomUUID() + "-" + originalFileName;
        Path destination = uploadPath.resolve(storedFileName);

        try {
            Files.copy(file.getInputStream(), destination, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException exception) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to store uploaded file.");
        }

        FileDocument document = new FileDocument();
        document.setOriginalFileName(originalFileName);
        document.setStoredFileName(storedFileName);
        document.setContentType(file.getContentType() != null ? file.getContentType() : "application/octet-stream");
        document.setSize(file.getSize());
        document.setStoragePath(destination.toString());
        document.setUploader(uploader);
        document.setCourse(course);

        FileDocument savedDocument = fileDocumentRepository.save(document);
        log.info("Uploaded file {} by {}", originalFileName, uploader.getUsername());
        return toResponse(savedDocument);
    }

    public List<FileUploadResponse> listFiles() {
        return fileDocumentRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    public Resource loadAsResource(Long id) {
        FileDocument document = fileDocumentRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "File not found."));

        try {
            Path path = Path.of(document.getStoragePath());
            Resource resource = new UrlResource(path.toUri());
            if (!resource.exists()) {
                throw new ApiException(HttpStatus.NOT_FOUND, "Stored file is missing.");
            }
            return resource;
        } catch (IOException exception) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to read stored file.");
        }
    }

    public FileDocument getMetadata(Long id) {
        return fileDocumentRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "File not found."));
    }

    public FileUploadResponse toResponse(FileDocument document) {
        return new FileUploadResponse(
                document.getId(),
                document.getOriginalFileName(),
                document.getContentType(),
                document.getSize(),
                document.getUploader().getFullName(),
                document.getCourse() != null ? document.getCourse().getCode() : null,
                document.getUploadedAt()
        );
    }
}
