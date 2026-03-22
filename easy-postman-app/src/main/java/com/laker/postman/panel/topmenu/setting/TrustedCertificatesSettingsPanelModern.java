package com.laker.postman.panel.topmenu.setting;

import cn.hutool.json.JSONUtil;
import com.laker.postman.common.constants.ModernColors;
import com.laker.postman.model.TrustedCertificateEntry;
import com.laker.postman.service.http.okhttp.OkHttpClientManager;
import com.laker.postman.service.http.ssl.CustomTrustMaterialLoader;
import com.laker.postman.service.setting.SettingManager;
import com.laker.postman.util.FontsUtil;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;
import com.laker.postman.util.NotificationUtil;
import lombok.Getter;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * 自定义受信任证书 / truststore 设置面板。
 */
public class TrustedCertificatesSettingsPanelModern extends ModernSettingsPanel {
    private static final int SECTION_SPACING = 12;

    private JCheckBox customTrustMaterialEnabledCheckBox;
    private JTable trustMaterialTable;
    private TrustMaterialTableModel tableModel;
    private JButton addBtn;
    private JButton editBtn;
    private JButton deleteBtn;

    private boolean originalEnabled;
    private String originalEntriesSnapshot;

    @Override
    protected void buildContent(JPanel contentPanel) {
        JPanel trustedMaterialSection = createModernSection(
                I18nUtil.getMessage(MessageKeys.SETTINGS_REQUEST_TRUSTED_MATERIAL_TITLE),
                I18nUtil.getMessage(MessageKeys.SETTINGS_REQUEST_TRUSTED_MATERIAL_DESCRIPTION)
        );

        customTrustMaterialEnabledCheckBox = new JCheckBox(
                I18nUtil.getMessage(MessageKeys.SETTINGS_REQUEST_TRUSTED_MATERIAL_ENABLED_CHECKBOX),
                SettingManager.isCustomTrustMaterialEnabled()
        );
        trustedMaterialSection.add(createCheckBoxRow(
                customTrustMaterialEnabledCheckBox,
                I18nUtil.getMessage(MessageKeys.SETTINGS_REQUEST_TRUSTED_MATERIAL_ENABLED_TOOLTIP)
        ));
        trustedMaterialSection.add(createVerticalSpace(8));

        JLabel hintLabel = new JLabel("<html>" +
                I18nUtil.getMessage(MessageKeys.SETTINGS_REQUEST_TRUSTED_MATERIAL_HINT) +
                "</html>");
        hintLabel.setFont(FontsUtil.getDefaultFontWithOffset(Font.PLAIN, -2));
        hintLabel.setForeground(getTextSecondaryColor());
        hintLabel.setBorder(new EmptyBorder(0, 0, 0, 0));
        hintLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        trustedMaterialSection.add(hintLabel);
        trustedMaterialSection.add(createVerticalSpace(12));

        trustedMaterialSection.add(createActionBar());
        trustedMaterialSection.add(createVerticalSpace(8));
        trustedMaterialSection.add(createTablePanel());

        contentPanel.add(trustedMaterialSection);
        contentPanel.add(createVerticalSpace(SECTION_SPACING));

        originalEnabled = customTrustMaterialEnabledCheckBox.isSelected();
        originalEntriesSnapshot = snapshotEntries();
        setHasUnsavedChanges(false);
    }

