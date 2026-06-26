package com.laker.postman.common.component.table;

import org.testng.annotations.Test;

import javax.swing.JTable;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.List;

import static org.testng.Assert.assertFalse;

public class EnhancedTablePanelTest {

    @Test
    public void shouldIgnoreDoubleClickCellDetailPopupWhenDisabled() {
        EnhancedTablePanel panel = new EnhancedTablePanel(new String[]{"URL"});
        panel.setCellDetailDialogOnDoubleClickEnabled(false);
        panel.setData(List.<Object[]>of(new Object[]{"https://example.com/" + "very-long-path".repeat(12)}));

        JTable table = panel.getTable();
        table.setSize(500, 100);
        MouseEvent doubleClick = new MouseEvent(
                table,
                MouseEvent.MOUSE_CLICKED,
                System.currentTimeMillis(),
                0,
                8,
                8,
                2,
                false,
                MouseEvent.BUTTON1
        );

        for (MouseListener listener : table.getMouseListeners()) {
            listener.mouseClicked(doubleClick);
        }

        assertFalse(panel.isCellDetailDialogOnDoubleClickEnabled());
    }
}
