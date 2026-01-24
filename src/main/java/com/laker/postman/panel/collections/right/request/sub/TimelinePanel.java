package com.laker.postman.panel.collections.right.request.sub;

import com.formdev.flatlaf.FlatLaf;
import com.laker.postman.model.HttpEventInfo;
import com.laker.postman.util.FontsUtil;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;

import javax.swing.*;
import java.awt.*;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

public class TimelinePanel extends JPanel {
    private List<Stage> stages = new ArrayList<>();
    private long total;

    // 优化后的瀑布条颜色 - 更现代、柔和的色系
    private static final Color[] COLORS = {
            new Color(0x4A90E2), // 蓝色 - DNS
            new Color(0x50E3C2), // 青绿色 - Socket
            new Color(0x9B59B6), // 紫色 - SSL
            new Color(0xF39C12), // 橙色 - Request
            new Color(0xE74C3C), // 红色 - Waiting (TTFB)
            new Color(0x2ECC71)  // 绿色 - Download
    };

    // 暗色主题下的瀑布条颜色
    private static final Color[] DARK_COLORS = {
            new Color(0x5DA5E8), // 更亮的蓝色
            new Color(0x5FE9CE), // 更亮的青绿色
            new Color(0xA569BD), // 更亮的紫色
            new Color(0xF5A623), // 更亮的橙色
            new Color(0xEC644B), // 更亮的红色
            new Color(0x52D681)  // 更亮的绿色
    };

    private HttpEventInfo httpEventInfo;

    // 瀑布图参数
    private static final int BAR_HEIGHT = 22; // 瀑布条的高度（增加到22）
    private static final int BAR_GAP = 6; // 瀑布条之间的垂直间距（增加）
    private static final int RIGHT_PAD = 32; // 右侧内边距
    private static final int TOP_PAD = 20; // 顶部内边距（增加）
    private static final int BOTTOM_PAD = 8; // 底部内边距
    private static final int BAR_RADIUS = 6; // 瀑布条圆角半径（减小，更现代）
    private static final int MIN_BAR_WIDTH = 14; // 瀑布条最小宽度
    private static final int LABEL_LEFT_PAD = 24; // 标签左侧内边距
    private static final int LABEL_RIGHT_PAD = 14; // 标签右侧内边距（增加）
    private static final int DESC_LEFT_PAD = 20; // 描述文本左侧内边距（增加）

    // 信息区参数
    private static final int INFO_BLOCK_H_GAP = 18; // 信息区块之间的水平间距（增加）
    private static final int INFO_BLOCK_V_GAP = 6; // 信息区块之间的垂直间距（增加）
    private static final int INFO_TEXT_LINE_HEIGHT = 20; // 信息文本行高（增加）
    private static final int INFO_TEXT_EXTRA_GAP = 4; // 信息区每项之间额外空白
    private static final int INFO_TEXT_BOTTOM_PAD = 16;  // 信息区底部内边距
    private static final int INFO_TEXT_LEFT_PAD = 24; // 信息区左侧内边距（增加）

    /**
     * 检查当前是否为暗色主题
     */
    private boolean isDarkTheme() {
        return FlatLaf.isLafDark();
    }

    /**
     * 获取信息区背景色 - 主题适配
     */
    private Color getInfoBgColor() {
        return isDarkTheme() ? new Color(45, 47, 49) : new Color(250, 251, 252);
    }

    /**
     * 获取信息区边框色 - 主题适配
     */
    private Color getInfoBorderColor() {
        return isDarkTheme() ? new Color(70, 73, 75) : new Color(220, 225, 230);
    }

    /**
     * 获取标签文字颜色 - 主题适配
     */
    private Color getLabelTextColor() {
        return isDarkTheme() ? new Color(210, 210, 210) : new Color(50, 50, 50);
    }

    /**
     * 获取信息文字颜色 - 主题适配
     */
    private Color getInfoTextColor() {
        return isDarkTheme() ? new Color(190, 190, 190) : new Color(80, 80, 80);
    }

