package com.laker.postman.panel.collections.left;

import cn.hutool.core.io.FileUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.laker.postman.common.SingletonFactory;
import com.laker.postman.common.dialog.LargeInputDialog;
import com.laker.postman.common.frame.MainFrame;
import com.laker.postman.model.CurlRequest;
import com.laker.postman.model.HttpRequestItem;
import com.laker.postman.panel.collections.right.RequestEditPanel;
import com.laker.postman.service.curl.CurlParser;
import com.laker.postman.service.postman.PostmanImport;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static com.laker.postman.panel.collections.left.RequestCollectionsLeftPanel.GROUP;
import static com.laker.postman.panel.collections.left.RequestCollectionsLeftPanel.REQUEST;

@Slf4j
public class ImportAndExportComponent {

    private ImportAndExportComponent() {

    }


    public static JButton getImportBtn() {
        // 使用SVG图标美化
        JButton importBtn = new JButton(new FlatSVGIcon("icons/import.svg", 20, 20));
        importBtn.setToolTipText(I18nUtil.getMessage(MessageKeys.COLLECTIONS_IMPORT_TOOLTIP));
        importBtn.setFocusPainted(false);
        importBtn.setBackground(Color.WHITE);
        // 合并导入菜单
        JPopupMenu importMenu = getImportMenu();
        importBtn.addActionListener(e -> {
            // 智能检测剪贴板内容
            String clipboardText = null;
            try {
                Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                Transferable t = clipboard.getContents(null);
                if (t != null && t.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                    clipboardText = (String) t.getTransferData(DataFlavor.stringFlavor);
                }
            } catch (Exception ignored) {
            }
            if (clipboardText != null && clipboardText.trim().toLowerCase().startsWith("curl")) {
                int result = JOptionPane.showConfirmDialog(SingletonFactory.getInstance(MainFrame.class),
                        I18nUtil.getMessage(MessageKeys.COLLECTIONS_IMPORT_CURL_DETECTED),
                        I18nUtil.getMessage(MessageKeys.COLLECTIONS_IMPORT_CURL_TITLE), JOptionPane.YES_NO_OPTION);
                if (result == JOptionPane.YES_OPTION) {
                    importCurlToCollection(clipboardText); // 自动填充
                    return;
                }
            }
            importMenu.show(importBtn, 0, importBtn.getHeight());
        });
        return importBtn;
    }

    private static JPopupMenu getImportMenu() {
        JPopupMenu importMenu = new JPopupMenu();
        JMenuItem importEasyToolsItem = new JMenuItem(I18nUtil.getMessage(MessageKeys.COLLECTIONS_IMPORT_EASY),
                new FlatSVGIcon("icons/easy.svg", 20, 20));
        importEasyToolsItem.addActionListener(e -> importRequestCollection());
        JMenuItem importPostmanItem = new JMenuItem(I18nUtil.getMessage(MessageKeys.COLLECTIONS_IMPORT_POSTMAN),
                new FlatSVGIcon("icons/postman.svg", 20, 20));
        importPostmanItem.addActionListener(e -> importPostmanCollection());
        JMenuItem importCurlItem = new JMenuItem(I18nUtil.getMessage(MessageKeys.COLLECTIONS_IMPORT_CURL),
                new FlatSVGIcon("icons/curl.svg", 20, 20));
        importCurlItem.addActionListener(e -> importCurlToCollection(null));
        importMenu.add(importEasyToolsItem);
        importMenu.add(importPostmanItem);
        importMenu.add(importCurlItem);
        return importMenu;
    }


