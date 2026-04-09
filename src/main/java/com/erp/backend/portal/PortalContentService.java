package com.erp.backend.portal;

import static com.erp.backend.portal.PortalContentSupport.brandFor;
import static com.erp.backend.portal.PortalContentSupport.row;
import static com.erp.backend.portal.PortalContentSupport.section;
import static com.erp.backend.portal.PortalContentSupport.titleFor;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.erp.backend.auth.AuthenticatedUser;
import com.erp.backend.common.ApiException;
import com.erp.backend.entity.AppUser;
import com.erp.backend.entity.Course;
import com.erp.backend.entity.Enrollment;
import com.erp.backend.entity.StudentProfile;
import com.erp.backend.entity.TeacherProfile;
import com.erp.backend.repository.AppUserRepository;
import com.erp.backend.repository.CourseRepository;
import com.erp.backend.repository.DepartmentRepository;
import com.erp.backend.repository.EnrollmentRepository;
import com.erp.backend.repository.FileDocumentRepository;
import com.erp.backend.repository.NoticeRepository;

@Service
public class PortalContentService {

    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a", Locale.ENGLISH);

    private final AppUserRepository appUserRepository;
    private final DepartmentRepository departmentRepository;
    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final NoticeRepository noticeRepository;
    private final FileDocumentRepository fileDocumentRepository;

    public PortalContentService(
            AppUserRepository appUserRepository,
            DepartmentRepository departmentRepository,
            CourseRepository courseRepository,
            EnrollmentRepository enrollmentRepository,
            NoticeRepository noticeRepository,
            FileDocumentRepository fileDocumentRepository
    ) {
        this.appUserRepository = appUserRepository;
        this.departmentRepository = departmentRepository;
        this.courseRepository = courseRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.noticeRepository = noticeRepository;
        this.fileDocumentRepository = fileDocumentRepository;
    }

    public AdminDashboardResponse getAdminDashboard(AppUser user) {
        AuthenticatedUser portalUser = toPortalUser(user);
        List<AppUser> users = getUsersSorted();

        return new AdminDashboardResponse(
                brandFor(Role.ADMIN),
                titleFor(Role.ADMIN),
                portalUser.userName(),
                portalUser.userMeta(),
                "Institutional Overview",
                "Live institutional summary generated from the connected MySQL database.",
                List.of(
                        new ActionButton("Manage Users", "secondary", "/admin/user-management"),
                        new ActionButton("View Reports", "primary", "/admin/reports")
                ),
                List.of(
                        new StatCard("Total Students", String.valueOf(appUserRepository.countByRole(Role.STUDENT)), null),
                        new StatCard("Total Teachers", String.valueOf(appUserRepository.countByRole(Role.TEACHER)), null),
                        new StatCard("Departments", String.valueOf(departmentRepository.count()), null),
                        new StatCard("Active Courses", String.valueOf(courseRepository.count()), null)
                ),
                new DataTablePayload(
                        "Recent Users",
                        List.of("Name & Email", "Role", "Verified", "Joined"),
                        users.stream()
                                .limit(6)
                                .map(appUser -> row(
                                        "Name & Email", appUser.getFullName() + " | " + appUser.getEmail(),
                                        "Role", appUser.getRole().name(),
                                        "Verified", appUser.isEmailVerified() ? "Yes" : "No",
                                        "Joined", formatDateTime(appUser.getCreatedAt())
                                ))
                                .toList(),
                        "Manage Users"
                ),
                getVisibleNotices(Role.ADMIN, 5)
        );
    }

    public TeacherDashboardResponse getTeacherDashboard(AppUser user) {
        AuthenticatedUser portalUser = toPortalUser(user);
        List<Course> teacherCourses = courseRepository.findAllByTeacherOrderByCodeAsc(user);
        List<Enrollment> teacherEnrollments = teacherCourses.stream()
                .flatMap(course -> course.getEnrollments().stream())
                .toList();

        long distinctStudents = teacherEnrollments.stream()
                .map(enrollment -> enrollment.getStudent().getId())
                .filter(Objects::nonNull)
                .distinct()
                .count();

        return new TeacherDashboardResponse(
                brandFor(Role.TEACHER),
                titleFor(Role.TEACHER),
                portalUser.userName(),
                portalUser.userMeta(),
                "Welcome back, " + user.getFullName(),
                "Your dashboard is now driven by courses and enrollments stored in the database.",
                List.of(
                        new ActionButton("My Classes", "secondary", "/teacher/classes"),
                        new ActionButton("Student List", "primary", "/teacher/student-list")
                ),
                List.of(
                        new StatCard("Assigned Courses", String.valueOf(teacherCourses.size()), null),
                        new StatCard("Total Students", String.valueOf(distinctStudents), null),
                        new StatCard("Active Enrollments", String.valueOf(teacherEnrollments.size()), null),
                        new StatCard("Files Uploaded", String.valueOf(fileDocumentRepository.findAllByUploaderOrderByUploadedAtDesc(user).size()), null)
                ),
                teacherCourses.stream()
                        .limit(4)
                        .map(course -> new ClassCard(
                                course.getTitle(),
                                course.getEnrollments().isEmpty() ? "No Students" : "Active",
                                course.getCode() + " | " + safe(course.getDepartment() != null ? course.getDepartment().getName() : null)
                                        + " | " + course.getEnrollments().size() + " Students",
                                List.of(
                                        new ActionButton("Roster", "secondary", "/teacher/student-list"),
                                        new ActionButton("Attendance", "secondary", "/teacher/attendance")
                                )
                        ))
                        .toList(),
                new InfoPanel(
                        "Attendance Overview",
                        "Average attendance based on current enrollments",
                        teacherCourses.stream()
                                .limit(5)
                                .map(course -> new InfoItem(
                                        course.getCode(),
                                        formatPercent(averageAttendance(course.getEnrollments().stream().toList()))
                                ))
                                .toList()
                ),
                new InfoPanel(
                        "Assigned Courses",
                        "Live teaching load from the database",
                        teacherCourses.stream()
                                .limit(5)
                                .map(course -> new InfoItem(course.getCode(), course.getTitle()))
                                .toList()
                )
        );
    }

