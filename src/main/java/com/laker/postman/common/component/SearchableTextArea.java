package com.laker.postman.common.component;

import lombok.Getter;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rtextarea.RTextScrollPane;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;

/**
 * 带搜索替换功能的文本编辑器容器
 * <p>
 * 将 RSyntaxTextArea 和 SearchReplacePanel 组合在一起，
 * 实现类似 Postman 的搜索功能（Cmd+F 呼出，浮动在右上角）
 * </p>
 *
 * <h2>使用示例</h2>
 * <pre>{@code
 * // 创建带搜索功能的文本编辑器
 * SearchableTextArea searchableArea = new SearchableTextArea();
 * RSyntaxTextArea textArea = searchableArea.getTextArea();
 *
 * // 配置文本编辑器
 * textArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JAVASCRIPT);
 *
 * // 将容器添加到界面
 * panel.add(searchableArea, BorderLayout.CENTER);
 *
 * // Cmd+F / Ctrl+F 会自动触发搜索
 * // Cmd+Shift+F / Ctrl+Shift+F 会自动触发替换
 * }</pre>
 */
public class SearchableTextArea extends JPanel {

    @Getter
    private final RSyntaxTextArea textArea;
    private final RTextScrollPane scrollPane;
    private final SearchReplacePanel searchPanel;
    private final JPanel overlayPanel;

    public SearchableTextArea(RSyntaxTextArea textArea) {
        this.textArea = textArea;

        setLayout(new BorderLayout());

        // 创建滚动面板
        scrollPane = new RTextScrollPane(textArea);

        // 创建搜索替换面板
        searchPanel = new SearchReplacePanel(textArea);

        // 监听搜索面板大小变化，自动更新位置
        searchPanel.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                updateSearchPanelPosition();
            }
        });

        // 创建 overlay 面板用于浮动搜索框
        overlayPanel = new JPanel(null) {
            @Override
            public boolean isOptimizedDrawingEnabled() {
                return false; // 允许组件重叠
            }
        };
        overlayPanel.setOpaque(false);

        // 使用 JLayeredPane 实现浮动效果
        JLayeredPane layeredPane = new JLayeredPane() {
            @Override
            public void doLayout() {
                super.doLayout();
                // 确保滚动面板填充整个 layeredPane
                if (getComponentCount() > 0) {
                    scrollPane.setBounds(0, 0, getWidth(), getHeight());
                    overlayPanel.setBounds(0, 0, getWidth(), getHeight());
                }
            }
        };

        // 底层：文本编辑器
        layeredPane.add(scrollPane, JLayeredPane.DEFAULT_LAYER);

        // 上层：搜索面板（浮动在右上角）
        layeredPane.add(overlayPanel, JLayeredPane.PALETTE_LAYER);
        overlayPanel.add(searchPanel);

        add(layeredPane, BorderLayout.CENTER);

        // 监听大小变化，调整搜索面板位置
        layeredPane.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                updateSearchPanelPosition();
            }
        });

        // 注册快捷键
        registerKeyBindings();
    }

    /**
     * 注册快捷键
     */
    private void registerKeyBindings() {
        // Cmd+F / Ctrl+F - 显示搜索
        KeyStroke findKey = KeyStroke.getKeyStroke(KeyEvent.VK_F,
                Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx());
        textArea.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(findKey, "showSearch");
        textArea.getActionMap().put("showSearch", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                searchPanel.showSearch();
                updateSearchPanelPosition();
            }
        });

        // Cmd+Shift+F / Ctrl+Shift+F - 显示替换
        KeyStroke replaceKey = KeyStroke.getKeyStroke(KeyEvent.VK_F,
                Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx() | java.awt.event.InputEvent.SHIFT_DOWN_MASK);

        textArea.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(replaceKey, "showReplace");
        textArea.getActionMap().put("showReplace", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                searchPanel.showReplace();
                updateSearchPanelPosition();
            }
        });
    }

    /**
     * 更新搜索面板位置（右上角）
     */
    private void updateSearchPanelPosition() {
        if (!searchPanel.isVisible()) {
            return;
        }

        // 先让面板自行计算最佳尺寸
        searchPanel.revalidate();
        Dimension panelSize = searchPanel.getPreferredSize();

        // 确保面板有足够的宽度和高度
        int panelWidth = Math.max(panelSize.width, 300);  // 最小宽度 300px
        int panelHeight = panelSize.height;

        // 计算右上角位置
        int x = Math.max(10, overlayPanel.getWidth() - panelWidth - 10);
        int y = 10;

        // 设置面板位置和大小
        searchPanel.setBounds(x, y, panelWidth, panelHeight);
        overlayPanel.setSize(overlayPanel.getParent().getSize());
        overlayPanel.revalidate();
        overlayPanel.repaint();
    }
}
