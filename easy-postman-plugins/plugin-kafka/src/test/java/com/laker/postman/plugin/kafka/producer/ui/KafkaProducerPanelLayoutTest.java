package com.laker.postman.plugin.kafka.producer.ui;

import static com.laker.postman.plugin.kafka.KafkaI18n.t;
import static org.testng.Assert.assertEquals;
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

public class KafkaProducerPanelLayoutTest {

    @Test
    public void sendActionShouldUseSharedCompactPrimaryButtonAcrossLocales() {
        Locale originalLocale = I18nUtil.currentLocale();
        try {
            assertSendActionUsesSharedCompactPrimaryButton(Locale.CHINESE);
            assertSendActionUsesSharedCompactPrimaryButton(Locale.ENGLISH);
        } finally {
            I18nUtil.setLocale(originalLocale);
        }
    }

    private static void assertSendActionUsesSharedCompactPrimaryButton(Locale locale) {
        I18nUtil.setLocale(locale);
        KafkaProducerPanel panel = new KafkaProducerPanel(() -> {
        });
        panel.setSize(new Dimension(1320, 760));
        layoutRecursively(panel);

        JButton sendButton = panel.sendBtn;
        assertEquals(sendButton.getText(), t(MessageKeys.TOOLBOX_KAFKA_SEND),
                locale + " send button should use the localized compact action text");
        assertEquals(sendButton.getToolTipText(), t(MessageKeys.TOOLBOX_KAFKA_SEND),
                locale + " send button tooltip should keep the full action");
        assertCompactPrimaryActionButton(sendButton, locale);
    }

    private static void assertCompactPrimaryActionButton(JButton button, Locale locale) {
        assertEquals(button.getClass().getName(),
                "com.laker.postman.common.component.button.CompactPrimaryButton",
                locale + " send button should use the shared compact primary action component");
        assertEquals(button.getPreferredSize().height, 30,
                locale + " compact primary action should not use the larger regular primary height");
        assertEquals(button.getHeight(), 30,
                locale + " compact primary action should keep its rendered height");
        String style = String.valueOf(button.getClientProperty(FlatClientProperties.STYLE));
        assertTrue(style.contains("arc: 6"), locale + " compact primary action should be a rounded rectangle");
        assertTrue(style.contains("margin: 2,8,2,8"),
                locale + " compact primary action should use compact margins");
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
