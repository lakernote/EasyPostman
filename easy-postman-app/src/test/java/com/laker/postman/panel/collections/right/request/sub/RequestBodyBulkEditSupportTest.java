package com.laker.postman.panel.collections.right.request.sub;

import com.laker.postman.common.component.table.FormDataTablePanel;
import com.laker.postman.common.component.table.FormUrlencodedTablePanel;
import com.laker.postman.model.HttpFormData;
import com.laker.postman.model.HttpFormUrlencoded;
import com.laker.postman.test.AbstractSwingUiTest;
import org.testng.annotations.Test;

import javax.swing.*;
import java.lang.reflect.Method;
import java.util.List;

import static org.testng.Assert.*;

public class RequestBodyBulkEditSupportTest extends AbstractSwingUiTest {

    @Test
    public void shouldPreferFirstSeparatorWhenParsingUrlencodedLines() throws Exception {
        FormDataTablePanel formDataTablePanel = new FormDataTablePanel(false, false);
        FormUrlencodedTablePanel formUrlencodedTablePanel = new FormUrlencodedTablePanel(false, false);
        RequestBodyBulkEditSupport support = new RequestBodyBulkEditSupport(new JPanel(), formDataTablePanel, formUrlencodedTablePanel);

        invokeVoid(support, "parseFormUrlencodedBulkText",
                "redirect=https://a.example\nexpires=2026-03-26T10:00:00Z");

        List<HttpFormUrlencoded> rows = formUrlencodedTablePanel.getFormDataList();
        assertEquals(rows.size(), 2);
        assertEquals(rows.get(0).getKey(), "redirect");
        assertEquals(rows.get(0).getValue(), "https://a.example");
        assertEquals(rows.get(1).getKey(), "expires");
        assertEquals(rows.get(1).getValue(), "2026-03-26T10:00:00Z");
    }

    @Test
    public void shouldRoundTripTextFormDataStartingWithAtSign() throws Exception {
        FormDataTablePanel formDataTablePanel = new FormDataTablePanel(false, false);
        FormUrlencodedTablePanel formUrlencodedTablePanel = new FormUrlencodedTablePanel(false, false);
        RequestBodyBulkEditSupport support = new RequestBodyBulkEditSupport(new JPanel(), formDataTablePanel, formUrlencodedTablePanel);

        formDataTablePanel.setFormDataList(List.of(
                new HttpFormData(true, "note", HttpFormData.TYPE_TEXT, "@alice")
        ));

        String bulkText = (String) invoke(support, "buildFormDataBulkText");
        assertEquals(bulkText, "note: @@alice\n");

        invokeVoid(support, "parseFormDataBulkText", bulkText);

        List<HttpFormData> rows = formDataTablePanel.getFormDataList();
        assertEquals(rows.size(), 1);
        assertEquals(rows.get(0).getKey(), "note");
        assertEquals(rows.get(0).getType(), HttpFormData.TYPE_TEXT);
        assertEquals(rows.get(0).getValue(), "@alice");
    }

    @Test
    public void shouldPreserveDuplicateFormDataRowStateByOccurrence() throws Exception {
        FormDataTablePanel formDataTablePanel = new FormDataTablePanel(false, false);
        FormUrlencodedTablePanel formUrlencodedTablePanel = new FormUrlencodedTablePanel(false, false);
        RequestBodyBulkEditSupport support = new RequestBodyBulkEditSupport(new JPanel(), formDataTablePanel, formUrlencodedTablePanel);

        formDataTablePanel.setFormDataList(List.of(
                new HttpFormData(false, "dup", HttpFormData.TYPE_TEXT, "first"),
                new HttpFormData(true, "dup", HttpFormData.TYPE_TEXT, "second")
        ));

        invokeVoid(support, "parseFormDataBulkText", "dup: left\ndup: right");

        List<HttpFormData> rows = formDataTablePanel.getFormDataList();
        assertEquals(rows.size(), 2);
        assertFalse(rows.get(0).isEnabled());
        assertTrue(rows.get(1).isEnabled());
        assertEquals(rows.get(0).getValue(), "left");
        assertEquals(rows.get(1).getValue(), "right");
    }

    @Test
    public void shouldPreserveDuplicateUrlencodedRowStateByOccurrence() throws Exception {
        FormDataTablePanel formDataTablePanel = new FormDataTablePanel(false, false);
        FormUrlencodedTablePanel formUrlencodedTablePanel = new FormUrlencodedTablePanel(false, false);
        RequestBodyBulkEditSupport support = new RequestBodyBulkEditSupport(new JPanel(), formDataTablePanel, formUrlencodedTablePanel);

        formUrlencodedTablePanel.setFormDataList(List.of(
                new HttpFormUrlencoded(false, "dup", "first"),
                new HttpFormUrlencoded(true, "dup", "second")
        ));

        invokeVoid(support, "parseFormUrlencodedBulkText", "dup: left\ndup: right");

        List<HttpFormUrlencoded> rows = formUrlencodedTablePanel.getFormDataList();
        assertEquals(rows.size(), 2);
        assertFalse(rows.get(0).isEnabled());
        assertTrue(rows.get(1).isEnabled());
        assertEquals(rows.get(0).getValue(), "left");
        assertEquals(rows.get(1).getValue(), "right");
    }

    private static Object invoke(Object target, String methodName) throws Exception {
        Method method = target.getClass().getDeclaredMethod(methodName);
        method.setAccessible(true);
        return method.invoke(target);
    }

    private static void invokeVoid(Object target, String methodName, String argument) throws Exception {
        Method method = target.getClass().getDeclaredMethod(methodName, String.class);
        method.setAccessible(true);
        method.invoke(target, argument);
    }
}
