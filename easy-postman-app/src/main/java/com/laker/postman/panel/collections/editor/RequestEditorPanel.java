package com.laker.postman.panel.collections.editor;

import com.laker.postman.collection.model.RequestGroup;
import com.laker.postman.request.model.RequestItemProtocolEnum;
import com.laker.postman.request.model.SavedResponse;
import com.laker.postman.request.model.HttpRequestItem;


import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.util.IdUtil;
import com.laker.postman.common.UiSingletonPanel;
import com.laker.postman.common.UiSingletonFactory;
import com.laker.postman.common.component.tab.ClosableTabComponent;
import com.laker.postman.common.component.tab.PlusPanel;
import com.laker.postman.common.component.tab.PlusTabComponent;
import com.laker.postman.common.component.tab.TabbedPaneDragHandler;
import com.laker.postman.frame.MainFrame;
import com.laker.postman.panel.collections.tree.CollectionGroupSelectionDialog;
import com.laker.postman.panel.collections.tree.CollectionTreePanel;
import com.laker.postman.panel.collections.editor.request.RequestEditSubPanel;
import com.laker.postman.service.collections.ActiveCollectionTreeNodeRepository;
import com.laker.postman.service.collections.CollectionTreeQueryService;
import com.laker.postman.service.setting.ShortcutManager;
import com.laker.postman.service.collections.GroupInheritanceHelper;
import com.laker.postman.service.variable.RequestExecutionContext;
import com.laker.postman.service.variable.RequestExecutionScope;
import com.laker.postman.service.curl.CurlImportUtil;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;
import com.laker.postman.util.NotificationUtil;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import javax.swing.tree.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.formdev.flatlaf.FlatClientProperties.*;
import static com.laker.postman.util.ClipboardUtil.getClipboardCurlText;

/**
 * 请求编辑面板，支持多标签页，每个标签页为独立的请求编辑子面板
 */
@Slf4j
public class RequestEditorPanel extends UiSingletonPanel {
    public static final String REQUEST_STRING = I18nUtil.getMessage(MessageKeys.NEW_REQUEST);
    public static final String PLUS_TAB = "+";
    @Getter
    private JTabbedPane tabbedPane; // 使用 JTabbedPane 管理多个请求编辑子面板

    @Getter
    private TabbedPaneDragHandler dragHandler; // Tab 拖拽排序支持

    // 普通交互下，新增/选中 tab 后会在下一轮 EDT 初始化当前编辑器。
    @Setter
    private boolean autoInitializeSelectedTabOnTabAdd = true;
    // 启动恢复多个请求时，仅最后一个 tab 自动成为选中 tab。
    @Getter
    @Setter
    private boolean startupRestoreSelectingLastTab;

    // 预览模式：单击使用的临时 tab（可被下次单击替换）
    private Component previewTab = null; // 可以是 RequestEditSubPanel 或 GroupEditPanel
    private int previewTabIndex = -1; // 预览 tab 的索引
    private final RequestEditorSaveCoordinator saveCoordinator = new RequestEditorSaveCoordinator();


    // 新建Tab，可指定标题
    public RequestEditSubPanel addNewTab(String title, RequestItemProtocolEnum protocol) {
        cancelStartupRestoreAutoSelectionIfNeeded();
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
        RequestEditorTabController.setTabNewRequest(subPanel, true); // 设置新建状态
        return subPanel;
    }

    // 新建Tab，可指定标题
    public void addNewTab(String title) {
        addNewTab(title, RequestItemProtocolEnum.HTTP);
    }

