package com.laker.postman.test;

import org.testng.SkipException;
import org.testng.annotations.BeforeClass;

import java.awt.*;

public abstract class AbstractSwingUiTest {

    @BeforeClass
    public void skipWhenNoDisplayEnvironment() {
        if (shouldSkipForHeadlessEnvironment()) {
            throw new SkipException("Skipping Swing UI test in headless/no-display environment");
        }
    }

    protected boolean shouldSkipForHeadlessEnvironment() {
        if (GraphicsEnvironment.isHeadless()) {
            return true;
        }

        String osName = System.getProperty("os.name", "").toLowerCase();
        boolean linuxLike = osName.contains("linux");
        if (!linuxLike) {
            return false;
        }

        String display = System.getenv("DISPLAY");
        String waylandDisplay = System.getenv("WAYLAND_DISPLAY");
        return isBlank(display) && isBlank(waylandDisplay);
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
