package com.laker.postman.panel.collections.right.request.sub;

import com.laker.postman.common.constants.ModernColors;
import com.laker.postman.model.HttpEventInfo;
import com.laker.postman.util.FontsUtil;
import lombok.experimental.UtilityClass;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * 响应大小 Tooltip 构建器
 * 使用 Swing 原生面板替代 HTML 字符串，行列对齐、扁平现代。
 */
@UtilityClass
public class ResponseTooltipBuilder {

    /**
     * 构建响应大小的 Swing Tooltip 面板
     */
    public static JPanel buildSizeTooltipPanel(long uncompressedBytes,
                                               HttpEventInfo info,
                                               ResponseSizeCalculator.SizeInfo sizeInfo) {
        JPanel root = new JPanel();
        root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
        root.setOpaque(true);
        root.setBackground(ModernColors.getCardBackgroundColor());
        root.setBorder(new EmptyBorder(10, 12, 10, 14));

        // ── Response ──────────────────────────
        root.add(sectionHeader("Response"));
        root.add(Box.createVerticalStrut(4));
        root.add(row("Headers",
                ResponseSizeCalculator.formatBytes(info.getHeaderBytesReceived()),
                ModernColors.getTextPrimary()));

        if (sizeInfo.isCompressed()) {
            String enc = sizeInfo.getEncoding() != null ? sizeInfo.getEncoding() : "compressed";
            root.add(row("Body (" + enc + ")",
                    ResponseSizeCalculator.formatBytes(info.getBodyBytesReceived()),
                    ModernColors.SUCCESS));
            root.add(subRow("Uncompressed",
                    ResponseSizeCalculator.formatBytes(uncompressedBytes)));
            root.add(Box.createVerticalStrut(6));
            root.add(compressionBadge(sizeInfo));
        } else {
            root.add(row("Body",
                    ResponseSizeCalculator.formatBytes(uncompressedBytes),
                    ModernColors.getTextPrimary()));
        }

        // ── divider ───────────────────────────
        root.add(Box.createVerticalStrut(8));
        root.add(divider());
        root.add(Box.createVerticalStrut(8));

        // ── Request ───────────────────────────
        root.add(sectionHeader("Request"));
        root.add(Box.createVerticalStrut(4));
        root.add(row("Headers",
                ResponseSizeCalculator.formatBytes(info.getHeaderBytesSent()),
                ModernColors.getTextPrimary()));
        root.add(row("Body",
                ResponseSizeCalculator.formatBytes(info.getBodyBytesSent()),
                ModernColors.getTextPrimary()));

        return root;
    }

    // ── 内部构建方法 ──────────────────────────────────────────────────

