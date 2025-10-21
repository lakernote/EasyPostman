package com.laker.postman.panel.toolbox;

import com.laker.postman.util.I18nUtil;
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

    private JTextField timestampField;
    private JTextField dateField;
    private JTextArea resultArea;
    private JComboBox<String> unitCombo;
    private Timer timer;

    public TimestampPanel() {
        initUI();
        startAutoRefresh();
    }

    private void initUI() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // 顶部面板
        JPanel topPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // 当前时间戳
        gbc.gridx = 0; gbc.gridy = 0;
        topPanel.add(new JLabel(I18nUtil.getMessage("toolbox.timestamp.current") + ":"), gbc);

        gbc.gridx = 1; gbc.weightx = 1.0;
        JLabel currentLabel = new JLabel(String.valueOf(System.currentTimeMillis()));
        currentLabel.setFont(new Font(Font.MONOSPACED, Font.BOLD, 12));
        topPanel.add(currentLabel, gbc);

        JButton refreshBtn = new JButton(I18nUtil.getMessage("button.refresh"));
        refreshBtn.addActionListener(e -> currentLabel.setText(String.valueOf(System.currentTimeMillis())));
        gbc.gridx = 2; gbc.weightx = 0;
        topPanel.add(refreshBtn, gbc);

        // 分隔线
        gbc.gridx = 0; gbc.gridy = 1; gbc.gridwidth = 3;
        topPanel.add(new JSeparator(), gbc);
        gbc.gridwidth = 1;

        // 时间戳转日期部分
        gbc.gridx = 0; gbc.gridy = 2;
        topPanel.add(new JLabel(I18nUtil.getMessage("toolbox.timestamp.input") + ":"), gbc);

        gbc.gridx = 1; gbc.weightx = 1.0;
        timestampField = new JTextField();
        timestampField.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        topPanel.add(timestampField, gbc);

        gbc.gridx = 2; gbc.weightx = 0;
        unitCombo = new JComboBox<>(new String[]{
            I18nUtil.getMessage("toolbox.timestamp.milliseconds"),
            I18nUtil.getMessage("toolbox.timestamp.seconds")
        });
        topPanel.add(unitCombo, gbc);

        gbc.gridx = 1; gbc.gridy = 3;
        JButton convertToDateBtn = new JButton(I18nUtil.getMessage("toolbox.timestamp.toDate"));
        convertToDateBtn.addActionListener(e -> convertToDate());
        topPanel.add(convertToDateBtn, gbc);

        // 分隔线
        gbc.gridx = 0; gbc.gridy = 4; gbc.gridwidth = 3;
        topPanel.add(new JSeparator(), gbc);
        gbc.gridwidth = 1;

        // 日期转时间戳部分
        gbc.gridx = 0; gbc.gridy = 5;
        topPanel.add(new JLabel("Date to Timestamp:"), gbc);

        gbc.gridx = 1; gbc.weightx = 1.0;
        dateField = new JTextField();
        dateField.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        dateField.setToolTipText("Format: yyyy-MM-dd HH:mm:ss");
        topPanel.add(dateField, gbc);

        gbc.gridx = 2; gbc.weightx = 0;
        JButton convertToTimestampBtn = new JButton("To Timestamp");
        convertToTimestampBtn.addActionListener(e -> convertToTimestamp());
        topPanel.add(convertToTimestampBtn, gbc);

        add(topPanel, BorderLayout.NORTH);

        // 结果区域
        JPanel resultPanel = new JPanel(new BorderLayout(5, 5));

        JPanel resultHeaderPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        resultHeaderPanel.add(new JLabel(I18nUtil.getMessage("toolbox.timestamp.output")));
        JButton copyBtn = new JButton(I18nUtil.getMessage("button.copy"));
        copyBtn.addActionListener(e -> copyToClipboard());
        resultHeaderPanel.add(copyBtn);

        resultPanel.add(resultHeaderPanel, BorderLayout.NORTH);
        resultArea = new JTextArea();
        resultArea.setEditable(false);
        resultArea.setRows(10);
        resultArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        resultPanel.add(new JScrollPane(resultArea), BorderLayout.CENTER);

        add(resultPanel, BorderLayout.CENTER);

        // 自动刷新当前时间戳
        timer = new Timer(1000, e -> currentLabel.setText(String.valueOf(System.currentTimeMillis())));
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
                I18nUtil.getMessage("button.copy") + " " + I18nUtil.getMessage("success"),
                I18nUtil.getMessage("tip"),
                JOptionPane.INFORMATION_MESSAGE);
        }
    }
}
