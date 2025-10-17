package com.laker.postman.panel.collections.right;

import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.util.IdUtil;
import com.laker.postman.common.SingletonFactory;
import com.laker.postman.common.SingletonBasePanel;
import com.laker.postman.common.component.tab.ClosableTabComponent;
import com.laker.postman.common.component.tab.PlusPanel;
import com.laker.postman.common.component.tab.PlusTabComponent;
import com.laker.postman.model.CurlRequest;
import com.laker.postman.model.HttpFormData;
import com.laker.postman.model.HttpHeader;
import com.laker.postman.model.HttpParam;
import com.laker.postman.model.HttpRequestItem;
import com.laker.postman.model.RequestItemProtocolEnum;
import com.laker.postman.panel.collections.left.RequestCollectionsLeftPanel;
import com.laker.postman.panel.collections.right.request.RequestEditSubPanel;
import com.laker.postman.service.collections.RequestsTabsService;
import com.laker.postman.service.curl.CurlParser;
import com.laker.postman.service.http.HttpUtil;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;
import jiconfont.icons.font_awesome.FontAwesome;
import jiconfont.swing.IconFontSwing;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeModel;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.laker.postman.util.SystemUtil.getClipboardCurlText;

/**
 * 请求编辑面板，支持多标签页，每个标签页为独立的请求编辑子面板
 */
@Slf4j
public class RequestEditPanel extends SingletonBasePanel {
    public static final String REQUEST_STRING = I18nUtil.getMessage(MessageKeys.NEW_REQUEST);
    public static final String PLUS_TAB = "+";
    public static final String GROUP = "group";
    @Getter
    private JTabbedPane tabbedPane; // 使用 JTabbedPane 管理多个请求编辑子面板


    // 新建Tab，可指定标题
    public RequestEditSubPanel addNewTab(String title, RequestItemProtocolEnum protocol) {
        // 先移除+Tab
        if (tabbedPane.getTabCount() > 0 && isPlusTab(tabbedPane.getTabCount() - 1)) {
            tabbedPane.removeTabAt(tabbedPane.getTabCount() - 1);
        }
        String tabTitle = title != null ? title : REQUEST_STRING;
        RequestEditSubPanel subPanel = new RequestEditSubPanel(IdUtil.simpleUUID(), protocol);
        tabbedPane.addTab(tabTitle, subPanel);
        tabbedPane.setTabComponentAt(tabbedPane.getTabCount() - 1, new ClosableTabComponent(tabTitle, protocol));
        tabbedPane.setSelectedIndex(tabbedPane.getTabCount() - 1);
        // 保证“+”Tab始终在最后
        addPlusTab();
        RequestsTabsService.updateTabNew(subPanel, true); // 设置新建状态
        return subPanel;
    }

    // 新建Tab，可指定标题
    public RequestEditSubPanel addNewTab(String title) {
        return addNewTab(title, RequestItemProtocolEnum.HTTP);
    }

    // 添加"+"Tab
    public void addPlusTab() {
        tabbedPane.addTab(PLUS_TAB, new PlusPanel());
        // 使用新版 PlusTabComponent，无需点击回调
        PlusTabComponent plusTabComponent = new PlusTabComponent();
        tabbedPane.setTabComponentAt(tabbedPane.getTabCount() - 1, plusTabComponent);
    }

    // 判断是否为“+”Tab
    private boolean isPlusTab(int idx) {
        if (idx < 0 || idx >= tabbedPane.getTabCount()) return false;
        return PLUS_TAB.equals(tabbedPane.getTitleAt(idx));
    }

    // 获取当前激活的请求内容
    public HttpRequestItem getCurrentRequest() {
        RequestEditSubPanel subPanel = getCurrentSubPanel();
        return subPanel != null ? subPanel.getCurrentRequest() : null;
    }

    // 更新当前Tab内容
    public void updateRequest(HttpRequestItem item) {
        RequestEditSubPanel subPanel = getCurrentSubPanel();
        if (subPanel != null) subPanel.initPanelData(item);
    }

    private RequestEditSubPanel getCurrentSubPanel() {
        Component comp = tabbedPane.getSelectedComponent();
        if (comp instanceof RequestEditSubPanel requestEditSubPanel) return requestEditSubPanel;
        return null;
    }

