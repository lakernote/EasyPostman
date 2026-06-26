package com.laker.postman.panel.collections.editor.request.sub;

import com.laker.postman.util.I18nUtil;
import com.laker.postman.test.AbstractSwingUiTest;
import org.testng.annotations.Test;

import javax.swing.*;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.lang.reflect.InvocationTargetException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

public class ResponseHeadersPanelTest extends AbstractSwingUiTest {

    @Test(description = "Response header name column should grow for long header names without consuming the value column")
    public void shouldSizeNameColumnForLongHeaderNames() throws Exception {
        ResponseHeadersPanel panel = createPanelOnEdt();
        setHeadersOnEdt(panel, headers(
                "content-type", "application/json",
                "X-Extremely-Long-Response-Header-Name", "value"
        ));

        JTable table = findResponseHeadersTable(panel);
        TableColumn nameColumn = table.getColumnModel().getColumn(0);
        TableColumn valueColumn = table.getColumnModel().getColumn(1);

        assertTrue(nameColumn.getPreferredWidth() >= 260,
                "long response header names should be readable by default");
        assertTrue(nameColumn.getPreferredWidth() <= 320,
                "name column should stay bounded so values remain prominent");
        assertTrue(valueColumn.getMinWidth() >= 240,
                "value column should keep a practical minimum width");
    }

    @Test(description = "Response header name column should not waste space when all header names are short")
    public void shouldKeepNameColumnCompactForShortHeaderNames() throws Exception {
        ResponseHeadersPanel panel = createPanelOnEdt();
        setHeadersOnEdt(panel, headers(
                "date", "Fri, 26 Jun 2026 13:15:27 GMT",
                "server", "awselb/2.0"
        ));

        JTable table = findResponseHeadersTable(panel);
        TableColumn nameColumn = table.getColumnModel().getColumn(0);

        assertEquals(nameColumn.getPreferredWidth(), 140,
                "short response header names should keep the name column compact");
    }

    @Test(description = "Response header table column titles should follow the active application locale")
    public void shouldLocalizeResponseHeaderColumnTitles() throws Exception {
        Locale originalLocale = I18nUtil.currentLocale();
        try {
            I18nUtil.setLocale(Locale.CHINESE);
            ResponseHeadersPanel panel = createPanelOnEdt();

            JTable table = findResponseHeadersTable(panel);

            assertEquals(table.getColumnName(0), "名称");
            assertEquals(table.getColumnName(1), "值");
        } finally {
            I18nUtil.setLocale(originalLocale);
        }
    }

    private static ResponseHeadersPanel createPanelOnEdt() throws Exception {
        AtomicReference<ResponseHeadersPanel> panelRef = new AtomicReference<>();
        runOnEdt(() -> panelRef.set(new ResponseHeadersPanel()));
        return panelRef.get();
    }

    private static void setHeadersOnEdt(ResponseHeadersPanel panel, Map<String, List<String>> headers) throws Exception {
        runOnEdt(() -> panel.setHeaders(headers));
    }

    private static JTable findResponseHeadersTable(ResponseHeadersPanel panel) throws Exception {
        AtomicReference<JTable> tableRef = new AtomicReference<>();
        runOnEdt(() -> tableRef.set(findTable(panel)));
        JTable table = tableRef.get();
        assertNotNull(table, "response headers panel should contain a JTable");
        return table;
    }

    private static JTable findTable(Component component) {
        if (component instanceof JTable table) {
            return table;
        }
        if (!(component instanceof Container container)) {
            return null;
        }
        for (Component child : container.getComponents()) {
            JTable table = findTable(child);
            if (table != null) {
                return table;
            }
        }
        return null;
    }

    private static void runOnEdt(Runnable action) throws Exception {
        if (SwingUtilities.isEventDispatchThread()) {
            action.run();
            return;
        }
        try {
            SwingUtilities.invokeAndWait(action);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof Exception exception) {
                throw exception;
            }
            if (cause instanceof Error error) {
                throw error;
            }
            throw e;
        }
    }

    private static Map<String, List<String>> headers(String firstName, String firstValue,
                                                     String secondName, String secondValue) {
        Map<String, List<String>> headers = new LinkedHashMap<>();
        headers.put(firstName, List.of(firstValue));
        headers.put(secondName, List.of(secondValue));
        return headers;
    }
}
