package com.laker.postman.service;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.laker.postman.model.HttpRequestItem;
import com.laker.postman.service.collections.RequestCollectionsFactory;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
public class RequestCollectionPersistence {
    private final String filePath;
    private final DefaultMutableTreeNode rootTreeNode;
    private final DefaultTreeModel treeModel;

    public RequestCollectionPersistence(String filePath, DefaultMutableTreeNode rootTreeNode, DefaultTreeModel treeModel) {
        this.filePath = filePath;
        this.rootTreeNode = rootTreeNode;
        this.treeModel = treeModel;
    }

    public void exportRequestCollection(File fileToSave) throws IOException {
        try (Writer writer = new OutputStreamWriter(new FileOutputStream(fileToSave), StandardCharsets.UTF_8)) {
            JSONArray array = new JSONArray();
            for (int i = 0; i < rootTreeNode.getChildCount(); i++) {
                DefaultMutableTreeNode groupNode = (DefaultMutableTreeNode) rootTreeNode.getChildAt(i);
                array.add(buildGroupJson(groupNode));
            }
            writer.write(array.toStringPretty());
        }
    }

    public void initRequestGroupsFromFile() {
        File file = new File(filePath);
        if (!file.exists()) { // 如果文件不存在，则创建默认请求组
            RequestCollectionsFactory.createDefaultRequestGroups(rootTreeNode, treeModel); // 创建默认请求组
            saveRequestGroups(); // 保存默认请求组到文件
            log.info("未找到请求组文件，已创建默认请求组");
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
            log.info("加载请求组完成");
        } catch (Exception e) {
            log.error("加载请求组失败", e);
            JOptionPane.showMessageDialog(null, "加载请求组失败: " + e.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void saveRequestGroups() {
        try (Writer writer = new OutputStreamWriter(new FileOutputStream(filePath), StandardCharsets.UTF_8)) {
            JSONArray array = new JSONArray();
            for (int i = 0; i < rootTreeNode.getChildCount(); i++) {
                DefaultMutableTreeNode groupNode = (DefaultMutableTreeNode) rootTreeNode.getChildAt(i);
                array.add(buildGroupJson(groupNode));
            }
            writer.write(array.toStringPretty());
            log.debug("保存内容为: {}", array.toStringPretty());
        } catch (Exception ex) {
            log.error("保存失败", ex);
        }
    }

    public DefaultMutableTreeNode parseGroupNode(JSONObject groupJson) {
        String name = groupJson.getStr("name");
        DefaultMutableTreeNode groupNode = new DefaultMutableTreeNode(new Object[]{"group", name});
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
        groupJson.set("name", obj[1]);
        JSONArray children = new JSONArray();
        for (int i = 0; i < node.getChildCount(); i++) {
            DefaultMutableTreeNode child = (DefaultMutableTreeNode) node.getChildAt(i);
            Object[] childObj = (Object[]) child.getUserObject();
            if ("group".equals(childObj[0])) {
                children.add(buildGroupJson(child));
            } else if ("request".equals(childObj[0])) {
                JSONObject reqJson = new JSONObject();
                reqJson.set("type", "request");
                reqJson.set("data", JSONUtil.parseObj(childObj[1]));
                children.add(reqJson);
            }
        }
        groupJson.set("children", children);
        return groupJson;
    }
}