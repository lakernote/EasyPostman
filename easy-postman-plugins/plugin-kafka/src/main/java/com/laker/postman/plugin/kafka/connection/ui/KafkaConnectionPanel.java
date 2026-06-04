package com.laker.postman.plugin.kafka.connection.ui;

import com.formdev.flatlaf.FlatClientProperties;
import com.laker.postman.common.component.button.SecondaryButton;
import com.laker.postman.common.component.connection.ConnectionToolbarUi;
import com.laker.postman.plugin.kafka.MessageKeys;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.*;

import static com.laker.postman.plugin.kafka.KafkaI18n.t;

public class KafkaConnectionPanel extends JPanel {

    private static final String SEPARATOR_FG = "Separator.foreground";
    private static final String CARD_CONNECT = "connect";
    private static final String CARD_DISCONNECT = "disconnect";
    private static final int KAFKA_LABEL_WIDTH = 72;
    private static final int SHORT_LABEL_WIDTH = 52;
    private static final int BOOTSTRAP_FIELD_WIDTH = 260;
    private static final int PROTOCOL_FIELD_WIDTH = 148;
    private static final int CLIENT_ID_FIELD_WIDTH = BOOTSTRAP_FIELD_WIDTH;
    private static final int SASL_MECHANISM_FIELD_WIDTH = 160;
    private static final int AUTH_FIELD_WIDTH = 170;

    public final JComboBox<String> profileCombo;
    public final JButton newProfileBtn;
    public final JButton saveProfileBtn;
    public final JButton saveAsProfileBtn;
    public final JButton deleteProfileBtn;
    public final JTextField bootstrapField;
    public final JTextField clientIdField;
    public final JComboBox<String> securityProtocolCombo;
    public final JComboBox<String> saslMechanismCombo;
    public final JTextField usernameField;
    public final JPasswordField passwordField;
    public final JPanel optionsRow;
    public final SecondaryButton connectBtn;
    private final CardLayout connectionStateLayout;
    private final JPanel connectionStatePanel;

