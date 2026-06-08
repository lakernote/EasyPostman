package com.laker.postman.common.component.table;

import com.laker.postman.model.Variable;
import com.laker.postman.request.model.HttpFormUrlencoded;
import com.laker.postman.test.AbstractSwingUiTest;
import org.testng.annotations.Test;

import javax.swing.JTable;
import javax.swing.SwingUtilities;
import java.awt.Cursor;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.util.List;

import static org.testng.Assert.assertEquals;

public class AbstractTablePanelDeleteButtonTest extends AbstractSwingUiTest {

    @Test
    public void deleteColumnHoverShouldUseHandCursorOnlyWhenDeleteActionIsAvailable() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            FormUrlencodedTablePanel panel = new FormUrlencodedTablePanel(true, false);
            panel.setFormDataList(List.of(new HttpFormUrlencoded(true, "token", "abc")));

            JTable table = panel.getTable();
            table.setSize(520, 80);
            table.doLayout();

            int deleteColumn = table.getColumnCount() - 1;
            dispatchMouseMoved(table, cellCenter(table, 0, deleteColumn));

            assertEquals(table.getCursor().getType(), Cursor.HAND_CURSOR);

            dispatchMouseMoved(table, cellCenter(table, 0, 1));

            assertEquals(table.getCursor().getType(), Cursor.DEFAULT_CURSOR);
        });
    }

    @Test
    public void variableTableDeleteColumnHoverShouldNotBeOverriddenByDragHandleCursor() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            EasyVariableTablePanel panel = new EasyVariableTablePanel("Name", "Value", true, false);
            panel.setVariableList(List.of(new Variable(true, "token", "abc")));

            JTable table = panel.getTable();
            table.setSize(520, 80);
            table.doLayout();

            int deleteColumn = table.getColumnCount() - 1;
            dispatchMouseMoved(table, cellCenter(table, 0, deleteColumn));

            assertEquals(table.getCursor().getType(), Cursor.HAND_CURSOR);
        });
    }

    private static Point cellCenter(JTable table, int row, int column) {
        Rectangle rect = table.getCellRect(row, column, true);
        return new Point(rect.x + rect.width / 2, rect.y + rect.height / 2);
    }

    private static void dispatchMouseMoved(JTable table, Point point) {
        table.dispatchEvent(new MouseEvent(
                table,
                MouseEvent.MOUSE_MOVED,
                System.currentTimeMillis(),
                0,
                point.x,
                point.y,
                0,
                false
        ));
    }
}
