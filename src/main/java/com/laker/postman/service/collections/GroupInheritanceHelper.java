package com.laker.postman.service.collections;

import com.laker.postman.model.AuthType;
import com.laker.postman.model.HttpRequestItem;
import com.laker.postman.model.RequestGroup;
import com.laker.postman.service.js.ScriptFragment;
import com.laker.postman.service.js.ScriptMerger;
import lombok.experimental.UtilityClass;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import java.util.ArrayList;
import java.util.List;


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

        // 收集分组脚本
        List<ScriptFragment> groupPreScripts = new ArrayList<>();
        List<ScriptFragment> groupPostScripts = new ArrayList<>();

        // 查找父分组并收集脚本
        TreeNode parent = requestNode.getParent();
        if (parent instanceof DefaultMutableTreeNode parentNode) {
            collectGroupSettings(mergedItem, parentNode, groupPreScripts, groupPostScripts);
        }

        // 合并前置脚本（外层到内层的顺序）
        String mergedPreScript = ScriptMerger.mergePreScripts(groupPreScripts, requestPreScript);
        mergedItem.setPrescript(mergedPreScript);

        // 合并后置脚本（内层到外层的顺序）
        // 注意：groupPostScripts已经按从内到外的顺序收集
        String mergedPostScript = ScriptMerger.mergePostScripts(requestPostScript, groupPostScripts);
        mergedItem.setPostscript(mergedPostScript);

        return mergedItem;
    }

    /**
     * 收集分组设置和脚本
     * <p>
     * 核心策略：
     * - 认证：就近原则，找到第一个有认证的分组就停止
     * - 前置脚本：从外到内收集（外层先执行）
     * - 后置脚本：从内到外收集（内层先执行）
     */
    private static void collectGroupSettings(
            HttpRequestItem item,
            DefaultMutableTreeNode groupNode,
            List<ScriptFragment> preScripts,
            List<ScriptFragment> postScripts) {

        if (groupNode == null) {
            return;
        }

        Object userObj = groupNode.getUserObject();
        if (!(userObj instanceof Object[] obj) || !"group".equals(obj[0])) {
            // 不是分组节点，继续向上递归
            TreeNode parent = groupNode.getParent();
            if (parent instanceof DefaultMutableTreeNode parentNode &&
                    !"root".equals(String.valueOf(parentNode.getUserObject()))) {
                collectGroupSettings(item, parentNode, preScripts, postScripts);
            }
            return;
        }

        Object groupData = obj[1];
        if (!(groupData instanceof RequestGroup group)) {
            return;
        }

        // 【认证】就近原则：内层优先，一旦找到有认证的分组就设置并停止继承
        if (AuthType.INHERIT.getConstant().equals(item.getAuthType()) && group.hasAuth()) {
            item.setAuthType(group.getAuthType());
            item.setAuthUsername(group.getAuthUsername());
            item.setAuthPassword(group.getAuthPassword());
            item.setAuthToken(group.getAuthToken());
        }

        // 先递归处理父分组（外层）
        TreeNode parent = groupNode.getParent();
        if (parent instanceof DefaultMutableTreeNode parentNode &&
                !"root".equals(String.valueOf(parentNode.getUserObject()))) {
            collectGroupSettings(item, parentNode, preScripts, postScripts);
        }

        // 【前置脚本】在递归返回后添加（这样外层脚本在列表前面，内层在后面）
        if (group.hasPreScript()) {
            preScripts.add(ScriptFragment.of(group.getName() + " 脚本", group.getPrescript()));
        }

        // 【后置脚本】在递归返回后添加（这样内层脚本在列表前面，外层在后面）
        if (group.hasPostScript()) {
            postScripts.add(ScriptFragment.of(group.getName() + " 脚本", group.getPostscript()));
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
