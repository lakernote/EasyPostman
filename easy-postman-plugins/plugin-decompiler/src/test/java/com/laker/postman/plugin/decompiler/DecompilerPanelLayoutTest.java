package com.laker.postman.plugin.decompiler;

import static com.laker.postman.plugin.decompiler.DecompilerI18n.t;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

import com.formdev.flatlaf.FlatClientProperties;
import com.laker.postman.util.I18nUtil;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.util.Locale;
import javax.swing.JButton;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.border.EmptyBorder;
import org.testng.annotations.Test;

public class DecompilerPanelLayoutTest {

    @Test
    public void fileAndCodeActionsShouldAvoidTruncatedTextAcrossLocales() {
        Locale originalLocale = I18nUtil.currentLocale();
        try {
            assertActionsUseCompactTextAndIconButtons(Locale.CHINESE);
            assertActionsUseCompactTextAndIconButtons(Locale.ENGLISH);
        } finally {
            I18nUtil.setLocale(originalLocale);
        }
    }

    @Test
    public void fileTreeShouldUseSinglePlainScrollPaneCard() {
        DecompilerPanel panel = layoutPanel(Locale.ENGLISH);

        JScrollPane treeScrollPane = findScrollPaneWithView(panel, JTree.class);

        assertNotNull(treeScrollPane, "file tree scroll pane should be present");
        assertTrue(treeScrollPane.getBorder() instanceof EmptyBorder,
                "file tree should keep the plain tree card border instead of stacking a framed card border");
    }

    @Test
    public void fileTreeToolbarActionsShouldStayGroupedAtLeadingEdge() {
        DecompilerPanel panel = layoutPanel(Locale.ENGLISH);

        JButton sortByNameButton = findButtonByTooltip(panel, t(MessageKeys.TOOLBOX_DECOMPILER_SORT_BY_NAME));
        JButton sortBySizeButton = findButtonByTooltip(panel, t(MessageKeys.TOOLBOX_DECOMPILER_SORT_BY_SIZE));

        assertNotNull(sortByNameButton, "sort by name button should be present");
        assertNotNull(sortBySizeButton, "sort by size button should be present");
        assertTrue(sortByNameButton.getX() <= 120,
                "sort by name button should stay near the leading toolbar actions, actual x="
                        + sortByNameButton.getX());
        assertTrue(sortBySizeButton.getX() <= 160,
                "sort by size button should stay near the leading toolbar actions, actual x="
                        + sortBySizeButton.getX());
    }

    private static void assertActionsUseCompactTextAndIconButtons(Locale locale) {
        DecompilerPanel panel = layoutPanel(locale);

        JButton browseButton = findButtonByText(panel, t(MessageKeys.TOOLBOX_DECOMPILER_BROWSE));
        assertNotNull(browseButton, locale + " browse button should be present");
        assertEquals(browseButton.getToolTipText(), t(MessageKeys.TOOLBOX_DECOMPILER_SELECT_FILE_PROMPT),
                locale + " browse tooltip should keep the full file-selection action");
        assertTrue(!browseButton.getText().contains("..."),
                locale + " browse label should use short text instead of an ellipsis-prone label");
        assertTextFitsPreferredWidth(browseButton, locale + " browse button");
        String browseStyle = String.valueOf(browseButton.getClientProperty(FlatClientProperties.STYLE));
        assertTrue(browseStyle.contains("arc: 6"), locale + " browse button should use compact rounded styling");

        assertIconOnlyToolbarButton(panel, t(MessageKeys.TOOLBOX_DECOMPILER_CLEAR), locale);
        assertIconOnlyToolbarButton(panel, t(MessageKeys.TOOLBOX_DECOMPILER_COPY_CODE), locale);
    }

    private static void assertIconOnlyToolbarButton(DecompilerPanel panel, String tooltip, Locale locale) {
        JButton button = findButtonByTooltip(panel, tooltip);
        assertNotNull(button, locale + " button not found: " + tooltip);
        assertTrue(button.getText() == null || button.getText().isBlank(),
                locale + " " + tooltip + " should use icon-only text");
        assertNotNull(button.getIcon(), locale + " " + tooltip + " should expose an SVG icon");
        assertTrue(FlatClientProperties.BUTTON_TYPE_TOOLBAR_BUTTON.equals(
                        button.getClientProperty(FlatClientProperties.BUTTON_TYPE)),
                locale + " " + tooltip + " should use the shared toolbar button style");
        Dimension expectedSize = new Dimension(28, 28);
        assertEquals(button.getPreferredSize(), expectedSize,
                locale + " " + tooltip + " should keep the compact toolbar preferred size");
    }

    private static void assertTextFitsPreferredWidth(JButton button, String label) {
        int textWidth = button.getFontMetrics(button.getFont()).stringWidth(button.getText());
        int iconWidth = button.getIcon() == null ? 0 : button.getIcon().getIconWidth();
        int requiredWidth = textWidth + iconWidth + button.getIconTextGap() + 16;
        assertTrue(button.getPreferredSize().width >= requiredWidth,
                label + " preferred width " + button.getPreferredSize().width
                        + " should fit content width " + requiredWidth);
    }

    private static DecompilerPanel layoutPanel(Locale locale) {
        I18nUtil.setLocale(locale);
        DecompilerPanel panel = new DecompilerPanel();
        panel.setSize(new Dimension(1320, 760));
        layoutRecursively(panel);
        return panel;
    }

    private static JButton findButtonByText(Component component, String text) {
        if (component instanceof JButton button && text.equals(button.getText())) {
            return button;
        }
        if (component instanceof Container container) {
            for (Component child : container.getComponents()) {
                JButton found = findButtonByText(child, text);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
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

    private static JScrollPane findScrollPaneWithView(Component component, Class<?> viewType) {
        if (component instanceof JScrollPane scrollPane && viewType.isInstance(scrollPane.getViewport().getView())) {
            return scrollPane;
        }
        if (component instanceof Container container) {
            for (Component child : container.getComponents()) {
                JScrollPane found = findScrollPaneWithView(child, viewType);
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
