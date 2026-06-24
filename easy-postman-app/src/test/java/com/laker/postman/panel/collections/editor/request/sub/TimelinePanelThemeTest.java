package com.laker.postman.panel.collections.editor.request.sub;

import com.laker.postman.common.constants.ModernColors;
import com.laker.postman.common.constants.ThemeColors;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.swing.*;
import java.awt.*;
import java.util.Map;

import static com.laker.postman.test.ThemeTokenTestSupport.remember;
import static com.laker.postman.test.ThemeTokenTestSupport.restore;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotEquals;

public class TimelinePanelThemeTest {
    private Map<String, Object> previousThemeTokens;

    @BeforeMethod
    public void rememberThemeTokens() {
        previousThemeTokens = remember(
                ThemeColors.SURFACE,
                ThemeColors.BACKGROUND,
                ThemeColors.BORDER_LIGHT,
                ThemeColors.TEXT_PRIMARY,
                ThemeColors.TEXT_SECONDARY,
                ThemeColors.TEXT_HINT,
                ThemeColors.DIVIDER,
                ThemeColors.PRIMARY,
                ThemeColors.SECONDARY,
                ThemeColors.ACCENT,
                ThemeColors.WARNING_DARK,
                ThemeColors.ERROR,
                ThemeColors.SUCCESS,
                ThemeColors.NEUTRAL
        );
    }

    @AfterMethod
    public void tearDown() {
        restore(previousThemeTokens);
    }

    @Test
    public void surfaceColorsShouldUseSemanticThemeTokens() {
        Color surface = new Color(12, 13, 14);
        Color background = new Color(22, 23, 24);
        Color border = new Color(32, 33, 34);
        UIManager.put(ThemeColors.SURFACE, surface);
        UIManager.put(ThemeColors.BACKGROUND, background);
        UIManager.put(ThemeColors.BORDER_LIGHT, border);

        assertEquals(TimelineTheme.panelBackground(), surface);
        Color sectionBackground = ModernColors.blendColors(
                surface,
                background,
                ModernColors.isDarkTheme() ? 0.32f : 0.18f
        );
        assertEquals(TimelineTheme.infoBackground(), sectionBackground);
        assertEquals(TimelineTheme.barAreaBackground(), sectionBackground);
        assertEquals(TimelineTheme.infoBorder(),
                ModernColors.withAlpha(border, ModernColors.isDarkTheme() ? 150 : 135));
    }

    @Test
    public void trackColorsShouldUseSemanticSurfaceAndDividerTokens() {
        Color surface = new Color(244, 245, 246);
        Color background = new Color(34, 35, 36);
        Color divider = new Color(214, 215, 216);
        UIManager.put(ThemeColors.SURFACE, surface);
        UIManager.put(ThemeColors.BACKGROUND, background);
        UIManager.put(ThemeColors.DIVIDER, divider);

        Color base = ModernColors.isDarkTheme() ? background : surface;
        assertEquals(TimelineTheme.barTrackBackground(),
                ModernColors.blendColors(base, divider, ModernColors.isDarkTheme() ? 0.18f : 0.12f));
        assertEquals(TimelineTheme.barTrackBorder(),
                ModernColors.withAlpha(divider, ModernColors.isDarkTheme() ? 115 : 90));
        assertEquals(TimelineTheme.gridLine(),
                ModernColors.withAlpha(divider, ModernColors.isDarkTheme() ? 55 : 45));
    }

    @Test
    public void textColorsShouldUseSemanticThemeTokens() {
        Color label = new Color(42, 43, 44);
        Color info = new Color(52, 53, 54);
        Color description = new Color(62, 63, 64);
        Color separator = new Color(72, 73, 74);
        Color error = new Color(82, 83, 84);
        UIManager.put(ThemeColors.TEXT_PRIMARY, label);
        UIManager.put(ThemeColors.TEXT_SECONDARY, info);
        UIManager.put(ThemeColors.TEXT_HINT, description);
        UIManager.put(ThemeColors.DIVIDER, separator);
        UIManager.put(ThemeColors.ERROR, error);

        assertEquals(TimelineTheme.labelText(), label);
        assertEquals(TimelineTheme.infoText(), info);
        assertEquals(TimelineTheme.descriptionText(), description);
        assertEquals(TimelineTheme.separator(),
                ModernColors.withAlpha(separator, ModernColors.isDarkTheme() ? 105 : 90));
        assertEquals(TimelineTheme.certificateWarning(), error);
    }

    @Test
    public void barColorsShouldUseStageSpecificSemanticColors() {
        Color primary = new Color(11, 12, 13);
        Color secondary = new Color(21, 22, 23);
        Color accent = new Color(31, 32, 33);
        Color warningDark = new Color(51, 52, 53);
        Color success = new Color(61, 62, 63);
        Color neutral = new Color(81, 82, 83);
        Color textSecondary = new Color(91, 92, 93);
        UIManager.put(ThemeColors.PRIMARY, primary);
        UIManager.put(ThemeColors.SECONDARY, secondary);
        UIManager.put(ThemeColors.ACCENT, accent);
        UIManager.put(ThemeColors.WARNING_DARK, warningDark);
        UIManager.put(ThemeColors.SUCCESS, success);
        UIManager.put(ThemeColors.NEUTRAL, neutral);
        UIManager.put(ThemeColors.TEXT_SECONDARY, textSecondary);

        Color requestSend = neutral;
        Color ttfb = warningDark;
        Color contentDownload = ModernColors.blendColors(
                ModernColors.isDarkTheme()
                        ? new Color(0x4D, 0xB6, 0xAC)
                        : new Color(0x0D, 0x94, 0x88),
                textSecondary,
                ModernColors.isDarkTheme() ? 0.10f : 0.06f
        );

        assertEquals(TimelineTheme.barColors(), new Color[]{
                primary, secondary, accent, requestSend, ttfb, contentDownload
        });
        assertEquals(TimelineTheme.dnsLookupColor(), primary);
        assertEquals(TimelineTheme.socketConnectionColor(), secondary);
        assertEquals(TimelineTheme.tlsHandshakeColor(), accent);
        assertEquals(TimelineTheme.requestSendColor(), requestSend);
        assertEquals(TimelineTheme.timeToFirstByteColor(), ttfb);
        assertEquals(TimelineTheme.contentDownloadColor(), contentDownload);
        assertNotEquals(TimelineTheme.requestSendColor(), TimelineTheme.timeToFirstByteColor(),
                "Request Send and TTFB should not both read as amber stages");
        assertNotEquals(TimelineTheme.requestSendColor(), warningDark,
                "Request Send is transfer metadata, not a warning state");
        assertNotEquals(TimelineTheme.contentDownloadColor(), success,
                "Content Download is transfer timing, not a success state");
        assertNotEquals(TimelineTheme.contentDownloadColor(), accent,
                "Content Download should remain distinct from the SSL accent stage");
    }
}
