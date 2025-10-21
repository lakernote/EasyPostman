package com.laker.postman.panel.toolbox;

import com.laker.postman.util.I18nUtil;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
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

    public UuidPanel() {
        initUI();
    }

    private void initUI() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // 顶部工具栏
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));

        JButton generateBtn = new JButton(I18nUtil.getMessage("toolbox.uuid.generate"));
        JButton copyBtn = new JButton(I18nUtil.getMessage("button.copy"));
        JButton clearBtn = new JButton(I18nUtil.getMessage("button.clear"));

        topPanel.add(new JLabel(I18nUtil.getMessage("toolbox.uuid.generate") + ":"));

        // 添加数量选择器
        SpinnerNumberModel spinnerModel = new SpinnerNumberModel(1, 1, 100, 1);
        countSpinner = new JSpinner(spinnerModel);
        countSpinner.setPreferredSize(new Dimension(60, 25));
        topPanel.add(countSpinner);
        topPanel.add(new JLabel("UUID(s)"));

        topPanel.add(generateBtn);
        topPanel.add(copyBtn);
        topPanel.add(clearBtn);

        add(topPanel, BorderLayout.NORTH);

        // UUID显示区域
        uuidArea = new JTextArea();
        uuidArea.setEditable(false);
        uuidArea.setLineWrap(true);
        uuidArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        add(new JScrollPane(uuidArea), BorderLayout.CENTER);

        // 底部信息面板
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JLabel infoLabel = new JLabel("ℹ️ UUID Version 4 (Random)");
        infoLabel.setFont(infoLabel.getFont().deriveFont(Font.ITALIC));
        bottomPanel.add(infoLabel);
        add(bottomPanel, BorderLayout.SOUTH);

        // 按钮事件
        generateBtn.addActionListener(e -> {
            int count = (Integer) countSpinner.getValue();
            generateUuid(count);
        });
        copyBtn.addActionListener(e -> copyToClipboard());
        clearBtn.addActionListener(e -> uuidArea.setText(""));

        // 初始生成一个UUID
        generateUuid(1);
    }

    private void generateUuid(int count) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) {
            sb.append(UUID.randomUUID().toString());
            if (i < count - 1) {
                sb.append("\n");
            }
        }
        uuidArea.setText(sb.toString());
    }

    private void copyToClipboard() {
        String text = uuidArea.getText().trim();
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
