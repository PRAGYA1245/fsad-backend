package com.erp.backend.portal;

import java.util.List;
import java.util.Map;

record ActionButton(
        String label,
        String variant,
        String path
) {
}

record StatCard(
        String title,
        String value,
        String sub
) {
}

record Notice(
        String level,
        String title
) {
}

record DataTablePayload(
        String title,
        List<String> columns,
        List<Map<String, String>> rows,
        String linkLabel
) {
}

record InfoItem(
        String label,
        String value
) {
}

record InfoPanel(
        String title,
        String subtitle,
        List<InfoItem> items
) {
}

record HeroPayload(
        String intro,
        String title,
        List<String> meta
) {
}

record ClassCard(
        String title,
        String status,
        String subtitle,
        List<ActionButton> actions
) {
}

record AdminDashboardResponse(
        String brand,
        String portalTitle,
        String userName,
        String userMeta,
        String title,
        String subtitle,
        List<ActionButton> actions,
        List<StatCard> stats,
        DataTablePayload recentActivity,
        List<Notice> notices
) {
}

record TeacherDashboardResponse(
        String brand,
        String portalTitle,
        String userName,
        String userMeta,
        String title,
        String subtitle,
        List<ActionButton> actions,
        List<StatCard> stats,
        List<ClassCard> classes,
        InfoPanel attendanceTracker,
        InfoPanel dailyTimetable
) {
}

record StudentDashboardResponse(
        String brand,
        String portalTitle,
        String userName,
        String userMeta,
        HeroPayload hero,
        List<StatCard> stats,
        List<StatCard> portfolioStats,
        DataTablePayload courseRegistration,
        List<Notice> notices
) {
}

record ProfileResponse(
        String brand,
        String portalTitle,
        String userName,
        String userMeta,
        String title,
        String subtitle,
        List<StatCard> stats,
        DataTablePayload primaryTable,
        InfoPanel secondaryPanel
) {
}

record SectionResponse(
        String brand,
        String portalTitle,
        String userName,
        String userMeta,
        String title,
        String subtitle,
        List<StatCard> stats,
        DataTablePayload table,
        String noticesTitle,
        List<Notice> notices
) {
}
