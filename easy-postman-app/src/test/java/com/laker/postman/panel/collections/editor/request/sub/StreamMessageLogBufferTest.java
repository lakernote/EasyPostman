package com.laker.postman.panel.collections.editor.request.sub;

import org.testng.annotations.Test;

import java.util.List;

import static org.testng.Assert.assertEquals;

public class StreamMessageLogBufferTest {

    @Test
    public void appendAndTrimShouldKeepNewestRowsAndTrackDroppedCount() {
        StreamMessageLogBuffer<Integer> buffer = new StreamMessageLogBuffer<>(3);

        List<Integer> firstDrop = buffer.appendAndTrim(List.of(1, 2));
        List<Integer> secondDrop = buffer.appendAndTrim(List.of(3, 4, 5));

        assertEquals(firstDrop, List.of());
        assertEquals(secondDrop, List.of(1, 2));
        assertEquals(buffer.rows(), List.of(3, 4, 5));
        assertEquals(buffer.droppedCount(), 2);
    }

    @Test
    public void clearShouldResetRowsAndDroppedCount() {
        StreamMessageLogBuffer<Integer> buffer = new StreamMessageLogBuffer<>(2);
        buffer.appendAndTrim(List.of(1, 2, 3));

        buffer.clear();

        assertEquals(buffer.rows(), List.of());
        assertEquals(buffer.droppedCount(), 0);
    }
}
