package com.laker.postman.common.component.table;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.laker.postman.util.FontsUtil;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import java.awt.*;

/**
 * 所有 EasyPostman 表格面板的抽象基类
 * 提供通用的表格UI配置、工具方法和递归保护机制
 *
 * @param <T> 表格数据模型类型
 * @author Laker
 */
@Slf4j
public abstract class AbstractEasyPostmanTablePanel<T> extends JPanel {

    // ========== 共同字段 ==========

    /**
     * 表格数据模型
     */
    protected DefaultTableModel tableModel;

    /**
     * 表格组件
     */
    @Getter
    protected JTable table;

    /**
     * 列名数组
     */
    protected final String[] columns;

    /**
     * 表格是否可编辑
     */
    @Getter
    protected boolean editable = true;

    /**
     * 批量操作时抑制自动追加空行的标志
     */
    protected boolean suppressAutoAppendRow = false;

    /**
     * 防止递归调用 stopCellEditing 的标志
     */
    protected boolean isStoppingCellEdit = false;

    // ========== 构造函数 ==========

    /**
     * 构造函数
     *
     * @param columns 列名数组
     */
    protected AbstractEasyPostmanTablePanel(String[] columns) {
        this.columns = columns;
    }

    // ========== 抽象方法（子类必须实现）==========

    /**
     * 获取启用列的索引
     *
     * @return 启用列索引
     */
    protected abstract int getEnabledColumnIndex();

    /**
     * 获取删除列的索引
     *
     * @return 删除列索引
     */
    protected abstract int getDeleteColumnIndex();

    /**
     * 获取第一个可编辑列的索引（用于Tab导航）
     *
     * @return 第一个可编辑列索引
     */
    protected abstract int getFirstEditableColumnIndex();

    /**
     * 获取最后一个可编辑列的索引（用于Tab导航）
     *
     * @return 最后一个可编辑列索引
     */
    protected abstract int getLastEditableColumnIndex();

    /**
     * 检查指定单元格是否可编辑（用于Tab导航）
     *
     * @param row    行索引
     * @param column 列索引
     * @return true 如果可编辑
     */
    protected abstract boolean isCellEditableForNavigation(int row, int column);

    /**
     * 检查指定行是否有内容
     *
     * @param row 行索引
     * @return true 如果有内容
     */
    protected abstract boolean hasContentInRow(int row);

    // ========== 通用 UI 配置方法 ==========

    /**
     * 初始化表格 UI
     * 所有表格共用的 UI 配置
     */
    protected void initializeTableUI() {
        table.setFillsViewportHeight(true);
        table.setRowHeight(28);
        table.setFont(FontsUtil.getDefaultFontWithOffset(Font.PLAIN, -1)); // 比标准字体小1号
        table.getTableHeader().setFont(FontsUtil.getDefaultFontWithOffset(Font.BOLD, -1)); // 比标准字体小1号（粗体）
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

        // 关键设置：失去焦点时自动停止编辑并保存，实现 Postman 风格的即时保存
        table.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
    }

    /**
     * 设置启用列的宽度
     *
     * @param width 宽度
     */
    protected void setEnabledColumnWidth(int width) {
        int colIndex = getEnabledColumnIndex();
        table.getColumnModel().getColumn(colIndex).setPreferredWidth(width);
        table.getColumnModel().getColumn(colIndex).setMaxWidth(width);
        table.getColumnModel().getColumn(colIndex).setMinWidth(width);
    }

    /**
     * 设置删除列的宽度
     *
     * @param width 宽度
     */
    protected void setDeleteColumnWidth(int width) {
        int colIndex = getDeleteColumnIndex();
        table.getColumnModel().getColumn(colIndex).setPreferredWidth(width);
        table.getColumnModel().getColumn(colIndex).setMaxWidth(width);
        table.getColumnModel().getColumn(colIndex).setMinWidth(width);
    }

    // ========== 通用工具方法 ==========

