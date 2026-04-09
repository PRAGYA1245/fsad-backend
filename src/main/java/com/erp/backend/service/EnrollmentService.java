package com.erp.backend.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.erp.backend.common.ApiException;
import com.erp.backend.dto.EnrollmentRequest;
import com.erp.backend.dto.EnrollmentResponse;
import com.erp.backend.dto.EnrollmentUpdateRequest;
import com.erp.backend.entity.AppUser;
import com.erp.backend.entity.Course;
import com.erp.backend.entity.Enrollment;
import com.erp.backend.portal.Role;
import com.erp.backend.repository.EnrollmentRepository;

@Service
@Transactional
public class EnrollmentService {

    private static final Logger log = LoggerFactory.getLogger(EnrollmentService.class);

    private final EnrollmentRepository enrollmentRepository;
    private final CourseService courseService;
    private final CurrentUserService currentUserService;
    private final UserService userService;

    public EnrollmentService(
            EnrollmentRepository enrollmentRepository,
            CourseService courseService,
            CurrentUserService currentUserService,
            UserService userService
    ) {
        this.enrollmentRepository = enrollmentRepository;
        this.courseService = courseService;
        this.currentUserService = currentUserService;
        this.userService = userService;
    }

    public List<EnrollmentResponse> getMyEnrollments(Jwt jwt) {
        AppUser currentUser = currentUserService.getCurrentUser(jwt);
        if (currentUser.getRole() == Role.STUDENT) {
            return enrollmentRepository.findAllByStudentOrderByEnrolledAtDesc(currentUser).stream()
                    .map(this::toResponse)
                    .toList();
        }

        return enrollmentRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    public EnrollmentResponse enroll(Jwt jwt, EnrollmentRequest request) {
        AppUser currentUser = currentUserService.getCurrentUser(jwt);
        AppUser student = request.studentId() != null && currentUser.getRole() == Role.ADMIN
                ? userService.getUserEntity(request.studentId())
                : currentUser;

        if (student.getRole() != Role.STUDENT) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Enrollment can only be created for student users.");
        }

        Course course = courseService.findCourseEntity(request.courseId());
        enrollmentRepository.findByStudentAndCourse(student, course)
                .ifPresent(existing -> {
                    throw new ApiException(HttpStatus.CONFLICT, "Student is already enrolled in this course.");
                });

        Enrollment enrollment = new Enrollment();
        enrollment.setStudent(student);
        enrollment.setCourse(course);

        Enrollment savedEnrollment = enrollmentRepository.save(enrollment);
        log.info("Created enrollment for student {} in course {}", student.getUsername(), course.getCode());
        return toResponse(savedEnrollment);
    }

    public EnrollmentResponse updateEnrollment(Long id, EnrollmentUpdateRequest request) {
        Enrollment enrollment = enrollmentRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Enrollment not found."));

        enrollment.setStatus(request.status());
        if (request.attendancePercentage() != null) {
            enrollment.setAttendancePercentage(request.attendancePercentage());
        }
        if (request.grade() != null) {
            enrollment.setGrade(request.grade());
        }

        Enrollment savedEnrollment = enrollmentRepository.save(enrollment);
        log.info("Updated enrollment {}", savedEnrollment.getId());
        return toResponse(savedEnrollment);
    }

    public EnrollmentResponse toResponse(Enrollment enrollment) {
        return new EnrollmentResponse(
                enrollment.getId(),
                enrollment.getStudent().getId(),
                enrollment.getStudent().getFullName(),
                enrollment.getCourse().getId(),
                enrollment.getCourse().getCode(),
                enrollment.getCourse().getTitle(),
                enrollment.getStatus(),
                enrollment.getAttendancePercentage(),
                enrollment.getGrade(),
                enrollment.getEnrolledAt()
        );
    }
}
