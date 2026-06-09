package com.laker.postman.common.component;

import com.laker.postman.common.constants.ModernColors;
import com.laker.postman.service.render.HttpHtmlRenderer;
import com.laker.postman.util.EditorThemeUtil;
import com.laker.postman.util.FontsUtil;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
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
 * Markdown 编辑器组件
 * 支持实时预览、工具栏、撤销/重做、导出等功能
 */
public class MarkdownEditorPanel extends JPanel {
    private RSyntaxTextArea editorArea;
    private SearchableTextArea searchableTextArea;
    private JTextPane previewPane;
    private JSplitPane splitPane;
    private JPanel toolbarPanel;
    private final List<DocumentListener> changeListeners = new ArrayList<>();
    private final UndoManager undoManager = new UndoManager();

    private JPanel editorPanelRef;
    private JPanel previewPanelRef;

    private static final int MODE_SPLIT = 0;
    private static final int MODE_EDIT_ONLY = 1;
    private static final int MODE_PREVIEW_ONLY = 2;
    private int viewMode = MODE_SPLIT;

    private JButton undoButton;
    private JButton redoButton;
    private boolean editable = true;

    public MarkdownEditorPanel() {
        initUI();
        setupKeyBindings();
    }

    @Override
    public void updateUI() {
        super.updateUI();

        if (toolbarPanel != null && editorPanelRef != null && previewPanelRef != null) {
            removeAll();

            toolbarPanel = createEnhancedToolbar();
            add(toolbarPanel, BorderLayout.NORTH);

            add(splitPane, BorderLayout.CENTER);

            JPanel statusBar = createStatusBar();
            add(statusBar, BorderLayout.SOUTH);

            setEditable(editable);
            updatePreviewPaneStyles();

            revalidate();
            repaint();
        }
    }

    /**
     * 更新预览面板样式以适应主题变化
     */
    private void updatePreviewPaneStyles() {
        if (previewPane != null) {
            updatePreview();
        }
    }


    /**
     * 将 Color 转换为十六进制字符串
     */
    private String toHex(Color color) {
        return String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
    }

    private String getTableStyle() {
        return "border-collapse:collapse;width:100%;margin:0 0 8px 0;border:1px solid " + toHex(ModernColors.getBorderLightColor()) + ";";
    }

    private String getTableCellStyle() {
        return "padding:4px 8px;border:1px solid " + toHex(ModernColors.getBorderLightColor()) + ";";
    }

    private String getTableHeaderStyle() {
        String bgColor = toHex(ModernColors.getHoverBackgroundColor());
        return getTableCellStyle() + "font-weight:600;background-color:" + bgColor + ";";
    }

    private String getCodeBlockStyle() {
        return "background-color:" + toHex(ModernColors.getConsoleTextAreaBg()) +
                ";padding:8px;overflow:auto;font-size:10px;line-height:1.5;border-radius:4px;" +
                "margin:0 0 8px 0;font-family:monospace;color:" +
                toHex(ModernColors.getConsoleText()) +
                ";display:block;white-space:pre;word-wrap:normal;";
    }

    private String getInlineCodeStyle() {
        String bgColor = toHex(ModernColors.getHoverBackgroundColor());
        String textColor = toHex(ModernColors.getErrorDark());
        return "background-color:" + bgColor + ";color:" + textColor +
                ";padding:1px 4px;margin:0 1px;font-size:10px;border-radius:3px;font-family:monospace;";
    }

    private String getHeadingStyle(int level) {
        String dividerColor = toHex(ModernColors.getDividerBorderColor());
        return switch (level) {
            case 1 ->
                    "font-size:18px;font-weight:600;margin:4px 0 4px 0;border-bottom:2px solid " + dividerColor + ";padding-bottom:0.2em;";
            case 2 ->
                    "font-size:16px;font-weight:600;margin:4px 0 3px 0;border-bottom:1px solid " + dividerColor + ";padding-bottom:0.2em;";
            case 3 -> "font-size:14px;font-weight:600;margin:4px 0 3px 0;";
            case 4 -> "font-size:12px;font-weight:600;margin:4px 0 3px 0;";
            case 5 -> "font-size:11px;font-weight:600;margin:4px 0 3px 0;";
            case 6 ->
                    "font-size:10px;font-weight:600;margin:4px 0 3px 0;color:" + toHex(ModernColors.getTextHint()) + ";";
            default -> "";
        };
    }

