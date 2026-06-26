package com.laker.postman.plugin.capture;

import java.text.SimpleDateFormat;
import java.util.Date;

import static com.laker.postman.plugin.capture.CaptureI18n.t;

record CaptureDiagnosticEvent(long timestamp,
                              CaptureDiagnosticLevel level,
                              CaptureDiagnosticPhase phase,
                              CaptureDiagnosticRole role,
                              String title,
                              String detail,
                              String suggestion) {

    static CaptureDiagnosticEvent debug(CaptureDiagnosticPhase phase,
                                        CaptureDiagnosticRole role,
                                        String title,
                                        String detail,
                                        String suggestion) {
        return event(CaptureDiagnosticLevel.DEBUG, phase, role, title, detail, suggestion);
    }

    static CaptureDiagnosticEvent info(CaptureDiagnosticPhase phase,
                                       CaptureDiagnosticRole role,
                                       String title,
                                       String detail,
                                       String suggestion) {
        return event(CaptureDiagnosticLevel.INFO, phase, role, title, detail, suggestion);
    }

    static CaptureDiagnosticEvent warn(CaptureDiagnosticPhase phase,
                                       CaptureDiagnosticRole role,
                                       String title,
                                       String detail,
                                       String suggestion) {
        return event(CaptureDiagnosticLevel.WARN, phase, role, title, detail, suggestion);
    }

    static CaptureDiagnosticEvent error(CaptureDiagnosticPhase phase,
                                        CaptureDiagnosticRole role,
                                        String title,
                                        String detail,
                                        String suggestion) {
        return event(CaptureDiagnosticLevel.ERROR, phase, role, title, detail, suggestion);
    }

    String formattedText() {
        StringBuilder builder = new StringBuilder();
        builder.append('[')
                .append(formatTime(timestamp))
                .append("] ")
                .append(level)
                .append(' ')
                .append(phase)
                .append(" | ")
                .append(role.displayText())
                .append('\n');
        appendSection(builder, t(MessageKeys.TOOLBOX_CAPTURE_DIAGNOSTIC_SUMMARY), title);
        appendSection(builder, t(MessageKeys.TOOLBOX_CAPTURE_DIAGNOSTIC_DETAIL), detail);
        appendSection(builder, t(MessageKeys.TOOLBOX_CAPTURE_DIAGNOSTIC_SUGGESTION), suggestion);
        return builder.toString();
    }

    private static CaptureDiagnosticEvent event(CaptureDiagnosticLevel level,
                                                CaptureDiagnosticPhase phase,
                                                CaptureDiagnosticRole role,
                                                String title,
                                                String detail,
                                                String suggestion) {
        return new CaptureDiagnosticEvent(
                System.currentTimeMillis(),
                level,
                phase,
                role,
                title == null ? "" : title,
                detail == null ? "" : detail,
                suggestion == null ? "" : suggestion
        );
    }

    private static void appendSection(StringBuilder builder, String label, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        builder.append(label).append(": ").append(value).append('\n');
    }

    private static String formatTime(long timestamp) {
        return new SimpleDateFormat("HH:mm:ss.SSS").format(new Date(timestamp));
    }
}
