package com.laker.postman.common.component.table;

import com.laker.postman.model.HttpFormData;
import com.laker.postman.util.FontsUtil;
import com.laker.postman.util.IconUtil;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
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
    private static final int COL_DELETE = 4;

    // Use constants from HttpFormData to avoid duplication
    private static final String[] TYPE_OPTIONS = {HttpFormData.TYPE_TEXT, HttpFormData.TYPE_FILE};

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

        setDeleteColumnWidth();

        // Setup Tab key navigation
        setupTabKeyNavigation();
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

        // Set custom renderer for delete column
        table.getColumnModel().getColumn(COL_DELETE).setCellRenderer(new DeleteButtonRenderer());
    }

    /**
     * Create a modern-styled ComboBox for the Type column
     */
    private JComboBox<String> createModernTypeComboBox() {
        JComboBox<String> comboBox = new JComboBox<>(TYPE_OPTIONS);
        comboBox.setFont(FontsUtil.getDefaultFontWithOffset(Font.PLAIN, -1));

        // 自定义下拉列表的渲染器
        comboBox.setRenderer(new DefaultListCellRenderer() {
            private final Icon textIcon = IconUtil.createThemed("icons/file.svg", IconUtil.SIZE_SMALL, IconUtil.SIZE_SMALL);
            private final Icon fileIcon = IconUtil.createThemed("icons/binary.svg", IconUtil.SIZE_SMALL, IconUtil.SIZE_SMALL);

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

            // 使用 IconUtil 创建主题适配的图标
            textIcon = IconUtil.createThemed("icons/file.svg", IconUtil.SIZE_SMALL, IconUtil.SIZE_SMALL);
            fileIcon = IconUtil.createThemed("icons/binary.svg", IconUtil.SIZE_SMALL, IconUtil.SIZE_SMALL);

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
