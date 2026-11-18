package com.ddd.application.utils;

import com.ddd.application.dto.export.AttendeeExportRowDto;
import com.ddd.application.dto.export.EventAttendeeExportDto;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public final class CsvExportUtil {

    private static final String CSV_DELIMITER = ",";
    private static final String CRLF = "\r\n";
    private static final String UTF8_BOM = "\uFEFF"; // Forces Excel to parse UTF-8 (Vietnamese characters)

    // Standard human-readable UTC timestamp
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter
            .ofPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(ZoneId.of("UTC"));

    private CsvExportUtil() {
        // Private constructor for static utility class
    }

    public static byte[] generateAttendeesCsv(EventAttendeeExportDto exportData) {
        StringBuilder sb = new StringBuilder();

        sb.append(UTF8_BOM);

        writeEventHeader(sb, exportData);

        // Blank line separating event info from attendees table
        sb.append(CRLF);

        writeTableHeaders(sb);

        if (exportData.attendees() != null && !exportData.attendees().isEmpty()) {
            for (AttendeeExportRowDto row : exportData.attendees()) {
                writeAttendeeRow(sb, row);
            }
        }

        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    private static void writeEventHeader(StringBuilder sb, EventAttendeeExportDto event) {
        appendLine(sb, "# EVENT INFORMATION");
        appendLine(sb, "Event ID", event.eventId());
        appendLine(sb, "Event Name", event.eventName());
        appendLine(sb, "Location", event.location());
        appendLine(sb, "Event Date", formatInstant(event.dateHappen()));
        appendLine(sb, "Registration Window",
                formatInstant(event.dateOpen()) + " - " + formatInstant(event.dateClose()));
        appendLine(sb, "Capacity", event.capacity() != null ? event.capacity().toString() : "N/A");
        appendLine(sb, "Status", event.status());
    }

    private static void writeTableHeaders(StringBuilder sb) {
        appendLine(sb,
                "No.",
                "Student ID",
                "Full Name",
                "Email",
                "Gender",
                "Major",
                "Registration Date",
                "Status",
                "Check-in Time",
                "Check-out Time",
                "Note / Reason"
        );
    }

    private static void writeAttendeeRow(StringBuilder sb, AttendeeExportRowDto row) {
        appendLine(sb,
                String.valueOf(row.index()),
                row.studentId(),
                row.lastName() + " " + row.firstName(),
                row.email(),
                row.gender(),
                row.majorName(),
                formatInstant(row.regDate()),
                row.status(),
                formatInstant(row.checkInAt()),
                formatInstant(row.checkOutAt()),
                row.reason()
        );
    }

    /**
     * Appends a list of fields as a single CSV line with CRLF terminator.
     */
    private static void appendLine(StringBuilder sb, Object... fields) {
        for (int i = 0; i < fields.length; i++) {
            if (i > 0) {
                sb.append(CSV_DELIMITER);
            }
            sb.append(escapeCsv(fields[i]));
        }
        sb.append(CRLF);
    }

    /**
     * RFC 4180 CSV escaping:
     * - Returns empty string for null.
     * - Doubles any internal quotes (") -> ("").
     * - Wraps in double quotes if the field contains comma, quote, or newline.
     */
    public static String escapeCsv(Object value) {
        if (value == null) {
            return "";
        }

        String str = value.toString();
        boolean containsSpecialChar = str.contains(",") || str.contains("\"")
                || str.contains("\n") || str.contains("\r");

        if (containsSpecialChar) {
            // Escape " with ""
            String escaped = str.replace("\"", "\"\"");
            return "\"" + escaped + "\"";
        }

        return str;
    }

    private static String formatInstant(Instant instant) {
        return instant != null ? DATE_TIME_FORMATTER.format(instant) : "";
    }
}