    // 添加"+"Tab
    public void addPlusTab() {
        if (tabbedPane.getTabCount() > 0 && isPlusTab(tabbedPane.getTabCount() - 1)) {
            if (autoInitializeSelectedTabOnTabAdd) {
                initializeSelectedTabSoon();
            }
            return;
        }
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
        cancelStartupRestoreAutoSelectionIfNeeded();
        String id = item.getId();
        if (id == null || id.isEmpty()) {
            addNewTab(null);
            updateRequest(item);
            return;
        }

        // 设置全局请求上下文（供分组变量使用）
        ActiveCollectionTreeNodeRepository repository = new ActiveCollectionTreeNodeRepository();
        repository.getRootNode().ifPresent(rootNode -> {
            DefaultMutableTreeNode requestNode = CollectionTreeQueryService.findRequestNodeById(rootNode, id);
            if (requestNode != null) {
                RequestExecutionContext.setCurrentScope(RequestExecutionScope.fromVariables(
                        GroupInheritanceHelper.getMergedGroupVariables(requestNode)
                ));
            }
        });

        // 1. 先查找是否已有固定的 tab（不包括预览 tab）
        if (switchToExistingRequestTab(id)) {
            return;
        }

        // 2. 验证并清理无效的预览 tab
        validatePreviewTab();

        // 3. 复用或创建预览 tab
        String name = CharSequenceUtil.isNotBlank(item.getName()) ? item.getName() : REQUEST_STRING;
        RequestEditSubPanel newPanel = new RequestEditSubPanel(id, item.getProtocol());
        newPanel.initPanelData(item);

        showOrUpdatePreviewTab(newPanel, name, item.getProtocol());
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
        cancelStartupRestoreAutoSelectionIfNeeded();
        String groupId = group.getId();

        // 1. 先查找是否已有固定的 Group tab（不包括预览 tab）
        if (switchToExistingGroupTab(groupId)) {
            return;
        }

        // 2. 验证并清理无效的预览 tab
        validatePreviewTab();

        // 3. 复用或创建预览 tab
        String groupName = group.getName();
        GroupEditPanel groupEditPanel = new GroupEditPanel(groupNode, group, () -> {
            CollectionTreePanel leftPanel = UiSingletonFactory.getInstance(CollectionTreePanel.class);
            leftPanel.getTreeModel().nodeChanged(groupNode);
            leftPanel.getPersistence().saveRequestGroups();
        });

        boolean isRoot = groupNode.getLevel() == 1;
        showOrUpdatePreviewTab(groupEditPanel, groupName, null, isRoot);
    }

