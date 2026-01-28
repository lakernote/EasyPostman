package com.laker.postman.common.component;

import com.laker.postman.common.constants.ModernColors;
import com.laker.postman.util.FontsUtil;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.StyleSheet;
import javax.swing.undo.UndoManager;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * 企业级 Markdown 编辑器组件
 * 功能特性：
 * - 左右分栏编辑预览
 * - 丰富的工具栏和快捷键
 * - 行号显示
 * - 撤销/重做
 * - 查找替换
 * - 全屏模式
 * - 导出功能
 * - 完整的 Markdown 语法支持
 */
public class MarkdownEditorPanel extends JPanel {
    private JTextArea editorArea;
    private JTextArea lineNumberArea;
    private JEditorPane previewPane;
    private JSplitPane splitPane;
    private JToggleButton previewToggle;
    private JPanel toolbarPanel;
    private final List<DocumentListener> changeListeners = new ArrayList<>();
    private final UndoManager undoManager = new UndoManager();

    // 保存编辑器和预览面板的引用，避免视图切换时丢失
    private JPanel editorPanelRef;
    private JPanel previewPanelRef;

    // 视图模式
    private static final int MODE_SPLIT = 0;
    private static final int MODE_EDIT_ONLY = 1;
    private static final int MODE_PREVIEW_ONLY = 2;
    private int viewMode = MODE_SPLIT;

    // 工具栏按钮
    private JButton undoButton;
    private JButton redoButton;

    public MarkdownEditorPanel() {
        initUI();
        setupKeyBindings();
    }

    private void initUI() {
        setLayout(new BorderLayout());

        // 先创建编辑器和预览面板（在创建工具栏之前，因为工具栏需要引用它们）
        editorPanelRef = createEditorPanel();
        previewPanelRef = createPreviewPanel();

        // 创建分割面板
        splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, editorPanelRef, previewPanelRef);
        splitPane.setResizeWeight(0.5);
        splitPane.setBorder(null);

        // 创建工具栏（在编辑器创建之后）
        toolbarPanel = createEnhancedToolbar();
        add(toolbarPanel, BorderLayout.NORTH);

        add(splitPane, BorderLayout.CENTER);

        // 创建状态栏（在编辑器创建之后）
        JPanel statusBar = createStatusBar();
        add(statusBar, BorderLayout.SOUTH);

