package com.laker.postman.common.component.table;

import com.laker.postman.request.model.HttpFormData;
import com.laker.postman.test.AbstractSwingUiTest;
import org.testng.annotations.Test;

import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.table.TableCellRenderer;
import java.awt.Color;
import java.awt.Component;
import java.util.List;

import static org.testng.Assert.assertEquals;

public class FormDataTablePanelRendererTest extends AbstractSwingUiTest {

    @Test
    public void formDataCustomRenderersShouldKeepStripedRowBackground() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            FormDataTablePanel panel = new FormDataTablePanel(false, false);
            panel.setFormDataList(List.of(
                    new HttpFormData(true, "key1", HttpFormData.TYPE_TEXT, "value1"),
                    new HttpFormData(true, "key2", HttpFormData.TYPE_TEXT, "value2")
            ));

            JTable table = panel.getTable();
            table.setBackground(new Color(240, 244, 248));
            table.setSize(960, 120);
            table.doLayout();

            Color stripedBackground = TableUIConstants.getRowBackground(table, 1);

            assertEquals(renderedBackground(table, 1, 2), stripedBackground);
            assertEquals(renderedBackground(table, 1, 3), stripedBackground);
            assertEquals(renderedBackground(table, 1, 4), stripedBackground);
            assertEquals(renderedBackground(table, 1, 5), stripedBackground);
        });
    }

    private static Color renderedBackground(JTable table, int row, int column) {
        TableCellRenderer renderer = table.getCellRenderer(row, column);
        Component component = renderer.getTableCellRendererComponent(
                table,
                table.getValueAt(row, column),
                false,
                false,
                row,
                column
        );
        return component.getBackground();
    }
}
