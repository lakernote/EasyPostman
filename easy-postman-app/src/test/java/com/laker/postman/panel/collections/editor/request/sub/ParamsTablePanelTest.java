package com.laker.postman.panel.collections.editor.request.sub;

import com.laker.postman.request.model.HttpParam;
import com.laker.postman.test.AbstractSwingUiTest;
import org.testng.annotations.Test;

import javax.swing.*;
import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;

public class ParamsTablePanelTest extends AbstractSwingUiTest {

    @Test
    public void pathVariablesTableShouldOnlyContainUrlGeneratedRows() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            ParamsTablePanel panel = ParamsTablePanel.pathVariablesPanel();

            panel.setParamsList(List.of(new HttpParam(true, "id", "123", "user id")));

            JTable table = panel.getTable();
            assertEquals(table.getRowCount(), 1);
            assertFalse(table.isCellEditable(0, 0));
            assertFalse(table.isCellEditable(0, 1));
            assertTrue(table.isCellEditable(0, 2));
            assertTrue(table.isCellEditable(0, 3));
            assertFalse(table.isCellEditable(0, 4));
        });
    }

    @Test
    public void pathVariablesTableShouldKeepBlankColumnsForQueryTableAlignment() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            ParamsTablePanel queryPanel = new ParamsTablePanel();
            ParamsTablePanel pathPanel = ParamsTablePanel.pathVariablesPanel();

            JTable queryTable = queryPanel.getTable();
            JTable pathTable = pathPanel.getTable();
            assertEquals(pathTable.getColumnClass(0), Object.class);
            assertEquals(
                    pathTable.getColumnModel().getColumn(0).getPreferredWidth(),
                    queryTable.getColumnModel().getColumn(0).getPreferredWidth()
            );
            assertEquals(
                    pathTable.getColumnModel().getColumn(4).getPreferredWidth(),
                    queryTable.getColumnModel().getColumn(4).getPreferredWidth()
            );
        });
    }

    @Test
    public void paramsTablesShouldUseNonScrollableTableLayout() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            assertNonScrollableTableLayout(new ParamsTablePanel());
            assertNonScrollableTableLayout(ParamsTablePanel.pathVariablesPanel());
        });
    }

    @Test
    public void pathVariablesTableShouldNotKeepEmptyAppendRow() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            ParamsTablePanel panel = ParamsTablePanel.pathVariablesPanel();

            panel.setParamsList(List.of());

            assertEquals(panel.getTable().getRowCount(), 0);
        });
    }

    private void assertNonScrollableTableLayout(ParamsTablePanel panel) {
        assertFalse(panel.getComponent(0) instanceof JScrollPane);
        assertSame(panel.getComponent(0), panel.getTable().getTableHeader());
        assertSame(panel.getComponent(1), panel.getTable());
    }
}
