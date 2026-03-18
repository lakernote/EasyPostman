package com.laker.postman.common.component.dialog;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.laker.postman.common.SingletonFactory;
import com.laker.postman.frame.MainFrame;
import com.laker.postman.model.Workspace;
import com.laker.postman.model.WorkspaceType;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.*;
import java.util.List;

/**
 * 工作区选择对话框 - 可复用组件
 * 用于在多个场景中选择目标工作区（如转移环境、转移集合等）
 */
public class WorkspaceSelectionDialog extends JDialog {
    private final JList<Workspace> workspaceList;
    private final DefaultListModel<Workspace> listModel;
    private Workspace selectedWorkspace;
    private final JTextField searchField;

    public WorkspaceSelectionDialog(String title, List<Workspace> workspaces) {
        super(SingletonFactory.getInstance(MainFrame.class), title, true);
        setSize(500, 400);
        setLocationRelativeTo(SingletonFactory.getInstance(MainFrame.class));
        setLayout(new BorderLayout(10, 10));

        // 主面板
        JPanel mainPanel = new JPanel(new BorderLayout(5, 5));
        mainPanel.setBorder(new EmptyBorder(15, 15, 15, 15));

        // 顶部搜索框
        JPanel searchPanel = new JPanel(new BorderLayout(5, 0));
        JLabel searchLabel = new JLabel(I18nUtil.getMessage(MessageKeys.GENERAL_SEARCH) + ":");
        searchField = new JTextField();
        searchField.setPreferredSize(new Dimension(0, 30));
        searchPanel.add(searchLabel, BorderLayout.WEST);
        searchPanel.add(searchField, BorderLayout.CENTER);
        mainPanel.add(searchPanel, BorderLayout.NORTH);

        // 创建工作区列表
        listModel = new DefaultListModel<>();
        for (Workspace workspace : workspaces) {
            listModel.addElement(workspace);
        }

        workspaceList = new JList<>(listModel);
        workspaceList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        workspaceList.setFixedCellHeight(40);
        workspaceList.setCellRenderer(new WorkspaceListCellRenderer());

        JScrollPane scrollPane = new JScrollPane(workspaceList);
        scrollPane.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
        mainPanel.add(scrollPane, BorderLayout.CENTER);

        // 说明文字
        JLabel hintLabel = new JLabel(I18nUtil.getMessage(MessageKeys.WORKSPACE_SELECT_HINT));
        hintLabel.setFont(hintLabel.getFont().deriveFont(Font.ITALIC, 11f));
        hintLabel.setForeground(Color.GRAY);
        hintLabel.setBorder(new EmptyBorder(5, 0, 0, 0));
        mainPanel.add(hintLabel, BorderLayout.SOUTH);

        add(mainPanel, BorderLayout.CENTER);

        // 按钮面板
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        buttonPanel.setBorder(new EmptyBorder(0, 15, 10, 15));

        JButton okButton = new JButton(I18nUtil.getMessage(MessageKeys.GENERAL_OK));
        JButton cancelButton = new JButton(I18nUtil.getMessage(MessageKeys.GENERAL_CANCEL));

        okButton.setPreferredSize(new Dimension(80, 30));
        cancelButton.setPreferredSize(new Dimension(80, 30));

        okButton.addActionListener(e -> onOkClicked());
        cancelButton.addActionListener(e -> onCancelClicked());

        buttonPanel.add(cancelButton);
        buttonPanel.add(okButton);
        add(buttonPanel, BorderLayout.SOUTH);

        // 注册事件监听器
        registerListeners(workspaces);

        // 自动选中第一个
        if (!listModel.isEmpty()) {
            workspaceList.setSelectedIndex(0);
        }

        // 设置默认按钮
        getRootPane().setDefaultButton(okButton);
    }

    private void registerListeners(List<Workspace> allWorkspaces) {
        // 搜索框监听
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                filterWorkspaces();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                filterWorkspaces();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                filterWorkspaces();
            }

            private void filterWorkspaces() {
                String filter = searchField.getText().trim().toLowerCase();
                listModel.clear();
                for (Workspace workspace : allWorkspaces) {
                    if (filter.isEmpty() ||
                            workspace.getName().toLowerCase().contains(filter) ||
                            (workspace.getDescription() != null && workspace.getDescription().toLowerCase().contains(filter))) {
                        listModel.addElement(workspace);
                    }
                }
                // 自动选中第一个搜索结果
                if (!listModel.isEmpty()) {
                    workspaceList.setSelectedIndex(0);
                }
            }
        });

        // 双击确认
        workspaceList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int index = workspaceList.locationToIndex(e.getPoint());
                    if (index >= 0) {
                        workspaceList.setSelectedIndex(index);
                        onOkClicked();
                    }
                }
            }
        });

        // Enter键确认
        workspaceList.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    onOkClicked();
                }
            }
        });

        // ESC键取消
        KeyStroke escapeKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(escapeKeyStroke, "ESCAPE");
        getRootPane().getActionMap().put("ESCAPE", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onCancelClicked();
            }
        });
    }

    private void onOkClicked() {
        Workspace selected = workspaceList.getSelectedValue();
        if (selected == null) {
            JOptionPane.showMessageDialog(this,
                    I18nUtil.getMessage(MessageKeys.WORKSPACE_SELECT_REQUIRED),
                    I18nUtil.getMessage(MessageKeys.GENERAL_TIP),
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        selectedWorkspace = selected;
        dispose();
    }

    private void onCancelClicked() {
        selectedWorkspace = null;
        dispose();
    }

    /**
     * 显示对话框并返回选择的工作区
     *
     * @return 选择的工作区，如果取消则返回null
     */
    public Workspace showDialog() {
        setVisible(true);
        return selectedWorkspace;
    }

    /**
     * 工作区列表单元格渲染器
     */
    private static class WorkspaceListCellRenderer extends DefaultListCellRenderer {
        private static final String HTML_START = "<html>";
        private static final String HTML_END = "</html>";

        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                      boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

            if (value instanceof Workspace workspace) {
                // 根据工作区类型设置图标
                FlatSVGIcon icon = workspace.getType() == WorkspaceType.GIT
                        ? new FlatSVGIcon("icons/git.svg", 20, 20)
                        : new FlatSVGIcon("icons/local.svg", 18, 18);
                setIcon(icon);

                // 构建显示文本：名称 + 类型 + 描述
                StringBuilder text = new StringBuilder();
                text.append(HTML_START);
                text.append("<b>").append(workspace.getName()).append("</b>");
                text.append("<br>");
                text.append("<small style='color: gray;'>");

                // 显示工作区类型
                text.append(workspace.getType() == WorkspaceType.LOCAL
                        ? I18nUtil.getMessage(MessageKeys.WORKSPACE_TYPE_LOCAL)
                        : I18nUtil.getMessage(MessageKeys.WORKSPACE_TYPE_GIT));

                // 如果有描述，追加显示
                if (workspace.getDescription() != null && !workspace.getDescription().trim().isEmpty()) {
                    text.append(" - ").append(workspace.getDescription());
                }

                text.append("</small>");
                text.append(HTML_END);

                setText(text.toString());
                setBorder(new EmptyBorder(5, 10, 5, 10));
            }

            return this;
        }
    }
}