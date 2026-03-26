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
import java.util.function.Consumer;

import static com.laker.postman.panel.collections.left.RequestCollectionsLeftPanel.REQUEST;

@Slf4j
@UtilityClass
public class RequestCollectionsService {
    public static HttpRequestItem getLastNonNewRequest() {
        return getLastNonNewRequest(OpenedRequestsService.getAll());
    }

    public static HttpRequestItem getLastNonNewRequest(List<HttpRequestItem> requestItems) {
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
        restoreOpenedRequests(OpenedRequestsService.getAll(), null);
    }

    public static void restoreOpenedRequests(List<HttpRequestItem> requestItems, Runnable onComplete) {
        Runnable restoreTask = () -> {
            RequestCollectionsLeftPanel leftPanel = SingletonFactory.getInstance(RequestCollectionsLeftPanel.class);
            List<HttpRequestItem> restorableRequests = buildRestorableOpenedRequests(requestItems, leftPanel.getRootTreeNode());
            long restoreStartNanos = System.nanoTime();
            StartupDiagnostics.mark("Opened requests restore prepared; validRequests=" + restorableRequests.size());
            for (int i = 0; i < restorableRequests.size(); i++) {
                HttpRequestItem item = restorableRequests.get(i);
                boolean selectTab = i == restorableRequests.size() - 1;
                RequestEditSubPanel panel = RequestsTabsService.addTab(item, selectTab, true);
                RequestsTabsService.updateTabNew(panel, item.isNewRequest());
            }
            OpenedRequestsService.clear();
            StartupDiagnostics.mark("Opened requests restore finished in "
                    + StartupDiagnostics.formatSince(restoreStartNanos));
            if (onComplete != null) {
                onComplete.run();
            }
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