    public KafkaConnectionPanel(Runnable connectAction, Runnable disconnectAction) {
        super(new BorderLayout());
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, UIManager.getColor(SEPARATOR_FG)),
                BorderFactory.createEmptyBorder(3, 6, 3, 6)));

        profileCombo = new JComboBox<>();
        profileCombo.setEditable(false);
        ConnectionToolbarUi.compactControl(profileCombo);
        profileCombo.setRenderer(ConnectionToolbarUi.displayRenderer(value -> value == null ? "" : value));

        newProfileBtn = ConnectionToolbarUi.iconButton(t(MessageKeys.TOOLBOX_KAFKA_PROFILE_NEW), "icons/plus.svg");
        saveProfileBtn = ConnectionToolbarUi.iconButton(t(MessageKeys.TOOLBOX_KAFKA_PROFILE_SAVE), "icons/save.svg");
        saveProfileBtn.setToolTipText(t(MessageKeys.TOOLBOX_KAFKA_PROFILE_SAVE) + " (Ctrl+S)");
        saveAsProfileBtn = ConnectionToolbarUi.iconButton(t(MessageKeys.TOOLBOX_KAFKA_PROFILE_SAVE_AS), "icons/duplicate.svg");
        deleteProfileBtn = ConnectionToolbarUi.iconButton(t(MessageKeys.TOOLBOX_KAFKA_PROFILE_DELETE), "icons/delete.svg");

        JPanel form = new JPanel(new MigLayout(
                "insets 0, fillx, gapy 2, novisualpadding, hidemode 3",
                "[grow,fill]",
                "[][]"
        ));
        form.setOpaque(false);

        bootstrapField = new JTextField("localhost:9092");
        bootstrapField.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, t(MessageKeys.TOOLBOX_KAFKA_HOST_PLACEHOLDER));
        ConnectionToolbarUi.compactControl(bootstrapField);
        bootstrapField.addActionListener(e -> connectAction.run());

        clientIdField = new JTextField("easy-postman-toolbox");
        clientIdField.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, t(MessageKeys.TOOLBOX_KAFKA_CLIENT_ID_PLACEHOLDER));
        ConnectionToolbarUi.compactControl(clientIdField);

        securityProtocolCombo = new JComboBox<>(new String[]{"PLAINTEXT", "SASL_PLAINTEXT", "SASL_SSL", "SSL"});
        ConnectionToolbarUi.compactControl(securityProtocolCombo);

        saslMechanismCombo = new JComboBox<>(new String[]{"PLAIN", "SCRAM-SHA-256", "SCRAM-SHA-512"});
        ConnectionToolbarUi.compactControl(saslMechanismCombo);

        usernameField = new JTextField("");
        usernameField.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, t(MessageKeys.TOOLBOX_KAFKA_USER_PLACEHOLDER));
        ConnectionToolbarUi.compactControl(usernameField);

        passwordField = new JPasswordField("");
        passwordField.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, t(MessageKeys.TOOLBOX_KAFKA_PASS_PLACEHOLDER));
        ConnectionToolbarUi.compactControl(passwordField);
        passwordField.addActionListener(e -> connectAction.run());

        connectBtn = new SecondaryButton(t(MessageKeys.TOOLBOX_KAFKA_CONNECT), "icons/connect.svg");
        ConnectionToolbarUi.compactConnectionButton(connectBtn);
        connectBtn.addActionListener(e -> connectAction.run());

        SecondaryButton disconnectBtn = new SecondaryButton(t(MessageKeys.TOOLBOX_KAFKA_DISCONNECT), "icons/ws-close.svg");
        ConnectionToolbarUi.compactConnectionButton(disconnectBtn);
        disconnectBtn.addActionListener(e -> disconnectAction.run());

        connectionStateLayout = new CardLayout();
        connectionStatePanel = new JPanel(connectionStateLayout);
        connectionStatePanel.setOpaque(false);
        connectionStatePanel.add(connectBtn, CARD_CONNECT);
        connectionStatePanel.add(disconnectBtn, CARD_DISCONNECT);
        showDisconnectedState();

        JPanel mainRow = new JPanel(new MigLayout(
                "insets 0, fillx, novisualpadding, gapx 0",
                ConnectionToolbarUi.profileActionColumns()
                        + ConnectionToolbarUi.connectionFieldColumns(KAFKA_LABEL_WIDTH, BOOTSTRAP_FIELD_WIDTH) + "4"
                        + ConnectionToolbarUi.connectionFieldColumns(KAFKA_LABEL_WIDTH, PROTOCOL_FIELD_WIDTH)
                        + "6[" + ConnectionToolbarUi.CONNECTION_ACTION_BUTTON_WIDTH + "!]push",
                "[]"
        ));
        mainRow.setOpaque(false);
        mainRow.add(ConnectionToolbarUi.label(t(MessageKeys.TOOLBOX_KAFKA_PROFILE)));
        mainRow.add(profileCombo);
        mainRow.add(newProfileBtn);
        mainRow.add(saveProfileBtn);
        mainRow.add(saveAsProfileBtn);
        mainRow.add(deleteProfileBtn);
        mainRow.add(ConnectionToolbarUi.verticalSeparator(),
                "w 1!, h " + ConnectionToolbarUi.VERTICAL_SEPARATOR_HEIGHT + "!");
        mainRow.add(ConnectionToolbarUi.label(t(MessageKeys.TOOLBOX_KAFKA_HOST)));
        mainRow.add(bootstrapField);
        mainRow.add(ConnectionToolbarUi.label(t(MessageKeys.TOOLBOX_KAFKA_SECURITY_PROTOCOL)));
        mainRow.add(securityProtocolCombo);
        mainRow.add(connectionStatePanel, "h " + ConnectionToolbarUi.CONNECTION_BUTTON_HEIGHT + "!");

        optionsRow = new JPanel(new MigLayout(
                "insets 2 0 2 0, fillx, novisualpadding, gapx 0",
                ConnectionToolbarUi.profileActionColumns()
                        + ConnectionToolbarUi.connectionFieldColumns(KAFKA_LABEL_WIDTH, CLIENT_ID_FIELD_WIDTH) + "4"
                        + ConnectionToolbarUi.longConnectionFieldColumns(SASL_MECHANISM_FIELD_WIDTH) + "4"
                        + ConnectionToolbarUi.connectionFieldColumns(SHORT_LABEL_WIDTH, AUTH_FIELD_WIDTH) + "4"
                        + ConnectionToolbarUi.connectionFieldColumns(SHORT_LABEL_WIDTH, AUTH_FIELD_WIDTH) + "push",
                "[]"
        ));
        optionsRow.setOpaque(false);
        optionsRow.add(ConnectionToolbarUi.label(t(MessageKeys.TOOLBOX_KAFKA_CLIENT_ID)), "skip 7");
        optionsRow.add(clientIdField);
        optionsRow.add(ConnectionToolbarUi.label(t(MessageKeys.TOOLBOX_KAFKA_SASL_MECHANISM)));
        optionsRow.add(saslMechanismCombo);
        optionsRow.add(ConnectionToolbarUi.label(t(MessageKeys.TOOLBOX_KAFKA_USER)));
        optionsRow.add(usernameField);
        optionsRow.add(ConnectionToolbarUi.label(t(MessageKeys.TOOLBOX_KAFKA_PASS)));
        optionsRow.add(passwordField);

        form.add(mainRow, "growx, wrap");
        form.add(optionsRow, "growx");
        add(form, BorderLayout.CENTER);
        ConnectionToolbarUi.lockConnectionPanelHeight(this, true);
    }

    public void setConnectionOptionsVisible(boolean visible) {
        optionsRow.setVisible(visible);
        revalidate();
        repaint();
    }

    public void showConnectedState() {
        connectionStateLayout.show(connectionStatePanel, CARD_DISCONNECT);
    }

    public void showDisconnectedState() {
        connectionStateLayout.show(connectionStatePanel, CARD_CONNECT);
    }
}
