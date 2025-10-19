package com.laker.postman.panel.collections.right.request.sub;

import com.laker.postman.model.HttpEventInfo;
import com.laker.postman.util.EasyPostManFontUtil;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;

import javax.swing.*;
import java.awt.*;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

public class TimelineWaterfallChartPanel extends JPanel {
    private List<Stage> stages = new ArrayList<>();
    private long total;
    private static final Color[] COLORS = {
            new Color(0x4F8EF7), new Color(0x34C759), new Color(0xAF52DE), new Color(0xFF9500), new Color(0xFF375F), new Color(0x32D1C6)
    };
    private HttpEventInfo httpEventInfo;

    // 瀑布图参数
    private static final int BAR_HEIGHT = 16; // 瀑布条的高度
    private static final int BAR_GAP = 3; // 瀑布条之间的垂直间距
    private static final int RIGHT_PAD = 30; // 右侧内边距
    private static final int TOP_PAD = 15; // 顶部内边距
    private static final int BOTTOM_PAD = 5; // 底部内边距
    private static final int BAR_RADIUS = 10; // 瀑布条圆角半径
    private static final int MIN_BAR_WIDTH = 12; // 瀑布条最小宽度
    private static final int LABEL_LEFT_PAD = 24; // 标签左侧内边距
    private static final int LABEL_RIGHT_PAD = 12; // 标签右侧内边距
    private static final int DESC_LEFT_PAD = 18; // 描述文本左侧内边距

    // 信息区参数
    private static final int INFO_BLOCK_H_GAP = 16; // 信息区块之间的水平间距
    private static final int INFO_BLOCK_V_GAP = 3; // 信息区块之间的垂直间距
    private static final int INFO_TEXT_LINE_HEIGHT = 16; // 信息文本行高
    private static final int INFO_TEXT_EXTRA_GAP = 6; // 信息区每项之间额外空白
    private static final int INFO_TEXT_BOTTOM_PAD = 20;  // 信息区底部内边距
    private static final int INFO_TEXT_LEFT_PAD = 20; // 信息区左侧内边距
    private static final Color INFO_BG_COLOR = new Color(245, 247, 250);
    private static final Color INFO_BORDER_COLOR = new Color(220, 220, 220);

