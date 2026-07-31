package com.laker.postman.frame;

import org.testng.annotations.Test;

import java.awt.Dimension;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class MainWindowSizePolicyTest {

    @Test
    public void shouldResolvePreferredWindowSizeByScreenWidth() {
        assertEquals(MainWindowSizePolicy.preferredSizeForScreenWidth(3840), new Dimension(1920, 1200));
        assertEquals(MainWindowSizePolicy.preferredSizeForScreenWidth(2560), new Dimension(1600, 1000));
        assertEquals(MainWindowSizePolicy.preferredSizeForScreenWidth(1920), new Dimension(1400, 900));
        assertEquals(MainWindowSizePolicy.preferredSizeForScreenWidth(1280), new Dimension(1280, 800));
        assertEquals(MainWindowSizePolicy.preferredSizeForScreenWidth(1279), new Dimension(1100, 700));
    }

    @Test
    public void shouldKeepMinimumSizeWithinEveryConnectedDisplay() {
        Dimension minimumSize = MainWindowSizePolicy.minimumSizeForDisplayAreas(
                new Dimension(3840, 2120),
                new Dimension(1920, 1040));

        assertEquals(minimumSize, new Dimension(1100, 700));
    }

    @Test
    public void shouldReduceMinimumHeightForScaledSecondaryDisplay() {
        Dimension minimumSize = MainWindowSizePolicy.minimumSizeForDisplayAreas(
                new Dimension(2560, 1400),
                new Dimension(1280, 680));

        assertEquals(minimumSize, new Dimension(1100, 680));
    }

    @Test
    public void shouldConstrainOversizedRestoredWindowToUsableDisplayArea() {
        Dimension actualSize = MainWindowSizePolicy.constrainToUsableArea(
                new Dimension(1920, 1200),
                new Dimension(1100, 700),
                new Dimension(1920, 1040));

        assertEquals(actualSize, new Dimension(1920, 1040));
    }

    @Test
    public void shouldStartMaximizedOnSmallScreensOnly() {
        assertTrue(MainWindowSizePolicy.shouldStartMaximized(1366));
        assertFalse(MainWindowSizePolicy.shouldStartMaximized(1367));
    }
}
