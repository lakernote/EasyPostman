package com.laker.postman.common.component;

import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.extras.components.FlatTextField;
import com.laker.postman.util.UiI18n;
import com.laker.postman.util.UiMessageKeys;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.testng.annotations.Test;

import javax.swing.*;
import java.awt.*;
import java.lang.reflect.Field;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class SearchReplacePanelLayoutTest {

    @Test
    public void shouldUseCompactMatchCountText() {
        assertEquals(UiI18n.get(UiMessageKeys.SEARCH_STATUS_COUNT, 1, 1_000), "1/1000");
    }

    @Test
    public void shouldUseCompactOverlayMetrics() throws Exception {
        Dimension[] collapsedSize = new Dimension[1];
        Dimension[] expandedSize = new Dimension[1];

        SwingUtilities.invokeAndWait(() -> {
            SearchReplacePanel panel = new SearchReplacePanel(new RSyntaxTextArea(), true);
            try {
                SearchTextField searchField = getField(panel, "searchField", SearchTextField.class);
                FlatTextField replaceField = getField(panel, "replaceField", FlatTextField.class);
                AbstractButton toggleReplaceBtn = getField(panel, "toggleReplaceBtn", AbstractButton.class);
                JPanel replacePanel = getField(panel, "replacePanel", JPanel.class);
                JLabel statusLabel = getField(panel, "statusLabel", JLabel.class);

                assertEquals(searchField.getPreferredSize(), new Dimension(180, 30));
                assertEquals(searchField.getMaximumSize(), new Dimension(200, 30));
                assertEquals(replaceField.getPreferredSize(), new Dimension(180, 30));
                assertEquals(replaceField.getMaximumSize(), new Dimension(200, 30));
                assertEquals(toggleReplaceBtn.getPreferredSize(), new Dimension(22, 22));
                assertEquals(toggleReplaceBtn.getClientProperty(FlatClientProperties.BUTTON_TYPE),
                        FlatClientProperties.BUTTON_TYPE_TOOLBAR_BUTTON);
                assertFalse(toggleReplaceBtn.isSelected());
                assertEquals(statusLabel.getPreferredSize(), new Dimension(82, 22));
                assertEquals(statusLabel.getText(), "");
                assertFalse(replacePanel.isVisible());

                collapsedSize[0] = panel.getPreferredSize();
                replacePanel.setVisible(true);
                expandedSize[0] = panel.getPreferredSize();
            } catch (ReflectiveOperationException e) {
                throw new AssertionError(e);
            }
        });

        assertEquals(collapsedSize[0].height, 40);
        assertEquals(expandedSize[0].height, 74);
        assertTrue(expandedSize[0].width <= 395,
                "Search overlay should stay compact inside code editors: " + expandedSize[0]);
    }

    private static <T> T getField(SearchReplacePanel panel, String fieldName, Class<T> fieldType)
            throws ReflectiveOperationException {
        Field field = SearchReplacePanel.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        return fieldType.cast(field.get(panel));
    }
}
