package com.laker.postman.common.table.map;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.laker.postman.common.table.EasyPostmanTextFieldCellEditor;
import com.laker.postman.common.table.EasyPostmanTextFieldCellRenderer;
import com.laker.postman.model.HttpParam;
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

/**
 * Params table panel with checkbox and delete button columns
 * Similar to EasyHttpHeadersTablePanel but for request parameters
 */
@Slf4j
public class EasyParamsTablePanel extends JPanel {
    // Table components
    private DefaultTableModel tableModel;
    @Getter
    private JTable table;
    private final String[] columns;

    @Getter
    private boolean editable = true;

    /**
     * Flag to suppress auto-append row during batch operations
     */
    private boolean suppressAutoAppendRow = false;

    // Column indices
    private static final int COL_ENABLED = 0;
    private static final int COL_KEY = 1;
    private static final int COL_VALUE = 2;
    private static final int COL_DELETE = 3;

    /**
     * Set whether to suppress auto-append row feature
     */
    public void setAutoAppendRowSuppressed(boolean suppressed) {
        this.suppressAutoAppendRow = suppressed;
    }

    public EasyParamsTablePanel() {
        this.columns = new String[]{"", "Key", "Value", ""};
        initializeComponents();
        initializeTableUI();
        setupCellRenderersAndEditors();
        setupTableListeners();
        addAutoAppendRowFeature();

        // Add initial empty row
        addRow();
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
            public Class<?> getColumnClass(int columnIndex) {
                if (columnIndex == COL_ENABLED) {
                    return Boolean.class;
                }
                return Object.class;
            }

            @Override
            public boolean isCellEditable(int row, int column) {
                if (!editable) {
                    return false;
                }

                // Checkbox column is always editable
                if (column == COL_ENABLED) {
                    return true;
                }

                // Delete column is not editable (uses custom renderer)
                if (column == COL_DELETE) {
                    return false;
                }

                // Key and Value columns are editable
                return column == COL_KEY || column == COL_VALUE;
            }
        };

        // Create table
        table = new JTable(tableModel);

        // Wrap table in JScrollPane to show headers
        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        add(scrollPane, BorderLayout.CENTER);
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

        // Configure Tab key behavior to move between columns
        table.setSurrendersFocusOnKeystroke(true);

        // Set column widths
        table.getColumnModel().getColumn(COL_ENABLED).setPreferredWidth(40);
        table.getColumnModel().getColumn(COL_ENABLED).setMaxWidth(40);
        table.getColumnModel().getColumn(COL_ENABLED).setMinWidth(40);

        table.getColumnModel().getColumn(COL_DELETE).setPreferredWidth(40);
        table.getColumnModel().getColumn(COL_DELETE).setMaxWidth(40);
        table.getColumnModel().getColumn(COL_DELETE).setMinWidth(40);

