package com.laker.postman.common.component.table;

import com.laker.postman.common.component.EasyTextField;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.TableCellEditor;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.InputMethodEvent;
import java.awt.event.InputMethodListener;
import java.text.AttributedCharacterIterator;

/**
 * Value editor that uses a text field for short content and a wrapping text area for long content.
 *
 * <p>Mode changes are suspended while an input method is composing text and resumed on the next EDT
 * cycle after composition is committed. This preserves the original compact/expanded editing
 * experience without changing the focus owner in the middle of an input-method event.</p>
 */
@Slf4j
public class EasySmartValueCellEditor extends AbstractCellEditor implements TableCellEditor {

    protected static final String CARD_SINGLE = "single";
    protected static final String CARD_MULTI = "multi";

    private static final int MAX_EDITOR_LINES = 5;
    private static final int MIN_EDITOR_LINES = 2;

    protected final JPanel containerPanel;
    protected final CardLayout cardLayout;
    protected EasyTextField textField;

    private final JTextArea textArea;
    private final JScrollPane scrollPane;
    private final boolean multiLineEnabled;

    private JTable currentTable;
    private int currentRow;
    private int currentColumn;
    private int originalRowHeight;
    private long editingSession;
    private long queuedReevaluationSession = -1;

    private boolean isMultiLine;
    private boolean rowHeightExpanded;
    private boolean switching;
    private boolean ignoreFocusLost;
    private boolean editingActive;
    private boolean inputMethodComposing;
    private boolean modeReevaluationRequested;

    private final DocumentListener textFieldListener = new DocumentListener() {
        @Override
        public void insertUpdate(DocumentEvent event) {
            scheduleModeReevaluation();
        }

        @Override
        public void removeUpdate(DocumentEvent event) {
            scheduleModeReevaluation();
        }

        @Override
        public void changedUpdate(DocumentEvent event) {
            // Plain text documents do not need attribute-change handling.
        }
    };

    private final InputMethodListener inputMethodListener = new InputMethodListener() {
        @Override
        public void inputMethodTextChanged(InputMethodEvent event) {
            updateInputMethodComposition(event);
        }

        @Override
        public void caretPositionChanged(InputMethodEvent event) {
            // Caret-only input-method events do not change composition state.
        }
    };

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

        this.textField.getDocument().addDocumentListener(textFieldListener);
        this.textField.addInputMethodListener(inputMethodListener);