    /**
     * 获取描述文字颜色 - 主题适配
     */
    private Color getDescTextColor() {
        return isDarkTheme() ? new Color(130, 130, 130) : new Color(120, 120, 120);
    }

    /**
     * 获取分隔线颜色 - 主题适配
     */
    private Color getSeparatorColor() {
        return isDarkTheme() ? new Color(65, 65, 65) : new Color(220, 220, 220);
    }

    /**
     * 获取瀑布条颜色数组 - 根据主题返回
     */
    private Color[] getBarColors() {
        return isDarkTheme() ? DARK_COLORS : COLORS;
    }

    public TimelinePanel(List<Stage> stages, HttpEventInfo httpEventInfo) {
        setLayout(new BorderLayout()); // 明确使用 BorderLayout
        this.httpEventInfo = httpEventInfo;
        setStages(stages);
    }

    public void setStages(List<Stage> stages) {
        this.stages = stages != null ? stages : new ArrayList<>();
        total = this.stages.stream().mapToLong(s -> Math.max(0, s.end - s.start)).sum();
        revalidate();
        repaint();
    }

    public void setHttpEventInfo(HttpEventInfo info) {
        this.httpEventInfo = info;
        repaint();
    }

    @Override
    public Dimension getPreferredSize() {
        // 统一计算 label、desc 最大宽度
        int labelMaxWidth = 80, descMaxWidth = 80;
        Graphics g = getGraphics();
        if (g != null) {
            g.setFont(FontsUtil.getDefaultFont(Font.BOLD));
            for (Stage s : stages) {
                int w = g.getFontMetrics().stringWidth(s.label);
                if (w > labelMaxWidth) labelMaxWidth = w;
            }
            g.setFont(FontsUtil.getDefaultFont(Font.PLAIN));
            ; // 使用用户设置的字体大小
            for (Stage s : stages) {
                if (s.desc != null && !s.desc.isEmpty()) {
                    int w = g.getFontMetrics().stringWidth(s.desc);
                    if (w > descMaxWidth) descMaxWidth = w;
                }
            }
        }
        int leftPad = LABEL_LEFT_PAD + labelMaxWidth + LABEL_RIGHT_PAD;
        int infoTextBlockHeight = getInfoBlockHeight();
        int w = leftPad + 150 + DESC_LEFT_PAD + descMaxWidth + RIGHT_PAD;
        int h = infoTextBlockHeight + TOP_PAD + BOTTOM_PAD + stages.size() * BAR_HEIGHT + (stages.size() - 1) * BAR_GAP;
        h = Math.min(h, 400); // 限制最大高度为400
        return new Dimension(w, Math.max(h, 120));
    }