        // Setup Tab key navigation to move between columns in the same row
        setupTabKeyNavigation();
    }

    /**
     * Setup Tab key navigation to move between columns instead of rows
     */
    private void setupTabKeyNavigation() {
        // Override Tab and Shift+Tab behavior for proper column-by-column navigation
        InputMap inputMap = table.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        ActionMap actionMap = table.getActionMap();

        // Tab key - move to next editable cell in the same row
        inputMap.put(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_TAB, 0), "nextCell");
        actionMap.put("nextCell", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                moveToNextEditableCell(false);
            }
        });

        // Shift+Tab - move to previous editable cell in the same row
        inputMap.put(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_TAB, java.awt.event.InputEvent.SHIFT_DOWN_MASK), "previousCell");
        actionMap.put("previousCell", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                moveToNextEditableCell(true);
            }
        });
    }

    /**
     * Move to the next (or previous) editable cell in the current row
     *
     * @param reverse true to move backwards (Shift+Tab), false to move forwards (Tab)
     */
    private void moveToNextEditableCell(boolean reverse) {
        int currentRow = table.getSelectedRow();
        int currentColumn = table.getSelectedColumn();

        if (currentRow < 0 || currentColumn < 0) {
            // No cell selected, select the first editable cell
            table.changeSelection(0, COL_KEY, false, false);
            table.editCellAt(0, COL_KEY);
            return;
        }

        // Stop editing current cell
        if (table.isEditing()) {
            table.getCellEditor().stopCellEditing();
        }

        // Find next editable column in the current row
        int nextColumn = currentColumn;
        int columnCount = table.getColumnCount();

        if (reverse) {
            // Move backwards
            do {
                nextColumn--;
                if (nextColumn < 0) {
                    // Move to previous row, last editable column
                    if (currentRow > 0) {
                        currentRow--;
                        nextColumn = COL_VALUE; // Last editable column
                    } else {
                        nextColumn = 0; // Stay at first position
                    }
                    break;
                }
            } while (nextColumn >= 0 && !isCellEditable(currentRow, nextColumn));
        } else {
            // Move forwards
            do {
                nextColumn++;
                if (nextColumn >= columnCount) {
                    // Move to next row, first editable column
                    if (currentRow < table.getRowCount() - 1) {
                        currentRow++;
                        nextColumn = COL_KEY; // First editable column
                    } else {
                        nextColumn = columnCount - 1; // Stay at last position
                    }
                    break;
                }
            } while (nextColumn < columnCount && !isCellEditable(currentRow, nextColumn));
        }

        // Select and start editing the next cell
        if (nextColumn >= 0 && nextColumn < columnCount && currentRow >= 0 && currentRow < table.getRowCount()) {
            table.changeSelection(currentRow, nextColumn, false, false);
            if (isCellEditable(currentRow, nextColumn)) {
                table.editCellAt(currentRow, nextColumn);
                Component editor = table.getEditorComponent();
                if (editor instanceof JTextField) {
                    editor.requestFocusInWindow();
                    ((JTextField) editor).selectAll();
                }
            }
        }
    }

    /**
     * Check if a cell is editable
     */
    private boolean isCellEditable(int row, int column) {
        if (!editable) {
            return false;
        }
        // Only Key and Value columns are editable via Tab navigation
        // (Checkbox is editable but we skip it in Tab navigation)
        return column == COL_KEY || column == COL_VALUE;
    }

    private void setupCellRenderersAndEditors() {
        setEmptyCellWhiteBackgroundRenderer();

        // Set editors for Key and Value columns
        setColumnEditor(COL_KEY, new EasyPostmanTextFieldCellEditor());
        setColumnEditor(COL_VALUE, new EasyPostmanTextFieldCellEditor());
        setColumnRenderer(COL_KEY, new EasyPostmanTextFieldCellRenderer());
        setColumnRenderer(COL_VALUE, new EasyPostmanTextFieldCellRenderer());

        // Set custom renderer for delete column
        setColumnRenderer(COL_DELETE, new DeleteButtonRenderer());
    }

    private void setupTableListeners() {
        addTableRightMouseListener();
        addDeleteButtonListener();
    }

    /**
     * Add mouse listener for delete button clicks
     */
    private void addDeleteButtonListener() {
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (!editable) {
                    return;
                }

                int column = table.columnAtPoint(e.getPoint());
                int row = table.rowAtPoint(e.getPoint());

                if (column == COL_DELETE && row >= 0) {
                    // Convert view row to model row
                    int modelRow = row;
                    if (table.getRowSorter() != null) {
                        modelRow = table.getRowSorter().convertRowIndexToModel(row);
                    }

                    // Check if row is valid
                    if (modelRow < 0 || modelRow >= tableModel.getRowCount()) {
                        return;
                    }

                    // Prevent deleting the last row (keep at least one empty row like Postman)
                    // The last row is always the empty template row
                    int rowCount = tableModel.getRowCount();
                    if (modelRow == rowCount - 1 && rowCount == 1) {
                        // Don't delete if it's the only row
                        return;
                    }

                    // Stop cell editing before deleting
                    stopCellEditing();

                    // Delete the row
                    tableModel.removeRow(modelRow);

                    // Ensure there's always an empty row at the end
                    ensureEmptyLastRow();
                }
            }
        });
    }

    /**
     * Custom renderer for delete button column
     */
    private class DeleteButtonRenderer extends JLabel implements TableCellRenderer {
        private final Icon deleteIcon;

        public DeleteButtonRenderer() {
            setHorizontalAlignment(SwingConstants.CENTER);
            setOpaque(true);
            deleteIcon = new FlatSVGIcon("icons/close.svg", 16, 16);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus, int row, int column) {
            // Set background
            if (isSelected) {
                setBackground(table.getSelectionBackground());
                setForeground(table.getSelectionForeground());
            } else {
                setBackground(table.getBackground());
                setForeground(table.getForeground());
            }

            // Clear icon by default
            setIcon(null);
            setCursor(Cursor.getDefaultCursor());

            // Convert view row to model row
            int modelRow = row;
            if (table.getRowSorter() != null) {
                modelRow = table.getRowSorter().convertRowIndexToModel(row);
            }

            // Show delete icon for all rows except the last empty row
            if (modelRow >= 0 && modelRow < tableModel.getRowCount()) {
                int rowCount = tableModel.getRowCount();
                boolean isLastRow = (modelRow == rowCount - 1);

                // Show delete icon if:
                // 1. It's not the last row (OR)
                // 2. It's the last row but there are multiple rows and it has content
                Object keyObj = tableModel.getValueAt(modelRow, COL_KEY);
                Object valueObj = tableModel.getValueAt(modelRow, COL_VALUE);
                String keyStr = keyObj == null ? "" : keyObj.toString().trim();
                String valueStr = valueObj == null ? "" : valueObj.toString().trim();
                boolean isEmpty = keyStr.isEmpty() && valueStr.isEmpty();

                boolean shouldShowIcon = false;
                if (!isLastRow) {
                    // Not the last row - always show delete icon
                    shouldShowIcon = true;
                } else {
                    // Last row - only show if it has content and there are multiple rows
                    shouldShowIcon = !isEmpty && rowCount > 1;
                }

                if (shouldShowIcon && editable) {
                    setIcon(deleteIcon);
                    setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                }
            }

            return this;
        }
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

                JPopupMenu contextMenu = createContextPopupMenu();
                contextMenu.show(e.getComponent(), e.getX(), e.getY());
            }
        };

        table.addMouseListener(tableMouseListener);
    }

    /**
     * Create context menu
     */
    private JPopupMenu createContextPopupMenu() {
        JPopupMenu menu = new JPopupMenu();

        JMenuItem addItem = new JMenuItem("Add");
        addItem.addActionListener(e -> addRowAndScroll());
        menu.add(addItem);

        JMenuItem removeItem = new JMenuItem("Remove");
        removeItem.addActionListener(e -> deleteSelectedRow());
        menu.add(removeItem);

        return menu;
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

                    // Check Key and Value columns only
                    for (int col = COL_KEY; col <= COL_VALUE; col++) {
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
                            tableModel.addRow(new Object[]{true, "", "", ""});
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
            tableModel.addRow(new Object[]{true, "", "", ""});
        } else if (values.length == 2) {
            // Legacy support: if 2 values provided, treat as Key, Value
            tableModel.addRow(new Object[]{true, values[0], values[1], ""});
        } else {
            // Ensure we have exactly 4 columns
            Object[] row = new Object[4];
            row[0] = true; // Default enabled
            for (int i = 0; i < Math.min(values.length, 2); i++) {
                row[i + 1] = values[i];
            }
            // Fill remaining columns with empty strings
            for (int i = values.length; i < 3; i++) {
                row[i + 1] = "";
            }
            row[3] = ""; // Delete column
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
        // Stop cell editing before modifying table structure
        stopCellEditing();

        int selectedRow = table.getSelectedRow();
        if (selectedRow >= 0) {
            // Convert view index to model index if using row sorter
            int modelRow = selectedRow;
            if (table.getRowSorter() != null) {
                modelRow = table.getRowSorter().convertRowIndexToModel(selectedRow);
            }

            if (modelRow >= 0 && modelRow < tableModel.getRowCount()) {
                int rowCount = tableModel.getRowCount();

                // Don't delete if it's the only row (keep at least one empty row like Postman)
                if (rowCount <= 1) {
                    return;
                }

                // Delete the row
                tableModel.removeRow(modelRow);
                ensureEmptyLastRow();
            }
        }
    }

    /**
     * Clear all rows in the table
     */
    public void clear() {
        // Stop cell editing before modifying table structure
        stopCellEditing();

        suppressAutoAppendRow = true;
        try {
            tableModel.setRowCount(0);
            // Add an empty row after clearing
            tableModel.addRow(new Object[]{true, "", "", ""});
        } finally {
            suppressAutoAppendRow = false;
        }
    }

    /**
     * Scroll to last row
     */
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
            // Store enabled state
            row.put("Enabled", tableModel.getValueAt(i, COL_ENABLED));
            // Store Key and Value
            row.put("Key", tableModel.getValueAt(i, COL_KEY));
            row.put("Value", tableModel.getValueAt(i, COL_VALUE));
            rows.add(row);
        }

        return rows;
    }

    /**
     * Set all rows from a list of maps
     */
    public void setRows(List<Map<String, Object>> rows) {
        // Stop cell editing before modifying table structure
        stopCellEditing();

        suppressAutoAppendRow = true;
        try {
            // Clear existing data
            tableModel.setRowCount(0);

            // Add new rows
            if (rows != null) {
                for (Map<String, Object> row : rows) {
                    Object enabled = row.get("Enabled");
                    if (enabled == null) {
                        enabled = true; // Default to enabled
                    }
                    Object key = row.get("Key");
                    Object value = row.get("Value");

                    tableModel.addRow(new Object[]{enabled, key, value, ""});
                }
            }

            // Ensure there's always an empty row at the end
            if (tableModel.getRowCount() == 0 || hasContentInLastRow()) {
                tableModel.addRow(new Object[]{true, "", "", ""});
            }
        } finally {
            suppressAutoAppendRow = false;
        }
    }

    /**
     * Set data from Map (legacy compatibility - all enabled by default)
     */
    public void setMap(Map<String, String> map) {
        // Stop cell editing before modifying table structure
        stopCellEditing();

        suppressAutoAppendRow = true;
        try {
            tableModel.setRowCount(0);
            if (map != null) {
                for (Map.Entry<String, String> entry : map.entrySet()) {
                    tableModel.addRow(new Object[]{true, entry.getKey(), entry.getValue(), ""});
                }
            }

            // Ensure there's always an empty row at the end
            if (tableModel.getRowCount() == 0 || hasContentInLastRow()) {
                tableModel.addRow(new Object[]{true, "", "", ""});
            }
        } finally {
            suppressAutoAppendRow = false;
        }
    }

    /**
     * Get enabled parameters as Map (only returns enabled rows)
     */
    public Map<String, String> getMap() {
        Map<String, String> map = new LinkedHashMap<>();
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            Object enabledObj = tableModel.getValueAt(i, COL_ENABLED);
            Object keyObj = tableModel.getValueAt(i, COL_KEY);
            Object valueObj = tableModel.getValueAt(i, COL_VALUE);

            boolean enabled = enabledObj instanceof Boolean ? (Boolean) enabledObj : true;
            String key = keyObj == null ? "" : keyObj.toString().trim();
            String value = valueObj == null ? "" : valueObj.toString().trim();

            if (enabled && !key.isEmpty()) {
                map.put(key, value);
            }
        }
        return map;
    }

    /**
     * Get params list with enabled state (new format)
     */
    public List<HttpParam> getParamsList() {
        List<HttpParam> paramsList = new ArrayList<>();
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            Object enabledObj = tableModel.getValueAt(i, COL_ENABLED);
            Object keyObj = tableModel.getValueAt(i, COL_KEY);
            Object valueObj = tableModel.getValueAt(i, COL_VALUE);

            boolean enabled = enabledObj instanceof Boolean ? (Boolean) enabledObj : true;
            String key = keyObj == null ? "" : keyObj.toString().trim();
            String value = valueObj == null ? "" : valueObj.toString().trim();

            // Only add non-empty params
            if (!key.isEmpty()) {
                paramsList.add(new HttpParam(enabled, key, value));
            }
        }
        return paramsList;
    }

    /**
     * Set params list with enabled state (new format)
     */
    public void setParamsList(List<HttpParam> paramsList) {
        // Stop cell editing before modifying table structure
        stopCellEditing();

        suppressAutoAppendRow = true;
        try {
            tableModel.setRowCount(0);
            if (paramsList != null) {
                for (HttpParam param : paramsList) {
                    tableModel.addRow(new Object[]{param.isEnabled(), param.getKey(), param.getValue(), ""});
                }
            }

            // Ensure there's always an empty row at the end
            if (tableModel.getRowCount() == 0 || hasContentInLastRow()) {
                tableModel.addRow(new Object[]{true, "", "", ""});
            }
        } finally {
            suppressAutoAppendRow = false;
        }
    }

    /**
     * Check if the last row has any content
     */
    private boolean hasContentInLastRow() {
        int rowCount = tableModel.getRowCount();
        if (rowCount == 0) {
            return false;
        }

        int lastRow = rowCount - 1;
        Object keyObj = tableModel.getValueAt(lastRow, COL_KEY);
        Object valueObj = tableModel.getValueAt(lastRow, COL_VALUE);

        String key = keyObj == null ? "" : keyObj.toString().trim();
        String value = valueObj == null ? "" : valueObj.toString().trim();

        return !key.isEmpty() || !value.isEmpty();
    }

    /**
     * Ensure there's always an empty row at the end of the table, like Postman
     * This method should be called after row deletions to maintain consistency
     */
    private void ensureEmptyLastRow() {
        suppressAutoAppendRow = true;
        try {
            if (tableModel.getRowCount() == 0 || hasContentInLastRow()) {
                tableModel.addRow(new Object[]{true, "", "", ""});
            }
        } finally {
            suppressAutoAppendRow = false;
        }
    }

    /**
     * Safely stop cell editing to prevent ArrayIndexOutOfBoundsException
     * when modifying table data while editing is in progress
     */
    private void stopCellEditing() {
        if (table.isEditing()) {
            TableCellEditor editor = table.getCellEditor();
            if (editor != null) {
                editor.stopCellEditing();
            }
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
}