        if (enableAutoMultiLine) {
            this.textArea = new JTextArea();
            this.textArea.setLineWrap(true);
            this.textArea.setFont(textField.getFont());
            this.textArea.addInputMethodListener(inputMethodListener);
            this.textArea.getDocument().addDocumentListener(new DocumentListener() {
                @Override
                public void insertUpdate(DocumentEvent event) {
                    onTextAreaChanged();
                }

                @Override
                public void removeUpdate(DocumentEvent event) {
                    onTextAreaChanged();
                }

                @Override
                public void changedUpdate(DocumentEvent event) {
                    // Plain text documents do not need attribute-change handling.
                }
            });

            this.scrollPane = new JScrollPane(textArea);
            this.scrollPane.setBorder(null);
            this.scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
            this.scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

            this.containerPanel.add(textField, CARD_SINGLE);
            this.containerPanel.add(scrollPane, CARD_MULTI);
        } else {
            this.textArea = null;
            this.scrollPane = null;
            this.containerPanel.add(textField, CARD_SINGLE);
        }
        cardLayout.show(containerPanel, CARD_SINGLE);
    }

    protected boolean isMultiLineMode() {
        return isMultiLine;
    }

    protected void replaceTextField(EasyTextField newField) {
        textField.getDocument().removeDocumentListener(textFieldListener);
        textField.removeInputMethodListener(inputMethodListener);
        containerPanel.remove(textField);

        this.textField = newField;
        this.textField.getDocument().addDocumentListener(textFieldListener);
        this.textField.addInputMethodListener(inputMethodListener);
        containerPanel.add(newField, CARD_SINGLE);
        cardLayout.show(containerPanel, CARD_SINGLE);
    }

    private void onTextAreaChanged() {
        if (switching || !editingActive || !isMultiLine || currentTable == null) return;
        SwingUtilities.invokeLater(() -> {
            if (switching || !editingActive || !isMultiLine) return;
            String text = textArea.getText();
            if (needsMultiLineEdit(text)) {
                updateRowHeight(countLines(text));
            }
            scheduleModeReevaluation();
        });
    }

    private void scheduleModeReevaluation() {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(this::scheduleModeReevaluation);
            return;
        }
        if (switching || !editingActive || !multiLineEnabled) return;
        modeReevaluationRequested = true;
        queueModeReevaluationIfReady();
    }

    private void queueModeReevaluationIfReady() {
        if (!modeReevaluationRequested
                || switching
                || !editingActive
                || inputMethodComposing
                || !multiLineEnabled) {
            return;
        }

        long session = editingSession;
        if (queuedReevaluationSession == session) return;
        queuedReevaluationSession = session;
        SwingUtilities.invokeLater(() -> runQueuedModeReevaluation(session));
    }

    private void runQueuedModeReevaluation(long session) {
        if (queuedReevaluationSession == session) {
            queuedReevaluationSession = -1;
        }
        if (session != editingSession || !editingActive) return;
        if (switching || inputMethodComposing) {
            return;
        }

        modeReevaluationRequested = false;
        reevaluateEditorMode();
        queueModeReevaluationIfReady();
    }

    private void reevaluateEditorMode() {
        if (switching || !editingActive || inputMethodComposing || currentTable == null) return;
        if (isMultiLine) {
            String text = textArea.getText();
            if (!needsMultiLineEdit(text)) {
                switchToSingleLine(text);
            }
        } else {
            String text = textField.getText();
            if (needsMultiLineEdit(text)) {
                switchToMultiLine(text);
            }
        }
    }

    void updateInputMethodComposition(InputMethodEvent event) {
        if (event.getID() != InputMethodEvent.INPUT_METHOD_TEXT_CHANGED) return;
        AttributedCharacterIterator text = event.getText();
        int characterCount = text == null ? 0 : text.getEndIndex() - text.getBeginIndex();
        inputMethodComposing = characterCount > event.getCommittedCharacterCount();
        if (!inputMethodComposing) {
            queueModeReevaluationIfReady();
        }
    }

    private void switchToMultiLine(String text) {
        if (switching || isMultiLine || inputMethodComposing || currentTable == null) return;
        log.debug("[switchToMultiLine] text.length={}", text.length());
        boolean transferFocus = textField.isFocusOwner();
        switching = true;
        ignoreFocusLost = transferFocus;
        try {
            int caretPosition = Math.min(textField.getCaretPosition(), text.length());
            isMultiLine = true;
            textArea.setText(text);
            textArea.setCaretPosition(caretPosition);
            cardLayout.show(containerPanel, CARD_MULTI);

            rowHeightExpanded = true;
            updateRowHeight(countLines(text));
            containerPanel.revalidate();
            containerPanel.repaint();
            completeFocusTransfer(textArea, transferFocus, true);
        } finally {
            switching = false;
        }
    }

    private void switchToSingleLine(String text) {
        if (switching || !isMultiLine || inputMethodComposing || currentTable == null) return;
        log.debug("[switchToSingleLine] text.length={}", text.length());
        boolean transferFocus = textArea.isFocusOwner();
        switching = true;
        ignoreFocusLost = transferFocus;
        try {
            int caretPosition = Math.min(textArea.getCaretPosition(), text.length());
            isMultiLine = false;
            textField.setText(text);
            textField.setCaretPosition(caretPosition);
            cardLayout.show(containerPanel, CARD_SINGLE);

            restoreRowHeight();
            containerPanel.revalidate();
            containerPanel.repaint();
            completeFocusTransfer(textField, transferFocus, false);
        } finally {
            switching = false;
        }
    }

    private void completeFocusTransfer(JTextField target, boolean transferFocus, boolean expectedMultiLineMode) {
        completeFocusTransfer((JTextComponent) target, transferFocus, expectedMultiLineMode);
    }

    private void completeFocusTransfer(JTextArea target, boolean transferFocus, boolean expectedMultiLineMode) {
        completeFocusTransfer((JTextComponent) target, transferFocus, expectedMultiLineMode);
    }

    private void completeFocusTransfer(JTextComponent target,
                                       boolean transferFocus,
                                       boolean expectedMultiLineMode) {
        if (!transferFocus) {
            ignoreFocusLost = false;
            return;
        }

        long session = editingSession;
        target.requestFocusInWindow();
        SwingUtilities.invokeLater(() -> {
            if (session != editingSession || !editingActive) return;

            Component focusOwner = KeyboardFocusManager
                    .getCurrentKeyboardFocusManager()
                    .getFocusOwner();
            boolean focusRemainsInTable = focusOwner == null
                    || focusOwner == currentTable
                    || SwingUtilities.isDescendingFrom(focusOwner, currentTable);
            if (!focusRemainsInTable) {
                ignoreFocusLost = false;
                forceStopCellEditing();
                return;
            }

            if (isMultiLine == expectedMultiLineMode) {
                target.requestFocusInWindow();
            }
            SwingUtilities.invokeLater(() -> {
                if (session == editingSession) {
                    ignoreFocusLost = false;
                }
            });
        });
    }

    private void updateRowHeight(int lines) {
        if (currentTable == null || !rowHeightExpanded) return;
        int visibleLines = Math.min(MAX_EDITOR_LINES, Math.max(MIN_EDITOR_LINES, lines));
        applyRowHeight(currentTable, currentRow,
                textArea.getFontMetrics(textArea.getFont()).getHeight() * visibleLines + 14);
    }

    private static void applyRowHeight(JTable table, int row, int height) {
        if (table == null || row < 0 || row >= table.getRowCount()) return;
        table.setRowHeight(row, height);
        SwingUtilities.invokeLater(() -> {
            Container parent = table.getParent();
            while (parent != null) {
                if (parent instanceof JScrollPane) {
                    parent.validate();
                    break;
                }
                parent = parent.getParent();
            }
            if (row < table.getRowCount()) {
                table.scrollRectToVisible(table.getCellRect(row, 0, true));
            }
        });
    }

    @Override
    public Object getCellEditorValue() {
        return isMultiLine && textArea != null ? textArea.getText() : textField.getText();
    }

    @Override
    public Component getTableCellEditorComponent(JTable table,
                                                 Object value,
                                                 boolean isSelected,
                                                 int row,
                                                 int column) {
        editingSession++;
        modeReevaluationRequested = false;
        inputMethodComposing = false;

        if (rowHeightExpanded && currentTable != null && originalRowHeight > 0) {
            currentTable.setRowHeight(currentRow, originalRowHeight);
            rowHeightExpanded = false;
        }

        this.currentTable = table;
        this.currentRow = row;
        this.currentColumn = column;
        this.originalRowHeight = table.getRowHeight(row);
        this.editingActive = true;
        this.ignoreFocusLost = false;
        styleEditorComponents(table, row);

        String text = value == null ? "" : value.toString();
        switching = true;
        try {
            if (textArea != null && needsMultiLineEdit(text)) {
                isMultiLine = true;
                textArea.setText(text);
                textArea.setCaretPosition(text.length());
                cardLayout.show(containerPanel, CARD_MULTI);
                rowHeightExpanded = true;
                updateRowHeight(countLines(text));
            } else {
                isMultiLine = false;
                textField.setText(text);
                cardLayout.show(containerPanel, CARD_SINGLE);
                SwingUtilities.invokeLater(textField::selectAll);
            }
        } finally {
            switching = false;
        }
        return containerPanel;
    }

    private void styleEditorComponents(JTable table, int row) {
        TableUIConstants.styleCellEditorContainer(containerPanel, table, row);
        TableUIConstants.styleContainedTextCellEditor(textField, table, row);
        if (textArea != null) {
            TableUIConstants.styleContainedTextCellEditor(textArea, table, row);
        }
        if (scrollPane != null) {
            TableUIConstants.styleEditorScrollPane(scrollPane, table, row);
        }
    }

    @Override
    public boolean stopCellEditing() {
        if (ignoreFocusLost) return false;
        finishEditingSession();
        return super.stopCellEditing();
    }

    public boolean forceStopCellEditing() {
        ignoreFocusLost = false;
        finishEditingSession();
        return super.stopCellEditing();
    }

    @Override
    public void cancelCellEditing() {
        if (ignoreFocusLost) return;
        finishEditingSession();
        super.cancelCellEditing();
    }

    private void finishEditingSession() {
        editingActive = false;
        inputMethodComposing = false;
        ignoreFocusLost = false;
        modeReevaluationRequested = false;
        editingSession++;
        restoreRowHeight();
    }

    private void restoreRowHeight() {
        if (!rowHeightExpanded || currentTable == null || originalRowHeight <= 0) return;
        currentTable.setRowHeight(currentRow, originalRowHeight);
        rowHeightExpanded = false;
        JTable table = currentTable;
        SwingUtilities.invokeLater(() -> {
            Container parent = table.getParent();
            while (parent != null) {
                if (parent instanceof JScrollPane) {
                    parent.validate();
                    parent.repaint();
                    break;
                }
                parent = parent.getParent();
            }
        });
    }

    private boolean needsMultiLineEdit(String text) {
        if (text == null || text.isEmpty()) return false;
        if (text.contains("\n")) return true;
        if (currentTable == null) return false;
        Font font = textField.getFont();
        if (font == null) return false;
        FontMetrics metrics = textField.getFontMetrics(font);
        if (metrics == null) return false;
        int width = containerPanel.getWidth();
        if (width <= 0) {
            width = currentTable.getColumnModel().getColumn(currentColumn).getWidth();
        }
        width -= 20;
        return width > 0 && metrics.stringWidth(text) > width;
    }

    private int countLines(String text) {
        if (text == null || text.isEmpty() || currentTable == null) return 1;
        int width = containerPanel.getWidth();
        if (width <= 0) {
            width = currentTable.getColumnModel().getColumn(currentColumn).getWidth();
        }
        width -= 20;
        if (width <= 0) return text.split("\n", -1).length;

        FontMetrics metrics = textField.getFontMetrics(textField.getFont());
        int total = 0;
        for (String segment : text.split("\n", -1)) {
            total += segment.isEmpty()
                    ? 1
                    : Math.max(1, (int) Math.ceil((double) metrics.stringWidth(segment) / width));
            if (total >= MAX_EDITOR_LINES) return MAX_EDITOR_LINES;
        }
        return total;
    }
}
