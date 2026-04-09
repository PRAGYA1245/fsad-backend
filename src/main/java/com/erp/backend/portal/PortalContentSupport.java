package com.erp.backend.portal;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.erp.backend.auth.AuthenticatedUser;

final class PortalContentSupport {

    private PortalContentSupport() {
    }

    static SectionResponse section(
            String brand,
            String portalTitle,
            AuthenticatedUser user,
            String title,
            String subtitle,
            List<StatCard> stats,
            DataTablePayload table,
            String noticesTitle,
            List<Notice> notices
    ) {
        return new SectionResponse(
                brand,
                portalTitle,
                user.userName(),
                user.userMeta(),
                title,
                subtitle,
                stats,
                table,
                noticesTitle,
                notices
        );
    }

    static DataTablePayload table(String title, List<String> columns, List<Map<String, String>> rows) {
        return new DataTablePayload(title, columns, rows, null);
    }

    static Map<String, String> row(String... values) {
        if (values.length % 2 != 0) {
            throw new IllegalArgumentException("Rows must be provided as key/value pairs.");
        }

        Map<String, String> row = new LinkedHashMap<>();
        for (int index = 0; index < values.length; index += 2) {
            row.put(values[index], values[index + 1]);
        }
        return row;
    }

    static String brandFor(Role role) {
        return role == Role.ADMIN ? "ERP" : "EduConnect ERP";
    }

    static String titleFor(Role role) {
        return switch (role) {
            case ADMIN -> "Admin Control Panel";
            case TEACHER -> "Faculty Dashboard";
            case STUDENT -> "Student Portal";
        };
    }
}
