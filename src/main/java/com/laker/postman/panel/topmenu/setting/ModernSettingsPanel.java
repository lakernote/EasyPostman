package com.laker.postman.panel.topmenu.setting;

import com.formdev.flatlaf.FlatLaf;
import com.laker.postman.common.constants.ModernColors;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;
import lombok.Getter;

import javax.swing.*;
import javax.swing.border.AbstractBorder;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

/**
 * 现代化设置面板基类
 * 提供统一的现代化UI风格和交互体验
 */
public abstract class ModernSettingsPanel extends JPanel {
    protected JButton saveBtn;
    @Getter
    protected JButton cancelBtn;
    protected JButton applyBtn;
    protected final Map<JTextField, Predicate<String>> validators = new HashMap<>();
    private final Map<JTextField, String> errorMessages = new HashMap<>();
    protected final Map<JComponent, Object> originalValues = new HashMap<>();

    // 状态管理
    protected boolean hasUnsavedChanges = false;
    protected JPanel warningPanel;
    protected JLabel warningLabel;

    private static final int BORDER_RADIUS = 8;     // 圆角半径
    private static final int LABEL_WIDTH = 220;     // 标签宽度
    private static final int FIELD_WIDTH = 300;     // 字段宽度

    /**
     * 检查当前是否为暗色主题
     */
    protected boolean isDarkTheme() {
        return FlatLaf.isLafDark();
    }

    /**
     * 获取主题适配的主背景色
     */
    protected Color getBackgroundColor() {
        return ModernColors.getBackgroundColor();
    }

    /**
     * 获取主题适配的卡片/区域背景色
     */
    protected Color getCardBackgroundColor() {
        return ModernColors.getCardBackgroundColor();
    }

    /**
     * 获取主题适配的输入框背景色
     */
    protected Color getInputBackgroundColor() {
        return ModernColors.getInputBackgroundColor();
    }

    /**
     * 获取主题适配的主文本颜色
     */
    protected Color getTextPrimaryColor() {
        return ModernColors.getTextPrimary();
    }

    /**
     * 获取主题适配的次要文本颜色
     */
    protected Color getTextSecondaryColor() {
        return ModernColors.getTextSecondary();
    }

    /**
     * 获取主题适配的边框颜色（浅色）
     */
    protected Color getBorderLightColor() {
        return ModernColors.getBorderLightColor();
    }

    /**
     * 获取主题适配的边框颜色（中等）
     */
    protected Color getBorderMediumColor() {
        return ModernColors.getBorderMediumColor();
    }

    /**
     * 获取主题适配的悬停背景色
     */
    protected Color getHoverBackgroundColor() {
        return ModernColors.getHoverBackgroundColor();
    }

    /**
     * 获取主题适配的按钮背景色（暗色，pressed状态）
     */
    protected Color getButtonDarkColor() {
        return ModernColors.getButtonPressedColor();
    }

    /**
     * 获取主题适配的滚动条轨道颜色
     */
    protected Color getScrollbarTrackColor() {
        return ModernColors.getScrollbarTrackColor();
    }

    /**
     * 获取主题适配的滚动条滑块颜色
     */
    protected Color getScrollbarThumbColor() {
        return ModernColors.getScrollbarThumbColor();
    }

    /**
     * 获取主题适配的滚动条滑块悬停颜色
     */
    protected Color getScrollbarThumbHoverColor() {
        return ModernColors.getScrollbarThumbHoverColor();
    }

    /**
     * 获取主题适配的阴影颜色
     */
    protected Color getShadowColor(int alpha) {
        return ModernColors.getShadowColor(alpha);
    }

    /**
     * 获取主题适配的警告背景色
     */
    protected Color getWarningBackgroundColor() {
        return ModernColors.getWarningBackgroundColor();
    }

    /**
     * 获取主题适配的警告边框颜色
     */
    protected Color getWarningBorderColor() {
        return ModernColors.getWarningBorderColor();
    }

    /**
     * 获取状态修改图标颜色 - 主题适配
     * 使用警告色表示有未保存的修改
     */
    protected Color getStateModifiedColor() {
        return ModernColors.WARNING;
    }

    protected ModernSettingsPanel() {
        initUI();
        registerListeners();
    }

