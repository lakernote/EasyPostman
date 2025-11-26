package com.laker.postman.service.collections;

import com.laker.postman.model.AuthType;
import com.laker.postman.model.HttpRequestItem;
import com.laker.postman.model.RequestGroup;
import lombok.experimental.UtilityClass;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;


/**
 * 分组继承工具类
 * 处理认证和脚本从父分组继承的逻辑
 * <p>
 * 树结构：
 * - Folder（文件夹）：可嵌套的分组节点 ["group", RequestGroup对象]，可配置认证和脚本
 * - Request（请求）：请求节点 ["request", HttpRequestItem对象]，可继承父级设置
 * <p>
 * 继承规则（遵循 Postman 行为）：
 * <p>
 * 1. 认证继承：
 * - 仅当请求的认证类型为 "Inherit auth from parent" 时才继承
 * - 就近原则：最近的有认证的父文件夹优先
 * - 示例：FolderA(Basic) -> FolderB(Bearer) -> Request(Inherit) => 使用 Bearer
 * <p>
 * 2. 前置脚本执行顺序（从外到内）：
 * - 外层 Folder 前置脚本 -> 内层 Folder 前置脚本 -> Request 前置脚本 -> [发送请求]
 * - 外层脚本先执行，可以为内层准备数据
 * <p>
 * 3. 后置脚本执行顺序（从内到外）：
 * - [收到响应] -> Request 后置脚本 -> 内层 Folder 后置脚本 -> 外层 Folder 后置脚本
 * - 内层脚本先执行，可以处理响应数据供外层使用
 */
@UtilityClass
public class GroupInheritanceHelper {

    /**
     * 合并分组级别的认证和脚本到请求
     *
     * @param item        请求项
     * @param requestNode 请求在树中的节点
     * @return 合并后的请求项（新对象，不修改原对象）
     */
    public static HttpRequestItem mergeGroupSettings(HttpRequestItem item, DefaultMutableTreeNode requestNode) {
        if (item == null || requestNode == null) {
            return item;
        }

        // 创建副本，避免修改原对象
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
        // 前置脚本：请求脚本在最后执行（在分组脚本之后）
        if (requestPreScript != null && !requestPreScript.trim().isEmpty()) {
            String groupScripts = mergedItem.getPrescript();
            if (groupScripts == null || groupScripts.trim().isEmpty()) {
                mergedItem.setPrescript(requestPreScript);
            } else {
                mergedItem.setPrescript(groupScripts + "\n\n// === 请求级脚本 ===\n\n" + requestPreScript);
            }
        }

        // 后置脚本：请求脚本在最先执行（在分组脚本之前）
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
     * <p>
     * 核心策略：
     * - 认证：先处理当前层，找到第一个有认证的分组就停止（就近原则）
     * - 前置脚本：先递归父节点，再处理当前节点（结果：外层 → 内层）
     * - 后置脚本：先处理当前节点，再递归父节点（结果：内层 → 外层）
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

        // 【认证】先处理当前层（内层优先，一旦找到有认证的分组就设置并停止继承）
        if (AuthType.INHERIT.getConstant().equals(item.getAuthType()) && group.hasAuth()) {
            item.setAuthType(group.getAuthType());
            item.setAuthUsername(group.getAuthUsername());
            item.setAuthPassword(group.getAuthPassword());
            item.setAuthToken(group.getAuthToken());
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
     * @param root      树的根节点
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

