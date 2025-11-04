package com.laker.postman.panel.topmenu.setting;

import com.laker.postman.common.constants.ModernColors;
import com.laker.postman.model.ClientCertificate;
import com.laker.postman.service.ClientCertificateService;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;
import com.laker.postman.util.NotificationUtil;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.io.File;
import java.text.MessageFormat;
import java.util.List;

/**
 * 现代化客户端证书设置面板
 * 继承 ModernSettingsPanel 获得统一的现代化UI风格
 */
public class ClientCertificateSettingsPanelModern extends ModernSettingsPanel {
    private static final int SECTION_SPACING = 12;

    private JTable certificateTable;
    private CertificateTableModel tableModel;
    private JButton addBtn;
    private JButton editBtn;
    private JButton deleteBtn;
    private JButton helpBtn;
    private Window parentWindow;

    public ClientCertificateSettingsPanelModern(Window parentWindow) {
        this.parentWindow = parentWindow;
    }

    @Override
    protected void buildContent(JPanel contentPanel) {
        // 说明区域
        JPanel descSection = createDescriptionSection();
        contentPanel.add(descSection);
        contentPanel.add(createVerticalSpace(SECTION_SPACING));

        // 证书表格区域
        JPanel tableSection = createModernSection(
                I18nUtil.getMessage(MessageKeys.CERT_LIST_TITLE),
                ""
        );

        // 操作按钮栏
        JPanel actionBar = createActionBar();
        tableSection.add(actionBar);
        tableSection.add(Box.createVerticalStrut(8));

        // 表格
        JScrollPane scrollPane = createTablePanel();
        scrollPane.setAlignmentX(Component.LEFT_ALIGNMENT);
        tableSection.add(scrollPane);

        contentPanel.add(tableSection);
        contentPanel.add(createVerticalSpace(SECTION_SPACING));


        // 加载证书数据
        loadCertificates();
    }

    @Override
    protected void registerListeners() {
        // 证书设置面板不需要 Save/Apply/Cancel 按钮
        // 直接隐藏父类的按钮栏
        if (saveBtn != null) saveBtn.setVisible(false);
        if (applyBtn != null) applyBtn.setVisible(false);
        if (cancelBtn != null) {
            cancelBtn.setText(I18nUtil.getMessage(MessageKeys.CERT_CLOSE));
            cancelBtn.addActionListener(e -> {
                if (parentWindow != null) {
                    parentWindow.dispose();
                }
            });
        }

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

    /**
     * 创建说明区域
     */
    private JPanel createDescriptionSection() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(ModernColors.BG_WHITE);
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel titleLabel = new JLabel(I18nUtil.getMessage(MessageKeys.CERT_TITLE));
        titleLabel.setFont(new Font(titleLabel.getFont().getName(), Font.BOLD, 18));
        titleLabel.setForeground(ModernColors.TEXT_PRIMARY);
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel descLabel = new JLabel("<html><body style='width: 600px'>" +
                I18nUtil.getMessage(MessageKeys.CERT_DESCRIPTION) +
                "</body></html>");
        descLabel.setFont(new Font(descLabel.getFont().getName(), Font.PLAIN, 13));
        descLabel.setForeground(ModernColors.TEXT_SECONDARY);
        descLabel.setBorder(new EmptyBorder(8, 0, 0, 0));
        descLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        panel.add(titleLabel);
        panel.add(descLabel);

        return panel;
    }

