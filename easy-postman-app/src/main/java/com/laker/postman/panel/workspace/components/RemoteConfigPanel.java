package com.laker.postman.panel.workspace.components;

import com.laker.postman.common.component.ToolWindowSurfaceStyle;
import com.laker.postman.common.component.button.ModernButtonFactory;
import com.laker.postman.common.component.setting.SettingsInputStyle;
import com.laker.postman.common.constants.ModernColors;
import com.laker.postman.model.GitAuthType;
import com.laker.postman.model.Workspace;
import com.laker.postman.service.WorkspaceService;
import com.laker.postman.util.FontsUtil;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;
import lombok.extern.slf4j.Slf4j;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.*;
import java.util.List;

import static com.laker.postman.util.MessageKeys.WORKSPACE_CONFIG_PROGRESS_DONE;
import static com.laker.postman.util.MessageKeys.WORKSPACE_CONFIG_PROGRESS_FAILED;
import static com.laker.postman.util.MessageKeys.WORKSPACE_CONFIG_PROGRESS_START;
import static com.laker.postman.util.MessageKeys.WORKSPACE_CONFIG_PROGRESS_VALIDATING;
import static com.laker.postman.util.MessageKeys.WORKSPACE_VALIDATION_GIT_URL_INVALID;

/**
 * Embedded remote repository configuration panel for initialized Git workspaces.
 */
@Slf4j
public class RemoteConfigPanel extends JPanel {

    private static final int FORM_LABEL_WIDTH = 128;
    private static final int FORM_LABEL_GAP = 12;
    private static final int URL_FIELD_WIDTH = 520;
    private static final int FORM_MAX_WIDTH = FORM_LABEL_WIDTH + FORM_LABEL_GAP + URL_FIELD_WIDTH;
    private static final int BRANCH_FIELD_WIDTH = 260;
    private static final int FORM_CONTROL_HEIGHT = 32;
    private static final int PROGRESS_BAR_WIDTH = 140;
    private static final int PROGRESS_BAR_HEIGHT = 4;

    private final transient Workspace workspace;
    private final transient Runnable configuredAction;
    private JTextField remoteUrlField;
    private JTextField remoteBranchField;
    private GitAuthPanel gitAuthPanel;
    private JLabel statusLabel;
    private JProgressBar progressBar;
    private JButton saveButton;

    public RemoteConfigPanel(Workspace workspace, Runnable configuredAction) {
        this.workspace = workspace;
        this.configuredAction = configuredAction;
        initComponents();
        initUI();
    }

    private void initComponents() {
        remoteUrlField = new JTextField(32);
        SettingsInputStyle.apply(remoteUrlField);

        remoteBranchField = new JTextField(18);
        remoteBranchField.setText(defaultRemoteBranch());
        SettingsInputStyle.apply(remoteBranchField);

        gitAuthPanel = new GitAuthPanel();

        statusLabel = new JLabel(I18nUtil.getMessage(MessageKeys.PROGRESS_PANEL_FILL_CONFIG));
        statusLabel.setFont(FontsUtil.getDefaultFontWithOffset(Font.PLAIN, -1));
        statusLabel.setForeground(ModernColors.getTextSecondary());

        progressBar = new JProgressBar();
        progressBar.setStringPainted(false);
        progressBar.setPreferredSize(new Dimension(PROGRESS_BAR_WIDTH, PROGRESS_BAR_HEIGHT));
        progressBar.setMinimumSize(new Dimension(PROGRESS_BAR_WIDTH, PROGRESS_BAR_HEIGHT));
        progressBar.setVisible(false);

        saveButton = ModernButtonFactory.createCompactButton(
                I18nUtil.getMessage(MessageKeys.BUTTON_SAVE),
                false,
                "icons/save.svg"
        );
        saveButton.addActionListener(e -> configureRemote());
    }

    private void initUI() {
        setLayout(new BorderLayout());
        setOpaque(false);
        add(createHeader(), BorderLayout.NORTH);
        add(createContentPanel(), BorderLayout.CENTER);
    }

    private JPanel createHeader() {
        JPanel header = new JPanel(new MigLayout(
                "insets 0, fillx, wrap 1, novisualpadding",
                "[left]",
                "[]"
        ));
        ToolWindowSurfaceStyle.applySectionHeader(header, 8, 18, 8, 18);
        header.add(createHeaderActionRow(), "w " + FORM_MAX_WIDTH + "!, growx 0");
        return header;
    }

