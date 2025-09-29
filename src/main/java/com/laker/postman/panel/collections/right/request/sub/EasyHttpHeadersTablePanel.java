package com.laker.postman.panel.collections.right.request.sub;

import com.laker.postman.common.table.EasyPostmanTextFieldCellEditor;
import com.laker.postman.common.table.EasyPostmanTextFieldCellRenderer;
import com.laker.postman.util.EasyPostManFontUtil;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.*;
import java.util.List;

@Slf4j
public class EasyHttpHeadersTablePanel extends JPanel {
    // Table components
    private DefaultTableModel tableModel;
    @Getter
    private JTable table;
    private JPopupMenu popupMenu;
    private final String[] columns;

    // Default header keys for consistency
    private static final String USER_AGENT = "User-Agent";
    private static final String ACCEPT = "Accept";
    private static final String ACCEPT_ENCODING = "Accept-Encoding";
    private static final String CONNECTION = "Connection";
    private static final Set<String> DEFAULT_HEADER_KEYS = new HashSet<>();

    static {
        DEFAULT_HEADER_KEYS.add(USER_AGENT);
        DEFAULT_HEADER_KEYS.add(ACCEPT);
        DEFAULT_HEADER_KEYS.add(ACCEPT_ENCODING);
        DEFAULT_HEADER_KEYS.add(CONNECTION);
    }

    @Getter
    private boolean editable = true;

    /**
     * Flag to suppress auto-append row during batch operations
     */
    private boolean suppressAutoAppendRow = false;

    public EasyHttpHeadersTablePanel() {
        this.columns = new String[]{"Key", "Value"};
        initializeComponents();
        initializeTableUI();
        setupCellRenderersAndEditors();
        setupPopupMenu();
        setupTableListeners();
        addAutoAppendRowFeature();
    }

