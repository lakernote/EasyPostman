package com.laker.postman.panel.workspace.components;

import com.laker.postman.util.EasyPostManFontUtil;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;
import lombok.Getter;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.function.Consumer;

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

    public ProgressDialog(Window parent, String title) {
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

    /**
     * 创建一个标准的SwingWorker，带有进度更新功能
     */
    protected SwingWorker<Void, String> createStandardWorker(ProgressStep... steps) {
        return new SwingWorker<Void, String>() {
            @Override
            protected Void doInBackground() throws Exception {
                int totalSteps = steps.length;
                for (int i = 0; i < totalSteps; i++) {
                    ProgressStep step = steps[i];
                    publish(step.message);
                    setProgress(step.progress);

                    if (step.action != null) {
                        step.action.accept(this);
                    } else {
                        // 默认模拟延迟
                        Thread.sleep(step.duration);
                    }
                }
                return null;
            }

            @Override
            protected void process(java.util.List<String> chunks) {
                if (!chunks.isEmpty()) {
                    progressPanel.getStatusLabel().setText(chunks.get(chunks.size() - 1));
                }
            }
        };
    }

    /**
     * 创建一个智能的SwingWorker，支持动态进度更新
     */
    protected SwingWorker<Void, String> createDynamicWorker(String operationName, Runnable actualOperation) {
        return new SwingWorker<Void, String>() {
            @Override
            protected Void doInBackground() throws Exception {
                publish("正在" + operationName + "...");
                setProgress(0);

                long startTime = System.currentTimeMillis();

                // 启动一个监控线程，动态更新进度
                Thread progressMonitor = new Thread(() -> {
                    try {
                        int progress = 0;
                        while (progress < 90 && !isCancelled()) {
                            Thread.sleep(100);
                            progress = Math.min(90, (int)((System.currentTimeMillis() - startTime) / 50)); // 每50ms增加1%
                            setProgress(progress);
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                });

                progressMonitor.start();

                try {
                    // 执行实际操作
                    actualOperation.run();

                    // 操作完成，设置进度到100%
                    setProgress(100);
                    publish(operationName + "完成！");
                } finally {
                    progressMonitor.interrupt();
                }

                return null;
            }

            @Override
            protected void process(java.util.List<String> chunks) {
                if (!chunks.isEmpty()) {
                    progressPanel.getStatusLabel().setText(chunks.get(chunks.size() - 1));
                }
            }
        };
    }

    /**
     * 创建一个带有实际操作回调的SwingWorker
     */
    protected SwingWorker<Void, String> createCallbackWorker(ProgressCallback callback) {
        return new SwingWorker<Void, String>() {
            @Override
            protected Void doInBackground() throws Exception {
                ProgressReporter reporter = new ProgressReporter() {
                    @Override
                    public void updateProgress(int progress, String message) {
                        setProgress(progress);
                        publish(message);
                    }

                    @Override
                    public void setMessage(String message) {
                        publish(message);
                    }
                };

                callback.execute(reporter);
                return null;
            }

            @Override
            protected void process(java.util.List<String> chunks) {
                if (!chunks.isEmpty()) {
                    progressPanel.getStatusLabel().setText(chunks.get(chunks.size() - 1));
                }
            }
        };
    }

    /**
     * 进度报告接口
     */
    public interface ProgressReporter {
        void updateProgress(int progress, String message);
        void setMessage(String message);
    }

    /**
     * 进度回调接口
     */
    public interface ProgressCallback {
        void execute(ProgressReporter reporter) throws Exception;
    }

    /**
     * 进度步骤封装类
     */
    public static class ProgressStep {
        public final String message;
        public final int progress;
        public final long duration;
        public final Consumer<SwingWorker<Void, String>> action;

        public ProgressStep(String message, int progress, long duration) {
            this(message, progress, duration, null);
        }

        public ProgressStep(String message, int progress, Consumer<SwingWorker<Void, String>> action) {
            this(message, progress, 500, action);
        }

        public ProgressStep(String message, int progress, long duration, Consumer<SwingWorker<Void, String>> action) {
            this.message = message;
            this.progress = progress;
            this.duration = duration;
            this.action = action;
        }
    }
}
