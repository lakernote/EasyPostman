package com.laker.postman.panel.toolbox;

import com.formdev.flatlaf.extras.components.FlatTextField;
import com.laker.postman.util.CronExpressionUtil;
import com.laker.postman.util.EasyPostManFontUtil;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.util.Date;
import java.util.List;

/**
 * Cron表达式工具面板
 */
@Slf4j
public class CronPanel extends JPanel {

    private FlatTextField cronField;
    private JTextArea descriptionArea;
    private JTable nextExecutionTable;
    private DefaultTableModel tableModel;

    // Cron字段选择器
    private JComboBox<String> secondCombo;
    private JComboBox<String> minuteCombo;
    private JComboBox<String> hourCombo;
    private JComboBox<String> dayCombo;
    private JComboBox<String> monthCombo;
    private JComboBox<String> weekCombo;
    private FlatTextField yearField;

    public CronPanel() {
        initUI();
    }

    private void initUI() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // 创建标签页
        JTabbedPane tabbedPane = new JTabbedPane();

        // Tab 1: Cron表达式解析
        tabbedPane.addTab("Parse Cron", createParsePanel());

        // Tab 2: Cron表达式生成
        tabbedPane.addTab("Generate Cron", createGeneratePanel());

        add(tabbedPane, BorderLayout.CENTER);
    }

    private JPanel createParsePanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // 顶部：Cron输入
        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));

        // 第一行：Cron表达式输入
        JPanel row1 = new JPanel(new BorderLayout(5, 5));
        row1.add(new JLabel(I18nUtil.getMessage(MessageKeys.TOOLBOX_CRON_EXPRESSION) + ":"), BorderLayout.WEST);
        cronField = new FlatTextField();
        cronField.setFont(EasyPostManFontUtil.getDefaultFont(Font.PLAIN, 14));
        cronField.setText("0 0 12 * * ?");
        cronField.setPlaceholderText("Enter cron expression (e.g. 0 0 12 * * ?)");
        cronField.setBackground(Color.WHITE);
        row1.add(cronField, BorderLayout.CENTER);
        topPanel.add(row1);

        topPanel.add(Box.createVerticalStrut(5));

        // 第二行：操作按钮
        JPanel row2 = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        JButton parseBtn = new JButton(I18nUtil.getMessage(MessageKeys.TOOLBOX_CRON_PARSE));
        JButton copyBtn = new JButton(I18nUtil.getMessage(MessageKeys.BUTTON_COPY));
        JButton clearBtn = new JButton(I18nUtil.getMessage(MessageKeys.BUTTON_CLEAR));

        row2.add(parseBtn);
        row2.add(copyBtn);
        row2.add(clearBtn);
        topPanel.add(row2);

        panel.add(topPanel, BorderLayout.NORTH);

        // 中间：分割面板
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);

        // 描述区域
        JPanel descPanel = new JPanel(new BorderLayout(5, 5));
        descPanel.add(new JLabel(I18nUtil.getMessage(MessageKeys.TOOLBOX_CRON_DESCRIPTION) + ":"), BorderLayout.NORTH);
        descriptionArea = new JTextArea();
        descriptionArea.setEditable(false);
        descriptionArea.setLineWrap(true);
        descriptionArea.setWrapStyleWord(true);
        descPanel.add(new JScrollPane(descriptionArea), BorderLayout.CENTER);

        // 下次执行时间表格
        JPanel tablePanel = new JPanel(new BorderLayout(5, 5));
        tablePanel.add(new JLabel(I18nUtil.getMessage(MessageKeys.TOOLBOX_CRON_NEXT_EXECUTIONS) + ":"), BorderLayout.NORTH);
        String[] columns = {"#", "Execution Time"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        nextExecutionTable = new JTable(tableModel);
        tablePanel.add(new JScrollPane(nextExecutionTable), BorderLayout.CENTER);

        splitPane.setTopComponent(descPanel);
        splitPane.setBottomComponent(tablePanel);
        splitPane.setDividerLocation(250);

        panel.add(splitPane, BorderLayout.CENTER);

        // 按钮事件
        parseBtn.addActionListener(e -> parseCron());
        copyBtn.addActionListener(e -> copyToClipboard(cronField.getText()));
        clearBtn.addActionListener(e -> {
            cronField.setText("");
            descriptionArea.setText("");
            tableModel.setRowCount(0);
        });

        cronField.addActionListener(e -> parseCron());

        return panel;
    }

    private JPanel createGeneratePanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // 主面板
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));

        // Cron字段说明
        JPanel infoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JLabel infoLabel = new JLabel("<html><b>Cron Format:</b> Second Minute Hour Day Month Week [Year]</html>");
        infoPanel.add(infoLabel);
        mainPanel.add(infoPanel);

        mainPanel.add(Box.createVerticalStrut(10));

        // 秒
        mainPanel.add(createFieldPanel("Second (0-59):",
                secondCombo = new JComboBox<>(new String[]{"*", "0", "15", "30", "45", "0/5", "0/10", "0/15"})));

        // 分钟
        mainPanel.add(createFieldPanel("Minute (0-59):",
                minuteCombo = new JComboBox<>(new String[]{"*", "0", "15", "30", "45", "0/5", "0/10", "0/15"})));

        // 小时
        mainPanel.add(createFieldPanel("Hour (0-23):",
                hourCombo = new JComboBox<>(new String[]{"*", "0", "6", "12", "18", "0/2", "0/4", "0/6"})));

        // 日
        mainPanel.add(createFieldPanel("Day (1-31):",
                dayCombo = new JComboBox<>(new String[]{"*", "?", "1", "15", "L", "1-15", "*/2"})));

        // 月
        mainPanel.add(createFieldPanel("Month (1-12):",
                monthCombo = new JComboBox<>(new String[]{"*", "1", "6", "12", "1-6", "*/2", "*/3"})));

        // 周
        mainPanel.add(createFieldPanel("Week (0-7):",
                weekCombo = new JComboBox<>(new String[]{"?", "*", "1", "2", "3", "4", "5", "6", "7", "MON-FRI"})));

        // 年（可选）
        JPanel yearPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        yearPanel.add(new JLabel("Year (optional):"));
        yearField = new FlatTextField();
        yearField.setColumns(10);
        yearField.setText("");
        yearField.setPlaceholderText("Enter year (optional):");
        yearPanel.add(yearField);
        mainPanel.add(yearPanel);

        mainPanel.add(Box.createVerticalStrut(10));

        // 生成按钮和结果
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        JButton generateBtn = new JButton(I18nUtil.getMessage(MessageKeys.TOOLBOX_CRON_GENERATE));
        JButton copyGenBtn = new JButton(I18nUtil.getMessage(MessageKeys.BUTTON_COPY));
        JButton presetBtn = new JButton("Quick Presets");

        buttonPanel.add(generateBtn);
        buttonPanel.add(copyGenBtn);
        buttonPanel.add(presetBtn);
        mainPanel.add(buttonPanel);

        mainPanel.add(Box.createVerticalStrut(10));

        // 生成的Cron表达式
        JPanel resultPanel = new JPanel(new BorderLayout(5, 5));
        resultPanel.add(new JLabel("Generated Cron:"), BorderLayout.WEST);
        JTextField generatedField = new JTextField();
        generatedField.setEditable(false);
        generatedField.setFont(EasyPostManFontUtil.getDefaultFont(Font.BOLD, 14));
        resultPanel.add(generatedField, BorderLayout.CENTER);
        mainPanel.add(resultPanel);

        panel.add(mainPanel, BorderLayout.NORTH);

        // 常用预设
        JPanel presetPanel = createPresetPanel();
        panel.add(presetPanel, BorderLayout.CENTER);

        // 按钮事件
        generateBtn.addActionListener(e -> {
            String cron = generateCronExpression();
            generatedField.setText(cron);
            cronField.setText(cron);
        });

        copyGenBtn.addActionListener(e -> copyToClipboard(generatedField.getText()));

        presetBtn.addActionListener(e -> showPresetMenu(presetBtn, generatedField));

        return panel;
    }

    private JPanel createFieldPanel(String label, JComboBox<String> combo) {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        JLabel fieldLabel = new JLabel(label);
        fieldLabel.setPreferredSize(new Dimension(150, 25));
        panel.add(fieldLabel);
        combo.setEditable(true);
        combo.setPreferredSize(new Dimension(200, 25));
        panel.add(combo);
        return panel;
    }

    private JPanel createPresetPanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(BorderFactory.createTitledBorder("Common Presets"));

        String[][] presets = {
                {"Every second", "* * * * * ?"},
                {"Every minute", "0 * * * * ?"},
                {"Every 5 minutes", "0 */5 * * * ?"},
                {"Every 15 minutes", "0 */15 * * * ?"},
                {"Every 30 minutes", "0 */30 * * * ?"},
                {"Every hour", "0 0 * * * ?"},
                {"Every 2 hours", "0 0 */2 * * ?"},
                {"Every day at noon", "0 0 12 * * ?"},
                {"Every day at midnight", "0 0 0 * * ?"},
                {"Every Monday at 9 AM", "0 0 9 ? * MON"},
                {"Every weekday at 9 AM", "0 0 9 ? * MON-FRI"},
                {"First day of month", "0 0 0 1 * ?"},
                {"Last day of month", "0 0 0 L * ?"}
        };

        JTextArea presetArea = new JTextArea(15, 50);
        presetArea.setEditable(false);

        StringBuilder sb = new StringBuilder();
        sb.append("Common Cron Expressions:\n\n");
        for (String[] preset : presets) {
            sb.append(String.format("%-25s : %s\n", preset[0], preset[1]));
        }
        sb.append("\n");
        sb.append("Special Characters:\n");
        sb.append("  *  - All values\n");
        sb.append("  ?  - No specific value (Day/Week)\n");
        sb.append("  -  - Range (e.g., 1-5)\n");
        sb.append("  ,  - List (e.g., 1,3,5)\n");
        sb.append("  /  - Increment (e.g., 0/15)\n");
        sb.append("  L  - Last (e.g., L = last day of month)\n");
        sb.append("  W  - Weekday (e.g., 15W = nearest weekday to 15th)\n");
        sb.append("  #  - Nth day (e.g., 2#1 = first Monday)\n");

        presetArea.setText(sb.toString());
        panel.add(new JScrollPane(presetArea), BorderLayout.CENTER);

        return panel;
    }

    private void showPresetMenu(JButton button, JTextField targetField) {
        JPopupMenu menu = new JPopupMenu();

        String[][] presets = {
                {"Every minute", "0 * * * * ?"},
                {"Every 5 minutes", "0 */5 * * * ?"},
                {"Every hour", "0 0 * * * ?"},
                {"Every day at noon", "0 0 12 * * ?"},
                {"Every day at midnight", "0 0 0 * * ?"},
                {"Every weekday at 9 AM", "0 0 9 ? * MON-FRI"}
        };

        for (String[] preset : presets) {
            JMenuItem item = new JMenuItem(preset[0] + " → " + preset[1]);
            item.addActionListener(e -> {
                targetField.setText(preset[1]);
                cronField.setText(preset[1]);
                parseCron();
            });
            menu.add(item);
        }

        menu.show(button, 0, button.getHeight());
    }

    private String generateCronExpression() {
        StringBuilder cron = new StringBuilder();
        cron.append(secondCombo.getSelectedItem()).append(" ");
        cron.append(minuteCombo.getSelectedItem()).append(" ");
        cron.append(hourCombo.getSelectedItem()).append(" ");
        cron.append(dayCombo.getSelectedItem()).append(" ");
        cron.append(monthCombo.getSelectedItem()).append(" ");
        cron.append(weekCombo.getSelectedItem());

        String year = yearField.getText().trim();
        if (!year.isEmpty()) {
            cron.append(" ").append(year);
        }

        return cron.toString();
    }

    private void parseCron() {
        String cronExpression = cronField.getText().trim();

        if (cronExpression.isEmpty()) {
            descriptionArea.setText("Please enter a Cron expression");
            tableModel.setRowCount(0);
            return;
        }

        try {
            // 验证Cron表达式
            if (!CronExpressionUtil.isValid(cronExpression)) {
                descriptionArea.setText("❌ Invalid Cron expression. Expected format:\nSecond Minute Hour Day Month Week [Year]");
                tableModel.setRowCount(0);
                return;
            }

            // 解析Cron表达式
            String[] parts = cronExpression.split("\\s+");

            // 生成描述
            StringBuilder desc = new StringBuilder();
            desc.append("✅ Cron Expression Analysis:\n\n");
            desc.append("Expression: ").append(cronExpression).append("\n\n");
            desc.append("Fields:\n");
            desc.append("  Second: ").append(parts[0]).append("\n");
            desc.append("  Minute: ").append(parts[1]).append("\n");
            desc.append("  Hour:   ").append(parts[2]).append("\n");
            desc.append("  Day:    ").append(parts[3]).append("\n");
            desc.append("  Month:  ").append(parts[4]).append("\n");
            desc.append("  Week:   ").append(parts[5]).append("\n");
            if (parts.length > 6) {
                desc.append("  Year:   ").append(parts[6]).append("\n");
            }

            desc.append("\nDescription:\n");
            desc.append(CronExpressionUtil.describe(cronExpression));

            descriptionArea.setText(desc.toString());
            descriptionArea.setCaretPosition(0);

            // 计算下次执行时间
            calculateNextExecutions(cronExpression);

        } catch (Exception ex) {
            descriptionArea.setText("❌ Error parsing Cron expression:\n" + ex.getMessage());
            tableModel.setRowCount(0);
            log.error("Cron parse error", ex);
        }
    }

    private void calculateNextExecutions(String cronExpression) {
        tableModel.setRowCount(0);

        try {
            // 使用工具类计算下次执行时间
            List<Date> executionTimes = CronExpressionUtil.getNextExecutionTimes(cronExpression, 10);

            int index = 1;
            for (Date time : executionTimes) {
                tableModel.addRow(new Object[]{
                        index++,
                        CronExpressionUtil.formatDate(time)
                });
            }

            if (executionTimes.isEmpty()) {
                tableModel.addRow(new Object[]{"N/A", "Unable to calculate execution times"});
            }

        } catch (Exception ex) {
            log.error("Calculate next executions error", ex);
            tableModel.addRow(new Object[]{"Error", ex.getMessage()});
        }
    }

    private void copyToClipboard(String text) {
        if (!text.isEmpty()) {
            Toolkit.getDefaultToolkit().getSystemClipboard()
                    .setContents(new StringSelection(text), null);
            JOptionPane.showMessageDialog(this,
                    "Copied: " + text,
                    I18nUtil.getMessage(MessageKeys.TIP),
                    JOptionPane.INFORMATION_MESSAGE);
        }
    }
}
