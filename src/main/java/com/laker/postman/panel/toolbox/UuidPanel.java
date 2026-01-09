package com.laker.postman.panel.toolbox;

import com.laker.postman.common.constants.ModernColors;
import com.laker.postman.util.FontsUtil;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import javax.swing.Timer;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.NetworkInterface;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.regex.Pattern;

/**
 * UUID生成工具面板 - 增强版
 */
@Slf4j
public class UuidPanel extends JPanel {

    private JTextArea uuidArea;
    private JSpinner countSpinner;
    private JCheckBox uppercaseCheckBox;
    private JCheckBox withHyphensCheckBox;
    private JLabel statusLabel;
    private JComboBox<String> versionComboBox;
    private JComboBox<String> separatorComboBox;
    private JTextArea parseArea;
    private JTextField namespaceField;
    private JTextField nameField;

    // UUID 正则表达式
    private static final Pattern UUID_NO_HYPHEN_PATTERN = Pattern.compile("^[0-9a-fA-F]{32}$");

    // 预定义命名空间
    private static final UUID NAMESPACE_DNS = UUID.fromString("6ba7b810-9dad-11d1-80b4-00c04fd430c8");
    private static final UUID NAMESPACE_URL = UUID.fromString("6ba7b811-9dad-11d1-80b4-00c04fd430c8");
    private static final UUID NAMESPACE_OID = UUID.fromString("6ba7b812-9dad-11d1-80b4-00c04fd430c8");
    private static final UUID NAMESPACE_X500 = UUID.fromString("6ba7b814-9dad-11d1-80b4-00c04fd430c8");

    public UuidPanel() {
        initUI();
    }

    private void initUI() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // 主分割面板
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setResizeWeight(0.6);
        splitPane.setDividerLocation(0.6);

        // 左侧：生成面板
        JPanel leftPanel = createGeneratorPanel();
        splitPane.setLeftComponent(leftPanel);

        // 右侧：解析面板
        JPanel rightPanel = createParsePanel();
        splitPane.setRightComponent(rightPanel);

