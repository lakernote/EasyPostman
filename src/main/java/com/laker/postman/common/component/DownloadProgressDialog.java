package com.laker.postman.common.component;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.laker.postman.common.setting.SettingManager;
import com.laker.postman.util.FileSizeDisplayUtil;
import lombok.Getter;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.time.Millisecond;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;

import javax.swing.*;
import java.awt.*;
import java.text.SimpleDateFormat;

/**
 * 通用下载进度对话框组件
 */
public class DownloadProgressDialog extends JDialog {
    private final JLabel detailsLabel;
    private final JLabel speedLabel;
    private final JLabel timeLabel;
    private final JButton cancelButton;
    private final JButton closeButton;
    private final TimeSeries speedSeries;
    private final ChartPanel chartPanel;
    private final Timer updateTimer;

    @Getter
    private boolean cancelled = false;

    // 是否自动关闭对话框
    private boolean autoClose = false;
    // 当前下载信息
    private int currentContentLength;
    private int currentTotalBytes;

    // 记录用于速度计算的最后字节数和时间
    private long lastBytesForSpeed = 0;
    private long lastTimeForSpeed = 0;

    public DownloadProgressDialog(String title) {
        super((JFrame) null, title, false);
        setModal(false);
        setSize(450, 350); // 加大窗口尺寸以容纳图表
        setLocationRelativeTo(null);
        setResizable(true);

        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        JLabel iconLabel = new JLabel(new FlatSVGIcon("icons/download.svg", 24, 24));
        JPanel titlePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        titlePanel.add(iconLabel);
        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font(titleLabel.getFont().getName(), Font.BOLD, 13));
        titlePanel.add(titleLabel);

        detailsLabel = new JLabel("Downloaded: 0 KB");
        speedLabel = new JLabel("Speed: 0 KB/s");
        timeLabel = new JLabel("Time left: Calculating...");
        JPanel infoPanel = new JPanel(new GridLayout(3, 1, 5, 0));
        infoPanel.add(detailsLabel);
        infoPanel.add(speedLabel);
        infoPanel.add(timeLabel);

        // 创建速度图表
        speedSeries = new TimeSeries("Speed (KB/s)");
        TimeSeriesCollection dataset = new TimeSeriesCollection(speedSeries);
        JFreeChart chart = ChartFactory.createTimeSeriesChart(
                null,  // 图表标题
                "Time", // X轴标题
                "Speed (KB/s)", // Y轴标题
                dataset, // 数据集
                false, // 是否显示图例
                true, // 是否生成工具提示
                false // 是否生成URL链接
        );

        // 设置图表样式
        XYPlot plot = (XYPlot) chart.getPlot();
        DateAxis dateAxis = (DateAxis) plot.getDomainAxis();
        dateAxis.setDateFormatOverride(new SimpleDateFormat("HH:mm:ss"));
        dateAxis.setAutoRange(true);
//        dateAxis.setFixedAutoRange(20000); // 显示20秒的数据，减少初始空白区域

        NumberAxis valueAxis = (NumberAxis) plot.getRangeAxis();
        valueAxis.setAutoRangeIncludesZero(true);

        // 设置图表背景透明，线条颜色更明显
        chart.setBackgroundPaint(null);
        plot.setBackgroundPaint(null);
        plot.setOutlinePaint(Color.DARK_GRAY);

        // 设置线条颜色和粗细
        plot.getRenderer().setSeriesPaint(0, new Color(0, 120, 220)); // 蓝色线条
        plot.getRenderer().setSeriesStroke(0, new BasicStroke(2.0f)); // 加粗线条

        chartPanel = new ChartPanel(chart);
        chartPanel.setPreferredSize(new Dimension(400, 150));
        chartPanel.setMouseWheelEnabled(true);

        // 取消按钮
        cancelButton = new JButton("Cancel", new FlatSVGIcon("icons/cancel.svg", 16, 16));
        cancelButton.addActionListener(e -> {
            cancelled = true;
            dispose();
        });

        // 关闭按钮（下载完成后可见）
        closeButton = new JButton("Close", new FlatSVGIcon("icons/check.svg", 16, 16));
        closeButton.setVisible(false); // 初始不可见
        closeButton.addActionListener(e -> dispose());

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        buttonPanel.add(closeButton);
        buttonPanel.add(cancelButton);

