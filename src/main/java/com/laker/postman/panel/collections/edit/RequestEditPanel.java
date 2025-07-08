package com.laker.postman.panel.collections.edit;

import cn.hutool.core.util.IdUtil;
import com.laker.postman.common.SingletonFactory;
import com.laker.postman.common.panel.BasePanel;
import com.laker.postman.common.tab.ClosableTabComponent;
import com.laker.postman.common.tab.PlusTabComponent;
import com.laker.postman.model.CurlRequest;
import com.laker.postman.model.HttpRequestItem;
import com.laker.postman.panel.collections.RequestCollectionsLeftPanel;
import com.laker.postman.service.curl.CurlParser;
import jiconfont.icons.font_awesome.FontAwesome;
import jiconfont.swing.IconFontSwing;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeModel;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

/**
 * 请求编辑面板，支持多标签页，每个标签页为独立的请求编辑子面板
 */
@Slf4j
public class RequestEditPanel extends BasePanel {
    public static final String REQUEST_STRING = "Request ";
    @Getter
    private JTabbedPane tabbedPane; // 使用 JTabbedPane 管理多个请求编辑子面板


    // 新建Tab，可指定标题
    public RequestEditSubPanel addNewTab(String title) {
        // 先移除+Tab
        if (tabbedPane.getTabCount() > 0 && isPlusTab(tabbedPane.getTabCount() - 1)) {
            tabbedPane.removeTabAt(tabbedPane.getTabCount() - 1);
        }
        String tabTitle = title != null ? title : REQUEST_STRING + (tabbedPane.getTabCount() + 1);
        RequestEditSubPanel subPanel = new RequestEditSubPanel(IdUtil.simpleUUID());
        tabbedPane.addTab(tabTitle, subPanel);
        tabbedPane.setTabComponentAt(tabbedPane.getTabCount() - 1,
                new ClosableTabComponent(tabTitle, subPanel, tabbedPane, this::saveCurrentRequest));
        tabbedPane.setSelectedIndex(tabbedPane.getTabCount() - 1);
        // 保证“+”Tab始终在最后
        addPlusTab();
        return subPanel;
    }

    // 添加“+”Tab
    private void addPlusTab() {
        JPanel plusPanel = new JPanel();
        plusPanel.setOpaque(false);
        tabbedPane.addTab("+", plusPanel);
        // 使用新版 PlusTabComponent，无需点击回调
        PlusTabComponent plusTabComponent = new PlusTabComponent();
        tabbedPane.setTabComponentAt(tabbedPane.getTabCount() - 1, plusTabComponent);
    }

    // 判断是否为“+”Tab
    private boolean isPlusTab(int idx) {
        if (idx < 0 || idx >= tabbedPane.getTabCount()) return false;
        return "+".equals(tabbedPane.getTitleAt(idx));
    }

    // 获取当前激活的请求内容
    public HttpRequestItem getCurrentRequest() {
        RequestEditSubPanel subPanel = getCurrentSubPanel();
        return subPanel != null ? subPanel.getCurrentRequest() : null;
    }

    // 更新当前Tab内容
    public void updateRequest(HttpRequestItem item) {
        RequestEditSubPanel subPanel = getCurrentSubPanel();
        if (subPanel != null) subPanel.updateRequestForm(item);
    }