    public StudentDashboardResponse getStudentDashboard(AppUser user) {
        AuthenticatedUser portalUser = toPortalUser(user);
        StudentProfile profile = user.getStudentProfile();
        List<Enrollment> enrollments = enrollmentRepository.findAllByStudentOrderByEnrolledAtDesc(user);
        List<com.erp.backend.entity.Notice> notices = noticeRepository
                .findAllByAudienceRoleIsNullOrAudienceRoleOrderByCreatedAtDesc(Role.STUDENT);

        int totalCredits = enrollments.stream()
                .map(Enrollment::getCourse)
                .filter(Objects::nonNull)
                .mapToInt(Course::getCredits)
                .sum();

        long completedCourses = enrollments.stream()
                .filter(enrollment -> enrollment.getStatus() == com.erp.backend.entity.EnrollmentStatus.COMPLETED)
                .count();

        return new StudentDashboardResponse(
                brandFor(Role.STUDENT),
                titleFor(Role.STUDENT),
                portalUser.userName(),
                portalUser.userMeta(),
                new HeroPayload(
                        "Welcome back,",
                        user.getFullName(),
                        List.of(
                                "Student ID: " + safe(profile != null ? profile.getStudentId() : null),
                                safe(user.getDepartment() != null ? user.getDepartment().getName() : null),
                                "Current Semester: " + (profile != null ? profile.getSemester() : 0)
                        )
                ),
                List.of(
                        new StatCard("Cumulative GPA", profile != null ? formatDecimal(profile.getCgpa()) + " / 10.0" : "N/A", null),
                        new StatCard("Overall Attendance", profile != null ? formatPercent(profile.getAttendancePercentage()) : "N/A", null),
                        new StatCard("Active Courses", String.valueOf(enrollments.size()), null)
                ),
                List.of(
                        new StatCard("Completed Courses", String.valueOf(completedCourses), null),
                        new StatCard("Current Credits", String.valueOf(totalCredits), null),
                        new StatCard("Visible Notices", String.valueOf(notices.size()), null),
                        new StatCard("Uploaded Files", String.valueOf(fileDocumentRepository.findAllByUploaderOrderByUploadedAtDesc(user).size()), null)
                ),
                new DataTablePayload(
                        "Current Course Registration",
                        List.of("Course Code", "Title", "Credits", "Status"),
                        enrollments.stream()
                                .map(enrollment -> row(
                                        "Course Code", enrollment.getCourse().getCode(),
                                        "Title", enrollment.getCourse().getTitle(),
                                        "Credits", String.valueOf(enrollment.getCourse().getCredits()),
                                        "Status", enrollment.getStatus().name()
                                ))
                                .toList(),
                        null
                ),
                getVisibleNotices(Role.STUDENT, 5)
        );
    }

    public ProfileResponse getProfile(Role role, AppUser user) {
        AuthenticatedUser portalUser = toPortalUser(user);

        return switch (role) {
            case STUDENT -> buildStudentProfile(user, portalUser);
            case TEACHER -> buildTeacherProfile(user, portalUser);
            case ADMIN -> buildAdminProfile(user, portalUser);
        };
    }

    public SectionResponse getSection(Role role, String slug, AppUser user) {
        AuthenticatedUser portalUser = toPortalUser(user);

        return switch (role) {
            case ADMIN -> buildAdminSection(slug, portalUser);
            case TEACHER -> buildTeacherSection(slug, user, portalUser);
            case STUDENT -> buildStudentSection(slug, user, portalUser);
        };
    }

