package com.laker.postman.service.collections;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.laker.postman.model.HttpRequestItem;
import com.laker.postman.model.RequestGroup;
import lombok.extern.slf4j.Slf4j;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class RequestsPersistence {
    // 静态锁映射：每个文件路径对应一个锁对象，确保不同实例操作同一文件时使用同一个锁
    private static final ConcurrentHashMap<String, Object> FILE_LOCKS = new ConcurrentHashMap<>();
    // 静态加载状态映射：记录每个文件是否正在加载
    private static final ConcurrentHashMap<String, Boolean> LOADING_STATUS = new ConcurrentHashMap<>();

    private String filePath;
    private final DefaultMutableTreeNode rootTreeNode;
    private final DefaultTreeModel treeModel;

    public RequestsPersistence(String filePath, DefaultMutableTreeNode rootTreeNode, DefaultTreeModel treeModel) {
        this.filePath = filePath;
        this.rootTreeNode = rootTreeNode;
        this.treeModel = treeModel;
    }

    /**
     * 获取文件对应的锁对象
     * 使用 computeIfAbsent 确保同一文件路径始终返回同一个锁对象
     */
    private Object getFileLock() {
        return FILE_LOCKS.computeIfAbsent(filePath, k -> new Object());
    }

    /**
     * 检查文件是否正在加载
     */
    private boolean isFileLoading() {
        return LOADING_STATUS.getOrDefault(filePath, false);
    }

    /**
     * 设置文件加载状态
     */
    private void setFileLoading(boolean loading) {
        if (loading) {
            LOADING_STATUS.put(filePath, true);
        } else {
            LOADING_STATUS.remove(filePath);
        }
    }

    public void exportRequestCollection(File fileToSave) throws IOException {
        synchronized (getFileLock()) {
            try (Writer writer = new OutputStreamWriter(new FileOutputStream(fileToSave), StandardCharsets.UTF_8)) {
                JSONArray array = new JSONArray();
                for (int i = 0; i < rootTreeNode.getChildCount(); i++) {
                    DefaultMutableTreeNode groupNode = (DefaultMutableTreeNode) rootTreeNode.getChildAt(i);
                    array.add(buildGroupJson(groupNode));
                }
                writer.write(array.toStringPretty());
            }
        }
    }

    /**
     * 内部保存方法，绕过加载状态检查，仅在初始化时使用
     */
    private void saveRequestGroupsInternal() {
        try (Writer writer = new OutputStreamWriter(new FileOutputStream(filePath), StandardCharsets.UTF_8)) {
            JSONArray array = new JSONArray();
            for (int i = 0; i < rootTreeNode.getChildCount(); i++) {
                DefaultMutableTreeNode groupNode = (DefaultMutableTreeNode) rootTreeNode.getChildAt(i);
                array.add(buildGroupJson(groupNode));
            }
            writer.write(array.toStringPretty());
            log.debug("Saved request groups to: {}", filePath);
        } catch (Exception ex) {
            log.error("Error saving request groups to file: {}", filePath, ex);
        }
    }

    public void initRequestGroupsFromFile() {
        Object lock = getFileLock();
        synchronized (lock) {
            setFileLoading(true); // 设置加载状态
            try {
                File file = new File(filePath);
                if (!file.exists()) { // 如果文件不存在，则创建默认请求组
                    DefaultRequestsFactory.create(rootTreeNode, treeModel); // 创建默认请求组
                    saveRequestGroupsInternal(); // 使用内部方法保存，绕过加载状态检查
                    log.info("File not found, created default request groups.");
                    return;
                }
                try {

                    JSONArray array = JSONUtil.readJSONArray(file, StandardCharsets.UTF_8);
                    List<DefaultMutableTreeNode> groupNodeList = new ArrayList<>();
                    for (Object o : array) {
                        JSONObject groupJson = (JSONObject) o;
                        DefaultMutableTreeNode groupNode = parseGroupNode(groupJson);
                        groupNodeList.add(groupNode);
                    }
                    groupNodeList.forEach(rootTreeNode::add);
                    treeModel.reload(rootTreeNode);
                    log.info("Loaded request groups from file: {}", filePath);
                } catch (Exception e) {
                    log.error("Error loading request groups from file: {}", filePath, e);
                }
            } finally {
                setFileLoading(false); // 重置加载状态
            }
        }
    }

    public void saveRequestGroups() {
        // 如果正在加载数据，跳过保存操作，避免保存空数据
        if (isFileLoading()) {
            log.warn("Skipping save operation for file '{}' because it is being loaded", filePath);
            return;
        }

        Object lock = getFileLock();
        synchronized (lock) {
            // 再次检查加载状态（双重检查）
            if (isFileLoading()) {
                log.warn("Skipping save operation for file '{}' because it is being loaded (double-check)", filePath);
                return;
            }

            try (Writer writer = new OutputStreamWriter(new FileOutputStream(filePath), StandardCharsets.UTF_8)) {
                JSONArray array = new JSONArray();
                for (int i = 0; i < rootTreeNode.getChildCount(); i++) {
                    DefaultMutableTreeNode groupNode = (DefaultMutableTreeNode) rootTreeNode.getChildAt(i);
                    array.add(buildGroupJson(groupNode));
                }
                writer.write(array.toStringPretty());
                log.debug("Saved request groups to: {}", filePath);
            } catch (Exception ex) {
                log.error("Error saving request groups to file: {}", filePath, ex);
            }
        }
    }

    public DefaultMutableTreeNode parseGroupNode(JSONObject groupJson) {
        String name = groupJson.getStr("name");

        // 创建RequestGroup对象
        RequestGroup group = new RequestGroup(name);

        // 解析分组ID（向后兼容：如果没有ID则自动生成）
        if (groupJson.containsKey("id")) {
            String id = groupJson.getStr("id");
            if (id != null && !id.isEmpty()) {
                group.setId(id);
            }
        }

        // 解析分组级别的认证信息
        if (groupJson.containsKey("authType")) {
            group.setAuthType(groupJson.getStr("authType", ""));
            group.setAuthUsername(groupJson.getStr("authUsername", ""));
            group.setAuthPassword(groupJson.getStr("authPassword", ""));
            group.setAuthToken(groupJson.getStr("authToken", ""));
        }

        // 解析分组级别的脚本
        if (groupJson.containsKey("prescript")) {
            group.setPrescript(groupJson.getStr("prescript", ""));
        }
        if (groupJson.containsKey("postscript")) {
            group.setPostscript(groupJson.getStr("postscript", ""));
        }

        DefaultMutableTreeNode groupNode = new DefaultMutableTreeNode(new Object[]{"group", group});
        JSONArray children = groupJson.getJSONArray("children");
        if (children != null) {
            for (Object child : children) {
                JSONObject childJson = (JSONObject) child;
                String type = childJson.getStr("type");
                if ("group".equals(type)) {
                    groupNode.add(parseGroupNode(childJson));
                } else if ("request".equals(type)) {
                    JSONObject dataJson = childJson.getJSONObject("data");
                    HttpRequestItem item = JSONUtil.toBean(dataJson, HttpRequestItem.class);
                    // 确保请求体不为 null
                    item.setBody(item.getBody() != null ? item.getBody() : "");
                    if (item.getId() == null || item.getId().isEmpty()) {
                        String id = dataJson.getStr("id");
                        if (id == null || id.isEmpty()) {
                            item.setId(UUID.randomUUID().toString());
                        } else {
                            item.setId(id);
                        }
                    }
                    groupNode.add(new DefaultMutableTreeNode(new Object[]{"request", item}));
                }
            }
        }
        return groupNode;
    }

    public JSONObject buildGroupJson(DefaultMutableTreeNode node) {
        JSONObject groupJson = new JSONObject();
        Object[] obj = (Object[]) node.getUserObject();
        groupJson.set("type", "group");

        // 处理分组名称和属性
        Object groupData = obj[1];
        if (groupData instanceof RequestGroup group) {
            // 新格式：使用RequestGroup对象
            groupJson.set("id", group.getId());
            groupJson.set("name", group.getName());
            groupJson.set("authType", group.getAuthType());
            groupJson.set("authUsername", group.getAuthUsername());
            groupJson.set("authPassword", group.getAuthPassword());
            groupJson.set("authToken", group.getAuthToken());
            groupJson.set("prescript", group.getPrescript());
            groupJson.set("postscript", group.getPostscript());
        } else if (groupData instanceof String name) {
            // 旧格式兼容：字符串名称
            groupJson.set("name", name);
        }

        JSONArray children = new JSONArray();
        for (int i = 0; i < node.getChildCount(); i++) {
            DefaultMutableTreeNode child = (DefaultMutableTreeNode) node.getChildAt(i);
            Object[] childObj = (Object[]) child.getUserObject();
            if ("group".equals(childObj[0])) {
                children.add(buildGroupJson(child));
            } else if ("request".equals(childObj[0])) {
                JSONObject reqJson = new JSONObject();
                reqJson.set("type", "request");
                HttpRequestItem requestItem = (HttpRequestItem) childObj[1];
                JSONObject itemJson = JSONUtil.parseObj(requestItem);
                reqJson.set("data", itemJson);
                children.add(reqJson);
            }
        }
        groupJson.set("children", children);
        return groupJson;
    }

    /**
     * 切换数据文件路径并重新加载集合
     */
    public void setDataFilePath(String path) {
        if (path == null || path.isBlank()) return;

        // 先获取旧文件路径的锁，防止在切换过程中旧文件被保存
        String oldPath = this.filePath;
        Object oldLock = FILE_LOCKS.computeIfAbsent(oldPath, k -> new Object());

        synchronized (oldLock) {
            // 更新文件路径
            this.filePath = path;

            // 获取新文件路径的锁
            Object newLock = getFileLock();
            synchronized (newLock) {
                setFileLoading(true); // 设置新文件的加载状态，防止在加载期间保存空数据
                try {
                    // 清空现有树
                    rootTreeNode.removeAllChildren();
                    treeModel.reload(rootTreeNode);
                    // 重新加载
                    initRequestGroupsFromFile();
                } finally {
                    setFileLoading(false); // 无论成功或失败，都要重置加载状态
                }
            }
        }
    }
}