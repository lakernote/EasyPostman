package com.laker.postman.panel.collections.right.request.sub;

import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.json.JSON;
import cn.hutool.json.JSONUtil;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.laker.postman.common.table.TextOrFileTableCellEditor;
import com.laker.postman.common.table.TextOrFileTableCellRenderer;
import com.laker.postman.common.table.map.EasyNameValueTablePanel;
import com.laker.postman.common.table.map.EasyTablePanel;
import com.laker.postman.model.RequestItemProtocolEnum;
import com.laker.postman.model.VariableSegment;
import com.laker.postman.util.EasyPostmanVariableUtil;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;
import lombok.Getter;
import lombok.Setter;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rsyntaxtextarea.Theme;
import org.fife.ui.rtextarea.RTextScrollPane;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.MouseInputAdapter;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.List;
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
    private JComboBox<String> bodyTypeComboBox;
    private JLabel formatLabel;
    private JComboBox<String> rawTypeComboBox;
    @Getter
    private EasyTablePanel formDataTablePanel;
    @Getter
    private EasyNameValueTablePanel formUrlencodedTablePanel;
    @Getter
    private RSyntaxTextArea bodyArea;
    private CardLayout bodyCardLayout;
    private JPanel bodyCardPanel;
    private String currentBodyType = BODY_TYPE_RAW;
    @Getter
    private JButton wsSendButton;
    private JButton formatButton;
    private final boolean isWebSocketMode;

    private Timer wsTimer; // 定时发送用
    private JButton wsTimedSendButton; // 定时发送按钮
    private JTextField wsIntervalField; // 定时间隔输入框
    private JCheckBox wsClearInputCheckBox; // 清空输入复选框

    @Setter
    private transient ActionListener wsSendActionListener; // 外部注入的发送回调

    public RequestBodyPanel(RequestItemProtocolEnum protocol) {
        this.isWebSocketMode = protocol.isWebSocketProtocol();
        setLayout(new BorderLayout());
        if (isWebSocketMode) {
            initWebSocketBodyPanel();
        } else {
            initHttpBodyPanel();
        }
    }

    /**
     * 初始化 HTTP 模式下的 Body 面板
     */
    private void initHttpBodyPanel() {
        JPanel bodyTypePanel = new JPanel(new BorderLayout());
        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JLabel bodyTypeLabel = new JLabel(I18nUtil.getMessage(MessageKeys.REQUEST_BODY_TYPE));
        leftPanel.add(bodyTypeLabel);
        String[] bodyTypes = new String[]{BODY_TYPE_NONE, BODY_TYPE_FORM_DATA, BODY_TYPE_FORM_URLENCODED, BODY_TYPE_RAW};
        bodyTypeComboBox = new JComboBox<>(bodyTypes);
        bodyTypeComboBox.setSelectedItem(currentBodyType);
        bodyTypeComboBox.addActionListener(e -> switchBodyType((String) bodyTypeComboBox.getSelectedItem()));
        leftPanel.add(bodyTypeComboBox);
        leftPanel.add(Box.createHorizontalStrut(10));
        formatLabel = new JLabel(I18nUtil.getMessage(MessageKeys.REQUEST_BODY_FORMAT));
        String[] rawTypes = {RAW_TYPE_JSON};
        rawTypeComboBox = new JComboBox<>(rawTypes);
        rawTypeComboBox.setSelectedItem(RAW_TYPE_JSON);
        boolean showFormatControls = isBodyTypeRAW();
        rawTypeComboBox.setVisible(showFormatControls);
        formatLabel.setVisible(showFormatControls);
        leftPanel.add(formatLabel);
        leftPanel.add(rawTypeComboBox);
        formatButton = new JButton(new FlatSVGIcon("icons/format.svg", 20, 20));
        formatButton.addActionListener(e -> formatBody());
        formatButton.setVisible(isBodyTypeRAW());
        leftPanel.add(Box.createHorizontalStrut(5));
        leftPanel.add(formatButton);
        bodyTypePanel.add(leftPanel, BorderLayout.WEST);
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

    /**
     * 初始化 WebSocket 模式下的 Body 面板
     */
    private void initWebSocketBodyPanel() {
        JPanel bodyTypePanel = new JPanel(new BorderLayout());
        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JLabel bodyTypeLabel = new JLabel(I18nUtil.getMessage(MessageKeys.WEBSOCKET_PANEL_LABEL_SEND_MESSAGE));
        leftPanel.add(bodyTypeLabel);
        String[] bodyTypes = new String[]{BODY_TYPE_RAW};
        bodyTypeComboBox = new JComboBox<>(bodyTypes);
        bodyTypeComboBox.setSelectedItem(BODY_TYPE_RAW);
        bodyTypeComboBox.setVisible(false);
        leftPanel.add(bodyTypeComboBox);
        formatLabel = null;
        rawTypeComboBox = null;
        formatButton = null;
        bodyTypePanel.add(leftPanel, BorderLayout.WEST);
        add(bodyTypePanel, BorderLayout.NORTH);
        bodyCardLayout = new CardLayout();
        bodyCardPanel = new JPanel(bodyCardLayout);
        JPanel rawPanel = createRawPanel();
        bodyCardPanel.add(rawPanel, BODY_TYPE_RAW);
        add(bodyCardPanel, BorderLayout.CENTER);
        bodyCardLayout.show(bodyCardPanel, BODY_TYPE_RAW);
        // WebSocket底部操作按钮
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        wsClearInputCheckBox = new JCheckBox(I18nUtil.getMessage(MessageKeys.WEBSOCKET_PANEL_CHECKBOX_CLEAR));
        bottomPanel.add(wsClearInputCheckBox);
        bottomPanel.add(new JLabel(I18nUtil.getMessage(MessageKeys.WEBSOCKET_PANEL_LABEL_TIMEOUT)));
        wsIntervalField = new JTextField("1000", 5); // 默认1000ms
        bottomPanel.add(wsIntervalField);
        wsTimedSendButton = new JButton(I18nUtil.getMessage(MessageKeys.WEBSOCKET_PANEL_BUTTON_START));
        wsTimedSendButton.setIcon(new FlatSVGIcon("icons/time.svg", 16, 16));
        wsTimedSendButton.addActionListener(e -> toggleWsTimer());
        bottomPanel.add(wsTimedSendButton);
        wsSendButton = new JButton(I18nUtil.getMessage(MessageKeys.WEBSOCKET_PANEL_BUTTON_SEND));
        wsSendButton.setIcon(new FlatSVGIcon("icons/send.svg", 16, 16));
        wsSendButton.setVisible(true);
        wsSendButton.addActionListener(e -> wsSendAndMaybeClear());
        bottomPanel.add(wsSendButton);
        add(bottomPanel, BorderLayout.SOUTH);
    }

    private boolean isBodyTypeRAW() {
        return BODY_TYPE_RAW.equals(currentBodyType);
    }

    private JPanel createNonePanel() {
        JPanel nonePanel = new JPanel(new BorderLayout());
        nonePanel.add(new JLabel(I18nUtil.getMessage(MessageKeys.REQUEST_BODY_NONE), SwingConstants.CENTER), BorderLayout.CENTER);
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
        bodyArea = new RSyntaxTextArea(5, 20);
        bodyArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JSON); // 默认JSON高亮
        bodyArea.setCodeFoldingEnabled(true); // 启用代码折叠
        bodyArea.setLineWrap(true); // 自动换行
        // 设置主题
        try (InputStream in = getClass().getResourceAsStream("/org/fife/ui/rsyntaxtextarea/themes/vs.xml")) {
            if (in != null) {
                Theme theme = Theme.load(in);
                theme.apply(bodyArea);
            }
        } catch (Exception ignored) {
        }
        RTextScrollPane scrollPane = new RTextScrollPane(bodyArea); // 使用RSyntaxTextArea的滚动面板 显示行号
        panel.add(scrollPane, BorderLayout.CENTER);

        // ====== 变量高亮和悬浮提示 ======
        // 变量高亮
        DefaultHighlighter highlighter = (DefaultHighlighter) bodyArea.getHighlighter();
        DefaultHighlighter.DefaultHighlightPainter definedPainter = new DefaultHighlighter.DefaultHighlightPainter(new Color(180, 210, 255, 120));
        DefaultHighlighter.DefaultHighlightPainter undefinedPainter = new DefaultHighlighter.DefaultHighlightPainter(new Color(255, 200, 200, 120));
        bodyArea.getDocument().addDocumentListener(new DocumentListener() {
            void updateHighlights() {
                highlighter.removeAllHighlights();
                String text = bodyArea.getText();
                java.util.List<VariableSegment> segments = EasyPostmanVariableUtil.getVariableSegments(text);
                for (VariableSegment seg : segments) {
                    boolean isDefined = EasyPostmanVariableUtil.isVariableDefined(seg.name);
                    try {
                        highlighter.addHighlight(seg.start, seg.end, isDefined ? definedPainter : undefinedPainter);
                    } catch (BadLocationException ignored) {
                    }
                }
            }

            public void insertUpdate(DocumentEvent e) {
                updateHighlights();
            }

            public void removeUpdate(DocumentEvent e) {
                updateHighlights();
            }

            public void changedUpdate(DocumentEvent e) {
                updateHighlights();
            }
        });
        // 初始化高亮
        SwingUtilities.invokeLater(() -> {
            String text = bodyArea.getText();
            java.util.List<VariableSegment> segments = EasyPostmanVariableUtil.getVariableSegments(text);
            for (VariableSegment seg : segments) {
                boolean isDefined = EasyPostmanVariableUtil.isVariableDefined(seg.name);
                try {
                    highlighter.addHighlight(seg.start, seg.end, isDefined ? definedPainter : undefinedPainter);
                } catch (BadLocationException ignored) {
                }
            }
        });
        // 悬浮提示
        bodyArea.addMouseMotionListener(new MouseInputAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                int pos = bodyArea.viewToModel2D(e.getPoint());
                String text = bodyArea.getText();
                java.util.List<VariableSegment> segments = EasyPostmanVariableUtil.getVariableSegments(text);
                for (VariableSegment seg : segments) {
                    if (pos >= seg.start && pos <= seg.end) {
                        String varName = seg.name;
                        String varValue = EasyPostmanVariableUtil.getVariableValue(varName);
                        if (varValue != null) {
                            bodyArea.setToolTipText(varName + " = " + varValue);
                        } else {
                            bodyArea.setToolTipText("[" + varName + "] not found");
                        }
                        return;
                    }
                }
                bodyArea.setToolTipText(null);
            }
        });

        return panel;
    }

    // WebSocket发送并根据checkbox清空输入
    private void wsSendAndMaybeClear() {
        if (wsSendActionListener != null) {
            wsSendActionListener.actionPerformed(new ActionEvent(wsSendButton, ActionEvent.ACTION_PERFORMED, null));
        }
        if (wsClearInputCheckBox != null && wsClearInputCheckBox.isSelected()) {
            bodyArea.setText("");
        }
    }

    // 定时发送逻辑
    private void toggleWsTimer() {
        // 只有已连接WebSocket时才能启动定时器
        if (wsSendButton == null || !wsSendButton.isEnabled()) {
            // 未连接时，直接返回，不允许启动定时器
            return;
        }
        if (wsTimer != null && wsTimer.isRunning()) {
            wsTimer.stop();
            wsTimedSendButton.setText(I18nUtil.getMessage(MessageKeys.WEBSOCKET_PANEL_BUTTON_START));
            wsIntervalField.setEnabled(true);
            wsClearInputCheckBox.setEnabled(true);
            wsSendButton.setEnabled(true);
        } else {
            int interval = 1000;
            try {
                interval = Integer.parseInt(wsIntervalField.getText().trim());
                if (interval < 100) interval = 100; // 最小100ms
            } catch (Exception ignored) {
            }
            wsTimer = new Timer(interval, e -> {
                if (wsSendButton.isEnabled()) {
                    wsSendAndMaybeClear();
                }
            });
            wsTimer.start();
            wsTimedSendButton.setText(I18nUtil.getMessage(MessageKeys.WEBSOCKET_PANEL_BUTTON_STOP));
            wsIntervalField.setEnabled(false);
            wsClearInputCheckBox.setEnabled(false);
            wsSendButton.setEnabled(true);
        }
    }

    /**
     * WebSocket连接状态变化时调用，控制发送和定时按钮的可用性
     *
     * @param connected 是否已连接
     */
    public void setWebSocketConnected(boolean connected) {
        if (wsSendButton != null) wsSendButton.setEnabled(connected);
        if (wsTimedSendButton != null) wsTimedSendButton.setEnabled(connected);
    }

    private void switchBodyType(String bodyType) {
        currentBodyType = bodyType;
        bodyCardLayout.show(bodyCardPanel, bodyType);
        // 只有HTTP模式才需要动态调整format控件的显示
        if (!isWebSocketMode && formatLabel != null && rawTypeComboBox != null && formatButton != null) {
            boolean isRaw = BODY_TYPE_RAW.equals(bodyType);
            rawTypeComboBox.setVisible(isRaw);
            formatLabel.setVisible(isRaw);
            formatButton.setVisible(isRaw);
        }
    }

    private void formatBody() {
        if (!isBodyTypeRAW()) {
            JOptionPane.showMessageDialog(this, I18nUtil.getMessage(MessageKeys.REQUEST_BODY_FORMAT_ONLY_RAW));
            return;
        }
        String bodyText = bodyArea.getText();
        if (CharSequenceUtil.isBlank(bodyText)) {
            JOptionPane.showMessageDialog(this, I18nUtil.getMessage(MessageKeys.REQUEST_BODY_FORMAT_EMPTY));
            return;
        }
        if (RAW_TYPE_JSON.equals(RAW_TYPE_JSON)) {
            if (JSONUtil.isTypeJSON(bodyText)) {
                JSON json = JSONUtil.parse(bodyText);
                bodyArea.setText(JSONUtil.toJsonPrettyStr(json));
            } else {
                JOptionPane.showMessageDialog(this, I18nUtil.getMessage(MessageKeys.REQUEST_BODY_FORMAT_INVALID_JSON));
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
            List<Map<String, Object>> rows = formDataTablePanel.getRows();
            for (Map<String, Object> row : rows) {
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
            List<Map<String, Object>> rows = formDataTablePanel.getRows();
            for (Map<String, Object> row : rows) {
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
}