    /**
     * 创建操作按钮栏
     */
    private JPanel createActionBar() {
        JPanel actionBar = new JPanel();
        actionBar.setLayout(new BoxLayout(actionBar, BoxLayout.X_AXIS));
        actionBar.setBackground(ModernColors.BG_WHITE);
        actionBar.setAlignmentX(Component.LEFT_ALIGNMENT);
        actionBar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));

        addBtn = createModernButton(I18nUtil.getMessage(MessageKeys.CERT_ADD), true);
        editBtn = createModernButton(I18nUtil.getMessage(MessageKeys.CERT_EDIT), false);
        deleteBtn = createModernButton(I18nUtil.getMessage(MessageKeys.CERT_DELETE), false);
        helpBtn = createIconButton("ℹ️", I18nUtil.getMessage(MessageKeys.CERT_HELP));

        actionBar.add(addBtn);
        actionBar.add(Box.createHorizontalStrut(6));
        actionBar.add(editBtn);
        actionBar.add(Box.createHorizontalStrut(6));
        actionBar.add(deleteBtn);
        actionBar.add(Box.createHorizontalStrut(12));
        actionBar.add(helpBtn);
        actionBar.add(Box.createHorizontalGlue());

        return actionBar;
    }

    /**
     * 创建表格面板
     */
    private JScrollPane createTablePanel() {
        tableModel = new CertificateTableModel();
        certificateTable = new JTable(tableModel);
        certificateTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        certificateTable.setRowHeight(28);
        certificateTable.setShowGrid(true);
        certificateTable.setGridColor(ModernColors.BORDER_LIGHT);
        certificateTable.setBackground(ModernColors.BG_WHITE);
        certificateTable.setSelectionBackground(ModernColors.SELECTED_BG);
        certificateTable.setSelectionForeground(ModernColors.TEXT_PRIMARY);
        certificateTable.getTableHeader().setReorderingAllowed(false);
        certificateTable.getTableHeader().setBackground(ModernColors.BG_LIGHT);
        certificateTable.getTableHeader().setForeground(ModernColors.TEXT_PRIMARY);
        certificateTable.getTableHeader().setFont(new Font(certificateTable.getFont().getName(), Font.BOLD, 12));

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
        certificateTable.getColumnModel().getColumn(0).setCellRenderer(centerRenderer);
        certificateTable.getColumnModel().getColumn(3).setCellRenderer(centerRenderer);
        certificateTable.getColumnModel().getColumn(4).setCellRenderer(centerRenderer);

        JScrollPane scrollPane = new JScrollPane(certificateTable);
        scrollPane.setBorder(BorderFactory.createLineBorder(ModernColors.BORDER_LIGHT, 1));
        scrollPane.setPreferredSize(new Dimension(650, 350));
        scrollPane.setMaximumSize(new Dimension(Integer.MAX_VALUE, 450));

        return scrollPane;
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
            NotificationUtil.showSuccess(I18nUtil.getMessage(MessageKeys.CERT_ADD_SUCCESS));
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
            NotificationUtil.showSuccess(I18nUtil.getMessage(MessageKeys.CERT_EDIT_SUCCESS));
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
            NotificationUtil.showSuccess(I18nUtil.getMessage(MessageKeys.CERT_DELETE_SUCCESS));
        }
    }

    private void showHelp() {
        String content = I18nUtil.getMessage(MessageKeys.CERT_HELP_CONTENT);
        JTextArea textArea = new JTextArea(content, 20, 60);
        textArea.setEditable(false);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        textArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        textArea.setBackground(ModernColors.BG_LIGHT);
        textArea.setBorder(new EmptyBorder(10, 10, 10, 10));

        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setPreferredSize(new Dimension(700, 400));

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
        private List<ClientCertificate> certificates = new java.util.ArrayList<>();

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
                NotificationUtil.showSuccess(I18nUtil.getMessage(MessageKeys.CERT_STATUS_UPDATED));
            }
        }
    }

    /**
     * 证书编辑对话框
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
                    portField = createTextField(10, I18nUtil.getMessage(MessageKeys.CERT_PORT_PLACEHOLDER)),
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
            certPathField = createTextField(28, I18nUtil.getMessage(MessageKeys.CERT_CERT_PATH_PLACEHOLDER));
            formPanel.add(certPathField, gbc);

            gbc.gridx = 2;
            gbc.weightx = 0;
            JButton certPathBtn = new JButton(I18nUtil.getMessage(MessageKeys.CERT_SELECT_FILE));
            certPathBtn.addActionListener(e -> selectFile(certPathField));
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
            keyPathField = createTextField(28, I18nUtil.getMessage(MessageKeys.CERT_KEY_PATH_PLACEHOLDER));
            formPanel.add(keyPathField, gbc);

            gbc.gridx = 2;
            gbc.weightx = 0;
            JButton keyPathBtn = new JButton(I18nUtil.getMessage(MessageKeys.CERT_SELECT_FILE));
            keyPathBtn.addActionListener(e -> selectFile(keyPathField));
            formPanel.add(keyPathBtn, gbc);
            row++;

            // 密码
            gbc.gridx = 0;
            gbc.gridy = row;
            gbc.weightx = 0;
            gbc.gridwidth = 1;
            formPanel.add(new JLabel(I18nUtil.getMessage(MessageKeys.CERT_PASSWORD) + ":"), gbc);
            gbc.gridx = 1;
            gbc.weightx = 1.0;
            gbc.gridwidth = 2;
            passwordField = new JPasswordField(30);
            formPanel.add(passwordField, gbc);
            row++;

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

        private JTextField createTextField(int columns, String placeholder) {
            JTextField field = new JTextField();
            field.setColumns(columns);
            if (placeholder != null && !placeholder.isEmpty()) {
                field.setToolTipText(placeholder);
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

        private void selectFile(JTextField targetField) {
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

