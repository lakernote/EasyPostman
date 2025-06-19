package com.laker.postman.panel.collections.edit;

import cn.hutool.core.util.IdUtil;
import com.laker.postman.common.SingletonPanelFactory;
import com.laker.postman.common.tab.ClosableTabComponent;
import com.laker.postman.common.tab.PlusTabComponent;
import com.laker.postman.model.HttpRequestItem;
import com.laker.postman.panel.collections.RequestCollectionsSubPanel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import javax.swing.tree.TreeModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

/**
 * 请求编辑面板，支持多标签页，每个标签页为独立的请求编辑子面板
 */
@Slf4j
public class RequestEditPanel extends JPanel {
    @Getter
    private final JTabbedPane tabbedPane; // 使用 JTabbedPane 管理多个请求编辑子面板

    private static RequestEditPanel INSTANCE; // 单例模式

    public static RequestEditPanel getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new RequestEditPanel();
        }
        return INSTANCE;
    }

    private RequestEditPanel() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        tabbedPane = new JTabbedPane();
        add(tabbedPane, BorderLayout.CENTER);
        addNewTab("请求1");
        setupSaveShortcut();
        // 新增：监听tab切换，选中“+”Tab时自动新增
        tabbedPane.addChangeListener(e -> {
            int idx = tabbedPane.getSelectedIndex();
            if (idx == tabbedPane.getTabCount() - 1 && isPlusTab(idx)) {
                addNewTab(null);
            }
        });
    }

    // 新建Tab，可指定标题
    public void addNewTab(String title) {
        // 先移除+Tab
        if (tabbedPane.getTabCount() > 0 && isPlusTab(tabbedPane.getTabCount() - 1)) {
            tabbedPane.removeTabAt(tabbedPane.getTabCount() - 1);
        }
        String tabTitle = title != null ? title : "请求" + (tabbedPane.getTabCount() + 1);
        RequestEditSubPanel subPanel = new RequestEditSubPanel();
        tabbedPane.addTab(tabTitle, subPanel);
        tabbedPane.setTabComponentAt(tabbedPane.getTabCount() - 1,
                new ClosableTabComponent(tabTitle, subPanel, tabbedPane, this::saveCurrentRequest));
        tabbedPane.setSelectedIndex(tabbedPane.getTabCount() - 1);
        // 保证“+”Tab始终在最后
        addPlusTab();
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
                HttpRequestItem tabItem = subPanel.getCurrentRequest();
                if (id.equals(tabItem.getId())) {
                    tabbedPane.setSelectedIndex(i);
                    subPanel.updateRequestForm(item);
                    return;
                }
            }
        }
        // 没有同id Tab则新建
        RequestEditSubPanel subPanel = new RequestEditSubPanel();
        subPanel.updateRequestForm(item);
        String name = item.getName() != null ? item.getName() : "请求" + (tabbedPane.getTabCount());
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

        String id = currentItem.getId();
        boolean isNewRequest = id == null || id.isEmpty();

        // 查找请求集合面板
        RequestCollectionsSubPanel collectionPanel = SingletonPanelFactory.getInstance(RequestCollectionsSubPanel.class);
        if (collectionPanel == null) {
            JOptionPane.showMessageDialog(this, "无法找到请求集合面板，保存失败", "错误", JOptionPane.ERROR_MESSAGE);
            return;
        }

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
     * 保存新请求（分组选择优化为树结构）
     */
    private void saveNewRequest(RequestCollectionsSubPanel collectionPanel, HttpRequestItem item) {
        // 获取分组树模型（假设有 getGroupTreeModel 方法，否则递归构建）
        TreeModel groupTreeModel = collectionPanel.getGroupTreeModel();
        if (groupTreeModel == null || groupTreeModel.getRoot() == null) {
            JOptionPane.showMessageDialog(null, "请先创建一个分组", "提示", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        // 优化UI：使用更紧凑的垂直Box布局，分组树只显示group节点
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 18, 10, 18));

        // 请求名称输入
        JPanel namePanel = new JPanel(new BorderLayout(8, 0));
        JLabel nameLabel = new JLabel("请求名称:");
        nameLabel.setPreferredSize(new Dimension(70, 28));
        JTextField nameField = new JTextField(20);
        nameField.setPreferredSize(new Dimension(180, 28));
        namePanel.add(nameLabel, BorderLayout.WEST);
        namePanel.add(nameField, BorderLayout.CENTER);
        namePanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        namePanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(namePanel);
        panel.add(Box.createVerticalStrut(12));

        // 分组树选择
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

        // 显示对话框
        int result = JOptionPane.showConfirmDialog(null, panel, "保存请求", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result == JOptionPane.OK_OPTION) {
            String requestName = nameField.getText();
            if (requestName == null || requestName.trim().isEmpty()) {
                JOptionPane.showMessageDialog(null, "请输入请求名称", "提示", JOptionPane.WARNING_MESSAGE);
                return;
            }
            javax.swing.tree.TreePath selectedPath = groupTree.getSelectionPath();
            if (selectedPath == null) {
                JOptionPane.showMessageDialog(null, "请选择分组", "提示", JOptionPane.WARNING_MESSAGE);
                return;
            }
            Object selectedGroupNode = selectedPath.getLastPathComponent();
            // 取出 Object[] 作为分组参数
            Object[] groupObj = null;
            if (selectedGroupNode instanceof javax.swing.tree.DefaultMutableTreeNode node) {
                Object userObj = node.getUserObject();
                if (userObj instanceof Object[] arr && "group".equals(arr[0])) {
                    groupObj = arr;
                }
            }
            if (groupObj == null) {
                JOptionPane.showMessageDialog(null, "请选择有效的分组节点", "提示", JOptionPane.WARNING_MESSAGE);
                return;
            }
            item.setName(requestName);
            // 为请求生成一个新的ID
            item.setId(IdUtil.simpleUUID());
            collectionPanel.saveRequestToGroup(groupObj, item);
            int currentTabIndex = tabbedPane.getSelectedIndex();
            if (currentTabIndex >= 0) {
                // 更新当前Tab标题
                tabbedPane.setTitleAt(currentTabIndex, requestName);
                // 更新自定义标签组件
                Component tabComp = tabbedPane.getTabComponentAt(currentTabIndex);
                if (tabComp instanceof ClosableTabComponent) {
                    tabbedPane.setTabComponentAt(currentTabIndex, new ClosableTabComponent(requestName, getCurrentSubPanel(), tabbedPane, this::saveCurrentRequest));
                }
                // 同步刷新当前编辑面板内容
                RequestEditSubPanel subPanel = getCurrentSubPanel();
                if (subPanel != null) {
                    subPanel.updateRequestForm(item);
                }
            }
            JOptionPane.showMessageDialog(null, "请求已保存", "成功", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private JTree getGroupTree(TreeModel groupTreeModel) {
        JTree groupTree = new JTree(groupTreeModel);
        groupTree.setRootVisible(false);
        groupTree.setShowsRootHandles(true);
        // 只显示group节点，非group节点隐藏
        groupTree.setCellRenderer(new javax.swing.tree.DefaultTreeCellRenderer() {
            @Override
            public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {
                super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
                if (value instanceof javax.swing.tree.DefaultMutableTreeNode node) {
                    Object userObj = node.getUserObject();
                    if (userObj instanceof Object[] arr && "group".equals(arr[0])) {
                        setText(String.valueOf(arr[1]));
                        setIcon(getDefaultClosedIcon());
                    } else {
                        setText(""); // 非group节点不显示
                        setIcon(null);
                    }
                }
                return this;
            }
        });
        return groupTree;
    }

    /**
     * 更新已存在的请求
     */
    private void updateExistingRequest(RequestCollectionsSubPanel collectionPanel, HttpRequestItem item) {
        if (!collectionPanel.updateExistingRequest(item)) {
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
}