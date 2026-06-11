package com.laker.postman.common.component;

import com.laker.postman.util.UiI18n;
import com.laker.postman.util.UiMessageKeys;
import lombok.Getter;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rtextarea.RTextArea;

import javax.swing.*;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.text.DefaultEditorKit;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Locale;

/**
 * 带搜索替换功能的文本编辑器容器
 * <p>
 * 将 RSyntaxTextArea 和 SearchReplacePanel 组合在一起，
 * 实现类似 Postman 的搜索功能（Cmd+F 呼出，浮动在右上角）
 * </p>
 *
 * <h2>使用示例</h2>
 * <pre>{@code
 * // 创建带搜索替换功能的文本编辑器（默认启用替换功能）
 * SearchableTextArea searchableArea = new SearchableTextArea(textArea);
 *
 * // 创建仅搜索功能的文本编辑器（禁用替换功能）
 * SearchableTextArea searchOnlyArea = new SearchableTextArea(textArea, false);
 *
 * // 配置文本编辑器
 * textArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JAVASCRIPT);
 *
 * // 将容器添加到界面
 * panel.add(searchableArea, BorderLayout.CENTER);
 *
 * // Cmd+F / Ctrl+F 会自动触发搜索
 * // Cmd+Shift+F / Ctrl+Shift+F 会自动触发替换（仅在启用替换功能时可用）
 * }</pre>
 */
public class SearchableTextArea extends JPanel {
    private static final String EDITOR_SHORTCUT_HINTS_INSTALLED = "easyPostman.editorShortcutHintsInstalled";
    private static final String POPUP_MESSAGE_KEY_PROPERTY = "easyPostman.editorPopupMessageKey";

    @Getter
    private final RSyntaxTextArea textArea;
    private final SyntaxEditorScrollPane scrollPane;
    private final SearchReplacePanel searchPanel;
    private final JPanel overlayPanel;
    private final boolean enableReplace;
    private JsonCopyTargetResolver.CopyTarget currentJsonCopyTarget;
    private int jsonContextPopupOffset = -1;

    /**
     * 创建带搜索替换功能的文本编辑器（默认启用替换功能）
     */
    public SearchableTextArea(RSyntaxTextArea textArea) {
        this(textArea, true);
    }

