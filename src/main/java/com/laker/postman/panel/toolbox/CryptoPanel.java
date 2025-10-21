package com.laker.postman.panel.toolbox;

import com.laker.postman.util.I18nUtil;
import lombok.extern.slf4j.Slf4j;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

/**
 * 加解密工具面板
 */
@Slf4j
public class CryptoPanel extends JPanel {

    private JTextArea inputArea;
    private JTextArea outputArea;
    private JComboBox<String> algorithmCombo;
    private JTextField keyField;
    private JCheckBox encryptModeCheckBox;

    public CryptoPanel() {
        initUI();
    }

    private void initUI() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // 顶部工具栏
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        topPanel.add(new JLabel(I18nUtil.getMessage("toolbox.crypto.algorithm") + ":"));

        algorithmCombo = new JComboBox<>(new String[]{
            "MD5", "SHA-1", "SHA-256", "SHA-512", "AES-128"
        });
        topPanel.add(algorithmCombo);

        encryptModeCheckBox = new JCheckBox("Encrypt Mode", false);
        encryptModeCheckBox.setToolTipText("For AES: checked=encrypt, unchecked=decrypt");
        topPanel.add(encryptModeCheckBox);

        topPanel.add(new JLabel("Key:"));
        keyField = new JTextField(16);
        keyField.setToolTipText("Required for AES encryption/decryption");
        topPanel.add(keyField);

        JButton calculateBtn = new JButton(I18nUtil.getMessage("toolbox.hash.calculate"));
        JButton copyBtn = new JButton(I18nUtil.getMessage("button.copy"));
        JButton clearBtn = new JButton(I18nUtil.getMessage("button.clear"));

        topPanel.add(calculateBtn);
        topPanel.add(copyBtn);
        topPanel.add(clearBtn);

        add(topPanel, BorderLayout.NORTH);

        // 中间分割面板
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);

        // 输入区域
        JPanel inputPanel = new JPanel(new BorderLayout(5, 5));
        inputPanel.add(new JLabel(I18nUtil.getMessage("toolbox.crypto.input")), BorderLayout.NORTH);
        inputArea = new JTextArea();
        inputArea.setLineWrap(true);
        inputArea.setWrapStyleWord(true);
        inputArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        inputPanel.add(new JScrollPane(inputArea), BorderLayout.CENTER);

        // 输出区域
        JPanel outputPanel = new JPanel(new BorderLayout(5, 5));
        outputPanel.add(new JLabel(I18nUtil.getMessage("toolbox.crypto.output")), BorderLayout.NORTH);
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
        calculateBtn.addActionListener(e -> calculate());
        copyBtn.addActionListener(e -> copyToClipboard());
        clearBtn.addActionListener(e -> {
            inputArea.setText("");
            outputArea.setText("");
        });

        // 算法选择监听
        algorithmCombo.addActionListener(e -> {
            boolean isAES = algorithmCombo.getSelectedIndex() == 4;
            keyField.setEnabled(isAES);
            encryptModeCheckBox.setEnabled(isAES);
        });

        // 初始化状态
        keyField.setEnabled(false);
        encryptModeCheckBox.setEnabled(false);
    }

    private void calculate() {
        String input = inputArea.getText();
        if (input.isEmpty()) {
            outputArea.setText("");
            return;
        }

        try {
            int selectedIndex = algorithmCombo.getSelectedIndex();

            if (selectedIndex == 4) { // AES
                String key = keyField.getText();
                if (key.isEmpty()) {
                    outputArea.setText("❌ Error: Key is required for AES encryption/decryption");
                    return;
                }

                if (encryptModeCheckBox.isSelected()) {
                    String encrypted = aesEncrypt(input, key);
                    outputArea.setText("✅ AES Encrypted (Base64):\n\n" + encrypted);
                } else {
                    String decrypted = aesDecrypt(input, key);
                    outputArea.setText("✅ AES Decrypted:\n\n" + decrypted);
                }
            } else {
                String algorithm = switch (selectedIndex) {
                    case 0 -> "MD5";
                    case 1 -> "SHA-1";
                    case 2 -> "SHA-256";
                    case 3 -> "SHA-512";
                    default -> "MD5";
                };

                MessageDigest md = MessageDigest.getInstance(algorithm);
                byte[] hash = md.digest(input.getBytes(StandardCharsets.UTF_8));

                StringBuilder hexString = new StringBuilder();
                for (byte b : hash) {
                    String hex = Integer.toHexString(0xff & b);
                    if (hex.length() == 1) hexString.append('0');
                    hexString.append(hex);
                }

                outputArea.setText("✅ " + algorithm + " Hash:\n\n" + hexString.toString());
            }
        } catch (Exception ex) {
            log.error("Crypto calculation error", ex);
            outputArea.setText("❌ Error: " + ex.getMessage());
        }
    }

    private String aesEncrypt(String text, String key) throws Exception {
        SecretKeySpec secretKey = new SecretKeySpec(padKey(key).getBytes(StandardCharsets.UTF_8), "AES");
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);
        byte[] encrypted = cipher.doFinal(text.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(encrypted);
    }

    private String aesDecrypt(String encryptedText, String key) throws Exception {
        SecretKeySpec secretKey = new SecretKeySpec(padKey(key).getBytes(StandardCharsets.UTF_8), "AES");
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, secretKey);
        byte[] decrypted = cipher.doFinal(Base64.getDecoder().decode(encryptedText));
        return new String(decrypted, StandardCharsets.UTF_8);
    }

    private String padKey(String key) {
        // AES-128 requires 16-byte key
        if (key.length() > 16) {
            return key.substring(0, 16);
        } else if (key.length() < 16) {
            return String.format("%-16s", key).replace(' ', '0');
        }
        return key;
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
