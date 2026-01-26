package com.laker.postman.panel.collections.left;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.laker.postman.common.SingletonBasePanel;
import com.laker.postman.common.SingletonFactory;
import com.laker.postman.common.component.SearchTextField;
import com.laker.postman.common.component.button.ExportButton;
import com.laker.postman.common.component.button.ImportButton;
import com.laker.postman.common.component.dialog.CurlImportDialog;
import com.laker.postman.frame.MainFrame;
import com.laker.postman.model.*;
import com.laker.postman.panel.collections.right.RequestEditPanel;
import com.laker.postman.panel.topmenu.TopMenuBar;
import com.laker.postman.service.EnvironmentService;
import com.laker.postman.service.apipost.ApiPostCollectionParser;
import com.laker.postman.service.common.CollectionParseResult;
import com.laker.postman.service.common.TreeNodeBuilder;
import com.laker.postman.service.curl.CurlParser;
import com.laker.postman.service.har.HarParser;
import com.laker.postman.service.http.HttpUtil;
import com.laker.postman.service.ideahttp.IntelliJHttpParser;
import com.laker.postman.service.postman.PostmanCollectionParser;
import com.laker.postman.service.swagger.SwaggerParser;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;
import com.laker.postman.util.NotificationUtil;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileFilter;
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

import static com.laker.postman.panel.collections.left.RequestCollectionsLeftPanel.*;

@Slf4j
public class LeftTopPanel extends SingletonBasePanel {
    private SearchTextField searchField;
    private ImportButton importBtn;

