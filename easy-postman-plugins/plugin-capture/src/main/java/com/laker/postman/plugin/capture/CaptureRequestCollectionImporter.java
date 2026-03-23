package com.laker.postman.plugin.capture;

import com.laker.postman.common.SingletonFactory;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.laker.postman.frame.MainFrame;
import com.laker.postman.model.HttpHeader;
import com.laker.postman.model.HttpRequestItem;
import com.laker.postman.model.RequestGroup;
import com.laker.postman.model.RequestItemProtocolEnum;
import com.laker.postman.panel.collections.left.RequestCollectionsLeftPanel;
import com.laker.postman.panel.collections.right.RequestEditPanel;
import com.laker.postman.panel.collections.right.request.sub.RequestBodyPanel;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.NotificationUtil;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import static com.laker.postman.plugin.capture.CaptureI18n.t;

final class CaptureRequestCollectionImporter {

    void importFlows(List<CaptureFlow> flows) {
        if (flows == null || flows.isEmpty()) {
            NotificationUtil.showWarning(t(MessageKeys.TOOLBOX_CAPTURE_IMPORT_EMPTY));
            return;
        }

        RequestCollectionsLeftPanel collectionPanel = SingletonFactory.getInstance(RequestCollectionsLeftPanel.class);
        RequestEditPanel requestEditPanel = SingletonFactory.getInstance(RequestEditPanel.class);

        Object[] groupObj = chooseGroup(collectionPanel.getGroupTreeModel());
        if (groupObj == null) {
            return;
        }

        Map<String, Integer> nameCounts = new LinkedHashMap<>();
        HttpRequestItem lastImported = null;
        for (CaptureFlow flow : flows) {
            String requestName = nextRequestName(flow, nameCounts);
            HttpRequestItem item = toHttpRequestItem(flow, requestName);
            collectionPanel.saveRequestToGroup(groupObj, item);
            lastImported = item;
        }

        if (lastImported != null) {
            collectionPanel.locateAndSelectRequest(lastImported.getId());
            requestEditPanel.showOrCreateTab(lastImported);
        }
        NotificationUtil.showSuccess(t(MessageKeys.TOOLBOX_CAPTURE_IMPORT_SUCCESS, flows.size()));
    }

