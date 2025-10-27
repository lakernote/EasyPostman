package com.laker.postman.panel.topmenu.setting;

import com.laker.postman.model.ClientCertificate;
import com.laker.postman.service.ClientCertificateService;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.io.File;
import java.text.MessageFormat;

/**
 * 客户端证书设置面板
 * 优化后的版本：使用国际化常量、改进UI布局和交互体验
 */
public class ClientCertificateSettingsPanel extends JPanel {

    private JTable certificateTable;
    private CertificateTableModel tableModel;
    private JButton addBtn;
    private JButton editBtn;
    private JButton deleteBtn;
    private JButton helpBtn;

    public ClientCertificateSettingsPanel() {
        initUI();
        registerListeners();
        loadCertificates();
    }

    private void initUI() {
        setLayout(new BorderLayout(10, 10));
        setBorder(new EmptyBorder(15, 15, 15, 15));

        // 顶部说明面板
        add(createDescriptionPanel(), BorderLayout.NORTH);

        // 表格面板
        add(createTablePanel(), BorderLayout.CENTER);

        // 按钮面板
        add(createButtonPanel(), BorderLayout.SOUTH);
    }

    private JPanel createDescriptionPanel() {
        JPanel panel = new JPanel(new BorderLayout());

        JLabel titleLabel = new JLabel(I18nUtil.getMessage(MessageKeys.CERT_TITLE));
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 16f));

        JLabel descLabel = new JLabel("<html><body style='width: 600px'>" +
                I18nUtil.getMessage(MessageKeys.CERT_DESCRIPTION) +
                "</body></html>");
        descLabel.setFont(descLabel.getFont().deriveFont(Font.PLAIN, 11f));
        descLabel.setForeground(Color.GRAY);

        panel.add(titleLabel, BorderLayout.NORTH);
        panel.add(Box.createVerticalStrut(5), BorderLayout.CENTER);
        panel.add(descLabel, BorderLayout.SOUTH);

        return panel;
    }

    private JScrollPane createTablePanel() {
        tableModel = new CertificateTableModel();
        certificateTable = new JTable(tableModel);
        certificateTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        certificateTable.setRowHeight(28);
        certificateTable.getTableHeader().setReorderingAllowed(false);

        // 设置列宽
        certificateTable.getColumnModel().getColumn(0).setPreferredWidth(60);  // Enabled
        certificateTable.getColumnModel().getColumn(0).setMaxWidth(80);
        certificateTable.getColumnModel().getColumn(1).setPreferredWidth(150); // Name
        certificateTable.getColumnModel().getColumn(2).setPreferredWidth(250); // Host
        certificateTable.getColumnModel().getColumn(3).setPreferredWidth(80);  // Port
        certificateTable.getColumnModel().getColumn(3).setMaxWidth(100);
        certificateTable.getColumnModel().getColumn(4).setPreferredWidth(80);  // Type
        certificateTable.getColumnModel().getColumn(4).setMaxWidth(100);

        // 居中显示某些列
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        certificateTable.getColumnModel().getColumn(3).setCellRenderer(centerRenderer);
        certificateTable.getColumnModel().getColumn(4).setCellRenderer(centerRenderer);

        JScrollPane scrollPane = new JScrollPane(certificateTable);
        return scrollPane;
    }

    private JPanel createButtonPanel() {
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));

        addBtn = new JButton(I18nUtil.getMessage(MessageKeys.CERT_ADD));
        editBtn = new JButton(I18nUtil.getMessage(MessageKeys.CERT_EDIT));
        deleteBtn = new JButton(I18nUtil.getMessage(MessageKeys.CERT_DELETE));
        helpBtn = new JButton(I18nUtil.getMessage(MessageKeys.CERT_HELP));
        buttonPanel.add(addBtn);
        buttonPanel.add(editBtn);
        buttonPanel.add(deleteBtn);
        buttonPanel.add(Box.createHorizontalStrut(20));
        buttonPanel.add(helpBtn);

        return buttonPanel;
    }

    private void registerListeners() {
        addBtn.addActionListener(e -> showAddDialog());
        editBtn.addActionListener(e -> showEditDialog());
        deleteBtn.addActionListener(e -> deleteCertificate());
        helpBtn.addActionListener(e -> showHelp());

        certificateTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                updateButtonStates();
            }
        });

        updateButtonStates();
    }

    private void updateButtonStates() {
        boolean hasSelection = certificateTable.getSelectedRow() >= 0;
        editBtn.setEnabled(hasSelection);
        deleteBtn.setEnabled(hasSelection);
    }

    private void loadCertificates() {
        tableModel.loadCertificates();
    }

    private void showAddDialog() {
        ClientCertificate cert = new ClientCertificate();
        CertificateEditDialog dialog = new CertificateEditDialog(
                SwingUtilities.getWindowAncestor(this), cert, true);
        dialog.setVisible(true);

        if (dialog.isConfirmed()) {
            ClientCertificateService.addCertificate(cert);
            loadCertificates();
        }
    }

    private void showEditDialog() {
        int selectedRow = certificateTable.getSelectedRow();
        if (selectedRow < 0) return;

        ClientCertificate cert = tableModel.getCertificateAt(selectedRow);
        CertificateEditDialog dialog = new CertificateEditDialog(
                SwingUtilities.getWindowAncestor(this), cert, false);
        dialog.setVisible(true);

        if (dialog.isConfirmed()) {
            ClientCertificateService.updateCertificate(cert);
            loadCertificates();
        }
    }

    private void deleteCertificate() {
        int selectedRow = certificateTable.getSelectedRow();
        if (selectedRow < 0) return;

        ClientCertificate cert = tableModel.getCertificateAt(selectedRow);
        String displayName = cert.getName() != null && !cert.getName().isEmpty()
                ? cert.getName()
                : cert.getHost();
        String message = MessageFormat.format(
                I18nUtil.getMessage(MessageKeys.CERT_DELETE_CONFIRM),
                displayName);

        int result = JOptionPane.showConfirmDialog(
                this,
                message,
                I18nUtil.getMessage(MessageKeys.CERT_DELETE),
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);

        if (result == JOptionPane.YES_OPTION) {
            ClientCertificateService.deleteCertificate(cert.getId());
            loadCertificates();
            updateButtonStates();
        }
    }

    private void showHelp() {
        String content = I18nUtil.getMessage(MessageKeys.CERT_HELP_CONTENT);
        JTextArea textArea = new JTextArea(content, 18, 60);
        textArea.setEditable(false);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        textArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        JScrollPane scrollPane = new JScrollPane(textArea);

        JOptionPane.showMessageDialog(
                this,
                scrollPane,
                I18nUtil.getMessage(MessageKeys.CERT_HELP_TITLE),
                JOptionPane.INFORMATION_MESSAGE);
    }

    /**
     * 证书表格模型
     */
    private class CertificateTableModel extends AbstractTableModel {
        private final String[] columnNames = {
                I18nUtil.getMessage(MessageKeys.CERT_ENABLED),
                I18nUtil.getMessage(MessageKeys.CERT_NAME),
                I18nUtil.getMessage(MessageKeys.CERT_HOST),
                I18nUtil.getMessage(MessageKeys.CERT_PORT),
                I18nUtil.getMessage(MessageKeys.CERT_CERT_TYPE)
        };
        private java.util.List<ClientCertificate> certificates = new java.util.ArrayList<>();

        public void loadCertificates() {
            certificates = ClientCertificateService.getAllCertificates();
            fireTableDataChanged();
        }

        public ClientCertificate getCertificateAt(int row) {
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
            if (columnIndex == 0) return Boolean.class;
            return String.class;
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            ClientCertificate cert = certificates.get(rowIndex);
            return switch (columnIndex) {
                case 0 -> cert.isEnabled();
                case 1 -> cert.getName() != null ? cert.getName() : "";
                case 2 -> cert.getHost();
                case 3 -> cert.getPort() == 0 ?
                        I18nUtil.getMessage(MessageKeys.CERT_PORT_ALL) :
                        String.valueOf(cert.getPort());
                case 4 -> cert.getCertType();
                default -> "";
            };
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return columnIndex == 0; // 只允许编辑 Enabled 列
        }

        @Override
        public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
            if (columnIndex == 0) {
                ClientCertificate cert = certificates.get(rowIndex);
                cert.setEnabled((Boolean) aValue);
                ClientCertificateService.updateCertificate(cert);
                fireTableCellUpdated(rowIndex, columnIndex);
            }
        }
    }

    /**
     * 证书编辑对话框
     * 优化后的版本：改进UI布局、表单验证和用户体验
     */
    private static class CertificateEditDialog extends JDialog {
        private final ClientCertificate certificate;
        private boolean confirmed = false;

        private JTextField nameField;
        private JTextField hostField;
        private JTextField portField;
        private JComboBox<String> certTypeCombo;
        private JTextField certPathField;
        private JTextField keyPathField;
        private JPasswordField passwordField;
        private JCheckBox enabledCheckBox;

        public CertificateEditDialog(Window owner, ClientCertificate cert, boolean isNew) {
            super(owner, isNew ? I18nUtil.getMessage(MessageKeys.CERT_ADD) :
                            I18nUtil.getMessage(MessageKeys.CERT_EDIT),
                    Dialog.ModalityType.APPLICATION_MODAL);
            this.certificate = cert;
            initUI();
            loadData();
            pack();
            setLocationRelativeTo(owner);
        }

        private void initUI() {
            setLayout(new BorderLayout(10, 10));
            ((JPanel) getContentPane()).setBorder(new EmptyBorder(20, 20, 20, 20));

            // 表单面板
            add(createFormPanel(), BorderLayout.CENTER);

            // 按钮面板
            add(createDialogButtonPanel(), BorderLayout.SOUTH);

            setMinimumSize(new Dimension(600, 450));
        }

        private JPanel createFormPanel() {
            JPanel formPanel = new JPanel(new GridBagLayout());
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(8, 8, 8, 8);
            gbc.anchor = GridBagConstraints.WEST;
            gbc.fill = GridBagConstraints.HORIZONTAL;

            int row = 0;

            // 名称（可选）
            addFormRow(formPanel, gbc, row++,
                    I18nUtil.getMessage(MessageKeys.CERT_NAME) + ":",
                    nameField = createTextField(30, I18nUtil.getMessage(MessageKeys.CERT_NAME_PLACEHOLDER)),
                    false);

            // 主机名（必填）
            addFormRow(formPanel, gbc, row++,
                    I18nUtil.getMessage(MessageKeys.CERT_HOST) + ":",
                    hostField = createTextField(30, I18nUtil.getMessage(MessageKeys.CERT_HOST_PLACEHOLDER)),
                    true);

            // 端口
            addFormRow(formPanel, gbc, row++,
                    I18nUtil.getMessage(MessageKeys.CERT_PORT) + ":",
                    portField = createTextField(10, I18nUtil.getMessage(MessageKeys.CERT_PORT_ALL)),
                    false);

            // 证书类型
            gbc.gridx = 0;
            gbc.gridy = row;
            gbc.weightx = 0;
            gbc.gridwidth = 1;
            formPanel.add(new JLabel(I18nUtil.getMessage(MessageKeys.CERT_CERT_TYPE) + ":"), gbc);
            gbc.gridx = 1;
            gbc.weightx = 1.0;
            gbc.gridwidth = 2;
            certTypeCombo = new JComboBox<>(new String[]{
                    I18nUtil.getMessage(MessageKeys.CERT_TYPE_PFX),
                    I18nUtil.getMessage(MessageKeys.CERT_TYPE_PEM)
            });
            certTypeCombo.addActionListener(e -> updateFieldVisibility());
            formPanel.add(certTypeCombo, gbc);
            row++;

            // 证书文件（必填）
            gbc.gridx = 0;
            gbc.gridy = row;
            gbc.weightx = 0;
            gbc.gridwidth = 1;
            JLabel certPathLabel = new JLabel(I18nUtil.getMessage(MessageKeys.CERT_CERT_PATH) + ":");
            certPathLabel.setForeground(Color.RED);
            formPanel.add(certPathLabel, gbc);

            gbc.gridx = 1;
            gbc.weightx = 1.0;
            gbc.gridwidth = 1;
            certPathField = createTextField(28, "");
            formPanel.add(certPathField, gbc);

            gbc.gridx = 2;
            gbc.weightx = 0;
            JButton certPathBtn = new JButton(I18nUtil.getMessage(MessageKeys.CERT_SELECT_FILE));
            certPathBtn.addActionListener(e -> selectFile(certPathField, "Certificate Files",
                    "*.pfx", "*.p12", "*.pem", "*.crt"));
            formPanel.add(certPathBtn, gbc);
            row++;

            // 私钥文件（PEM格式时必填）
            gbc.gridx = 0;
            gbc.gridy = row;
            gbc.weightx = 0;
            gbc.gridwidth = 1;
            JLabel keyPathLabel = new JLabel(I18nUtil.getMessage(MessageKeys.CERT_KEY_PATH) + ":");
            formPanel.add(keyPathLabel, gbc);

            gbc.gridx = 1;
            gbc.weightx = 1.0;
            gbc.gridwidth = 1;
            keyPathField = createTextField(28, "");
            formPanel.add(keyPathField, gbc);

            gbc.gridx = 2;
            gbc.weightx = 0;
            JButton keyPathBtn = new JButton(I18nUtil.getMessage(MessageKeys.CERT_SELECT_FILE));
            keyPathBtn.addActionListener(e -> selectFile(keyPathField, "Private Key Files",
                    "*.key", "*.pem"));
            formPanel.add(keyPathBtn, gbc);
            row++;

            // 密码
            addFormRow(formPanel, gbc, row++,
                    I18nUtil.getMessage(MessageKeys.CERT_PASSWORD) + ":",
                    passwordField = new JPasswordField(30),
                    false);

            // 启用
            gbc.gridx = 0;
            gbc.gridy = row;
            gbc.weightx = 0;
            gbc.gridwidth = 1;
            formPanel.add(new JLabel(I18nUtil.getMessage(MessageKeys.CERT_ENABLED) + ":"), gbc);
            gbc.gridx = 1;
            gbc.weightx = 1.0;
            gbc.gridwidth = 2;
            enabledCheckBox = new JCheckBox();
            formPanel.add(enabledCheckBox, gbc);

            return formPanel;
        }

        private void addFormRow(JPanel panel, GridBagConstraints gbc, int row,
                                String label, JTextField field, boolean required) {
            gbc.gridx = 0;
            gbc.gridy = row;
            gbc.weightx = 0;
            gbc.gridwidth = 1;
            JLabel jLabel = new JLabel(label);
            if (required) {
                jLabel.setForeground(Color.RED);
            }
            panel.add(jLabel, gbc);

            gbc.gridx = 1;
            gbc.weightx = 1.0;
            gbc.gridwidth = 2;
            panel.add(field, gbc);
        }

        private JTextField createTextField(int columns, String tooltip) {
            JTextField field = new JTextField(columns);
            if (tooltip != null && !tooltip.isEmpty()) {
                field.setToolTipText(tooltip);
            }
            return field;
        }

        private JPanel createDialogButtonPanel() {
            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));

            JButton saveBtn = new JButton(I18nUtil.getMessage(MessageKeys.CERT_SAVE));
            JButton cancelBtn = new JButton(I18nUtil.getMessage(MessageKeys.CERT_CANCEL));

            saveBtn.addActionListener(e -> save());
            cancelBtn.addActionListener(e -> dispose());

            buttonPanel.add(saveBtn);
            buttonPanel.add(cancelBtn);

            return buttonPanel;
        }

        private void updateFieldVisibility() {
            boolean isPEM = I18nUtil.getMessage(MessageKeys.CERT_TYPE_PEM).equals(certTypeCombo.getSelectedItem());
            keyPathField.setEnabled(isPEM);
            if (!isPEM) {
                keyPathField.setText("");
            }
        }

        private void loadData() {
            nameField.setText(certificate.getName() != null ? certificate.getName() : "");
            hostField.setText(certificate.getHost() != null ? certificate.getHost() : "");
            portField.setText(certificate.getPort() > 0 ? String.valueOf(certificate.getPort()) : "0");

            if (ClientCertificate.CERT_TYPE_PEM.equals(certificate.getCertType())) {
                certTypeCombo.setSelectedItem(I18nUtil.getMessage(MessageKeys.CERT_TYPE_PEM));
            } else {
                certTypeCombo.setSelectedItem(I18nUtil.getMessage(MessageKeys.CERT_TYPE_PFX));
            }

            certPathField.setText(certificate.getCertPath() != null ? certificate.getCertPath() : "");
            keyPathField.setText(certificate.getKeyPath() != null ? certificate.getKeyPath() : "");
            passwordField.setText(certificate.getCertPassword() != null ? certificate.getCertPassword() : "");
            enabledCheckBox.setSelected(certificate.isEnabled());

            updateFieldVisibility();
        }

        private void selectFile(JTextField targetField, String description, String... extensions) {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
            fileChooser.setDialogTitle(I18nUtil.getMessage(MessageKeys.CERT_SELECT_FILE));

            // 设置初始目录为当前文本框的文件路径（如果有）
            String currentPath = targetField.getText();
            if (currentPath != null && !currentPath.trim().isEmpty()) {
                File currentFile = new File(currentPath);
                if (currentFile.getParentFile() != null && currentFile.getParentFile().exists()) {
                    fileChooser.setCurrentDirectory(currentFile.getParentFile());
                }
            }

            if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                File file = fileChooser.getSelectedFile();
                targetField.setText(file.getAbsolutePath());
            }
        }

        private void save() {
            // 验证主机名
            String host = hostField.getText().trim();
            if (host.isEmpty()) {
                showError(I18nUtil.getMessage(MessageKeys.CERT_VALIDATION_HOST_REQUIRED));
                hostField.requestFocus();
                return;
            }

            // 验证证书文件
            String certPath = certPathField.getText().trim();
            if (certPath.isEmpty()) {
                showError(I18nUtil.getMessage(MessageKeys.CERT_VALIDATION_CERT_REQUIRED));
                certPathField.requestFocus();
                return;
            }

            // 验证 PEM 格式需要私钥文件
            boolean isPEM = I18nUtil.getMessage(MessageKeys.CERT_TYPE_PEM).equals(certTypeCombo.getSelectedItem());
            String keyPath = keyPathField.getText().trim();
            if (isPEM && keyPath.isEmpty()) {
                showError(I18nUtil.getMessage(MessageKeys.CERT_VALIDATION_KEY_REQUIRED));
                keyPathField.requestFocus();
                return;
            }

            // 验证文件是否存在
            if (!new File(certPath).exists()) {
                showError(MessageFormat.format(
                        I18nUtil.getMessage(MessageKeys.CERT_VALIDATION_FILE_NOT_FOUND), certPath));
                certPathField.requestFocus();
                return;
            }

            if (isPEM && !new File(keyPath).exists()) {
                showError(MessageFormat.format(
                        I18nUtil.getMessage(MessageKeys.CERT_VALIDATION_FILE_NOT_FOUND), keyPath));
                keyPathField.requestFocus();
                return;
            }

            // 保存数据
            certificate.setName(nameField.getText().trim());
            certificate.setHost(host);

            try {
                int port = Integer.parseInt(portField.getText().trim());
                certificate.setPort(port);
            } catch (NumberFormatException e) {
                certificate.setPort(0);
            }

            certificate.setCertType(isPEM ? ClientCertificate.CERT_TYPE_PEM : ClientCertificate.CERT_TYPE_PFX);
            certificate.setCertPath(certPath);
            certificate.setKeyPath(isPEM ? keyPath : null);
            certificate.setCertPassword(new String(passwordField.getPassword()));
            certificate.setEnabled(enabledCheckBox.isSelected());

            confirmed = true;
            dispose();
        }

        private void showError(String message) {
            JOptionPane.showMessageDialog(this, message,
                    I18nUtil.getMessage(MessageKeys.CERT_ERROR),
                    JOptionPane.ERROR_MESSAGE);
        }

        public boolean isConfirmed() {
            return confirmed;
        }
    }
}

