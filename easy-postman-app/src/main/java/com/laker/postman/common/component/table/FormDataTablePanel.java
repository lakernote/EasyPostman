package com.laker.postman.common.component.table;

import com.laker.postman.request.model.HttpFormData;


import com.laker.postman.util.FontsUtil;
import com.laker.postman.util.IconUtil;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
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
public class FormDataTablePanel extends AbstractTablePanel<HttpFormData> {
    // Column indices
    private static final int COL_ENABLED = 0;
    private static final int COL_KEY = 1;
    private static final int COL_TYPE = 2;
    private static final int COL_VALUE = 3;
    private static final int COL_DESCRIPTION = 4;
    private static final int COL_DELETE = 5;

    // Use constants from HttpFormData to avoid duplication
    private static final String[] TYPE_OPTIONS = {HttpFormData.TYPE_TEXT, HttpFormData.TYPE_FILE};
    private static final int TYPE_COLUMN_WIDTH = 64;

    /**
     * 构造函数，创建默认的 Form-Data 表格面板
     */
    public FormDataTablePanel() {
        this(true, true);
    }

    /**
     * 构造函数，支持自定义配置
     *
     * @param popupMenuEnabled     是否启用右键菜单
     * @param autoAppendRowEnabled 是否启用自动补空行
     */
    public FormDataTablePanel(boolean popupMenuEnabled, boolean autoAppendRowEnabled) {
        super(new String[]{
                "",
                I18nUtil.getMessage(MessageKeys.REQUEST_TABLE_COLUMN_KEY),
                "",
                I18nUtil.getMessage(MessageKeys.REQUEST_TABLE_COLUMN_VALUE),
                I18nUtil.getMessage(MessageKeys.REQUEST_TABLE_COLUMN_DESCRIPTION),
                ""
        });
        initializeComponents();
        initializeTableUI();
        setupCellRenderersAndEditors();
        if (popupMenuEnabled) {
            setupTableListeners();
        } else {
            addDeleteButtonListener();
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
        return COL_DESCRIPTION;
    }

    @Override
    protected int getEnterTargetColumnIndex() {
        return COL_VALUE;
    }

    @Override
    protected boolean isCellEditableForNavigation(int row, int column) {
        // COL_KEY, COL_TYPE, COL_VALUE are editable
        return column == COL_KEY || column == COL_TYPE || column == COL_VALUE || column == COL_DESCRIPTION;
    }

    @Override
    protected boolean hasContentInRow(int row) {
        String key = getStringValue(row, COL_KEY);
        String value = getStringValue(row, COL_VALUE);
        String description = getStringValue(row, COL_DESCRIPTION);
        return !key.isEmpty() || !value.isEmpty() || !description.isEmpty();
    }

    @Override
    protected Object[] createEmptyRow() {
        // FormData has a Type column with default value.
        return new Object[]{true, "", HttpFormData.TYPE_TEXT, "", "", ""};
    }

    // ========== 初始化方法 ==========
    @Override
    protected void initializeTableUI() {
        // 调用父类的通用UI配置
        super.initializeTableUI();

        // 设置列宽
        setEnabledColumnWidth(40);

        setFlexibleColumnWidth(COL_KEY, 260, 140);
        setFixedColumnWidth(COL_TYPE, TYPE_COLUMN_WIDTH);
        setFlexibleColumnWidth(COL_VALUE, 520, 180);
        setFlexibleColumnWidth(COL_DESCRIPTION, 320, 160);

        setDeleteColumnWidth();
        installPostmanLikeHeaderGrouping();

        // Setup Tab key navigation
        setupTabKeyNavigation();
    }

    private void setFixedColumnWidth(int columnIndex, int width) {
        TableColumn column = table.getColumnModel().getColumn(columnIndex);
        column.setPreferredWidth(width);
        column.setMaxWidth(width);
        column.setMinWidth(width);
        column.setResizable(false);
    }

    private void setFlexibleColumnWidth(int columnIndex, int preferredWidth, int minWidth) {
        TableColumn column = table.getColumnModel().getColumn(columnIndex);
        column.setPreferredWidth(preferredWidth);
        column.setMinWidth(minWidth);
    }

    private void installPostmanLikeHeaderGrouping() {
        table.getColumnModel().getColumn(COL_KEY).setHeaderRenderer(
                new FormDataHeaderRenderer(I18nUtil.getMessage(MessageKeys.REQUEST_TABLE_COLUMN_KEY), false,
                        TableUIConstants.PADDING_LEFT, 0)
        );
        table.getColumnModel().getColumn(COL_TYPE).setHeaderRenderer(
                new FormDataHeaderRenderer("", true, 0, TableUIConstants.PADDING_RIGHT)
        );
    }

    private void setupCellRenderersAndEditors() {
        // Set editors and renderers for Key, Type, Value columns
        table.getColumnModel().getColumn(COL_KEY).setCellEditor(new EasyPostmanTextFieldCellEditor());
        table.getColumnModel().getColumn(COL_KEY).setCellRenderer(new EasyTextFieldCellRenderer());

        // Set Type column to dropdown editor with custom renderer
        JComboBox<String> typeCombo = createModernTypeComboBox();
        table.getColumnModel().getColumn(COL_TYPE).setCellEditor(new DefaultCellEditor(typeCombo));
        table.getColumnModel().getColumn(COL_TYPE).setCellRenderer(new TypeColumnRenderer());

        // Set Value column to dynamic text/file editor and renderer
        table.getColumnModel().getColumn(COL_VALUE).setCellEditor(new TextOrFileTableCellEditor());
        table.getColumnModel().getColumn(COL_VALUE).setCellRenderer(new TextOrFileTableCellRenderer());

        table.getColumnModel().getColumn(COL_DESCRIPTION).setCellEditor(new EasySmartValueCellEditor());
        table.getColumnModel().getColumn(COL_DESCRIPTION).setCellRenderer(new EasyTextFieldCellRenderer());

        // Set custom renderer for delete column
        table.getColumnModel().getColumn(COL_DELETE).setCellRenderer(new DeleteButtonRenderer());
        installTypeColumnPopupBehavior();
    }

    /**
     * Create a modern-styled ComboBox for the Type column
     */
    private JComboBox<String> createModernTypeComboBox() {
        JComboBox<String> comboBox = new JComboBox<>(TYPE_OPTIONS);
        comboBox.setFont(FontsUtil.getDefaultFontWithOffset(Font.PLAIN, -1));
        comboBox.setMaximumRowCount(TYPE_OPTIONS.length);
        comboBox.setBorder(BorderFactory.createEmptyBorder());

        // 自定义下拉列表的渲染器
        comboBox.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value,
                                                          int index, boolean isSelected, boolean cellHasFocus) {
                JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                label.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
                label.setIcon(null);
                return label;
            }
        });

        return comboBox;
    }

    private void installTypeColumnPopupBehavior() {
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (!editable || !SwingUtilities.isLeftMouseButton(e)) {
                    return;
                }
                int row = table.rowAtPoint(e.getPoint());
                int column = table.columnAtPoint(e.getPoint());
                if (row < 0 || column != COL_TYPE || !table.isCellEditable(row, column)) {
                    return;
                }
                SwingUtilities.invokeLater(() -> showTypePopup(row));
            }
        });
    }

    private void showTypePopup(int row) {
        if (row < 0 || row >= table.getRowCount()) {
            return;
        }
        if (!table.isEditing() || table.getEditingRow() != row || table.getEditingColumn() != COL_TYPE) {
            table.editCellAt(row, COL_TYPE);
        }
        Component editor = table.getEditorComponent();
        if (editor instanceof JComboBox<?> comboBox && comboBox.isShowing()) {
            comboBox.showPopup();
        }
    }

    /**
     * Type列的自定义渲染器，显示紧凑的 Text/File 下拉入口
     */
    private class TypeColumnRenderer extends JPanel implements TableCellRenderer {
        private final JLabel textLabel;
        private final JLabel arrowLabel;

        public TypeColumnRenderer() {
            setLayout(new BorderLayout(2, 0));
            setOpaque(true);
            setBorder(BorderFactory.createEmptyBorder(0, 6, 0, 4));

            // 创建文本标签
            textLabel = new JLabel();
            textLabel.setFont(FontsUtil.getDefaultFontWithOffset(Font.PLAIN, -1)); // 比标准字体小1号
            textLabel.setVerticalAlignment(SwingConstants.CENTER);
            textLabel.setOpaque(false);

            arrowLabel = new JLabel(IconUtil.createThemed("icons/chevron-down.svg", 10, 10));
            arrowLabel.setHorizontalAlignment(SwingConstants.RIGHT);
            arrowLabel.setOpaque(false);

            // 添加组件
            add(textLabel, BorderLayout.CENTER);
            add(arrowLabel, BorderLayout.EAST);

        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus, int row, int column) {
            String typeValue = value != null ? value.toString() : HttpFormData.TYPE_TEXT;
            Color background = TableUIConstants.getCellBackground(isSelected, row == hoveredRow, false, table, row);
            Color foreground = isSelected ? table.getSelectionForeground() : table.getForeground();
            setBackground(background);
            textLabel.setForeground(foreground);
            arrowLabel.setForeground(foreground);

            // 根据类型设置图标和文本
            if (HttpFormData.TYPE_FILE.equalsIgnoreCase(typeValue)) {
                textLabel.setText("File");
            } else {
                textLabel.setText("Text");
            }

            return this;
        }
    }

    private static class FormDataHeaderRenderer extends JLabel implements TableCellRenderer {
        private final boolean rightBoundary;
        private final int leftPadding;
        private final int rightPadding;

        private FormDataHeaderRenderer(String text, boolean rightBoundary, int leftPadding, int rightPadding) {
            super(text);
            this.rightBoundary = rightBoundary;
            this.leftPadding = leftPadding;
            this.rightPadding = rightPadding;
            setOpaque(true);
            setHorizontalAlignment(SwingConstants.LEADING);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                       boolean hasFocus, int row, int column) {
            setFont(table.getTableHeader().getFont());
            setBackground(table.getTableHeader().getBackground());
            setForeground(table.getTableHeader().getForeground());
            setBorder(TableUIConstants.createFormDataGroupedHeaderBorder(
                    table.getGridColor(), rightBoundary, leftPadding, rightPadding));
            return this;
        }
    }


    /**
     * 从 tableModel 直接读取，不停止单元格编辑。
     * 用于 tab 指示器等后台场景，避免打断用户正在进行的输入（如 Tab 导航）。
     */
    public List<HttpFormData> getFormDataListFromModel() {
        List<HttpFormData> dataList = new ArrayList<>();
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            boolean enabled = getBooleanValue(i, COL_ENABLED);
            String key = getStringValue(i, COL_KEY);
            String type = getStringValue(i, COL_TYPE);
            if (type.isEmpty()) type = HttpFormData.TYPE_TEXT;
            String value = getStringValue(i, COL_VALUE);
            String description = getStringValue(i, COL_DESCRIPTION);
            if (!key.isEmpty()) {
                dataList.add(new HttpFormData(enabled, key, HttpFormData.normalizeType(type), value, description));
            }
        }
        return dataList;
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
            String description = getStringValue(i, COL_DESCRIPTION);

            // Only add non-empty params
            if (!key.isEmpty()) {
                // Normalize type to ensure consistency
                String normalizedType = HttpFormData.normalizeType(type);
                dataList.add(new HttpFormData(enabled, key, normalizedType, value, description));
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
                            param.getDescription(),
                            ""
                    });
                }
            }

            // Ensure there's always an empty row at the end
            if (tableModel.getRowCount() == 0 || hasContentInLastRow()) {
                tableModel.addRow(createEmptyRow());
            }
        } finally {
            suppressAutoAppendRow = false;
        }
    }
}
