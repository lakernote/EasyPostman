package com.laker.postman.panel.collections.editor.request.sub;

import com.laker.postman.common.constants.ModernColors;
import com.laker.postman.http.runtime.model.HttpEventInfo;
import com.laker.postman.http.runtime.model.HttpResponse;
import com.laker.postman.util.FontsUtil;
import com.laker.postman.util.HttpHeaderConstants;
import com.laker.postman.util.TimeDisplayUtil;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.List;

/**
 * 响应顶部状态栏。
 * <p>
 * 负责状态码、耗时、响应大小和大小明细提示；ResponsePanel 只负责把响应数据交给它。
 */
final class ResponseStatusBar extends JPanel {
    private final JLabel statusCodeLabel;
    private final JLabel responseTimeLabel;
    private final JLabel responseSizeLabel;
    private final JLabel separatorAfterStatus;
    private final JLabel separatorAfterTime;

    ResponseStatusBar() {
        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(0, 6, 0, 10));

        statusCodeLabel = createStatusLabel();
        responseTimeLabel = createTimeLabel();
        responseSizeLabel = createSizeLabel();
        separatorAfterStatus = createSeparator();
        separatorAfterTime = createSeparator();
        separatorAfterStatus.setVisible(false);
        separatorAfterTime.setVisible(false);

        add(statusCodeLabel);
        add(Box.createHorizontalStrut(6));
        add(separatorAfterStatus);
        add(Box.createHorizontalStrut(6));
        add(responseTimeLabel);
        add(Box.createHorizontalStrut(6));
        add(separatorAfterTime);
        add(Box.createHorizontalStrut(6));
        add(responseSizeLabel);
    }

    void setStatus(int code) {
        if (code > 0) {
            statusCodeLabel.setText(String.valueOf(code));
            statusCodeLabel.setForeground(ResponseStatusUiMetadata.statusColor(code));
        } else {
            statusCodeLabel.setText("");
            statusCodeLabel.setForeground(ModernColors.getTextPrimary());
        }
        separatorAfterStatus.setVisible(code > 0);
    }

    void setResponseTime(long ms) {
        responseTimeLabel.setText(TimeDisplayUtil.formatElapsedTime(ms));
        responseTimeLabel.setForeground(ModernColors.getTextSecondary());
        separatorAfterTime.setVisible(ms >= 0);
    }

    void setResponseSize(long bytes, HttpResponse httpResponse) {
        String encoding = responseEncoding(httpResponse);
        HttpEventInfo httpEventInfo = httpResponse != null ? httpResponse.httpEventInfo : null;
        ResponseSizeCalculator.SizeInfo sizeInfo = ResponseSizeCalculator.calculate(bytes, httpEventInfo, encoding);
        updateSizeLabel(sizeInfo);
        if (httpEventInfo != null) {
            attachSizeTooltip(bytes, httpEventInfo, sizeInfo);
        }
    }

    void clear() {
        setStatus(0);
        responseTimeLabel.setText("");
        responseSizeLabel.setText("");
        separatorAfterTime.setVisible(false);
        updateSizeLabel(ResponseSizeCalculator.calculate(0, null, null));
        responseSizeLabel.setText("");
    }

    private JLabel createStatusLabel() {
        JLabel label = new JLabel() {
            @Override
            protected void paintComponent(Graphics g) {
                String text = getText();
                if (text != null && !text.isEmpty()) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    int w = getWidth();
                    int h = getHeight();
                    Color c = getForeground();
                    g2.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), 22));
                    g2.fillRoundRect(0, 0, w - 1, h - 1, h, h);
                    g2.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), 80));
                    g2.setStroke(new BasicStroke(1f));
                    g2.drawRoundRect(0, 0, w - 1, h - 1, h, h);
                    g2.dispose();
                }
                super.paintComponent(g);
            }
        };
        label.setFont(FontsUtil.getDefaultFont(Font.BOLD));
        label.setOpaque(false);
        label.setBorder(BorderFactory.createEmptyBorder(1, 8, 1, 8));
        label.setToolTipText("HTTP Status Code");
        return label;
    }

    private JLabel createTimeLabel() {
        JLabel label = new JLabel();
        label.setFont(FontsUtil.getDefaultFontWithOffset(Font.PLAIN, -1));
        label.setForeground(ModernColors.getTextHint());
        label.setToolTipText("Response Time");
        return label;
    }

    private JLabel createSizeLabel() {
        JLabel label = new JLabel();
        label.setFont(FontsUtil.getDefaultFontWithOffset(Font.PLAIN, -1));
        label.setForeground(ModernColors.getTextHint());
        label.setToolTipText("Response Size");
        label.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return label;
    }

    private JLabel createSeparator() {
        JLabel sep = new JLabel("|");
        sep.setFont(FontsUtil.getDefaultFontWithOffset(Font.PLAIN, -2));
        sep.setForeground(ModernColors.getBorderMediumColor());
        sep.setBorder(BorderFactory.createEmptyBorder(0, 2, 0, 2));
        return sep;
    }

    private String responseEncoding(HttpResponse httpResponse) {
        if (httpResponse == null || httpResponse.headers == null) {
            return null;
        }
        List<String> encodings = httpResponse.headers.get(HttpHeaderConstants.CONTENT_ENCODING);
        return encodings != null && !encodings.isEmpty() ? encodings.get(0) : null;
    }

    private void updateSizeLabel(ResponseSizeCalculator.SizeInfo sizeInfo) {
        responseSizeLabel.setText(sizeInfo.getDisplayText());
        responseSizeLabel.setForeground(sizeInfo.getNormalColor());
        responseSizeLabel.setCursor(new Cursor(Cursor.HAND_CURSOR));
        responseSizeLabel.setToolTipText(null);

        for (MouseListener listener : responseSizeLabel.getMouseListeners()) {
            responseSizeLabel.removeMouseListener(listener);
        }
    }

    private void attachSizeTooltip(long bytes, HttpEventInfo httpEventInfo, ResponseSizeCalculator.SizeInfo sizeInfo) {
        responseSizeLabel.addMouseListener(new MouseAdapter() {
            private Timer showTimer;
            private Timer hideTimer;

            @Override
            public void mouseEntered(MouseEvent e) {
                responseSizeLabel.setForeground(sizeInfo.getHoverColor());
                if (hideTimer != null) {
                    hideTimer.stop();
                }
                showTimer = new Timer(350, evt -> {
                    JPanel panel = ResponseTooltipBuilder.buildSizeTooltipPanel(bytes, httpEventInfo, sizeInfo);
                    ResponseSizeTooltipWindow.showTooltip(responseSizeLabel, panel);
                });
                showTimer.setRepeats(false);
                showTimer.start();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                responseSizeLabel.setForeground(sizeInfo.getNormalColor());
                if (showTimer != null) {
                    showTimer.stop();
                }
                hideTimer = new Timer(200, evt -> ResponseSizeTooltipWindow.hideTooltip());
                hideTimer.setRepeats(false);
                hideTimer.start();
            }
        });
    }
}
