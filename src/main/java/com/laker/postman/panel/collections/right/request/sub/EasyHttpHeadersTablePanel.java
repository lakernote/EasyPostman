package com.laker.postman.panel.collections.right.request.sub;

import com.laker.postman.common.component.table.AbstractTablePanel;
import com.laker.postman.common.component.table.HttpHeaderKeyEasyTextFieldCellEditor;
import com.laker.postman.common.component.table.EasyTextFieldCellRenderer;
import com.laker.postman.common.component.table.HttpHeaderValueEasyTextFieldCellEditor;
import com.laker.postman.util.HttpHeaderConstants;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import java.util.*;

@Slf4j
public class EasyHttpHeadersTablePanel extends AbstractTablePanel<Map<String, Object>> {

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

    // Parent panel reference for handling default headers
    private EasyRequestHttpHeadersPanel parentPanel;

    public EasyHttpHeadersTablePanel() {
        super(new String[]{"", "Key", "Value", ""});
        initializeComponents();
        initializeTableUI();
        setupCellRenderersAndEditors();
        setupTableListeners();
        addAutoAppendRowFeature();
        setupDefaultHeaderDetection();
    }

    /**
     * Set parent panel reference
     */
    public void setParentPanel(EasyRequestHttpHeadersPanel parentPanel) {
        this.parentPanel = parentPanel;
    }

    /**
     * 设置默认 header 检测
     * 当用户输入的 key 与默认 header 相同时：
     * 1. 检测到已存在同名的默认 header
     * 2. 自动展开默认 headers（如果是隐藏状态）
     * 3. 清空当前输入
     * 4. 聚焦到已存在的默认 header 行
     */
    private void setupDefaultHeaderDetection() {
        tableModel.addTableModelListener(e -> {
            // 只处理 Key 列的更新事件
            if (e.getType() != javax.swing.event.TableModelEvent.UPDATE) {
                return;
            }

            int column = e.getColumn();
            if (column != COL_KEY) {
                return;
            }

            int row = e.getFirstRow();
            if (row < 0 || row >= tableModel.getRowCount()) {
                return;
            }

            // 延迟处理，避免在编辑过程中触发
            SwingUtilities.invokeLater(() -> handleDefaultHeaderInput(row));
        });
    }

    /**
     * 处理默认 header 输入
     * 当用户在自定义 header 行输入默认 header 名称时，自动跳转到对应的默认 header 行
     */
    private void handleDefaultHeaderInput(int row) {
        if (parentPanel == null) {
            return;
        }

        try {
            Object keyObj = tableModel.getValueAt(row, COL_KEY);
            if (keyObj == null) {
                return;
            }

            String inputKey = keyObj.toString().trim();
            if (inputKey.isEmpty()) {
                return;
            }

            // 检查是否是默认 header key
            boolean isDefaultKey = DEFAULT_HEADER_KEYS.stream()
                    .anyMatch(defaultKey -> defaultKey.equalsIgnoreCase(inputKey));

            if (!isDefaultKey) {
                return;
            }

            // 查找是否已存在同名的默认 header（排除当前行）
            int existingRow = findExistingHeaderRow(inputKey, row);
            if (existingRow < 0) {
                // 没有找到已存在的，说明可能是用户想修改默认 header 的值
                return;
            }

            // 找到了已存在的默认 header
            // 1. 清空当前行
            tableModel.setValueAt("", row, COL_KEY);
            tableModel.setValueAt("", row, COL_VALUE);

            // 2. 确保默认 headers 可见
            parentPanel.ensureDefaultHeadersVisible();

            // 3. 延迟聚焦到默认 header 行
            focusOnExistingHeader(inputKey);

        } catch (Exception ex) {
            log.warn("Error handling default header input", ex);
        }
    }

    /**
     * 聚焦到已存在的 header（带延迟以等待UI更新完成）
     */
    private void focusOnExistingHeader(String headerKey) {
        SwingUtilities.invokeLater(() -> {
            try {
                // 短暂等待，确保展开动画完成
                Thread.sleep(100);
                parentPanel.focusOnHeader(headerKey);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                log.warn("Interrupted while focusing on header", ex);
            }
        });
    }

    /**
     * 查找已存在的 header 行（排除指定行）
     */
    private int findExistingHeaderRow(String key, int excludeRow) {
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            if (i == excludeRow) {
                continue;
            }

            Object keyObj = tableModel.getValueAt(i, COL_KEY);
            if (keyObj != null && key.equalsIgnoreCase(keyObj.toString().trim())) {
                return i;
            }
        }
        return -1;
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
        return column == COL_KEY || column == COL_VALUE;
    }

    @Override
    protected boolean hasContentInRow(int row) {
        String key = getStringValue(row, COL_KEY);
        String value = getStringValue(row, COL_VALUE);
        return !key.isEmpty() || !value.isEmpty();
    }

    // ========== 初始化方法 ==========

    @Override
    protected boolean isCellEditable(int row, int column) {
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
        Object keyObj = tableModel.getValueAt(row, COL_KEY);
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

    @Override
    protected boolean canDeleteRow(int modelRow) {
        // Check if it's a default header
        Object keyObj = tableModel.getValueAt(modelRow, COL_KEY);
        String keyStr = keyObj == null ? "" : keyObj.toString().trim();
        return !DEFAULT_HEADER_KEYS.contains(keyStr);
    }

    @Override
    protected JPopupMenu createContextPopupMenu(int viewRow) {
        JPopupMenu menu = new JPopupMenu();

        // Convert view row to model row if needed
        int modelRow = viewRow;
        if (viewRow >= 0 && table.getRowSorter() != null) {
            modelRow = table.getRowSorter().convertRowIndexToModel(viewRow);
        }

        // Check if the selected row contains a default header
        boolean isDefaultHeaderRow = false;
        if (modelRow >= 0 && modelRow < tableModel.getRowCount()) {
            Object keyObj = tableModel.getValueAt(modelRow, COL_KEY);
            if (keyObj != null) {
                String key = keyObj.toString().trim();
                isDefaultHeaderRow = DEFAULT_HEADER_KEYS.contains(key);
            }
        }

        if (isDefaultHeaderRow) {
            // For default header rows, show a disabled menu item
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

    @Override
    protected void initializeTableUI() {
        // 调用父类的通用UI配置
        super.initializeTableUI();

        // 设置列宽
        setEnabledColumnWidth(40);
        setDeleteColumnWidth();

        // Setup Tab key navigation to move between columns in the same row
        setupTabKeyNavigation();
    }

    /**
     * 设置单元格渲染器和编辑器
     * - Key 列：使用智能自动补全编辑器，提供常见的 HTTP Header 名称建议
     * - Value 列：使用上下文感知编辑器，根据 Key 提供对应的值建议
     */
    private void setupCellRenderersAndEditors() {
        // Key 列：支持自动补全的编辑器（显示常见 HTTP Header 名称）
        setColumnEditor(COL_KEY, new HttpHeaderKeyEasyTextFieldCellEditor(HttpHeaderConstants.COMMON_HEADERS));

        // Value 列：智能编辑器（根据 Key 动态提供建议）
        setColumnEditor(COL_VALUE, new HttpHeaderValueEasyTextFieldCellEditor(COL_KEY));

        // 设置渲染器以支持变量高亮等功能
        setColumnRenderer(COL_KEY, new EasyTextFieldCellRenderer());
        setColumnRenderer(COL_VALUE, new EasyTextFieldCellRenderer());

        // 删除列使用自定义按钮渲染器
        setColumnRenderer(COL_DELETE, new DeleteButtonRenderer());
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
