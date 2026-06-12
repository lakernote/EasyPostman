package com.laker.postman.panel.performance.controller;

import com.laker.postman.common.constants.ModernColors;
import com.laker.postman.panel.performance.PerformanceStagePropertyLayout;
import com.laker.postman.util.FontsUtil;
import com.laker.postman.util.IconUtil;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.*;

public final class ControllerDescriptionPanel extends JPanel {

    private ControllerDescriptionPanel(String titleKey, String descriptionKey, String iconPath) {
        setLayout(new MigLayout(
                "insets 12 20 12 20, fillx, novisualpadding",
                "[grow,fill]",
                "[]8[]push"
        ));
        PerformanceStagePropertyLayout.applyCompactBorder(this);

        JLabel titleLabel = new JLabel(
                I18nUtil.getMessage(titleKey),
                IconUtil.createThemed(iconPath, 18, 18),
                SwingConstants.LEFT
        );
        titleLabel.setIconTextGap(8);
        titleLabel.setFont(FontsUtil.getDefaultFontWithOffset(Font.BOLD, 1));
        titleLabel.setForeground(ModernColors.getTextPrimary());

        JTextArea descriptionArea = PerformanceStagePropertyLayout.createHintArea(
                I18nUtil.getMessage(descriptionKey),
                7,
                72
        );

        add(titleLabel, "wrap");
        add(descriptionArea, "growx, wmin 0");
    }

    public static ControllerDescriptionPanel simpleControllerPanel() {
        return new ControllerDescriptionPanel(
                MessageKeys.PERFORMANCE_SIMPLE_NODE,
                MessageKeys.PERFORMANCE_SIMPLE_DESCRIPTION,
                "icons/performance-simple-controller.svg"
        );
    }

    public static ControllerDescriptionPanel onceOnlyControllerPanel() {
        return new ControllerDescriptionPanel(
                MessageKeys.PERFORMANCE_ONCE_ONLY_NODE,
                MessageKeys.PERFORMANCE_ONCE_ONLY_DESCRIPTION,
                "icons/performance-once-only-controller.svg"
        );
    }
}