    private void initUI() {
        setLayout(new BorderLayout(0, 0));
        setBackground(getBackgroundColor());

        // 创建主容器
        JPanel mainContainer = new JPanel(new BorderLayout(0, 0));
        mainContainer.setBackground(getBackgroundColor());

        // 未保存更改警告面板
        warningPanel = createWarningPanel();
        warningPanel.setVisible(false);

        // 主内容区域
        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setBackground(getBackgroundColor());
        contentPanel.setBorder(new EmptyBorder(12, 12, 12, 12));

        // 子类实现具体内容
        buildContent(contentPanel);

        // 滚动面板
        JScrollPane scrollPane = new JScrollPane(contentPanel);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        customizeScrollBar(scrollPane);

        // 组装主容器
        mainContainer.add(warningPanel, BorderLayout.NORTH);
        mainContainer.add(scrollPane, BorderLayout.CENTER);

        // 底部按钮栏
        JPanel buttonBar = createModernButtonBar();

        add(mainContainer, BorderLayout.CENTER);
        add(buttonBar, BorderLayout.SOUTH);
    }

    /**
     * 子类实现此方法来构建具体的设置内容
     */
    protected abstract void buildContent(JPanel contentPanel);

    /**
     * 子类实现此方法来注册监听器
     */
    protected abstract void registerListeners();

    /**
     * 创建现代化的区域面板
     */
    protected JPanel createModernSection(String title, String description) {
        JPanel section = new JPanel();
        section.setLayout(new BoxLayout(section, BoxLayout.Y_AXIS));
        section.setBackground(getCardBackgroundColor());
        section.setBorder(new CompoundBorder(
                new ModernRoundedBorder(),
                new EmptyBorder(12, 12, 12, 12)
        ));
        section.setAlignmentX(Component.LEFT_ALIGNMENT);
        // 修复横向滚动条：限制最大宽度，只允许高度自动扩展
        section.setMaximumSize(new Dimension(Short.MAX_VALUE, Integer.MAX_VALUE));

        // 标题
        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font(titleLabel.getFont().getName(), Font.BOLD, 14));
        titleLabel.setForeground(getTextPrimaryColor());
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        // 描述（可选）
        if (description != null && !description.isEmpty()) {
            JLabel descLabel = new JLabel("<html>" + description + "</html>");
            descLabel.setFont(new Font(descLabel.getFont().getName(), Font.PLAIN, 11));
            descLabel.setForeground(getTextSecondaryColor());
            descLabel.setBorder(new EmptyBorder(4, 0, 8, 0));
            descLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

            section.add(titleLabel);
            section.add(descLabel);
        } else {
            titleLabel.setBorder(new EmptyBorder(0, 0, 8, 0));
            section.add(titleLabel);
        }

