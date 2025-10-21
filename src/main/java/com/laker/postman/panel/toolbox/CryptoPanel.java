package com.laker.postman.panel.toolbox;

import com.formdev.flatlaf.extras.components.FlatTextField;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;
import lombok.extern.slf4j.Slf4j;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * 加解密工具面板 - 专注于双向加密解密
 */
@Slf4j
public class CryptoPanel extends JPanel {

    private JTextArea inputArea;
    private JTextArea outputArea;
    private JComboBox<String> algorithmCombo;
    private FlatTextField keyField;

    public CryptoPanel() {
        initUI();
    }

    private void initUI() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // 顶部工具栏
        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));

        // 第一行：算法选择
        JPanel row1 = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        row1.add(new JLabel(I18nUtil.getMessage(MessageKeys.TOOLBOX_CRYPTO_ALGORITHM) + ":"));
        algorithmCombo = new JComboBox<>(new String[]{
                "AES-128", "AES-256", "DES"
        });
        row1.add(algorithmCombo);
        topPanel.add(row1);

        // 第二行：密钥输入
        JPanel row2 = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        row2.add(new JLabel(I18nUtil.getMessage(MessageKeys.TOOLBOX_CRYPTO_KEY) + ":"));
        keyField = new FlatTextField();
        keyField.setColumns(30);
        keyField.setPlaceholderText("AES-128: 16 characters, AES-256: 32 characters, DES: 8 characters");
        keyField.setBackground(Color.WHITE);
        row2.add(keyField);
        topPanel.add(row2);

        // 第三行：操作按钮
        JPanel row3 = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        JButton encryptBtn = new JButton(I18nUtil.getMessage(MessageKeys.TOOLBOX_CRYPTO_ENCRYPT));
        JButton decryptBtn = new JButton(I18nUtil.getMessage(MessageKeys.TOOLBOX_CRYPTO_DECRYPT));
        JButton copyBtn = new JButton(I18nUtil.getMessage(MessageKeys.BUTTON_COPY));
        JButton clearBtn = new JButton(I18nUtil.getMessage(MessageKeys.BUTTON_CLEAR));

        row3.add(encryptBtn);
        row3.add(decryptBtn);
        row3.add(copyBtn);
        row3.add(clearBtn);
        topPanel.add(row3);

        add(topPanel, BorderLayout.NORTH);

        // 中间分割面板
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);

        // 输入区域
        JPanel inputPanel = new JPanel(new BorderLayout(5, 5));
        inputPanel.add(new JLabel(I18nUtil.getMessage(MessageKeys.TOOLBOX_CRYPTO_INPUT)), BorderLayout.NORTH);
        inputArea = new JTextArea();
        inputArea.setLineWrap(true);
        inputArea.setWrapStyleWord(true);
        inputArea.setBackground(Color.WHITE);
        inputPanel.add(new JScrollPane(inputArea), BorderLayout.CENTER);

        // 输出区域
        JPanel outputPanel = new JPanel(new BorderLayout(5, 5));
        outputPanel.add(new JLabel(I18nUtil.getMessage(MessageKeys.TOOLBOX_CRYPTO_OUTPUT)), BorderLayout.NORTH);
        outputArea = new JTextArea();
        outputArea.setLineWrap(true);
        outputArea.setWrapStyleWord(true);
        outputArea.setEditable(false);
        outputArea.setBackground(Color.WHITE);
        outputPanel.add(new JScrollPane(outputArea), BorderLayout.CENTER);

        splitPane.setTopComponent(inputPanel);
        splitPane.setBottomComponent(outputPanel);
        splitPane.setDividerLocation(200);

        add(splitPane, BorderLayout.CENTER);

        // 按钮事件
        encryptBtn.addActionListener(e -> encrypt());
        decryptBtn.addActionListener(e -> decrypt());
        copyBtn.addActionListener(e -> copyToClipboard());
        clearBtn.addActionListener(e -> {
            inputArea.setText("");
            outputArea.setText("");
        });
    }

    private void encrypt() {
        String input = inputArea.getText();
        String key = keyField.getText();

        if (input.isEmpty()) {
            outputArea.setText("");
            return;
        }

        if (key.isEmpty()) {
            outputArea.setText("❌ Error: Key is required for encryption");
            return;
        }

        try {
            String algorithm = switch (algorithmCombo.getSelectedIndex()) {
                case 0 -> "AES"; // AES-128
                case 1 -> "AES"; // AES-256
                case 2 -> "DES";
                default -> "AES";
            };

            int keyLength = switch (algorithmCombo.getSelectedIndex()) {
                case 0 -> 16; // AES-128
                case 1 -> 32; // AES-256
                case 2 -> 8;  // DES
                default -> 16;
            };

            if (key.length() != keyLength) {
                outputArea.setText("❌ Error: Key must be exactly " + keyLength + " characters for " +
                        algorithmCombo.getSelectedItem());
                return;
            }

            SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), algorithm);
            Cipher cipher = Cipher.getInstance(algorithm);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);

            byte[] encrypted = cipher.doFinal(input.getBytes(StandardCharsets.UTF_8));
            String result = Base64.getEncoder().encodeToString(encrypted);

            outputArea.setText("✅ Encrypted (Base64):\n\n" + result);
        } catch (Exception ex) {
            log.error("Encryption error", ex);
            outputArea.setText("❌ Error: " + ex.getMessage());
        }
    }

    private void decrypt() {
        String input = inputArea.getText();
        String key = keyField.getText();

        if (input.isEmpty()) {
            outputArea.setText("");
            return;
        }

        if (key.isEmpty()) {
            outputArea.setText("❌ Error: Key is required for decryption");
            return;
        }

        try {
            String algorithm = switch (algorithmCombo.getSelectedIndex()) {
                case 0 -> "AES"; // AES-128
                case 1 -> "AES"; // AES-256
                case 2 -> "DES";
                default -> "AES";
            };

            SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), algorithm);
            Cipher cipher = Cipher.getInstance(algorithm);
            cipher.init(Cipher.DECRYPT_MODE, secretKey);

            byte[] decrypted = cipher.doFinal(Base64.getDecoder().decode(input));
            String result = new String(decrypted, StandardCharsets.UTF_8);

            outputArea.setText("✅ Decrypted:\n\n" + result);
        } catch (Exception ex) {
            log.error("Decryption error", ex);
            outputArea.setText("❌ Error: " + ex.getMessage());
        }
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