    private ProfileResponse buildStudentProfile(AppUser user, AuthenticatedUser portalUser) {
        StudentProfile profile = user.getStudentProfile();

        return new ProfileResponse(
                brandFor(Role.STUDENT),
                titleFor(Role.STUDENT),
                portalUser.userName(),
                portalUser.userMeta(),
                "My Profile",
                "Personal and academic information loaded from your account record.",
                List.of(
                        new StatCard("Current Semester", profile != null ? String.valueOf(profile.getSemester()) : "N/A", null),
                        new StatCard("Cumulative GPA", profile != null ? formatDecimal(profile.getCgpa()) : "N/A", null),
                        new StatCard("Attendance", profile != null ? formatPercent(profile.getAttendancePercentage()) : "N/A", null)
                ),
                new DataTablePayload(
                        "Basic Information",
                        List.of("Field", "Value"),
                        List.of(
                                row("Field", "Name", "Value", user.getFullName()),
                                row("Field", "Student ID", "Value", safe(profile != null ? profile.getStudentId() : null)),
                                row("Field", "Department", "Value", safe(user.getDepartment() != null ? user.getDepartment().getName() : null)),
                                row("Field", "Email", "Value", user.getEmail()),
                                row("Field", "Phone", "Value", safe(profile != null ? profile.getPhone() : null))
                        ),
                        null
                ),
                new InfoPanel(
                        "Address and Guardian",
                        "Additional student profile details",
                        List.of(
                                new InfoItem("Program", safe(profile != null ? profile.getProgram() : null)),
                                new InfoItem("Address", safe(profile != null ? profile.getAddress() : null)),
                                new InfoItem("Guardian", safe(profile != null ? profile.getGuardianName() : null)),
                                new InfoItem("Guardian Contact", safe(profile != null ? profile.getGuardianPhone() : null))
                        )
                )
        );
    }

    private ProfileResponse buildTeacherProfile(AppUser user, AuthenticatedUser portalUser) {
        TeacherProfile profile = user.getTeacherProfile();
        List<Course> teacherCourses = courseRepository.findAllByTeacherOrderByCodeAsc(user);
        long studentCount = teacherCourses.stream()
                .flatMap(course -> course.getEnrollments().stream())
                .map(enrollment -> enrollment.getStudent().getId())
                .filter(Objects::nonNull)
                .distinct()
                .count();

        return new ProfileResponse(
                brandFor(Role.TEACHER),
                titleFor(Role.TEACHER),
                portalUser.userName(),
                portalUser.userMeta(),
                "Faculty Profile",
                "Faculty information and teaching load from the database.",
                List.of(
                        new StatCard("Assigned Courses", String.valueOf(teacherCourses.size()), null),
                        new StatCard("Students", String.valueOf(studentCount), null),
                        new StatCard("Experience", profile != null ? profile.getExperienceYears() + " Years" : "N/A", null)
                ),
                new DataTablePayload(
                        "Basic Information",
                        List.of("Field", "Value"),
                        List.of(
                                row("Field", "Name", "Value", user.getFullName()),
                                row("Field", "Employee ID", "Value", safe(profile != null ? profile.getEmployeeId() : null)),
                                row("Field", "Department", "Value", safe(user.getDepartment() != null ? user.getDepartment().getName() : null)),
                                row("Field", "Email", "Value", user.getEmail()),
                                row("Field", "Phone", "Value", safe(profile != null ? profile.getPhone() : null))
                        ),
                        null
                ),
                new InfoPanel(
                        "Teaching Details",
                        "Professional information from the faculty profile",
                        List.of(
                                new InfoItem("Designation", safe(profile != null ? profile.getDesignation() : null)),
                                new InfoItem("Specialization", safe(profile != null ? profile.getSpecialization() : null)),
                                new InfoItem("Office Hours", safe(profile != null ? profile.getOfficeHours() : null)),
                                new InfoItem("Courses", teacherCourses.isEmpty() ? "No courses assigned" : String.valueOf(teacherCourses.size()))
                        )
                )
        );
    }

    private ProfileResponse buildAdminProfile(AppUser user, AuthenticatedUser portalUser) {
        TeacherProfile profile = user.getTeacherProfile();
        long verifiedUsers = appUserRepository.findAll().stream()
                .filter(AppUser::isEmailVerified)
                .count();

        return new ProfileResponse(
                brandFor(Role.ADMIN),
                titleFor(Role.ADMIN),
                portalUser.userName(),
                portalUser.userMeta(),
                "Administrator Profile",
                "Administrative access and system summary from the live database.",
                List.of(
                        new StatCard("Total Users", String.valueOf(appUserRepository.count()), null),
                        new StatCard("Departments", String.valueOf(departmentRepository.count()), null),
                        new StatCard("Courses", String.valueOf(courseRepository.count()), null)
                ),
                new DataTablePayload(
                        "Basic Information",
                        List.of("Field", "Value"),
                        List.of(
                                row("Field", "Name", "Value", user.getFullName()),
                                row("Field", "Employee ID", "Value", safe(profile != null ? profile.getEmployeeId() : null)),
                                row("Field", "Department", "Value", safe(user.getDepartment() != null ? user.getDepartment().getName() : null)),
                                row("Field", "Email", "Value", user.getEmail()),
                                row("Field", "Phone", "Value", safe(profile != null ? profile.getPhone() : null))
                        ),
                        null
                ),
                new InfoPanel(
                        "Access Summary",
                        "Live administrative metrics",
                        List.of(
                                new InfoItem("Role", user.getRole().name()),
                                new InfoItem("Designation", safe(profile != null ? profile.getDesignation() : null)),
                                new InfoItem("Verified Users", String.valueOf(verifiedUsers)),
                                new InfoItem("Visible Notices", String.valueOf(getVisibleNotices(Role.ADMIN, 100).size()))
                        )
                )
        );
    }

