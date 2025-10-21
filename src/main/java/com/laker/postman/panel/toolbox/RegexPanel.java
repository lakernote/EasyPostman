package com.laker.postman.panel.toolbox;

import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * 正则表达式测试工具面板
 */
@Slf4j
public class RegexPanel extends JPanel {

    private JTextField patternField;
    private JTextArea textArea;
    private JTextArea resultArea;
    private JCheckBox caseSensitiveBox;
    private JCheckBox multilineBox;
    private JCheckBox dotallBox;
    private JTable matchTable;
    private DefaultTableModel tableModel;

    public RegexPanel() {
        initUI();
    }

    private void initUI() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // 顶部面板：正则表达式输入
        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));

        // 第一行：正则表达式
        JPanel row1 = new JPanel(new BorderLayout(5, 5));
        row1.add(new JLabel(I18nUtil.getMessage(MessageKeys.TOOLBOX_REGEX_PATTERN) + ":"), BorderLayout.WEST);
        patternField = new JTextField();
        patternField.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        patternField.setToolTipText("Enter regular expression pattern");
        row1.add(patternField, BorderLayout.CENTER);
        topPanel.add(row1);

        // 第二行：标志选项
        JPanel row2 = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        row2.add(new JLabel(I18nUtil.getMessage(MessageKeys.TOOLBOX_REGEX_FLAGS) + ":"));
        caseSensitiveBox = new JCheckBox("Case Sensitive", true);
        multilineBox = new JCheckBox("Multiline (^$ match line)", false);
        dotallBox = new JCheckBox("Dotall (. matches \\n)", false);
        row2.add(caseSensitiveBox);
        row2.add(multilineBox);
        row2.add(dotallBox);
        topPanel.add(row2);

        // 第三行：操作按钮
        JPanel row3 = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        JButton testBtn = new JButton(I18nUtil.getMessage(MessageKeys.TOOLBOX_REGEX_TEST));
        JButton copyBtn = new JButton(I18nUtil.getMessage(MessageKeys.BUTTON_COPY));
        JButton clearBtn = new JButton(I18nUtil.getMessage(MessageKeys.BUTTON_CLEAR));

        row3.add(testBtn);
        row3.add(copyBtn);
        row3.add(clearBtn);
        topPanel.add(row3);

        add(topPanel, BorderLayout.NORTH);

        // 中间：分割面板
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);

        // 测试文本区域
        JPanel textPanel = new JPanel(new BorderLayout(5, 5));
        textPanel.add(new JLabel(I18nUtil.getMessage(MessageKeys.TOOLBOX_REGEX_TEXT) + ":"), BorderLayout.NORTH);
        textArea = new JTextArea();
        textArea.setLineWrap(true);
        textArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        textPanel.add(new JScrollPane(textArea), BorderLayout.CENTER);

        // 结果显示区域
        JPanel resultPanel = new JPanel(new BorderLayout(5, 5));
        JLabel resultLabel = new JLabel(I18nUtil.getMessage(MessageKeys.TOOLBOX_REGEX_RESULT) + ":");
        resultPanel.add(resultLabel, BorderLayout.NORTH);

        // 使用标签页显示不同结果
        JTabbedPane resultTabs = new JTabbedPane();

        // 匹配结果表格
        String[] columns = {"#", "Match", "Start", "End", "Length"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        matchTable = new JTable(tableModel);
        matchTable.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));
        resultTabs.addTab(I18nUtil.getMessage(MessageKeys.TOOLBOX_REGEX_MATCHES), new JScrollPane(matchTable));

        // 详细信息
        resultArea = new JTextArea();
        resultArea.setEditable(false);
        resultArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        resultTabs.addTab("Details", new JScrollPane(resultArea));

        resultPanel.add(resultTabs, BorderLayout.CENTER);

        splitPane.setTopComponent(textPanel);
        splitPane.setBottomComponent(resultPanel);
        splitPane.setDividerLocation(150);

        add(splitPane, BorderLayout.CENTER);

        // 按钮事件
        testBtn.addActionListener(e -> testRegex());
        copyBtn.addActionListener(e -> copyToClipboard());
        clearBtn.addActionListener(e -> {
            patternField.setText("");
            textArea.setText("");
            resultArea.setText("");
            tableModel.setRowCount(0);
        });

        // 回车快速测试
        patternField.addActionListener(e -> testRegex());
    }

    private void testRegex() {
        String patternStr = patternField.getText();
        String text = textArea.getText();

        if (patternStr.isEmpty()) {
            resultArea.setText("⚠️ Please enter a regular expression pattern");
            return;
        }

        if (text.isEmpty()) {
            resultArea.setText("⚠️ Please enter test text");
            return;
        }

        try {
            // 构建正则表达式标志
            int flags = 0;
            if (!caseSensitiveBox.isSelected()) {
                flags |= Pattern.CASE_INSENSITIVE;
            }
            if (multilineBox.isSelected()) {
                flags |= Pattern.MULTILINE;
            }
            if (dotallBox.isSelected()) {
                flags |= Pattern.DOTALL;
            }

            Pattern pattern = Pattern.compile(patternStr, flags);
            Matcher matcher = pattern.matcher(text);

            // 清空之前的结果
            tableModel.setRowCount(0);
            StringBuilder details = new StringBuilder();
            details.append("✅ Pattern compiled successfully\n\n");
            details.append("Pattern: ").append(patternStr).append("\n");
            details.append("Flags: ");
            if (!caseSensitiveBox.isSelected()) details.append("CASE_INSENSITIVE ");
            if (multilineBox.isSelected()) details.append("MULTILINE ");
            if (dotallBox.isSelected()) details.append("DOTALL ");
            details.append("\n\n");

            int count = 0;
            while (matcher.find()) {
                count++;
                String match = matcher.group();
                int start = matcher.start();
                int end = matcher.end();
                int length = match.length();

                // 添加到表格
                tableModel.addRow(new Object[]{count, match, start, end, length});

                // 添加到详细信息
                details.append("Match #").append(count).append(":\n");
                details.append("  Text: \"").append(match).append("\"\n");
                details.append("  Position: [").append(start).append(", ").append(end).append(")\n");
                details.append("  Length: ").append(length).append("\n");

                // 显示捕获组
                if (matcher.groupCount() > 0) {
                    details.append("  Groups:\n");
                    for (int i = 1; i <= matcher.groupCount(); i++) {
                        String group = matcher.group(i);
                        details.append("    Group ").append(i).append(": \"")
                               .append(group != null ? group : "null").append("\"\n");
                    }
                }
                details.append("\n");
            }

            if (count == 0) {
                details.append("❌ No matches found\n");
            } else {
                details.append("📊 Total matches: ").append(count).append("\n");
            }

            resultArea.setText(details.toString());
            resultArea.setCaretPosition(0);

        } catch (PatternSyntaxException ex) {
            resultArea.setText("❌ Invalid regular expression:\n\n" +
                             "Error: " + ex.getDescription() + "\n" +
                             "Position: " + ex.getIndex() + "\n" +
                             "Pattern: " + patternStr);
            tableModel.setRowCount(0);
            log.error("Regex pattern error", ex);
        } catch (Exception ex) {
            resultArea.setText("❌ Error: " + ex.getMessage());
            tableModel.setRowCount(0);
            log.error("Regex test error", ex);
        }
    }

    private void copyToClipboard() {
        String text = resultArea.getText();
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

