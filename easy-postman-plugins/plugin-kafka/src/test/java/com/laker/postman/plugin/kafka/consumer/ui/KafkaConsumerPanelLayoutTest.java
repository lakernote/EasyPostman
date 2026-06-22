package com.laker.postman.plugin.kafka.consumer.ui;

import static com.laker.postman.plugin.kafka.KafkaI18n.t;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

import com.formdev.flatlaf.FlatClientProperties;
import com.laker.postman.plugin.kafka.MessageKeys;
import com.laker.postman.util.I18nUtil;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.util.Locale;
import javax.swing.JButton;
import org.testng.annotations.Test;

public class KafkaConsumerPanelLayoutTest {

    @Test
    public void consumeActionsShouldUseShortTextWithFullTooltipsAcrossLocales() {
        Locale originalLocale = I18nUtil.currentLocale();
        try {
            assertConsumeActionsUseShortText(Locale.CHINESE);
            assertConsumeActionsUseShortText(Locale.ENGLISH);
        } finally {
            I18nUtil.setLocale(originalLocale);
        }
    }

    private static void assertConsumeActionsUseShortText(Locale locale) {
        I18nUtil.setLocale(locale);
        KafkaConsumerPanel panel = new KafkaConsumerPanel(() -> {
        }, () -> {
        }, () -> {
        }, () -> {
        });
        panel.setSize(new Dimension(1320, 760));
        layoutRecursively(panel);

        JButton startButton = findButtonByTooltip(panel, t(MessageKeys.TOOLBOX_KAFKA_START_CONSUME));
        assertNotNull(startButton, locale + " start consume button should keep full tooltip");
        assertEquals(startButton.getText(), t(MessageKeys.TOOLBOX_KAFKA_START_CONSUME_SHORT),
                locale + " start consume button should use compact text");
        assertCompactPrimaryActionButton(startButton, locale);

        JButton stopButton = findButtonByTooltip(panel, t(MessageKeys.TOOLBOX_KAFKA_STOP_CONSUME));
        assertNotNull(stopButton, locale + " stop consume button should keep full tooltip");
        assertEquals(stopButton.getText(), t(MessageKeys.TOOLBOX_KAFKA_STOP_CONSUME_SHORT),
                locale + " stop consume button should use compact text");
    }

    private static void assertCompactPrimaryActionButton(JButton button, Locale locale) {
        assertEquals(button.getClass().getName(),
                "com.laker.postman.common.component.button.CompactPrimaryButton",
                locale + " start consume button should use the shared compact primary action component");
        assertEquals(button.getPreferredSize().height, 30,
                locale + " compact primary action should not use the larger regular primary height");
        assertEquals(button.getHeight(), 30,
                locale + " compact primary action should keep its rendered height");
        String style = String.valueOf(button.getClientProperty(FlatClientProperties.STYLE));
        assertTrue(style.contains("arc: 6"), locale + " compact primary action should be a rounded rectangle");
        assertTrue(style.contains("margin: 2,8,2,8"),
                locale + " compact primary action should use compact margins");
    }

    private static JButton findButtonByTooltip(Component component, String tooltip) {
        if (component instanceof JButton button && tooltip.equals(button.getToolTipText())) {
            return button;
        }
        if (component instanceof Container container) {
            for (Component child : container.getComponents()) {
                JButton found = findButtonByTooltip(child, tooltip);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private static void layoutRecursively(Component component) {
        if (component instanceof Container container) {
            container.doLayout();
            for (Component child : container.getComponents()) {
                layoutRecursively(child);
            }
        }
    }
}