    private String getBlockquoteStyle() {
        Color accentColor = ModernColors.getAccent();
        String borderColor = toHex(accentColor);
        String bgColor = String.format(java.util.Locale.ROOT, "rgba(%d,%d,%d,0.08)",
                accentColor.getRed(), accentColor.getGreen(), accentColor.getBlue());
        return "padding:6px 10px;color:" + toHex(ModernColors.getTextSecondary()) +
                ";border-left:3px solid " + borderColor + ";background-color:" + bgColor +
                ";margin:0 0 8px 0;border-radius:0 3px 3px 0;";
    }

    private String getHrStyle() {
        return "height:2px;margin:24px 0;background-color:" + toHex(ModernColors.getDividerBorderColor()) + ";border:0;";
    }


    private void initUI() {
        setLayout(new BorderLayout());
        ToolWindowSurfaceStyle.applyCard(this);

        editorPanelRef = createEditorPanel();
        previewPanelRef = createPreviewPanel();

        splitPane = ToolWindowChrome.createHorizontalCardSplitPane(editorPanelRef, previewPanelRef, 0);
        splitPane.setResizeWeight(0.5);

        toolbarPanel = createEnhancedToolbar();
        add(toolbarPanel, BorderLayout.NORTH);

        add(splitPane, BorderLayout.CENTER);

        JPanel statusBar = createStatusBar();
        add(statusBar, BorderLayout.SOUTH);

        SwingUtilities.invokeLater(() -> splitPane.setDividerLocation(0.5));
    }