    /**
     * 区块标题行（加粗，主题主色）
     */
    private static JPanel sectionHeader(String title) {
        JPanel p = row();
        JLabel lbl = new JLabel(title);
        lbl.setFont(FontsUtil.getDefaultFont(Font.BOLD));
        lbl.setForeground(ModernColors.PRIMARY);
        p.add(lbl);
        p.add(Box.createHorizontalGlue());
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, p.getPreferredSize().height + 2));
        return p;
    }

    /**
     * 普通数据行：label 左对齐，value 右对齐
     */
    private static JPanel row(String label, String value, Color valueColor) {
        JPanel p = row();
        JLabel lblKey = new JLabel(label);
        lblKey.setFont(FontsUtil.getDefaultFontWithOffset(Font.PLAIN, -1));
        lblKey.setForeground(ModernColors.getTextHint());

        JLabel lblVal = new JLabel(value);
        lblVal.setFont(FontsUtil.getDefaultFontWithOffset(Font.BOLD, -1));
        lblVal.setForeground(valueColor);

        p.add(lblKey);
        p.add(Box.createHorizontalGlue());
        p.add(Box.createHorizontalStrut(16));
        p.add(lblVal);

        // 限制到单行高度
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, p.getPreferredSize().height + 2));
        return p;
    }

    /**
     * 缩进的次级行（用于 Uncompressed 等说明）
     */
    private static JPanel subRow(String label, String value) {
        JPanel p = row();
        p.setBorder(new EmptyBorder(0, 10, 0, 0));

        JLabel lblKey = new JLabel(label);
        lblKey.setFont(FontsUtil.getDefaultFontWithOffset(Font.PLAIN, -2));
        lblKey.setForeground(ModernColors.getTextHint());

        JLabel lblVal = new JLabel(value);
        lblVal.setFont(FontsUtil.getDefaultFontWithOffset(Font.PLAIN, -2));
        lblVal.setForeground(ModernColors.getTextSecondary());

        p.add(lblKey);
        p.add(Box.createHorizontalGlue());
        p.add(Box.createHorizontalStrut(16));
        p.add(lblVal);

        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, p.getPreferredSize().height + 2));
        return p;
    }

    /**
     * gzip 压缩信息 badge
     */
    private static JPanel compressionBadge(ResponseSizeCalculator.SizeInfo sizeInfo) {
        JPanel badge = new JPanel(new GridBagLayout());
        badge.setOpaque(true);
        boolean dark = ModernColors.isDarkTheme();
        Color successColor = ModernColors.SUCCESS;
        badge.setBackground(new Color(
                successColor.getRed(), successColor.getGreen(), successColor.getBlue(),
                dark ? 30 : 20));
        badge.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(
                        successColor.getRed(), successColor.getGreen(), successColor.getBlue(), 70), 1, true),
                BorderFactory.createEmptyBorder(5, 8, 5, 8)
        ));

        Font f = FontsUtil.getDefaultFontWithOffset(Font.PLAIN, -1);
        Font fb = FontsUtil.getDefaultFontWithOffset(Font.BOLD, -1);

        String enc = sizeInfo.getEncoding() != null ? sizeInfo.getEncoding() : "compressed";
        JLabel ratioKey = new JLabel(enc + " ratio");
        ratioKey.setFont(f);
        ratioKey.setForeground(ModernColors.SUCCESS_DARK);
        JLabel ratioVal = new JLabel(String.format("%.1f%%", sizeInfo.getCompressionRatio()));
        ratioVal.setFont(fb);
        ratioVal.setForeground(ModernColors.SUCCESS);

        JLabel savedKey = new JLabel("saved");
        savedKey.setFont(f);
        savedKey.setForeground(ModernColors.SUCCESS_DARK);
        JLabel savedVal = new JLabel(ResponseSizeCalculator.formatBytes(sizeInfo.getSavedBytes()));
        savedVal.setFont(fb);
        savedVal.setForeground(ModernColors.SUCCESS);

        GridBagConstraints c = new GridBagConstraints();

        c.gridx = 0;
        c.gridy = 0;
        c.anchor = GridBagConstraints.WEST;
        c.weightx = 1;
        c.insets = new Insets(0, 0, 2, 12);
        badge.add(ratioKey, c);

        c.gridx = 1;
        c.weightx = 0;
        c.anchor = GridBagConstraints.EAST;
        c.insets = new Insets(0, 0, 2, 0);
        badge.add(ratioVal, c);

        c.gridx = 0;
        c.gridy = 1;
        c.weightx = 1;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(0, 0, 0, 12);
        badge.add(savedKey, c);

        c.gridx = 1;
        c.weightx = 0;
        c.anchor = GridBagConstraints.EAST;
        c.insets = new Insets(0, 0, 0, 0);
        badge.add(savedVal, c);

        // 包一层让 badge 撑满行宽
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);
        wrapper.setAlignmentX(Component.LEFT_ALIGNMENT);
        wrapper.add(badge, BorderLayout.CENTER);
        return wrapper;
    }

    /**
     * 1px 水平分隔线
     */
    private static JPanel divider() {
        JPanel line = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                g.setColor(ModernColors.getBorderLightColor());
                g.drawLine(0, 0, getWidth(), 0);
            }
        };
        line.setOpaque(false);
        line.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        line.setPreferredSize(new Dimension(0, 1));
        return line;
    }

    /**
     * 水平 row 容器
     */
    private static JPanel row() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.X_AXIS));
        p.setOpaque(false);
        p.setAlignmentX(Component.LEFT_ALIGNMENT);
        return p;
    }

}
