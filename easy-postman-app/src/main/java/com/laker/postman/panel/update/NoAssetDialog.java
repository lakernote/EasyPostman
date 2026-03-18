package com.laker.postman.panel.update;

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
 * 有新版本但暂无安装包的提示对话框
 * <p>
 * 样式与 ModernUpdateDialog 保持一致：
 * - 渐变 Header（使用 WARNING 色系）
 * - 中间说明区
 * - 底部操作按钮：「稍后」+ 「去 GitHub 下载」
 * </p>
 */
public class NoAssetDialog extends JDialog {

    private boolean goToGitHub = false;

    // 缓存渐变，避免 resize 时频繁创建
    private transient GradientPaint cachedGradient;
    private int cachedW = -1;
    private int cachedH = -1;

    public NoAssetDialog(Frame parent, UpdateInfo updateInfo) {
        super(parent, I18nUtil.getMessage(MessageKeys.UPDATE_AVAILABLE_NO_ASSET_TITLE,
                updateInfo.getLatestVersion()), true);
        initComponents(updateInfo);
        setMinimumSize(new Dimension(520, 280));
        pack();
        setSize(Math.max(getWidth(), 560), Math.max(getHeight(), 300));
        setLocationRelativeTo(parent);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setResizable(false);
    }

    private void initComponents(UpdateInfo updateInfo) {
        setLayout(new BorderLayout());
        JPanel mainPanel = new JPanel(new BorderLayout(0, 0));
        mainPanel.add(createHeaderPanel(updateInfo), BorderLayout.NORTH);
        mainPanel.add(createBodyPanel(updateInfo), BorderLayout.CENTER);
        mainPanel.add(createButtonPanel(), BorderLayout.SOUTH);
        add(mainPanel);
    }

    // ──────────────────────────────── Header ────────────────────────────────

    private JPanel createHeaderPanel(UpdateInfo updateInfo) {
        JPanel panel = new JPanel(new BorderLayout(16, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                // 使用暖黄色渐变（WARNING 色系）区别于普通更新对话框的蓝色
                Color from = ModernColors.isDarkTheme()
                        ? new Color(92, 72, 20)
                        : new Color(255, 249, 219);
                Color to = ModernColors.isDarkTheme()
                        ? new Color(70, 60, 30)
                        : new Color(255, 237, 170);
                if (cachedGradient == null || cachedW != getWidth() || cachedH != getHeight()) {
                    cachedGradient = new GradientPaint(0, 0, from, getWidth(), getHeight(), to);
                    cachedW = getWidth();
                    cachedH = getHeight();
                }
                g2.setPaint(cachedGradient);
                g2.fillRect(0, 0, getWidth(), getHeight());
                // 装饰圆形（同 ModernUpdateDialog）
                g2.setColor(ModernColors.warningWithAlpha(25));
                g2.fillOval(-40, -40, 160, 160);
                g2.fillOval(getWidth() - 120, getHeight() - 80, 160, 120);
                g2.dispose();
            }
        };
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(22, 24, 22, 24));

        // 警告图标
        JLabel iconLabel = new JLabel(IconUtil.createThemed("icons/warning.svg", 52, 52));
        panel.add(iconLabel, BorderLayout.WEST);

