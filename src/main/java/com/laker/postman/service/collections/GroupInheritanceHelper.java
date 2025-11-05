package com.laker.postman.service.collections;

import com.laker.postman.model.AuthType;
import com.laker.postman.model.HttpRequestItem;
import com.laker.postman.model.RequestGroup;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;


/**
 * Group inheritance helper
 * Handles authentication and script inheritance from parent groups
 *
 * Inheritance rules (follows Postman behavior):
 * 1. Auth: Only inherit when request selects "Inherit auth from parent"
 * 2. Pre-request scripts: Collection -> Folder -> Request (outer to inner)
 * 3. Test scripts: Request -> Folder -> Collection (inner to outer)
 */
public class GroupInheritanceHelper {

    private GroupInheritanceHelper() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * Merge group-level auth and scripts into request
     *
     * @param item Request item
     * @param requestNode Request node in tree
     * @return Merged request item (new object)
     */
    public static HttpRequestItem mergeGroupSettings(HttpRequestItem item, DefaultMutableTreeNode requestNode) {
        if (item == null || requestNode == null) {
            return item;
        }

        // Create copy to avoid modifying original object
        HttpRequestItem mergedItem = cloneRequest(item);

        // Find parent group and merge settings
        TreeNode parent = requestNode.getParent();
        if (parent instanceof DefaultMutableTreeNode parentNode) {
            mergeGroupSettingsRecursive(mergedItem, parentNode);
        }

        return mergedItem;
    }

    /**
     * Recursively merge parent group settings
     *
     * Recursion order: Request -> Parent Folder -> ... -> Collection
     *
     * Auth: Only inherit when AUTH_TYPE_INHERIT (follows Postman)
     * Pre-script concat: Collection + (Parent + Request) = C, P, R
     * Post-script concat: (Request + Parent) + Collection = R, P, C
     */
    private static void mergeGroupSettingsRecursive(HttpRequestItem item, DefaultMutableTreeNode groupNode) {
        if (groupNode == null) {
            return;
        }

        Object userObj = groupNode.getUserObject();
        if (userObj instanceof Object[] obj && "group".equals(obj[0])) {
            Object groupData = obj[1];
            if (groupData instanceof RequestGroup group) {
                // Only inherit auth when request explicitly selects "Inherit auth from parent"
                // If request selects "No Auth" or specific auth type, don't inherit
                if (AuthType.INHERIT.getConstant().equals(item.getAuthType()) && group.hasAuth()) {
                    item.setAuthType(group.getAuthType());
                    item.setAuthUsername(group.getAuthUsername());
                    item.setAuthPassword(group.getAuthPassword());
                    item.setAuthToken(group.getAuthToken());
                }

                // Merge pre-request script (group script first)
                if (group.hasPreScript()) {
                    String groupScript = group.getPrescript();
                    String requestScript = item.getPrescript();
                    if (requestScript == null || requestScript.trim().isEmpty()) {
                        item.setPrescript(groupScript);
                    } else {
                        item.setPrescript(groupScript + "\n\n// === Request-level script ===\n\n" + requestScript);
                    }
                }

                // Merge post-response script (group script last)
                if (group.hasPostScript()) {
                    String groupScript = group.getPostscript();
                    String requestScript = item.getPostscript();
                    if (requestScript == null || requestScript.trim().isEmpty()) {
                        item.setPostscript(groupScript);
                    } else {
                        item.setPostscript(requestScript + "\n\n// === Group-level script ===\n\n" + groupScript);
                    }
                }
            }
        }

        // Continue to parent group
        TreeNode parent = groupNode.getParent();
        if (parent instanceof DefaultMutableTreeNode parentNode &&
            !"root".equals(String.valueOf(parentNode.getUserObject()))) {
            mergeGroupSettingsRecursive(item, parentNode);
        }
    }

    /**
     * Clone request object (shallow copy, sufficient for temporary merge)
     */
    private static HttpRequestItem cloneRequest(HttpRequestItem item) {
        HttpRequestItem clone = new HttpRequestItem();
        clone.setId(item.getId());
        clone.setName(item.getName());
        clone.setUrl(item.getUrl());
        clone.setMethod(item.getMethod());
        clone.setProtocol(item.getProtocol());
        clone.setHeadersList(item.getHeadersList());
        clone.setBodyType(item.getBodyType());
        clone.setBody(item.getBody());
        clone.setParamsList(item.getParamsList());
        clone.setFormDataList(item.getFormDataList());
        clone.setUrlencodedList(item.getUrlencodedList());
        clone.setAuthType(item.getAuthType());
        clone.setAuthUsername(item.getAuthUsername());
        clone.setAuthPassword(item.getAuthPassword());
        clone.setAuthToken(item.getAuthToken());
        clone.setPrescript(item.getPrescript());
        clone.setPostscript(item.getPostscript());
        return clone;
    }

    /**
     * Find request node in tree by request ID
     *
     * @param root Root node of tree
     * @param requestId Request ID to find
     * @return Request node or null if not found
     */
    public static DefaultMutableTreeNode findRequestNode(DefaultMutableTreeNode root, String requestId) {
        if (root == null || requestId == null) {
            return null;
        }

        // Check current node
        Object userObj = root.getUserObject();
        if (userObj instanceof Object[] obj && "request".equals(obj[0])) {
            com.laker.postman.model.HttpRequestItem req = (com.laker.postman.model.HttpRequestItem) obj[1];
            if (requestId.equals(req.getId())) {
                return root;
            }
        }

        // Search children recursively
        for (int i = 0; i < root.getChildCount(); i++) {
            DefaultMutableTreeNode child = (DefaultMutableTreeNode) root.getChildAt(i);
            DefaultMutableTreeNode result = findRequestNode(child, requestId);
            if (result != null) {
                return result;
            }
        }

        return null;
    }
}

