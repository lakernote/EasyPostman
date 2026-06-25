package com.laker.postman.panel.collections.editor.request.sub;

import com.laker.postman.http.runtime.model.HttpEventInfo;
import com.laker.postman.common.component.ToolWindowSurfaceStyle;
import com.laker.postman.util.FontsUtil;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

public class TimelinePanel extends JPanel {
    private List<Stage> stages = new ArrayList<>();
    private long total;

    private HttpEventInfo httpEventInfo;

    // 交互状态
    private int hoveredBarIndex = -1; // 当前鼠标悬停的瀑布条索引

    // 瀑布图参数
    private static final int SECTION_RADIUS = 8;
    private static final int BAR_HEIGHT = 16;
    private static final int BAR_GAP = 6;
    private static final int RIGHT_PAD = 28;
    private static final int TOP_PAD = 34;
    private static final int BOTTOM_PAD = 18;
    private static final int BAR_RADIUS = 5;
    private static final int MIN_BAR_WIDTH = 14;
    private static final int SUB_MILLISECOND_MARKER_WIDTH = 4;
    private static final int SUB_MILLISECOND_VISUAL_SLOT_WIDTH = 8;
    private static final int LABEL_LEFT_PAD = 22;
    private static final int LABEL_RIGHT_PAD = 16;

    // 信息区参数
    private static final int INFO_BLOCK_H_GAP = 16;
    private static final int INFO_BLOCK_V_GAP = 10;
    private static final int INFO_TEXT_TOP_PAD = 10;
    private static final int INFO_TEXT_LINE_HEIGHT = 18;
    private static final int INFO_TEXT_EXTRA_GAP = 4; // 信息区每项之间额外空白
    private static final int INFO_TEXT_BOTTOM_PAD = 12;
    private static final int INFO_TEXT_LEFT_PAD = 18;
    private static final int INFO_COLUMN_GAP = 32;
    private static final int INFO_LABEL_MIN_WIDTH = 54;
    static final int INFO_LABEL_VALUE_GAP = 8;
    static final int INFO_VALUE_MIN_WIDTH = 120;

    // 区域间距
    private static final int AREA_GAP = 10;

    public TimelinePanel(List<Stage> stages, HttpEventInfo httpEventInfo) {
        setLayout(new BorderLayout()); // 明确使用 BorderLayout
        ToolWindowSurfaceStyle.applyCard(this);
        this.httpEventInfo = httpEventInfo;
        setStages(stages);
        ToolTipManager.sharedInstance().registerComponent(this);
        setupMouseListeners(); // 设置鼠标监听器
    }

    public void setStages(List<Stage> stages) {
        this.stages = stages != null ? stages : new ArrayList<>();
        total = this.stages.stream().mapToLong(s -> Math.max(0, s.end - s.start)).sum();
        revalidate();
        repaint();
    }

    public void setHttpEventInfo(HttpEventInfo info) {
        this.httpEventInfo = info;
        revalidate();
        repaint();
    }

