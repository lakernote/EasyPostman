package com.laker.postman.panel.toolbox;

import com.formdev.flatlaf.extras.components.FlatTextField;
import com.laker.postman.util.EasyPostManFontUtil;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 时间戳转换工具面板
 */
@Slf4j
public class TimestampPanel extends JPanel {

    private FlatTextField timestampField;
    private FlatTextField dateField;
    private JTextArea resultArea;
    private JComboBox<String> unitCombo;
    private Timer timer;

    public TimestampPanel() {
        initUI();
        startAutoRefresh();
    }

    private void initUI() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // 主容器使用垂直布局
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));

        // 1. 当前时间戳区域
        mainPanel.add(createCurrentTimestampPanel());
        mainPanel.add(Box.createVerticalStrut(15));

        // 2. 时间戳转日期区域
        mainPanel.add(createTimestampToDatePanel());
        mainPanel.add(Box.createVerticalStrut(15));

        // 3. 日期转时间戳区域
        mainPanel.add(createDateToTimestampPanel());
        mainPanel.add(Box.createVerticalStrut(15));

        // 4. 结果显示区域
        mainPanel.add(createResultPanel());

        add(mainPanel, BorderLayout.CENTER);
    }

    /**
     * 创建当前时间戳面板
     */
    private JPanel createCurrentTimestampPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 5));
        panel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(),
                I18nUtil.getMessage(MessageKeys.TOOLBOX_TIMESTAMP_CURRENT)
        ));

        // 时间戳显示标签
        JLabel currentLabel = new JLabel(String.valueOf(System.currentTimeMillis()));
        currentLabel.setFont(EasyPostManFontUtil.getDefaultFont(Font.BOLD, 14));
        currentLabel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));

        // 按钮面板
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));

        JButton copyCurrentBtn = new JButton(I18nUtil.getMessage(MessageKeys.BUTTON_COPY));
        copyCurrentBtn.addActionListener(e -> {
            Toolkit.getDefaultToolkit().getSystemClipboard()
                    .setContents(new StringSelection(currentLabel.getText()), null);
            JOptionPane.showMessageDialog(this,
                    I18nUtil.getMessage(MessageKeys.SUCCESS),
                    I18nUtil.getMessage(MessageKeys.TIP),
                    JOptionPane.INFORMATION_MESSAGE);
        });

        JButton refreshBtn = new JButton(I18nUtil.getMessage(MessageKeys.BUTTON_REFRESH));
        refreshBtn.addActionListener(e -> currentLabel.setText(String.valueOf(System.currentTimeMillis())));

        btnPanel.add(copyCurrentBtn);
        btnPanel.add(refreshBtn);

        panel.add(currentLabel, BorderLayout.CENTER);
        panel.add(btnPanel, BorderLayout.EAST);

        // 自动刷新当前时间戳
        timer = new Timer(1000, e -> currentLabel.setText(String.valueOf(System.currentTimeMillis())));

        return panel;
    }

    /**
     * 创建时间戳转日期面板
     */
    private JPanel createTimestampToDatePanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(),
                I18nUtil.getMessage(MessageKeys.TOOLBOX_TIMESTAMP_TO_DATE)
        ));

        // 输入区域
        JPanel inputPanel = new JPanel(new BorderLayout(10, 5));

        // 输入框和单位选择在同一行
        JPanel fieldPanel = new JPanel(new BorderLayout(5, 0));
        timestampField = new FlatTextField();
        timestampField.setPlaceholderText("1729468800000");
        timestampField.setBackground(Color.WHITE);
        timestampField.setPreferredSize(new Dimension(200, 32));

        unitCombo = new JComboBox<>(new String[]{
                I18nUtil.getMessage(MessageKeys.TOOLBOX_TIMESTAMP_MILLISECONDS),
                I18nUtil.getMessage(MessageKeys.TOOLBOX_TIMESTAMP_SECONDS)
        });
        unitCombo.setPreferredSize(new Dimension(120, 32));

        fieldPanel.add(timestampField, BorderLayout.CENTER);
        fieldPanel.add(unitCombo, BorderLayout.EAST);

        inputPanel.add(new JLabel(I18nUtil.getMessage(MessageKeys.TOOLBOX_TIMESTAMP_INPUT) + ":"), BorderLayout.WEST);
        inputPanel.add(fieldPanel, BorderLayout.CENTER);

        // 转换按钮
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 5));
        JButton convertBtn = new JButton("🔄 " + I18nUtil.getMessage(MessageKeys.TOOLBOX_TIMESTAMP_TO_DATE));
        convertBtn.addActionListener(e -> convertToDate());
        btnPanel.add(convertBtn);

        panel.add(inputPanel, BorderLayout.NORTH);
        panel.add(btnPanel, BorderLayout.CENTER);

        return panel;
    }

    /**
     * 创建日期转时间戳面板
     */
    private JPanel createDateToTimestampPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(),
                "Date to Timestamp"
        ));

        // 输入区域
        JPanel inputPanel = new JPanel(new BorderLayout(10, 5));

        dateField = new FlatTextField();
        dateField.setPlaceholderText("2025-10-21 12:00:00");
        dateField.setToolTipText("Format: yyyy-MM-dd HH:mm:ss");
        dateField.setBackground(Color.WHITE);
        dateField.setPreferredSize(new Dimension(200, 32));

        inputPanel.add(new JLabel("Date Input:"), BorderLayout.WEST);
        inputPanel.add(dateField, BorderLayout.CENTER);

        // 快速填充按钮
        JPanel quickBtnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        JButton nowBtn = new JButton("Now");
        nowBtn.setToolTipText("Fill with current date time");
        nowBtn.addActionListener(e -> {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            dateField.setText(sdf.format(new Date()));
        });
        quickBtnPanel.add(nowBtn);

        inputPanel.add(quickBtnPanel, BorderLayout.EAST);

        // 转换按钮
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 5));
        JButton convertBtn = new JButton("🔄 To Timestamp");
        convertBtn.addActionListener(e -> convertToTimestamp());
        btnPanel.add(convertBtn);

        panel.add(inputPanel, BorderLayout.NORTH);
        panel.add(btnPanel, BorderLayout.CENTER);

        return panel;
    }

    /**
     * 创建结果显示面板
     */
    private JPanel createResultPanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(),
                I18nUtil.getMessage(MessageKeys.TOOLBOX_TIMESTAMP_OUTPUT)
        ));

        resultArea = new JTextArea();
        resultArea.setEditable(false);
        resultArea.setRows(12);
        resultArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        resultArea.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        JScrollPane scrollPane = new JScrollPane(resultArea);
        scrollPane.setPreferredSize(new Dimension(400, 250));

        // 按钮面板
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 5));
        JButton copyBtn = new JButton(I18nUtil.getMessage(MessageKeys.BUTTON_COPY));
        copyBtn.addActionListener(e -> copyToClipboard());

        JButton clearBtn = new JButton("Clear");
        clearBtn.addActionListener(e -> resultArea.setText(""));

        btnPanel.add(clearBtn);
        btnPanel.add(copyBtn);

        panel.add(scrollPane, BorderLayout.CENTER);
        panel.add(btnPanel, BorderLayout.SOUTH);

        return panel;
    }

    private void startAutoRefresh() {
        if (timer != null && !timer.isRunning()) {
            timer.start();
        }
    }

    private void convertToDate() {
        String input = timestampField.getText().trim();
        if (input.isEmpty()) {
            resultArea.setText("");
            return;
        }

        try {
            long timestamp = Long.parseLong(input);

            // 如果选择的是秒，转换为毫秒
            if (unitCombo.getSelectedIndex() == 1) {
                timestamp *= 1000;
            }

            Date date = new Date(timestamp);
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            SimpleDateFormat sdf2 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
            SimpleDateFormat sdf3 = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss 'GMT'");

            StringBuilder sb = new StringBuilder();
            sb.append("📅 Formatted Dates:\n\n");
            sb.append("Standard:    ").append(sdf.format(date)).append("\n");
            sb.append("ISO 8601:    ").append(sdf2.format(date)).append("\n");
            sb.append("HTTP Date:   ").append(sdf3.format(date)).append("\n\n");
            sb.append("⏱️ Timestamps:\n\n");
            sb.append("Milliseconds: ").append(timestamp).append("\n");
            sb.append("Seconds:      ").append(timestamp / 1000).append("\n\n");
            sb.append("📊 Additional Info:\n\n");
            sb.append("Day of Week:  ").append(new SimpleDateFormat("EEEE").format(date)).append("\n");
            sb.append("Week of Year: ").append(new SimpleDateFormat("w").format(date));

            resultArea.setText(sb.toString());
        } catch (Exception ex) {
            log.error("Timestamp conversion error", ex);
            resultArea.setText("❌ Error: " + ex.getMessage());
        }
    }

    private void convertToTimestamp() {
        String input = dateField.getText().trim();
        if (input.isEmpty()) {
            resultArea.setText("");
            return;
        }

        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            Date date = sdf.parse(input);
            long timestamp = date.getTime();

            StringBuilder sb = new StringBuilder();
            sb.append("📅 Input Date:\n\n");
            sb.append(input).append("\n\n");
            sb.append("⏱️ Timestamps:\n\n");
            sb.append("Milliseconds: ").append(timestamp).append("\n");
            sb.append("Seconds:      ").append(timestamp / 1000);

            resultArea.setText(sb.toString());
            timestampField.setText(String.valueOf(timestamp));
        } catch (ParseException ex) {
            log.error("Date parsing error", ex);
            resultArea.setText("❌ Error: Invalid date format\n\nExpected format: yyyy-MM-dd HH:mm:ss\nExample: 2023-12-25 15:30:00");
        }
    }

    private void copyToClipboard() {
        String text = resultArea.getText();
        if (!text.isEmpty()) {
            Toolkit.getDefaultToolkit().getSystemClipboard()
                    .setContents(new StringSelection(text), null);
            JOptionPane.showMessageDialog(this,
                    I18nUtil.getMessage(MessageKeys.BUTTON_COPY) + " " + I18nUtil.getMessage(MessageKeys.SUCCESS),
                    I18nUtil.getMessage(MessageKeys.TIP),
                    JOptionPane.INFORMATION_MESSAGE);
        }
    }
}