    private SectionResponse buildAdminSection(String slug, AuthenticatedUser portalUser) {
        List<AppUser> users = getUsersSorted();
        List<Course> courses = getCoursesSorted();
        List<com.erp.backend.entity.Notice> notices = getEntityNoticesSorted();

        return switch (slug) {
            case "user-management" -> section(
                    brandFor(Role.ADMIN),
                    titleFor(Role.ADMIN),
                    portalUser,
                    "User Management",
                    "Live user records stored in the database.",
                    List.of(
                            new StatCard("Total Users", String.valueOf(users.size()), null),
                            new StatCard("Students", String.valueOf(appUserRepository.countByRole(Role.STUDENT)), null),
                            new StatCard("Teachers", String.valueOf(appUserRepository.countByRole(Role.TEACHER)), null)
                    ),
                    new DataTablePayload(
                            "Users",
                            List.of("Username", "Email", "Role", "Verified"),
                            users.stream()
                                    .limit(10)
                                    .map(appUser -> row(
                                            "Username", appUser.getUsername(),
                                            "Email", appUser.getEmail(),
                                            "Role", appUser.getRole().name(),
                                            "Verified", appUser.isEmailVerified() ? "Yes" : "No"
                                    ))
                                    .toList(),
                            null
                    ),
                    "Recent Notices",
                    getVisibleNotices(Role.ADMIN, 5)
            );
            case "departments" -> section(
                    brandFor(Role.ADMIN),
                    titleFor(Role.ADMIN),
                    portalUser,
                    "Departments",
                    "Departments and their live user/course counts.",
                    List.of(
                            new StatCard("Departments", String.valueOf(departmentRepository.count()), null),
                            new StatCard("Mapped Users", String.valueOf(users.stream().filter(appUser -> appUser.getDepartment() != null).count()), null),
                            new StatCard("Mapped Courses", String.valueOf(courseRepository.count()), null)
                    ),
                    new DataTablePayload(
                            "Department Directory",
                            List.of("Code", "Name", "Users", "Courses"),
                            departmentRepository.findAll().stream()
                                    .map(department -> row(
                                            "Code", department.getCode(),
                                            "Name", department.getName(),
                                            "Users", String.valueOf(department.getUsers().size()),
                                            "Courses", String.valueOf(department.getCourses().size())
                                    ))
                                    .toList(),
                            null
                    ),
                    "Department Notices",
                    getVisibleNotices(Role.ADMIN, 5)
            );
            case "courses" -> section(
                    brandFor(Role.ADMIN),
                    titleFor(Role.ADMIN),
                    portalUser,
                    "Courses",
                    "Course catalog from the live database.",
                    List.of(
                            new StatCard("Active Courses", String.valueOf(courses.size()), null),
                            new StatCard("Assigned Teachers", String.valueOf(courses.stream().map(Course::getTeacher).filter(Objects::nonNull).map(AppUser::getId).distinct().count()), null),
                            new StatCard("Enrollments", String.valueOf(enrollmentRepository.count()), null)
                    ),
                    new DataTablePayload(
                            "Course Catalog",
                            List.of("Code", "Title", "Department", "Teacher"),
                            courses.stream()
                                    .limit(10)
                                    .map(course -> row(
                                            "Code", course.getCode(),
                                            "Title", course.getTitle(),
                                            "Department", safe(course.getDepartment() != null ? course.getDepartment().getName() : null),
                                            "Teacher", safe(course.getTeacher() != null ? course.getTeacher().getFullName() : null)
                                    ))
                                    .toList(),
                            null
                    ),
                    "Course Notices",
                    getVisibleNotices(Role.ADMIN, 5)
            );
            case "academic-calendar" -> section(
                    brandFor(Role.ADMIN),
                    titleFor(Role.ADMIN),
                    portalUser,
                    "Academic Updates",
                    "No dedicated calendar entity is configured, so this section shows live notices.",
                    List.of(
                            new StatCard("Total Notices", String.valueOf(notices.size()), null),
                            new StatCard("High Priority", String.valueOf(notices.stream().filter(notice -> notice.getLevel() == com.erp.backend.entity.NoticeLevel.HIGH).count()), null),
                            new StatCard("Student Facing", String.valueOf(notices.stream().filter(notice -> notice.getAudienceRole() == Role.STUDENT).count()), null)
                    ),
                    new DataTablePayload(
                            "Academic Updates",
                            List.of("Title", "Audience", "Level", "Created At"),
                            notices.stream()
                                    .limit(10)
                                    .map(notice -> row(
                                            "Title", notice.getTitle(),
                                            "Audience", notice.getAudienceRole() == null ? "All" : notice.getAudienceRole().name(),
                                            "Level", notice.getLevel().name(),
                                            "Created At", formatDateTime(notice.getCreatedAt())
                                    ))
                                    .toList(),
                            null
                    ),
                    "System Notices",
                    getVisibleNotices(Role.ADMIN, 5)
            );
            case "reports" -> section(
                    brandFor(Role.ADMIN),
                    titleFor(Role.ADMIN),
                    portalUser,
                    "Reports",
                    "Live counts generated from the current database state.",
                    List.of(
                            new StatCard("Users", String.valueOf(appUserRepository.count()), null),
                            new StatCard("Courses", String.valueOf(courseRepository.count()), null),
                            new StatCard("Enrollments", String.valueOf(enrollmentRepository.count()), null)
                    ),
                    new DataTablePayload(
                            "System Metrics",
                            List.of("Metric", "Value"),
                            List.of(
                                    row("Metric", "Departments", "Value", String.valueOf(departmentRepository.count())),
                                    row("Metric", "Teachers", "Value", String.valueOf(appUserRepository.countByRole(Role.TEACHER))),
                                    row("Metric", "Students", "Value", String.valueOf(appUserRepository.countByRole(Role.STUDENT))),
                                    row("Metric", "Uploaded Files", "Value", String.valueOf(fileDocumentRepository.count()))
                            ),
                            null
                    ),
                    "Report Notices",
                    getVisibleNotices(Role.ADMIN, 5)
            );
            case "system-settings" -> section(
                    brandFor(Role.ADMIN),
                    titleFor(Role.ADMIN),
                    portalUser,
                    "System Summary",
                    "Operational summary generated from live user and file records.",
                    List.of(
                            new StatCard("Verified Users", String.valueOf(users.stream().filter(AppUser::isEmailVerified).count()), null),
                            new StatCard("OAuth Users", String.valueOf(users.stream().filter(AppUser::isOauthAccount).count()), null),
                            new StatCard("Uploads", String.valueOf(fileDocumentRepository.count()), null)
                    ),
                    new DataTablePayload(
                            "Configuration Snapshot",
                            List.of("Setting", "Value"),
                            List.of(
                                    row("Setting", "Email Verified Users", "Value", String.valueOf(users.stream().filter(AppUser::isEmailVerified).count())),
                                    row("Setting", "Disabled Accounts", "Value", String.valueOf(users.stream().filter(appUser -> !appUser.isEnabled()).count())),
                                    row("Setting", "OAuth Accounts", "Value", String.valueOf(users.stream().filter(AppUser::isOauthAccount).count())),
                                    row("Setting", "Stored Files", "Value", String.valueOf(fileDocumentRepository.count()))
                            ),
                            null
                    ),
                    "System Notices",
                    getVisibleNotices(Role.ADMIN, 5)
            );
            default -> throw new ApiException(HttpStatus.NOT_FOUND, "Admin section not found.");
        };
    }

