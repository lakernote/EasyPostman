package com.laker.postman.panel.performance.execution;


import lombok.experimental.UtilityClass;

@UtilityClass
class SseEventFormatter {

    void appendEvent(BoundedTextAccumulator buffer, String id, String type, String data) {
        if (id != null && !id.isBlank()) {
            buffer.append("id: ");
            buffer.append(id);
            buffer.append("\n");
        }
        if (type != null && !type.isBlank()) {
            buffer.append("event: ");
            buffer.append(type);
            buffer.append("\n");
        }
        String eventData = data == null ? "" : data;
        appendDataLines(buffer, eventData);
        buffer.append("\n");
    }

    private void appendDataLines(BoundedTextAccumulator buffer, String eventData) {
        int lineStart = 0;
        for (int i = 0; i < eventData.length(); i++) {
            char ch = eventData.charAt(i);
            if (ch != '\n' && ch != '\r') {
                continue;
            }
            buffer.append("data: ");
            buffer.append(eventData, lineStart, i);
            buffer.append("\n");
            if (ch == '\r' && i + 1 < eventData.length() && eventData.charAt(i + 1) == '\n') {
                i++;
            }
            lineStart = i + 1;
        }
        buffer.append("data: ");
        buffer.append(eventData, lineStart, eventData.length());
        buffer.append("\n");
    }
}
