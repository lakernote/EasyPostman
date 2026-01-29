package com.laker.postman.common.component;

import com.formdev.flatlaf.extras.components.FlatTextField;
import com.laker.postman.common.constants.ModernColors;
import com.laker.postman.util.FontsUtil;
import com.laker.postman.util.IconUtil;
import lombok.extern.slf4j.Slf4j;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rtextarea.SearchContext;
import org.fife.ui.rtextarea.SearchEngine;
import org.fife.ui.rtextarea.SearchResult;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

@Slf4j
public class SearchReplacePanel extends JPanel {

    // 图标路径常量
    private static final String ICON_EXPAND = "icons/expand.svg";
    private static final String ICON_COLLAPSE = "icons/collapse.svg";

    // 状态消息常量
    private static final String MSG_NO_RESULTS = "No results";
    private static final String MSG_REPLACED_FORMAT = "%d replaced";

    private final RSyntaxTextArea textArea;
    private final SearchTextField searchField;
    private final FlatTextField replaceField;
    private final JToggleButton toggleReplaceBtn;
    private final JPanel replacePanel;
    private final JLabel statusLabel;  // 搜索结果状态标签

    // 防抖Timer，避免输入时频繁搜索
    private Timer searchDebounceTimer;

    public SearchReplacePanel(RSyntaxTextArea textArea) {
        this.textArea = textArea;

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UIManager.getColor("Component.borderColor"), 1),
                new EmptyBorder(3, 3, 3, 3)
        ));
        // 搜索面板
        JPanel searchPanel = new JPanel();
        searchPanel.setLayout(new BoxLayout(searchPanel, BoxLayout.X_AXIS));
        searchPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        // 展开/收起替换面板的按钮（放在最左边，类似 Postman）
        toggleReplaceBtn = new JToggleButton(IconUtil.createThemed(ICON_EXPAND, 16, 16));
        toggleReplaceBtn.setToolTipText("Toggle Replace");
        toggleReplaceBtn.setFocusable(false);
        toggleReplaceBtn.setContentAreaFilled(false);
        toggleReplaceBtn.setBorderPainted(false);
        toggleReplaceBtn.setPreferredSize(new Dimension(16, 16));
        toggleReplaceBtn.setMaximumSize(new Dimension(16, 16));
        // actionListener 将在 replacePanel 创建后设置

        // 搜索输入框 - 使用 SearchTextField 复用大小写敏感和整词匹配功能
        searchField = new SearchTextField();
        searchField.setPreferredSize(new Dimension(180, 28));
        searchField.setMaximumSize(new Dimension(220, 28));
        searchField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    if (e.isShiftDown()) {
                        findPrevious();
                    } else {
                        findNext();
                    }
                } else if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    hidePanel();
                }
            }
        });

        // 添加文本变化监听器，实时更新搜索结果状态（使用防抖）
        searchField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            @Override
            public void insertUpdate(javax.swing.event.DocumentEvent e) {
                scheduleSearchUpdate();
            }

            @Override
            public void removeUpdate(javax.swing.event.DocumentEvent e) {
                scheduleSearchUpdate();
            }

            @Override
            public void changedUpdate(javax.swing.event.DocumentEvent e) {
                scheduleSearchUpdate();
            }
        });

        // 查找按钮
        JButton findPrevBtn = createIconButton("icons/arrow-up.svg", "Previous (Shift+Enter)", e -> findPrevious());
        JButton findNextBtn = createIconButton("icons/arrow-down.svg", "Next (Enter)", e -> findNext());


        // 状态标签
        statusLabel = new JLabel(MSG_NO_RESULTS);
        statusLabel.setFont(FontsUtil.getDefaultFontWithOffset(Font.PLAIN, -2));
        statusLabel.setForeground(ModernColors.getTextDisabled());
        statusLabel.setPreferredSize(new Dimension(70, 24));
        statusLabel.setMaximumSize(new Dimension(90, 24));
        statusLabel.setHorizontalAlignment(SwingConstants.CENTER);

        // 关闭按钮
        JButton closeBtn = createIconButton("icons/close.svg", "Close (Esc)", e -> hidePanel());


        // 组装搜索面板
        searchPanel.add(toggleReplaceBtn);
        searchPanel.add(Box.createHorizontalStrut(2));
        searchPanel.add(searchField);
        searchPanel.add(Box.createHorizontalStrut(2));
        searchPanel.add(findPrevBtn);
        searchPanel.add(findNextBtn);
        searchPanel.add(Box.createHorizontalStrut(4));
        searchPanel.add(statusLabel);
        searchPanel.add(Box.createHorizontalStrut(2));
        searchPanel.add(closeBtn);
        searchPanel.add(Box.createHorizontalGlue());

        add(searchPanel);

        // 替换面板（默认隐藏）
        replacePanel = new JPanel();
        replacePanel.setLayout(new BoxLayout(replacePanel, BoxLayout.X_AXIS));
        replacePanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        replacePanel.setVisible(false);

        // 左侧占位符，保持与搜索框对齐
        JPanel spacer = new JPanel();
        spacer.setOpaque(false);
        spacer.setPreferredSize(new Dimension(16, 16));  // 与切换按钮宽度一致
        spacer.setMaximumSize(new Dimension(16, 16));

        // 替换输入框
        replaceField = new FlatTextField();
        replaceField.setPlaceholderText("Enter Replace");
        replaceField.setPreferredSize(new Dimension(180, 28));
        replaceField.setMaximumSize(new Dimension(220, 28));
        replaceField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    replace();
                } else if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    hidePanel();
                }
            }
        });

        // 替换按钮
        JButton replaceBtn = createIconButton("icons/replace.svg", "Replace", e -> replace());
        JButton replaceAllBtn = createIconButton("icons/replace-all.svg", "Replace All", e -> replaceAll());

        // 组装替换面板
        replacePanel.add(spacer);  // 左侧占位符
        replacePanel.add(Box.createHorizontalStrut(2));
        replacePanel.add(replaceField);
        replacePanel.add(Box.createHorizontalStrut(2));
        replacePanel.add(replaceBtn);
        replacePanel.add(Box.createHorizontalStrut(2));
        replacePanel.add(replaceAllBtn);
        replacePanel.add(Box.createHorizontalGlue());

        add(Box.createVerticalStrut(2));
        add(replacePanel);

        // 设置 toggleReplaceBtn 的 actionListener（现在 replacePanel 已经初始化）
        toggleReplaceBtn.addActionListener(e -> {
            boolean selected = toggleReplaceBtn.isSelected();
            replacePanel.setVisible(selected);
            // 更新图标
            if (selected) {
                toggleReplaceBtn.setIcon(IconUtil.createThemed(ICON_COLLAPSE, 16, 16));
            } else {
                toggleReplaceBtn.setIcon(IconUtil.createThemed(ICON_EXPAND, 16, 16));
            }
            // 先标记需要重新布局
            invalidate();
            // 强制重新计算 PreferredSize
            revalidate();
            // 触发父容器重新布局以调整面板大小和位置
            Container parent = getParent();
            if (parent != null) {
                parent.revalidate();
                parent.repaint();
            }
            // 直接触发 ComponentListener 的 resize 事件
            // 通过改变 bounds 来触发
            Rectangle bounds = getBounds();
            setBounds(bounds.x, bounds.y, bounds.width, getPreferredSize().height);
        });

        // 默认隐藏
        setVisible(false);
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension size = super.getPreferredSize();
        // 确保宽度足够显示所有控件
        int minWidth = 280;  // 减小最小宽度以匹配更紧凑的设计
        // 高度根据是否显示替换面板动态调整
        int height = replacePanel.isVisible() ? 70 : 36;
        return new Dimension(Math.max(size.width, minWidth), Math.max(size.height, height));
    }

    /**
     * 创建图标按钮
     */
    private JButton createIconButton(String iconPath, String tooltip, java.awt.event.ActionListener listener) {
        JButton btn = new JButton(IconUtil.createThemed(iconPath, 16, 16));
        btn.setToolTipText(tooltip);
        btn.setPreferredSize(new Dimension(24, 24));
        btn.setMaximumSize(new Dimension(24, 24));
        if (listener != null) {
            btn.addActionListener(listener);
        }
        return btn;
    }


    /**
     * 显示搜索面板（仅搜索模式）
     */
    public void showSearch() {
        replacePanel.setVisible(false);
        toggleReplaceBtn.setSelected(false);
        toggleReplaceBtn.setIcon(IconUtil.createThemed(ICON_EXPAND, 16, 16));
        setVisible(true);

        // 如果有选中文本，将其作为搜索内容
        String selectedText = textArea.getSelectedText();
        if (selectedText != null && !selectedText.isEmpty() && !selectedText.contains("\n")) {
            searchField.setText(selectedText);
        }

        searchField.requestFocusInWindow();
        searchField.selectAll();
    }

    /**
     * 显示搜索替换面板（替换模式）
     */
    public void showReplace() {
        replacePanel.setVisible(true);
        toggleReplaceBtn.setSelected(true);
        toggleReplaceBtn.setIcon(IconUtil.createThemed(ICON_COLLAPSE, 16, 16));
        setVisible(true);

        // 如果有选中文本，将其作为搜索内容
        String selectedText = textArea.getSelectedText();
        if (selectedText != null && !selectedText.isEmpty() && !selectedText.contains("\n")) {
            searchField.setText(selectedText);
        }

        searchField.requestFocusInWindow();
        searchField.selectAll();
    }

    /**
     * 隐藏搜索面板
     */
    public void hidePanel() {
        setVisible(false);
        textArea.requestFocusInWindow();
    }

    /**
     * 查找下一个
     */
    private void findNext() {
        String searchText = searchField.getText();
        if (searchText.isEmpty()) {
            statusLabel.setText("");
            return;
        }

        SearchContext context = createSearchContext();
        context.setSearchForward(true);

        SearchResult result = SearchEngine.find(textArea, context);
        updateStatusFromResult(result);
    }

    /**
     * 查找上一个
     */
    private void findPrevious() {
        String searchText = searchField.getText();
        if (searchText.isEmpty()) {
            statusLabel.setText("");
            return;
        }

        SearchContext context = createSearchContext();
        context.setSearchForward(false);
        SearchResult result = SearchEngine.find(textArea, context);
        updateStatusFromResult(result);
    }

    /**
     * 替换当前匹配项
     */
    private void replace() {
        String searchText = searchField.getText();

        if (searchText.isEmpty()) {
            return;
        }

        SearchContext context = createSearchContext();
        context.setSearchForward(true);

        SearchResult result = SearchEngine.replace(textArea, context);
        if (result.wasFound()) {
            // 替换成功后自动查找下一个
            findNext();
        } else {
            statusLabel.setText(MSG_NO_RESULTS);
        }
    }

    /**
     * 替换所有匹配项
     */
    private void replaceAll() {
        String searchText = searchField.getText();

        if (searchText.isEmpty()) {
            return;
        }

        SearchContext context = createSearchContext();

        SearchResult result = SearchEngine.replaceAll(textArea, context);
        int count = result.getCount();

        // 显示替换结果
        if (count > 0) {
            statusLabel.setText(String.format(MSG_REPLACED_FORMAT, count));
            // 2秒后清除状态
            Timer timer = new Timer(2000, e -> updateSearchStatus());
            timer.setRepeats(false);
            timer.start();
        } else {
            statusLabel.setText(MSG_NO_RESULTS);
        }
    }

    /**
     * 创建搜索上下文
     */
    private SearchContext createSearchContext() {
        SearchContext context = new SearchContext();
        context.setSearchFor(searchField.getText());
        context.setReplaceWith(replaceField.getText());
        context.setMatchCase(searchField.isCaseSensitive());
        context.setRegularExpression(false);  // 不支持正则表达式
        context.setWholeWord(searchField.isWholeWord());
        return context;
    }

    /**
     * 根据搜索结果更新状态标签
     */
    private void updateStatusFromResult(SearchResult result) {
        if (result.wasFound()) {
            int totalCount = result.getCount();
            if (totalCount > 0) {
                // 计算当前是第几个匹配
                int currentIndex = getCurrentMatchIndex();
                statusLabel.setText(currentIndex + " of " + totalCount);
            } else {
                statusLabel.setText("");
            }
        } else {
            statusLabel.setText(MSG_NO_RESULTS);
        }
    }

    /**
     * 计算当前选中位置对应的匹配索引
     */
    private int calculateCurrentIndex(int selStart) {
        try {
            String searchText = searchField.getText();
            if (searchText.isEmpty()) {
                return 1;
            }

            // 从文档开头开始计数
            textArea.setCaretPosition(0);
            SearchContext context = createSearchContext();
            context.setSearchForward(true);

            int index = 0;

            while (true) {
                SearchResult tempResult = SearchEngine.find(textArea, context);
                if (!tempResult.wasFound()) {
                    break;
                }
                index++;

                int matchStart = tempResult.getMatchRange().getStartOffset();

                // 找到第一个起始位置 >= 当前选中位置的匹配项
                if (matchStart >= selStart) {
                    return index;
                }
            }

            // 如果所有匹配都在选中位置之前，返回总数
            return Math.max(1, index);
        } catch (Exception e) {
            log.warn("Failed to calculate current index", e);
            return 1;
        }
    }

    /**
     * 计算当前匹配项是第几个
     */
    private int getCurrentMatchIndex() {
        try {
            String searchText = searchField.getText();
            if (searchText.isEmpty()) {
                return 1;
            }

            // 获取当前选中的位置
            int currentSelStart = textArea.getSelectionStart();

            // 保存当前状态
            int savedCaret = textArea.getCaretPosition();

            int result = calculateCurrentIndex(currentSelStart);

            // 恢复光标
            textArea.setCaretPosition(savedCaret);
            textArea.setSelectionStart(currentSelStart);
            textArea.setSelectionEnd(textArea.getSelectionEnd());

            return result;
        } catch (Exception e) {
            log.warn("Failed to calculate match index", e);
            return 1;
        }
    }

    /**
     * 调度搜索状态更新（防抖）
     */
    private void scheduleSearchUpdate() {
        // 取消之前的定时器
        if (searchDebounceTimer != null && searchDebounceTimer.isRunning()) {
            searchDebounceTimer.stop();
        }

        // 创建新的防抖定时器，300ms 后执行
        searchDebounceTimer = new Timer(300, e -> updateSearchStatus());
        searchDebounceTimer.setRepeats(false);
        searchDebounceTimer.start();
    }

    /**
     * 更新搜索状态（用于实时更新）
     */
    private void updateSearchStatus() {
        String searchText = searchField.getText();
        if (searchText.isEmpty()) {
            statusLabel.setText("");
            return;
        }

        // 保存当前状态
        int savedCaret = textArea.getCaretPosition();
        int savedSelStart = textArea.getSelectionStart();
        int savedSelEnd = textArea.getSelectionEnd();

        try {
            // 从文档开头开始搜索以获取总数
            textArea.setCaretPosition(0);
            SearchContext context = createSearchContext();
            context.setSearchForward(true);

            SearchResult firstResult = SearchEngine.find(textArea, context);

            if (firstResult.wasFound()) {
                int totalCount = firstResult.getCount();
                // 计算当前匹配索引
                int currentIndex = calculateCurrentIndex(savedSelStart);
                statusLabel.setText(currentIndex + " of " + totalCount);
            } else {
                statusLabel.setText(MSG_NO_RESULTS);
            }
        } finally {
            // 恢复光标和选择
            textArea.setCaretPosition(savedCaret);
            textArea.setSelectionStart(savedSelStart);
            textArea.setSelectionEnd(savedSelEnd);
        }
    }


}
