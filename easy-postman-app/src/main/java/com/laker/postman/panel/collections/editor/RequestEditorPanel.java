package com.laker.postman.panel.collections.editor;

import com.laker.postman.common.UiSingletonPanel;
import com.laker.postman.common.component.tab.TabbedPaneDragHandler;
import com.laker.postman.collection.model.RequestGroup;
import com.laker.postman.panel.collections.editor.request.RequestEditSubPanel;
import com.laker.postman.panel.collections.tree.CollectionGroupSelectionDialog;
import com.laker.postman.request.model.HttpRequestItem;
import com.laker.postman.request.model.RequestItemProtocolEnum;
import com.laker.postman.request.model.SavedResponse;
import com.laker.postman.service.collections.RequestSaveEventPublisher;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;
import lombok.Getter;
import lombok.Setter;

import javax.swing.*;
import javax.swing.tree.*;
import java.awt.*;
import java.awt.event.ContainerAdapter;
import java.awt.event.ContainerEvent;
import java.util.Optional;

import static com.formdev.flatlaf.FlatClientProperties.*;

/**
 * 请求编辑面板，支持多标签页，每个标签页为独立的请求编辑子面板
 */
public class RequestEditorPanel extends UiSingletonPanel {
    private static final String REQUEST_STRING = I18nUtil.getMessage(MessageKeys.NEW_REQUEST);
    static final String PLUS_TAB = "+";
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

    private RequestEditorPreviewTabManager previewTabManager;
    private RequestEditorSaveController saveController;
    private RequestEditorShortcutInstaller shortcutInstaller;
    private RequestEditorTabOpenController tabOpenController;
    private RequestEditorTabCloseController tabCloseController;
    private RequestEditorTabStateController tabStateController;
    private RequestEditorTabLifecycleController tabLifecycleController;
    private RequestEditorTabRemovalController tabRemovalController;
    private RequestEditorTabInitializationScheduler tabInitializationScheduler;
    private final CollectionTreeEditorGateway collectionTreeGateway = new CollectionTreeEditorGateway();


    // 新建Tab，可指定标题
    public RequestEditSubPanel addNewTab(String title, RequestItemProtocolEnum protocol) {
        cancelStartupRestoreAutoSelectionIfNeeded();
        return tabLifecycleController.addNewRequestTab(title, protocol);
    }

    // 新建Tab，可指定标题
    public void addNewTab(String title) {
        addNewTab(title, RequestItemProtocolEnum.HTTP);
    }

    // 添加"+"Tab
    public void addPlusTab() {
        tabLifecycleController.addPlusTab();
    }

    // 判断是否为“+”Tab
    private boolean isPlusTab(int idx) {
        return tabLifecycleController.isPlusTab(idx);
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
        tabOpenController.showOrCreateRequestPreview(item);
    }

    /**
     * 将预览 tab 转为固定 tab
     */
    public void promotePreviewTabToPermanent() {
        previewTabManager.promoteToPermanent();
    }

    /**
     * 显示或创建 Group 的预览 tab（用于单击 Group 时）
     */
    public void showOrCreatePreviewTabForGroup(DefaultMutableTreeNode groupNode, RequestGroup group) {
        tabOpenController.showOrCreateGroupPreview(groupNode, group);
    }

    // showOrCreateTab 需适配 "+" Tab（双击时调用，创建固定 tab）
    public void showOrCreateTab(HttpRequestItem item) {
        tabOpenController.showOrCreateRequestTab(item);
    }

    /**
     * 重新加载快捷键（快捷键设置修改后调用）
     */
    public void reloadShortcuts() {
        shortcutInstaller.install();
    }

    /**
     * 保存当前请求
     */
    public boolean saveCurrentRequest() {
        return saveController.saveCurrentRequest();
    }

    protected Optional<CollectionGroupSelectionDialog.RequestNameSelection> chooseGroupAndRequestName(
            TreeModel groupTreeModel,
            String defaultName
    ) {
        return CollectionGroupSelectionDialog.chooseGroupAndRequestName(groupTreeModel, defaultName);
    }

    // 用于动态更新tab红点
    public void updateTabDirty(RequestEditSubPanel panel, boolean dirty) {
        tabStateController.updateRequestDirty(panel, dirty);
    }

    public void updateTabProtocol(RequestEditSubPanel panel, RequestItemProtocolEnum protocol) {
        tabStateController.updateRequestProtocol(panel, protocol);
    }

    /**
     * 更新 GroupEditPanel 的 Tab 标题
     */
    void updateGroupTabTitle(GroupEditPanel panel, String newTitle) {
        tabStateController.updateGroupTitle(panel, newTitle);
    }

    @Override
    protected void initUI() {
        setLayout(new BorderLayout());
        tabbedPane = createRequestTabbedPane();
        tabLifecycleController = new RequestEditorTabLifecycleController(
                tabbedPane,
                PLUS_TAB,
                REQUEST_STRING,
                () -> autoInitializeSelectedTabOnTabAdd,
                this::initializeSelectedTabSoon);
        installDeferredTabInitialization();
        add(tabbedPane, BorderLayout.CENTER);
        installPreviewAndDragSupport();
        createTabControllers();
    }

    private JTabbedPane createRequestTabbedPane() {
        JTabbedPane tabs = new JTabbedPane(SwingConstants.TOP, JTabbedPane.SCROLL_TAB_LAYOUT);
        tabs.putClientProperty(TABBED_PANE_TAB_AREA_INSETS, new Insets(0, 0, 0, 5));
        tabs.putClientProperty(TABBED_PANE_TAB_INSETS, new Insets(3, 5, 3, 5));
        tabs.putClientProperty(TABBED_PANE_TAB_HEIGHT, 38);
        return tabs;
    }

