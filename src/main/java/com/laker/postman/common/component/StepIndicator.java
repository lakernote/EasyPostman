package com.laker.postman.common.component;

import com.formdev.flatlaf.FlatLaf;
import com.laker.postman.common.constants.ModernColors;
import com.laker.postman.util.FontsUtil;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;

import javax.swing.*;
import java.awt.*;

/**
 * 步骤指示器组件
 * 支持暗黑和亮色主题自动适配
 */
public class StepIndicator extends JPanel {
    private final String[] steps;
    private int currentStep = 0;

    public StepIndicator() {
        this.steps = getStepsForOperation();

        setLayout(new FlowLayout(FlowLayout.CENTER, 20, 0));
        setOpaque(false);

        initSteps();
    }

    /**
     * 检查当前是否为暗色主题
     */
    private boolean isDarkTheme() {
        return FlatLaf.isLafDark();
    }

    /**
     * 获取激活状态的圆圈背景色（主题适配）
     */
    private Color getActiveCircleBackground() {
        return ModernColors.PRIMARY;
    }

    /**
     * 获取非激活状态的圆圈背景色（主题适配）
     */
    private Color getInactiveCircleBackground() {
        return isDarkTheme() ? new Color(85, 87, 90) : new Color(226, 232, 240);
    }

    /**
     * 获取圆圈边框颜色（主题适配）
     */
    private Color getCircleBorderColor() {
        return isDarkTheme() ? new Color(70, 73, 75) : Color.WHITE;
    }

    /**
     * 获取激活状态的文本颜色（主题适配）
     */
    private Color getActiveTextColor() {
        return ModernColors.getTextPrimary();
    }

    /**
     * 获取非激活状态的文本颜色（主题适配）
     */
    private Color getInactiveTextColor() {
        return ModernColors.getTextSecondary();
    }

    /**
     * 获取箭头颜色（主题适配）
     */
    private Color getArrowColor() {
        return ModernColors.getTextSecondary();
    }

    private String[] getStepsForOperation() {
        return new String[]{I18nUtil.getMessage(MessageKeys.STEP_CHECK_STATUS),
                I18nUtil.getMessage(MessageKeys.STEP_CONFIRM_CHANGE),
                I18nUtil.getMessage(MessageKeys.STEP_SELECT_STRATEGY),
                I18nUtil.getMessage(MessageKeys.STEP_EXECUTE_OPERATION)};
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

        // 圆圈 - 使用主题适配的颜色
        JLabel circle = new JLabel(String.valueOf(number), SwingConstants.CENTER);
        circle.setPreferredSize(new Dimension(30, 30));
        circle.setOpaque(true);
        circle.setBackground(active ? getActiveCircleBackground() : getInactiveCircleBackground());
        circle.setForeground(Color.WHITE); // 圆圈内的数字始终使用白色
        circle.setFont(FontsUtil.getDefaultFont(Font.BOLD));
        circle.setBorder(BorderFactory.createLineBorder(getCircleBorderColor(), 2));

        // 文本 - 使用主题适配的颜色
        JLabel label = new JLabel(text, SwingConstants.CENTER);
        label.setFont(FontsUtil.getDefaultFontWithOffset(Font.PLAIN, -2));
        label.setForeground(active ? getActiveTextColor() : getInactiveTextColor());

        panel.add(circle, BorderLayout.CENTER);
        panel.add(label, BorderLayout.SOUTH);

        return panel;
    }

    private JLabel createArrow() {
        JLabel arrow = new JLabel("→");
        arrow.setFont(FontsUtil.getDefaultFontWithOffset(Font.PLAIN, +4)); // 比标准字体大4号
        arrow.setForeground(getArrowColor()); // 使用主题适配的箭头颜色
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