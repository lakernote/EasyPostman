package com.laker.postman.common.component.table;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.laker.postman.model.HttpFormData;
import com.laker.postman.util.FontsUtil;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * Form-Data 表格面板组件
 * 专门用于处理 form-data 类型的请求体数据
 * 支持 Enable、Key、Type(Text/File)、Value 和 Delete 列结构
 */
@Slf4j
public class EasyPostmanFormDataTablePanel extends AbstractEasyPostmanTablePanel<HttpFormData> {

    // Column indices
    private static final int COL_ENABLED = 0;
    private static final int COL_KEY = 1;
    private static final int COL_TYPE = 2;
    private static final int COL_VALUE = 3;
    private static final int COL_DELETE = 4;

    // Use constants from HttpFormData to avoid duplication
    private static final String[] TYPE_OPTIONS = {HttpFormData.TYPE_TEXT, HttpFormData.TYPE_FILE};

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
        super(new String[]{"", "Key", "Type", "Value", ""});
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
    protected int getFirstEditableColumnIndex() {
        return COL_KEY;
    }

    @Override
    protected int getLastEditableColumnIndex() {
        return COL_VALUE;
    }

    @Override
    protected boolean isCellEditableForNavigation(int row, int column) {
        // COL_KEY, COL_TYPE, COL_VALUE are editable
        return column == COL_KEY || column == COL_TYPE || column == COL_VALUE;
    }

    @Override
    protected boolean hasContentInRow(int row) {
        String key = getStringValue(row, COL_KEY);
        String value = getStringValue(row, COL_VALUE);
        return !key.isEmpty() || !value.isEmpty();
    }

