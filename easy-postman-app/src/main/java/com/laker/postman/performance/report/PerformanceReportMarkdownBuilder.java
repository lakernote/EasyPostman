package com.laker.postman.performance.report;


import lombok.experimental.UtilityClass;

import java.util.List;

@UtilityClass
public class PerformanceReportMarkdownBuilder {

    public String build(String title, String emptyText, List<ReportTable> tables) {
        boolean hasRows = tables != null && tables.stream().anyMatch(table -> !table.rows().isEmpty());
        if (!hasRows) {
            return emptyText;
        }

        StringBuilder markdown = new StringBuilder(1024);
        markdown.append("# ").append(title).append("\n\n");
        for (ReportTable table : tables) {
            appendMarkdownTable(markdown, table);
        }
        return markdown.toString();
    }

    private void appendMarkdownTable(StringBuilder markdown, ReportTable table) {
        if (table.rows().isEmpty()) {
            return;
        }
        markdown.append("## ").append(table.title()).append("\n\n");
        for (String column : table.columns()) {
            markdown.append("| ").append(escapeMarkdownCell(column)).append(' ');
        }
        markdown.append("|\n");
        for (int col = 0; col < table.columns().size(); col++) {
            markdown.append("| --- ");
        }
        markdown.append("|\n");
        for (Object[] row : table.rows()) {
            for (Object value : row) {
                markdown.append("| ").append(escapeMarkdownCell(value == null ? "" : value.toString())).append(' ');
            }
            markdown.append("|\n");
        }
        markdown.append('\n');
    }

    private String escapeMarkdownCell(String value) {
        return value == null ? "" : value.replace("|", "\\|").replace("\n", " ");
    }

    public record ReportTable(String title, List<String> columns, List<Object[]> rows) {
    }
}
