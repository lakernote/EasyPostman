package com.laker.postman.common.component;

import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.extras.components.FlatTextField;
import com.laker.postman.common.constants.ModernColors;
import com.laker.postman.util.FontsUtil;
import com.laker.postman.util.IconUtil;
import com.laker.postman.util.UiI18n;
import com.laker.postman.util.UiMessageKeys;
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
import java.awt.geom.RoundRectangle2D;

@Slf4j
public class SearchReplacePanel extends JPanel {

    // 图标路径常量 - chevron-right 表示替换面板已收起，chevron-down 表示替换面板已展开
    private static final String ICON_EXPAND = "icons/chevron-right.svg";
    private static final String ICON_COLLAPSE = "icons/chevron-down.svg";

    private static final int CORNER_RADIUS = 8;
    private static final int FIELD_WIDTH = 180;
    private static final int FIELD_MAX_WIDTH = 200;
    private static final int FIELD_HEIGHT = SearchTextField.DEFAULT_HEIGHT;
    private static final int TOOL_BUTTON_SIZE = 22;
    private static final int STATUS_WIDTH = 82;
    private static final int STATUS_MAX_WIDTH = 96;
    private static final int ROW_GAP = 4;
    private static final int GROUP_GAP = 6;
    private static final int PANEL_HORIZONTAL_PADDING = 7;
    private static final int PANEL_VERTICAL_PADDING = 5;
    private static final String TOOL_BUTTON_STYLE = "arc: 6; margin: 0,0,0,0";
    private static final String TEXT_FIELD_STYLE = "arc: 8; margin: 4,8,4,8";

    private final RSyntaxTextArea textArea;
    private final SearchTextField searchField;
    private final FlatTextField replaceField;
    private final JButton toggleReplaceBtn;
    private final JPanel replacePanel;
    private final Component replaceGap;
    private final JLabel statusLabel;  // 搜索结果状态标签
    private final boolean enableReplace;  // 是否启用替换功能

    // 防抖Timer，避免输入时频繁搜索
    private Timer searchDebounceTimer;

    /**
     * 创建搜索替换面板（默认启用替换功能）
     */
    public SearchReplacePanel(RSyntaxTextArea textArea) {
        this(textArea, true);
    }

    /**
     * 创建搜索替换面板
     *
     * @param textArea      目标文本区域
     * @param enableReplace 是否启用替换功能
     */
    public SearchReplacePanel(RSyntaxTextArea textArea, boolean enableReplace) {
        this.textArea = textArea;
        this.enableReplace = enableReplace;

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(new EmptyBorder(
                PANEL_VERTICAL_PADDING,
                PANEL_HORIZONTAL_PADDING,
                PANEL_VERTICAL_PADDING,
                PANEL_HORIZONTAL_PADDING
        ));
        // 保持透明，背景和边框由 paintComponent 绘制。
        setOpaque(false);
        // 搜索面板
        JPanel searchPanel = new JPanel();
        searchPanel.setLayout(new BoxLayout(searchPanel, BoxLayout.X_AXIS));
        searchPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        searchPanel.setOpaque(false);  // 透明以显示父面板的圆角背景

        // 展开/收起替换面板的按钮（放在最左边，类似 Postman）
        toggleReplaceBtn = new JButton(IconUtil.createThemed(ICON_EXPAND, 16, 16));
        toggleReplaceBtn.setToolTipText(UiI18n.get(UiMessageKeys.SEARCH_TOGGLE_REPLACE));
        toggleReplaceBtn.setFocusable(false);
        setFixedSize(toggleReplaceBtn, TOOL_BUTTON_SIZE, TOOL_BUTTON_SIZE);
        toggleReplaceBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        toggleReplaceBtn.setVisible(enableReplace);
        toggleReplaceBtn.putClientProperty(FlatClientProperties.BUTTON_TYPE, FlatClientProperties.BUTTON_TYPE_TOOLBAR_BUTTON);
        toggleReplaceBtn.putClientProperty(FlatClientProperties.STYLE, TOOL_BUTTON_STYLE);
        Component toggleGap = Box.createHorizontalStrut(ROW_GAP);
        toggleGap.setVisible(enableReplace);

        // 搜索输入框 - 使用 SearchTextField 复用大小写敏感和整词匹配功能
        searchField = new SearchTextField();
        configureOverlayTextField(searchField);
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
                // 如果搜索框被清空，立即清除文本区域的选中内容和状态
                if (searchField.getText().isEmpty()) {
                    // 取消之前的定时器
                    if (searchDebounceTimer != null && searchDebounceTimer.isRunning()) {
                        searchDebounceTimer.stop();
                    }
                    clearSearchHighlights();
                    statusLabel.setText("");
                    searchField.putClientProperty(FlatClientProperties.OUTLINE, null);
                } else {
                    scheduleSearchUpdate();
                }
            }

