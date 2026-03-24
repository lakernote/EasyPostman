package com.laker.postman.service.collections;

import com.laker.postman.common.SingletonFactory;
import com.laker.postman.frame.MainFrame;
import com.laker.postman.model.HttpRequestItem;
import com.laker.postman.panel.collections.left.RequestCollectionsLeftPanel;
import com.laker.postman.panel.collections.right.request.RequestEditSubPanel;
import com.laker.postman.startup.StartupDiagnostics;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static com.laker.postman.panel.collections.left.RequestCollectionsLeftPanel.REQUEST;

@Slf4j
@UtilityClass
public class RequestCollectionsService {
    static final int RESTORE_BATCH_SIZE = 1;
    static final int RESTORE_DELAY_MS = 25;
    public static final int STARTUP_RESTORE_INITIAL_DELAY_MS = 180;


    public static HttpRequestItem getLastNonNewRequest() {
        return getLastNonNewRequest(OpenedRequestsService.getAll());
    }

    static HttpRequestItem getLastNonNewRequest(List<HttpRequestItem> requestItems) {
        if (requestItems == null || requestItems.isEmpty()) {
            return null;
        }
        for (int i = requestItems.size() - 1; i >= 0; i--) {
            HttpRequestItem item = requestItems.get(i);
            if (!item.isNewRequest()) {
                return item;
            }
        }
        return null;
    }

    public static void restoreOpenedRequests() {
        restoreOpenedRequestsIncrementally(OpenedRequestsService.getAll(), 0, null);
    }

    public static void restoreOpenedRequestsIncrementally(List<HttpRequestItem> requestItems, Runnable onComplete) {
        restoreOpenedRequestsIncrementally(requestItems, 0, onComplete);
    }

    public static void restoreOpenedRequestsIncrementally(List<HttpRequestItem> requestItems,
                                                          int initialDelayMs,
                                                          Runnable onComplete) {
        Runnable restoreTask = () -> {
            RequestCollectionsLeftPanel leftPanel = SingletonFactory.getInstance(RequestCollectionsLeftPanel.class);
            List<HttpRequestItem> restorableRequests = buildRestorableOpenedRequests(requestItems, leftPanel.getRootTreeNode());
            StartupDiagnostics.mark("Incremental restore prepared; validRequests=" + restorableRequests.size());
            restoreOpenedRequestsIncrementally(
                    restorableRequests,
                    RESTORE_BATCH_SIZE,
                    RESTORE_DELAY_MS,
                    initialDelayMs,
                    (item, selectTab) -> {
                        RequestEditSubPanel panel = RequestsTabsService.addTab(item, selectTab, true);
                        RequestsTabsService.updateTabNew(panel, item.isNewRequest());
                    },
                    () -> {
                        OpenedRequestsService.clear();
                        if (onComplete != null) {
                            onComplete.run();
                        }
                    }
            );
        };

        if (SwingUtilities.isEventDispatchThread()) {
            restoreTask.run();
        } else {
            SwingUtilities.invokeLater(restoreTask);
        }
    }

    static List<HttpRequestItem> buildRestorableOpenedRequests(List<HttpRequestItem> requestItems,
                                                               DefaultMutableTreeNode rootNode) {
        if (requestItems == null || requestItems.isEmpty()) {
            return List.of();
        }

        List<HttpRequestItem> restorableItems = new ArrayList<>();
        for (HttpRequestItem item : requestItems) {
            // 验证请求对象和ID是否有效
            if (item == null || item.getId() == null || item.getId().isEmpty()) {
                log.warn("Skip restoring request as it is null or has null/empty ID: {}",
                        item != null ? item.getName() : "null");
                continue;
            }

            HttpRequestItem resolvedItem = item;
            // 如果不是新请求，从tree中找到完整数据
            if (!item.isNewRequest()) {
                DefaultMutableTreeNode node = findRequestNodeById(rootNode, item.getId());
                if (node == null) {
                    // 请求在左侧树中已不存在，跳过恢复
                    log.warn("Skip restoring request {} (id={}) as it no longer exists in the tree",
                            item.getName(), item.getId());
                    continue;
                }
                // 从tree节点中获取完整的请求数据
                Object[] userObj = (Object[]) node.getUserObject();
                resolvedItem = (HttpRequestItem) userObj[1];
            }
            restorableItems.add(resolvedItem);
        }
        return restorableItems;
    }

