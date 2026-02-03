package com.laker.postman.common.component.table;

import com.laker.postman.common.component.EasyTextField;

import javax.swing.*;
import javax.swing.table.TableCellEditor;
import java.awt.*;

/**
 * 智能值列单元格编辑器
 * <p>
 * 根据内容长度自动选择编辑器：
 * - 短文本：单行 TextField
 * - 长文本（超出列宽）或包含换行符：多行 TextArea（自动撑开行高）
 * <p>
 * 特性：
 * - 自动检测文本长度，超出列宽时使用多行编辑器
 * - 支持换行符的文本编辑
 * - 编辑时自动撑开行高，结束时恢复
 * - 保护期机制防止行高撑开时意外关闭编辑器
 */
public class EasySmartValueCellEditor extends AbstractCellEditor implements TableCellEditor {
    /**
     * 单行文本编辑器
     */
    protected EasyTextField textField;

    /**
     * 多行文本编辑器
     */
    private JTextArea textArea;

    /**
     * 多行编辑器的滚动面板
     */
    private JScrollPane scrollPane;

    /**
     * 当前是否使用多行编辑器
     */
    private boolean isMultiLine;

    /**
     * 当前正在编辑的表格
     */
    private JTable currentTable;

    /**
     * 当前正在编辑的行
     */
    private int currentRow;

    /**
     * 原始行高（用于恢复）
     */
    private int originalRowHeight;

    /**
     * 行高是否已撑开
     */
    private boolean rowHeightExpanded = false;

    /**
     * 最后一次撑开行高的时间戳
     */
    private long lastExpandTime = 0;

    /**
     * 撑开后的保护期时长（毫秒），防止意外关闭编辑器
     */
    private static final long EXPAND_PROTECTION_MS = 200;

    /**
     * 多行编辑器的最大行数
     */
    private static final int MAX_EDITOR_LINES = 5;

    /**
     * 多行编辑器的最小行数
     */
    private static final int MIN_EDITOR_LINES = 2;

    /**
     * 默认行高
     */
    private static final int DEFAULT_ROW_HEIGHT = 28;

    public EasySmartValueCellEditor() {
        this(true);
    }

    /**
     * 构造函数
     *
     * @param enableAutoMultiLine 是否启用自动多行编辑（根据内容长度判断）
     */
    public EasySmartValueCellEditor(boolean enableAutoMultiLine) {
        // 初始化单行编辑器
        this.textField = new EasyTextField(1);
        this.textField.setBorder(null);

        // 初始化多行编辑器
        if (enableAutoMultiLine) {
            this.textArea = new JTextArea();
            this.textArea.setLineWrap(true); // 自动换行
            this.textArea.setFont(textField.getFont());

            this.scrollPane = new JScrollPane(textArea);
            this.scrollPane.setBorder(null);
            this.scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
            this.scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        }
    }

    @Override
    public Object getCellEditorValue() {
        if (isMultiLine && textArea != null) {
            return textArea.getText();
        }
        return textField.getText();
    }

    @Override
    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
        this.currentTable = table;
        this.currentRow = row;
        String text = value == null ? "" : value.toString();