    private void installDeferredTabInitialization() {
        tabInitializationScheduler = new RequestEditorTabInitializationScheduler(tabbedPane, () -> startupRestoreSelectingLastTab);
        tabbedPane.addContainerListener(new ContainerAdapter() {
            @Override
            public void componentAdded(ContainerEvent e) {
                if (tabbedPane.getTabCount() > 0 && autoInitializeSelectedTabOnTabAdd) {
                    tabInitializationScheduler.initializeSelectedTabSoon();
                }
            }
        });
        tabbedPane.addChangeListener(e -> tabInitializationScheduler.initializeSelectedTabSoon());
    }

    private void installPreviewAndDragSupport() {
        previewTabManager = new RequestEditorPreviewTabManager(
                tabbedPane,
                this::isPlusTab,
                this::addPlusTab,
                RequestEditorTabResourceCleaner::cleanup);

        dragHandler = TabbedPaneDragHandler.install(tabbedPane,
                previewTabManager::previewTabIndex,
                previewTabManager::setPreviewTabIndex);
    }

    private void createTabControllers() {
        tabStateController = new RequestEditorTabStateController(tabbedPane);
        tabRemovalController = new RequestEditorTabRemovalController(tabbedPane, previewTabManager);
        saveController = new RequestEditorSaveController(
                this,
                tabStateController::currentRequestTab,
                this::promotePreviewTabToPermanent,
                this::chooseGroupAndRequestName,
                tabStateController::refreshNewRequestTab,
                collectionTreeGateway);
        tabCloseController = new RequestEditorTabCloseController(
                tabbedPane,
                this::isPlusTab,
                this::saveCurrentRequest,
                tabRemovalController::removeAt,
                tabRemovalController::removeComponent);
        tabOpenController = new RequestEditorTabOpenController(
                tabbedPane,
                previewTabManager,
                new RequestEditorExecutionScopeSynchronizer(),
                this::cancelStartupRestoreAutoSelectionIfNeeded,
                this::addPlusTab,
                this::isPlusTab,
                this::addNewTab,
                (item, tab) -> tab.initPanelData(item),
                REQUEST_STRING,
                collectionTreeGateway);
        shortcutInstaller = createShortcutInstaller();
    }

    /**
     * 在下一轮 EDT 初始化当前选中的请求编辑器，保持切换直接且不额外引入定时器。
     */
    void initializeSelectedTabSoon() {
        tabInitializationScheduler.initializeSelectedTabSoon();
    }

    public void initializeSelectedStartupRestoreTab() {
        tabInitializationScheduler.initializeSelectedStartupRestoreTab();
    }

    public void warmUpDeferredRequestTabsAfterStartup() {
        tabInitializationScheduler.warmUpDeferredRequestTabsAfterStartup();
    }

    private void cancelStartupRestoreAutoSelectionIfNeeded() {
        if (!startupRestoreSelectingLastTab) {
            return;
        }
        startupRestoreSelectingLastTab = false;
    }

    @Override
    protected void registerListeners() {
        RequestSaveEventPublisher.register(tabStateController::syncSavedRequest);
        reloadShortcuts();
        new RequestEditorPlusTabController(tabbedPane, this, PLUS_TAB, REQUEST_STRING, this::addNewTab).install();
    }

    private RequestEditorShortcutInstaller createShortcutInstaller() {
        return new RequestEditorShortcutInstaller(
                this,
                tabStateController::currentRequestTab,
                () -> addNewTab(null),
                this::closeCurrentTab,
                this::closeOtherTabs,
                this::closeAllTabs);
    }

    /**
     * 显示分组编辑面板（参考 Postman，不使用弹窗）
     * 双击时调用，创建固定 tab
     */
    public void showGroupEditPanel(DefaultMutableTreeNode groupNode, RequestGroup group) {
        tabOpenController.showOrCreateGroupTab(groupNode, group);
    }

    /**
     * 关闭当前标签页
     */
    public void closeCurrentTab() {
        tabCloseController.closeCurrentTab();
    }

    /**
     * 关闭其他标签页
     */
    public void closeOtherTabs() {
        tabCloseController.closeOtherTabs();
    }

    /**
     * 关闭所有标签页
     */
    public void closeAllTabs() {
        tabCloseController.closeAllTabs();
    }

    /**
     * 单击保存的响应：在预览 Tab 中显示
     */
    public void showOrCreatePreviewTabForSavedResponse(SavedResponse savedResponse) {
        tabOpenController.showOrCreateSavedResponsePreview(savedResponse);
    }

    /**
     * 双击保存的响应：在固定 Tab 中显示
     */
    public void showOrCreateTabForSavedResponse(SavedResponse savedResponse) {
        tabOpenController.showOrCreateSavedResponseTab(savedResponse);
    }

    // ==================== Helper Methods ====================

    public void removeTabAtWithCleanup(int index) {
        tabRemovalController.removeAt(index);
    }

    /**
     * 更新所有打开标签页的布局方向
     *
     * @param isVertical true=垂直布局，false=水平布局
     */
    public void updateAllTabsLayout(boolean isVertical) {
        tabStateController.updateAllRequestLayouts(isVertical);
    }

    public void updateAllRequestEditorTabsVisibility() {
        tabStateController.updateAllRequestEditorTabsVisibility();
    }

}
