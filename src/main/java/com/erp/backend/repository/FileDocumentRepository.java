package com.erp.backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.erp.backend.entity.AppUser;
import com.erp.backend.entity.Course;
import com.erp.backend.entity.FileDocument;

public interface FileDocumentRepository extends JpaRepository<FileDocument, Long> {

    List<FileDocument> findAllByUploaderOrderByUploadedAtDesc(AppUser uploader);

    List<FileDocument> findAllByCourseOrderByUploadedAtDesc(Course course);
}