    private void initializeComponents() {
        setLayout(new BorderLayout());
        setBackground(new Color(248, 250, 252));
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(237, 237, 237)),
                BorderFactory.createEmptyBorder(4, 4, 4, 4)));

        // Initialize table model with custom cell editing logic
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                if (!editable) {
                    return false;
                }

                // Check if this is a default header
                Object keyObj = getValueAt(row, 0);
                if (keyObj != null) {
                    String key = keyObj.toString().trim();
                    if (DEFAULT_HEADER_KEYS.contains(key)) {
                        // For default headers: Key column not editable, Value column editable
                        return column == 1;
                    }
                }

                // For non-default headers: both columns editable
                return true;
            }
        };

        // Create table
        table = new JTable(tableModel);
        add(table, BorderLayout.CENTER);
    }

    private void initializeTableUI() {
        table.setFillsViewportHeight(true);
        table.setRowHeight(24);
        table.setFont(EasyPostManFontUtil.getDefaultFont(Font.PLAIN, 11));
        table.getTableHeader().setFont(EasyPostManFontUtil.getDefaultFont(Font.BOLD, 11));
        table.getTableHeader().setBackground(new Color(240, 242, 245));
        table.getTableHeader().setForeground(new Color(33, 33, 33));
        table.setGridColor(new Color(237, 237, 237));
        table.setShowHorizontalLines(true);
        table.setShowVerticalLines(true);
        table.setIntercellSpacing(new Dimension(2, 2));
        table.setRowMargin(2);
        table.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(237, 237, 237)),
                BorderFactory.createEmptyBorder(2, 2, 2, 2)));
        table.setOpaque(false);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        table.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
    }

    private void setupCellRenderersAndEditors() {
        setEmptyCellWhiteBackgroundRenderer();
        setColumnEditor(0, new EasyPostmanTextFieldCellEditor());
        setColumnEditor(1, new EasyPostmanTextFieldCellEditor());
        setColumnRenderer(0, new EasyPostmanTextFieldCellRenderer());
        setColumnRenderer(1, new EasyPostmanTextFieldCellRenderer());
    }

    private void setupPopupMenu() {
        popupMenu = createPopupMenu();
    }

    private void setupTableListeners() {
        addTableRightMouseListener();
    }

    /**
     * Set empty cell white background renderer
     */
    private void setEmptyCellWhiteBackgroundRenderer() {
        DefaultTableCellRenderer renderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                                                           boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

                if (!isSelected) {
                    c.setBackground(Color.WHITE);
                }

                return c;
            }
        };

        table.setDefaultRenderer(Object.class, renderer);
    }

    /**
     * Set column editor
     */
    public void setColumnEditor(int column, TableCellEditor editor) {
        if (column >= 0 && column < table.getColumnCount()) {
            table.getColumnModel().getColumn(column).setCellEditor(editor);
        }
    }

    /**
     * Set column renderer
     */
    public void setColumnRenderer(int column, TableCellRenderer renderer) {
        if (column >= 0 && column < table.getColumnCount()) {
            table.getColumnModel().getColumn(column).setCellRenderer(renderer);
        }
    }

    /**
     * Create popup menu for add/remove operations
     */
    private JPopupMenu createPopupMenu() {
        JPopupMenu menu = new JPopupMenu();

        JMenuItem addItem = new JMenuItem("Add");
        addItem.addActionListener(e -> addRowAndScroll());

        JMenuItem removeItem = new JMenuItem("Remove");
        removeItem.addActionListener(e -> deleteSelectedRow());

        menu.add(addItem);
        menu.add(removeItem);

        return menu;
    }

    /**
     * Add right-click menu listener
     */
    private void addTableRightMouseListener() {
        MouseAdapter tableMouseListener = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    showPopupMenu(e);
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    showPopupMenu(e);
                }
            }

            private void showPopupMenu(MouseEvent e) {
                if (!editable) {
                    return;
                }

                int viewRow = table.rowAtPoint(e.getPoint());
                if (viewRow >= 0) {
                    table.setRowSelectionInterval(viewRow, viewRow);
                }

                popupMenu.show(e.getComponent(), e.getX(), e.getY());
            }
        };

        table.addMouseListener(tableMouseListener);
    }

    /**
     * Add auto-append row feature when editing the last row
     */
    private void addAutoAppendRowFeature() {
        tableModel.addTableModelListener(e -> {
            if (suppressAutoAppendRow || !editable) {
                return;
            }

            SwingUtilities.invokeLater(() -> {
                try {
                    int rowCount = tableModel.getRowCount();
                    if (rowCount == 0) {
                        return;
                    }

                    // Check if the last row has any content
                    int lastRow = rowCount - 1;
                    boolean lastRowHasContent = false;

                    for (int col = 0; col < tableModel.getColumnCount(); col++) {
                        Object value = tableModel.getValueAt(lastRow, col);
                        if (value != null && !value.toString().trim().isEmpty()) {
                            lastRowHasContent = true;
                            break;
                        }
                    }

                    // Add empty row if last row has content
                    if (lastRowHasContent) {
                        suppressAutoAppendRow = true;
                        try {
                            tableModel.addRow(new Object[]{"", ""});
                        } finally {
                            suppressAutoAppendRow = false;
                        }
                    }
                } catch (Exception ex) {
                    log.warn("Error in auto-append row feature", ex);
                }
            });
        });
    }

    // Public API methods

    /**
     * Add a new row with the given values
     */
    public void addRow(Object... values) {
        if (values == null || values.length == 0) {
            tableModel.addRow(new Object[]{"", ""});
        } else {
            // Ensure we have exactly 2 columns
            Object[] row = new Object[2];
            for (int i = 0; i < Math.min(values.length, 2); i++) {
                row[i] = values[i];
            }
            // Fill remaining columns with empty strings
            for (int i = values.length; i < 2; i++) {
                row[i] = "";
            }
            tableModel.addRow(row);
        }
    }

    /**
     * Add a new row and scroll to it
     */
    public void addRowAndScroll() {
        addRow();
        scrollToLastRow();
    }

    /**
     * Delete the currently selected row
     */
    public void deleteSelectedRow() {
        int selectedRow = table.getSelectedRow();
        if (selectedRow >= 0) {
            // Convert view index to model index if using row sorter
            int modelRow = selectedRow;
            if (table.getRowSorter() != null) {
                modelRow = table.getRowSorter().convertRowIndexToModel(selectedRow);
            }

            if (modelRow >= 0 && modelRow < tableModel.getRowCount()) {
                tableModel.removeRow(modelRow);
            }
        }
    }

    /**
     * Clear all rows in the table
     */
    public void clear() {
        suppressAutoAppendRow = true;
        try {
            tableModel.setRowCount(0);
        } finally {
            suppressAutoAppendRow = false;
        }
    }

    /**
     * Scroll to make the rectangle visible
     */
    public void scrollRectToVisible() {
        scrollToLastRow();
    }

    private void scrollToLastRow() {
        SwingUtilities.invokeLater(() -> {
            int rowCount = table.getRowCount();
            if (rowCount > 0) {
                Rectangle rect = table.getCellRect(rowCount - 1, 0, true);
                table.scrollRectToVisible(rect);
            }
        });
    }

    /**
     * Get all rows as a list of maps (model data, not view data)
     */
    public List<Map<String, Object>> getRows() {
        List<Map<String, Object>> rows = new ArrayList<>();

        for (int i = 0; i < tableModel.getRowCount(); i++) {
            Map<String, Object> row = new LinkedHashMap<>();
            for (int j = 0; j < tableModel.getColumnCount(); j++) {
                String columnName = tableModel.getColumnName(j);
                Object value = tableModel.getValueAt(i, j);
                row.put(columnName, value);
            }
            rows.add(row);
        }

        return rows;
    }

    /**
     * Set all rows from a list of maps
     */
    public void setRows(List<Map<String, Object>> rows) {
        suppressAutoAppendRow = true;
        try {
            // Clear existing data
            tableModel.setRowCount(0);

            // Add new rows
            if (rows != null) {
                for (Map<String, Object> row : rows) {
                    Object[] rowData = new Object[columns.length];
                    for (int i = 0; i < columns.length; i++) {
                        String columnName = columns[i];
                        rowData[i] = row.get(columnName);
                    }
                    tableModel.addRow(rowData);
                }
            }
        } finally {
            suppressAutoAppendRow = false;
        }
    }

    /**
     * Add table model listener
     */
    public void addTableModelListener(TableModelListener l) {
        if (tableModel != null) {
            tableModel.addTableModelListener(l);
        }
    }

    /**
     * Remove table model listener
     */
    public void removeTableModelListener(TableModelListener l) {
        if (tableModel != null) {
            tableModel.removeTableModelListener(l);
        }
    }

    /**
     * Set whether the table is editable
     */
    public void setEditable(boolean editable) {
        this.editable = editable;
        table.repaint(); // Refresh to update cell editability
    }

    /**
     * Check if a header key is a default header
     */
    public boolean isDefaultHeader(String key) {
        return key != null && DEFAULT_HEADER_KEYS.contains(key.trim());
    }
}