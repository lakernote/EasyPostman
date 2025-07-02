package com.laker.postman.common.setting;

import java.io.*;
import java.util.Properties;

public class SettingManager {
    private static final String CONFIG_FILE = System.getProperty("user.home") + File.separator + "easy_postman_settings.properties";
    private static final Properties props = new Properties();

    static {
        load();
    }

    public static void load() {
        File file = new File(CONFIG_FILE);
        if (file.exists()) {
            try (FileInputStream fis = new FileInputStream(file)) {
                props.load(fis);
            } catch (IOException e) {
                // ignore
            }
        }
    }

    public static void save() {
        try (FileOutputStream fos = new FileOutputStream(CONFIG_FILE)) {
            props.store(fos, "EasyPostman Settings");
        } catch (IOException e) {
            // ignore
        }
    }

    public static int getMaxBodySize() {
        String val = props.getProperty("max_body_size");
        if (val != null) {
            try {
                return Integer.parseInt(val);
            } catch (NumberFormatException e) {
                return 10 * 1024;
            }
        }
        return 10 * 1024;
    }

    public static void setMaxBodySize(int size) {
        props.setProperty("max_body_size", String.valueOf(size));
        save();
    }
}

