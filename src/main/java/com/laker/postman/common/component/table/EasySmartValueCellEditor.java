package com.laker.postman.common.component.table;

import com.laker.postman.common.component.EasyTextField;

import javax.swing.*;
import javax.swing.table.TableCellEditor;
import java.awt.*;

/**
 * 智能值列单元格编辑器
 * 根据内容长度自动选择：
 * - 短文本：单行 TextField
 * - 长文本（超出列宽）：多行 TextArea（自动撑开行高）
 */
public class EasySmartValueCellEditor extends AbstractCellEditor implements TableCellEditor {
    private final EasyTextField textField;
    private JTextArea textArea;
    private JScrollPane scrollPane;
    private boolean isMultiLine;
    private JTable currentTable;
    private int currentRow;
    private int originalRowHeight;

    public EasySmartValueCellEditor() {
        this(true);
    }

    /**
     * @param enableAutoMultiLine 是否启用自动多行编辑（根据内容长度）
     */
    public EasySmartValueCellEditor(boolean enableAutoMultiLine) {
        this.textField = new EasyTextField(1); // 单行文本框
        this.textField.setBorder(null); // 去掉边框，和表格样式一致

        // 初始化多行编辑器
        if (enableAutoMultiLine) {
            this.textArea = new JTextArea();
            this.textArea.setLineWrap(true); // 自动换行
            this.textArea.setWrapStyleWord(true); // 按单词换行
            this.textArea.setFont(textField.getFont()); // 统一字体
            this.scrollPane = new JScrollPane(textArea); // 包裹在滚动面板中
            this.scrollPane.setBorder(null); // 去掉边框
            this.scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED); // 根据需要显示垂直滚动条
            this.scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER); // 不显示水平滚动条
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
            // 使用多行编辑器
            isMultiLine = true;
            textArea.setText(text);
            textArea.setCaretPosition(0);

            // 设置 TextArea 的行数（最多5行）
            int lines = Math.min(5, Math.max(2, countLines(text, table, column)));
            textArea.setRows(lines);

            // 🔑 关键：撑开行高以适应多行内容
            expandRowHeight(table, row, lines);

            return scrollPane;
        } else {
            // 使用单行编辑器
            isMultiLine = false;
            textField.setText(text);

            // 恢复默认行高
            restoreRowHeight(table, row);

            return textField;
        }
    }

    /**
     * 撑开行高以适应多行编辑器
     */
    private void expandRowHeight(JTable table, int row, int lines) {
        // 保存原始行高
        this.originalRowHeight = table.getRowHeight(row);

        // 计算新的行高：基础高度 + 行数 * 行高
        FontMetrics fm = textArea.getFontMetrics(textArea.getFont());
        int lineHeight = fm.getHeight();
        // 根据行数动态设置最小高度：2行时至少40px，3行及以上至少60px
        int minHeight = lines <= 2 ? 40 : 60;
        int padding = 10; // 上下边距
        int newHeight = Math.max(minHeight, lineHeight * lines + padding);

        // 设置新行高
        table.setRowHeight(row, newHeight);
    }

    /**
     * 恢复默认行高
     */
    private void restoreRowHeight(JTable table, int row) {
        if (originalRowHeight > 0) {
            table.setRowHeight(row, originalRowHeight);
        } else {
            // 恢复为默认行高
            table.setRowHeight(row, table.getRowHeight());
        }
    }

    @Override
    public boolean stopCellEditing() {
        // 停止编辑时恢复行高
        if (currentTable != null && currentRow >= 0 && isMultiLine) {
            restoreRowHeight(currentTable, currentRow);
        }
        return super.stopCellEditing();
    }

    @Override
    public void cancelCellEditing() {
        // 取消编辑时恢复行高
        if (currentTable != null && currentRow >= 0 && isMultiLine) {
            restoreRowHeight(currentTable, currentRow);
        }
        super.cancelCellEditing();
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
     * 计算文本需要的行数
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
        }

        // 返回实际需要的总行数（考虑了真实换行符和宽度限制）
        return Math.max(actualLineCount, totalLinesNeeded);
    }
}