        add(splitPane, BorderLayout.CENTER);
    }

    /**
     * 创建生成器面板
     */
    private JPanel createGeneratorPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));

        // 顶部配置面板
        JPanel topPanel = new JPanel(new BorderLayout(10, 10));

        // 生成配置面板
        JPanel configPanel = new JPanel();
        configPanel.setLayout(new BoxLayout(configPanel, BoxLayout.Y_AXIS));
        TitledBorder configBorder = BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(),
                I18nUtil.getMessage(MessageKeys.TOOLBOX_UUID_BATCH_GENERATE)
        );
        configPanel.setBorder(configBorder);

        // 第一行：版本和数量
        JPanel row1 = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        row1.add(new JLabel(I18nUtil.getMessage(MessageKeys.TOOLBOX_UUID_VERSION) + ":"));
        versionComboBox = new JComboBox<>(new String[]{
            "UUID v4 (Random)",
            "UUID v1 (Time-based)",
            "UUID v3 (Name-based MD5)",
            "UUID v5 (Name-based SHA-1)"
        });
        versionComboBox.setPreferredSize(new Dimension(200, 28));
        row1.add(versionComboBox);

        row1.add(Box.createHorizontalStrut(10));
        row1.add(new JLabel(I18nUtil.getMessage(MessageKeys.TOOLBOX_UUID_COUNT) + ":"));
        SpinnerNumberModel spinnerModel = new SpinnerNumberModel(1, 1, 10000, 1);
        countSpinner = new JSpinner(spinnerModel);
        countSpinner.setPreferredSize(new Dimension(80, 28));
        row1.add(countSpinner);
        configPanel.add(row1);

        // 第二行：格式配置
        JPanel row2 = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        row2.add(new JLabel(I18nUtil.getMessage(MessageKeys.TOOLBOX_UUID_FORMAT) + ":"));

        uppercaseCheckBox = new JCheckBox(I18nUtil.getMessage(MessageKeys.TOOLBOX_UUID_UPPERCASE));
        uppercaseCheckBox.setSelected(false);
        row2.add(uppercaseCheckBox);

        withHyphensCheckBox = new JCheckBox(I18nUtil.getMessage(MessageKeys.TOOLBOX_UUID_WITH_HYPHENS));
        withHyphensCheckBox.setSelected(true);
        row2.add(withHyphensCheckBox);

        row2.add(Box.createHorizontalStrut(10));
        row2.add(new JLabel(I18nUtil.getMessage(MessageKeys.TOOLBOX_UUID_SEPARATOR) + ":"));
        separatorComboBox = new JComboBox<>(new String[]{
            I18nUtil.getMessage(MessageKeys.TOOLBOX_UUID_SEPARATOR_NEWLINE),
            I18nUtil.getMessage(MessageKeys.TOOLBOX_UUID_SEPARATOR_COMMA),
            I18nUtil.getMessage(MessageKeys.TOOLBOX_UUID_SEPARATOR_SPACE),
            I18nUtil.getMessage(MessageKeys.TOOLBOX_UUID_SEPARATOR_SEMICOLON)
        });
        separatorComboBox.setPreferredSize(new Dimension(100, 28));
        row2.add(separatorComboBox);
        configPanel.add(row2);

        // 第三行：命名空间和名称（用于 v3 和 v5）
        JPanel row3 = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        row3.add(new JLabel(I18nUtil.getMessage(MessageKeys.TOOLBOX_UUID_NAMESPACE) + ":"));
        JComboBox<String> namespaceCombo = new JComboBox<>(new String[]{
            "DNS", "URL", "OID", "X.500", I18nUtil.getMessage(MessageKeys.TOOLBOX_UUID_NAMESPACE_CUSTOM)
        });
        namespaceCombo.setPreferredSize(new Dimension(100, 28));
        row3.add(namespaceCombo);

        namespaceField = new JTextField();
        namespaceField.setPreferredSize(new Dimension(150, 28));
        namespaceField.setEnabled(false);
        namespaceField.setToolTipText(I18nUtil.getMessage(MessageKeys.TOOLBOX_UUID_NAMESPACE_HINT));
        row3.add(namespaceField);

        row3.add(new JLabel(I18nUtil.getMessage(MessageKeys.TOOLBOX_UUID_NAME) + ":"));
        nameField = new JTextField("example.com");
        nameField.setPreferredSize(new Dimension(150, 28));
        row3.add(nameField);
        row3.setVisible(false); // 默认隐藏，只在选择 v3/v5 时显示
        configPanel.add(row3);

        topPanel.add(configPanel, BorderLayout.CENTER);

        // 操作按钮面板
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));

        JButton generateBtn = new JButton(I18nUtil.getMessage(MessageKeys.TOOLBOX_UUID_GENERATE));
        generateBtn.setPreferredSize(new Dimension(120, 32));
        generateBtn.setFocusPainted(false);

        JButton copyBtn = new JButton(I18nUtil.getMessage(MessageKeys.BUTTON_COPY));
        copyBtn.setPreferredSize(new Dimension(100, 32));
        copyBtn.setFocusPainted(false);

        JButton copyOneBtn = new JButton(I18nUtil.getMessage(MessageKeys.TOOLBOX_UUID_COPY_ONE));
        copyOneBtn.setPreferredSize(new Dimension(120, 32));
        copyOneBtn.setFocusPainted(false);

        JButton exportBtn = new JButton(I18nUtil.getMessage(MessageKeys.TOOLBOX_UUID_EXPORT));
        exportBtn.setPreferredSize(new Dimension(100, 32));
        exportBtn.setFocusPainted(false);

        JButton clearBtn = new JButton(I18nUtil.getMessage(MessageKeys.BUTTON_CLEAR));
        clearBtn.setPreferredSize(new Dimension(100, 32));
        clearBtn.setFocusPainted(false);

        buttonPanel.add(generateBtn);
        buttonPanel.add(copyBtn);
        buttonPanel.add(copyOneBtn);
        buttonPanel.add(exportBtn);
        buttonPanel.add(clearBtn);

        topPanel.add(buttonPanel, BorderLayout.SOUTH);
        panel.add(topPanel, BorderLayout.NORTH);

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
        uuidArea.setFont(FontsUtil.getDefaultFontWithOffset(Font.PLAIN, +1));
        uuidArea.setBackground(ModernColors.getBackgroundColor());
        uuidArea.setForeground(ModernColors.getTextPrimary());
        uuidArea.setMargin(new Insets(10, 10, 10, 10));

        JScrollPane scrollPane = new JScrollPane(uuidArea);
        scrollPane.setBorder(BorderFactory.createLineBorder(ModernColors.getBorderMediumColor()));
        centerPanel.add(scrollPane, BorderLayout.CENTER);

        panel.add(centerPanel, BorderLayout.CENTER);

        // 底部信息面板
        JPanel bottomPanel = new JPanel(new BorderLayout(10, 5));
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(5, 0, 0, 0));

        JPanel infoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        JLabel infoLabel = new JLabel(I18nUtil.getMessage(MessageKeys.TOOLBOX_UUID_VERSION_INFO));
        infoLabel.setFont(FontsUtil.getDefaultFont(Font.ITALIC));
        infoLabel.setForeground(ModernColors.getTextSecondary());
        infoPanel.add(infoLabel);

        statusLabel = new JLabel("");
        statusLabel.setFont(FontsUtil.getDefaultFontWithOffset(Font.PLAIN, -1));
        statusLabel.setForeground(ModernColors.SUCCESS);
        JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        statusPanel.add(statusLabel);

        bottomPanel.add(infoPanel, BorderLayout.WEST);
        bottomPanel.add(statusPanel, BorderLayout.EAST);
        panel.add(bottomPanel, BorderLayout.SOUTH);

        // 按钮事件
        generateBtn.addActionListener(e -> {
            int count = (Integer) countSpinner.getValue();
            generateUuid(count);
        });

        copyBtn.addActionListener(e -> copyToClipboard());

        copyOneBtn.addActionListener(e -> copyFirstUuid());

        exportBtn.addActionListener(e -> exportToFile());

        clearBtn.addActionListener(e -> {
            uuidArea.setText("");
            statusLabel.setText("");
        });

        // 复选框变更时自动重新生成
        uppercaseCheckBox.addActionListener(e -> refreshIfNotEmpty());
        withHyphensCheckBox.addActionListener(e -> refreshIfNotEmpty());
        separatorComboBox.addActionListener(e -> refreshIfNotEmpty());

        // 版本选择变更事件
        versionComboBox.addActionListener(e -> {
            int selectedIndex = versionComboBox.getSelectedIndex();
            // v3 和 v5 需要显示命名空间和名称输入框
            row3.setVisible(selectedIndex == 2 || selectedIndex == 3);
            configPanel.revalidate();
            configPanel.repaint();
        });

        // 命名空间选择变更事件
        namespaceCombo.addActionListener(e -> {
            int selected = namespaceCombo.getSelectedIndex();
            if (selected == 4) { // Custom
                namespaceField.setEnabled(true);
                namespaceField.setText("");
            } else {
                namespaceField.setEnabled(false);
                UUID namespace = switch (selected) {
                    case 0 -> NAMESPACE_DNS;
                    case 1 -> NAMESPACE_URL;
                    case 2 -> NAMESPACE_OID;
                    case 3 -> NAMESPACE_X500;
                    default -> NAMESPACE_DNS;
                };
                namespaceField.setText(namespace.toString());
            }
        });

        // 初始化命名空间字段
        namespaceField.setText(NAMESPACE_DNS.toString());

        // 初始生成一个UUID
        generateUuid(1);

        return panel;
    }

    /**
     * 创建解析面板
     */
    private JPanel createParsePanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));

        TitledBorder parseBorder = BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(),
                I18nUtil.getMessage(MessageKeys.TOOLBOX_UUID_PARSE)
        );
        panel.setBorder(parseBorder);

        // 顶部输入区域
        JPanel inputPanel = new JPanel(new BorderLayout(5, 5));
        inputPanel.add(new JLabel(I18nUtil.getMessage(MessageKeys.TOOLBOX_UUID_INPUT) + ":"), BorderLayout.NORTH);

        JTextField inputField = new JTextField();
        inputField.setFont(FontsUtil.getDefaultFontWithOffset(Font.PLAIN, +1));
        inputField.setPreferredSize(new Dimension(0, 30));
        inputPanel.add(inputField, BorderLayout.CENTER);

        JButton parseBtn = new JButton(I18nUtil.getMessage(MessageKeys.TOOLBOX_UUID_PARSE));
        parseBtn.setPreferredSize(new Dimension(100, 30));
        parseBtn.setFocusPainted(false);
        inputPanel.add(parseBtn, BorderLayout.EAST);

        panel.add(inputPanel, BorderLayout.NORTH);

        // 解析结果显示区域
        parseArea = new JTextArea();
        parseArea.setEditable(false);
        parseArea.setFont(FontsUtil.getDefaultFont(Font.PLAIN));
        parseArea.setBackground(ModernColors.getBackgroundColor());
        parseArea.setForeground(ModernColors.getTextPrimary());
        parseArea.setMargin(new Insets(10, 10, 10, 10));
        parseArea.setLineWrap(true);
        parseArea.setWrapStyleWord(true);

        JScrollPane scrollPane = new JScrollPane(parseArea);
        scrollPane.setBorder(BorderFactory.createLineBorder(ModernColors.getBorderMediumColor()));
        panel.add(scrollPane, BorderLayout.CENTER);

        // 解析按钮事件
        parseBtn.addActionListener(e -> parseUuid(inputField.getText().trim()));

        // 输入框回车事件
        inputField.addActionListener(e -> parseUuid(inputField.getText().trim()));

        return panel;
    }

    /**
     * 生成UUID
     */
    private void generateUuid(int count) {
        StringBuilder sb = new StringBuilder();
        boolean uppercase = uppercaseCheckBox.isSelected();
        boolean withHyphens = withHyphensCheckBox.isSelected();
        int versionIndex = versionComboBox.getSelectedIndex();
        String separator = getSeparator();

        for (int i = 0; i < count; i++) {
            String uuid = switch (versionIndex) {
                case 0 -> UUID.randomUUID().toString(); // v4
                case 1 -> generateUuidV1(); // v1
                case 2 -> generateUuidV3(); // v3
                case 3 -> generateUuidV5(); // v5
                default -> UUID.randomUUID().toString();
            };

            if (!withHyphens) {
                uuid = uuid.replace("-", "");
            }

            if (uppercase) {
                uuid = uuid.toUpperCase();
            }

            sb.append(uuid);
            if (i < count - 1) {
                sb.append(separator);
            }
        }

        uuidArea.setText(sb.toString());
        uuidArea.setCaretPosition(0);

        // 更新状态信息
        String statusText = String.format("%s: %d",
                I18nUtil.getMessage(MessageKeys.TOOLBOX_UUID_COUNT), count);
        statusLabel.setText(statusText);
    }

    /**
     * 生成 UUID v1 (基于时间)
     */
    private String generateUuidV1() {
        long timestamp = System.currentTimeMillis();
        long time = timestamp * 10000 + 0x01b21dd213814000L;

        long timeLow = time & 0xFFFFFFFFL;
        long timeMid = (time >> 32) & 0xFFFFL;
        long timeHi = ((time >> 48) & 0x0FFFL) | 0x1000; // Version 1

        SecureRandom random = new SecureRandom();
        long clockSeq = random.nextInt(0x3FFF) | 0x8000; // Variant
        long node = getNodeId();

        return String.format("%08x-%04x-%04x-%04x-%012x",
                timeLow, timeMid, timeHi, clockSeq, node);
    }

    /**
     * 生成 UUID v3 (基于名称的 MD5)
     */
    private String generateUuidV3() {
        return generateNameBasedUuid("MD5", 3);
    }

    /**
     * 生成 UUID v5 (基于名称的 SHA-1)
     */
    private String generateUuidV5() {
        return generateNameBasedUuid("SHA-1", 5);
    }

    /**
     * 生成基于名称的 UUID（v3 或 v5）
     */
    private String generateNameBasedUuid(String algorithm, int version) {
        try {
            String namespaceStr = namespaceField.getText().trim();
            String name = nameField.getText().trim();

            if (name.isEmpty()) {
                name = "example.com"; // 默认名称
            }

            // 解析命名空间 UUID
            UUID namespace;
            try {
                namespaceStr = namespaceStr.replace("-", "");
                if (namespaceStr.length() == 32) {
                    namespaceStr = String.format("%s-%s-%s-%s-%s",
                        namespaceStr.substring(0, 8),
                        namespaceStr.substring(8, 12),
                        namespaceStr.substring(12, 16),
                        namespaceStr.substring(16, 20),
                        namespaceStr.substring(20, 32));
                }
                namespace = UUID.fromString(namespaceStr);
            } catch (Exception e) {
                namespace = NAMESPACE_DNS; // 默认使用 DNS 命名空间
            }

            // 将命名空间 UUID 和名称组合
            byte[] namespaceBytes = toBytes(namespace);
            byte[] nameBytes = name.getBytes(StandardCharsets.UTF_8);
            byte[] combined = new byte[namespaceBytes.length + nameBytes.length];
            System.arraycopy(namespaceBytes, 0, combined, 0, namespaceBytes.length);
            System.arraycopy(nameBytes, 0, combined, namespaceBytes.length, nameBytes.length);

            // 计算哈希
            MessageDigest digest = MessageDigest.getInstance(algorithm);
            byte[] hash = digest.digest(combined);

            // 构造 UUID
            long msb = 0;
            long lsb = 0;

            for (int i = 0; i < 8; i++) {
                msb = (msb << 8) | (hash[i] & 0xff);
            }
            for (int i = 8; i < 16; i++) {
                lsb = (lsb << 8) | (hash[i] & 0xff);
            }

            // 设置版本和变体
            msb &= ~0x0000f000L; // 清除版本位
            msb |= ((long) version) << 12; // 设置版本
            lsb &= ~0xc000000000000000L; // 清除变体位
            lsb |= 0x8000000000000000L; // 设置变体（RFC 4122）

            return formatUuid(msb, lsb);
        } catch (Exception e) {
            log.error("Failed to generate name-based UUID", e);
            return UUID.randomUUID().toString(); // 失败时返回随机 UUID
        }
    }

    /**
     * 将 UUID 转换为字节数组
     */
    private byte[] toBytes(UUID uuid) {
        long msb = uuid.getMostSignificantBits();
        long lsb = uuid.getLeastSignificantBits();
        byte[] buffer = new byte[16];

        for (int i = 0; i < 8; i++) {
            buffer[i] = (byte) (msb >>> (8 * (7 - i)));
        }
        for (int i = 8; i < 16; i++) {
            buffer[i] = (byte) (lsb >>> (8 * (7 - i)));
        }

        return buffer;
    }

    /**
     * 格式化 UUID
     */
    private String formatUuid(long msb, long lsb) {
        return String.format("%08x-%04x-%04x-%04x-%012x",
                (msb >> 32) & 0xFFFFFFFFL,
                (msb >> 16) & 0xFFFFL,
                msb & 0xFFFFL,
                (lsb >> 48) & 0xFFFFL,
                lsb & 0xFFFFFFFFFFFFL);
    }

    /**
     * 获取节点ID (MAC地址或随机数)
     */
    private long getNodeId() {
        try {
            NetworkInterface network = NetworkInterface.getNetworkInterfaces().nextElement();
            byte[] mac = network.getHardwareAddress();
            if (mac != null) {
                long node = 0;
                for (int i = 0; i < Math.min(mac.length, 6); i++) {
                    node = (node << 8) | (mac[i] & 0xff);
                }
                return node;
            }
        } catch (Exception e) {
            // Ignore
        }
        // 使用随机数作为节点ID
        return new SecureRandom().nextLong() & 0xFFFFFFFFFFFFL;
    }

    /**
     * 获取分隔符
     */
    private String getSeparator() {
        int index = separatorComboBox.getSelectedIndex();
        return switch (index) {
            case 1 -> ", ";
            case 2 -> " ";
            case 3 -> "; ";
            default -> "\n";
        };
    }

    /**
     * 解析UUID
     */
    private void parseUuid(String input) {
        if (input.isEmpty()) {
            parseArea.setText(I18nUtil.getMessage(MessageKeys.TOOLBOX_UUID_PARSE_EMPTY));
            return;
        }

        // 移除可能的分隔符
        String uuid = input.replace("-", "").replace(" ", "").trim();

        if (!UUID_NO_HYPHEN_PATTERN.matcher(uuid).matches()) {
            parseArea.setText(I18nUtil.getMessage(MessageKeys.TOOLBOX_UUID_PARSE_INVALID));
            return;
        }

        // 添加连字符
        String formattedUuid = String.format("%s-%s-%s-%s-%s",
                uuid.substring(0, 8),
                uuid.substring(8, 12),
                uuid.substring(12, 16),
                uuid.substring(16, 20),
                uuid.substring(20, 32));

        StringBuilder result = new StringBuilder();
        result.append("═══════════════════════════════\n");
        result.append(I18nUtil.getMessage(MessageKeys.TOOLBOX_UUID_PARSE_RESULT)).append("\n");
        result.append("═══════════════════════════════\n\n");

        result.append("📋 ").append(I18nUtil.getMessage(MessageKeys.TOOLBOX_UUID_STANDARD_FORMAT)).append(":\n");
        result.append("   ").append(formattedUuid).append("\n\n");

        result.append("🔤 ").append(I18nUtil.getMessage(MessageKeys.TOOLBOX_UUID_UPPERCASE)).append(":\n");
        result.append("   ").append(formattedUuid.toUpperCase()).append("\n\n");

        result.append("🔗 ").append(I18nUtil.getMessage(MessageKeys.TOOLBOX_UUID_WITHOUT_HYPHENS)).append(":\n");
        result.append("   ").append(uuid).append("\n\n");

        // 解析版本和变体
        int version = Integer.parseInt(uuid.substring(12, 13), 16) >> 4;
        int variant = Integer.parseInt(uuid.substring(16, 17), 16) >> 4;

        result.append("ℹ️  ").append(I18nUtil.getMessage(MessageKeys.TOOLBOX_UUID_VERSION)).append(": ");
        result.append(version).append("\n");
        result.append("   ");
        switch (version) {
            case 1: result.append("(Time-based)"); break;
            case 2: result.append("(DCE Security)"); break;
            case 3: result.append("(Name-based MD5)"); break;
            case 4: result.append("(Random)"); break;
            case 5: result.append("(Name-based SHA-1)"); break;
            default: result.append("(Unknown)"); break;
        }
        result.append("\n\n");

        result.append("🔀 ").append(I18nUtil.getMessage(MessageKeys.TOOLBOX_UUID_VARIANT)).append(": ");
        if ((variant & 0x8) == 0x8) {
            result.append("RFC 4122\n");
        } else {
            result.append("Reserved\n");
        }
        result.append("\n");

        // 如果是 v1，解析时间戳
        if (version == 1) {
            try {
                long timeLow = Long.parseLong(uuid.substring(0, 8), 16);
                long timeMid = Long.parseLong(uuid.substring(8, 12), 16);
                long timeHi = Long.parseLong(uuid.substring(12, 16), 16) & 0x0FFF;

                long timestamp = (timeHi << 48) | (timeMid << 32) | timeLow;
                long unixTime = (timestamp - 0x01b21dd213814000L) / 10000;

                Instant instant = Instant.ofEpochMilli(unixTime);
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")
                        .withZone(ZoneId.systemDefault());

                result.append("⏰ ").append(I18nUtil.getMessage(MessageKeys.TOOLBOX_UUID_TIMESTAMP)).append(":\n");
                result.append("   ").append(formatter.format(instant)).append("\n");
                result.append("   (").append(unixTime).append(" ms)\n");
            } catch (Exception e) {
                log.warn("Failed to parse timestamp: {}", e.getMessage());
            }
        }

        // 如果是 v3 或 v5，提示这是基于名称的 UUID
        if (version == 3 || version == 5) {
            result.append("\n");
            result.append("📝 ").append(I18nUtil.getMessage(MessageKeys.TOOLBOX_UUID_NAME_BASED)).append("\n");
            result.append("   ").append(I18nUtil.getMessage(MessageKeys.TOOLBOX_UUID_NAME_BASED_DESC)).append("\n");
        }

        parseArea.setText(result.toString());
        parseArea.setCaretPosition(0);
    }

    /**
     * 复制到剪贴板
     */
    private void copyToClipboard() {
        String text = uuidArea.getText().trim();
        if (!text.isEmpty()) {
            Toolkit.getDefaultToolkit().getSystemClipboard()
                    .setContents(new StringSelection(text), null);

            statusLabel.setText("✓ " + I18nUtil.getMessage(MessageKeys.BUTTON_COPY) + " " +
                    I18nUtil.getMessage(MessageKeys.SUCCESS));

            Timer timer = new Timer(3000, e -> updateStatusWithCount());
            timer.setRepeats(false);
            timer.start();
        } else {
            showInfoMessage(I18nUtil.getMessage(MessageKeys.TOOLBOX_UUID_EMPTY));
        }
    }

    /**
     * 复制第一个UUID
     */
    private void copyFirstUuid() {
        String text = uuidArea.getText().trim();
        if (!text.isEmpty()) {
            String[] uuids = text.split("[,;\\s\\n]+");
            if (uuids.length > 0 && !uuids[0].isEmpty()) {
                Toolkit.getDefaultToolkit().getSystemClipboard()
                        .setContents(new StringSelection(uuids[0]), null);

                statusLabel.setText("✓ " + I18nUtil.getMessage(MessageKeys.TOOLBOX_UUID_COPY_ONE) + " " +
                        I18nUtil.getMessage(MessageKeys.SUCCESS));

                Timer timer = new Timer(3000, e -> updateStatusWithCount());
                timer.setRepeats(false);
                timer.start();
            }
        } else {
            showInfoMessage(I18nUtil.getMessage(MessageKeys.TOOLBOX_UUID_EMPTY));
        }
    }

    /**
     * 导出到文件
     */
    private void exportToFile() {
        String text = uuidArea.getText().trim();
        if (text.isEmpty()) {
            showInfoMessage(I18nUtil.getMessage(MessageKeys.TOOLBOX_UUID_EMPTY));
            return;
        }

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle(I18nUtil.getMessage(MessageKeys.TOOLBOX_UUID_EXPORT));
        fileChooser.setSelectedFile(new File("uuids_" + System.currentTimeMillis() + ".txt"));

        int result = fileChooser.showSaveDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            try (FileWriter writer = new FileWriter(file)) {
                writer.write(text);
                statusLabel.setText("✓ " + I18nUtil.getMessage(MessageKeys.TOOLBOX_UUID_EXPORT_SUCCESS));

                Timer timer = new Timer(3000, e -> updateStatusWithCount());
                timer.setRepeats(false);
                timer.start();
            } catch (IOException ex) {
                log.error("Failed to export UUIDs", ex);
                JOptionPane.showMessageDialog(this,
                        I18nUtil.getMessage(MessageKeys.TOOLBOX_UUID_EXPORT_FAILED) + ": " + ex.getMessage(),
                        I18nUtil.getMessage(MessageKeys.ERROR),
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /**
     * 如果有内容则刷新
     */
    private void refreshIfNotEmpty() {
        String text = uuidArea.getText().trim();
        if (!text.isEmpty()) {
            int count = (Integer) countSpinner.getValue();
            generateUuid(count);
        }
    }

    /**
     * 更新状态显示数量
     */
    private void updateStatusWithCount() {
        String currentText = uuidArea.getText().trim();
        if (!currentText.isEmpty()) {
            String separator = getSeparator().trim();
            String[] parts;
            if (separator.isEmpty()) {
                parts = currentText.split("\\s+");
            } else {
                parts = currentText.split("[,;\\s]+|\\n+");
            }
            List<String> validUuids = new ArrayList<>();
            for (String part : parts) {
                if (!part.isEmpty()) {
                    validUuids.add(part);
                }
            }
            statusLabel.setText(String.format("%s: %d",
                    I18nUtil.getMessage(MessageKeys.TOOLBOX_UUID_COUNT), validUuids.size()));
        }
    }

    /**
     * 显示信息消息
     */
    private void showInfoMessage(String message) {
        JOptionPane.showMessageDialog(this,
                message,
                I18nUtil.getMessage(MessageKeys.TIP),
                JOptionPane.INFORMATION_MESSAGE);
    }
}
