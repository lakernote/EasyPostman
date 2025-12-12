package com.laker.postman.panel.collections.right.request.sub;

import cn.hutool.core.text.CharSequenceUtil;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.laker.postman.common.component.SearchTextField;
import com.laker.postman.common.component.table.EasyPostmanFormDataTablePanel;
import com.laker.postman.common.component.table.EasyPostmanFormUrlencodedTablePanel;
import com.laker.postman.model.RequestItemProtocolEnum;
import com.laker.postman.model.VariableSegment;
import com.laker.postman.util.*;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
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
import java.awt.event.ItemEvent;
import java.awt.event.MouseEvent;
import java.io.InputStream;

/**
 * 请求Body相关的独立面板，支持none、form-data、x-www-form-urlencoded、raw
 */
@Slf4j
public class RequestBodyPanel extends JPanel {
    public static final String BODY_TYPE_NONE = "none";
    public static final String BODY_TYPE_FORM_DATA = "form-data";
    public static final String BODY_TYPE_FORM_URLENCODED = "x-www-form-urlencoded";
    public static final String BODY_TYPE_RAW = "raw";
    public static final String RAW_TYPE_JSON = "JSON";
    public static final String RAW_TYPE_TEXT = "Text";
    public static final String RAW_TYPE_XML = "XML";

    @Getter
    private JComboBox<String> bodyTypeComboBox;
    private JLabel formatLabel;
    @Getter
    private JComboBox<String> rawTypeComboBox;
    @Getter
    private EasyPostmanFormDataTablePanel formDataTablePanel;
    @Getter
    private EasyPostmanFormUrlencodedTablePanel formUrlencodedTablePanel;
    @Getter
    private RSyntaxTextArea bodyArea;
    private CardLayout bodyCardLayout;
    private JPanel bodyCardPanel;
    private String currentBodyType = BODY_TYPE_NONE;
    @Getter
    private JButton wsSendButton;
    private JButton formatButton;
    private final boolean isWebSocketMode;

    private Timer wsTimer; // 定时发送用
    private JButton wsTimedSendButton; // 定时发送按钮
    private JTextField wsIntervalField; // 定时间隔输入框
    private JCheckBox wsClearInputCheckBox; // 清空输入复选框
    private SearchTextField searchField; // HTTP模式下的搜索框

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
        // 优化：所有控件同排显示
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
        JLabel bodyTypeLabel = new JLabel(I18nUtil.getMessage(MessageKeys.REQUEST_BODY_TYPE));
        topPanel.add(bodyTypeLabel);
        String[] bodyTypes = new String[]{BODY_TYPE_NONE, BODY_TYPE_FORM_DATA, BODY_TYPE_FORM_URLENCODED, BODY_TYPE_RAW};
        bodyTypeComboBox = new JComboBox<>(bodyTypes);
        bodyTypeComboBox.setSelectedItem(currentBodyType);
        bodyTypeComboBox.addActionListener(e -> switchBodyType((String) bodyTypeComboBox.getSelectedItem()));
        topPanel.add(bodyTypeComboBox);
        formatLabel = new JLabel(I18nUtil.getMessage(MessageKeys.REQUEST_BODY_FORMAT));
        String[] rawTypes = {RAW_TYPE_JSON, RAW_TYPE_XML, RAW_TYPE_TEXT};
        rawTypeComboBox = new JComboBox<>(rawTypes);
        rawTypeComboBox.setSelectedItem(RAW_TYPE_JSON);
        boolean showFormatControls = isBodyTypeRAW();
        rawTypeComboBox.setVisible(showFormatControls);
        topPanel.add(rawTypeComboBox);

        // 搜索区控件
        searchField = new SearchTextField();
        JButton prevButton = new JButton(new FlatSVGIcon("icons/arrow-up.svg", 18, 18));
        prevButton.setToolTipText("Previous");
        JButton nextButton = new JButton(new FlatSVGIcon("icons/arrow-down.svg", 18, 18));
        nextButton.setToolTipText("Next");
        topPanel.add(searchField);
        topPanel.add(prevButton);
        topPanel.add(nextButton);
        formatButton = new JButton(new FlatSVGIcon("icons/format.svg", 18, 18));
        formatButton.addActionListener(e -> formatBody());
        formatButton.setVisible(isBodyTypeRAW());
        topPanel.add(formatButton);
        bodyTypePanel.add(topPanel, BorderLayout.NORTH);