    /**
     * 设置鼠标监听器，实现交互效果
     */
    private void setupMouseListeners() {
        addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            @Override
            public void mouseMoved(java.awt.event.MouseEvent e) {
                int oldHovered = hoveredBarIndex;
                hoveredBarIndex = getStageRowIndexAtPoint(e.getPoint());

                // 只在悬停状态改变时重绘
                if (oldHovered != hoveredBarIndex) {
                    setCursor(Cursor.getDefaultCursor());
                    repaint();
                }
            }
        });

        addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                if (hoveredBarIndex != -1) {
                    hoveredBarIndex = -1;
                    setCursor(Cursor.getDefaultCursor());
                    repaint();
                }
            }
        });
    }

    /**
     * 获取鼠标位置对应的瀑布条索引
     */
    private int getBarIndexAtPoint(Point p) {
        return getStageIndexAtPoint(p, true);
    }

    private int getStageRowIndexAtPoint(Point p) {
        return getStageIndexAtPoint(p, false);
    }

    private int getStageIndexAtPoint(Point p, boolean requireVisibleDurationBar) {
        if (p == null || stages.isEmpty()) {
            return -1;
        }

        int infoTextBlockHeight = getInfoBlockHeight();
        int barY = INFO_BLOCK_V_GAP + infoTextBlockHeight + AREA_GAP + TOP_PAD;
        int sectionX = INFO_BLOCK_H_GAP;
        int stageLabelX = sectionX + LABEL_LEFT_PAD;
        int barStartX = getBarStartX(sectionX);
        int trackRightX = Math.max(barStartX + MIN_BAR_WIDTH, getWidth() - RIGHT_PAD);
        if (p.x < stageLabelX || p.x > trackRightX) {
            return -1;
        }

        for (int i = 0; i < stages.size(); i++) {
            int barBottom = barY + BAR_HEIGHT;
            if (p.y >= barY && p.y <= barBottom) {
                Stage stage = stages.get(i);
                if (!requireVisibleDurationBar || durationMillis(stage) > 0) {
                    return i;
                }
                return -1;
            }
            barY += BAR_HEIGHT + BAR_GAP;
        }

        return -1;
    }

    @Override
    public String getToolTipText(MouseEvent event) {
        if (event == null) {
            return null;
        }
        Point point = event.getPoint();
        if (isPointInInfoArea(point)) {
            return buildConnectionInfoTooltip();
        }
        int stageIndex = getStageRowIndexAtPoint(point);
        if (stageIndex >= 0 && stageIndex < stages.size()) {
            Stage stage = stages.get(stageIndex);
            return buildStageTooltip(stage);
        }
        return null;
    }

    private String buildStageTooltip(Stage stage) {
        StringBuilder tooltip = new StringBuilder("<html><b>")
                .append(escapeHtml(stage.label))
                .append("</b><br/>")
                .append(escapeHtml(I18nUtil.getMessage(MessageKeys.WATERFALL_STAGE_DURATION)))
                .append(": ")
                .append(escapeHtml(formatStageDuration(stage)));
        String note = getStageTimingNote(stage);
        if (note != null && !note.isBlank()) {
            tooltip.append("<br/>")
                    .append(escapeHtml(note));
        }
        if (stage.desc != null && !stage.desc.isBlank()) {
            tooltip.append("<br/>")
                    .append(escapeHtml(stage.desc));
        }
        tooltip.append("</html>");
        return tooltip.toString();
    }

    @Override
    public Dimension getPreferredSize() {
        // 统一计算 label 最大宽度；阶段说明只在 tooltip 中展示，不参与瀑布条宽度预算。
        int labelMaxWidth = 80;
        FontMetrics metrics = getFontMetrics(FontsUtil.getDefaultFont(Font.BOLD));
        if (metrics != null) {
            for (Stage s : stages) {
                int w = metrics.stringWidth(s.label);
                if (w > labelMaxWidth) labelMaxWidth = w;
            }
        }
        int leftPad = LABEL_LEFT_PAD + labelMaxWidth + LABEL_RIGHT_PAD;
        int infoTextBlockHeight = getInfoBlockHeight();
        int w = leftPad + 480 + RIGHT_PAD;
        // 总高度 = 顶部间距 + 信息区高度 + 底部间距 + 区域间距 + 顶部内边距 + 瀑布条总高度 + 底部内边距 + 底部间距
        int barsTotalHeight = stages.isEmpty() ? 0 : stages.size() * BAR_HEIGHT + (stages.size() - 1) * BAR_GAP;
        int h = INFO_BLOCK_V_GAP + infoTextBlockHeight + AREA_GAP + TOP_PAD + barsTotalHeight + BOTTOM_PAD + INFO_BLOCK_V_GAP;
        return new Dimension(w, Math.max(h, 150));
    }

    // 信息区高度自适应（去除标题高度和分隔线高度）
    private int getInfoBlockHeight() {
        int infoLines = getInfoRowsCount();
        return INFO_TEXT_TOP_PAD
                + Math.max(infoLines, 1) * (INFO_TEXT_LINE_HEIGHT + INFO_TEXT_EXTRA_GAP)
                + INFO_TEXT_BOTTOM_PAD;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // 0. 绘制面板背景
        g2.setColor(TimelineTheme.panelBackground());
        g2.fillRect(0, 0, getWidth(), getHeight());

        // 1. 绘制信息区
        int infoTextBlockHeight = getInfoBlockHeight();
        int sectionX = INFO_BLOCK_H_GAP;
        int sectionWidth = Math.max(0, getWidth() - 2 * INFO_BLOCK_H_GAP);
        g2.setColor(TimelineTheme.infoBackground());
        g2.fillRoundRect(sectionX, INFO_BLOCK_V_GAP, sectionWidth, infoTextBlockHeight, SECTION_RADIUS, SECTION_RADIUS);
        g2.setColor(TimelineTheme.infoBorder());
        g2.setStroke(new BasicStroke(1.0f));
        g2.drawRoundRect(sectionX, INFO_BLOCK_V_GAP, sectionWidth, infoTextBlockHeight, SECTION_RADIUS, SECTION_RADIUS);
        drawConnectionInfo(g2, sectionX, INFO_BLOCK_V_GAP, sectionWidth);

        // 2. 绘制瀑布条区域背景和边框
        int n = stages.size();
        int barsTotalHeight = n > 0 ? n * BAR_HEIGHT + (n - 1) * BAR_GAP : 0;
        int barAreaTop = INFO_BLOCK_V_GAP + infoTextBlockHeight + AREA_GAP;
        int barAreaHeight = TOP_PAD + barsTotalHeight + BOTTOM_PAD;

        // 绘制瀑布条区域背景
        g2.setColor(TimelineTheme.barAreaBackground());
        g2.fillRoundRect(sectionX, barAreaTop, sectionWidth, barAreaHeight, SECTION_RADIUS, SECTION_RADIUS);

        // 绘制瀑布条区域边框（与信息区边框一致）
        g2.setColor(TimelineTheme.infoBorder());
        g2.setStroke(new BasicStroke(1.0f));
        g2.drawRoundRect(sectionX, barAreaTop, sectionWidth, barAreaHeight, SECTION_RADIUS, SECTION_RADIUS);

        // 3. 绘制瀑布条
        int labelMaxWidth = 0;
        g2.setFont(FontsUtil.getDefaultFont(Font.BOLD));
        labelMaxWidth = getStageLabelMaxWidth(g2.getFontMetrics(), 0);
        int stageLabelX = sectionX + LABEL_LEFT_PAD;
        int barStartX = stageLabelX + labelMaxWidth + LABEL_RIGHT_PAD;
        int panelW = getWidth();
        boolean hasSubMillisecondStage = stages.stream().anyMatch(TimelinePanel::isSubMillisecondStage);
        int trackRightX = Math.max(barStartX + MIN_BAR_WIDTH, panelW - RIGHT_PAD);
        int trackWidth = Math.max(MIN_BAR_WIDTH, trackRightX - barStartX);
        long displayTotalDuration = Math.max(0, total);
        int[] barWidths = resolveStageVisualWidths(stages, displayTotalDuration, trackWidth);

        // 绘制
        // 瀑布条Y坐标 = 信息区高度 + 信息区底部间距 + 区域间距 + 瀑布条区域顶部内边距
        int barY = barAreaTop + TOP_PAD;
        int currentX = barStartX;
        drawScaleHeader(g2, stageLabelX, barStartX, trackWidth, barAreaTop, displayTotalDuration, hasSubMillisecondStage);

        // 计算文字垂直居中的位置
        g2.setFont(FontsUtil.getDefaultFont(Font.PLAIN));
        FontMetrics fm = g2.getFontMetrics();
        int textYOffset = (BAR_HEIGHT - fm.getHeight()) / 2 + fm.getAscent();

        for (int i = 0; i < n; i++, barY += BAR_HEIGHT + BAR_GAP) {
            Stage s = stages.get(i);
            int barW = barWidths[i];
            Color[] barColors = TimelineTheme.barColors();
            Color color = barColors[i % barColors.length];

            // 绘制悬停背景高亮
            boolean isHovered = (i == hoveredBarIndex);
            if (isHovered) {
                g2.setColor(TimelineTheme.hoveredBarBackground());
                g2.fillRoundRect(sectionX + 8, barY - 4, Math.max(0, sectionWidth - 16), BAR_HEIGHT + 8, 6, 6);
            }

            // label 区域
            g2.setFont(FontsUtil.getDefaultFont(Font.BOLD));
            g2.setColor(isHovered ? TimelineTheme.hoveredLabelText() : TimelineTheme.labelText());
            String label = s.label;
            int labelStrW = g2.getFontMetrics().stringWidth(label);
            if (labelStrW > labelMaxWidth && labelMaxWidth > 10) {
                for (int cut = label.length(); cut > 0; cut--) {
                    String sub = label.substring(0, cut) + "...";
                    if (g2.getFontMetrics().stringWidth(sub) <= labelMaxWidth) {
                        label = sub;
                        break;
                    }
                }
            }
            g2.drawString(label, stageLabelX, barY + textYOffset);

            // 先绘制整行时间轨道，让瀑布条不再悬浮在大块背景上。
            g2.setStroke(new BasicStroke(1.0f));
            g2.setColor(TimelineTheme.barTrackBackground());
            g2.fillRoundRect(barStartX, barY, trackWidth, BAR_HEIGHT, BAR_RADIUS, BAR_RADIUS);
            g2.setColor(TimelineTheme.barTrackBorder());
            g2.drawRoundRect(barStartX, barY, trackWidth, BAR_HEIGHT, BAR_RADIUS, BAR_RADIUS);
            g2.setColor(TimelineTheme.gridLine());
            for (int grid = 1; grid < 4; grid++) {
                int gridX = barStartX + trackWidth * grid / 4;
                g2.drawLine(gridX, barY + 2, gridX, barY + BAR_HEIGHT - 2);
            }

            // bar 区域：保持扁平语义色，避免把耗时阶段画成过重的状态卡片。
            long duration = durationMillis(s);
            String ms = formatStageDuration(s);
            int strW = g2.getFontMetrics(FontsUtil.getDefaultFont(Font.PLAIN)).stringWidth(ms);
            if (duration > 0) {
                g2.setColor(color);
                g2.fillRoundRect(currentX, barY, barW, BAR_HEIGHT, BAR_RADIUS, BAR_RADIUS);

                g2.setColor(TimelineTheme.barHighlight(isHovered));
                g2.fillRoundRect(currentX, barY, barW, 1, BAR_RADIUS, BAR_RADIUS);

                // 悬停时添加外边框高亮
                if (isHovered) {
                    g2.setColor(TimelineTheme.hoveredBarOutline());
                    g2.setStroke(new BasicStroke(1.5f));
                    g2.drawRoundRect(currentX, barY, barW, BAR_HEIGHT, BAR_RADIUS, BAR_RADIUS);
                }
            } else if (isSubMillisecondStage(s)) {
                int markerX = Math.max(barStartX, resolveSubMillisecondMarkerX(
                        currentX + Math.max(0, (barW - SUB_MILLISECOND_MARKER_WIDTH) / 2),
                        SUB_MILLISECOND_MARKER_WIDTH,
                        trackRightX
                ));
                g2.setColor(color);
                g2.fillRoundRect(markerX, barY, SUB_MILLISECOND_MARKER_WIDTH, BAR_HEIGHT, BAR_RADIUS, BAR_RADIUS);
            }

            // 耗时文字只放在柱子内部；窄柱和 <1ms> 标记通过 tooltip 展示完整耗时。
            g2.setFont(FontsUtil.getDefaultFont(Font.PLAIN));
            if (duration > 0 && shouldDrawDurationTextInsideBar(barW, strW)) {
                g2.setColor(TimelineTheme.barText());
                g2.drawString(ms, currentX + barW - strW - 6, barY + textYOffset);
            }
            // 已观测到的阶段都会占用视觉位置；未记录阶段只保留空轨道。
            if (hasObservedTiming(s)) {
                currentX += barW;
            }
        }
        g2.dispose();
    }

    private int getBarStartX(int sectionX) {
        FontMetrics metrics = getFontMetrics(FontsUtil.getDefaultFont(Font.BOLD));
        int labelMaxWidth = getStageLabelMaxWidth(metrics, 0);
        return sectionX + LABEL_LEFT_PAD + labelMaxWidth + LABEL_RIGHT_PAD;
    }

    private int getStageLabelMaxWidth(FontMetrics metrics, int minimum) {
        int labelMaxWidth = minimum;
        if (metrics != null) {
            for (Stage s : stages) {
                int w = metrics.stringWidth(s.label);
                if (w > labelMaxWidth) {
                    labelMaxWidth = w;
                }
            }
        }
        return labelMaxWidth;
    }

    private boolean isPointInInfoArea(Point point) {
        if (point == null) {
            return false;
        }
        int sectionX = INFO_BLOCK_H_GAP;
        int sectionWidth = Math.max(0, getWidth() - 2 * INFO_BLOCK_H_GAP);
        int infoTextBlockHeight = getInfoBlockHeight();
        return point.x >= sectionX
                && point.x <= sectionX + sectionWidth
                && point.y >= INFO_BLOCK_V_GAP
                && point.y <= INFO_BLOCK_V_GAP + infoTextBlockHeight;
    }

    private void drawConnectionInfo(Graphics2D g2, int sectionX, int sectionY, int sectionWidth) {
        List<InfoItem> items = buildConnectionInfoItems();
        String warning = httpEventInfo == null ? null : httpEventInfo.getSslCertWarning();
        boolean hasWarning = warning != null && !warning.isBlank();
        List<String> infoLabels = new ArrayList<>();
        for (InfoItem item : items) {
            infoLabels.add(item.label);
        }
        if (hasWarning) {
            infoLabels.add(I18nUtil.getMessage(MessageKeys.WATERFALL_CERT_WARNING));
        }
        int contentX = sectionX + INFO_TEXT_LEFT_PAD;
        int contentY = sectionY + INFO_TEXT_TOP_PAD;
        int usableWidth = Math.max(0, sectionWidth - 2 * INFO_TEXT_LEFT_PAD);
        int columnWidth = Math.max(120, (usableWidth - INFO_COLUMN_GAP) / 2);
        int labelWidth = resolveInfoLabelWidth(
                g2.getFontMetrics(FontsUtil.getDefaultFont(Font.BOLD)),
                infoLabels,
                columnWidth
        );
        int rowHeight = INFO_TEXT_LINE_HEIGHT + INFO_TEXT_EXTRA_GAP;
        int baselineOffset = g2.getFontMetrics(FontsUtil.getDefaultFont(Font.PLAIN)).getAscent();

        g2.setColor(TimelineTheme.separator());
        int dividerX = contentX + columnWidth + INFO_COLUMN_GAP / 2;
        g2.drawLine(dividerX, contentY + 2, dividerX,
                sectionY + getInfoBlockHeight() - INFO_TEXT_BOTTOM_PAD - 2);

        for (int i = 0; i < items.size(); i++) {
            InfoItem item = items.get(i);
            int column = i % 2;
            int row = i / 2;
            int x = contentX + column * (columnWidth + INFO_COLUMN_GAP);
            int baseline = contentY + row * rowHeight + baselineOffset;
            drawInfoItem(g2, item, x, baseline, columnWidth, labelWidth);
        }

        if (hasWarning) {
            int row = (items.size() + 1) / 2;
            int baseline = contentY + row * rowHeight + baselineOffset;
            InfoItem warningItem = new InfoItem(I18nUtil.getMessage(MessageKeys.WATERFALL_CERT_WARNING), warning, true);
            drawInfoItem(g2, warningItem, contentX, baseline, usableWidth, labelWidth);
        }
    }

    private void drawScaleHeader(Graphics2D g2,
                                 int labelX,
                                 int trackX,
                                 int trackWidth,
                                 int barAreaTop,
                                 long totalDuration,
                                 boolean hasSubMillisecondStage) {
        g2.setFont(FontsUtil.getDefaultFontWithOffset(Font.PLAIN, -2));
        FontMetrics fm = g2.getFontMetrics();
        int baseline = barAreaTop + 20;
        g2.setColor(TimelineTheme.descriptionText());
        g2.drawString(I18nUtil.getMessage(MessageKeys.WATERFALL_STAGE_TOTAL_DURATION)
                + ": " + formatTotalDuration(totalDuration, hasSubMillisecondStage), labelX, baseline);

        for (int i = 0; i <= 4; i++) {
            long value = Math.round(totalDuration * (i / 4.0d));
            String text = formatDuration(value);
            int x = trackX + trackWidth * i / 4;
            int textX;
            if (i == 0) {
                textX = x;
            } else if (i == 4) {
                textX = x - fm.stringWidth(text);
            } else {
                textX = x - fm.stringWidth(text) / 2;
            }
            g2.drawString(text, textX, baseline);
        }
    }

    private String formatDuration(long millis) {
        return Math.max(0, millis) + "ms";
    }

    private String formatTotalDuration(long millis, boolean hasSubMillisecondStage) {
        if (millis == 0 && hasSubMillisecondStage) {
            return I18nUtil.getMessage(MessageKeys.WATERFALL_STAGE_DURATION_SUB_MILLISECOND);
        }
        return formatDuration(millis);
    }

    private String formatStageDuration(Stage stage) {
        long duration = durationMillis(stage);
        if (duration > 0) {
            return formatDuration(duration);
        }
        if (isSubMillisecondStage(stage)) {
            return I18nUtil.getMessage(MessageKeys.WATERFALL_STAGE_DURATION_SUB_MILLISECOND);
        }
        return I18nUtil.getMessage(MessageKeys.WATERFALL_STAGE_DURATION_NOT_RECORDED);
    }

    private String getStageTimingNote(Stage stage) {
        if (isSubMillisecondStage(stage)) {
            return I18nUtil.getMessage(MessageKeys.WATERFALL_STAGE_DURATION_SUB_MILLISECOND_NOTE);
        }
        if (!hasObservedTiming(stage)) {
            return I18nUtil.getMessage(MessageKeys.WATERFALL_STAGE_DURATION_NOT_RECORDED_NOTE);
        }
        return null;
    }

    private void drawInfoItem(Graphics2D g2, InfoItem item, int x, int baseline, int width, int labelWidth) {
        int resolvedLabelWidth = Math.min(labelWidth, Math.max(INFO_LABEL_MIN_WIDTH, width - 12));
        int valueX = x + resolvedLabelWidth;
        int valueWidth = Math.max(12, width - resolvedLabelWidth);
        Color labelColor = item.warning ? TimelineTheme.certificateWarning() : TimelineTheme.labelText();
        Color valueColor = item.warning ? TimelineTheme.certificateWarning() : TimelineTheme.infoText();

        g2.setFont(FontsUtil.getDefaultFont(Font.BOLD));
        g2.setColor(labelColor);
        g2.drawString(ellipsis(g2, item.label, resolvedLabelWidth - INFO_LABEL_VALUE_GAP), x, baseline);

        g2.setFont(FontsUtil.getDefaultFont(Font.PLAIN));
        g2.setColor(valueColor);
        g2.drawString(ellipsis(g2, item.value, valueWidth), valueX, baseline);
    }

    // 根据当前语言的 key 宽度动态分配 label 区，避免英文信息项被固定宽度截断。
    static int resolveInfoLabelWidth(FontMetrics metrics, List<String> labels, int itemWidth) {
        int preferredWidth = INFO_LABEL_MIN_WIDTH;
        if (metrics != null && labels != null) {
            for (String label : labels) {
                String text = label == null || label.isBlank() ? "-" : label;
                preferredWidth = Math.max(preferredWidth, metrics.stringWidth(text) + INFO_LABEL_VALUE_GAP);
            }
        }
        int maxWidthWithValueSpace = Math.max(INFO_LABEL_MIN_WIDTH, itemWidth - INFO_VALUE_MIN_WIDTH);
        return Math.min(preferredWidth, maxWidthWithValueSpace);
    }

    private List<InfoItem> buildConnectionInfoItems() {
        String protocol = null;
        String localAddr = null;
        String remoteAddr = null;
        String tls = null;
        String cipher = null;
        String certCN = null;
        String issuerCN = null;
        String validUntil = null;
        if (httpEventInfo != null) {
            protocol = httpEventInfo.getProtocol();
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
        List<InfoItem> items = new ArrayList<>();
        items.add(new InfoItem(I18nUtil.getMessage(MessageKeys.WATERFALL_HTTP_VERSION), valueOrDash(protocol), false));
        items.add(new InfoItem(I18nUtil.getMessage(MessageKeys.WATERFALL_TLS_PROTOCOL), valueOrDash(tls), false));
        items.add(new InfoItem(I18nUtil.getMessage(MessageKeys.WATERFALL_LOCAL_ADDRESS), valueOrDash(localAddr), false));
        items.add(new InfoItem(I18nUtil.getMessage(MessageKeys.WATERFALL_REMOTE_ADDRESS), valueOrDash(remoteAddr), false));
        items.add(new InfoItem(I18nUtil.getMessage(MessageKeys.WATERFALL_CIPHER_NAME), valueOrDash(cipher), false));
        items.add(new InfoItem(I18nUtil.getMessage(MessageKeys.WATERFALL_CERTIFICATE_CN), valueOrDash(certCN), false));
        items.add(new InfoItem(I18nUtil.getMessage(MessageKeys.WATERFALL_ISSUER_CN), valueOrDash(issuerCN), false));
        items.add(new InfoItem(I18nUtil.getMessage(MessageKeys.WATERFALL_VALID_UNTIL), valueOrDash(validUntil), false));
        return items;
    }

    private String buildConnectionInfoTooltip() {
        StringBuilder tooltip = new StringBuilder("<html><table cellspacing='0' cellpadding='2'>");
        for (InfoItem item : buildConnectionInfoItems()) {
            tooltip.append("<tr><td><b>")
                    .append(escapeHtml(item.label))
                    .append("</b></td><td>")
                    .append(escapeHtml(item.value))
                    .append("</td></tr>");
        }
        String warning = httpEventInfo == null ? null : httpEventInfo.getSslCertWarning();
        if (warning != null && !warning.isBlank()) {
            tooltip.append("<tr><td><b>")
                    .append(escapeHtml(I18nUtil.getMessage(MessageKeys.WATERFALL_CERT_WARNING)))
                    .append("</b></td><td>")
                    .append(escapeHtml(warning))
                    .append("</td></tr>");
        }
        tooltip.append("</table></html>");
        return tooltip.toString();
    }

    private String ellipsis(Graphics2D g2, String text, int maxWidth) {
        String value = valueOrDash(text);
        if (maxWidth <= 0) {
            return "";
        }
        FontMetrics fm = g2.getFontMetrics();
        if (fm.stringWidth(value) <= maxWidth) {
            return value;
        }
        String suffix = "...";
        int suffixWidth = fm.stringWidth(suffix);
        if (suffixWidth >= maxWidth) {
            return suffix;
        }
        String truncated = value;
        while (!truncated.isEmpty() && fm.stringWidth(truncated) + suffixWidth > maxWidth) {
            truncated = truncated.substring(0, truncated.length() - 1);
        }
        return truncated + suffix;
    }

    private String valueOrDash(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    private String escapeHtml(String value) {
        String text = valueOrDash(value);
        return text
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    // 计算两列摘要区实际行数（用于动态布局）
    private int getInfoRowsCount() {
        int rows = (buildConnectionInfoItems().size() + 1) / 2;
        if (httpEventInfo != null && httpEventInfo.getSslCertWarning() != null && !httpEventInfo.getSslCertWarning().isEmpty()) {
            rows += 1;
        }
        return rows;
    }

    private record InfoItem(String label, String value, boolean warning) {
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

    private static long durationMillis(Stage stage) {
        return Math.max(0, stage.end - stage.start);
    }

    private static boolean hasObservedTiming(Stage stage) {
        return durationMillis(stage) > 0 || isSubMillisecondStage(stage);
    }

    private static boolean isSubMillisecondStage(Stage stage) {
        return stage.start > 0 && stage.end == stage.start;
    }

    private static boolean hasObservedRange(long start, long end) {
        return start > 0 && end >= start;
    }

    static boolean shouldDrawDurationTextInsideBar(int barWidth, int textWidth) {
        return barWidth > Math.max(0, textWidth) + 12;
    }

    static int resolveSubMillisecondMarkerX(int desiredMarkerX, int markerWidth, int trackRightX) {
        return Math.min(desiredMarkerX, trackRightX - Math.max(0, markerWidth));
    }

    static int[] resolveStageVisualWidths(List<Stage> stages, long totalDuration, int availableWidth) {
        if (stages == null || stages.isEmpty()) {
            return new int[0];
        }
        int[] widths = new int[stages.size()];
        if (availableWidth <= 0) {
            return widths;
        }

        int durationStageCount = 0;
        int subMillisecondStageCount = 0;
        long durationSum = 0;
        for (Stage stage : stages) {
            long duration = durationMillis(stage);
            if (duration > 0) {
                durationStageCount++;
                durationSum += duration;
            } else if (isSubMillisecondStage(stage)) {
                subMillisecondStageCount++;
            }
        }
        if (durationStageCount == 0 && subMillisecondStageCount == 0) {
            return widths;
        }

        int subMillisecondSlotWidth = 0;
        if (subMillisecondStageCount > 0) {
            int stageCount = durationStageCount + subMillisecondStageCount;
            int maxSlotWidth = Math.max(1, availableWidth / Math.max(1, stageCount));
            subMillisecondSlotWidth = Math.min(SUB_MILLISECOND_VISUAL_SLOT_WIDTH, maxSlotWidth);
            if ((long) subMillisecondSlotWidth * subMillisecondStageCount > availableWidth) {
                subMillisecondSlotWidth = Math.max(1, availableWidth / subMillisecondStageCount);
            }
        }

        int subMillisecondWidthSum = subMillisecondSlotWidth * subMillisecondStageCount;
        int durationAvailableWidth = Math.max(0, availableWidth - subMillisecondWidthSum);
        int minDurationWidth = durationStageCount == 0 ? 0 : Math.min(MIN_BAR_WIDTH, durationAvailableWidth / durationStageCount);
        int minDurationWidthSum = minDurationWidth * durationStageCount;
        int remainingDurationWidth = Math.max(0, durationAvailableWidth - minDurationWidthSum);
        int remainingExtraWidth = remainingDurationWidth;
        int remainingDurationStages = durationStageCount;
        long ratioTotalDuration = durationSum > 0 ? durationSum : Math.max(1, totalDuration);

        for (int i = 0; i < stages.size(); i++) {
            Stage stage = stages.get(i);
            long duration = durationMillis(stage);
            if (duration > 0) {
                remainingDurationStages--;
                int extraWidth;
                if (remainingDurationStages == 0) {
                    extraWidth = remainingExtraWidth;
                } else {
                    extraWidth = (int) Math.round((double) remainingDurationWidth * duration / ratioTotalDuration);
                    extraWidth = Math.min(extraWidth, remainingExtraWidth);
                }
                widths[i] = minDurationWidth + extraWidth;
                remainingExtraWidth -= extraWidth;
            } else if (isSubMillisecondStage(stage)) {
                widths[i] = subMillisecondSlotWidth;
            }
        }

        return widths;
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
            if (hasObservedRange(dnsS, dnsE)) {
                starts[0] = dnsS;
                ends[0] = dnsE;
            }
            // Socket
            if (hasObservedRange(connS, connE)) {
                long socketEnd = (sslS > connS && sslE > sslS && sslE <= connE) ? sslS : connE;
                starts[1] = connS;
                ends[1] = socketEnd;
            }
            // SSL
            if (hasObservedRange(sslS, sslE)) {
                starts[2] = sslS;
                ends[2] = sslE;
            }
            // Request Send
            long reqStart = Math.min(reqHS > 0 ? reqHS : Long.MAX_VALUE, reqBS > 0 ? reqBS : Long.MAX_VALUE);
            long reqEnd = Math.max(reqHE, reqBE);
            if (reqStart != Long.MAX_VALUE && reqEnd > 0 && reqEnd >= reqStart) {
                starts[3] = reqStart;
                ends[3] = reqEnd;
            }
            // Waiting (TTFB)
            if (reqEnd > 0 && respHS >= reqEnd) {
                starts[4] = reqEnd;
                ends[4] = respHS;
            }
            // Content Download
            if (hasObservedRange(respBS, respBE)) {
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
