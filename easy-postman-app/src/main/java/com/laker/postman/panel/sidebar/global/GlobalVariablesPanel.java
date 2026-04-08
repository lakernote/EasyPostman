package com.laker.postman.panel.sidebar.global;

import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.json.JSONUtil;
import com.laker.postman.common.component.SearchTextField;
import com.laker.postman.common.component.button.EditButton;
import com.laker.postman.common.component.button.SaveButton;
import com.laker.postman.common.component.table.EasyVariableTablePanel;
import com.laker.postman.common.constants.ModernColors;
import com.laker.postman.model.Variable;
import com.laker.postman.service.GlobalVariablesService;
import com.laker.postman.util.FontsUtil;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.IconUtil;
import com.laker.postman.util.MessageKeys;
import com.laker.postman.util.NotificationUtil;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.TableModelEvent;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * 全局变量管理面板。
 */
@Slf4j
public class GlobalVariablesPanel extends JPanel {

    private final EasyVariableTablePanel variablesTablePanel = new EasyVariableTablePanel();
    private final SearchTextField tableSearchField = new SearchTextField();
    private final JLabel shortcutHintLabel = new JLabel();
    private List<Variable> originalVariables = new ArrayList<>();
    private String originalVariablesSnapshot = "[]";
    private boolean isLoadingData = false;

    public GlobalVariablesPanel() {
        initUI();
        registerKeyboardShortcuts();
        initTableValidationAndAutoSave();
        refreshData();
    }

    public final void refreshData() {
        isLoadingData = true;
        try {
            List<Variable> variables = GlobalVariablesService.getInstance()
                    .getGlobalVariables()
                    .getVariableList();
            List<Variable> copiedVariables = copyVariables(variables);
            variablesTablePanel.setVariableList(copiedVariables);
            originalVariables = copyVariables(variablesTablePanel.getVariableListFromModel());
            originalVariablesSnapshot = JSONUtil.toJsonStr(originalVariables);
            applyCurrentSearchFilter();
        } finally {
            isLoadingData = false;
        }
    }

    public void prepareForDisplay() {
        refreshData();
        clearSearch();
        focusPreferredTarget();
    }

    private void initUI() {
        setLayout(new BorderLayout(0, 0));
        setBackground(ModernColors.getBackgroundColor());
        add(createTopPanel(), BorderLayout.NORTH);

        JPanel tableContainer = new JPanel(new BorderLayout());
        tableContainer.setOpaque(false);
        tableContainer.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        tableContainer.add(variablesTablePanel, BorderLayout.CENTER);
        add(tableContainer, BorderLayout.CENTER);
        add(createFooterPanel(), BorderLayout.SOUTH);
    }

