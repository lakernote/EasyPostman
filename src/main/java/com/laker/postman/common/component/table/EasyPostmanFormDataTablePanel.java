package com.laker.postman.common.component.table;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.laker.postman.model.HttpFormData;
import com.laker.postman.util.FontsUtil;
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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.laker.postman.common.component.table.TableUIConstants.SELECT_FILE_TEXT;

/**
 * Form-Data 表格面板组件
 * 专门用于处理 form-data 类型的请求体数据
 * 支持 Enable、Key、Type(Text/File)、Value 和 Delete 列结构
 */
@Slf4j
public class EasyPostmanFormDataTablePanel extends JPanel {

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
    private static final int COL_TYPE = 2;
    private static final int COL_VALUE = 3;
    private static final int COL_DELETE = 4;

    private static final String[] TYPE_OPTIONS = {"Text", "File"};
    private static final String TYPE_TEXT = "Text";
    private static final String TYPE_FILE = "File";

    /**
     * 构造函数，创建默认的 Form-Data 表格面板
     */
    public EasyPostmanFormDataTablePanel() {
        this(true, true);
    }

    /**
     * 构造函数，支持自定义配置
     *
     * @param popupMenuEnabled     是否启用右键菜单
     * @param autoAppendRowEnabled 是否启用自动补空行
     */
    public EasyPostmanFormDataTablePanel(boolean popupMenuEnabled, boolean autoAppendRowEnabled) {
        this.columns = new String[]{"", "Key", "Type", "Value", ""};
        initializeComponents();
        initializeTableUI();
        setupCellRenderersAndEditors();
        if (popupMenuEnabled) {
            setupTableListeners();
        }
        if (autoAppendRowEnabled) {
            addAutoAppendRowFeature();
        }

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

                // Key, Type and Value columns are editable
                return column == COL_KEY || column == COL_TYPE || column == COL_VALUE;
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
        table.setRowHeight(28);
        table.setFont(FontsUtil.getDefaultFont(Font.PLAIN, 11));
        table.getTableHeader().setFont(FontsUtil.getDefaultFont(Font.BOLD, 11));
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

        table.getColumnModel().getColumn(COL_TYPE).setPreferredWidth(80);
        table.getColumnModel().getColumn(COL_TYPE).setMaxWidth(80);
        table.getColumnModel().getColumn(COL_TYPE).setMinWidth(80);

        table.getColumnModel().getColumn(COL_DELETE).setPreferredWidth(40);
        table.getColumnModel().getColumn(COL_DELETE).setMaxWidth(40);
        table.getColumnModel().getColumn(COL_DELETE).setMinWidth(40);

        // Setup Tab key navigation
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
        // Only Key, Type and Value columns are editable via Tab navigation
        return column == COL_KEY || column == COL_TYPE || column == COL_VALUE;
    }

