package com.laker.postman.panel.toolbox;

import com.laker.postman.common.component.ToolWindowActionToolbar;
import com.laker.postman.common.component.ToolWindowChrome;
import com.laker.postman.common.component.ToolWindowSurfaceStyle;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * 编解码工具面板
 */
@Slf4j
public class EncoderPanel extends JPanel {

    private JTextArea inputArea;
    private JTextArea outputArea;
    private JComboBox<String> typeCombo;

    public EncoderPanel() {
        initUI();
    }

    private void initUI() {
        setLayout(new BorderLayout(10, 10));
        ToolWindowSurfaceStyle.applyCard(this);
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // 顶部工具栏
        JLabel typeLabel = new JLabel(I18nUtil.getMessage(MessageKeys.TOOLBOX_ENCODER_TITLE) + ":");

        typeCombo = new JComboBox<>(new String[]{
                "Base64", "URL", "HTML Entity", "Unicode"
        });

        JButton encodeBtn = new JButton(I18nUtil.getMessage(MessageKeys.TOOLBOX_ENCODER_ENCODE));
        JButton decodeBtn = new JButton(I18nUtil.getMessage(MessageKeys.TOOLBOX_ENCODER_DECODE));
        JButton copyBtn = new JButton(I18nUtil.getMessage(MessageKeys.BUTTON_COPY));
        JButton clearBtn = new JButton(I18nUtil.getMessage(MessageKeys.BUTTON_CLEAR));

        JPanel topPanel = ToolWindowActionToolbar.inlineLeft(
                typeLabel,
                typeCombo,
                encodeBtn,
                decodeBtn,
                copyBtn,
                clearBtn
        );

        add(topPanel, BorderLayout.NORTH);

        // 中间分割面板
        // 输入区域
        JPanel inputPanel = new JPanel(new BorderLayout(5, 5));
        inputPanel.setOpaque(false);
        inputPanel.add(new JLabel(I18nUtil.getMessage(MessageKeys.TOOLBOX_ENCODER_INPUT)), BorderLayout.NORTH);
        inputArea = new JTextArea();
        inputArea.setLineWrap(true);
        inputArea.setWrapStyleWord(true);
        ToolWindowSurfaceStyle.applyTextComponentInput(inputArea);
        JScrollPane inputScrollPane = new JScrollPane(inputArea);
        ToolWindowSurfaceStyle.applyScrollPaneCard(inputScrollPane);
        inputPanel.add(inputScrollPane, BorderLayout.CENTER);

        // 输出区域
        JPanel outputPanel = new JPanel(new BorderLayout(5, 5));
        outputPanel.setOpaque(false);
        outputPanel.add(new JLabel(I18nUtil.getMessage(MessageKeys.TOOLBOX_ENCODER_OUTPUT)), BorderLayout.NORTH);
        outputArea = new JTextArea();
        outputArea.setLineWrap(true);
        outputArea.setWrapStyleWord(true);
        outputArea.setEditable(false);
        ToolWindowSurfaceStyle.applyTextComponentCard(outputArea);
        JScrollPane outputScrollPane = new JScrollPane(outputArea);
        ToolWindowSurfaceStyle.applyScrollPaneCard(outputScrollPane);
        outputPanel.add(outputScrollPane, BorderLayout.CENTER);

        JSplitPane splitPane = ToolWindowChrome.createVerticalInnerSplitPane(inputPanel, outputPanel, 200);

        add(splitPane, BorderLayout.CENTER);

        // 按钮事件
        encodeBtn.addActionListener(e -> encode());
        decodeBtn.addActionListener(e -> decode());
        copyBtn.addActionListener(e -> copyToClipboard());
        clearBtn.addActionListener(e -> {
            inputArea.setText("");
            outputArea.setText("");
        });
    }

    private void encode() {
        String input = inputArea.getText();
        if (input.isEmpty()) {
            outputArea.setText("");
            return;
        }

        try {
            String result = switch (typeCombo.getSelectedIndex()) {
                case 0 -> Base64.getEncoder().encodeToString(input.getBytes(StandardCharsets.UTF_8));
                case 1 -> URLEncoder.encode(input, StandardCharsets.UTF_8);
                case 2 -> htmlEncode(input);
                case 3 -> unicodeEncode(input);
                default -> input;
            };
            outputArea.setText(result);
        } catch (Exception ex) {
            log.error("Encode error", ex);
            outputArea.setText("Error: " + ex.getMessage());
        }
    }

    private void decode() {
        String input = inputArea.getText();
        if (input.isEmpty()) {
            outputArea.setText("");
            return;
        }

        try {
            String result = switch (typeCombo.getSelectedIndex()) {
                case 0 -> new String(Base64.getDecoder().decode(input), StandardCharsets.UTF_8);
                case 1 -> URLDecoder.decode(input, StandardCharsets.UTF_8);
                case 2 -> htmlDecode(input);
                case 3 -> unicodeDecode(input);
                default -> input;
            };
            outputArea.setText(result);
        } catch (Exception ex) {
            log.error("Decode error", ex);
            outputArea.setText("Error: " + ex.getMessage());
        }
    }

    private void copyToClipboard() {
        String text = outputArea.getText();
        if (!text.isEmpty()) {
            Toolkit.getDefaultToolkit().getSystemClipboard()
                    .setContents(new StringSelection(text), null);
        }
    }

    private String htmlEncode(String str) {
        return str.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private String htmlDecode(String str) {
        return str.replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                .replace("&#39;", "'");
    }

    private String unicodeEncode(String str) {
        StringBuilder sb = new StringBuilder();
        for (char c : str.toCharArray()) {
            if (c < 128) {
                sb.append(c);
            } else {
                sb.append(String.format("\\u%04x", (int) c));
            }
        }
        return sb.toString();
    }

    private String unicodeDecode(String str) {
        StringBuilder sb = new StringBuilder();
        int i = 0;
        while (i < str.length()) {
            if (str.charAt(i) == '\\' && i + 1 < str.length() && str.charAt(i + 1) == 'u' && i + 5 <= str.length()) {
                String hex = str.substring(i + 2, i + 6);
                try {
                    sb.append((char) Integer.parseInt(hex, 16));
                    i += 6;
                } catch (NumberFormatException e) {
                    sb.append(str.charAt(i));
                    i++;
                }
            } else {
                sb.append(str.charAt(i));
                i++;
            }
        }
        return sb.toString();
    }
}
