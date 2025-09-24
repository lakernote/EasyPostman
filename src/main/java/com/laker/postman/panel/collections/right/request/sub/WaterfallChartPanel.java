package com.laker.postman.panel.collections.right.request.sub;

import com.laker.postman.model.HttpEventInfo;
import com.laker.postman.util.EasyPostManFontUtil;

import javax.swing.*;
import java.awt.*;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

public class WaterfallChartPanel extends JPanel {
    private List<Stage> stages = new ArrayList<>();
    private long total;
    // 布局参数集中管理
    private static final int BAR_HEIGHT = 20, BAR_GAP = 10, RIGHT_PAD = 30, TOP_PAD = 28, BOTTOM_PAD = 36, BAR_RADIUS = 10, MIN_BAR_WIDTH = 12;
    private static final int LABEL_LEFT_PAD = 24, LABEL_RIGHT_PAD = 12, DESC_LEFT_PAD = 18;
    private static final int INFO_BLOCK_H_GAP = 16, INFO_BLOCK_V_GAP = 10;
    private static final Color[] COLORS = {
            new Color(0x4F8EF7), new Color(0x34C759), new Color(0xAF52DE), new Color(0xFF9500), new Color(0xFF375F), new Color(0x32D1C6)
    };
    private HttpEventInfo httpEventInfo;

    private static final int INFO_TEXT_LINE_HEIGHT = 18;
    private static final int INFO_TEXT_BOTTOM_PAD = 10;  // 信息区底部内边距
    private static final int INFO_TEXT_LEFT_PAD = 20;
    private static final Color INFO_BG_COLOR = new Color(245, 247, 250);
    private static final Color INFO_BORDER_COLOR = new Color(220, 220, 220);