    // 信息区高度自适应（去除标题高度和分隔线高度）
    private int getInfoBlockHeight() {
        int infoLines = getInfoLinesCount();
        int minLines = 7;
        // 每行高度加上额外空白
        return INFO_BLOCK_V_GAP + Math.max(infoLines, minLines) * (INFO_TEXT_LINE_HEIGHT + INFO_TEXT_EXTRA_GAP) + INFO_TEXT_BOTTOM_PAD;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // 1. 绘制信息区
        int infoTextBlockHeight = getInfoBlockHeight();
        g2.setColor(getInfoBgColor());
        g2.fillRoundRect(INFO_BLOCK_H_GAP, INFO_BLOCK_V_GAP, getWidth() - 2 * INFO_BLOCK_H_GAP, infoTextBlockHeight - 2, 10, 10);
        g2.setColor(getInfoBorderColor());
        g2.setStroke(new BasicStroke(1.0f));
        g2.drawRoundRect(INFO_BLOCK_H_GAP, INFO_BLOCK_V_GAP, getWidth() - 2 * INFO_BLOCK_H_GAP, infoTextBlockHeight - 2, 10, 10);
        int infoY = INFO_BLOCK_V_GAP + INFO_TEXT_LINE_HEIGHT - 3;
        g2.setFont(FontsUtil.getDefaultFont(Font.PLAIN));
        g2.setColor(getInfoTextColor());
        // 动态渲染字段并在remote address/cipher name下方画线
        String protocol = null, localAddr = null, remoteAddr = null, tls = null, cipher = null, certCN = null, issuerCN = null, validUntil = null;
        if (httpEventInfo != null) {
            protocol = httpEventInfo.getProtocol() != null ? httpEventInfo.getProtocol().toString() : null;
            localAddr = httpEventInfo.getLocalAddress();
            remoteAddr = httpEventInfo.getRemoteAddress();
            tls = httpEventInfo.getTlsVersion();
            cipher = httpEventInfo.getCipherName();
            if (httpEventInfo.getPeerCertificates() != null && !httpEventInfo.getPeerCertificates().isEmpty()) {
                var cert = httpEventInfo.getPeerCertificates().get(0);
                if (cert instanceof X509Certificate x509) {
                    certCN = x509.getSubjectX500Principal().getName();
                    issuerCN = x509.getIssuerX500Principal().getName();
                    validUntil = x509.getNotAfter().toString();
                }
            }
        }
        int labelX = INFO_TEXT_LEFT_PAD;
        int valueXOffset = 110;
        int lineStartX = INFO_TEXT_LEFT_PAD;
        int lineEndX = getWidth() - INFO_TEXT_LEFT_PAD;
        int remoteLineY = -1, cipherLineY = -1;
        // HTTP Version
        g2.setFont(FontsUtil.getDefaultFont(Font.BOLD));
        g2.setColor(getInfoTextColor());
        g2.drawString(I18nUtil.getMessage(MessageKeys.WATERFALL_HTTP_VERSION), labelX, infoY);
        g2.setFont(FontsUtil.getDefaultFont(Font.PLAIN));
        g2.drawString(protocol != null ? protocol : "-", labelX + valueXOffset, infoY);
        infoY += INFO_TEXT_LINE_HEIGHT + INFO_TEXT_EXTRA_GAP;
        // Local Address
        g2.setFont(FontsUtil.getDefaultFont(Font.BOLD));
        g2.drawString(I18nUtil.getMessage(MessageKeys.WATERFALL_LOCAL_ADDRESS), labelX, infoY);
        g2.setFont(FontsUtil.getDefaultFont(Font.PLAIN));
        g2.drawString(localAddr != null ? localAddr : "-", labelX + valueXOffset, infoY);
        infoY += INFO_TEXT_LINE_HEIGHT + INFO_TEXT_EXTRA_GAP;
        // Remote Address
        g2.setFont(FontsUtil.getDefaultFont(Font.BOLD));
        g2.drawString(I18nUtil.getMessage(MessageKeys.WATERFALL_REMOTE_ADDRESS), labelX, infoY);
        g2.setFont(FontsUtil.getDefaultFont(Font.PLAIN));
        g2.drawString(remoteAddr != null ? remoteAddr : "-", labelX + valueXOffset, infoY);
        if (remoteAddr != null && !remoteAddr.isEmpty()) remoteLineY = infoY + 5;
        infoY += INFO_TEXT_LINE_HEIGHT + INFO_TEXT_EXTRA_GAP;
        if (remoteLineY > 0) {
            g2.setColor(getSeparatorColor());
            g2.drawLine(lineStartX, remoteLineY, lineEndX, remoteLineY);
            g2.setColor(getInfoTextColor());
        }
        // TLS Protocol
        g2.setFont(FontsUtil.getDefaultFont(Font.BOLD));
        g2.drawString(I18nUtil.getMessage(MessageKeys.WATERFALL_TLS_PROTOCOL), labelX, infoY);
        g2.setFont(FontsUtil.getDefaultFont(Font.PLAIN));
        g2.drawString(tls != null ? tls : "-", labelX + valueXOffset, infoY);
        infoY += INFO_TEXT_LINE_HEIGHT + INFO_TEXT_EXTRA_GAP;
        // Cipher Name
        g2.setFont(FontsUtil.getDefaultFont(Font.BOLD));
        g2.drawString(I18nUtil.getMessage(MessageKeys.WATERFALL_CIPHER_NAME), labelX, infoY);
        g2.setFont(FontsUtil.getDefaultFont(Font.PLAIN));
        g2.drawString(cipher != null ? cipher : "-", labelX + valueXOffset, infoY);
        if (cipher != null && !cipher.isEmpty()) cipherLineY = infoY + 5;
        infoY += INFO_TEXT_LINE_HEIGHT + INFO_TEXT_EXTRA_GAP;
        if (cipherLineY > 0) {
            g2.setColor(getSeparatorColor());
            g2.drawLine(lineStartX, cipherLineY, lineEndX, cipherLineY);
            g2.setColor(getInfoTextColor());
        }
        // Certificate CN
        g2.setFont(FontsUtil.getDefaultFont(Font.BOLD));
        g2.drawString(I18nUtil.getMessage(MessageKeys.WATERFALL_CERTIFICATE_CN), labelX, infoY);
        g2.setFont(FontsUtil.getDefaultFont(Font.PLAIN));
        g2.drawString(certCN != null ? certCN : "-", labelX + valueXOffset, infoY);
        infoY += INFO_TEXT_LINE_HEIGHT + INFO_TEXT_EXTRA_GAP;
        // Issuer CN
        g2.setFont(FontsUtil.getDefaultFont(Font.BOLD));
        g2.drawString(I18nUtil.getMessage(MessageKeys.WATERFALL_ISSUER_CN), labelX, infoY);
        g2.setFont(FontsUtil.getDefaultFont(Font.PLAIN));
        g2.drawString(issuerCN != null ? issuerCN : "-", labelX + valueXOffset, infoY);
        infoY += INFO_TEXT_LINE_HEIGHT + INFO_TEXT_EXTRA_GAP;
        // Valid Until
        g2.setFont(FontsUtil.getDefaultFont(Font.BOLD));
        g2.drawString(I18nUtil.getMessage(MessageKeys.WATERFALL_VALID_UNTIL), labelX, infoY);
        g2.setFont(FontsUtil.getDefaultFont(Font.PLAIN));
        g2.drawString(validUntil != null ? validUntil : "-", labelX + valueXOffset, infoY);
        infoY += INFO_TEXT_LINE_HEIGHT + INFO_TEXT_EXTRA_GAP;

        // SSL Certificate Warning - 如果有警告则显示
        if (httpEventInfo != null && httpEventInfo.getSslCertWarning() != null && !httpEventInfo.getSslCertWarning().isEmpty()) {
            g2.setFont(FontsUtil.getDefaultFont(Font.BOLD));
            g2.setColor(new Color(220, 53, 69)); // 红色警告
            g2.drawString("Cert Warning:", labelX, infoY);
            g2.setFont(FontsUtil.getDefaultFont(Font.PLAIN));

            // 处理过长的警告文本，可能需要换行或截断
            String warning = httpEventInfo.getSslCertWarning();
            int maxWarningWidth = getWidth() - INFO_TEXT_LEFT_PAD - valueXOffset - 30;
            FontMetrics fm = g2.getFontMetrics();

            if (fm.stringWidth(warning) > maxWarningWidth) {
                // 截断文本并添加省略号
                String truncated = warning;
                while (fm.stringWidth(truncated + "...") > maxWarningWidth && !truncated.isEmpty()) {
                    truncated = truncated.substring(0, truncated.length() - 1);
                }
                g2.drawString(truncated + "...", labelX + valueXOffset, infoY);
            } else {
                g2.drawString(warning, labelX + valueXOffset, infoY);
            }
            g2.setColor(getInfoTextColor()); // 恢复默认颜色
        }

        // 2. 绘制瀑布条区
        int gapBetweenBarAndDesc = 20;
        int labelMaxWidth = 0;
        int descMaxWidth = 0;
        g2.setFont(FontsUtil.getDefaultFont(Font.BOLD));
        for (Stage s : stages) {
            int w = g2.getFontMetrics().stringWidth(s.label);
            if (w > labelMaxWidth) labelMaxWidth = w;
        }
        g2.setFont(FontsUtil.getDefaultFont(Font.PLAIN));
        for (Stage s : stages) {
            if (s.desc != null && !s.desc.isEmpty()) {
                int w = g2.getFontMetrics().stringWidth(s.desc);
                if (w > descMaxWidth) descMaxWidth = w;
            }
        }
        int leftPad = LABEL_LEFT_PAD + labelMaxWidth + LABEL_RIGHT_PAD;
        int panelW = getWidth();
        int n = stages.size();
        int minBarSum = MIN_BAR_WIDTH * n;
        int availableBarSum = panelW - leftPad - gapBetweenBarAndDesc - descMaxWidth - RIGHT_PAD;
        int totalBarSum = 0;
        long totalDuration = total > 0 ? total : 1;
        int[] barWidths = new int[n];
        // 先按比例分配理论宽度
        for (int i = 0; i < n; i++) {
            long duration = Math.max(0, stages.get(i).end - stages.get(i).start);
            double ratio = (double) duration / totalDuration;
            if (duration == 0) {
                barWidths[i] = 2;
            } else {
                barWidths[i] = MIN_BAR_WIDTH + (int) Math.round(ratio * (availableBarSum - minBarSum));
            }
            totalBarSum += barWidths[i];
        }
        // 如果总宽度超出可用宽度，则按比例缩小
        if (totalBarSum > availableBarSum) {
            double scale = (double) availableBarSum / totalBarSum;
            totalBarSum = 0;
            for (int i = 0; i < n; i++) {
                barWidths[i] = Math.max(MIN_BAR_WIDTH, (int) Math.round(barWidths[i] * scale));
                totalBarSum += barWidths[i];
            }
        }

        // 绘制
        int barY = getInfoBlockHeight() + TOP_PAD;
        int currentX = leftPad;

        // 计算文字垂直居中的位置
        g2.setFont(FontsUtil.getDefaultFont(Font.PLAIN));
        FontMetrics fm = g2.getFontMetrics();
        int textYOffset = (BAR_HEIGHT - fm.getHeight()) / 2 + fm.getAscent();

        for (int i = 0; i < n; i++, barY += BAR_HEIGHT + BAR_GAP) {
            Stage s = stages.get(i);
            int barW = barWidths[i];
            Color[] barColors = getBarColors();
            Color color = barColors[i % barColors.length];

            // label 区域
            g2.setFont(FontsUtil.getDefaultFont(Font.BOLD));
            g2.setColor(getLabelTextColor());
            String label = s.label;
            int labelStrW = g2.getFontMetrics().stringWidth(label);
            boolean labelTruncated = false;
            if (labelStrW > labelMaxWidth && labelMaxWidth > 10) {
                for (int cut = label.length(); cut > 0; cut--) {
                    String sub = label.substring(0, cut) + "...";
                    if (g2.getFontMetrics().stringWidth(sub) <= labelMaxWidth) {
                        label = sub;
                        labelTruncated = true;
                        break;
                    }
                }
            }
            g2.drawString(label, labelX, barY + textYOffset);
            // 设置 label 悬浮提示
            if (labelTruncated) {
                setToolTipText(s.label);
            }

            // bar 区域 - 优化渐变和阴影效果
            if (barW <= 3) {
                // 非常窄的条使用纯色
                g2.setColor(color);
                g2.fillRect(currentX, barY, barW, BAR_HEIGHT);
            } else {
                // 添加微妙的阴影效果（仅在亮色主题下）
                if (!isDarkTheme()) {
                    g2.setColor(new Color(0, 0, 0, 10));
                    g2.fillRoundRect(currentX + 1, barY + 2, barW, BAR_HEIGHT, BAR_RADIUS, BAR_RADIUS);
                }

                // 使用垂直渐变，从稍亮到稍暗
                float brightnessMultiplier = isDarkTheme() ? 1.15f : 1.1f;
                float darknessMultiplier = isDarkTheme() ? 0.9f : 0.85f;

                Color topColor = brighter(color, brightnessMultiplier);
                Color bottomColor = darker(color, darknessMultiplier);

                GradientPaint gp = new GradientPaint(
                        currentX, (float) barY, topColor,
                        currentX, (float) (barY + BAR_HEIGHT), bottomColor
                );
                g2.setPaint(gp);
                g2.fillRoundRect(currentX, barY, barW, BAR_HEIGHT, BAR_RADIUS, BAR_RADIUS);

                // 添加高光效果（顶部细线）
                g2.setColor(new Color(255, 255, 255, isDarkTheme() ? 20 : 40));
                g2.fillRoundRect(currentX, barY, barW, 2, BAR_RADIUS, BAR_RADIUS);
            }

            // 耗时始终在bar内右侧，bar太窄则不显示
            String ms = (s.end - s.start) + "ms";
            g2.setFont(FontsUtil.getDefaultFont(Font.PLAIN));
            int strW = g2.getFontMetrics().stringWidth(ms);
            if (barW > strW + 12) {
                // 使用更清晰的白色文字
                g2.setColor(Color.WHITE);
                g2.drawString(ms, currentX + barW - strW - 6, barY + textYOffset);
            }
            // desc 区域
            if (s.desc != null && !s.desc.isEmpty()) {
                g2.setFont(FontsUtil.getDefaultFont(Font.PLAIN));
                g2.setColor(getDescTextColor());
                int descX = currentX + barW + DESC_LEFT_PAD;
                int maxDescW = panelW - descX - RIGHT_PAD;
                String desc = s.desc;
                int descW = g2.getFontMetrics().stringWidth(desc);
                boolean descTruncated = false;
                if (descW > maxDescW && maxDescW > 10) {
                    for (int cut = desc.length(); cut > 0; cut--) {
                        String sub = desc.substring(0, cut) + "...";
                        if (g2.getFontMetrics().stringWidth(sub) <= maxDescW) {
                            desc = sub;
                            descTruncated = true;
                            break;
                        }
                    }
                }
                g2.drawString(desc, descX, barY + textYOffset);
                // 设置 desc 悬浮提示
                if (descTruncated) {
                    setToolTipText(s.desc);
                }
            }
            // 只有非0ms阶段才递增currentX
            if (barW > 3) {
                currentX += barW;
            }
        }
        g2.dispose();
    }

