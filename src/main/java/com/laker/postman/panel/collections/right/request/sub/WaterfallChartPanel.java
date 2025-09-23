package com.laker.postman.panel.collections.right.request.sub;

import com.laker.postman.model.HttpEventInfo;
import com.laker.postman.util.EasyPostManFontUtil;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class WaterfallChartPanel extends JPanel {
    private List<Stage> stages = new ArrayList<>();
    private long total;
    private static final int BAR_HEIGHT = 20, BAR_GAP = 10, RIGHT_PAD = 30, TOP_PAD = 28, BOTTOM_PAD = 36, BAR_RADIUS = 10, MIN_BAR_WIDTH = 12;
    private static final Color[] COLORS = {
            new Color(0x4F8EF7), new Color(0x34C759), new Color(0xAF52DE), new Color(0xFF9500), new Color(0xFF375F), new Color(0x32D1C6)
    };

    public WaterfallChartPanel(List<Stage> stages) {
        setStages(stages);
    }

    public void setStages(List<Stage> stages) {
        this.stages = stages != null ? stages : new ArrayList<>();
        total = this.stages.stream().mapToLong(s -> Math.max(0, s.end - s.start)).sum();
        revalidate();
        repaint();
    }

    @Override
    public Dimension getPreferredSize() {
        int reserveDesc = 80; //
        int labelMaxWidth = 0;
        Graphics g = getGraphics();
        if (g != null) {
            g.setFont(EasyPostManFontUtil.getDefaultFont(Font.BOLD, 13));
            for (Stage s : stages) {
                int w = g.getFontMetrics().stringWidth(s.label);
                if (w > labelMaxWidth) labelMaxWidth = w;
            }
        } else {
            labelMaxWidth = 80; // fallback
        }
        int leftPad = labelMaxWidth + 30;
        int w = leftPad + 150 + reserveDesc + RIGHT_PAD; // 200为最小bar宽度
        int h = TOP_PAD + BOTTOM_PAD + stages.size() * BAR_HEIGHT + (stages.size() - 1) * BAR_GAP;
        return new Dimension(w, Math.max(h, 100));
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
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
        int leftPad = labelMaxWidth + 30;
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
            // 0ms时也显示一条极细的线（如2px）
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
            g2.drawString(totalStr, leftPad + (availableBarSum - strW) / 2, 22);
        }
        // 绘制
        for (int i = 0, y = TOP_PAD, currentX = leftPad; i < n; i++, y += BAR_HEIGHT + BAR_GAP) {
            Stage s = stages.get(i);
            int barW = barWidths[i];
            Color color = COLORS[i % COLORS.length];
            // 0ms时画极细竖线（有高度），x为currentX
            if (barW == 2) {
                g2.setColor(color.darker());
                g2.fillRect(currentX, y, 2, BAR_HEIGHT);
            } else {
                GradientPaint gp = new GradientPaint(currentX, y, color.brighter(), currentX + barW, y + BAR_HEIGHT, color.darker());
                g2.setPaint(gp);
                g2.fillRoundRect(currentX, y, barW, BAR_HEIGHT, BAR_RADIUS, BAR_RADIUS);
            }
            g2.setFont(EasyPostManFontUtil.getDefaultFont(Font.BOLD, 13));
            g2.setColor(new Color(40, 40, 40));
            g2.drawString(s.label, leftPad - labelMaxWidth - 30 + 18, y + BAR_HEIGHT - 7);
            // 耗时始终在bar内右侧，bar太窄则不显示
            String ms = (s.end - s.start) + "ms";
            g2.setFont(EasyPostManFontUtil.getDefaultFont(Font.PLAIN, 11));
            int strW = g2.getFontMetrics().stringWidth(ms);
            if (barW > strW + 8) {
                g2.setColor(Color.WHITE);
                g2.drawString(ms, currentX + barW - strW - 6, y + BAR_HEIGHT - 7);
            }
            // 描述始终在bar右侧gap后，且不超出面板宽度
            if (s.desc != null && !s.desc.isEmpty()) {
                g2.setFont(EasyPostManFontUtil.getDefaultFont(Font.PLAIN, 11));
                g2.setColor(new Color(140, 140, 140));
                int descX = currentX + barW + gapBetweenBarAndDesc;
                int maxDescW = panelW - descX - RIGHT_PAD;
                String desc = s.desc;
                int descW = g2.getFontMetrics().stringWidth(desc);
                if (descW > maxDescW && maxDescW > 10) {
                    for (int cut = desc.length(); cut > 0; cut--) {
                        String sub = desc.substring(0, cut) + "...";
                        if (g2.getFontMetrics().stringWidth(sub) <= maxDescW) {
                            desc = sub;
                            break;
                        }
                    }
                }
                g2.drawString(desc, descX, y + BAR_HEIGHT - 7);
            }
            // 只有非0ms阶段才递增currentX
            if (barW != 2) {
                currentX += barW;
            }
        }
        g2.dispose();
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