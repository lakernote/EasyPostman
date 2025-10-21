package com.laker.postman.panel.toolbox;

import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Hash计算工具面板
 */
@Slf4j
public class HashPanel extends JPanel {

    private JTextArea inputArea;
    private JTextArea outputArea;
    private JCheckBox allHashCheckBox;

    public HashPanel() {
        initUI();
    }

    private void initUI() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // 顶部工具栏
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));

        JButton md5Btn = new JButton("MD5");
        JButton sha1Btn = new JButton("SHA-1");
        JButton sha256Btn = new JButton("SHA-256");
        JButton sha512Btn = new JButton("SHA-512");
        JButton copyBtn = new JButton(I18nUtil.getMessage(MessageKeys.BUTTON_COPY));
        JButton clearBtn = new JButton(I18nUtil.getMessage(MessageKeys.BUTTON_CLEAR));

        allHashCheckBox = new JCheckBox("All", false);
        allHashCheckBox.setToolTipText("Calculate all hash algorithms at once");

        topPanel.add(allHashCheckBox);
        topPanel.add(new JSeparator(SwingConstants.VERTICAL));
        topPanel.add(md5Btn);
        topPanel.add(sha1Btn);
        topPanel.add(sha256Btn);
        topPanel.add(sha512Btn);
        topPanel.add(new JSeparator(SwingConstants.VERTICAL));
        topPanel.add(copyBtn);
        topPanel.add(clearBtn);

        add(topPanel, BorderLayout.NORTH);

        // 中间分割面板
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);

        // 输入区域
        JPanel inputPanel = new JPanel(new BorderLayout(5, 5));
        inputPanel.add(new JLabel(I18nUtil.getMessage(MessageKeys.TOOLBOX_HASH_INPUT)), BorderLayout.NORTH);
        inputArea = new JTextArea();
        inputArea.setLineWrap(true);
        inputArea.setWrapStyleWord(true);
        inputArea.setBackground(Color.WHITE);
        inputPanel.add(new JScrollPane(inputArea), BorderLayout.CENTER);

        // 输出区域
        JPanel outputPanel = new JPanel(new BorderLayout(5, 5));
        outputPanel.add(new JLabel(I18nUtil.getMessage(MessageKeys.TOOLBOX_HASH_OUTPUT)), BorderLayout.NORTH);
        outputArea = new JTextArea();
        outputArea.setLineWrap(true);
        outputArea.setEditable(false);
        outputPanel.add(new JScrollPane(outputArea), BorderLayout.CENTER);

        splitPane.setTopComponent(inputPanel);
        splitPane.setBottomComponent(outputPanel);
        splitPane.setDividerLocation(200);

        add(splitPane, BorderLayout.CENTER);

        // 按钮事件
        md5Btn.addActionListener(e -> {
            if (allHashCheckBox.isSelected()) {
                calculateAllHashes();
            } else {
                calculateHash("MD5");
            }
        });
        sha1Btn.addActionListener(e -> {
            if (allHashCheckBox.isSelected()) {
                calculateAllHashes();
            } else {
                calculateHash("SHA-1");
            }
        });
        sha256Btn.addActionListener(e -> {
            if (allHashCheckBox.isSelected()) {
                calculateAllHashes();
            } else {
                calculateHash("SHA-256");
            }
        });
        sha512Btn.addActionListener(e -> {
            if (allHashCheckBox.isSelected()) {
                calculateAllHashes();
            } else {
                calculateHash("SHA-512");
            }
        });
        copyBtn.addActionListener(e -> copyToClipboard());
        clearBtn.addActionListener(e -> {
            inputArea.setText("");
            outputArea.setText("");
        });
    }

    private void calculateHash(String algorithm) {
        String input = inputArea.getText();
        if (input.isEmpty()) {
            outputArea.setText("");
            return;
        }

        try {
            MessageDigest md = MessageDigest.getInstance(algorithm);
            byte[] hash = md.digest(input.getBytes(StandardCharsets.UTF_8));

            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }

            outputArea.setText(algorithm + ": " + hexString.toString());
        } catch (Exception ex) {
            log.error("Hash calculation error", ex);
            outputArea.setText("Error: " + ex.getMessage());
        }
    }

    private void calculateAllHashes() {
        String input = inputArea.getText();
        if (input.isEmpty()) {
            outputArea.setText("");
            return;
        }

        StringBuilder result = new StringBuilder();
        String[] algorithms = {"MD5", "SHA-1", "SHA-256", "SHA-512"};

        for (String algorithm : algorithms) {
            try {
                MessageDigest md = MessageDigest.getInstance(algorithm);
                byte[] hash = md.digest(input.getBytes(StandardCharsets.UTF_8));

                StringBuilder hexString = new StringBuilder();
                for (byte b : hash) {
                    String hex = Integer.toHexString(0xff & b);
                    if (hex.length() == 1) hexString.append('0');
                    hexString.append(hex);
                }

                result.append(algorithm).append(": ").append(hexString.toString()).append("\n\n");
            } catch (Exception ex) {
                log.error("Hash calculation error for " + algorithm, ex);
                result.append(algorithm).append(": Error - ").append(ex.getMessage()).append("\n\n");
            }
        }

        outputArea.setText(result.toString().trim());
    }

    private void copyToClipboard() {
        String text = outputArea.getText();
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
