package com.laker.postman.panel.collections.left;

import cn.hutool.core.io.FileUtil;
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
import com.laker.postman.common.tab.ClosableTabComponent;
import com.laker.postman.common.tree.RequestTreeCellRenderer;
import com.laker.postman.common.tree.TreeTransferHandler;
import com.laker.postman.model.CurlRequest;
import com.laker.postman.model.HttpRequestItem;
import com.laker.postman.model.PreparedRequest;
import com.laker.postman.panel.collections.right.RequestEditPanel;
import com.laker.postman.panel.collections.right.request.RequestEditSubPanel;
import com.laker.postman.service.RequestCollectionPersistence;
import com.laker.postman.service.curl.CurlParser;
import com.laker.postman.service.http.HttpRequestFactory;
import com.laker.postman.service.http.PreparedRequestBuilder;
import com.laker.postman.service.postman.PostmanImport;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;
import com.laker.postman.util.UserSettingsUtil;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static com.laker.postman.util.SystemUtil.COLLECTION_PATH;
import static com.laker.postman.util.SystemUtil.getClipboardCurlText;

/**
 * 请求集合面板，展示所有请求分组和请求项
 * 文件路径：用户目录下的 easy-tools/requestCollection.json
 * 支持请求的增删改查、分组管理、拖拽排序等功能
 */
@Slf4j
public class RequestCollectionsLeftPanel extends SingletonBasePanel {
    public static final String REQUEST = "request";
    public static final String GROUP = "group";
    public static final String ROOT = "-请求集合-";
    // 请求集合的根节点
    private DefaultMutableTreeNode rootTreeNode;
    // 请求树组件
    private JTree requestTree;
    // 树模型，用于管理树节点
    private DefaultTreeModel treeModel;

