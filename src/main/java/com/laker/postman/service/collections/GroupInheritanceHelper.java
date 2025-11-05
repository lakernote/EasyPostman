package com.laker.postman.service.collections;

import com.laker.postman.model.HttpRequestItem;
import com.laker.postman.model.RequestGroup;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;

import static com.laker.postman.panel.collections.right.request.sub.AuthTabPanel.AUTH_TYPE_NONE;

/**
 * 分组继承工具类
 * 负责处理分组级别的认证和脚本继承
 */
public class GroupInheritanceHelper {

    private GroupInheritanceHelper() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * 合并分组级别的认证和脚本到请求
     * 如果请求本身没有配置认证，则使用分组的认证
     * 脚本会在请求脚本之前/之后执行
     *
     * @param item 请求项
     * @param requestNode 请求在树中的节点
     * @return 合并后的请求项（新对象）
     */
    public static HttpRequestItem mergeGroupSettings(HttpRequestItem item, DefaultMutableTreeNode requestNode) {
        if (item == null || requestNode == null) {
            return item;
        }

        // 创建副本以避免修改原对象
        HttpRequestItem mergedItem = cloneRequest(item);

        // 查找父分组并合并设置
        TreeNode parent = requestNode.getParent();
        if (parent instanceof DefaultMutableTreeNode parentNode) {
            mergeGroupSettingsRecursive(mergedItem, parentNode);
        }

        return mergedItem;
    }

    /**
     * 递归合并父分组的设置
     */
    private static void mergeGroupSettingsRecursive(HttpRequestItem item, DefaultMutableTreeNode groupNode) {
        if (groupNode == null) {
            return;
        }

        Object userObj = groupNode.getUserObject();
        if (userObj instanceof Object[] obj && "group".equals(obj[0])) {
            Object groupData = obj[1];
            if (groupData instanceof RequestGroup group) {
                // 如果请求没有设置认证，使用分组的认证
                if (AUTH_TYPE_NONE.equals(item.getAuthType()) && group.hasAuth()) {
                    item.setAuthType(group.getAuthType());
                    item.setAuthUsername(group.getAuthUsername());
                    item.setAuthPassword(group.getAuthPassword());
                    item.setAuthToken(group.getAuthToken());
                }

                // 合并前置脚本（分组脚本在前）
                if (group.hasPreScript()) {
                    String groupScript = group.getPrescript();
                    String requestScript = item.getPrescript();
                    if (requestScript == null || requestScript.trim().isEmpty()) {
                        item.setPrescript(groupScript);
                    } else {
                        item.setPrescript(groupScript + "\n\n// === Request-level script ===\n\n" + requestScript);
                    }
                }

                // 合并后置脚本（分组脚本在后）
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

        // 继续向上查找父分组
        TreeNode parent = groupNode.getParent();
        if (parent instanceof DefaultMutableTreeNode parentNode &&
            !"root".equals(String.valueOf(parentNode.getUserObject()))) {
            mergeGroupSettingsRecursive(item, parentNode);
        }
    }

    /**
     * 克隆请求对象（浅拷贝，但足够用于临时合并）
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
     * 查找请求在树中的节点
     *
     * @param rootNode 根节点
     * @param requestId 请求ID
     * @return 请求节点，如果未找到返回null
     */
    public static DefaultMutableTreeNode findRequestNode(DefaultMutableTreeNode rootNode, String requestId) {
        if (rootNode == null || requestId == null) {
            return null;
        }

        Object userObj = rootNode.getUserObject();
        if (userObj instanceof Object[] obj && "request".equals(obj[0])) {
            HttpRequestItem item = (HttpRequestItem) obj[1];
            if (requestId.equals(item.getId())) {
                return rootNode;
            }
        }

        // 递归查找子节点
        for (int i = 0; i < rootNode.getChildCount(); i++) {
            DefaultMutableTreeNode child = (DefaultMutableTreeNode) rootNode.getChildAt(i);
            DefaultMutableTreeNode found = findRequestNode(child, requestId);
            if (found != null) {
                return found;
            }
        }

        return null;
    }
}