    @Override
    protected void registerListeners() {
        saveBtn.addActionListener(e -> saveSettings(true));
        applyBtn.addActionListener(e -> saveSettings(false));
        cancelBtn.addActionListener(e -> {
            if (confirmDiscardChanges()) {
                Window window = SwingUtilities.getWindowAncestor(this);
                if (window instanceof JDialog dialog) {
                    dialog.dispose();
                }
            }
        });

        customTrustMaterialEnabledCheckBox.addActionListener(e -> updateDirtyState());
        addBtn.addActionListener(e -> showAddDialog());
        editBtn.addActionListener(e -> showEditDialog());
        deleteBtn.addActionListener(e -> deleteEntry());
        trustMaterialTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                updateButtonStates();
            }
        });
        updateButtonStates();
    }

    private JPanel createActionBar() {
        JPanel actionBar = new JPanel();
        actionBar.setLayout(new BoxLayout(actionBar, BoxLayout.X_AXIS));
        actionBar.setBackground(getCardBackgroundColor());
        actionBar.setAlignmentX(Component.LEFT_ALIGNMENT);
        actionBar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));

        addBtn = createModernButton(I18nUtil.getMessage(MessageKeys.SETTINGS_REQUEST_TRUSTED_MATERIAL_ADD), true);
        editBtn = createModernButton(I18nUtil.getMessage(MessageKeys.SETTINGS_REQUEST_TRUSTED_MATERIAL_EDIT), false);
        deleteBtn = createModernButton(I18nUtil.getMessage(MessageKeys.SETTINGS_REQUEST_TRUSTED_MATERIAL_DELETE), false);

        Dimension buttonSize = new Dimension(88, 28);
        addBtn.setPreferredSize(buttonSize);
        editBtn.setPreferredSize(buttonSize);
        deleteBtn.setPreferredSize(buttonSize);

        actionBar.add(addBtn);
        actionBar.add(Box.createHorizontalStrut(6));
        actionBar.add(editBtn);
        actionBar.add(Box.createHorizontalStrut(6));
        actionBar.add(deleteBtn);
        actionBar.add(Box.createHorizontalGlue());
        return actionBar;
    }

    private JScrollPane createTablePanel() {
        tableModel = new TrustMaterialTableModel(SettingManager.getCustomTrustMaterialEntries());
        trustMaterialTable = new JTable(tableModel);
        trustMaterialTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        trustMaterialTable.setRowHeight(28);
        trustMaterialTable.setShowGrid(true);
        trustMaterialTable.getTableHeader().setReorderingAllowed(false);
        trustMaterialTable.getTableHeader().setFont(FontsUtil.getDefaultFontWithOffset(Font.BOLD, -2));
        trustMaterialTable.setFont(FontsUtil.getDefaultFontWithOffset(Font.PLAIN, -2));
        trustMaterialTable.setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);
        trustMaterialTable.setFillsViewportHeight(true);
        trustMaterialTable.setBackground(getInputBackgroundColor());
        trustMaterialTable.setForeground(getTextPrimaryColor());
        trustMaterialTable.setSelectionBackground(getHoverBackgroundColor());

        trustMaterialTable.getColumnModel().getColumn(0).setPreferredWidth(72);
        trustMaterialTable.getColumnModel().getColumn(0).setMinWidth(68);
        trustMaterialTable.getColumnModel().getColumn(0).setMaxWidth(84);
        trustMaterialTable.getColumnModel().getColumn(1).setPreferredWidth(320);
        trustMaterialTable.getColumnModel().getColumn(2).setPreferredWidth(80);
        trustMaterialTable.getColumnModel().getColumn(2).setMinWidth(74);
        trustMaterialTable.getColumnModel().getColumn(2).setMaxWidth(96);
        trustMaterialTable.getColumnModel().getColumn(3).setPreferredWidth(96);
        trustMaterialTable.getColumnModel().getColumn(3).setMinWidth(88);
        trustMaterialTable.getColumnModel().getColumn(3).setMaxWidth(112);

        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        trustMaterialTable.getColumnModel().getColumn(2).setCellRenderer(centerRenderer);
        trustMaterialTable.getColumnModel().getColumn(3).setCellRenderer(centerRenderer);

        trustMaterialTable.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int row = trustMaterialTable.rowAtPoint(e.getPoint());
                    int col = trustMaterialTable.columnAtPoint(e.getPoint());
                    if (row >= 0 && col != 0) {
                        showEditDialog();
                    }
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(trustMaterialTable);
        scrollPane.setBorder(BorderFactory.createLineBorder(ModernColors.getBorderLightColor(), 1));
        scrollPane.setPreferredSize(new Dimension(520, 260));
        scrollPane.setMaximumSize(new Dimension(640, 340));
        scrollPane.setMinimumSize(new Dimension(360, 180));
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setAlignmentX(Component.LEFT_ALIGNMENT);
        return scrollPane;
    }

    private void saveSettings(boolean closeAfterSave) {
        List<TrustedCertificateEntry> entries = tableModel.getEntries();
        if (customTrustMaterialEnabledCheckBox.isSelected()) {
            if (entries.isEmpty()) {
                NotificationUtil.showError(I18nUtil.getMessage(
                        MessageKeys.SETTINGS_VALIDATION_TRUSTED_MATERIAL_PATH_ERROR));
                return;
            }
            try {
                CustomTrustMaterialLoader.loadTrustManager(entries);
            } catch (Exception ex) {
                NotificationUtil.showError(I18nUtil.getMessage(
                        MessageKeys.SETTINGS_VALIDATION_TRUSTED_MATERIAL_LOAD_ERROR) + ": " + ex.getMessage());
                return;
            }
        }

        try {
            SettingManager.setCustomTrustMaterialEnabled(customTrustMaterialEnabledCheckBox.isSelected());
            SettingManager.setCustomTrustMaterialEntries(entries);
            OkHttpClientManager.clearClientCache();

            originalEnabled = customTrustMaterialEnabledCheckBox.isSelected();
            originalEntriesSnapshot = snapshotEntries();
            setHasUnsavedChanges(false);
            NotificationUtil.showSuccess(I18nUtil.getMessage(MessageKeys.SETTINGS_SAVE_SUCCESS_MESSAGE));

            if (closeAfterSave) {
                Window window = SwingUtilities.getWindowAncestor(this);
                if (window instanceof JDialog dialog) {
                    dialog.dispose();
                }
            }
        } catch (Exception ex) {
            NotificationUtil.showError(I18nUtil.getMessage(MessageKeys.SETTINGS_SAVE_ERROR_MESSAGE) + ": " + ex.getMessage());
        }
    }

    private void showAddDialog() {
        TrustedCertificateEntry entry = new TrustedCertificateEntry();
        TrustMaterialEditDialog dialog = new TrustMaterialEditDialog(
                SwingUtilities.getWindowAncestor(this),
                entry,
                true
        );
        dialog.setVisible(true);
        if (dialog.isConfirmed()) {
            tableModel.addEntry(entry);
            updateButtonStates();
            updateDirtyState();
        }
    }

    private void showEditDialog() {
        if (tableModel.isEmpty()) {
            return;
        }
        int selectedRow = trustMaterialTable.getSelectedRow();
        if (selectedRow < 0) {
            return;
        }
        TrustedCertificateEntry originalEntry = tableModel.getEntryAt(selectedRow);
        TrustedCertificateEntry workingCopy = copyEntry(originalEntry);
        TrustMaterialEditDialog dialog = new TrustMaterialEditDialog(
                SwingUtilities.getWindowAncestor(this),
                workingCopy,
                false
        );
        dialog.setVisible(true);
        if (dialog.isConfirmed()) {
            tableModel.updateEntry(selectedRow, workingCopy);
            updateDirtyState();
        }
    }

    private void deleteEntry() {
        if (tableModel.isEmpty()) {
            return;
        }
        int selectedRow = trustMaterialTable.getSelectedRow();
        if (selectedRow < 0) {
            return;
        }
        int result = JOptionPane.showConfirmDialog(
                this,
                tableModel.getEntryAt(selectedRow).getPath(),
                I18nUtil.getMessage(MessageKeys.SETTINGS_REQUEST_TRUSTED_MATERIAL_DELETE),
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );
        if (result == JOptionPane.YES_OPTION) {
            tableModel.removeEntry(selectedRow);
            updateButtonStates();
            updateDirtyState();
        }
    }

    private void updateButtonStates() {
        boolean hasSelection = trustMaterialTable != null
                && tableModel != null
                && !tableModel.isEmpty()
                && trustMaterialTable.getSelectedRow() >= 0;
        if (editBtn != null) {
            editBtn.setEnabled(hasSelection);
        }
        if (deleteBtn != null) {
            deleteBtn.setEnabled(hasSelection);
        }
    }

    private void updateDirtyState() {
        boolean changed = customTrustMaterialEnabledCheckBox.isSelected() != originalEnabled
                || !snapshotEntries().equals(originalEntriesSnapshot);
        setHasUnsavedChanges(changed);
    }

    private String snapshotEntries() {
        return JSONUtil.toJsonStr(tableModel != null ? tableModel.getEntries() : List.of());
    }

    private TrustedCertificateEntry copyEntry(TrustedCertificateEntry entry) {
        TrustedCertificateEntry copy = new TrustedCertificateEntry();
        copy.setEnabled(entry.isEnabled());
        copy.setPath(entry.getPath());
        copy.setPassword(entry.getPassword());
        return copy;
    }

    private final class TrustMaterialTableModel extends AbstractTableModel {
        private final String[] columnNames = {
                I18nUtil.getMessage(MessageKeys.CERT_ENABLED),
                I18nUtil.getMessage(MessageKeys.SETTINGS_REQUEST_TRUSTED_MATERIAL_FILE_COLUMN),
                I18nUtil.getMessage(MessageKeys.SETTINGS_REQUEST_TRUSTED_MATERIAL_TYPE_COLUMN),
                I18nUtil.getMessage(MessageKeys.SETTINGS_REQUEST_TRUSTED_MATERIAL_PASSWORD_COLUMN)
        };
        private final List<TrustedCertificateEntry> entries = new ArrayList<>();

        private TrustMaterialTableModel(List<TrustedCertificateEntry> initialEntries) {
            if (initialEntries != null) {
                for (TrustedCertificateEntry entry : initialEntries) {
                    entries.add(copyEntry(entry));
                }
            }
        }

        @Override
        public int getRowCount() {
            return entries.isEmpty() ? 1 : entries.size();
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
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return !entries.isEmpty() && columnIndex == 0;
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            if (entries.isEmpty()) {
                return columnIndex == 1
                        ? I18nUtil.getMessage(MessageKeys.SETTINGS_REQUEST_TRUSTED_MATERIAL_DIALOG_EMPTY)
                        : columnIndex == 0 ? Boolean.FALSE : "";
            }

            TrustedCertificateEntry entry = entries.get(rowIndex);
            return switch (columnIndex) {
                case 0 -> entry.isEnabled();
                case 1 -> entry.getPath();
                case 2 -> entry.getDisplayType();
                case 3 -> entry.getMaskedPasswordHint();
                default -> "";
            };
        }

        @Override
        public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
            if (entries.isEmpty() || columnIndex != 0) {
                return;
            }
            entries.get(rowIndex).setEnabled(Boolean.TRUE.equals(aValue));
            fireTableCellUpdated(rowIndex, columnIndex);
            updateDirtyState();
        }

        private List<TrustedCertificateEntry> getEntries() {
            List<TrustedCertificateEntry> copies = new ArrayList<>();
            for (TrustedCertificateEntry entry : entries) {
                copies.add(copyEntry(entry));
            }
            return copies;
        }

        private TrustedCertificateEntry getEntryAt(int row) {
            return entries.get(row);
        }

        private void addEntry(TrustedCertificateEntry entry) {
            entries.add(copyEntry(entry));
            fireTableDataChanged();
        }

        private void updateEntry(int row, TrustedCertificateEntry entry) {
            entries.set(row, copyEntry(entry));
            fireTableRowsUpdated(row, row);
        }

        private void removeEntry(int row) {
            entries.remove(row);
            fireTableDataChanged();
        }

        private boolean isEmpty() {
            return entries.isEmpty();
        }
    }

    private static final class TrustMaterialEditDialog extends JDialog {
        private final TrustedCertificateEntry entry;
        @Getter
        private boolean confirmed = false;

        private JTextField pathField;
        private JPasswordField passwordField;
        private JCheckBox enabledCheckBox;

        private TrustMaterialEditDialog(Window owner, TrustedCertificateEntry entry, boolean isNew) {
            super(owner,
                    isNew
                            ? I18nUtil.getMessage(MessageKeys.SETTINGS_REQUEST_TRUSTED_MATERIAL_DIALOG_ADD_TITLE)
                            : I18nUtil.getMessage(MessageKeys.SETTINGS_REQUEST_TRUSTED_MATERIAL_DIALOG_EDIT_TITLE),
                    Dialog.ModalityType.APPLICATION_MODAL);
            this.entry = entry;
            initUI(owner);
        }

        private void initUI(Window owner) {
            setLayout(new BorderLayout(10, 10));
            ((JPanel) getContentPane()).setBorder(new EmptyBorder(18, 18, 18, 18));

            JPanel formPanel = new JPanel(new GridBagLayout());
            formPanel.setOpaque(false);
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(6, 6, 6, 6);
            gbc.anchor = GridBagConstraints.WEST;
            gbc.fill = GridBagConstraints.HORIZONTAL;

            JLabel pathLabel = new JLabel(I18nUtil.getMessage(MessageKeys.SETTINGS_REQUEST_TRUSTED_MATERIAL_PATH));
            pathLabel.setFont(FontsUtil.getDefaultFontWithOffset(Font.PLAIN, -1));
            gbc.gridx = 0;
            gbc.gridy = 0;
            gbc.weightx = 0;
            formPanel.add(pathLabel, gbc);

            pathField = new JTextField(entry.getPath(), 30);
            pathField.setToolTipText(I18nUtil.getMessage(MessageKeys.SETTINGS_REQUEST_TRUSTED_MATERIAL_PATH_TOOLTIP));
            gbc.gridx = 1;
            gbc.weightx = 1.0;
            formPanel.add(pathField, gbc);

            JButton browseButton = new JButton(I18nUtil.getMessage(MessageKeys.SETTINGS_REQUEST_TRUSTED_MATERIAL_BROWSE));
            gbc.gridx = 2;
            gbc.weightx = 0;
            formPanel.add(browseButton, gbc);

            JLabel passwordLabel = new JLabel(I18nUtil.getMessage(MessageKeys.SETTINGS_REQUEST_TRUSTED_MATERIAL_PASSWORD));
            passwordLabel.setFont(FontsUtil.getDefaultFontWithOffset(Font.PLAIN, -1));
            gbc.gridx = 0;
            gbc.gridy = 1;
            gbc.weightx = 0;
            formPanel.add(passwordLabel, gbc);

            passwordField = new JPasswordField(entry.getPassword(), 30);
            passwordField.setToolTipText(I18nUtil.getMessage(MessageKeys.SETTINGS_REQUEST_TRUSTED_MATERIAL_PASSWORD_TOOLTIP));
            gbc.gridx = 1;
            gbc.gridwidth = 2;
            gbc.weightx = 1.0;
            formPanel.add(passwordField, gbc);
            gbc.gridwidth = 1;

            JLabel enabledLabel = new JLabel(I18nUtil.getMessage(MessageKeys.CERT_ENABLED));
            enabledLabel.setFont(FontsUtil.getDefaultFontWithOffset(Font.PLAIN, -1));
            gbc.gridx = 0;
            gbc.gridy = 2;
            gbc.weightx = 0;
            formPanel.add(enabledLabel, gbc);

            enabledCheckBox = new JCheckBox();
            enabledCheckBox.setSelected(entry.isEnabled());
            enabledCheckBox.setOpaque(false);
            gbc.gridx = 1;
            gbc.weightx = 1.0;
            gbc.gridwidth = 2;
            formPanel.add(enabledCheckBox, gbc);

            JLabel hintLabel = new JLabel("<html>" +
                    I18nUtil.getMessage(MessageKeys.SETTINGS_REQUEST_TRUSTED_MATERIAL_HINT) +
                    "</html>");
            hintLabel.setFont(FontsUtil.getDefaultFontWithOffset(Font.PLAIN, -2));
            hintLabel.setForeground(ModernColors.getTextSecondary());
            hintLabel.setBorder(new EmptyBorder(4, 0, 0, 0));
            gbc.gridx = 0;
            gbc.gridy = 3;
            gbc.gridwidth = 3;
            formPanel.add(hintLabel, gbc);

            add(formPanel, BorderLayout.CENTER);

            JButton cancelButton = new JButton(I18nUtil.getMessage(MessageKeys.SETTINGS_DIALOG_CANCEL));
            JButton okButton = new JButton(I18nUtil.getMessage(MessageKeys.SETTINGS_DIALOG_SAVE));
            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
            buttonPanel.setOpaque(false);
            buttonPanel.add(cancelButton);
            buttonPanel.add(okButton);
            add(buttonPanel, BorderLayout.SOUTH);

            browseButton.addActionListener(e -> chooseFile());
            cancelButton.addActionListener(e -> dispose());
            okButton.addActionListener(e -> onConfirm());

            pack();
            setMinimumSize(new Dimension(660, 240));
            setLocationRelativeTo(owner);
        }

        private void chooseFile() {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
            fileChooser.setDialogTitle(I18nUtil.getMessage(
                    MessageKeys.SETTINGS_REQUEST_TRUSTED_MATERIAL_DIALOG_BROWSE_TITLE));
            String currentPath = pathField.getText().trim();
            if (!currentPath.isEmpty()) {
                File currentFile = new File(currentPath);
                File parent = currentFile.isDirectory() ? currentFile : currentFile.getParentFile();
                if (parent != null && parent.exists()) {
                    fileChooser.setCurrentDirectory(parent);
                }
            }
            if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                File file = fileChooser.getSelectedFile();
                if (file != null) {
                    pathField.setText(file.getAbsolutePath());
                }
            }
        }

        private void onConfirm() {
            String path = pathField.getText().trim();
            if (path.isEmpty() || !new File(path).isFile()) {
                NotificationUtil.showError(I18nUtil.getMessage(
                        MessageKeys.SETTINGS_VALIDATION_TRUSTED_MATERIAL_PATH_ERROR));
                return;
            }
            try {
                CustomTrustMaterialLoader.loadTrustManager(path, new String(passwordField.getPassword()));
            } catch (Exception ex) {
                NotificationUtil.showError(I18nUtil.getMessage(
                        MessageKeys.SETTINGS_VALIDATION_TRUSTED_MATERIAL_LOAD_ERROR) + ": " + ex.getMessage());
                return;
            }

            entry.setPath(path);
            entry.setPassword(new String(passwordField.getPassword()));
            entry.setEnabled(enabledCheckBox.isSelected());
            confirmed = true;
            dispose();
        }
    }
}