    @Override
    protected Object[] createEmptyRow() {
        // FormData has 5 columns including Type column with default value
        return new Object[]{true, "", HttpFormData.TYPE_TEXT, "", ""};
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

    @Override
    protected void initializeTableUI() {
        // 调用父类的通用UI配置
        super.initializeTableUI();

        // 设置列宽
        setEnabledColumnWidth(40);

        // Type 列特殊宽度
        table.getColumnModel().getColumn(COL_TYPE).setPreferredWidth(80);
        table.getColumnModel().getColumn(COL_TYPE).setMaxWidth(80);
        table.getColumnModel().getColumn(COL_TYPE).setMinWidth(80);

        setDeleteColumnWidth(40);

        // Setup Tab key navigation
        setupTabKeyNavigation();
    }


    private void setupCellRenderersAndEditors() {
        setEmptyCellWhiteBackgroundRenderer();

        // Set editors and renderers for Key, Type, Value columns
        table.getColumnModel().getColumn(COL_KEY).setCellEditor(new EasyPostmanTextFieldCellEditor());
        table.getColumnModel().getColumn(COL_KEY).setCellRenderer(new EasyPostmanTextFieldCellRenderer());

        // Set Type column to dropdown editor with custom renderer
        JComboBox<String> typeCombo = createModernTypeComboBox();
        table.getColumnModel().getColumn(COL_TYPE).setCellEditor(new DefaultCellEditor(typeCombo));
        table.getColumnModel().getColumn(COL_TYPE).setCellRenderer(new TypeColumnRenderer());

        // Set Value column to dynamic text/file editor and renderer
        table.getColumnModel().getColumn(COL_VALUE).setCellEditor(new TextOrFileTableCellEditor());
        table.getColumnModel().getColumn(COL_VALUE).setCellRenderer(new TextOrFileTableCellRenderer());

        // Set custom renderer for delete column
        table.getColumnModel().getColumn(COL_DELETE).setCellRenderer(new DeleteButtonRenderer());
    }

    /**
     * Create a modern-styled ComboBox for the Type column
     */
    private JComboBox<String> createModernTypeComboBox() {
        JComboBox<String> comboBox = new JComboBox<>(TYPE_OPTIONS);
        comboBox.setFont(FontsUtil.getDefaultFont(Font.PLAIN, 11));

        // 自定义下拉列表的渲染器
        comboBox.setRenderer(new DefaultListCellRenderer() {
            private final Icon textIcon = new FlatSVGIcon("icons/file.svg", 16, 16);
            private final Icon fileIcon = new FlatSVGIcon("icons/binary.svg", 16, 16);

            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value,
                                                          int index, boolean isSelected, boolean cellHasFocus) {
                JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

                String typeValue = value != null ? value.toString() : "";

                // 设置图标和文本颜色
                if (HttpFormData.TYPE_FILE.equalsIgnoreCase(typeValue)) {
                    label.setIcon(fileIcon);
                } else {
                    label.setIcon(textIcon);
                }

                return label;
            }
        });

        return comboBox;
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
     * Type列的自定义渲染器，显示 Text/File 图标和文本
     */
    private class TypeColumnRenderer extends JPanel implements TableCellRenderer {
        private final JLabel iconLabel;
        private final JLabel textLabel;
        private final Icon textIcon;
        private final Icon fileIcon;

        public TypeColumnRenderer() {
            setLayout(new BorderLayout(4, 0));
            setOpaque(true);

            // 创建图标标签
            iconLabel = new JLabel();
            iconLabel.setHorizontalAlignment(SwingConstants.CENTER);
            iconLabel.setPreferredSize(new Dimension(20, 20));

            // 创建文本标签
            textLabel = new JLabel();
            textLabel.setFont(FontsUtil.getDefaultFontWithOffset(Font.PLAIN, -1)); // 比标准字体小1号
            textLabel.setVerticalAlignment(SwingConstants.CENTER);

            // 加载图标
            textIcon = new FlatSVGIcon("icons/file.svg", 16, 16);
            fileIcon = new FlatSVGIcon("icons/binary.svg", 16, 16);

            // 添加组件
            add(iconLabel, BorderLayout.WEST);
            add(textLabel, BorderLayout.CENTER);

        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus, int row, int column) {
            String typeValue = value != null ? value.toString() : HttpFormData.TYPE_TEXT;

            // 根据类型设置图标和文本
            if (HttpFormData.TYPE_FILE.equalsIgnoreCase(typeValue)) {
                iconLabel.setIcon(fileIcon);
                textLabel.setText("File");
            } else {
                iconLabel.setIcon(textIcon);
                textLabel.setText("Text");
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
     * <p>
     * /**
     * 添加一行数据 (内部使用)
     *
     * @param values 行数据
     */
    private void addRow(Object... values) {
        if (values == null || values.length == 0) {
            tableModel.addRow(new Object[]{true, "", HttpFormData.TYPE_TEXT, "", ""});
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
     * Get form-data list with enabled state (new format)
     */
    public List<HttpFormData> getFormDataList() {
        // Stop cell editing to ensure any in-progress edits are committed
        // Use parent class method with recursion protection
        stopCellEditingWithProtection();

        List<HttpFormData> dataList = new ArrayList<>();
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            boolean enabled = getBooleanValue(i, COL_ENABLED);
            String key = getStringValue(i, COL_KEY);
            String type = getStringValue(i, COL_TYPE);
            if (type.isEmpty()) {
                type = HttpFormData.TYPE_TEXT;
            }
            String value = getStringValue(i, COL_VALUE);

            // Only add non-empty params
            if (!key.isEmpty()) {
                // Normalize type to ensure consistency
                String normalizedType = HttpFormData.normalizeType(type);
                dataList.add(new HttpFormData(enabled, key, normalizedType, value));
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
                    // Normalize type to ensure consistency (Text/File with capital first letter)
                    String normalizedType = HttpFormData.normalizeType(param.getType());
                    tableModel.addRow(new Object[]{
                            param.isEnabled(),
                            param.getKey(),
                            normalizedType,
                            param.getValue(),
                            ""
                    });
                }
            }

            // Ensure there's always an empty row at the end
            if (tableModel.getRowCount() == 0 || hasContentInLastRow()) {
                tableModel.addRow(new Object[]{true, "", HttpFormData.TYPE_TEXT, "", ""});
            }
        } finally {
            suppressAutoAppendRow = false;
        }
    }
}