    private SectionResponse buildTeacherSection(String slug, AppUser user, AuthenticatedUser portalUser) {
        List<Course> courses = courseRepository.findAllByTeacherOrderByCodeAsc(user);
        List<Enrollment> enrollments = courses.stream()
                .flatMap(course -> course.getEnrollments().stream())
                .sorted(this::compareEnrollmentsByEnrolledAtDesc)
                .toList();

        return switch (slug) {
            case "classes" -> section(
                    brandFor(Role.TEACHER),
                    titleFor(Role.TEACHER),
                    portalUser,
                    "Classes",
                    "Assigned classes from the live database.",
                    List.of(
                            new StatCard("Total Classes", String.valueOf(courses.size()), null),
                            new StatCard("Students", String.valueOf(enrollments.stream().map(enrollment -> enrollment.getStudent().getId()).distinct().count()), null),
                            new StatCard("Uploads", String.valueOf(fileDocumentRepository.findAllByUploaderOrderByUploadedAtDesc(user).size()), null)
                    ),
                    new DataTablePayload(
                            "Class Allocation",
                            List.of("Course", "Department", "Students", "Credits"),
                            tableRowsOrEmpty(
                                    courses.stream()
                                            .map(course -> row(
                                                    "Course", course.getCode() + " - " + course.getTitle(),
                                                    "Department", safe(course.getDepartment() != null ? course.getDepartment().getName() : null),
                                                    "Students", String.valueOf(course.getEnrollments().size()),
                                                    "Credits", String.valueOf(course.getCredits())
                                            ))
                                            .toList(),
                                    List.of("Course", "Department", "Students", "Credits"),
                                    "No classes assigned yet."
                            ),
                            null
                    ),
                    "Faculty Notices",
                    getVisibleNotices(Role.TEACHER, 5)
            );
            case "attendance" -> section(
                    brandFor(Role.TEACHER),
                    titleFor(Role.TEACHER),
                    portalUser,
                    "Attendance",
                    "Attendance details calculated from current enrollments.",
                    List.of(
                            new StatCard("Courses", String.valueOf(courses.size()), null),
                            new StatCard("Enrollments", String.valueOf(enrollments.size()), null),
                            new StatCard("Average Attendance", formatPercent(averageAttendance(enrollments)), null)
                    ),
                    new DataTablePayload(
                            "Attendance Register",
                            List.of("Course", "Student", "Attendance", "Status"),
                            tableRowsOrEmpty(
                                    enrollments.stream()
                                            .limit(12)
                                            .map(enrollment -> row(
                                                    "Course", enrollment.getCourse().getCode(),
                                                    "Student", enrollment.getStudent().getFullName(),
                                                    "Attendance", formatPercent(enrollment.getAttendancePercentage()),
                                                    "Status", enrollment.getStatus().name()
                                            ))
                                            .toList(),
                                    List.of("Course", "Student", "Attendance", "Status"),
                                    "No attendance records found."
                            ),
                            null
                    ),
                    "Attendance Notices",
                    getVisibleNotices(Role.TEACHER, 5)
            );
            case "marks" -> section(
                    brandFor(Role.TEACHER),
                    titleFor(Role.TEACHER),
                    portalUser,
                    "Marks",
                    "Grades stored against live enrollment records.",
                    List.of(
                            new StatCard("Courses", String.valueOf(courses.size()), null),
                            new StatCard("Graded", String.valueOf(enrollments.stream().filter(enrollment -> enrollment.getGrade() != null && !enrollment.getGrade().isBlank()).count()), null),
                            new StatCard("Ungraded", String.valueOf(enrollments.stream().filter(enrollment -> enrollment.getGrade() == null || enrollment.getGrade().isBlank()).count()), null)
                    ),
                    new DataTablePayload(
                            "Gradebook",
                            List.of("Student", "Course", "Grade", "Status"),
                            tableRowsOrEmpty(
                                    enrollments.stream()
                                            .limit(12)
                                            .map(enrollment -> row(
                                                    "Student", enrollment.getStudent().getFullName(),
                                                    "Course", enrollment.getCourse().getCode(),
                                                    "Grade", safe(enrollment.getGrade()),
                                                    "Status", enrollment.getStatus().name()
                                            ))
                                            .toList(),
                                    List.of("Student", "Course", "Grade", "Status"),
                                    "No marks have been recorded yet."
                            ),
                            null
                    ),
                    "Evaluation Notices",
                    getVisibleNotices(Role.TEACHER, 5)
            );
            case "timetable" -> section(
                    brandFor(Role.TEACHER),
                    titleFor(Role.TEACHER),
                    portalUser,
                    "Teaching Load",
                    "Live course allocation for this faculty member.",
                    List.of(
                            new StatCard("Courses", String.valueOf(courses.size()), null),
                            new StatCard("Credits", String.valueOf(courses.stream().mapToInt(Course::getCredits).sum()), null),
                            new StatCard("Students", String.valueOf(enrollments.stream().map(enrollment -> enrollment.getStudent().getId()).distinct().count()), null)
                    ),
                    new DataTablePayload(
                            "Assigned Courses",
                            List.of("Code", "Title", "Credits", "Students"),
                            tableRowsOrEmpty(
                                    courses.stream()
                                            .map(course -> row(
                                                    "Code", course.getCode(),
                                                    "Title", course.getTitle(),
                                                    "Credits", String.valueOf(course.getCredits()),
                                                    "Students", String.valueOf(course.getEnrollments().size())
                                            ))
                                            .toList(),
                                    List.of("Code", "Title", "Credits", "Students"),
                                    "No teaching timetable data is available yet."
                            ),
                            null
                    ),
                    "Timetable Notices",
                    getVisibleNotices(Role.TEACHER, 5)
            );
            case "student-list" -> section(
                    brandFor(Role.TEACHER),
                    titleFor(Role.TEACHER),
                    portalUser,
                    "Student List",
                    "Students linked to your assigned courses.",
                    List.of(
                            new StatCard("Students", String.valueOf(enrollments.stream().map(enrollment -> enrollment.getStudent().getId()).distinct().count()), null),
                            new StatCard("Courses", String.valueOf(courses.size()), null),
                            new StatCard("Active", String.valueOf(enrollments.stream().filter(enrollment -> enrollment.getStatus() == com.erp.backend.entity.EnrollmentStatus.ACTIVE).count()), null)
                    ),
                    new DataTablePayload(
                            "Roster Snapshot",
                            List.of("Student", "Student ID", "Course", "Attendance"),
                            tableRowsOrEmpty(
                                    enrollments.stream()
                                            .limit(12)
                                            .map(enrollment -> row(
                                                    "Student", enrollment.getStudent().getFullName(),
                                                    "Student ID", safe(enrollment.getStudent().getStudentProfile() != null ? enrollment.getStudent().getStudentProfile().getStudentId() : null),
                                                    "Course", enrollment.getCourse().getCode(),
                                                    "Attendance", formatPercent(enrollment.getAttendancePercentage())
                                            ))
                                            .toList(),
                                    List.of("Student", "Student ID", "Course", "Attendance"),
                                    "No students are enrolled yet."
                            ),
                            null
                    ),
                    "Student Notices",
                    getVisibleNotices(Role.TEACHER, 5)
            );
            default -> throw new ApiException(HttpStatus.NOT_FOUND, "Teacher section not found.");
        };
    }