        // 判断是否需要多行编辑
        if (textArea != null && needsMultiLineEdit(text, table, column)) {
            return setupMultiLineEditor(text, table, row, column);
        } else {
            // 使用单行编辑器
            isMultiLine = false;
            textField.setText(text);
            return textField;
        }
    }

    /**
     * 设置多行编辑器并处理行高
     */
    private Component setupMultiLineEditor(String text, JTable table, int row, int column) {
        // 使用多行编辑器
        isMultiLine = true;
        textArea.setText(text);
        textArea.setCaretPosition(0);

        // 设置 TextArea 的行数（最少2行，最多5行）
        int lines = Math.min(MAX_EDITOR_LINES, Math.max(MIN_EDITOR_LINES, countLines(text, table, column)));
        textArea.setRows(lines);

        // 检查当前行高是否已经撑开
        int currentRowHeight = table.getRowHeight(row);
        boolean alreadyExpanded = currentRowHeight > DEFAULT_ROW_HEIGHT;

        // 只在未撑开时才撑开行高
        if (!alreadyExpanded) {
            expandRowHeight(table, row, lines);
        } else {
            // 行高已经撑开，不需要再次撑开，但需要标记为已撑开状态
            this.originalRowHeight = DEFAULT_ROW_HEIGHT;
            rowHeightExpanded = true;
        }

        return scrollPane;
    }

    /**
     * 撑开行高以适应多行编辑器
     */
    private void expandRowHeight(JTable table, int row, int lines) {
        // 保存原始行高并计算新行高
        this.originalRowHeight = table.getRowHeight(row);
        FontMetrics fm = textArea.getFontMetrics(textArea.getFont());
        int lineHeight = fm.getHeight();

        // 根据实际行数计算高度：行高 * 行数 + 上下内边距
        // 内边距：上下各 4px，滚动条区域 6px，共约 14px
        int padding = 14;
        int newHeight = lineHeight * lines + padding;

        // 确保最小高度足够显示内容（避免文字被截断）
        int minHeight = lineHeight * lines + 10;
        newHeight = Math.max(minHeight, newHeight);

        // 立即撑开行高，但延迟 revalidate
        table.setRowHeight(row, newHeight);
        rowHeightExpanded = true;
        lastExpandTime = System.currentTimeMillis();

        // 延迟更新布局，等待编辑器组件完全显示后再更新
        // 双重 invokeLater 确保编辑器完全初始化
        SwingUtilities.invokeLater(() -> SwingUtilities.invokeLater(() -> {
            if (currentTable != null && rowHeightExpanded) {
                currentTable.revalidate();
                currentTable.repaint();
            }
        }));
    }


    @Override
    public boolean stopCellEditing() {
        // 保护期：如果刚撑开行高（<200ms），拒绝停止编辑
        if (rowHeightExpanded && lastExpandTime > 0) {
            long elapsed = System.currentTimeMillis() - lastExpandTime;
            if (elapsed < EXPAND_PROTECTION_MS) {
                return false; // 拒绝停止编辑
            }
        }

        // 恢复行高
        if (currentTable != null && currentRow >= 0 && rowHeightExpanded) {
            restoreRowHeight(currentTable, currentRow);
            rowHeightExpanded = false;
            lastExpandTime = 0;
        }

        return super.stopCellEditing();
    }

    @Override
    public void cancelCellEditing() {
        // 保护期：如果刚撑开行高，不恢复行高，尝试重启编辑
        if (rowHeightExpanded && lastExpandTime > 0) {
            long elapsed = System.currentTimeMillis() - lastExpandTime;
            if (elapsed < EXPAND_PROTECTION_MS) {
                // 保存信息用于重启
                final JTable table = currentTable;
                final int row = currentRow;
                final int col = 2; // Value 列固定为 2

                super.cancelCellEditing(); // 完成取消

                // 延迟重启编辑
                SwingUtilities.invokeLater(() -> {
                    if (table != null) {
                        table.editCellAt(row, col);
                        Component editor = table.getEditorComponent();
                        if (editor != null) {
                            editor.requestFocusInWindow();
                        }
                    }
                });
                return; // 不恢复行高
            }
        }

        // 正常情况：恢复行高
        if (currentTable != null && currentRow >= 0 && rowHeightExpanded) {
            restoreRowHeight(currentTable, currentRow);
            rowHeightExpanded = false;
            lastExpandTime = 0;
        }

        super.cancelCellEditing();
    }

    /**
     * 恢复行高到原始值
     */
    private void restoreRowHeight(JTable table, int row) {
        if (originalRowHeight > 0) {
            table.setRowHeight(row, originalRowHeight);
        }

        // 异步更新布局
        SwingUtilities.invokeLater(() -> {
            table.revalidate();
            table.repaint();
        });

        originalRowHeight = 0;
    }

    /**
     * 判断是否需要多行编辑
     * 如果文本会被截断（渲染器会显示 ...）或包含换行符，则使用多行编辑
     */
    private boolean needsMultiLineEdit(String text, JTable table, int column) {
        if (text == null || text.isEmpty()) {
            return false;
        }

        // 如果文本包含换行符，必须使用多行编辑
        if (text.contains("\n")) {
            return true;
        }

        // 计算可显示的字符数（使用与渲染器相同的逻辑）
        int columnWidth = table.getColumnModel().getColumn(column).getWidth();
        Font font = textField.getFont();
        if (font == null) {
            return false;
        }

        FontMetrics fm = textField.getFontMetrics(font);
        if (fm == null) {
            return false;
        }

        int ellipsisWidth = fm.stringWidth("...");
        int availableWidth = columnWidth - 10 - ellipsisWidth;

        if (availableWidth <= 0) {
            return false;
        }

        int textWidth = fm.stringWidth(text);

        // 如果文本宽度超过可用宽度，需要多行编辑
        return textWidth > availableWidth;
    }

    /**
     * 计算文本需要的行数（已考虑最大行数限制）
     */
    private int countLines(String text, JTable table, int column) {
        if (text == null || text.isEmpty()) {
            return 1;
        }

        int columnWidth = table.getColumnModel().getColumn(column).getWidth() - 20; // 减去滚动条宽度
        Font font = textField.getFont();
        FontMetrics fm = textField.getFontMetrics(font);

        // 计算实际换行符产生的行数（换行符分割后的数组长度就是行数）
        String[] lines = text.split("\n", -1);
        int actualLineCount = lines.length;

        // 计算每一行因为宽度限制需要的额外行数
        int totalLinesNeeded = 0;
        for (String line : lines) {
            if (line.isEmpty()) {
                totalLinesNeeded += 1; // 空行也占一行
            } else {
                int lineWidth = fm.stringWidth(line);
                int linesForThisLine = Math.max(1, (int) Math.ceil((double) lineWidth / columnWidth));
                totalLinesNeeded += linesForThisLine;
            }

            // 提前退出：如果已经超过最大行数，直接返回最大值
            if (totalLinesNeeded >= MAX_EDITOR_LINES) {
                return MAX_EDITOR_LINES;
            }
        }

        // 返回实际需要的总行数（考虑了真实换行符和宽度限制），但不超过最大行数
        return Math.min(MAX_EDITOR_LINES, Math.max(actualLineCount, totalLinesNeeded));
    }
}
