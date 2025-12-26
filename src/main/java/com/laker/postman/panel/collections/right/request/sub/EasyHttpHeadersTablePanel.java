package com.laker.postman.panel.collections.right.request.sub;

import com.laker.postman.common.component.table.AbstractEasyPostmanTablePanel;
import com.laker.postman.common.component.table.EasyPostmanTextFieldCellEditor;
import com.laker.postman.common.component.table.EasyPostmanTextFieldCellRenderer;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
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
public class EasyHttpHeadersTablePanel extends AbstractEasyPostmanTablePanel<Map<String, Object>> {

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

    // Column indices
    private static final int COL_ENABLED = 0;
    private static final int COL_KEY = 1;
    private static final int COL_VALUE = 2;
    private static final int COL_DELETE = 3;

    public EasyHttpHeadersTablePanel() {
        super(new String[]{"", "Key", "Value", ""});
        initializeComponents();
        initializeTableUI();
        setupCellRenderersAndEditors();
        setupTableListeners();
        addAutoAppendRowFeature();
    }

    // ========== 实现抽象方法 ==========

    @Override
    protected int getEnabledColumnIndex() {
        return COL_ENABLED;
    }

    @Override
    protected int getDeleteColumnIndex() {
        return COL_DELETE;
    }

    @Override
    protected boolean hasContentInRow(int row) {
        String key = getStringValue(row, COL_KEY);
        String value = getStringValue(row, COL_VALUE);
        return !key.isEmpty() || !value.isEmpty();
    }

    // ========== 初始化方法 ==========

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

                // Check if this is a default header
                Object keyObj = getValueAt(row, COL_KEY);
                if (keyObj != null) {
                    String key = keyObj.toString().trim();
                    if (DEFAULT_HEADER_KEYS.contains(key)) {
                        // For default headers: Key column not editable, Value column editable
                        return column == COL_VALUE;
                    }
                }

