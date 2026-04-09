package com.erp.backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.erp.backend.entity.AppUser;
import com.erp.backend.entity.Course;
import com.erp.backend.entity.Enrollment;

public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {

    List<Enrollment> findAllByStudentOrderByEnrolledAtDesc(AppUser student);

    List<Enrollment> findAllByCourseOrderByEnrolledAtDesc(Course course);

    Optional<Enrollment> findByStudentAndCourse(AppUser student, Course course);
}