    // showOrCreateTab 需适配 "+" Tab（双击时调用，创建固定 tab）
    public void showOrCreateTab(HttpRequestItem item) {
        cancelStartupRestoreAutoSelectionIfNeeded();
        String id = item.getId();
        if (id == null || id.isEmpty()) {
            addNewTab(null);
            updateRequest(item);
            return;
        }

        // 设置全局请求上下文（供分组变量使用）
        ActiveCollectionTreeNodeRepository repository = new ActiveCollectionTreeNodeRepository();
        repository.getRootNode().ifPresent(rootNode -> {
            DefaultMutableTreeNode requestNode = CollectionTreeQueryService.findRequestNodeById(rootNode, id);
            if (requestNode != null) {
                RequestExecutionContext.setCurrentScope(RequestExecutionScope.fromVariables(
                        GroupInheritanceHelper.getMergedGroupVariables(requestNode)
                ));
            }
        });

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
    private static final String ACTION_SEND_REQUEST = "sendRequest";
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

        // 发送请求快捷键 (Cmd+Enter / Ctrl+Enter)
        KeyStroke sendKey = ShortcutManager.getKeyStroke(ShortcutManager.SEND_REQUEST);
        if (sendKey != null) {
            inputMap.put(sendKey, ACTION_SEND_REQUEST);
            actionMap.put(ACTION_SEND_REQUEST, new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    // 获取当前活动的 SubPanel 并触发发送按钮点击
                    RequestEditSubPanel currentSubPanel = getCurrentSubPanel();
                    if (currentSubPanel != null && currentSubPanel.getRequestLinePanel() != null) {
                        JButton sendButton = currentSubPanel.getRequestLinePanel().getSendButton();
                        if (sendButton != null && sendButton.isEnabled()) {
                            // 先将焦点转移到 sendButton，触发 table cell editor 的
                            // terminateEditOnFocusLost 机制，确保正在编辑的 cell
                            // 值提交到 model，然后再执行发送逻辑。
                            sendButton.requestFocusInWindow();
                            SwingUtilities.invokeLater(sendButton::doClick);
                        }
                    }
                }
            });
        }

        // 保存请求快捷键 (Cmd+S / Ctrl+S)
        KeyStroke saveKey = ShortcutManager.getKeyStroke(ShortcutManager.SAVE_REQUEST);
        if (saveKey != null) {
            inputMap.put(saveKey, ACTION_SAVE_REQUEST);
            actionMap.put(ACTION_SAVE_REQUEST, new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    // 获取当前活动的 SubPanel 并触发保存按钮点击（保持与发送按钮一致）
                    RequestEditSubPanel currentSubPanel = getCurrentSubPanel();
                    if (currentSubPanel != null && currentSubPanel.getRequestLinePanel() != null) {
                        JButton saveButton = currentSubPanel.getRequestLinePanel().getSaveButton();
                        if (saveButton != null && saveButton.isEnabled()) {
                            // 先将焦点转移到 saveButton，触发 table cell editor 的
                            // terminateEditOnFocusLost 机制，确保正在编辑的 cell
                            // 值提交到 model，然后再执行点击保存逻辑。
                            saveButton.requestFocusInWindow();
                            SwingUtilities.invokeLater(saveButton::doClick);
                        }
                    }
                }
            });
        }

        // 新建标签页快捷键
        KeyStroke newTabKey = ShortcutManager.getKeyStroke(ShortcutManager.NEW_REQUEST);
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
        KeyStroke closeCurrentKey = ShortcutManager.getKeyStroke(ShortcutManager.CLOSE_CURRENT_TAB);
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
        KeyStroke closeOthersKey = ShortcutManager.getKeyStroke(ShortcutManager.CLOSE_OTHER_TABS);
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
        KeyStroke closeAllKey = ShortcutManager.getKeyStroke(ShortcutManager.CLOSE_ALL_TABS);
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
    public boolean saveCurrentRequest() {
        RequestEditSubPanel currentSubPanel = getCurrentSubPanel();
        CollectionTreePanel collectionPanel = UiSingletonFactory.getInstance(CollectionTreePanel.class);
        return saveCoordinator.saveCurrentRequest(new RequestEditorSaveCoordinator.SaveContext() {
            @Override
            public boolean isSavedResponseTab() {
                return currentSubPanel != null && currentSubPanel.isSavedResponseTab();
            }

            @Override
            public void showSavedResponseReadonly() {
                NotificationUtil.showInfo(I18nUtil.getMessage(MessageKeys.SAVED_RESPONSE_READONLY));
            }

            @Override
            public void promotePreviewTabToPermanent() {
                RequestEditorPanel.this.promotePreviewTabToPermanent();
            }

            @Override
            public String validateRequestSettings() {
                return currentSubPanel != null ? currentSubPanel.validateRequestSettings() : null;
            }

            @Override
            public void showSettingsValidationError(String error) {
                NotificationUtil.showError(error);
            }

            @Override
            public HttpRequestItem currentRequest() {
                return getCurrentRequest();
            }

            @Override
            public void onNoRequestToSave() {
                log.warn("没有可保存的请求");
            }

            @Override
            public TreeModel groupTreeModel() {
                return collectionPanel.getGroupTreeModel();
            }

            @Override
            public Optional<CollectionGroupSelectionDialog.RequestNameSelection> chooseGroupAndRequestName(
                    TreeModel groupTreeModel,
                    String defaultName
            ) {
                return RequestEditorPanel.this.chooseGroupAndRequestName(groupTreeModel, defaultName);
            }

            @Override
            public String newRequestId() {
                return IdUtil.simpleUUID();
            }

            @Override
            public void saveRequestToGroup(RequestGroup group, HttpRequestItem item) {
                collectionPanel.saveRequestToGroup(group, item);
            }

            @Override
            public void refreshNewRequestTab(String requestName, HttpRequestItem item) {
                RequestEditorPanel.this.refreshNewRequestTab(requestName, item);
            }

            @Override
            public boolean updateExistingRequest(HttpRequestItem item) {
                return collectionPanel.updateExistingRequest(item);
            }

            @Override
            public void showUpdateExistingRequestFailed(HttpRequestItem item) {
                log.error("更新请求失败: {}", item.getId() + " - " + item.getName());
                JOptionPane.showMessageDialog(
                        RequestEditorPanel.this,
                        I18nUtil.getMessage(MessageKeys.UPDATE_REQUEST_FAILED),
                        I18nUtil.getMessage(MessageKeys.ERROR),
                        JOptionPane.ERROR_MESSAGE
                );
            }
        });
    }

    private void refreshNewRequestTab(String requestName, HttpRequestItem item) {
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

    protected Optional<CollectionGroupSelectionDialog.RequestNameSelection> chooseGroupAndRequestName(
            TreeModel groupTreeModel,
            String defaultName
    ) {
        return CollectionGroupSelectionDialog.chooseGroupAndRequestName(groupTreeModel, defaultName);
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

    public void updateTabProtocol(RequestEditSubPanel panel, RequestItemProtocolEnum protocol) {
        int idx = tabbedPane.indexOfComponent(panel);
        if (idx < 0 || protocol == null) {
            return;
        }
        Component tabComp = tabbedPane.getTabComponentAt(idx);
        if (!(tabComp instanceof ClosableTabComponent closable)) {
            return;
        }
        String title = tabbedPane.getTitleAt(idx);
        ClosableTabComponent updated = new ClosableTabComponent(title, protocol);
        updated.setDirty(closable.isDirty());
        updated.setNewRequest(closable.isNewRequest());
        updated.setPreviewMode(closable.isPreviewMode());
        tabbedPane.setTabComponentAt(idx, updated);
    }

    /**
     * 更新 GroupEditPanel 的 Tab 标题
     */
    public void updateGroupTabTitle(GroupEditPanel panel, String newTitle) {
        int idx = tabbedPane.indexOfComponent(panel);
        if (idx < 0) return;

        // 重新创建 Tab 组件以更新标题
        boolean isRoot = panel.getGroupNode() != null && panel.getGroupNode().getLevel() == 1;
        tabbedPane.setTabComponentAt(idx, new ClosableTabComponent(newTitle, null, isRoot));
        tabbedPane.setToolTipTextAt(idx, newTitle);
    }

    @Override
    protected void initUI() {
        setLayout(new BorderLayout());
        // 设置tabbedPane为单行滚动模式，防止多行tab顺序混乱
        tabbedPane = new JTabbedPane(SwingConstants.TOP, JTabbedPane.SCROLL_TAB_LAYOUT);
        // 设置整个tabbedPane区域的内边距
        tabbedPane.putClientProperty(TABBED_PANE_TAB_AREA_INSETS, new Insets(0, 0, 0, 5));
        // 设置tabbedPane中一个个头部标签的的内边距（上、左、下、右）
        tabbedPane.putClientProperty(TABBED_PANE_TAB_INSETS, new Insets(3, 5, 3, 5));
        tabbedPane.putClientProperty(TABBED_PANE_TAB_HEIGHT, 38); // 设置tab高度，配合内边距让tab更美观
        tabbedPane.addContainerListener(new ContainerAdapter() {
            @Override
            public void componentAdded(ContainerEvent e) {
                if (tabbedPane.getTabCount() > 0 && autoInitializeSelectedTabOnTabAdd) {
                    initializeSelectedTabSoon();
                }
            }
        });
        tabbedPane.addChangeListener(e -> initializeSelectedTabSoon());

        add(tabbedPane, BorderLayout.CENTER);

        // 安装 Tab 拖拽排序支持（IDEA 风格蓝色竖线指示）
        // 通过 getter/setter 传入 previewTabIndex，确保拖拽移动后索引正确同步
        dragHandler = TabbedPaneDragHandler.install(tabbedPane,
                () -> previewTabIndex,
                v -> previewTabIndex = v);
    }

    /**
     * 在下一轮 EDT 初始化当前选中的请求编辑器，保持切换直接且不额外引入定时器。
     */
    public void initializeSelectedTabSoon() {
        if (startupRestoreSelectingLastTab) {
            return;
        }
        scheduleSelectedRequestTabInitialization(false);
    }

    private void ensureSelectedRequestTabInitialized(boolean animatePlaceholderTransition) {
        Component selectedComponent = tabbedPane.getSelectedComponent();
        if (selectedComponent instanceof RequestEditSubPanel requestEditSubPanel && !requestEditSubPanel.isEditorInitialized()) {
            requestEditSubPanel.ensureEditorInitialized(animatePlaceholderTransition);
        }
    }

    private void scheduleSelectedRequestTabInitialization(boolean animatePlaceholderTransition) {
        SwingUtilities.invokeLater(() -> {
            if (startupRestoreSelectingLastTab) {
                return;
            }
            ensureSelectedRequestTabInitialized(animatePlaceholderTransition);
        });
    }

    public void initializeSelectedStartupRestoreTab() {
        scheduleSelectedRequestTabInitialization(true);
    }

    public void warmUpDeferredRequestTabsAfterStartup() {
        SwingUtilities.invokeLater(() -> warmUpDeferredRequestTabsSequentially(0));
    }

    private void warmUpDeferredRequestTabsSequentially(int startIndex) {
        for (int i = Math.max(0, startIndex); i < tabbedPane.getTabCount(); i++) {
            if (i == tabbedPane.getSelectedIndex()) {
                continue;
            }
            Component component = tabbedPane.getComponentAt(i);
            if (!(component instanceof RequestEditSubPanel requestEditSubPanel) || requestEditSubPanel.isEditorInitialized()) {
                continue;
            }
            requestEditSubPanel.ensureEditorInitialized(false);
            int nextIndex = i + 1;
            SwingUtilities.invokeLater(() -> warmUpDeferredRequestTabsSequentially(nextIndex));
            return;
        }
    }

    private void cancelStartupRestoreAutoSelectionIfNeeded() {
        if (!startupRestoreSelectingLastTab) {
            return;
        }
        startupRestoreSelectingLastTab = false;
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
                        int result = JOptionPane.showConfirmDialog(UiSingletonFactory.getInstance(RequestEditorPanel.class),
                                I18nUtil.getMessage(MessageKeys.CLIPBOARD_CURL_DETECTED),
                                I18nUtil.getMessage(MessageKeys.IMPORT_CURL), JOptionPane.YES_NO_OPTION);
                        if (result == JOptionPane.YES_OPTION) {
                            try {
                                HttpRequestItem item = CurlImportUtil.fromCurl(curlText);
                                if (item != null) {
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
     * 显示分组编辑面板（参考 Postman，不使用弹窗）
     * 双击时调用，创建固定 tab
     */
    public void showGroupEditPanel(DefaultMutableTreeNode groupNode, RequestGroup group) {
        cancelStartupRestoreAutoSelectionIfNeeded();
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
            CollectionTreePanel leftPanel = UiSingletonFactory.getInstance(CollectionTreePanel.class);
            leftPanel.getTreeModel().nodeChanged(groupNode);
            leftPanel.getPersistence().saveRequestGroups();
        });

        // 添加为新 Tab
        String groupName = group.getName();
        boolean isRoot = groupNode.getLevel() == 1;
        tabbedPane.addTab(groupName, groupEditPanel);
        tabbedPane.setTabComponentAt(tabbedPane.getTabCount() - 1, new ClosableTabComponent(groupName, null, isRoot));
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
            int result = JOptionPane.showConfirmDialog(UiSingletonFactory.getInstance(MainFrame.class),
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
        removeTabAtWithCleanup(currentIndex);

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
                int result = JOptionPane.showConfirmDialog(UiSingletonFactory.getInstance(MainFrame.class),
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
            removeTabComponentWithCleanup(comp);
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
                int result = JOptionPane.showConfirmDialog(UiSingletonFactory.getInstance(MainFrame.class),
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
            removeTabComponentWithCleanup(comp);
        }
    }

    /**
     * 单击保存的响应：在预览 Tab 中显示
     */
    public void showOrCreatePreviewTabForSavedResponse(SavedResponse savedResponse) {
        cancelStartupRestoreAutoSelectionIfNeeded();
        if (savedResponse == null) {
            return;
        }

        String savedResponseId = savedResponse.getId();

        if (savedResponseId == null || savedResponseId.isEmpty()) {
            return;
        }

        if (switchToExistingRequestTab(savedResponseId)) {
            return;
        }

        // 2. 验证并清理无效的预览 tab
        validatePreviewTab();

        // 3. 创建新 panel
        String name = savedResponse.getName();
        RequestEditSubPanel newPanel = new RequestEditSubPanel(savedResponse);
        newPanel.loadSavedResponse(savedResponse);

        showOrUpdatePreviewTab(newPanel, name, RequestItemProtocolEnum.SAVED_RESPONSE);
    }

    /**
     * 双击保存的响应：在固定 Tab 中显示
     */
    public void showOrCreateTabForSavedResponse(SavedResponse savedResponse) {
        cancelStartupRestoreAutoSelectionIfNeeded();
        String savedResponseId = savedResponse.getId();

        if (savedResponseId == null || savedResponseId.isEmpty()) {
            return;
        }

        // 1. 如果当前预览的就是这个 savedResponse，则将预览 tab 转为固定 tab
        if (previewTab instanceof RequestEditSubPanel subPanel && savedResponseId.equals(subPanel.getId())) {
            promotePreviewTabToPermanent();
            return;
        }

        // 2. 查找同样 savedResponse 的固定 Tab（不查"+"Tab）
        // 使用统一的 switchToExistingRequestTab 方法
        if (switchToExistingRequestTab(savedResponseId)) {
            return;
        }

        // 3. 创建新的固定 tab
        String name = savedResponse.getName();
        RequestEditSubPanel newPanel = new RequestEditSubPanel(savedResponse);
        newPanel.loadSavedResponse(savedResponse);

        int plusTabIdx = tabbedPane.getTabCount() > 0 ? tabbedPane.getTabCount() - 1 : 0; // 插入到"+"Tab前
        tabbedPane.insertTab(name, null, newPanel, null, plusTabIdx);
        ClosableTabComponent tabComponent = new ClosableTabComponent(name, RequestItemProtocolEnum.SAVED_RESPONSE);
        tabbedPane.setTabComponentAt(plusTabIdx, tabComponent);
        tabbedPane.setSelectedIndex(plusTabIdx);
    }

    // ==================== Helper Methods ====================

    /**
     * 验证预览 tab 是否有效，如果无效则清理
     */
    private void validatePreviewTab() {
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
    }

    private void showOrUpdatePreviewTab(Component panel, String name, RequestItemProtocolEnum protocol) {
        showOrUpdatePreviewTab(panel, name, protocol, false);
    }

    /**
     * 显示或更新预览 tab
     *
     * @param panel    要显示的面板
     * @param name     tab 名称
     * @param protocol 协议类型
     * @param isRoot   是否为根文件夹（collection）
     */
    private void showOrUpdatePreviewTab(Component panel, String name, RequestItemProtocolEnum protocol, boolean isRoot) {
        if (previewTab != null && previewTabIndex >= 0) {
            cleanupTabComponent(previewTab);
            previewTab = panel;
            tabbedPane.setComponentAt(previewTabIndex, previewTab);
            tabbedPane.setTitleAt(previewTabIndex, name);
            ClosableTabComponent tabComponent = new ClosableTabComponent(name, protocol, isRoot);
            tabComponent.setPreviewMode(true);
            tabbedPane.setTabComponentAt(previewTabIndex, tabComponent);
            tabbedPane.setSelectedIndex(previewTabIndex);
        } else {
            // 创建新的预览 tab
            int plusTabIdx = tabbedPane.getTabCount() > 0 ? tabbedPane.getTabCount() - 1 : 0;
            if (isPlusTab(plusTabIdx)) {
                tabbedPane.removeTabAt(plusTabIdx);
            }
            previewTab = panel;
            tabbedPane.addTab(name, previewTab);
            previewTabIndex = tabbedPane.getTabCount() - 1;
            ClosableTabComponent tabComponent = new ClosableTabComponent(name, protocol, isRoot);
            tabComponent.setPreviewMode(true);
            tabbedPane.setTabComponentAt(previewTabIndex, tabComponent);
            tabbedPane.setSelectedIndex(previewTabIndex);
            addPlusTab();
        }
    }

    public void removeTabAtWithCleanup(int index) {
        if (index < 0 || index >= tabbedPane.getTabCount()) {
            return;
        }

        Component component = tabbedPane.getComponentAt(index);
        cleanupTabComponent(component);
        if (component == previewTab) {
            previewTab = null;
            previewTabIndex = -1;
        } else if (previewTabIndex > index) {
            previewTabIndex--;
        }
        tabbedPane.removeTabAt(index);
    }

    public void removeTabComponentWithCleanup(Component component) {
        int index = tabbedPane.indexOfComponent(component);
        if (index >= 0) {
            removeTabAtWithCleanup(index);
        }
    }

    private void cleanupTabComponent(Component component) {
        if (component instanceof RequestEditSubPanel requestEditSubPanel) {
            requestEditSubPanel.disposeResources();
        }
    }

    /**
     * 切换到已存在的 Request 或 SavedResponse tab
     *
     * @param id 请求ID 或 SavedResponse ID
     * @return 如果找到并切换成功返回true，否则返回false
     */
    private boolean switchToExistingRequestTab(String id) {
        for (int i = 0; i < tabbedPane.getTabCount() - 1; i++) {
            if (i != previewTabIndex) {
                Component comp = tabbedPane.getComponentAt(i);
                if (comp instanceof RequestEditSubPanel subPanel) {
                    if (id.equals(subPanel.getId())) {
                        tabbedPane.setSelectedIndex(i);
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * 切换到已存在的 Group tab
     *
     * @param groupId 分组ID
     * @return 如果找到并切换成功返回true，否则返回false
     */
    private boolean switchToExistingGroupTab(String groupId) {
        if (groupId != null && !groupId.isEmpty()) {
            for (int i = 0; i < tabbedPane.getTabCount() - 1; i++) {
                if (i != previewTabIndex) {
                    Component comp = tabbedPane.getComponentAt(i);
                    if (comp instanceof GroupEditPanel existingPanel && groupId.equals(existingPanel.getGroup().getId())) {
                        tabbedPane.setSelectedIndex(i);
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * 更新所有打开标签页的布局方向
     *
     * @param isVertical true=垂直布局，false=水平布局
     */
    public void updateAllTabsLayout(boolean isVertical) {
        // 遍历所有标签页（排除 "+" 标签）
        int tabCount = tabbedPane.getTabCount();
        for (int i = 0; i < tabCount; i++) {
            Component comp = tabbedPane.getComponentAt(i);
            if (comp instanceof RequestEditSubPanel subPanel) {
                subPanel.updateLayoutOrientation(isVertical);
            }
        }
    }

    public void updateAllRequestEditorTabsVisibility() {
        int tabCount = tabbedPane.getTabCount();
        for (int i = 0; i < tabCount; i++) {
            Component comp = tabbedPane.getComponentAt(i);
            if (comp instanceof RequestEditSubPanel subPanel) {
                subPanel.updateRequestEditorTabsVisibility();
            }
        }
    }

}
