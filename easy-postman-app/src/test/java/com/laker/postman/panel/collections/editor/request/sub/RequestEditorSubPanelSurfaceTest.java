package com.laker.postman.panel.collections.editor.request.sub;

import com.laker.postman.common.constants.ThemeColors;
import com.laker.postman.http.request.AppRequestHeaderDefaults;
import com.laker.postman.request.model.RequestItemProtocolEnum;
import com.laker.postman.test.AbstractSwingUiTest;
import org.testng.annotations.Test;

import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.table.JTableHeader;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class RequestEditorSubPanelSurfaceTest extends AbstractSwingUiTest {

    @Test
    public void requestAndResponseSubPanelsShouldUseCardSurface() throws Exception {
        Object previousSurface = UIManager.get(ThemeColors.SURFACE);
        Object previousInputBackground = UIManager.get(ThemeColors.INPUT_BACKGROUND);
        Color surface = new Color(255, 255, 255);
        Color inputBackground = new Color(248, 250, 252);
        UIManager.put(ThemeColors.SURFACE, surface);
        UIManager.put(ThemeColors.INPUT_BACKGROUND, inputBackground);

        try {
            List<JComponent> panels = createPanels();

            for (JComponent panel : panels) {
                assertTrue(panel.isOpaque(), panel.getClass().getSimpleName());
                assertEquals(panel.getBackground(), surface, panel.getClass().getSimpleName());
                assertTablesUseCardSurface(panel, surface, inputBackground);
            }
        } finally {
            UIManager.put(ThemeColors.SURFACE, previousSurface);
            UIManager.put(ThemeColors.INPUT_BACKGROUND, previousInputBackground);
        }
    }

    private void assertTablesUseCardSurface(Container container, Color surface, Color inputBackground) {
        for (Component child : container.getComponents()) {
            if (child instanceof JTable table) {
                assertEquals(table.getBackground(), surface, table.getClass().getSimpleName());
                JTableHeader header = table.getTableHeader();
                if (header != null) {
                    assertEquals(header.getBackground(), inputBackground, table.getClass().getSimpleName());
                }
            }
            if (child instanceof Container childContainer) {
                assertTablesUseCardSurface(childContainer, surface, inputBackground);
            }
        }
    }

    private List<JComponent> createPanels() throws Exception {
        @SuppressWarnings("unchecked")
        List<JComponent>[] holder = new List[1];
        SwingUtilities.invokeAndWait(() -> holder[0] = List.of(
                new RequestBodyPanel(RequestItemProtocolEnum.HTTP),
                new EasyRequestParamsPanel(),
                new EasyRequestHttpHeadersPanel(AppRequestHeaderDefaults.generatedHeaderPolicy()),
                new EasyVariablesPanel(),
                new AuthTabPanel(),
                new RequestSettingsPanel(),
                new ScriptPanel(),
                new ResponseBodyPanel(false),
                new ResponseHeadersPanel(),
                new NetworkLogPanel(),
                new WebSocketResponsePanel(),
                new SSEResponsePanel()
        ));
        return holder[0];
    }
}