                // For non-default headers: Key and Value columns editable
                return column == COL_KEY || column == COL_VALUE;
            }
        };

        // Create table
        table = new JTable(tableModel);
        add(table, BorderLayout.CENTER);
    }

    @Override
    protected void initializeTableUI() {
        // 调用父类的通用UI配置
        super.initializeTableUI();

        // 设置列宽
        setEnabledColumnWidth(40);
        setDeleteColumnWidth(40);

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
            int attempts = 0;
            int maxAttempts = columnCount + table.getRowCount() * columnCount;
            do {
                nextColumn--;
                attempts++;
                if (nextColumn < 0) {
                    // Move to previous row, last editable column
                    if (currentRow > 0) {
                        currentRow--;
                        nextColumn = COL_VALUE; // Last editable column
                    } else {
                        nextColumn = COL_KEY; // Stay at first editable column
                        break;
                    }
                }
                if (attempts >= maxAttempts) {
                    // Safety break to prevent infinite loop
                    nextColumn = COL_KEY;
                    break;
                }
            } while (!isCellEditableForNavigation(currentRow, nextColumn));
        } else {
            // Move forwards
            int attempts = 0;
            int maxAttempts = columnCount + table.getRowCount() * columnCount;
            do {
                nextColumn++;
                attempts++;
                if (nextColumn >= columnCount) {
                    // Move to next row, first editable column
                    if (currentRow < table.getRowCount() - 1) {
                        currentRow++;
                        nextColumn = COL_KEY; // First editable column
                    } else {
                        nextColumn = COL_VALUE; // Stay at last editable column
                        break;
                    }
                }
                if (attempts >= maxAttempts) {
                    // Safety break to prevent infinite loop
                    nextColumn = COL_KEY;
                    break;
                }
            } while (!isCellEditableForNavigation(currentRow, nextColumn));
        }

        // Select and start editing the next cell
        if (nextColumn >= 0 && nextColumn < columnCount && currentRow >= 0 && currentRow < table.getRowCount()) {
            table.changeSelection(currentRow, nextColumn, false, false);
            if (isCellEditableForNavigation(currentRow, nextColumn)) {
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
     * Check if a cell is editable for Tab navigation
     */
    private boolean isCellEditableForNavigation(int row, int column) {
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

                // Only react to left mouse button clicks for delete action
                if (!SwingUtilities.isLeftMouseButton(e)) {
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

                    // Get row data
                    Object keyObj = tableModel.getValueAt(modelRow, COL_KEY);
                    String keyStr = keyObj == null ? "" : keyObj.toString().trim();

                    // Check if it's a default header
                    boolean isDefaultHeader = DEFAULT_HEADER_KEYS.contains(keyStr);

                    // Don't delete default headers
                    if (isDefaultHeader) {
                        return;
                    }

                    // Prevent deleting the last row (keep at least one empty row like Postman)
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

                    // Convert view row index to model row index if row sorter is used
                    int modelRow = viewRow;
                    if (table.getRowSorter() != null) {
                        modelRow = table.getRowSorter().convertRowIndexToModel(viewRow);
                    }

                    // Check if the selected row contains a default header
                    Object keyObj = tableModel.getValueAt(modelRow, COL_KEY);
                    boolean isDefaultHeaderRow = false;
                    if (keyObj != null) {
                        String key = keyObj.toString().trim();
                        isDefaultHeaderRow = DEFAULT_HEADER_KEYS.contains(key);
                    }

                    // Create appropriate popup menu based on whether it's a default header
                    JPopupMenu contextMenu = createContextPopupMenu(isDefaultHeaderRow);
                    contextMenu.show(e.getComponent(), e.getX(), e.getY());
                } else {
                    // No row selected, show normal menu for adding new row
                    JPopupMenu contextMenu = createContextPopupMenu(false);
                    contextMenu.show(e.getComponent(), e.getX(), e.getY());
                }
            }
        };

        table.addMouseListener(tableMouseListener);
    }

    /**
     * Create context menu based on whether it's a default header row
     */
    private JPopupMenu createContextPopupMenu(boolean isDefaultHeaderRow) {
        JPopupMenu menu = new JPopupMenu();

        if (isDefaultHeaderRow) {
            // For default header rows, show a disabled menu item indicating it's a default header
            JMenuItem defaultHeaderItem = new JMenuItem("Default Header (Cannot Delete)");
            defaultHeaderItem.setEnabled(false);
            menu.add(defaultHeaderItem);
        } else {
            // For normal rows, show add/remove options
            JMenuItem addItem = new JMenuItem("Add");
            addItem.addActionListener(e -> addRowAndScroll());
            menu.add(addItem);

            JMenuItem removeItem = new JMenuItem("Remove");
            removeItem.addActionListener(e -> deleteSelectedRow());
            menu.add(removeItem);
        }

        return menu;
    }

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

                // Check if it's a default header
                Object keyObj = tableModel.getValueAt(modelRow, COL_KEY);
                String keyStr = keyObj == null ? "" : keyObj.toString().trim();
                boolean isDefaultHeader = DEFAULT_HEADER_KEYS.contains(keyStr);

                // Don't delete default headers
                if (isDefaultHeader) {
                    return;
                }

                // Delete the row
                tableModel.removeRow(modelRow);
                ensureEmptyLastRow();
            }
        }
    }

    /**
     * Scroll to make the rectangle visible
     * Wrapper for backward compatibility
     */
    public void scrollRectToVisible() {
        scrollToLastRow();
    }

    /**
     * Get all rows as a list of maps (model data, not view data)
     */
    public List<Map<String, Object>> getRows() {
        // Stop cell editing to ensure any in-progress edits are committed
        // Use parent class method with recursion protection
        stopCellEditingWithProtection();

        List<Map<String, Object>> rows = new ArrayList<>();

        for (int i = 0; i < tableModel.getRowCount(); i++) {
            Map<String, Object> row = new LinkedHashMap<>();
            // Store enabled state
            row.put("Enabled", getBooleanValue(i, COL_ENABLED));
            // Store Key and Value
            row.put("Key", getStringValue(i, COL_KEY));
            row.put("Value", getStringValue(i, COL_VALUE));
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
            if (rows != null && !rows.isEmpty()) {
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
            ensureEmptyLastRow();
        } finally {
            suppressAutoAppendRow = false;
        }
    }
}
