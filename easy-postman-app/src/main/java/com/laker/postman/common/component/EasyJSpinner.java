package com.laker.postman.common.component;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.text.ParseException;

/**
 * 增强版 JSpinner，自动提交输入
 *
 * <h2>功能特性</h2>
 * <ul>
 *   <li>实时生效：输入停止300ms后自动提交（防抖）</li>
 *   <li>失焦提交：点击其他控件时立即提交</li>
 *   <li>回车提交：按回车键立即提交并跳转焦点</li>
 *   <li>异常处理：输入无效时自动恢复到上一个有效值</li>
 * </ul>
 *
 * <h2>用户体验</h2>
 * <ul>
 *   <li>✓ 输入后等待300ms → 自动提交</li>
 *   <li>✓ 点击其他控件 → 立即提交</li>
 *   <li>✓ 点击空白区域 → 300ms后自动提交</li>
 *   <li>✓ 按回车键 → 立即提交并跳转</li>
 * </ul>
 *
 * @author laker
 * @since 4.2.20
 */
public class EasyJSpinner extends JSpinner {

    /**
     * 防抖延迟时间（毫秒）
     */
    private static final int DEBOUNCE_DELAY = 300;

    /**
     * 防抖 Timer
     */
    private final Timer debounceTimer;

    /**
     * 使用数据模型创建 EasyJSpinner
     *
     * @param model Spinner 的数据模型
     */
    public EasyJSpinner(SpinnerModel model) {
        super(model);

        // 创建防抖 Timer
        this.debounceTimer = new Timer(DEBOUNCE_DELAY, e -> commitValue());
        this.debounceTimer.setRepeats(false);

        // 初始化自动提交功能
        initAutoCommit();
    }

    /**
     * 初始化自动提交功能
     */
    private void initAutoCommit() {
        JComponent editor = getEditor();
        if (!(editor instanceof DefaultEditor)) {
            return;
        }

        JFormattedTextField textField = ((DefaultEditor) editor).getTextField();

        // 设置焦点丢失行为：保持当前编辑内容（避免自动恢复到旧值）
        // PERSIST 模式下，失焦时会尝试提交，但不会自动恢复
        textField.setFocusLostBehavior(JFormattedTextField.PERSIST);

        // 1. 文本变化监听：输入停止后自动提交（带防抖）
        textField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                // 用户正在输入时，延迟提交
                debounceTimer.restart();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                // 用户正在删除时，延迟提交
                debounceTimer.restart();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                // 文本属性变化时，延迟提交
                debounceTimer.restart();
            }
        });

        // 2. 失焦监听：焦点离开时立即提交
        textField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                debounceTimer.stop();  // 停止防抖
                commitValue();         // 立即提交
            }
        });

        // 3. 回车键监听：立即提交并跳转焦点
        textField.addActionListener(e -> {
            debounceTimer.stop();  // 停止防抖
            if (commitValue()) {
                textField.transferFocus();
            }
        });
    }

    /**
     * 提交当前值
     *
     * <p>改进策略：
     * <ul>
     *   <li>保存并恢复光标位置</li>
     *   <li>提交失败时不做任何操作，保持用户输入</li>
     *   <li>避免在用户输入过程中的异常格式化</li>
     *   <li>使用延迟执行确保光标位置正确恢复</li>
     * </ul>
     *
     * @return 是否提交成功
     */
    private boolean commitValue() {
        JComponent editor = getEditor();
        if (!(editor instanceof DefaultEditor)) {
            return false;
        }

        JFormattedTextField textField = ((DefaultEditor) editor).getTextField();

        // 保存当前光标位置和文本
        int caretPosition = textField.getCaretPosition();
        String currentText = textField.getText();

        try {
            // 尝试提交编辑
            commitEdit();

            // 提交成功后，使用 invokeLater 确保在所有格式化完成后再恢复光标
            SwingUtilities.invokeLater(() -> {
                try {
                    String newText = textField.getText();
                    // 计算合适的光标位置
                    int newCaretPos;
                    if (newText.equals(currentText)) {
                        // 文本未变化，恢复原光标位置
                        newCaretPos = Math.min(caretPosition, newText.length());
                    } else {
                        // 文本已格式化，将光标放在末尾
                        newCaretPos = newText.length();
                    }
                    textField.setCaretPosition(newCaretPos);
                } catch (Exception e) {
                    // 忽略光标设置异常，不影响功能
                }
            });

            return true;
        } catch (ParseException ex) {
            // 提交失败（例如：输入无效或超出范围）
            // 保持当前文本和光标位置不变，让用户继续编辑
            // 这避免了输入过程中的异常格式化（例如 "10001" -> "0010"）
            return false;
        }
    }

    /**
     * 强制提交当前值（用于 Ctrl/Cmd+S 保存时）
     */
    public void forceCommit() {
        debounceTimer.stop();  // 停止防抖
        commitValue();
    }
}

