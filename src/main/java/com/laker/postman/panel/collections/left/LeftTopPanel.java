package com.laker.postman.panel.collections.left;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.formdev.flatlaf.extras.components.FlatTextField;
import com.laker.postman.common.SingletonFactory;
import com.laker.postman.common.component.SearchTextField;
import com.laker.postman.common.dialog.LargeInputDialog;
import com.laker.postman.common.frame.MainFrame;
import com.laker.postman.common.panel.SingletonBasePanel;
import com.laker.postman.model.CurlRequest;
import com.laker.postman.model.HttpRequestItem;
import com.laker.postman.panel.collections.right.RequestEditPanel;
import com.laker.postman.service.curl.CurlParser;
import com.laker.postman.service.postman.PostmanImport;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreeNode;
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

import static com.laker.postman.panel.collections.left.RequestCollectionsLeftPanel.*;

@Slf4j
public class LeftTopPanel extends SingletonBasePanel {
    private FlatTextField searchField;

    @Override
    protected void initUI() {
        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
        setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5)); // 设置上下左右边距

        JButton importBtn = getImportBtn();
        JButton exportBtn = new JButton(new FlatSVGIcon("icons/export.svg", 20, 20));
        exportBtn.setFocusPainted(false);
        exportBtn.setBackground(Color.WHITE);
        exportBtn.setToolTipText(I18nUtil.getMessage(MessageKeys.COLLECTIONS_EXPORT_TOOLTIP));
        exportBtn.addActionListener(e -> exportRequestCollection());

        searchField = new SearchTextField();

        add(importBtn);
        add(exportBtn);
        add(searchField);
    }

    @Override
    protected void registerListeners() {
        // 搜索过滤逻辑
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            private void filterTree() {
                RequestCollectionsLeftPanel leftPanel = SingletonFactory.getInstance(RequestCollectionsLeftPanel.class);
                String text = searchField.getText().trim();
                if (text.isEmpty()) {
                    // 展开所有一级分组，显示全部
                    expandAll(leftPanel.getRequestTree(), false);
                    leftPanel.getTreeModel().setRoot(leftPanel.getRootTreeNode());
                    leftPanel.getTreeModel().reload();
                    return;
                }
                DefaultMutableTreeNode filteredRoot = new DefaultMutableTreeNode(ROOT);
                filterNodes(leftPanel.getRootTreeNode(), filteredRoot, text.toLowerCase());
                leftPanel.getTreeModel().setRoot(filteredRoot);
                leftPanel.getTreeModel().reload();
                expandAll(leftPanel.getRequestTree(), true);
            }

            @Override
            public void insertUpdate(DocumentEvent e) {
                filterTree();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                filterTree();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                filterTree();
            }
        });
    }


    public JButton getImportBtn() {
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

    private JPopupMenu getImportMenu() {
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
    private void importRequestCollection() {
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
    private void importPostmanCollection() {
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

    private void importCurlToCollection(String defaultCurl) {
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
            boolean saved = saveRequestWithGroupDialog(item);
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


    /**
     * 通过弹窗让用户选择分组和命名，保存 HttpRequestItem 到集合（公用方法，适用于cURL导入等场景）
     *
     * @param item 要保存的请求
     */
    private boolean saveRequestWithGroupDialog(HttpRequestItem item) {
        RequestCollectionsLeftPanel collectionPanel = SingletonFactory.getInstance(RequestCollectionsLeftPanel.class);
        RequestEditPanel requestEditPanel = SingletonFactory.getInstance(RequestEditPanel.class);
        TreeModel groupTreeModel = collectionPanel.getGroupTreeModel();
        Object[] result = requestEditPanel.showGroupAndNameDialog(groupTreeModel, item.getName());
        if (result == null) return false;
        Object[] groupObj = (Object[]) result[0];
        String requestName = (String) result[1];
        item.setName(requestName);
        item.setId(IdUtil.simpleUUID());
        collectionPanel.saveRequestToGroup(groupObj, item);
        requestEditPanel.showOrCreateTab(item); // 打开请求编辑tab
        // tree选中新增的请求节点
        collectionPanel.locateAndSelectRequest(item.getId());
        return true;
    }


    // 递归解析Postman集合为树结构，返回标准分组/请求节点列表
    private List<DefaultMutableTreeNode> parsePostmanItemsToTree(JSONArray items) {
        List<DefaultMutableTreeNode> nodeList = new ArrayList<>();
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

    // 导出请求集合到JSON文件
    private void exportRequestCollection() {
        RequestCollectionsLeftPanel leftPanel = SingletonFactory.getInstance(RequestCollectionsLeftPanel.class);
        MainFrame mainFrame = SingletonFactory.getInstance(MainFrame.class);
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle(I18nUtil.getMessage(MessageKeys.COLLECTIONS_EXPORT_DIALOG_TITLE));
        fileChooser.setSelectedFile(new File(EXPORT_FILE_NAME));
        int userSelection = fileChooser.showSaveDialog(mainFrame);
        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File fileToSave = fileChooser.getSelectedFile();
            try {
                leftPanel.getPersistence().exportRequestCollection(fileToSave);
                JOptionPane.showMessageDialog(mainFrame,
                        I18nUtil.getMessage(MessageKeys.COLLECTIONS_EXPORT_SUCCESS),
                        I18nUtil.getMessage(MessageKeys.GENERAL_TIP), JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                log.error("Export error", ex);
                JOptionPane.showMessageDialog(mainFrame,
                        I18nUtil.getMessage(MessageKeys.COLLECTIONS_EXPORT_FAIL, ex.getMessage()),
                        I18nUtil.getMessage(MessageKeys.GENERAL_ERROR), JOptionPane.ERROR_MESSAGE);
            }
        }
    }


    // 递归过滤节点
    private boolean filterNodes(DefaultMutableTreeNode src, DefaultMutableTreeNode dest, String keyword) {
        boolean matched = false;
        Object userObj = src.getUserObject();
        if (userObj instanceof Object[] obj) {
            String type = String.valueOf(obj[0]);
            if (GROUP.equals(type)) {
                String groupName = String.valueOf(obj[1]);
                DefaultMutableTreeNode groupNode = new DefaultMutableTreeNode(obj.clone());
                boolean childMatched = false;
                for (int i = 0; i < src.getChildCount(); i++) {
                    DefaultMutableTreeNode child = (DefaultMutableTreeNode) src.getChildAt(i);
                    if (filterNodes(child, groupNode, keyword)) {
                        childMatched = true;
                    }
                }
                if (groupName.toLowerCase().contains(keyword) || childMatched) {
                    dest.add(groupNode);
                    matched = true;
                }
            } else if (REQUEST.equals(type)) {
                HttpRequestItem item = (HttpRequestItem) obj[1];
                if (item.getName() != null && item.getName().toLowerCase().contains(keyword)) {
                    dest.add(new DefaultMutableTreeNode(obj.clone()));
                    matched = true;
                }
            }
        } else {
            // 处理 root 节点
            boolean childMatched = false;
            for (int i = 0; i < src.getChildCount(); i++) {
                DefaultMutableTreeNode child = (DefaultMutableTreeNode) src.getChildAt(i);
                if (filterNodes(child, dest, keyword)) {
                    childMatched = true;
                }
            }
            matched = childMatched;
        }
        return matched;
    }

    // 展开/收起所有节点
    private void expandAll(JTree tree, boolean expand) {
        TreeNode root = (TreeNode) tree.getModel().getRoot();
        expandAll(tree, new TreePath(root), expand);
    }

    private void expandAll(JTree tree, TreePath parent, boolean expand) {
        TreeNode node = (TreeNode) parent.getLastPathComponent();
        if (node.getChildCount() >= 0) {
            for (int i = 0; i < node.getChildCount(); i++) {
                TreeNode n = node.getChildAt(i);
                TreePath path = parent.pathByAddingChild(n);
                expandAll(tree, path, expand);
            }
        }
        if (expand) {
            tree.expandPath(parent);
        } else {
            tree.collapsePath(parent);
        }
    }
}