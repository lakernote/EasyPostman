package com.laker.postman.panel.toolbox;

import cn.hutool.json.JSONUtil;
import com.laker.postman.util.I18nUtil;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;

/**
 * JSON工具面板
 */
@Slf4j
public class JsonToolPanel extends JPanel {

    private JTextArea inputArea;
    private JTextArea outputArea;

    public JsonToolPanel() {
        initUI();
    }

    private void initUI() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // 顶部工具栏
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));

        JButton formatBtn = new JButton(I18nUtil.getMessage("toolbox.json.format"));
        JButton compressBtn = new JButton(I18nUtil.getMessage("toolbox.json.compress"));
        JButton validateBtn = new JButton(I18nUtil.getMessage("toolbox.json.validate"));
        JButton copyBtn = new JButton(I18nUtil.getMessage("button.copy"));
        JButton clearBtn = new JButton(I18nUtil.getMessage("button.clear"));

        topPanel.add(formatBtn);
        topPanel.add(compressBtn);
        topPanel.add(validateBtn);
        topPanel.add(new JSeparator(SwingConstants.VERTICAL));
        topPanel.add(copyBtn);
        topPanel.add(clearBtn);

        add(topPanel, BorderLayout.NORTH);

        // 中间分割面板
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);

        // 输入区域
        JPanel inputPanel = new JPanel(new BorderLayout(5, 5));
        inputPanel.add(new JLabel(I18nUtil.getMessage("toolbox.json.input")), BorderLayout.NORTH);
        inputArea = new JTextArea();
        inputArea.setLineWrap(true);
        inputArea.setWrapStyleWord(true);
        inputArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        inputPanel.add(new JScrollPane(inputArea), BorderLayout.CENTER);

        // 输出区域
        JPanel outputPanel = new JPanel(new BorderLayout(5, 5));
        outputPanel.add(new JLabel(I18nUtil.getMessage("toolbox.json.output")), BorderLayout.NORTH);
        outputArea = new JTextArea();
        outputArea.setLineWrap(true);
        outputArea.setWrapStyleWord(true);
        outputArea.setEditable(false);
        outputArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        outputPanel.add(new JScrollPane(outputArea), BorderLayout.CENTER);

        splitPane.setTopComponent(inputPanel);
        splitPane.setBottomComponent(outputPanel);
        splitPane.setDividerLocation(200);

        add(splitPane, BorderLayout.CENTER);

        // 按钮事件
        formatBtn.addActionListener(e -> formatJson());
        compressBtn.addActionListener(e -> compressJson());
        validateBtn.addActionListener(e -> validateJson());
        copyBtn.addActionListener(e -> copyToClipboard());
        clearBtn.addActionListener(e -> {
            inputArea.setText("");
            outputArea.setText("");
        });
    }

    private void formatJson() {
        String input = inputArea.getText().trim();
        if (input.isEmpty()) {
            outputArea.setText("");
            return;
        }

        try {
            String formatted = JSONUtil.toJsonPrettyStr(JSONUtil.parse(input));
            outputArea.setText(formatted);
        } catch (Exception ex) {
            log.error("JSON format error", ex);
            outputArea.setText("❌ " + I18nUtil.getMessage("toolbox.json.error") + ": " + ex.getMessage());
        }
    }

    private void compressJson() {
        String input = inputArea.getText().trim();
        if (input.isEmpty()) {
            outputArea.setText("");
            return;
        }

        try {
            String compressed = JSONUtil.toJsonStr(JSONUtil.parse(input));
            outputArea.setText(compressed);
        } catch (Exception ex) {
            log.error("JSON compress error", ex);
            outputArea.setText("❌ " + I18nUtil.getMessage("toolbox.json.error") + ": " + ex.getMessage());
        }
    }

    private void validateJson() {
        String input = inputArea.getText().trim();
        if (input.isEmpty()) {
            outputArea.setText("⚠️ Input is empty");
            return;
        }

        try {
            JSONUtil.parse(input);
            outputArea.setText("✅ Valid JSON\n\n" +
                "Characters: " + input.length() + "\n" +
                "Lines: " + (input.split("\n").length));
        } catch (Exception ex) {
            log.error("JSON validate error", ex);
            outputArea.setText("❌ " + I18nUtil.getMessage("toolbox.json.error") + ":\n\n" + ex.getMessage());
        }
    }

    private void copyToClipboard() {
        String text = outputArea.getText();
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
