package com.laker.postman.panel.collections.editor.request.sub;

import com.laker.postman.common.component.table.FormDataTablePanel;
import com.laker.postman.common.component.table.FormUrlencodedTablePanel;
import com.laker.postman.request.defaults.GeneratedRequestHeaderPolicy;
import com.laker.postman.request.model.HttpFormData;
import com.laker.postman.request.model.HttpFormUrlencoded;
import com.laker.postman.request.model.HttpHeader;
import com.laker.postman.request.model.HttpParam;
import com.laker.postman.test.AbstractSwingUiTest;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;
import org.testng.annotations.Test;

import javax.swing.JTable;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.Container;
import java.util.Locale;
import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

public class RequestDescriptionColumnTablePanelTest extends AbstractSwingUiTest {

    @Test
    public void paramsTableShouldExposeDescriptionColumnAndPersistValue() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            ParamsTablePanel panel = new ParamsTablePanel();
            panel.setParamsList(List.of(new HttpParam(true, "page", "1", "pagination page")));

            assertDescriptionColumn(panel.getTable(), 3);
            assertEquals(panel.getParamsList().get(0).getDescription(), "pagination page");
        });
    }

    @Test
    public void headersTableShouldExposeDescriptionColumnAndPersistValue() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            EasyRequestHttpHeadersPanel panel = new EasyRequestHttpHeadersPanel(
                    GeneratedRequestHeaderPolicy.standard("EasyPostman/test"));
            panel.setHeadersList(List.of(new HttpHeader(true, "X-Trace", "1", "trace id")));

            JTable table = findTable(panel);
            assertNotNull(table);
            assertDescriptionColumn(table, 3);
            HttpHeader header = panel.getHeadersList().stream()
                    .filter(value -> "X-Trace".equals(value.getKey()))
                    .findFirst()
                    .orElseThrow();
            assertEquals(header.getDescription(), "trace id");
        });
    }

    @Test
    public void formDataTableShouldExposeDescriptionColumnAndPersistValue() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            FormDataTablePanel panel = new FormDataTablePanel(false, false);
            panel.setFormDataList(List.of(new HttpFormData(
                    true,
                    "file",
                    HttpFormData.TYPE_FILE,
                    "/tmp/demo.txt",
                    "upload target"
            )));

            assertDescriptionColumn(panel.getTable(), 4);
            assertEquals(panel.getFormDataList().get(0).getDescription(), "upload target");
        });
    }

    @Test
    public void formUrlencodedTableShouldExposeDescriptionColumnAndPersistValue() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            FormUrlencodedTablePanel panel = new FormUrlencodedTablePanel(false, false);
            panel.setFormDataList(List.of(new HttpFormUrlencoded(true, "token", "abc", "login token")));

            assertDescriptionColumn(panel.getTable(), 3);
            assertEquals(panel.getFormDataList().get(0).getDescription(), "login token");
        });
    }

    @Test
    public void requestParameterLabelsShouldFollowCurrentLocale() throws Exception {
        Locale originalLocale = I18nUtil.currentLocale();
        try {
            I18nUtil.setLocale(Locale.CHINESE);
            SwingUtilities.invokeAndWait(() -> {
                assertColumnHeaders(
                        new ParamsTablePanel().getTable(),
                        "",
                        I18nUtil.getMessage(MessageKeys.REQUEST_TABLE_COLUMN_KEY),
                        I18nUtil.getMessage(MessageKeys.REQUEST_TABLE_COLUMN_VALUE),
                        I18nUtil.getMessage(MessageKeys.REQUEST_TABLE_COLUMN_DESCRIPTION),
                        ""
                );
                assertColumnHeaders(
                        new EasyRequestHeadersTablePanel().getTable(),
                        "",
                        I18nUtil.getMessage(MessageKeys.REQUEST_TABLE_COLUMN_KEY),
                        I18nUtil.getMessage(MessageKeys.REQUEST_TABLE_COLUMN_VALUE),
                        I18nUtil.getMessage(MessageKeys.REQUEST_TABLE_COLUMN_DESCRIPTION),
                        ""
                );
                assertColumnHeaders(
                        new FormDataTablePanel(false, false).getTable(),
                        "",
                        I18nUtil.getMessage(MessageKeys.REQUEST_TABLE_COLUMN_KEY),
                        I18nUtil.getMessage(MessageKeys.REQUEST_TABLE_COLUMN_TYPE),
                        I18nUtil.getMessage(MessageKeys.REQUEST_TABLE_COLUMN_VALUE),
                        I18nUtil.getMessage(MessageKeys.REQUEST_TABLE_COLUMN_DESCRIPTION),
                        ""
                );
                assertColumnHeaders(
                        new FormUrlencodedTablePanel(false, false).getTable(),
                        "",
                        I18nUtil.getMessage(MessageKeys.REQUEST_TABLE_COLUMN_KEY),
                        I18nUtil.getMessage(MessageKeys.REQUEST_TABLE_COLUMN_VALUE),
                        I18nUtil.getMessage(MessageKeys.REQUEST_TABLE_COLUMN_DESCRIPTION),
                        ""
                );

                EasyRequestParamsPanel paramsPanel = new EasyRequestParamsPanel();
                JLabel titleLabel = findLabel(paramsPanel, I18nUtil.getMessage(MessageKeys.REQUEST_PARAMS_TITLE));
                assertNotNull(titleLabel);
            });
        } finally {
            I18nUtil.setLocale(originalLocale);
        }
    }

    private static void assertDescriptionColumn(JTable table, int descriptionColumn) {
        assertEquals(
                table.getColumnName(descriptionColumn),
                I18nUtil.getMessage(MessageKeys.REQUEST_TABLE_COLUMN_DESCRIPTION)
        );
    }

    private static void assertColumnHeaders(JTable table, String... expectedHeaders) {
        assertEquals(table.getColumnCount(), expectedHeaders.length);
        for (int i = 0; i < expectedHeaders.length; i++) {
            assertEquals(table.getColumnName(i), expectedHeaders[i], "column " + i);
        }
    }

    private static JTable findTable(Component component) {
        if (component instanceof JTable table) {
            return table;
        }
        if (component instanceof Container container) {
            for (Component child : container.getComponents()) {
                JTable table = findTable(child);
                if (table != null) {
                    return table;
                }
            }
        }
        return null;
    }

    private static JLabel findLabel(Component component, String text) {
        if (component instanceof JLabel label && text.equals(label.getText())) {
            return label;
        }
        if (component instanceof Container container) {
            for (Component child : container.getComponents()) {
                JLabel label = findLabel(child, text);
                if (label != null) {
                    return label;
                }
            }
        }
        return null;
    }
}
