package com.laker.postman.panel.collections.left.dialog;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.laker.postman.common.SingletonFactory;
import com.laker.postman.frame.MainFrame;
import com.laker.postman.model.HttpRequestItem;
import com.laker.postman.model.RequestItemProtocolEnum;
import com.laker.postman.panel.collections.left.RequestCollectionsLeftPanel;
import com.laker.postman.panel.collections.right.RequestEditPanel;
import com.laker.postman.panel.collections.right.request.sub.RequestBodyPanel;
import com.laker.postman.service.http.HttpRequestFactory;
import com.laker.postman.util.FontsUtil;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import java.awt.*;

import static com.laker.postman.panel.collections.left.RequestCollectionsLeftPanel.REQUEST;
import static com.laker.postman.service.collections.DefaultRequestsFactory.APPLICATION_JSON;
import static com.laker.postman.service.collections.DefaultRequestsFactory.CONTENT_TYPE;
import static com.laker.postman.service.http.HttpRequestFactory.*;

/**
 * 添加请求对话框
 * 包含协议选择和名称输入
 */
public class AddRequestDialog {
    private final DefaultMutableTreeNode groupNode;
    private final RequestCollectionsLeftPanel leftPanel;
    private JDialog dialog;
    private JTextField nameField;
    private JToggleButton httpBtn;
    private JToggleButton wsBtn;
    private JToggleButton sseBtn;

    public AddRequestDialog(DefaultMutableTreeNode groupNode, RequestCollectionsLeftPanel leftPanel) {
        this.groupNode = groupNode;
        this.leftPanel = leftPanel;
        initDialog();
    }

    /**
     * 初始化对话框
     */
    private void initDialog() {
        dialog = new JDialog(
                SingletonFactory.getInstance(MainFrame.class),
                I18nUtil.getMessage(MessageKeys.COLLECTIONS_DIALOG_ADD_REQUEST_TITLE),
                true
        );
        dialog.setSize(400, 260);
        dialog.setLocationRelativeTo(SingletonFactory.getInstance(MainFrame.class));
        dialog.setLayout(new BorderLayout());

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
        JLabel nameLabel = new JLabel(I18nUtil.getMessage(MessageKeys.COLLECTIONS_DIALOG_ADD_REQUEST_NAME));
        nameLabel.setFont(nameLabel.getFont().deriveFont(Font.BOLD));

        nameField = new JTextField();
        nameField.setPreferredSize(new Dimension(0, 30));

        namePanel.add(nameLabel, BorderLayout.NORTH);
        namePanel.add(nameField, BorderLayout.CENTER);

        return namePanel;
    }

    /**
     * 创建协议选择面板
     */
    private JPanel createProtocolPanel() {
        ButtonGroup protocolGroup;
        JPanel protocolPanel = new JPanel();
        protocolPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 10, 5));
        protocolPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(),
                I18nUtil.getMessage(MessageKeys.COLLECTIONS_DIALOG_ADD_REQUEST_PROTOCOL)
        ));

        protocolGroup = new ButtonGroup();
        httpBtn = createProtocolButton("HTTP", "icons/http.svg", true);
        wsBtn = createProtocolButton("WebSocket", "icons/websocket.svg", false);
        sseBtn = createProtocolButton("SSE", "icons/sse.svg", false);

        protocolGroup.add(httpBtn);
        protocolGroup.add(wsBtn);
        protocolGroup.add(sseBtn);

        protocolPanel.add(httpBtn);
        protocolPanel.add(wsBtn);
        protocolPanel.add(sseBtn);

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
        btn.setFont(FontsUtil.getDefaultFont(Font.PLAIN, 11));
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
        createNewRequest(protocol, requestName);
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
     * 创建新请求
     */
    private void createNewRequest(RequestItemProtocolEnum protocol, String requestName) {
        if (groupNode == null) return;

        HttpRequestItem defaultRequest = HttpRequestFactory.createDefaultRequest();
        defaultRequest.setProtocol(protocol);
        defaultRequest.setName(requestName);
        defaultRequest.getHeaders().put(USER_AGENT, EASY_POSTMAN_CLIENT);

        configureRequestByProtocol(defaultRequest, protocol);

        // 添加到树中
        DefaultMutableTreeNode reqNode = new DefaultMutableTreeNode(new Object[]{REQUEST, defaultRequest});
        groupNode.add(reqNode);
        leftPanel.getTreeModel().reload(groupNode);
        leftPanel.getRequestTree().expandPath(new TreePath(groupNode.getPath()));
        leftPanel.getPersistence().saveRequestGroups();

        // 自动打开新创建的请求
        SingletonFactory.getInstance(RequestEditPanel.class).showOrCreateTab(defaultRequest);
    }

    /**
     * 根据协议类型配置请求
     */
    private void configureRequestByProtocol(HttpRequestItem request, RequestItemProtocolEnum protocol) {
        request.setMethod("GET");
        request.setUrl("");

        if (protocol.isWebSocketProtocol()) {
            // WebSocket 默认配置
            request.getHeaders().put(CONTENT_TYPE, APPLICATION_JSON);
            request.setBodyType(RequestBodyPanel.BODY_TYPE_RAW);
            request.getHeaders().put(ACCEPT_ENCODING, "identity");
        } else if (protocol.isSseProtocol()) {
            // SSE 默认配置
            request.getHeaders().put(ACCEPT, TEXT_EVENT_STREAM);
            request.getHeaders().put(ACCEPT_ENCODING, "identity");
        }
        // HTTP 使用默认配置即可
    }

    /**
     * 显示对话框
     */
    public void show() {
        SwingUtilities.invokeLater(nameField::requestFocus);
        dialog.setVisible(true);
    }
}