    private void setupCellRenderersAndEditors() {
        setEmptyCellWhiteBackgroundRenderer();

        // Set editors and renderers for Key, Type, Value columns
        table.getColumnModel().getColumn(COL_KEY).setCellEditor(new EasyPostmanTextFieldCellEditor());
        table.getColumnModel().getColumn(COL_KEY).setCellRenderer(new EasyPostmanTextFieldCellRenderer());

        // Set Type column to dropdown editor with custom renderer
        JComboBox<String> typeCombo = new JComboBox<>(TYPE_OPTIONS);
        table.getColumnModel().getColumn(COL_TYPE).setCellEditor(new DefaultCellEditor(typeCombo));
        table.getColumnModel().getColumn(COL_TYPE).setCellRenderer(new TypeColumnRenderer());

        // Set Value column to dynamic text/file editor and renderer
        table.getColumnModel().getColumn(COL_VALUE).setCellEditor(new TextOrFileTableCellEditor());
        table.getColumnModel().getColumn(COL_VALUE).setCellRenderer(new TextOrFileTableCellRenderer());

        // Set custom renderer for delete column
        table.getColumnModel().getColumn(COL_DELETE).setCellRenderer(new DeleteButtonRenderer());
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

                    // Prevent deleting the last row
                    int rowCount = tableModel.getRowCount();
                    if (modelRow == rowCount - 1 && rowCount == 1) {
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

                // Show delete icon if not the last empty row
                Object keyObj = tableModel.getValueAt(modelRow, COL_KEY);
                Object valueObj = tableModel.getValueAt(modelRow, COL_VALUE);
                String keyStr = keyObj == null ? "" : keyObj.toString().trim();
                String valueStr = valueObj == null ? "" : valueObj.toString().trim();
                boolean isEmpty = keyStr.isEmpty() && valueStr.isEmpty();

                boolean shouldShowIcon = false;
                if (!isLastRow) {
                    shouldShowIcon = true;
                } else {
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
     * Custom renderer for Type column
     * 提供与其他列一致的样式和更美观的显示效果
     */
    private class TypeColumnRenderer extends DefaultTableCellRenderer {
        public TypeColumnRenderer() {
            setVerticalAlignment(SwingConstants.CENTER);
            setHorizontalAlignment(SwingConstants.LEFT);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus, int row, int column) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

            // 设置背景色 - 与其他列保持一致
            if (isSelected) {
                setBackground(table.getSelectionBackground());
                setForeground(table.getSelectionForeground());
            } else {
                setBackground(table.getBackground());
                setForeground(table.getForeground());
            }

            // 添加内边距，与其他列保持一致
            setBorder(TableUIConstants.createLabelBorder());
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
                    Object keyValue = tableModel.getValueAt(lastRow, COL_KEY);
                    Object valueValue = tableModel.getValueAt(lastRow, COL_VALUE);

                    if ((keyValue != null && !keyValue.toString().trim().isEmpty()) ||
                            (valueValue != null && !valueValue.toString().trim().isEmpty())) {
                        lastRowHasContent = true;
                    }

                    // Add empty row if last row has content
                    if (lastRowHasContent) {
                        suppressAutoAppendRow = true;
                        try {
                            tableModel.addRow(new Object[]{true, "", TYPE_TEXT, "", ""});
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

    /**
     * 添加表格模型监听器
     *
     * @param listener 表格模型监听器
     */
    public void addTableModelListener(TableModelListener listener) {
        if (tableModel != null) {
            tableModel.addTableModelListener(listener);
        }
    }

    /**
     * 清空表格数据
     */
    public void clear() {
        // Stop cell editing before modifying table structure
        stopCellEditing();

        suppressAutoAppendRow = true;
        try {
            tableModel.setRowCount(0);
            // Add an empty row after clearing
            tableModel.addRow(new Object[]{true, "", TYPE_TEXT, "", ""});
        } finally {
            suppressAutoAppendRow = false;
        }
    }

    /**
     * 添加一行数据 (内部使用)
     *
     * @param values 行数据
     */
    private void addRow(Object... values) {
        if (values == null || values.length == 0) {
            tableModel.addRow(new Object[]{true, "", TYPE_TEXT, "", ""});
        } else if (values.length == 3) {
            // Legacy support: key, type, value
            tableModel.addRow(new Object[]{true, values[0], values[1], values[2], ""});
        } else if (values.length == 4) {
            // New format: enabled, key, type, value
            tableModel.addRow(new Object[]{values[0], values[1], values[2], values[3], ""});
        } else {
            // Ensure we have exactly 5 columns
            Object[] row = new Object[5];
            row[0] = true; // Default enabled
            for (int i = 0; i < Math.min(values.length, 3); i++) {
                row[i + 1] = values[i];
            }
            // Fill remaining columns with empty strings
            for (int i = values.length; i < 4; i++) {
                row[i + 1] = "";
            }
            row[4] = ""; // Delete column
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

                // Don't delete if it's the only row
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
     * 获取所有行数据
     *
     * @return 所有行数据的列表
     */
    public List<Map<String, Object>> getRows() {
        List<Map<String, Object>> rows = new ArrayList<>();

        for (int i = 0; i < tableModel.getRowCount(); i++) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("Enabled", tableModel.getValueAt(i, COL_ENABLED));
            row.put("Key", tableModel.getValueAt(i, COL_KEY));
            row.put("Type", tableModel.getValueAt(i, COL_TYPE));
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
                        enabled = true;
                    }
                    Object key = row.get("Key");
                    Object type = row.get("Type");
                    if (type == null) {
                        type = TYPE_TEXT;
                    }
                    Object value = row.get("Value");

                    tableModel.addRow(new Object[]{enabled, key, type, value, ""});
                }
            }

            // Ensure there's always an empty row at the end
            if (tableModel.getRowCount() == 0 || hasContentInLastRow()) {
                tableModel.addRow(new Object[]{true, "", TYPE_TEXT, "", ""});
            }
        } finally {
            suppressAutoAppendRow = false;
        }
    }

    /**
     * 获取 Form-Data 中的文本数据（仅返回已启用的行）
     * 保持向后兼容性
     *
     * @return Key-Value 形式的文本数据
     */
    public Map<String, String> getFormData() {
        Map<String, String> formData = new LinkedHashMap<>();
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            Object enabledObj = tableModel.getValueAt(i, COL_ENABLED);
            Object keyObj = tableModel.getValueAt(i, COL_KEY);
            Object typeObj = tableModel.getValueAt(i, COL_TYPE);
            Object valueObj = tableModel.getValueAt(i, COL_VALUE);

            boolean enabled = enabledObj instanceof Boolean ? (Boolean) enabledObj : true;
            String key = keyObj == null ? "" : keyObj.toString().trim();
            String type = typeObj == null ? TYPE_TEXT : typeObj.toString();
            String value = valueObj == null ? "" : valueObj.toString();

            if (enabled && !key.isEmpty() && TYPE_TEXT.equals(type)) {
                formData.put(key, value);
            }
        }
        return formData;
    }

    /**
     * 获取 Form-Data 中的文件数据（仅返回已启用的行）
     * 保持向后兼容性
     *
     * @return Key-FilePath 形式的文件数据
     */
    public Map<String, String> getFormFiles() {
        Map<String, String> formFiles = new LinkedHashMap<>();
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            Object enabledObj = tableModel.getValueAt(i, COL_ENABLED);
            Object keyObj = tableModel.getValueAt(i, COL_KEY);
            Object typeObj = tableModel.getValueAt(i, COL_TYPE);
            Object valueObj = tableModel.getValueAt(i, COL_VALUE);

            boolean enabled = enabledObj instanceof Boolean ? (Boolean) enabledObj : true;
            String key = keyObj == null ? "" : keyObj.toString().trim();
            String type = typeObj == null ? TYPE_TEXT : typeObj.toString();
            String value = valueObj == null ? "" : valueObj.toString();

            if (enabled && !key.isEmpty() && TYPE_FILE.equals(type) && !value.isEmpty() && !value.equals(SELECT_FILE_TEXT)) {
                formFiles.put(key, value);
            }
        }
        return formFiles;
    }

    /**
     * Get form-data list with enabled state (new format)
     */
    public List<HttpFormData> getFormDataList() {
        List<HttpFormData> dataList = new ArrayList<>();
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            Object enabledObj = tableModel.getValueAt(i, COL_ENABLED);
            Object keyObj = tableModel.getValueAt(i, COL_KEY);
            Object typeObj = tableModel.getValueAt(i, COL_TYPE);
            Object valueObj = tableModel.getValueAt(i, COL_VALUE);

            boolean enabled = enabledObj instanceof Boolean ? (Boolean) enabledObj : true;
            String key = keyObj == null ? "" : keyObj.toString().trim();
            String type = typeObj == null ? TYPE_TEXT : typeObj.toString();
            String value = valueObj == null ? "" : valueObj.toString();

            // Only add non-empty params
            if (!key.isEmpty()) {
                dataList.add(new HttpFormData(enabled, key, type, value));
            }
        }
        return dataList;
    }

    /**
     * Set form-data list with enabled state (new format)
     */
    public void setFormDataList(List<HttpFormData> dataList) {
        // Stop cell editing before modifying table structure
        stopCellEditing();

        suppressAutoAppendRow = true;
        try {
            tableModel.setRowCount(0);
            if (dataList != null) {
                for (HttpFormData param : dataList) {
                    tableModel.addRow(new Object[]{
                            param.isEnabled(),
                            param.getKey(),
                            param.getType(),
                            param.getValue(),
                            ""
                    });
                }
            }

            // Ensure there's always an empty row at the end
            if (tableModel.getRowCount() == 0 || hasContentInLastRow()) {
                tableModel.addRow(new Object[]{true, "", TYPE_TEXT, "", ""});
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
     * Ensure there's always an empty row at the end of the table
     */
    private void ensureEmptyLastRow() {
        suppressAutoAppendRow = true;
        try {
            if (tableModel.getRowCount() == 0 || hasContentInLastRow()) {
                tableModel.addRow(new Object[]{true, "", TYPE_TEXT, "", ""});
            }
        } finally {
            suppressAutoAppendRow = false;
        }
    }

    /**
     * Safely stop cell editing
     */
    private void stopCellEditing() {
        if (table.isEditing()) {
            TableCellEditor editor = table.getCellEditor();
            if (editor != null) {
                editor.stopCellEditing();
            }
        }
    }
}
