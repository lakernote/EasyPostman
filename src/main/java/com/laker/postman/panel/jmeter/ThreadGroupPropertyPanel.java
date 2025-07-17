package com.laker.postman.panel.jmeter;

import javax.swing.*;
import java.awt.*;
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
    private final JSpinner rampUpDurationSpinner;

    // 尖刺模式面板组件
    private final JPanel spikePanel;
    private final JSpinner spikeMinThreadsSpinner;
    private final JSpinner spikeMaxThreadsSpinner;
    private final JSpinner spikeRampUpTimeSpinner;
    private final JSpinner spikeHoldTimeSpinner;
    private final JSpinner spikeRampDownTimeSpinner;
    private final JSpinner spikeDurationSpinner;

    // 峰值模式面板组件
    private final JPanel peakPanel;
    private final JSpinner peakMinThreadsSpinner;
    private final JSpinner peakMaxThreadsSpinner;
    private final JSpinner peakIterationsSpinner;
    private final JSpinner peakHoldTimeSpinner;
    private final JSpinner peakDurationSpinner;

    // 阶梯模式面板组件
    private final JPanel stairsPanel;
    private final JSpinner stairsStartThreadsSpinner;
    private final JSpinner stairsEndThreadsSpinner;
    private final JSpinner stairsStepSpinner;
    private final JSpinner stairsHoldTimeSpinner;
    private final JSpinner stairsDurationSpinner;

    // 负载模式预览相关
    private final ThreadLoadPreviewPanel previewPanel;

    ThreadGroupPropertyPanel() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        // 顶部模式选择区域
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 2));
        topPanel.add(new JLabel("线程模式:"));
        modeComboBox = new JComboBox<>(ThreadGroupData.ThreadMode.values());
        modeComboBox.setPreferredSize(new Dimension(150, 28));
        topPanel.add(modeComboBox);

        // 中间部分：左侧配置面板，右侧预览图
        JPanel mainPanel = new JPanel(new BorderLayout(10, 0));

        // 中间卡片布局，用于切换不同模式的配置面板
        cardLayout = new CardLayout();
        cardPanel = new JPanel(cardLayout);
        cardPanel.setPreferredSize(new Dimension(350, 150));

        // 初始化所有控件和面板
        // 1. 固定模式面板
        fixedPanel = new JPanel(new GridBagLayout());
        fixedPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        fixedNumThreadsSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 1000, 1));
        fixedNumThreadsSpinner.setPreferredSize(new Dimension(80, 28));
        fixedLoopsSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 100000, 1));
        fixedLoopsSpinner.setPreferredSize(new Dimension(80, 28));
        useTimeCheckBox = new JCheckBox("按时间");
        durationSpinner = new JSpinner(new SpinnerNumberModel(60, 1, 86400, 10));
        durationSpinner.setPreferredSize(new Dimension(80, 28));

        // 2. 递增模式面板
        rampUpPanel = new JPanel(new GridBagLayout());
        rampUpPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        rampUpStartThreadsSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 1000, 1));
        rampUpStartThreadsSpinner.setPreferredSize(new Dimension(80, 28));
        rampUpEndThreadsSpinner = new JSpinner(new SpinnerNumberModel(10, 1, 1000, 1));
        rampUpEndThreadsSpinner.setPreferredSize(new Dimension(80, 28));
        rampUpTimeSpinner = new JSpinner(new SpinnerNumberModel(30, 1, 3600, 5));
        rampUpTimeSpinner.setPreferredSize(new Dimension(80, 28));
        rampUpDurationSpinner = new JSpinner(new SpinnerNumberModel(120, 1, 86400, 10));
        rampUpDurationSpinner.setPreferredSize(new Dimension(80, 28));

        // 3. 尖刺模式面板
        spikePanel = new JPanel(new GridBagLayout());
        spikePanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        spikeMinThreadsSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 1000, 1));
        spikeMinThreadsSpinner.setPreferredSize(new Dimension(80, 28));
        spikeMaxThreadsSpinner = new JSpinner(new SpinnerNumberModel(20, 1, 1000, 1));
        spikeMaxThreadsSpinner.setPreferredSize(new Dimension(80, 28));
        spikeRampUpTimeSpinner = new JSpinner(new SpinnerNumberModel(10, 1, 3600, 1));
        spikeRampUpTimeSpinner.setPreferredSize(new Dimension(80, 28));
        spikeHoldTimeSpinner = new JSpinner(new SpinnerNumberModel(5, 0, 3600, 1));
        spikeHoldTimeSpinner.setPreferredSize(new Dimension(80, 28));
        spikeRampDownTimeSpinner = new JSpinner(new SpinnerNumberModel(10, 1, 3600, 1));
        spikeRampDownTimeSpinner.setPreferredSize(new Dimension(80, 28));
        spikeDurationSpinner = new JSpinner(new SpinnerNumberModel(120, 1, 86400, 10));
        spikeDurationSpinner.setPreferredSize(new Dimension(80, 28));

        // 4. 峰值模式面板
        peakPanel = new JPanel(new GridBagLayout());
        peakPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        peakMinThreadsSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 1000, 1));
        peakMinThreadsSpinner.setPreferredSize(new Dimension(80, 28));
        peakMaxThreadsSpinner = new JSpinner(new SpinnerNumberModel(20, 1, 1000, 1));
        peakMaxThreadsSpinner.setPreferredSize(new Dimension(80, 28));
        peakIterationsSpinner = new JSpinner(new SpinnerNumberModel(3, 1, 100, 1));
        peakIterationsSpinner.setPreferredSize(new Dimension(80, 28));
        peakHoldTimeSpinner = new JSpinner(new SpinnerNumberModel(10, 1, 3600, 1));
        peakHoldTimeSpinner.setPreferredSize(new Dimension(80, 28));
        peakDurationSpinner = new JSpinner(new SpinnerNumberModel(180, 1, 86400, 10));
        peakDurationSpinner.setPreferredSize(new Dimension(80, 28));

        // 5. 阶梯模式面板
        stairsPanel = new JPanel(new GridBagLayout());
        stairsPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        stairsStartThreadsSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 1000, 1));
        stairsStartThreadsSpinner.setPreferredSize(new Dimension(80, 28));
        stairsEndThreadsSpinner = new JSpinner(new SpinnerNumberModel(20, 1, 1000, 1));
        stairsEndThreadsSpinner.setPreferredSize(new Dimension(80, 28));
        stairsStepSpinner = new JSpinner(new SpinnerNumberModel(5, 1, 100, 1));
        stairsStepSpinner.setPreferredSize(new Dimension(80, 28));
        stairsHoldTimeSpinner = new JSpinner(new SpinnerNumberModel(10, 1, 3600, 1));
        stairsHoldTimeSpinner.setPreferredSize(new Dimension(80, 28));
        stairsDurationSpinner = new JSpinner(new SpinnerNumberModel(240, 1, 86400, 10));
        stairsDurationSpinner.setPreferredSize(new Dimension(80, 28));

        // 设置各个面板的布局
        setupFixedPanel();
        setupRampUpPanel();
        setupSpikePanel();
        setupPeakPanel();
        setupStairsPanel();

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
                updatePreview();
            }
        });

        // 初始设置
        durationSpinner.setEnabled(false); // 默认按循环次数执行

        // 按时间执行时禁用循环次数，按循环次数执行时禁用持续时间
        useTimeCheckBox.addActionListener(e -> {
            boolean useTime = useTimeCheckBox.isSelected();
            fixedLoopsSpinner.setEnabled(!useTime);
            durationSpinner.setEnabled(useTime);
        });

        // 预览图表区域
        previewPanel = new ThreadLoadPreviewPanel();
        previewPanel.setPreferredSize(new Dimension(500, 180));
        previewPanel.setBorder(BorderFactory.createTitledBorder("负载模式预览"));

        // 左侧配置区包装在一个面板中，以便控制布局
        JPanel configPanel = new JPanel(new BorderLayout());
        configPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 5));
        configPanel.add(cardPanel, BorderLayout.NORTH);
        configPanel.setPreferredSize(new Dimension(400, 180));

        // 添加到主面板
        mainPanel.add(configPanel, BorderLayout.WEST);
        mainPanel.add(previewPanel, BorderLayout.CENTER);

        // 整体布局
        add(topPanel, BorderLayout.NORTH);
        add(mainPanel, BorderLayout.CENTER);

        // 为所有输入组件添加变化监听，刷新预览图
        addPreviewUpdateListeners();
    }

    // 设置固定模式面板
    private void setupFixedPanel() {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(3, 5, 3, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;

        // 第一行
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 1;
        fixedPanel.add(new JLabel("用户数:", SwingConstants.RIGHT), gbc);

        gbc.gridx = 1;
        fixedPanel.add(fixedNumThreadsSpinner, gbc);

        gbc.gridx = 2;
        fixedPanel.add(new JLabel("执行方式:", SwingConstants.RIGHT), gbc);

        gbc.gridx = 3;
        fixedPanel.add(useTimeCheckBox, gbc);

        // 第二行
        gbc.gridx = 0;
        gbc.gridy = 1;
        fixedPanel.add(new JLabel("循环次数:", SwingConstants.RIGHT), gbc);

        gbc.gridx = 1;
        fixedPanel.add(fixedLoopsSpinner, gbc);

        gbc.gridx = 2;
        fixedPanel.add(new JLabel("持续时间(秒):", SwingConstants.RIGHT), gbc);

        gbc.gridx = 3;
        fixedPanel.add(durationSpinner, gbc);
    }

    // 设置递增模式面板
    private void setupRampUpPanel() {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(3, 5, 3, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;

        // 第一行
        gbc.gridx = 0;
        gbc.gridy = 0;
        rampUpPanel.add(new JLabel("起始用户数:", SwingConstants.RIGHT), gbc);

        gbc.gridx = 1;
        rampUpPanel.add(rampUpStartThreadsSpinner, gbc);

        gbc.gridx = 2;
        rampUpPanel.add(new JLabel("最终用户数:", SwingConstants.RIGHT), gbc);

        gbc.gridx = 3;
        rampUpPanel.add(rampUpEndThreadsSpinner, gbc);

        // 第二行
        gbc.gridx = 0;
        gbc.gridy = 1;
        rampUpPanel.add(new JLabel("递增时间(秒):", SwingConstants.RIGHT), gbc);

        gbc.gridx = 1;
        rampUpPanel.add(rampUpTimeSpinner, gbc);

        gbc.gridx = 2;
        rampUpPanel.add(new JLabel("测试持续(秒):", SwingConstants.RIGHT), gbc);

        gbc.gridx = 3;
        rampUpPanel.add(rampUpDurationSpinner, gbc);
    }

    // 设置尖刺模式面板
    private void setupSpikePanel() {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(3, 5, 3, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;

        // 第一行
        gbc.gridx = 0;
        gbc.gridy = 0;
        spikePanel.add(new JLabel("最小用户数:", SwingConstants.RIGHT), gbc);

        gbc.gridx = 1;
        spikePanel.add(spikeMinThreadsSpinner, gbc);

        gbc.gridx = 2;
        spikePanel.add(new JLabel("最大用户数:", SwingConstants.RIGHT), gbc);

        gbc.gridx = 3;
        spikePanel.add(spikeMaxThreadsSpinner, gbc);

        // 第二行
        gbc.gridx = 0;
        gbc.gridy = 1;
        spikePanel.add(new JLabel("上升时间(秒):", SwingConstants.RIGHT), gbc);

        gbc.gridx = 1;
        spikePanel.add(spikeRampUpTimeSpinner, gbc);

        gbc.gridx = 2;
        spikePanel.add(new JLabel("保持时间(秒):", SwingConstants.RIGHT), gbc);

        gbc.gridx = 3;
        spikePanel.add(spikeHoldTimeSpinner, gbc);

        // 第三行
        gbc.gridx = 0;
        gbc.gridy = 2;
        spikePanel.add(new JLabel("下降时间(秒):", SwingConstants.RIGHT), gbc);

        gbc.gridx = 1;
        spikePanel.add(spikeRampDownTimeSpinner, gbc);

        gbc.gridx = 2;
        spikePanel.add(new JLabel("测试持续(秒):", SwingConstants.RIGHT), gbc);

        gbc.gridx = 3;
        spikePanel.add(spikeDurationSpinner, gbc);
    }

    // 设置峰值模式面板
    private void setupPeakPanel() {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(3, 5, 3, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;

        // 第一行
        gbc.gridx = 0;
        gbc.gridy = 0;
        peakPanel.add(new JLabel("最小用户数:", SwingConstants.RIGHT), gbc);

        gbc.gridx = 1;
        peakPanel.add(peakMinThreadsSpinner, gbc);

        gbc.gridx = 2;
        peakPanel.add(new JLabel("最大用户数:", SwingConstants.RIGHT), gbc);

        gbc.gridx = 3;
        peakPanel.add(peakMaxThreadsSpinner, gbc);

        // 第二行
        gbc.gridx = 0;
        gbc.gridy = 1;
        peakPanel.add(new JLabel("峰值次数:", SwingConstants.RIGHT), gbc);

        gbc.gridx = 1;
        peakPanel.add(peakIterationsSpinner, gbc);

        gbc.gridx = 2;
        peakPanel.add(new JLabel("保持时间(秒):", SwingConstants.RIGHT), gbc);

        gbc.gridx = 3;
        peakPanel.add(peakHoldTimeSpinner, gbc);

        // 第三行
        gbc.gridx = 0;
        gbc.gridy = 2;
        peakPanel.add(new JLabel("测试持续(秒):", SwingConstants.RIGHT), gbc);

        gbc.gridx = 1;
        peakPanel.add(peakDurationSpinner, gbc);
    }

    // 设置阶梯模式面板
    private void setupStairsPanel() {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(3, 5, 3, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;

        // 第一行
        gbc.gridx = 0;
        gbc.gridy = 0;
        stairsPanel.add(new JLabel("起始用户数:", SwingConstants.RIGHT), gbc);

        gbc.gridx = 1;
        stairsPanel.add(stairsStartThreadsSpinner, gbc);

        gbc.gridx = 2;
        stairsPanel.add(new JLabel("最终用户数:", SwingConstants.RIGHT), gbc);

        gbc.gridx = 3;
        stairsPanel.add(stairsEndThreadsSpinner, gbc);

        // 第二行
        gbc.gridx = 0;
        gbc.gridy = 1;
        stairsPanel.add(new JLabel("阶梯步长:", SwingConstants.RIGHT), gbc);

        gbc.gridx = 1;
        stairsPanel.add(stairsStepSpinner, gbc);

        gbc.gridx = 2;
        stairsPanel.add(new JLabel("阶梯保持(秒):", SwingConstants.RIGHT), gbc);

        gbc.gridx = 3;
        stairsPanel.add(stairsHoldTimeSpinner, gbc);

        // 第三行
        gbc.gridx = 0;
        gbc.gridy = 2;
        stairsPanel.add(new JLabel("测试持续(秒):", SwingConstants.RIGHT), gbc);

        gbc.gridx = 1;
        stairsPanel.add(stairsDurationSpinner, gbc);
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
        rampUpDurationSpinner.addChangeListener(e -> updatePreview());

        // 尖刺模式参数变化监听
        spikeMinThreadsSpinner.addChangeListener(e -> updatePreview());
        spikeMaxThreadsSpinner.addChangeListener(e -> updatePreview());
        spikeRampUpTimeSpinner.addChangeListener(e -> updatePreview());
        spikeHoldTimeSpinner.addChangeListener(e -> updatePreview());
        spikeRampDownTimeSpinner.addChangeListener(e -> updatePreview());
        spikeDurationSpinner.addChangeListener(e -> updatePreview());

        // 峰值模式参数变化监听
        peakMinThreadsSpinner.addChangeListener(e -> updatePreview());
        peakMaxThreadsSpinner.addChangeListener(e -> updatePreview());
        peakIterationsSpinner.addChangeListener(e -> updatePreview());
        peakHoldTimeSpinner.addChangeListener(e -> updatePreview());
        peakDurationSpinner.addChangeListener(e -> updatePreview());

        // 阶梯模式参数变化监听
        stairsStartThreadsSpinner.addChangeListener(e -> updatePreview());
        stairsEndThreadsSpinner.addChangeListener(e -> updatePreview());
        stairsStepSpinner.addChangeListener(e -> updatePreview());
        stairsHoldTimeSpinner.addChangeListener(e -> updatePreview());
        stairsDurationSpinner.addChangeListener(e -> updatePreview());
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
                previewData.rampUpDuration = (Integer) rampUpDurationSpinner.getValue();
                break;

            case SPIKE:
                previewData.spikeMinThreads = (Integer) spikeMinThreadsSpinner.getValue();
                previewData.spikeMaxThreads = (Integer) spikeMaxThreadsSpinner.getValue();
                previewData.spikeRampUpTime = (Integer) spikeRampUpTimeSpinner.getValue();
                previewData.spikeHoldTime = (Integer) spikeHoldTimeSpinner.getValue();
                previewData.spikeRampDownTime = (Integer) spikeRampDownTimeSpinner.getValue();
                previewData.spikeDuration = (Integer) spikeDurationSpinner.getValue();
                break;

            case PEAK:
                previewData.peakMinThreads = (Integer) peakMinThreadsSpinner.getValue();
                previewData.peakMaxThreads = (Integer) peakMaxThreadsSpinner.getValue();
                previewData.peakIterations = (Integer) peakIterationsSpinner.getValue();
                previewData.peakHoldTime = (Integer) peakHoldTimeSpinner.getValue();
                previewData.peakDuration = (Integer) peakDurationSpinner.getValue();
                break;

            case STAIRS:
                previewData.stairsStartThreads = (Integer) stairsStartThreadsSpinner.getValue();
                previewData.stairsEndThreads = (Integer) stairsEndThreadsSpinner.getValue();
                previewData.stairsStep = (Integer) stairsStepSpinner.getValue();
                previewData.stairsHoldTime = (Integer) stairsHoldTimeSpinner.getValue();
                previewData.stairsDuration = (Integer) stairsDurationSpinner.getValue();
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
        rampUpDurationSpinner.setValue(data.rampUpDuration);

        // 设置尖刺模式参数
        spikeMinThreadsSpinner.setValue(data.spikeMinThreads);
        spikeMaxThreadsSpinner.setValue(data.spikeMaxThreads);
        spikeRampUpTimeSpinner.setValue(data.spikeRampUpTime);
        spikeHoldTimeSpinner.setValue(data.spikeHoldTime);
        spikeRampDownTimeSpinner.setValue(data.spikeRampDownTime);
        spikeDurationSpinner.setValue(data.spikeDuration);

        // 设置峰值模式参数
        peakMinThreadsSpinner.setValue(data.peakMinThreads);
        peakMaxThreadsSpinner.setValue(data.peakMaxThreads);
        peakIterationsSpinner.setValue(data.peakIterations);
        peakHoldTimeSpinner.setValue(data.peakHoldTime);
        peakDurationSpinner.setValue(data.peakDuration);

        // 设置阶梯模式参数
        stairsStartThreadsSpinner.setValue(data.stairsStartThreads);
        stairsEndThreadsSpinner.setValue(data.stairsEndThreads);
        stairsStepSpinner.setValue(data.stairsStep);
        stairsHoldTimeSpinner.setValue(data.stairsHoldTime);
        stairsDurationSpinner.setValue(data.stairsDuration);

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
        data.rampUpDuration = (Integer) rampUpDurationSpinner.getValue();

        // 保存尖刺模式参数
        data.spikeMinThreads = (Integer) spikeMinThreadsSpinner.getValue();
        data.spikeMaxThreads = (Integer) spikeMaxThreadsSpinner.getValue();
        data.spikeRampUpTime = (Integer) spikeRampUpTimeSpinner.getValue();
        data.spikeHoldTime = (Integer) spikeHoldTimeSpinner.getValue();
        data.spikeRampDownTime = (Integer) spikeRampDownTimeSpinner.getValue();
        data.spikeDuration = (Integer) spikeDurationSpinner.getValue();

        // 保存峰值模式参数
        data.peakMinThreads = (Integer) peakMinThreadsSpinner.getValue();
        data.peakMaxThreads = (Integer) peakMaxThreadsSpinner.getValue();
        data.peakIterations = (Integer) peakIterationsSpinner.getValue();
        data.peakHoldTime = (Integer) peakHoldTimeSpinner.getValue();
        data.peakDuration = (Integer) peakDurationSpinner.getValue();

        // 保存阶梯模式参数
        data.stairsStartThreads = (Integer) stairsStartThreadsSpinner.getValue();
        data.stairsEndThreads = (Integer) stairsEndThreadsSpinner.getValue();
        data.stairsStep = (Integer) stairsStepSpinner.getValue();
        data.stairsHoldTime = (Integer) stairsHoldTimeSpinner.getValue();
        data.stairsDuration = (Integer) stairsDurationSpinner.getValue();
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
        int rampUpDuration;
        // 尖刺模式
        int spikeMinThreads;
        int spikeMaxThreads;
        int spikeRampUpTime;
        int spikeHoldTime;
        int spikeRampDownTime;
        int spikeDuration;
        // 峰值模式
        int peakMinThreads;
        int peakMaxThreads;
        int peakIterations;
        int peakHoldTime;
        int peakDuration;
        // 阶梯模式
        int stairsStartThreads;
        int stairsEndThreads;
        int stairsStep;
        int stairsHoldTime;
        int stairsDuration;
    }

    // 预览面板实现
    private static class ThreadLoadPreviewPanel extends JPanel {
        private ThreadLoadPreviewData previewData;
        private static final Color GRID_COLOR = new Color(220, 220, 220);
        private static final Color CURVE_COLOR = new Color(41, 121, 255);
        private static final Color AXIS_COLOR = new Color(100, 100, 100);
        private static final Color TEXT_COLOR = new Color(80, 80, 80);
        private static final int PADDING = 40;

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

            // 绘制网格和坐标轴
            drawGrid(g2d, width, height);

            // 根据不同模式绘制曲线
            drawCurve(g2d, width, height);

            g2d.dispose();
        }

        private void drawGrid(Graphics2D g2d, int width, int height) {
            g2d.setColor(GRID_COLOR);
            g2d.setStroke(new BasicStroke(0.5f));

            // 水平网格线 - 5条线，均匀分布
            int maxThreads = getMaxThreads();
            for (int i = 0; i <= 5; i++) {
                int y = PADDING + i * height / 5;
                g2d.draw(new Line2D.Double(PADDING, y, PADDING + width, y));

                // 添加Y轴刻度
                if (i > 0) {
                    int threadValue = maxThreads - (maxThreads * i / 5);
                    g2d.setColor(TEXT_COLOR);
                    g2d.drawString(String.valueOf(threadValue), PADDING - 30, y + 5);
                    g2d.setColor(GRID_COLOR);
                }
            }

            // 垂直网格线 - 根据测试持续时间计算
            int duration = getDuration();
            int numVerticalLines = Math.min(10, duration); // 最多10条垂直线
            for (int i = 0; i <= numVerticalLines; i++) {
                int x = PADDING + i * width / numVerticalLines;
                g2d.draw(new Line2D.Double(x, PADDING, x, PADDING + height));

                // 添加X轴刻度
                if (i > 0) {
                    int timeValue = duration * i / numVerticalLines;
                    g2d.setColor(TEXT_COLOR);
                    g2d.drawString(timeValue + "s", x - 10, PADDING + height + 15);
                    g2d.setColor(GRID_COLOR);
                }
            }

            // 坐标轴
            g2d.setColor(AXIS_COLOR);
            g2d.setStroke(new BasicStroke(1.5f));

            // X轴
            g2d.draw(new Line2D.Double(PADDING, PADDING + height, PADDING + width, PADDING + height));
            // Y轴
            g2d.draw(new Line2D.Double(PADDING, PADDING, PADDING, PADDING + height));

            // 坐标轴标签
            g2d.setColor(TEXT_COLOR);
            g2d.drawString("时间 (秒)", PADDING + width / 2 - 20, PADDING + height + 30);

//            // 旋转90度绘制Y轴标签 - 简化为只显示"线程"二字
//            g2d.rotate(-Math.PI / 2);
//            g2d.drawString("线程", -PADDING - height / 2 - 20, PADDING - 15);
//            g2d.rotate(Math.PI / 2); // 恢复旋转

            // 在左上角添加模式信息
            g2d.drawString("模式: " + previewData.mode.getDisplayName(), PADDING, PADDING - 10);
        }

        private int getMaxThreads() {

            // 根据当前模式计算实际使用的最大线程数
            int actualMax;
            switch (previewData.mode) {
                case FIXED:
                    actualMax = previewData.fixedThreads;
                    break;
                case RAMP_UP:
                    actualMax = Math.max(previewData.rampUpStartThreads, previewData.rampUpEndThreads);
                    break;
                case SPIKE:
                    actualMax = previewData.spikeMaxThreads;
                    break;
                case PEAK:
                    actualMax = previewData.peakMaxThreads;
                    break;
                case STAIRS:
                    actualMax = previewData.stairsEndThreads;
                    break;
                default:
                    actualMax = 20; // 默认值
            }
            // 增加30% 的安全余量
            return (int) (actualMax * 1.3);
        }

        private int getDuration() {
            // 根据当前模式返回持续时间（用于X轴刻度）
            switch (previewData.mode) {
                case FIXED:
                    return previewData.useTime ? previewData.duration : 60;
                case RAMP_UP:
                    return previewData.rampUpDuration;
                case SPIKE:
                    return previewData.spikeDuration;
                case PEAK:
                    return previewData.peakDuration;
                case STAIRS:
                    return previewData.stairsDuration;
                default:
                    return 60;
            }
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
            int maxThreads = getMaxThreads();

            int x1 = PADDING;
            int y1 = PADDING + height;

            // 起点 (0,0)
            points.add(new Point(x1, y1));

            // 快速上升到固定线程数
            x1 += 10;
            int y2 = PADDING + height - height * previewData.fixedThreads / maxThreads;
            if (y2 < PADDING) y2 = PADDING + 5;
            points.add(new Point(x1, y2));

            // 保持到结束
            x1 = PADDING + width;
            points.add(new Point(x1, y2));
        }

        private void drawRampUpCurve(List<Point> points, int width, int height) {
            int maxThreads = getMaxThreads();
            int duration = getDuration();

            int x1 = PADDING;
            int y1 = PADDING + height;

            // 起点 (0,0)
            points.add(new Point(x1, y1));

            // 递增起点
            x1 += 10;
            int y2 = PADDING + height - height * previewData.rampUpStartThreads / maxThreads;
            if (y2 < PADDING) y2 = PADDING + 5;
            points.add(new Point(x1, y2));

            // 递增终点
            int rampUpEndX = PADDING + (width * previewData.rampUpTime / duration);
            int y3 = PADDING + height - height * previewData.rampUpEndThreads / maxThreads;
            if (y3 < PADDING) y3 = PADDING + 5;
            points.add(new Point(rampUpEndX, y3));

            // 结束点
            points.add(new Point(PADDING + width, y3));
        }

        private void drawSpikeCurve(List<Point> points, int width, int height) {
            int maxThreads = getMaxThreads();
            int duration = getDuration();

            int x = PADDING;
            int yMin = PADDING + height - height * previewData.spikeMinThreads / maxThreads;
            if (yMin < PADDING) yMin = PADDING + 5;
            int yMax = PADDING + height - height * previewData.spikeMaxThreads / maxThreads;
            if (yMax < PADDING) yMax = PADDING + 5;

            // 起点
            points.add(new Point(x, PADDING + height));

            // 最小线程
            x += 10;
            points.add(new Point(x, yMin));

            // 计算时间比例
            int totalPhaseTime = previewData.spikeRampUpTime + previewData.spikeHoldTime + previewData.spikeRampDownTime;
            int availWidth = width - 20;

            // 上升
            x += availWidth * previewData.spikeRampUpTime / duration;
            points.add(new Point(x, yMax));

            // 保持
            x += availWidth * previewData.spikeHoldTime / duration;
            points.add(new Point(x, yMax));

            // 下降
            x += availWidth * previewData.spikeRampDownTime / duration;
            points.add(new Point(x, yMin));

            // 结束
            points.add(new Point(PADDING + width, yMin));
        }

        private void drawPeakCurve(List<Point> points, int width, int height) {
            int maxThreads = getMaxThreads();
            int duration = getDuration();

            int x = PADDING;
            int yMin = PADDING + height - height * previewData.peakMinThreads / maxThreads;
            if (yMin < PADDING) yMin = PADDING + 5;
            int yMax = PADDING + height - height * previewData.peakMaxThreads / maxThreads;
            if (yMax < PADDING) yMax = PADDING + 5;

            // 起点
            points.add(new Point(x, PADDING + height));

            // 最小线程
            x += 10;
            points.add(new Point(x, yMin));

            // 循环峰值
            int iterations = previewData.peakIterations;
            int cycleTime = previewData.peakHoldTime * 3;  // 每个循环: 上升+保持+下降
            int cycleWidth = (width - 20) / duration * cycleTime;

            for (int i = 0; i < iterations; i++) {
                // 计算每个周期的起始位置
                int cycleStartX = x + (i * cycleWidth);

                // 保证不超出图表宽度
                if (cycleStartX > PADDING + width) break;

                // 上升到峰值
                int peakX = cycleStartX + cycleWidth / 3;
                peakX = Math.min(peakX, PADDING + width);
                points.add(new Point(peakX, yMax));

                // 保持峰值
                int holdEndX = peakX + cycleWidth / 3;
                holdEndX = Math.min(holdEndX, PADDING + width);
                points.add(new Point(holdEndX, yMax));

                // 下降到谷值
                int valleyX = holdEndX + cycleWidth / 3;
                valleyX = Math.min(valleyX, PADDING + width);
                points.add(new Point(valleyX, yMin));
            }

            // 结束
            points.add(new Point(PADDING + width, yMin));
        }

        private void drawStairsCurve(List<Point> points, int width, int height) {
            int maxThreads = getMaxThreads();
            int duration = getDuration();

            int x = PADDING;
            int startThreads = previewData.stairsStartThreads;
            int endThreads = previewData.stairsEndThreads;
            int step = previewData.stairsStep;

            // 起点
            points.add(new Point(x, PADDING + height));

            // 起始点
            x += 10;
            int y = PADDING + height - height * startThreads / maxThreads;
            if (y < PADDING) y = PADDING + 5;
            points.add(new Point(x, y));

            // 计算阶梯数
            int steps = (endThreads - startThreads) / step;
            if (steps <= 0) steps = 1;

            int holdTime = previewData.stairsHoldTime;
            int stepWidth = (width - 20) / duration * holdTime;
            int currentThreads = startThreads;

            for (int i = 0; i < steps && currentThreads < endThreads; i++) {
                // 当前阶梯水平线
                x += stepWidth;
                if (x > PADDING + width) break;
                points.add(new Point(x, y));

                // 上升到下一阶梯
                currentThreads += step;
                if (currentThreads > endThreads) currentThreads = endThreads;

                y = PADDING + height - height * currentThreads / maxThreads;
                if (y < PADDING) y = PADDING + 5;

                points.add(new Point(x, y));
            }

            // 结束点
            points.add(new Point(PADDING + width, y));
        }
    }
}