    private JPanel createTopPanel() {
        JPanel topPanel = new JPanel(new BorderLayout(12, 0));
        topPanel.setOpaque(false);
        topPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, ModernColors.getDividerBorderColor()),
                BorderFactory.createEmptyBorder(12, 14, 10, 14)
        ));

        topPanel.add(createHeaderPanel(), BorderLayout.WEST);
        topPanel.add(createToolbar(), BorderLayout.EAST);
        return topPanel;
    }

    private JPanel createHeaderPanel() {
        JPanel headerPanel = new JPanel(new BorderLayout(10, 0));
        headerPanel.setOpaque(false);

        JLabel iconLabel = new JLabel(IconUtil.createThemed("icons/global-variables.svg", 22, 22));
        iconLabel.setBorder(BorderFactory.createEmptyBorder(2, 0, 0, 0));
        headerPanel.add(iconLabel, BorderLayout.WEST);

        JPanel textPanel = new JPanel();
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
        textPanel.setOpaque(false);

        JLabel titleLabel = new JLabel(I18nUtil.getMessage(MessageKeys.GLOBAL_VARIABLES_TITLE));
        titleLabel.setFont(FontsUtil.getDefaultFontWithOffset(Font.BOLD, 2));
        titleLabel.setForeground(ModernColors.getTextPrimary());

        JLabel subtitleLabel = new JLabel(I18nUtil.getMessage(MessageKeys.GLOBAL_VARIABLES_SUBTITLE));
        subtitleLabel.setFont(FontsUtil.getDefaultFontWithOffset(Font.PLAIN, -1));
        subtitleLabel.setForeground(ModernColors.getTextSecondary());

        textPanel.add(titleLabel);
        textPanel.add(Box.createVerticalStrut(3));
        textPanel.add(subtitleLabel);
        headerPanel.add(textPanel, BorderLayout.CENTER);
        return headerPanel;
    }

    private JPanel createToolbar() {
        JPanel toolbarPanel = new JPanel();
        toolbarPanel.setOpaque(false);
        toolbarPanel.setLayout(new BoxLayout(toolbarPanel, BoxLayout.X_AXIS));

        EditButton bulkEditButton = new EditButton();
        bulkEditButton.setToolTipText(I18nUtil.getMessage(MessageKeys.ENV_BULK_EDIT));
        bulkEditButton.setPreferredSize(new Dimension(bulkEditButton.getPreferredSize().width, 32));
        bulkEditButton.setMaximumSize(new Dimension(bulkEditButton.getMaximumSize().width, 32));
        bulkEditButton.addActionListener(e -> showBulkEditDialog());

        SaveButton saveButton = new SaveButton();
        saveButton.setToolTipText(I18nUtil.getMessage(MessageKeys.BUTTON_SAVE));
        saveButton.setPreferredSize(new Dimension(saveButton.getPreferredSize().width, 32));
        saveButton.setMaximumSize(new Dimension(saveButton.getMaximumSize().width, 32));
        saveButton.addActionListener(e -> saveVariablesManually());

        tableSearchField.setPlaceholderText(I18nUtil.getMessage(MessageKeys.GLOBAL_VARIABLES_SEARCH_PLACEHOLDER));
        tableSearchField.setPreferredSize(new Dimension(280, 34));
        tableSearchField.setMinimumSize(new Dimension(220, 34));
        tableSearchField.setMaximumSize(new Dimension(320, 34));
        tableSearchField.addActionListener(e -> filterTableRows());
        tableSearchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                filterTableRows();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                filterTableRows();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                filterTableRows();
            }
        });
        tableSearchField.addPropertyChangeListener("caseSensitive", evt -> {
            if (!tableSearchField.getText().isEmpty()) {
                filterTableRows();
            }
        });
        tableSearchField.addPropertyChangeListener("wholeWord", evt -> {
            if (!tableSearchField.getText().isEmpty()) {
                filterTableRows();
            }
        });

        toolbarPanel.add(tableSearchField);
        toolbarPanel.add(Box.createHorizontalStrut(6));
        toolbarPanel.add(bulkEditButton);
        toolbarPanel.add(Box.createHorizontalStrut(4));
        toolbarPanel.add(saveButton);

        return toolbarPanel;
    }

    private JPanel createFooterPanel() {
        JPanel footerPanel = new JPanel(new BorderLayout());
        footerPanel.setOpaque(false);
        footerPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, ModernColors.getDividerBorderColor()),
                BorderFactory.createEmptyBorder(7, 14, 8, 14)
        ));

        shortcutHintLabel.setText(I18nUtil.getMessage(MessageKeys.GLOBAL_VARIABLES_SHORTCUT_HINT));
        shortcutHintLabel.setFont(FontsUtil.getDefaultFontWithOffset(Font.PLAIN, -2));
        shortcutHintLabel.setForeground(ModernColors.getTextSecondary());
        footerPanel.add(shortcutHintLabel, BorderLayout.EAST);
        return footerPanel;
    }

    private void registerKeyboardShortcuts() {
        int menuShortcutMask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();
        InputMap inputMap = getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        ActionMap actionMap = getActionMap();

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_F, menuShortcutMask), "focusSearch");
        actionMap.put("focusSearch", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                focusSearchField();
            }
        });

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_S, menuShortcutMask), "saveGlobals");
        actionMap.put("saveGlobals", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                saveVariablesManually();
            }
        });
    }

    private void initTableValidationAndAutoSave() {
        variablesTablePanel.addTableModelListener(e -> {
            if (isLoadingData) {
                return;
            }

            if (e.getType() == TableModelEvent.INSERT
                    || e.getType() == TableModelEvent.UPDATE
                    || e.getType() == TableModelEvent.DELETE) {
                SwingUtilities.invokeLater(() -> {
                    if (!isLoadingData && !variablesTablePanel.isDragging() && isVariablesChanged()) {
                        autoSaveVariables();
                    }
                });
            }
        });
    }

    private void autoSaveVariables() {
        try {
            persistVariables(false);
            log.debug("Auto-saved global variables");
        } catch (Exception ex) {
            log.error("Failed to auto-save global variables", ex);
        }
    }

    private void saveVariablesManually() {
        try {
            persistVariables(true);
            NotificationUtil.showSuccess(I18nUtil.getMessage(MessageKeys.GLOBAL_VARIABLES_SAVE_SUCCESS));
        } catch (Exception ex) {
            log.error("Failed to save global variables manually", ex);
        }
    }

    private boolean isVariablesChanged() {
        String curJson = JSONUtil.toJsonStr(variablesTablePanel.getVariableListFromModel());
        return !CharSequenceUtil.equals(curJson, originalVariablesSnapshot);
    }

    private void persistVariables(boolean stopEditing) {
        if (stopEditing) {
            variablesTablePanel.stopCellEditing();
        }

        List<Variable> variableList = stopEditing
                ? variablesTablePanel.getVariableList()
                : variablesTablePanel.getVariableListFromModel();
        GlobalVariablesService service = GlobalVariablesService.getInstance();
        List<Variable> mergedVariables = service.mergeVariables(originalVariables, variableList);
        service.replaceGlobalVariables(mergedVariables);
        updateSnapshotAndTable(mergedVariables, variableList);
    }

    private void updateSnapshotAndTable(List<Variable> persistedVariables, List<Variable> currentTableVariables) {
        List<Variable> persistedCopy = copyVariables(persistedVariables);
        originalVariables = persistedCopy;
        originalVariablesSnapshot = JSONUtil.toJsonStr(persistedCopy);

        if (JSONUtil.toJsonStr(currentTableVariables).equals(originalVariablesSnapshot)) {
            return;
        }

        isLoadingData = true;
        try {
            variablesTablePanel.setVariableList(persistedCopy);
            applyCurrentSearchFilter();
        } finally {
            isLoadingData = false;
        }
    }

    private List<Variable> copyVariables(List<Variable> variables) {
        List<Variable> result = new ArrayList<>();
        if (variables == null) {
            return result;
        }
        for (Variable variable : variables) {
            if (variable == null) {
                continue;
            }
            result.add(new Variable(variable.isEnabled(), variable.getKey(), variable.getValue()));
        }
        return result;
    }

    private void filterTableRows() {
        String keyword = tableSearchField.getText();
        boolean caseSensitive = tableSearchField.isCaseSensitive();
        boolean wholeWord = tableSearchField.isWholeWord();

        if (keyword == null || keyword.trim().isEmpty()) {
            variablesTablePanel.getTable().setRowSorter(null);
            tableSearchField.setNoResult(false);
            return;
        }

        TableRowSorter<TableModel> sorter = new TableRowSorter<>(variablesTablePanel.getTable().getModel());
        final String searchKeyword = caseSensitive ? keyword : keyword.toLowerCase();

        sorter.setRowFilter(new RowFilter<>() {
            @Override
            public boolean include(Entry<? extends TableModel, ? extends Integer> entry) {
                String name = String.valueOf(entry.getValue(1));
                String value = String.valueOf(entry.getValue(2));
                String searchName = caseSensitive ? name : name.toLowerCase();
                String searchValue = caseSensitive ? value : value.toLowerCase();
                if (wholeWord) {
                    return matchesWholeWord(searchName, searchKeyword) || matchesWholeWord(searchValue, searchKeyword);
                }
                return searchName.contains(searchKeyword) || searchValue.contains(searchKeyword);
            }
        });

        variablesTablePanel.getTable().setRowSorter(sorter);
        tableSearchField.setNoResult(variablesTablePanel.getTable().getRowCount() == 0);
    }

    private void applyCurrentSearchFilter() {
        if (CharSequenceUtil.isBlank(tableSearchField.getText())) {
            variablesTablePanel.getTable().setRowSorter(null);
            tableSearchField.setNoResult(false);
            return;
        }
        filterTableRows();
    }

    private void clearSearch() {
        tableSearchField.setText("");
        variablesTablePanel.getTable().setRowSorter(null);
        tableSearchField.setNoResult(false);
    }

    private void focusSearchField() {
        SwingUtilities.invokeLater(() -> {
            tableSearchField.requestFocusInWindow();
            tableSearchField.selectAll();
        });
    }

    private void focusPreferredTarget() {
        SwingUtilities.invokeLater(() -> {
            JTable table = variablesTablePanel.getTable();
            if (variablesTablePanel.getVariableListFromModel().isEmpty()) {
                table.requestFocusInWindow();
                table.changeSelection(0, 1, false, false);
                if (table.editCellAt(0, 1)) {
                    Component editor = table.getEditorComponent();
                    if (editor instanceof JTextField textField) {
                        textField.requestFocusInWindow();
                        textField.selectAll();
                    } else if (editor != null) {
                        editor.requestFocusInWindow();
                    }
                }
                return;
            }

            table.clearSelection();
            table.requestFocusInWindow();
        });
    }

    private boolean matchesWholeWord(String text, String keyword) {
        if (text == null || keyword == null) {
            return false;
        }

        int index = 0;
        while ((index = text.indexOf(keyword, index)) != -1) {
            int start = index;
            int end = index + keyword.length();

            if (start > 0) {
                char prevChar = text.charAt(start - 1);
                if (Character.isLetterOrDigit(prevChar) || prevChar == '_') {
                    index++;
                    continue;
                }
            }

            if (end < text.length()) {
                char nextChar = text.charAt(end);
                if (Character.isLetterOrDigit(nextChar) || nextChar == '_') {
                    index++;
                    continue;
                }
            }

            return true;
        }

        return false;
    }

    private void showBulkEditDialog() {
        StringBuilder text = new StringBuilder();
        List<Variable> currentVariables = variablesTablePanel.getVariableList();
        for (Variable variable : currentVariables) {
            if (!variable.getKey().isEmpty()) {
                text.append(variable.getKey()).append(": ").append(variable.getValue()).append("\n");
            }
        }

        JTextArea textArea = new JTextArea(text.toString());
        textArea.setLineWrap(false);
        textArea.setTabSize(4);
        textArea.setBackground(ModernColors.getInputBackgroundColor());
        textArea.setForeground(ModernColors.getTextPrimary());
        textArea.setCaretColor(ModernColors.PRIMARY);
        textArea.setCaretPosition(textArea.getDocument().getLength());

        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setPreferredSize(new Dimension(600, 400));

        JPanel hintPanel = new JPanel();
        hintPanel.setLayout(new BoxLayout(hintPanel, BoxLayout.Y_AXIS));
        hintPanel.setOpaque(false);

        JLabel hintLabel = new JLabel(I18nUtil.getMessage(MessageKeys.ENV_BULK_EDIT_HINT));
        hintLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        hintLabel.setForeground(ModernColors.getTextPrimary());

        JLabel formatLabel = new JLabel(I18nUtil.getMessage(MessageKeys.ENV_BULK_EDIT_SUPPORTED_FORMATS));
        formatLabel.setForeground(ModernColors.getTextSecondary());
        formatLabel.setFont(FontsUtil.getDefaultFontWithOffset(Font.PLAIN, -2));
        formatLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        hintPanel.add(hintLabel);
        hintPanel.add(Box.createVerticalStrut(6));
        hintPanel.add(formatLabel);
        hintPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        JPanel contentPanel = new JPanel(new BorderLayout(0, 5));
        contentPanel.add(hintPanel, BorderLayout.NORTH);
        contentPanel.add(scrollPane, BorderLayout.CENTER);
        contentPanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        JButton okButton = new JButton(I18nUtil.getMessage(MessageKeys.GENERAL_OK));
        JButton cancelButton = new JButton(I18nUtil.getMessage(MessageKeys.BUTTON_CANCEL));
        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);

        Window window = SwingUtilities.getWindowAncestor(this);
        JDialog dialog = new JDialog(window, I18nUtil.getMessage(MessageKeys.GLOBAL_VARIABLES_BULK_EDIT_TITLE),
                Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setLayout(new BorderLayout());
        dialog.add(contentPanel, BorderLayout.CENTER);
        dialog.add(buttonPanel, BorderLayout.SOUTH);
        dialog.setSize(650, 400);
        dialog.setMinimumSize(new Dimension(500, 300));
        dialog.setResizable(true);
        dialog.setLocationRelativeTo(this);

        okButton.addActionListener(e -> {
            parseBulkText(textArea.getText());
            dialog.dispose();
        });
        cancelButton.addActionListener(e -> dialog.dispose());
        dialog.getRootPane().registerKeyboardAction(
                e -> dialog.dispose(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW
        );
        dialog.getRootPane().setDefaultButton(okButton);
        dialog.setVisible(true);
    }

    private void parseBulkText(String text) {
        if (text == null || text.trim().isEmpty()) {
            variablesTablePanel.setVariableList(new ArrayList<>());
            return;
        }

        List<Variable> variables = new ArrayList<>();
        String[] lines = text.split("\n");

        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("#") || line.startsWith("//")) {
                continue;
            }

            String[] parts = null;
            if (line.contains(":")) {
                parts = line.split(":", 2);
            } else if (line.contains("=")) {
                parts = line.split("=", 2);
            }

            if (parts != null && parts.length == 2) {
                String key = parts[0].trim();
                String value = parts[1].trim();
                if (!key.isEmpty()) {
                    variables.add(new Variable(true, key, value));
                }
            } else if (line.contains(":") || line.contains("=")) {
                String key = line.replaceAll("[=:].*", "").trim();
                if (!key.isEmpty()) {
                    variables.add(new Variable(true, key, ""));
                }
            }
        }

        variablesTablePanel.setVariableList(variables);
    }
}
