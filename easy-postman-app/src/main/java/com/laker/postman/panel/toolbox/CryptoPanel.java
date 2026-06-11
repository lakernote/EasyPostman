package com.laker.postman.panel.toolbox;

import com.formdev.flatlaf.extras.components.FlatTextField;
import com.laker.postman.common.component.ToolWindowSurfaceStyle;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;
import lombok.extern.slf4j.Slf4j;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.KeyEvent;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.text.MessageFormat;
import java.util.Base64;

/**
 * 加解密工具面板 - 专注于双向加密解密
 */
@Slf4j
public class CryptoPanel extends JPanel {

    private JTextArea inputArea;
    private JTextArea outputArea;
    private JComboBox<String> algorithmCombo;
    private JComboBox<String> modeCombo;
    private FlatTextField keyField;
    private FlatTextField ivField;
    private JPanel ivPanel;
    private JCheckBox base64UrlCheckBox;

    public CryptoPanel() {
        initUI();
        setupKeyBindings();
    }

    private void initUI() {
        ToolboxWorkbench.applyRoot(this);

        // 顶部工具栏
        add(createToolbar(), BorderLayout.NORTH);

        // 中间分割面板
        add(createMainPanel(), BorderLayout.CENTER);
    }

    private JPanel createToolbar() {
        // 配置面板
        JPanel configPanel = new JPanel();
        configPanel.setOpaque(false);
        configPanel.setLayout(new BoxLayout(configPanel, BoxLayout.Y_AXIS));
        configPanel.setBorder(BorderFactory.createEmptyBorder());

        // 第一行：算法和模式选择
        JPanel row1 = ToolboxWorkbench.optionsRow();
        row1.add(new JLabel(I18nUtil.getMessage(MessageKeys.TOOLBOX_CRYPTO_ALGORITHM) + ":"));
        algorithmCombo = new JComboBox<>(new String[]{"AES-128", "AES-256", "DES"});
        algorithmCombo.addActionListener(e -> updatePlaceholders());
        row1.add(algorithmCombo);

        row1.add(Box.createHorizontalStrut(15));
        row1.add(new JLabel(I18nUtil.getMessage(MessageKeys.TOOLBOX_CRYPTO_MODE) + ":"));
        modeCombo = new JComboBox<>(new String[]{"ECB", "CBC"});
        modeCombo.addActionListener(e -> updateIvVisibility());
        row1.add(modeCombo);

        configPanel.add(row1);

        // 第二行：密钥输入和生成
        JPanel row2 = ToolboxWorkbench.optionsRow();
        row2.add(new JLabel(I18nUtil.getMessage(MessageKeys.TOOLBOX_CRYPTO_KEY) + ":"));
        keyField = new FlatTextField();
        keyField.setColumns(35);
        ToolWindowSurfaceStyle.applyTextComponentInput(keyField);
        row2.add(keyField);

        JButton generateKeyBtn = new JButton(I18nUtil.getMessage(MessageKeys.TOOLBOX_CRYPTO_GENERATE_KEY));
        generateKeyBtn.setToolTipText(I18nUtil.getMessage(MessageKeys.TOOLBOX_CRYPTO_GENERATE_KEY));
        generateKeyBtn.addActionListener(e -> generateKey());
        row2.add(generateKeyBtn);

        configPanel.add(row2);

        // 第三行：IV输入和生成（仅CBC模式显示）
        ivPanel = ToolboxWorkbench.optionsRow();
        ivPanel.add(new JLabel(I18nUtil.getMessage(MessageKeys.TOOLBOX_CRYPTO_IV) + ":"));
        ivField = new FlatTextField();
        ivField.setColumns(35);
        ivField.setPlaceholderText(I18nUtil.getMessage(MessageKeys.TOOLBOX_CRYPTO_IV_PLACEHOLDER));
        ToolWindowSurfaceStyle.applyTextComponentInput(ivField);
        ivPanel.add(ivField);

        JButton generateIvBtn = new JButton(I18nUtil.getMessage(MessageKeys.TOOLBOX_CRYPTO_GENERATE_IV));
        generateIvBtn.setToolTipText(I18nUtil.getMessage(MessageKeys.TOOLBOX_CRYPTO_GENERATE_IV));
        generateIvBtn.addActionListener(e -> generateIv());
        ivPanel.add(generateIvBtn);

        configPanel.add(ivPanel);

        base64UrlCheckBox = new JCheckBox(I18nUtil.getMessage(MessageKeys.TOOLBOX_CRYPTO_BASE64URL), false);
        base64UrlCheckBox.setToolTipText(I18nUtil.getMessage(MessageKeys.TOOLBOX_CRYPTO_BASE64URL_TOOLTIP));

        JButton encryptBtn = new JButton(I18nUtil.getMessage(MessageKeys.TOOLBOX_CRYPTO_ENCRYPT));
        encryptBtn.setToolTipText("Ctrl+E / Cmd+E");
        encryptBtn.addActionListener(e -> encrypt());

        JButton decryptBtn = new JButton(I18nUtil.getMessage(MessageKeys.TOOLBOX_CRYPTO_DECRYPT));
        decryptBtn.setToolTipText("Ctrl+D / Cmd+D");
        decryptBtn.addActionListener(e -> decrypt());

        JButton copyBtn = new JButton(I18nUtil.getMessage(MessageKeys.BUTTON_COPY));
        copyBtn.setToolTipText("Ctrl+C / Cmd+C");
        copyBtn.addActionListener(e -> copyToClipboard());

        JButton clearBtn = new JButton(I18nUtil.getMessage(MessageKeys.BUTTON_CLEAR));
        clearBtn.setToolTipText("Ctrl+L / Cmd+L");
        clearBtn.addActionListener(e -> clearAll());

        JPanel optionsPanel = ToolboxWorkbench.leftToolbar(
                base64UrlCheckBox,
                ToolboxWorkbench.verticalSeparator(),
                encryptBtn,
                decryptBtn,
                copyBtn,
                clearBtn);

        // 初始化状态
        updatePlaceholders();
        updateIvVisibility();

        return ToolboxWorkbench.stackedTop(
                configPanel,
                ToolboxWorkbench.toolbar(optionsPanel, null)
        );
    }

