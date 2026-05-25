package com.laker.postman.frame;

import org.testng.annotations.Test;

import java.awt.Dimension;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class MainWindowSizePolicyTest {

    @Test
    public void shouldResolveMinimumWindowSizeByScreenWidth() {
        assertEquals(MainWindowSizePolicy.minimumSizeForScreenWidth(3840), new Dimension(1920, 1200));
        assertEquals(MainWindowSizePolicy.minimumSizeForScreenWidth(2560), new Dimension(1600, 1000));
        assertEquals(MainWindowSizePolicy.minimumSizeForScreenWidth(1920), new Dimension(1400, 900));
        assertEquals(MainWindowSizePolicy.minimumSizeForScreenWidth(1280), new Dimension(1280, 800));
        assertEquals(MainWindowSizePolicy.minimumSizeForScreenWidth(1279), new Dimension(1100, 700));
    }

    @Test
    public void shouldStartMaximizedOnSmallScreensOnly() {
        assertTrue(MainWindowSizePolicy.shouldStartMaximized(1366));
        assertFalse(MainWindowSizePolicy.shouldStartMaximized(1367));
    }
}
