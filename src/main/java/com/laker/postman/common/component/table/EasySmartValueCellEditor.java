package com.laker.postman.common.component.table;

import com.laker.postman.common.component.EasyTextField;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.TableCellEditor;
import java.awt.*;

@Slf4j
public class EasySmartValueCellEditor extends AbstractCellEditor implements TableCellEditor {

    protected static final String CARD_SINGLE = "single";
    protected static final String CARD_MULTI = "multi";

    protected final JPanel containerPanel;
    protected final CardLayout cardLayout;

    protected EasyTextField textField;
    private JTextArea textArea;
    private JScrollPane scrollPane;

    private boolean isMultiLine;

    protected boolean isMultiLineMode() {
        return isMultiLine;
    }

    private JTable currentTable;
    private int currentRow;
    private int currentColumn;
    private int originalRowHeight;
    private boolean rowHeightExpanded = false;

    private static final int MAX_EDITOR_LINES = 5;
    private static final int MIN_EDITOR_LINES = 2;

    private boolean switching = false;
    private boolean ignoreFocusLost = false;

    private transient DocumentListener textFieldListener;
    private final boolean multiLineEnabled;

    public EasySmartValueCellEditor() {
        this(true);
    }

    public EasySmartValueCellEditor(boolean enableAutoMultiLine) {
        this.multiLineEnabled = enableAutoMultiLine;
        this.textField = new EasyTextField(1);
        this.textField.setBorder(null);

        this.cardLayout = new CardLayout();
        this.containerPanel = new JPanel(cardLayout);
        this.containerPanel.setBorder(null);

        if (enableAutoMultiLine) {
            this.textArea = new JTextArea();
            this.textArea.setLineWrap(true);
            this.textArea.setFont(textField.getFont());

            this.scrollPane = new JScrollPane(textArea);
            this.scrollPane.setBorder(null);
            this.scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
            this.scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

            this.containerPanel.add(textField, CARD_SINGLE);
            this.containerPanel.add(scrollPane, CARD_MULTI);
            cardLayout.show(containerPanel, CARD_SINGLE);

            bindTextFieldListener(textField);

            this.textArea.getDocument().addDocumentListener(new DocumentListener() {
                @Override
                public void insertUpdate(DocumentEvent e) {
                    onTextAreaChanged();
                }

                @Override
                public void removeUpdate(DocumentEvent e) {
                    onTextAreaChanged();
                }

                @Override
                public void changedUpdate(DocumentEvent e) {
                }
            });
        } else {
            this.containerPanel.add(textField, CARD_SINGLE);
        }
    }

