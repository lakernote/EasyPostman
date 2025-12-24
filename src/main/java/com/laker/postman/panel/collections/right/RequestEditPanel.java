package com.laker.postman.panel.collections.right;

import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.util.IdUtil;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.laker.postman.common.SingletonBasePanel;
import com.laker.postman.common.SingletonFactory;
import com.laker.postman.common.component.tab.ClosableTabComponent;
import com.laker.postman.common.component.tab.PlusPanel;
import com.laker.postman.common.component.tab.PlusTabComponent;
import com.laker.postman.frame.MainFrame;
import com.laker.postman.model.CurlRequest;
import com.laker.postman.model.HttpRequestItem;
import com.laker.postman.model.RequestGroup;
import com.laker.postman.model.RequestItemProtocolEnum;
import com.laker.postman.panel.collections.left.RequestCollectionsLeftPanel;
import com.laker.postman.panel.collections.right.request.RequestEditSubPanel;
import com.laker.postman.service.collections.RequestsTabsService;
import com.laker.postman.service.curl.CurlParser;
import com.laker.postman.service.http.HttpUtil;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;
import com.laker.postman.util.NotificationUtil;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import javax.swing.tree.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

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

    // 预览模式：单击使用的临时 tab（可被下次单击替换）
    private Component previewTab = null; // 可以是 RequestEditSubPanel 或 GroupEditPanel
    private int previewTabIndex = -1; // 预览 tab 的索引


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
    public void addNewTab(String title) {
        addNewTab(title, RequestItemProtocolEnum.HTTP);
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

    /**
     * 显示或创建预览 tab（用于单击时的临时预览）
     * 预览 tab 的特点：
     * 1. 如果没有预览 tab，创建一个新的
     * 2. 如果已有预览 tab，复用它（替换内容）
     * 3. 如果该 request 已有固定 tab，则切换到固定 tab
     * 4. 预览 tab 会在标题显示斜体，提示这是临时的
     */
    public void showOrCreatePreviewTab(HttpRequestItem item) {
        String id = item.getId();
        if (id == null || id.isEmpty()) {
            addNewTab(null);
            updateRequest(item);
            return;
        }

        // 1. 先查找是否已有固定的 tab（不包括预览 tab）
        for (int i = 0; i < tabbedPane.getTabCount() - 1; i++) {
            Component comp = tabbedPane.getComponentAt(i);
            if (i != previewTabIndex && comp instanceof RequestEditSubPanel subPanel) {
                if (id.equals(subPanel.getId())) {
                    tabbedPane.setSelectedIndex(i);
                    return;
                }
            }
        }

        // 2. 检查预览 tab 是否存在且有效
        if (previewTab != null && previewTabIndex >= 0 && previewTabIndex < tabbedPane.getTabCount()) {
            Component currentPreview = tabbedPane.getComponentAt(previewTabIndex);
            if (currentPreview != previewTab) {
                previewTab = null;
                previewTabIndex = -1;
            }
        } else {
            previewTab = null;
            previewTabIndex = -1;
        }

        // 3. 复用或创建预览 tab
        String name = CharSequenceUtil.isNotBlank(item.getName()) ? item.getName() : REQUEST_STRING;
        RequestEditSubPanel newPanel = new RequestEditSubPanel(id, item.getProtocol());
        newPanel.initPanelData(item);

        if (previewTab != null && previewTabIndex >= 0) {
            // 复用现有预览 tab：替换内容
            previewTab = newPanel;
            tabbedPane.setComponentAt(previewTabIndex, previewTab);
            tabbedPane.setTitleAt(previewTabIndex, name);
            ClosableTabComponent tabComponent = new ClosableTabComponent(name, item.getProtocol());
            tabComponent.setPreviewMode(true);
            tabbedPane.setTabComponentAt(previewTabIndex, tabComponent);
            tabbedPane.setSelectedIndex(previewTabIndex);
        } else {
            // 创建新的预览 tab
            int plusTabIdx = tabbedPane.getTabCount() > 0 ? tabbedPane.getTabCount() - 1 : 0;
            if (isPlusTab(plusTabIdx)) {
                tabbedPane.removeTabAt(plusTabIdx);
            }
            previewTab = newPanel;
            tabbedPane.addTab(name, previewTab);
            previewTabIndex = tabbedPane.getTabCount() - 1;
            ClosableTabComponent tabComponent = new ClosableTabComponent(name, item.getProtocol());
            tabComponent.setPreviewMode(true);
            tabbedPane.setTabComponentAt(previewTabIndex, tabComponent);
            tabbedPane.setSelectedIndex(previewTabIndex);
            addPlusTab();
        }
    }

    /**
     * 将预览 tab 转为固定 tab
     */
    public void promotePreviewTabToPermanent() {
        if (previewTab != null && previewTabIndex >= 0 && previewTabIndex < tabbedPane.getTabCount()) {
            Component tabComponent = tabbedPane.getTabComponentAt(previewTabIndex);
            if (tabComponent instanceof ClosableTabComponent closableTab) {
                closableTab.setPreviewMode(false);
            }
            previewTab = null;
            previewTabIndex = -1;
        }
    }

    /**
     * 显示或创建 Group 的预览 tab（用于单击 Group 时）
     */
    public void showOrCreatePreviewTabForGroup(DefaultMutableTreeNode groupNode, RequestGroup group) {
        String groupId = group.getId();

        // 1. 先查找是否已有固定的 Group tab（不包括预览 tab）
        if (groupId != null && !groupId.isEmpty()) {
            for (int i = 0; i < tabbedPane.getTabCount() - 1; i++) {
                if (i != previewTabIndex) {
                    Component comp = tabbedPane.getComponentAt(i);
                    if (comp instanceof GroupEditPanel existingPanel) {
                        if (groupId.equals(existingPanel.getGroup().getId())) {
                            tabbedPane.setSelectedIndex(i);
                            return;
                        }
                    }
                }
            }
        }

        // 2. 检查预览 tab 是否存在且有效
        if (previewTab != null && previewTabIndex >= 0 && previewTabIndex < tabbedPane.getTabCount()) {
            Component currentPreview = tabbedPane.getComponentAt(previewTabIndex);
            if (currentPreview != previewTab) {
                previewTab = null;
                previewTabIndex = -1;
            }
        } else {
            previewTab = null;
            previewTabIndex = -1;
        }

        // 3. 复用或创建预览 tab
        String groupName = group.getName();
        GroupEditPanel groupEditPanel = new GroupEditPanel(groupNode, group, () -> {
            RequestCollectionsLeftPanel leftPanel = SingletonFactory.getInstance(RequestCollectionsLeftPanel.class);
            leftPanel.getTreeModel().nodeChanged(groupNode);
            leftPanel.getPersistence().saveRequestGroups();
        });

        if (previewTab != null && previewTabIndex >= 0) {
            // 复用现有预览 tab：替换内容
            previewTab = groupEditPanel;
            tabbedPane.setComponentAt(previewTabIndex, previewTab);
            tabbedPane.setTitleAt(previewTabIndex, groupName);
            ClosableTabComponent tabComponent = new ClosableTabComponent(groupName, null);
            tabComponent.setPreviewMode(true);
            tabbedPane.setTabComponentAt(previewTabIndex, tabComponent);
            tabbedPane.setSelectedIndex(previewTabIndex);
        } else {
            // 创建新的预览 tab
            int plusTabIdx = tabbedPane.getTabCount() > 0 ? tabbedPane.getTabCount() - 1 : 0;
            if (isPlusTab(plusTabIdx)) {
                tabbedPane.removeTabAt(plusTabIdx);
            }
            previewTab = groupEditPanel;
            tabbedPane.addTab(groupName, previewTab);
            previewTabIndex = tabbedPane.getTabCount() - 1;
            ClosableTabComponent tabComponent = new ClosableTabComponent(groupName, null);
            tabComponent.setPreviewMode(true);
            tabbedPane.setTabComponentAt(previewTabIndex, tabComponent);
            tabbedPane.setSelectedIndex(previewTabIndex);
            addPlusTab();
        }
    }

    // showOrCreateTab 需适配 "+" Tab（双击时调用，创建固定 tab）
    public void showOrCreateTab(HttpRequestItem item) {
        String id = item.getId();
        if (id == null || id.isEmpty()) {
            addNewTab(null);
            updateRequest(item);
            return;
        }

        // 如果当前预览的就是这个 request，则将预览 tab 转为固定 tab
        if (previewTab instanceof RequestEditSubPanel subPanel && id.equals(subPanel.getId())) {
            promotePreviewTabToPermanent();
            return;
        }

        // 查找同id Tab（不查"+"Tab）
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
    private static final String ACTION_CLOSE_CURRENT_TAB = "closeCurrentTab";
    private static final String ACTION_CLOSE_OTHER_TABS = "closeOtherTabs";
    private static final String ACTION_CLOSE_ALL_TABS = "closeAllTabs";

    /**
     * 重新加载快捷键（快捷键设置修改后调用）
     */
    public void reloadShortcuts() {
        // 清除所有现有的快捷键绑定
        InputMap inputMap = this.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap actionMap = this.getActionMap();

        inputMap.clear();
        actionMap.clear();

        // 重新注册快捷键
        registerShortcuts();
    }

    /**
     * 统一注册所有快捷键
     */
    private void registerShortcuts() {
        InputMap inputMap = this.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap actionMap = this.getActionMap();

        // 保存快捷键
        KeyStroke saveKey = com.laker.postman.service.setting.ShortcutManager.getKeyStroke(
                com.laker.postman.service.setting.ShortcutManager.SAVE_REQUEST);
        if (saveKey != null) {
            inputMap.put(saveKey, ACTION_SAVE_REQUEST);
            actionMap.put(ACTION_SAVE_REQUEST, new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    saveCurrentRequest();
                }
            });
        }

        // 新建标签页快捷键
        KeyStroke newTabKey = com.laker.postman.service.setting.ShortcutManager.getKeyStroke(
                com.laker.postman.service.setting.ShortcutManager.NEW_REQUEST);
        if (newTabKey != null) {
            inputMap.put(newTabKey, ACTION_NEW_REQUEST_TAB);
            actionMap.put(ACTION_NEW_REQUEST_TAB, new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    addNewTab(null);
                }
            });
        }

        // 关闭当前标签页快捷键
        KeyStroke closeCurrentKey = com.laker.postman.service.setting.ShortcutManager.getKeyStroke(
                com.laker.postman.service.setting.ShortcutManager.CLOSE_CURRENT_TAB);
        if (closeCurrentKey != null) {
            inputMap.put(closeCurrentKey, ACTION_CLOSE_CURRENT_TAB);
            actionMap.put(ACTION_CLOSE_CURRENT_TAB, new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    closeCurrentTab();
                }
            });
        }

        // 关闭其他标签页快捷键
        KeyStroke closeOthersKey = com.laker.postman.service.setting.ShortcutManager.getKeyStroke(
                com.laker.postman.service.setting.ShortcutManager.CLOSE_OTHER_TABS);
        if (closeOthersKey != null) {
            inputMap.put(closeOthersKey, ACTION_CLOSE_OTHER_TABS);
            actionMap.put(ACTION_CLOSE_OTHER_TABS, new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    closeOtherTabs();
                }
            });
        }

        // 关闭所有标签页快捷键
        KeyStroke closeAllKey = com.laker.postman.service.setting.ShortcutManager.getKeyStroke(
                com.laker.postman.service.setting.ShortcutManager.CLOSE_ALL_TABS);
        if (closeAllKey != null) {
            inputMap.put(closeAllKey, ACTION_CLOSE_ALL_TABS);
            actionMap.put(ACTION_CLOSE_ALL_TABS, new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    closeAllTabs();
                }
            });
        }
    }

    /**
     * 保存当前请求
     */
    public void saveCurrentRequest() {
        // 保存请求时，如果当前是预览 tab，则转为固定 tab（模仿 Postman 行为）
        promotePreviewTabToPermanent();

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

        // 创建主面板，使用GridBagLayout以获得更好的布局控制
        JPanel mainPanel = new JPanel(new GridBagLayout());
        mainPanel.setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(0, 0, 12, 0);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // 请求名称标签
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0;
        JLabel nameLabel = new JLabel(I18nUtil.getMessage(MessageKeys.REQUEST_NAME) + ":");
        nameLabel.setFont(nameLabel.getFont().deriveFont(Font.BOLD, 13f));
        mainPanel.add(nameLabel, gbc);

        // 请求名称输入框
        gbc.gridy = 1;
        gbc.weightx = 1.0;
        gbc.insets = new Insets(0, 0, 20, 0);
        JTextField nameField = new JTextField(25);
        nameField.setPreferredSize(new Dimension(350, 32));
        if (defaultName != null && !defaultName.trim().isEmpty()) {
            nameField.setText(defaultName);
            nameField.selectAll(); // 自动选中文本，方便用户直接修改
        }
        mainPanel.add(nameField, gbc);

        // 分组选择标签
        gbc.gridy = 2;
        gbc.insets = new Insets(0, 0, 8, 0);
        JLabel groupLabel = new JLabel(I18nUtil.getMessage(MessageKeys.SELECT_GROUP) + ":");
        groupLabel.setFont(groupLabel.getFont().deriveFont(Font.BOLD, 13f));
        mainPanel.add(groupLabel, gbc);

        // 分组树
        gbc.gridy = 3;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.insets = new Insets(0, 0, 0, 0);
        JTree groupTree = getGroupTree(groupTreeModel);

        // 展开第一层节点
        for (int i = 0; i < groupTree.getRowCount(); i++) {
            groupTree.expandRow(i);
        }

        JScrollPane treeScroll = new JScrollPane(groupTree);
        treeScroll.setPreferredSize(new Dimension(350, 200));
        treeScroll.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200), 1),
                BorderFactory.createEmptyBorder(4, 4, 4, 4)
        ));
        mainPanel.add(treeScroll, gbc);

        // 创建自定义对话框以支持更好的交互
        JDialog dialog = new JDialog(SingletonFactory.getInstance(MainFrame.class),
                I18nUtil.getMessage(MessageKeys.SAVE_REQUEST), true);
        dialog.setLayout(new BorderLayout());
        dialog.add(mainPanel, BorderLayout.CENTER);

        // 按钮面板
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 12));
        buttonPanel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(220, 220, 220)));

        JButton cancelButton = new JButton(I18nUtil.getMessage(MessageKeys.BUTTON_CANCEL));
        JButton okButton = new JButton(I18nUtil.getMessage(MessageKeys.GENERAL_OK));
        okButton.setPreferredSize(new Dimension(80, 32));
        cancelButton.setPreferredSize(new Dimension(80, 32));

        // 设置确定按钮为默认按钮样式
        okButton.setBackground(new Color(0, 123, 255));
        okButton.setForeground(Color.WHITE);
        okButton.setFocusPainted(false);
        okButton.setBorderPainted(false);
        okButton.setOpaque(true);

        final Object[] result = {null};

        // 确定按钮逻辑
        Runnable okAction = () -> {
            String requestName = nameField.getText();
            if (requestName == null || requestName.trim().isEmpty()) {
                NotificationUtil.showWarning(I18nUtil.getMessage(MessageKeys.PLEASE_ENTER_REQUEST_NAME));
                nameField.requestFocusInWindow();
                return;
            }

            TreePath selectedPath = groupTree.getSelectionPath();
            if (selectedPath == null) {
                NotificationUtil.showWarning(I18nUtil.getMessage(MessageKeys.PLEASE_SELECT_GROUP));
                groupTree.requestFocusInWindow();
                return;
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
                NotificationUtil.showWarning(I18nUtil.getMessage(MessageKeys.PLEASE_SELECT_VALID_GROUP));
                return;
            }

            result[0] = new Object[]{groupObj, requestName.trim()};
            dialog.dispose();
        };

        okButton.addActionListener(e -> okAction.run());
        cancelButton.addActionListener(e -> dialog.dispose());

        buttonPanel.add(cancelButton);
        buttonPanel.add(okButton);
        dialog.add(buttonPanel, BorderLayout.SOUTH);

        // 支持回车键确认
        nameField.addActionListener(e -> okAction.run());

        // 支持ESC键取消
        dialog.getRootPane().registerKeyboardAction(
                e -> dialog.dispose(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW
        );

        // 设置默认按钮
        dialog.getRootPane().setDefaultButton(okButton);

        // 设置对话框属性
        dialog.setSize(420, 420);
        dialog.setLocationRelativeTo(SingletonFactory.getInstance(MainFrame.class));
        dialog.setResizable(false);

        // 显示对话框后自动聚焦到名称输入框
        SwingUtilities.invokeLater(() -> nameField.requestFocusInWindow());

        dialog.setVisible(true);

        return (Object[]) result[0];
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
                    if (userObj instanceof Object[] arr && GROUP.equals(arr[0]) && arr[1] instanceof RequestGroup group) {
                        setText(group.getName());
                        // 橙色实心文件夹，模拟Postman分组
                        setIcon(new FlatSVGIcon("icons/group.svg", 16, 16));
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

    /**
     * 更新 GroupEditPanel 的 Tab 标题
     */
    public void updateGroupTabTitle(GroupEditPanel panel, String newTitle) {
        int idx = tabbedPane.indexOfComponent(panel);
        if (idx < 0) return;

        // 重新创建 Tab 组件以更新标题
        tabbedPane.setTabComponentAt(idx, new ClosableTabComponent(newTitle, null));
        tabbedPane.setToolTipTextAt(idx, newTitle);
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

                                    if (curlRequest.headersList != null && !curlRequest.headersList.isEmpty()) {
                                        item.setHeadersList(curlRequest.headersList);
                                    }

                                    item.setBody(curlRequest.body);

                                    if (curlRequest.paramsList != null && !curlRequest.paramsList.isEmpty()) {
                                        item.setParamsList(curlRequest.paramsList);
                                    }

                                    if (curlRequest.formDataList != null && !curlRequest.formDataList.isEmpty()) {
                                        item.setFormDataList(curlRequest.formDataList);
                                    }

                                    if (curlRequest.urlencodedList != null && !curlRequest.urlencodedList.isEmpty()) {
                                        item.setUrlencodedList(curlRequest.urlencodedList);
                                    }

                                    if (HttpUtil.isSSERequest(item)) {
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
                                NotificationUtil.showError(I18nUtil.getMessage(MessageKeys.PARSE_CURL_ERROR, ex.getMessage()));
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

    /**
     * 显示分组编辑面板（参考 Postman，不使用弹窗）
     * 双击时调用，创建固定 tab
     */
    public void showGroupEditPanel(DefaultMutableTreeNode groupNode, RequestGroup group) {
        String groupId = group.getId();

        // 如果当前预览的就是这个 group，则将预览 tab 转为固定 tab
        if (previewTab instanceof GroupEditPanel previewGroupPanel &&
                groupId != null && groupId.equals(previewGroupPanel.getGroup().getId())) {
            promotePreviewTabToPermanent();
            return;
        }

        // 先查找是否已经打开了相同的分组（使用 ID 判断）
        if (groupId != null && !groupId.isEmpty()) {
            for (int i = 0; i < tabbedPane.getTabCount() - 1; i++) {
                if (i != previewTabIndex) {
                    Component comp = tabbedPane.getComponentAt(i);
                    if (comp instanceof GroupEditPanel existingPanel) {
                        if (groupId.equals(existingPanel.getGroup().getId())) {
                            tabbedPane.setSelectedIndex(i);
                            return;
                        }
                    }
                }
            }
        }

        // 没有找到，创建新的分组编辑面板
        // 先移除+Tab
        if (tabbedPane.getTabCount() > 0 && isPlusTab(tabbedPane.getTabCount() - 1)) {
            tabbedPane.removeTabAt(tabbedPane.getTabCount() - 1);
        }

        // 创建分组编辑面板
        GroupEditPanel groupEditPanel = new GroupEditPanel(groupNode, group, () -> {
            // 保存回调
            RequestCollectionsLeftPanel leftPanel = SingletonFactory.getInstance(RequestCollectionsLeftPanel.class);
            leftPanel.getTreeModel().nodeChanged(groupNode);
            leftPanel.getPersistence().saveRequestGroups();
        });

        // 添加为新 Tab
        String groupName = group.getName();
        tabbedPane.addTab(groupName, groupEditPanel);
        tabbedPane.setTabComponentAt(tabbedPane.getTabCount() - 1, new ClosableTabComponent(groupName, null));
        tabbedPane.setSelectedIndex(tabbedPane.getTabCount() - 1);

        // 恢复+Tab
        addPlusTab();
    }

    /**
     * 关闭当前标签页
     */
    public void closeCurrentTab() {
        int currentIndex = tabbedPane.getSelectedIndex();
        if (currentIndex < 0 || isPlusTab(currentIndex)) {
            return;
        }

        Component component = tabbedPane.getComponentAt(currentIndex);

        // 只有 RequestEditSubPanel 才需要检查是否修改
        if (component instanceof RequestEditSubPanel editSubPanel && editSubPanel.isModified()) {
            int result = JOptionPane.showConfirmDialog(SingletonFactory.getInstance(MainFrame.class),
                    I18nUtil.getMessage(MessageKeys.TAB_UNSAVED_CHANGES_SAVE_CURRENT),
                    I18nUtil.getMessage(MessageKeys.TAB_UNSAVED_CHANGES_TITLE),
                    JOptionPane.YES_NO_CANCEL_OPTION,
                    JOptionPane.WARNING_MESSAGE);
            if (result == JOptionPane.CANCEL_OPTION) return;
            if (result == JOptionPane.YES_OPTION) {
                saveCurrentRequest();
            }
        }


        // 对于其他类型的面板（如 GroupEditPanel），直接关闭
        tabbedPane.remove(currentIndex);

        // 如果还有请求Tab，且没有选中Tab，则选中最后一个请求Tab
        int count = tabbedPane.getTabCount();
        if (count > 1) { // 还有请求Tab和+Tab
            int selected = tabbedPane.getSelectedIndex();
            if (selected == -1 || selected == count - 1) { // 没有选中或选中的是+Tab
                tabbedPane.setSelectedIndex(count - 2); // 选中最后一个请求Tab
            }
        }
    }

    /**
     * 关闭其他标签页
     */
    public void closeOtherTabs() {
        int currentIndex = tabbedPane.getSelectedIndex();
        if (currentIndex < 0 || isPlusTab(currentIndex)) {
            return;
        }

        // 保存当前组件的引用，因为在删除其他标签后索引会改变
        Component currentComponent = tabbedPane.getComponentAt(currentIndex);

        List<Component> toRemove = new ArrayList<>();
        for (int i = 0; i < tabbedPane.getTabCount(); i++) {
            Component comp = tabbedPane.getComponentAt(i);
            // 收集所有非当前、非 + Tab 的组件（包括 RequestEditSubPanel 和 GroupEditPanel）
            if (i != currentIndex && !(comp instanceof PlusPanel)) {
                toRemove.add(comp);
            }
        }

        for (Component comp : toRemove) {
            // 只对 RequestEditSubPanel 检查是否修改
            if (comp instanceof RequestEditSubPanel subPanel && subPanel.isModified()) {
                int idx = tabbedPane.indexOfComponent(comp);
                int result = JOptionPane.showConfirmDialog(SingletonFactory.getInstance(MainFrame.class),
                        I18nUtil.getMessage(MessageKeys.TAB_UNSAVED_CHANGES_SAVE_OTHERS),
                        I18nUtil.getMessage(MessageKeys.TAB_UNSAVED_CHANGES_TITLE),
                        JOptionPane.YES_NO_CANCEL_OPTION,
                        JOptionPane.WARNING_MESSAGE);
                if (result == JOptionPane.CANCEL_OPTION) {
                    tabbedPane.setSelectedIndex(idx);
                    return;
                }
                if (result == JOptionPane.YES_OPTION) {
                    tabbedPane.setSelectedIndex(idx);
                    saveCurrentRequest();
                }
            }
            tabbedPane.remove(comp);
        }

        // 操作完成后，定位到当前tab（使用保存的组件引用而不是旧的索引）
        int idx = tabbedPane.indexOfComponent(currentComponent);
        if (idx >= 0) tabbedPane.setSelectedIndex(idx);
    }

    /**
     * 关闭所有标签页
     */
    public void closeAllTabs() {
        List<Component> toRemove = new ArrayList<>();
        for (int i = 0; i < tabbedPane.getTabCount(); i++) {
            Component comp = tabbedPane.getComponentAt(i);
            // 收集所有非 + Tab 的组件（包括 RequestEditSubPanel 和 GroupEditPanel）
            if (!(comp instanceof PlusPanel)) {
                toRemove.add(comp);
            }
        }

        for (Component comp : toRemove) {
            // 只对 RequestEditSubPanel 检查是否修改
            if (comp instanceof RequestEditSubPanel subPanel && subPanel.isModified()) {
                int idx = tabbedPane.indexOfComponent(comp);
                int result = JOptionPane.showConfirmDialog(SingletonFactory.getInstance(MainFrame.class),
                        I18nUtil.getMessage(MessageKeys.TAB_UNSAVED_CHANGES_SAVE_ALL),
                        I18nUtil.getMessage(MessageKeys.TAB_UNSAVED_CHANGES_TITLE),
                        JOptionPane.YES_NO_CANCEL_OPTION,
                        JOptionPane.WARNING_MESSAGE);
                if (result == JOptionPane.CANCEL_OPTION) {
                    tabbedPane.setSelectedIndex(idx);
                    return;
                }
                if (result == JOptionPane.YES_OPTION) {
                    tabbedPane.setSelectedIndex(idx);
                    saveCurrentRequest();
                }
            }
            tabbedPane.remove(comp);
        }
    }
}
