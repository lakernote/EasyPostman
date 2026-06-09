package com.laker.postman.plugin.kafka.producer.ui;

import com.formdev.flatlaf.FlatClientProperties;
import com.laker.postman.plugin.kafka.MessageKeys;
import com.laker.postman.plugin.kafka.shared.ui.KafkaPropertiesEditorPanel;
import com.laker.postman.common.component.PlaceholderTextArea;
import com.laker.postman.common.component.SearchableTextArea;
import com.laker.postman.common.component.ToolWindowChrome;
import com.laker.postman.common.component.ToolWindowSurfaceStyle;
import com.laker.postman.common.component.button.ClearButton;
import com.laker.postman.common.component.button.PrimaryButton;
import com.laker.postman.common.constants.ModernColors;
import com.laker.postman.util.EditorThemeUtil;
import com.laker.postman.util.FontsUtil;
import com.laker.postman.util.IconUtil;
import net.miginfocom.swing.MigLayout;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

import static com.laker.postman.plugin.kafka.KafkaI18n.t;

public class KafkaProducerPanel extends JPanel {

    private static final String MIG_GROWX = "growx";
    private static final String MIG_GROWX_WRAP = "growx, wrap";
    private static final String ACTION_KAFKA_SEND = "kafka-send";

    public final JTextField topicField;
    public final JTextField keyField;
    public final PlaceholderTextArea headersArea;
    public final JSpinner partitionSpinner;
    public final RSyntaxTextArea payloadArea;
    public final KafkaPropertiesEditorPanel customPropsPanel;
    public final PrimaryButton sendBtn;
    public final JLabel statusLabel;
    public final JPanel advancedPanel;

    public KafkaProducerPanel(Runnable sendAction) {
        super(new BorderLayout(0, 0));
        ToolWindowSurfaceStyle.applyCard(this);

        JPanel titleBar = new JPanel(new BorderLayout());
        ToolWindowSurfaceStyle.applySectionHeader(titleBar, 6, 10, 6, 10);
        JLabel titleLbl = new JLabel(t(MessageKeys.TOOLBOX_KAFKA_PRODUCER_TITLE));
        titleLbl.setFont(FontsUtil.getDefaultFontWithOffset(Font.BOLD, -1));
        titleBar.add(titleLbl, BorderLayout.WEST);

        JToggleButton advancedToggleBtn = new JToggleButton();
        advancedToggleBtn.setIcon(IconUtil.createThemed("icons/more.svg", 16, 16));
        advancedToggleBtn.setSelectedIcon(IconUtil.createColored("icons/more.svg", 16, 16, ModernColors.getPrimary()));
        advancedToggleBtn.setToolTipText(t(MessageKeys.TOOLBOX_KAFKA_ADVANCED_OPTIONS));
        advancedToggleBtn.setSelected(false);
        advancedToggleBtn.setPreferredSize(new Dimension(28, 28));
        advancedToggleBtn.setFocusable(false);
        advancedToggleBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        advancedToggleBtn.putClientProperty(FlatClientProperties.BUTTON_TYPE, FlatClientProperties.BUTTON_TYPE_TOOLBAR_BUTTON);
        titleBar.add(advancedToggleBtn, BorderLayout.EAST);

        JPanel form = new JPanel(new MigLayout(
                "insets 8 10 6 10, fillx, gapy 6",
                "[]8[grow,fill]8[]8[grow,fill]",
                "[]"
        ));
        form.setOpaque(false);

        topicField = new JTextField("");
        topicField.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, t(MessageKeys.TOOLBOX_KAFKA_TOPIC_PLACEHOLDER));
        topicField.setPreferredSize(new Dimension(180, 30));

