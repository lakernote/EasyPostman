package com.laker.postman.panel.collections.editor.request.sub;

import com.laker.postman.common.constants.ThemeColors;
import com.laker.postman.service.http.NetworkLogEventStage;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.testng.Assert.assertEquals;

public class NetworkLogStageTest {

    private static final List<String> THEME_TOKEN_KEYS = List.of(
            ThemeColors.ERROR,
            ThemeColors.SUCCESS,
            ThemeColors.PRIMARY,
            ThemeColors.INFO,
            ThemeColors.WARNING,
            ThemeColors.TEXT_PRIMARY
    );

    private Map<String, Object> previousThemeTokens;

    @BeforeMethod
    public void rememberThemeTokens() {
        previousThemeTokens = new HashMap<>();
        for (String key : THEME_TOKEN_KEYS) {
            previousThemeTokens.put(key, UIManager.get(key));
        }
    }

    @AfterMethod
    public void restoreThemeTokens() {
        for (Map.Entry<String, Object> entry : previousThemeTokens.entrySet()) {
            UIManager.put(entry.getKey(), entry.getValue());
        }
    }

    @Test
    public void shouldMapEveryHttpNetworkLogEventStageToUiStage() {
        for (NetworkLogEventStage eventStage : NetworkLogEventStage.values()) {
            assertEquals(NetworkLogStage.fromEventStage(eventStage).name(), eventStage.name(),
                    "Every service-layer network log stage should have a UI rendering stage");
        }
    }

    @Test
    public void shouldUseDefaultUiStageWhenEventStageIsNull() {
        assertEquals(NetworkLogStage.fromEventStage(null), NetworkLogStage.DEFAULT);
    }

    @Test
    public void shouldReadStageColorsFromThemeTokens() {
        Color error = new Color(20, 40, 60);
        Color success = new Color(1, 2, 3);
        Color primary = new Color(220, 160, 100);
        Color info = new Color(10, 11, 12);
        Color warning = new Color(13, 14, 15);
        Color text = new Color(16, 17, 18);
        UIManager.put(ThemeColors.ERROR, error);
        UIManager.put(ThemeColors.SUCCESS, success);
        UIManager.put(ThemeColors.PRIMARY, primary);
        UIManager.put(ThemeColors.INFO, info);
        UIManager.put(ThemeColors.WARNING, warning);
        UIManager.put(ThemeColors.TEXT_PRIMARY, text);

        assertEquals(NetworkLogStage.FAILED.getColor(), error);
        assertEquals(NetworkLogStage.CALL_END.getColor(), success);
        assertEquals(NetworkLogStage.CONNECT_START.getColor(), primary);
        assertEquals(NetworkLogStage.SECURE_CONNECT_START.getColor(), new Color(120, 100, 80));
        assertEquals(NetworkLogStage.REQUEST_HEADERS_START.getColor(), warning);
        assertEquals(NetworkLogStage.RESPONSE_BODY_END.getColor(), info);
        assertEquals(NetworkLogStage.REDIRECT.getColor(), warning);
        assertEquals(NetworkLogStage.DEFAULT.getColor(), text);
    }
}
