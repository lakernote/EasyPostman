package com.laker.postman.service.collections;

import com.laker.postman.model.HttpRequestItem;
import com.laker.postman.panel.collections.right.request.RequestEditSubPanel;
import com.laker.postman.service.UnSavedNewRequestService;
import com.laker.postman.util.UserSettingsUtil;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import java.util.List;

import static com.laker.postman.panel.collections.left.RequestCollectionsLeftPanel.REQUEST;

@Slf4j
public class RequestCollectionsService {
    // 请求集合的根节点
    private DefaultMutableTreeNode rootTreeNode;
    // 请求树组件
    private JTree requestTree;

    public RequestCollectionsService(DefaultMutableTreeNode rootTreeNode, JTree requestTree) {
        this.rootTreeNode = rootTreeNode;
        this.requestTree = requestTree;
    }

    public int restoreUnSavedNewRequests() {
        List<HttpRequestItem> unSavedRequests = UnSavedNewRequestService.getAll();
        for (HttpRequestItem item : unSavedRequests) {
            RequestEditSubPanel panel = RequestCollectionsTabsService.addTab(item);
            RequestCollectionsTabsService.updateTabNew(panel, true);
        }
        UnSavedNewRequestService.clear();
        return unSavedRequests.size();
    }

    public DefaultMutableTreeNode restoreLastSavedRequests() {
        String lastId = UserSettingsUtil.getLastOpenRequestId();
        if (lastId != null && !lastId.isEmpty()) {
            DefaultMutableTreeNode node = findRequestNodeById(rootTreeNode, lastId);
            if (node != null) {
                TreePath path = new TreePath(node.getPath());
                requestTree.setSelectionPath(path);
                requestTree.scrollPathToVisible(path);
                Object[] obj = (Object[]) node.getUserObject();
                if (REQUEST.equals(obj[0])) {
                    HttpRequestItem item = (HttpRequestItem) obj[1];
                    RequestCollectionsTabsService.addTab(item);
                    return node;
                }
            }
        }
        return null;
    }

    /**
     * 根据ID查找请求节点
     */
    public DefaultMutableTreeNode findRequestNodeById(DefaultMutableTreeNode node, String id) {
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
}
