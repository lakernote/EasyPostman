package com.laker.postman.panel.toolbox;

import com.laker.postman.common.component.AppToolWindowChrome;
import com.laker.postman.common.component.ToolWindowActionToolbar;
import com.laker.postman.common.component.ToolWindowSurfaceStyle;
import com.laker.postman.common.constants.ModernColors;
import com.laker.postman.util.FontsUtil;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.KeyEvent;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;

/**
 * Hash计算工具面板
 */
@Slf4j
public class HashPanel extends JPanel {

    private JTextArea inputArea;
    private JTextArea outputArea;
    private JCheckBox calculateAllCheckBox;
    private JCheckBox uppercaseCheckBox;
    private JLabel statusLabel;

    private final Map<String, JButton> algorithmButtons = new HashMap<>();
    private String lastSelectedAlgorithm = "MD5";
    private boolean autoCalculate = false;

    public HashPanel() {
        initUI();
        setupKeyBindings();
    }

    private void initUI() {
        setLayout(new BorderLayout(5, 5));
        ToolWindowSurfaceStyle.applyCard(this);
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // 顶部工具栏
        add(createToolbar(), BorderLayout.NORTH);

        // 中间分割面板
        add(createMainPanel(), BorderLayout.CENTER);

        // 底部状态栏
        add(createStatusBar(), BorderLayout.SOUTH);
    }

    private JPanel createToolbar() {
        JPanel toolbarPanel = new JPanel(new BorderLayout(5, 5));
        toolbarPanel.setOpaque(false);

        // 算法按钮面板
        JPanel algorithmPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        algorithmPanel.setOpaque(false);
        algorithmPanel.add(new JLabel(I18nUtil.getMessage(MessageKeys.TOOLBOX_HASH_OUTPUT) + ":"));

        String[] algorithms = {"MD5", "SHA-1", "SHA-256", "SHA-512"};

        for (String algorithm : algorithms) {
            JButton btn = new JButton(algorithm);
            btn.setFocusPainted(false);
            btn.addActionListener(e -> {
                lastSelectedAlgorithm = algorithm;
                if (calculateAllCheckBox.isSelected()) {
                    calculateAllHashes();
                } else {
                    calculateHash(algorithm);
                }
                updateButtonStates(algorithm);
            });
            algorithmButtons.put(algorithm, btn);
            algorithmPanel.add(btn);
        }

        calculateAllCheckBox = new JCheckBox(I18nUtil.getMessage(MessageKeys.TOOLBOX_HASH_CALCULATE_ALL), false);
        calculateAllCheckBox.setToolTipText(I18nUtil.getMessage(MessageKeys.TOOLBOX_HASH_CALCULATE_ALL_TOOLTIP));
        calculateAllCheckBox.addActionListener(e -> {
            if (calculateAllCheckBox.isSelected() && !inputArea.getText().isEmpty()) {
                calculateAllHashes();
            } else if (!inputArea.getText().isEmpty()) {
                calculateHash(lastSelectedAlgorithm);
            }
        });

        uppercaseCheckBox = new JCheckBox(I18nUtil.getMessage(MessageKeys.TOOLBOX_HASH_UPPERCASE), false);
        uppercaseCheckBox.setToolTipText(I18nUtil.getMessage(MessageKeys.TOOLBOX_HASH_UPPERCASE_TOOLTIP));
        uppercaseCheckBox.addActionListener(e -> {
            if (!outputArea.getText().isEmpty()) {
                if (calculateAllCheckBox.isSelected()) {
                    calculateAllHashes();
                } else {
                    calculateHash(lastSelectedAlgorithm);
                }
            }
        });

        JButton calculateBtn = new JButton(I18nUtil.getMessage(MessageKeys.BUTTON_CALCULATE));
        calculateBtn.setToolTipText("Ctrl+Enter / Cmd+Enter");
        calculateBtn.addActionListener(e -> {
            if (calculateAllCheckBox.isSelected()) {
                calculateAllHashes();
            } else {
                calculateHash(lastSelectedAlgorithm);
            }
        });

        JButton copyBtn = new JButton(I18nUtil.getMessage(MessageKeys.BUTTON_COPY));
        copyBtn.setToolTipText("Ctrl+C / Cmd+C");
        copyBtn.addActionListener(e -> copyToClipboard());

        JButton clearBtn = new JButton(I18nUtil.getMessage(MessageKeys.BUTTON_CLEAR));
        clearBtn.setToolTipText("Ctrl+L / Cmd+L");
        clearBtn.addActionListener(e -> clearAll());

        JSeparator actionSeparator = new JSeparator(SwingConstants.VERTICAL);
        actionSeparator.setPreferredSize(new Dimension(1, ToolWindowActionToolbar.ACTION_SIZE));
        JPanel optionsPanel = ToolWindowActionToolbar.inlineLeft(
                calculateAllCheckBox,
                uppercaseCheckBox,
                actionSeparator,
                calculateBtn,
                copyBtn,
                clearBtn);

        toolbarPanel.add(algorithmPanel, BorderLayout.NORTH);
        toolbarPanel.add(optionsPanel, BorderLayout.SOUTH);

        // 默认选中MD5按钮
        updateButtonStates("MD5");

        return toolbarPanel;
    }

