package com.laker.postman.test;

import lombok.experimental.UtilityClass;

import javax.swing.*;
import java.util.HashMap;
import java.util.Map;

@UtilityClass
public class ThemeTokenTestSupport {
    public static Map<String, Object> remember(String... keys) {
        Map<String, Object> values = new HashMap<>();
        for (String key : keys) {
            values.put(key, UIManager.get(key));
        }
        return values;
    }

    public static void restore(Map<String, Object> values) {
        for (Map.Entry<String, Object> entry : values.entrySet()) {
            UIManager.put(entry.getKey(), entry.getValue());
        }
    }
}
