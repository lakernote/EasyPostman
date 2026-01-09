package com.laker.postman.panel.sidebar;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.laker.postman.common.SingletonBasePanel;
import com.laker.postman.common.SingletonFactory;
import com.laker.postman.common.component.SearchTextField;
import com.laker.postman.common.constants.ModernColors;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class ConsolePanel extends SingletonBasePanel {
    private JTextPane consoleLogArea;
    private transient StyledDocument consoleDoc;
    private SearchTextField searchField;
    private JButton closeBtn;
    private JButton prevBtn;
    private JButton nextBtn;
    private JLabel matchCountLabel;
    private JToggleButton autoScrollBtn;
    private JComboBox<String> logLevelFilter;
    private boolean autoScroll = true;

    // 搜索相关
    private final transient List<Integer> matchPositions = new ArrayList<>();
    private int currentMatchIndex = -1;

    // 日志过滤相关
    private String currentFilter = "All";
    private final transient List<LogEntry> allLogs = new ArrayList<>();
    private static final int MAX_LOG_ENTRIES = 10000; // 最多保存10000条日志，防止内存溢出
    private static final int MAX_DISPLAY_LINES = 5000; // 最多显示5000行，超过则删除旧行，优化显示性能

    // 日志类型
    public enum LogType {
        INFO, ERROR, SUCCESS, WARN, DEBUG, TRACE, CUSTOM
    }

    // 日志条目类
    private static class LogEntry {
        final String message;
        final LogType type;

        LogEntry(String message, LogType type) {
            this.message = message;
            this.type = type;
        }
    }

    @Override
    protected void initUI() {
        setLayout(new BorderLayout());
        createConsolePanel();
    }

    @Override
    public void updateUI() {
        super.updateUI();
        updateConsoleColors();
    }

    /**
     * 更新控制台颜色以适配当前主题
     */
    private void updateConsoleColors() {
        if (consoleLogArea != null) {
            consoleLogArea.setBackground(ModernColors.getConsoleTextAreaBg());
            consoleLogArea.setForeground(ModernColors.getConsoleText());
            consoleLogArea.setCaretColor(ModernColors.getConsoleText());
            consoleLogArea.setSelectionColor(ModernColors.getConsoleSelectionBg());

            // 更新滚动面板背景
            Container parent = consoleLogArea.getParent();
            if (parent instanceof JViewport) {
                parent.setBackground(ModernColors.getConsoleTextAreaBg());
            }
        }

        // 更新工具栏颜色
        if (matchCountLabel != null) {
            matchCountLabel.setForeground(ModernColors.getTextSecondary());
        }

        // 刷新显示的日志以应用新的颜色方案
        if (consoleLogArea != null && !allLogs.isEmpty()) {
            refreshDisplayedLogs();
        }
    }

    @Override
    protected void registerListeners() {
        // 搜索框文本变化时重新查找
        searchField.addActionListener(e -> performSearch());

        // 实时搜索
        searchField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            @Override
            public void insertUpdate(javax.swing.event.DocumentEvent e) {
                performSearch();
            }

            @Override
            public void removeUpdate(javax.swing.event.DocumentEvent e) {
                performSearch();
            }

            @Override
            public void changedUpdate(javax.swing.event.DocumentEvent e) {
                performSearch();
            }
        });

        // 监听大小写敏感和整词匹配选项变化
        searchField.addPropertyChangeListener("caseSensitive", evt -> performSearch());
        searchField.addPropertyChangeListener("wholeWord", evt -> performSearch());

        nextBtn.addActionListener(e -> navigateToNextMatch());
        prevBtn.addActionListener(e -> navigateToPreviousMatch());

        // 自动滚动切换
        autoScrollBtn.addActionListener(e -> {
            autoScroll = autoScrollBtn.isSelected();
            if (autoScroll) {
                // 如果启用自动滚动，立即滚动到底部
                SwingUtilities.invokeLater(() -> {
                    try {
                        consoleLogArea.setCaretPosition(consoleDoc.getLength());
                    } catch (Exception ex) {
                        // ignore
                    }
                });
            }
        });

        // 日志级别过滤
        logLevelFilter.addActionListener(e -> {
            currentFilter = (String) logLevelFilter.getSelectedItem();
            refreshDisplayedLogs();
        });

        registerKeyboardShortcuts();
    }

    /**
     * 注册键盘快捷键
     */
    private void registerKeyboardShortcuts() {
        // 快捷键：Ctrl/Cmd+F 聚焦搜索框
        int modifier = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();
        getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
                KeyStroke.getKeyStroke(KeyEvent.VK_F, modifier), "focusSearch"
        );
        getActionMap().put("focusSearch", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                searchField.requestFocusInWindow();
                searchField.selectAll();
            }
        });

        // Esc 清除搜索
        searchField.getInputMap(JComponent.WHEN_FOCUSED).put(
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "clearSearch"
        );
        searchField.getActionMap().put("clearSearch", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                clearSearch();
            }
        });
    }

    /**
     * 清除搜索
     */
    private void clearSearch() {
        searchField.setText("");
        consoleLogArea.getHighlighter().removeAllHighlights();
        matchPositions.clear();
        currentMatchIndex = -1;
        updateMatchCounter();
    }

    /**
     * 执行搜索并查找所有匹配项（支持大小写敏感和整词匹配）
     */
    private void performSearch() {
        String keyword = searchField.getText();
        matchPositions.clear();
        currentMatchIndex = -1;
        consoleLogArea.getHighlighter().removeAllHighlights();

        if (keyword.isEmpty()) {
            updateMatchCounter();
            return;
        }

        String text = consoleLogArea.getText();
        boolean caseSensitive = searchField.isCaseSensitive();
        boolean wholeWord = searchField.isWholeWord();

        // 准备搜索用的文本和关键词
        String searchText = caseSensitive ? text : text.toLowerCase();
        String searchKeyword = caseSensitive ? keyword : keyword.toLowerCase();

        int pos = 0;
        while ((pos = searchText.indexOf(searchKeyword, pos)) != -1) {
            // 如果启用整词匹配，需要检查边界
            if (wholeWord) {
                boolean isWordStart = pos == 0 || !Character.isLetterOrDigit(text.charAt(pos - 1));
                boolean isWordEnd = (pos + searchKeyword.length() >= text.length()) ||
                        !Character.isLetterOrDigit(text.charAt(pos + searchKeyword.length()));

                if (isWordStart && isWordEnd) {
                    matchPositions.add(pos);
                }
            } else {
                matchPositions.add(pos);
            }
            pos += searchKeyword.length();
        }

        if (!matchPositions.isEmpty()) {
            currentMatchIndex = 0;
            highlightAllMatches(keyword.length());
            highlightCurrentMatch(keyword.length());
        }

        updateMatchCounter();
    }

    /**
     * 导航到下一个匹配项
     */
    private void navigateToNextMatch() {
        if (matchPositions.isEmpty()) return;

        currentMatchIndex = (currentMatchIndex + 1) % matchPositions.size();
        highlightCurrentMatch(searchField.getText().length());
        updateMatchCounter();
    }

    /**
     * 导航到上一个匹配项
     */
    private void navigateToPreviousMatch() {
        if (matchPositions.isEmpty()) return;

        currentMatchIndex = (currentMatchIndex - 1 + matchPositions.size()) % matchPositions.size();
        highlightCurrentMatch(searchField.getText().length());
        updateMatchCounter();
    }

    /**
     * 高亮所有匹配项
     */
    private void highlightAllMatches(int length) {
        try {
            // 使用柔和的黄色高亮所有匹配项（浅色背景）
            DefaultHighlighter.DefaultHighlightPainter painter =
                    new DefaultHighlighter.DefaultHighlightPainter(new Color(254, 249, 195));

            for (int pos : matchPositions) {
                consoleLogArea.getHighlighter().addHighlight(pos, pos + length, painter);
            }
        } catch (BadLocationException ex) {
            log.debug("Highlight failed", ex);
        }
    }

    /**
     * 高亮当前匹配项
     */
    private void highlightCurrentMatch(int length) {
        if (currentMatchIndex < 0 || currentMatchIndex >= matchPositions.size()) return;

        try {
            int pos = matchPositions.get(currentMatchIndex);

            // 重新高亮所有匹配项
            consoleLogArea.getHighlighter().removeAllHighlights();
            highlightAllMatches(length);

            // 当前匹配项使用明亮橙色高亮
            DefaultHighlighter.DefaultHighlightPainter currentPainter =
                    new DefaultHighlighter.DefaultHighlightPainter(new Color(254, 215, 170));
            consoleLogArea.getHighlighter().addHighlight(pos, pos + length, currentPainter);

            // 滚动到当前匹配项
            consoleLogArea.setCaretPosition(pos);
        } catch (BadLocationException ex) {
            log.debug("Highlight current match failed", ex);
        }
    }

    /**
     * 更新匹配计数器
     */
    private void updateMatchCounter() {
        if (matchPositions.isEmpty()) {
            matchCountLabel.setText("");
        } else {
            matchCountLabel.setText(String.format("%d/%d", currentMatchIndex + 1, matchPositions.size()));
        }
    }

    private void createConsolePanel() {
        consoleLogArea = new JTextPane();
        consoleLogArea.setEditable(false);
        consoleLogArea.setFocusable(true);
        consoleLogArea.setCursor(Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR));
        // 使用 ModernColors 定义的专业控制台背景色（主题适配）
        consoleLogArea.setBackground(ModernColors.getConsoleTextAreaBg());
        consoleLogArea.setForeground(ModernColors.getConsoleText());
        consoleLogArea.setCaretColor(ModernColors.getConsoleText());
        consoleLogArea.setSelectionColor(ModernColors.getConsoleSelectionBg());
        consoleDoc = consoleLogArea.getStyledDocument();
        JScrollPane logScroll = new JScrollPane(consoleLogArea);
        logScroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        logScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        // 使用 ModernColors 定义的背景色（主题适配）
        logScroll.getViewport().setBackground(ModernColors.getConsoleTextAreaBg());

        // 顶部工具栏
        JPanel topPanel = new JPanel(new BorderLayout(5, 0));
        topPanel.setBorder(BorderFactory.createEmptyBorder(4, 6, 4, 6));
        topPanel.setOpaque(true);
        // 使用 ModernColors 定义的工具栏背景色和边框（主题适配）
        topPanel.setBackground(ModernColors.getConsoleToolbarBg());
        topPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, ModernColors.getConsoleToolbarBorder()),
                BorderFactory.createEmptyBorder(4, 6, 4, 6)
        ));

        // 中间：搜索栏
        JPanel centerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 3, 0));
        centerPanel.setOpaque(false);

        logLevelFilter = new JComboBox<>(new String[]{"All", "INFO", "ERROR", "WARN", "DEBUG"});
        logLevelFilter.setPreferredSize(new Dimension(90, 28));
        logLevelFilter.setFocusable(false);
        logLevelFilter.setToolTipText("Filter logs by level");

        searchField = new SearchTextField();
        searchField.setPreferredSize(new Dimension(200, 28));

        prevBtn = new JButton();
        FlatSVGIcon prevIcon = new FlatSVGIcon("icons/arrow-up.svg", 16, 16);
        prevIcon.setColorFilter(new FlatSVGIcon.ColorFilter(color -> UIManager.getColor("Button.foreground")));
        prevBtn.setIcon(prevIcon);
        prevBtn.setToolTipText("Previous match (Shift+Enter)");
        prevBtn.setFocusable(false);
        prevBtn.setPreferredSize(new Dimension(28, 28));

        nextBtn = new JButton();
        FlatSVGIcon nextIcon = new FlatSVGIcon("icons/arrow-down.svg", 16, 16);
        nextIcon.setColorFilter(new FlatSVGIcon.ColorFilter(color -> UIManager.getColor("Button.foreground")));
        nextBtn.setIcon(nextIcon);
        nextBtn.setToolTipText("Next match (Enter)");
        nextBtn.setFocusable(false);
        nextBtn.setPreferredSize(new Dimension(28, 28));

        matchCountLabel = new JLabel("");
        matchCountLabel.setForeground(ModernColors.getTextSecondary());
        matchCountLabel.setPreferredSize(new Dimension(50, 28));
        matchCountLabel.setHorizontalAlignment(SwingConstants.CENTER);

        centerPanel.add(logLevelFilter);
        centerPanel.add(searchField);
        centerPanel.add(prevBtn);
        centerPanel.add(nextBtn);
        centerPanel.add(matchCountLabel);

        // 右侧：工具按钮
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 3, 0));
        rightPanel.setOpaque(false);

        autoScrollBtn = new JToggleButton();
        FlatSVGIcon autoScrollIcon = new FlatSVGIcon("icons/auto-scroll.svg", 16, 16);
        autoScrollIcon.setColorFilter(new FlatSVGIcon.ColorFilter(color -> UIManager.getColor("Button.foreground")));
        autoScrollBtn.setIcon(autoScrollIcon);
        autoScrollBtn.setSelected(true);
        autoScrollBtn.setToolTipText("Auto-scroll to bottom");
        autoScrollBtn.setFocusable(false);
        autoScrollBtn.setPreferredSize(new Dimension(28, 28));

        JButton clearBtn = getClearBtn();
        clearBtn.setPreferredSize(new Dimension(28, 28));

        closeBtn = new JButton();
        FlatSVGIcon closeIcon = new FlatSVGIcon("icons/close.svg", 16, 16);
        closeIcon.setColorFilter(new FlatSVGIcon.ColorFilter(color -> UIManager.getColor("Button.foreground")));
        closeBtn.setIcon(closeIcon);
        closeBtn.setToolTipText("Hide console");
        closeBtn.setFocusable(false);
        closeBtn.setPreferredSize(new Dimension(28, 28));

        rightPanel.add(autoScrollBtn);
        rightPanel.add(clearBtn);
        rightPanel.add(closeBtn);

        topPanel.add(centerPanel, BorderLayout.CENTER);
        topPanel.add(rightPanel, BorderLayout.EAST);

        add(topPanel, BorderLayout.NORTH);
        add(logScroll, BorderLayout.CENTER);
    }

    private JButton getClearBtn() {
        JButton clearBtn = new JButton();
        FlatSVGIcon clearIcon = new FlatSVGIcon("icons/clear.svg", 16, 16);
        clearIcon.setColorFilter(new FlatSVGIcon.ColorFilter(color -> UIManager.getColor("Button.foreground")));
        clearBtn.setIcon(clearIcon);
        clearBtn.setToolTipText("Clear console");
        clearBtn.setFocusable(false);
        clearBtn.addActionListener(e -> clearConsole());
        return clearBtn;
    }

    /**
     * 清空控制台
     */
    private void clearConsole() {
        try {
            consoleDoc.remove(0, consoleDoc.getLength());
            allLogs.clear(); // 清除所有日志记录
            matchPositions.clear();
            currentMatchIndex = -1;
            updateMatchCounter();
        } catch (BadLocationException ex) {
            log.debug("Clear console failed", ex);
        }
    }

    public synchronized void appendConsoleLog(String msg) {
        appendConsoleLog(msg, LogType.INFO);
    }

    public synchronized void appendConsoleLog(String msg, LogType type) {
        // 添加到所有日志列表
        allLogs.add(new LogEntry(msg, type));

        // 如果超过最大数量，删除最老的日志（FIFO）
        if (allLogs.size() > MAX_LOG_ENTRIES) {
            allLogs.remove(0);
        }

        SwingUtilities.invokeLater(() -> {
            // 检查是否应该显示此日志
            if (!shouldDisplayLog(type)) {
                return;
            }

            Style style = consoleLogArea.addStyle("logStyle", null);
            applyLogStyle(style, type);

            try {
                // 保存当前的光标位置
                int currentCaretPosition = consoleLogArea.getCaretPosition();

                consoleDoc.insertString(consoleDoc.getLength(), msg + "\n", style);

                // 限制显示的行数，避免性能问题
                limitDisplayLines();

                // 根据自动滚动设置决定光标位置
                if (autoScroll) {
                    // 滚动到底部
                    consoleLogArea.setCaretPosition(consoleDoc.getLength());
                } else {
                    // 保持用户当前的浏览位置，不自动滚动
                    try {
                        // 如果原位置仍然有效，恢复到原位置
                        if (currentCaretPosition <= consoleDoc.getLength()) {
                            consoleLogArea.setCaretPosition(currentCaretPosition);
                        }
                    } catch (Exception ex) {
                        // 如果恢复位置失败，不做处理
                    }
                }

                // 如果正在搜索，更新搜索结果
                if (!searchField.getText().isEmpty()) {
                    performSearch();
                }
            } catch (BadLocationException e) {
                log.debug("Append console log failed", e);
            }
        });
    }

    /**
     * 限制显示的行数，删除最老的行
     */
    private void limitDisplayLines() {
        try {
            Element root = consoleDoc.getDefaultRootElement();
            int lineCount = root.getElementCount();

            // 如果超过最大显示行数，删除最老的行
            if (lineCount > MAX_DISPLAY_LINES) {
                int linesToRemove = lineCount - MAX_DISPLAY_LINES;
                Element firstLine = root.getElement(linesToRemove - 1);
                int endOffset = firstLine.getEndOffset();
                consoleDoc.remove(0, endOffset);
            }
        } catch (BadLocationException e) {
            log.debug("Limit display lines failed", e);
        }
    }

    /**
     * 判断是否应该显示此日志
     */
    private boolean shouldDisplayLog(LogType type) {
        if ("All".equals(currentFilter)) {
            return true;
        }
        return type.name().equals(currentFilter);
    }

    /**
     * 刷新显示的日志（过滤后）
     */
    private void refreshDisplayedLogs() {
        SwingUtilities.invokeLater(() -> {
            try {
                // 清空当前显示
                consoleDoc.remove(0, consoleDoc.getLength());

                // 重新添加符合过滤条件的日志
                for (LogEntry entry : allLogs) {
                    if (shouldDisplayLog(entry.type)) {
                        Style style = consoleLogArea.addStyle("logStyle", null);
                        applyLogStyle(style, entry.type);
                        consoleDoc.insertString(consoleDoc.getLength(), entry.message + "\n", style);
                    }
                }

                // 滚动到底部
                if (autoScroll) {
                    consoleLogArea.setCaretPosition(consoleDoc.getLength());
                }

                // 更新搜索结果
                if (!searchField.getText().isEmpty()) {
                    performSearch();
                }
            } catch (BadLocationException e) {
                log.debug("Refresh logs failed", e);
            }
        });
    }

    /**
     * 应用日志样式 - 使用 ModernColors 统一配色
     */
    private void applyLogStyle(Style style, LogType type) {
        // 为所有日志类型设置更大的行间距，提升可读性
        StyleConstants.setLineSpacing(style, 0.2f);

        switch (type) {
            case ERROR:
                // 使用 ModernColors 定义的错误色（主题适配）
                StyleConstants.setForeground(style, ModernColors.getConsoleError());
                StyleConstants.setBold(style, true);
                break;
            case SUCCESS:
                // 使用 ModernColors 定义的调试色（绿色，主题适配）
                StyleConstants.setForeground(style, ModernColors.getConsoleDebug());
                StyleConstants.setBold(style, true);
                break;
            case WARN:
                // 使用 ModernColors 定义的警告色（主题适配）
                StyleConstants.setForeground(style, ModernColors.getConsoleWarn());
                StyleConstants.setBold(style, true);
                break;
            case DEBUG:
                // 使用 ModernColors 定义的 INFO 色（蓝色，主题适配）
                StyleConstants.setForeground(style, ModernColors.getConsoleInfo());
                StyleConstants.setBold(style, false);
                break;
            case TRACE:
                // 使用 ModernColors 定义的类名色（紫色，主题适配）
                StyleConstants.setForeground(style, ModernColors.getConsoleClassName());
                StyleConstants.setBold(style, false);
                break;
            case CUSTOM:
                // 使用 ModernColors 定义的方法名色（青色，主题适配）
                StyleConstants.setForeground(style, ModernColors.getConsoleMethodName());
                StyleConstants.setBold(style, false);
                break;
            case INFO:
            default:
                // 使用 ModernColors 定义的普通文本色（主题适配）
                StyleConstants.setForeground(style, ModernColors.getConsoleText());
                StyleConstants.setBold(style, false);
        }
    }

    // 静态代理方法，便于外部调用
    public static void appendLog(String msg) {
        SingletonFactory.getInstance(ConsolePanel.class).appendConsoleLog(msg);
    }

    public static void appendLog(String msg, LogType type) {
        SingletonFactory.getInstance(ConsolePanel.class).appendConsoleLog(msg, type);
    }

    public void setCloseAction(ActionListener listener) {
        closeBtn.addActionListener(listener);
    }
}
