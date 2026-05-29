package com.laker.postman.common.component;

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
import static org.testng.Assert.assertNotNull;

public class StepIndicatorTest {
    private Map<String, Object> previousThemeTokens;

    @BeforeMethod
    public void rememberThemeTokens() {
        previousThemeTokens = remember(ThemeColors.BORDER_LIGHT);
    }

    @AfterMethod
    public void tearDown() {
        restore(previousThemeTokens);
    }

    @Test
    public void inactiveStepCircleShouldUseSemanticBorderLightColor() {
        Color inactiveBackground = new Color(21, 22, 23);
        UIManager.put(ThemeColors.BORDER_LIGHT, inactiveBackground);

        StepIndicator indicator = new StepIndicator();
        JLabel secondStepCircle = findLabel(indicator, "2");

        assertNotNull(secondStepCircle);
        assertEquals(secondStepCircle.getBackground(), inactiveBackground);
    }

    private JLabel findLabel(Container container, String text) {
        for (Component component : container.getComponents()) {
            if (component instanceof JLabel label && text.equals(label.getText())) {
                return label;
            }
            if (component instanceof Container childContainer) {
                JLabel result = findLabel(childContainer, text);
                if (result != null) {
                    return result;
                }
            }
        }
        return null;
    }
}