    /**
     * 安全地停止单元格编辑
     * 防止在修改表格数据时出现 ArrayIndexOutOfBoundsException
     */
    public void stopCellEditing() {
        if (table.isEditing()) {
            TableCellEditor editor = table.getCellEditor();
            if (editor != null) {
                editor.stopCellEditing();
            }
        }
    }

    /**
     * 带递归保护的停止编辑
     * 在调用可能触发表格事件的 getter 方法时使用
     */
    protected void stopCellEditingWithProtection() {
        if (!isStoppingCellEdit) {
            isStoppingCellEdit = true;
            try {
                stopCellEditing();
            } finally {
                isStoppingCellEdit = false;
            }
        }
    }

    /**
     * 清空表格数据
     */
    public void clear() {
        stopCellEditing();

        suppressAutoAppendRow = true;
        try {
            tableModel.setRowCount(0);
            tableModel.addRow(createEmptyRow());
        } finally {
            suppressAutoAppendRow = false;
        }
    }

    /**
     * 创建空行数据
     *
     * @return 空行数组
     */
    protected Object[] createEmptyRow() {
        Object[] row = new Object[columns.length];
        row[getEnabledColumnIndex()] = true; // 默认启用
        for (int i = 0; i < columns.length; i++) {
            if (i != getEnabledColumnIndex()) {
                row[i] = "";
            }
        }
        return row;
    }

    /**
     * 检查最后一行是否有内容
     *
     * @return true 如果最后一行有内容
     */
    protected boolean hasContentInLastRow() {
        int rowCount = tableModel.getRowCount();
        if (rowCount == 0) {
            return false;
        }
        return hasContentInRow(rowCount - 1);
    }

    /**
     * 确保表格最后一行是空行
     * 类似 Postman 的自动追加空行功能
     */
    protected void ensureEmptyLastRow() {
        suppressAutoAppendRow = true;
        try {
            if (tableModel.getRowCount() == 0 || hasContentInLastRow()) {
                tableModel.addRow(createEmptyRow());
            }
        } finally {
            suppressAutoAppendRow = false;
        }
    }

    /**
     * 添加表格模型监听器
     *
     * @param listener 监听器
     */
    public void addTableModelListener(TableModelListener listener) {
        if (tableModel != null) {
            tableModel.addTableModelListener(listener);
        }
    }

    /**
     * 设置表格是否可编辑
     *
     * @param editable 是否可编辑
     */
    public void setEditable(boolean editable) {
        this.editable = editable;
        table.repaint(); // 刷新显示
    }

    /**
     * 滚动到最后一行
     */
    protected void scrollToLastRow() {
        SwingUtilities.invokeLater(() -> {
            int rowCount = table.getRowCount();
            if (rowCount > 0) {
                Rectangle rect = table.getCellRect(rowCount - 1, 0, true);
                table.scrollRectToVisible(rect);
            }
        });
    }

    /**
     * 从表格模型的指定行获取字符串值
     *
     * @param row 行索引
     * @param col 列索引
     * @return 字符串值，null 转为空字符串
     */
    protected String getStringValue(int row, int col) {
        Object obj = tableModel.getValueAt(row, col);
        return obj == null ? "" : obj.toString().trim();
    }

    /**
     * 从表格模型的指定行获取布尔值
     *
     * @param row 行索引
     * @param col 列索引
     * @return 布尔值，非Boolean类型默认为true
     */
    protected boolean getBooleanValue(int row, int col) {
        Object obj = tableModel.getValueAt(row, col);
        return obj instanceof Boolean ? (Boolean) obj : true;
    }

