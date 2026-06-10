package com.laker.postman.plugin.clientcert;

import com.laker.postman.common.component.button.ModernButtonFactory;
import com.laker.postman.common.component.setting.SettingsInputStyle;
import com.laker.postman.common.component.setting.SettingsHintLabel;
import com.laker.postman.common.component.ToolWindowActionToolbar;
import com.laker.postman.common.component.ToolWindowSurfaceStyle;
import com.laker.postman.common.constants.ModernColors;
import com.laker.postman.model.ClientCertificate;
import com.laker.postman.plugin.api.service.ClientCertificatePluginService;
import com.laker.postman.util.FontsUtil;
import com.laker.postman.util.NotificationUtil;
import lombok.Getter;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.io.File;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ClientCertificateSettingsPanel extends JPanel {

    private static final int DESCRIPTION_WIDTH = 620;
    private static final Dimension ACTION_BUTTON_SIZE = new Dimension(82, 30);
    private static final Dimension HELP_BUTTON_SIZE = new Dimension(70, 30);
    private static final Dimension CLOSE_BUTTON_SIZE = new Dimension(96, 32);

    private final Window parentWindow;
    private final ClientCertificatePluginService certificateService;
    private final CertificateTableModel tableModel = new CertificateTableModel();

    private JTable certificateTable;
    private JButton editButton;
    private JButton deleteButton;

    public ClientCertificateSettingsPanel(Window parentWindow, ClientCertificatePluginService certificateService) {
        this.parentWindow = parentWindow;
        this.certificateService = Objects.requireNonNull(certificateService, "certificateService");
        initUI();
        registerListeners();
        loadCertificates();
    }

    private void initUI() {
        setLayout(new BorderLayout(0, 12));
        ToolWindowSurfaceStyle.applyBackground(this);
        setBorder(new EmptyBorder(24, 24, 16, 24));

        add(createDescriptionPanel(), BorderLayout.NORTH);
        add(createTablePanel(), BorderLayout.CENTER);
        add(createFooterPanel(), BorderLayout.SOUTH);
    }

    private JPanel createDescriptionPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 6));
        panel.setOpaque(false);

        JLabel titleLabel = new JLabel(ClientCertI18n.t(MessageKeys.CERT_TITLE));
        titleLabel.setFont(FontsUtil.getDefaultFont(Font.BOLD));
        titleLabel.setForeground(ModernColors.getTextPrimary());

        SettingsHintLabel descriptionLabel = new SettingsHintLabel(
                ClientCertI18n.t(MessageKeys.CERT_DESCRIPTION),
                DESCRIPTION_WIDTH
        );

        panel.add(titleLabel, BorderLayout.NORTH);
        panel.add(descriptionLabel, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createTablePanel() {
        JPanel section = new JPanel(new BorderLayout(0, 8));
        ToolWindowSurfaceStyle.applyCard(section);
        section.setBorder(new EmptyBorder(14, 14, 14, 14));

        JPanel headerPanel = new JPanel(new BorderLayout(10, 0));
        headerPanel.setOpaque(false);

        JLabel sectionTitle = new JLabel(ClientCertI18n.t(MessageKeys.CERT_LIST_TITLE));
        sectionTitle.setFont(FontsUtil.getDefaultFont(Font.BOLD));
        sectionTitle.setForeground(ModernColors.getTextPrimary());
        headerPanel.add(sectionTitle, BorderLayout.WEST);
        headerPanel.add(createActionBar(), BorderLayout.EAST);

        section.add(headerPanel, BorderLayout.NORTH);
        section.add(createTableScrollPane(), BorderLayout.CENTER);
        return section;
    }

    private JPanel createActionBar() {
        JButton addButton = createSizedButton(ClientCertI18n.t(MessageKeys.CERT_ADD), true, ACTION_BUTTON_SIZE);
        editButton = createSizedButton(ClientCertI18n.t(MessageKeys.CERT_EDIT), false, ACTION_BUTTON_SIZE);
        deleteButton = createSizedButton(ClientCertI18n.t(MessageKeys.CERT_DELETE), false, ACTION_BUTTON_SIZE);
        JButton helpButton = createSizedButton(ClientCertI18n.t(MessageKeys.CERT_HELP), false, HELP_BUTTON_SIZE);

        addButton.addActionListener(e -> showAddDialog());
        editButton.addActionListener(e -> showEditDialog());
        deleteButton.addActionListener(e -> deleteCertificate());
        helpButton.addActionListener(e -> showHelp());

        return ToolWindowActionToolbar.inlineRight(addButton, editButton, deleteButton, helpButton);
    }

    private JScrollPane createTableScrollPane() {
        certificateTable = new JTable(tableModel);
        certificateTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        certificateTable.setRowHeight(28);
        certificateTable.setShowGrid(true);
        certificateTable.getTableHeader().setReorderingAllowed(false);
        certificateTable.getTableHeader().setFont(FontsUtil.getDefaultFontWithOffset(Font.BOLD, -2));
        certificateTable.setFont(FontsUtil.getDefaultFontWithOffset(Font.PLAIN, -2));
        certificateTable.setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);

        certificateTable.getColumnModel().getColumn(0).setPreferredWidth(70);
        certificateTable.getColumnModel().getColumn(0).setMinWidth(65);
        certificateTable.getColumnModel().getColumn(0).setMaxWidth(80);
        certificateTable.getColumnModel().getColumn(1).setPreferredWidth(110);
        certificateTable.getColumnModel().getColumn(1).setMinWidth(80);
        certificateTable.getColumnModel().getColumn(2).setPreferredWidth(220);
        certificateTable.getColumnModel().getColumn(2).setMinWidth(120);
        certificateTable.getColumnModel().getColumn(3).setPreferredWidth(70);
        certificateTable.getColumnModel().getColumn(3).setMinWidth(54);
        certificateTable.getColumnModel().getColumn(3).setMaxWidth(86);
        certificateTable.getColumnModel().getColumn(4).setPreferredWidth(80);
        certificateTable.getColumnModel().getColumn(4).setMinWidth(64);
        certificateTable.getColumnModel().getColumn(4).setMaxWidth(94);

        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        certificateTable.getColumnModel().getColumn(3).setCellRenderer(centerRenderer);
        certificateTable.getColumnModel().getColumn(4).setCellRenderer(centerRenderer);

        JScrollPane scrollPane = new JScrollPane(certificateTable);
        ToolWindowSurfaceStyle.applyTableScrollPaneCard(scrollPane, certificateTable);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        return scrollPane;
    }

    private JPanel createFooterPanel() {
        JButton closeButton = createSizedButton(ClientCertI18n.t(MessageKeys.CERT_CLOSE), false, CLOSE_BUTTON_SIZE);
        closeButton.addActionListener(e -> closeParentWindow());
        return ToolWindowActionToolbar.inlineRight(closeButton);
    }

    private static JButton createSizedButton(String text, boolean primary, Dimension size) {
        JButton button = ModernButtonFactory.createButton(text, primary);
        button.setPreferredSize(size);
        button.setMinimumSize(size);
        button.setMaximumSize(size);
        return button;
    }

    private void registerListeners() {
        certificateTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                updateButtonStates();
            }
        });
        certificateTable.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int row = certificateTable.rowAtPoint(e.getPoint());
                    int column = certificateTable.columnAtPoint(e.getPoint());
                    if (row >= 0 && column != 0) {
                        showEditDialog();
                    }
                }
            }
        });
        updateButtonStates();
    }

    private void loadCertificates() {
        tableModel.loadCertificates(certificateService.getAllCertificates());
        updateButtonStates();
    }

    private void updateButtonStates() {
        boolean hasSelection = certificateTable.getSelectedRow() >= 0;
        editButton.setEnabled(hasSelection);
        deleteButton.setEnabled(hasSelection);
    }

    private void closeParentWindow() {
        Window window = parentWindow != null ? parentWindow : SwingUtilities.getWindowAncestor(this);
        if (window != null) {
            window.dispose();
        }
    }

    private void showAddDialog() {
        ClientCertificate certificate = new ClientCertificate();
        CertificateEditDialog dialog = new CertificateEditDialog(
                SwingUtilities.getWindowAncestor(this), certificate, true);
        dialog.setVisible(true);

        if (dialog.isConfirmed()) {
            certificateService.addCertificate(certificate);
            loadCertificates();
            NotificationUtil.showSuccess(ClientCertI18n.t(MessageKeys.CERT_ADD_SUCCESS));
        }
    }

    private void showEditDialog() {
        int selectedRow = certificateTable.getSelectedRow();
        if (selectedRow < 0) {
            return;
        }

        ClientCertificate certificate = tableModel.getCertificateAt(selectedRow);
        CertificateEditDialog dialog = new CertificateEditDialog(
                SwingUtilities.getWindowAncestor(this), certificate, false);
        dialog.setVisible(true);

        if (dialog.isConfirmed()) {
            certificateService.updateCertificate(certificate);
            loadCertificates();
            NotificationUtil.showSuccess(ClientCertI18n.t(MessageKeys.CERT_EDIT_SUCCESS));
        }
    }

    private void deleteCertificate() {
        int selectedRow = certificateTable.getSelectedRow();
        if (selectedRow < 0) {
            return;
        }

        ClientCertificate certificate = tableModel.getCertificateAt(selectedRow);
        String displayName = certificate.getName() != null && !certificate.getName().isEmpty()
                ? certificate.getName()
                : certificate.getHost();
        String message = MessageFormat.format(
                ClientCertI18n.t(MessageKeys.CERT_DELETE_CONFIRM),
                displayName);

        int result = JOptionPane.showConfirmDialog(
                this,
                message,
                ClientCertI18n.t(MessageKeys.CERT_DELETE),
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );

        if (result == JOptionPane.YES_OPTION) {
            certificateService.deleteCertificate(certificate.getId());
            loadCertificates();
            NotificationUtil.showSuccess(ClientCertI18n.t(MessageKeys.CERT_DELETE_SUCCESS));
        }
    }

    private void showHelp() {
        JTextArea textArea = new JTextArea(ClientCertI18n.t(MessageKeys.CERT_HELP_CONTENT), 18, 50);
        textArea.setEditable(false);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        textArea.setFont(FontsUtil.getDefaultFontWithOffset(Font.PLAIN, -2));
        ToolWindowSurfaceStyle.applyTextComponentCard(textArea);
        textArea.setBorder(new EmptyBorder(10, 10, 10, 10));

        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setPreferredSize(new Dimension(600, 350));
        ToolWindowSurfaceStyle.applyScrollPaneCard(scrollPane);

        JOptionPane.showMessageDialog(
                this,
                scrollPane,
                ClientCertI18n.t(MessageKeys.CERT_HELP_TITLE),
                JOptionPane.INFORMATION_MESSAGE
        );
    }

    private final class CertificateTableModel extends AbstractTableModel {
        private final String[] columnNames = {
                ClientCertI18n.t(MessageKeys.CERT_ENABLED),
                ClientCertI18n.t(MessageKeys.CERT_NAME),
                ClientCertI18n.t(MessageKeys.CERT_HOST),
                ClientCertI18n.t(MessageKeys.CERT_PORT),
                ClientCertI18n.t(MessageKeys.CERT_CERT_TYPE)
        };
        private List<ClientCertificate> certificates = new ArrayList<>();

        void loadCertificates(List<ClientCertificate> certificates) {
            this.certificates = certificates == null ? new ArrayList<>() : new ArrayList<>(certificates);
            fireTableDataChanged();
        }

        ClientCertificate getCertificateAt(int row) {
            return certificates.get(row);
        }

        @Override
        public int getRowCount() {
            return certificates.size();
        }

        @Override
        public int getColumnCount() {
            return columnNames.length;
        }

        @Override
        public String getColumnName(int column) {
            return columnNames[column];
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            return columnIndex == 0 ? Boolean.class : String.class;
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            ClientCertificate certificate = certificates.get(rowIndex);
            return switch (columnIndex) {
                case 0 -> certificate.isEnabled();
                case 1 -> certificate.getName() == null ? "" : certificate.getName();
                case 2 -> certificate.getHost();
                case 3 -> certificate.getPort() == 0
                        ? ClientCertI18n.t(MessageKeys.CERT_PORT_ALL)
                        : String.valueOf(certificate.getPort());
                case 4 -> certificate.getCertType();
                default -> "";
            };
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return columnIndex == 0;
        }

        @Override
        public void setValueAt(Object value, int rowIndex, int columnIndex) {
            if (columnIndex != 0) {
                return;
            }
            ClientCertificate certificate = certificates.get(rowIndex);
            certificate.setEnabled(Boolean.TRUE.equals(value));
            certificateService.updateCertificate(certificate);
            fireTableCellUpdated(rowIndex, columnIndex);
            NotificationUtil.showSuccess(ClientCertI18n.t(MessageKeys.CERT_STATUS_UPDATED));
        }
    }

    private record FormRow(String label, Component field, boolean required) {
    }

    private static final class CertificateEditDialog extends JDialog {
        private final ClientCertificate certificate;
        @Getter
        private boolean confirmed;

        private JTextField nameField;
        private JTextField hostField;
        private JTextField portField;
        private JComboBox<String> certTypeCombo;
        private JTextField certPathField;
        private JTextField keyPathField;
        private JPasswordField passwordField;
        private JCheckBox enabledCheckBox;

        CertificateEditDialog(Window owner, ClientCertificate certificate, boolean isNew) {
            super(owner, isNew ? ClientCertI18n.t(MessageKeys.CERT_ADD) : ClientCertI18n.t(MessageKeys.CERT_EDIT),
                    Dialog.ModalityType.APPLICATION_MODAL);
            this.certificate = certificate;
            initUI();
            loadData();
            pack();
            setMinimumSize(new Dimension(600, 450));
            setLocationRelativeTo(owner);
        }

        private void initUI() {
            ToolWindowSurfaceStyle.applyDialogWindowChrome(this);
            setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
            setLayout(new BorderLayout(10, 10));
            ToolWindowSurfaceStyle.applyDialogSurface((JPanel) getContentPane());
            ((JPanel) getContentPane()).setBorder(new EmptyBorder(20, 20, 20, 20));
            add(createFormPanel(), BorderLayout.CENTER);
            add(createDialogButtonPanel(), BorderLayout.SOUTH);
        }

        private JPanel createFormPanel() {
            JPanel formPanel = new JPanel(new GridBagLayout());
            ToolWindowSurfaceStyle.applyDialogSurface(formPanel);
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(8, 8, 8, 8);
            gbc.anchor = GridBagConstraints.WEST;
            gbc.fill = GridBagConstraints.HORIZONTAL;

            addFormRow(formPanel, gbc, 0, new FormRow(
                    ClientCertI18n.t(MessageKeys.CERT_NAME) + ":",
                    nameField = createTextField(30, ClientCertI18n.t(MessageKeys.CERT_NAME_PLACEHOLDER)),
                    false
            ));
            addFormRow(formPanel, gbc, 1, new FormRow(
                    ClientCertI18n.t(MessageKeys.CERT_HOST) + ":",
                    hostField = createTextField(30, ClientCertI18n.t(MessageKeys.CERT_HOST_PLACEHOLDER)),
                    true
            ));
            addFormRow(formPanel, gbc, 2, new FormRow(
                    ClientCertI18n.t(MessageKeys.CERT_PORT) + ":",
                    portField = createTextField(10, ClientCertI18n.t(MessageKeys.CERT_PORT_PLACEHOLDER)),
                    false
            ));

            int row = 3;
            addTypeRow(formPanel, gbc, row++);
            addPathRow(formPanel, gbc, row++, true);
            addPathRow(formPanel, gbc, row++, false);
            addFormRow(formPanel, gbc, row++, new FormRow(
                    ClientCertI18n.t(MessageKeys.CERT_PASSWORD) + ":",
                    passwordField = createPasswordField(30),
                    false
            ));
            addFormRow(formPanel, gbc, row, new FormRow(
                    ClientCertI18n.t(MessageKeys.CERT_ENABLED) + ":",
                    enabledCheckBox = new JCheckBox(),
                    false
            ));

            return formPanel;
        }

        private void addFormRow(JPanel panel, GridBagConstraints gbc, int row, FormRow formRow) {
            gbc.gridx = 0;
            gbc.gridy = row;
            gbc.weightx = 0;
            gbc.gridwidth = 1;
            JLabel label = new JLabel(formRow.label());
            if (formRow.required()) {
                label.setForeground(ModernColors.getError());
            }
            panel.add(label, gbc);

            gbc.gridx = 1;
            gbc.weightx = 1.0;
            gbc.gridwidth = 2;
            panel.add(formRow.field(), gbc);
        }

        private void addTypeRow(JPanel panel, GridBagConstraints gbc, int row) {
            gbc.gridx = 0;
            gbc.gridy = row;
            gbc.weightx = 0;
            gbc.gridwidth = 1;
            panel.add(new JLabel(ClientCertI18n.t(MessageKeys.CERT_CERT_TYPE) + ":"), gbc);

            gbc.gridx = 1;
            gbc.weightx = 1.0;
            gbc.gridwidth = 2;
            certTypeCombo = new JComboBox<>(new String[]{
                    ClientCertI18n.t(MessageKeys.CERT_TYPE_PFX),
                    ClientCertI18n.t(MessageKeys.CERT_TYPE_PEM)
            });
            SettingsInputStyle.apply(certTypeCombo);
            certTypeCombo.addActionListener(e -> updateFieldVisibility());
            panel.add(certTypeCombo, gbc);
        }

        private void addPathRow(JPanel panel, GridBagConstraints gbc, int row, boolean certificatePath) {
            gbc.gridx = 0;
            gbc.gridy = row;
            gbc.weightx = 0;
            gbc.gridwidth = 1;
            JLabel label = new JLabel(ClientCertI18n.t(certificatePath
                    ? MessageKeys.CERT_CERT_PATH
                    : MessageKeys.CERT_KEY_PATH) + ":");
            if (certificatePath) {
                label.setForeground(ModernColors.getError());
            }
            panel.add(label, gbc);

            gbc.gridx = 1;
            gbc.weightx = 1.0;
            gbc.gridwidth = 1;
            JTextField field = createTextField(28, ClientCertI18n.t(certificatePath
                    ? MessageKeys.CERT_CERT_PATH_PLACEHOLDER
                    : MessageKeys.CERT_KEY_PATH_PLACEHOLDER));
            if (certificatePath) {
                certPathField = field;
            } else {
                keyPathField = field;
            }
            panel.add(field, gbc);

            gbc.gridx = 2;
            gbc.weightx = 0;
            JButton selectButton = ModernButtonFactory.createButton(ClientCertI18n.t(MessageKeys.CERT_SELECT_FILE), false);
            selectButton.addActionListener(e -> selectFile(field));
            panel.add(selectButton, gbc);
        }

        private JTextField createTextField(int columns, String placeholder) {
            JTextField field = new JTextField(columns);
            SettingsInputStyle.apply(field);
            if (placeholder != null && !placeholder.isEmpty()) {
                field.setToolTipText(placeholder);
            }
            return field;
        }

        private JPasswordField createPasswordField(int columns) {
            JPasswordField field = new JPasswordField(columns);
            SettingsInputStyle.apply(field);
            return field;
        }

        private JPanel createDialogButtonPanel() {
            JButton cancelButton = ModernButtonFactory.createButton(ClientCertI18n.t(MessageKeys.CERT_CANCEL), false);
            JButton saveButton = ModernButtonFactory.createButton(ClientCertI18n.t(MessageKeys.CERT_SAVE), true);
            saveButton.addActionListener(e -> save());
            cancelButton.addActionListener(e -> dispose());

            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
            ToolWindowSurfaceStyle.applyDialogFooter(buttonPanel);
            buttonPanel.add(cancelButton);
            buttonPanel.add(saveButton);
            return buttonPanel;
        }

        private void updateFieldVisibility() {
            boolean isPem = ClientCertI18n.t(MessageKeys.CERT_TYPE_PEM).equals(certTypeCombo.getSelectedItem());
            keyPathField.setEnabled(isPem);
            if (!isPem) {
                keyPathField.setText("");
            }
        }

        private void loadData() {
            nameField.setText(certificate.getName() == null ? "" : certificate.getName());
            hostField.setText(certificate.getHost() == null ? "" : certificate.getHost());
            portField.setText(certificate.getPort() > 0 ? String.valueOf(certificate.getPort()) : "0");
            certTypeCombo.setSelectedItem(ClientCertificate.CERT_TYPE_PEM.equals(certificate.getCertType())
                    ? ClientCertI18n.t(MessageKeys.CERT_TYPE_PEM)
                    : ClientCertI18n.t(MessageKeys.CERT_TYPE_PFX));
            certPathField.setText(certificate.getCertPath() == null ? "" : certificate.getCertPath());
            keyPathField.setText(certificate.getKeyPath() == null ? "" : certificate.getKeyPath());
            passwordField.setText(certificate.getCertPassword() == null ? "" : certificate.getCertPassword());
            enabledCheckBox.setSelected(certificate.isEnabled());
            updateFieldVisibility();
        }

        private void selectFile(JTextField targetField) {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
            fileChooser.setDialogTitle(ClientCertI18n.t(MessageKeys.CERT_SELECT_FILE));

            String currentPath = targetField.getText();
            if (currentPath != null && !currentPath.trim().isEmpty()) {
                File currentFile = new File(currentPath);
                if (currentFile.getParentFile() != null && currentFile.getParentFile().exists()) {
                    fileChooser.setCurrentDirectory(currentFile.getParentFile());
                }
            }

            if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                targetField.setText(fileChooser.getSelectedFile().getAbsolutePath());
            }
        }

        private void save() {
            String host = hostField.getText().trim();
            if (host.isEmpty()) {
                showError(ClientCertI18n.t(MessageKeys.CERT_VALIDATION_HOST_REQUIRED));
                hostField.requestFocus();
                return;
            }

            String certPath = certPathField.getText().trim();
            if (certPath.isEmpty()) {
                showError(ClientCertI18n.t(MessageKeys.CERT_VALIDATION_CERT_REQUIRED));
                certPathField.requestFocus();
                return;
            }

            boolean isPem = ClientCertI18n.t(MessageKeys.CERT_TYPE_PEM).equals(certTypeCombo.getSelectedItem());
            String keyPath = keyPathField.getText().trim();
            if (isPem && keyPath.isEmpty()) {
                showError(ClientCertI18n.t(MessageKeys.CERT_VALIDATION_KEY_REQUIRED));
                keyPathField.requestFocus();
                return;
            }

            if (!new File(certPath).exists()) {
                showError(MessageFormat.format(ClientCertI18n.t(MessageKeys.CERT_VALIDATION_FILE_NOT_FOUND), certPath));
                certPathField.requestFocus();
                return;
            }
            if (isPem && !new File(keyPath).exists()) {
                showError(MessageFormat.format(ClientCertI18n.t(MessageKeys.CERT_VALIDATION_FILE_NOT_FOUND), keyPath));
                keyPathField.requestFocus();
                return;
            }

            certificate.setName(nameField.getText().trim());
            certificate.setHost(host);
            certificate.setPort(parsePort(portField.getText().trim()));
            certificate.setCertType(isPem ? ClientCertificate.CERT_TYPE_PEM : ClientCertificate.CERT_TYPE_PFX);
            certificate.setCertPath(certPath);
            certificate.setKeyPath(isPem ? keyPath : null);
            certificate.setCertPassword(new String(passwordField.getPassword()));
            certificate.setEnabled(enabledCheckBox.isSelected());

            confirmed = true;
            dispose();
        }

        private int parsePort(String value) {
            if (value == null || value.isBlank()) {
                return 0;
            }
            try {
                return Math.max(0, Integer.parseInt(value));
            } catch (NumberFormatException ignored) {
                return 0;
            }
        }

        private void showError(String message) {
            JOptionPane.showMessageDialog(
                    this,
                    message,
                    ClientCertI18n.t(MessageKeys.CERT_ERROR),
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }
}
