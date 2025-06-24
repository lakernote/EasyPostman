package com.laker.postman.panel.collections;

import cn.hutool.core.io.FileUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.laker.postman.common.panel.BasePanel;
import com.laker.postman.common.dialog.LargeInputDialog;
import com.laker.postman.common.tab.ClosableTabComponent;
import com.laker.postman.common.tree.RequestTreeCellRenderer;
import com.laker.postman.common.tree.TreeTransferHandler;
import com.laker.postman.model.CurlRequest;
import com.laker.postman.model.HttpRequestItem;
import com.laker.postman.panel.collections.edit.RequestEditPanel;
import com.laker.postman.panel.collections.edit.RequestEditSubPanel;
import com.laker.postman.service.HttpService;
import com.laker.postman.service.RequestCollectionPersistence;
import com.laker.postman.util.CurlParser;
import com.laker.postman.util.PostmanImport;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static com.laker.postman.util.SystemUtil.COLLECTION_PATH;

/**
 * 请求集合面板，展示所有请求分组和请求项
 * 文件路径：用户目录下的 easy-tools/requestCollection.json
 * 支持请求的增删改查、分组管理、拖拽排序等功能
 */
@Slf4j
public class RequestCollectionsSubPanel extends BasePanel {
    // 请求集合的根节点
    private DefaultMutableTreeNode rootTreeNode;
    // 请求树组件
    private JTree requestTree;
    // 树模型，用于管理树节点
    private DefaultTreeModel treeModel;

    private JTextField searchField;

    private RequestCollectionPersistence persistence;

    @Override
    protected void initUI() {
        setLayout(new BorderLayout());
        setPreferredSize(new Dimension(200, 200));

        // 顶部面板，导入导出按钮在最上方，环境信息在下方
        JPanel topPanel = getTopPanel();
        add(topPanel, BorderLayout.NORTH);

        JScrollPane treeScrollPane = getTreeScrollPane();
        add(treeScrollPane, BorderLayout.CENTER);

        JPanel btnPanel = getBtnPanel();
        add(btnPanel, BorderLayout.SOUTH);
    }