    /**
     * 添加自动追加空行功能
     * 当最后一行有内容时，自动在末尾添加新的空行（类似Postman行为）
     */
    protected void addAutoAppendRowFeature() {
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
                    boolean lastRowHasContent = hasContentInRow(lastRow);

                    // Add empty row if last row has content
                    if (lastRowHasContent) {
                        suppressAutoAppendRow = true;
                        try {
                            tableModel.addRow(createEmptyRow());
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
     * 通用删除按钮渲染器
     * 显示删除图标，对于非空行或非最后一行显示删除按钮
     */
    protected class DeleteButtonRenderer extends JLabel implements TableCellRenderer {
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

                // Use abstract method to check if row has content
                boolean isEmpty = !hasContentInRow(modelRow);

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

    // ========== Tab 键导航功能 ==========

    /**
     * 设置Tab键导航，支持在可编辑单元格之间跳转
     */
    protected void setupTabKeyNavigation() {
        InputMap inputMap = table.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        ActionMap actionMap = table.getActionMap();

        // Tab key - move to next editable cell
        inputMap.put(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_TAB, 0), "nextCell");
        actionMap.put("nextCell", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                moveToNextEditableCell(false);
            }
        });

        // Shift+Tab - move to previous editable cell
        inputMap.put(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_TAB, java.awt.event.InputEvent.SHIFT_DOWN_MASK), "previousCell");
        actionMap.put("previousCell", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                moveToNextEditableCell(true);
            }
        });
    }

    /**
     * 移动到下一个（或上一个）可编辑单元格
     *
     * @param reverse true表示向后移动（Shift+Tab），false表示向前移动（Tab）
     */
    protected void moveToNextEditableCell(boolean reverse) {
        int currentRow = table.getSelectedRow();
        int currentColumn = table.getSelectedColumn();

        if (currentRow < 0 || currentColumn < 0) {
            // 没有选中的单元格，选择第一个可编辑单元格
            table.changeSelection(0, getFirstEditableColumnIndex(), false, false);
            table.editCellAt(0, getFirstEditableColumnIndex());
            return;
        }

        // 停止当前单元格的编辑
        if (table.isEditing()) {
            table.getCellEditor().stopCellEditing();
        }

        // 查找下一个可编辑列
        int nextColumn = currentColumn;
        int columnCount = table.getColumnCount();

        if (reverse) {
            // 向后移动
            int attempts = 0;
            int maxAttempts = columnCount + table.getRowCount() * columnCount;
            do {
                nextColumn--;
                attempts++;
                if (nextColumn < 0) {
                    // 移动到上一行的最后一个可编辑列
                    if (currentRow > 0) {
                        currentRow--;
                        nextColumn = getLastEditableColumnIndex();
                    } else {
                        nextColumn = getFirstEditableColumnIndex();
                        break;
                    }
                }
                if (attempts >= maxAttempts) {
                    // 安全退出，防止无限循环
                    nextColumn = getFirstEditableColumnIndex();
                    break;
                }
            } while (!isCellEditableForNavigation(currentRow, nextColumn));
        } else {
            // 向前移动
            int attempts = 0;
            int maxAttempts = columnCount + table.getRowCount() * columnCount;
            do {
                nextColumn++;
                attempts++;
                if (nextColumn >= columnCount) {
                    // 移动到下一行的第一个可编辑列
                    if (currentRow < table.getRowCount() - 1) {
                        currentRow++;
                        nextColumn = getFirstEditableColumnIndex();
                    } else {
                        nextColumn = getLastEditableColumnIndex();
                        break;
                    }
                }
                if (attempts >= maxAttempts) {
                    // 安全退出，防止无限循环
                    nextColumn = getFirstEditableColumnIndex();
                    break;
                }
            } while (!isCellEditableForNavigation(currentRow, nextColumn));
        }

        // 选择并开始编辑下一个单元格
        if (nextColumn >= 0 && nextColumn < columnCount && currentRow >= 0 && currentRow < table.getRowCount()) {
            table.changeSelection(currentRow, nextColumn, false, false);
            if (isCellEditableForNavigation(currentRow, nextColumn)) {
                table.editCellAt(currentRow, nextColumn);
                Component editor = table.getEditorComponent();
                if (editor instanceof JTextField textField) {
                    editor.requestFocusInWindow();
                    textField.selectAll();
                }
            }
        }
    }
}