    private JSplitPane createMainPanel() {
        // 输入区域
        inputArea = new JTextArea();
        inputArea.setLineWrap(true);
        inputArea.setWrapStyleWord(true);
        ToolWindowSurfaceStyle.applyTextComponentInput(inputArea);
        JScrollPane inputScrollPane = new JScrollPane(inputArea);
        ToolWindowSurfaceStyle.applyFramedScrollPaneCard(inputScrollPane);
        JPanel inputPanel = ToolboxWorkbench.editorSection(
                I18nUtil.getMessage(MessageKeys.TOOLBOX_CRYPTO_INPUT),
                inputScrollPane
        );

        // 输出区域
        outputArea = new JTextArea();
        outputArea.setLineWrap(true);
        outputArea.setWrapStyleWord(true);
        outputArea.setEditable(false);
        ToolWindowSurfaceStyle.applyTextComponentCard(outputArea);
        JScrollPane outputScrollPane = new JScrollPane(outputArea);
        ToolWindowSurfaceStyle.applyFramedScrollPaneCard(outputScrollPane);
        JPanel outputPanel = ToolboxWorkbench.editorSection(
                I18nUtil.getMessage(MessageKeys.TOOLBOX_CRYPTO_OUTPUT),
                outputScrollPane
        );

        return ToolboxWorkbench.editorSplit(inputPanel, outputPanel, 260);
    }

