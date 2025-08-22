package com.laker.postman.panel.collections.right;

import cn.hutool.core.util.IdUtil;
import com.laker.postman.common.SingletonFactory;
import com.laker.postman.common.panel.SingletonBasePanel;
import com.laker.postman.common.tab.ClosableTabComponent;
import com.laker.postman.common.tab.PlusTabComponent;
import com.laker.postman.model.CurlRequest;
import com.laker.postman.model.HttpRequestItem;
import com.laker.postman.panel.collections.left.RequestCollectionsLeftPanel;
import com.laker.postman.panel.collections.right.request.RequestEditSubPanel;
import com.laker.postman.service.curl.CurlParser;
import com.laker.postman.util.I18nUtil;
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
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import static com.laker.postman.util.SystemUtil.getClipboardCurlText;

/**
 * 请求编辑面板，支持多标签页，每个标签页为独立的请求编辑子面板
 */
@Slf4j
public class RequestEditPanel extends SingletonBasePanel {
    public static final String REQUEST_STRING = I18nUtil.getMessage("new.request");
    public static final String PLUS_TAB = "+";
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
        updateTabNew(subPanel, true); // 设置新建状态
        return subPanel;
    }

    // 添加"+"Tab
    public void addPlusTab() {
        JPanel plusPanel = new JPanel(new BorderLayout());
        plusPanel.setOpaque(false);
        plusPanel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)); // 设置手型光标

        // 创建主要内容面板，使用垂直居中的布局
        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setOpaque(false);

        // 添加垂直弹性空间，使内容在垂直方向居中
        contentPanel.add(Box.createVerticalGlue());

        // 添加加号图标
        JLabel plusIcon = new JLabel();
        try {
            plusIcon.setIcon(IconFontSwing.buildIcon(FontAwesome.PLUS_CIRCLE, 32, new Color(100, 100, 100)));
        } catch (Exception e) {
            plusIcon.setText("+");
            plusIcon.setFont(plusIcon.getFont().deriveFont(Font.BOLD, 24f));
        }
        plusIcon.setAlignmentX(Component.CENTER_ALIGNMENT);
        plusIcon.setHorizontalAlignment(SwingConstants.CENTER);
        plusIcon.setForeground(new Color(100, 100, 100));
        contentPanel.add(plusIcon);

        // 添加垂直间距
        contentPanel.add(Box.createVerticalStrut(10));

        // 添加主标题
        JLabel createRequestLabel = new JLabel(I18nUtil.getMessage("create.new.request"));
        createRequestLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        createRequestLabel.setHorizontalAlignment(SwingConstants.CENTER);
        createRequestLabel.setForeground(new Color(100, 100, 100));
        createRequestLabel.setFont(createRequestLabel.getFont().deriveFont(Font.BOLD, 14f));
        contentPanel.add(createRequestLabel);

        // 添加垂直间距
        contentPanel.add(Box.createVerticalStrut(5));

        // 添加提示文本
        JLabel hintLabel = new JLabel("Click here to create a new request");
        hintLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        hintLabel.setHorizontalAlignment(SwingConstants.CENTER);
        hintLabel.setForeground(new Color(150, 150, 150));
        hintLabel.setFont(hintLabel.getFont().deriveFont(11f));
        contentPanel.add(hintLabel);

        // 添加垂直弹性空间，使内容在垂直方向居中
        contentPanel.add(Box.createVerticalGlue());

        // 将内容面板添加到中心位置
        plusPanel.add(contentPanel, BorderLayout.CENTER);

        // 添加鼠标悬停效果
        plusPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                plusIcon.setForeground(new Color(65, 105, 225)); // 蓝色高亮
                createRequestLabel.setForeground(new Color(65, 105, 225));
                hintLabel.setForeground(new Color(100, 149, 237)); // 稍浅的蓝色
                if (plusIcon.getIcon() != null) {
                    try {
                        plusIcon.setIcon(IconFontSwing.buildIcon(FontAwesome.PLUS_CIRCLE, 32, new Color(65, 105, 225)));
                    } catch (Exception ex) {
                        // 忽略图标设置错误
                    }
                }
                plusPanel.repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                plusIcon.setForeground(new Color(100, 100, 100)); // 恢复原色
                createRequestLabel.setForeground(new Color(100, 100, 100));
                hintLabel.setForeground(new Color(150, 150, 150)); // 恢复原色
                if (plusIcon.getIcon() != null) {
                    try {
                        plusIcon.setIcon(IconFontSwing.buildIcon(FontAwesome.PLUS_CIRCLE, 32, new Color(100, 100, 100)));
                    } catch (Exception ex) {
                        // 忽略图标设置错误
                    }
                }
                plusPanel.repaint();
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e)) {
                    addNewTab(I18nUtil.getMessage("new.request"));
                }
            }
        });

        tabbedPane.addTab(PLUS_TAB, plusPanel);
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
        if (subPanel != null) subPanel.updateRequestForm(item);
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

        boolean isNewRequest = currentItem.isNewRequest();

        // 查找请求集合面板
        RequestCollectionsLeftPanel collectionPanel = SingletonFactory.getInstance(RequestCollectionsLeftPanel.class);

        if (isNewRequest) {
            // 新请求：弹出对话框让用户输入名称和选择文件夹
            saveNewRequest(collectionPanel, currentItem);
        } else {
            // 已存在的请求：弹出确认对话框
            int confirm = JOptionPane.showConfirmDialog(this,
                    I18nUtil.getMessage("update.current.request", currentItem.getName()),
                    I18nUtil.getMessage("update.request"),
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
            JOptionPane.showMessageDialog(null, I18nUtil.getMessage("please.select.group"), I18nUtil.getMessage("tip"), JOptionPane.INFORMATION_MESSAGE);
            return null;
        }
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 18, 10, 18));
        JPanel namePanel = new JPanel(new BorderLayout(8, 0));
        JLabel nameLabel = new JLabel(I18nUtil.getMessage("request.name"));
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
        JLabel groupLabel = new JLabel(I18nUtil.getMessage("select.group"));
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
        int result = JOptionPane.showConfirmDialog(null, panel, I18nUtil.getMessage("save.request"), JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result == JOptionPane.OK_OPTION) {
            String requestName = nameField.getText();
            if (requestName == null || requestName.trim().isEmpty()) {
                JOptionPane.showMessageDialog(null, I18nUtil.getMessage("please.enter.request.name"), I18nUtil.getMessage("tip"), JOptionPane.WARNING_MESSAGE);
                return null;
            }
            javax.swing.tree.TreePath selectedPath = groupTree.getSelectionPath();
            if (selectedPath == null) {
                JOptionPane.showMessageDialog(null, I18nUtil.getMessage("please.select.group"), I18nUtil.getMessage("tip"), JOptionPane.WARNING_MESSAGE);
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
                JOptionPane.showMessageDialog(null, I18nUtil.getMessage("please.select.valid.group"), I18nUtil.getMessage("tip"), JOptionPane.WARNING_MESSAGE);
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
        JOptionPane.showMessageDialog(null, I18nUtil.getMessage("request.saved"), I18nUtil.getMessage("success"), JOptionPane.INFORMATION_MESSAGE);
    }

    /**
     * 更新已存在的请求
     */
    private void updateExistingRequest(RequestCollectionsLeftPanel collectionPanel, HttpRequestItem item) {
        if (!collectionPanel.updateExistingRequest(item)) {
            log.error("更新请求失败: {}", item.getId() + " - " + item.getName());
            JOptionPane.showMessageDialog(this, I18nUtil.getMessage("update.request.failed"), I18nUtil.getMessage("error"), JOptionPane.ERROR_MESSAGE);
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

    public void updateTabNew(RequestEditSubPanel panel, boolean isNew) {
        int idx = tabbedPane.indexOfComponent(panel);
        if (idx < 0) return;
        Component tabComp = tabbedPane.getTabComponentAt(idx);
        if (tabComp instanceof ClosableTabComponent closable) {
            closable.setNewRequest(isNew);
        }
    }

    /**
     * 设置标签页切换监听器，实现反向定位到左侧树节点
     */
    private void setupTabSelectionListener() {
        tabbedPane.addChangeListener(e -> {
            // 获取当前选中的标签页
            int selectedIndex = tabbedPane.getSelectedIndex();
            if (selectedIndex < 0 || isPlusTab(selectedIndex)) {
                return; // 如果是+Tab或无效索引，不处理
            }

            Component selectedComponent = tabbedPane.getComponentAt(selectedIndex);
            if (selectedComponent instanceof RequestEditSubPanel subPanel) {
                HttpRequestItem currentRequest = subPanel.getCurrentRequest();
                if (currentRequest != null && currentRequest.getId() != null) {
                    // 在左侧树中定位到对应的请求节点
                    locateRequestInLeftTree(currentRequest.getId());
                }
            }
        });
    }

    /**
     * 在左侧集合树中定位指定ID的请求
     */
    private void locateRequestInLeftTree(String requestId) {
        if (requestId == null || requestId.isEmpty()) {
            return;
        }

        SwingUtilities.invokeLater(() -> {
            try {
                RequestCollectionsLeftPanel collectionPanel = SingletonFactory.getInstance(RequestCollectionsLeftPanel.class);
                collectionPanel.locateAndSelectRequest(requestId);
            } catch (Exception ex) {
                log.debug("定位请求节点时出错: {}", ex.getMessage());
            }
        });
    }

    @Override
    protected void initUI() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0)); // 去掉默认边框
        tabbedPane = new JTabbedPane();
        add(tabbedPane, BorderLayout.CENTER);
        setupSaveShortcut();
        setupTabSelectionListener(); // 添加标签页切换监听器
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
                        int result = JOptionPane.showConfirmDialog(null, I18nUtil.getMessage("clipboard.curl.detected"), I18nUtil.getMessage("import.curl"), JOptionPane.YES_NO_OPTION);
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
                                JOptionPane.showMessageDialog(null, I18nUtil.getMessage("parse.curl.error", ex.getMessage()), I18nUtil.getMessage("error"), JOptionPane.ERROR_MESSAGE);
                            }
                        }
                    }
                    addNewTab(I18nUtil.getMessage("new.request"));
                }
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
        JOptionPane.showMessageDialog(null, I18nUtil.getMessage("request.saved"), I18nUtil.getMessage("success"), JOptionPane.INFORMATION_MESSAGE);
        return true;
    }
}