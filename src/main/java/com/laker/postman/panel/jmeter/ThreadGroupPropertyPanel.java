package com.laker.postman.panel.jmeter;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;

public class ThreadGroupPropertyPanel extends JPanel {
    private final JComboBox<ThreadGroupData.ThreadMode> modeComboBox;
    private final CardLayout cardLayout;
    private final JPanel cardPanel;
    private JMeterTreeNode currentNode;

    // 固定模式面板组件
    private final JPanel fixedPanel;
    private final JSpinner fixedNumThreadsSpinner;
    private final JSpinner fixedLoopsSpinner;
    private final JCheckBox useTimeCheckBox;
    private final JSpinner durationSpinner;

    // 递增模式面板组件
    private final JPanel rampUpPanel;
    private final JSpinner rampUpStartThreadsSpinner;
    private final JSpinner rampUpEndThreadsSpinner;
    private final JSpinner rampUpTimeSpinner;

    // 尖刺模式面板组件
    private final JPanel spikePanel;
    private final JSpinner spikeMinThreadsSpinner;
    private final JSpinner spikeMaxThreadsSpinner;
    private final JSpinner spikeRampUpTimeSpinner;
    private final JSpinner spikeHoldTimeSpinner;
    private final JSpinner spikeRampDownTimeSpinner;

    // 峰值模式面板组件
    private final JPanel peakPanel;
    private final JSpinner peakMinThreadsSpinner;
    private final JSpinner peakMaxThreadsSpinner;
    private final JSpinner peakIterationsSpinner;
    private final JSpinner peakHoldTimeSpinner;

    // 阶梯模式面板组件
    private final JPanel stairsPanel;
    private final JSpinner stairsStartThreadsSpinner;
    private final JSpinner stairsEndThreadsSpinner;
    private final JSpinner stairsStepSpinner;
    private final JSpinner stairsHoldTimeSpinner;

    // 负载模式预览相关
    private final ThreadLoadPreviewPanel previewPanel;

    ThreadGroupPropertyPanel() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // 顶部模式选择区域
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topPanel.add(new JLabel("线程模式:"));
        modeComboBox = new JComboBox<>(ThreadGroupData.ThreadMode.values());
        modeComboBox.setPreferredSize(new Dimension(150, 30));
        topPanel.add(modeComboBox);

        // 中间卡片布局，用于切换不同模式的配置面板
        cardLayout = new CardLayout();
        cardPanel = new JPanel(cardLayout);

        // 1. 固定模式面板
        fixedPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx = 0;
        gbc.gridy = 0;
        fixedPanel.add(new JLabel("用户数:"), gbc);