    private SectionResponse buildStudentSection(String slug, AppUser user, AuthenticatedUser portalUser) {
        List<Enrollment> enrollments = enrollmentRepository.findAllByStudentOrderByEnrolledAtDesc(user);

        return switch (slug) {
            case "academic-registration" -> section(
                    brandFor(Role.STUDENT),
                    titleFor(Role.STUDENT),
                    portalUser,
                    "Academic Registration",
                    "Registration records stored for the logged-in student.",
                    List.of(
                            new StatCard("Registered Courses", String.valueOf(enrollments.size()), null),
                            new StatCard("Total Credits", String.valueOf(enrollments.stream().map(Enrollment::getCourse).filter(Objects::nonNull).mapToInt(Course::getCredits).sum()), null),
                            new StatCard("Completed", String.valueOf(enrollments.stream().filter(enrollment -> enrollment.getStatus() == com.erp.backend.entity.EnrollmentStatus.COMPLETED).count()), null)
                    ),
                    new DataTablePayload(
                            "Registration Snapshot",
                            List.of("Course", "Title", "Credits", "Status"),
                            tableRowsOrEmpty(
                                    enrollments.stream()
                                            .map(enrollment -> row(
                                                    "Course", enrollment.getCourse().getCode(),
                                                    "Title", enrollment.getCourse().getTitle(),
                                                    "Credits", String.valueOf(enrollment.getCourse().getCredits()),
                                                    "Status", enrollment.getStatus().name()
                                            ))
                                            .toList(),
                                    List.of("Course", "Title", "Credits", "Status"),
                                    "No course registrations found."
                            ),
                            null
                    ),
                    "Student Notices",
                    getVisibleNotices(Role.STUDENT, 5)
            );
            case "attendance-register" -> section(
                    brandFor(Role.STUDENT),
                    titleFor(Role.STUDENT),
                    portalUser,
                    "Attendance Register",
                    "Attendance values from your enrollment records.",
                    List.of(
                            new StatCard("Courses", String.valueOf(enrollments.size()), null),
                            new StatCard("Average Attendance", formatPercent(averageAttendance(enrollments)), null),
                            new StatCard("Uploaded Files", String.valueOf(fileDocumentRepository.findAllByUploaderOrderByUploadedAtDesc(user).size()), null)
                    ),
                    new DataTablePayload(
                            "Attendance by Course",
                            List.of("Course", "Title", "Attendance", "Status"),
                            tableRowsOrEmpty(
                                    enrollments.stream()
                                            .map(enrollment -> row(
                                                    "Course", enrollment.getCourse().getCode(),
                                                    "Title", enrollment.getCourse().getTitle(),
                                                    "Attendance", formatPercent(enrollment.getAttendancePercentage()),
                                                    "Status", enrollment.getStatus().name()
                                            ))
                                            .toList(),
                                    List.of("Course", "Title", "Attendance", "Status"),
                                    "No attendance records found."
                            ),
                            null
                    ),
                    "Attendance Notices",
                    getVisibleNotices(Role.STUDENT, 5)
            );
            case "courses" -> section(
                    brandFor(Role.STUDENT),
                    titleFor(Role.STUDENT),
                    portalUser,
                    "Courses",
                    "Courses currently linked to this student in the database.",
                    List.of(
                            new StatCard("Active Courses", String.valueOf(enrollments.size()), null),
                            new StatCard("Credits", String.valueOf(enrollments.stream().map(Enrollment::getCourse).filter(Objects::nonNull).mapToInt(Course::getCredits).sum()), null),
                            new StatCard("Teachers", String.valueOf(enrollments.stream().map(Enrollment::getCourse).map(Course::getTeacher).filter(Objects::nonNull).map(AppUser::getId).distinct().count()), null)
                    ),
                    new DataTablePayload(
                            "Current Courses",
                            List.of("Code", "Title", "Faculty", "Credits"),
                            tableRowsOrEmpty(
                                    enrollments.stream()
                                            .map(enrollment -> row(
                                                    "Code", enrollment.getCourse().getCode(),
                                                    "Title", enrollment.getCourse().getTitle(),
                                                    "Faculty", safe(enrollment.getCourse().getTeacher() != null ? enrollment.getCourse().getTeacher().getFullName() : null),
                                                    "Credits", String.valueOf(enrollment.getCourse().getCredits())
                                            ))
                                            .toList(),
                                    List.of("Code", "Title", "Faculty", "Credits"),
                                    "No courses found."
                            ),
                            null
                    ),
                    "Course Notices",
                    getVisibleNotices(Role.STUDENT, 5)
            );
            case "exams" -> section(
                    brandFor(Role.STUDENT),
                    titleFor(Role.STUDENT),
                    portalUser,
                    "Assessment Summary",
                    "This installation does not yet have a dedicated exam table, so grades are shown from enrollments.",
                    List.of(
                            new StatCard("Courses", String.valueOf(enrollments.size()), null),
                            new StatCard("Graded", String.valueOf(enrollments.stream().filter(enrollment -> enrollment.getGrade() != null && !enrollment.getGrade().isBlank()).count()), null),
                            new StatCard("Pending", String.valueOf(enrollments.stream().filter(enrollment -> enrollment.getGrade() == null || enrollment.getGrade().isBlank()).count()), null)
                    ),
                    new DataTablePayload(
                            "Exam / Grade Status",
                            List.of("Course", "Grade", "Status", "Attendance"),
                            tableRowsOrEmpty(
                                    enrollments.stream()
                                            .map(enrollment -> row(
                                                    "Course", enrollment.getCourse().getCode(),
                                                    "Grade", safe(enrollment.getGrade()),
                                                    "Status", enrollment.getStatus().name(),
                                                    "Attendance", formatPercent(enrollment.getAttendancePercentage())
                                            ))
                                            .toList(),
                                    List.of("Course", "Grade", "Status", "Attendance"),
                                    "No grade records found."
                            ),
                            null
                    ),
                    "Exam Notices",
                    getVisibleNotices(Role.STUDENT, 5)
            );
            case "projects" -> buildEmptyStudentSection(portalUser, "Projects", "No project records are stored in the database yet.");
            case "clubs-activities" -> buildEmptyStudentSection(portalUser, "Clubs & Activities", "No club or activity records are stored in the database yet.");
            case "feedback" -> buildEmptyStudentSection(portalUser, "Feedback", "No feedback records are stored in the database yet.");
            case "fee-payments" -> buildEmptyStudentSection(portalUser, "Fee Payments", "No fee payment records are stored in the database yet.");
            case "hostel-management" -> buildEmptyStudentSection(portalUser, "Hostel Management", "No hostel records are stored in the database yet.");
            case "hall-ticket" -> buildEmptyStudentSection(portalUser, "Hall Ticket", "No hall ticket records are stored in the database yet.");
            default -> throw new ApiException(HttpStatus.NOT_FOUND, "Student section not found.");
        };
    }