    private Object[] chooseGroup(TreeModel groupTreeModel) {
        if (groupTreeModel == null || groupTreeModel.getRoot() == null) {
            JOptionPane.showMessageDialog(null,
                    I18nUtil.getMessage(com.laker.postman.util.MessageKeys.PLEASE_SELECT_GROUP),
                    I18nUtil.getMessage(com.laker.postman.util.MessageKeys.SAVE_REQUEST),
                    JOptionPane.INFORMATION_MESSAGE);
            return null;
        }

        JTree groupTree = createGroupTree(groupTreeModel);
        JPanel mainPanel = new JPanel(new BorderLayout(0, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));

        JLabel groupLabel = new JLabel(I18nUtil.getMessage(com.laker.postman.util.MessageKeys.SELECT_GROUP) + ":");
        groupLabel.setFont(groupLabel.getFont().deriveFont(groupLabel.getFont().getStyle() | java.awt.Font.BOLD, 13f));
        mainPanel.add(groupLabel, BorderLayout.NORTH);

        JScrollPane treeScroll = new JScrollPane(groupTree);
        treeScroll.setPreferredSize(new Dimension(350, 220));
        treeScroll.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200), 1),
                BorderFactory.createEmptyBorder(4, 4, 4, 4)
        ));
        mainPanel.add(treeScroll, BorderLayout.CENTER);

        JDialog dialog = new JDialog(SingletonFactory.getInstance(MainFrame.class),
                I18nUtil.getMessage(com.laker.postman.util.MessageKeys.SELECT_GROUP), true);
        dialog.setLayout(new BorderLayout());
        dialog.add(mainPanel, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 12));
        JButton cancelButton = new JButton(I18nUtil.getMessage(com.laker.postman.util.MessageKeys.BUTTON_CANCEL));
        JButton okButton = new JButton(I18nUtil.getMessage(com.laker.postman.util.MessageKeys.GENERAL_OK));
        cancelButton.setPreferredSize(new Dimension(80, 32));
        okButton.setPreferredSize(new Dimension(80, 32));
        buttonPanel.add(cancelButton);
        buttonPanel.add(okButton);
        dialog.add(buttonPanel, BorderLayout.SOUTH);

        final Object[] result = {null};
        Runnable okAction = () -> {
            TreePath selectedPath = groupTree.getSelectionPath();
            if (selectedPath == null) {
                NotificationUtil.showWarning(I18nUtil.getMessage(com.laker.postman.util.MessageKeys.PLEASE_SELECT_GROUP));
                groupTree.requestFocusInWindow();
                return;
            }

            Object selectedNode = selectedPath.getLastPathComponent();
            if (selectedNode instanceof DefaultMutableTreeNode node) {
                Object userObj = node.getUserObject();
                if (userObj instanceof Object[] arr && RequestCollectionsLeftPanel.GROUP.equals(arr[0])) {
                    result[0] = arr;
                    dialog.dispose();
                    return;
                }
            }

            NotificationUtil.showWarning(I18nUtil.getMessage(com.laker.postman.util.MessageKeys.PLEASE_SELECT_VALID_GROUP));
        };

        okButton.addActionListener(e -> okAction.run());
        cancelButton.addActionListener(e -> dialog.dispose());
        dialog.getRootPane().setDefaultButton(okButton);
        dialog.getRootPane().registerKeyboardAction(
                e -> dialog.dispose(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW
        );

        dialog.setSize(420, 390);
        dialog.setLocationRelativeTo(SingletonFactory.getInstance(MainFrame.class));
        dialog.setResizable(false);
        SwingUtilities.invokeLater(groupTree::requestFocusInWindow);
        dialog.setVisible(true);
        return (Object[]) result[0];
    }

    private JTree createGroupTree(TreeModel groupTreeModel) {
        DefaultMutableTreeNode rootNode = (DefaultMutableTreeNode) groupTreeModel.getRoot();
        TreeModel filteredModel = new DefaultTreeModel(rootNode) {
            @Override
            public int getChildCount(Object parent) {
                if (parent == rootNode) {
                    return rootNode.getChildCount();
                }
                if (parent instanceof DefaultMutableTreeNode node) {
                    Object userObj = node.getUserObject();
                    if (userObj instanceof Object[] arr && RequestCollectionsLeftPanel.GROUP.equals(arr[0])) {
                        int groupCount = 0;
                        for (int i = 0; i < node.getChildCount(); i++) {
                            Object childObj = ((DefaultMutableTreeNode) node.getChildAt(i)).getUserObject();
                            if (childObj instanceof Object[] cArr && RequestCollectionsLeftPanel.GROUP.equals(cArr[0])) {
                                groupCount++;
                            }
                        }
                        return groupCount;
                    }
                }
                return 0;
            }

            @Override
            public Object getChild(Object parent, int index) {
                if (parent == rootNode) {
                    return rootNode.getChildAt(index);
                }
                if (parent instanceof DefaultMutableTreeNode node) {
                    int groupIdx = -1;
                    for (int i = 0; i < node.getChildCount(); i++) {
                        Object childObj = ((DefaultMutableTreeNode) node.getChildAt(i)).getUserObject();
                        if (childObj instanceof Object[] cArr && RequestCollectionsLeftPanel.GROUP.equals(cArr[0])) {
                            groupIdx++;
                            if (groupIdx == index) {
                                return node.getChildAt(i);
                            }
                        }
                    }
                }
                return null;
            }

            @Override
            public boolean isLeaf(Object node) {
                if (node == rootNode) {
                    return rootNode.getChildCount() == 0;
                }
                if (node instanceof DefaultMutableTreeNode treeNode) {
                    Object userObj = treeNode.getUserObject();
                    if (userObj instanceof Object[] arr && RequestCollectionsLeftPanel.GROUP.equals(arr[0])) {
                        for (int i = 0; i < treeNode.getChildCount(); i++) {
                            Object childObj = ((DefaultMutableTreeNode) treeNode.getChildAt(i)).getUserObject();
                            if (childObj instanceof Object[] cArr && RequestCollectionsLeftPanel.GROUP.equals(cArr[0])) {
                                return false;
                            }
                        }
                        return true;
                    }
                }
                return true;
            }
        };

        JTree groupTree = new JTree(filteredModel);
        groupTree.setRootVisible(false);
        groupTree.setShowsRootHandles(true);
        groupTree.setRowHeight(28);
        groupTree.putClientProperty("FlatLaf.style", "wideCellRenderer: true");
        groupTree.setCellRenderer(new DefaultTreeCellRenderer() {
            @Override
            public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel,
                                                          boolean expanded, boolean leaf, int row, boolean hasFocus) {
                super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
                if (value instanceof DefaultMutableTreeNode node) {
                    Object userObj = node.getUserObject();
                    if (userObj instanceof Object[] arr
                            && RequestCollectionsLeftPanel.GROUP.equals(arr[0])
                            && arr[1] instanceof RequestGroup group) {
                        setText(group.getName());
                        String iconName = node.getLevel() == 1 ? "icons/collection.svg" : "icons/group.svg";
                        setIcon(new FlatSVGIcon(iconName, 16, 16));
                    } else {
                        setText("");
                        setIcon(null);
                    }
                }
                return this;
            }
        });
        for (int i = 0; i < groupTree.getRowCount(); i++) {
            groupTree.expandRow(i);
        }
        if (groupTree.getRowCount() > 0) {
            groupTree.setSelectionRow(0);
        }
        return groupTree;
    }

    private HttpRequestItem toHttpRequestItem(CaptureFlow flow, String requestName) {
        HttpRequestItem item = new HttpRequestItem();
        item.setId(UUID.randomUUID().toString().replace("-", ""));
        item.setName(requestName);
        item.setUrl(flow.collectionRequestUrl());
        item.setMethod(flow.method());
        item.setProtocol(resolveProtocol(flow));
        item.setHeadersList(toHeaders(flow));
        item.setDescription(buildDescription(flow));
        populateBody(flow, item);
        return item;
    }

    private List<HttpHeader> toHeaders(CaptureFlow flow) {
        List<HttpHeader> headers = new ArrayList<>();
        for (Map.Entry<String, String> entry : flow.requestHeadersSnapshot().entrySet()) {
            String key = entry.getKey();
            if (shouldSkipHeader(key, flow)) {
                continue;
            }
            headers.add(new HttpHeader(true, key, entry.getValue()));
        }
        return headers;
    }

    private boolean shouldSkipHeader(String key, CaptureFlow flow) {
        if (key == null || key.isBlank()) {
            return true;
        }
        String normalized = key.toLowerCase(Locale.ROOT);
        if ("host".equals(normalized)
                || "content-length".equals(normalized)
                || "proxy-connection".equals(normalized)
                || "transfer-encoding".equals(normalized)) {
            return true;
        }
        if (flow.isWebSocketProtocol()) {
            return "connection".equals(normalized)
                    || "upgrade".equals(normalized)
                    || normalized.startsWith("sec-websocket-");
        }
        return false;
    }

    private RequestItemProtocolEnum resolveProtocol(CaptureFlow flow) {
        if (flow.isWebSocketProtocol()) {
            return RequestItemProtocolEnum.WEBSOCKET;
        }
        if (flow.isSseProtocol()) {
            return RequestItemProtocolEnum.SSE;
        }
        return RequestItemProtocolEnum.HTTP;
    }

    private void populateBody(CaptureFlow flow, HttpRequestItem item) {
        if (flow.isWebSocketProtocol()) {
            item.setBodyType(RequestBodyPanel.BODY_TYPE_RAW);
            item.setBody("");
            return;
        }
        String bodyText = flow.requestBodyImportText();
        if (!bodyText.isBlank()) {
            item.setBodyType(RequestBodyPanel.BODY_TYPE_RAW);
            item.setBody(bodyText);
        } else {
            item.setBodyType(RequestBodyPanel.BODY_TYPE_NONE);
            item.setBody("");
        }
    }

    private String buildDescription(CaptureFlow flow) {
        StringBuilder builder = new StringBuilder();
        builder.append("Imported from Capture\n");
        builder.append("Captured At: ").append(flow.startedAtText()).append('\n');
        builder.append("Source URL: ").append(flow.collectionRequestUrl()).append('\n');
        if (flow.requestBodyPartial()) {
            builder.append("Note: request body preview was truncated during capture.\n");
        }
        return builder.toString().trim();
    }

    private String nextRequestName(CaptureFlow flow, Map<String, Integer> nameCounts) {
        String baseName = suggestedName(flow);
        int nextIndex = nameCounts.getOrDefault(baseName, 0) + 1;
        nameCounts.put(baseName, nextIndex);
        return nextIndex == 1 ? baseName : baseName + " " + nextIndex;
    }

    private String suggestedName(CaptureFlow flow) {
        String path = flow.path();
        if (path == null || path.isBlank() || "/".equals(path)) {
            return flow.host();
        }
        return path.length() > 64 ? path.substring(0, 64) + "..." : path;
    }
}
