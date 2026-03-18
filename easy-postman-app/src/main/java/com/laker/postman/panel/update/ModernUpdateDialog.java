package com.laker.postman.panel.update;

import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.json.JSONObject;
import com.formdev.flatlaf.FlatClientProperties;
import com.laker.postman.common.constants.ModernColors;
import com.laker.postman.model.UpdateInfo;
import com.laker.postman.util.FontsUtil;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.IconUtil;
import com.laker.postman.util.MessageKeys;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * 现代化更新对话框 - 简洁清晰的更新提示
 */
public class ModernUpdateDialog extends JDialog {

    private int userChoice = -1;
    // 缓存渐变，避免 resize 时频繁创建对象（transient：不参与序列化）
    private transient GradientPaint cachedGradient;
    private int cachedGradientWidth = -1;
    private int cachedGradientHeight = -1;

    public ModernUpdateDialog(Frame parent, UpdateInfo updateInfo) {
        super(parent, I18nUtil.getMessage(MessageKeys.UPDATE_NEW_VERSION_AVAILABLE), true);
        initComponents(updateInfo);
        setMinimumSize(new Dimension(550, 360));
        pack();
        setSize(Math.max(getWidth(), 650), Math.max(getHeight(), 420));
        setLocationRelativeTo(parent);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setResizable(true);
    }

    private void initComponents(UpdateInfo updateInfo) {
        setLayout(new BorderLayout());
        JPanel mainPanel = new JPanel(new BorderLayout(0, 0));
        mainPanel.add(createHeaderPanel(updateInfo), BorderLayout.NORTH);
        mainPanel.add(createChangelogPanel(updateInfo), BorderLayout.CENTER);
        mainPanel.add(createButtonPanel(), BorderLayout.SOUTH);
        add(mainPanel);
    }

    private JPanel createHeaderPanel(UpdateInfo updateInfo) {
        JPanel panel = new JPanel(new BorderLayout(16, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                // 缓存渐变，只在尺寸变化时重建
                if (cachedGradient == null
                        || cachedGradientWidth != getWidth()
                        || cachedGradientHeight != getHeight()) {
                    cachedGradient = new GradientPaint(
                            0, 0, ModernColors.PRIMARY_LIGHTER,
                            getWidth(), getHeight(), ModernColors.SECONDARY_LIGHTER);
                    cachedGradientWidth = getWidth();
                    cachedGradientHeight = getHeight();
                }
                g2.setPaint(cachedGradient);
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setColor(ModernColors.primaryWithAlpha(20));
                g2.fillOval(-50, -50, 200, 200);
                g2.fillOval(getWidth() - 150, getHeight() - 100, 200, 150);
                g2.dispose();
            }
        };
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(24, 24, 24, 24));

        panel.add(new JLabel(IconUtil.createThemed("icons/info.svg", 64, 64)), BorderLayout.WEST);

        JPanel infoPanel = new JPanel();
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
        infoPanel.setOpaque(false);

        JLabel titleLabel = new JLabel(I18nUtil.getMessage(MessageKeys.UPDATE_NEW_VERSION_AVAILABLE));
        titleLabel.setFont(FontsUtil.getDefaultFontWithOffset(Font.BOLD, 8));
        titleLabel.setForeground(ModernColors.getTextPrimary());
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        String verPrefix = I18nUtil.isChinese() ? "版本" : "Version";
        JLabel versionLabel = new JLabel(verPrefix + "  " + updateInfo.getCurrentVersion() + "  →  " + updateInfo.getLatestVersion());
        versionLabel.setFont(FontsUtil.getDefaultFontWithOffset(Font.BOLD, 3));
        versionLabel.setForeground(ModernColors.PRIMARY);
        versionLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        infoPanel.add(titleLabel);
        infoPanel.add(Box.createVerticalStrut(8));
        infoPanel.add(versionLabel);

        // 发布时间（有就加，没有就不加，不用重复 add 其他组件）
        String publishedAt = updateInfo.getReleaseInfo() != null
                ? updateInfo.getReleaseInfo().getStr("published_at", "") : "";
        if (!publishedAt.isEmpty()) {
            String dateStr = publishedAt.length() >= 10 ? publishedAt.substring(0, 10) : publishedAt;
            JLabel dateLabel = new JLabel((I18nUtil.isChinese() ? "发布于 " : "Released on ") + dateStr);
            dateLabel.setFont(FontsUtil.getDefaultFont(Font.PLAIN));
            dateLabel.setForeground(ModernColors.getTextHint());
            dateLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            infoPanel.add(Box.createVerticalStrut(6));
            infoPanel.add(dateLabel);
        }