    private JPanel createHeaderActionRow() {
        JPanel row = new JPanel(new MigLayout(
                "insets 0, fillx, novisualpadding",
                "[grow,fill]8[]",
                "[]"
        ));
        row.setOpaque(false);
        row.add(createHeaderTitlePanel(), "growx, wmin 0");
        row.add(saveButton, "aligny center, h " + ModernButtonFactory.COMPACT_BUTTON_HEIGHT + "!");
        return row;
    }

    private JPanel createHeaderTitlePanel() {
        JPanel titlePanel = new JPanel(new GridLayout(2, 1, 0, 2));
        titlePanel.setOpaque(false);
        JLabel title = new JLabel(I18nUtil.getMessage(MessageKeys.WORKSPACE_REMOTE_CONFIG_TITLE));
        title.setFont(FontsUtil.getDefaultFontWithOffset(Font.BOLD, 1));
        JLabel workspaceLabel = new JLabel(I18nUtil.getMessage(MessageKeys.WORKSPACE_NAME) + ": " + workspace.getName());
        workspaceLabel.setFont(FontsUtil.getDefaultFontWithOffset(Font.PLAIN, -1));
        workspaceLabel.setForeground(ModernColors.getTextSecondary());
        workspaceLabel.setToolTipText(workspace.getName());
        titlePanel.add(title);
        titlePanel.add(workspaceLabel);
        return titlePanel;
    }

    private JPanel createContentPanel() {
        JPanel panel = new JPanel(new MigLayout(
                "insets 10 18 12 18, fillx, wrap 1, novisualpadding",
                "[left]",
                "[]8[]"
        ));
        panel.setOpaque(false);
        panel.add(createConfigPanel(), "w " + FORM_MAX_WIDTH + "!, growx 0");
        panel.add(createStatusPanel(), "w " + FORM_MAX_WIDTH + "!, growx 0");
        return panel;
    }

    private JPanel createConfigPanel() {
        JPanel formPanel = new JPanel(new MigLayout(
                "insets 0, fillx, wrap 1, novisualpadding",
                "[grow,fill]",
                "[]10[]10[]"
        ));
        formPanel.setOpaque(false);
        formPanel.add(createRepositoryFieldsPanel(), "growx");
        formPanel.add(createSeparator(), "growx");
        formPanel.add(gitAuthPanel, "growx");
        return formPanel;
    }

    private JPanel createRepositoryFieldsPanel() {
        JPanel panel = new JPanel(new MigLayout(
                "insets 0, fillx, wrap 2, novisualpadding",
                "[" + FORM_LABEL_WIDTH + "!,right]" + FORM_LABEL_GAP + "[grow,fill]",
                "[" + FORM_CONTROL_HEIGHT + "!]8[" + FORM_CONTROL_HEIGHT + "!]"
        ));
        panel.setOpaque(false);
        panel.add(createFormLabel(I18nUtil.getMessage(MessageKeys.WORKSPACE_GIT_URL) + ":"));
        panel.add(remoteUrlField, "w " + URL_FIELD_WIDTH + "!, h " + FORM_CONTROL_HEIGHT + "!");
        panel.add(createFormLabel(I18nUtil.getMessage(MessageKeys.WORKSPACE_DETAIL_REMOTE_BRANCH) + ":"));
        panel.add(remoteBranchField, "w " + BRANCH_FIELD_WIDTH + "!, h " + FORM_CONTROL_HEIGHT + "!");
        return panel;
    }

    private JSeparator createSeparator() {
        JSeparator separator = new JSeparator();
        separator.setForeground(ModernColors.getTabSeparatorColor());
        separator.setBackground(ModernColors.getTabSeparatorColor());
        return separator;
    }

    private JPanel createStatusPanel() {
        JPanel panel = new JPanel(new MigLayout(
                "insets 2 0 0 0, fillx, hidemode 3, novisualpadding",
                "[grow,fill]8[]",
                "[]"
        ));
        panel.setOpaque(false);
        panel.add(statusLabel, "growx, wmin 0");
        panel.add(progressBar, "w " + PROGRESS_BAR_WIDTH + "!, h " + PROGRESS_BAR_HEIGHT + "!");
        return panel;
    }