        // 延迟设置分割位置，等待组件布局完成
        SwingUtilities.invokeLater(() -> {
            splitPane.setDividerLocation(0.5);
        });
    }

    /**
     * 创建增强的工具栏（扁平化设计，支持响应式换行）
     */
    private JPanel createEnhancedToolbar() {
        JPanel toolbarContainer = new JPanel(new WrapLayout(FlowLayout.LEFT, 5, 2));
        toolbarContainer.setBorder(BorderFactory.createCompoundBorder(
                new MatteBorder(0, 0, 1, 0, new Color(230, 230, 230)),
                new EmptyBorder(3, 5, 3, 5)
        ));
        toolbarContainer.setBackground(new Color(250, 250, 250));

        // 撤销/重做组
        undoButton = createFlatButton("↶", "撤销 (Ctrl+Z)", e -> undo());
        redoButton = createFlatButton("↷", "重做 (Ctrl+Y)", e -> redo());
        undoButton.setEnabled(false);
        redoButton.setEnabled(false);
        toolbarContainer.add(undoButton);
        toolbarContainer.add(redoButton);
        toolbarContainer.add(createVerticalDivider());

        // 标题组
        toolbarContainer.add(createFlatButton("H1", "一级标题", "# ", ""));
        toolbarContainer.add(createFlatButton("H2", "二级标题", "## ", ""));
        toolbarContainer.add(createFlatButton("H3", "三级标题", "### ", ""));
        toolbarContainer.add(createVerticalDivider());

        // 文本格式组
        toolbarContainer.add(createFlatButton("<html><b>B</b></html>", "粗体 (Ctrl+B)", "**", "**"));
        toolbarContainer.add(createFlatButton("<html><i>I</i></html>", "斜体 (Ctrl+I)", "_", "_"));
        toolbarContainer.add(createFlatButton("<html><s>S</s></html>", "删除线", "~~", "~~"));
        toolbarContainer.add(createFlatButton("<html><code>`</code></html>", "行内代码", "`", "`"));
        toolbarContainer.add(createVerticalDivider());

        // 插入组
        toolbarContainer.add(createFlatButton("🔗", "链接 (Ctrl+K)", "[", "](url)"));
        toolbarContainer.add(createFlatButton("🖼", "图片", "![", "](url)"));
        toolbarContainer.add(createFlatActionButton("⊞", "表格", this::insertTable));
        toolbarContainer.add(createFlatButton("{}", "代码块", "```\n", "\n```"));
        toolbarContainer.add(createVerticalDivider());

        // 列表组
        toolbarContainer.add(createFlatButton("•", "无序列表", "- ", ""));
        toolbarContainer.add(createFlatButton("☑", "任务列表", "- [ ] ", ""));
        toolbarContainer.add(createFlatButton("❝", "引用", "> ", ""));
        toolbarContainer.add(createFlatButton("─", "分割线", "---\n", ""));
        toolbarContainer.add(createVerticalDivider());

        // 更多功能按钮
        JButton moreButton = createFlatButton("⋮", "更多", null);
        JPopupMenu moreMenu = createMoreMenu();
        moreButton.addActionListener(e -> moreMenu.show(moreButton, 0, moreButton.getHeight()));
        toolbarContainer.add(moreButton);
        toolbarContainer.add(createVerticalDivider());

        // 视图切换 - 使用图标按钮组
        JToggleButton splitViewBtn = new JToggleButton("⚏");
        JToggleButton editViewBtn = new JToggleButton("✎");
        JToggleButton previewViewBtn = new JToggleButton("👁");

        splitViewBtn.setToolTipText("分栏模式");
        editViewBtn.setToolTipText("仅编辑");
        previewViewBtn.setToolTipText("仅预览");

        ButtonGroup viewGroup = new ButtonGroup();
        viewGroup.add(splitViewBtn);
        viewGroup.add(editViewBtn);
        viewGroup.add(previewViewBtn);

        // 先设置样式，再设置选中状态
        styleToggleButton(splitViewBtn);
        styleToggleButton(editViewBtn);
        styleToggleButton(previewViewBtn);

        // 默认选中分栏模式
        splitViewBtn.setSelected(true);

        splitViewBtn.addActionListener(e -> { viewMode = MODE_SPLIT; updateViewMode(); });
        editViewBtn.addActionListener(e -> { viewMode = MODE_EDIT_ONLY; updateViewMode(); });
        previewViewBtn.addActionListener(e -> { viewMode = MODE_PREVIEW_ONLY; updateViewMode(); });

        toolbarContainer.add(splitViewBtn);
        toolbarContainer.add(editViewBtn);
        toolbarContainer.add(previewViewBtn);

        return toolbarContainer;
    }

    /**
     * 自定义 WrapLayout - 支持自动换行的 FlowLayout
     */
    private static class WrapLayout extends FlowLayout {
        public WrapLayout(int align, int hgap, int vgap) {
            super(align, hgap, vgap);
        }

        @Override
        public Dimension preferredLayoutSize(Container target) {
            return layoutSize(target, true);
        }

        @Override
        public Dimension minimumLayoutSize(Container target) {
            return layoutSize(target, false);
        }

        private Dimension layoutSize(Container target, boolean preferred) {
            synchronized (target.getTreeLock()) {
                int targetWidth = target.getWidth();
                if (targetWidth == 0) {
                    targetWidth = Integer.MAX_VALUE;
                }

                int hgap = getHgap();
                int vgap = getVgap();
                Insets insets = target.getInsets();
                int horizontalInsetsAndGap = insets.left + insets.right + (hgap * 2);
                int maxWidth = targetWidth - horizontalInsetsAndGap;

                Dimension dim = new Dimension(0, 0);
                int rowWidth = 0;
                int rowHeight = 0;

                int nmembers = target.getComponentCount();

                for (int i = 0; i < nmembers; i++) {
                    Component m = target.getComponent(i);

                    if (m.isVisible()) {
                        Dimension d = preferred ? m.getPreferredSize() : m.getMinimumSize();

                        if (rowWidth + d.width > maxWidth) {
                            // 换行
                            addRow(dim, rowWidth, rowHeight);
                            rowWidth = 0;
                            rowHeight = 0;
                        }

                        if (rowWidth != 0) {
                            rowWidth += hgap;
                        }

                        rowWidth += d.width;
                        rowHeight = Math.max(rowHeight, d.height);
                    }
                }

                addRow(dim, rowWidth, rowHeight);

                dim.width += horizontalInsetsAndGap;
                dim.height += insets.top + insets.bottom + vgap * 2;

                return dim;
            }
        }

        private void addRow(Dimension dim, int rowWidth, int rowHeight) {
            dim.width = Math.max(dim.width, rowWidth);

            if (dim.height > 0) {
                dim.height += getVgap();
            }

            dim.height += rowHeight;
        }
    }

    /**
     * 创建更多功能菜单
     */
    private JPopupMenu createMoreMenu() {
        JPopupMenu menu = new JPopupMenu();

        JMenuItem findItem = new JMenuItem("🔍 查找替换");
        findItem.setToolTipText("Ctrl+F");
        findItem.addActionListener(e -> showFindDialog());

        JMenuItem exportItem = new JMenuItem("💾 导出 HTML");
        exportItem.addActionListener(e -> exportToHtml());

        JMenuItem copyItem = new JMenuItem("📋 复制 HTML");
        copyItem.addActionListener(e -> copyHtmlToClipboard());

        menu.add(findItem);
        menu.addSeparator();
        menu.add(exportItem);
        menu.add(copyItem);

        return menu;
    }

    /**
     * 创建扁平化按钮
     */
    private JButton createFlatButton(String text, String tooltip, ActionListener action) {
        JButton button = new JButton(text);
        button.setToolTipText(tooltip);
        button.setFont(FontsUtil.getDefaultFontWithOffset(Font.PLAIN, 0));
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setContentAreaFilled(false);
        button.setMargin(new Insets(4, 8, 4, 8));
        // 不设置固定宽度，让按钮根据内容自适应
        button.setPreferredSize(null);
        button.setMinimumSize(new Dimension(28, 28));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        // 鼠标悬停效果
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                if (button.isEnabled()) {
                    button.setContentAreaFilled(true);
                    button.setBackground(new Color(240, 240, 240));
                }
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent evt) {
                button.setContentAreaFilled(false);
            }
        });

        if (action != null) {
            button.addActionListener(action);
        }

        return button;
    }

    /**
     * 创建扁平化格式按钮
     */
    private JButton createFlatButton(String text, String tooltip, String prefix, String suffix) {
        return createFlatButton(text, tooltip, e -> insertFormat(prefix, suffix));
    }

    /**
     * 创建扁平化操作按钮（Runnable）
     */
    private JButton createFlatActionButton(String text, String tooltip, Runnable action) {
        return createFlatButton(text, tooltip, e -> action.run());
    }

    /**
     * 样式化切换按钮
     */
    private void styleToggleButton(JToggleButton button) {
        button.setFont(FontsUtil.getDefaultFontWithOffset(Font.PLAIN, 0));
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setContentAreaFilled(false);
        button.setMargin(new Insets(4, 10, 4, 10));
        // 不设置固定宽度，让按钮根据图标自适应
        button.setPreferredSize(null);
        button.setMinimumSize(new Dimension(32, 28));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        button.addItemListener(e -> {
            if (button.isSelected()) {
                button.setContentAreaFilled(true);
                button.setBackground(new Color(220, 230, 240));
            } else {
                button.setContentAreaFilled(false);
            }
        });

        button.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                if (!button.isSelected()) {
                    button.setContentAreaFilled(true);
                    button.setBackground(new Color(240, 240, 240));
                }
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent evt) {
                if (!button.isSelected()) {
                    button.setContentAreaFilled(false);
                }
            }
        });
    }

    /**
     * 创建垂直分割线
     */
    private Component createVerticalDivider() {
        JSeparator separator = new JSeparator(SwingConstants.VERTICAL);
        separator.setPreferredSize(new Dimension(1, 20));
        separator.setForeground(new Color(220, 220, 220));
        return separator;
    }

    /**
     * 创建工具栏分组
     */
    private JPanel createToolbarSection(String title, JComponent[] components) {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 3, 0));
        panel.setOpaque(false);

        JLabel label = new JLabel(title + ":");
        label.setFont(FontsUtil.getDefaultFontWithOffset(Font.PLAIN, -1));
        label.setForeground(Color.GRAY);
        panel.add(label);

        for (JComponent component : components) {
            panel.add(component);
        }

        return panel;
    }

    /**
     * 创建分隔符
     */
    private Component createSeparator() {
        JSeparator separator = new JSeparator(SwingConstants.VERTICAL);
        separator.setMaximumSize(new Dimension(1, 30));
        return separator;
    }

    /**
     * 创建编辑器面板（包含行号）
     */
    private JPanel createEditorPanel() {
        JPanel panel = new JPanel(new BorderLayout());

        // 行号区域
        lineNumberArea = new JTextArea("1");
        lineNumberArea.setFont(new Font("Monospaced", Font.PLAIN, 13));
        lineNumberArea.setBackground(new Color(240, 240, 240));
        lineNumberArea.setForeground(Color.GRAY);
        lineNumberArea.setEditable(false);
        lineNumberArea.setBorder(new EmptyBorder(10, 5, 10, 5));
        lineNumberArea.setPreferredSize(new Dimension(40, Integer.MAX_VALUE));

        // 编辑器区域
        editorArea = new JTextArea();
        editorArea.setFont(new Font("Monospaced", Font.PLAIN, 14));
        editorArea.setLineWrap(true);
        editorArea.setWrapStyleWord(true);
        editorArea.setBorder(new EmptyBorder(10, 10, 10, 10));
        editorArea.setTabSize(4);

        // 添加撤销/重做支持
        editorArea.getDocument().addUndoableEditListener(e -> {
            undoManager.addEdit(e.getEdit());
            updateUndoRedoButtons();
        });

        // 监听内容变化，更新行号和预览
        editorArea.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                updateLineNumbers();
                updatePreview();
                notifyChangeListeners(e);
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                updateLineNumbers();
                updatePreview();
                notifyChangeListeners(e);
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                updateLineNumbers();
                updatePreview();
                notifyChangeListeners(e);
            }
        });

        // 滚动同步
        JScrollPane editorScrollPane = new JScrollPane(editorArea);
        editorScrollPane.setRowHeaderView(lineNumberArea);
        editorScrollPane.setBorder(BorderFactory.createLineBorder(ModernColors.getBorderLightColor()));

        panel.add(editorScrollPane, BorderLayout.CENTER);
        return panel;
    }

    /**
     * 创建预览面板
     */
    private JPanel createPreviewPanel() {
        JPanel panel = new JPanel(new BorderLayout());

        previewPane = new JEditorPane();
        previewPane.setContentType("text/html");
        previewPane.setEditable(false);
        previewPane.setBorder(new EmptyBorder(10, 10, 10, 10));

        // 创建独立的 HTMLEditorKit 实例，避免影响全局 HTML 渲染器
        // 重要：不要使用共享的 HTMLEditorKit，这会影响其他组件（如 JTree）的 HTML 渲染
        HTMLEditorKit kit = new HTMLEditorKit();
        StyleSheet styleSheet = new StyleSheet(); // 创建新的 StyleSheet 而不是使用默认的

        // 基础样式
        styleSheet.addRule("body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', 'Helvetica Neue', Arial, sans-serif; font-size: 14px; line-height: 1.6; color: #24292e; padding: 16px; background: #fff; }");

        // 标题样式
        styleSheet.addRule("h1, h2 { border-bottom: 1px solid #eaecef; padding-bottom: 0.3em; }");
        styleSheet.addRule("h1 { font-size: 2em; margin: 0.67em 0; font-weight: 600; }");
        styleSheet.addRule("h2 { font-size: 1.5em; margin: 0.75em 0; font-weight: 600; }");
        styleSheet.addRule("h3 { font-size: 1.25em; margin: 1em 0; font-weight: 600; }");
        styleSheet.addRule("h4 { font-size: 1em; margin: 1.33em 0; font-weight: 600; }");
        styleSheet.addRule("h5 { font-size: 0.875em; margin: 1.67em 0; font-weight: 600; }");
        styleSheet.addRule("h6 { font-size: 0.85em; margin: 2.33em 0; font-weight: 600; color: #6a737d; }");

        // 段落和文本
        styleSheet.addRule("p { margin-top: 0; margin-bottom: 16px; }");
        styleSheet.addRule("strong { font-weight: 600; }");
        styleSheet.addRule("em { font-style: italic; }");
        styleSheet.addRule("del { text-decoration: line-through; }");

        // 代码样式
        styleSheet.addRule("code { background-color: rgba(27,31,35,0.05); padding: 0.2em 0.4em; margin: 0; font-size: 85%; border-radius: 3px; font-family: 'SF Mono', Monaco, 'Cascadia Code', 'Roboto Mono', Consolas, 'Courier New', monospace; }");
        styleSheet.addRule("pre { background-color: #f6f8fa; padding: 16px; overflow: auto; font-size: 85%; line-height: 1.45; border-radius: 6px; margin-top: 0; margin-bottom: 16px; }");
        styleSheet.addRule("pre code { background-color: transparent; border: 0; display: inline; max-width: auto; padding: 0; margin: 0; overflow: visible; line-height: inherit; word-wrap: normal; }");

        // 引用
        styleSheet.addRule("blockquote { padding: 0 1em; color: #6a737d; border-left: 0.25em solid #dfe2e5; margin: 0 0 16px 0; }");
        styleSheet.addRule("blockquote > :first-child { margin-top: 0; }");
        styleSheet.addRule("blockquote > :last-child { margin-bottom: 0; }");

        // 列表
        styleSheet.addRule("ul, ol { padding-left: 2em; margin-top: 0; margin-bottom: 16px; }");
        styleSheet.addRule("li { word-wrap: break-all; }");
        styleSheet.addRule("li > p { margin-top: 16px; }");
        styleSheet.addRule("li + li { margin-top: 0.25em; }");

        // 任务列表
        styleSheet.addRule("input[type='checkbox'] { margin-right: 0.5em; }");

        // 表格
        styleSheet.addRule("table { border-spacing: 0; border-collapse: collapse; display: block; width: max-content; max-width: 100%; overflow: auto; margin-top: 0; margin-bottom: 16px; }");
        styleSheet.addRule("table tr { background-color: #fff; border-top: 1px solid #c6cbd1; }");
        styleSheet.addRule("table tr:nth-child(2n) { background-color: #f6f8fa; }");
        styleSheet.addRule("table th, table td { padding: 6px 13px; border: 1px solid #dfe2e5; }");
        styleSheet.addRule("table th { font-weight: 600; background-color: #f6f8fa; }");

        // 水平线
        styleSheet.addRule("hr { height: 0.25em; padding: 0; margin: 24px 0; background-color: #e1e4e8; border: 0; }");

        // 链接
        styleSheet.addRule("a { color: #0366d6; text-decoration: none; }");
        styleSheet.addRule("a:hover { text-decoration: underline; }");

        // 图片
        styleSheet.addRule("img { max-width: 100%; box-sizing: content-box; background-color: #fff; border-style: none; }");

        // 将独立的 StyleSheet 设置到 kit 中
        kit.setStyleSheet(styleSheet);
        // 设置独立的 EditorKit 到预览面板，隔离样式影响
        previewPane.setEditorKit(kit);

        JScrollPane scrollPane = new JScrollPane(previewPane);
        scrollPane.setBorder(BorderFactory.createLineBorder(ModernColors.getBorderLightColor()));

        panel.add(scrollPane, BorderLayout.CENTER);
        return panel;
    }

    /**
     * 创建状态栏
     */
    private JPanel createStatusBar() {
        JPanel statusBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 3));
        statusBar.setBorder(new MatteBorder(1, 0, 0, 0, ModernColors.getBorderLightColor()));

        JLabel statusLabel = new JLabel("就绪");
        statusLabel.setFont(FontsUtil.getDefaultFontWithOffset(Font.PLAIN, -1));
        statusLabel.setForeground(Color.GRAY);
        statusBar.add(statusLabel);

        // 字数统计
        JLabel wordCountLabel = new JLabel("字数: 0");
        wordCountLabel.setFont(FontsUtil.getDefaultFontWithOffset(Font.PLAIN, -1));
        wordCountLabel.setForeground(Color.GRAY);
        statusBar.add(new JSeparator(SwingConstants.VERTICAL));
        statusBar.add(wordCountLabel);

        // 行列号
        JLabel positionLabel = new JLabel("行: 1, 列: 1");
        positionLabel.setFont(FontsUtil.getDefaultFontWithOffset(Font.PLAIN, -1));
        positionLabel.setForeground(Color.GRAY);
        statusBar.add(new JSeparator(SwingConstants.VERTICAL));
        statusBar.add(positionLabel);

        // 更新状态栏
        editorArea.addCaretListener(e -> {
            try {
                int pos = editorArea.getCaretPosition();
                int line = editorArea.getLineOfOffset(pos);
                int col = pos - editorArea.getLineStartOffset(line);
                positionLabel.setText(String.format("行: %d, 列: %d", line + 1, col + 1));

                String text = editorArea.getText();
                wordCountLabel.setText(String.format("字数: %d | 字符: %d",
                        text.split("\\s+").length, text.length()));
            } catch (Exception ex) {
                // Ignore
            }
        });

        return statusBar;
    }


    /**
     * 设置快捷键
     */
    private void setupKeyBindings() {
        InputMap inputMap = editorArea.getInputMap(JComponent.WHEN_FOCUSED);
        ActionMap actionMap = editorArea.getActionMap();

        // Ctrl+B - 粗体
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_B, InputEvent.CTRL_DOWN_MASK), "bold");
        actionMap.put("bold", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                insertFormat("**", "**");
            }
        });

        // Ctrl+I - 斜体
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_I, InputEvent.CTRL_DOWN_MASK), "italic");
        actionMap.put("italic", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                insertFormat("_", "_");
            }
        });

        // Ctrl+K - 链接
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_K, InputEvent.CTRL_DOWN_MASK), "link");
        actionMap.put("link", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                insertFormat("[", "](url)");
            }
        });

        // Ctrl+Z - 撤销
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_Z, InputEvent.CTRL_DOWN_MASK), "undo");
        actionMap.put("undo", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                undo();
            }
        });

        // Ctrl+Y - 重做
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_Y, InputEvent.CTRL_DOWN_MASK), "redo");
        actionMap.put("redo", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                redo();
            }
        });

        // Ctrl+F - 查找
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_F, InputEvent.CTRL_DOWN_MASK), "find");
        actionMap.put("find", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                showFindDialog();
            }
        });
    }

    /**
     * 插入格式化文本
     */
    private void insertFormat(String prefix, String suffix) {
        int start = editorArea.getSelectionStart();
        int end = editorArea.getSelectionEnd();
        String selectedText = editorArea.getSelectedText();

        if (selectedText != null && !selectedText.isEmpty()) {
            editorArea.replaceSelection(prefix + selectedText + suffix);
            editorArea.setSelectionStart(start + prefix.length());
            editorArea.setSelectionEnd(end + prefix.length());
        } else {
            editorArea.insert(prefix + suffix, start);
            editorArea.setCaretPosition(start + prefix.length());
        }
        editorArea.requestFocus();
    }

    /**
     * 插入表格
     */
    private void insertTable() {
        String table = """
                | 列1 | 列2 | 列3 |
                | --- | --- | --- |
                | 单元格 | 单元格 | 单元格 |
                | 单元格 | 单元格 | 单元格 |
                """;
        int pos = editorArea.getCaretPosition();
        editorArea.insert(table, pos);
        editorArea.requestFocus();
    }

    /**
     * 撤销
     */
    private void undo() {
        if (undoManager.canUndo()) {
            undoManager.undo();
            updateUndoRedoButtons();
        }
    }

    /**
     * 重做
     */
    private void redo() {
        if (undoManager.canRedo()) {
            undoManager.redo();
            updateUndoRedoButtons();
        }
    }

    /**
     * 更新撤销/重做按钮状态
     */
    private void updateUndoRedoButtons() {
        if (undoButton != null) {
            undoButton.setEnabled(undoManager.canUndo());
        }
        if (redoButton != null) {
            redoButton.setEnabled(undoManager.canRedo());
        }
    }

    /**
     * 显示查找对话框
     */
    private void showFindDialog() {
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "查找", true);
        dialog.setLayout(new BorderLayout(10, 10));
        dialog.getRootPane().setBorder(new EmptyBorder(10, 10, 10, 10));

        JPanel panel = new JPanel(new GridLayout(2, 2, 5, 5));
        panel.add(new JLabel("查找:"));
        JTextField findField = new JTextField(20);
        panel.add(findField);

        panel.add(new JLabel("替换为:"));
        JTextField replaceField = new JTextField(20);
        panel.add(replaceField);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton findButton = new JButton("查找下一个");
        JButton replaceButton = new JButton("替换");
        JButton replaceAllButton = new JButton("全部替换");
        JButton closeButton = new JButton("关闭");

        findButton.addActionListener(e -> {
            String text = editorArea.getText();
            String find = findField.getText();
            int pos = editorArea.getCaretPosition();
            int index = text.indexOf(find, pos);
            if (index >= 0) {
                editorArea.setSelectionStart(index);
                editorArea.setSelectionEnd(index + find.length());
            } else {
                JOptionPane.showMessageDialog(dialog, "未找到匹配项", "查找", JOptionPane.INFORMATION_MESSAGE);
            }
        });

        replaceButton.addActionListener(e -> {
            String selected = editorArea.getSelectedText();
            String find = findField.getText();
            if (find.equals(selected)) {
                editorArea.replaceSelection(replaceField.getText());
            }
        });

        replaceAllButton.addActionListener(e -> {
            String text = editorArea.getText();
            String find = findField.getText();
            String replace = replaceField.getText();
            text = text.replace(find, replace);
            editorArea.setText(text);
            JOptionPane.showMessageDialog(dialog, "替换完成", "查找", JOptionPane.INFORMATION_MESSAGE);
        });

        closeButton.addActionListener(e -> dialog.dispose());

        buttonPanel.add(findButton);
        buttonPanel.add(replaceButton);
        buttonPanel.add(replaceAllButton);
        buttonPanel.add(closeButton);

        dialog.add(panel, BorderLayout.CENTER);
        dialog.add(buttonPanel, BorderLayout.SOUTH);
        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    /**
     * 导出为 HTML
     */
    private void exportToHtml() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("导出 HTML");
        fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("HTML 文件", "html"));

        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                String html = convertMarkdownToHtml(editorArea.getText());
                java.io.File file = fileChooser.getSelectedFile();
                if (!file.getName().endsWith(".html")) {
                    file = new java.io.File(file.getAbsolutePath() + ".html");
                }
                java.nio.file.Files.writeString(file.toPath(), html);
                JOptionPane.showMessageDialog(this, "导出成功！", "导出", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "导出失败: " + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /**
     * 复制 HTML 到剪贴板
     */
    private void copyHtmlToClipboard() {
        String html = convertMarkdownToHtml(editorArea.getText());
        StringSelection selection = new StringSelection(html);
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, selection);
        JOptionPane.showMessageDialog(this, "HTML 已复制到剪贴板！", "复制", JOptionPane.INFORMATION_MESSAGE);
    }

    /**
     * 更新视图模式
     */
    private void updateViewMode() {
        switch (viewMode) {
            case MODE_SPLIT:
                splitPane.setLeftComponent(editorPanelRef);
                splitPane.setRightComponent(previewPanelRef);
                splitPane.setDividerSize(5);
                SwingUtilities.invokeLater(() -> splitPane.setDividerLocation(0.5));
                break;
            case MODE_EDIT_ONLY:
                splitPane.setLeftComponent(editorPanelRef);
                splitPane.setRightComponent(null);
                splitPane.setDividerSize(0);
                break;
            case MODE_PREVIEW_ONLY:
                splitPane.setLeftComponent(null);
                splitPane.setRightComponent(previewPanelRef);
                splitPane.setDividerSize(0);
                break;
            default:
                break;
        }
        splitPane.revalidate();
        splitPane.repaint();
        updatePreview();
    }

    /**
     * 更新行号
     */
    private void updateLineNumbers() {
        int lineCount = editorArea.getLineCount();
        StringBuilder lineNumbers = new StringBuilder();
        for (int i = 1; i <= lineCount; i++) {
            lineNumbers.append(i).append("\n");
        }
        lineNumberArea.setText(lineNumbers.toString());
    }

    /**
     * 更新预览
     */
    private void updatePreview() {
        String markdown = editorArea.getText();
        String html = convertMarkdownToHtml(markdown);
        previewPane.setText(html);
        previewPane.setCaretPosition(0);
    }

    /**
     * 企业级 Markdown 到 HTML 转换
     * 支持完整的 GitHub Flavored Markdown (GFM) 语法
     */
    private String convertMarkdownToHtml(String markdown) {
        if (markdown == null || markdown.isEmpty()) {
            return "<html><body></body></html>";
        }

        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html><html><head>");
        html.append("<meta charset='UTF-8'>");
        html.append("</head><body>");

        String[] lines = markdown.split("\n");
        boolean inCodeBlock = false;
        boolean inList = false;
        boolean inOrderedList = false;
        boolean inTable = false;
        String codeLanguage = "";

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];

            // 代码块
            if (line.trim().startsWith("```")) {
                if (inCodeBlock) {
                    html.append("</code></pre>");
                    inCodeBlock = false;
                    codeLanguage = "";
                } else {
                    // 提取语言标识
                    codeLanguage = line.trim().substring(3).trim();
                    html.append("<pre><code");
                    if (!codeLanguage.isEmpty()) {
                        html.append(" class='language-").append(escapeHtml(codeLanguage)).append("'");
                    }
                    html.append(">");
                    inCodeBlock = true;
                }
                continue;
            }

            if (inCodeBlock) {
                html.append(escapeHtml(line)).append("\n");
                continue;
            }

            // 表格处理
            if (line.trim().startsWith("|") && line.trim().endsWith("|")) {
                if (!inTable) {
                    html.append("<table>");
                    inTable = true;
                }

                // 检查是否是分隔行
                if (line.matches("^\\|[\\s\\-:|]+\\|$")) {
                    // 跳过分隔行
                    continue;
                }

                // 判断是否是表头（下一行是分隔行）
                boolean isHeader = false;
                if (i + 1 < lines.length && lines[i + 1].matches("^\\|[\\s\\-:|]+\\|$")) {
                    isHeader = true;
                    html.append("<thead><tr>");
                } else if (inTable && html.toString().contains("<thead>")) {
                    if (!html.toString().contains("<tbody>")) {
                        html.append("<tbody>");
                    }
                    html.append("<tr>");
                } else {
                    html.append("<tr>");
                }

                String[] cells = line.split("\\|");
                for (int j = 1; j < cells.length - 1; j++) {
                    String cell = cells[j].trim();
                    if (isHeader) {
                        html.append("<th>").append(processInlineMarkdown(cell)).append("</th>");
                    } else {
                        html.append("<td>").append(processInlineMarkdown(cell)).append("</td>");
                    }
                }

                html.append("</tr>");
                if (isHeader) {
                    html.append("</thead>");
                }
                continue;
            } else if (inTable) {
                if (html.toString().contains("<tbody>")) {
                    html.append("</tbody>");
                }
                html.append("</table>");
                inTable = false;
            }

            // 标题
            if (line.startsWith("# ")) {
                closeLists(html, inList, inOrderedList);
                inList = false;
                inOrderedList = false;
                html.append("<h1>").append(processInlineMarkdown(line.substring(2))).append("</h1>");
            } else if (line.startsWith("## ")) {
                closeLists(html, inList, inOrderedList);
                inList = false;
                inOrderedList = false;
                html.append("<h2>").append(processInlineMarkdown(line.substring(3))).append("</h2>");
            } else if (line.startsWith("### ")) {
                closeLists(html, inList, inOrderedList);
                inList = false;
                inOrderedList = false;
                html.append("<h3>").append(processInlineMarkdown(line.substring(4))).append("</h3>");
            } else if (line.startsWith("#### ")) {
                closeLists(html, inList, inOrderedList);
                inList = false;
                inOrderedList = false;
                html.append("<h4>").append(processInlineMarkdown(line.substring(5))).append("</h4>");
            } else if (line.startsWith("##### ")) {
                closeLists(html, inList, inOrderedList);
                inList = false;
                inOrderedList = false;
                html.append("<h5>").append(processInlineMarkdown(line.substring(6))).append("</h5>");
            } else if (line.startsWith("###### ")) {
                closeLists(html, inList, inOrderedList);
                inList = false;
                inOrderedList = false;
                html.append("<h6>").append(processInlineMarkdown(line.substring(7))).append("</h6>");
            }
            // 水平线
            else if (line.trim().equals("---") || line.trim().equals("***") || line.trim().equals("___")) {
                closeLists(html, inList, inOrderedList);
                inList = false;
                inOrderedList = false;
                html.append("<hr>");
            }
            // 任务列表
            else if (line.trim().matches("^[-*]\\s+\\[[ xX]\\]\\s+.*")) {
                if (!inList) {
                    html.append("<ul class='task-list'>");
                    inList = true;
                }
                boolean checked = line.toLowerCase().contains("[x]");
                String content = line.trim().replaceFirst("^[-*]\\s+\\[[ xX]\\]\\s+", "");
                html.append("<li class='task-list-item'>");
                html.append("<input type='checkbox' disabled ");
                if (checked) html.append("checked");
                html.append("> ");
                html.append(processInlineMarkdown(content));
                html.append("</li>");
            }
            // 无序列表
            else if (line.trim().startsWith("- ") || line.trim().startsWith("* ")) {
                if (inOrderedList) {
                    html.append("</ol>");
                    inOrderedList = false;
                }
                if (!inList) {
                    html.append("<ul>");
                    inList = true;
                }
                String content = line.substring(line.indexOf(" ") + 1);
                html.append("<li>").append(processInlineMarkdown(content)).append("</li>");
            }
            // 有序列表
            else if (line.trim().matches("^\\d+\\.\\s.*")) {
                if (inList && !inOrderedList) {
                    html.append("</ul>");
                    inList = false;
                }
                if (!inOrderedList) {
                    html.append("<ol>");
                    inOrderedList = true;
                }
                String content = line.substring(line.indexOf(" ") + 1);
                html.append("<li>").append(processInlineMarkdown(content)).append("</li>");
            }
            // 引用
            else if (line.trim().startsWith("> ")) {
                closeLists(html, inList, inOrderedList);
                inList = false;
                inOrderedList = false;
                String content = line.substring(line.indexOf(">") + 1).trim();
                html.append("<blockquote>").append(processInlineMarkdown(content)).append("</blockquote>");
            }
            // 空行
            else if (line.trim().isEmpty()) {
                closeLists(html, inList, inOrderedList);
                inList = false;
                inOrderedList = false;
                html.append("<br>");
            }
            // 普通段落
            else {
                closeLists(html, inList, inOrderedList);
                inList = false;
                inOrderedList = false;
                html.append("<p>").append(processInlineMarkdown(line)).append("</p>");
            }
        }

        // 关闭未闭合的标签
        closeLists(html, inList, inOrderedList);
        if (inTable) {
            if (html.toString().contains("<tbody>")) {
                html.append("</tbody>");
            }
            html.append("</table>");
        }
        if (inCodeBlock) {
            html.append("</code></pre>");
        }

        html.append("</body></html>");
        return html.toString();
    }

    /**
     * 关闭列表标签
     */
    private void closeLists(StringBuilder html, boolean inList, boolean inOrderedList) {
        if (inList) {
            html.append("</ul>");
        }
        if (inOrderedList) {
            html.append("</ol>");
        }
    }

    /**
     * 处理行内 Markdown 语法（GFM 增强版）
     */
    private String processInlineMarkdown(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }

        // 先转义 HTML，防止用户输入的 HTML 标签被执行
        text = escapeHtml(text);

        // 然后处理 Markdown 语法（此时可以安全地插入 HTML 标签）

        // 粗斜体 ***text*** (必须在粗体和斜体之前处理)
        text = text.replaceAll("\\*\\*\\*(.+?)\\*\\*\\*", "<strong><em>$1</em></strong>");
        text = text.replaceAll("___(.+?)___", "<strong><em>$1</em></strong>");

        // 粗体 **text** 或 __text__
        text = text.replaceAll("\\*\\*(.+?)\\*\\*", "<strong>$1</strong>");
        text = text.replaceAll("__(.+?)__", "<strong>$1</strong>");

        // 斜体 *text* 或 _text_ (必须在粗体之后处理)
        text = text.replaceAll("\\*(.+?)\\*", "<em>$1</em>");
        text = text.replaceAll("(?<!_)_(.+?)_(?!_)", "<em>$1</em>");

        // 删除线 ~~text~~
        text = text.replaceAll("~~(.+?)~~", "<del>$1</del>");

        // 高亮 ==text== (部分编辑器支持)
        text = text.replaceAll("==(.+?)==", "<mark>$1</mark>");

        // 行内代码 `code` (必须在其他处理之后，避免代码中的特殊字符被处理)
        text = text.replaceAll("`(.+?)`", "<code>$1</code>");

        // 图片 ![alt](url)
        text = text.replaceAll("!\\[([^\\]]*)\\]\\(([^)]+)\\)", "<img src=\"$2\" alt=\"$1\" style=\"max-width: 100%;\" />");

        // 链接 [text](url)
        text = text.replaceAll("\\[([^\\]]+)\\]\\(([^)]+)\\)", "<a href=\"$2\">$1</a>");

        // 自动链接 <url>
        text = text.replaceAll("&lt;(https?://[^&]+)&gt;", "<a href=\"$1\">$1</a>");

        return text;
    }

    /**
     * HTML 转义
     */
    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    /**
     * 获取编辑器文本
     */
    public String getText() {
        return editorArea.getText();
    }


    /**
     * 设置编辑器文本
     */
    public void setText(String text) {
        editorArea.setText(text);
        updatePreview();
    }

    /**
     * 添加文档变化监听器
     */
    public void addDocumentListener(DocumentListener listener) {
        changeListeners.add(listener);
    }

    /**
     * 设置工具栏可见性
     */
    public void setToolbarVisible(boolean visible) {
        if (toolbarPanel != null) {
            toolbarPanel.setVisible(visible);
        }
    }

    /**
     * 设置状态栏可见性
     */
    public void setStatusBarVisible(boolean visible) {
        Component[] components = getComponents();
        for (Component comp : components) {
            if (comp instanceof JPanel) {
                JPanel panel = (JPanel) comp;
                if (panel.getLayout() instanceof FlowLayout) {
                    // 状态栏使用 FlowLayout
                    panel.setVisible(visible);
                    break;
                }
            }
        }
    }

    /**
     * 简化模式：隐藏工具栏和状态栏，适合嵌入场景
     */
    public void setSimpleMode(boolean simple) {
        setToolbarVisible(!simple);
        setStatusBarVisible(!simple);
        revalidate();
        repaint();
    }

    /**
     * 通知所有监听器
     */
    private void notifyChangeListeners(DocumentEvent e) {
        for (DocumentListener listener : changeListeners) {
            if (e.getType() == DocumentEvent.EventType.INSERT) {
                listener.insertUpdate(e);
            } else if (e.getType() == DocumentEvent.EventType.REMOVE) {
                listener.removeUpdate(e);
            } else {
                listener.changedUpdate(e);
            }
        }
    }

}