    private void bindTextFieldListener(EasyTextField field) {
        textFieldListener = new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                onTextFieldChanged();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                onTextFieldChanged();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
            }
        };
        field.getDocument().addDocumentListener(textFieldListener);
    }

    protected void replaceTextField(EasyTextField newField) {
        if (!multiLineEnabled) {
            containerPanel.remove(textField);
            this.textField = newField;
            containerPanel.add(newField, CARD_SINGLE);
            cardLayout.show(containerPanel, CARD_SINGLE);
            return;
        }
        if (textFieldListener != null) {
            textField.getDocument().removeDocumentListener(textFieldListener);
            textFieldListener = null;
        }
        containerPanel.removeAll();
        this.textField = newField;
        containerPanel.add(newField, CARD_SINGLE);
        containerPanel.add(scrollPane, CARD_MULTI);
        cardLayout.show(containerPanel, CARD_SINGLE);
        bindTextFieldListener(newField);
    }

    // ─── DocumentListener 回调 ──────────────────────────────────────────────

    private void onTextFieldChanged() {
        if (switching || textArea == null || currentTable == null) return;
        SwingUtilities.invokeLater(() -> {
            if (switching || isMultiLine) return;
            if (needsMultiLineEdit(textField.getText())) switchToMultiLine(textField.getText());
        });
    }

    private void onTextAreaChanged() {
        if (switching || textArea == null || currentTable == null) return;
        SwingUtilities.invokeLater(() -> {
            if (switching || !isMultiLine) return;
            String text = textArea.getText();
            if (!needsMultiLineEdit(text)) switchToSingleLine(text);
            else updateRowHeight(countLines(text));
        });
    }

    // ─── 编辑器切换 ─────────────────────────────────────────────────────────

    private void switchToMultiLine(String text) {
        if (isMultiLine || currentTable == null) return;
        log.debug("[switchToMultiLine] text.length={}", text.length());
        switching = true;
        ignoreFocusLost = true;
        try {
            int caretPos = Math.min(textField.getCaretPosition(), text.length());
            isMultiLine = true;
            textArea.setText(text);
            textArea.setCaretPosition(caretPos);

            containerPanel.setFocusable(true);
            containerPanel.requestFocusInWindow();
            cardLayout.show(containerPanel, CARD_MULTI);

            if (!rowHeightExpanded) {
                originalRowHeight = currentTable.getRowHeight(currentRow);
                rowHeightExpanded = true;
            }
            int lines = Math.min(MAX_EDITOR_LINES, Math.max(MIN_EDITOR_LINES, countLines(text)));
            applyRowHeight(currentTable, currentRow,
                    textArea.getFontMetrics(textArea.getFont()).getHeight() * lines + 14);

            containerPanel.revalidate();
            containerPanel.repaint();
            SwingUtilities.invokeLater(() -> {
                textArea.requestFocusInWindow();
                SwingUtilities.invokeLater(() -> {
                    ignoreFocusLost = false;
                    containerPanel.setFocusable(false);
                });
            });
        } finally {
            switching = false;
        }
    }

    private void switchToSingleLine(String text) {
        if (!isMultiLine || currentTable == null) return;
        log.debug("[switchToSingleLine] text.length={}", text.length());
        switching = true;
        ignoreFocusLost = true;
        try {
            int caretPos = Math.min(textArea.getCaretPosition(), text.length());
            isMultiLine = false;
            textField.setText(text);
            textField.setCaretPosition(caretPos);

            containerPanel.setFocusable(true);
            containerPanel.requestFocusInWindow();
            cardLayout.show(containerPanel, CARD_SINGLE);

            if (rowHeightExpanded && originalRowHeight > 0) {
                applyRowHeight(currentTable, currentRow, originalRowHeight);
                rowHeightExpanded = false;
            }

            containerPanel.revalidate();
            containerPanel.repaint();
            SwingUtilities.invokeLater(() -> {
                textField.requestFocusInWindow();
                SwingUtilities.invokeLater(() -> {
                    ignoreFocusLost = false;
                    containerPanel.setFocusable(false);
                });
            });
        } finally {
            switching = false;
        }
    }

    // ─── 行高管理 ────────────────────────────────────────────────────────────

    private void updateRowHeight(int lines) {
        if (currentTable == null || !rowHeightExpanded) return;
        applyRowHeight(currentTable, currentRow,
                textArea.getFontMetrics(textArea.getFont()).getHeight() * lines + 14);
    }

    private static void applyRowHeight(JTable table, int row, int height) {
        if (table == null) return;
        log.debug("[applyRowHeight] row={} height={} before={}", row, height, table.getRowHeight(row));
        table.setRowHeight(row, height);
        SwingUtilities.invokeLater(() -> {
            Container p = table.getParent();
            while (p != null) {
                if (p instanceof JScrollPane) {
                    log.debug("[applyRowHeight] validate JScrollPane size={}", p.getSize());
                    p.validate();
                    break;
                }
                p = p.getParent();
            }
            Rectangle cellRect = table.getCellRect(row, 0, true);
            log.debug("[applyRowHeight] scrollRectToVisible={}", cellRect);
            table.scrollRectToVisible(cellRect);
        });
    }

    // ─── CellEditor 主流程 ──────────────────────────────────────────────────

    @Override
    public Object getCellEditorValue() {
        return (isMultiLine && textArea != null) ? textArea.getText() : textField.getText();
    }

    @Override
    public Component getTableCellEditorComponent(JTable table, Object value,
                                                 boolean isSelected, int row, int column) {
        log.debug("[getComponent] START row={} col={} rowHeightExpanded={} currentRow={} originalRowHeight={}",
                row, column, rowHeightExpanded, currentRow, originalRowHeight);

        // 先同步恢复上一次撑开的行高，必须在修改 currentRow/currentTable 之前
        if (rowHeightExpanded && currentTable != null && originalRowHeight > 0) {
            log.debug("[getComponent] restoring previous row={} to height={}", currentRow, originalRowHeight);
            currentTable.setRowHeight(currentRow, originalRowHeight);
            rowHeightExpanded = false;
        }

        this.currentTable = table;
        this.currentRow = row;
        this.currentColumn = column;
        this.originalRowHeight = table.getRowHeight(row);
        log.debug("[getComponent] originalRowHeight saved={}", this.originalRowHeight);

        String text = value == null ? "" : value.toString();

        if (textArea != null && needsMultiLineEdit(text)) {
            log.debug("[getComponent] MULTI-LINE mode, text.length={}", text.length());
            isMultiLine = true;
            switching = true;
            ignoreFocusLost = true;
            try {
                textArea.setText(text);
                textArea.setCaretPosition(text.length());
            } finally {
                switching = false;
            }
            cardLayout.show(containerPanel, CARD_MULTI);
            rowHeightExpanded = true;
            int lines = Math.min(MAX_EDITOR_LINES, Math.max(MIN_EDITOR_LINES, countLines(text)));
            int newHeight = textArea.getFontMetrics(textArea.getFont()).getHeight() * lines + 14;
            log.debug("[getComponent] scheduled setRowHeight row={} newHeight={} lines={}", row, newHeight, lines);
            final int finalRow = row;
            SwingUtilities.invokeLater(() -> {
                log.debug("[getComponent] invokeLater setRowHeight row={} newHeight={}", finalRow, newHeight);
                table.setRowHeight(finalRow, newHeight);
                Container p = table.getParent();
                while (p != null) {
                    if (p instanceof JScrollPane) {
                        log.debug("[getComponent] validate JScrollPane size={}", p.getSize());
                        p.validate();
                        log.debug("[getComponent] after validate JScrollPane size={}", p.getSize());
                        break;
                    }
                    p = p.getParent();
                }
                Rectangle cellRect = table.getCellRect(finalRow, 0, true);
                log.debug("[getComponent] scrollRectToVisible cellRect={}", cellRect);
                table.scrollRectToVisible(cellRect);
                SwingUtilities.invokeLater(() -> {
                    ignoreFocusLost = false;
                    log.debug("[getComponent] ignoreFocusLost reset to false");
                });
            });
        } else {
            log.debug("[getComponent] SINGLE-LINE mode");
            isMultiLine = false;
            switching = true;
            try {
                textField.setText(text);
            } finally {
                switching = false;
            }
            cardLayout.show(containerPanel, CARD_SINGLE);
        }

        log.debug("[getComponent] END");
        return containerPanel;
    }

    @Override
    public boolean stopCellEditing() {
        log.debug("[stopCellEditing] ignoreFocusLost={} rowHeightExpanded={}", ignoreFocusLost, rowHeightExpanded);
        if (ignoreFocusLost) return false;
        restoreRowHeight();
        return super.stopCellEditing();
    }

    @Override
    public void cancelCellEditing() {
        log.debug("[cancelCellEditing] ignoreFocusLost={} rowHeightExpanded={}", ignoreFocusLost, rowHeightExpanded);
        if (ignoreFocusLost) return;
        restoreRowHeight();
        super.cancelCellEditing();
    }

    private void restoreRowHeight() {
        log.debug("[restoreRowHeight] rowHeightExpanded={} currentRow={} originalRowHeight={}",
                rowHeightExpanded, currentRow, originalRowHeight);
        if (rowHeightExpanded && currentTable != null && originalRowHeight > 0) {
            log.debug("[restoreRowHeight] restoring row={} to height={}", currentRow, originalRowHeight);
            currentTable.setRowHeight(currentRow, originalRowHeight);
            rowHeightExpanded = false;
            final JTable t = currentTable;
            SwingUtilities.invokeLater(() -> {
                Container p = t.getParent();
                while (p != null) {
                    if (p instanceof JScrollPane) {
                        p.validate();
                        p.repaint();
                        log.debug("[restoreRowHeight] validate done");
                        break;
                    }
                    p = p.getParent();
                }
            });
        }
    }

    // ─── 判断逻辑 ────────────────────────────────────────────────────────────

    private boolean needsMultiLineEdit(String text) {
        if (text == null || text.isEmpty()) return false;
        if (text.contains("\n")) return true;
        if (currentTable == null) return false;
        Font font = textField.getFont();
        if (font == null) return false;
        FontMetrics fm = textField.getFontMetrics(font);
        if (fm == null) return false;
        int w = containerPanel.getWidth();
        if (w <= 0) w = currentTable.getColumnModel().getColumn(currentColumn).getWidth();
        w -= 20;
        return w > 0 && fm.stringWidth(text) > w;
    }

    private int countLines(String text) {
        if (text == null || text.isEmpty()) return 1;
        if (currentTable == null) return 1;
        int w = containerPanel.getWidth();
        if (w <= 0) w = currentTable.getColumnModel().getColumn(currentColumn).getWidth();
        w -= 20;
        if (w <= 0) return text.split("\n", -1).length;
        FontMetrics fm = textField.getFontMetrics(textField.getFont());
        int total = 0;
        for (String seg : text.split("\n", -1)) {
            total += seg.isEmpty() ? 1 : Math.max(1, (int) Math.ceil((double) fm.stringWidth(seg) / w));
            if (total >= MAX_EDITOR_LINES) return MAX_EDITOR_LINES;
        }
        return Math.min(MAX_EDITOR_LINES, total);
    }
}
