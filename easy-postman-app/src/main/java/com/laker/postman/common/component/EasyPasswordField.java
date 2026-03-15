package com.laker.postman.common.component;

import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.extras.components.FlatPasswordField;
import com.laker.postman.util.FontsUtil;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import javax.swing.event.DocumentListener;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoManager;
import java.awt.*;
import java.awt.event.ActionEvent;

/**
 * EasyPostman 密码输入框组件
 * <p>
 * 功能特性：
 * 1. 集成 FlatLaf 显示/隐藏密码按钮（眼睛图标）
 * 2. 支持撤回/重做操作
 * - Windows: Ctrl+Z 撤回，Ctrl+Y 重做
 * - macOS: Cmd+Z 撤回，Cmd+Shift+Z 重做
 * 3. 支持占位符文本
 * 4. 统一的字体样式
 * 5. 支持自定义样式（通过 FlatLaf 的 STYLE 属性）
 *
 */
@Slf4j
public class EasyPasswordField extends FlatPasswordField {

    private static final String SHOW_REVEAL_BUTTON_STYLE = "showRevealButton: true";

    private final UndoManager undoManager = new UndoManager();
    private boolean showRevealButton = true;

    /**
     * 创建默认的密码输入框
     */
    public EasyPasswordField() {
        this(20);
    }

    /**
     * 创建指定列数的密码输入框
     *
     * @param columns 显示列数
     */
    public EasyPasswordField(int columns) {
        super();
        setColumns(columns);
        init();
    }

    /**
     * 创建带初始文本和列数的密码输入框
     *
     * @param text    初始文本
     * @param columns 显示列数
     */
    public EasyPasswordField(String text, int columns) {
        super();
        setText(text);
        setColumns(columns);
        init();
    }

    /**
     * 创建带占位符的密码输入框
     *
     * @param columns         显示列数
     * @param placeholderText 占位符文本
     */
    public EasyPasswordField(int columns, String placeholderText) {
        super();
        setColumns(columns);
        setPlaceholderText(placeholderText);
        init();
    }

    /**
     * 创建带初始文本、列数和占位符的密码输入框
     *
     * @param text            初始文本
     * @param columns         显示列数
     * @param placeholderText 占位符文本
     */
    public EasyPasswordField(String text, int columns, String placeholderText) {
        super();
        setText(text);
        setColumns(columns);
        setPlaceholderText(placeholderText);
        init();
    }

    /**
     * 初始化组件
     */
    private void init() {
        // 设置默认字体
        setFont(FontsUtil.getDefaultFont(Font.PLAIN));

        // 启用密码显示/隐藏按钮
        updateRevealButtonStyle();

        // 初始化撤回/重做功能
        initUndoRedo();
    }

    /**
     * 更新显示/隐藏按钮样式
     */
    private void updateRevealButtonStyle() {
        if (showRevealButton) {
            putClientProperty(FlatClientProperties.STYLE, SHOW_REVEAL_BUTTON_STYLE);
        } else {
            putClientProperty(FlatClientProperties.STYLE, null);
        }
    }

    /**
     * 设置是否显示密码显示/隐藏按钮
     *
     * @param show true 显示按钮，false 隐藏按钮
     */
    public void setShowRevealButton(boolean show) {
        if (this.showRevealButton != show) {
            this.showRevealButton = show;
            updateRevealButtonStyle();
        }
    }

    /**
     * 获取是否显示密码显示/隐藏按钮
     *
     * @return true 显示按钮，false 隐藏按钮
     */
    public boolean isShowRevealButton() {
        return showRevealButton;
    }

    /**
     * 初始化撤回/重做功能
     */
    private void initUndoRedo() {
        getDocument().addUndoableEditListener(undoManager);

        // Undo - 撤回
        getInputMap().put(KeyStroke.getKeyStroke("control Z"), "Undo");
        getInputMap().put(KeyStroke.getKeyStroke("meta Z"), "Undo"); // macOS Cmd+Z
        getActionMap().put("Undo", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    if (undoManager.canUndo()) {
                        undoManager.undo();
                    }
                } catch (CannotUndoException ex) {
                    log.debug("Cannot undo", ex);
                }
            }
        });

        // Redo - 重做
        getInputMap().put(KeyStroke.getKeyStroke("control Y"), "Redo");
        getInputMap().put(KeyStroke.getKeyStroke("meta shift Z"), "Redo"); // macOS Cmd+Shift+Z
        getActionMap().put("Redo", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    if (undoManager.canRedo()) {
                        undoManager.redo();
                    }
                } catch (CannotRedoException ex) {
                    log.debug("Cannot redo", ex);
                }
            }
        });
    }

    /**
     * 获取密码文本（便捷方法）
     *
     * @return 密码字符串
     */
    public String getPasswordText() {
        return new String(getPassword());
    }

    /**
     * 设置密码文本（便捷方法）
     *
     * @param password 密码字符串
     */
    public void setPasswordText(String password) {
        setText(password);
    }

    /**
     * 清空密码
     */
    public void clearPassword() {
        setText("");
    }

    /**
     * 添加文档监听器（便捷方法）
     *
     * @param listener 文档监听器
     */
    public void addDocumentListener(DocumentListener listener) {
        getDocument().addDocumentListener(listener);
    }

    /**
     * 移除文档监听器（便捷方法）
     *
     * @param listener 文档监听器
     */
    public void removeDocumentListener(DocumentListener listener) {
        getDocument().removeDocumentListener(listener);
    }

    /**
     * 重置撤回管理器
     * 当需要清除撤回历史时调用此方法
     */
    public void resetUndoManager() {
        undoManager.discardAllEdits();
    }

    /**
     * 设置自定义样式
     * 注意：如果要保留显示/隐藏按钮，请在样式字符串中包含 "showRevealButton: true"
     *
     * @param style FlatLaf 样式字符串
     */
    public void setCustomStyle(String style) {
        if (showRevealButton && (style == null || !style.contains("showRevealButton"))) {
            // 如果开启了显示按钮但样式中没有，则追加
            String newStyle = style == null ? SHOW_REVEAL_BUTTON_STYLE :
                    style + "; " + SHOW_REVEAL_BUTTON_STYLE;
            putClientProperty(FlatClientProperties.STYLE, newStyle);
        } else {
            putClientProperty(FlatClientProperties.STYLE, style);
        }
    }
}