    private SectionResponse buildEmptyStudentSection(AuthenticatedUser portalUser, String title, String subtitle) {
        return section(
                brandFor(Role.STUDENT),
                titleFor(Role.STUDENT),
                portalUser,
                title,
                subtitle,
                List.of(
                        new StatCard("Records", "0", null),
                        new StatCard("Status", "Not Configured", null),
                        new StatCard("Notices", String.valueOf(getVisibleNotices(Role.STUDENT, 100).size()), null)
                ),
                new DataTablePayload(
                        "Current Status",
                        List.of("Item", "Value"),
                        List.of(row("Item", "Database", "Value", "No records available for this module")),
                        null
                ),
                "Student Notices",
                getVisibleNotices(Role.STUDENT, 5)
        );
    }

    private List<AppUser> getUsersSorted() {
        return appUserRepository.findAll().stream()
                .sorted(this::compareUsersByCreatedAtDesc)
                .toList();
    }

    private List<Course> getCoursesSorted() {
        return courseRepository.findAll().stream()
                .sorted(this::compareCoursesByCode)
                .toList();
    }

    private List<com.erp.backend.entity.Notice> getEntityNoticesSorted() {
        return noticeRepository.findAll().stream()
                .sorted(this::compareNoticesByCreatedAtDesc)
                .toList();
    }

