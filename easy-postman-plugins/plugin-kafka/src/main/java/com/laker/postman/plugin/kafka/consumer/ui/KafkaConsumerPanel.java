package com.laker.postman.plugin.kafka.consumer.ui;

import com.formdev.flatlaf.FlatClientProperties;
import com.laker.postman.plugin.kafka.consumer.KafkaConsumedMessage;
import com.laker.postman.plugin.kafka.shared.KafkaPanelSupport;
import com.laker.postman.plugin.kafka.shared.ui.KafkaPropertiesEditorPanel;
import com.laker.postman.common.component.SearchableTextArea;
import com.laker.postman.common.component.button.ClearButton;
import com.laker.postman.common.component.button.CloseButton;
import com.laker.postman.common.component.button.CopyButton;
import com.laker.postman.common.component.button.PrimaryButton;
import com.laker.postman.common.component.button.SecondaryButton;
import com.laker.postman.common.component.table.EnhancedTablePanel;
import com.laker.postman.common.constants.ModernColors;
import com.laker.postman.util.EditorThemeUtil;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.IconUtil;
import com.laker.postman.util.JsonUtil;
import com.laker.postman.util.MessageKeys;
import com.laker.postman.util.NotificationUtil;
import net.miginfocom.swing.MigLayout;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class KafkaConsumerPanel extends JPanel {

    private static final String SEPARATOR_FG = "Separator.foreground";
    private static final String LABEL_DISABLED_FG = "Label.disabledForeground";
    private static final String CARD_CONSUME_START = "consume-start";
    private static final String CARD_CONSUME_STOP = "consume-stop";

    public final JTextField topicField;
    public final JTextField groupIdField;
    public final KafkaPartitionSelector partitionSelector;
    public final JComboBox<String> autoOffsetCombo;
    public final JTextField consumeStartValueField;
    public final JSpinner pollTimeoutSpinner;
    public final JSpinner batchSizeSpinner;
    public final JSpinner maxViewSpinner;
    public final KafkaPropertiesEditorPanel customPropsPanel;
    public final CardLayout consumeBtnCardLayout;
    public final JPanel consumeBtnCard;
    public final JLabel statusLabel;
    public final EnhancedTablePanel messageTablePanel;
    public final RSyntaxTextArea detailArea;
    public final JSplitPane detailSplit;

    private boolean detailPanelVisible = false;
    private final JLabel detailTopicLabel;
    private final JLabel detailPartitionLabel;
    private final JLabel detailOffsetLabel;
    private final JLabel detailKeyLabel;
    private final JLabel detailTimeLabel;
    private final JLabel consumeStartValueLabel;

    public KafkaConsumerPanel(Runnable startAction, Runnable stopAction, Runnable clearAction, Runnable selectionChanged) {
        super(new BorderLayout(0, 0));

        JPanel titleBar = new JPanel(new MigLayout("insets 5 10 5 8, fillx", "[]push[]4[]4[]4[]", "[]"));
        titleBar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, UIManager.getColor(SEPARATOR_FG)));

        JLabel titleLbl = new JLabel(t(MessageKeys.TOOLBOX_KAFKA_CONSUMER_TITLE));
        titleLbl.setFont(titleLbl.getFont().deriveFont(Font.BOLD, 12f));

        JToggleButton detailToggleBtn = new JToggleButton();
        detailToggleBtn.setIcon(IconUtil.createThemed("icons/detail.svg", 16, 16));
        detailToggleBtn.setSelectedIcon(IconUtil.createColored("icons/detail.svg", 16, 16, ModernColors.PRIMARY));
        detailToggleBtn.setToolTipText(t(MessageKeys.TOOLBOX_KAFKA_MESSAGE_DETAIL));
        detailToggleBtn.setSelected(false);
        detailToggleBtn.setPreferredSize(new Dimension(28, 28));
        detailToggleBtn.setFocusable(false);
        detailToggleBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        detailToggleBtn.putClientProperty(FlatClientProperties.BUTTON_TYPE, FlatClientProperties.BUTTON_TYPE_TOOLBAR_BUTTON);

        JToggleButton advancedToggleBtn = new JToggleButton();
        advancedToggleBtn.setIcon(IconUtil.createThemed("icons/more.svg", 16, 16));
        advancedToggleBtn.setSelectedIcon(IconUtil.createColored("icons/more.svg", 16, 16, ModernColors.PRIMARY));
        advancedToggleBtn.setToolTipText(t(MessageKeys.TOOLBOX_KAFKA_ADVANCED_OPTIONS));
        advancedToggleBtn.setSelected(false);
        advancedToggleBtn.setPreferredSize(new Dimension(28, 28));
        advancedToggleBtn.setFocusable(false);
        advancedToggleBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        advancedToggleBtn.putClientProperty(FlatClientProperties.BUTTON_TYPE, FlatClientProperties.BUTTON_TYPE_TOOLBAR_BUTTON);

        ClearButton clearConsumeBtn = new ClearButton();
        clearConsumeBtn.setToolTipText(t(MessageKeys.TOOLBOX_KAFKA_CONSUMER_CLEAR));
        clearConsumeBtn.addActionListener(e -> clearAction.run());

        titleBar.add(titleLbl);
        titleBar.add(clearConsumeBtn);
        titleBar.add(advancedToggleBtn);
        titleBar.add(detailToggleBtn);

        JPanel mainControls = new JPanel(new MigLayout(
                "insets 6 10 6 8, fillx",
                "[]8[grow,fill]8[]",
                "[]"
        ));

        topicField = new JTextField("");
        topicField.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, t(MessageKeys.TOOLBOX_KAFKA_TOPIC_PLACEHOLDER));
        topicField.setPreferredSize(new Dimension(0, 30));

        PrimaryButton startConsumeBtn = new PrimaryButton(t(MessageKeys.TOOLBOX_KAFKA_START_CONSUME), "icons/start.svg");
        startConsumeBtn.addActionListener(e -> startAction.run());

        SecondaryButton stopConsumeBtn = new SecondaryButton(t(MessageKeys.TOOLBOX_KAFKA_STOP_CONSUME), "icons/stop.svg");
        stopConsumeBtn.addActionListener(e -> stopAction.run());

        consumeBtnCardLayout = new CardLayout();
        consumeBtnCard = new JPanel(consumeBtnCardLayout);
        consumeBtnCard.setOpaque(false);
        consumeBtnCard.add(startConsumeBtn, CARD_CONSUME_START);
        consumeBtnCard.add(stopConsumeBtn, CARD_CONSUME_STOP);
        consumeBtnCardLayout.show(consumeBtnCard, CARD_CONSUME_START);

        mainControls.add(new JLabel(t(MessageKeys.TOOLBOX_KAFKA_TOPIC)));
        mainControls.add(topicField);
        mainControls.add(consumeBtnCard);

        JPanel advancedPanel = new JPanel(new BorderLayout(0, 0));
        advancedPanel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, UIManager.getColor(SEPARATOR_FG)));

        JPanel advancedRowPanel = new JPanel(new MigLayout(
                "insets 4 10 6 8, fillx",
                "[]8[grow,fill]16[]8[grow,fill]16[]8[140!]28[]8[grow,fill]",
                "[]6[]"
        ));

        groupIdField = new JTextField("easy-postman-consumer");
        groupIdField.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, t(MessageKeys.TOOLBOX_KAFKA_GROUP_ID_PLACEHOLDER));
        groupIdField.setPreferredSize(new Dimension(0, 28));

        partitionSelector = new KafkaPartitionSelector();
        partitionSelector.setPreferredSize(new Dimension(0, 28));

        autoOffsetCombo = new JComboBox<>(new String[]{
                t(MessageKeys.TOOLBOX_KAFKA_OFFSET_RESET_LATEST),
                t(MessageKeys.TOOLBOX_KAFKA_OFFSET_RESET_EARLIEST),
                t(MessageKeys.TOOLBOX_KAFKA_OFFSET_RESET_NONE),
                t(MessageKeys.TOOLBOX_KAFKA_OFFSET_RESET_TIMESTAMP),
                t(MessageKeys.TOOLBOX_KAFKA_OFFSET_RESET_OFFSET)
        });
        autoOffsetCombo.setSelectedIndex(0);
        autoOffsetCombo.setPreferredSize(new Dimension(140, 28));

        consumeStartValueField = new JTextField("");
        consumeStartValueField.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, t(MessageKeys.TOOLBOX_KAFKA_OFFSET_VALUE_PLACEHOLDER));
        consumeStartValueField.setPreferredSize(new Dimension(0, 28));

        pollTimeoutSpinner = new JSpinner(new SpinnerNumberModel(1000, 100, 30000, 100));
        pollTimeoutSpinner.setPreferredSize(new Dimension(110, 28));

        batchSizeSpinner = new JSpinner(new SpinnerNumberModel(100, 1, 5000, 1));
        batchSizeSpinner.setPreferredSize(new Dimension(110, 28));

        maxViewSpinner = new JSpinner(new SpinnerNumberModel(500, 50, 10000, 50));
        maxViewSpinner.setPreferredSize(new Dimension(110, 28));

        advancedRowPanel.add(new JLabel(t(MessageKeys.TOOLBOX_KAFKA_GROUP_ID)));
        advancedRowPanel.add(groupIdField);
        advancedRowPanel.add(new JLabel(t(MessageKeys.TOOLBOX_KAFKA_PARTITION)));
        advancedRowPanel.add(partitionSelector);
        advancedRowPanel.add(new JLabel(t(MessageKeys.TOOLBOX_KAFKA_OFFSET_RESET)));
        advancedRowPanel.add(autoOffsetCombo);
        consumeStartValueLabel = new JLabel(t(MessageKeys.TOOLBOX_KAFKA_OFFSET_VALUE));
        advancedRowPanel.add(consumeStartValueLabel);
        advancedRowPanel.add(consumeStartValueField, "growx, wrap");

        advancedRowPanel.add(new JLabel(t(MessageKeys.TOOLBOX_KAFKA_POLL_TIMEOUT)));
        advancedRowPanel.add(pollTimeoutSpinner);
        advancedRowPanel.add(new JLabel(t(MessageKeys.TOOLBOX_KAFKA_BATCH_SIZE)));
        advancedRowPanel.add(batchSizeSpinner);
        advancedRowPanel.add(new JLabel(t(MessageKeys.TOOLBOX_KAFKA_MAX_VIEW)));
        advancedRowPanel.add(maxViewSpinner);

        customPropsPanel = new KafkaPropertiesEditorPanel(
                t(MessageKeys.TOOLBOX_KAFKA_CONSUMER_CUSTOM_PROPERTIES),
                t(MessageKeys.TOOLBOX_KAFKA_CONSUMER_CUSTOM_PROPERTIES_HINT),
                t(MessageKeys.TOOLBOX_KAFKA_CONSUMER_CUSTOM_PROPERTIES_PLACEHOLDER),
                UIManager.getColor(SEPARATOR_FG),
                UIManager.getColor(LABEL_DISABLED_FG));

        advancedPanel.add(advancedRowPanel, BorderLayout.NORTH);
        advancedPanel.add(customPropsPanel, BorderLayout.CENTER);
        advancedPanel.setVisible(false);
        advancedToggleBtn.addActionListener(e -> advancedPanel.setVisible(advancedToggleBtn.isSelected()));

        String[] columns = {
                t(MessageKeys.TOOLBOX_KAFKA_COL_TIME),
                t(MessageKeys.TOOLBOX_KAFKA_COL_RECORD_TIME),
                t(MessageKeys.TOOLBOX_KAFKA_COL_TOPIC),
                t(MessageKeys.TOOLBOX_KAFKA_COL_PARTITION),
                t(MessageKeys.TOOLBOX_KAFKA_COL_OFFSET),
                t(MessageKeys.TOOLBOX_KAFKA_COL_KEY),
                t(MessageKeys.TOOLBOX_KAFKA_COL_HEADERS),
                t(MessageKeys.TOOLBOX_KAFKA_COL_VALUE)
        };
        messageTablePanel = new EnhancedTablePanel(columns);
        messageTablePanel.getTable().getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                selectionChanged.run();
                if (!detailPanelVisible && messageTablePanel.getTable().getSelectedRow() >= 0) {
                    detailToggleBtn.setSelected(true);
                    showDetailPanel();
                }
            }
        });

        JPanel detailPanel = new JPanel(new BorderLayout(0, 0));
        detailPanel.setMinimumSize(new Dimension(0, 0));

        JPanel detailHeader = new JPanel(new MigLayout("insets 4 10 4 8, fillx", "[]8[]8[]8[]8[]push[]4[]", "[]"));
        detailHeader.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, UIManager.getColor(SEPARATOR_FG)));

        detailTopicLabel = buildChipLabel("Topic: —", ModernColors.INFO);
        detailPartitionLabel = buildChipLabel("Partition: —", new Color(120, 80, 200));
        detailOffsetLabel = buildChipLabel("Offset: —", new Color(20, 150, 100));
        detailKeyLabel = buildChipLabel("Key: —", new Color(180, 100, 0));
        detailTimeLabel = buildChipLabel("—", null);
        detailTimeLabel.setFont(detailTimeLabel.getFont().deriveFont(Font.PLAIN, 10f));
        detailTimeLabel.setForeground(UIManager.getColor(LABEL_DISABLED_FG));

        CopyButton copyValueBtn = new CopyButton();
        copyValueBtn.setToolTipText(t(MessageKeys.TOOLBOX_KAFKA_COPY_VALUE));
        copyValueBtn.addActionListener(e -> copyDetailValue());

        CloseButton closeDetailBtn = new CloseButton();
        closeDetailBtn.setToolTipText(t(MessageKeys.TOOLBOX_KAFKA_CLOSE_DETAIL));
        closeDetailBtn.addActionListener(e -> {
            detailToggleBtn.setSelected(false);
            hideDetailPanel();
        });

        detailHeader.add(detailTopicLabel);
        detailHeader.add(detailPartitionLabel);
        detailHeader.add(detailOffsetLabel);
        detailHeader.add(detailKeyLabel);
        detailHeader.add(detailTimeLabel);
        detailHeader.add(copyValueBtn);
        detailHeader.add(closeDetailBtn);

        detailArea = new RSyntaxTextArea();
        detailArea.setEditable(false);
        detailArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JSON);
        detailArea.setCodeFoldingEnabled(true);
        detailArea.setAntiAliasingEnabled(true);
        detailArea.setLineWrap(false);
        detailArea.setHighlightCurrentLine(true);
        EditorThemeUtil.loadTheme(detailArea);
        detailArea.setText("");
        SearchableTextArea searchableDetail = new SearchableTextArea(detailArea, false);

        detailPanel.add(detailHeader, BorderLayout.NORTH);
        detailPanel.add(searchableDetail, BorderLayout.CENTER);

        detailSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, messageTablePanel, detailPanel);
        detailSplit.setDividerSize(4);
        detailSplit.setResizeWeight(1.0);
        detailSplit.setContinuousLayout(true);
        detailSplit.setBorder(BorderFactory.createEmptyBorder());
        SwingUtilities.invokeLater(() -> detailSplit.setDividerLocation(1.0));

        detailToggleBtn.addActionListener(e -> {
            detailPanelVisible = detailToggleBtn.isSelected();
            if (detailPanelVisible) {
                showDetailPanel();
            } else {
                hideDetailPanel();
            }
        });

        statusLabel = new JLabel(t(MessageKeys.TOOLBOX_KAFKA_CONSUMER_READY));
        statusLabel.setForeground(UIManager.getColor(LABEL_DISABLED_FG));
        statusLabel.setFont(statusLabel.getFont().deriveFont(Font.PLAIN, 11f));
        statusLabel.setBorder(new EmptyBorder(3, 10, 3, 8));

        JPanel north = new JPanel(new BorderLayout());
        north.add(titleBar, BorderLayout.NORTH);
        north.add(mainControls, BorderLayout.CENTER);
        north.add(advancedPanel, BorderLayout.SOUTH);

        add(north, BorderLayout.NORTH);
        add(detailSplit, BorderLayout.CENTER);
        add(statusLabel, BorderLayout.SOUTH);
    }

    public void setConsuming(boolean consuming) {
        consumeBtnCardLayout.show(consumeBtnCard, consuming ? CARD_CONSUME_STOP : CARD_CONSUME_START);
    }

    public void setConsumeStartValueVisible(boolean visible) {
        consumeStartValueLabel.setVisible(visible);
        consumeStartValueField.setVisible(visible);
        revalidate();
        repaint();
    }

    public void showDetailPanel() {
        detailPanelVisible = true;
        int total = detailSplit.getHeight();
        int loc = total > 0 ? (int) (total * 0.60) : 300;
        detailSplit.setDividerLocation(loc);
    }

    public void hideDetailPanel() {
        detailPanelVisible = false;
        detailSplit.setDividerLocation(1.0);
    }

    public void clearDetail() {
        detailArea.setText("");
        detailTopicLabel.setText("Topic: —");
        detailPartitionLabel.setText("Partition: —");
        detailOffsetLabel.setText("Offset: —");
        detailKeyLabel.setText("Key: —");
        detailTimeLabel.setText("—");
    }

    public void updateDetail(KafkaConsumedMessage msg) {
        if (msg == null) {
            clearDetail();
            return;
        }
        detailTopicLabel.setText("Topic: " + msg.topic());
        detailPartitionLabel.setText("Partition: " + msg.partition());
        detailOffsetLabel.setText("Offset: " + msg.offset());
        detailKeyLabel.setText("Key: " + (msg.key() == null || msg.key().isBlank() ? "—" : msg.key()));
        detailTimeLabel.setText(msg.receiveTime());

        String value = msg.value() == null ? "" : msg.value().trim();
        if (KafkaPanelSupport.isJsonText(value)) {
            detailArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JSON);
            String pretty = JsonUtil.toJsonPrettyStr(value);
            detailArea.setText(pretty != null ? pretty : value);
        } else {
            detailArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_NONE);
            detailArea.setText(value);
        }
        detailArea.setCaretPosition(0);
    }

    public void updateEditorFont() {
        detailArea.setFont(com.laker.postman.util.FontsUtil.getDefaultFont(Font.PLAIN));
    }

    private void copyDetailValue() {
        String txt = detailArea.getText().trim();
        if (!txt.isEmpty()) {
            Toolkit.getDefaultToolkit().getSystemClipboard()
                    .setContents(new java.awt.datatransfer.StringSelection(txt), null);
            NotificationUtil.showSuccess(t(MessageKeys.TOOLBOX_KAFKA_VALUE_COPIED));
        }
    }

    private JLabel buildChipLabel(String text, Color bgColor) {
        JLabel lbl = new JLabel(text) {
            @Override
            protected void paintComponent(Graphics g) {
                if (bgColor != null) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(new Color(bgColor.getRed(), bgColor.getGreen(), bgColor.getBlue(), 30));
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                    g2.setColor(new Color(bgColor.getRed(), bgColor.getGreen(), bgColor.getBlue(), 140));
                    g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
                    g2.dispose();
                }
                super.paintComponent(g);
            }
        };
        lbl.setFont(lbl.getFont().deriveFont(Font.PLAIN, 11f));
        if (bgColor != null) {
            lbl.setForeground(ModernColors.isDarkTheme()
                    ? new Color(Math.min(bgColor.getRed() + 80, 255), Math.min(bgColor.getGreen() + 80, 255), Math.min(bgColor.getBlue() + 80, 255))
                    : bgColor.darker());
        }
        lbl.setBorder(new EmptyBorder(2, 6, 2, 6));
        lbl.setOpaque(false);
        return lbl;
    }

    private static String t(String key, Object... args) {
        return I18nUtil.getMessage(key, args);
    }
}
