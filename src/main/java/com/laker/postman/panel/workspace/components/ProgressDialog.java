package com.laker.postman.panel.workspace.components;

import com.laker.postman.util.EasyPostManFontUtil;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;
import lombok.Getter;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

/**
 * 带进度显示的对话框基类
 * 提供通用的进度显示、按钮控制等功能
 */
public abstract class ProgressDialog extends JDialog {

    @Getter
    protected boolean confirmed = false;
    @Getter
    protected boolean operationInProgress = false;

    protected ProgressPanel progressPanel;
    protected JButton confirmButton;
    protected JButton cancelButton;

    ProgressDialog(Window parent, String title) {
        super(parent, title, ModalityType.APPLICATION_MODAL);
    }

    /**
     * 初始化对话框
     */
    protected void initDialog() {
        setupLayout();
        setupEventHandlers();
        pack();
        setLocationRelativeTo(getParent());
    }

    /**
     * 设置布局 - 子类需要实现
     */
    protected abstract void setupLayout();

    /**
     * 设置事件处理器 - 子类需要实现
     */
    protected abstract void setupEventHandlers();

    /**
     * 验证输入 - 子类需要实现
     */
    protected abstract void validateInput() throws IllegalArgumentException;

    /**
     * 获取操作任务 - 子类需要实现
     *
     * @return 返回一个SwingWorker来执行后台任务
     */
    protected abstract SwingWorker<Void, String> createWorkerTask();

    /**
     * 操作成功后的处理 - 子类可以重写
     */
    protected void onOperationSuccess() {
        confirmed = true;
        progressPanel.setProgressText("操作成功！");
        progressPanel.getStatusLabel().setText("操作完成，正在关闭对话框...");

        // 延迟关闭对话框
        Timer timer = new Timer(1000, e -> dispose());
        timer.setRepeats(false);
        timer.start();
    }

    /**
     * 操作失败后的处理 - 子类可以重写
     */
    protected void onOperationFailure(Exception e) {
        operationInProgress = false;
        confirmButton.setEnabled(true);
        setInputComponentsEnabled(true);
        progressPanel.setVisible(false);
        pack();

        progressPanel.reset();
        progressPanel.setProgressText("操作失败");
        progressPanel.getStatusLabel().setText("请检查输入信息后重试");

        showError("操作失败: " + e.getMessage());
    }

    /**
     * 设置输入组件启用状态 - 子类需要实现
     */
    protected abstract void setInputComponentsEnabled(boolean enabled);

    /**
     * 创建标准按钮面板
     */
    protected JPanel createStandardButtonPanel(String confirmText) {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));

        cancelButton = new JButton(I18nUtil.getMessage(MessageKeys.BUTTON_CANCEL));
        cancelButton.setFont(EasyPostManFontUtil.getDefaultFont(Font.PLAIN, 12));
        cancelButton.addActionListener(this::handleCancel);

        confirmButton = new JButton(confirmText);
        confirmButton.setFont(EasyPostManFontUtil.getDefaultFont(Font.BOLD, 12));
        confirmButton.addActionListener(this::handleConfirm);

        panel.add(cancelButton);
        panel.add(confirmButton);

        // 设置默认按钮
        getRootPane().setDefaultButton(confirmButton);

        return panel;
    }

    /**
     * 处理取消操作
     */
    private void handleCancel(ActionEvent e) {
        if (operationInProgress) {
            int choice = JOptionPane.showConfirmDialog(
                    this,
                    "操作正在进行中，确定要取消吗？",
                    "确认取消",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE
            );
            if (choice == JOptionPane.YES_OPTION) {
                dispose();
            }
        } else {
            dispose();
        }
    }

    /**
     * 处理确认操作
     */
    private void handleConfirm(ActionEvent e) {
        try {
            validateInput();
            showProgressAndStartOperation();
        } catch (IllegalArgumentException ex) {
            showError(ex.getMessage());
        }
    }

    /**
     * 显示进度并开始操作
     */
    private void showProgressAndStartOperation() {
        // 切换界面状态
        operationInProgress = true;
        progressPanel.setVisible(true);
        confirmButton.setEnabled(false);

        // 禁用所有输入组件
        setInputComponentsEnabled(false);

        // 重新调整窗口大小
        pack();

        // 创建并执行后台任务
        SwingWorker<Void, String> worker = createWorkerTask();

        // 监听进度变化
        worker.addPropertyChangeListener(evt -> {
            if ("progress".equals(evt.getPropertyName())) {
                progressPanel.getProgressBar().setValue((Integer) evt.getNewValue());
            }
        });

        // 设置进度更新处理器
        worker.addPropertyChangeListener(evt -> {
            if ("state".equals(evt.getPropertyName()) &&
                    SwingWorker.StateValue.DONE == evt.getNewValue()) {
                try {
                    worker.get(); // 检查是否有异常
                    onOperationSuccess();
                } catch (Exception ex) {
                    onOperationFailure(ex);
                }
            }
        });

        worker.execute();
    }

    /**
     * 显示错误对话框
     */
    protected void showError(String message) {
        JOptionPane.showMessageDialog(
                this,
                message,
                I18nUtil.getMessage(MessageKeys.ERROR),
                JOptionPane.ERROR_MESSAGE
        );
    }
}
