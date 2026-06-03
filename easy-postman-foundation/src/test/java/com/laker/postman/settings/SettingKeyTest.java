package com.laker.postman.settings;

import org.testng.annotations.Test;

import java.util.Properties;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;

public class SettingKeyTest {

    @Test
    public void shouldReadDefaultAndNormalizeIntegerValues() {
        SettingKey<Integer> portKey = SettingKey.integerKey(
                "proxy_port",
                8080,
                value -> Math.max(1, Math.min(65535, value))
        );
        Properties properties = new Properties();

        assertEquals(portKey.read(properties), Integer.valueOf(8080));

        properties.setProperty("proxy_port", "abc");
        assertEquals(portKey.read(properties), Integer.valueOf(8080));

        properties.setProperty("proxy_port", "0");
        assertEquals(portKey.read(properties), Integer.valueOf(1));

        portKey.write(properties, 70000);
        assertEquals(properties.getProperty("proxy_port"), "65535");
    }

    @Test
    public void shouldRemovePropertyWhenWritingNull() {
        SettingKey<String> key = SettingKey.stringKey("optional_value", "");
        Properties properties = new Properties();
        properties.setProperty("optional_value", "present");

        key.write(properties, null);

        assertFalse(properties.containsKey("optional_value"));
    }
}