    // showOrCreateTab 需适配 “+” Tab
    public void showOrCreateTab(HttpRequestItem item) {
        String id = item.getId();
        if (id == null || id.isEmpty()) {
            addNewTab(null);
            updateRequest(item);
            return;
        }
        // 查找同id Tab（不查“+”Tab）
        for (int i = 0; i < tabbedPane.getTabCount() - 1; i++) {
            Component comp = tabbedPane.getComponentAt(i);
            if (comp instanceof RequestEditSubPanel subPanel) {
                if (id.equals(subPanel.getId())) {
                    tabbedPane.setSelectedIndex(i);
                    return;
                }
            }
        }
        // 没有同id Tab则新建
        RequestEditSubPanel subPanel = new RequestEditSubPanel(id, item.getProtocol());
        subPanel.initPanelData(item);
        String name = CharSequenceUtil.isNotBlank(item.getName()) ? item.getName() : REQUEST_STRING;
        int plusTabIdx = tabbedPane.getTabCount() > 0 ? tabbedPane.getTabCount() - 1 : 0; // 插入到“+”Tab前
        tabbedPane.insertTab(name, null, subPanel, null, plusTabIdx);
        tabbedPane.setTabComponentAt(plusTabIdx, new ClosableTabComponent(name, item.getProtocol()));
        tabbedPane.setSelectedIndex(plusTabIdx);
    }

    // 快捷键 action 名称常量
    private static final String ACTION_SAVE_REQUEST = "saveRequest";
    private static final String ACTION_NEW_REQUEST_TAB = "newRequestTab";

