package com.laker.postman.common.component.combobox;

import com.laker.postman.model.Environment;
import com.laker.postman.model.EnvironmentItem;
import com.laker.postman.service.EnvironmentService;
import com.laker.postman.util.FontsUtil;
import lombok.Setter;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.List;
import java.util.function.Consumer;

public class EnvironmentComboBox extends JComboBox<EnvironmentItem> {
    private boolean isUpdating = false;
    @Setter
    private Consumer<Environment> onEnvironmentChange;

    public EnvironmentComboBox() {
        setRenderer(new EnvironmentItemRenderer());
        setPreferredSize(new Dimension(120, 28));
        setMaximumSize(new Dimension(120, 28));
        setFont(FontsUtil.getDefaultFont(Font.PLAIN)); // 使用用户设置的字体大小
        setFocusable(false);
        // 设置无边框样式
        setBorder(new EmptyBorder(0, 0, 0, 0));

        // 应用自定义样式
        applyCustomStyle();

        addActionListener(e -> {
            if (isUpdating) return;
            EnvironmentItem item = (EnvironmentItem) getSelectedItem();
            if (item != null && item.getEnvironment() != null) {
                EnvironmentService.setActiveEnvironment(item.getEnvironment().getId());
                if (onEnvironmentChange != null) {
                    onEnvironmentChange.accept(item.getEnvironment());
                }
            }
        });
        reload();
    }

    @Override
    public void updateUI() {
        super.updateUI();
        // 主题切换后重新应用自定义样式
        applyCustomStyle();
    }

    /**
     * 应用自定义样式，使用 FlatLaf 的 client property 方式
     * 这样只会影响这个特定的 ComboBox 实例
     */
    private void applyCustomStyle() {
        Color backgroundColor = UIManager.getColor("MenuBar.background");
        if (backgroundColor != null) {
            String styleValue = String.format(
                "buttonBackground: %s; " +
                "buttonEditableBackground: %s; " +
                "buttonFocusedBackground: %s; " +
                "buttonSeparatorColor: null; " +
                "buttonDisabledSeparatorColor: null",
                colorToHex(backgroundColor),
                colorToHex(backgroundColor),
                colorToHex(backgroundColor)
            );

            putClientProperty("FlatLaf.style", styleValue);
            setBackground(backgroundColor);
        }
    }

    /**
     * 将 Color 转换为十六进制字符串
     */
    private static String colorToHex(Color color) {
        return String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
    }

    public void reload() {
        isUpdating = true;
        removeAllItems();
        List<Environment> envs = EnvironmentService.getAllEnvironments();
        Environment activeEnv = null;
        for (Environment env : envs) {
            addItem(new EnvironmentItem(env));
            if (env.isActive()) {
                activeEnv = env;
            }
        }
        if (activeEnv != null) {
            for (int i = 0; i < getItemCount(); i++) {
                EnvironmentItem item = getItemAt(i);
                if (item.getEnvironment().getId().equals(activeEnv.getId())) {
                    setSelectedIndex(i);
                    break;
                }
            }
        } else if (getItemCount() > 0) {
            setSelectedIndex(0);
        }
        isUpdating = false;
    }

    /**
     * 选中指定环境
     */
    public void setSelectedEnvironment(Environment env) {
        if (env == null) return;
        isUpdating = true;
        for (int i = 0; i < getItemCount(); i++) {
            EnvironmentItem item = getItemAt(i);
            if (item.getEnvironment().getId().equals(env.getId())) {
                setSelectedIndex(i);
                break;
            }
        }
        isUpdating = false;
    }
}