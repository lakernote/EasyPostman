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
import java.util.LinkedList;
import java.util.Queue;
import java.util.function.Consumer;

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
    private final Queue<Double> recentSpeedQueue = new LinkedList<>();
    private final int MAX_SPEED_SAMPLES = 5; // 用于计算平均速度的样本数
    private final Timer updateTimer;
    private final long startTime;
    private long lastUpdateTime;

    @Getter
    private boolean cancelled = false;

    // 是否自动关闭对话框
    private boolean autoClose = false;

    // 下载是否已完成
    private boolean downloadCompleted = false;

    // 下载完成或取消时调用的回调函数
    private Consumer<Boolean> onFinishCallback;

    // 当前下载信息
    private int currentContentLength;
    private int currentTotalBytes;

    public DownloadProgressDialog(String title) {
        super((JFrame) null, title, false);
        setModal(false);
        setSize(450, 350); // 加大窗口尺寸以容纳图表
        setLocationRelativeTo(null);
        setResizable(true);
        startTime = System.currentTimeMillis();
        lastUpdateTime = startTime;

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
        dateAxis.setDateFormatOverride(new SimpleDateFormat("mm:ss"));
        dateAxis.setAutoRange(true);
        dateAxis.setFixedAutoRange(30000); // 显示30秒的数据

        NumberAxis valueAxis = (NumberAxis) plot.getRangeAxis();
        valueAxis.setAutoRangeIncludesZero(true);

        // 设置图表背景透明
        chart.setBackgroundPaint(null);
        plot.setBackgroundPaint(null);

        chartPanel = new ChartPanel(chart);
        chartPanel.setPreferredSize(new Dimension(400, 150));
        chartPanel.setMouseWheelEnabled(true);

        // 取消按钮
        cancelButton = new JButton("Cancel", new FlatSVGIcon("icons/cancel.svg", 16, 16));
        cancelButton.addActionListener(e -> {
            cancelled = true;
            if (onFinishCallback != null) {
                onFinishCallback.accept(true); // 传递取消状态
            }
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
     * @param contentLength    内容总长度
     * @param onFinishCallback 完成回调
     * @return 是否显示了对话框
     */
    public boolean startDownload(int contentLength, Consumer<Boolean> onFinishCallback) {
        this.currentContentLength = contentLength;
        this.currentTotalBytes = 0;
        this.onFinishCallback = onFinishCallback;
        this.downloadCompleted = false;

        // 重置UI状态
        closeButton.setVisible(false);
        cancelButton.setVisible(true);

        // 根据设置决定是否显示
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
        long now = System.currentTimeMillis();

        // 更新实时速度（只在定时器中展示，这里只收集数据）
        if (now > lastUpdateTime) {
            double instantSpeed = (bytesRead * 1000.0) / (now - lastUpdateTime);
            recentSpeedQueue.add(instantSpeed);
            while (recentSpeedQueue.size() > MAX_SPEED_SAMPLES) {
                recentSpeedQueue.poll();
            }
            lastUpdateTime = now;
        }
    }

    /**
     * 完成下载
     *
     * @param success 是否成功完成
     */
    public void finishDownload(boolean success) {
        if (isVisible()) {
            updateTimer.stop();
            downloadCompleted = true;

            // 更新界面状态
            cancelButton.setVisible(false);
            closeButton.setVisible(true);

            // 添加完成标记到详情信息
            detailsLabel.setText(detailsLabel.getText() + " (完成)");

            // 调用回调
            if (onFinishCallback != null) {
                onFinishCallback.accept(cancelled || !success);
            }

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

        long now = System.currentTimeMillis();
        long elapsed = now - startTime;

        // 计算平均下载速度（使用最近几个样本的平均值）
        double avgSpeed = calculateAverageSpeed();

        // 更新图表
        speedSeries.addOrUpdate(new Millisecond(), avgSpeed / 1024.0);

        // 更新文本信息
        String sizeStr = FileSizeDisplayUtil.formatDownloadSize(currentTotalBytes, currentContentLength);
        String speedStr;
        if (avgSpeed > 1024 * 1024) {
            speedStr = String.format("Speed: %.2f MB/s", avgSpeed / (1024 * 1024));
        } else {
            speedStr = String.format("Speed: %.2f KB/s", avgSpeed / 1024);
        }

        String remainStr;
        if (currentContentLength > 0 && avgSpeed > 0) {
            long remainSeconds = (long) ((currentContentLength - currentTotalBytes) / avgSpeed);
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
     * 计算平均下载速度
     *
     * @return 字节/秒
     */
    private double calculateAverageSpeed() {
        if (recentSpeedQueue.isEmpty()) {
            return 0;
        }

        double total = 0;
        for (Double speed : recentSpeedQueue) {
            total += speed;
        }
        return total / recentSpeedQueue.size();
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