        JPanel info = new JPanel();
        info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));
        info.setOpaque(false);

        JLabel titleLabel = new JLabel(I18nUtil.getMessage(
                MessageKeys.UPDATE_AVAILABLE_NO_ASSET_TITLE, updateInfo.getLatestVersion()));
        titleLabel.setFont(FontsUtil.getDefaultFontWithOffset(Font.BOLD, 7));
        titleLabel.setForeground(ModernColors.isDarkTheme()
                ? new Color(255, 220, 100) : new Color(120, 70, 0));
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        String verPrefix = I18nUtil.isChinese() ? "版本" : "Version";
        String currentV = updateInfo.getCurrentVersion() != null ? updateInfo.getCurrentVersion() : "-";
        JLabel versionLabel = new JLabel(verPrefix + "  " + currentV + "  →  " + updateInfo.getLatestVersion());
        versionLabel.setFont(FontsUtil.getDefaultFontWithOffset(Font.BOLD, 2));
        versionLabel.setForeground(ModernColors.WARNING);
        versionLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        // 发布时间
        String publishedAt = updateInfo.getReleaseInfo() != null
                ? updateInfo.getReleaseInfo().getStr("published_at", "") : "";
        if (publishedAt.length() >= 10) {
            String dateStr = publishedAt.substring(0, 10);
            JLabel dateLabel = new JLabel((I18nUtil.isChinese() ? "发布于 " : "Released on ") + dateStr);
            dateLabel.setFont(FontsUtil.getDefaultFont(Font.PLAIN));
            dateLabel.setForeground(ModernColors.getTextHint());
            dateLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            info.add(titleLabel);
            info.add(Box.createVerticalStrut(6));
            info.add(versionLabel);
            info.add(Box.createVerticalStrut(4));
            info.add(dateLabel);
        } else {
            info.add(titleLabel);
            info.add(Box.createVerticalStrut(6));
            info.add(versionLabel);
        }

        panel.add(info, BorderLayout.CENTER);
        return panel;
    }

    // ──────────────────────────────── Body ────────────────────────────────

    private JPanel createBodyPanel(UpdateInfo updateInfo) {
        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setBorder(new EmptyBorder(18, 24, 6, 24));

        // 说明文字（带 HTML 多行支持）
        String hint = I18nUtil.isChinese()
                ? buildZhHint(updateInfo)
                : buildEnHint(updateInfo);

        JLabel msgLabel = new JLabel("<html><body style='width:440px'>" + hint + "</body></html>");
        msgLabel.setFont(FontsUtil.getDefaultFontWithOffset(Font.PLAIN, 1));
        msgLabel.setForeground(ModernColors.getTextSecondary());
        panel.add(msgLabel, BorderLayout.NORTH);

        // 信息提示条（带左侧竖线）
        JPanel tipBar = buildTipBar();
        panel.add(tipBar, BorderLayout.CENTER);

        return panel;
    }

    private String buildZhHint(UpdateInfo updateInfo) {
        return "新版本 <b>" + updateInfo.getLatestVersion() + "</b> 已发布，"
                + "但当前平台的安装包 <b>尚未上传完成</b>（资源可能仍在构建或上传中）。<br>"
                + "您可以前往 GitHub 页面查看，稍后再来此处检查更新，"
                + "或在「设置 → 更新」中将更新源切换为 <b>GitHub</b>。";
    }

    private String buildEnHint(UpdateInfo updateInfo) {
        return "Version <b>" + updateInfo.getLatestVersion() + "</b> has been released, "
                + "but the installer for your platform is <b>not yet available</b> "
                + "(assets may still be building or uploading).<br>"
                + "You can visit GitHub to check, or switch the update source to "
                + "<b>GitHub</b> in Settings → Update.";
    }

    private JPanel buildTipBar() {
        JPanel bar = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                // 背景
                Color bg = ModernColors.isDarkTheme()
                        ? new Color(70, 65, 50) : new Color(255, 249, 219);
                g2.setColor(bg);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                // 左侧 3px 竖线
                g2.setColor(ModernColors.WARNING);
                g2.fillRoundRect(0, 0, 3, getHeight(), 3, 3);
                g2.dispose();
            }
        };
        bar.setOpaque(false);
        bar.setBorder(new EmptyBorder(10, 14, 10, 14));

        String tipText = I18nUtil.isChinese()
                ? "💡 也可以在「设置 → 更新源」中切换为 GitHub，程序会直接从 GitHub 获取下载链接。"
                : "💡 You can also switch the update source to GitHub in Settings → Update Source.";

        JLabel tipLabel = new JLabel("<html><body style='width:410px'>" + tipText + "</body></html>");
        tipLabel.setFont(FontsUtil.getDefaultFont(Font.PLAIN));
        tipLabel.setForeground(ModernColors.getTextHint());
        bar.add(tipLabel, BorderLayout.CENTER);
        return bar;
    }

    // ──────────────────────────────── Buttons ────────────────────────────────

    private JPanel createButtonPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(new EmptyBorder(12, 24, 18, 24));

        JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        buttonsPanel.setOpaque(false);

        JButton laterButton = new JButton(I18nUtil.getMessage(MessageKeys.UPDATE_LATER));
        laterButton.putClientProperty(FlatClientProperties.BUTTON_TYPE,
                FlatClientProperties.BUTTON_TYPE_BORDERLESS);
        laterButton.setBorder(new EmptyBorder(8, 16, 8, 16));
        laterButton.addActionListener(e -> dispose());

        JButton goButton = new JButton(I18nUtil.getMessage(MessageKeys.UPDATE_AVAILABLE_NO_ASSET_GO_GITHUB));
        goButton.setBorder(new EmptyBorder(8, 20, 8, 20));
        // 用警告色做主按钮（区别于普通更新对话框的蓝色）
        goButton.putClientProperty(FlatClientProperties.STYLE,
                "background: #F59E0B; foreground: #FFFFFF; arc: 8");
        goButton.addActionListener(e -> { goToGitHub = true; dispose(); });

        buttonsPanel.add(laterButton);
        buttonsPanel.add(goButton);
        panel.add(buttonsPanel, BorderLayout.EAST);

        getRootPane().setDefaultButton(goButton);
        return panel;
    }

    // ──────────────────────────────── Public API ────────────────────────────────

    public boolean showDialogAndWait() {
        setVisible(true);
        return goToGitHub;
    }

    /**
     * 显示对话框，返回用户是否选择前往 GitHub。
     */
    public static boolean show(Frame parent, UpdateInfo updateInfo) {
        return new NoAssetDialog(parent, updateInfo).showDialogAndWait();
    }
}

