package com.laker.postman.common.component;


import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 支持自动补全的增强版 EasyTextField
 * 继承自 EasyTextField，保留所有原有功能（变量高亮、撤销重做等）
 * 新增：支持普通文本的自动补全功能
 */
public class AutoCompleteEasyTextField extends EasyTextField {
    private static final int MAX_VISIBLE_ROWS = 8;
    private static final int ROW_HEIGHT = 28;
    private static final int MIN_POPUP_WIDTH = 200;
    private static final int POPUP_BORDER_WIDTH = 4;

    private JWindow popup;
    private JList<String> suggestionList;
    private DefaultListModel<String> listModel;
    private List<String> suggestions;
    private boolean autoCompleteEnabled = false;
    private boolean isUpdatingSuggestions = false;

    public AutoCompleteEasyTextField(int columns) {
        super(columns);
        initAutoComplete();
    }

    public AutoCompleteEasyTextField(String text, int columns) {
        super(text, columns);
        initAutoComplete();
    }

    public AutoCompleteEasyTextField(String text, int columns, String placeholderText) {
        super(text, columns, placeholderText);
        initAutoComplete();
    }

    /**
     * 设置自动补全建议列表
     */
    public void setSuggestions(List<String> suggestions) {
        if (suggestions == null || suggestions.isEmpty()) {
            this.suggestions = Collections.emptyList();
            this.autoCompleteEnabled = false;
            hidePopup();
        } else {
            // 只有当列表真正改变时才更新
            if (!suggestions.equals(this.suggestions)) {
                this.suggestions = new ArrayList<>(suggestions);
            }
        }
    }

    /**
     * 启用或禁用自动补全
     */
    public void setAutoCompleteEnabled(boolean enabled) {
        this.autoCompleteEnabled = enabled;
        if (!enabled) {
            hidePopup();
        }
    }