        add(bodyTypePanel, BorderLayout.NORTH);
        bodyCardLayout = new CardLayout();
        bodyCardPanel = new JPanel(bodyCardLayout);
        bodyCardPanel.add(createNonePanel(), BODY_TYPE_NONE);
        bodyCardPanel.add(createFormDataPanel(), BODY_TYPE_FORM_DATA);
        bodyCardPanel.add(createFormUrlencodedPanel(), BODY_TYPE_FORM_URLENCODED);
        bodyCardPanel.add(createRawPanel(), BODY_TYPE_RAW);
        add(bodyCardPanel, BorderLayout.CENTER);
        bodyCardLayout.show(bodyCardPanel, currentBodyType);

        // 搜索跳转逻辑（只在raw类型时可用）
        searchField.addActionListener(e -> {
            if (isBodyTypeRAW()) searchInBodyArea(bodyArea, searchField.getText(), true);
        });
        prevButton.addActionListener(e -> {
            if (isBodyTypeRAW()) searchInBodyArea(bodyArea, searchField.getText(), false);
        });
        nextButton.addActionListener(e -> {
            if (isBodyTypeRAW()) searchInBodyArea(bodyArea, searchField.getText(), true);
        });

        // 监听搜索选项变化，触发重新搜索
        searchField.addPropertyChangeListener("caseSensitive", evt -> {
            if (isBodyTypeRAW() && !searchField.getText().isEmpty()) {
                searchInBodyArea(bodyArea, searchField.getText(), true);
            }
        });
        searchField.addPropertyChangeListener("wholeWord", evt -> {
            if (isBodyTypeRAW() && !searchField.getText().isEmpty()) {
                searchInBodyArea(bodyArea, searchField.getText(), true);
            }
        });
        // 切换body类型时，控制搜索区显示
        bodyTypeComboBox.addActionListener(e -> {
            boolean isRaw = BODY_TYPE_RAW.equals(bodyTypeComboBox.getSelectedItem());
            rawTypeComboBox.setVisible(isRaw);
            formatLabel.setVisible(isRaw);
            formatButton.setVisible(isRaw);
            searchField.setVisible(isRaw);
            prevButton.setVisible(isRaw);
            nextButton.setVisible(isRaw);
        });
        // 初始化显示状态
        boolean isRaw = BODY_TYPE_RAW.equals(bodyTypeComboBox.getSelectedItem());
        rawTypeComboBox.setVisible(isRaw);
        formatLabel.setVisible(isRaw);
        formatButton.setVisible(isRaw);
        searchField.setVisible(isRaw);
        prevButton.setVisible(isRaw);
        nextButton.setVisible(isRaw);
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
        formDataTablePanel = new EasyPostmanFormDataTablePanel();
        return formDataTablePanel;
    }

    private JPanel createFormUrlencodedPanel() {
        formUrlencodedTablePanel = new EasyPostmanFormUrlencodedTablePanel();
        return formUrlencodedTablePanel;
    }

    private JPanel createRawPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        bodyArea = new RSyntaxTextArea(5, 20);
        bodyArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JSON_WITH_COMMENTS); // 默认JSON高亮
        bodyArea.setCodeFoldingEnabled(true); // 启用代码折叠
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
                java.util.List<VariableSegment> segments = VariableUtil.getVariableSegments(text);
                for (VariableSegment seg : segments) {
                    boolean isDefined = VariableUtil.isVariableDefined(seg.name);
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
            java.util.List<VariableSegment> segments = VariableUtil.getVariableSegments(text);
            for (VariableSegment seg : segments) {
                boolean isDefined = VariableUtil.isVariableDefined(seg.name);
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
                java.util.List<VariableSegment> segments = VariableUtil.getVariableSegments(text);
                for (VariableSegment seg : segments) {
                    if (pos >= seg.start && pos <= seg.end) {
                        String varName = seg.name;
                        String varValue = VariableUtil.getVariableValue(varName);
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
        // 监听 rawTypeComboBox 选项变化，切换高亮风格
        if (rawTypeComboBox != null) {
            rawTypeComboBox.addItemListener(e -> {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    String selected = (String) e.getItem();
                    switch (selected) {
                        case RAW_TYPE_JSON:
                            bodyArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JSON_WITH_COMMENTS);
                            break;
                        case RAW_TYPE_XML:
                            bodyArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_XML);
                            break;
                        case RAW_TYPE_TEXT:
                            bodyArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_NONE);
                            break;
                        default:
                            bodyArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_NONE);
                    }
                }
            });
        }
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
        String selectedFormat = (String) rawTypeComboBox.getSelectedItem();
        if (RAW_TYPE_JSON.equals(selectedFormat)) {
            if (!JsonUtil.isTypeJSON(bodyText) && JsonUtil.isTypeJSON5(bodyText)) {
                int result = JOptionPane.showConfirmDialog(
                        this,
                        I18nUtil.getMessage(MessageKeys.REQUEST_BODY_FORMAT_JSON5_WARNING),
                        I18nUtil.getMessage(MessageKeys.REQUEST_BODY_FORMAT),
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.WARNING_MESSAGE
                );
                if (result != JOptionPane.YES_OPTION) {
                    return; // 用户选择取消，不执行格式化
                }
            }
            String prettyJson = JsonUtil.formatJson5(bodyText);
            bodyArea.setText(prettyJson);
        } else if (RAW_TYPE_XML.equals(selectedFormat)) {
            bodyArea.setText(XmlUtil.formatXml(bodyText));
        } else {
            log.debug("Unsupported format type or content is not JSON/XML");
        }

    }

    /**
     * 在 bodyArea 中搜索关键字并跳转，支持大小写敏感、整词匹配、循环查找
     */
    private void searchInBodyArea(RSyntaxTextArea area, String keyword, boolean forward) {
        if (keyword == null || keyword.isEmpty()) return;
        String text = area.getText();
        if (text == null || text.isEmpty()) return;

        // 获取搜索选项
        boolean caseSensitive = searchField != null && searchField.isCaseSensitive();
        boolean wholeWord = searchField != null && searchField.isWholeWord();

        int caret = area.getCaretPosition();
        int pos;

        if (forward) {
            // 向后查找
            int start = caret;
            if (area.getSelectedText() != null) {
                start = area.getSelectionEnd();
            }
            pos = findNext(text, keyword, start, caseSensitive, wholeWord);
            if (pos == -1) {
                // 循环查找：从头开始
                pos = findNext(text, keyword, 0, caseSensitive, wholeWord);
            }
        } else {
            // 向前查找
            int start = caret;
            if (area.getSelectedText() != null) {
                start = area.getSelectionStart() - 1;
            }
            pos = findPrevious(text, keyword, start, caseSensitive, wholeWord);
            if (pos == -1) {
                // 循环查找：从末尾开始
                pos = findPrevious(text, keyword, text.length(), caseSensitive, wholeWord);
            }
        }

        if (pos != -1) {
            area.setCaretPosition(pos);
            area.select(pos, pos + keyword.length());
            area.requestFocusInWindow();
        }
    }

    /**
     * 向后查找匹配
     */
    private int findNext(String text, String keyword, int fromIndex, boolean caseSensitive, boolean wholeWord) {
        String searchText = caseSensitive ? text : text.toLowerCase();
        String searchKeyword = caseSensitive ? keyword : keyword.toLowerCase();

        int pos = fromIndex;
        while ((pos = searchText.indexOf(searchKeyword, pos)) != -1) {
            if (!wholeWord || isWholeWord(text, pos, keyword.length())) {
                return pos;
            }
            pos++;
        }
        return -1;
    }

    /**
     * 向前查找匹配
     */
    private int findPrevious(String text, String keyword, int fromIndex, boolean caseSensitive, boolean wholeWord) {
        if (fromIndex > text.length()) {
            fromIndex = text.length();
        }

        String searchText = caseSensitive ? text : text.toLowerCase();
        String searchKeyword = caseSensitive ? keyword : keyword.toLowerCase();

        int pos = fromIndex;
        while ((pos = searchText.lastIndexOf(searchKeyword, pos)) != -1) {
            if (!wholeWord || isWholeWord(text, pos, keyword.length())) {
                return pos;
            }
            pos--;
            if (pos < 0) break;
        }
        return -1;
    }

    /**
     * 判断是否为整词匹配
     */
    private boolean isWholeWord(String text, int start, int length) {
        int end = start + length;

        // 检查前一个字符
        if (start > 0) {
            char prevChar = text.charAt(start - 1);
            if (Character.isLetterOrDigit(prevChar) || prevChar == '_') {
                return false;
            }
        }

        // 检查后一个字符
        if (end < text.length()) {
            char nextChar = text.charAt(end);
            return !Character.isLetterOrDigit(nextChar) && nextChar != '_';
        }

        return true;
    }

    // getter方法，供主面板调用
    public String getBodyType() {
        return currentBodyType;
    }

    public String getRawBody() {
        return bodyArea != null ? bodyArea.getText().trim() : null;
    }

}