    /**
     * 创建工具栏
     */
    private JPanel createEnhancedToolbar() {
        JPanel toolbarContainer = new JPanel(new WrapLayout(FlowLayout.LEFT, 5, 2));
        ToolWindowSurfaceStyle.applySectionHeader(toolbarContainer, 3, 5, 3, 5);

        undoButton = createFlatButton("↶", I18nUtil.getMessage(MessageKeys.MARKDOWN_UNDO), e -> undo());
        redoButton = createFlatButton("↷", I18nUtil.getMessage(MessageKeys.MARKDOWN_REDO), e -> redo());
        undoButton.setEnabled(false);
        redoButton.setEnabled(false);
        toolbarContainer.add(undoButton);
        toolbarContainer.add(redoButton);
        toolbarContainer.add(createVerticalDivider());

        toolbarContainer.add(createFlatButton("H1", I18nUtil.getMessage(MessageKeys.MARKDOWN_HEADING1), "# ", ""));
        toolbarContainer.add(createFlatButton("H2", I18nUtil.getMessage(MessageKeys.MARKDOWN_HEADING2), "## ", ""));
        toolbarContainer.add(createFlatButton("H3", I18nUtil.getMessage(MessageKeys.MARKDOWN_HEADING3), "### ", ""));
        toolbarContainer.add(createVerticalDivider());

        toolbarContainer.add(createFlatButton("<html><b>B</b></html>", I18nUtil.getMessage(MessageKeys.MARKDOWN_BOLD), "**", "**"));
        toolbarContainer.add(createFlatButton("<html><i>I</i></html>", I18nUtil.getMessage(MessageKeys.MARKDOWN_ITALIC), "_", "_"));
        toolbarContainer.add(createFlatButton("<html><s>S</s></html>", I18nUtil.getMessage(MessageKeys.MARKDOWN_STRIKETHROUGH), "~~", "~~"));
        toolbarContainer.add(createFlatButton("<html><code>`</code></html>", I18nUtil.getMessage(MessageKeys.MARKDOWN_INLINE_CODE), "`", "`"));
        toolbarContainer.add(createVerticalDivider());

        toolbarContainer.add(createFlatButton("🔗", I18nUtil.getMessage(MessageKeys.MARKDOWN_LINK), "[", "](url)"));
        toolbarContainer.add(createFlatButton("🖼", I18nUtil.getMessage(MessageKeys.MARKDOWN_IMAGE), "![", "](url)"));
        toolbarContainer.add(createFlatActionButton("⊞", I18nUtil.getMessage(MessageKeys.MARKDOWN_TABLE), this::insertTable));
        toolbarContainer.add(createFlatButton("{}", I18nUtil.getMessage(MessageKeys.MARKDOWN_CODE_BLOCK), "```\n", "\n```"));
        toolbarContainer.add(createVerticalDivider());

        toolbarContainer.add(createFlatButton("•", I18nUtil.getMessage(MessageKeys.MARKDOWN_UNORDERED_LIST), "- ", ""));
        toolbarContainer.add(createFlatButton("☑", I18nUtil.getMessage(MessageKeys.MARKDOWN_TASK_LIST), "- [ ] ", ""));
        toolbarContainer.add(createFlatButton("❝", I18nUtil.getMessage(MessageKeys.MARKDOWN_QUOTE), "> ", ""));
        toolbarContainer.add(createFlatButton("─", I18nUtil.getMessage(MessageKeys.MARKDOWN_HORIZONTAL_LINE), "---\n", ""));
        toolbarContainer.add(createVerticalDivider());

        JButton moreButton = createFlatButton("⋮", I18nUtil.getMessage(MessageKeys.MARKDOWN_MORE), null);
        JPopupMenu moreMenu = createMoreMenu();
        moreButton.addActionListener(e -> moreMenu.show(moreButton, 0, moreButton.getHeight()));
        toolbarContainer.add(moreButton);
        toolbarContainer.add(createVerticalDivider());

        JToggleButton splitViewBtn = new JToggleButton("⚏");
        JToggleButton editViewBtn = new JToggleButton("✎");
        JToggleButton previewViewBtn = new JToggleButton("👁");

        splitViewBtn.setToolTipText(I18nUtil.getMessage(MessageKeys.MARKDOWN_VIEW_SPLIT));
        editViewBtn.setToolTipText(I18nUtil.getMessage(MessageKeys.MARKDOWN_VIEW_EDIT_ONLY));
        previewViewBtn.setToolTipText(I18nUtil.getMessage(MessageKeys.MARKDOWN_VIEW_PREVIEW_ONLY));

        ButtonGroup viewGroup = new ButtonGroup();
        viewGroup.add(splitViewBtn);
        viewGroup.add(editViewBtn);
        viewGroup.add(previewViewBtn);

        styleToggleButton(splitViewBtn);
        styleToggleButton(editViewBtn);
        styleToggleButton(previewViewBtn);

        splitViewBtn.setSelected(true);

        splitViewBtn.addActionListener(e -> {
            viewMode = MODE_SPLIT;
            updateViewMode();
        });
        editViewBtn.addActionListener(e -> {
            viewMode = MODE_EDIT_ONLY;
            updateViewMode();
        });
        previewViewBtn.addActionListener(e -> {
            viewMode = MODE_PREVIEW_ONLY;
            updateViewMode();
        });

        toolbarContainer.add(splitViewBtn);
        toolbarContainer.add(editViewBtn);
        toolbarContainer.add(previewViewBtn);

        return toolbarContainer;
    }