    private JPanel getBtnPanel() {
        // 按钮面板
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4)); // 增加间距
        JButton addGroupBtn = new JButton(new FlatSVGIcon("icons/plus.svg", 20, 20));
        addGroupBtn.setText("Group");
        addGroupBtn.addActionListener(e -> {
            String groupName = JOptionPane.showInputDialog(this, "请输入组名：");
            if (groupName != null && !groupName.trim().isEmpty()) {
                DefaultMutableTreeNode groupNode = new DefaultMutableTreeNode(new Object[]{"group", groupName});
                rootTreeNode.add(groupNode);
                treeModel.reload();
                saveRequestGroups();
            }
        });

        JButton saveBtn = new JButton(new FlatSVGIcon("icons/save.svg", 20, 20));
        saveBtn.setText("Save");
        saveBtn.addActionListener(e -> {
            RequestEditPanel.getInstance().saveCurrentRequest();
        });

        JButton exportBtn = new JButton(new FlatSVGIcon("icons/download.svg", 20, 20));
        exportBtn.setText("Export");
        exportBtn.setToolTipText("导出请求集合为本地文件");
        exportBtn.setFocusPainted(false);
        exportBtn.setBackground(Color.WHITE);
        exportBtn.setIconTextGap(6);
        exportBtn.addActionListener(e -> exportRequestCollection());

        btnPanel.add(addGroupBtn);
        btnPanel.add(saveBtn);
        btnPanel.add(exportBtn);
        return btnPanel;
    }

    private JScrollPane getTreeScrollPane() {
        // 初始化请求树
        rootTreeNode = new DefaultMutableTreeNode("-请求集合-");
        treeModel = new DefaultTreeModel(rootTreeNode);
        // 初始化持久化工具
        persistence = new RequestCollectionPersistence(COLLECTION_PATH, rootTreeNode, treeModel);
        // 创建树组件
        requestTree = new JTree(treeModel) {
            @Override
            public boolean isPathEditable(TreePath path) {
                // 禁止根节点重命名
                Object node = path.getLastPathComponent();
                if (node instanceof DefaultMutableTreeNode treeNode) {
                    return treeNode.getParent() != null;
                }
                return false;
            }
        };
        // 不显示根节点
        requestTree.setRootVisible(false);
        // 让 JTree 组件显示根节点的“展开/收起”小三角（即树形结构的手柄）。
        requestTree.setShowsRootHandles(true);
        // 设置树的字体和行高
        requestTree.setCellRenderer(new RequestTreeCellRenderer());
        requestTree.setRowHeight(28);
        requestTree.setBackground(new Color(245, 247, 250));
        JScrollPane treeScrollPane = new JScrollPane(requestTree);
        treeScrollPane.getVerticalScrollBar().setUnitIncrement(16);
        treeScrollPane.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Color.LIGHT_GRAY));
        // 启用拖拽排序
        requestTree.setDragEnabled(true); // 启用拖拽
        requestTree.setDropMode(DropMode.ON_OR_INSERT); // 设置拖拽模式为插入
        requestTree.setTransferHandler(
                new TreeTransferHandler(requestTree, treeModel, this::saveRequestGroups)
        );
        return treeScrollPane;
    }

    private JPanel getTopPanel() {
        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.X_AXIS)); // 水平布局
        JPanel importExportPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 5)); // 居中布局
        // 导入按钮
        JButton importBtn = getImportBtn();
        // 搜索过滤输入框
        getSearchField();

        importExportPanel.add(importBtn);
        importExportPanel.add(searchField);

        topPanel.add(importExportPanel);
        return topPanel;
    }

    private void getSearchField() {
        searchField = new JTextField();
        searchField.setToolTipText("搜索分组或请求名称");
        searchField.setPreferredSize(new Dimension(180, 28));
        searchField.setMaximumSize(new Dimension(180, 28));
    }

    private JButton getImportBtn() {
        // 使用SVG图标美化
        JButton importBtn = new JButton(new FlatSVGIcon("icons/upload.svg", 20, 20));
        importBtn.setText("Import");
        importBtn.setToolTipText("导入请求集合或请求项");
        importBtn.setFocusPainted(false);
        importBtn.setBackground(Color.WHITE);
        importBtn.setIconTextGap(6);
        // 合并导入菜单
        JPopupMenu importMenu = new JPopupMenu();
        JMenuItem importEasyToolsItem = new JMenuItem("Import from EasyPostman", new FlatSVGIcon("icons/easy.svg", 20, 20));
        importEasyToolsItem.setToolTipText("Import collections exported by EasyTools");
        importEasyToolsItem.addActionListener(e -> importRequestCollection());
        JMenuItem importPostmanItem = new JMenuItem("Import from Postman", new FlatSVGIcon("icons/postman.svg", 20, 20));
        importPostmanItem.setToolTipText("Import collections exported by Postman");
        importPostmanItem.addActionListener(e -> importPostmanCollection());
        JMenuItem importCurlItem = new JMenuItem("Import from cURL", new FlatSVGIcon("icons/curl.svg", 20, 20));
        importCurlItem.setToolTipText("Paste cURL command to import request");
        importCurlItem.addActionListener(e -> importCurlToCollection());
        importMenu.add(importEasyToolsItem);
        importMenu.add(importPostmanItem);
        importMenu.add(importCurlItem);
        importBtn.addActionListener(e -> importMenu.show(importBtn, 0, importBtn.getHeight()));
        return importBtn;
    }

    @Override
    protected void registerListeners() {

        // 点击事件：加载请求（切换节点时）
        requestTree.addTreeSelectionListener(e -> {
            DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) requestTree.getLastSelectedPathComponent();
            if (selectedNode != null && selectedNode.getUserObject() instanceof Object[] obj) {
                if ("request".equals(obj[0])) {
                    HttpRequestItem item = (HttpRequestItem) obj[1];
                    RequestEditPanel.getInstance().showOrCreateTab(item); // 修改为智能Tab切换/新建
                }
            }
        });

        // 鼠标点击事件：无论是否切换都触发（解决重复点击同一节点无响应问题）
        requestTree.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int selRow = requestTree.getRowForLocation(e.getX(), e.getY());
                TreePath selPath = requestTree.getPathForLocation(e.getX(), e.getY());
                if (selRow != -1 && selPath != null) {
                    DefaultMutableTreeNode node = (DefaultMutableTreeNode) selPath.getLastPathComponent();
                    if (node.getUserObject() instanceof Object[] obj) {
                        if ("request".equals(obj[0])) {
                            HttpRequestItem item = (HttpRequestItem) obj[1];
                            RequestEditPanel.getInstance().showOrCreateTab(item);
                        }
                    }
                }
            }

            @Override
            public void mousePressed(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    int x = e.getX();
                    int y = e.getY();
                    // 获取鼠标点击位置的行号
                    int row = requestTree.getClosestRowForLocation(x, y);
                    if (row != -1) {
                        requestTree.setSelectionRow(row);
                        showPopupMenu(x, y);
                    }
                }
            }

            private void showPopupMenu(int x, int y) {
                JPopupMenu menu = new JPopupMenu();
                DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) requestTree.getLastSelectedPathComponent();
                Object userObj = selectedNode != null ? selectedNode.getUserObject() : null;
                // 根节点不显示任何操作
                if (selectedNode == rootTreeNode) {
                    return;
                }
                // 仅分组节点可新增文件/请求
                if (userObj instanceof Object[] && "group".equals(((Object[]) userObj)[0])) {
                    JMenuItem addGroupItem = new JMenuItem("Add Group");
                    addGroupItem.addActionListener(e -> addGroupUnderSelected());
                    menu.add(addGroupItem);
                    JMenuItem addRequestItem = new JMenuItem("Add Request");
                    addRequestItem.addActionListener(e -> addRequestUnderSelected());
                    menu.add(addRequestItem);
                    JMenuItem duplicateGroupItem = new JMenuItem("Duplicate");
                    duplicateGroupItem.addActionListener(e -> duplicateSelectedGroup());
                    menu.add(duplicateGroupItem);
                    // 导出为Postman
                    JMenuItem exportPostmanItem = new JMenuItem("Export as Postman v2.1");
                    exportPostmanItem.addActionListener(e -> exportGroupAsPostman(selectedNode));
                    menu.add(exportPostmanItem);
                    menu.addSeparator();
                }
                // 请求节点右键菜单增加“复制”
                if (userObj instanceof Object[] && "request".equals(((Object[]) userObj)[0])) {
                    JMenuItem duplicateItem = new JMenuItem("Duplicate");
                    duplicateItem.addActionListener(e -> duplicateSelectedRequest());
                    menu.add(duplicateItem);
                    // 复制为cURL命令
                    JMenuItem copyAsCurlItem = new JMenuItem("Copy as cUrl");
                    copyAsCurlItem.addActionListener(e -> copySelectedRequestAsCurl());
                    menu.add(copyAsCurlItem);
                    menu.addSeparator();
                }
                // 只有非根节点才显示重命名/删除
                if (selectedNode != rootTreeNode) {
                    JMenuItem renameItem = new JMenuItem("Rename");
                    JMenuItem deleteItem = new JMenuItem("Delete");
                    renameItem.addActionListener(e -> renameSelectedItem());
                    deleteItem.addActionListener(e -> deleteSelectedItem());
                    menu.add(renameItem);
                    menu.add(deleteItem);
                }
                menu.show(requestTree, x, y);
            }

            private void renameSelectedItem() {
                DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) requestTree.getLastSelectedPathComponent();
                if (selectedNode == null) return;

                Object userObj = selectedNode.getUserObject();
                if (userObj instanceof Object[] obj) {
                    if ("group".equals(obj[0])) {
                        String newName = JOptionPane.showInputDialog("输入新的组名：", obj[1]);
                        if (newName != null && !newName.isEmpty()) {
                            obj[1] = newName;
                            treeModel.nodeChanged(selectedNode);
                            saveRequestGroups();
                        }
                    } else if ("request".equals(obj[0])) {
                        HttpRequestItem item = (HttpRequestItem) obj[1];
                        String oldName = item.getName();
                        String newName = JOptionPane.showInputDialog("输入新的请求名：", oldName);
                        if (newName != null && !newName.isEmpty()) {
                            item.setName(newName);
                            treeModel.nodeChanged(selectedNode);
                            saveRequestGroups();
                            // 同步更新已打开Tab的标题
                            RequestEditPanel editPanel = RequestEditPanel.getInstance();
                            JTabbedPane tabbedPane = editPanel.getTabbedPane();
                            for (int i = 0; i < tabbedPane.getTabCount(); i++) {
                                Component comp = tabbedPane.getComponentAt(i);
                                if (comp instanceof RequestEditSubPanel subPanel) {
                                    HttpRequestItem tabItem = subPanel.getCurrentRequest();
                                    if (tabItem != null && item.getId().equals(tabItem.getId())) {
                                        tabbedPane.setTitleAt(i, newName);
                                        // 更新自定义标签组件
                                        tabbedPane.setTabComponentAt(i, new ClosableTabComponent(newName, subPanel, tabbedPane, editPanel::saveCurrentRequest));
                                        // 同步刷新内容
                                        subPanel.updateRequestForm(item);
                                    }
                                }
                            }
                        }
                    }
                }
            }

            private void deleteSelectedItem() {
                DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) requestTree.getLastSelectedPathComponent();
                if (selectedNode != null && selectedNode.getParent() != null) {
                    // 先关闭相关Tab
                    Object userObj = selectedNode.getUserObject();
                    RequestEditPanel editPanel = RequestEditPanel.getInstance();
                    JTabbedPane tabbedPane = editPanel.getTabbedPane();
                    if (userObj instanceof Object[] obj) {
                        if ("request".equals(obj[0])) {
                            HttpRequestItem item = (HttpRequestItem) obj[1];
                            // 关闭所有与该请求id匹配的Tab
                            for (int i = tabbedPane.getTabCount() - 1; i >= 0; i--) {
                                Component comp = tabbedPane.getComponentAt(i);
                                if (comp instanceof RequestEditSubPanel subPanel) {
                                    HttpRequestItem tabItem = subPanel.getCurrentRequest();
                                    if (tabItem != null && item.getId().equals(tabItem.getId())) {
                                        tabbedPane.remove(i);
                                    }
                                }
                            }
                            if (tabbedPane.getTabCount() > 1) {
                                tabbedPane.setSelectedIndex(tabbedPane.getTabCount() - 2);
                            }
                        } else if ("group".equals(obj[0])) {
                            // 递归关闭该组下所有请求Tab
                            closeTabsForGroup(selectedNode, tabbedPane);
                            if (tabbedPane.getTabCount() > 1) {
                                tabbedPane.setSelectedIndex(tabbedPane.getTabCount() - 2);
                            }
                        }
                    }
                    // 删除树节点
                    DefaultMutableTreeNode parent = (DefaultMutableTreeNode) selectedNode.getParent();
                    parent.remove(selectedNode);
                    treeModel.reload();
                    // 删除后保持父节点展开
                    TreePath parentPath = new TreePath(parent.getPath());
                    requestTree.expandPath(parentPath);
                    saveRequestGroups();
                }
            }

            // 递归关闭分组下所有请求Tab
            private void closeTabsForGroup(DefaultMutableTreeNode groupNode, JTabbedPane tabbedPane) {
                for (int i = 0; i < groupNode.getChildCount(); i++) {
                    DefaultMutableTreeNode child = (DefaultMutableTreeNode) groupNode.getChildAt(i);
                    Object userObj = child.getUserObject();
                    if (userObj instanceof Object[] obj) {
                        if ("request".equals(obj[0])) {
                            HttpRequestItem item = (HttpRequestItem) obj[1];
                            for (int j = tabbedPane.getTabCount() - 1; j >= 0; j--) {
                                Component comp = tabbedPane.getComponentAt(j);
                                if (comp instanceof RequestEditSubPanel subPanel) {
                                    HttpRequestItem tabItem = subPanel.getCurrentRequest();
                                    if (tabItem != null && item.getId().equals(tabItem.getId())) {
                                        tabbedPane.remove(j);
                                    }
                                }
                            }
                        } else if ("group".equals(obj[0])) {
                            closeTabsForGroup(child, tabbedPane);
                        }
                    }
                }
            }
        });

        // 搜索过滤逻辑
        searchField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            private void filterTree() {
                String text = searchField.getText().trim();
                if (text.isEmpty()) {
                    // 展开所有一级分组，显示全部
                    expandAll(requestTree, false);
                    treeModel.setRoot(rootTreeNode);
                    treeModel.reload();
                    return;
                }
                DefaultMutableTreeNode filteredRoot = new DefaultMutableTreeNode("-请求集合-");
                filterNodes(rootTreeNode, filteredRoot, text.toLowerCase());
                treeModel.setRoot(filteredRoot);
                treeModel.reload();
                expandAll(requestTree, true);
            }

            @Override
            public void insertUpdate(javax.swing.event.DocumentEvent e) {
                filterTree();
            }

            @Override
            public void removeUpdate(javax.swing.event.DocumentEvent e) {
                filterTree();
            }

            @Override
            public void changedUpdate(javax.swing.event.DocumentEvent e) {
                filterTree();
            }
        });

        SwingUtilities.invokeLater(() -> {
            persistence.initRequestGroupsFromFile(this::createDefaultRequestGroups);
        }); // 异步加载请求组

    }

    // 导出请求集合到JSON文件
    private void exportRequestCollection() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("导出请求集合");
        fileChooser.setSelectedFile(new File("EasyPostman-Collections.json"));
        int userSelection = fileChooser.showSaveDialog(this);
        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File fileToSave = fileChooser.getSelectedFile();
            try {
                persistence.exportRequestCollection(fileToSave);
                JOptionPane.showMessageDialog(this, "导出成功！", "提示", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                log.error("导出失败", ex);
                JOptionPane.showMessageDialog(this, "导出失败: " + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    // 导入请求集合JSON文件
    private void importRequestCollection() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("导入请求集合");
        int userSelection = fileChooser.showOpenDialog(this);
        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File fileToOpen = fileChooser.getSelectedFile();
            try {
                // 导入时不清空老数据，而是全部加入到一个新分组下
                String groupName = "EasyPostman";
                DefaultMutableTreeNode easyPostmanGroup = findGroupNode(rootTreeNode, groupName);
                if (easyPostmanGroup == null) {
                    easyPostmanGroup = new DefaultMutableTreeNode(new Object[]{"group", groupName});
                    rootTreeNode.add(easyPostmanGroup);
                }
                // 读取并解析文件
                JSONArray array = JSONUtil.readJSONArray(fileToOpen, java.nio.charset.StandardCharsets.UTF_8);
                for (Object o : array) {
                    JSONObject groupJson = (JSONObject) o;
                    DefaultMutableTreeNode groupNode = persistence.parseGroupNode(groupJson);
                    easyPostmanGroup.add(groupNode);
                }
                treeModel.reload();
                persistence.saveRequestGroups();
                requestTree.expandPath(new TreePath(easyPostmanGroup.getPath()));
                JOptionPane.showMessageDialog(this, "导入成功！", "提示", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                log.error("导入失败", ex);
                JOptionPane.showMessageDialog(this, "导入失败: " + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    // 新增：导入Postman集合
    private void importPostmanCollection() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("从Postman导入");
        int userSelection = fileChooser.showOpenDialog(this);
        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File fileToOpen = fileChooser.getSelectedFile();
            try {
                String json = FileUtil.readString(fileToOpen, StandardCharsets.UTF_8);
                JSONObject postmanRoot = JSONUtil.parseObj(json);
                if (postmanRoot.containsKey("info") && postmanRoot.containsKey("item")) {
                    // 解析 collection 名称
                    String collectionName = postmanRoot.getJSONObject("info").getStr("name", "Postman");
                    JSONArray items = postmanRoot.getJSONArray("item");
                    DefaultMutableTreeNode collectionNode = new DefaultMutableTreeNode(new Object[]{"group", collectionName});
                    java.util.List<DefaultMutableTreeNode> children = parsePostmanItemsToTree(items);
                    for (DefaultMutableTreeNode child : children) {
                        collectionNode.add(child);
                    }
                    rootTreeNode.add(collectionNode);
                    treeModel.reload();
                    persistence.saveRequestGroups();
                    requestTree.expandPath(new TreePath(collectionNode.getPath()));
                    JOptionPane.showMessageDialog(this, "导入成功！", "提示", JOptionPane.INFORMATION_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(this, "不是有效的Postman集合文件", "错误", JOptionPane.ERROR_MESSAGE);
                }
            } catch (Exception ex) {
                log.error("Postman导入失败", ex);
                JOptionPane.showMessageDialog(this, "导入失败: " + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    // 新增：cURL导入逻辑

    /**
     * 从cURL命令导入请求项到集合
     */
    private void importCurlToCollection() {
        String curlText = LargeInputDialog.show(this, "从cURL导入", "请输入cURL命令:");
        if (curlText == null || curlText.trim().isEmpty()) return;
        try {
            CurlRequest curlRequest = CurlParser.parse(curlText);
            if (curlRequest.url == null) {
                JOptionPane.showMessageDialog(this, "无法解析cURL命令", "错误", JOptionPane.ERROR_MESSAGE);
                return;
            }
            // 构造HttpRequestItem
            HttpRequestItem item = new HttpRequestItem();
            item.setId(UUID.randomUUID().toString());
            item.setName(curlRequest.url);
            item.setUrl(curlRequest.url);
            item.setMethod(curlRequest.method);
            item.setHeaders(curlRequest.headers);
            item.setBody(curlRequest.body);
            item.setParams(curlRequest.params);
            item.setFormData(curlRequest.formData);
            item.setFormFiles(curlRequest.formFiles);
            // 导入到默认分组
            DefaultMutableTreeNode defaultGroupNode = findGroupNode(rootTreeNode, "cURL");
            if (defaultGroupNode == null) {
                defaultGroupNode = new DefaultMutableTreeNode(new Object[]{"group", "cURL"});
                rootTreeNode.add(defaultGroupNode);
            }
            DefaultMutableTreeNode reqNode = new DefaultMutableTreeNode(new Object[]{"request", item});
            defaultGroupNode.add(reqNode);
            treeModel.reload();
            persistence.saveRequestGroups();
            requestTree.expandPath(new TreePath(defaultGroupNode.getPath()));
            JOptionPane.showMessageDialog(this, "cURL已成功导入！", "成功", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "解析cURL出错: " + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void addGroupUnderSelected() {
        DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) requestTree.getLastSelectedPathComponent();
        if (selectedNode == null) return;
        String groupName = JOptionPane.showInputDialog(this, "请输入文件夹名称：");
        if (groupName != null && !groupName.trim().isEmpty()) {
            DefaultMutableTreeNode groupNode = new DefaultMutableTreeNode(new Object[]{"group", groupName});
            selectedNode.add(groupNode);
            treeModel.reload(selectedNode);
            requestTree.expandPath(new TreePath(selectedNode.getPath()));
            persistence.saveRequestGroups();
        }
    }

    private void addRequestUnderSelected() {
        DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) requestTree.getLastSelectedPathComponent();
        if (selectedNode == null) return;
        DefaultMutableTreeNode reqNode = new DefaultMutableTreeNode(new Object[]{"request", HttpService.createDefaultRequest()});
        selectedNode.add(reqNode);
        treeModel.reload(selectedNode);
        requestTree.expandPath(new TreePath(selectedNode.getPath()));
        persistence.saveRequestGroups();
    }

    // 创建默认请求组和测试请求
    private void createDefaultRequestGroups() {
        log.info("创建默认请求组和测试请求");
        try {
            DefaultMutableTreeNode defaultGroupNode = new DefaultMutableTreeNode(new Object[]{"group", "Default Group"});
            rootTreeNode.add(defaultGroupNode);
            // 经典的测试请求示例
            HttpRequestItem example = HttpService.createDefaultRequest();
            example.setName("环境变量+脚本示例");
            example.setMethod("GET");
            example.setUrl("{{baseUrl}}?q=lakernote");
            example.getParams().put("q", "lakernote");
            example.setPrescript("console.log('这是一个预请求脚本');");
            example.setPostscript("console.log('这是一个后置脚本');");
            defaultGroupNode.add(new DefaultMutableTreeNode(new Object[]{"request", example}));

            // GET 示例
            DefaultMutableTreeNode getNode = new DefaultMutableTreeNode(new Object[]{"request", HttpService.createDefaultRequest()});
            defaultGroupNode.add(getNode);

            // POST JSON 示例
            HttpRequestItem postJson = HttpService.createDefaultRequest();
            postJson.setName("POST-JSON 示例");
            postJson.setMethod("POST");
            postJson.setUrl("https://httpbin.org/post");
            postJson.getHeaders().put("Content-Type", "application/json");
            postJson.setBody("{\"name\":\"EasyTools\",\"type\":\"json\"}");
            defaultGroupNode.add(new DefaultMutableTreeNode(new Object[]{"request", postJson}));

            // POST form-data 示例
            HttpRequestItem postFormData = HttpService.createDefaultRequest();
            postFormData.setName("POST-form-data 示例");
            postFormData.setMethod("POST");
            postFormData.setUrl("https://httpbin.org/post");
            postFormData.getHeaders().put("Content-Type", "multipart/form-data");
            postFormData.getFormData().put("key1", "value1");
            postFormData.getFormData().put("key2", "value2");
            defaultGroupNode.add(new DefaultMutableTreeNode(new Object[]{"request", postFormData}));

            // POST x-www-form-urlencoded 示例
            HttpRequestItem postUrl = HttpService.createDefaultRequest();
            postUrl.setName("POST-x-www-form-urlencoded 示例");
            postUrl.setMethod("POST");
            postUrl.setUrl("https://httpbin.org/post");
            postUrl.getHeaders().put("Content-Type", "application/x-www-form-urlencoded");
            postUrl.setBody("key1=value1&key2=value2");
            defaultGroupNode.add(new DefaultMutableTreeNode(new Object[]{"request", postUrl}));

            // PUT 示例
            HttpRequestItem put = HttpService.createDefaultRequest();
            put.setName("PUT 示例");
            put.setMethod("PUT");
            put.setUrl("https://httpbin.org/put");
            put.getHeaders().put("Content-Type", "application/json");
            put.setBody("{\"update\":true}");
            defaultGroupNode.add(new DefaultMutableTreeNode(new Object[]{"request", put}));

            // DELETE 示例
            HttpRequestItem delete = HttpService.createDefaultRequest();
            delete.setName("DELETE 示例");
            delete.setMethod("DELETE");
            delete.setUrl("https://httpbin.org/delete");
            defaultGroupNode.add(new DefaultMutableTreeNode(new Object[]{"request", delete}));

            treeModel.reload();
            persistence.saveRequestGroups();
            requestTree.expandPath(new TreePath(defaultGroupNode.getPath()));
        } catch (Exception ex) {
            log.error("创建默认请求组失败", ex);
        }
    }

    private void saveRequestGroups() {
        persistence.saveRequestGroups();
    }

    /**
     * 将请求保存到指定分组
     *
     * @param group 分组信息，[type, name] 形式的数组
     * @param item  请求项
     */
    public void saveRequestToGroup(Object[] group, HttpRequestItem item) {
        if (group == null || !"group".equals(group[0])) {
            return;
        }
        DefaultMutableTreeNode groupNode = findGroupNode(rootTreeNode, (String) group[1]);
        if (groupNode == null) {
            return;
        }
        DefaultMutableTreeNode requestNode = new DefaultMutableTreeNode(new Object[]{"request", item});
        groupNode.add(requestNode);
        treeModel.reload(groupNode);
        requestTree.expandPath(new TreePath(groupNode.getPath()));
        persistence.saveRequestGroups();
    }

    /**
     * 根据名称查找分组节点
     */
    private DefaultMutableTreeNode findGroupNode(DefaultMutableTreeNode node, String groupName) {
        if (node == null) return null;

        Object userObj = node.getUserObject();
        if (userObj instanceof Object[] obj) {
            if ("group".equals(obj[0]) && groupName.equals(obj[1])) {
                return node;
            }
        }

        for (int i = 0; i < node.getChildCount(); i++) {
            DefaultMutableTreeNode child = (DefaultMutableTreeNode) node.getChildAt(i);
            DefaultMutableTreeNode result = findGroupNode(child, groupName);
            if (result != null) {
                return result;
            }
        }

        return null;
    }

    /**
     * 更新已存在的请求
     *
     * @param item 请求项
     * @return 是否更新成功
     */
    public boolean updateExistingRequest(HttpRequestItem item) {
        if (item == null || item.getId() == null || item.getId().isEmpty()) {
            return false;
        }
        DefaultMutableTreeNode requestNode = findRequestNodeById(rootTreeNode, item.getId());
        if (requestNode == null) {
            return false;
        }
        Object[] userObj = (Object[]) requestNode.getUserObject();
        HttpRequestItem originalItem = (HttpRequestItem) userObj[1];
        String originalName = originalItem.getName();
        item.setName(originalName);
        userObj[1] = item;
        treeModel.nodeChanged(requestNode);
        persistence.saveRequestGroups();
        // 新增：保存后去除Tab红点
        SwingUtilities.invokeLater(() -> {
            RequestEditPanel editPanel = RequestEditPanel.getInstance();
            JTabbedPane tabbedPane = editPanel.getTabbedPane();
            for (int i = 0; i < tabbedPane.getTabCount(); i++) {
                Component comp = tabbedPane.getComponentAt(i);
                if (comp instanceof RequestEditSubPanel subPanel) {
                    HttpRequestItem tabItem = subPanel.getCurrentRequest();
                    if (tabItem != null && item.getId().equals(tabItem.getId())) {
                        editPanel.updateTabDirty(subPanel, false);
                        subPanel.updateTablesBorder(false);
                        subPanel.setOriginalRequestItem(item);
                    }
                }
            }
        });
        return true;
    }

    /**
     * 根据ID查找请求节点
     */
    private DefaultMutableTreeNode findRequestNodeById(DefaultMutableTreeNode node, String id) {
        if (node == null) return null;

        Object userObj = node.getUserObject();
        if (userObj instanceof Object[] obj) {
            if ("request".equals(obj[0])) {
                HttpRequestItem item = (HttpRequestItem) obj[1];
                if (id.equals(item.getId())) {
                    return node;
                }
            }
        }

        for (int i = 0; i < node.getChildCount(); i++) {
            DefaultMutableTreeNode child = (DefaultMutableTreeNode) node.getChildAt(i);
            DefaultMutableTreeNode result = findRequestNodeById(child, id);
            if (result != null) {
                return result;
            }
        }

        return null;
    }

    /**
     * 获取分组树的 TreeModel（用于分组选择树）
     */
    public DefaultTreeModel getGroupTreeModel() {
        return treeModel;
    }

    // 复制请求方法
    private void duplicateSelectedRequest() {
        DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) requestTree.getLastSelectedPathComponent();
        if (selectedNode == null) return;
        Object userObj = selectedNode.getUserObject();
        if (userObj instanceof Object[] obj && "request".equals(obj[0])) {
            HttpRequestItem item = (HttpRequestItem) obj[1];
            // 深拷贝请求项（假设HttpRequestItem有clone或可用JSON序列化实现深拷贝）
            HttpRequestItem copy = JSONUtil.toBean(JSONUtil.parse(item).toString(), HttpRequestItem.class);
            copy.setId(java.util.UUID.randomUUID().toString());
            copy.setName(item.getName() + " Copy");
            DefaultMutableTreeNode parent = (DefaultMutableTreeNode) selectedNode.getParent();
            DefaultMutableTreeNode copyNode = new DefaultMutableTreeNode(new Object[]{"request", copy});
            int idx = parent.getIndex(selectedNode) + 1;
            parent.insert(copyNode, idx);
            treeModel.reload(parent);
            requestTree.expandPath(new TreePath(parent.getPath()));
            persistence.saveRequestGroups();
        }
    }

    // 复制请求为cUrl方法
    private void copySelectedRequestAsCurl() {
        DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) requestTree.getLastSelectedPathComponent();
        if (selectedNode == null) return;
        Object userObj = selectedNode.getUserObject();
        if (userObj instanceof Object[] obj && "request".equals(obj[0])) {
            HttpRequestItem item = (HttpRequestItem) obj[1];
            try {
                String curl = CurlParser.toCurl(item);
                Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new java.awt.datatransfer.StringSelection(curl), null);
                JOptionPane.showMessageDialog(this, "cUrl命令已复制到剪贴板！", "提示", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "生成cUrl命令失败: " + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void duplicateSelectedGroup() {
        DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) requestTree.getLastSelectedPathComponent();
        if (selectedNode == null) return;
        Object userObj = selectedNode.getUserObject();
        if (userObj instanceof Object[] obj && "group".equals(obj[0])) {
            // 深拷贝分组及其所有子节点
            DefaultMutableTreeNode copyNode = deepCopyGroupNode(selectedNode);
            Object[] copyObj = (Object[]) copyNode.getUserObject();
            copyObj[1] = copyObj[1] + " Copy";
            DefaultMutableTreeNode parent = (DefaultMutableTreeNode) selectedNode.getParent();
            if (parent != null) {
                int idx = parent.getIndex(selectedNode) + 1;
                parent.insert(copyNode, idx);
                treeModel.reload(parent);
                requestTree.expandPath(new TreePath(parent.getPath()));
                persistence.saveRequestGroups();
            }
        }
    }

    // 深拷贝分组节点及其所有子节点
    private DefaultMutableTreeNode deepCopyGroupNode(DefaultMutableTreeNode node) {
        Object userObj = node.getUserObject();
        Object[] obj = userObj instanceof Object[] ? ((Object[]) userObj).clone() : null;
        DefaultMutableTreeNode copy = new DefaultMutableTreeNode(obj);
        for (int i = 0; i < node.getChildCount(); i++) {
            DefaultMutableTreeNode child = (DefaultMutableTreeNode) node.getChildAt(i);
            Object childUserObj = child.getUserObject();
            if (childUserObj instanceof Object[] childObj) {
                if ("group".equals(childObj[0])) {
                    copy.add(deepCopyGroupNode(child));
                } else if ("request".equals(childObj[0])) {
                    // 深拷贝请求节点
                    HttpRequestItem item = (HttpRequestItem) childObj[1];
                    HttpRequestItem copyItem = JSONUtil.toBean(JSONUtil.parse(item).toString(), HttpRequestItem.class);
                    copyItem.setId(java.util.UUID.randomUUID().toString());
                    Object[] reqObj = new Object[]{"request", copyItem};
                    copy.add(new DefaultMutableTreeNode(reqObj));
                }
            }
        }
        return copy;
    }

    // 递归过滤节点
    private boolean filterNodes(DefaultMutableTreeNode src, DefaultMutableTreeNode dest, String keyword) {
        boolean matched = false;
        Object userObj = src.getUserObject();
        if (userObj instanceof Object[] obj) {
            String type = String.valueOf(obj[0]);
            if ("group".equals(type)) {
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
            } else if ("request".equals(type)) {
                HttpRequestItem item = (HttpRequestItem) obj[1];
                if (item.getName() != null && item.getName().toLowerCase().contains(keyword)) {
                    dest.add(new DefaultMutableTreeNode(obj.clone()));
                    matched = true;
                }
            }
        } else {
            // 处理 root 节点（userObj 不是 Object[]，比如 "-请求集合-"）
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
        javax.swing.tree.TreeNode root = (javax.swing.tree.TreeNode) tree.getModel().getRoot();
        expandAll(tree, new TreePath(root), expand);
    }

    private void expandAll(JTree tree, TreePath parent, boolean expand) {
        javax.swing.tree.TreeNode node = (javax.swing.tree.TreeNode) parent.getLastPathComponent();
        if (node.getChildCount() >= 0) {
            for (int i = 0; i < node.getChildCount(); i++) {
                javax.swing.tree.TreeNode n = node.getChildAt(i);
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

    // 递归解析Postman集合为树结构，返回标准分组/请求节点列表
    private java.util.List<DefaultMutableTreeNode> parsePostmanItemsToTree(JSONArray items) {
        java.util.List<DefaultMutableTreeNode> nodeList = new java.util.ArrayList<>();
        for (Object obj : items) {
            JSONObject item = (JSONObject) obj;
            if (item.containsKey("item")) {
                // 文件夹节点
                String folderName = item.getStr("name", "未命名文件夹");
                DefaultMutableTreeNode folderNode = new DefaultMutableTreeNode(new Object[]{"group", folderName});
                // 先处理自身 request
                if (item.containsKey("request")) {
                    HttpRequestItem req = PostmanImport.parsePostmanSingleItem(item);
                    folderNode.add(new DefaultMutableTreeNode(new Object[]{"request", req}));
                }
                // 递归处理子节点
                JSONArray children = item.getJSONArray("item");
                java.util.List<DefaultMutableTreeNode> childNodes = parsePostmanItemsToTree(children);
                for (DefaultMutableTreeNode child : childNodes) {
                    folderNode.add(child);
                }
                nodeList.add(folderNode);
            } else if (item.containsKey("request")) {
                // 纯请求节点
                HttpRequestItem req = PostmanImport.parsePostmanSingleItem(item);
                nodeList.add(new DefaultMutableTreeNode(new Object[]{"request", req}));
            }
        }
        return nodeList;
    }

    // 导出分组为Postman Collection
    private void exportGroupAsPostman(DefaultMutableTreeNode groupNode) {
        if (groupNode == null || !(groupNode.getUserObject() instanceof Object[] obj) || !"group".equals(obj[0])) {
            JOptionPane.showMessageDialog(this, "请选择分组节点导出", "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String groupName = String.valueOf(obj[1]);
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("导出为Postman Collection");
        fileChooser.setSelectedFile(new File(groupName + "-postman.json"));
        int userSelection = fileChooser.showSaveDialog(this);
        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File fileToSave = fileChooser.getSelectedFile();
            try {
                JSONObject postmanCollection = PostmanImport.buildPostmanCollectionFromTreeNode(groupNode, groupName);
                FileUtil.writeUtf8String(postmanCollection.toStringPretty(), fileToSave);
                JOptionPane.showMessageDialog(this, "导出成功！", "提示", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                log.error("导出Postman失败", ex);
                JOptionPane.showMessageDialog(this, "导出失败: " + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}