package com.laker.postman.panel.toolbox;

import com.laker.postman.util.EasyPostManFontUtil;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.util.UUID;

/**
 * UUID生成工具面板
 */
@Slf4j
public class UuidPanel extends JPanel {

    private JTextArea uuidArea;
    private JSpinner countSpinner;
    private JCheckBox uppercaseCheckBox;
    private JCheckBox withHyphensCheckBox;
    private JLabel statusLabel;

    public UuidPanel() {
        initUI();
    }

    private void initUI() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // 顶部配置面板
        JPanel topPanel = new JPanel(new BorderLayout(10, 10));

        // 生成配置面板
        JPanel configPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        TitledBorder configBorder = BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(),
                I18nUtil.getMessage(MessageKeys.TOOLBOX_UUID_BATCH_GENERATE)
        );
        configPanel.setBorder(configBorder);

        // 数量配置
        configPanel.add(new JLabel(I18nUtil.getMessage(MessageKeys.TOOLBOX_UUID_COUNT) + ":"));
        SpinnerNumberModel spinnerModel = new SpinnerNumberModel(1, 1, 1000, 1);
        countSpinner = new JSpinner(spinnerModel);
        countSpinner.setPreferredSize(new Dimension(80, 28));
        configPanel.add(countSpinner);

        configPanel.add(Box.createHorizontalStrut(20));

        // 格式配置
        JLabel formatLabel = new JLabel(I18nUtil.getMessage(MessageKeys.TOOLBOX_UUID_FORMAT) + ":");
        configPanel.add(formatLabel);

        uppercaseCheckBox = new JCheckBox(I18nUtil.getMessage(MessageKeys.TOOLBOX_UUID_UPPERCASE));
        uppercaseCheckBox.setSelected(false);
        configPanel.add(uppercaseCheckBox);

        withHyphensCheckBox = new JCheckBox(I18nUtil.getMessage(MessageKeys.TOOLBOX_UUID_WITH_HYPHENS));
        withHyphensCheckBox.setSelected(true);
        configPanel.add(withHyphensCheckBox);

        topPanel.add(configPanel, BorderLayout.CENTER);

        // 操作按钮面板
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));

        JButton generateBtn = new JButton(I18nUtil.getMessage(MessageKeys.TOOLBOX_UUID_GENERATE));
        generateBtn.setPreferredSize(new Dimension(120, 32));
        generateBtn.setFocusPainted(false);

        JButton copyBtn = new JButton(I18nUtil.getMessage(MessageKeys.BUTTON_COPY));
        copyBtn.setPreferredSize(new Dimension(100, 32));
        copyBtn.setFocusPainted(false);

        JButton clearBtn = new JButton(I18nUtil.getMessage(MessageKeys.BUTTON_CLEAR));
        clearBtn.setPreferredSize(new Dimension(100, 32));
        clearBtn.setFocusPainted(false);

        buttonPanel.add(generateBtn);
        buttonPanel.add(copyBtn);
        buttonPanel.add(clearBtn);

        topPanel.add(buttonPanel, BorderLayout.SOUTH);
        add(topPanel, BorderLayout.NORTH);

        // 中间UUID显示区域
        JPanel centerPanel = new JPanel(new BorderLayout(5, 5));
        TitledBorder centerBorder = BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(),
                I18nUtil.getMessage(MessageKeys.TOOLBOX_UUID_GENERATED)
        );
        centerPanel.setBorder(centerBorder);

        uuidArea = new JTextArea();
        uuidArea.setEditable(false);
        uuidArea.setLineWrap(true);
        uuidArea.setWrapStyleWord(false);
        uuidArea.setFont(EasyPostManFontUtil.getDefaultFont(Font.PLAIN, 13));
        uuidArea.setBackground(new Color(250, 250, 250));
        uuidArea.setMargin(new Insets(10, 10, 10, 10));

        JScrollPane scrollPane = new JScrollPane(uuidArea);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200)));
        centerPanel.add(scrollPane, BorderLayout.CENTER);

        add(centerPanel, BorderLayout.CENTER);

        // 底部信息面板
        JPanel bottomPanel = new JPanel(new BorderLayout(10, 5));
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(5, 0, 0, 0));

        JPanel infoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        JLabel infoLabel = new JLabel(I18nUtil.getMessage(MessageKeys.TOOLBOX_UUID_VERSION_INFO));
        infoLabel.setFont(EasyPostManFontUtil.getDefaultFont(Font.ITALIC, 12));
        infoLabel.setForeground(new Color(100, 100, 100));
        infoPanel.add(infoLabel);

        statusLabel = new JLabel("");
        statusLabel.setFont(EasyPostManFontUtil.getDefaultFont(Font.PLAIN, 11));
        statusLabel.setForeground(new Color(60, 150, 60));
        JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        statusPanel.add(statusLabel);

        bottomPanel.add(infoPanel, BorderLayout.WEST);
        bottomPanel.add(statusPanel, BorderLayout.EAST);
        add(bottomPanel, BorderLayout.SOUTH);

        // 按钮事件
        generateBtn.addActionListener(e -> {
            int count = (Integer) countSpinner.getValue();
            generateUuid(count);
        });

        copyBtn.addActionListener(e -> copyToClipboard());

        clearBtn.addActionListener(e -> {
            uuidArea.setText("");
            statusLabel.setText("");
        });

        // 复选框变更时自动重新生成
        uppercaseCheckBox.addActionListener(e -> {
            String text = uuidArea.getText().trim();
            if (!text.isEmpty()) {
                int count = (Integer) countSpinner.getValue();
                generateUuid(count);
            }
        });

        withHyphensCheckBox.addActionListener(e -> {
            String text = uuidArea.getText().trim();
            if (!text.isEmpty()) {
                int count = (Integer) countSpinner.getValue();
                generateUuid(count);
            }
        });

        // 初始生成一个UUID
        generateUuid(1);
    }

    private void generateUuid(int count) {
        StringBuilder sb = new StringBuilder();
        boolean uppercase = uppercaseCheckBox.isSelected();
        boolean withHyphens = withHyphensCheckBox.isSelected();

        for (int i = 0; i < count; i++) {
            String uuid = UUID.randomUUID().toString();

            if (!withHyphens) {
                uuid = uuid.replace("-", "");
            }

            if (uppercase) {
                uuid = uuid.toUpperCase();
            }

            sb.append(uuid);
            if (i < count - 1) {
                sb.append("\n");
            }
        }

        uuidArea.setText(sb.toString());
        uuidArea.setCaretPosition(0);

        // 更新状态信息
        String statusText = String.format("%s: %d",
                I18nUtil.getMessage(MessageKeys.TOOLBOX_UUID_COUNT), count);
        statusLabel.setText(statusText);
    }

    private void copyToClipboard() {
        String text = uuidArea.getText().trim();
        if (!text.isEmpty()) {
            Toolkit.getDefaultToolkit().getSystemClipboard()
                    .setContents(new StringSelection(text), null);

            // 显示成功消息
            statusLabel.setText("✓ " + I18nUtil.getMessage(MessageKeys.BUTTON_COPY) + " " +
                    I18nUtil.getMessage(MessageKeys.SUCCESS));

            // 3秒后清除状态消息
            Timer timer = new Timer(3000, e -> {
                String currentText = uuidArea.getText().trim();
                if (!currentText.isEmpty()) {
                    String[] lines = currentText.split("\n");
                    statusLabel.setText(String.format("%s: %d",
                            I18nUtil.getMessage(MessageKeys.TOOLBOX_UUID_COUNT), lines.length));
                }
            });
            timer.setRepeats(false);
            timer.start();
        } else {
            JOptionPane.showMessageDialog(this,
                    I18nUtil.getMessage(MessageKeys.GENERAL_INFO),
                    I18nUtil.getMessage(MessageKeys.TIP),
                    JOptionPane.INFORMATION_MESSAGE);
        }
    }
}