    /**
     * 调整颜色使其变亮
     */
    private Color brighter(Color color, float factor) {
        int r = Math.min(255, (int) (color.getRed() * factor));
        int g = Math.min(255, (int) (color.getGreen() * factor));
        int b = Math.min(255, (int) (color.getBlue() * factor));
        return new Color(r, g, b);
    }

    /**
     * 调整颜色使其变暗
     */
    private Color darker(Color color, float factor) {
        int r = (int) (color.getRed() * factor);
        int g = (int) (color.getGreen() * factor);
        int b = (int) (color.getBlue() * factor);
        return new Color(r, g, b);
    }

    // 计算实际显示的info行数（用于动态布局）
    private int getInfoLinesCount() {
        // 基础行数：HTTP Version, Local Address, Remote Address, TLS Protocol, Cipher Name, Certificate CN, Issuer CN, Valid Until
        int lines = 7;

        // 如果有 SSL 证书警告，添加一行
        if (httpEventInfo != null && httpEventInfo.getSslCertWarning() != null && !httpEventInfo.getSslCertWarning().isEmpty()) {
            lines += 1;
        }

        return lines;
    }

    public static class Stage {
        public final String label;
        public final long start, end;
        public final String desc;

        public Stage(String label, long start, long end, String desc) {
            this.label = label;
            this.start = start;
            this.end = end;
            this.desc = desc;
        }

    }

