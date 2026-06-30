package com.laker.postman.panel.collections.editor.request;

import org.testng.annotations.Test;

import java.lang.reflect.Field;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

import static org.testng.Assert.assertEquals;

public class RequestEditorRuntimeControllerTest {

    @Test
    public void streamMessageTimeFormatterShouldIncludeMilliseconds() throws Exception {
        Field field = RequestEditorRuntimeController.class.getDeclaredField("TIME_FORMATTER");
        field.setAccessible(true);
        DateTimeFormatter formatter = (DateTimeFormatter) field.get(null);

        String formatted = LocalTime.of(10, 15, 30, 123_000_000).format(formatter);

        assertEquals(formatted, "10:15:30.123");
    }
}
