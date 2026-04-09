package com.erp.backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.erp.backend.entity.AppUser;
import com.erp.backend.entity.Course;

public interface CourseRepository extends JpaRepository<Course, Long> {

    Optional<Course> findByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCase(String code);

    List<Course> findAllByTeacherOrderByCodeAsc(AppUser teacher);
}
