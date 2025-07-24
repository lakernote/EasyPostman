package com.laker.postman.panel.collections.edit;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSON;
import cn.hutool.json.JSONUtil;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.laker.postman.common.table.TextOrFileTableCellEditor;
import com.laker.postman.common.table.TextOrFileTableCellRenderer;
import com.laker.postman.common.table.map.EasyNameValueTablePanel;
import com.laker.postman.common.table.map.EasyTablePanel;
import lombok.Getter;

import javax.swing.*;
import java.awt.*;
import java.util.LinkedHashMap;
import java.util.Map;

import static com.laker.postman.common.table.TableUIConstants.SELECT_FILE_TEXT;

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
    private final JComboBox<String> bodyTypeComboBox;
    private final JLabel formatLabel;
    private final JComboBox<String> rawTypeComboBox;
    @Getter
    private EasyTablePanel formDataTablePanel;
    @Getter
    private EasyNameValueTablePanel formUrlencodedTablePanel;
    @Getter
    private JTextArea bodyArea;
    private final CardLayout bodyCardLayout;
    private final JPanel bodyCardPanel;
    private String currentBodyType = BODY_TYPE_RAW;
    private String currentRawType = RAW_TYPE_JSON;
    @Getter
    private JButton wsSendButton;
    private final JLabel bodyTypeLabel;
    private final JButton formatButton;

    public RequestBodyPanel() {
        setLayout(new BorderLayout());
        JPanel bodyTypePanel = new JPanel(new BorderLayout());
        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        bodyTypeLabel = new JLabel("Type:");
        leftPanel.add(bodyTypeLabel);
        String[] bodyTypes = {BODY_TYPE_NONE, BODY_TYPE_FORM_DATA, BODY_TYPE_FORM_URLENCODED, BODY_TYPE_RAW};
        bodyTypeComboBox = new JComboBox<>(bodyTypes);
        bodyTypeComboBox.setSelectedItem(currentBodyType);
        bodyTypeComboBox.addActionListener(e -> switchBodyType((String) bodyTypeComboBox.getSelectedItem()));
        leftPanel.add(bodyTypeComboBox);
        leftPanel.add(Box.createHorizontalStrut(10));
        formatLabel = new JLabel("Format:");
        leftPanel.add(formatLabel);
        String[] rawTypes = {RAW_TYPE_JSON};
        rawTypeComboBox = new JComboBox<>(rawTypes);
        rawTypeComboBox.setSelectedItem(RAW_TYPE_JSON);
        rawTypeComboBox.setVisible(isBodyTypeRAW());
        formatLabel.setVisible(isBodyTypeRAW());
        leftPanel.add(rawTypeComboBox);
        bodyTypePanel.add(leftPanel, BorderLayout.WEST);
        // 创建 formatButton 并放在右侧
        formatButton = new JButton(new FlatSVGIcon("icons/format.svg", 20, 20));
        formatButton.addActionListener(e -> formatBody());
        formatButton.setVisible(isBodyTypeRAW());
        formatButton.setPreferredSize(new Dimension(32, 32)); // 推荐比图标略大
        bodyTypePanel.add(formatButton, BorderLayout.EAST);
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

    private boolean isBodyTypeRAW() {
        return BODY_TYPE_RAW.equals(currentBodyType);
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
        formDataTablePanel.setColumnRenderer(2, new TextOrFileTableCellRenderer());
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
        bodyArea.setBackground(Color.WHITE);
        bodyArea.setLineWrap(true);
        JScrollPane scrollPane = new JScrollPane(bodyArea);
        panel.add(scrollPane, BorderLayout.CENTER);
        JPanel bottomPanel = new JPanel(new BorderLayout());
        // formatButton 已经在顶部面板创建并添加，这里无需再创建
        wsSendButton = new JButton("Send Message");
        wsSendButton.setVisible(false);
        bottomPanel.add(wsSendButton, BorderLayout.EAST);
        panel.add(bottomPanel, BorderLayout.SOUTH);
        return panel;
    }

    private void switchBodyType(String bodyType) {
        currentBodyType = bodyType;
        bodyCardLayout.show(bodyCardPanel, bodyType);
        boolean isRaw = BODY_TYPE_RAW.equals(bodyType);
        rawTypeComboBox.setVisible(isRaw);
        formatLabel.setVisible(isRaw);
        formatButton.setVisible(isRaw);
    }

    private void formatBody() {
        if (!isBodyTypeRAW()) {
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


    public String getRawBody() {
        return bodyArea != null ? bodyArea.getText().trim() : null;
    }

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

    public Map<String, String> getFormFiles() {
        Map<String, String> formFiles = new LinkedHashMap<>();
        if (formDataTablePanel != null) {
            java.util.List<Map<String, Object>> rows = formDataTablePanel.getRows();
            for (java.util.Map<String, Object> row : rows) {
                String key = row.get("Key") == null ? null : row.get("Key").toString();
                String type = row.get("Type") == null ? null : row.get("Type").toString();
                String value = row.get("Value") == null ? null : row.get("Value").toString();
                if (key != null && !key.trim().isEmpty() && "File".equals(type) && value != null && !value.equals(SELECT_FILE_TEXT)) {
                    formFiles.put(key.trim(), value);
                }
            }
        }
        return formFiles;
    }

    public Map<String, String> getUrlencoded() {
        if (formUrlencodedTablePanel == null) return null;
        return formUrlencodedTablePanel.getMap();
    }

    // 切换到WebSocket消息发送面板
    public void showWebSocketSendPanel(boolean show) {
        if (show) {
            // 自动切换到Body Tab的 type=raw
            bodyTypeComboBox.setSelectedItem(BODY_TYPE_RAW);
            currentBodyType = BODY_TYPE_RAW;
            bodyCardLayout.show(bodyCardPanel, BODY_TYPE_RAW);
            rawTypeComboBox.setSelectedItem(RAW_TYPE_JSON);
            currentRawType = RAW_TYPE_JSON;
            wsSendButton.setVisible(true);
            formatLabel.setVisible(false);
            rawTypeComboBox.setVisible(false);
            bodyTypeComboBox.setVisible(false);
            bodyTypeLabel.setVisible(false);
            formatButton.setVisible(false);
        } else {
            wsSendButton.setVisible(false);
            bodyTypeComboBox.setVisible(true);
            bodyTypeLabel.setVisible(true);
        }
    }
}