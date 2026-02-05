package com.laker.postman.service.collections;

import cn.hutool.core.collection.CollUtil;
import com.laker.postman.model.AuthType;
import com.laker.postman.model.Variable;
import com.laker.postman.model.HttpHeader;
import com.laker.postman.model.HttpRequestItem;
import com.laker.postman.model.RequestGroup;
import com.laker.postman.service.js.ScriptFragment;
import com.laker.postman.service.js.ScriptMerger;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

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
@Slf4j
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

        // 收集分组链
        List<RequestGroup> groupChain = collectGroupChain(requestNode);
        if (groupChain.isEmpty()) {
            return item; // 无父分组，直接返回原对象
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
        List<Variable> groupVariables = new ArrayList<>();

        // 应用继承逻辑
        applyAuthInheritance(mergedItem, groupChain);
        collectScriptsHeadersAndVariables(groupChain, groupPreScripts, groupPostScripts, groupHeaders, groupVariables);

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
     * 收集分组链（从外到内）
     * <p>
     * 优化点：
     * - 使用迭代代替递归，避免栈溢出
     * - 直接向上遍历，性能为 O(h)，h 为树的高度
     */
    private static List<RequestGroup> collectGroupChain(DefaultMutableTreeNode startNode) {
        List<RequestGroup> groupChain = new ArrayList<>();

        if (startNode == null) {
            return groupChain;
        }

        TreeNode currentNode = startNode.getParent(); // 从父节点开始

        while (currentNode != null) {
            if (!(currentNode instanceof DefaultMutableTreeNode)) {
                break;
            }

            DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode) currentNode;
            Object userObj = treeNode.getUserObject();

            // 检查是否是根节点
            if (userObj == null || "root".equals(String.valueOf(userObj))) {
                break;
            }

            // 检查是否是分组节点
            if (userObj instanceof Object[] obj && obj.length >= 2 && "group".equals(obj[0])) {
                Object groupData = obj[1];
                if (groupData instanceof RequestGroup group) {
                    groupChain.add(0, group); // 插入到列表头部，保持外层到内层的顺序
                }
            }

            currentNode = currentNode.getParent();
        }

        return groupChain;
    }

    /**
     * 应用认证继承（就近原则：从内到外查找第一个有认证的分组）
     */
    private static void applyAuthInheritance(HttpRequestItem item, List<RequestGroup> groupChain) {
        if (!AuthType.INHERIT.getConstant().equals(item.getAuthType())) {
            return; // 不需要继承认证
        }

        // 从内到外查找第一个有认证的分组
        for (int i = groupChain.size() - 1; i >= 0; i--) {
            RequestGroup group = groupChain.get(i);
            if (group.hasAuth()) {
                item.setAuthType(group.getAuthType());
                item.setAuthUsername(group.getAuthUsername());
                item.setAuthPassword(group.getAuthPassword());
                item.setAuthToken(group.getAuthToken());
                return; // 找到后立即返回
            }
        }
    }

    /**
     * 收集脚本、请求头和变量
     */
    private static void collectScriptsHeadersAndVariables(
            List<RequestGroup> groupChain,
            List<ScriptFragment> preScripts,
            List<ScriptFragment> postScripts,
            List<HttpHeader> headers,
            List<Variable> variables) {

        // 【前置脚本】外层到内层顺序添加
        for (RequestGroup group : groupChain) {
            if (group.hasPreScript()) {
                preScripts.add(ScriptFragment.of(group.getName() + " PreScript", group.getPrescript()));
            }
        }

        // 【后置脚本】内层到外层顺序添加（反向遍历）
        for (int i = groupChain.size() - 1; i >= 0; i--) {
            RequestGroup group = groupChain.get(i);
            if (group.hasPostScript()) {
                postScripts.add(ScriptFragment.of(group.getName() + " PostScript", group.getPostscript()));
            }
        }

        // 【请求头】外层到内层顺序添加（后续合并时内层覆盖外层）
        for (RequestGroup group : groupChain) {
            if (group.hasHeaders() && CollUtil.isNotEmpty(group.getHeaders())) {
                headers.addAll(group.getHeaders());
            }
        }

        // 【变量】外层到内层顺序添加（后续合并时内层覆盖外层）
        for (RequestGroup group : groupChain) {
            if (CollUtil.isNotEmpty(group.getVariables())) {
                variables.addAll(group.getVariables());
            }
        }
    }

    /**
     * 克隆请求对象（深拷贝关键字段，避免修改原对象）
     * <p>
     * 优化点：
     * - 对集合类字段进行深拷贝，避免修改原对象
     * - 使用链式调用提高可读性
     */
    private static HttpRequestItem cloneRequest(HttpRequestItem item) {
        HttpRequestItem clone = new HttpRequestItem();

        // 基本字段
        clone.setId(item.getId());
        clone.setName(item.getName());
        clone.setUrl(item.getUrl());
        clone.setMethod(item.getMethod());
        clone.setProtocol(item.getProtocol());
        clone.setBodyType(item.getBodyType());
        clone.setBody(item.getBody());

        // 认证字段
        clone.setAuthType(item.getAuthType());
        clone.setAuthUsername(item.getAuthUsername());
        clone.setAuthPassword(item.getAuthPassword());
        clone.setAuthToken(item.getAuthToken());

        // 脚本字段
        clone.setPrescript(item.getPrescript());
        clone.setPostscript(item.getPostscript());

        // 集合字段（深拷贝以避免修改原对象）
        clone.setHeadersList(item.getHeadersList() != null ?
            new ArrayList<>(item.getHeadersList()) : new ArrayList<>());
        clone.setParamsList(item.getParamsList() != null ?
            new ArrayList<>(item.getParamsList()) : new ArrayList<>());
        clone.setFormDataList(item.getFormDataList() != null ?
            new ArrayList<>(item.getFormDataList()) : new ArrayList<>());
        clone.setUrlencodedList(item.getUrlencodedList() != null ?
            new ArrayList<>(item.getUrlencodedList()) : new ArrayList<>());

        return clone;
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
     * <p>
     * 优化点：
     * - 提前返回空情况
     * - 预估容量减少扩容
     * - 批量过滤无效请求头
     *
     * @param groupHeaders   Group 级别的请求头列表（已按从外到内排序）
     * @param requestHeaders Request 级别的请求头列表
     * @return 合并后的请求头列表
     */
    private static List<HttpHeader> mergeHeaders(List<HttpHeader> groupHeaders, List<HttpHeader> requestHeaders) {
        boolean hasGroupHeaders = CollUtil.isNotEmpty(groupHeaders);
        boolean hasRequestHeaders = CollUtil.isNotEmpty(requestHeaders);

        // 优化：提前返回空情况
        if (!hasGroupHeaders && !hasRequestHeaders) {
            return new ArrayList<>();
        }

        if (!hasGroupHeaders) {
            return new ArrayList<>(requestHeaders);
        }

        if (!hasRequestHeaders) {
            return new ArrayList<>(groupHeaders);
        }

        // 使用 LinkedHashMap 保持插入顺序，同时支持覆盖
        // Key 使用小写以实现不区分大小写的匹配
        // 预估容量以减少扩容
        int estimatedSize = groupHeaders.size() + requestHeaders.size();
        Map<String, HttpHeader> mergedMap = new LinkedHashMap<>((int) (estimatedSize / 0.75) + 1);

        // 1. 先添加 Group 级别的请求头（外层到内层）
        for (HttpHeader header : groupHeaders) {
            if (isValidHeader(header)) {
                String keyLower = header.getKey().toLowerCase();
                mergedMap.put(keyLower, header);
            }
        }

        // 2. 再添加 Request 级别的请求头（覆盖同名的 Group 请求头）
        for (HttpHeader header : requestHeaders) {
            if (isValidHeader(header)) {
                String keyLower = header.getKey().toLowerCase();
                mergedMap.put(keyLower, header);
            }
        }

        // 3. 返回合并后的列表
        return new ArrayList<>(mergedMap.values());
    }

    /**
     * 验证请求头是否有效
     */
    private static boolean isValidHeader(HttpHeader header) {
        return header != null &&
               header.getKey() != null &&
               !header.getKey().trim().isEmpty();
    }

    /**
     * 获取合并后的分组级别变量
     * <p>
     * 用于变量解析器获取当前请求所继承的所有分组变量
     * 变量优先级：内层分组覆盖外层分组
     *
     * @param requestNode 请求在树中的节点
     * @return 合并后的变量列表，按优先级排序（内层优先）
     */
    public static List<Variable> getMergedGroupVariables(DefaultMutableTreeNode requestNode) {
        if (requestNode == null) {
            return new ArrayList<>();
        }

        List<RequestGroup> groupChain = collectGroupChain(requestNode);
        if (groupChain.isEmpty()) {
            return new ArrayList<>();
        }

        // 使用 LinkedHashMap 保持插入顺序，同时支持覆盖
        // Key 使用变量名，内层覆盖外层
        Map<String, Variable> mergedMap = new LinkedHashMap<>();

        // 外层到内层顺序添加，内层会覆盖外层的同名变量
        for (RequestGroup group : groupChain) {
            if (CollUtil.isNotEmpty(group.getVariables())) {
                for (Variable variable : group.getVariables()) {
                    if (variable != null && variable.getKey() != null && !variable.getKey().trim().isEmpty()) {
                        mergedMap.put(variable.getKey(), variable);
                    }
                }
            }
        }

        return new ArrayList<>(mergedMap.values());
    }
}