    private JSplitPane createMainPanel() {
        // 输入区域
        JPanel inputPanel = new JPanel(new BorderLayout(5, 5));
        inputPanel.setOpaque(false);
        inputPanel.add(new JLabel(I18nUtil.getMessage(MessageKeys.TOOLBOX_HASH_INPUT)), BorderLayout.NORTH);

        inputArea = new JTextArea();
        inputArea.setLineWrap(true);
        inputArea.setWrapStyleWord(true);
        inputArea.setFont(FontsUtil.getDefaultFontWithOffset(Font.PLAIN, +1));
        inputArea.setMargin(new Insets(5, 5, 5, 5));
        ToolWindowSurfaceStyle.applyTextComponentInput(inputArea);

        // 添加实时计算功能
        inputArea.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                handleInputChange();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                handleInputChange();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                handleInputChange();
            }
        });

        JScrollPane inputScroll = new JScrollPane(inputArea);
        ToolWindowSurfaceStyle.applyFramedScrollPaneCard(inputScroll);
        inputPanel.add(inputScroll, BorderLayout.CENTER);

        // 输出区域
        JPanel outputPanel = new JPanel(new BorderLayout(5, 5));
        outputPanel.setOpaque(false);
        outputPanel.add(new JLabel(I18nUtil.getMessage(MessageKeys.TOOLBOX_HASH_OUTPUT)), BorderLayout.NORTH);

        outputArea = new JTextArea();
        outputArea.setLineWrap(true);
        outputArea.setEditable(false);
        outputArea.setFont(FontsUtil.getDefaultFontWithOffset(Font.PLAIN, +1));
        outputArea.setMargin(new Insets(5, 5, 5, 5));
        ToolWindowSurfaceStyle.applyTextComponentCard(outputArea);

        JScrollPane outputScroll = new JScrollPane(outputArea);
        ToolWindowSurfaceStyle.applyFramedScrollPaneCard(outputScroll);
        outputPanel.add(outputScroll, BorderLayout.CENTER);

        JSplitPane splitPane = AppToolWindowChrome.createVerticalInnerSplitPane(inputPanel, outputPanel, 260);
        splitPane.setResizeWeight(0.5);

        return splitPane;
    }

    private JPanel createStatusBar() {
        JPanel statusPanel = new JPanel(new BorderLayout());
        statusPanel.setOpaque(false);
        statusPanel.setBorder(BorderFactory.createEmptyBorder(5, 0, 0, 0));

        statusLabel = new JLabel(" ");
        statusLabel.setFont(FontsUtil.getDefaultFontWithOffset(Font.PLAIN, -1));
        statusLabel.setForeground(ModernColors.getTextSecondary());

        statusPanel.add(statusLabel, BorderLayout.WEST);

        return statusPanel;
    }

    private void handleInputChange() {
        String input = inputArea.getText();
        if (input.isEmpty()) {
            outputArea.setText("");
            updateStatus("");
        } else if (autoCalculate) {
            if (calculateAllCheckBox.isSelected()) {
                calculateAllHashes();
            } else {
                calculateHash(lastSelectedAlgorithm);
            }
        }
    }

    private void calculateHash(String algorithm) {
        String input = inputArea.getText();
        if (input.isEmpty()) {
            outputArea.setText("");
            updateStatus("");
            return;
        }

        try {
            long startTime = System.currentTimeMillis();
            MessageDigest md = MessageDigest.getInstance(algorithm);
            byte[] hash = md.digest(input.getBytes(StandardCharsets.UTF_8));

            String hexString = bytesToHex(hash);
            long duration = System.currentTimeMillis() - startTime;

            outputArea.setText(algorithm + ": " + hexString);
            updateStatus(String.format("✓ %s calculated in %d ms | Input: %d bytes | Output: %d chars",
                    algorithm, duration, input.getBytes(StandardCharsets.UTF_8).length, hexString.length()));
        } catch (Exception ex) {
            log.error("Hash calculation error for " + algorithm, ex);
            outputArea.setText("Error: " + ex.getMessage());
            updateStatus("✗ Error calculating " + algorithm);
        }
    }

    private void calculateAllHashes() {
        String input = inputArea.getText();
        if (input.isEmpty()) {
            outputArea.setText("");
            updateStatus("");
            return;
        }

        StringBuilder result = new StringBuilder();
        String[] algorithms = {"MD5", "SHA-1", "SHA-256", "SHA-512"};
        long startTime = System.currentTimeMillis();
        int successCount = 0;

        for (String algorithm : algorithms) {
            try {
                MessageDigest md = MessageDigest.getInstance(algorithm);
                byte[] hash = md.digest(input.getBytes(StandardCharsets.UTF_8));
                String hexString = bytesToHex(hash);

                result.append(String.format("%-10s: %s%n%n", algorithm, hexString));
                successCount++;
            } catch (Exception ex) {
                log.error("Hash calculation error for " + algorithm, ex);
                result.append(String.format("%-10s: Error - %s%n%n", algorithm, ex.getMessage()));
            }
        }

        long duration = System.currentTimeMillis() - startTime;
        outputArea.setText(result.toString().trim());
        updateStatus(String.format("✓ %d/%d algorithms calculated in %d ms | Input: %d bytes",
                successCount, algorithms.length, duration, input.getBytes(StandardCharsets.UTF_8).length));
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        return uppercaseCheckBox.isSelected() ? hexString.toString().toUpperCase() : hexString.toString();
    }

    private void updateButtonStates(String selectedAlgorithm) {
        algorithmButtons.forEach((algorithm, button) -> {
            if (algorithm.equals(selectedAlgorithm)) {
                button.setBackground(ModernColors.getSelectionBackgroundColor());
                button.setOpaque(true);
            } else {
                button.setBackground(null);
                button.setOpaque(false);
            }
        });
    }

    private void updateStatus(String message) {
        statusLabel.setText(message);
    }

    private void copyToClipboard() {
        String text = outputArea.getText();
        if (!text.isEmpty()) {
            Toolkit.getDefaultToolkit().getSystemClipboard()
                    .setContents(new StringSelection(text), null);
            updateStatus("✓ Copied to clipboard");

            // 短暂显示复制成功提示
            Timer timer = new Timer(2000, e -> updateStatus(" "));
            timer.setRepeats(false);
            timer.start();
        }
    }

    private void clearAll() {
        inputArea.setText("");
        outputArea.setText("");
        updateStatus("Cleared");
    }

    private void setupKeyBindings() {
        // Ctrl+Enter / Cmd+Enter 计算
        KeyStroke calculateKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER,
                Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx());
        inputArea.getInputMap(JComponent.WHEN_FOCUSED).put(calculateKeyStroke, "calculate");
        inputArea.getActionMap().put("calculate", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                if (calculateAllCheckBox.isSelected()) {
                    calculateAllHashes();
                } else {
                    calculateHash(lastSelectedAlgorithm);
                }
            }
        });

        // Ctrl+L / Cmd+L 清空
        KeyStroke clearKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_L,
                Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx());
        inputArea.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(clearKeyStroke, "clear");
        inputArea.getActionMap().put("clear", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                clearAll();
            }
        });
    }
}