        return section;
    }

    /**
     * 创建现代化的字段行（标签 + 输入框）
     */
    protected JPanel createFieldRow(String labelText, String tooltip, JComponent inputComponent) {
        JPanel row = new JPanel();
        row.setLayout(new BoxLayout(row, BoxLayout.X_AXIS));
        row.setBackground(getCardBackgroundColor());
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));

        // 标签
        JLabel label = new JLabel(labelText);
        label.setFont(new Font(label.getFont().getName(), Font.PLAIN, 12));
        label.setForeground(getTextPrimaryColor());
        label.setPreferredSize(new Dimension(LABEL_WIDTH, 32));
        label.setMinimumSize(new Dimension(LABEL_WIDTH, 32));
        label.setMaximumSize(new Dimension(LABEL_WIDTH, 32));

        if (tooltip != null && !tooltip.isEmpty()) {
            label.setToolTipText(tooltip);
        }

        // 输入组件样式化
        styleInputComponent(inputComponent);
        inputComponent.setPreferredSize(new Dimension(FIELD_WIDTH, 34));
        inputComponent.setMaximumSize(new Dimension(FIELD_WIDTH, 34));

        row.add(label);
        row.add(Box.createHorizontalStrut(16));
        row.add(inputComponent);
        row.add(Box.createHorizontalGlue());

        return row;
    }

    /**
     * 创建现代化的复选框行
     */
    protected JPanel createCheckBoxRow(JCheckBox checkBox, String tooltip) {
        JPanel row = new JPanel();
        row.setLayout(new BoxLayout(row, BoxLayout.X_AXIS));
        row.setBackground(getCardBackgroundColor());
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));

        // 样式化复选框
        checkBox.setFont(new Font(checkBox.getFont().getName(), Font.PLAIN, 12));
        checkBox.setForeground(getTextPrimaryColor());
        checkBox.setBackground(getCardBackgroundColor());
        checkBox.setFocusPainted(false);

        if (tooltip != null && !tooltip.isEmpty()) {
            checkBox.setToolTipText(tooltip);
        }

        // 添加悬停效果
        checkBox.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (checkBox.isEnabled()) {
                    checkBox.setCursor(new Cursor(Cursor.HAND_CURSOR));
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                checkBox.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
            }
        });

        row.add(checkBox);
        row.add(Box.createHorizontalGlue());

        return row;
    }


    /**
     * 样式化输入组件
     */
    private void styleInputComponent(JComponent component) {
        component.setFont(new Font(component.getFont().getName(), Font.PLAIN, 12));
        component.setBackground(getInputBackgroundColor());
        component.setForeground(getTextPrimaryColor());

        if (component instanceof JTextField field) {
            field.setBorder(new CompoundBorder(
                    new RoundedLineBorder(getBorderMediumColor(), 1, 8),
                    new EmptyBorder(8, 14, 8, 14)
            ));

            // 焦点效果
            field.addFocusListener(new FocusAdapter() {
                @Override
                public void focusGained(FocusEvent e) {
                    // 检查是否有验证错误
                    if (!hasValidationError(field)) {
                        field.setBorder(new CompoundBorder(
                                new RoundedLineBorder(ModernColors.PRIMARY, 2, 8),
                                new EmptyBorder(7, 13, 7, 13)
                        ));
                    }
                }

                @Override
                public void focusLost(FocusEvent e) {
                    // 检查是否有验证错误
                    if (!hasValidationError(field)) {
                        field.setBorder(new CompoundBorder(
                                new RoundedLineBorder(getBorderMediumColor(), 1, 8),
                                new EmptyBorder(8, 14, 8, 14)
                        ));
                    }
                }
            });
        } else if (component instanceof JComboBox comboBox) {
            comboBox.setBackground(getInputBackgroundColor());
            comboBox.setForeground(getTextPrimaryColor());
            comboBox.setCursor(new Cursor(Cursor.HAND_CURSOR));
        }
    }

    /**
     * 创建现代化的按钮栏
     */
    private JPanel createModernButtonBar() {
        JPanel buttonBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 12));
        buttonBar.setBackground(getBackgroundColor());
        buttonBar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, getBorderLightColor()),
                BorderFactory.createEmptyBorder(0, 16, 0, 16)
        ));

        cancelBtn = createModernButton(
                I18nUtil.getMessage(MessageKeys.SETTINGS_DIALOG_CANCEL),
                false
        );

        applyBtn = createModernButton(
                I18nUtil.getMessage(MessageKeys.SETTINGS_DIALOG_APPLY),
                false
        );
        applyBtn.setEnabled(false); // 初始禁用

        saveBtn = createModernButton(
                I18nUtil.getMessage(MessageKeys.SETTINGS_DIALOG_SAVE),
                true
        );

        buttonBar.add(cancelBtn);
        buttonBar.add(applyBtn);
        buttonBar.add(saveBtn);

        return buttonBar;
    }

    /**
     * 创建现代化按钮
     */
    protected JButton createModernButton(String text, boolean isPrimary) {
        JButton button = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // 背景
                if (isPrimary) {
                    if (!isEnabled()) {
                        g2.setColor(ModernColors.getTextDisabled());
                    } else if (getModel().isPressed()) {
                        g2.setColor(ModernColors.PRIMARY_DARKER);
                    } else if (getModel().isRollover()) {
                        g2.setColor(ModernColors.PRIMARY_DARK);
                    } else {
                        g2.setColor(ModernColors.PRIMARY);
                    }
                } else {
                    if (!isEnabled()) {
                        g2.setColor(getBackgroundColor());
                    } else if (getModel().isPressed()) {
                        g2.setColor(getButtonDarkColor());
                    } else if (getModel().isRollover()) {
                        g2.setColor(getHoverBackgroundColor());
                    } else {
                        g2.setColor(getCardBackgroundColor());
                    }
                }

                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);

                // 边框（非主按钮）
                if (!isPrimary) {
                    g2.setColor(isEnabled() ? getBorderMediumColor() : getBorderLightColor());
                    g2.setStroke(new BasicStroke(1));
                    g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
                }

                g2.dispose();

                // 文字
                super.paintComponent(g);
            }
        };

        button.setFont(new Font(button.getFont().getName(), Font.PLAIN, 12));
        button.setForeground(isPrimary ? ModernColors.getTextInverse() : getTextPrimaryColor());
        button.setPreferredSize(new Dimension(100, 34));
        button.setContentAreaFilled(false);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));

        // 悬停动画
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (button.isEnabled()) {
                    button.repaint();
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                button.repaint();
            }
        });

        return button;
    }

    /**
     * 添加间距
     */
    protected Component createVerticalSpace(int height) {
        return Box.createVerticalStrut(height);
    }

    /**
     * 设置验证器
     */
    protected void setupValidator(JTextField field, Predicate<String> validator, String errorMessage) {
        validators.put(field, validator);
        errorMessages.put(field, errorMessage);

        field.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                validateField();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                validateField();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                validateField();
            }

            private void validateField() {
                String text = field.getText().trim();
                boolean valid = text.isEmpty() || validator.test(text);

                if (valid) {
                    // 根据焦点状态设置不同的边框
                    if (field.hasFocus()) {
                        field.setBorder(new CompoundBorder(
                                new RoundedLineBorder(ModernColors.PRIMARY, 2, 8),
                                new EmptyBorder(7, 13, 7, 13)
                        ));
                    } else {
                        field.setBorder(new CompoundBorder(
                                new RoundedLineBorder(getBorderMediumColor(), 1, 8),
                                new EmptyBorder(8, 14, 8, 14)
                        ));
                    }
                    field.setToolTipText(null);
                } else {
                    field.setBorder(new CompoundBorder(
                            new RoundedLineBorder(ModernColors.ERROR, 2, 8),
                            new EmptyBorder(7, 13, 7, 13)
                    ));
                    field.setToolTipText(errorMessage);
                }
            }
        });
    }

    /**
     * 验证所有字段
     */
    protected boolean validateAllFields() {
        for (Map.Entry<JTextField, Predicate<String>> entry : validators.entrySet()) {
            JTextField field = entry.getKey();
            String text = field.getText().trim();
            if (!text.isEmpty() && !entry.getValue().test(text)) {
                field.requestFocus();
                return false;
            }
        }
        return true;
    }

    /**
     * 自定义滚动条样式
     */
    private void customizeScrollBar(JScrollPane scrollPane) {
        scrollPane.getVerticalScrollBar().setUI(new javax.swing.plaf.basic.BasicScrollBarUI() {
            @Override
            protected void configureScrollBarColors() {
                this.thumbColor = getScrollbarThumbColor();
                this.thumbDarkShadowColor = getScrollbarThumbColor();
                this.thumbHighlightColor = getScrollbarThumbColor();
                this.thumbLightShadowColor = getScrollbarThumbColor();
                this.trackColor = getScrollbarTrackColor();
            }

            @Override
            protected JButton createDecreaseButton(int orientation) {
                return createInvisibleButton();
            }

            @Override
            protected JButton createIncreaseButton(int orientation) {
                return createInvisibleButton();
            }

            private JButton createInvisibleButton() {
                JButton button = new JButton();
                button.setPreferredSize(new Dimension(0, 0));
                button.setMinimumSize(new Dimension(0, 0));
                button.setMaximumSize(new Dimension(0, 0));
                return button;
            }

            @Override
            protected void paintThumb(Graphics g, JComponent c, Rectangle thumbBounds) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(isThumbRollover() ? getScrollbarThumbHoverColor() : getScrollbarThumbColor());
                g2.fillRoundRect(thumbBounds.x + 2, thumbBounds.y + 2,
                        thumbBounds.width - 4, thumbBounds.height - 4, 6, 6);
                g2.dispose();
            }

            @Override
            protected void paintTrack(Graphics g, JComponent c, Rectangle trackBounds) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(getScrollbarTrackColor());
                g2.fillRect(trackBounds.x, trackBounds.y, trackBounds.width, trackBounds.height);
                g2.dispose();
            }
        });
    }

    /**
     * 现代化圆角边框
     */
    private class ModernRoundedBorder extends AbstractBorder {
        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // 多层阴影效果
            int shadowSize = 4;
            for (int i = shadowSize; i > 0; i--) {
                int alpha = (int) (8 * (1 - (double) i / shadowSize));
                g2.setColor(getShadowColor(alpha));
                g2.fillRoundRect(x + i, y + i, width - i * 2, height - i * 2,
                        BORDER_RADIUS + 2, BORDER_RADIUS + 2);
            }

            // 背景（主题适配）
            g2.setColor(getCardBackgroundColor());
            g2.fillRoundRect(x + 1, y + 1, width - 2, height - 2, BORDER_RADIUS, BORDER_RADIUS);

            // 边框
            g2.setColor(getBorderLightColor());
            g2.setStroke(new BasicStroke(1));
            g2.drawRoundRect(x + 1, y + 1, width - 3, height - 3, BORDER_RADIUS, BORDER_RADIUS);

            g2.dispose();
        }

        @Override
        public Insets getBorderInsets(Component c) {
            return new Insets(4, 4, 4, 4);
        }

        @Override
        public Insets getBorderInsets(Component c, Insets insets) {
            insets.left = insets.top = insets.right = insets.bottom = 4;
            return insets;
        }
    }

    /**
     * 圆角线框边框
     */
    private static class RoundedLineBorder extends AbstractBorder {
        private final Color color;
        private final int thickness;
        private final int radius;

        public RoundedLineBorder(Color color, int thickness, int radius) {
            this.color = color;
            this.thickness = thickness;
            this.radius = radius;
        }

        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            g2.setStroke(new BasicStroke(thickness));
            g2.drawRoundRect(x + thickness / 2, y + thickness / 2,
                    width - thickness, height - thickness, radius, radius);
            g2.dispose();
        }

        @Override
        public Insets getBorderInsets(Component c) {
            return new Insets(thickness, thickness, thickness, thickness);
        }

        @Override
        public Insets getBorderInsets(Component c, Insets insets) {
            insets.left = insets.top = insets.right = insets.bottom = thickness;
            return insets;
        }
    }

    // 工具方法
    protected boolean isInteger(String s) {
        try {
            Integer.parseInt(s);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    protected boolean isPositiveInteger(String s) {
        return isInteger(s) && Integer.parseInt(s) >= 0;
    }

    /**
     * 检查字段是否有验证错误
     */
    protected boolean hasValidationError(JTextField field) {
        Predicate<String> validator = validators.get(field);
        if (validator == null) {
            return false;
        }
        String text = field.getText().trim();
        return !text.isEmpty() && !validator.test(text);
    }

    // ==================== 状态管理方法 ====================

    /**
     * 创建未保存更改警告面板
     */
    private JPanel createWarningPanel() {
        JPanel panel = new JPanel(new BorderLayout(12, 0));
        panel.setBackground(getWarningBackgroundColor());
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, getWarningBorderColor()),
                new EmptyBorder(12, 20, 12, 20)
        ));

        // 警告图标和文本
        JLabel iconLabel = new JLabel("⚠");
        iconLabel.setFont(new Font(iconLabel.getFont().getName(), Font.BOLD, 14));
        iconLabel.setForeground(getStateModifiedColor());

        warningLabel = new JLabel(I18nUtil.getMessage(MessageKeys.SETTINGS_UNSAVED_CHANGES_WARNING));
        warningLabel.setFont(new Font(warningLabel.getFont().getName(), Font.PLAIN, 11));
        warningLabel.setForeground(getTextPrimaryColor());

        // 按钮面板
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttonPanel.setOpaque(false);

        JButton discardBtn = createSmallButton(I18nUtil.getMessage(MessageKeys.SETTINGS_DISCARD_CHANGES));
        discardBtn.addActionListener(e -> discardChanges());

        JButton saveNowBtn = createSmallButton(I18nUtil.getMessage(MessageKeys.SETTINGS_SAVE_NOW));
        saveNowBtn.addActionListener(e -> {
            if (saveBtn != null) {
                saveBtn.doClick();
            }
        });

        buttonPanel.add(discardBtn);
        buttonPanel.add(saveNowBtn);

        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        leftPanel.setOpaque(false);
        leftPanel.add(iconLabel);
        leftPanel.add(warningLabel);

        panel.add(leftPanel, BorderLayout.WEST);
        panel.add(buttonPanel, BorderLayout.EAST);

        return panel;
    }

    /**
     * 创建小型按钮
     */
    private JButton createSmallButton(String text) {
        JButton button = new JButton(text);
        button.setFont(new Font(button.getFont().getName(), Font.PLAIN, 11));
        button.setForeground(getTextPrimaryColor());
        button.setBackground(getCardBackgroundColor());
        button.setBorder(new CompoundBorder(
                new RoundedLineBorder(getBorderMediumColor(), 1, 6),
                new EmptyBorder(4, 12, 4, 12)
        ));
        button.setContentAreaFilled(false);
        button.setFocusPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));

        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                button.setBackground(getHoverBackgroundColor());
                button.setOpaque(true);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                button.setOpaque(false);
            }
        });

        return button;
    }

    /**
     * 记录组件的原始值
     */
    protected void trackComponentValue(JComponent component) {
        if (component instanceof JTextField) {
            originalValues.put(component, ((JTextField) component).getText());
            ((JTextField) component).getDocument().addDocumentListener(new DocumentListener() {
                @Override
                public void insertUpdate(DocumentEvent e) {
                    checkForChanges();
                }

                @Override
                public void removeUpdate(DocumentEvent e) {
                    checkForChanges();
                }

                @Override
                public void changedUpdate(DocumentEvent e) {
                    checkForChanges();
                }
            });
        } else if (component instanceof JCheckBox) {
            originalValues.put(component, ((JCheckBox) component).isSelected());
            ((JCheckBox) component).addItemListener(e -> checkForChanges());
        } else if (component instanceof JComboBox) {
            originalValues.put(component, ((JComboBox<?>) component).getSelectedItem());
            ((JComboBox<?>) component).addItemListener(e -> checkForChanges());
        }
    }

    /**
     * 检查是否有未保存的更改
     */
    protected void checkForChanges() {
        boolean hasChanges = false;

        for (Map.Entry<JComponent, Object> entry : originalValues.entrySet()) {
            JComponent component = entry.getKey();
            Object originalValue = entry.getValue();

            if (component instanceof JTextField) {
                String currentValue = ((JTextField) component).getText();
                if (!currentValue.equals(originalValue)) {
                    hasChanges = true;
                    break;
                }
            } else if (component instanceof JCheckBox) {
                boolean currentValue = ((JCheckBox) component).isSelected();
                if (currentValue != (Boolean) originalValue) {
                    hasChanges = true;
                    break;
                }
            } else if (component instanceof JComboBox) {
                Object currentValue = ((JComboBox<?>) component).getSelectedItem();
                if (currentValue != null && !currentValue.equals(originalValue)) {
                    hasChanges = true;
                    break;
                }
            }
        }

        setHasUnsavedChanges(hasChanges);
    }

    /**
     * 设置未保存更改状态
     */
    protected void setHasUnsavedChanges(boolean hasChanges) {
        this.hasUnsavedChanges = hasChanges;
        if (warningPanel != null) {
            warningPanel.setVisible(hasChanges);
        }
        if (applyBtn != null) {
            applyBtn.setEnabled(hasChanges);
        }

        // 更新父窗口标题（如果是对话框）
        Window window = SwingUtilities.getWindowAncestor(this);
        if (window instanceof JDialog && hasChanges) {
            JDialog dialog = (JDialog) window;
            String title = dialog.getTitle();
            if (!title.startsWith("* ")) {
                dialog.setTitle("* " + title);
            }
        } else if (window instanceof JDialog && !hasChanges) {
            JDialog dialog = (JDialog) window;
            String title = dialog.getTitle();
            if (title.startsWith("* ")) {
                dialog.setTitle(title.substring(2));
            }
        }
    }

    /**
     * 放弃更改
     */
    protected void discardChanges() {
        for (Map.Entry<JComponent, Object> entry : originalValues.entrySet()) {
            JComponent component = entry.getKey();
            Object originalValue = entry.getValue();

            if (component instanceof JTextField) {
                ((JTextField) component).setText((String) originalValue);
            } else if (component instanceof JCheckBox) {
                ((JCheckBox) component).setSelected((Boolean) originalValue);
            } else if (component instanceof JComboBox) {
                ((JComboBox) component).setSelectedItem(originalValue);
            }
        }
        setHasUnsavedChanges(false);
    }

    /**
     * 确认放弃更改
     */
    protected boolean confirmDiscardChanges() {
        if (!hasUnsavedChanges) {
            return true;
        }

        int result = JOptionPane.showConfirmDialog(
                this,
                I18nUtil.getMessage(MessageKeys.SETTINGS_CONFIRM_DISCARD_MESSAGE),
                I18nUtil.getMessage(MessageKeys.SETTINGS_CONFIRM_DISCARD_TITLE),
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );

        return result == JOptionPane.YES_OPTION;
    }

    public boolean hasUnsavedChanges() {
        return hasUnsavedChanges;
    }
}

