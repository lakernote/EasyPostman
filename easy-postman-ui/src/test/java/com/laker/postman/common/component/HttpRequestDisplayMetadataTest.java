package com.laker.postman.common.component;

import com.laker.postman.common.constants.ThemeColors;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.swing.UIManager;
import java.awt.Color;
import java.util.Map;

import static com.laker.postman.test.ThemeTokenTestSupport.remember;
import static com.laker.postman.test.ThemeTokenTestSupport.restore;
import static org.testng.Assert.assertEquals;

public class HttpRequestDisplayMetadataTest {
    private static final String[] THEME_KEYS = {
            ThemeColors.HTTP_METHOD_GET,
            ThemeColors.HTTP_METHOD_POST,
            ThemeColors.HTTP_METHOD_DEFAULT,
            ThemeColors.HTTP_PROTOCOL_WS,
            ThemeColors.HTTP_PROTOCOL_SSE
    };

    private Map<String, Object> previousThemeTokens;

    @BeforeMethod
    public void rememberThemeTokens() {
        previousThemeTokens = remember(THEME_KEYS);
    }

    @AfterMethod
    public void restoreThemeTokens() {
        restore(previousThemeTokens);
    }

    @Test
    public void shouldResolveMethodDisplayColorsFromDedicatedThemeTokens() {
        UIManager.put(ThemeColors.HTTP_METHOD_GET, new Color(1, 35, 255));
        UIManager.put(ThemeColors.HTTP_METHOD_POST, new Color(80, 81, 82));
        UIManager.put(ThemeColors.HTTP_METHOD_DEFAULT, new Color(90, 91, 92));

        assertEquals(HttpRequestDisplayMetadata.methodColorHex("GET"), "#0123ff");
        assertEquals(HttpRequestDisplayMetadata.methodColor("post"), new Color(80, 81, 82));
        assertEquals(HttpRequestDisplayMetadata.methodColorHex("CUSTOM"), "#5a5b5c");
    }

    @Test
    public void shouldResolveProtocolDisplayColorsFromDedicatedThemeTokens() {
        UIManager.put(ThemeColors.HTTP_PROTOCOL_WS, new Color(10, 20, 30));
        UIManager.put(ThemeColors.HTTP_PROTOCOL_SSE, new Color(40, 50, 60));

        assertEquals(HttpRequestDisplayMetadata.protocolColorHex("WS"), "#0a141e");
        assertEquals(HttpRequestDisplayMetadata.protocolColor("websocket"), new Color(10, 20, 30));
        assertEquals(HttpRequestDisplayMetadata.protocolColorHex("SSE"), "#28323c");
    }

    @Test
    public void shouldProvideCompactMethodLabelsForTreeRows() {
        assertEquals(HttpRequestDisplayMetadata.methodLabel("DELETE"), "DEL");
        assertEquals(HttpRequestDisplayMetadata.methodLabel("options"), "OPT");
        assertEquals(HttpRequestDisplayMetadata.methodLabel("PATCH"), "PAT");
        assertEquals(HttpRequestDisplayMetadata.methodLabel("TRACE"), "TRC");
        assertEquals(HttpRequestDisplayMetadata.methodLabel("GET"), "GET");
        assertEquals(HttpRequestDisplayMetadata.methodLabel(null), "");
    }
}
