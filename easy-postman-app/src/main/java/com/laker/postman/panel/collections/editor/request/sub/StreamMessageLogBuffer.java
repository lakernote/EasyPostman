package com.laker.postman.panel.collections.editor.request.sub;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

final class StreamMessageLogBuffer<T> {

    static final int DEFAULT_MAX_ROWS = 10_000;

    private final int maxRows;
    private final List<T> rows = new ArrayList<>();
    private long droppedCount;

    StreamMessageLogBuffer() {
        this(DEFAULT_MAX_ROWS);
    }

    StreamMessageLogBuffer(int maxRows) {
        if (maxRows <= 0) {
            throw new IllegalArgumentException("maxRows must be positive");
        }
        this.maxRows = maxRows;
    }

    List<T> appendAndTrim(List<T> rowsToAppend) {
        if (rowsToAppend == null || rowsToAppend.isEmpty()) {
            return List.of();
        }
        rows.addAll(rowsToAppend);
        int overflow = rows.size() - maxRows;
        if (overflow <= 0) {
            return List.of();
        }
        List<T> droppedRows = new ArrayList<>(rows.subList(0, overflow));
        rows.subList(0, overflow).clear();
        droppedCount += overflow;
        return droppedRows;
    }

    List<T> filtered(Predicate<T> predicate) {
        return rows.stream().filter(predicate).toList();
    }

    void clear() {
        rows.clear();
        droppedCount = 0;
    }

    List<T> rows() {
        return Collections.unmodifiableList(rows);
    }

    long droppedCount() {
        return droppedCount;
    }
}