    private RequestEditSubPanel getCurrentSubPanel() {
        Component comp = tabbedPane.getSelectedComponent();
        if (comp instanceof RequestEditSubPanel) return (RequestEditSubPanel) comp;
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
                    subPanel.updateRequestForm(item);
                    return;
                }
            }
        }
        // 没有同id Tab则新建
        RequestEditSubPanel subPanel = new RequestEditSubPanel(id);
        subPanel.updateRequestForm(item);
        String name = item.getName() != null ? item.getName() : REQUEST_STRING + (tabbedPane.getTabCount());
        int plusTabIdx = tabbedPane.getTabCount() > 0 ? tabbedPane.getTabCount() - 1 : 0;
        tabbedPane.insertTab(name, null, subPanel, null, plusTabIdx);
        tabbedPane.setTabComponentAt(plusTabIdx,
                new ClosableTabComponent(name, subPanel, tabbedPane, this::saveCurrentRequest));
        tabbedPane.setSelectedIndex(plusTabIdx);
    }

    /**
     * 设置保存快捷键 (Ctrl+S)
     */
    private void setupSaveShortcut() {
        // 创建保存动作
        Action saveAction = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                saveCurrentRequest();
            }
        };

        // 注册快捷键
        InputMap inputMap = getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap actionMap = getActionMap();

        // Ctrl+S 快捷键
        KeyStroke ctrlS = KeyStroke.getKeyStroke(KeyEvent.VK_S, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx());
        inputMap.put(ctrlS, "saveRequest");
        actionMap.put("saveRequest", saveAction);
    }

    /**
     * 保存当前请求
     */
    public void saveCurrentRequest() {
        HttpRequestItem currentItem = getCurrentRequest();
        if (currentItem == null) {
            return;
        }

        String name = currentItem.getName();
        boolean isNewRequest = name == null;

        // 查找请求集合面板
        RequestCollectionsLeftPanel collectionPanel = SingletonFactory.getInstance(RequestCollectionsLeftPanel.class);

        if (isNewRequest) {
            // 新请求：弹出对话框让用户输入名称和选择文件夹
            saveNewRequest(collectionPanel, currentItem);
        } else {
            // 已存在的请求：弹出确认对话框
            int confirm = JOptionPane.showConfirmDialog(this,
                    "是否更新当前请求?\n" + "名称: " + currentItem.getName(),
                    "更新请求",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE);

            if (confirm == JOptionPane.YES_OPTION) {
                updateExistingRequest(collectionPanel, currentItem);
            }
        }
    }

    /**
     * 公共方法：弹窗让用户选择分组并输入请求名称，返回分组Object[]和请求名
     *
     * @param groupTreeModel 分组树模型
     * @param defaultName    默认请求名，可为null
     * @return Object[]{Object[] groupObj, String requestName}，若取消返回null
     */
    public static Object[] showGroupAndNameDialog(TreeModel groupTreeModel, String defaultName) {
        if (groupTreeModel == null || groupTreeModel.getRoot() == null) {
            JOptionPane.showMessageDialog(null, "请先创建一个分组", "提示", JOptionPane.INFORMATION_MESSAGE);
            return null;
        }
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 18, 10, 18));
        JPanel namePanel = new JPanel(new BorderLayout(8, 0));
        JLabel nameLabel = new JLabel("请求名称:");
        nameLabel.setPreferredSize(new Dimension(70, 28));
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
        JLabel groupLabel = new JLabel("选择分组:");
        groupLabel.setPreferredSize(new Dimension(70, 28));
        groupPanel.add(groupLabel, BorderLayout.WEST);
        JTree groupTree = getGroupTree(groupTreeModel);
        JScrollPane treeScroll = new JScrollPane(groupTree);
        treeScroll.setPreferredSize(new Dimension(220, 160));
        treeScroll.setBorder(BorderFactory.createLineBorder(new Color(220, 220, 220)));
        groupPanel.add(treeScroll, BorderLayout.CENTER);
        groupPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 180));
        groupPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(groupPanel);
        int result = JOptionPane.showConfirmDialog(null, panel, "保存请求", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result == JOptionPane.OK_OPTION) {
            String requestName = nameField.getText();
            if (requestName == null || requestName.trim().isEmpty()) {
                JOptionPane.showMessageDialog(null, "请输入请求名称", "提示", JOptionPane.WARNING_MESSAGE);
                return null;
            }
            javax.swing.tree.TreePath selectedPath = groupTree.getSelectionPath();
            if (selectedPath == null) {
                JOptionPane.showMessageDialog(null, "请选择分组", "提示", JOptionPane.WARNING_MESSAGE);
                return null;
            }
            Object selectedGroupNode = selectedPath.getLastPathComponent();
            Object[] groupObj = null;
            if (selectedGroupNode instanceof javax.swing.tree.DefaultMutableTreeNode node) {
                Object userObj = node.getUserObject();
                if (userObj instanceof Object[] arr && "group".equals(arr[0])) {
                    groupObj = arr;
                }
            }
            if (groupObj == null) {
                JOptionPane.showMessageDialog(null, "请选择有效的分组节点", "提示", JOptionPane.WARNING_MESSAGE);
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
                    if (userObj instanceof Object[] arr && "group".equals(arr[0])) {
                        int groupCount = 0;
                        for (int i = 0; i < node.getChildCount(); i++) {
                            Object childObj = ((DefaultMutableTreeNode) node.getChildAt(i)).getUserObject();
                            if (childObj instanceof Object[] cArr && "group".equals(cArr[0])) {
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
                        if (childObj instanceof Object[] cArr && "group".equals(cArr[0])) {
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
                    if (userObj instanceof Object[] arr && "group".equals(arr[0])) {
                        for (int i = 0; i < treeNode.getChildCount(); i++) {
                            Object childObj = ((DefaultMutableTreeNode) treeNode.getChildAt(i)).getUserObject();
                            if (childObj instanceof Object[] cArr && "group".equals(cArr[0])) {
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
        groupTree.setCellRenderer(new javax.swing.tree.DefaultTreeCellRenderer() {
            @Override
            public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {
                super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
                if (value instanceof DefaultMutableTreeNode node) {
                    Object userObj = node.getUserObject();
                    if (userObj instanceof Object[] arr && "group".equals(arr[0])) {
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
                tabbedPane.setTabComponentAt(currentTabIndex, new ClosableTabComponent(requestName, getCurrentSubPanel(), tabbedPane, this::saveCurrentRequest));
            }
            RequestEditSubPanel subPanel = getCurrentSubPanel();
            if (subPanel != null) {
                subPanel.updateRequestForm(item);
            }
        }
        JOptionPane.showMessageDialog(null, "请求已保存", "成功", JOptionPane.INFORMATION_MESSAGE);
    }

    /**
     * 更新已存在的请求
     */
    private void updateExistingRequest(RequestCollectionsLeftPanel collectionPanel, HttpRequestItem item) {
        if (!collectionPanel.updateExistingRequest(item)) {
            log.error("更新请求失败: {}", item.getId() + " - " + item.getName());
            JOptionPane.showMessageDialog(this, "更新请求失败", "错误", JOptionPane.ERROR_MESSAGE);
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
        setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        tabbedPane = new JTabbedPane();
        add(tabbedPane, BorderLayout.CENTER);
        addNewTab(REQUEST_STRING + "1"); // 默认添加第一个请求Tab
        setupSaveShortcut();
        // 监听tab切换，选中“+”Tab时自动新增
        tabbedPane.addChangeListener(e -> {
            int idx = tabbedPane.getSelectedIndex();
            if (idx == tabbedPane.getTabCount() - 1 && isPlusTab(idx)) {
                // 检测剪贴板cURL
                String curlText = RequestCollectionsLeftPanel.getClipboardCurlText();
                if (curlText != null) {
                    int result = JOptionPane.showConfirmDialog(this, "检测到剪贴板有 cURL 命令，是否导入到新请求？", "导入cURL", JOptionPane.YES_NO_OPTION);
                    if (result == JOptionPane.YES_OPTION) {
                        try {
                            CurlRequest curlRequest = CurlParser.parse(curlText);
                            if (curlRequest.url != null) {
                                HttpRequestItem item = new HttpRequestItem();
                                item.setName(null);
                                item.setUrl(curlRequest.url);
                                item.setMethod(curlRequest.method);
                                item.setHeaders(curlRequest.headers);
                                item.setBody(curlRequest.body);
                                item.setParams(curlRequest.params);
                                item.setFormData(curlRequest.formData);
                                item.setFormFiles(curlRequest.formFiles);
                                // 新建Tab并填充内容
                                RequestEditSubPanel tab = addNewTab(null);
                                item.setId(tab.getId());
                                tab.updateRequestForm(item);
                                // 清空剪贴板内容
                                Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(""), null);
                                return;
                            }
                        } catch (Exception ex) {
                            JOptionPane.showMessageDialog(this, "解析cURL出错: " + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
                        }
                    }
                }
                addNewTab(null);
            }
        });
    }

    @Override
    protected void registerListeners() {

    }


    public RequestEditSubPanel getRequestEditSubPanel(String reqItemId) {
        // 通过 RequestEditPanel 获取 tabbedPane
        RequestEditPanel requestEditPanel = SingletonFactory.getInstance(RequestEditPanel.class);
        JTabbedPane tabbedPane = requestEditPanel.getTabbedPane();
        for (int i = 0; i < tabbedPane.getTabCount() - 1; i++) {
            Component comp = tabbedPane.getComponentAt(i);
            if (comp instanceof RequestEditSubPanel subPanel) {
                if (reqItemId.equals(subPanel.getId())) {
                    return subPanel;
                }
            }
        }
        return null;
    }

    /**
     * 通过弹窗让用户选择分组和命名，保存 HttpRequestItem 到集合（公用方法，适用于cURL导入等场景）
     *
     * @param item 要保存的请求
     */
    public static boolean saveRequestWithGroupDialog(HttpRequestItem item) {
        RequestCollectionsLeftPanel collectionPanel = SingletonFactory.getInstance(RequestCollectionsLeftPanel.class);
        TreeModel groupTreeModel = collectionPanel.getGroupTreeModel();
        Object[] result = showGroupAndNameDialog(groupTreeModel, item.getName());
        if (result == null) return false;
        Object[] groupObj = (Object[]) result[0];
        String requestName = (String) result[1];
        item.setName(requestName);
        item.setId(IdUtil.simpleUUID());
        collectionPanel.saveRequestToGroup(groupObj, item);
        JOptionPane.showMessageDialog(null, "请求已保存", "成功", JOptionPane.INFORMATION_MESSAGE);
        return true;
    }
}
