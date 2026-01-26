package com.laker.postman.common.component;


import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;

/**
 * 支持自动补全的增强版 EasyTextField
 * 继承自 EasyTextField，保留所有原有功能（变量高亮、撤销重做等）
 * 新增：支持普通文本的自动补全功能
 */
public class AutoCompleteEasyTextField extends EasyTextField {
    private JWindow popup;
    private JList<String> suggestionList;
    private DefaultListModel<String> listModel;
    private List<String> suggestions;
    private boolean autoCompleteEnabled = false;

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
        this.suggestions = suggestions != null ? new ArrayList<>(suggestions) : new ArrayList<>();
        this.autoCompleteEnabled = !this.suggestions.isEmpty();
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
        this.suggestions = new ArrayList<>();
        this.listModel = new DefaultListModel<>();
        this.suggestionList = new JList<>(listModel);
        this.popup = new JWindow();

        popup.setFocusableWindowState(false);

        suggestionList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        suggestionList.setVisibleRowCount(8);
        suggestionList.setFont(getFont());

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
        // 监听文本变化
        getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            @Override
            public void insertUpdate(javax.swing.event.DocumentEvent e) {
                if (autoCompleteEnabled) {
                    SwingUtilities.invokeLater(() -> updateSuggestions());
                }
            }

            @Override
            public void removeUpdate(javax.swing.event.DocumentEvent e) {
                if (autoCompleteEnabled) {
                    SwingUtilities.invokeLater(() -> updateSuggestions());
                }
            }

            @Override
            public void changedUpdate(javax.swing.event.DocumentEvent e) {
                if (autoCompleteEnabled) {
                    SwingUtilities.invokeLater(() -> updateSuggestions());
                }
            }
        });

        // 键盘事件处理
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (!popup.isVisible() || !autoCompleteEnabled) {
                    return;
                }

                switch (e.getKeyCode()) {
                    case KeyEvent.VK_DOWN:
                        e.consume();
                        int selectedIndex = suggestionList.getSelectedIndex();
                        if (selectedIndex < listModel.getSize() - 1) {
                            suggestionList.setSelectedIndex(selectedIndex + 1);
                            suggestionList.ensureIndexIsVisible(selectedIndex + 1);
                        }
                        break;
                    case KeyEvent.VK_UP:
                        e.consume();
                        int upSelectedIndex = suggestionList.getSelectedIndex();
                        if (upSelectedIndex > 0) {
                            suggestionList.setSelectedIndex(upSelectedIndex - 1);
                            suggestionList.ensureIndexIsVisible(upSelectedIndex - 1);
                        }
                        break;
                    case KeyEvent.VK_ENTER:
                    case KeyEvent.VK_TAB:
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

    private void updateSuggestions() {
        if (!autoCompleteEnabled || suggestions.isEmpty()) {
            hidePopup();
            return;
        }

        String text = getText().trim();
        listModel.clear();

        if (text.isEmpty()) {
            // 空文本时显示所有建议
            for (String suggestion : suggestions) {
                listModel.addElement(suggestion);
            }
        } else {
            // 检查是否完全匹配某个建议（大小写不敏感）
            boolean exactMatch = false;
            for (String suggestion : suggestions) {
                if (suggestion.equalsIgnoreCase(text)) {
                    exactMatch = true;
                    break;
                }
            }

            // 如果完全匹配，不显示建议列表
            if (exactMatch) {
                hidePopup();
                return;
            }

            // 智能过滤匹配的建议
            // 1. 先添加开头匹配的（优先级高）
            // 2. 再添加包含匹配的（优先级低）
            String lowerText = text.toLowerCase();
            List<String> startsWithMatches = new ArrayList<>();
            List<String> containsMatches = new ArrayList<>();

            for (String suggestion : suggestions) {
                String lowerSuggestion = suggestion.toLowerCase();
                if (lowerSuggestion.startsWith(lowerText)) {
                    startsWithMatches.add(suggestion);
                } else if (lowerSuggestion.contains(lowerText)) {
                    containsMatches.add(suggestion);
                }
            }

            // 先添加开头匹配的
            for (String match : startsWithMatches) {
                listModel.addElement(match);
            }
            // 再添加包含匹配的
            for (String match : containsMatches) {
                listModel.addElement(match);
            }
        }

        if (listModel.isEmpty()) {
            hidePopup();
        } else {
            suggestionList.setSelectedIndex(0);
            showPopup();
        }
    }

    private void showPopup() {
        try {
            Point location = getLocationOnScreen();
            int x = location.x;
            int y = location.y + getHeight();

            int popupWidth = Math.max(getWidth(), 200);
            int popupHeight = Math.min(listModel.getSize() * 28 + 4, 224);

            popup.setLocation(x, y);
            popup.setSize(popupWidth, popupHeight);

            if (!popup.isVisible()) {
                popup.setVisible(true);
            }
        } catch (IllegalComponentStateException e) {
            // 组件不可见时忽略
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
            setText(selected);
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