    @Override
    protected void initUI() {
        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
        setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5)); // 设置上下左右边距

        importBtn = new ImportButton();
        importBtn.addActionListener(e -> handleImportClick());

        ExportButton exportBtn = new ExportButton();
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

                // 获取搜索选项
                boolean caseSensitive = searchField.isCaseSensitive();
                boolean wholeWord = searchField.isWholeWord();

                DefaultMutableTreeNode filteredRoot = new DefaultMutableTreeNode(ROOT);
                filterNodes(leftPanel.getRootTreeNode(), filteredRoot, text, caseSensitive, wholeWord);
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

        // 监听搜索选项变化，触发重新过滤
        searchField.addPropertyChangeListener("caseSensitive", evt -> {
            if (!searchField.getText().isEmpty()) {
                // 直接触发过滤逻辑
                RequestCollectionsLeftPanel leftPanel = SingletonFactory.getInstance(RequestCollectionsLeftPanel.class);
                String text = searchField.getText().trim();
                boolean caseSensitive = searchField.isCaseSensitive();
                boolean wholeWord = searchField.isWholeWord();

                DefaultMutableTreeNode filteredRoot = new DefaultMutableTreeNode(ROOT);
                filterNodes(leftPanel.getRootTreeNode(), filteredRoot, text, caseSensitive, wholeWord);
                leftPanel.getTreeModel().setRoot(filteredRoot);
                leftPanel.getTreeModel().reload();
                expandAll(leftPanel.getRequestTree(), true);
            }
        });
        searchField.addPropertyChangeListener("wholeWord", evt -> {
            if (!searchField.getText().isEmpty()) {
                // 直接触发过滤逻辑
                RequestCollectionsLeftPanel leftPanel = SingletonFactory.getInstance(RequestCollectionsLeftPanel.class);
                String text = searchField.getText().trim();
                boolean caseSensitive = searchField.isCaseSensitive();
                boolean wholeWord = searchField.isWholeWord();

                DefaultMutableTreeNode filteredRoot = new DefaultMutableTreeNode(ROOT);
                filterNodes(leftPanel.getRootTreeNode(), filteredRoot, text, caseSensitive, wholeWord);
                leftPanel.getTreeModel().setRoot(filteredRoot);
                leftPanel.getTreeModel().reload();
                expandAll(leftPanel.getRequestTree(), true);
            }
        });
    }


    private void handleImportClick() {
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

        // 显示导入菜单
        JPopupMenu importMenu = getImportMenu();
        importMenu.show(importBtn, 0, importBtn.getHeight());
    }

    private JPopupMenu getImportMenu() {
        JPopupMenu importMenu = new JPopupMenu();
        JMenuItem importEasyToolsItem = new JMenuItem(I18nUtil.getMessage(MessageKeys.COLLECTIONS_IMPORT_EASY),
                new FlatSVGIcon("icons/easy.svg", 20, 20));
        importEasyToolsItem.addActionListener(e -> importRequestCollection());
        JMenuItem importPostmanItem = new JMenuItem(I18nUtil.getMessage(MessageKeys.COLLECTIONS_IMPORT_POSTMAN),
                new FlatSVGIcon("icons/postman.svg", 20, 20));
        importPostmanItem.addActionListener(e -> importPostmanCollection());
        JMenuItem importSwagger2Item = new JMenuItem(I18nUtil.getMessage(MessageKeys.COLLECTIONS_IMPORT_SWAGGER2),
                new FlatSVGIcon("icons/swagger.svg", 20, 20));
        importSwagger2Item.addActionListener(e -> importSwaggerCollection());
        JMenuItem importOpenApi3Item = new JMenuItem(I18nUtil.getMessage(MessageKeys.COLLECTIONS_IMPORT_OPENAPI3),
                new FlatSVGIcon("icons/openapi.svg", 20, 20));
        importOpenApi3Item.addActionListener(e -> importSwaggerCollection());
        JMenuItem importHarItem = new JMenuItem(I18nUtil.getMessage(MessageKeys.COLLECTIONS_IMPORT_HAR),
                new FlatSVGIcon("icons/har.svg", 20, 20));
        importHarItem.addActionListener(e -> importHarCollection());
        JMenuItem importHttpItem = new JMenuItem(I18nUtil.getMessage(MessageKeys.COLLECTIONS_IMPORT_HTTP),
                new FlatSVGIcon("icons/idea-http.svg", 20, 20));
        importHttpItem.addActionListener(e -> importHttpFile());
        JMenuItem importApiPostItem = new JMenuItem(I18nUtil.getMessage(MessageKeys.COLLECTIONS_IMPORT_APIPOST),
                new FlatSVGIcon("icons/apipost.svg", 20, 20));
        importApiPostItem.addActionListener(e -> importApiPostCollection());
        JMenuItem importCurlItem = new JMenuItem(I18nUtil.getMessage(MessageKeys.COLLECTIONS_IMPORT_CURL),
                new FlatSVGIcon("icons/curl.svg", 20, 20));
        importCurlItem.addActionListener(e -> importCurlToCollection(null));
        importMenu.add(importEasyToolsItem);
        importMenu.add(importPostmanItem);
        importMenu.add(importSwagger2Item);
        importMenu.add(importOpenApi3Item);
        importMenu.add(importHarItem);
        importMenu.add(importHttpItem);
        importMenu.add(importApiPostItem);
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
                    RequestGroup group = new RequestGroup(groupName);
                    easyPostmanGroup = new DefaultMutableTreeNode(new Object[]{GROUP, group});
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
                NotificationUtil.showSuccess(I18nUtil.getMessage(MessageKeys.COLLECTIONS_IMPORT_SUCCESS));
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
                CollectionParseResult parseResult =
                        PostmanCollectionParser.parsePostmanCollection(json);
                if (parseResult != null) {
                    DefaultMutableTreeNode collectionNode =
                            TreeNodeBuilder.buildFromParseResult(parseResult);
                    leftPanel.getRootTreeNode().add(collectionNode);
                    leftPanel.getTreeModel().reload();
                    leftPanel.getPersistence().saveRequestGroups();
                    leftPanel.getRequestTree().expandPath(new TreePath(collectionNode.getPath()));
                    NotificationUtil.showSuccess(I18nUtil.getMessage(MessageKeys.COLLECTIONS_IMPORT_SUCCESS));
                } else {
                    NotificationUtil.showError(I18nUtil.getMessage(MessageKeys.COLLECTIONS_IMPORT_POSTMAN_INVALID));
                }
            } catch (Exception ex) {
                log.error("Import error", ex);
                NotificationUtil.showError(I18nUtil.getMessage(MessageKeys.COLLECTIONS_IMPORT_FAIL, ex.getMessage()));
            }
        }
    }

    // 导入HAR集合
    private void importHarCollection() {
        RequestCollectionsLeftPanel leftPanel = SingletonFactory.getInstance(RequestCollectionsLeftPanel.class);
        MainFrame mainFrame = SingletonFactory.getInstance(MainFrame.class);
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle(I18nUtil.getMessage(MessageKeys.COLLECTIONS_IMPORT_HAR_DIALOG_TITLE));
        int userSelection = fileChooser.showOpenDialog(mainFrame);
        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File fileToOpen = fileChooser.getSelectedFile();
            try {
                String json = FileUtil.readString(fileToOpen, StandardCharsets.UTF_8);
                DefaultMutableTreeNode collectionNode = HarParser.parseHar(json);
                if (collectionNode != null) {
                    leftPanel.getRootTreeNode().add(collectionNode);
                    leftPanel.getTreeModel().reload();
                    leftPanel.getPersistence().saveRequestGroups();
                    leftPanel.getRequestTree().expandPath(new TreePath(collectionNode.getPath()));
                    NotificationUtil.showSuccess(I18nUtil.getMessage(MessageKeys.COLLECTIONS_IMPORT_SUCCESS));
                } else {
                    NotificationUtil.showError(I18nUtil.getMessage(MessageKeys.COLLECTIONS_IMPORT_HAR_INVALID));
                }
            } catch (Exception ex) {
                log.error("Import HAR error", ex);
                NotificationUtil.showError(I18nUtil.getMessage(MessageKeys.COLLECTIONS_IMPORT_FAIL, ex.getMessage()));
            }
        }
    }

    // 导入Swagger/OpenAPI集合
    private void importSwaggerCollection() {
        RequestCollectionsLeftPanel leftPanel = SingletonFactory.getInstance(RequestCollectionsLeftPanel.class);
        MainFrame mainFrame = SingletonFactory.getInstance(MainFrame.class);
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle(I18nUtil.getMessage(MessageKeys.COLLECTIONS_IMPORT_SWAGGER_DIALOG_TITLE));
        int userSelection = fileChooser.showOpenDialog(mainFrame);
        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File fileToOpen = fileChooser.getSelectedFile();
            try {
                String json = FileUtil.readString(fileToOpen, StandardCharsets.UTF_8);
                CollectionParseResult parseResult = SwaggerParser.parseSwagger(json);
                if (parseResult != null) {
                    DefaultMutableTreeNode collectionNode =
                            TreeNodeBuilder.buildFromParseResult(parseResult);
                    leftPanel.getRootTreeNode().add(collectionNode);
                    leftPanel.getTreeModel().reload();
                    leftPanel.getPersistence().saveRequestGroups();
                    leftPanel.getRequestTree().expandPath(new TreePath(collectionNode.getPath()));

                    // 导入环境变量
                    importEnvironmentsFromParseResult(parseResult);

                    NotificationUtil.showSuccess(I18nUtil.getMessage(MessageKeys.COLLECTIONS_IMPORT_SUCCESS));
                } else {
                    NotificationUtil.showError(I18nUtil.getMessage(MessageKeys.COLLECTIONS_IMPORT_SWAGGER_INVALID));
                }
            } catch (Exception ex) {
                log.error("Import Swagger error", ex);
                NotificationUtil.showError(I18nUtil.getMessage(MessageKeys.COLLECTIONS_IMPORT_FAIL, ex.getMessage()));
            }
        }
    }

    /**
     * 从解析结果中导入环境变量
     */
    private void importEnvironmentsFromParseResult(CollectionParseResult parseResult) {
        if (parseResult.getEnvironments() == null || parseResult.getEnvironments().isEmpty()) {
            return;
        }

        int importedCount = 0;
        for (Environment env : parseResult.getEnvironments()) {
            try {
                EnvironmentService.saveEnvironment(env);
                importedCount++;
                log.info("导入环境变量: {} ({}个变量)", env.getName(), env.getVariableList().size());
            } catch (Exception e) {
                log.error("导入环境变量失败: {}", env.getName(), e);
            }
        }

        if (importedCount > 0) {
            log.info("成功导入 {} 个环境", importedCount);

            // 刷新 TopMenuBar 的环境下拉框
            try {
                TopMenuBar topMenuBar = SingletonFactory.getInstance(TopMenuBar.class);
                if (topMenuBar.getEnvironmentComboBox() != null) {
                    topMenuBar.getEnvironmentComboBox().reload();
                    log.debug("已刷新环境下拉框");
                }
            } catch (Exception e) {
                log.error("刷新环境下拉框失败", e);
            }
        }
    }

    // 导入HTTP文件
    private void importHttpFile() {
        RequestCollectionsLeftPanel leftPanel = SingletonFactory.getInstance(RequestCollectionsLeftPanel.class);
        MainFrame mainFrame = SingletonFactory.getInstance(MainFrame.class);
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle(I18nUtil.getMessage(MessageKeys.COLLECTIONS_IMPORT_HTTP_DIALOG_TITLE));
        // 设置文件过滤器，只显示 .http 文件
        FileFilter httpFilter = new FileFilter() {
            @Override
            public boolean accept(File f) {
                return f.isDirectory() || f.getName().toLowerCase().endsWith(".http");
            }

            @Override
            public String getDescription() {
                return "HTTP Files (*.http)";
            }
        };
        fileChooser.setFileFilter(httpFilter);
        int userSelection = fileChooser.showOpenDialog(mainFrame);
        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File fileToOpen = fileChooser.getSelectedFile();
            try {
                String content = FileUtil.readString(fileToOpen, StandardCharsets.UTF_8);
                String filename = fileToOpen.getName();
                CollectionParseResult parseResult =
                        IntelliJHttpParser.parseHttpFile(content, filename);
                if (parseResult != null) {
                    DefaultMutableTreeNode collectionNode =
                            TreeNodeBuilder.buildFromParseResult(parseResult);
                    leftPanel.getRootTreeNode().add(collectionNode);
                    leftPanel.getTreeModel().reload();
                    leftPanel.getPersistence().saveRequestGroups();
                    leftPanel.getRequestTree().expandPath(new TreePath(collectionNode.getPath()));

                    // 导入环境变量
                    importEnvironmentsFromParseResult(parseResult);

                    NotificationUtil.showSuccess(I18nUtil.getMessage(MessageKeys.COLLECTIONS_IMPORT_SUCCESS));
                } else {
                    NotificationUtil.showError(I18nUtil.getMessage(MessageKeys.COLLECTIONS_IMPORT_HTTP_INVALID));
                }
            } catch (Exception ex) {
                log.error("Import HTTP file error", ex);
                NotificationUtil.showError(I18nUtil.getMessage(MessageKeys.COLLECTIONS_IMPORT_FAIL, ex.getMessage()));
            }
        }
    }

    /**
     * 导入 ApiPost 文档
     */
    private void importApiPostCollection() {
        RequestCollectionsLeftPanel leftPanel = SingletonFactory.getInstance(RequestCollectionsLeftPanel.class);
        MainFrame mainFrame = SingletonFactory.getInstance(MainFrame.class);
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle(I18nUtil.getMessage(MessageKeys.COLLECTIONS_IMPORT_APIPOST_DIALOG_TITLE));
        int userSelection = fileChooser.showOpenDialog(mainFrame);
        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File fileToOpen = fileChooser.getSelectedFile();
            try {
                String json = FileUtil.readString(fileToOpen, StandardCharsets.UTF_8);
                CollectionParseResult parseResult =
                        ApiPostCollectionParser.parseApiPostCollection(json);
                if (parseResult != null) {
                    DefaultMutableTreeNode collectionNode =
                            TreeNodeBuilder.buildFromParseResult(parseResult);
                    leftPanel.getRootTreeNode().add(collectionNode);
                    leftPanel.getTreeModel().reload();
                    leftPanel.getPersistence().saveRequestGroups();
                    leftPanel.getRequestTree().expandPath(new TreePath(collectionNode.getPath()));
                    NotificationUtil.showSuccess(I18nUtil.getMessage(MessageKeys.COLLECTIONS_IMPORT_SUCCESS));
                } else {
                    NotificationUtil.showError(I18nUtil.getMessage(MessageKeys.COLLECTIONS_IMPORT_APIPOST_INVALID));
                }
            } catch (Exception ex) {
                log.error("Import ApiPost error", ex);
                NotificationUtil.showError(I18nUtil.getMessage(MessageKeys.COLLECTIONS_IMPORT_FAIL, ex.getMessage()));
            }
        }
    }

    private void importCurlToCollection(String defaultCurl) {
        MainFrame mainFrame = SingletonFactory.getInstance(MainFrame.class);
        String curlText;
        // 如果已经提供了 curl 文本（从剪贴板检测到的），直接使用，跳过输入对话框
        if (defaultCurl != null && !defaultCurl.trim().isEmpty()) {
            curlText = defaultCurl;
        } else {
            // 否则弹出输入对话框
            curlText = CurlImportDialog.show(mainFrame,
                    I18nUtil.getMessage(MessageKeys.COLLECTIONS_IMPORT_CURL_DIALOG_TITLE),
                    I18nUtil.getMessage(MessageKeys.COLLECTIONS_IMPORT_CURL_DIALOG_PROMPT), defaultCurl);
            if (curlText == null || curlText.trim().isEmpty()) return;
        }
        try {
            CurlRequest curlRequest = CurlParser.parse(curlText);
            if (curlRequest.url == null) {
                NotificationUtil.showError(I18nUtil.getMessage(MessageKeys.COLLECTIONS_IMPORT_CURL_PARSE_FAIL));
                return;
            }
            // 构造HttpRequestItem
            HttpRequestItem item = new HttpRequestItem();
            item.setName(curlRequest.url);
            item.setUrl(curlRequest.url);
            item.setMethod(curlRequest.method);

            if (curlRequest.headersList != null && !curlRequest.headersList.isEmpty()) {
                item.setHeadersList(curlRequest.headersList);
            }

            item.setBody(curlRequest.body);

            if (curlRequest.paramsList != null && !curlRequest.paramsList.isEmpty()) {
                item.setParamsList(curlRequest.paramsList);
            }

            if (curlRequest.formDataList != null && !curlRequest.formDataList.isEmpty()) {
                item.setFormDataList(curlRequest.formDataList);
            }

            if (curlRequest.urlencodedList != null && !curlRequest.urlencodedList.isEmpty()) {
                item.setUrlencodedList(curlRequest.urlencodedList);
            }

            if (HttpUtil.isSSERequest(item)) {
                item.setProtocol(RequestItemProtocolEnum.SSE);
            } else if (HttpUtil.isWebSocketRequest(item.getUrl())) {
                item.setProtocol(RequestItemProtocolEnum.WEBSOCKET);
            } else {
                item.setProtocol(RequestItemProtocolEnum.HTTP);
            }
            // 统一用RequestEditPanel弹窗选择分组和命名
            boolean saved = saveRequestWithGroupDialog(item);
            // 导入成功后清空剪贴板
            if (saved) {
                Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(""), null);
            }
        } catch (Exception ex) {
            NotificationUtil.showError(I18nUtil.getMessage(MessageKeys.COLLECTIONS_IMPORT_CURL_PARSE_ERROR, ex.getMessage()));
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
                NotificationUtil.showSuccess(I18nUtil.getMessage(MessageKeys.COLLECTIONS_EXPORT_SUCCESS));
            } catch (Exception ex) {
                log.error("Export error", ex);
                JOptionPane.showMessageDialog(mainFrame,
                        I18nUtil.getMessage(MessageKeys.COLLECTIONS_EXPORT_FAIL, ex.getMessage()),
                        I18nUtil.getMessage(MessageKeys.GENERAL_ERROR), JOptionPane.ERROR_MESSAGE);
            }
        }
    }


    // 递归过滤节点，支持大小写敏感和整词匹配
    private boolean filterNodes(DefaultMutableTreeNode src, DefaultMutableTreeNode dest, String keyword,
                                boolean caseSensitive, boolean wholeWord) {
        boolean matched = false;
        Object userObj = src.getUserObject();
        if (userObj instanceof Object[] obj) {
            String type = String.valueOf(obj[0]);
            if (GROUP.equals(type)) {
                String groupName = obj[1] instanceof RequestGroup group ? group.getName() : String.valueOf(obj[1]);
                DefaultMutableTreeNode groupNode = new DefaultMutableTreeNode(obj.clone());
                boolean childMatched = false;
                for (int i = 0; i < src.getChildCount(); i++) {
                    DefaultMutableTreeNode child = (DefaultMutableTreeNode) src.getChildAt(i);
                    if (filterNodes(child, groupNode, keyword, caseSensitive, wholeWord)) {
                        childMatched = true;
                    }
                }
                if (matchesText(groupName, keyword, caseSensitive, wholeWord) || childMatched) {
                    dest.add(groupNode);
                    matched = true;
                }
            } else if (REQUEST.equals(type)) {
                HttpRequestItem item = (HttpRequestItem) obj[1];
                boolean nameMatch = item.getName() != null &&
                        matchesText(item.getName(), keyword, caseSensitive, wholeWord);
                boolean urlMatch = item.getUrl() != null &&
                        matchesText(item.getUrl(), keyword, caseSensitive, wholeWord);
                if (nameMatch || urlMatch) {
                    dest.add(new DefaultMutableTreeNode(obj.clone()));
                    matched = true;
                }
            }
        } else {
            // 处理 root 节点
            boolean childMatched = false;
            for (int i = 0; i < src.getChildCount(); i++) {
                DefaultMutableTreeNode child = (DefaultMutableTreeNode) src.getChildAt(i);
                if (filterNodes(child, dest, keyword, caseSensitive, wholeWord)) {
                    childMatched = true;
                }
            }
            matched = childMatched;
        }
        return matched;
    }

    /**
     * 判断文本是否匹配关键字，支持大小写敏感和整词匹配
     */
    private boolean matchesText(String text, String keyword, boolean caseSensitive, boolean wholeWord) {
        if (text == null || keyword == null) {
            return false;
        }

        String searchText = caseSensitive ? text : text.toLowerCase();
        String searchKeyword = caseSensitive ? keyword : keyword.toLowerCase();

        if (!wholeWord) {
            // 简单包含匹配
            return searchText.contains(searchKeyword);
        }

        // 整词匹配：使用正则表达式
        // \b 表示单词边界
        String regex = "\\b" + java.util.regex.Pattern.quote(searchKeyword) + "\\b";
        int flags = caseSensitive ? 0 : java.util.regex.Pattern.CASE_INSENSITIVE;
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(regex, flags);
        return pattern.matcher(text).find();
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