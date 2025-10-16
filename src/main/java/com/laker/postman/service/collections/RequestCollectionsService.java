package com.laker.postman.service.collections;

import com.laker.postman.common.SingletonFactory;
import com.laker.postman.frame.MainFrame;
import com.laker.postman.model.HttpRequestItem;
import com.laker.postman.panel.collections.left.RequestCollectionsLeftPanel;
import com.laker.postman.panel.collections.right.request.RequestEditSubPanel;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeSelectionModel;
import java.awt.*;
import java.util.List;
import java.util.function.Consumer;

import static com.laker.postman.panel.collections.left.RequestCollectionsLeftPanel.REQUEST;

@Slf4j
public class RequestCollectionsService {


    private RequestCollectionsService() {
        // Prevent instantiation
    }

    public static HttpRequestItem getLastNonNewRequest() {
        List<HttpRequestItem> requestItems = OpenedRequestsService.getAll();
        for (int i = requestItems.size() - 1; i >= 0; i--) {
            HttpRequestItem item = requestItems.get(i);
            if (!item.isNewRequest()) {
                return item;
            }
        }
        return null;
    }

    public static void restoreOpenedRequests() {
        List<HttpRequestItem> requestItems = OpenedRequestsService.getAll();
        for (HttpRequestItem item : requestItems) {
            RequestEditSubPanel panel = RequestsTabsService.addTab(item);
            RequestsTabsService.updateTabNew(panel, item.isNewRequest());
        }
        OpenedRequestsService.clear();
    }

    /**
     * 根据ID查找请求节点
     */
    public static DefaultMutableTreeNode findRequestNodeById(DefaultMutableTreeNode node, String id) {
        if (node == null) return null;

        Object userObj = node.getUserObject();
        if (userObj instanceof Object[] obj) {
            if (REQUEST.equals(obj[0])) {
                HttpRequestItem item = (HttpRequestItem) obj[1];
                if (id.equals(item.getId())) {
                    return node;
                }
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