    // 工具方法：根据 HttpEventInfo 生成六大阶段（即使为0也显示）
    public static List<Stage> buildStandardStages(HttpEventInfo info) {
        String[] labels = {
                I18nUtil.getMessage(MessageKeys.WATERFALL_STAGE_DNS),
                I18nUtil.getMessage(MessageKeys.WATERFALL_STAGE_SOCKET),
                I18nUtil.getMessage(MessageKeys.WATERFALL_STAGE_SSL),
                I18nUtil.getMessage(MessageKeys.WATERFALL_STAGE_REQUEST_SEND),
                I18nUtil.getMessage(MessageKeys.WATERFALL_STAGE_WAITING),
                I18nUtil.getMessage(MessageKeys.WATERFALL_STAGE_CONTENT_DOWNLOAD)
        };
        String[] descs = {
                I18nUtil.getMessage(MessageKeys.WATERFALL_STAGE_DESC_DNS),
                I18nUtil.getMessage(MessageKeys.WATERFALL_STAGE_DESC_SOCKET),
                I18nUtil.getMessage(MessageKeys.WATERFALL_STAGE_DESC_SSL),
                I18nUtil.getMessage(MessageKeys.WATERFALL_STAGE_DESC_REQUEST_SEND),
                I18nUtil.getMessage(MessageKeys.WATERFALL_STAGE_DESC_WAITING),
                I18nUtil.getMessage(MessageKeys.WATERFALL_STAGE_DESC_CONTENT_DOWNLOAD),
        };
        long[] starts = new long[6];
        long[] ends = new long[6];
        for (int i = 0; i < 6; i++) {
            starts[i] = 0;
            ends[i] = 0;
        }
        if (info != null) {
            long dnsS = info.getDnsStart();
            long dnsE = info.getDnsEnd();
            long connS = info.getConnectStart();
            long connE = info.getConnectEnd();
            long sslS = info.getSecureConnectStart();
            long sslE = info.getSecureConnectEnd();
            long reqHS = info.getRequestHeadersStart();
            long reqHE = info.getRequestHeadersEnd();
            long reqBS = info.getRequestBodyStart();
            long reqBE = info.getRequestBodyEnd();
            long respHS = info.getResponseHeadersStart();
            long respBS = info.getResponseBodyStart();
            long respBE = info.getResponseBodyEnd();
            // DNS
            if (dnsS > 0 && dnsE > dnsS) {
                starts[0] = dnsS;
                ends[0] = dnsE;
            }
            // Socket
            if (connS > 0 && connE > connS) {
                long socketEnd = (sslS > connS && sslE > sslS && sslE <= connE) ? sslS : connE;
                starts[1] = connS;
                ends[1] = socketEnd;
            }
            // SSL
            if (sslS > 0 && sslE > sslS) {
                starts[2] = sslS;
                ends[2] = sslE;
            }
            // Request Send
            long reqStart = Math.min(reqHS > 0 ? reqHS : Long.MAX_VALUE, reqBS > 0 ? reqBS : Long.MAX_VALUE);
            long reqEnd = Math.max(reqHE, reqBE);
            if (reqEnd > reqStart) {
                starts[3] = reqStart;
                ends[3] = reqEnd;
            }
            // Waiting (TTFB)
            if (reqEnd > 0 && respHS > reqEnd) {
                starts[4] = reqEnd;
                ends[4] = respHS;
            }
            // Content Download
            if (respBS > 0 && respBE > respBS) {
                starts[5] = respBS;
                ends[5] = respBE;
            }
        }
        List<Stage> stages = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            stages.add(new Stage(labels[i], starts[i], ends[i], descs[i]));
        }
        return stages;
    }
}