    private List<Notice> getVisibleNotices(Role role, int limit) {
        return noticeRepository.findAllByAudienceRoleIsNullOrAudienceRoleOrderByCreatedAtDesc(role).stream()
                .limit(limit)
                .map(notice -> new Notice(notice.getLevel().name(), notice.getTitle()))
                .toList();
    }

    private AuthenticatedUser toPortalUser(AppUser user) {
        return new AuthenticatedUser(
                null,
                String.valueOf(user.getId()),
                user.getRole(),
                user.getRole().getRoute(),
                user.getFullName(),
                buildMeta(user)
        );
    }

    private String buildMeta(AppUser user) {
        if (user.getStudentProfile() != null) {
            return safe(user.getStudentProfile().getProgram()) + " | Semester " + user.getStudentProfile().getSemester();
        }
        if (user.getTeacherProfile() != null) {
            return safe(user.getTeacherProfile().getDesignation());
        }
        return user.getRole().getLabel();
    }

    private List<Map<String, String>> tableRowsOrEmpty(List<Map<String, String>> rows, List<String> columns, String message) {
        if (!rows.isEmpty()) {
            return rows;
        }

        Map<String, String> fallbackRow = new LinkedHashMap<>();
        for (int index = 0; index < columns.size(); index++) {
            fallbackRow.put(columns.get(index), index == 0 ? message : "-");
        }
        return List.of(fallbackRow);
    }

    private double averageAttendance(List<Enrollment> enrollments) {
        return enrollments.stream()
                .mapToDouble(Enrollment::getAttendancePercentage)
                .average()
                .orElse(0.0);
    }

    private int compareUsersByCreatedAtDesc(AppUser left, AppUser right) {
        return compareLocalDateTimeDesc(left != null ? left.getCreatedAt() : null, right != null ? right.getCreatedAt() : null);
    }

    private int compareCoursesByCode(Course left, Course right) {
        String leftCode = left != null ? left.getCode() : null;
        String rightCode = right != null ? right.getCode() : null;

        if (leftCode == null && rightCode == null) {
            return 0;
        }
        if (leftCode == null) {
            return 1;
        }
        if (rightCode == null) {
            return -1;
        }
        return leftCode.compareToIgnoreCase(rightCode);
    }

    private int compareNoticesByCreatedAtDesc(com.erp.backend.entity.Notice left, com.erp.backend.entity.Notice right) {
        return compareLocalDateTimeDesc(left != null ? left.getCreatedAt() : null, right != null ? right.getCreatedAt() : null);
    }

    private int compareEnrollmentsByEnrolledAtDesc(Enrollment left, Enrollment right) {
        return compareLocalDateTimeDesc(left != null ? left.getEnrolledAt() : null, right != null ? right.getEnrolledAt() : null);
    }

    private int compareLocalDateTimeDesc(LocalDateTime left, LocalDateTime right) {
        if (left == null && right == null) {
            return 0;
        }
        if (left == null) {
            return 1;
        }
        if (right == null) {
            return -1;
        }
        return right.compareTo(left);
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "Not set" : value;
    }

    private String formatPercent(double value) {
        return formatDecimal(value) + "%";
    }

    private String formatDecimal(double value) {
        return String.format(Locale.ENGLISH, "%.1f", value);
    }

    private String formatDateTime(LocalDateTime value) {
        return value == null ? "Not available" : DATE_TIME_FORMATTER.format(value);
    }
}