    /**
     * 支持自动换行的 FlowLayout
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
        ToolWindowSurfaceStyle.applyPopupMenuCard(menu);

        JMenuItem exportItem = new JMenuItem("💾 " + I18nUtil.getMessage(MessageKeys.MARKDOWN_EXPORT_HTML));
        exportItem.addActionListener(e -> exportToHtml());

        JMenuItem copyItem = new JMenuItem("📋 " + I18nUtil.getMessage(MessageKeys.MARKDOWN_COPY_HTML));
        copyItem.addActionListener(e -> copyHtmlToClipboard());

        menu.add(exportItem);
        menu.add(copyItem);

        return menu;
    }

    private JButton createFlatButton(String text, String tooltip, ActionListener action) {
        JButton button = new JButton(text);
        button.setToolTipText(tooltip);
        button.setFont(FontsUtil.getDefaultFontWithOffset(Font.PLAIN, 0));
        button.setFocusPainted(false);
        button.setMargin(new Insets(4, 8, 4, 8));
        button.setPreferredSize(null);
        button.setMinimumSize(new Dimension(28, 28));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.putClientProperty(com.formdev.flatlaf.FlatClientProperties.BUTTON_TYPE,
                com.formdev.flatlaf.FlatClientProperties.BUTTON_TYPE_TOOLBAR_BUTTON);

        if (action != null) {
            button.addActionListener(action);
        }

        return button;
    }

    private JButton createFlatButton(String text, String tooltip, String prefix, String suffix) {
        return createFlatButton(text, tooltip, e -> insertFormat(prefix, suffix));
    }

    private JButton createFlatActionButton(String text, String tooltip, Runnable action) {
        return createFlatButton(text, tooltip, e -> action.run());
    }

    private void styleToggleButton(JToggleButton button) {
        button.setFont(FontsUtil.getDefaultFontWithOffset(Font.PLAIN, 0));
        button.setFocusPainted(false);
        button.setMargin(new Insets(4, 10, 4, 10));
        button.setPreferredSize(null);
        button.setMinimumSize(new Dimension(32, 28));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.putClientProperty(com.formdev.flatlaf.FlatClientProperties.BUTTON_TYPE,
                com.formdev.flatlaf.FlatClientProperties.BUTTON_TYPE_TOOLBAR_BUTTON);
    }

    private Component createVerticalDivider() {
        JSeparator separator = new JSeparator(SwingConstants.VERTICAL);
        separator.setPreferredSize(new Dimension(1, 20));
        separator.setForeground(ModernColors.getDividerBorderColor());
        return separator;
    }

    /**
     * 创建编辑器面板
     */
    private JPanel createEditorPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        ToolWindowSurfaceStyle.applyCard(panel);

