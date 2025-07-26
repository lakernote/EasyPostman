package com.laker.postman.common.component;

import com.formdev.flatlaf.extras.FlatSVGIcon;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Timer;
import java.util.TimerTask;

/**
 * 内存占用显示标签组件，显示当前JVM内存使用情况。
 * 支持自动刷新和双击手动GC功能。
 */
public class MemoryLabel extends JLabel {
    private Timer refreshTimer;
    private final int refreshInterval;
    private boolean autoRefresh = true;

    /**
     * 创建内存标签，默认1秒刷新一次
     */
    public MemoryLabel() {
        this(1000);
    }

    /**
     * 创建内存标签，指定刷新间隔
     *
     * @param refreshIntervalMs 刷新间隔(毫秒)
     */
    public MemoryLabel(int refreshIntervalMs) {
        super();
        this.refreshInterval = refreshIntervalMs;

        // 设置图标和字体
        setIcon(new FlatSVGIcon("icons/computer.svg", 20, 20));
        setFont(getFont().deriveFont(Font.BOLD));
        setToolTipText("当前JVM内存使用情况，双击GC");

        // 初始刷新
        updateMemoryInfo();

        // 添加双击GC功能
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    System.gc();
                    updateMemoryInfo();
                    JOptionPane.showMessageDialog(
                            MemoryLabel.this.getParent(),
                            "GC已触发!",
                            "信息",
                            JOptionPane.INFORMATION_MESSAGE);
                }
            }
        });

        // 启动定时刷新
        startAutoRefresh();
    }

    /**
     * 更新内存信息显示
     */
    public void updateMemoryInfo() {
        Runtime rt = Runtime.getRuntime();
        long used = rt.totalMemory() - rt.freeMemory();
        long max = rt.maxMemory();
        setText(formatSize(used) + " / " + formatSize(max));
    }

    /**
     * 格式化内存大小，自动显示为 MB 或 GB
     */
    private String formatSize(long bytes) {
        double mb = bytes / 1024.0 / 1024;
        if (mb < 1024) {
            if (mb == (long) mb) {
                return String.format("%dMB", (long) mb);
            } else {
                return String.format("%.2fMB", mb).replaceAll("\\.0+$", "").replaceAll("(\\.\\d*[1-9])0+$", "$1");
            }
        } else {
            double gb = mb / 1024.0;
            if (gb == (long) gb) {
                return String.format("%dGB", (long) gb);
            } else {
                return String.format("%.2fGB", gb).replaceAll("\\.0+$", "").replaceAll("(\\.\\d*[1-9])0+$", "$1");
            }
        }
    }

    /**
     * 启动自动刷新
     */
    public void startAutoRefresh() {
        if (refreshTimer != null) {
            refreshTimer.cancel();
        }

        autoRefresh = true;
        refreshTimer = new Timer();
        refreshTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                SwingUtilities.invokeLater(() -> updateMemoryInfo());
            }
        }, 0, refreshInterval);
    }

    /**
     * 停止自动刷新
     */
    public void stopAutoRefresh() {
        autoRefresh = false;
        if (refreshTimer != null) {
            refreshTimer.cancel();
            refreshTimer = null;
        }
    }

    /**
     * 组件被移除时停止定时器
     */
    @Override
    public void removeNotify() {
        stopAutoRefresh();
        super.removeNotify();
    }

    /**
     * 设置是否自动刷新
     *
     * @param autoRefresh 是否自动刷新
     */
    public void setAutoRefresh(boolean autoRefresh) {
        if (this.autoRefresh == autoRefresh) return;

        if (autoRefresh) {
            startAutoRefresh();
        } else {
            stopAutoRefresh();
        }
    }

}