        panel.add(infoPanel, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createChangelogPanel(UpdateInfo updateInfo) {
        JPanel mainPanel = new JPanel(new BorderLayout(0, 8));
        mainPanel.setBorder(new EmptyBorder(0, 24, 0, 24));

        JLabel titleLabel = new JLabel("📝 " + I18nUtil.getMessage(MessageKeys.UPDATE_WHATS_NEW));
        titleLabel.setFont(FontsUtil.getDefaultFontWithOffset(Font.BOLD, 3));
        titleLabel.setForeground(ModernColors.getTextPrimary());
        mainPanel.add(titleLabel, BorderLayout.NORTH);

        JTextArea textArea = new JTextArea(extractChangelog(updateInfo.getReleaseInfo()));
        textArea.setEditable(false);
        textArea.setFocusable(false);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        textArea.setFont(FontsUtil.getDefaultFontWithOffset(Font.PLAIN, 1));
        // 跟随 LAF，不硬设颜色，主题切换自动适配
        textArea.putClientProperty(FlatClientProperties.STYLE, "");
        textArea.setBorder(new EmptyBorder(12, 12, 12, 12));
        textArea.setCaretPosition(0);

        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setBorder(BorderFactory.createLineBorder(ModernColors.getBorderLightColor(), 1));
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.setPreferredSize(new Dimension(0, 180));
        mainPanel.add(scrollPane, BorderLayout.CENTER);

        return mainPanel;
    }

    private String extractChangelog(JSONObject releaseInfo) {
        if (releaseInfo == null) {
            return I18nUtil.getMessage(MessageKeys.UPDATE_NO_CHANGELOG);
        }
        String body = releaseInfo.getStr("body");
        if (CharSequenceUtil.isBlank(body)) {
            return I18nUtil.getMessage(MessageKeys.UPDATE_DEFAULT_CHANGELOG);
        }
        return body.trim()
                .replaceAll("(?m)^#{1,6}\\s+", "▸ ")
                .replaceAll("(?m)^[-*]\\s+", "  • ")
                .replaceAll("\\*\\*(.+?)\\*\\*", "$1")
                .replaceAll("\\*(.+?)\\*", "$1")
                .replaceAll("```[\\s\\S]*?```", I18nUtil.getMessage(MessageKeys.UPDATE_CODE_EXAMPLE))
                .replaceAll("`(.+?)`", "$1")
                .replaceAll("\\[(.+?)]\\(.+?\\)", "$1")
                .replaceAll("\\n{3,}", "\n\n");
    }

    private JPanel createButtonPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(new EmptyBorder(16, 24, 20, 24));

        JLabel tipLabel = new JLabel(I18nUtil.getMessage(MessageKeys.UPDATE_SAVE_TIP));
        tipLabel.setFont(FontsUtil.getDefaultFont(Font.PLAIN));
        tipLabel.setForeground(ModernColors.getTextHint());
        panel.add(tipLabel, BorderLayout.WEST);

        JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        buttonsPanel.setOpaque(false);

        JButton laterButton = createSecondaryButton(I18nUtil.getMessage(MessageKeys.UPDATE_LATER));
        laterButton.addActionListener(e -> { userChoice = 2; dispose(); });

        JButton manualButton = createSecondaryButton(I18nUtil.getMessage(MessageKeys.UPDATE_MANUAL_DOWNLOAD));
        manualButton.addActionListener(e -> { userChoice = 0; dispose(); });

        JButton autoButton = createPrimaryButton(I18nUtil.getMessage(MessageKeys.UPDATE_NOW));
        autoButton.addActionListener(e -> { userChoice = 1; dispose(); });

        buttonsPanel.add(laterButton);
        buttonsPanel.add(manualButton);
        buttonsPanel.add(autoButton);
        panel.add(buttonsPanel, BorderLayout.EAST);

        getRootPane().setDefaultButton(autoButton);
        return panel;
    }

    /** 主要操作按钮 — 不设 BUTTON_TYPE，FlatLaf 默认渲染为 accent 色（蓝底白字） */
    private JButton createPrimaryButton(String text) {
        JButton button = new JButton(text);
        button.setBorder(new EmptyBorder(8, 20, 8, 20));
        return button;
    }

    /** 次要操作按钮 — 无填充无边框，点击有 ripple 效果 */
    private JButton createSecondaryButton(String text) {
        JButton button = new JButton(text);
        button.putClientProperty(FlatClientProperties.BUTTON_TYPE, FlatClientProperties.BUTTON_TYPE_BORDERLESS);
        button.setBorder(new EmptyBorder(8, 16, 8, 16));
        return button;
    }

    public int showDialogAndGetChoice() {
        setVisible(true);
        return userChoice;
    }

    public static int showUpdateDialog(Frame parent, UpdateInfo updateInfo) {
        return new ModernUpdateDialog(parent, updateInfo).showDialogAndGetChoice();
    }
}

