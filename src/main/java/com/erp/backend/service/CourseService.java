package com.erp.backend.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.erp.backend.common.ApiException;
import com.erp.backend.dto.CourseRequest;
import com.erp.backend.dto.CourseResponse;
import com.erp.backend.entity.AppUser;
import com.erp.backend.entity.Course;
import com.erp.backend.repository.CourseRepository;

@Service
@Transactional
public class CourseService {

    private static final Logger log = LoggerFactory.getLogger(CourseService.class);

    private final CourseRepository courseRepository;
    private final DepartmentService departmentService;
    private final UserService userService;

    public CourseService(
            CourseRepository courseRepository,
            DepartmentService departmentService,
            UserService userService
    ) {
        this.courseRepository = courseRepository;
        this.departmentService = departmentService;
        this.userService = userService;
    }

    public List<CourseResponse> getAllCourses() {
        return courseRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    public CourseResponse getCourse(Long id) {
        return toResponse(findCourseEntity(id));
    }

    public List<CourseResponse> getTeacherCourses(Long teacherId) {
        AppUser teacher = userService.getUserEntity(teacherId);
        return courseRepository.findAllByTeacherOrderByCodeAsc(teacher).stream()
                .map(this::toResponse)
                .toList();
    }

    public Course findCourseEntity(Long id) {
        return courseRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Course not found."));
    }

    public CourseResponse createCourse(CourseRequest request) {
        if (courseRepository.existsByCodeIgnoreCase(request.code())) {
            throw new ApiException(HttpStatus.CONFLICT, "Course code already exists.");
        }

        Course course = new Course();
        applyRequest(course, request);
        Course savedCourse = courseRepository.save(course);
        log.info("Created course {}", savedCourse.getCode());
        return toResponse(savedCourse);
    }

    public CourseResponse updateCourse(Long id, CourseRequest request) {
        Course course = findCourseEntity(id);
        courseRepository.findByCodeIgnoreCase(request.code())
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new ApiException(HttpStatus.CONFLICT, "Course code already exists.");
                });

        applyRequest(course, request);
        Course savedCourse = courseRepository.save(course);
        log.info("Updated course {}", savedCourse.getCode());
        return toResponse(savedCourse);
    }

    public void deleteCourse(Long id) {
        Course course = findCourseEntity(id);
        courseRepository.delete(course);
        log.info("Deleted course {}", course.getCode());
    }

    public CourseResponse toResponse(Course course) {
        return new CourseResponse(
                course.getId(),
                course.getCode(),
                course.getTitle(),
                course.getDescription(),
                course.getCredits(),
                course.getDepartment() != null ? course.getDepartment().getName() : null,
                course.getTeacher() != null ? course.getTeacher().getFullName() : null,
                course.getEnrollments().size()
        );
    }

    private void applyRequest(Course course, CourseRequest request) {
        course.setCode(request.code().trim());
        course.setTitle(request.title().trim());
        course.setDescription(request.description());
        course.setCredits(request.credits());
        course.setDepartment(departmentService.findDepartmentEntity(request.departmentId()));
        course.setTeacher(request.teacherId() != null ? userService.getUserEntity(request.teacherId()) : null);
    }
}