            @Override
            public void changedUpdate(javax.swing.event.DocumentEvent e) {
                scheduleSearchUpdate();
            }
        });

        // 查找按钮
        JButton findPrevBtn = createIconButton("icons/arrow-up.svg", UiI18n.get(UiMessageKeys.SEARCH_PREVIOUS), e -> findPrevious());
        JButton findNextBtn = createIconButton("icons/arrow-down.svg", UiI18n.get(UiMessageKeys.SEARCH_NEXT), e -> findNext());


        // 状态标签
        statusLabel = new JLabel("");
        statusLabel.setFont(FontsUtil.getDefaultFontWithOffset(Font.PLAIN, -2));
        statusLabel.setForeground(ModernColors.getTextHint());
        statusLabel.setPreferredSize(new Dimension(STATUS_WIDTH, TOOL_BUTTON_SIZE));
        statusLabel.setMaximumSize(new Dimension(STATUS_MAX_WIDTH, TOOL_BUTTON_SIZE));
        statusLabel.setHorizontalAlignment(SwingConstants.CENTER);

        // 关闭按钮
        JButton closeBtn = createIconButton("icons/x.svg", UiI18n.get(UiMessageKeys.SEARCH_CLOSE), e -> hidePanel());


        // 组装搜索面板
        searchPanel.add(toggleReplaceBtn);
        searchPanel.add(toggleGap);
        searchPanel.add(searchField);
        searchPanel.add(Box.createHorizontalStrut(ROW_GAP));
        searchPanel.add(findPrevBtn);
        searchPanel.add(findNextBtn);
        searchPanel.add(Box.createHorizontalStrut(GROUP_GAP));
        searchPanel.add(statusLabel);
        searchPanel.add(Box.createHorizontalStrut(ROW_GAP));
        searchPanel.add(closeBtn);
        searchPanel.add(Box.createHorizontalGlue());

        add(searchPanel);

        // 替换面板（默认隐藏）
        replacePanel = new JPanel();
        replacePanel.setLayout(new BoxLayout(replacePanel, BoxLayout.X_AXIS));
        replacePanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        replacePanel.setVisible(false);
        replacePanel.setOpaque(false);  // 透明以显示父面板的圆角背景

        // 左侧占位符，保持与搜索框对齐（切换按钮宽度 + 水平间距）
        int spacerWidth = TOOL_BUTTON_SIZE + ROW_GAP;
        JPanel spacer = new JPanel();
        spacer.setOpaque(false);
        setFixedSize(spacer, spacerWidth, TOOL_BUTTON_SIZE);

        // 替换输入框
        replaceField = new FlatTextField();
        replaceField.setPlaceholderText(UiI18n.get(UiMessageKeys.SEARCH_REPLACE_PLACEHOLDER));
        configureOverlayTextField(replaceField);
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
        JButton replaceBtn = createIconButton("icons/replace.svg", UiI18n.get(UiMessageKeys.SEARCH_REPLACE), e -> replace());
        JButton replaceAllBtn = createIconButton("icons/replace-all.svg", UiI18n.get(UiMessageKeys.SEARCH_REPLACE_ALL), e -> replaceAll());

        // 组装替换面板
        replacePanel.add(spacer);  // 左侧占位符（已包含按钮宽度+间距）
        replacePanel.add(replaceField);
        replacePanel.add(Box.createHorizontalStrut(ROW_GAP));
        replacePanel.add(replaceBtn);
        replacePanel.add(Box.createHorizontalStrut(ROW_GAP));
        replacePanel.add(replaceAllBtn);
        replacePanel.add(Box.createHorizontalGlue());

        replaceGap = Box.createVerticalStrut(ROW_GAP);
        replaceGap.setVisible(false);
        add(replaceGap);
        add(replacePanel);

        // 设置 toggleReplaceBtn 的 actionListener（现在 replacePanel 已经初始化）
        toggleReplaceBtn.addActionListener(e -> {
            setReplaceVisible(!replacePanel.isVisible());
        });

        // 默认隐藏
        setVisible(false);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();

        // 启用抗锯齿以获得平滑的圆角
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int width = getWidth();
        int height = getHeight();

        // 绘制圆角背景
        g2.setColor(ModernColors.getCardBackgroundColor());
        g2.fill(new RoundRectangle2D.Float(
                0,
                0,
                width - 1.0f,
                height - 1.0f,
                CORNER_RADIUS,
                CORNER_RADIUS
        ));

        // 绘制细微的边框
        g2.setColor(ModernColors.getBorderLightColor());
        g2.draw(new RoundRectangle2D.Float(
                0,
                0,
                width - 1.0f,
                height - 1.0f,
                CORNER_RADIUS,
                CORNER_RADIUS
        ));

        g2.dispose();
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension size = super.getPreferredSize();
        int rowHeight = replacePanel.isVisible()
                ? FIELD_HEIGHT + ROW_GAP + FIELD_HEIGHT
                : FIELD_HEIGHT;
        int height = rowHeight + PANEL_VERTICAL_PADDING * 2;
        return new Dimension(size.width, Math.max(size.height, height));
    }

    /**
     * 创建图标按钮 - 现代化扁平设计
     */
    private JButton createIconButton(String iconPath, String tooltip, java.awt.event.ActionListener listener) {
        JButton btn = new JButton(IconUtil.createThemed(iconPath, 16, 16));
        btn.setToolTipText(tooltip);
        setFixedSize(btn, TOOL_BUTTON_SIZE, TOOL_BUTTON_SIZE);
        btn.setFocusable(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.putClientProperty(FlatClientProperties.BUTTON_TYPE, FlatClientProperties.BUTTON_TYPE_TOOLBAR_BUTTON);
        btn.putClientProperty(FlatClientProperties.STYLE, TOOL_BUTTON_STYLE);
        if (listener != null) {
            btn.addActionListener(listener);
        }
        return btn;
    }

    private static void configureOverlayTextField(JComponent field) {
        field.setPreferredSize(new Dimension(FIELD_WIDTH, FIELD_HEIGHT));
        field.setMaximumSize(new Dimension(FIELD_MAX_WIDTH, FIELD_HEIGHT));
        field.setMinimumSize(new Dimension(FIELD_WIDTH, FIELD_HEIGHT));
        field.putClientProperty(FlatClientProperties.STYLE, TEXT_FIELD_STYLE);
    }

    private static void setFixedSize(Component component, int width, int height) {
        Dimension size = new Dimension(width, height);
        component.setPreferredSize(size);
        component.setMinimumSize(size);
        component.setMaximumSize(size);
    }

    private void setReplaceVisible(boolean visible) {
        replacePanel.setVisible(visible);
        replaceGap.setVisible(visible);
        toggleReplaceBtn.setIcon(IconUtil.createThemed(visible ? ICON_COLLAPSE : ICON_EXPAND, 16, 16));

        invalidate();
        revalidate();
        Container parent = getParent();
        if (parent != null) {
            parent.revalidate();
            parent.repaint();
        }
        Rectangle bounds = getBounds();
        setBounds(bounds.x, bounds.y, bounds.width, getPreferredSize().height);
    }


    /**
     * 显示搜索面板（仅搜索模式）
     */
    public void showSearch() {
        setReplaceVisible(false);
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
        if (!enableReplace) {
            // 如果未启用替换功能，则只显示搜索
            showSearch();
            return;
        }
        setReplaceVisible(true);
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
        // 清除搜索高亮和选中状态
        clearSearchHighlights();
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
            clearSearchHighlights();
            return;
        }

        SearchContext context = createSearchContext();
        context.setSearchForward(true);

        SearchResult result = SearchEngine.find(textArea, context);
        updateStatusFromResult(result);

        // 确保文本区域获得焦点以显示选中效果
        textArea.requestFocusInWindow();
    }

    /**
     * 查找上一个
     */
    private void findPrevious() {
        String searchText = searchField.getText();
        if (searchText.isEmpty()) {
            statusLabel.setText("");
            clearSearchHighlights();
            return;
        }

        SearchContext context = createSearchContext();
        context.setSearchForward(false);
        SearchResult result = SearchEngine.find(textArea, context);
        updateStatusFromResult(result);

        // 确保文本区域获得焦点以显示选中效果
        textArea.requestFocusInWindow();
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
            statusLabel.setText(noResultsText());
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
            statusLabel.setText(UiI18n.get(UiMessageKeys.SEARCH_REPLACED_COUNT, count));
            // 2秒后清除状态
            Timer timer = new Timer(2000, e -> updateSearchStatus());
            timer.setRepeats(false);
            timer.start();
        } else {
            statusLabel.setText(noResultsText());
        }
    }

    /**
     * 创建搜索上下文。禁用 markAll，避免搜索/替换时触发全量高亮扫描。
     */
    private SearchContext createSearchContext() {
        SearchContext context = new SearchContext();
        context.setSearchFor(searchField.getText());
        context.setReplaceWith(replaceField.getText());
        context.setMatchCase(searchField.isCaseSensitive());
        context.setRegularExpression(false);  // 不支持正则表达式
        context.setWholeWord(searchField.isWholeWord());
        context.setMarkAll(false);
        return context;
    }

    /**
     * 根据搜索结果更新状态标签，并通过 outline 给搜索框着色反馈
     */
    private void updateStatusFromResult(SearchResult result) {
        if (result.wasFound()) {
            // 计算总匹配数和当前索引
            int totalCount = calculateTotalMatches();
            if (totalCount > 0) {
                // 计算当前是第几个匹配
                int currentIndex = getCurrentMatchIndex(totalCount);
                statusLabel.setText(UiI18n.get(UiMessageKeys.SEARCH_STATUS_COUNT, currentIndex, totalCount));
            } else {
                statusLabel.setText("");
            }
            // 有结果：清除红色边框
            searchField.putClientProperty(FlatClientProperties.OUTLINE, null);
        } else {
            statusLabel.setText(noResultsText());
            // 无结果且搜索框非空：显示红色边框（同 IDEA 行为）
            if (!searchField.getText().isEmpty()) {
                searchField.putClientProperty(FlatClientProperties.OUTLINE, FlatClientProperties.OUTLINE_ERROR);
            }
        }
    }

    /**
     * 计算文档中的总匹配数
     */
    private int calculateTotalMatches() {
        try {
            String searchText = searchField.getText();
            if (searchText.isEmpty()) {
                return 0;
            }

            return countMatches(textArea.getText(), searchText);
        } catch (Exception e) {
            log.warn("Failed to calculate total matches", e);
            return 0;
        }
    }

    private int countMatches(String text, String searchText) {
        if (searchText.isEmpty()) {
            return 0;
        }

        int count = 0;
        int searchLength = searchText.length();
        int matchStart = findNextMatch(text, searchText, 0);
        while (matchStart >= 0) {
            count++;
            matchStart = findNextMatch(text, searchText, matchStart + searchLength);
        }
        return count;
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

            String text = textArea.getText();
            int searchLength = searchText.length();
            int index = 0;
            int matchStart = findNextMatch(text, searchText, 0);

            while (matchStart >= 0) {
                index++;

                // 找到第一个起始位置 >= 当前选中位置的匹配项
                if (matchStart >= selStart) {
                    return index;
                }

                matchStart = findNextMatch(text, searchText, matchStart + searchLength);
            }

            // 如果所有匹配都在选中位置之前，返回总数
            return Math.max(1, index);
        } catch (Exception e) {
            log.warn("Failed to calculate current index", e);
            return 1;
        }
    }

    private int findNextMatch(String text, String searchText, int fromIndex) {
        int searchLength = searchText.length();
        int lastStart = text.length() - searchLength;
        if (searchLength == 0 || fromIndex > lastStart) {
            return -1;
        }

        boolean matchCase = searchField.isCaseSensitive();
        boolean wholeWord = searchField.isWholeWord();
        char firstChar = searchText.charAt(0);

        for (int index = Math.max(0, fromIndex); index <= lastStart; index++) {
            if (!charsEqual(text.charAt(index), firstChar, matchCase)) {
                continue;
            }
            if (!text.regionMatches(!matchCase, index, searchText, 0, searchLength)) {
                continue;
            }
            if (!wholeWord || isWholeWord(text, index, searchLength)) {
                return index;
            }
        }
        return -1;
    }

    private boolean charsEqual(char left, char right, boolean matchCase) {
        return matchCase
                ? left == right
                : (Character.toLowerCase(left) == Character.toLowerCase(right)
                || Character.toUpperCase(left) == Character.toUpperCase(right));
    }

    private boolean isWholeWord(String text, int offset, int length) {
        boolean wordBoundaryBefore = offset == 0 || !Character.isLetterOrDigit(text.charAt(offset - 1));
        int afterOffset = offset + length;
        boolean wordBoundaryAfter = afterOffset >= text.length()
                || !Character.isLetterOrDigit(text.charAt(afterOffset));
        return wordBoundaryBefore && wordBoundaryAfter;
    }

    /**
     * 计算当前匹配项是第几个
     */
    private int getCurrentMatchIndex(int totalCount) {
        try {
            String searchText = searchField.getText();
            if (searchText.isEmpty() || totalCount == 0) {
                return 1;
            }

            // 获取当前选中的位置
            int currentSelStart = textArea.getSelectionStart();

            return calculateCurrentIndex(currentSelStart);
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
            searchField.putClientProperty(FlatClientProperties.OUTLINE, null);
            return;
        }

        // 保存当前状态
        int savedCaret = textArea.getCaretPosition();
        int savedSelStart = textArea.getSelectionStart();
        int savedSelEnd = textArea.getSelectionEnd();

        try {
            // 计算总匹配数
            int totalCount = calculateTotalMatches();

            if (totalCount > 0) {
                // 输入搜索文本时，从文档开头开始搜索并高亮第一个匹配
                textArea.setCaretPosition(0);
                SearchContext context = createSearchContext();
                context.setSearchForward(true);
                SearchResult firstResult = SearchEngine.find(textArea, context);

                if (firstResult.wasFound()) {
                    // 显示当前匹配位置和总匹配数。
                    statusLabel.setText(UiI18n.get(UiMessageKeys.SEARCH_STATUS_COUNT, 1, totalCount));
                    searchField.putClientProperty(FlatClientProperties.OUTLINE, null);
                } else {
                    statusLabel.setText(noResultsText());
                    searchField.putClientProperty(FlatClientProperties.OUTLINE, FlatClientProperties.OUTLINE_ERROR);
                }
            } else {
                statusLabel.setText(noResultsText());
                searchField.putClientProperty(FlatClientProperties.OUTLINE, FlatClientProperties.OUTLINE_ERROR);
            }
        } finally {
            // 不恢复光标，保持在第一个匹配位置
            // 但如果没有找到，恢复原来的位置
            if (statusLabel.getText().equals(noResultsText())) {
                textArea.setCaretPosition(savedCaret);
                textArea.setSelectionStart(savedSelStart);
                textArea.setSelectionEnd(savedSelEnd);
            }
        }
    }

    private static String noResultsText() {
        return UiI18n.get(UiMessageKeys.SEARCH_NO_RESULTS);
    }

    /**
     * 清除文本区域的选中内容
     */
    private void clearTextSelection() {
        if (textArea != null) {
            textArea.setSelectionStart(textArea.getCaretPosition());
            textArea.setSelectionEnd(textArea.getCaretPosition());
        }
    }

    /**
     * 清除搜索高亮和选中状态
     */
    private void clearSearchHighlights() {
        if (textArea != null) {
            // 清除选中状态
            clearTextSelection();
            // 使 SearchEngine 清除之前的搜索状态
            // 通过执行一次空搜索来重置状态
            SearchContext context = new SearchContext();
            context.setSearchFor("");
            context.setMarkAll(false);
            SearchEngine.find(textArea, context);
        }
    }
}