    /**
     * 统一注册所有快捷键
     */
    private void registerShortcuts() {
        InputMap inputMap = this.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap actionMap = this.getActionMap();

        // 保存快捷键 Ctrl+S / Cmd+S
        KeyStroke saveKey = KeyStroke.getKeyStroke(KeyEvent.VK_S, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx());
        inputMap.put(saveKey, ACTION_SAVE_REQUEST);
        actionMap.put(ACTION_SAVE_REQUEST, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                saveCurrentRequest();
            }
        });

        // 新建标签页快捷键 Ctrl+N / Cmd+N
        KeyStroke newTabKey = KeyStroke.getKeyStroke('N', Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx());
        inputMap.put(newTabKey, ACTION_NEW_REQUEST_TAB);
        actionMap.put(ACTION_NEW_REQUEST_TAB, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                addNewTab(null);
            }
        });
    }

    /**
     * 保存当前请求
     */
    public void saveCurrentRequest() {
        HttpRequestItem currentItem = getCurrentRequest();
        if (currentItem == null) {
            log.error("没有可保存的请求");
            return;
        }

        boolean isNewRequest = currentItem.isNewRequest();

        // 查找请求集合面板
        RequestCollectionsLeftPanel collectionPanel = SingletonFactory.getInstance(RequestCollectionsLeftPanel.class);

        if (isNewRequest) {
            // 新请求：弹出对话框让用户输入名称和选择文件夹
            saveNewRequest(collectionPanel, currentItem);
        } else {
            updateExistingRequest(collectionPanel, currentItem);
        }
    }

    /**
     * 公共方法：弹窗让用户选择分组并输入请求名称，返回分组Object[]和请求名
     *
     * @param groupTreeModel 分组树模型
     * @param defaultName    默认请求名，可为null
     * @return Object[]{Object[] groupObj, String requestName}，若取消返回null
     */
    public Object[] showGroupAndNameDialog(TreeModel groupTreeModel, String defaultName) {
        if (groupTreeModel == null || groupTreeModel.getRoot() == null) {
            JOptionPane.showMessageDialog(this, I18nUtil.getMessage(MessageKeys.PLEASE_SELECT_GROUP),
                    I18nUtil.getMessage(MessageKeys.TIP), JOptionPane.INFORMATION_MESSAGE);
            return null;
        }
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 18, 10, 18));
        JPanel namePanel = new JPanel(new BorderLayout(8, 0));
        JLabel nameLabel = new JLabel(I18nUtil.getMessage(MessageKeys.REQUEST_NAME));
        nameLabel.setPreferredSize(new Dimension(100, 28));
        JTextField nameField = new JTextField(20);
        nameField.setPreferredSize(new Dimension(180, 28));
        if (defaultName != null) nameField.setText(defaultName);
        namePanel.add(nameLabel, BorderLayout.WEST);
        namePanel.add(nameField, BorderLayout.CENTER);
        namePanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        namePanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(namePanel);
        panel.add(Box.createVerticalStrut(12));
        JPanel groupPanel = new JPanel(new BorderLayout(8, 0));
        JLabel groupLabel = new JLabel(I18nUtil.getMessage(MessageKeys.SELECT_GROUP));
        groupLabel.setPreferredSize(new Dimension(100, 28));
        groupPanel.add(groupLabel, BorderLayout.WEST);
        JTree groupTree = getGroupTree(groupTreeModel);
        JScrollPane treeScroll = new JScrollPane(groupTree);
        treeScroll.setPreferredSize(new Dimension(220, 180));
        treeScroll.setBorder(BorderFactory.createLineBorder(new Color(220, 220, 220)));
        groupPanel.add(treeScroll, BorderLayout.CENTER);
        groupPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 180));
        groupPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(groupPanel);
        int result = JOptionPane.showConfirmDialog(this, panel, I18nUtil.getMessage(MessageKeys.SAVE_REQUEST),
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result == JOptionPane.OK_OPTION) {
            String requestName = nameField.getText();
            if (requestName == null || requestName.trim().isEmpty()) {
                JOptionPane.showMessageDialog(this, I18nUtil.getMessage(MessageKeys.PLEASE_ENTER_REQUEST_NAME),
                        I18nUtil.getMessage(MessageKeys.TIP), JOptionPane.WARNING_MESSAGE);
                return null;
            }
            javax.swing.tree.TreePath selectedPath = groupTree.getSelectionPath();
            if (selectedPath == null) {
                JOptionPane.showMessageDialog(this, I18nUtil.getMessage(MessageKeys.PLEASE_SELECT_GROUP),
                        I18nUtil.getMessage(I18nUtil.getMessage(MessageKeys.TIP)), JOptionPane.WARNING_MESSAGE);
                return null;
            }
            Object selectedGroupNode = selectedPath.getLastPathComponent();
            Object[] groupObj = null;
            if (selectedGroupNode instanceof javax.swing.tree.DefaultMutableTreeNode node) {
                Object userObj = node.getUserObject();
                if (userObj instanceof Object[] arr && GROUP.equals(arr[0])) {
                    groupObj = arr;
                }
            }
            if (groupObj == null) {
                JOptionPane.showMessageDialog(this, I18nUtil.getMessage(MessageKeys.PLEASE_SELECT_VALID_GROUP),
                        I18nUtil.getMessage(MessageKeys.TIP), JOptionPane.WARNING_MESSAGE);
                return null;
            }
            return new Object[]{groupObj, requestName};
        }
        return null;
    }

    private static JTree getGroupTree(TreeModel groupTreeModel) {
        DefaultMutableTreeNode rootNode = (DefaultMutableTreeNode) groupTreeModel.getRoot();
        TreeModel filteredModel = new DefaultTreeModel(rootNode) {
            @Override
            public int getChildCount(Object parent) {
                if (parent == rootNode) {
                    // 根节点不过滤，直接返回所有子节点
                    return rootNode.getChildCount();
                }
                if (parent instanceof DefaultMutableTreeNode node) {
                    Object userObj = node.getUserObject();
                    if (userObj instanceof Object[] arr && GROUP.equals(arr[0])) {
                        int groupCount = 0;
                        for (int i = 0; i < node.getChildCount(); i++) {
                            Object childObj = ((DefaultMutableTreeNode) node.getChildAt(i)).getUserObject();
                            if (childObj instanceof Object[] cArr && GROUP.equals(cArr[0])) {
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
                        if (childObj instanceof Object[] cArr && GROUP.equals(cArr[0])) {
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
                    if (userObj instanceof Object[] arr && GROUP.equals(arr[0])) {
                        for (int i = 0; i < treeNode.getChildCount(); i++) {
                            Object childObj = ((DefaultMutableTreeNode) treeNode.getChildAt(i)).getUserObject();
                            if (childObj instanceof Object[] cArr && GROUP.equals(cArr[0])) {
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
        groupTree.setCellRenderer(new DefaultTreeCellRenderer() {
            @Override
            public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {
                super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
                if (value instanceof DefaultMutableTreeNode node) {
                    Object userObj = node.getUserObject();
                    if (userObj instanceof Object[] arr && GROUP.equals(arr[0])) {
                        setText(String.valueOf(arr[1]));
                        // 橙色实心文件夹，模拟Postman分组
                        setIcon(IconFontSwing.buildIcon(FontAwesome.FOLDER_O, 14, new Color(255, 140, 0)));
                    } else {
                        setText("");
                        setIcon(null);
                    }
                }
                return this;
            }
        });
        return groupTree;
    }

    /**
     * 保存新请求（分组选择优化为树结构）
     */
    private void saveNewRequest(RequestCollectionsLeftPanel collectionPanel, HttpRequestItem item) {
        TreeModel groupTreeModel = collectionPanel.getGroupTreeModel();
        Object[] result = showGroupAndNameDialog(groupTreeModel, item.getName());
        if (result == null) return;
        Object[] groupObj = (Object[]) result[0];
        String requestName = (String) result[1];
        item.setName(requestName);
        item.setId(IdUtil.simpleUUID());
        collectionPanel.saveRequestToGroup(groupObj, item);
        int currentTabIndex = tabbedPane.getSelectedIndex();
        if (currentTabIndex >= 0) {
            tabbedPane.setTitleAt(currentTabIndex, requestName);
            Component tabComp = tabbedPane.getTabComponentAt(currentTabIndex);
            if (tabComp instanceof ClosableTabComponent) {
                tabbedPane.setTabComponentAt(currentTabIndex, new ClosableTabComponent(requestName, item.getProtocol()));
            }
            RequestEditSubPanel subPanel = getCurrentSubPanel();
            if (subPanel != null) {
                subPanel.initPanelData(item);
            }
        }
    }

    /**
     * 更新已存在的请求
     */
    private void updateExistingRequest(RequestCollectionsLeftPanel collectionPanel, HttpRequestItem item) {
        if (!collectionPanel.updateExistingRequest(item)) {
            log.error("更新请求失败: {}", item.getId() + " - " + item.getName());
            JOptionPane.showMessageDialog(this, I18nUtil.getMessage(MessageKeys.UPDATE_REQUEST_FAILED),
                    I18nUtil.getMessage(MessageKeys.ERROR), JOptionPane.ERROR_MESSAGE);
        }
    }

    // 用于动态更新tab红点
    public void updateTabDirty(RequestEditSubPanel panel, boolean dirty) {
        int idx = tabbedPane.indexOfComponent(panel);
        if (idx < 0) return;
        Component tabComp = tabbedPane.getTabComponentAt(idx);
        if (tabComp instanceof ClosableTabComponent closable) {
            closable.setDirty(dirty);
        }
    }

    @Override
    protected void initUI() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0)); // 去掉默认边框
        // 设置tabbedPane为单行滚动模式，防止多行tab顺序混乱
        tabbedPane = new JTabbedPane(SwingConstants.TOP, JTabbedPane.SCROLL_TAB_LAYOUT);
        add(tabbedPane, BorderLayout.CENTER);
    }

    @Override
    protected void registerListeners() {
        // 统一注册所有快捷键
        registerShortcuts();
        // 添加鼠标监听，只在左键点击"+"Tab时新增
        tabbedPane.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                // 根据鼠标点击位置确定点击的标签页索引，而不是使用getSelectedIndex() 因为切换语言后可能不准了
                int clickedTabIndex = tabbedPane.indexAtLocation(e.getX(), e.getY());

                // 如果点击位置不在任何标签页上，直接返回
                if (clickedTabIndex < 0) {
                    return;
                }

                // 判断是否点击的是"+"Tab（最后一个标签页且是PlusTab）
                if (clickedTabIndex == tabbedPane.getTabCount() - 1 && isPlusTab(clickedTabIndex)) {
                    // 检测剪贴板cURL
                    String curlText = getClipboardCurlText();
                    if (curlText != null) {
                        int result = JOptionPane.showConfirmDialog(SingletonFactory.getInstance(RequestEditPanel.class),
                                I18nUtil.getMessage(MessageKeys.CLIPBOARD_CURL_DETECTED),
                                I18nUtil.getMessage(MessageKeys.IMPORT_CURL), JOptionPane.YES_NO_OPTION);
                        if (result == JOptionPane.YES_OPTION) {
                            try {
                                CurlRequest curlRequest = CurlParser.parse(curlText);
                                if (curlRequest.url != null) {
                                    HttpRequestItem item = new HttpRequestItem();
                                    item.setName(null);
                                    item.setUrl(curlRequest.url);
                                    item.setMethod(curlRequest.method);

                                    // Convert headers map to list
                                    if (curlRequest.headers != null && !curlRequest.headers.isEmpty()) {
                                        List<HttpHeader> headersList = new ArrayList<>();
                                        for (Map.Entry<String, String> entry : curlRequest.headers.entrySet()) {
                                            headersList.add(new HttpHeader(true, entry.getKey(), entry.getValue()));
                                        }
                                        item.setHeadersList(headersList);
                                    }

                                    item.setBody(curlRequest.body);

                                    // Convert params map to list
                                    if (curlRequest.params != null && !curlRequest.params.isEmpty()) {
                                        List<HttpParam> paramsList = new ArrayList<>();
                                        for (Map.Entry<String, String> entry : curlRequest.params.entrySet()) {
                                            paramsList.add(new HttpParam(true, entry.getKey(), entry.getValue()));
                                        }
                                        item.setParamsList(paramsList);
                                    }

                                    // Convert formData and formFiles maps to list
                                    if ((curlRequest.formData != null && !curlRequest.formData.isEmpty()) ||
                                        (curlRequest.formFiles != null && !curlRequest.formFiles.isEmpty())) {
                                        List<HttpFormData> formDataList = new ArrayList<>();
                                        if (curlRequest.formData != null) {
                                            for (Map.Entry<String, String> entry : curlRequest.formData.entrySet()) {
                                                formDataList.add(new HttpFormData(true, entry.getKey(), "text", entry.getValue()));
                                            }
                                        }
                                        if (curlRequest.formFiles != null) {
                                            for (Map.Entry<String, String> entry : curlRequest.formFiles.entrySet()) {
                                                formDataList.add(new HttpFormData(true, entry.getKey(), "file", entry.getValue()));
                                            }
                                        }
                                        item.setFormDataList(formDataList);
                                    }

                                    if (HttpUtil.isSSERequest(item.getHeaders())) {
                                        item.setProtocol(RequestItemProtocolEnum.SSE);
                                    } else if (HttpUtil.isWebSocketRequest(item.getUrl())) {
                                        item.setProtocol(RequestItemProtocolEnum.WEBSOCKET);
                                    } else {
                                        item.setProtocol(RequestItemProtocolEnum.HTTP);
                                    }
                                    // 新建Tab并填充内容
                                    RequestEditSubPanel tab = addNewTab(null, item.getProtocol());
                                    item.setId(tab.getId());
                                    tab.initPanelData(item);
                                    // 清空剪贴板内容
                                    Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(""), null);
                                    return;
                                }
                            } catch (Exception ex) {
                                JOptionPane.showMessageDialog(SingletonFactory.getInstance(RequestEditPanel.class),
                                        I18nUtil.getMessage(MessageKeys.PARSE_CURL_ERROR, ex.getMessage()),
                                        I18nUtil.getMessage(MessageKeys.ERROR), JOptionPane.ERROR_MESSAGE);
                            }
                        }
                    }
                    addNewTab(REQUEST_STRING);
                }
            }
        });
    }


    public RequestEditSubPanel getRequestEditSubPanel(String reqItemId) {
        for (int i = 0; i < tabbedPane.getTabCount() - 1; i++) {
            Component comp = tabbedPane.getComponentAt(i);
            if (comp instanceof RequestEditSubPanel subPanel && reqItemId.equals(subPanel.getId())) {
                return subPanel;
            }
        }
        return null;
    }

    /**
     * 将当前 HTTP 协议的 tab 切换为 SSE 协议
     * 这会重新创建一个 SSE 类型的 RequestEditSubPanel，保留原有的请求数据
     */
    public void switchCurrentTabToSseProtocol() {
        int currentIndex = tabbedPane.getSelectedIndex();
        if (currentIndex < 0 || isPlusTab(currentIndex)) {
            return;
        }

        Component comp = tabbedPane.getComponentAt(currentIndex);
        if (!(comp instanceof RequestEditSubPanel oldPanel)) {
            return;
        }

        // 获取当前的请求数据
        HttpRequestItem currentItem = oldPanel.getCurrentRequest();
        if (currentItem == null) {
            return;
        }

        // 如果已经是 SSE 协议，不需要切换
        if (currentItem.getProtocol() == RequestItemProtocolEnum.SSE) {
            return;
        }

        // 修改协议为 SSE
        currentItem.setProtocol(RequestItemProtocolEnum.SSE);

        // 获取当前 tab 的标题
        String tabTitle = tabbedPane.getTitleAt(currentIndex);

        // 创建新的 SSE 协议的 RequestEditSubPanel
        RequestEditSubPanel newPanel = new RequestEditSubPanel(currentItem.getId(), RequestItemProtocolEnum.SSE);

        // 将请求数据填充到新面板
        newPanel.initPanelData(currentItem);

        // 替换 tab
        tabbedPane.setComponentAt(currentIndex, newPanel);
        ClosableTabComponent newTabComponent = new ClosableTabComponent(tabTitle, RequestItemProtocolEnum.SSE);
        newTabComponent.setNewRequest(true);
        tabbedPane.setTabComponentAt(currentIndex, newTabComponent);

        // 保持选中状态
        tabbedPane.setSelectedIndex(currentIndex);
        // click send button
        newPanel.clickSendButton();
    }
}