        JPanel southPanel = new JPanel();
        southPanel.setLayout(new BoxLayout(southPanel, BoxLayout.Y_AXIS));
        southPanel.add(infoPanel);
        southPanel.add(Box.createVerticalStrut(10));
        southPanel.add(buttonPanel);

        mainPanel.add(titlePanel, BorderLayout.NORTH);
        mainPanel.add(chartPanel, BorderLayout.CENTER); // 添加图表到中央区域
        mainPanel.add(southPanel, BorderLayout.SOUTH);
        setContentPane(mainPanel);
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

        // 创建定时器，定期更新UI（无论是否有新数据）
        updateTimer = new Timer(200, e -> updateUIWithLatestData());
    }

    public DownloadProgressDialog() {
        this("Download Progress");
    }

    /**
     * 设置是否自动关闭对话框
     *
     * @param autoClose 是否自动关闭
     */
    public void setAutoClose(boolean autoClose) {
        this.autoClose = autoClose;
    }

    /**
     * 开始下载并显示进度
     *
     * @param contentLength 内容总长度
     * @return 是否显示了对话框
     */
    public boolean startDownload(int contentLength) {
        this.currentContentLength = contentLength;
        this.currentTotalBytes = 0;
        this.lastBytesForSpeed = 0;
        this.lastTimeForSpeed = System.nanoTime();
        closeButton.setVisible(false);
        cancelButton.setVisible(true);
        boolean shouldDisplay = shouldShow(contentLength);
        if (shouldDisplay) {
            setVisible(true);
            updateTimer.start();
        }
        return shouldDisplay;
    }

    /**
     * 更新下载进度
     *
     * @param bytesRead 新读取的字节数
     */
    public void updateProgress(int bytesRead) {
        if (!isVisible()) return;
        currentTotalBytes += bytesRead;
    }

    /**
     * 完成下载
     */
    public void finishDownload() {
        if (isVisible()) {
            // 更新界面状态
            cancelButton.setVisible(false);
            closeButton.setVisible(true);
            updateTimer.stop();
            updateUIWithLatestData();
            // 如果设置为自动关闭，则关闭对话框
            if (autoClose) {
                dispose();
            }
        }
    }

    /**
     * 定时更新UI的方法
     */
    private void updateUIWithLatestData() {
        if (!isVisible()) return;
        long now = System.nanoTime();
        long bytesDelta = currentTotalBytes - lastBytesForSpeed;
        long timeDelta = now - lastTimeForSpeed;
        double speed = 0;
        if (timeDelta > 0) {
            speed = (bytesDelta * 1_000_000_000.0) / timeDelta;
        }
        lastBytesForSpeed = currentTotalBytes;
        lastTimeForSpeed = now;
        speedSeries.addOrUpdate(new Millisecond(), speed / 1024.0);
        String sizeStr = FileSizeDisplayUtil.formatDownloadSize(currentTotalBytes, currentContentLength);
        String speedStr;
        if (speed > 1024 * 1024) {
            speedStr = String.format("Speed: %.2f MB/s", speed / (1024 * 1024));
        } else {
            speedStr = String.format("Speed: %.2f KB/s", speed / 1024);
        }
        String remainStr;
        if (currentContentLength > 0 && speed > 0) {
            long remainSeconds = (long) ((currentContentLength - currentTotalBytes) / speed);
            if (remainSeconds > 60) {
                remainStr = String.format("Time left: %d min %d sec", remainSeconds / 60, remainSeconds % 60);
            } else {
                remainStr = String.format("Time left: %d sec", remainSeconds);
            }
        } else {
            remainStr = "Time left: Calculating...";
        }
        updateProgress(sizeStr, speedStr, remainStr);
    }

    /**
     * 统一更新进度信息
     *
     * @param details 进度详情（如已下载大小）
     * @param speed   速度（如 KB/s）
     * @param time    剩余时间
     */
    private void updateProgress(String details, String speed, String time) {
        detailsLabel.setText(details);
        speedLabel.setText(speed);
        timeLabel.setText(time);
    }

    /**
     * 判断是否需要显示弹窗
     */
    private boolean shouldShow(int contentLength) {
        if (!SettingManager.isShowDownloadProgressDialog()) {
            return false;
        }
        int threshold = SettingManager.getDownloadProgressDialogThreshold();
        return contentLength > threshold || contentLength <= 0;
    }
}