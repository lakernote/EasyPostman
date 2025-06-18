package com.laker.postman.panel.collections.edit;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSON;
import cn.hutool.json.JSONUtil;
import com.laker.postman.common.table.FileCellEditor;
import com.laker.postman.common.table.FileCellRenderer;
import com.laker.postman.common.table.TextOrFileTableCellEditor;
import com.laker.postman.common.table.map.EasyNameValueTablePanel;
import com.laker.postman.common.table.map.EasyTablePanel;
import lombok.Getter;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 请求Body相关的独立面板，支持none、form-data、x-www-form-urlencoded、raw
 */
public class RequestBodyPanel extends JPanel {
    public static final String BODY_TYPE_NONE = "none";
    public static final String BODY_TYPE_FORM_DATA = "form-data";
    public static final String BODY_TYPE_FORM_URLENCODED = "x-www-form-urlencoded";
    public static final String BODY_TYPE_RAW = "raw";
    public static final String RAW_TYPE_JSON = "JSON";

    @Getter
    private JComboBox<String> bodyTypeComboBox;
    private JComboBox<String> rawTypeComboBox;
    @Getter
    private EasyTablePanel formDataTablePanel;
    @Getter
    private EasyNameValueTablePanel formUrlencodedTablePanel;
    @Getter
    private JTextArea bodyArea;
    private CardLayout bodyCardLayout;
    private JPanel bodyCardPanel;
    private String currentBodyType = BODY_TYPE_RAW;
    private String currentRawType = RAW_TYPE_JSON;

    public RequestBodyPanel() {
        setLayout(new BorderLayout());
        JPanel bodyTypePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        bodyTypePanel.add(new JLabel("Type:"));
        String[] bodyTypes = {BODY_TYPE_NONE, BODY_TYPE_FORM_DATA, BODY_TYPE_FORM_URLENCODED, BODY_TYPE_RAW};
        bodyTypeComboBox = new JComboBox<>(bodyTypes);
        bodyTypeComboBox.setSelectedItem(currentBodyType);
        bodyTypeComboBox.addActionListener(e -> switchBodyType((String) bodyTypeComboBox.getSelectedItem()));
        bodyTypePanel.add(bodyTypeComboBox);
        bodyTypePanel.add(Box.createHorizontalStrut(10));
        bodyTypePanel.add(new JLabel("Format:"));
        String[] rawTypes = {RAW_TYPE_JSON};
        rawTypeComboBox = new JComboBox<>(rawTypes);
        rawTypeComboBox.setSelectedItem(RAW_TYPE_JSON);
        rawTypeComboBox.addActionListener(e -> currentRawType = (String) rawTypeComboBox.getSelectedItem());
        rawTypeComboBox.setVisible(BODY_TYPE_RAW.equals(currentBodyType));
        bodyTypePanel.add(rawTypeComboBox);
        add(bodyTypePanel, BorderLayout.NORTH);
        bodyCardLayout = new CardLayout();
        bodyCardPanel = new JPanel(bodyCardLayout);
        bodyCardPanel.add(createNonePanel(), BODY_TYPE_NONE);
        bodyCardPanel.add(createFormDataPanel(), BODY_TYPE_FORM_DATA);
        bodyCardPanel.add(createFormUrlencodedPanel(), BODY_TYPE_FORM_URLENCODED);
        bodyCardPanel.add(createRawPanel(), BODY_TYPE_RAW);
        add(bodyCardPanel, BorderLayout.CENTER);
        bodyCardLayout.show(bodyCardPanel, currentBodyType);
    }

    private JPanel createNonePanel() {
        JPanel nonePanel = new JPanel(new BorderLayout());
        nonePanel.add(new JLabel("No Body for this request", SwingConstants.CENTER), BorderLayout.CENTER);
        return nonePanel;
    }