    private void initAutoComplete() {
        this.suggestions = Collections.emptyList();
        this.listModel = new DefaultListModel<>();
        this.suggestionList = new JList<>(listModel);
        this.popup = new JWindow();

        popup.setFocusableWindowState(false);
        popup.setType(Window.Type.POPUP);

        suggestionList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        suggestionList.setVisibleRowCount(MAX_VISIBLE_ROWS);
        suggestionList.setFont(getFont());
        suggestionList.setFocusable(false);

        suggestionList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value,
                                                          int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
                return this;
            }
        });

        JScrollPane scrollPane = new JScrollPane(suggestionList);
        scrollPane.setBorder(new LineBorder(Color.GRAY, 1));
        popup.add(scrollPane);

        setupAutoCompleteListeners();
    }

    private void setupAutoCompleteListeners() {
        // 监听文本变化 - 优化：减少不必要的更新
        getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            private void scheduleUpdate() {
                if (autoCompleteEnabled && !isUpdatingSuggestions) {
                    SwingUtilities.invokeLater(() -> updateSuggestions());
                }
            }

            @Override
            public void insertUpdate(javax.swing.event.DocumentEvent e) {
                scheduleUpdate();
            }

            @Override
            public void removeUpdate(javax.swing.event.DocumentEvent e) {
                scheduleUpdate();
            }

            @Override
            public void changedUpdate(javax.swing.event.DocumentEvent e) {
                scheduleUpdate();
            }
        });

        // 键盘事件处理
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (!popup.isVisible() || !autoCompleteEnabled) {
                    return;
                }
                handleKeyboardNavigation(e);
            }
        });

        // 鼠标点击选择
        suggestionList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 1) {
                    acceptSelectedSuggestion();
                }
            }
        });

        // 失去焦点时隐藏
        addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                SwingUtilities.invokeLater(() -> {
                    if (!popup.isActive()) {
                        hidePopup();
                    }
                });
            }
        });
    }

    private void handleKeyboardNavigation(KeyEvent e) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_DOWN:
                e.consume();
                moveSelection(1);
                break;
            case KeyEvent.VK_UP:
                e.consume();
                moveSelection(-1);
                break;
            case KeyEvent.VK_ENTER, KeyEvent.VK_TAB:
                if (suggestionList.getSelectedIndex() >= 0) {
                    e.consume();
                    acceptSelectedSuggestion();
                }
                break;
            case KeyEvent.VK_ESCAPE:
                e.consume();
                hidePopup();
                break;
            default:
                // Do nothing for other keys
                break;
        }
    }

    private void moveSelection(int direction) {
        int currentIndex = suggestionList.getSelectedIndex();
        int newIndex = currentIndex + direction;

        if (newIndex >= 0 && newIndex < listModel.getSize()) {
            suggestionList.setSelectedIndex(newIndex);
            suggestionList.ensureIndexIsVisible(newIndex);
        }
    }

    private void updateSuggestions() {
        if (!autoCompleteEnabled || suggestions.isEmpty() || isUpdatingSuggestions) {
            hidePopup();
            return;
        }

        isUpdatingSuggestions = true;
        try {
            String text = getText();
            text = (text == null) ? "" : text.trim();

            listModel.clear();

            if (text.isEmpty()) {
                addAllSuggestions();
            } else {
                filterAndAddSuggestions(text);
            }

            updatePopupVisibility();
        } finally {
            isUpdatingSuggestions = false;
        }
    }

    private void addAllSuggestions() {
        for (String suggestion : suggestions) {
            listModel.addElement(suggestion);
        }
    }

    private void filterAndAddSuggestions(String text) {
        // 检查是否完全匹配
        if (hasExactMatch(text)) {
            return; // 完全匹配时不显示建议列表
        }

        // 三级匹配策略：精确前缀 > 忽略大小写前缀 > 包含匹配
        if (tryAddPrefixMatches(text, true)) {
            return;
        }
        if (tryAddPrefixMatches(text, false)) {
            return;
        }
        addContainsMatches(text);
    }

    private boolean hasExactMatch(String text) {
        for (String suggestion : suggestions) {
            if (suggestion.equalsIgnoreCase(text)) {
                return true;
            }
        }
        return false;
    }

    private boolean tryAddPrefixMatches(String text, boolean caseSensitive) {
        boolean found = false;
        String compareText = caseSensitive ? text : text.toLowerCase();

        for (String suggestion : suggestions) {
            String compareSuggestion = caseSensitive ? suggestion : suggestion.toLowerCase();
            if (compareSuggestion.startsWith(compareText)) {
                listModel.addElement(suggestion);
                found = true;
            }
        }
        return found;
    }

    private void addContainsMatches(String text) {
        String lowerText = text.toLowerCase();
        for (String suggestion : suggestions) {
            if (suggestion.toLowerCase().contains(lowerText)) {
                listModel.addElement(suggestion);
            }
        }
    }

    private void updatePopupVisibility() {
        if (listModel.isEmpty()) {
            hidePopup();
        } else {
            suggestionList.setSelectedIndex(0);
            showPopup();
        }
    }

    private void showPopup() {
        if (!isShowing()) {
            hidePopup();
            return;
        }

        try {
            Point location = getLocationOnScreen();
            Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();

            int popupWidth = Math.max(getWidth(), MIN_POPUP_WIDTH);
            int popupHeight = Math.min(listModel.getSize() * ROW_HEIGHT + POPUP_BORDER_WIDTH,
                                      MAX_VISIBLE_ROWS * ROW_HEIGHT + POPUP_BORDER_WIDTH);

            int x = location.x;
            int y = location.y + getHeight();

            // 检查右边界
            if (x + popupWidth > screenSize.width) {
                x = screenSize.width - popupWidth - 10;
            }

            // 检查下边界，如果空间不够则显示在上方
            if (y + popupHeight > screenSize.height) {
                y = location.y - popupHeight;
                // 如果上方也不够，则尽可能显示在下方
                if (y < 0) {
                    y = location.y + getHeight();
                    popupHeight = Math.min(popupHeight, screenSize.height - y - 10);
                }
            }

            popup.setLocation(x, y);
            popup.setSize(popupWidth, popupHeight);

            if (!popup.isVisible()) {
                popup.setVisible(true);
            }
        } catch (IllegalComponentStateException e) {
            // 组件不可见时忽略
            hidePopup();
        }
    }

    private void hidePopup() {
        if (popup != null && popup.isVisible()) {
            popup.setVisible(false);
        }
    }

    private void acceptSelectedSuggestion() {
        String selected = suggestionList.getSelectedValue();
        if (selected != null) {
            isUpdatingSuggestions = true;
            try {
                setText(selected);
                setCaretPosition(selected.length());
            } finally {
                isUpdatingSuggestions = false;
            }
            hidePopup();
        }
    }

    /**
     * 显示所有建议（通常在获得焦点或点击时调用）
     */
    public void showAllSuggestions() {
        if (autoCompleteEnabled && !suggestions.isEmpty()) {
            updateSuggestions();
        }
    }
}