    private FlatTextField searchField;

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
    }

    private JScrollPane getTreeScrollPane() {
        // 初始化请求树
        rootTreeNode = new DefaultMutableTreeNode(ROOT);
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
        JScrollPane treeScrollPane = new JScrollPane(requestTree);
        treeScrollPane.getVerticalScrollBar().setUnitIncrement(16); // 设置滚动条增量
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
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.X_AXIS));
        topPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5)); // 设置上下左右边距

        JButton importBtn = getImportBtn();
        JButton exportBtn = new JButton(new FlatSVGIcon("icons/export.svg", 20, 20));
        exportBtn.setFocusPainted(false);
        exportBtn.setBackground(Color.WHITE);
        exportBtn.setToolTipText(I18nUtil.getMessage(MessageKeys.COLLECTIONS_EXPORT_TOOLTIP));
        exportBtn.addActionListener(e -> exportRequestCollection());

        getSearchField();

        topPanel.add(importBtn);
        topPanel.add(exportBtn);
        topPanel.add(searchField);
        return topPanel;
    }

    private void getSearchField() {
        searchField = new SearchTextField();
    }

    private JButton getImportBtn() {
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
                int result = JOptionPane.showConfirmDialog(null, I18nUtil.getMessage(MessageKeys.COLLECTIONS_IMPORT_CURL_DETECTED), I18nUtil.getMessage(MessageKeys.COLLECTIONS_IMPORT_CURL_TITLE), JOptionPane.YES_NO_OPTION);
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
        JMenuItem importEasyToolsItem = new JMenuItem(I18nUtil.getMessage(MessageKeys.COLLECTIONS_IMPORT_EASY), new FlatSVGIcon("icons/easy.svg", 20, 20));
        importEasyToolsItem.setToolTipText(I18nUtil.getMessage(MessageKeys.COLLECTIONS_IMPORT_EASY_TOOLTIP));
        importEasyToolsItem.addActionListener(e -> importRequestCollection());
        JMenuItem importPostmanItem = new JMenuItem(I18nUtil.getMessage(MessageKeys.COLLECTIONS_IMPORT_POSTMAN), new FlatSVGIcon("icons/postman.svg", 20, 20));
        importPostmanItem.setToolTipText(I18nUtil.getMessage(MessageKeys.COLLECTIONS_IMPORT_POSTMAN_TOOLTIP));
        importPostmanItem.addActionListener(e -> importPostmanCollection());
        JMenuItem importCurlItem = new JMenuItem(I18nUtil.getMessage(MessageKeys.COLLECTIONS_IMPORT_CURL), new FlatSVGIcon("icons/curl.svg", 20, 20));
        importCurlItem.setToolTipText(I18nUtil.getMessage(MessageKeys.COLLECTIONS_IMPORT_CURL_TOOLTIP));
        importCurlItem.addActionListener(e -> importCurlToCollection(null));
        importMenu.add(importEasyToolsItem);
        importMenu.add(importPostmanItem);
        importMenu.add(importCurlItem);
        return importMenu;
    }

    @Override
    protected void registerListeners() {

        // 鼠标点击事件：无论是否切换都触发（解决重复点击同一节点无响应问题）
        requestTree.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int selRow = requestTree.getRowForLocation(e.getX(), e.getY());
                TreePath selPath = requestTree.getPathForLocation(e.getX(), e.getY());
                // 如果点击位置没有直接命中节点，则获取最近的行
                if (selRow == -1 || selPath == null) {
                    // 获取最接近点击位置的行
                    selRow = requestTree.getClosestRowForLocation(e.getX(), e.getY());
                    if (selRow != -1) {
                        selPath = requestTree.getPathForRow(selRow);
                    }
                }

                if (selRow != -1 && selPath != null) {
                    DefaultMutableTreeNode node = (DefaultMutableTreeNode) selPath.getLastPathComponent();
                    if (node.getUserObject() instanceof Object[] obj) {
                        if (REQUEST.equals(obj[0])) {
                            HttpRequestItem item = (HttpRequestItem) obj[1];
                            // 记录最后打开的请求ID
                            UserSettingsUtil.saveLastOpenRequestId(item.getId());
                            SingletonFactory.getInstance(RequestEditPanel.class).showOrCreateTab(item);
                        }
                    }
                }
            }

            @Override
            public void mousePressed(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    int x = e.getX();
                    int y = e.getY();
                    int row = requestTree.getClosestRowForLocation(x, y);
                    if (row != -1) {
                        requestTree.setSelectionRow(row);
                    } else {
                        requestTree.clearSelection(); // 没有节点时取消选中
                    }
                    showPopupMenu(x, y); // 无论是否有节点都弹出菜单
                }
            }

            private void showPopupMenu(int x, int y) {
                JPopupMenu menu = new JPopupMenu();
                DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) requestTree.getLastSelectedPathComponent();
                Object userObj = selectedNode != null ? selectedNode.getUserObject() : null;
                // 如果树为空或未选中任何节点，允许新增分组
                if (selectedNode == null || selectedNode == rootTreeNode) {
                    JMenuItem addGroupItem = new JMenuItem(I18nUtil.getMessage(MessageKeys.COLLECTIONS_MENU_ADD_GROUP));
                    addGroupItem.addActionListener(e -> {
                        showAddGroupDialog(rootTreeNode);
                    });
                    menu.add(addGroupItem);
                    menu.show(requestTree, x, y);
                    return;
                }
                // 仅分组节点可新增文件/请求
                if (userObj instanceof Object[] && GROUP.equals(((Object[]) userObj)[0])) {
                    JMenuItem addGroupItem = new JMenuItem(I18nUtil.getMessage(MessageKeys.COLLECTIONS_MENU_ADD_GROUP));
                    addGroupItem.addActionListener(e -> addGroupUnderSelected());
                    menu.add(addGroupItem);
                    JMenuItem addRequestItem = new JMenuItem(I18nUtil.getMessage(MessageKeys.COLLECTIONS_MENU_ADD_REQUEST));
                    addRequestItem.addActionListener(e -> addRequestUnderSelected());
                    menu.add(addRequestItem);
                    JMenuItem duplicateGroupItem = new JMenuItem(I18nUtil.getMessage(MessageKeys.COLLECTIONS_MENU_DUPLICATE));
                    duplicateGroupItem.addActionListener(e -> duplicateSelectedGroup());
                    menu.add(duplicateGroupItem);
                    // 导出为Postman
                    JMenuItem exportPostmanItem = new JMenuItem(I18nUtil.getMessage(MessageKeys.COLLECTIONS_MENU_EXPORT_POSTMAN));
                    exportPostmanItem.addActionListener(e -> exportGroupAsPostman(selectedNode));
                    menu.add(exportPostmanItem);
                    menu.addSeparator();
                }
                // 请求节点右键菜单增加"复制"
                if (userObj instanceof Object[] && REQUEST.equals(((Object[]) userObj)[0])) {
                    JMenuItem duplicateItem = new JMenuItem(I18nUtil.getMessage(MessageKeys.COLLECTIONS_MENU_DUPLICATE));
                    duplicateItem.addActionListener(e -> duplicateSelectedRequest());
                    menu.add(duplicateItem);
                    // 复制为cURL命令
                    JMenuItem copyAsCurlItem = new JMenuItem(I18nUtil.getMessage(MessageKeys.COLLECTIONS_MENU_COPY_CURL));
                    copyAsCurlItem.addActionListener(e -> copySelectedRequestAsCurl());
                    menu.add(copyAsCurlItem);
                    menu.addSeparator();
                }
                // 只有非根节点才显示重命名/删除
                if (selectedNode != rootTreeNode) {
                    JMenuItem renameItem = new JMenuItem(I18nUtil.getMessage(MessageKeys.COLLECTIONS_MENU_RENAME));
                    JMenuItem deleteItem = new JMenuItem(I18nUtil.getMessage(MessageKeys.COLLECTIONS_MENU_DELETE));
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
                    if (GROUP.equals(obj[0])) {
                        String newName = JOptionPane.showInputDialog(I18nUtil.getMessage(MessageKeys.COLLECTIONS_DIALOG_RENAME_GROUP_PROMPT), obj[1]);
                        if (newName != null) newName = newName.trim();
                        if (newName == null) {
                            // 用户取消输入，直接退出
                            return;
                        }
                        if (newName.isEmpty()) {
                            JOptionPane.showMessageDialog(null, I18nUtil.getMessage(MessageKeys.COLLECTIONS_DIALOG_RENAME_GROUP_EMPTY), I18nUtil.getMessage(MessageKeys.GENERAL_TIP), JOptionPane.WARNING_MESSAGE);
                            return;
                        }
                        obj[1] = newName;
                        treeModel.nodeChanged(selectedNode);
                        saveRequestGroups();
                    } else if (REQUEST.equals(obj[0])) {
                        HttpRequestItem item = (HttpRequestItem) obj[1];
                        String oldName = item.getName();
                        String newName = JOptionPane.showInputDialog(I18nUtil.getMessage(MessageKeys.COLLECTIONS_DIALOG_RENAME_REQUEST_PROMPT), oldName);
                        if (newName != null) newName = newName.trim();
                        if (newName == null) {
                            // 用户取消输入，直接退出
                            return;
                        }
                        if (newName.isEmpty()) {
                            JOptionPane.showMessageDialog(null, I18nUtil.getMessage(MessageKeys.COLLECTIONS_DIALOG_RENAME_REQUEST_EMPTY), I18nUtil.getMessage(MessageKeys.GENERAL_TIP), JOptionPane.WARNING_MESSAGE);
                            return;
                        }
                        item.setName(newName);
                        treeModel.nodeChanged(selectedNode);
                        saveRequestGroups();
                        // 同步更新已打开Tab的标题
                        RequestEditPanel editPanel = SingletonFactory.getInstance(RequestEditPanel.class);
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

            private void deleteSelectedItem() {
                DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) requestTree.getLastSelectedPathComponent();
                if (selectedNode != null && selectedNode.getParent() != null) {
                    // 删除前弹出确认提示
                    int confirm = JOptionPane.showConfirmDialog(
                            requestTree,
                            I18nUtil.getMessage(MessageKeys.COLLECTIONS_DELETE_CONFIRM),
                            I18nUtil.getMessage(MessageKeys.COLLECTIONS_DELETE_CONFIRM_TITLE),
                            JOptionPane.YES_NO_OPTION
                    );
                    if (confirm != JOptionPane.YES_OPTION) {
                        return;
                    }
                    // 先关闭相关Tab
                    Object userObj = selectedNode.getUserObject();
                    RequestEditPanel editPanel = SingletonFactory.getInstance(RequestEditPanel.class);
                    JTabbedPane tabbedPane = editPanel.getTabbedPane();
                    if (userObj instanceof Object[] obj) {
                        if (REQUEST.equals(obj[0])) {
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
                        } else if (GROUP.equals(obj[0])) {
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
                        if (REQUEST.equals(obj[0])) {
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
                        } else if (GROUP.equals(obj[0])) {
                            closeTabsForGroup(child, tabbedPane);
                        }
                    }
                }
            }
        });

        // 搜索过滤逻辑
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            private void filterTree() {
                String text = searchField.getText().trim();
                if (text.isEmpty()) {
                    // 展开所有一级分组，显示全部
                    expandAll(requestTree, false);
                    treeModel.setRoot(rootTreeNode);
                    treeModel.reload();
                    return;
                }
                DefaultMutableTreeNode filteredRoot = new DefaultMutableTreeNode(ROOT);
                filterNodes(rootTreeNode, filteredRoot, text.toLowerCase());
                treeModel.setRoot(filteredRoot);
                treeModel.reload();
                expandAll(requestTree, true);
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

        SwingUtilities.invokeLater(() -> {  // 异步加载请求组
            persistence.initRequestGroupsFromFile(); // 从文件加载请求集合
            // 加载完成后，自动打开最后一次请求,如果没有这默认打开一个新建请求
            SwingUtilities.invokeLater(() -> {
                RequestEditPanel requestEditPanel = SingletonFactory.getInstance(RequestEditPanel.class);
                String lastId = UserSettingsUtil.getLastOpenRequestId();
                if (lastId != null && !lastId.isEmpty()) {
                    DefaultMutableTreeNode node = findRequestNodeById(rootTreeNode, lastId);
                    if (node != null) {
                        TreePath path = new TreePath(node.getPath());
                        requestTree.setSelectionPath(path);
                        requestTree.scrollPathToVisible(path);
                        Object[] obj = (Object[]) node.getUserObject();
                        if (REQUEST.equals(obj[0])) {
                            HttpRequestItem item = (HttpRequestItem) obj[1];
                            requestEditPanel.showOrCreateTab(item);
                            requestEditPanel.addPlusTab();
                        }
                    }
                } else {
                    // 没有lastId时，默认打开一个新建请求
                    requestEditPanel.addNewTab(RequestEditPanel.REQUEST_STRING);
                }

            });
        });

    }

    private void showAddGroupDialog(DefaultMutableTreeNode rootTreeNode) {
        String groupName = JOptionPane.showInputDialog(this, I18nUtil.getMessage(MessageKeys.COLLECTIONS_DIALOG_ADD_GROUP_PROMPT));
        if (groupName != null && !groupName.trim().isEmpty()) {
            DefaultMutableTreeNode groupNode = new DefaultMutableTreeNode(new Object[]{GROUP, groupName});
            rootTreeNode.add(groupNode);
            treeModel.reload(rootTreeNode);
            requestTree.expandPath(new TreePath(rootTreeNode.getPath()));
            persistence.saveRequestGroups();
        }
    }

    // 导出请求集合到JSON文件
    private void exportRequestCollection() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle(I18nUtil.getMessage(MessageKeys.COLLECTIONS_EXPORT_DIALOG_TITLE));
        fileChooser.setSelectedFile(new File("EasyPostman-Collections.json"));
        int userSelection = fileChooser.showSaveDialog(this);
        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File fileToSave = fileChooser.getSelectedFile();
            try {
                persistence.exportRequestCollection(fileToSave);
                JOptionPane.showMessageDialog(this, I18nUtil.getMessage(MessageKeys.COLLECTIONS_EXPORT_SUCCESS), I18nUtil.getMessage(MessageKeys.GENERAL_TIP), JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                log.error("导出失败", ex);
                JOptionPane.showMessageDialog(this, I18nUtil.getMessage(MessageKeys.COLLECTIONS_EXPORT_FAIL, ex.getMessage()), I18nUtil.getMessage(MessageKeys.GENERAL_ERROR), JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    // 导入请求集合JSON文件
    private void importRequestCollection() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle(I18nUtil.getMessage(MessageKeys.COLLECTIONS_IMPORT_DIALOG_TITLE));
        int userSelection = fileChooser.showOpenDialog(this);
        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File fileToOpen = fileChooser.getSelectedFile();
            try {
                // 导入时不清空老数据，而是全部加入到一个新分组下
                String groupName = "EasyPostman";
                DefaultMutableTreeNode easyPostmanGroup = findGroupNode(rootTreeNode, groupName);
                if (easyPostmanGroup == null) {
                    easyPostmanGroup = new DefaultMutableTreeNode(new Object[]{GROUP, groupName});
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
                JOptionPane.showMessageDialog(this, I18nUtil.getMessage(MessageKeys.COLLECTIONS_IMPORT_SUCCESS), I18nUtil.getMessage(MessageKeys.GENERAL_TIP), JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                log.error("导入失败", ex);
                JOptionPane.showMessageDialog(this, I18nUtil.getMessage(MessageKeys.COLLECTIONS_IMPORT_FAIL, ex.getMessage()), I18nUtil.getMessage(MessageKeys.GENERAL_ERROR), JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    // 新增：导入Postman集合
    private void importPostmanCollection() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle(I18nUtil.getMessage(MessageKeys.COLLECTIONS_IMPORT_POSTMAN_DIALOG_TITLE));
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
                    DefaultMutableTreeNode collectionNode = new DefaultMutableTreeNode(new Object[]{GROUP, collectionName});
                    java.util.List<DefaultMutableTreeNode> children = parsePostmanItemsToTree(items);
                    for (DefaultMutableTreeNode child : children) {
                        collectionNode.add(child);
                    }
                    rootTreeNode.add(collectionNode);
                    treeModel.reload();
                    persistence.saveRequestGroups();
                    requestTree.expandPath(new TreePath(collectionNode.getPath()));
                    JOptionPane.showMessageDialog(this, I18nUtil.getMessage(MessageKeys.COLLECTIONS_IMPORT_SUCCESS), I18nUtil.getMessage(MessageKeys.GENERAL_TIP), JOptionPane.INFORMATION_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(this, I18nUtil.getMessage(MessageKeys.COLLECTIONS_IMPORT_POSTMAN_INVALID), I18nUtil.getMessage(MessageKeys.GENERAL_ERROR), JOptionPane.ERROR_MESSAGE);
                }
            } catch (Exception ex) {
                log.error("Postman导入失败", ex);
                JOptionPane.showMessageDialog(this, I18nUtil.getMessage(MessageKeys.COLLECTIONS_IMPORT_FAIL, ex.getMessage()), I18nUtil.getMessage(MessageKeys.GENERAL_ERROR), JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void importCurlToCollection(String defaultCurl) {
        String curlText = LargeInputDialog.show(null, I18nUtil.getMessage(MessageKeys.COLLECTIONS_IMPORT_CURL_DIALOG_TITLE), I18nUtil.getMessage(MessageKeys.COLLECTIONS_IMPORT_CURL_DIALOG_PROMPT), defaultCurl);
        if (curlText == null || curlText.trim().isEmpty()) return;
        try {
            CurlRequest curlRequest = CurlParser.parse(curlText);
            if (curlRequest.url == null) {
                JOptionPane.showMessageDialog(null, I18nUtil.getMessage(MessageKeys.COLLECTIONS_IMPORT_CURL_PARSE_FAIL), I18nUtil.getMessage(MessageKeys.GENERAL_ERROR), JOptionPane.ERROR_MESSAGE);
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
            JOptionPane.showMessageDialog(this, I18nUtil.getMessage(MessageKeys.COLLECTIONS_IMPORT_CURL_PARSE_ERROR, ex.getMessage()), I18nUtil.getMessage(MessageKeys.GENERAL_ERROR), JOptionPane.ERROR_MESSAGE);
        }
    }

    private void addGroupUnderSelected() {
        DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) requestTree.getLastSelectedPathComponent();
        if (selectedNode == null) return;
        showAddGroupDialog(selectedNode);
    }

    private void addRequestUnderSelected() {
        DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) requestTree.getLastSelectedPathComponent();
        if (selectedNode == null) return;
        // 先检测剪贴板是否有cURL命令
        String curlText = getClipboardCurlText();
        if (curlText != null) {
            // 直接导入cURL到当前分组
            importCurlToGroup(curlText, selectedNode);
            return;
        }
        // 添加空请求
        DefaultMutableTreeNode reqNode = new DefaultMutableTreeNode(new Object[]{REQUEST, HttpRequestFactory.createDefaultRequest()});
        selectedNode.add(reqNode);
        treeModel.reload(selectedNode);
        requestTree.expandPath(new TreePath(selectedNode.getPath()));
        persistence.saveRequestGroups();
    }

    // 支持指定分组导入cURL
    private void importCurlToGroup(String curlText, DefaultMutableTreeNode groupNode) {
        String input = LargeInputDialog.show(null, I18nUtil.getMessage(MessageKeys.COLLECTIONS_IMPORT_CURL_DIALOG_TITLE), I18nUtil.getMessage(MessageKeys.COLLECTIONS_IMPORT_CURL_DIALOG_PROMPT), curlText);
        if (input == null || input.trim().isEmpty()) return;
        try {
            CurlRequest curlRequest = CurlParser.parse(input);
            if (curlRequest.url == null) {
                JOptionPane.showMessageDialog(null, I18nUtil.getMessage(MessageKeys.COLLECTIONS_IMPORT_CURL_PARSE_FAIL), I18nUtil.getMessage(MessageKeys.GENERAL_ERROR), JOptionPane.ERROR_MESSAGE);
                return;
            }
            HttpRequestItem item = new HttpRequestItem();
            item.setName(curlRequest.url);
            item.setUrl(curlRequest.url);
            item.setMethod(curlRequest.method);
            item.setHeaders(curlRequest.headers);
            item.setBody(curlRequest.body);
            item.setParams(curlRequest.params);
            item.setFormData(curlRequest.formData);
            item.setFormFiles(curlRequest.formFiles);
            // 直接保存到当前分组
            DefaultMutableTreeNode reqNode = new DefaultMutableTreeNode(new Object[]{REQUEST, item});
            groupNode.add(reqNode);
            treeModel.reload(groupNode);
            requestTree.expandPath(new TreePath(groupNode.getPath()));
            persistence.saveRequestGroups();
            // 导入成功后清空剪贴板
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(""), null);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, I18nUtil.getMessage(MessageKeys.COLLECTIONS_IMPORT_CURL_PARSE_ERROR, ex.getMessage()), I18nUtil.getMessage(MessageKeys.GENERAL_ERROR), JOptionPane.ERROR_MESSAGE);
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
        if (group == null || !GROUP.equals(group[0])) {
            return;
        }
        DefaultMutableTreeNode groupNode = findGroupNode(rootTreeNode, (String) group[1]);
        if (groupNode == null) {
            return;
        }
        DefaultMutableTreeNode requestNode = new DefaultMutableTreeNode(new Object[]{REQUEST, item});
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
            if (GROUP.equals(obj[0]) && groupName.equals(obj[1])) {
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
            RequestEditPanel editPanel = SingletonFactory.getInstance(RequestEditPanel.class);
            JTabbedPane tabbedPane = editPanel.getTabbedPane();
            for (int i = 0; i < tabbedPane.getTabCount(); i++) {
                Component comp = tabbedPane.getComponentAt(i);
                if (comp instanceof RequestEditSubPanel subPanel) {
                    HttpRequestItem tabItem = subPanel.getCurrentRequest();
                    if (tabItem != null && item.getId().equals(tabItem.getId())) {
                        editPanel.updateTabDirty(subPanel, false);
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
            if (REQUEST.equals(obj[0])) {
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
        if (userObj instanceof Object[] obj && REQUEST.equals(obj[0])) {
            HttpRequestItem item = (HttpRequestItem) obj[1];
            // 深拷贝请求项（假设HttpRequestItem有clone或可用JSON序列化实现深拷贝）
            HttpRequestItem copy = JSONUtil.toBean(JSONUtil.parse(item).toString(), HttpRequestItem.class);
            copy.setId(java.util.UUID.randomUUID().toString());
            copy.setName(item.getName() + I18nUtil.getMessage(MessageKeys.COLLECTIONS_MENU_COPY_SUFFIX));
            DefaultMutableTreeNode parent = (DefaultMutableTreeNode) selectedNode.getParent();
            DefaultMutableTreeNode copyNode = new DefaultMutableTreeNode(new Object[]{REQUEST, copy});
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
        if (userObj instanceof Object[] obj && REQUEST.equals(obj[0])) {
            HttpRequestItem item = (HttpRequestItem) obj[1];
            try {
                PreparedRequest req = PreparedRequestBuilder.build(item);
                // 对于 cURL 导出，直接进行变量替换（不需要前置脚本）
                PreparedRequestBuilder.replaceVariablesAfterPreScript(req);
                String curl = CurlParser.toCurl(req);
                Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(curl), null); // 将cUrl命令复制到剪贴板
                JOptionPane.showMessageDialog(this, I18nUtil.getMessage(MessageKeys.COLLECTIONS_MENU_COPY_CURL_SUCCESS), I18nUtil.getMessage(MessageKeys.GENERAL_TIP), JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, I18nUtil.getMessage(MessageKeys.COLLECTIONS_MENU_COPY_CURL_FAIL, ex.getMessage()), I18nUtil.getMessage(MessageKeys.GENERAL_ERROR), JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void duplicateSelectedGroup() {
        DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) requestTree.getLastSelectedPathComponent();
        if (selectedNode == null) return;
        Object userObj = selectedNode.getUserObject();
        if (userObj instanceof Object[] obj && GROUP.equals(obj[0])) {
            // 深拷贝分组及其所有子节点
            DefaultMutableTreeNode copyNode = deepCopyGroupNode(selectedNode);
            Object[] copyObj = (Object[]) copyNode.getUserObject();
            copyObj[1] = copyObj[1] + I18nUtil.getMessage(MessageKeys.COLLECTIONS_MENU_COPY_SUFFIX);
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
                if (GROUP.equals(childObj[0])) {
                    copy.add(deepCopyGroupNode(child));
                } else if (REQUEST.equals(childObj[0])) {
                    // 深拷贝请求节点
                    HttpRequestItem item = (HttpRequestItem) childObj[1];
                    HttpRequestItem copyItem = JSONUtil.toBean(JSONUtil.parse(item).toString(), HttpRequestItem.class);
                    copyItem.setId(java.util.UUID.randomUUID().toString());
                    Object[] reqObj = new Object[]{REQUEST, copyItem};
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
                DefaultMutableTreeNode folderNode = new DefaultMutableTreeNode(new Object[]{GROUP, folderName});
                // 先处理自身 request
                if (item.containsKey(REQUEST)) {
                    HttpRequestItem req = PostmanImport.parsePostmanSingleItem(item);
                    folderNode.add(new DefaultMutableTreeNode(new Object[]{REQUEST, req}));
                }
                // 递归处理子节点
                JSONArray children = item.getJSONArray("item");
                java.util.List<DefaultMutableTreeNode> childNodes = parsePostmanItemsToTree(children);
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

    // 导出分组为Postman Collection
    private void exportGroupAsPostman(DefaultMutableTreeNode groupNode) {
        if (groupNode == null || !(groupNode.getUserObject() instanceof Object[] obj) || !GROUP.equals(obj[0])) {
            JOptionPane.showMessageDialog(this, I18nUtil.getMessage(MessageKeys.COLLECTIONS_MENU_EXPORT_POSTMAN_SELECT_GROUP), I18nUtil.getMessage(MessageKeys.GENERAL_TIP), JOptionPane.WARNING_MESSAGE);
            return;
        }
        String groupName = String.valueOf(obj[1]);
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle(I18nUtil.getMessage(MessageKeys.COLLECTIONS_MENU_EXPORT_POSTMAN_DIALOG_TITLE));
        fileChooser.setSelectedFile(new File(groupName + "-postman.json"));
        int userSelection = fileChooser.showSaveDialog(this);
        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File fileToSave = fileChooser.getSelectedFile();
            try {
                JSONObject postmanCollection = PostmanImport.buildPostmanCollectionFromTreeNode(groupNode, groupName);
                FileUtil.writeUtf8String(postmanCollection.toStringPretty(), fileToSave);
                JOptionPane.showMessageDialog(this, I18nUtil.getMessage(MessageKeys.COLLECTIONS_EXPORT_SUCCESS), I18nUtil.getMessage(MessageKeys.GENERAL_TIP), JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                log.error("导出Postman失败", ex);
                JOptionPane.showMessageDialog(this, I18nUtil.getMessage(MessageKeys.COLLECTIONS_EXPORT_FAIL, ex.getMessage()), I18nUtil.getMessage(MessageKeys.GENERAL_ERROR), JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    // 创建一个可多选的请求/分组选择树（用于Runner面板弹窗）
    public JTree createRequestSelectionTree() {
        DefaultTreeModel model = new DefaultTreeModel(cloneTreeNode(rootTreeNode));
        JTree tree = new JTree(model);
        tree.setRootVisible(false);
        tree.setShowsRootHandles(true);
        tree.setCellRenderer(new RequestTreeCellRenderer());
        tree.setRowHeight(28);
        tree.setBackground(new Color(245, 247, 250));
        // 支持多选
        tree.getSelectionModel().setSelectionMode(TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION);
        return tree;
    }

    // 递归克隆树节点（只克隆结构和userObject，不共享引用）
    // 生成一份只读、临时的树结构，用于弹窗选择，保证主界面集合树的安全和稳定
    private DefaultMutableTreeNode cloneTreeNode(DefaultMutableTreeNode node) {
        Object userObj = node.getUserObject();
        DefaultMutableTreeNode copy = new DefaultMutableTreeNode(userObj instanceof Object[] ? ((Object[]) userObj).clone() : userObj);
        for (int i = 0; i < node.getChildCount(); i++) {
            DefaultMutableTreeNode child = (DefaultMutableTreeNode) node.getChildAt(i);
            copy.add(cloneTreeNode(child));
        }
        return copy;
    }

    // 获取树中选中的所有请求（包含分组下所有请求）
    public java.util.List<HttpRequestItem> getSelectedRequestsFromTree(JTree tree) {
        java.util.List<HttpRequestItem> result = new java.util.ArrayList<>();
        javax.swing.tree.TreePath[] paths = tree.getSelectionPaths();
        if (paths == null) return result;
        for (javax.swing.tree.TreePath path : paths) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
            collectRequestsRecursively(node, result);
        }
        // 去重（按id）
        Map<String, HttpRequestItem> map = new LinkedHashMap<>();
        for (HttpRequestItem item : result) {
            map.put(item.getId(), item);
        }
        return new ArrayList<>(map.values());
    }

    // 递归收集请求
    private void collectRequestsRecursively(DefaultMutableTreeNode node, java.util.List<HttpRequestItem> list) {
        Object userObj = node.getUserObject();
        if (userObj instanceof Object[] obj) {
            if (REQUEST.equals(obj[0])) {
                list.add((HttpRequestItem) obj[1]);
            } else if (GROUP.equals(obj[0])) {
                for (int i = 0; i < node.getChildCount(); i++) {
                    collectRequestsRecursively((DefaultMutableTreeNode) node.getChildAt(i), list);
                }
            }
        }
    }

    /**
     * 弹出多选请求对话框，回调返回选中的HttpRequestItem列表
     */
    public static void showMultiSelectRequestDialog(Consumer<List<HttpRequestItem>> onSelected) {
        RequestCollectionsLeftPanel requestCollectionsLeftPanel = SingletonFactory.getInstance(RequestCollectionsLeftPanel.class);
        JDialog dialog = new JDialog(SingletonFactory.getInstance(MainFrame.class), I18nUtil.getMessage(MessageKeys.COLLECTIONS_DIALOG_MULTI_SELECT_TITLE), true);
        dialog.setSize(400, 500);
        dialog.setResizable(false);
        dialog.setLocationRelativeTo(null);
        dialog.setLayout(new BorderLayout());

        // 用JTree展示集合树，支持多选
        JTree tree = requestCollectionsLeftPanel.createRequestSelectionTree();
        tree.getSelectionModel().setSelectionMode(TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION);
        JScrollPane treeScroll = new JScrollPane(tree);
        dialog.add(treeScroll, BorderLayout.CENTER);

        JButton okBtn = new JButton(I18nUtil.getMessage(MessageKeys.GENERAL_OK));
        okBtn.addActionListener(e -> {
            List<HttpRequestItem> selected = requestCollectionsLeftPanel.getSelectedRequestsFromTree(tree);
            if (selected.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, I18nUtil.getMessage(MessageKeys.COLLECTIONS_DIALOG_MULTI_SELECT_EMPTY), I18nUtil.getMessage(MessageKeys.GENERAL_TIP), JOptionPane.WARNING_MESSAGE);
                return;
            }
            onSelected.accept(selected);
            dialog.dispose();
        });
        JButton cancelBtn = new JButton(I18nUtil.getMessage(MessageKeys.GENERAL_CANCEL));
        cancelBtn.addActionListener(e -> dialog.dispose());
        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        btns.add(okBtn);
        btns.add(cancelBtn);
        dialog.add(btns, BorderLayout.SOUTH);
        dialog.setVisible(true);
    }

    /**
     * 根据请求ID定位并选中树中的对应节点
     *
     * @param requestId 请求ID
     */
    public void locateAndSelectRequest(String requestId) {
        if (requestId == null || requestId.isEmpty()) {
            return;
        }

        DefaultMutableTreeNode targetNode = findRequestNodeById(rootTreeNode, requestId);
        if (targetNode == null) { // 如果没有找到对应的请求节点
            return;
        }

        // 构建完整路径
        TreePath treePath = new TreePath(targetNode.getPath());

        // 展开父节点路径，确保目标节点可见
        requestTree.expandPath(treePath.getParentPath());

        // 选中目标节点
        requestTree.setSelectionPath(treePath);

        // 滚动到可见区域
        requestTree.scrollPathToVisible(treePath);

        // 确保焦点在树上（可选，用于突出显示）
        requestTree.requestFocusInWindow();
    }
}