    private JLabel createFormLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(FontsUtil.getDefaultFontWithOffset(Font.PLAIN, -1));
        label.setForeground(ModernColors.getTextPrimary());
        return label;
    }

    private void configureRemote() {
        try {
            validateInput();
        } catch (IllegalArgumentException ex) {
            showStatus(ex.getMessage(), true);
            return;
        }

        setBusy(true, I18nUtil.getMessage(WORKSPACE_CONFIG_PROGRESS_START));
        SwingWorker<Void, String> worker = createWorkerTask();
        worker.addPropertyChangeListener(evt -> {
            if ("progress".equals(evt.getPropertyName())) {
                progressBar.setValue((Integer) evt.getNewValue());
            }
        });
        worker.execute();
    }

    private SwingWorker<Void, String> createWorkerTask() {
        return new SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                publish(I18nUtil.getMessage(WORKSPACE_CONFIG_PROGRESS_START));
                setProgress(10);

                try {
                    String url = remoteUrlField.getText().trim();
                    String branch = remoteBranchField.getText().trim();
                    GitAuthType authType = (GitAuthType) gitAuthPanel.getAuthTypeCombo().getSelectedItem();

                    publish(I18nUtil.getMessage(WORKSPACE_CONFIG_PROGRESS_VALIDATING));
                    setProgress(30);

                    WorkspaceService.getInstance().addRemoteRepository(
                            workspace.getId(),
                            url,
                            branch,
                            authType,
                            gitAuthPanel.getUsername(),
                            gitAuthPanel.getPassword(),
                            gitAuthPanel.getToken(),
                            gitAuthPanel.getSshKeyPath(),
                            gitAuthPanel.getSshPassphrase()
                    );

                    publish(I18nUtil.getMessage(WORKSPACE_CONFIG_PROGRESS_DONE));
                    setProgress(100);
                } catch (Exception e) {
                    throw new RuntimeException(I18nUtil.getMessage(WORKSPACE_CONFIG_PROGRESS_FAILED, e.getMessage()), e);
                }
                return null;
            }

            @Override
            protected void process(List<String> chunks) {
                if (!chunks.isEmpty()) {
                    showStatus(chunks.get(chunks.size() - 1), false);
                }
            }

            @Override
            protected void done() {
                try {
                    get();
                    showStatus(I18nUtil.getMessage(MessageKeys.WORKSPACE_OPERATION_SUCCESS), false);
                    if (configuredAction != null) {
                        configuredAction.run();
                    }
                } catch (Exception e) {
                    log.warn("Failed to configure remote repository for workspace: {}", workspace.getId(), e);
                    showStatus(I18nUtil.getMessage(MessageKeys.WORKSPACE_OPERATION_FAILED_DETAIL, rootMessage(e)), true);
                } finally {
                    setBusy(false, null);
                }
            }
        };
    }

    private void validateInput() {
        String url = remoteUrlField.getText().trim();
        if (url.isEmpty()) {
            throw new IllegalArgumentException(I18nUtil.getMessage(MessageKeys.WORKSPACE_VALIDATION_GIT_URL_REQUIRED));
        }
        if (!url.startsWith("http://") && !url.startsWith("https://") && !url.startsWith("git@")) {
            throw new IllegalArgumentException(I18nUtil.getMessage(WORKSPACE_VALIDATION_GIT_URL_INVALID));
        }

        String branch = remoteBranchField.getText().trim();
        if (branch.isEmpty()) {
            throw new IllegalArgumentException(I18nUtil.getMessage(MessageKeys.WORKSPACE_VALIDATION_GIT_BRANCH_INVALID));
        }

        gitAuthPanel.validateAuth();
    }

    private void setBusy(boolean busy, String status) {
        setInputComponentsEnabled(!busy);
        saveButton.setEnabled(!busy);
        progressBar.setVisible(busy);
        progressBar.setValue(0);
        if (status != null) {
            showStatus(status, false);
        }
    }

    private void setInputComponentsEnabled(boolean enabled) {
        remoteUrlField.setEnabled(enabled);
        remoteBranchField.setEnabled(enabled);
        gitAuthPanel.setComponentsEnabled(enabled);
    }

    private void showStatus(String text, boolean error) {
        statusLabel.setText(text == null ? "" : text);
        statusLabel.setForeground(error ? ModernColors.getError() : ModernColors.getTextSecondary());
    }

    private String defaultRemoteBranch() {
        String branch = workspace.getRemoteBranch();
        if (branch == null || branch.isBlank()) {
            branch = workspace.getCurrentBranch();
        }
        return branch == null || branch.isBlank() ? "master" : branch;
    }

    private static String rootMessage(Exception e) {
        return e.getCause() != null && e.getCause().getMessage() != null
                ? e.getCause().getMessage()
                : e.getMessage();
    }
}
