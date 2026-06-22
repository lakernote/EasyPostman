package com.laker.postman.plugin.kafka;

import static com.laker.postman.plugin.kafka.KafkaI18n.t;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

import com.laker.postman.util.I18nUtil;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.util.Locale;
import javax.swing.JTabbedPane;
import org.testng.annotations.Test;

public class KafkaPanelLayoutTest {

    @Test
    public void sideAndWorkTabsShouldUseCompactLocalizedTitlesAcrossLocales() {
        Locale originalLocale = I18nUtil.currentLocale();
        try {
            assertTabsUseCompactTitles(Locale.CHINESE);
            assertTabsUseCompactTitles(Locale.ENGLISH);
        } finally {
            I18nUtil.setLocale(originalLocale);
        }
    }

    private static void assertTabsUseCompactTitles(Locale locale) {
        I18nUtil.setLocale(locale);
        KafkaPanel panel = new KafkaPanel();
        panel.setSize(new Dimension(1320, 760));
        layoutRecursively(panel);

        JTabbedPane sideTabs = findTabbedPaneWithTitle(panel, t(MessageKeys.TOOLBOX_KAFKA_TOPIC_TAB));
        assertNotNull(sideTabs, locale + " topic side tabs should be present");
        assertEquals(sideTabs.getToolTipTextAt(0), t(MessageKeys.TOOLBOX_KAFKA_TOPIC_MANAGEMENT),
                locale + " topic tab tooltip should keep full title");
        assertTrue(sideTabs.getBounds().width <= 260,
                locale + " side tabs should stay within sidebar width instead of expanding for long text");

        JTabbedPane workTabs = findTabbedPaneWithTitle(panel, t(MessageKeys.TOOLBOX_KAFKA_PRODUCER_TAB));
        assertNotNull(workTabs, locale + " work tabs should be present");
        assertEquals(workTabs.getTitleAt(0), t(MessageKeys.TOOLBOX_KAFKA_PRODUCER_TAB),
                locale + " producer tab should use compact title");
        assertEquals(workTabs.getToolTipTextAt(0), t(MessageKeys.TOOLBOX_KAFKA_PRODUCER_TITLE),
                locale + " producer tab tooltip should keep full title");
        assertEquals(workTabs.getTitleAt(1), t(MessageKeys.TOOLBOX_KAFKA_CONSUMER_TAB),
                locale + " consumer tab should use compact title");
        assertEquals(workTabs.getToolTipTextAt(1), t(MessageKeys.TOOLBOX_KAFKA_CONSUMER_TITLE),
                locale + " consumer tab tooltip should keep full title");
    }

    private static JTabbedPane findTabbedPaneWithTitle(Component component, String title) {
        if (component instanceof JTabbedPane tabbedPane) {
            for (int i = 0; i < tabbedPane.getTabCount(); i++) {
                if (title.equals(tabbedPane.getTitleAt(i))) {
                    return tabbedPane;
                }
            }
        }
        if (component instanceof Container container) {
            for (Component child : container.getComponents()) {
                JTabbedPane found = findTabbedPaneWithTitle(child, title);
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
