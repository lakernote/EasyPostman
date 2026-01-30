package com.laker.postman.service.collections;

import cn.hutool.core.collection.CollUtil;
import com.laker.postman.model.AuthType;
import com.laker.postman.model.HttpHeader;
import com.laker.postman.model.HttpRequestItem;
import com.laker.postman.model.RequestGroup;
import com.laker.postman.service.js.ScriptFragment;
import com.laker.postman.service.js.ScriptMerger;
import lombok.experimental.UtilityClass;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


/**
 * 分组继承工具类
 * 处理认证、脚本和请求头从父分组继承的逻辑
 * <p>
 * 树结构：
 * - Folder（文件夹）：可嵌套的分组节点 ["group", RequestGroup对象]，可配置认证、脚本和请求头
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
 * <p>
 * 4. 请求头继承（从外到内累积，内层覆盖外层）：
 * - 外层 Folder Headers -> 内层 Folder Headers -> Request Headers
 * - 同名请求头：内层覆盖外层（Request > Inner Folder > Outer Folder）
 * - 示例：FolderA(X-API-Key: key1) -> FolderB(X-API-Key: key2, X-Extra: val) -> Request(X-API-Key: key3)
 * => 最终使用: X-API-Key: key3, X-Extra: val
 */
@UtilityClass
public class GroupInheritanceHelper {

    /**
     * 合并分组级别的认证、脚本和请求头到请求
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

        // 收集分组脚本和请求头
        List<ScriptFragment> groupPreScripts = new ArrayList<>();
        List<ScriptFragment> groupPostScripts = new ArrayList<>();
        List<HttpHeader> groupHeaders = new ArrayList<>();

        // 查找父分组并收集脚本和请求头
        TreeNode parent = requestNode.getParent();
        if (parent instanceof DefaultMutableTreeNode parentNode) {
            collectGroupSettings(mergedItem, parentNode, groupPreScripts, groupPostScripts, groupHeaders);
        }

        // 合并前置脚本（外层到内层的顺序）
        String mergedPreScript = ScriptMerger.mergePreScripts(groupPreScripts, requestPreScript);
        mergedItem.setPrescript(mergedPreScript);

        // 合并后置脚本（内层到外层的顺序）
        // 注意：groupPostScripts已经按从内到外的顺序收集
        String mergedPostScript = ScriptMerger.mergePostScripts(requestPostScript, groupPostScripts);
        mergedItem.setPostscript(mergedPostScript);

        // 合并请求头（外层到内层累积，内层覆盖外层）
        List<HttpHeader> mergedHeaders = mergeHeaders(groupHeaders, mergedItem.getHeadersList());
        mergedItem.setHeadersList(mergedHeaders);

        return mergedItem;
    }

    /**
     * 收集分组设置、脚本和请求头
     * <p>
     * 核心策略：
     * - 认证：就近原则，找到第一个有认证的分组就停止
     * - 前置脚本：从外到内收集（外层先执行）
     * - 后置脚本：从内到外收集（内层先执行）
     * - 请求头：从外到内收集（内层覆盖外层）
     */
    private static void collectGroupSettings(
            HttpRequestItem item,
            DefaultMutableTreeNode groupNode,
            List<ScriptFragment> preScripts,
            List<ScriptFragment> postScripts,
            List<HttpHeader> headers) {

        if (groupNode == null) {
            return;
        }

        Object userObj = groupNode.getUserObject();
        if (!(userObj instanceof Object[] obj) || !"group".equals(obj[0])) {
            // 不是分组节点，继续向上递归
            TreeNode parent = groupNode.getParent();
            if (parent instanceof DefaultMutableTreeNode parentNode &&
                    !"root".equals(String.valueOf(parentNode.getUserObject()))) {
                collectGroupSettings(item, parentNode, preScripts, postScripts, headers);
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
            collectGroupSettings(item, parentNode, preScripts, postScripts, headers);
        }

        // 【前置脚本】在递归返回后添加（这样外层脚本在列表前面，内层在后面）
        if (group.hasPreScript()) {
            preScripts.add(ScriptFragment.of(group.getName() + " script", group.getPrescript()));
        }

        // 【后置脚本】在递归返回后添加（这样内层脚本在列表前面，外层在后面）
        if (group.hasPostScript()) {
            postScripts.add(ScriptFragment.of(group.getName() + " script", group.getPostscript()));
        }

        // 【请求头】在递归返回后添加（这样外层请求头在列表前面，内层在后面）
        // 后续合并时会按顺序处理，内层覆盖外层的同名请求头
        if (group.hasHeaders() && CollUtil.isNotEmpty(group.getHeaders())) {
            headers.addAll(group.getHeaders());
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
            HttpRequestItem req = (HttpRequestItem) obj[1];
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

    /**
     * 合并请求头（从外到内累积，内层覆盖外层的同名请求头）
     * <p>
     * 合并策略：
     * 1. 先添加所有 Group 级别的请求头（按从外到内的顺序）
     * 2. 再添加 Request 级别的请求头
     * 3. 同名请求头（Key 不区分大小写）：后面的覆盖前面的
     * <p>
     * 示例：
     * - Group A: [X-API-Key: key1, Accept: *]
     * - Group B: [X-API-Key: key2, Content-Type: json]
     * - Request: [X-API-Key: key3, Custom: value]
     * - 结果: [Accept: *, Content-Type: json, X-API-Key: key3, Custom: value]
     *
     * @param groupHeaders   Group 级别的请求头列表（已按从外到内排序）
     * @param requestHeaders Request 级别的请求头列表
     * @return 合并后的请求头列表
     */
    private static List<HttpHeader> mergeHeaders(List<HttpHeader> groupHeaders, List<HttpHeader> requestHeaders) {
        // 使用 LinkedHashMap 保持插入顺序，同时支持覆盖
        // Key 使用小写以实现不区分大小写的匹配
        Map<String, HttpHeader> mergedMap = new LinkedHashMap<>();

        // 1. 先添加 Group 级别的请求头（外层到内层）
        if (CollUtil.isNotEmpty(groupHeaders)) {
            for (HttpHeader header : groupHeaders) {
                if (header != null && header.getKey() != null && !header.getKey().trim().isEmpty()) {
                    String keyLower = header.getKey().toLowerCase();
                    mergedMap.put(keyLower, header);
                }
            }
        }

        // 2. 再添加 Request 级别的请求头（覆盖同名的 Group 请求头）
        if (CollUtil.isNotEmpty(requestHeaders)) {
            for (HttpHeader header : requestHeaders) {
                if (header != null && header.getKey() != null && !header.getKey().trim().isEmpty()) {
                    String keyLower = header.getKey().toLowerCase();
                    mergedMap.put(keyLower, header);
                }
            }
        }

        // 3. 返回合并后的列表
        return new ArrayList<>(mergedMap.values());
    }
}
