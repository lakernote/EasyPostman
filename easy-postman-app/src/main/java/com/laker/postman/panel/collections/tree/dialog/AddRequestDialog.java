package com.laker.postman.panel.collections.tree.dialog;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.laker.postman.common.UiSingletonFactory;
import com.laker.postman.common.component.ToolWindowSurfaceStyle;
import com.laker.postman.frame.MainFrame;
import com.laker.postman.panel.collections.editor.RequestEditorPanel;
import com.laker.postman.panel.collections.tree.CollectionTreePanel;
import com.laker.postman.http.request.HttpRequestFactory;
import com.laker.postman.request.model.HttpRequestItem;
import com.laker.postman.request.model.RequestItemProtocolEnum;
import com.laker.postman.service.collections.CollectionTreeNodes;
import com.laker.postman.util.FontsUtil;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import java.awt.*;

/**
 * 添加请求对话框
 * 包含协议选择和名称输入
 */
public class AddRequestDialog {
    private final DefaultMutableTreeNode groupNode;
    private final CollectionTreePanel leftPanel;
    private JDialog dialog;
    private JTextField nameField;
    private JToggleButton httpBtn;
    private JToggleButton wsBtn;
    private JToggleButton sseBtn;

    public AddRequestDialog(DefaultMutableTreeNode groupNode, CollectionTreePanel leftPanel) {
        this.groupNode = groupNode;
        this.leftPanel = leftPanel;
        initDialog();
    }

    /**
     * 初始化对话框
     */
    private void initDialog() {
        dialog = new JDialog(
                UiSingletonFactory.getInstance(MainFrame.class),
                I18nUtil.getMessage(MessageKeys.COLLECTIONS_DIALOG_ADD_REQUEST_TITLE),
                true
        );
        dialog.setSize(400, 260);
        dialog.setLocationRelativeTo(UiSingletonFactory.getInstance(MainFrame.class));
        dialog.setLayout(new BorderLayout());
        ToolWindowSurfaceStyle.applyBackground((JComponent) dialog.getContentPane());

        JPanel mainPanel = createMainPanel();
        JPanel buttonPanel = createButtonPanel();

        dialog.add(mainPanel, BorderLayout.CENTER);
        dialog.add(buttonPanel, BorderLayout.SOUTH);
        dialog.getRootPane().setDefaultButton((JButton) buttonPanel.getComponent(0));
    }