    public TimelineWaterfallChartPanel(List<Stage> stages, HttpEventInfo httpEventInfo) {
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
            g.setFont(EasyPostManFontUtil.getDefaultFont(Font.BOLD, 12));
            for (Stage s : stages) {
                int w = g.getFontMetrics().stringWidth(s.label);
                if (w > labelMaxWidth) labelMaxWidth = w;
            }
            g.setFont(EasyPostManFontUtil.getDefaultFont(Font.PLAIN, 12));
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
        // 1. 绘制信息区
        int infoTextBlockHeight = getInfoBlockHeight();
        g2.setColor(INFO_BG_COLOR);
        g2.fillRoundRect(INFO_BLOCK_H_GAP, INFO_BLOCK_V_GAP, getWidth() - 2 * INFO_BLOCK_H_GAP, infoTextBlockHeight - 2, 8, 8);
        g2.setColor(INFO_BORDER_COLOR);
        g2.drawRoundRect(INFO_BLOCK_H_GAP, INFO_BLOCK_V_GAP, getWidth() - 2 * INFO_BLOCK_H_GAP, infoTextBlockHeight - 2, 8, 8);
        int infoY = INFO_BLOCK_V_GAP + INFO_TEXT_LINE_HEIGHT - 3;
        g2.setFont(EasyPostManFontUtil.getDefaultFont(Font.PLAIN, 12));
        g2.setColor(Color.DARK_GRAY);
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
        g2.setFont(EasyPostManFontUtil.getDefaultFont(Font.BOLD, 12));
        g2.setColor(Color.DARK_GRAY);
        g2.drawString(I18nUtil.getMessage(MessageKeys.WATERFALL_HTTP_VERSION), labelX, infoY);
        g2.setFont(EasyPostManFontUtil.getDefaultFont(Font.PLAIN, 12));
        g2.drawString(protocol != null ? protocol : "", labelX + valueXOffset, infoY);
        infoY += INFO_TEXT_LINE_HEIGHT + INFO_TEXT_EXTRA_GAP;
        // Local Address
        g2.setFont(EasyPostManFontUtil.getDefaultFont(Font.BOLD, 12));
        g2.drawString(I18nUtil.getMessage(MessageKeys.WATERFALL_LOCAL_ADDRESS), labelX, infoY);
        g2.setFont(EasyPostManFontUtil.getDefaultFont(Font.PLAIN, 12));
        g2.drawString(localAddr != null ? localAddr : "", labelX + valueXOffset, infoY);
        infoY += INFO_TEXT_LINE_HEIGHT + INFO_TEXT_EXTRA_GAP;
        // Remote Address
        g2.setFont(EasyPostManFontUtil.getDefaultFont(Font.BOLD, 12));
        g2.drawString(I18nUtil.getMessage(MessageKeys.WATERFALL_REMOTE_ADDRESS), labelX, infoY);
        g2.setFont(EasyPostManFontUtil.getDefaultFont(Font.PLAIN, 12));
        g2.drawString(remoteAddr != null ? remoteAddr : "", labelX + valueXOffset, infoY);
        if (remoteAddr != null && !remoteAddr.isEmpty()) remoteLineY = infoY + 5;
        infoY += INFO_TEXT_LINE_HEIGHT + INFO_TEXT_EXTRA_GAP;
        if (remoteLineY > 0) {
            g2.setColor(new Color(230, 230, 230));
            g2.drawLine(lineStartX, remoteLineY, lineEndX, remoteLineY);
            g2.setColor(Color.DARK_GRAY);
        }
        // TLS Protocol
        g2.setFont(EasyPostManFontUtil.getDefaultFont(Font.BOLD, 12));
        g2.drawString(I18nUtil.getMessage(MessageKeys.WATERFALL_TLS_PROTOCOL), labelX, infoY);
        g2.setFont(EasyPostManFontUtil.getDefaultFont(Font.PLAIN, 12));
        g2.drawString(tls != null ? tls : "", labelX + valueXOffset, infoY);
        infoY += INFO_TEXT_LINE_HEIGHT + INFO_TEXT_EXTRA_GAP;
        // Cipher Name
        g2.setFont(EasyPostManFontUtil.getDefaultFont(Font.BOLD, 12));
        g2.drawString(I18nUtil.getMessage(MessageKeys.WATERFALL_CIPHER_NAME), labelX, infoY);
        g2.setFont(EasyPostManFontUtil.getDefaultFont(Font.PLAIN, 12));
        g2.drawString(cipher != null ? cipher : "", labelX + valueXOffset, infoY);
        if (cipher != null && !cipher.isEmpty()) cipherLineY = infoY + 5;
        infoY += INFO_TEXT_LINE_HEIGHT + INFO_TEXT_EXTRA_GAP;
        if (cipherLineY > 0) {
            g2.setColor(new Color(230, 230, 230));
            g2.drawLine(lineStartX, cipherLineY, lineEndX, cipherLineY);
            g2.setColor(Color.DARK_GRAY);
        }
        // Certificate CN
        g2.setFont(EasyPostManFontUtil.getDefaultFont(Font.BOLD, 12));
        g2.drawString(I18nUtil.getMessage(MessageKeys.WATERFALL_CERTIFICATE_CN), labelX, infoY);
        g2.setFont(EasyPostManFontUtil.getDefaultFont(Font.PLAIN, 12));
        g2.drawString(certCN != null ? certCN : "", labelX + valueXOffset, infoY);
        infoY += INFO_TEXT_LINE_HEIGHT + INFO_TEXT_EXTRA_GAP;
        // Issuer CN
        g2.setFont(EasyPostManFontUtil.getDefaultFont(Font.BOLD, 12));
        g2.drawString(I18nUtil.getMessage(MessageKeys.WATERFALL_ISSUER_CN), labelX, infoY);
        g2.setFont(EasyPostManFontUtil.getDefaultFont(Font.PLAIN, 12));
        g2.drawString(issuerCN != null ? issuerCN : "", labelX + valueXOffset, infoY);
        infoY += INFO_TEXT_LINE_HEIGHT + INFO_TEXT_EXTRA_GAP;
        // Valid Until
        g2.setFont(EasyPostManFontUtil.getDefaultFont(Font.BOLD, 12));
        g2.drawString(I18nUtil.getMessage(MessageKeys.WATERFALL_VALID_UNTIL), labelX, infoY);
        g2.setFont(EasyPostManFontUtil.getDefaultFont(Font.PLAIN, 12));
        g2.drawString(validUntil != null ? validUntil : "", labelX + valueXOffset, infoY);
        infoY += INFO_TEXT_LINE_HEIGHT + INFO_TEXT_EXTRA_GAP;

        // SSL Certificate Warning - 如果有警告则显示
        if (httpEventInfo != null && httpEventInfo.getSslCertWarning() != null && !httpEventInfo.getSslCertWarning().isEmpty()) {
            g2.setFont(EasyPostManFontUtil.getDefaultFont(Font.BOLD, 12));
            g2.setColor(new Color(220, 53, 69)); // 红色警告
            g2.drawString("Cert Warning:", labelX, infoY);
            g2.setFont(EasyPostManFontUtil.getDefaultFont(Font.PLAIN, 12));

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
            g2.setColor(Color.DARK_GRAY); // 恢复默认颜色
        }

        // 2. 绘制瀑布条区
        int gapBetweenBarAndDesc = 20;
        int labelMaxWidth = 0;
        int descMaxWidth = 0;
        g2.setFont(EasyPostManFontUtil.getDefaultFont(Font.BOLD, 12));
        for (Stage s : stages) {
            int w = g2.getFontMetrics().stringWidth(s.label);
            if (w > labelMaxWidth) labelMaxWidth = w;
        }
        g2.setFont(EasyPostManFontUtil.getDefaultFont(Font.PLAIN, 12));
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
        g2.setFont(EasyPostManFontUtil.getDefaultFont(Font.PLAIN, 12));
        FontMetrics fm = g2.getFontMetrics();
        int textYOffset = (BAR_HEIGHT - fm.getHeight()) / 2 + fm.getAscent();

        for (int i = 0; i < n; i++, barY += BAR_HEIGHT + BAR_GAP) {
            Stage s = stages.get(i);
            int barW = barWidths[i];
            Color color = COLORS[i % COLORS.length];
            // label 区域
            g2.setFont(EasyPostManFontUtil.getDefaultFont(Font.BOLD, 12));
            g2.setColor(new Color(40, 40, 40));
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
            // bar 区域
            if (barW == 2) {
                g2.setColor(color.darker());
                g2.fillRect(currentX, barY, 2, BAR_HEIGHT);
            } else {
                GradientPaint gp = new GradientPaint(currentX, barY, color.brighter(), currentX + barW, barY + BAR_HEIGHT, color.darker());
                g2.setPaint(gp);
                g2.fillRoundRect(currentX, barY, barW, BAR_HEIGHT, BAR_RADIUS, BAR_RADIUS);
            }
            // 耗时始终在bar内右侧，bar太窄则不显示
            String ms = (s.end - s.start) + "ms";
            g2.setFont(EasyPostManFontUtil.getDefaultFont(Font.PLAIN, 12));
            int strW = g2.getFontMetrics().stringWidth(ms);
            if (barW > strW + 8) {
                g2.setColor(Color.WHITE);
                g2.drawString(ms, currentX + barW - strW - 6, barY + textYOffset);
            }
            // desc 区域
            if (s.desc != null && !s.desc.isEmpty()) {
                g2.setFont(EasyPostManFontUtil.getDefaultFont(Font.PLAIN, 12));
                g2.setColor(new Color(140, 140, 140));
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
            if (barW != 2) {
                currentX += barW;
            }
        }
        g2.dispose();
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