    // 导入请求集合JSON文件
    private static void importRequestCollection() {
        RequestCollectionsLeftPanel leftPanel = SingletonFactory.getInstance(RequestCollectionsLeftPanel.class);
        MainFrame mainFrame = SingletonFactory.getInstance(MainFrame.class);
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle(I18nUtil.getMessage(MessageKeys.COLLECTIONS_IMPORT_DIALOG_TITLE));
        int userSelection = fileChooser.showOpenDialog(mainFrame);
        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File fileToOpen = fileChooser.getSelectedFile();
            try {
                // 导入时不清空老数据，而是全部加入到一个新分组下
                String groupName = "EasyPostman";
                DefaultMutableTreeNode easyPostmanGroup = leftPanel.findGroupNode(leftPanel.getRootTreeNode(), groupName);
                if (easyPostmanGroup == null) {
                    easyPostmanGroup = new DefaultMutableTreeNode(new Object[]{GROUP, groupName});
                    leftPanel.getRootTreeNode().add(easyPostmanGroup);
                }
                // 读取并解析文件
                JSONArray array = JSONUtil.readJSONArray(fileToOpen, java.nio.charset.StandardCharsets.UTF_8);
                for (Object o : array) {
                    JSONObject groupJson = (JSONObject) o;
                    DefaultMutableTreeNode groupNode = leftPanel.getPersistence().parseGroupNode(groupJson);
                    easyPostmanGroup.add(groupNode);
                }
                leftPanel.getTreeModel().reload();
                leftPanel.getPersistence().saveRequestGroups();
                leftPanel.getRequestTree().expandPath(new TreePath(easyPostmanGroup.getPath()));
                JOptionPane.showMessageDialog(mainFrame,
                        I18nUtil.getMessage(MessageKeys.COLLECTIONS_IMPORT_SUCCESS),
                        I18nUtil.getMessage(MessageKeys.GENERAL_TIP), JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                log.error("Import error", ex);
                JOptionPane.showMessageDialog(mainFrame,
                        I18nUtil.getMessage(MessageKeys.COLLECTIONS_IMPORT_FAIL, ex.getMessage()),
                        I18nUtil.getMessage(MessageKeys.GENERAL_ERROR), JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    // 导入Postman集合
    private static void importPostmanCollection() {
        RequestCollectionsLeftPanel leftPanel = SingletonFactory.getInstance(RequestCollectionsLeftPanel.class);
        MainFrame mainFrame = SingletonFactory.getInstance(MainFrame.class);
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle(I18nUtil.getMessage(MessageKeys.COLLECTIONS_IMPORT_POSTMAN_DIALOG_TITLE));
        int userSelection = fileChooser.showOpenDialog(mainFrame);
        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File fileToOpen = fileChooser.getSelectedFile();
            try {
                String json = FileUtil.readString(fileToOpen, StandardCharsets.UTF_8);
                JSONObject postmanRoot = JSONUtil.parseObj(json);
                if (postmanRoot.containsKey("info") && postmanRoot.containsKey("item")) {
                    // 解析 collection 名称
                    String collectionName = postmanRoot.getJSONObject("info").getStr("name", "Postman");
                    JSONArray items = postmanRoot.getJSONArray("item");
                    DefaultMutableTreeNode collectionNode = new DefaultMutableTreeNode(new Object[]{GROUP, collectionName});
                    java.util.List<DefaultMutableTreeNode> children = parsePostmanItemsToTree(items);
                    for (DefaultMutableTreeNode child : children) {
                        collectionNode.add(child);
                    }
                    leftPanel.getRootTreeNode().add(collectionNode);
                    leftPanel.getTreeModel().reload();
                    leftPanel.getPersistence().saveRequestGroups();
                    leftPanel.getRequestTree().expandPath(new TreePath(collectionNode.getPath()));
                    JOptionPane.showMessageDialog(mainFrame,
                            I18nUtil.getMessage(MessageKeys.COLLECTIONS_IMPORT_SUCCESS),
                            I18nUtil.getMessage(MessageKeys.GENERAL_TIP), JOptionPane.INFORMATION_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(mainFrame,
                            I18nUtil.getMessage(MessageKeys.COLLECTIONS_IMPORT_POSTMAN_INVALID),
                            I18nUtil.getMessage(MessageKeys.GENERAL_ERROR), JOptionPane.ERROR_MESSAGE);
                }
            } catch (Exception ex) {
                log.error("Import error", ex);
                JOptionPane.showMessageDialog(mainFrame,
                        I18nUtil.getMessage(MessageKeys.COLLECTIONS_IMPORT_FAIL, ex.getMessage()),
                        I18nUtil.getMessage(MessageKeys.GENERAL_ERROR), JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private static void importCurlToCollection(String defaultCurl) {
        MainFrame mainFrame = SingletonFactory.getInstance(MainFrame.class);
        String curlText = LargeInputDialog.show(mainFrame,
                I18nUtil.getMessage(MessageKeys.COLLECTIONS_IMPORT_CURL_DIALOG_TITLE),
                I18nUtil.getMessage(MessageKeys.COLLECTIONS_IMPORT_CURL_DIALOG_PROMPT), defaultCurl);
        if (curlText == null || curlText.trim().isEmpty()) return;
        try {
            CurlRequest curlRequest = CurlParser.parse(curlText);
            if (curlRequest.url == null) {
                JOptionPane.showMessageDialog(mainFrame,
                        I18nUtil.getMessage(MessageKeys.COLLECTIONS_IMPORT_CURL_PARSE_FAIL),
                        I18nUtil.getMessage(MessageKeys.GENERAL_ERROR), JOptionPane.ERROR_MESSAGE);
                return;
            }
            // 构造HttpRequestItem
            HttpRequestItem item = new HttpRequestItem();
            item.setName(curlRequest.url);
            item.setUrl(curlRequest.url);
            item.setMethod(curlRequest.method);
            item.setHeaders(curlRequest.headers);
            item.setBody(curlRequest.body);
            item.setParams(curlRequest.params);
            item.setFormData(curlRequest.formData);
            item.setFormFiles(curlRequest.formFiles);
            // 统一用RequestEditPanel弹窗选择分组和命名
            boolean saved = SingletonFactory.getInstance(RequestEditPanel.class).saveRequestWithGroupDialog(item);
            // 导入成功后清空剪贴板
            if (saved) {
                Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(""), null);
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(mainFrame,
                    I18nUtil.getMessage(MessageKeys.COLLECTIONS_IMPORT_CURL_PARSE_ERROR, ex.getMessage()),
                    I18nUtil.getMessage(MessageKeys.GENERAL_ERROR), JOptionPane.ERROR_MESSAGE);
        }
    }


    // 递归解析Postman集合为树结构，返回标准分组/请求节点列表
    private static java.util.List<DefaultMutableTreeNode> parsePostmanItemsToTree(JSONArray items) {
        java.util.List<DefaultMutableTreeNode> nodeList = new ArrayList<>();
        for (Object obj : items) {
            JSONObject item = (JSONObject) obj;
            if (item.containsKey("item")) {
                // 文件夹节点
                String folderName = item.getStr("name", "default group");
                DefaultMutableTreeNode folderNode = new DefaultMutableTreeNode(new Object[]{GROUP, folderName});
                // 先处理自身 request
                if (item.containsKey(REQUEST)) {
                    HttpRequestItem req = PostmanImport.parsePostmanSingleItem(item);
                    folderNode.add(new DefaultMutableTreeNode(new Object[]{REQUEST, req}));
                }
                // 递归处理子节点
                JSONArray children = item.getJSONArray("item");
                List<DefaultMutableTreeNode> childNodes = parsePostmanItemsToTree(children);
                for (DefaultMutableTreeNode child : childNodes) {
                    folderNode.add(child);
                }
                nodeList.add(folderNode);
            } else if (item.containsKey(REQUEST)) {
                // 纯请求节点
                HttpRequestItem req = PostmanImport.parsePostmanSingleItem(item);
                nodeList.add(new DefaultMutableTreeNode(new Object[]{REQUEST, req}));
            }
        }
        return nodeList;
    }
}