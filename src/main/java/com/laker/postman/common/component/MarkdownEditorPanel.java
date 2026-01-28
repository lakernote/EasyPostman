package com.laker.postman.common.component;

import com.laker.postman.common.constants.ModernColors;
import com.laker.postman.service.render.HttpHtmlRenderer;
import com.laker.postman.util.FontsUtil;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
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
 * 支持实时预览、工具栏、撤销/重做、查找替换、导出等功能
 */
public class MarkdownEditorPanel extends JPanel {
    private JTextArea editorArea;
    private JTextArea lineNumberArea;
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
        return "border-collapse:collapse;width:100%;margin:0 0 16px 0;border:1px solid " + toHex(ModernColors.getBorderLightColor()) + ";";
    }

    private String getTableCellStyle() {
        return "padding:8px 12px;border:1px solid " + toHex(ModernColors.getBorderLightColor()) + ";";
    }

    private String getTableHeaderStyle() {
        boolean isDark = ModernColors.isDarkTheme();
        String bgColor = isDark ? toHex(new Color(55, 58, 60)) : toHex(ModernColors.getHoverBackgroundColor());
        return getTableCellStyle() + "font-weight:600;background-color:" + bgColor + ";";
    }

    private String getCodeBlockStyle() {
        return "background-color:" + toHex(ModernColors.getConsoleTextAreaBg()) +
                ";padding:16px;overflow:auto;font-size:12px;line-height:1.5;border-radius:6px;border:1px solid " +
                toHex(ModernColors.getBorderLightColor()) +
                ";margin:0 0 16px 0;font-family:monospace;color:" +
                toHex(ModernColors.getConsoleText()) +
                ";display:block;white-space:pre;word-wrap:normal;";
    }

    private String getInlineCodeStyle() {
        boolean isDark = ModernColors.isDarkTheme();
        String bgColor = isDark ? toHex(new Color(65, 68, 70)) : toHex(ModernColors.getHoverBackgroundColor());
        String textColor = isDark ? "#8dd6f9" : toHex(ModernColors.ERROR_DARK);
        return "background-color:" + bgColor + ";color:" + textColor +
                ";padding:2px 6px;margin:0 2px;font-size:12px;border-radius:3px;font-family:monospace;";
    }

    private String getHeadingStyle(int level) {
        String dividerColor = toHex(ModernColors.getDividerBorderColor());
        switch (level) {
            case 1:
                return "font-size:28px;font-weight:600;margin:24px 0 16px 0;border-bottom:2px solid " + dividerColor + ";padding-bottom:0.3em;";
            case 2:
                return "font-size:24px;font-weight:600;margin:20px 0 14px 0;border-bottom:2px solid " + dividerColor + ";padding-bottom:0.3em;";
            case 3:
                return "font-size:20px;font-weight:600;margin:18px 0 12px 0;";
            case 4:
                return "font-size:16px;font-weight:600;margin:16px 0 10px 0;";
            case 5:
                return "font-size:14px;font-weight:600;margin:14px 0 8px 0;";
            case 6:
                return "font-size:13px;font-weight:600;margin:12px 0 8px 0;color:" + toHex(ModernColors.getTextHint()) + ";";
            default:
                return "";
        }
    }

    private String getBlockquoteStyle() {
        boolean isDark = ModernColors.isDarkTheme();
        String borderColor = isDark ? "#4a9eff" : toHex(ModernColors.ACCENT_LIGHT);
        String bgColor = isDark ? "rgba(74,158,255,0.08)" : "rgba(6,182,212,0.03)";
        return "padding:8px 16px;color:" + toHex(ModernColors.getTextSecondary()) +
                ";border-left:4px solid " + borderColor + ";background-color:" + bgColor +
                ";margin:0 0 16px 0;border-radius:0 4px 4px 0;";
    }

    private String getHrStyle() {
        return "height:2px;margin:24px 0;background-color:" + toHex(ModernColors.getDividerBorderColor()) + ";border:0;";
    }


    private void initUI() {
        setLayout(new BorderLayout());

        editorPanelRef = createEditorPanel();
        previewPanelRef = createPreviewPanel();

        splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, editorPanelRef, previewPanelRef);
        splitPane.setResizeWeight(0.5);
        splitPane.setBorder(null);

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
        toolbarContainer.setBorder(BorderFactory.createCompoundBorder(
                new MatteBorder(0, 0, 1, 0, ModernColors.getDividerBorderColor()),
                new EmptyBorder(3, 5, 3, 5)
        ));
        toolbarContainer.setOpaque(true);
        toolbarContainer.setBackground(UIManager.getColor("Panel.background"));

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

        JMenuItem findItem = new JMenuItem("🔍 " + I18nUtil.getMessage(MessageKeys.MARKDOWN_FIND));
        findItem.setToolTipText("Ctrl+F");
        findItem.addActionListener(e -> showFindDialog());

        JMenuItem exportItem = new JMenuItem("💾 " + I18nUtil.getMessage(MessageKeys.MARKDOWN_EXPORT_HTML));
        exportItem.addActionListener(e -> exportToHtml());

        JMenuItem copyItem = new JMenuItem("📋 " + I18nUtil.getMessage(MessageKeys.MARKDOWN_COPY_HTML));
        copyItem.addActionListener(e -> copyHtmlToClipboard());

        menu.add(findItem);
        menu.addSeparator();
        menu.add(exportItem);
        menu.add(copyItem);

        return menu;
    }

    private JButton createFlatButton(String text, String tooltip, ActionListener action) {
        JButton button = new JButton(text);
        button.setToolTipText(tooltip);
        button.setFont(FontsUtil.getDefaultFontWithOffset(Font.PLAIN, 0));
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setContentAreaFilled(false);
        button.setMargin(new Insets(4, 8, 4, 8));
        button.setPreferredSize(null);
        button.setMinimumSize(new Dimension(28, 28));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

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

    private JButton createFlatButton(String text, String tooltip, String prefix, String suffix) {
        return createFlatButton(text, tooltip, e -> insertFormat(prefix, suffix));
    }

    private JButton createFlatActionButton(String text, String tooltip, Runnable action) {
        return createFlatButton(text, tooltip, e -> action.run());
    }

    private void styleToggleButton(JToggleButton button) {
        button.setFont(FontsUtil.getDefaultFontWithOffset(Font.PLAIN, 0));
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setContentAreaFilled(false);
        button.setMargin(new Insets(4, 10, 4, 10));
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

    private Component createVerticalDivider() {
        JSeparator separator = new JSeparator(SwingConstants.VERTICAL);
        separator.setPreferredSize(new Dimension(1, 20));
        separator.setForeground(new Color(220, 220, 220));
        return separator;
    }

    /**
     * 创建编辑器面板
     */
    private JPanel createEditorPanel() {
        JPanel panel = new JPanel(new BorderLayout());

        // 行号区域
        lineNumberArea = new JTextArea("1");
        lineNumberArea.setFont(FontsUtil.getDefaultFontWithOffset(Font.PLAIN, 0));
        lineNumberArea.setBackground(new Color(240, 240, 240));
        lineNumberArea.setForeground(Color.GRAY);
        lineNumberArea.setEditable(false);
        lineNumberArea.setBorder(new EmptyBorder(10, 5, 10, 5));
        lineNumberArea.setPreferredSize(new Dimension(40, Integer.MAX_VALUE));

        // 编辑器区域
        editorArea = new JTextArea();
        editorArea.setFont(FontsUtil.getDefaultFontWithOffset(Font.PLAIN, 0));
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

        previewPane = new JTextPane();
        previewPane.setContentType("text/html");
        previewPane.setEditable(false);
        previewPane.setBorder(new EmptyBorder(5, 5, 5, 5));
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

        JLabel statusLabel = new JLabel(I18nUtil.getMessage(MessageKeys.MARKDOWN_READY));
        statusLabel.setFont(FontsUtil.getDefaultFontWithOffset(Font.PLAIN, -1));
        statusLabel.setForeground(Color.GRAY);
        statusBar.add(statusLabel);

        // 字数统计
        JLabel wordCountLabel = new JLabel(I18nUtil.getMessage(MessageKeys.MARKDOWN_STATUS_WORDS) + ": 0");
        wordCountLabel.setFont(FontsUtil.getDefaultFontWithOffset(Font.PLAIN, -1));
        wordCountLabel.setForeground(Color.GRAY);
        statusBar.add(new JSeparator(SwingConstants.VERTICAL));
        statusBar.add(wordCountLabel);

        // 行列号
        JLabel positionLabel = new JLabel(I18nUtil.getMessage(MessageKeys.MARKDOWN_STATUS_LINE) + ": 1, " +
                I18nUtil.getMessage(MessageKeys.MARKDOWN_STATUS_COLUMN) + ": 1");
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
                positionLabel.setText(String.format("%s: %d, %s: %d",
                        I18nUtil.getMessage(MessageKeys.MARKDOWN_STATUS_LINE), line + 1,
                        I18nUtil.getMessage(MessageKeys.MARKDOWN_STATUS_COLUMN), col + 1));

                String text = editorArea.getText();
                wordCountLabel.setText(String.format("%s: %d | %s: %d",
                        I18nUtil.getMessage(MessageKeys.MARKDOWN_STATUS_WORDS), text.split("\\s+").length,
                        I18nUtil.getMessage(MessageKeys.MARKDOWN_STATUS_CHARS), text.length()));
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

        // Ctrl+H - 替换
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_H, InputEvent.CTRL_DOWN_MASK), "replace");
        actionMap.put("replace", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                showFindDialog();
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
     * 显示查找对话框（使用国际化文本）
     */
    private void showFindDialog() {
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this),
                I18nUtil.getMessage(MessageKeys.MARKDOWN_FIND_TITLE), false); // 改为非模态对话框
        dialog.setLayout(new BorderLayout(10, 10));
        dialog.getRootPane().setBorder(new EmptyBorder(10, 10, 10, 10));

        // 主面板
        JPanel mainPanel = new JPanel(new BorderLayout(5, 10));

        // 输入面板
        JPanel inputPanel = new JPanel(new GridLayout(2, 2, 5, 5));
        inputPanel.add(new JLabel(I18nUtil.getMessage(MessageKeys.MARKDOWN_FIND_LABEL)));
        JTextField findField = new JTextField(20);
        inputPanel.add(findField);

        inputPanel.add(new JLabel(I18nUtil.getMessage(MessageKeys.MARKDOWN_REPLACE_LABEL)));
        JTextField replaceField = new JTextField(20);
        inputPanel.add(replaceField);

        mainPanel.add(inputPanel, BorderLayout.NORTH);

        // 选项面板
        JPanel optionsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JCheckBox caseSensitiveCheck = new JCheckBox(I18nUtil.getMessage(MessageKeys.MARKDOWN_CASE_SENSITIVE), false);
        JCheckBox wrapSearchCheck = new JCheckBox(I18nUtil.getMessage(MessageKeys.MARKDOWN_WRAP_SEARCH), true);
        optionsPanel.add(caseSensitiveCheck);
        optionsPanel.add(wrapSearchCheck);
        mainPanel.add(optionsPanel, BorderLayout.CENTER);

        dialog.add(mainPanel, BorderLayout.CENTER);

        // 按钮面板
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 5));
        JButton findNextButton = new JButton(I18nUtil.getMessage(MessageKeys.MARKDOWN_FIND_NEXT));
        JButton findPrevButton = new JButton(I18nUtil.getMessage(MessageKeys.MARKDOWN_FIND_PREV));
        JButton replaceButton = new JButton(I18nUtil.getMessage(MessageKeys.MARKDOWN_REPLACE));
        JButton replaceAllButton = new JButton(I18nUtil.getMessage(MessageKeys.MARKDOWN_REPLACE_ALL));
        JButton closeButton = new JButton(I18nUtil.getMessage(MessageKeys.MARKDOWN_CLOSE));

        // 查找下一个
        findNextButton.addActionListener(e -> {
            performFind(findField.getText(), true, caseSensitiveCheck.isSelected(),
                    wrapSearchCheck.isSelected(), dialog);
        });

        // 查找上一个
        findPrevButton.addActionListener(e -> {
            performFind(findField.getText(), false, caseSensitiveCheck.isSelected(),
                    wrapSearchCheck.isSelected(), dialog);
        });

        // 替换
        replaceButton.addActionListener(e -> {
            String selected = editorArea.getSelectedText();
            String find = findField.getText();
            boolean caseSensitive = caseSensitiveCheck.isSelected();

            if (selected != null && !selected.isEmpty()) {
                boolean matches = caseSensitive ? find.equals(selected) :
                        find.equalsIgnoreCase(selected);
                if (matches) {
                    editorArea.replaceSelection(replaceField.getText());
                    // 替换后自动查找下一个
                    performFind(findField.getText(), true, caseSensitive,
                            wrapSearchCheck.isSelected(), dialog);
                }
            }
        });

        // 全部替换
        replaceAllButton.addActionListener(e -> {
            String text = editorArea.getText();
            String find = findField.getText();
            String replace = replaceField.getText();

            if (find.isEmpty()) {
                return;
            }

            int count = 0;
            if (caseSensitiveCheck.isSelected()) {
                // 区分大小写
                String newText = text;
                int index = 0;
                while ((index = newText.indexOf(find, index)) != -1) {
                    count++;
                    index += find.length();
                }
                newText = text.replace(find, replace);
                editorArea.setText(newText);
            } else {
                // 不区分大小写
                StringBuilder result = new StringBuilder();
                String lowerText = text.toLowerCase();
                String lowerFind = find.toLowerCase();
                int lastIndex = 0;
                int index;

                while ((index = lowerText.indexOf(lowerFind, lastIndex)) != -1) {
                    result.append(text, lastIndex, index);
                    result.append(replace);
                    lastIndex = index + find.length();
                    count++;
                }
                result.append(text.substring(lastIndex));
                editorArea.setText(result.toString());
            }

            JOptionPane.showMessageDialog(dialog,
                    I18nUtil.getMessage(MessageKeys.MARKDOWN_REPLACE_COMPLETE) + " (" + count + ")",
                    I18nUtil.getMessage(MessageKeys.MARKDOWN_FIND_TITLE),
                    JOptionPane.INFORMATION_MESSAGE);
        });

        closeButton.addActionListener(e -> dialog.dispose());

        buttonPanel.add(findPrevButton);
        buttonPanel.add(findNextButton);
        buttonPanel.add(replaceButton);
        buttonPanel.add(replaceAllButton);
        buttonPanel.add(closeButton);

        dialog.add(buttonPanel, BorderLayout.SOUTH);

        // 设置默认按钮
        dialog.getRootPane().setDefaultButton(findNextButton);

        // 回车键查找下一个
        findField.addActionListener(e -> findNextButton.doClick());

        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);

        // 聚焦到查找框
        SwingUtilities.invokeLater(findField::requestFocus);
    }

    /**
     * 执行查找操作
     *
     * @param searchText    要查找的文本
     * @param forward       true=向前查找，false=向后查找
     * @param caseSensitive 是否区分大小写
     * @param wrapAround    是否循环查找
     * @param parentDialog  父对话框，用于显示消息
     */
    private void performFind(String searchText, boolean forward, boolean caseSensitive,
                             boolean wrapAround, JDialog parentDialog) {
        if (searchText == null || searchText.isEmpty()) {
            return;
        }

        String text = editorArea.getText();
        String searchInText = caseSensitive ? text : text.toLowerCase();
        String searchFor = caseSensitive ? searchText : searchText.toLowerCase();

        int currentPos = forward ? editorArea.getSelectionEnd() : editorArea.getSelectionStart();
        int index = -1;

        if (forward) {
            // 向前查找
            index = searchInText.indexOf(searchFor, currentPos);

            // 如果没找到且允许循环，从头开始
            if (index == -1 && wrapAround && currentPos > 0) {
                index = searchInText.indexOf(searchFor, 0);
            }
        } else {
            // 向后查找
            index = searchInText.lastIndexOf(searchFor, currentPos - 1);

            // 如果没找到且允许循环，从末尾开始
            if (index == -1 && wrapAround && currentPos < text.length()) {
                index = searchInText.lastIndexOf(searchFor, text.length());
            }
        }

        if (index >= 0) {
            // 找到了，选中并滚动到可见区域
            editorArea.setSelectionStart(index);
            editorArea.setSelectionEnd(index + searchText.length());
            editorArea.getCaret().setSelectionVisible(true);

            // 滚动到选中的文本
            try {
                Rectangle rect = editorArea.modelToView(index);
                if (rect != null) {
                    editorArea.scrollRectToVisible(rect);
                }
            } catch (Exception ex) {
                // 忽略滚动错误
            }

            editorArea.requestFocus();
        } else {
            // 没找到
            JOptionPane.showMessageDialog(parentDialog,
                    I18nUtil.getMessage(MessageKeys.MARKDOWN_NOT_FOUND),
                    I18nUtil.getMessage(MessageKeys.MARKDOWN_FIND_TITLE),
                    JOptionPane.INFORMATION_MESSAGE);
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
            return "<html><body style='margin:0;padding:0;font-size:14px;'></body></html>";
        }

        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html><html><head>");
        html.append("<meta charset='UTF-8'>");
        html.append("</head>");

        // 在 body 上使用 inline style 设置基本样式（不使用 StyleSheet）
        html.append("<body style='");
        html.append("margin:0;padding:0;");
        html.append("font-family:sans-serif;");
        html.append("font-size:14px;");
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
                    html.append("</code></pre>");
                    inCodeBlock = false;
                    codeLanguage = "";
                } else {
                    // 提取语言标识
                    codeLanguage = line.trim().substring(3).trim();
                    html.append("<pre style='").append(getCodeBlockStyle()).append("'>");
                    // 给 code 标签添加 style，移除可能的边框和背景
                    html.append("<code style='background:transparent;border:0;padding:0;margin:0;display:block;'");
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