        // 创建 RSyntaxTextArea 用于 Markdown 编辑
        editorArea = new RSyntaxTextArea();
        editorArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_MARKDOWN); // 设置为 Markdown 语法高亮
        editorArea.setCodeFoldingEnabled(false); // Markdown 不需要代码折叠
        editorArea.setTabSize(4); // 设置 Tab 宽度为 4 个空格
        // 加载编辑器主题 - 支持亮色和暗色主题自适应（必须在 setFont 之前，否则主题会覆盖字体）
        EditorThemeUtil.loadTheme(editorArea);
        // 设置字体（在 loadTheme 之后，确保不被主题覆盖）
        editorArea.setFont(FontsUtil.getDefaultFontWithOffset(Font.PLAIN, 0));

        // 使用 SearchableTextArea 包装器（启用搜索替换功能）
        searchableTextArea = new SearchableTextArea(editorArea, true);

        // 添加撤销/重做支持
        editorArea.getDocument().addUndoableEditListener(e -> {
            undoManager.addEdit(e.getEdit());
            updateUndoRedoButtons();
        });

        // 监听内容变化，更新预览
        editorArea.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                updatePreview();
                notifyChangeListeners(e);
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                updatePreview();
                notifyChangeListeners(e);
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                updatePreview();
                notifyChangeListeners(e);
            }
        });

        panel.add(searchableTextArea, BorderLayout.CENTER);
        return panel;
    }

    /**
     * 创建预览面板
     */
    private JPanel createPreviewPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        ToolWindowSurfaceStyle.applyCard(panel);

        previewPane = new JTextPane();
        previewPane.setContentType("text/html");
        previewPane.setEditable(false);
        previewPane.setBorder(new EmptyBorder(3, 3, 3, 3));
        previewPane.setBackground(ModernColors.getCardBackgroundColor());
        JScrollPane scrollPane = new JScrollPane(previewPane);
        ToolWindowSurfaceStyle.applyScrollPaneCard(scrollPane);

        panel.add(scrollPane, BorderLayout.CENTER);
        return panel;
    }

    /**
     * 创建状态栏
     */
    private JPanel createStatusBar() {
        JPanel statusBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 3));
        ToolWindowSurfaceStyle.applySectionHeader(statusBar);

        JLabel statusLabel = new JLabel(I18nUtil.getMessage(MessageKeys.MARKDOWN_READY));
        statusLabel.setFont(FontsUtil.getDefaultFontWithOffset(Font.PLAIN, -1));
        statusLabel.setForeground(ModernColors.getTextSecondary());
        statusBar.add(statusLabel);

        // 字数统计
        JLabel wordCountLabel = new JLabel(String.format("%s: 0 | %s: 0",
                I18nUtil.getMessage(MessageKeys.MARKDOWN_STATUS_WORDS),
                I18nUtil.getMessage(MessageKeys.MARKDOWN_STATUS_CHARS)));
        wordCountLabel.setFont(FontsUtil.getDefaultFontWithOffset(Font.PLAIN, -1));
        wordCountLabel.setForeground(ModernColors.getTextSecondary());
        statusBar.add(new JSeparator(SwingConstants.VERTICAL));
        statusBar.add(wordCountLabel);

        // 行列号
        JLabel positionLabel = new JLabel(I18nUtil.getMessage(MessageKeys.MARKDOWN_STATUS_LINE) + ": 1, " +
                I18nUtil.getMessage(MessageKeys.MARKDOWN_STATUS_COLUMN) + ": 1");
        positionLabel.setFont(FontsUtil.getDefaultFontWithOffset(Font.PLAIN, -1));
        positionLabel.setForeground(ModernColors.getTextSecondary());
        statusBar.add(new JSeparator(SwingConstants.VERTICAL));
        statusBar.add(positionLabel);

        // 更新状态栏
        editorArea.addCaretListener(e -> {
            try {
                int pos = editorArea.getCaretPosition();
                int line = editorArea.getLineOfOffset(pos);
                int col = pos - editorArea.getLineStartOffset(line);
                positionLabel.setText(String.format("%s: %d, %s: %d",
                        I18nUtil.getMessage(MessageKeys.MARKDOWN_STATUS_LINE), line + 1,
                        I18nUtil.getMessage(MessageKeys.MARKDOWN_STATUS_COLUMN), col + 1));

                String text = editorArea.getText();
                // 计算单词数：空文本或只有空白字符时为0
                int wordCount = 0;
                int charCount = 0;
                if (text != null) {
                    charCount = text.length();
                    if (!text.trim().isEmpty()) {
                        wordCount = text.trim().split("\\s+").length;
                    }
                }
                wordCountLabel.setText(String.format("%s: %d | %s: %d",
                        I18nUtil.getMessage(MessageKeys.MARKDOWN_STATUS_WORDS), wordCount,
                        I18nUtil.getMessage(MessageKeys.MARKDOWN_STATUS_CHARS), charCount));
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


        // Ctrl+` - 行内代码
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_BACK_QUOTE, InputEvent.CTRL_DOWN_MASK), "inlineCode");
        actionMap.put("inlineCode", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                insertFormat("`", "`");
            }
        });

        // Ctrl+Shift+K - 删除线
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_K, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK), "strikethrough");
        actionMap.put("strikethrough", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                insertFormat("~~", "~~");
            }
        });

        // Ctrl+Shift+C - 代码块
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_C, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK), "codeBlock");
        actionMap.put("codeBlock", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                insertFormat("```\n", "\n```");
            }
        });

        // Ctrl+Shift+Q - 引用
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_Q, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK), "quote");
        actionMap.put("quote", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                insertLinePrefix("> ");
            }
        });

        // Ctrl+Shift+L - 无序列表
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_L, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK), "unorderedList");
        actionMap.put("unorderedList", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                insertLinePrefix("- ");
            }
        });

        // Ctrl+Shift+O - 有序列表
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK), "orderedList");
        actionMap.put("orderedList", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                insertLinePrefix("1. ");
            }
        });

        // Ctrl+Shift+T - 任务列表
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_T, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK), "taskList");
        actionMap.put("taskList", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                insertLinePrefix("- [ ] ");
            }
        });

        // Ctrl+1-6 - 标题级别
        for (int i = 1; i <= 6; i++) {
            final int level = i;
            inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_0 + i, InputEvent.CTRL_DOWN_MASK), "heading" + i);
            actionMap.put("heading" + i, new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    String prefix = "#".repeat(level) + " ";
                    insertLinePrefix(prefix);
                }
            });
        }

        // Ctrl+Shift+H - 水平线
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_H, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK), "horizontalLine");
        actionMap.put("horizontalLine", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int pos = editorArea.getCaretPosition();
                editorArea.insert("\n---\n", pos);
                editorArea.requestFocus();
            }
        });

        // Ctrl+Shift+I - 插入图片
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_I, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK), "image");
        actionMap.put("image", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                insertFormat("![", "](url)");
            }
        });

        // Ctrl+Shift+T - 插入表格
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_T, InputEvent.CTRL_DOWN_MASK | InputEvent.ALT_DOWN_MASK), "table");
        actionMap.put("table", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                insertTable();
            }
        });

        // Ctrl+S - 触发保存事件（通知监听器）
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK), "save");
        actionMap.put("save", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // 触发文档变化事件，让外部监听器处理保存
                editorArea.getDocument().putProperty("save-requested", true);
            }
        });

        // Ctrl+E - 导出HTML
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_E, InputEvent.CTRL_DOWN_MASK), "export");
        actionMap.put("export", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                exportToHtml();
            }
        });

        // Ctrl+Shift+C - 复制HTML（不与代码块冲突，使用Alt）
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_C, InputEvent.CTRL_DOWN_MASK | InputEvent.ALT_DOWN_MASK), "copyHtml");
        actionMap.put("copyHtml", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                copyHtmlToClipboard();
            }
        });
    }

    /**
     * 在行首插入前缀（用于列表、引用、标题等）
     */
    private void insertLinePrefix(String prefix) {
        try {
            int pos = editorArea.getCaretPosition();
            int lineStart = editorArea.getLineStartOffset(editorArea.getLineOfOffset(pos));
            editorArea.insert(prefix, lineStart);
            editorArea.setCaretPosition(lineStart + prefix.length());
            editorArea.requestFocus();
        } catch (Exception e) {
            // 如果出错，就在光标位置插入
            int pos = editorArea.getCaretPosition();
            editorArea.insert(prefix, pos);
            editorArea.requestFocus();
        }
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
     * 导出为 HTML（使用国际化文本）
     */
    private void exportToHtml() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle(I18nUtil.getMessage(MessageKeys.MARKDOWN_EXPORT_HTML));
        fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("HTML 文件", "html"));

        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                String html = convertMarkdownToHtml(editorArea.getText());
                java.io.File file = fileChooser.getSelectedFile();
                if (!file.getName().endsWith(".html")) {
                    file = new java.io.File(file.getAbsolutePath() + ".html");
                }
                java.nio.file.Files.writeString(file.toPath(), html);
                JOptionPane.showMessageDialog(this,
                        I18nUtil.getMessage(MessageKeys.MARKDOWN_EXPORT_SUCCESS),
                        I18nUtil.getMessage(MessageKeys.MARKDOWN_EXPORT_HTML),
                        JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this,
                        I18nUtil.getMessage(MessageKeys.MARKDOWN_EXPORT_FAILED) + ": " + ex.getMessage(),
                        I18nUtil.getMessage(MessageKeys.GENERAL_ERROR),
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /**
     * 复制 HTML 到剪贴板（使用国际化文本）
     */
    private void copyHtmlToClipboard() {
        String html = convertMarkdownToHtml(editorArea.getText());
        StringSelection selection = new StringSelection(html);
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, selection);
        JOptionPane.showMessageDialog(this,
                I18nUtil.getMessage(MessageKeys.MARKDOWN_HTML_COPIED),
                I18nUtil.getMessage(MessageKeys.MARKDOWN_COPY_HTML),
                JOptionPane.INFORMATION_MESSAGE);
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
            return "<html><body style='margin:0;padding:12px;font-size:10px;'></body></html>";
        }

        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html><html><head>");
        html.append("<meta charset='UTF-8'>");
        html.append("</head>");

        // 在 body 上使用 inline style 设置基本样式（不使用 StyleSheet）
        html.append("<body style='");
        html.append("margin:0;padding:8px;"); // 设置内边距，更紧凑
        html.append("font-family:sans-serif;");
        html.append("font-size:10px;");
        html.append("line-height:1.6;");
        html.append("color:").append(toHex(ModernColors.getTextPrimary())).append(";");
        html.append("background:").append(toHex(ModernColors.getCardBackgroundColor())).append(";");
        html.append("'>");

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
                    html.append("</pre>");
                    inCodeBlock = false;
                    codeLanguage = "";
                } else {
                    // 提取语言标识
                    codeLanguage = line.trim().substring(3).trim();
                    html.append("<pre style='").append(getCodeBlockStyle()).append("'");
                    if (!codeLanguage.isEmpty()) {
                        html.append(" class='language-").append(escapeHtml(codeLanguage)).append("'");
                    }
                    html.append(">");
                    inCodeBlock = true;
                }
                continue;
            }

            if (inCodeBlock) {
                // 去除行尾空白字符，避免渲染为方框
                String codeLine = line.replaceAll("\\s+$", "");
                html.append(escapeHtml(codeLine)).append("\n");
                continue;
            }

            // 表格处理
            if (line.trim().startsWith("|") && line.trim().endsWith("|")) {
                if (!inTable) {
                    html.append("<table style='").append(getTableStyle()).append("'>");
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

                String[] cells = line.split("\\|", -1); // -1 保留尾部空字符串
                // 去除首尾的空元素（| 开头和结尾导致的）
                int start = cells[0].trim().isEmpty() ? 1 : 0;
                int end = cells[cells.length - 1].trim().isEmpty() ? cells.length - 1 : cells.length;

                for (int j = start; j < end; j++) {
                    String cell = cells[j].trim();
                    if (isHeader) {
                        html.append("<th style='").append(getTableHeaderStyle()).append("'>").append(processInlineMarkdown(cell)).append("</th>");
                    } else {
                        html.append("<td style='").append(getTableCellStyle()).append("'>").append(processInlineMarkdown(cell)).append("</td>");
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
                html.append("<h1 style='").append(getHeadingStyle(1)).append("'>").append(processInlineMarkdown(line.substring(2))).append("</h1>");
            } else if (line.startsWith("## ")) {
                closeLists(html, inList, inOrderedList);
                inList = false;
                inOrderedList = false;
                html.append("<h2 style='").append(getHeadingStyle(2)).append("'>").append(processInlineMarkdown(line.substring(3))).append("</h2>");
            } else if (line.startsWith("### ")) {
                closeLists(html, inList, inOrderedList);
                inList = false;
                inOrderedList = false;
                html.append("<h3 style='").append(getHeadingStyle(3)).append("'>").append(processInlineMarkdown(line.substring(4))).append("</h3>");
            } else if (line.startsWith("#### ")) {
                closeLists(html, inList, inOrderedList);
                inList = false;
                inOrderedList = false;
                html.append("<h4 style='").append(getHeadingStyle(4)).append("'>").append(processInlineMarkdown(line.substring(5))).append("</h4>");
            } else if (line.startsWith("##### ")) {
                closeLists(html, inList, inOrderedList);
                inList = false;
                inOrderedList = false;
                html.append("<h5 style='").append(getHeadingStyle(5)).append("'>").append(processInlineMarkdown(line.substring(6))).append("</h5>");
            } else if (line.startsWith("###### ")) {
                closeLists(html, inList, inOrderedList);
                inList = false;
                inOrderedList = false;
                html.append("<h6 style='").append(getHeadingStyle(6)).append("'>").append(processInlineMarkdown(line.substring(7))).append("</h6>");
            }
            // 水平线
            else if (line.trim().equals("---") || line.trim().equals("***") || line.trim().equals("___")) {
                closeLists(html, inList, inOrderedList);
                inList = false;
                inOrderedList = false;
                html.append("<hr style='").append(getHrStyle()).append("'>");
            }
            // 任务列表 - 使用表格布局确保对齐，优化样式
            else if (line.trim().matches("^[-*]\\s+\\[[ xX]\\]\\s+.*")) {
                closeLists(html, inList, inOrderedList);
                inList = false;
                inOrderedList = false;

                boolean checked = line.toLowerCase().contains("[x]");
                String content = line.trim().replaceFirst("^[-*]\\s+\\[[ xX]\\]\\s+", "");

                // 使用内联样式彻底覆盖表格样式，移除所有边框
                html.append("<table class='task-item' cellpadding='0' cellspacing='0' border='0' style='border: 0; border-collapse: separate; margin: 0; padding: 0; margin-bottom: 0.25em; width: 100%; background: transparent;'>");
                html.append("<tr style='border: 0; border-top: 0; background: transparent;'>");
                html.append("<td style='border: 0; padding: 0; margin: 0; width: 18px; padding-right: 6px; background: transparent; vertical-align: middle;'>");
                html.append("<input type='checkbox' disabled");
                if (checked) html.append(" checked");
                html.append(">");
                html.append("</td>");
                html.append("<td style='border: 0; padding: 0; margin: 0; background: transparent; vertical-align: middle;'>");
                html.append(processInlineMarkdown(content));
                html.append("</td>");
                html.append("</tr>");
                html.append("</table>");
            }
            // 无序列表 - 使用表格布局实现完全左对齐
            else if (line.trim().startsWith("- ") || line.trim().startsWith("* ")) {
                closeLists(html, inList, inOrderedList);
                inList = false;
                inOrderedList = false;

                String content = line.substring(line.indexOf(" ") + 1);

                // 使用表格布局，与任务列表保持一致的左对齐
                html.append("<table class='list-item' cellpadding='0' cellspacing='0' border='0' style='border: 0; border-collapse: separate; margin: 0; padding: 0; margin-bottom: 0.25em; width: 100%; background: transparent;'>");
                html.append("<tr style='border: 0; border-top: 0; background: transparent;'>");
                html.append("<td style='border: 0; padding: 0; margin: 0; width: 1%; padding-right: 6px; background: transparent; vertical-align: top; white-space: nowrap;'>");
                html.append("•"); // Unicode bullet point
                html.append("</td>");
                html.append("<td style='border: 0; padding: 0; margin: 0; background: transparent; vertical-align: top;'>");
                html.append(processInlineMarkdown(content));
                html.append("</td>");
                html.append("</tr>");
                html.append("</table>");
            }
            // 有序列表 - 使用表格布局实现完全左对齐
            else if (line.trim().matches("^\\d+\\.\\s.*")) {
                closeLists(html, inList, inOrderedList);
                inList = false;
                inOrderedList = false;

                // 提取序号和内容
                String trimmed = line.trim();
                int dotIndex = trimmed.indexOf('.');
                String number = trimmed.substring(0, dotIndex + 1); // 包括点号
                String content = trimmed.substring(dotIndex + 1).trim();

                // 使用表格布局，与任务列表保持一致的左对齐
                html.append("<table class='list-item' cellpadding='0' cellspacing='0' border='0' style='border: 0; border-collapse: separate; margin: 0; padding: 0; margin-bottom: 0.25em; width: 100%; background: transparent;'>");
                html.append("<tr style='border: 0; border-top: 0; background: transparent;'>");
                html.append("<td style='border: 0; padding: 0; margin: 0; width: 1%; padding-right: 6px; background: transparent; vertical-align: top; text-align: left; white-space: nowrap;'>");
                html.append(number);
                html.append("</td>");
                html.append("<td style='border: 0; padding: 0; margin: 0; background: transparent; vertical-align: top;'>");
                html.append(processInlineMarkdown(content));
                html.append("</td>");
                html.append("</tr>");
                html.append("</table>");
            }
            // 引用
            else if (line.trim().startsWith("> ")) {
                closeLists(html, inList, inOrderedList);
                inList = false;
                inOrderedList = false;
                String content = line.substring(line.indexOf(">") + 1).trim();
                html.append("<blockquote style='").append(getBlockquoteStyle()).append("'>").append(processInlineMarkdown(content)).append("</blockquote>");
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
                html.append("<p style='margin:0 0 6px 0;'>").append(processInlineMarkdown(line)).append("</p>");
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

        // 删除线 ~~text~~ - 使用 strike 标签，Swing HTMLEditorKit 原生支持
        text = text.replaceAll("~~(.+?)~~", "<strike>$1</strike>");

        // 高亮 ==text== (部分编辑器支持)
        text = text.replaceAll("==(.+?)==", "<mark>$1</mark>");

        // 行内代码 `code` (必须在其他处理之后，避免代码中的特殊字符被处理)
        text = text.replaceAll("`(.+?)`", "<code style='" + getInlineCodeStyle() + "'>$1</code>");

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
        return HttpHtmlRenderer.escapeHtml(text);
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

    public void setEditable(boolean editable) {
        this.editable = editable;
        editorArea.setEditable(editable);
        if (toolbarPanel != null) {
            setComponentTreeEnabled(toolbarPanel, editable);
        }
    }

    private void setComponentTreeEnabled(Component component, boolean enabled) {
        component.setEnabled(enabled);
        if (component instanceof Container container) {
            for (Component child : container.getComponents()) {
                setComponentTreeEnabled(child, enabled);
            }
        }
    }

    /**
     * 添加文档变化监听器
     */
    public void addDocumentListener(DocumentListener listener) {
        changeListeners.add(listener);
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
