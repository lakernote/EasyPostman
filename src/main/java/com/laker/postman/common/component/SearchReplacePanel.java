package com.laker.postman.common.component;

import com.laker.postman.util.IconUtil;
import lombok.extern.slf4j.Slf4j;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rtextarea.SearchContext;
import org.fife.ui.rtextarea.SearchEngine;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

/**
 * 搜索替换面板组件
 * <p>
 * 类似 Postman 的搜索替换功能，按 Cmd+F 呼出，显示在右上角。
 * 支持搜索、替换、大小写敏感、正则表达式、整词匹配等功能。
 * </p>
 *
 * <h2>使用示例</h2>
 * <pre>{@code
 * RSyntaxTextArea textArea = new RSyntaxTextArea();
 * SearchReplacePanel searchPanel = new SearchReplacePanel(textArea);
 *
 * // 将搜索面板添加到父容器（通常是 JLayeredPane 或带 OverlayLayout 的面板）
 * JPanel container = new JPanel(new BorderLayout());
 * container.add(new RTextScrollPane(textArea), BorderLayout.CENTER);
 *
 * // 使用 OverlayLayout 或 JLayeredPane 来实现浮动效果
 * JLayeredPane layeredPane = new JLayeredPane();
 * layeredPane.setLayout(new OverlayLayout(layeredPane));
 * layeredPane.add(searchPanel, JLayeredPane.PALETTE_LAYER);
 * }</pre>
 */
@Slf4j
public class SearchReplacePanel extends JPanel {

    private final RSyntaxTextArea textArea;
    private final SearchTextField searchField;
    private final JTextField replaceField;
    private final JToggleButton toggleReplaceBtn;
    private final JPanel replacePanel;

    public SearchReplacePanel(RSyntaxTextArea textArea) {
        this.textArea = textArea;

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UIManager.getColor("Component.borderColor"), 1),
                new EmptyBorder(8, 8, 8, 8)
        ));
        setBackground(UIManager.getColor("Panel.background"));

        // 搜索面板
        JPanel searchPanel = new JPanel();
        searchPanel.setLayout(new BoxLayout(searchPanel, BoxLayout.X_AXIS));
        searchPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        // 展开/收起替换面板的按钮（放在最左边，类似 Postman）
        toggleReplaceBtn = new JToggleButton(IconUtil.createThemed("icons/expand.svg", 16, 16));
        toggleReplaceBtn.setToolTipText("Toggle Replace");
        toggleReplaceBtn.setFocusable(false);
        toggleReplaceBtn.setContentAreaFilled(false);
        toggleReplaceBtn.setBorderPainted(false);
        toggleReplaceBtn.setPreferredSize(new Dimension(24, 24));
        toggleReplaceBtn.setMaximumSize(new Dimension(24, 24));
        // actionListener 将在 replacePanel 创建后设置

        // 搜索输入框 - 使用 SearchTextField 复用大小写敏感和整词匹配功能
        searchField = new SearchTextField();
        searchField.setPreferredSize(new Dimension(200, 28));
        searchField.setMaximumSize(new Dimension(250, 28));
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

        // 查找按钮
        JButton findPrevBtn = createIconButton("icons/arrow-up.svg", "Previous (Shift+Enter)", e -> findPrevious());
        JButton findNextBtn = createIconButton("icons/arrow-down.svg", "Next (Enter)", e -> findNext());

        // 关闭按钮
        JButton closeBtn = createIconButton("icons/close.svg", "Close (Esc)", e -> hidePanel());


        // 组装搜索面板
        searchPanel.add(toggleReplaceBtn);
        searchPanel.add(Box.createHorizontalStrut(4));
        searchPanel.add(searchField);
        searchPanel.add(Box.createHorizontalStrut(4));
        searchPanel.add(findPrevBtn);
        searchPanel.add(findNextBtn);
        searchPanel.add(Box.createHorizontalStrut(4));
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
        spacer.setPreferredSize(new Dimension(20, 20));  // 与切换按钮宽度一致
        spacer.setMaximumSize(new Dimension(20, 20));

        // 替换输入框
        replaceField = new JTextField(20);
        replaceField.putClientProperty("JTextField.placeholderText", "Replace");
        replaceField.setPreferredSize(new Dimension(100, 28));
        replaceField.setMaximumSize(new Dimension(100, 28));
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
        JButton replaceBtn = createTextButton("Replace", e -> replace());
        JButton replaceAllBtn = createTextButton("Replace All", e -> replaceAll());

        // 组装替换面板
        replacePanel.add(spacer);  // 左侧占位符
        replacePanel.add(Box.createHorizontalStrut(4));
        replacePanel.add(replaceField);
        replacePanel.add(Box.createHorizontalStrut(4));
        replacePanel.add(replaceBtn);
        replacePanel.add(Box.createHorizontalStrut(4));
        replacePanel.add(replaceAllBtn);
        replacePanel.add(Box.createHorizontalGlue());

        add(Box.createVerticalStrut(4));
        add(replacePanel);

        // 设置 toggleReplaceBtn 的 actionListener（现在 replacePanel 已经初始化）
        toggleReplaceBtn.addActionListener(e -> {
            boolean selected = toggleReplaceBtn.isSelected();
            replacePanel.setVisible(selected);
            // 更新图标
            if (selected) {
                toggleReplaceBtn.setIcon(IconUtil.createThemed("icons/collapse.svg", 16, 16));
            } else {
                toggleReplaceBtn.setIcon(IconUtil.createThemed("icons/expand.svg", 16, 16));
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
        int minWidth = 300;  // 移除正则表达式按钮后，宽度可以更小
        // 高度根据是否显示替换面板动态调整
        int height = replacePanel.isVisible() ? 80 : 40;
        return new Dimension(Math.max(size.width, minWidth), Math.max(size.height, height));
    }

    /**
     * 创建图标按钮
     */
    private JButton createIconButton(String iconPath, String tooltip, java.awt.event.ActionListener listener) {
        JButton btn = new JButton(IconUtil.createThemed(iconPath, 16, 16));
        btn.setToolTipText(tooltip);
        btn.setFocusable(false);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setPreferredSize(new Dimension(24, 24));
        btn.setMaximumSize(new Dimension(24, 24));
        if (listener != null) {
            btn.addActionListener(listener);
        }
        return btn;
    }


    /**
     * 创建文本按钮
     */
    private JButton createTextButton(String text, java.awt.event.ActionListener listener) {
        JButton btn = new JButton(text);
        btn.setFocusable(false);
        btn.setPreferredSize(new Dimension(100, 28));
        btn.setMaximumSize(new Dimension(100, 28));
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
        toggleReplaceBtn.setIcon(IconUtil.createThemed("icons/expand.svg", 16, 16));
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
        toggleReplaceBtn.setIcon(IconUtil.createThemed("icons/collapse.svg", 16, 16));
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
            return;
        }

        SearchContext context = createSearchContext();
        context.setSearchForward(true);

        SearchEngine.find(textArea, context).wasFound();
    }

    /**
     * 查找上一个
     */
    private void findPrevious() {
        String searchText = searchField.getText();
        if (searchText.isEmpty()) {
            return;
        }

        SearchContext context = createSearchContext();
        context.setSearchForward(false);
        SearchEngine.find(textArea, context).wasFound();
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

        SearchEngine.replace(textArea, context).wasFound();
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

        SearchEngine.replaceAll(textArea, context).getCount();
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


}
