package com.laker.postman.plugin.kafka.connection.ui;

import com.formdev.flatlaf.FlatClientProperties;
import com.laker.postman.plugin.kafka.MessageKeys;
import com.laker.postman.common.component.button.PrimaryButton;
import com.laker.postman.common.component.button.SecondaryButton;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.*;

import static com.laker.postman.plugin.kafka.KafkaI18n.t;

public class KafkaConnectionPanel extends JPanel {

    private static final String SEPARATOR_FG = "Separator.foreground";
    private static final String LABEL_DISABLED_FG = "Label.disabledForeground";
    private static final String PANEL_BG = "Panel.background";
    private static final String CARD_CONNECT = "connect";
    private static final String CARD_DISCONNECT = "disconnect";
    private static final String MIG_GROWX = "growx";
    private static final String MIG_GROWX_WRAP = "growx, wrap";

    public final JTextField bootstrapField;
    public final JTextField clientIdField;
    public final JComboBox<String> securityProtocolCombo;
    public final JComboBox<String> saslMechanismCombo;
    public final JTextField usernameField;
    public final JPasswordField passwordField;
    public final PrimaryButton connectBtn;
    public final CardLayout btnCardLayout;
    public final JPanel btnCard;
    public final JLabel connectionStatusLabel;

    public KafkaConnectionPanel(Runnable connectAction, Runnable disconnectAction) {
        super(new BorderLayout());
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, UIManager.getColor(SEPARATOR_FG)),
                BorderFactory.createEmptyBorder(2, 2, 2, 2)));

        JPanel form = new JPanel(new MigLayout(
                "insets 6 0 4 0, fillx, novisualpadding",
                "[grow,fill]12[1!][grow,fill]",
                "[]"
        ));
        form.setOpaque(false);

        JPanel connectionSection = createSectionPanel(false);
        JPanel authSection = createSectionPanel(true);
        JPanel connectionFields = new JPanel(new MigLayout(
                "insets 0, fillx, gapy 8, novisualpadding",
                "[]8[grow,fill]",
                "[][]"
        ));
        connectionFields.setOpaque(false);
        JPanel authFields = new JPanel(new MigLayout(
                "insets 0, fillx, gapy 8, novisualpadding",
                "[]8[grow,fill]8[]8[grow,fill]",
                "[][]"
        ));
        authFields.setOpaque(false);

        bootstrapField = new JTextField("localhost:9092");
        bootstrapField.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, t(MessageKeys.TOOLBOX_KAFKA_HOST_PLACEHOLDER));
        bootstrapField.setPreferredSize(new Dimension(0, 32));
        bootstrapField.addActionListener(e -> connectAction.run());

        clientIdField = new JTextField("easy-postman-toolbox");
        clientIdField.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, t(MessageKeys.TOOLBOX_KAFKA_CLIENT_ID_PLACEHOLDER));
        clientIdField.setPreferredSize(new Dimension(0, 32));

        securityProtocolCombo = new JComboBox<>(new String[]{"PLAINTEXT", "SASL_PLAINTEXT", "SASL_SSL", "SSL"});
        securityProtocolCombo.setPreferredSize(new Dimension(0, 32));

        saslMechanismCombo = new JComboBox<>(new String[]{"PLAIN", "SCRAM-SHA-256", "SCRAM-SHA-512"});
        saslMechanismCombo.setPreferredSize(new Dimension(0, 32));

        connectionFields.add(new JLabel(t(MessageKeys.TOOLBOX_KAFKA_HOST)));
        connectionFields.add(bootstrapField, MIG_GROWX_WRAP);
        connectionFields.add(new JLabel(t(MessageKeys.TOOLBOX_KAFKA_CLIENT_ID)));
        connectionFields.add(clientIdField, MIG_GROWX);

        usernameField = new JTextField("");
        usernameField.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, t(MessageKeys.TOOLBOX_KAFKA_USER_PLACEHOLDER));
        usernameField.setPreferredSize(new Dimension(0, 32));

        passwordField = new JPasswordField("");
        passwordField.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, t(MessageKeys.TOOLBOX_KAFKA_PASS_PLACEHOLDER));
        passwordField.setPreferredSize(new Dimension(0, 32));
        passwordField.addActionListener(e -> connectAction.run());

        authFields.add(new JLabel(t(MessageKeys.TOOLBOX_KAFKA_SECURITY_PROTOCOL)));
        authFields.add(securityProtocolCombo, MIG_GROWX);
        authFields.add(new JLabel(t(MessageKeys.TOOLBOX_KAFKA_SASL_MECHANISM)));
        authFields.add(saslMechanismCombo, MIG_GROWX_WRAP);
        authFields.add(new JLabel(t(MessageKeys.TOOLBOX_KAFKA_USER)));
        authFields.add(usernameField, MIG_GROWX);
        authFields.add(new JLabel(t(MessageKeys.TOOLBOX_KAFKA_PASS)));
        authFields.add(passwordField, MIG_GROWX);

        connectBtn = new PrimaryButton(t(MessageKeys.TOOLBOX_KAFKA_CONNECT), "icons/connect.svg");
        connectBtn.addActionListener(e -> connectAction.run());

        SecondaryButton disconnectBtn = new SecondaryButton(t(MessageKeys.TOOLBOX_KAFKA_DISCONNECT), "icons/ws-close.svg");
        disconnectBtn.addActionListener(e -> disconnectAction.run());

        btnCardLayout = new CardLayout();
        btnCard = new JPanel(btnCardLayout);
        btnCard.setOpaque(false);
        btnCard.add(connectBtn, CARD_CONNECT);
        btnCard.add(disconnectBtn, CARD_DISCONNECT);
        btnCardLayout.show(btnCard, CARD_CONNECT);

        connectionStatusLabel = new JLabel("●");
        connectionStatusLabel.setForeground(UIManager.getColor(LABEL_DISABLED_FG));
        connectionStatusLabel.setFont(connectionStatusLabel.getFont().deriveFont(Font.BOLD, 14f));
        connectionStatusLabel.setToolTipText(t(MessageKeys.TOOLBOX_KAFKA_STATUS_NOT_CONNECTED));

        JPanel actionRow = new JPanel(new MigLayout(
                "insets 0, fillx, novisualpadding",
                "[grow,fill]8[]4[]",
                "[]"));
        actionRow.setOpaque(false);
        actionRow.add(new JLabel(), MIG_GROWX);
        actionRow.add(btnCard);
        actionRow.add(connectionStatusLabel);

        connectionSection.add(connectionFields, BorderLayout.CENTER);

        JPanel authBody = new JPanel(new BorderLayout(0, 6));
        authBody.setOpaque(false);
        authBody.add(authFields, BorderLayout.CENTER);
        authBody.add(actionRow, BorderLayout.SOUTH);
        authSection.add(authBody, BorderLayout.CENTER);

        form.add(connectionSection, "grow");
        form.add(new JSeparator(SwingConstants.VERTICAL), "growy");
        form.add(authSection, "grow");

        add(form, BorderLayout.CENTER);
    }

    private static JPanel createSectionPanel(boolean addLeftPadding) {
        JPanel panel = new JPanel(new BorderLayout(0, 0));
        panel.setOpaque(true);
        panel.setBackground(UIManager.getColor(PANEL_BG));
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UIManager.getColor(SEPARATOR_FG)),
                BorderFactory.createEmptyBorder(6, addLeftPadding ? 10 : 8, 6, 8)));
        return panel;
    }
}
