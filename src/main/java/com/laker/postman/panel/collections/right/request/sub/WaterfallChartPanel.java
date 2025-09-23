package com.laker.postman.panel.collections.right.request.sub;

import com.laker.postman.util.EasyPostManFontUtil;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class WaterfallChartPanel extends JPanel {
    private List<Stage> stages;
    private long total;
    private int hoveredIndex = -1;
    private static final int BAR_HEIGHT = 22;
    private static final int BAR_GAP = 8;
    private static final int LEFT_PAD = 90;
    private static final int RIGHT_PAD = 30;
    private static final int TOP_PAD = 18;
    private static final int BOTTOM_PAD = 28;
    private static final int BAR_RADIUS = 7;
    private static final int MAX_BAR_WIDTH = 420;
    private static final int MIN_BAR_WIDTH = 8;
    private static final Color[] COLORS = {
            new Color(0xFBC02D), // DNS
            new Color(0x43A047), // TCP
            new Color(0x00897B), // SSL
            new Color(0x1976D2), // Request
            new Color(0xE64A19), // Waiting
            new Color(0x7E57C2)  // Download
    };

    public WaterfallChartPanel(List<Stage> stages) {
        setStages(stages);
    }

    public void setStages(List<Stage> stages) {
        this.stages = stages != null ? stages : new ArrayList<>();
        this.total = 0;
        if (!this.stages.isEmpty()) {
            long sum = 0;
            for (Stage s : this.stages) {
                sum += Math.max(0, s.end - s.start);
            }
            this.total = sum;
        }
        revalidate();
        repaint();
    }

    private int getBarWidthPx(int idx) {
        if (stages == null || stages.isEmpty() || total <= 0) return MIN_BAR_WIDTH;
        Stage s = stages.get(idx);
        long duration = Math.max(0, s.end - s.start);
        int minWidthSum = MIN_BAR_WIDTH * stages.size();
        int remainWidth = MAX_BAR_WIDTH - minWidthSum;
        if (remainWidth < 0) remainWidth = 0;
        if (duration == 0) return MIN_BAR_WIDTH;
        // 按比例分配剩余宽度
        double ratio = (double) duration / total;
        return MIN_BAR_WIDTH + (int) Math.round(ratio * remainWidth);
    }

    @Override
    public Dimension getPreferredSize() {
        int h = TOP_PAD + BOTTOM_PAD + stages.size() * BAR_HEIGHT + (stages.size() - 1) * BAR_GAP;
        return new Dimension(LEFT_PAD + MAX_BAR_WIDTH + RIGHT_PAD, Math.max(h, 80));
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        // 顶部居中显示 total
        if (total > 0) {
            String totalStr = "Total: " + total + " ms";
            g2.setFont(EasyPostManFontUtil.getDefaultFont(Font.BOLD, 13));
            int strW = g2.getFontMetrics().stringWidth(totalStr);
            int x = LEFT_PAD + (MAX_BAR_WIDTH - strW) / 2;
            g2.setColor(new Color(40, 40, 40));
            g2.drawString(totalStr, x, 14);
        }
        int y0 = TOP_PAD;
        for (int i = 0; i < stages.size(); i++) {
            Stage s = stages.get(i);
            int barY = y0 + i * (BAR_HEIGHT + BAR_GAP);
            int barX = LEFT_PAD;
            for (int j = 0; j < i; j++) {
                barX += getBarWidthPx(j);
            }
            int barW = getBarWidthPx(i);
            Color color = COLORS[i % COLORS.length];
            if (i == hoveredIndex) {
                color = color.darker();
            }
            g2.setColor(color);
            g2.fillRoundRect(barX, barY, barW, BAR_HEIGHT, BAR_RADIUS, BAR_RADIUS);
            // 阶段名
            g2.setColor(new Color(60, 60, 60));
            g2.drawString(s.label, 10, barY + BAR_HEIGHT - 6);
            // 耗时
            String ms = (s.end - s.start) + " ms";
            g2.setColor(Color.WHITE);
            int strW = g2.getFontMetrics().stringWidth(ms);
            if (barW > strW + 12) {
                g2.drawString(ms, barX + barW - strW - 6, barY + BAR_HEIGHT - 6);
            } else {
                g2.setColor(new Color(60, 60, 60));
                g2.drawString(ms, barX + barW + 8, barY + BAR_HEIGHT - 6);
            }
            // 说明
            if (s.desc != null && !s.desc.isEmpty()) {
                g2.setColor(new Color(120, 120, 120));
                g2.drawString(s.desc, barX + barW + 60, barY + BAR_HEIGHT - 6);
            }
        }
        g2.dispose();
    }

    public static class Stage {
        public final String label;
        public final long start;
        public final long end;
        public final String desc;

        public Stage(String label, long start, long end, String desc) {
            this.label = label;
            this.start = start;
            this.end = end;
            this.desc = desc;
        }

    }

    // 工具方法：根据 HttpEventInfo 生成六大阶段（即使为0也显示）
    public static List<Stage> buildStandardStages(com.laker.postman.model.HttpEventInfo info) {
        String[] labels = {"DNS", "Socket", "SSL", "Request Send", "Waiting (TTFB)", "Content Download"};
        String[] descs = {" (DnsStart→DnsEnd)", " (ConnectStart→ConnectEnd)", " (SecureConnectStart→SecureConnectEnd)", " (Headers/Body)", " (RequestEnd→RespHeadStart)", " (RespBodyStart→RespBodyEnd)"};
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
                long socketStart = connS;
                long socketEnd = (sslS > connS && sslE > sslS && sslE <= connE) ? sslS : connE;
                if (socketEnd > socketStart) {
                    starts[1] = socketStart;
                    ends[1] = socketEnd;
                }
            }
            // SSL
            if (sslS > 0 && sslE > sslS) {
                starts[2] = sslS;
                ends[2] = sslE;
            }
            // Request Send
            long reqStart = Math.min(reqHS > 0 ? reqHS : Long.MAX_VALUE, reqBS > 0 ? reqBS : Long.MAX_VALUE);
            long reqEnd = Math.max(reqHE, reqBE);
            if (reqStart < Long.MAX_VALUE && reqEnd > reqStart) {
                starts[3] = reqStart;
                ends[3] = reqEnd;
            }
            // Waiting (TTFB)
            if (respHS > 0 && reqEnd > 0 && respHS > reqEnd) {
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