        keyField = new JTextField("");
        keyField.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, t(MessageKeys.TOOLBOX_KAFKA_KEY_PLACEHOLDER));
        keyField.setPreferredSize(new Dimension(140, 30));

        partitionSpinner = new JSpinner(new SpinnerNumberModel(-1, -1, Integer.MAX_VALUE, 1));
        partitionSpinner.setPreferredSize(new Dimension(90, 30));

        sendBtn = new PrimaryButton(t(MessageKeys.TOOLBOX_KAFKA_SEND), "icons/send.svg");
        sendBtn.addActionListener(e -> sendAction.run());

        form.add(new JLabel(t(MessageKeys.TOOLBOX_KAFKA_TOPIC)));
        form.add(topicField, MIG_GROWX);
        form.add(new JLabel(t(MessageKeys.TOOLBOX_KAFKA_KEY)));
        form.add(keyField, MIG_GROWX_WRAP);

        form.add(new JLabel(t(MessageKeys.TOOLBOX_KAFKA_PARTITION)), "span, split 3");
        form.add(partitionSpinner, "w 90!");
        form.add(sendBtn, "push, al right");

        customPropsPanel = new KafkaPropertiesEditorPanel(
                t(MessageKeys.TOOLBOX_KAFKA_PRODUCER_CUSTOM_PROPERTIES),
                t(MessageKeys.TOOLBOX_KAFKA_PRODUCER_CUSTOM_PROPERTIES_HINT),
                t(MessageKeys.TOOLBOX_KAFKA_PRODUCER_CUSTOM_PROPERTIES_PLACEHOLDER),
                ModernColors.getDividerBorderColor(),
                ModernColors.getTextSecondary());
        advancedPanel = customPropsPanel;
        advancedPanel.setVisible(false);
        advancedToggleBtn.addActionListener(e -> advancedPanel.setVisible(advancedToggleBtn.isSelected()));

        JPanel headersPanel = new JPanel(new BorderLayout());
        headersPanel.setOpaque(false);
        JPanel headersHeader = new JPanel(new MigLayout("insets 4 8 4 8, fillx", "[]push", "[]"));
        ToolWindowSurfaceStyle.applySectionHeader(headersHeader);
        JLabel headersLbl = new JLabel(t(MessageKeys.TOOLBOX_KAFKA_HEADERS));
        headersLbl.setFont(FontsUtil.getDefaultFontWithOffset(Font.BOLD, -2));
        headersHeader.add(headersLbl);

        headersArea = new PlaceholderTextArea(2, 0);
        headersArea.setLineWrap(false);
        headersArea.setPlaceholder(t(MessageKeys.TOOLBOX_KAFKA_HEADERS_PLACEHOLDER));
        ToolWindowSurfaceStyle.applyTextComponentInput(headersArea);
        JScrollPane headersScroll = new JScrollPane(headersArea);
        ToolWindowSurfaceStyle.applyScrollPaneCard(headersScroll);

        headersPanel.add(headersHeader, BorderLayout.NORTH);
        headersPanel.add(headersScroll, BorderLayout.CENTER);

        JPanel payloadPanel = new JPanel(new BorderLayout());
        payloadPanel.setOpaque(false);
        JPanel payloadHeader = new JPanel(new MigLayout("insets 4 8 4 8, fillx", "[]push[]", "[]"));
        ToolWindowSurfaceStyle.applySectionHeader(payloadHeader);
        JLabel payloadLbl = new JLabel(t(MessageKeys.TOOLBOX_KAFKA_PAYLOAD));
        payloadLbl.setFont(FontsUtil.getDefaultFontWithOffset(Font.PLAIN, -2));
        ClearButton clearButton = new ClearButton();
        clearButton.setToolTipText(t(MessageKeys.TOOLBOX_KAFKA_CLEAR_PAYLOAD));
        payloadHeader.add(payloadLbl);
        payloadHeader.add(clearButton);

        payloadArea = new RSyntaxTextArea();
        payloadArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JSON);
        payloadArea.setCodeFoldingEnabled(true);
        payloadArea.setAntiAliasingEnabled(true);
        payloadArea.setText("{\n  \"message\": \"hello kafka\"\n}");
        payloadArea.getInputMap().put(KeyStroke.getKeyStroke("control ENTER"), ACTION_KAFKA_SEND);
        payloadArea.getInputMap().put(KeyStroke.getKeyStroke("meta ENTER"), ACTION_KAFKA_SEND);
        payloadArea.getActionMap().put(ACTION_KAFKA_SEND, new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                sendAction.run();
            }
        });
        clearButton.addActionListener(e -> payloadArea.setText(""));
        EditorThemeUtil.loadTheme(payloadArea);
        SearchableTextArea payloadSearchableArea = new SearchableTextArea(payloadArea);

        payloadPanel.add(payloadHeader, BorderLayout.NORTH);
        payloadPanel.add(payloadSearchableArea, BorderLayout.CENTER);

        JSplitPane editorSplit = ToolWindowChrome.createVerticalCardSplitPane(headersPanel, payloadPanel, 100);
        editorSplit.setDividerLocation(100);
        editorSplit.setResizeWeight(0.25);

        statusLabel = new JLabel(t(MessageKeys.TOOLBOX_KAFKA_PRODUCER_READY));
        statusLabel.setForeground(ModernColors.getTextSecondary());
        statusLabel.setBorder(new EmptyBorder(4, 8, 4, 8));

        add(titleBar, BorderLayout.NORTH);
        JPanel content = new JPanel(new BorderLayout(0, 0));
        content.setOpaque(false);
        content.add(form, BorderLayout.NORTH);
        JPanel editorContainer = new JPanel(new BorderLayout(0, 0));
        editorContainer.setOpaque(false);
        editorContainer.add(advancedPanel, BorderLayout.NORTH);
        editorContainer.add(editorSplit, BorderLayout.CENTER);
        content.add(editorContainer, BorderLayout.CENTER);
        add(content, BorderLayout.CENTER);
        add(statusLabel, BorderLayout.SOUTH);
    }

    public void updateEditorFont() {
        payloadArea.setFont(FontsUtil.getDefaultFont(Font.PLAIN));
    }
}