        gbc.gridx = 1;
        fixedNumThreadsSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 1000, 1));
        fixedPanel.add(fixedNumThreadsSpinner, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        fixedPanel.add(new JLabel("执行方式:"), gbc);

        gbc.gridx = 1;
        JPanel executionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        useTimeCheckBox = new JCheckBox("按时间");
        executionPanel.add(useTimeCheckBox);
        fixedPanel.add(executionPanel, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        fixedPanel.add(new JLabel("循环次数:"), gbc);

        gbc.gridx = 1;
        fixedLoopsSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 100000, 1));
        fixedPanel.add(fixedLoopsSpinner, gbc);

        gbc.gridx = 0;
        gbc.gridy = 3;
        fixedPanel.add(new JLabel("持续时间(秒):"), gbc);

        gbc.gridx = 1;
        durationSpinner = new JSpinner(new SpinnerNumberModel(60, 1, 86400, 10));
        fixedPanel.add(durationSpinner, gbc);

        // 按时间执行时禁用循环次数，按循环次数执行时禁用持续时间
        useTimeCheckBox.addActionListener(e -> {
            boolean useTime = useTimeCheckBox.isSelected();
            fixedLoopsSpinner.setEnabled(!useTime);
            durationSpinner.setEnabled(useTime);
        });

        // 2. 递增模式面板
        rampUpPanel = new JPanel(new GridBagLayout());
        gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx = 0;
        gbc.gridy = 0;
        rampUpPanel.add(new JLabel("起始用户数:"), gbc);

        gbc.gridx = 1;
        rampUpStartThreadsSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 1000, 1));
        rampUpPanel.add(rampUpStartThreadsSpinner, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        rampUpPanel.add(new JLabel("最终用户数:"), gbc);

        gbc.gridx = 1;
        rampUpEndThreadsSpinner = new JSpinner(new SpinnerNumberModel(10, 1, 1000, 1));
        rampUpPanel.add(rampUpEndThreadsSpinner, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        rampUpPanel.add(new JLabel("递增时间(秒):"), gbc);

        gbc.gridx = 1;
        rampUpTimeSpinner = new JSpinner(new SpinnerNumberModel(30, 1, 3600, 5));
        rampUpPanel.add(rampUpTimeSpinner, gbc);

        // 3. 尖刺模式面板
        spikePanel = new JPanel(new GridBagLayout());
        gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx = 0;
        gbc.gridy = 0;
        spikePanel.add(new JLabel("最小用户数:"), gbc);

        gbc.gridx = 1;
        spikeMinThreadsSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 1000, 1));
        spikePanel.add(spikeMinThreadsSpinner, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        spikePanel.add(new JLabel("最大用户数:"), gbc);

        gbc.gridx = 1;
        spikeMaxThreadsSpinner = new JSpinner(new SpinnerNumberModel(20, 1, 1000, 1));
        spikePanel.add(spikeMaxThreadsSpinner, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        spikePanel.add(new JLabel("上升时间(秒):"), gbc);

        gbc.gridx = 1;
        spikeRampUpTimeSpinner = new JSpinner(new SpinnerNumberModel(10, 1, 3600, 1));
        spikePanel.add(spikeRampUpTimeSpinner, gbc);

        gbc.gridx = 0;
        gbc.gridy = 3;
        spikePanel.add(new JLabel("保持时间(秒):"), gbc);

        gbc.gridx = 1;
        spikeHoldTimeSpinner = new JSpinner(new SpinnerNumberModel(5, 0, 3600, 1));
        spikePanel.add(spikeHoldTimeSpinner, gbc);

        gbc.gridx = 0;
        gbc.gridy = 4;
        spikePanel.add(new JLabel("下降时间(秒):"), gbc);

        gbc.gridx = 1;
        spikeRampDownTimeSpinner = new JSpinner(new SpinnerNumberModel(10, 1, 3600, 1));
        spikePanel.add(spikeRampDownTimeSpinner, gbc);

        // 4. 峰值模式面板
        peakPanel = new JPanel(new GridBagLayout());
        gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx = 0;
        gbc.gridy = 0;
        peakPanel.add(new JLabel("最小用户数:"), gbc);

        gbc.gridx = 1;
        peakMinThreadsSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 1000, 1));
        peakPanel.add(peakMinThreadsSpinner, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        peakPanel.add(new JLabel("最大用户数:"), gbc);

        gbc.gridx = 1;
        peakMaxThreadsSpinner = new JSpinner(new SpinnerNumberModel(20, 1, 1000, 1));
        peakPanel.add(peakMaxThreadsSpinner, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        peakPanel.add(new JLabel("峰值次数:"), gbc);

        gbc.gridx = 1;
        peakIterationsSpinner = new JSpinner(new SpinnerNumberModel(3, 1, 100, 1));
        peakPanel.add(peakIterationsSpinner, gbc);

        gbc.gridx = 0;
        gbc.gridy = 3;
        peakPanel.add(new JLabel("保持时间(秒):"), gbc);

        gbc.gridx = 1;
        peakHoldTimeSpinner = new JSpinner(new SpinnerNumberModel(10, 1, 3600, 1));
        peakPanel.add(peakHoldTimeSpinner, gbc);

        // 5. 阶梯模式面板
        stairsPanel = new JPanel(new GridBagLayout());
        gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx = 0;
        gbc.gridy = 0;
        stairsPanel.add(new JLabel("起始用户数:"), gbc);

        gbc.gridx = 1;
        stairsStartThreadsSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 1000, 1));
        stairsPanel.add(stairsStartThreadsSpinner, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        stairsPanel.add(new JLabel("最终用户数:"), gbc);

        gbc.gridx = 1;
        stairsEndThreadsSpinner = new JSpinner(new SpinnerNumberModel(20, 1, 1000, 1));
        stairsPanel.add(stairsEndThreadsSpinner, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        stairsPanel.add(new JLabel("阶梯步长:"), gbc);

        gbc.gridx = 1;
        stairsStepSpinner = new JSpinner(new SpinnerNumberModel(5, 1, 100, 1));
        stairsPanel.add(stairsStepSpinner, gbc);

        gbc.gridx = 0;
        gbc.gridy = 3;
        stairsPanel.add(new JLabel("阶梯保持(秒):"), gbc);

        gbc.gridx = 1;
        stairsHoldTimeSpinner = new JSpinner(new SpinnerNumberModel(10, 1, 3600, 1));
        stairsPanel.add(stairsHoldTimeSpinner, gbc);

        // 添加所有面板到卡片布局
        cardPanel.add(fixedPanel, ThreadGroupData.ThreadMode.FIXED.name());
        cardPanel.add(rampUpPanel, ThreadGroupData.ThreadMode.RAMP_UP.name());
        cardPanel.add(spikePanel, ThreadGroupData.ThreadMode.SPIKE.name());
        cardPanel.add(peakPanel, ThreadGroupData.ThreadMode.PEAK.name());
        cardPanel.add(stairsPanel, ThreadGroupData.ThreadMode.STAIRS.name());

        // 默认显示固定模式面板
        cardLayout.show(cardPanel, ThreadGroupData.ThreadMode.FIXED.name());

        // 模式切换监听器
        modeComboBox.addActionListener(e -> {
            ThreadGroupData.ThreadMode selectedMode = (ThreadGroupData.ThreadMode) modeComboBox.getSelectedItem();
            if (selectedMode != null) {
                cardLayout.show(cardPanel, selectedMode.name());
            }
        });

        // 初始设置
        durationSpinner.setEnabled(false); // 默认按循环次数执行

        // 底部添加图表预览区域
        previewPanel = new ThreadLoadPreviewPanel();
        previewPanel.setPreferredSize(new Dimension(380, 120));
        previewPanel.setBorder(BorderFactory.createTitledBorder("负载模式预览"));

        // 布局主面板
        add(topPanel, BorderLayout.NORTH);
        add(cardPanel, BorderLayout.CENTER);
        add(previewPanel, BorderLayout.SOUTH);

        // 为所有输入组件添加变化监听，刷新预览图
        addPreviewUpdateListeners();
    }

    private void addPreviewUpdateListeners() {
        // 模式选择变化监听
        modeComboBox.addActionListener(e -> updatePreview());

        // 固定模式参数变化监听
        fixedNumThreadsSpinner.addChangeListener(e -> updatePreview());
        fixedLoopsSpinner.addChangeListener(e -> updatePreview());
        useTimeCheckBox.addActionListener(e -> updatePreview());
        durationSpinner.addChangeListener(e -> updatePreview());

        // 递增模式参数变化监听
        rampUpStartThreadsSpinner.addChangeListener(e -> updatePreview());
        rampUpEndThreadsSpinner.addChangeListener(e -> updatePreview());
        rampUpTimeSpinner.addChangeListener(e -> updatePreview());

        // 尖刺模式参数变化监听
        spikeMinThreadsSpinner.addChangeListener(e -> updatePreview());
        spikeMaxThreadsSpinner.addChangeListener(e -> updatePreview());
        spikeRampUpTimeSpinner.addChangeListener(e -> updatePreview());
        spikeHoldTimeSpinner.addChangeListener(e -> updatePreview());
        spikeRampDownTimeSpinner.addChangeListener(e -> updatePreview());

        // 峰值模式参数变化监听
        peakMinThreadsSpinner.addChangeListener(e -> updatePreview());
        peakMaxThreadsSpinner.addChangeListener(e -> updatePreview());
        peakIterationsSpinner.addChangeListener(e -> updatePreview());
        peakHoldTimeSpinner.addChangeListener(e -> updatePreview());

        // 阶梯模式参数变化监听
        stairsStartThreadsSpinner.addChangeListener(e -> updatePreview());
        stairsEndThreadsSpinner.addChangeListener(e -> updatePreview());
        stairsStepSpinner.addChangeListener(e -> updatePreview());
        stairsHoldTimeSpinner.addChangeListener(e -> updatePreview());
    }

    private void updatePreview() {
        ThreadGroupData.ThreadMode mode = (ThreadGroupData.ThreadMode) modeComboBox.getSelectedItem();
        if (mode == null) return;

        ThreadLoadPreviewData previewData = new ThreadLoadPreviewData();
        previewData.mode = mode;

        switch (mode) {
            case FIXED:
                previewData.fixedThreads = (Integer) fixedNumThreadsSpinner.getValue();
                previewData.useTime = useTimeCheckBox.isSelected();
                previewData.duration = (Integer) durationSpinner.getValue();
                previewData.loops = (Integer) fixedLoopsSpinner.getValue();
                break;

            case RAMP_UP:
                previewData.rampUpStartThreads = (Integer) rampUpStartThreadsSpinner.getValue();
                previewData.rampUpEndThreads = (Integer) rampUpEndThreadsSpinner.getValue();
                previewData.rampUpTime = (Integer) rampUpTimeSpinner.getValue();
                break;

            case SPIKE:
                previewData.spikeMinThreads = (Integer) spikeMinThreadsSpinner.getValue();
                previewData.spikeMaxThreads = (Integer) spikeMaxThreadsSpinner.getValue();
                previewData.spikeRampUpTime = (Integer) spikeRampUpTimeSpinner.getValue();
                previewData.spikeHoldTime = (Integer) spikeHoldTimeSpinner.getValue();
                previewData.spikeRampDownTime = (Integer) spikeRampDownTimeSpinner.getValue();
                break;

            case PEAK:
                previewData.peakMinThreads = (Integer) peakMinThreadsSpinner.getValue();
                previewData.peakMaxThreads = (Integer) peakMaxThreadsSpinner.getValue();
                previewData.peakIterations = (Integer) peakIterationsSpinner.getValue();
                previewData.peakHoldTime = (Integer) peakHoldTimeSpinner.getValue();
                break;

            case STAIRS:
                previewData.stairsStartThreads = (Integer) stairsStartThreadsSpinner.getValue();
                previewData.stairsEndThreads = (Integer) stairsEndThreadsSpinner.getValue();
                previewData.stairsStep = (Integer) stairsStepSpinner.getValue();
                previewData.stairsHoldTime = (Integer) stairsHoldTimeSpinner.getValue();
                break;
        }

        previewPanel.setPreviewData(previewData);
        previewPanel.repaint();
    }

    // 回填数据
    public void setThreadGroupData(JMeterTreeNode node) {
        this.currentNode = node;
        ThreadGroupData data = node.threadGroupData;
        if (data == null) {
            data = new ThreadGroupData();
            node.threadGroupData = data;
        }

        // 设置模式
        modeComboBox.setSelectedItem(data.threadMode);
        cardLayout.show(cardPanel, data.threadMode.name());

        // 设置固定模式参数
        fixedNumThreadsSpinner.setValue(data.numThreads);
        fixedLoopsSpinner.setValue(data.loops);
        useTimeCheckBox.setSelected(data.useTime);
        durationSpinner.setValue(data.duration);

        // 更新UI状态
        fixedLoopsSpinner.setEnabled(!data.useTime);
        durationSpinner.setEnabled(data.useTime);

        // 设置递增模式参数
        rampUpStartThreadsSpinner.setValue(data.rampUpStartThreads);
        rampUpEndThreadsSpinner.setValue(data.rampUpEndThreads);
        rampUpTimeSpinner.setValue(data.rampUpTime);

        // 设置尖刺模式参数
        spikeMinThreadsSpinner.setValue(data.spikeMinThreads);
        spikeMaxThreadsSpinner.setValue(data.spikeMaxThreads);
        spikeRampUpTimeSpinner.setValue(data.spikeRampUpTime);
        spikeHoldTimeSpinner.setValue(data.spikeHoldTime);
        spikeRampDownTimeSpinner.setValue(data.spikeRampDownTime);

        // 设置峰值模式参数
        peakMinThreadsSpinner.setValue(data.peakMinThreads);
        peakMaxThreadsSpinner.setValue(data.peakMaxThreads);
        peakIterationsSpinner.setValue(data.peakIterations);
        peakHoldTimeSpinner.setValue(data.peakHoldTime);

        // 设置阶梯模式参数
        stairsStartThreadsSpinner.setValue(data.stairsStartThreads);
        stairsEndThreadsSpinner.setValue(data.stairsEndThreads);
        stairsStepSpinner.setValue(data.stairsStep);
        stairsHoldTimeSpinner.setValue(data.stairsHoldTime);

        // 更新预览图
        updatePreview();
    }

    public void saveThreadGroupData() {
        if (currentNode == null) return;
        ThreadGroupData data = currentNode.threadGroupData;
        if (data == null) {
            data = new ThreadGroupData();
            currentNode.threadGroupData = data;
        }

        // 保存模式
        data.threadMode = (ThreadGroupData.ThreadMode) modeComboBox.getSelectedItem();

        // 保存固定模式参数
        data.numThreads = (Integer) fixedNumThreadsSpinner.getValue();
        data.loops = (Integer) fixedLoopsSpinner.getValue();
        data.useTime = useTimeCheckBox.isSelected();
        data.duration = (Integer) durationSpinner.getValue();

        // 保存递增模式参数
        data.rampUpStartThreads = (Integer) rampUpStartThreadsSpinner.getValue();
        data.rampUpEndThreads = (Integer) rampUpEndThreadsSpinner.getValue();
        data.rampUpTime = (Integer) rampUpTimeSpinner.getValue();

        // 保存尖刺模式参数
        data.spikeMinThreads = (Integer) spikeMinThreadsSpinner.getValue();
        data.spikeMaxThreads = (Integer) spikeMaxThreadsSpinner.getValue();
        data.spikeRampUpTime = (Integer) spikeRampUpTimeSpinner.getValue();
        data.spikeHoldTime = (Integer) spikeHoldTimeSpinner.getValue();
        data.spikeRampDownTime = (Integer) spikeRampDownTimeSpinner.getValue();

        // 保存峰值模式参数
        data.peakMinThreads = (Integer) peakMinThreadsSpinner.getValue();
        data.peakMaxThreads = (Integer) peakMaxThreadsSpinner.getValue();
        data.peakIterations = (Integer) peakIterationsSpinner.getValue();
        data.peakHoldTime = (Integer) peakHoldTimeSpinner.getValue();

        // 保存阶梯模式参数
        data.stairsStartThreads = (Integer) stairsStartThreadsSpinner.getValue();
        data.stairsEndThreads = (Integer) stairsEndThreadsSpinner.getValue();
        data.stairsStep = (Integer) stairsStepSpinner.getValue();
        data.stairsHoldTime = (Integer) stairsHoldTimeSpinner.getValue();
    }

    // 预览数据模型
    private static class ThreadLoadPreviewData {
        ThreadGroupData.ThreadMode mode;
        // 固定模式
        int fixedThreads;
        boolean useTime;
        int duration;
        int loops;
        // 递增模式
        int rampUpStartThreads;
        int rampUpEndThreads;
        int rampUpTime;
        // 尖刺模式
        int spikeMinThreads;
        int spikeMaxThreads;
        int spikeRampUpTime;
        int spikeHoldTime;
        int spikeRampDownTime;
        // 峰值模式
        int peakMinThreads;
        int peakMaxThreads;
        int peakIterations;
        int peakHoldTime;
        // 阶梯模式
        int stairsStartThreads;
        int stairsEndThreads;
        int stairsStep;
        int stairsHoldTime;
    }

    // 预览面板实现
    private static class ThreadLoadPreviewPanel extends JPanel {
        private ThreadLoadPreviewData previewData;
        private static final Color GRID_COLOR = new Color(220, 220, 220);
        private static final Color CURVE_COLOR = new Color(41, 121, 255);
        private static final Color AXIS_COLOR = new Color(100, 100, 100);
        private static final Color TEXT_COLOR = new Color(80, 80, 80);
        private static final int PADDING = 20;

        public ThreadLoadPreviewPanel() {
            setBackground(Color.WHITE);
        }

        public void setPreviewData(ThreadLoadPreviewData data) {
            this.previewData = data;
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (previewData == null) return;

            Graphics2D g2d = (Graphics2D) g.create();
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int width = getWidth() - 2 * PADDING;
            int height = getHeight() - 2 * PADDING;

            // 绘制网格
            drawGrid(g2d, width, height);

            // 根据不同模式绘制曲线
            drawCurve(g2d, width, height);

            g2d.dispose();
        }

        private void drawGrid(Graphics2D g2d, int width, int height) {
            g2d.setColor(GRID_COLOR);
            g2d.setStroke(new BasicStroke(0.5f));

            // 水平网格线
            for (int i = 0; i <= 4; i++) {
                int y = PADDING + i * height / 4;
                g2d.draw(new Line2D.Double(PADDING, y, PADDING + width, y));
            }

            // 垂直网格线
            for (int i = 0; i <= 4; i++) {
                int x = PADDING + i * width / 4;
                g2d.draw(new Line2D.Double(x, PADDING, x, PADDING + height));
            }

            // 坐标轴
            g2d.setColor(AXIS_COLOR);
            g2d.setStroke(new BasicStroke(1.0f));

            // X轴
            g2d.draw(new Line2D.Double(PADDING, PADDING + height, PADDING + width, PADDING + height));
            // Y轴
            g2d.draw(new Line2D.Double(PADDING, PADDING, PADDING, PADDING + height));

            // 坐标轴标签
            g2d.setColor(TEXT_COLOR);
            g2d.drawString("时间", PADDING + width / 2, PADDING + height + 15);

            // 旋转90度绘制Y轴标签
            g2d.rotate(-Math.PI / 2);
            g2d.drawString("线程数", -PADDING - height / 2 - 20, PADDING - 5);
            g2d.rotate(Math.PI / 2); // 恢复旋转
        }

        private void drawCurve(Graphics2D g2d, int width, int height) {
            g2d.setColor(CURVE_COLOR);
            g2d.setStroke(new BasicStroke(2.0f));

            List<Point> points = new ArrayList<>();

            switch (previewData.mode) {
                case FIXED:
                    drawFixedCurve(points, width, height);
                    break;
                case RAMP_UP:
                    drawRampUpCurve(points, width, height);
                    break;
                case SPIKE:
                    drawSpikeCurve(points, width, height);
                    break;
                case PEAK:
                    drawPeakCurve(points, width, height);
                    break;
                case STAIRS:
                    drawStairsCurve(points, width, height);
                    break;
            }

            // 绘制线段
            if (points.size() >= 2) {
                for (int i = 0; i < points.size() - 1; i++) {
                    Point p1 = points.get(i);
                    Point p2 = points.get(i + 1);
                    g2d.draw(new Line2D.Double(p1.x, p1.y, p2.x, p2.y));
                }
            }

            // 绘制点
            g2d.setColor(Color.WHITE);
            g2d.setStroke(new BasicStroke(1.0f));
            for (Point p : points) {
                g2d.fill(new Rectangle2D.Double(p.x - 3, p.y - 3, 6, 6));
                g2d.setColor(CURVE_COLOR);
                g2d.draw(new Rectangle2D.Double(p.x - 3, p.y - 3, 6, 6));
                g2d.setColor(Color.WHITE);
            }
        }

        private void drawFixedCurve(List<Point> points, int width, int height) {
            int x1 = PADDING;
            int y1 = PADDING + height;

            // 起点 (0,0)
            points.add(new Point(x1, y1));

            // 快速上升到固定线程数
            x1 += 10;
            int y2 = PADDING + height - height * previewData.fixedThreads / 100;
            if (y2 < PADDING) y2 = PADDING + 5;
            points.add(new Point(x1, y2));

            // 保持到结束
            x1 = PADDING + width;
            points.add(new Point(x1, y2));
        }

        private void drawRampUpCurve(List<Point> points, int width, int height) {
            int x1 = PADDING;
            int y1 = PADDING + height;

            // 起点 (0,0)
            points.add(new Point(x1, y1));

            // 递增起点
            x1 += 10;
            int y2 = PADDING + height - height * previewData.rampUpStartThreads / 100;
            if (y2 < PADDING) y2 = PADDING + 5;
            points.add(new Point(x1, y2));

            // 递增终点
            int x3 = PADDING + width - 10;
            int y3 = PADDING + height - height * previewData.rampUpEndThreads / 100;
            if (y3 < PADDING) y3 = PADDING + 5;
            points.add(new Point(x3, y3));

            // 结束点
            points.add(new Point(PADDING + width, y3));
        }

        private void drawSpikeCurve(List<Point> points, int width, int height) {
            int x = PADDING;
            int yMin = PADDING + height - height * previewData.spikeMinThreads / 100;
            if (yMin < PADDING) yMin = PADDING + 5;
            int yMax = PADDING + height - height * previewData.spikeMaxThreads / 100;
            if (yMax < PADDING) yMax = PADDING + 5;

            // 起点
            points.add(new Point(x, PADDING + height));

            // 最小线程
            x += 10;
            points.add(new Point(x, yMin));

            // 计算时间比例
            int totalTime = previewData.spikeRampUpTime + previewData.spikeHoldTime + previewData.spikeRampDownTime;
            int availWidth = width - 20;

            // 上升
            x += availWidth * previewData.spikeRampUpTime / totalTime;
            points.add(new Point(x, yMax));

            // 保持
            x += availWidth * previewData.spikeHoldTime / totalTime;
            points.add(new Point(x, yMax));

            // 下降
            x += availWidth * previewData.spikeRampDownTime / totalTime;
            points.add(new Point(x, yMin));

            // 结束
            points.add(new Point(PADDING + width, yMin));
        }

        private void drawPeakCurve(List<Point> points, int width, int height) {
            int x = PADDING;
            int yMin = PADDING + height - height * previewData.peakMinThreads / 100;
            if (yMin < PADDING) yMin = PADDING + 5;
            int yMax = PADDING + height - height * previewData.peakMaxThreads / 100;
            if (yMax < PADDING) yMax = PADDING + 5;

            // 起点
            points.add(new Point(x, PADDING + height));

            // 最小线程
            x += 10;
            points.add(new Point(x, yMin));

            // 循环峰值
            int iterations = Math.min(previewData.peakIterations, 5); // 最多显示5次循环
            int segmentWidth = (width - 20) / iterations / 2;

            for (int i = 0; i < iterations; i++) {
                // 上升到峰值
                x += segmentWidth;
                points.add(new Point(x, yMax));

                // 下降到谷值
                x += segmentWidth;
                points.add(new Point(x, yMin));
            }

            // 结束
            points.add(new Point(PADDING + width, yMin));
        }

        private void drawStairsCurve(List<Point> points, int width, int height) {
            int x = PADDING;
            int startThreads = previewData.stairsStartThreads;
            int endThreads = previewData.stairsEndThreads;
            int step = previewData.stairsStep;

            // 起点
            points.add(new Point(x, PADDING + height));

            // 起始点
            x += 10;
            int y = PADDING + height - height * startThreads / 100;
            if (y < PADDING) y = PADDING + 5;
            points.add(new Point(x, y));

            // 计算阶梯数
            int steps = (endThreads - startThreads) / step;
            if (steps <= 0) steps = 1;

            int segmentWidth = (width - 20) / steps / 2;
            int currentThreads = startThreads;

            for (int i = 0; i < steps && currentThreads < endThreads; i++) {
                // 当前阶梯水平线
                x += segmentWidth;
                points.add(new Point(x, y));

                // 上升到下一阶梯
                currentThreads += step;
                if (currentThreads > endThreads) currentThreads = endThreads;

                y = PADDING + height - height * currentThreads / 100;
                if (y < PADDING) y = PADDING + 5;

                points.add(new Point(x, y));

                // 下一阶梯水平线
                x += segmentWidth;
                points.add(new Point(x, y));
            }

            // 结束点
            points.add(new Point(PADDING + width, y));
        }
    }
}