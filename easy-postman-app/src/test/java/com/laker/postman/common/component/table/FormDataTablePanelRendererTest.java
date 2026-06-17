package com.laker.postman.common.component.table;

import com.laker.postman.request.model.HttpFormData;
import com.laker.postman.test.AbstractSwingUiTest;
import org.testng.annotations.Test;

import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.table.TableCellRenderer;
import java.awt.Color;
import java.awt.Component;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

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

    @Test
    public void formDataTableShouldUsePostmanLikeColumnEmphasis() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            FormDataTablePanel panel = new FormDataTablePanel(false, false);
            JTable table = panel.getTable();

            assertEquals(table.getColumnName(2), "");
            assertEquals(table.getColumnModel().getColumn(2).getPreferredWidth(), 76);
            assertEquals(table.getColumnModel().getColumn(2).getMaxWidth(), 76);
            assertEquals(table.getColumnModel().getColumn(5).getPreferredWidth(), 30);
            assertEquals(table.getColumnModel().getColumn(5).getMaxWidth(), 40);
            assertTrue(table.getColumnModel().getColumn(1).getHeaderRenderer() != null);
            assertTrue(table.getColumnModel().getColumn(2).getHeaderRenderer() != null);
            assertTrue(table.getColumnModel().getColumn(3).getPreferredWidth()
                    > table.getColumnModel().getColumn(1).getPreferredWidth());
            assertTrue(table.getColumnModel().getColumn(4).getPreferredWidth()
                    < table.getColumnModel().getColumn(3).getPreferredWidth());
        });
    }

    @Test
    public void formDataTypeHeaderShouldRenderAsContinuationOfKeyHeader() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            FormDataTablePanel panel = new FormDataTablePanel(false, false);
            JTable table = panel.getTable();
            table.setSize(960, 120);
            table.doLayout();

            Component keyHeader = renderedHeaderComponent(table, 1);
            Component typeHeader = renderedHeaderComponent(table, 2);

            assertTrue(keyHeader instanceof JLabel);
            assertTrue(typeHeader instanceof JLabel);
            assertEquals(((JLabel) keyHeader).getText(), table.getColumnName(1));
            assertEquals(((JLabel) typeHeader).getText(), "");
            assertEquals(((JComponent) keyHeader).getBorder().getBorderInsets((Component) keyHeader).right, 0);
            assertTrue(((JComponent) typeHeader).getBorder().getBorderInsets((Component) typeHeader).right > 0);
        });
    }

    @Test
    public void deleteActionShouldUseVisibleDeleteColumn() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            FormDataTablePanel panel = new FormDataTablePanel(false, false);
            panel.setFormDataList(List.of(new HttpFormData(true, "key1", HttpFormData.TYPE_TEXT, "value1")));

            JTable table = panel.getTable();
            table.setSize(960, 120);
            table.doLayout();
            int deleteColumn = table.getColumnCount() - 1;

            assertNotNull(renderedIcon(table, 0, deleteColumn, false));

            dispatchMouseMoved(table, cellCenter(table, 0, deleteColumn));
            assertNotNull(renderedIcon(table, 0, deleteColumn, false));

            dispatchMouseExited(table);
            assertNotNull(renderedIcon(table, 0, deleteColumn, true));
        });
    }

    @Test
    public void deleteColumnActionShouldRemoveRow() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            FormDataTablePanel panel = new FormDataTablePanel(false, false);
            panel.setFormDataList(List.of(
                    new HttpFormData(true, "key1", HttpFormData.TYPE_TEXT, "value1"),
                    new HttpFormData(true, "key2", HttpFormData.TYPE_TEXT, "value2")
            ));

            JTable table = panel.getTable();
            table.setSize(960, 120);
            table.doLayout();
            int deleteColumn = table.getColumnCount() - 1;

            assertTrue(table.getColumnModel().getColumn(deleteColumn).getWidth() > 0);
            dispatchMouseMoved(table, cellCenter(table, 0, deleteColumn));
            dispatchMouseClicked(table, cellCenter(table, 0, deleteColumn));

            assertEquals(panel.getFormDataListFromModel().size(), 1);
            assertEquals(panel.getFormDataListFromModel().get(0).getKey(), "key2");
        });
    }

    @Test
    public void fileValueRendererShouldStayTextLikeAndKeepStatusInTooltip() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            FormDataTablePanel panel = new FormDataTablePanel(false, false);
            panel.setFormDataList(List.of(new HttpFormData(true, "file", HttpFormData.TYPE_FILE, "missing-file.txt")));

            JTable table = panel.getTable();
            table.setSize(960, 120);
            table.doLayout();

            Component component = renderedComponent(table, 0, 3, false);

            assertTrue(component instanceof JLabel);
            JLabel label = (JLabel) component;
            assertEquals(label.getIcon(), null);
            assertEquals(label.getText(), "missing-file.txt");
            assertTrue(label.getToolTipText().contains("file not found"));
        });
    }

    private static Color renderedBackground(JTable table, int row, int column) {
        return renderedComponent(table, row, column, false).getBackground();
    }

    private static Icon renderedIcon(JTable table, int row, int column, boolean selected) {
        Component component = renderedComponent(table, row, column, selected);
        return findIcon(component);
    }

    private static Component renderedComponent(JTable table, int row, int column, boolean selected) {
        TableCellRenderer renderer = table.getCellRenderer(row, column);
        return renderer.getTableCellRendererComponent(
                table,
                table.getValueAt(row, column),
                selected,
                false,
                row,
                column
        );
    }

    private static Component renderedHeaderComponent(JTable table, int column) {
        TableCellRenderer renderer = table.getColumnModel().getColumn(column).getHeaderRenderer();
        return renderer.getTableCellRendererComponent(
                table,
                table.getColumnName(column),
                false,
                false,
                -1,
                column
        );
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

    private static void dispatchMouseClicked(JTable table, Point point) {
        table.dispatchEvent(new MouseEvent(
                table,
                MouseEvent.MOUSE_CLICKED,
                System.currentTimeMillis(),
                0,
                point.x,
                point.y,
                1,
                false,
                MouseEvent.BUTTON1
        ));
    }

    private static void dispatchMouseExited(JTable table) {
        table.dispatchEvent(new MouseEvent(
                table,
                MouseEvent.MOUSE_EXITED,
                System.currentTimeMillis(),
                0,
                -1,
                -1,
                0,
                false
        ));
    }

    private static Icon findIcon(Component component) {
        if (component instanceof JLabel label && label.isVisible() && label.getIcon() != null) {
            return label.getIcon();
        }
        if (component instanceof java.awt.Container container) {
            for (Component child : container.getComponents()) {
                Icon icon = findIcon(child);
                if (icon != null) {
                    return icon;
                }
            }
        }
        return null;
    }
}
