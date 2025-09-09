package com.laker.postman.common.component;

import com.laker.postman.model.GitOperation;
import com.laker.postman.util.EasyPostManFontUtil;

import javax.swing.*;
import java.awt.*;

/**
 * 步骤指示器组件
 */
public class StepIndicator extends JPanel {
    private final GitOperation operation;
    private final String[] steps;
    private int currentStep = 0;

    public StepIndicator(GitOperation operation) {
        this.operation = operation;
        this.steps = getStepsForOperation(operation);

        setLayout(new FlowLayout(FlowLayout.CENTER, 20, 0));
        setOpaque(false);

        initSteps();
    }

    private String[] getStepsForOperation(GitOperation operation) {
        return switch (operation) {
            case COMMIT -> new String[]{"检查状态", "选择文件", "输入信息", "执行提交"};
            case PUSH -> new String[]{"检查状态", "确认变更", "选择策略", "执行推送"};
            case PULL -> new String[]{"检查状态", "确认变更", "选择策略", "执行拉取"};
        };
    }

    private void initSteps() {
        for (int i = 0; i < steps.length; i++) {
            if (i > 0) {
                add(createArrow());
            }
            add(createStepCircle(i + 1, steps[i], i == currentStep));
        }
    }

    private JPanel createStepCircle(int number, String text, boolean active) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);

        // 圆圈
        JLabel circle = new JLabel(String.valueOf(number), SwingConstants.CENTER);
        circle.setPreferredSize(new Dimension(30, 30));
        circle.setOpaque(true);
        circle.setBackground(active ? new Color(30, 144, 255) : Color.LIGHT_GRAY);
        circle.setForeground(Color.WHITE);
        circle.setFont(EasyPostManFontUtil.getDefaultFont(Font.BOLD, 12));
        circle.setBorder(BorderFactory.createLineBorder(Color.WHITE, 2));

        // 文本
        JLabel label = new JLabel(text, SwingConstants.CENTER);
        label.setFont(EasyPostManFontUtil.getDefaultFont(Font.PLAIN, 10));
        label.setForeground(active ? Color.BLACK : Color.GRAY);

        panel.add(circle, BorderLayout.CENTER);
        panel.add(label, BorderLayout.SOUTH);

        return panel;
    }

    private JLabel createArrow() {
        JLabel arrow = new JLabel("→");
        arrow.setFont(EasyPostManFontUtil.getDefaultFont(Font.PLAIN, 16));
        arrow.setForeground(Color.LIGHT_GRAY);
        return arrow;
    }

    public void setCurrentStep(int step) {
        this.currentStep = step;
        removeAll();
        initSteps();
        revalidate();
        repaint();
    }
}