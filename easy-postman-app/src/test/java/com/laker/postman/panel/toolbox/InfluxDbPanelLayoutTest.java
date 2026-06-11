package com.laker.postman.panel.toolbox;

import com.laker.postman.test.AbstractSwingUiTest;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;
import org.testng.annotations.Test;

import javax.swing.JButton;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;

import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

public class InfluxDbPanelLayoutTest extends AbstractSwingUiTest {

    @Test
    public void v1ConnectionFieldsShouldStayNearContentWidthInWideContainer() {
        InfluxDbPanel panel = new InfluxDbPanel();
        panel.setSize(new Dimension(1800, 900));
        layoutRecursively(panel);

        JButton reloadMetadataButton = findButton(
                panel,
                I18nUtil.getMessage(MessageKeys.TOOLBOX_INFLUX_RELOAD_META)
        );

        assertNotNull(reloadMetadataButton);
        Container v1FieldsRow = reloadMetadataButton.getParent();
        assertNotNull(v1FieldsRow);
        assertTrue(v1FieldsRow.getWidth() > 0, "v1 connection row should be laid out");
        assertTrue(v1FieldsRow.getWidth() <= v1FieldsRow.getPreferredSize().width + 16,
                "v1 connection row width " + v1FieldsRow.getWidth()
                        + " should stay near preferred width " + v1FieldsRow.getPreferredSize().width);
    }

    private static JButton findButton(Component component, String text) {
        if (component instanceof JButton button && text.equals(button.getText())) {
            return button;
        }
        if (component instanceof Container container) {
            for (Component child : container.getComponents()) {
                JButton found = findButton(child, text);
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
