package com.laker.postman.service.collections;

import com.laker.postman.model.AuthType;
import com.laker.postman.model.HttpRequestItem;
import com.laker.postman.model.RequestGroup;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;


/**
 * 分组继承工具类
 * 处理认证和脚本从父分组继承的逻辑
 *
 * 继承规则（遵循 Postman 行为）：
 * 1. 认证：仅当请求选择 "继承父级认证" 时才继承
 * 2. 前置脚本：Collection -> Folder -> Request（从外到内）
 * 3. 后置脚本：Request -> Folder -> Collection（从内到外）
 */
public class GroupInheritanceHelper {

    private GroupInheritanceHelper() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * 合并分组级别的认证和脚本到请求
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

        // 保存请求自己的脚本
        String requestPreScript = mergedItem.getPrescript();
        String requestPostScript = mergedItem.getPostscript();

        // 清空脚本，准备合并
        mergedItem.setPrescript(null);
        mergedItem.setPostscript(null);

        // 查找父分组并合并设置
        TreeNode parent = requestNode.getParent();
        if (parent instanceof DefaultMutableTreeNode parentNode) {
            mergeGroupSettingsRecursive(mergedItem, parentNode);
        }

        // 最后追加请求自己的脚本
        // 前置脚本：请求脚本在最后
        if (requestPreScript != null && !requestPreScript.trim().isEmpty()) {
            String groupScripts = mergedItem.getPrescript();
            if (groupScripts == null || groupScripts.trim().isEmpty()) {
                mergedItem.setPrescript(requestPreScript);
            } else {
                mergedItem.setPrescript(groupScripts + "\n\n// === 请求级脚本 ===\n\n" + requestPreScript);
            }
        }

        // 后置脚本：请求脚本在最前
        if (requestPostScript != null && !requestPostScript.trim().isEmpty()) {
            String groupScripts = mergedItem.getPostscript();
            if (groupScripts == null || groupScripts.trim().isEmpty()) {
                mergedItem.setPostscript(requestPostScript);
            } else {
                mergedItem.setPostscript(requestPostScript + "\n\n// === 分组级脚本 ===\n\n" + groupScripts);
            }
        }

        return mergedItem;
    }

    /**
     * 递归合并父分组的设置
     *
     * 关键策略：
     * - 前置脚本：先递归父节点，再处理当前节点（结果：外层先 w，然后内层 n）
     * - 后置脚本：先处理当前节点，再递归父节点（结果：内层先 n，然后外层 w）
     * - 认证：递归到外层后，内层覆盖外层（最近的父分组优先）
     */
    private static void mergeGroupSettingsRecursive(HttpRequestItem item, DefaultMutableTreeNode groupNode) {
        if (groupNode == null) {
            return;
        }

        Object userObj = groupNode.getUserObject();
        if (!(userObj instanceof Object[] obj) || !"group".equals(obj[0])) {
            // 不是分组节点，继续向上递归
            TreeNode parent = groupNode.getParent();
            if (parent instanceof DefaultMutableTreeNode parentNode &&
                !"root".equals(String.valueOf(parentNode.getUserObject()))) {
                mergeGroupSettingsRecursive(item, parentNode);
            }
            return;
        }

        Object groupData = obj[1];
        if (!(groupData instanceof RequestGroup group)) {
            return;
        }

        // 【后置脚本】先处理当前层（这样内层先追加）
        if (group.hasPostScript()) {
            String groupScript = group.getPostscript();
            String existingScript = item.getPostscript();
            if (existingScript == null || existingScript.trim().isEmpty()) {
                item.setPostscript(groupScript);
            } else {
                item.setPostscript(existingScript + "\n\n// === " + group.getName() + " 脚本 ===\n\n" + groupScript);
            }
        }

        // 递归处理父分组（外层）
        TreeNode parent = groupNode.getParent();
        if (parent instanceof DefaultMutableTreeNode parentNode &&
            !"root".equals(String.valueOf(parentNode.getUserObject()))) {
            mergeGroupSettingsRecursive(item, parentNode);
        }

        // 【前置脚本】在递归返回后处理（这样外层先追加，内层后追加）
        if (group.hasPreScript()) {
            String groupScript = group.getPrescript();
            String existingScript = item.getPrescript();
            if (existingScript == null || existingScript.trim().isEmpty()) {
                item.setPrescript(groupScript);
            } else {
                item.setPrescript(existingScript + "\n\n// === " + group.getName() + " 脚本 ===\n\n" + groupScript);
            }
        }

        // 【认证】在递归返回后处理（这样内层覆盖外层）
        if (AuthType.INHERIT.getConstant().equals(item.getAuthType()) && group.hasAuth()) {
            item.setAuthType(group.getAuthType());
            item.setAuthUsername(group.getAuthUsername());
            item.setAuthPassword(group.getAuthPassword());
            item.setAuthToken(group.getAuthToken());
        }
    }

    /**
     * 克隆请求对象（浅拷贝，足够用于临时合并）
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
     * 在树中通过请求 ID 查找请求节点
     *
     * @param root 树的根节点
     * @param requestId 要查找的请求 ID
     * @return 请求节点，如果未找到则返回 null
     */
    public static DefaultMutableTreeNode findRequestNode(DefaultMutableTreeNode root, String requestId) {
        if (root == null || requestId == null) {
            return null;
        }

        // 检查当前节点
        Object userObj = root.getUserObject();
        if (userObj instanceof Object[] obj && "request".equals(obj[0])) {
            com.laker.postman.model.HttpRequestItem req = (com.laker.postman.model.HttpRequestItem) obj[1];
            if (requestId.equals(req.getId())) {
                return root;
            }
        }

        // 递归搜索子节点
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