    static Timer restoreOpenedRequestsIncrementally(List<HttpRequestItem> requestItems,
                                                    int batchSize,
                                                    int delayMs,
                                                    int initialDelayMs,
                                                    BiConsumer<HttpRequestItem, Boolean> restoreAction,
                                                    Runnable onComplete) {
        if (requestItems == null || requestItems.isEmpty()) {
            if (onComplete != null) {
                onComplete.run();
            }
            return null;
        }

        int safeBatchSize = Math.max(1, batchSize);
        int safeDelayMs = Math.max(0, delayMs);
        AtomicInteger restoreIndex = new AtomicInteger();
        long totalRestoreStartNanos = System.nanoTime();
        Timer timer = new Timer(safeDelayMs, null);
        timer.addActionListener(e -> {
            long batchStartNanos = System.nanoTime();
            int startIndex = restoreIndex.get();
            int endIndex = Math.min(startIndex + safeBatchSize, requestItems.size());
            StartupDiagnostics.mark("Restoring opened requests batch " + (startIndex + 1) + "-" + endIndex
                    + "/" + requestItems.size());
            for (int i = startIndex; i < endIndex; i++) {
                HttpRequestItem item = requestItems.get(i);
                boolean selectTab = i == requestItems.size() - 1;
                restoreAction.accept(item, selectTab);
            }

            if (endIndex >= requestItems.size()) {
                timer.stop();
                StartupDiagnostics.mark("Incremental restore finished in "
                        + StartupDiagnostics.formatSince(totalRestoreStartNanos));
                if (onComplete != null) {
                    onComplete.run();
                }
                return;
            }
            StartupDiagnostics.mark("Opened requests batch rendered in "
                    + StartupDiagnostics.formatSince(batchStartNanos));
            restoreIndex.set(endIndex);
        });
        timer.setInitialDelay(Math.max(0, initialDelayMs));
        timer.setRepeats(true);
        timer.start();
        return timer;
    }

    /**
     * 根据ID查找请求节点
     */
    public static DefaultMutableTreeNode findRequestNodeById(DefaultMutableTreeNode node, String id) {
        if (node == null) return null;

        Object userObj = node.getUserObject();
        if (userObj instanceof Object[] obj && REQUEST.equals(obj[0])) {
            HttpRequestItem item = (HttpRequestItem) obj[1];
            if (id.equals(item.getId())) {
                return node;
            }
        }


        for (int i = 0; i < node.getChildCount(); i++) {
            DefaultMutableTreeNode child = (DefaultMutableTreeNode) node.getChildAt(i);
            DefaultMutableTreeNode result = findRequestNodeById(child, id);
            if (result != null) {
                return result;
            }
        }

        return null;
    }


    /**
     * 弹出多选请求对话框，回调返回选中的HttpRequestItem列表
     */
    public static void showMultiSelectRequestDialog(Consumer<List<HttpRequestItem>> onSelected) {
        RequestCollectionsLeftPanel requestCollectionsLeftPanel = SingletonFactory.getInstance(RequestCollectionsLeftPanel.class);
        JDialog dialog = new JDialog(SingletonFactory.getInstance(MainFrame.class),
                I18nUtil.getMessage(MessageKeys.COLLECTIONS_DIALOG_MULTI_SELECT_TITLE), true);
        dialog.setSize(400, 500);
        dialog.setResizable(false);
        dialog.setLocationRelativeTo(null);
        dialog.setLayout(new BorderLayout());

        // 用JTree展示集合树，支持多选
        JTree tree = requestCollectionsLeftPanel.createRequestSelectionTree();
        tree.getSelectionModel().setSelectionMode(TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION);

        // 默认展开第一个group
        DefaultMutableTreeNode root = (DefaultMutableTreeNode) tree.getModel().getRoot();
        if (root != null && root.getChildCount() > 0) {
            DefaultMutableTreeNode firstChild = (DefaultMutableTreeNode) root.getChildAt(0);
            tree.expandPath(new TreePath(firstChild.getPath()));
        }

        JScrollPane treeScroll = new JScrollPane(tree);
        dialog.add(treeScroll, BorderLayout.CENTER);

        JButton okBtn = new JButton(I18nUtil.getMessage(MessageKeys.GENERAL_OK));
        okBtn.addActionListener(e -> {
            List<HttpRequestItem> selected = requestCollectionsLeftPanel.getSelectedRequestsFromTree(tree);
            if (selected.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, I18nUtil.getMessage(MessageKeys.COLLECTIONS_DIALOG_MULTI_SELECT_EMPTY),
                        I18nUtil.getMessage(MessageKeys.GENERAL_TIP), JOptionPane.WARNING_MESSAGE);
                return;
            }
            onSelected.accept(selected);
            dialog.dispose();
        });
        JButton cancelBtn = new JButton(I18nUtil.getMessage(MessageKeys.GENERAL_CANCEL));
        cancelBtn.addActionListener(e -> dialog.dispose());
        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        btns.add(okBtn);
        btns.add(cancelBtn);
        dialog.add(btns, BorderLayout.SOUTH);
        dialog.setVisible(true);
    }
}