    private JPanel createFormDataPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        String[] columns = {"Key", "Type", "Value"};
        formDataTablePanel = new EasyTablePanel(columns);
        // 设置Type列为下拉框
        JComboBox<String> typeCombo = new JComboBox<>(new String[]{"Text", "File"});
        formDataTablePanel.setColumnEditor(1, new DefaultCellEditor(typeCombo));
        // 设置Value列根据Type动态切换编辑器和渲染器
        formDataTablePanel.setColumnEditor(2, new TextOrFileTableCellEditor());
        formDataTablePanel.setColumnRenderer(2, new TableCellRenderer() {
            private final FileCellRenderer fileRenderer = new FileCellRenderer();
            private final DefaultTableCellRenderer textRenderer = new DefaultTableCellRenderer();

            @Override
            public java.awt.Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Object type = table.getValueAt(row, 1);
                if ("File".equals(type)) {
                    return fileRenderer.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                } else {
                    return textRenderer.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                }
            }
        });
        panel.add(formDataTablePanel, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createFormUrlencodedPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        formUrlencodedTablePanel = new EasyNameValueTablePanel("Key", "Value");
        panel.add(formUrlencodedTablePanel, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createRawPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        bodyArea = new JTextArea(5, 20);
        bodyArea.setLineWrap(true);
        JScrollPane scrollPane = new JScrollPane(bodyArea);
        panel.add(scrollPane, BorderLayout.CENTER);
        JButton formatButton = new JButton("Format Body");
        formatButton.addActionListener(e -> formatBody());
        panel.add(formatButton, BorderLayout.SOUTH);
        return panel;
    }

    private void switchBodyType(String bodyType) {
        currentBodyType = bodyType;
        bodyCardLayout.show(bodyCardPanel, bodyType);
        rawTypeComboBox.setVisible(BODY_TYPE_RAW.equals(bodyType));
    }

    private void formatBody() {
        if (!BODY_TYPE_RAW.equals(currentBodyType)) {
            JOptionPane.showMessageDialog(this, "Only Raw type Body can be formatted");
            return;
        }
        String bodyText = bodyArea.getText();
        if (StrUtil.isBlank(bodyText)) {
            JOptionPane.showMessageDialog(this, "Body is empty, cannot format");
            return;
        }
        if (RAW_TYPE_JSON.equals(currentRawType)) {
            if (JSONUtil.isTypeJSON(bodyText)) {
                JSON json = JSONUtil.parse(bodyText);
                bodyArea.setText(JSONUtil.toJsonPrettyStr(json));
            } else {
                JOptionPane.showMessageDialog(this, "Body is not valid JSON, cannot format");
            }
        }
    }

    // getter方法，供主面板调用
    public String getBodyType() {
        return currentBodyType;
    }

    // 新增：获取raw body内容
    public String getRawBody() {
        return bodyArea != null ? bodyArea.getText().trim() : null;
    }

    // ��增：获取form-data内容
    public Map<String, String> getFormData() {
        Map<String, String> formData = new LinkedHashMap<>();
        if (formDataTablePanel != null) {
            java.util.List<java.util.Map<String, Object>> rows = formDataTablePanel.getRows();
            for (java.util.Map<String, Object> row : rows) {
                String key = row.get("Key") == null ? null : row.get("Key").toString();
                String type = row.get("Type") == null ? null : row.get("Type").toString();
                String value = row.get("Value") == null ? null : row.get("Value").toString();
                if (key != null && !key.trim().isEmpty() && "Text".equals(type)) {
                    formData.put(key.trim(), value);
                }
            }
        }
        return formData;
    }

    // 新增：获取form-data文件内容
    public Map<String, String> getFormFiles() {
        Map<String, String> formFiles = new LinkedHashMap<>();
        if (formDataTablePanel != null) {
            java.util.List<java.util.Map<String, Object>> rows = formDataTablePanel.getRows();
            for (java.util.Map<String, Object> row : rows) {
                String key = row.get("Key") == null ? null : row.get("Key").toString();
                String type = row.get("Type") == null ? null : row.get("Type").toString();
                String value = row.get("Value") == null ? null : row.get("Value").toString();
                if (key != null && !key.trim().isEmpty() && "File".equals(type) && value != null && !value.equals(FileCellEditor.COLUMN_TEXT)) {
                    formFiles.put(key.trim(), value);
                }
            }
        }
        return formFiles;
    }

    // 新增：��取x-www-form-urlencoded内容
    public String getFormUrlencodedBody() {
        if (formUrlencodedTablePanel == null) return null;
        StringBuilder sb = new StringBuilder();
        java.util.Map<String, String> map = formUrlencodedTablePanel.getMap();
        for (Map.Entry<String, String> entry : map.entrySet()) {
            if (!sb.isEmpty()) sb.append("&");
            sb.append(entry.getKey()).append("=").append(entry.getValue() == null ? "" : entry.getValue());
        }
        return sb.toString();
    }
}