package com.laker.postman.service.collections;

import com.laker.postman.model.HttpRequestItem;
import com.laker.postman.panel.collections.right.request.RequestEditSubPanel;
import lombok.extern.slf4j.Slf4j;

import javax.swing.tree.DefaultMutableTreeNode;
import java.util.List;

import static com.laker.postman.panel.collections.left.RequestCollectionsLeftPanel.REQUEST;

@Slf4j
public class RequestCollectionsService {


    private RequestCollectionsService() {
        // Prevent instantiation
    }

    public static void restoreOpenedRequests() {
        List<HttpRequestItem> unSavedRequests = OpenedRequestsService.getAll();
        for (HttpRequestItem item : unSavedRequests) {
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
}