    public WaterfallChartPanel(List<Stage> stages, HttpEventInfo httpEventInfo) {
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
            g.setFont(EasyPostManFontUtil.getDefaultFont(Font.BOLD, 13));
            for (Stage s : stages) {
                int w = g.getFontMetrics().stringWidth(s.label);
                if (w > labelMaxWidth) labelMaxWidth = w;
            }
            g.setFont(EasyPostManFontUtil.getDefaultFont(Font.PLAIN, 11));
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
        // 去除标题和分隔线
        return INFO_BLOCK_V_GAP + infoLines * INFO_TEXT_LINE_HEIGHT + INFO_TEXT_BOTTOM_PAD;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        // 1. 绘制信息区（去除标题和分隔线）
        int infoTextBlockHeight = getInfoBlockHeight();
        g2.setColor(INFO_BG_COLOR);
        g2.fillRoundRect(INFO_BLOCK_H_GAP, INFO_BLOCK_V_GAP, getWidth() - 2 * INFO_BLOCK_H_GAP, infoTextBlockHeight - 2, 8, 8);
        g2.setColor(INFO_BORDER_COLOR);
        g2.drawRoundRect(INFO_BLOCK_H_GAP, INFO_BLOCK_V_GAP, getWidth() - 2 * INFO_BLOCK_H_GAP, infoTextBlockHeight - 2, 8, 8);
        int infoY = INFO_BLOCK_V_GAP + INFO_TEXT_LINE_HEIGHT - 3;
        g2.setFont(EasyPostManFontUtil.getDefaultFont(Font.PLAIN, 12));
        g2.setColor(Color.DARK_GRAY);
        // 动态渲染字段并在remote address/cipher name下方画线
        if (httpEventInfo != null) {
            String protocol = httpEventInfo.getProtocol() != null ? httpEventInfo.getProtocol().toString() : null;
            String localAddr = httpEventInfo.getLocalAddress();
            String remoteAddr = httpEventInfo.getRemoteAddress();
            String tls = httpEventInfo.getTlsVersion();
            String cipher = httpEventInfo.getCipherName();
            String certCN = null, issuerCN = null, validUntil = null;
            if (httpEventInfo.getPeerCertificates() != null && !httpEventInfo.getPeerCertificates().isEmpty()) {
                var cert = httpEventInfo.getPeerCertificates().get(0);
                if (cert instanceof X509Certificate x509) {
                    certCN = x509.getSubjectX500Principal().getName();
                    issuerCN = x509.getIssuerX500Principal().getName();
                    validUntil = x509.getNotAfter().toString();
                }
            }
            int labelX = INFO_TEXT_LEFT_PAD;
            int valueXOffset = 110; // value 与 label 间距
            int lineStartX = INFO_TEXT_LEFT_PAD;
            int lineEndX = getWidth() - INFO_TEXT_LEFT_PAD;
            int remoteLineY = -1, cipherLineY = -1;
            if (notEmpty(protocol)) {
                g2.setFont(EasyPostManFontUtil.getDefaultFont(Font.BOLD, 12));
                g2.setColor(Color.DARK_GRAY);
                g2.drawString("HTTP Version:", labelX, infoY);
                g2.setFont(EasyPostManFontUtil.getDefaultFont(Font.PLAIN, 12));
                g2.setColor(Color.DARK_GRAY);
                g2.drawString(protocol, labelX + valueXOffset, infoY);
                infoY += INFO_TEXT_LINE_HEIGHT;
            }
            if (notEmpty(localAddr)) {
                g2.setFont(EasyPostManFontUtil.getDefaultFont(Font.BOLD, 12));
                g2.setColor(Color.DARK_GRAY);
                g2.drawString("Local Address:", labelX, infoY);
                g2.setFont(EasyPostManFontUtil.getDefaultFont(Font.PLAIN, 12));
                g2.setColor(Color.DARK_GRAY);
                g2.drawString(localAddr, labelX + valueXOffset, infoY);
                infoY += INFO_TEXT_LINE_HEIGHT;
            }
            if (notEmpty(remoteAddr)) {
                g2.setFont(EasyPostManFontUtil.getDefaultFont(Font.BOLD, 12));
                g2.setColor(Color.DARK_GRAY);
                g2.drawString("Remote Address:", labelX, infoY);
                g2.setFont(EasyPostManFontUtil.getDefaultFont(Font.PLAIN, 12));
                g2.setColor(Color.DARK_GRAY);
                g2.drawString(remoteAddr, labelX + valueXOffset, infoY);
                remoteLineY = infoY + 5;
                infoY += INFO_TEXT_LINE_HEIGHT;
            }
            if (remoteLineY > 0) {
                g2.setColor(new Color(230,230,230));
                g2.drawLine(lineStartX, remoteLineY, lineEndX, remoteLineY);
                g2.setColor(Color.DARK_GRAY);
            }
            if (notEmpty(tls)) {
                g2.setFont(EasyPostManFontUtil.getDefaultFont(Font.BOLD, 12));
                g2.setColor(Color.DARK_GRAY);
                g2.drawString("TLS Protocol:", labelX, infoY);
                g2.setFont(EasyPostManFontUtil.getDefaultFont(Font.PLAIN, 12));
                g2.setColor(Color.DARK_GRAY);
                g2.drawString(tls, labelX + valueXOffset, infoY);
                infoY += INFO_TEXT_LINE_HEIGHT;
            }
            if (notEmpty(cipher)) {
                g2.setFont(EasyPostManFontUtil.getDefaultFont(Font.BOLD, 12));
                g2.setColor(Color.DARK_GRAY);
                g2.drawString("Cipher Name:", labelX, infoY);
                g2.setFont(EasyPostManFontUtil.getDefaultFont(Font.PLAIN, 12));
                g2.setColor(Color.DARK_GRAY);
                g2.drawString(cipher, labelX + valueXOffset, infoY);
                cipherLineY = infoY + 5;
                infoY += INFO_TEXT_LINE_HEIGHT;
            }
            if (cipherLineY > 0) {
                g2.setColor(new Color(230,230,230));
                g2.drawLine(lineStartX, cipherLineY, lineEndX, cipherLineY);
                g2.setColor(Color.DARK_GRAY);
            }
            if (notEmpty(certCN)) {
                g2.setFont(EasyPostManFontUtil.getDefaultFont(Font.BOLD, 12));
                g2.setColor(Color.DARK_GRAY);
                g2.drawString("Certificate CN:", labelX, infoY);
                g2.setFont(EasyPostManFontUtil.getDefaultFont(Font.PLAIN, 12));
                g2.setColor(Color.DARK_GRAY);
                g2.drawString(certCN, labelX + valueXOffset, infoY);
                infoY += INFO_TEXT_LINE_HEIGHT;
            }
            if (notEmpty(issuerCN)) {
                g2.setFont(EasyPostManFontUtil.getDefaultFont(Font.BOLD, 12));
                g2.setColor(Color.DARK_GRAY);
                g2.drawString("Issuer CN:", labelX, infoY);
                g2.setFont(EasyPostManFontUtil.getDefaultFont(Font.PLAIN, 12));
                g2.setColor(Color.DARK_GRAY);
                g2.drawString(issuerCN, labelX + valueXOffset, infoY);
                infoY += INFO_TEXT_LINE_HEIGHT;
            }
            if (notEmpty(validUntil)) {
                g2.setFont(EasyPostManFontUtil.getDefaultFont(Font.BOLD, 12));
                g2.setColor(Color.DARK_GRAY);
                g2.drawString("Valid Until:", labelX, infoY);
                g2.setFont(EasyPostManFontUtil.getDefaultFont(Font.PLAIN, 12));
                g2.setColor(Color.DARK_GRAY);
                g2.drawString(validUntil, labelX + valueXOffset, infoY);
                infoY += INFO_TEXT_LINE_HEIGHT;
            }
        }
        // 2. 绘制瀑布条区
        int gapBetweenBarAndDesc = 20;
        int labelMaxWidth = 0;
        int descMaxWidth = 0;
        g2.setFont(EasyPostManFontUtil.getDefaultFont(Font.BOLD, 13));
        for (Stage s : stages) {
            int w = g2.getFontMetrics().stringWidth(s.label);
            if (w > labelMaxWidth) labelMaxWidth = w;
        }
        g2.setFont(EasyPostManFontUtil.getDefaultFont(Font.PLAIN, 11));
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
        // 顶部total
        if (total > 0) {
            String totalStr = "Total: " + total + " ms";
            g2.setFont(EasyPostManFontUtil.getDefaultFont(Font.BOLD, 15));
            int strW = g2.getFontMetrics().stringWidth(totalStr);
            g2.setColor(new Color(30, 30, 30));
            g2.drawString(totalStr, leftPad + (availableBarSum - strW) / 2, getInfoBlockHeight() + 22);
        }
        // 绘制
        int barY = getInfoBlockHeight() + TOP_PAD;
        int currentX = leftPad;
        for (int i = 0; i < n; i++, barY += BAR_HEIGHT + BAR_GAP) {
            Stage s = stages.get(i);
            int barW = barWidths[i];
            Color color = COLORS[i % COLORS.length];
            // label 区域
            g2.setFont(EasyPostManFontUtil.getDefaultFont(Font.BOLD, 13));
            g2.setColor(new Color(40, 40, 40));
            int labelX = LABEL_LEFT_PAD;
            int labelW = labelMaxWidth;
            String label = s.label;
            int labelStrW = g2.getFontMetrics().stringWidth(label);
            boolean labelTruncated = false;
            if (labelStrW > labelW && labelW > 10) {
                for (int cut = label.length(); cut > 0; cut--) {
                    String sub = label.substring(0, cut) + "...";
                    if (g2.getFontMetrics().stringWidth(sub) <= labelW) {
                        label = sub;
                        labelTruncated = true;
                        break;
                    }
                }
            }
            g2.drawString(label, labelX, barY + BAR_HEIGHT - 7);
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
            g2.setFont(EasyPostManFontUtil.getDefaultFont(Font.PLAIN, 11));
            int strW = g2.getFontMetrics().stringWidth(ms);
            if (barW > strW + 8) {
                g2.setColor(Color.WHITE);
                g2.drawString(ms, currentX + barW - strW - 6, barY + BAR_HEIGHT - 7);
            }
            // desc 区域
            if (s.desc != null && !s.desc.isEmpty()) {
                g2.setFont(EasyPostManFontUtil.getDefaultFont(Font.PLAIN, 11));
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
                g2.drawString(desc, descX, barY + BAR_HEIGHT - 7);
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
        int count = 0;
        if (httpEventInfo != null) {
            if (notEmpty(httpEventInfo.getProtocol() != null ? httpEventInfo.getProtocol().toString() : null)) count++;
            if (notEmpty(httpEventInfo.getLocalAddress())) count++;
            if (notEmpty(httpEventInfo.getRemoteAddress())) count++;
            if (notEmpty(httpEventInfo.getTlsVersion())) count++;
            if (notEmpty(httpEventInfo.getCipherName())) count++;
            String certCN = null, issuerCN = null, validUntil = null;
            if (httpEventInfo.getPeerCertificates() != null && !httpEventInfo.getPeerCertificates().isEmpty()) {
                var cert = httpEventInfo.getPeerCertificates().get(0);
                if (cert instanceof X509Certificate x509) {
                    certCN = x509.getSubjectX500Principal().getName();
                    issuerCN = x509.getIssuerX500Principal().getName();
                    validUntil = x509.getNotAfter().toString();
                }
            }
            if (notEmpty(certCN)) count++;
            if (notEmpty(issuerCN)) count++;
            if (notEmpty(validUntil)) count++;
        }
        return Math.max(count, 1); // 至少1行，避免高度为0
    }

    private boolean notEmpty(String s) {
        return s != null && !s.isEmpty();
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
        String[] labels = {"DNS", "Socket", "SSL", "Request Send", "Waiting (TTFB)", "Content Download"};
        String[] descs = {
                " (DnsStart→DnsEnd)",
                " (ConnectStart→ConnectEnd)",
                " (SecureConnectStart→SecureConnectEnd)",
                " (RequestHeadersStart→RequestBodyEnd)",
                " (RequestBodyEnd→ResponseHeadersStart)",
                " (ResponseBodyStart→ResponseBodyEnd)"
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