    /**
     * 创建带搜索功能的文本编辑器
     *
     * @param textArea      文本编辑器
     * @param enableReplace 是否启用替换功能
     */
    public SearchableTextArea(RSyntaxTextArea textArea, boolean enableReplace) {
        this.textArea = textArea;
        this.enableReplace = enableReplace;

        setLayout(new BorderLayout());

        scrollPane = new SyntaxEditorScrollPane(textArea);

        // 创建搜索替换面板
        searchPanel = new SearchReplacePanel(textArea, enableReplace);

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

        installLineNavigationKeyBindings();
        installEditorShortcutHints();

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
        KeyStroke findKey = KeyStroke.getKeyStroke(KeyEvent.VK_F, menuShortcutMask());
        textArea.getInputMap(JComponent.WHEN_FOCUSED).put(findKey, "showSearch");
        textArea.getActionMap().put("showSearch", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                searchPanel.showSearch();
                updateSearchPanelPosition();
            }
        });

        // Cmd+Shift+F / Ctrl+Shift+F - 显示替换（仅在启用替换功能时注册）
        if (enableReplace) {
            KeyStroke replaceKey = KeyStroke.getKeyStroke(KeyEvent.VK_F,
                    menuShortcutMask() | java.awt.event.InputEvent.SHIFT_DOWN_MASK);

            textArea.getInputMap(JComponent.WHEN_FOCUSED).put(replaceKey, "showReplace");
            textArea.getActionMap().put("showReplace", new AbstractAction() {
                @Override
                public void actionPerformed(java.awt.event.ActionEvent e) {
                    searchPanel.showReplace();
                    updateSearchPanelPosition();
                }
            });
        }
    }

    private int menuShortcutMask() {
        if (GraphicsEnvironment.isHeadless()) {
            return InputEvent.CTRL_DOWN_MASK;
        }
        return Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();
    }

    private void installLineNavigationKeyBindings() {
        InputMap inputMap = textArea.getInputMap(JComponent.WHEN_FOCUSED);
        bind(inputMap, KeyEvent.VK_HOME, 0, DefaultEditorKit.beginLineAction);
        bind(inputMap, KeyEvent.VK_END, 0, DefaultEditorKit.endLineAction);
        bind(inputMap, KeyEvent.VK_HOME, InputEvent.SHIFT_DOWN_MASK, DefaultEditorKit.selectionBeginLineAction);
        bind(inputMap, KeyEvent.VK_END, InputEvent.SHIFT_DOWN_MASK, DefaultEditorKit.selectionEndLineAction);
        bind(inputMap, KeyEvent.VK_LEFT, InputEvent.META_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK,
                DefaultEditorKit.selectionBeginLineAction);
        bind(inputMap, KeyEvent.VK_RIGHT, InputEvent.META_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK,
                DefaultEditorKit.selectionEndLineAction);
    }

    private void bind(InputMap inputMap, int keyCode, int modifiers, String actionName) {
        inputMap.put(KeyStroke.getKeyStroke(keyCode, modifiers), actionName);
    }

    private void installEditorShortcutHints() {
        if (Boolean.TRUE.equals(textArea.getClientProperty(EDITOR_SHORTCUT_HINTS_INSTALLED))) {
            return;
        }

        JPopupMenu sourceMenu = textArea.getPopupMenu();
        if (sourceMenu == null) {
            return;
        }

        JPopupMenu popupMenu = createEnhancedEditorPopupMenu(sourceMenu);
        textArea.setPopupMenu(popupMenu);
        installPopupOffsetTracker();
        popupMenu.addPopupMenuListener(new PopupMenuListener() {
            @Override
            public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
                refreshEnhancedEditorPopupMenu(popupMenu);
            }

            @Override
            public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
                clearJsonPopupTarget();
            }

            @Override
            public void popupMenuCanceled(PopupMenuEvent e) {
                clearJsonPopupTarget();
            }
        });
        refreshEnhancedEditorPopupMenu(popupMenu);
        textArea.putClientProperty(EDITOR_SHORTCUT_HINTS_INSTALLED, Boolean.TRUE);
    }

    private JPopupMenu createEnhancedEditorPopupMenu(JPopupMenu sourceMenu) {
        JPopupMenu popupMenu = new JPopupMenu();
        JMenuItem copyKeyItem = createMessageMenuItem(UiMessageKeys.EDITOR_POPUP_COPY_KEY);
        copyKeyItem.addActionListener(e -> copyJsonTarget(true));
        JMenuItem copyValueItem = createMessageMenuItem(UiMessageKeys.EDITOR_POPUP_COPY_VALUE);
        copyValueItem.addActionListener(e -> copyJsonTarget(false));
        popupMenu.add(copyKeyItem);
        popupMenu.add(copyValueItem);
        popupMenu.addSeparator();

        if (textArea.isEditable()) {
            addIfPresent(popupMenu, findActionMenuItem(sourceMenu, RTextArea.UNDO_ACTION));
            addIfPresent(popupMenu, findActionMenuItem(sourceMenu, RTextArea.REDO_ACTION));
            popupMenu.addSeparator();
            addIfPresent(popupMenu, findActionMenuItem(sourceMenu, RTextArea.CUT_ACTION));
            addIfPresent(popupMenu, findActionMenuItem(sourceMenu, RTextArea.COPY_ACTION));
            addIfPresent(popupMenu, findActionMenuItem(sourceMenu, RTextArea.PASTE_ACTION));
            addIfPresent(popupMenu, findActionMenuItem(sourceMenu, RTextArea.DELETE_ACTION));
            popupMenu.addSeparator();
        } else {
            addIfPresent(popupMenu, findActionMenuItem(sourceMenu, RTextArea.COPY_ACTION));
        }
        JMenuItem copyAllItem = createMessageMenuItem(UiMessageKeys.EDITOR_POPUP_COPY_ALL);
        copyAllItem.addActionListener(e -> copyAllText());
        popupMenu.add(copyAllItem);
        addIfPresent(popupMenu, findActionMenuItem(sourceMenu, RTextArea.SELECT_ALL_ACTION));
        JMenu foldingMenu = findFoldingMenu(sourceMenu);
        if (foldingMenu != null) {
            popupMenu.addSeparator();
            popupMenu.add(foldingMenu);
        }
        popupMenu.addSeparator();
        popupMenu.add(disabledHint(editorLineSelectionShortcutHint()));
        return popupMenu;
    }

    private void refreshEnhancedEditorPopupMenu(JPopupMenu popupMenu) {
        currentJsonCopyTarget = resolveCurrentJsonCopyTarget();
        for (Component component : popupMenu.getComponents()) {
            if (component instanceof JMenu menu) {
                localizeFoldingMenu(menu);
            } else if (component instanceof JMenuItem menuItem) {
                localizeEditorMenuItem(menuItem);
                refreshEditorMenuItemState(menuItem);
            }
        }
    }

    private JMenuItem findActionMenuItem(JPopupMenu menu, int actionConstant) {
        Action action = RTextArea.getAction(actionConstant);
        if (action == null) {
            return null;
        }
        for (Component component : menu.getComponents()) {
            if (component instanceof JMenuItem menuItem && menuItem.getAction() == action) {
                return menuItem;
            }
        }
        return new JMenuItem(action);
    }

    private JMenu findFoldingMenu(JPopupMenu menu) {
        for (Component component : menu.getComponents()) {
            if (component instanceof JMenu foldingMenu) {
                return foldingMenu;
            }
        }
        return null;
    }

    private void addIfPresent(JPopupMenu popupMenu, JMenuItem menuItem) {
        if (menuItem != null) {
            popupMenu.add(menuItem);
        }
    }

    private JMenuItem createMessageMenuItem(String messageKey) {
        JMenuItem menuItem = new JMenuItem(UiI18n.get(messageKey));
        menuItem.putClientProperty(POPUP_MESSAGE_KEY_PROPERTY, messageKey);
        return menuItem;
    }

    private void localizeEditorMenuItem(JMenuItem menuItem) {
        Object messageKey = menuItem.getClientProperty(POPUP_MESSAGE_KEY_PROPERTY);
        if (messageKey instanceof String key) {
            menuItem.setText(UiI18n.get(key));
            return;
        }

        Action action = menuItem.getAction();
        if (action == RTextArea.getAction(RTextArea.UNDO_ACTION)) {
            menuItem.setText(UiI18n.get(UiMessageKeys.EDITOR_POPUP_UNDO));
        } else if (action == RTextArea.getAction(RTextArea.REDO_ACTION)) {
            menuItem.setText(UiI18n.get(UiMessageKeys.EDITOR_POPUP_REDO));
        } else if (action == RTextArea.getAction(RTextArea.CUT_ACTION)) {
            menuItem.setText(UiI18n.get(UiMessageKeys.EDITOR_POPUP_CUT));
        } else if (action == RTextArea.getAction(RTextArea.COPY_ACTION)) {
            menuItem.setText(UiI18n.get(UiMessageKeys.EDITOR_POPUP_COPY_SELECTED));
        } else if (action == RTextArea.getAction(RTextArea.PASTE_ACTION)) {
            menuItem.setText(UiI18n.get(UiMessageKeys.EDITOR_POPUP_PASTE));
        } else if (action == RTextArea.getAction(RTextArea.DELETE_ACTION)) {
            menuItem.setText(UiI18n.get(UiMessageKeys.EDITOR_POPUP_DELETE));
        } else if (action == RTextArea.getAction(RTextArea.SELECT_ALL_ACTION)) {
            menuItem.setText(UiI18n.get(UiMessageKeys.EDITOR_POPUP_SELECT_ALL));
        }
    }

    private void refreshEditorMenuItemState(JMenuItem menuItem) {
        Object messageKey = menuItem.getClientProperty(POPUP_MESSAGE_KEY_PROPERTY);
        if (UiMessageKeys.EDITOR_POPUP_COPY_KEY.equals(messageKey)) {
            menuItem.setEnabled(currentJsonCopyTarget != null && currentJsonCopyTarget.key() != null);
        } else if (UiMessageKeys.EDITOR_POPUP_COPY_VALUE.equals(messageKey)) {
            menuItem.setEnabled(currentJsonCopyTarget != null && currentJsonCopyTarget.value() != null);
        } else if (UiMessageKeys.EDITOR_POPUP_COPY_ALL.equals(messageKey)) {
            menuItem.setEnabled(textArea.getDocument().getLength() > 0);
        }
    }

    private void localizeFoldingMenu(JMenu foldingMenu) {
        foldingMenu.setText(UiI18n.get(UiMessageKeys.EDITOR_POPUP_FOLDING));
        setMenuItemText(foldingMenu, 0, UiMessageKeys.EDITOR_POPUP_TOGGLE_CURRENT_FOLD);
        setMenuItemText(foldingMenu, 1, UiMessageKeys.EDITOR_POPUP_COLLAPSE_COMMENT_FOLDS);
        setMenuItemText(foldingMenu, 2, UiMessageKeys.EDITOR_POPUP_COLLAPSE_ALL_FOLDS);
        setMenuItemText(foldingMenu, 3, UiMessageKeys.EDITOR_POPUP_EXPAND_ALL_FOLDS);
    }

    private void setMenuItemText(JMenu menu, int index, String messageKey) {
        if (index < menu.getItemCount()) {
            JMenuItem item = menu.getItem(index);
            if (item != null) {
                item.setText(UiI18n.get(messageKey));
            }
        }
    }

    private String editorLineSelectionShortcutHint() {
        if (isMac()) {
            return UiI18n.get(UiMessageKeys.EDITOR_SHORTCUTS_SELECT_LINE_MAC);
        }
        return UiI18n.get(UiMessageKeys.EDITOR_SHORTCUTS_SELECT_LINE_DEFAULT);
    }

    private boolean isMac() {
        return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("mac");
    }

    private void installPopupOffsetTracker() {
        textArea.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                rememberJsonPopupOffset(e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                rememberJsonPopupOffset(e);
            }
        });
    }

    private void rememberJsonPopupOffset(MouseEvent e) {
        if (!e.isPopupTrigger()) {
            return;
        }
        int offset = textArea.viewToModel2D(e.getPoint());
        rememberJsonPopupOffset(offset);
    }

    void rememberJsonPopupOffset(int offset) {
        if (offset >= 0 && offset <= textArea.getDocument().getLength()) {
            jsonContextPopupOffset = offset;
            if (!isOffsetInsideSelection(offset)) {
                textArea.setCaretPosition(offset);
            }
        }
    }

    private boolean isOffsetInsideSelection(int offset) {
        int selectionStart = textArea.getSelectionStart();
        int selectionEnd = textArea.getSelectionEnd();
        return selectionStart != selectionEnd && offset >= selectionStart && offset < selectionEnd;
    }

    private JsonCopyTargetResolver.CopyTarget resolveCurrentJsonCopyTarget() {
        String text = textArea.getText();
        if (text == null || text.isEmpty()) {
            return null;
        }
        int offset = jsonContextPopupOffset >= 0 ? jsonContextPopupOffset : textArea.getCaretPosition();
        return JsonCopyTargetResolver.resolve(text, offset).orElse(null);
    }

    private void copyJsonTarget(boolean copyKey) {
        JsonCopyTargetResolver.CopyTarget target = currentJsonCopyTarget != null
                ? currentJsonCopyTarget
                : resolveCurrentJsonCopyTarget();
        if (target == null) {
            return;
        }

        copyTextToClipboard(copyKey ? target.key() : target.value());
    }

    private void copyAllText() {
        copyTextToClipboard(textArea.getText());
    }

    private void copyTextToClipboard(String text) {
        if (text == null || text.isEmpty()) {
            return;
        }
        try {
            Toolkit.getDefaultToolkit().getSystemClipboard()
                    .setContents(new StringSelection(text), null);
        } catch (RuntimeException ignored) {
            // Clipboard may be unavailable in headless or restricted environments.
        }
    }

    private void clearJsonPopupTarget() {
        currentJsonCopyTarget = null;
        jsonContextPopupOffset = -1;
    }

    private JMenuItem disabledHint(String text) {
        JMenuItem item = new JMenuItem(text);
        item.setEnabled(false);
        return item;
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
        int panelWidth = Math.max(panelSize.width, 250);  // 最小宽度 250px
        int panelHeight = panelSize.height;

        // 计算右上角位置
        int x = Math.max(10, overlayPanel.getWidth() - panelWidth - 10);
        int y = 5;

        // 设置面板位置和大小
        searchPanel.setBounds(x, y, panelWidth, panelHeight);
        overlayPanel.setSize(overlayPanel.getParent().getSize());
        overlayPanel.revalidate();
        overlayPanel.repaint();
    }

    /**
     * 显示搜索面板
     */
    public void showSearch() {
        searchPanel.showSearch();
        updateSearchPanelPosition();
    }

    /**
     * 显示替换面板
     */
    public void showReplace() {
        searchPanel.showReplace();
        updateSearchPanelPosition();
    }

    /**
     * 配置是否显示行号，便于保留原始编辑器体验
     */
    public void setLineNumbersEnabled(boolean enabled) {
        scrollPane.setLineNumbersEnabled(enabled);
    }
}