    /**
     * 创建主面板
     */
    private JPanel createMainPanel() {
        JPanel mainPanel = new JPanel();
        ToolWindowSurfaceStyle.applyBackground(mainPanel);
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 10, 20));

        JPanel namePanel = createNamePanel();
        JPanel protocolPanel = createProtocolPanel();

        mainPanel.add(namePanel);
        mainPanel.add(Box.createVerticalStrut(15));
        mainPanel.add(protocolPanel);

        return mainPanel;
    }

    /**
     * 创建名称输入面板
     */
    private JPanel createNamePanel() {
        JPanel namePanel = new JPanel(new BorderLayout(10, 5));
        namePanel.setOpaque(false);
        JLabel nameLabel = new JLabel(I18nUtil.getMessage(MessageKeys.COLLECTIONS_DIALOG_ADD_REQUEST_NAME));
        nameLabel.setFont(nameLabel.getFont().deriveFont(Font.BOLD));

        nameField = new JTextField();
        nameField.setPreferredSize(new Dimension(0, 30));
        ToolWindowSurfaceStyle.applyTextComponentInput(nameField);

        namePanel.add(nameLabel, BorderLayout.NORTH);
        namePanel.add(nameField, BorderLayout.CENTER);

        return namePanel;
    }

    /**
     * 创建协议选择面板
     */
    private JPanel createProtocolPanel() {
        ButtonGroup protocolGroup;
        JPanel protocolPanel = new JPanel(new BorderLayout(0, 8));
        protocolPanel.setOpaque(false);

        JLabel protocolLabel = new JLabel(I18nUtil.getMessage(MessageKeys.COLLECTIONS_DIALOG_ADD_REQUEST_PROTOCOL));
        protocolLabel.setFont(FontsUtil.getDefaultFont(Font.BOLD));

        JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        buttonsPanel.setOpaque(false);

        protocolGroup = new ButtonGroup();
        httpBtn = createProtocolButton("HTTP", "icons/http.svg", true);
        wsBtn = createProtocolButton("WebSocket", "icons/websocket.svg", false);
        sseBtn = createProtocolButton("SSE", "icons/sse.svg", false);

        protocolGroup.add(httpBtn);
        protocolGroup.add(wsBtn);
        protocolGroup.add(sseBtn);

        buttonsPanel.add(httpBtn);
        buttonsPanel.add(wsBtn);
        buttonsPanel.add(sseBtn);

        protocolPanel.add(protocolLabel, BorderLayout.NORTH);
        protocolPanel.add(buttonsPanel, BorderLayout.CENTER);

        return protocolPanel;
    }

    /**
     * 创建协议按钮
     */
    private JToggleButton createProtocolButton(String text, String iconPath, boolean selected) {
        JToggleButton btn = new JToggleButton(text);
        btn.setIcon(new FlatSVGIcon(iconPath, 24, 24));
        btn.setSelected(selected);
        btn.setFocusPainted(false);
        btn.setFont(FontsUtil.getDefaultFontWithOffset(Font.PLAIN, -1)); // 比标准字体小1号
        btn.setVerticalTextPosition(SwingConstants.BOTTOM);
        btn.setHorizontalTextPosition(SwingConstants.CENTER);
        btn.setPreferredSize(new Dimension(100, 60));
        return btn;
    }

    /**
     * 创建按钮面板
     */
    private JPanel createButtonPanel() {
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        ToolWindowSurfaceStyle.applyBackground(buttonPanel);

        JButton okButton = new JButton(I18nUtil.getMessage(MessageKeys.GENERAL_OK));
        JButton cancelButton = new JButton(I18nUtil.getMessage(MessageKeys.GENERAL_CANCEL));

        okButton.addActionListener(e -> handleOk());
        cancelButton.addActionListener(e -> dialog.dispose());

        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);

        return buttonPanel;
    }

    /**
     * 处理确定按钮
     */
    private void handleOk() {
        String requestName = nameField.getText().trim();
        if (requestName.isEmpty()) {
            JOptionPane.showMessageDialog(
                    dialog,
                    I18nUtil.getMessage(MessageKeys.COLLECTIONS_DIALOG_ADD_REQUEST_NAME_EMPTY),
                    I18nUtil.getMessage(MessageKeys.GENERAL_TIP),
                    JOptionPane.WARNING_MESSAGE
            );
            nameField.requestFocus();
            return;
        }

        RequestItemProtocolEnum protocol = getSelectedProtocol();
        createAndAddRequest(requestName, protocol);
        dialog.dispose();
    }

    /**
     * 获取选中的协议
     */
    private RequestItemProtocolEnum getSelectedProtocol() {
        if (httpBtn.isSelected()) {
            return RequestItemProtocolEnum.HTTP;
        } else if (wsBtn.isSelected()) {
            return RequestItemProtocolEnum.WEBSOCKET;
        } else if (sseBtn.isSelected()) {
            return RequestItemProtocolEnum.SSE;
        }
        return RequestItemProtocolEnum.HTTP;
    }

    /**
     * 创建并添加请求到树
     */
    private void createAndAddRequest(String requestName, RequestItemProtocolEnum protocol) {
        if (groupNode == null) return;

        HttpRequestItem defaultRequest = HttpRequestFactory.createBlankRequest(protocol);
        defaultRequest.setName(requestName);

        // 添加到树中
        DefaultMutableTreeNode reqNode = CollectionTreeNodes.requestNode(defaultRequest);
        groupNode.add(reqNode);
        leftPanel.getTreeModel().reload(groupNode);
        JTree tree = leftPanel.getRequestTree();
        tree.expandPath(new TreePath(groupNode.getPath()));
        leftPanel.getCollectionTreePersistence().saveCurrentTree();

        // 定位到新创建的请求节点
        TreePath newPath = new TreePath(reqNode.getPath());
        tree.setSelectionPath(newPath);
        tree.scrollPathToVisible(newPath);

        // 自动打开新创建的请求
        UiSingletonFactory.getInstance(RequestEditorPanel.class).showOrCreateTab(defaultRequest);
    }

    /**
     * 显示对话框
     */
    public void show() {
        SwingUtilities.invokeLater(nameField::requestFocus);
        dialog.setVisible(true);
    }
}