    private void setupKeyBindings() {
        // Ctrl+E / Cmd+E - 加密
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(
                KeyStroke.getKeyStroke(KeyEvent.VK_E, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()),
                "encrypt"
        );
        getActionMap().put("encrypt", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                encrypt();
            }
        });

        // Ctrl+D / Cmd+D - 解密
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(
                KeyStroke.getKeyStroke(KeyEvent.VK_D, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()),
                "decrypt"
        );
        getActionMap().put("decrypt", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                decrypt();
            }
        });

        // Ctrl+L / Cmd+L - 清空
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(
                KeyStroke.getKeyStroke(KeyEvent.VK_L, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()),
                "clear"
        );
        getActionMap().put("clear", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                clearAll();
            }
        });
    }

    private void updatePlaceholders() {
        String algorithm = (String) algorithmCombo.getSelectedItem();
        String placeholder;

        if ("AES-128".equals(algorithm)) {
            placeholder = I18nUtil.getMessage(MessageKeys.TOOLBOX_CRYPTO_KEY_PLACEHOLDER_AES128);
        } else if ("AES-256".equals(algorithm)) {
            placeholder = I18nUtil.getMessage(MessageKeys.TOOLBOX_CRYPTO_KEY_PLACEHOLDER_AES256);
        } else {
            placeholder = I18nUtil.getMessage(MessageKeys.TOOLBOX_CRYPTO_KEY_PLACEHOLDER_DES);
        }

        keyField.setPlaceholderText(placeholder);
    }

    private void updateIvVisibility() {
        boolean cbcMode = "CBC".equals(modeCombo.getSelectedItem());
        ivPanel.setVisible(cbcMode);
        revalidate();
        repaint();
    }

    private void generateKey() {
        int keyLength = getKeyLength();
        String key = generateRandomString(keyLength);
        keyField.setText(key);
    }

    private void generateIv() {
        String algorithm = (String) algorithmCombo.getSelectedItem();
        int ivLength = (algorithm != null && algorithm.startsWith("AES")) ? 16 : 8;
        String iv = generateRandomString(ivLength);
        ivField.setText(iv);
    }

    private String generateRandomString(int length) {
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder(length);
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }

        return sb.toString();
    }

    private int getKeyLength() {
        return switch (algorithmCombo.getSelectedIndex()) {
            case 0 -> 16; // AES-128
            case 1 -> 32; // AES-256
            case 2 -> 8;  // DES
            default -> 16;
        };
    }

    private void encrypt() {
        String input = inputArea.getText();
        String key = keyField.getText();

        if (input.isEmpty()) {
            outputArea.setText(I18nUtil.getMessage(MessageKeys.TOOLBOX_CRYPTO_ERROR_INPUT_EMPTY));
            return;
        }

        if (key.isEmpty()) {
            outputArea.setText("❌ " + I18nUtil.getMessage(MessageKeys.TOOLBOX_CRYPTO_ERROR_KEY_REQUIRED));
            return;
        }

        try {
            String algorithm = getAlgorithmName();
            int keyLength = getKeyLength();

            if (key.length() != keyLength) {
                String error = MessageFormat.format(
                        I18nUtil.getMessage(MessageKeys.TOOLBOX_CRYPTO_ERROR_KEY_LENGTH),
                        keyLength,
                        algorithmCombo.getSelectedItem()
                );
                outputArea.setText("❌ " + error);
                return;
            }

            String mode = (String) modeCombo.getSelectedItem();
            String transformation = algorithm + "/" + mode + "/PKCS5Padding";

            SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), algorithm);
            Cipher cipher = Cipher.getInstance(transformation);

            if ("CBC".equals(mode)) {
                String iv = ivField.getText();
                int ivLength = algorithm.equals("AES") ? 16 : 8;

                if (iv.isEmpty()) {
                    String error = MessageFormat.format(
                            I18nUtil.getMessage(MessageKeys.TOOLBOX_CRYPTO_ERROR_IV_REQUIRED),
                            mode
                    );
                    outputArea.setText("❌ " + error);
                    return;
                }

                if (iv.length() != ivLength) {
                    String error = MessageFormat.format(
                            I18nUtil.getMessage(MessageKeys.TOOLBOX_CRYPTO_ERROR_IV_LENGTH),
                            ivLength
                    );
                    outputArea.setText("❌ " + error);
                    return;
                }

                IvParameterSpec ivSpec = new IvParameterSpec(iv.getBytes(StandardCharsets.UTF_8));
                cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivSpec);
            } else {
                cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            }

            byte[] encrypted = cipher.doFinal(input.getBytes(StandardCharsets.UTF_8));
            String result = encodeBase64(encrypted);

            outputArea.setText(I18nUtil.getMessage(MessageKeys.TOOLBOX_CRYPTO_SUCCESS_ENCRYPTED) + "\n\n" + result);
            outputArea.setCaretPosition(0);
        } catch (Exception ex) {
            log.error("Encryption error", ex);
            outputArea.setText("❌ Error: " + ex.getMessage());
        }
    }

    private void decrypt() {
        String input = inputArea.getText();
        String key = keyField.getText();

        if (input.isEmpty()) {
            outputArea.setText(I18nUtil.getMessage(MessageKeys.TOOLBOX_CRYPTO_ERROR_INPUT_EMPTY));
            return;
        }

        if (key.isEmpty()) {
            outputArea.setText("❌ " + I18nUtil.getMessage(MessageKeys.TOOLBOX_CRYPTO_ERROR_KEY_REQUIRED));
            return;
        }

        try {
            String algorithm = getAlgorithmName();
            String mode = (String) modeCombo.getSelectedItem();
            String transformation = algorithm + "/" + mode + "/PKCS5Padding";

            SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), algorithm);
            Cipher cipher = Cipher.getInstance(transformation);

            if ("CBC".equals(mode)) {
                String iv = ivField.getText();
                int ivLength = algorithm.equals("AES") ? 16 : 8;

                if (iv.isEmpty()) {
                    String error = MessageFormat.format(
                            I18nUtil.getMessage(MessageKeys.TOOLBOX_CRYPTO_ERROR_IV_REQUIRED),
                            mode
                    );
                    outputArea.setText("❌ " + error);
                    return;
                }

                if (iv.length() != ivLength) {
                    String error = MessageFormat.format(
                            I18nUtil.getMessage(MessageKeys.TOOLBOX_CRYPTO_ERROR_IV_LENGTH),
                            ivLength
                    );
                    outputArea.setText("❌ " + error);
                    return;
                }

                IvParameterSpec ivSpec = new IvParameterSpec(iv.getBytes(StandardCharsets.UTF_8));
                cipher.init(Cipher.DECRYPT_MODE, secretKey, ivSpec);
            } else {
                cipher.init(Cipher.DECRYPT_MODE, secretKey);
            }

            byte[] decrypted = cipher.doFinal(decodeBase64(input.trim()));
            String result = new String(decrypted, StandardCharsets.UTF_8);

            outputArea.setText(I18nUtil.getMessage(MessageKeys.TOOLBOX_CRYPTO_SUCCESS_DECRYPTED) + "\n\n" + result);
            outputArea.setCaretPosition(0);
        } catch (Exception ex) {
            log.error("Decryption error", ex);
            outputArea.setText("❌ Error: " + ex.getMessage());
        }
    }

    private String getAlgorithmName() {
        return switch (algorithmCombo.getSelectedIndex()) {
            case 0, 1 -> "AES"; // AES-128 or AES-256
            case 2 -> "DES";
            default -> "AES";
        };
    }

    private String encodeBase64(byte[] data) {
        if (base64UrlCheckBox.isSelected()) {
            return Base64.getUrlEncoder().withoutPadding().encodeToString(data);
        } else {
            return Base64.getEncoder().encodeToString(data);
        }
    }

    private byte[] decodeBase64(String data) {
        if (base64UrlCheckBox.isSelected()) {
            return Base64.getUrlDecoder().decode(data);
        } else {
            return Base64.getDecoder().decode(data);
        }
    }

    private void copyToClipboard() {
        String text = outputArea.getText();
        if (!text.isEmpty()) {
            // 移除前面的成功标识行
            String[] lines = text.split("\n", 3);
            String contentToCopy = lines.length > 2 ? lines[2] : text;

            Toolkit.getDefaultToolkit().getSystemClipboard()
                    .setContents(new StringSelection(contentToCopy.trim()), null);
        }
    }

    private void clearAll() {
        inputArea.setText("");
        outputArea.setText("");
    }
}
