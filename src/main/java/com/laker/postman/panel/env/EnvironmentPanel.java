package com.laker.postman.panel.env;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.json.JSONUtil;
import com.laker.postman.common.SingletonBasePanel;
import com.laker.postman.common.SingletonFactory;
import com.laker.postman.common.component.SearchTextField;
import com.laker.postman.common.component.button.EditButton;
import com.laker.postman.common.component.button.ExportButton;
import com.laker.postman.common.component.button.ImportButton;
import com.laker.postman.common.component.button.SaveButton;
import com.laker.postman.common.component.combobox.EnvironmentComboBox;
import com.laker.postman.common.component.list.EnvironmentListCellRenderer;
import com.laker.postman.common.component.table.EasyVariableTablePanel;
import com.laker.postman.common.constants.ModernColors;
import com.laker.postman.model.Environment;
import com.laker.postman.model.EnvironmentItem;
import com.laker.postman.model.Variable;
import com.laker.postman.model.Workspace;
import com.laker.postman.panel.topmenu.TopMenuBar;
import com.laker.postman.service.EnvironmentService;
import com.laker.postman.service.ideahttp.IntelliJHttpEnvParser;
import com.laker.postman.service.postman.PostmanEnvironmentParser;
import com.laker.postman.service.workspace.WorkspaceTransferHelper;
import com.laker.postman.util.*;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * 环境变量管理面板
 */
@Slf4j
public class EnvironmentPanel extends SingletonBasePanel {
    public static final String EXPORT_FILE_NAME = "EasyPostman-Environments.json";
    private EasyVariableTablePanel variablesTablePanel;
    private transient Environment currentEnvironment;
    private JList<EnvironmentItem> environmentList;
    private DefaultListModel<EnvironmentItem> environmentListModel;
    private JTextField searchField;
    private ImportButton importBtn;
    private String originalVariablesSnapshot; // 原始变量快照，直接用json字符串
    private boolean isLoadingData = false; // 用于控制是否正在加载数据，防止自动保存
    private SearchTextField tableSearchField; // 表格搜索框
    private JPanel toolbarPanel; // 表格工具栏面板

    @Override
    protected void initUI() {
        setLayout(new BorderLayout());
        setPreferredSize(new Dimension(700, 400));

        // 左侧环境列表面板
        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.setPreferredSize(new Dimension(250, 200));
        // 顶部搜索和导入导出按钮
        leftPanel.add(getSearchAndImportPanel(), BorderLayout.NORTH);

        // 环境列表
        environmentListModel = new DefaultListModel<>();
        environmentList = new JList<>(environmentListModel);
        environmentList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION); // 支持多选
        environmentList.setFixedCellHeight(28); // 设置每行高度
        environmentList.setCellRenderer(new EnvironmentListCellRenderer());
        environmentList.setFixedCellWidth(0); // 让JList自适应宽度
        environmentList.setVisibleRowCount(-1); // 让JList显示所有行
        JScrollPane envListScroll = new JScrollPane(environmentList);
        envListScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER); // 禁用横向滚动条
        leftPanel.add(envListScroll, BorderLayout.CENTER);

        // 右侧 导入 导出 变量表格及操作
        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));

        // 顶部工具栏：保存按钮和搜索框
        JPanel tableToolbarPanel = createTableToolbar();
        rightPanel.add(tableToolbarPanel, BorderLayout.NORTH);

        // 变量表格容器，添加边距
        JPanel tableContainer = new JPanel(new BorderLayout());
        tableContainer.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        variablesTablePanel = new EasyVariableTablePanel();
        tableContainer.add(variablesTablePanel, BorderLayout.CENTER);
        rightPanel.add(tableContainer, BorderLayout.CENTER);


        // 使用 JSplitPane 将左右两个面板组合，支持拖动调整大小
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, rightPanel);
        splitPane.setDividerLocation(250); // 设置分隔条初始位置
        splitPane.setContinuousLayout(true); // 拖动时实时更新布局
        splitPane.setResizeWeight(0.3); // 设置左侧面板调整权重（30%）

        add(splitPane, BorderLayout.CENTER);

        // 初始化表格验证和自动保存功能
        initTableValidationAndAutoSave();
    }

    /**
     * 创建表格工具栏：保存按钮和搜索框
     */
    private JPanel createTableToolbar() {
        SaveButton saveButton;
        toolbarPanel = new JPanel();
        toolbarPanel.setLayout(new BoxLayout(toolbarPanel, BoxLayout.X_AXIS));
        // 加大边距，底部添加分隔线
        toolbarPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, ModernColors.getDividerBorderColor()),
                BorderFactory.createEmptyBorder(3, 10, 3, 10)
        ));

        // 左侧弹性空间，将所有控件推到右边
        toolbarPanel.add(Box.createHorizontalGlue());

        // 批量编辑按钮
        EditButton bulkEditButton = new EditButton(IconUtil.SIZE_MEDIUM);
        bulkEditButton.setToolTipText(I18nUtil.getMessage(MessageKeys.ENV_BULK_EDIT));
        bulkEditButton.setPreferredSize(new Dimension(bulkEditButton.getPreferredSize().width, 32));
        bulkEditButton.setMaximumSize(new Dimension(bulkEditButton.getMaximumSize().width, 32));
        bulkEditButton.addActionListener(e -> showBulkEditDialog());
        toolbarPanel.add(bulkEditButton);
        toolbarPanel.add(Box.createHorizontalStrut(4)); // 按钮间距

        // 保存按钮
        saveButton = new SaveButton();
        saveButton.setPreferredSize(new Dimension(saveButton.getPreferredSize().width, 32));
        saveButton.setMaximumSize(new Dimension(saveButton.getMaximumSize().width, 32));
        saveButton.addActionListener(e -> saveVariablesManually());
        toolbarPanel.add(saveButton);
        toolbarPanel.add(Box.createHorizontalStrut(4)); // 按钮和搜索框之间的间距

        // 表格搜索框
        tableSearchField = new SearchTextField();
        tableSearchField.setPreferredSize(new Dimension(200, 32));
        tableSearchField.setMaximumSize(new Dimension(200, 32));
        tableSearchField.addActionListener(e -> filterTableRows());

        // 监听搜索选项变化，触发重新过滤
        tableSearchField.addPropertyChangeListener("caseSensitive", evt -> {
            if (!tableSearchField.getText().isEmpty()) {
                filterTableRows();
            }
        });
        tableSearchField.addPropertyChangeListener("wholeWord", evt -> {
            if (!tableSearchField.getText().isEmpty()) {
                filterTableRows();
            }
        });

        toolbarPanel.add(tableSearchField);

        return toolbarPanel;
    }


    /**
     * 初始化表格自动保存功能
     * 类似 Postman，环境变量修改后即时生效，无需手动保存
     */
    private void initTableValidationAndAutoSave() {

        // 添加表格模型监听器，实现即时自动保存（类似 Postman）
        variablesTablePanel.addTableModelListener(e -> {
            if (currentEnvironment == null || isLoadingData) return;

            // 防止在加载数据时触发自动保存
            if (e.getType() == TableModelEvent.INSERT ||
                    e.getType() == TableModelEvent.UPDATE ||
                    e.getType() == TableModelEvent.DELETE) {

                // 使用 SwingUtilities.invokeLater 确保在事件处理完成后执行保存
                SwingUtilities.invokeLater(() -> {
                    // 在拖拽期间跳过自动保存，避免保存中间状态
                    if (!isLoadingData && !variablesTablePanel.isDragging() && isVariablesChanged()) {
                        autoSaveVariables();
                    }
                });
            }
        });
    }

    /**
     * 自动保存变量（静默保存，无提示）
     * 类似 Postman 的即时保存体验
     */
    private void autoSaveVariables() {
        if (currentEnvironment == null) return;

        try {
            variablesTablePanel.stopCellEditing();
            List<Variable> variableList = variablesTablePanel.getVariableList();
            currentEnvironment.setVariableList(new ArrayList<>(variableList)); // 使用副本避免并发修改
            EnvironmentService.saveEnvironment(currentEnvironment);
            // 保存后更新快照
            originalVariablesSnapshot = JSONUtil.toJsonStr(currentEnvironment.getVariableList());
            log.debug("Auto-saved environment: {}", currentEnvironment.getName());
        } catch (Exception ex) {
            log.error("Failed to auto-save environment variables", ex);
        }
    }

    private JPanel getSearchAndImportPanel() {
        JPanel importExportPanel = new JPanel();
        importExportPanel.setLayout(new BoxLayout(importExportPanel, BoxLayout.X_AXIS));
        importExportPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        importBtn = new ImportButton();
        importBtn.addActionListener(e -> showImportMenu());

        ExportButton exportBtn = new ExportButton();
        exportBtn.addActionListener(e -> exportEnvironments());

        searchField = new SearchTextField();

        importExportPanel.add(importBtn);
        importExportPanel.add(exportBtn);
        importExportPanel.add(searchField);
        return importExportPanel;
    }

    private void showImportMenu() {
        JPopupMenu importMenu = new JPopupMenu();
        JMenuItem importEasyToolsItem = new JMenuItem(I18nUtil.getMessage(MessageKeys.ENV_MENU_IMPORT_EASY),
                IconUtil.create("icons/easy.svg", IconUtil.SIZE_MEDIUM, IconUtil.SIZE_MEDIUM));
        importEasyToolsItem.addActionListener(e -> importEnvironments());
        JMenuItem importPostmanItem = new JMenuItem(I18nUtil.getMessage(MessageKeys.ENV_MENU_IMPORT_POSTMAN),
                IconUtil.create("icons/postman.svg", IconUtil.SIZE_MEDIUM, IconUtil.SIZE_MEDIUM)); // 彩色
        importPostmanItem.addActionListener(e -> importPostmanEnvironments());
        JMenuItem importIntelliJItem = new JMenuItem(I18nUtil.getMessage(MessageKeys.ENV_MENU_IMPORT_INTELLIJ),
                IconUtil.create("icons/idea-http.svg", IconUtil.SIZE_MEDIUM, IconUtil.SIZE_MEDIUM));
        importIntelliJItem.addActionListener(e -> importIntelliJEnvironments());
        importMenu.add(importEasyToolsItem);
        importMenu.add(importPostmanItem);
        importMenu.add(importIntelliJItem);
        importMenu.show(importBtn, 0, importBtn.getHeight());
    }

    @Override
    protected void registerListeners() {
        // 联动菜单栏右上角下拉框
        EnvironmentComboBox topComboBox = SingletonFactory.getInstance(TopMenuBar.class).getEnvironmentComboBox();
        if (topComboBox != null) {
            topComboBox.setOnEnvironmentChange(env -> {
                environmentListModel.clear();
                List<Environment> envs = EnvironmentService.getAllEnvironments();
                for (Environment envItem : envs) {
                    environmentListModel.addElement(new EnvironmentItem(envItem));
                }
                if (!environmentListModel.isEmpty()) {
                    environmentList.setSelectedIndex(topComboBox.getSelectedIndex()); // 设置选中当前激活环境
                }
                loadActiveEnvironmentVariables();
            });
        }
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) {
                reloadEnvironmentList(searchField.getText());
            }

            public void removeUpdate(DocumentEvent e) {
                reloadEnvironmentList(searchField.getText());
            }

            public void changedUpdate(DocumentEvent e) {
                reloadEnvironmentList(searchField.getText());
            }
        });

        // 表格搜索框监听器
        tableSearchField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) {
                filterTableRows();
            }

            public void removeUpdate(DocumentEvent e) {
                filterTableRows();
            }

            public void changedUpdate(DocumentEvent e) {
                filterTableRows();
            }
        });
        environmentList.addListSelectionListener(e -> { // 监听环境列表左键
            if (!e.getValueIsAdjusting()) {
                EnvironmentItem item = environmentList.getSelectedValue();
                if (item == null || item.getEnvironment() == currentEnvironment) {
                    return; // 没有切换环境，不处理
                }
                currentEnvironment = item.getEnvironment();
                loadVariables(currentEnvironment);
            }
        });
        // 环境列表右键菜单
        addRightMenuList();

        // 添加手动保存快捷键（虽然有自动保存，但保留手动保存让用户有掌控感）
        addSaveKeyStroke();

        // 默认加载当前激活环境变量
        loadActiveEnvironmentVariables();

        // 环境列表加载与搜索
        reloadEnvironmentList("");

    }

    @Override
    public void updateUI() {
        super.updateUI();
        // 更新工具栏边框颜色
        if (toolbarPanel != null) {
            toolbarPanel.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 1, 0, ModernColors.getDividerBorderColor()),
                    BorderFactory.createEmptyBorder(3, 10, 3, 10)
            ));
        }
    }

    /**
     * 添加手动保存快捷键（Cmd+S / Ctrl+S）
     * 虽然已有自动保存，但保留手动保存快捷键让用户有主动掌控感
     */
    private void addSaveKeyStroke() {
        KeyStroke saveKeyStroke = KeyStroke.getKeyStroke("meta S"); // Mac Command+S
        KeyStroke saveKeyStroke2 = KeyStroke.getKeyStroke("control S"); // Windows/Linux Ctrl+S
        String actionKey = "saveEnvironmentVariables";
        this.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(saveKeyStroke, actionKey);
        this.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(saveKeyStroke2, actionKey);
        this.getActionMap().put(actionKey, new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                saveVariablesManually();
            }
        });
    }


    private void addRightMenuList() {
        JPopupMenu envListMenu = new JPopupMenu();
        JMenuItem addItem = new JMenuItem(I18nUtil.getMessage(MessageKeys.ENV_BUTTON_ADD),
                IconUtil.createThemed("icons/environments.svg", IconUtil.SIZE_SMALL, IconUtil.SIZE_SMALL));
        addItem.addActionListener(e -> addEnvironment());
        envListMenu.add(addItem);
        envListMenu.addSeparator();
        JMenuItem renameItem = new JMenuItem(I18nUtil.getMessage(MessageKeys.ENV_BUTTON_RENAME),
                IconUtil.createThemed("icons/refresh.svg", IconUtil.SIZE_SMALL, IconUtil.SIZE_SMALL));
        // 设置 F2 快捷键显示
        renameItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F2, 0));
        JMenuItem copyItem = new JMenuItem(I18nUtil.getMessage(MessageKeys.ENV_BUTTON_DUPLICATE),
                IconUtil.createThemed("icons/duplicate.svg", IconUtil.SIZE_SMALL, IconUtil.SIZE_SMALL)); // 复制菜单项
        JMenuItem deleteItem = new JMenuItem(I18nUtil.getMessage(MessageKeys.ENV_BUTTON_DELETE),
                IconUtil.createThemed("icons/close.svg", IconUtil.SIZE_SMALL, IconUtil.SIZE_SMALL));
        // 设置 Delete 快捷键显示
        deleteItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0));

        // 动态更新删除菜单项文本（显示选中数量）
        envListMenu.addPopupMenuListener(new PopupMenuListener() {
            @Override
            public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
                int selectedCount = environmentList.getSelectedIndices().length;
                if (selectedCount > 1) {
                    deleteItem.setText(I18nUtil.getMessage(MessageKeys.ENV_BUTTON_DELETE) + " (" + selectedCount + ")");
                } else {
                    deleteItem.setText(I18nUtil.getMessage(MessageKeys.ENV_BUTTON_DELETE));
                }

                // 复制和重命名只在单选时可用
                renameItem.setEnabled(selectedCount == 1);
                copyItem.setEnabled(selectedCount == 1);
            }

            @Override
            public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
                // 菜单隐藏时无需处理
            }

            @Override
            public void popupMenuCanceled(PopupMenuEvent e) {
                // 菜单取消时无需处理
            }
        });

        JMenuItem exportPostmanItem = new JMenuItem(I18nUtil.getMessage(MessageKeys.ENV_BUTTON_EXPORT_POSTMAN),
                IconUtil.create("icons/postman.svg", IconUtil.SIZE_SMALL, IconUtil.SIZE_SMALL)); // 彩色
        exportPostmanItem.addActionListener(e -> exportSelectedEnvironmentAsPostman());
        renameItem.addActionListener(e -> renameSelectedEnvironment());
        copyItem.addActionListener(e -> copySelectedEnvironment()); // 复制事件
        deleteItem.addActionListener(e -> deleteSelectedEnvironments()); // 改为批量删除
        envListMenu.add(renameItem);
        envListMenu.add(copyItem);
        envListMenu.add(deleteItem);
        envListMenu.addSeparator();
        envListMenu.add(exportPostmanItem);

        // 转移到其他工作区
        JMenuItem moveToWorkspaceItem = new JMenuItem(I18nUtil.getMessage(MessageKeys.WORKSPACE_TRANSFER_MENU_ITEM),
                IconUtil.createThemed("icons/workspace.svg", IconUtil.SIZE_SMALL, IconUtil.SIZE_SMALL));
        moveToWorkspaceItem.addActionListener(e -> moveEnvironmentToWorkspace());
        envListMenu.add(moveToWorkspaceItem);

        // 添加键盘监听器，支持 F2 重命名和 Delete 删除
        environmentList.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                int selectedCount = environmentList.getSelectedIndices().length;
                if (selectedCount > 0) {
                    if (e.getKeyCode() == KeyEvent.VK_F2 && selectedCount == 1) {
                        // F2 重命名（仅单选时）
                        renameSelectedEnvironment();
                    } else if (e.getKeyCode() == KeyEvent.VK_DELETE || e.getKeyCode() == KeyEvent.VK_BACK_SPACE) {
                        // Delete 或 Backspace 批量删除
                        deleteSelectedEnvironments();
                    }
                }
            }
        });

        environmentList.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (e.isPopupTrigger() || e.getButton() == MouseEvent.BUTTON3) { // 右键菜单
                    int idx = environmentList.locationToIndex(e.getPoint());
                    if (idx >= 0) {
                        // 只有当点击的项目不在当前选中列表中时，才改变选择
                        int[] selectedIndices = environmentList.getSelectedIndices();
                        boolean isSelected = false;
                        for (int selectedIdx : selectedIndices) {
                            if (selectedIdx == idx) {
                                isSelected = true;
                                break;
                            }
                        }
                        // 如果点击的项目未被选中，则设置为当前选中项
                        if (!isSelected) {
                            environmentList.setSelectedIndex(idx);
                        }
                        // 如果点击的项目已经被选中，则保持当前的多选状态
                    }
                    envListMenu.show(environmentList, e.getX(), e.getY());
                }
                // 双击激活环境并联动下拉框
                if (e.getButton() == MouseEvent.BUTTON1 && e.getClickCount() == 2) {
                    int idx = environmentList.locationToIndex(e.getPoint());
                    if (idx >= 0) {
                        environmentList.setSelectedIndex(idx);
                        EnvironmentItem item = environmentList.getModel().getElementAt(idx);
                        if (item != null) {
                            Environment env = item.getEnvironment();
                            // 激活环境
                            EnvironmentService.setActiveEnvironment(env.getId());
                            // 联动顶部下拉框
                            EnvironmentComboBox comboBox = SingletonFactory.getInstance(TopMenuBar.class).getEnvironmentComboBox();
                            if (comboBox != null) {
                                comboBox.setSelectedEnvironment(env);
                            }
                            // 刷新面板
                            refreshUI();
                        }
                    }
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger()) mousePressed(e);
            }
        });
        // 拖拽排序支持
        environmentList.setDragEnabled(true);
        environmentList.setDropMode(DropMode.INSERT);
        environmentList.setTransferHandler(new TransferHandler() {
            private int fromIndex = -1;

            @Override
            protected Transferable createTransferable(JComponent c) {
                fromIndex = environmentList.getSelectedIndex();
                EnvironmentItem selected = environmentList.getSelectedValue();
                return new StringSelection(selected != null ? selected.toString() : "");
            }

            @Override
            public int getSourceActions(JComponent c) {
                return MOVE;
            }

            @Override
            public boolean canImport(TransferSupport support) {
                return support.isDrop();
            }

            @Override
            public boolean importData(TransferSupport support) {
                if (!canImport(support)) return false;
                JList.DropLocation dl = (JList.DropLocation) support.getDropLocation();
                int toIndex = dl.getIndex();
                if (fromIndex < 0 || toIndex < 0 || fromIndex == toIndex) return false;
                EnvironmentItem moved = environmentListModel.getElementAt(fromIndex);
                environmentListModel.remove(fromIndex);
                if (toIndex > fromIndex) toIndex--;
                environmentListModel.add(toIndex, moved);
                environmentList.setSelectedIndex(toIndex);
                // 1. 同步顺序到 EnvironmentService
                persistEnvironmentOrder();
                // 2. 同步到顶部下拉框
                syncComboBoxOrder();
                return true;
            }
        });
    }

    /**
     * 只加载当前激活环境变量
     */
    public void loadActiveEnvironmentVariables() {
        Environment env = EnvironmentService.getActiveEnvironment();
        currentEnvironment = env;
        loadVariables(env);
    }

    private void loadVariables(Environment env) {
        // 设置标志位，开始加载数据（必须在最前面，防止任何操作触发自动保存）
        isLoadingData = true;

        try {
            variablesTablePanel.stopCellEditing();
            currentEnvironment = env;
            variablesTablePanel.clear();

            if (env != null) {
                variablesTablePanel.setVariableList(env.getVariableList());
                originalVariablesSnapshot = JSONUtil.toJsonStr(env.getVariableList()); // 用rows做快照，保证同步
            } else {
                variablesTablePanel.clear();
                originalVariablesSnapshot = JSONUtil.toJsonStr(new ArrayList<>()); // 空快照
            }
        } finally {
            // 使用 finally 确保标志位一定会被清除，即使发生异常
            isLoadingData = false;
        }
    }

    /**
     * 保存当前环境的变量到文件（内部调用，静默保存）
     * 供自动保存和程序内部调用使用
     */
    private void saveVariables() {
        if (currentEnvironment == null) return;
        variablesTablePanel.stopCellEditing();

        // 保存到新格式 variableList
        List<Variable> variableList = variablesTablePanel.getVariableList();
        currentEnvironment.setVariableList(new ArrayList<>(variableList)); // 使用副本避免并发修改
        EnvironmentService.saveEnvironment(currentEnvironment);
        // 保存后更新快照为json字符串
        originalVariablesSnapshot = JSONUtil.toJsonStr(currentEnvironment.getVariableList());
    }

    /**
     * 手动保存环境变量（用户主动按 Cmd+S / Ctrl+S 时调用）
     * 显示保存成功通知，给用户反馈
     */
    private void saveVariablesManually() {
        if (currentEnvironment == null) return;

        // 调用保存逻辑
        saveVariables();

        // 显示保存成功通知（只有手动保存才显示，自动保存不打扰用户）
        NotificationUtil.showSuccess(I18nUtil.getMessage(MessageKeys.ENV_DIALOG_SAVE_SUCCESS));
    }

    /**
     * 导出所有环境变量为JSON文件
     */
    private void exportEnvironments() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle(I18nUtil.getMessage(MessageKeys.ENV_DIALOG_EXPORT_TITLE));
        fileChooser.setSelectedFile(new File(EXPORT_FILE_NAME));
        int userSelection = fileChooser.showSaveDialog(this);
        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File fileToSave = fileChooser.getSelectedFile();
            try (Writer writer = new OutputStreamWriter(new FileOutputStream(fileToSave), StandardCharsets.UTF_8)) {
                java.util.List<Environment> envs = EnvironmentService.getAllEnvironments();
                writer.write(JSONUtil.toJsonPrettyStr(envs));
                NotificationUtil.showSuccess(I18nUtil.getMessage(MessageKeys.ENV_DIALOG_EXPORT_SUCCESS));
            } catch (Exception ex) {
                log.error("Export Error", ex);
                JOptionPane.showMessageDialog(this,
                        I18nUtil.getMessage(MessageKeys.ENV_DIALOG_EXPORT_FAIL, ex.getMessage()),
                        I18nUtil.getMessage(MessageKeys.GENERAL_ERROR), JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /**
     * 导入环境变量JSON文件
     */
    private void importEnvironments() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle(I18nUtil.getMessage(MessageKeys.ENV_DIALOG_IMPORT_EASY_TITLE));
        int userSelection = fileChooser.showOpenDialog(this);
        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File fileToOpen = fileChooser.getSelectedFile();
            try {
                java.util.List<Environment> envs = JSONUtil.toList(JSONUtil.readJSONArray(fileToOpen, StandardCharsets.UTF_8), Environment.class);
                // 导入新环境
                refreshListAndComboFromAdd(envs);
            } catch (Exception ex) {
                log.error("Import Error", ex);
                JOptionPane.showMessageDialog(this,
                        I18nUtil.getMessage(MessageKeys.ENV_DIALOG_IMPORT_EASY_FAIL, ex.getMessage()),
                        I18nUtil.getMessage(MessageKeys.GENERAL_ERROR), JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void refreshListAndComboFromAdd(List<Environment> envs) {
        EnvironmentComboBox environmentComboBox = SingletonFactory.getInstance(TopMenuBar.class).getEnvironmentComboBox();
        for (Environment env : envs) {
            EnvironmentService.saveEnvironment(env);
            environmentComboBox.addItem(new EnvironmentItem(env)); // 添加到下拉框
            environmentListModel.addElement(new EnvironmentItem(env)); // 添加到列表
        }
        NotificationUtil.showSuccess(I18nUtil.getMessage(MessageKeys.ENV_DIALOG_IMPORT_EASY_SUCCESS));
    }

    /**
     * 导入Postman环境变量JSON文件
     */
    private void importPostmanEnvironments() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle(I18nUtil.getMessage(MessageKeys.ENV_DIALOG_IMPORT_POSTMAN_TITLE));
        int userSelection = fileChooser.showOpenDialog(this);
        if (userSelection == JFileChooser.APPROVE_OPTION) {
            java.io.File fileToOpen = fileChooser.getSelectedFile();
            try {
                String json = FileUtil.readString(fileToOpen, StandardCharsets.UTF_8);
                List<Environment> envs = PostmanEnvironmentParser.parsePostmanEnvironments(json);
                if (!envs.isEmpty()) {
                    // 导入新环境
                    refreshListAndComboFromAdd(envs);
                } else {
                    JOptionPane.showMessageDialog(this,
                            I18nUtil.getMessage(MessageKeys.ENV_DIALOG_IMPORT_POSTMAN_INVALID),
                            I18nUtil.getMessage(MessageKeys.ENV_DIALOG_IMPORT_POSTMAN_TITLE), JOptionPane.WARNING_MESSAGE);
                }
            } catch (Exception ex) {
                log.error("Import Error", ex);
                JOptionPane.showMessageDialog(this,
                        I18nUtil.getMessage(MessageKeys.ENV_DIALOG_IMPORT_POSTMAN_FAIL, ex.getMessage()),
                        I18nUtil.getMessage(MessageKeys.GENERAL_ERROR), JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /**
     * 导入IntelliJ IDEA HTTP Client环境变量JSON文件
     */
    private void importIntelliJEnvironments() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle(I18nUtil.getMessage(MessageKeys.ENV_DIALOG_IMPORT_INTELLIJ_TITLE));
        int userSelection = fileChooser.showOpenDialog(this);
        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File fileToOpen = fileChooser.getSelectedFile();
            try {
                String json = FileUtil.readString(fileToOpen, StandardCharsets.UTF_8);
                List<Environment> envs = IntelliJHttpEnvParser.parseIntelliJEnvironments(json);
                if (!envs.isEmpty()) {
                    // 导入新环境
                    refreshListAndComboFromAdd(envs);
                } else {
                    JOptionPane.showMessageDialog(this,
                            I18nUtil.getMessage(MessageKeys.ENV_DIALOG_IMPORT_INTELLIJ_INVALID),
                            I18nUtil.getMessage(MessageKeys.ENV_DIALOG_IMPORT_INTELLIJ_TITLE), JOptionPane.WARNING_MESSAGE);
                }
            } catch (Exception ex) {
                log.error("Import IntelliJ Error", ex);
                JOptionPane.showMessageDialog(this,
                        I18nUtil.getMessage(MessageKeys.ENV_DIALOG_IMPORT_INTELLIJ_FAIL, ex.getMessage()),
                        I18nUtil.getMessage(MessageKeys.GENERAL_ERROR), JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    // 新增环境
    private void addEnvironment() {
        String name = JOptionPane.showInputDialog(this,
                I18nUtil.getMessage(MessageKeys.ENV_DIALOG_ADD_PROMPT),
                I18nUtil.getMessage(MessageKeys.ENV_DIALOG_ADD_TITLE), JOptionPane.PLAIN_MESSAGE);
        if (name != null && !name.trim().isEmpty()) {
            Environment env = new Environment(name.trim());
            env.setId("env-" + IdUtil.simpleUUID());
            EnvironmentService.saveEnvironment(env);
            environmentListModel.addElement(new EnvironmentItem(env));
            environmentList.setSelectedValue(new EnvironmentItem(env), true);
            EnvironmentComboBox environmentComboBox = SingletonFactory.getInstance(TopMenuBar.class).getEnvironmentComboBox();
            if (environmentComboBox != null) {
                environmentComboBox.addItem(new EnvironmentItem(env));
            }
        }
    }

    private void reloadEnvironmentList(String filter) {
        environmentListModel.clear();
        java.util.List<Environment> envs = EnvironmentService.getAllEnvironments();
        int activeIdx = -1;
        for (Environment env : envs) {
            if (filter == null || filter.isEmpty() || env.getName().toLowerCase().contains(filter.toLowerCase())) {
                environmentListModel.addElement(new EnvironmentItem(env));
                if (env.isActive()) {
                    activeIdx = environmentListModel.size() - 1;
                }
            }
        }
        if (!environmentListModel.isEmpty()) {
            environmentList.setSelectedIndex(Math.max(activeIdx, 0));
        }
    }

    private void renameSelectedEnvironment() {
        EnvironmentItem item = environmentList.getSelectedValue();
        if (item == null) return;
        Environment env = item.getEnvironment();
        Object result = JOptionPane.showInputDialog(this,
                I18nUtil.getMessage(MessageKeys.ENV_DIALOG_RENAME_PROMPT),
                I18nUtil.getMessage(MessageKeys.ENV_DIALOG_RENAME_TITLE),
                JOptionPane.PLAIN_MESSAGE, null, null, env.getName());
        if (result != null) {
            String newName = result.toString().trim();
            if (!newName.isEmpty() && !newName.equals(env.getName())) {
                env.setName(newName);
                EnvironmentService.saveEnvironment(env);
                environmentListModel.setElementAt(new EnvironmentItem(env), environmentList.getSelectedIndex());
                // 同步刷新顶部环境下拉框
                SingletonFactory.getInstance(TopMenuBar.class).getEnvironmentComboBox().reload();
            } else {
                JOptionPane.showMessageDialog(this,
                        I18nUtil.getMessage(MessageKeys.ENV_DIALOG_RENAME_FAIL),
                        I18nUtil.getMessage(MessageKeys.ENV_DIALOG_SAVE_CHANGES_TITLE), JOptionPane.WARNING_MESSAGE);
            }
        }
    }

    /**
     * 批量删除选中的环境
     */
    private void deleteSelectedEnvironments() {
        List<EnvironmentItem> selectedItems = environmentList.getSelectedValuesList();
        if (selectedItems == null || selectedItems.isEmpty()) {
            return;
        }

        int selectedCount = selectedItems.size();
        String message;
        String title = I18nUtil.getMessage(MessageKeys.ENV_DIALOG_DELETE_TITLE);

        if (selectedCount == 1) {
            // 单个删除
            Environment env = selectedItems.get(0).getEnvironment();
            message = I18nUtil.getMessage(MessageKeys.ENV_DIALOG_DELETE_PROMPT, env.getName());
        } else {
            // 批量删除
            message = I18nUtil.getMessage(MessageKeys.ENV_DIALOG_DELETE_BATCH_PROMPT, selectedCount);
        }

        int confirm = JOptionPane.showConfirmDialog(this,
                message,
                title,
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);

        if (confirm == JOptionPane.YES_OPTION) {
            int deletedCount = 0;
            // 删除选中的环境
            for (EnvironmentItem item : selectedItems) {
                try {
                    Environment env = item.getEnvironment();
                    environmentListModel.removeElement(item);
                    EnvironmentService.deleteEnvironment(env.getId());
                    deletedCount++;
                    log.info("已删除环境: {}", env.getName());
                } catch (Exception e) {
                    log.error("删除环境失败: {}", item.getEnvironment().getName(), e);
                }
            }

            // 刷新顶部下拉框
            SingletonFactory.getInstance(TopMenuBar.class).getEnvironmentComboBox().reload();

            // 设置当前的变量表格为激活环境
            loadActiveEnvironmentVariables();

            // 显示删除成功消息
            if (deletedCount > 0) {
                NotificationUtil.showSuccess(
                        I18nUtil.getMessage(MessageKeys.ENV_DIALOG_DELETE_SUCCESS, deletedCount)
                );
            }
        }
    }


    // 复制环境方法
    private void copySelectedEnvironment() {
        EnvironmentItem item = environmentList.getSelectedValue();
        if (item == null) return;
        Environment env = item.getEnvironment();
        try {
            Environment copy = new Environment(env.getName() + " " + I18nUtil.getMessage(MessageKeys.ENV_NAME_COPY_SUFFIX));
            copy.setId("env-" + IdUtil.simpleUUID());
            // 复制变量
            for (String key : env.getVariables().keySet()) {
                copy.addVariable(key, env.getVariable(key));
            }
            EnvironmentService.saveEnvironment(copy);
            EnvironmentItem copyItem = new EnvironmentItem(copy);
            environmentListModel.addElement(copyItem);
            EnvironmentComboBox environmentComboBox = SingletonFactory.getInstance(TopMenuBar.class).getEnvironmentComboBox();
            if (environmentComboBox != null) {
                environmentComboBox.addItem(copyItem);
            }
            environmentList.setSelectedValue(copyItem, true);
        } catch (Exception ex) {
            log.error("复制环境失败", ex);
            JOptionPane.showMessageDialog(this,
                    I18nUtil.getMessage(MessageKeys.ENV_DIALOG_COPY_FAIL, ex.getMessage()),
                    I18nUtil.getMessage(MessageKeys.GENERAL_ERROR), JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * 刷新整个环境面板（列表和变量表格，保持激活环境高亮和选中）
     */
    public void refreshUI() {
        // 获取当前激活环境id
        Environment active = EnvironmentService.getActiveEnvironment();
        String activeId = active != null ? active.getId() : null;
        // 重新加载环境列表
        environmentListModel.clear();
        java.util.List<Environment> envs = EnvironmentService.getAllEnvironments();
        int selectIdx = -1;
        for (int i = 0; i < envs.size(); i++) {
            Environment env = envs.get(i);
            EnvironmentItem item = new EnvironmentItem(env);
            environmentListModel.addElement(item);
            if (activeId != null && activeId.equals(env.getId())) {
                selectIdx = i;
            }
        }
        // 先取消选中再选中，强制触发 selection 事件，保证表格刷新
        environmentList.clearSelection();
        if (selectIdx >= 0) {
            environmentList.setSelectedIndex(selectIdx);
            environmentList.ensureIndexIsVisible(selectIdx);
        }
        // 强制刷新变量表格，防止selection事件未触发
        EnvironmentItem selectedItem = environmentList.getSelectedValue();
        if (selectedItem != null) {
            loadVariables(selectedItem.getEnvironment());
        } else {
            variablesTablePanel.clear();
        }
    }

    // 判断当前表格内容和快照是否一致，使用JSON序列化比较
    private boolean isVariablesChanged() {
        String curJson = JSONUtil.toJsonStr(variablesTablePanel.getVariableList());
        boolean isVariablesChanged = !CharSequenceUtil.equals(curJson, originalVariablesSnapshot);
        if (isVariablesChanged) {
            log.debug("env name: {}", currentEnvironment != null ? currentEnvironment.getName() : "null");
            log.debug("current  variables: {}", curJson);
            log.debug("original variables: {}", originalVariablesSnapshot);
        }
        return isVariablesChanged;
    }

    // 导出选中环境为Postman格式
    private void exportSelectedEnvironmentAsPostman() {
        EnvironmentItem item = environmentList.getSelectedValue();
        if (item == null) return;
        Environment env = item.getEnvironment();
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle(I18nUtil.getMessage(MessageKeys.ENV_DIALOG_EXPORT_POSTMAN_TITLE));
        fileChooser.setSelectedFile(new File(env.getName() + "-postman-env.json"));
        int userSelection = fileChooser.showSaveDialog(this);
        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File fileToSave = fileChooser.getSelectedFile();
            try {
                // 只导出当前环境为Postman格式
                String postmanEnvJson = PostmanEnvironmentParser.toPostmanEnvironmentJson(env);
                FileUtil.writeUtf8String(postmanEnvJson, fileToSave);
                NotificationUtil.showSuccess(I18nUtil.getMessage(MessageKeys.ENV_DIALOG_EXPORT_POSTMAN_SUCCESS));
            } catch (Exception ex) {
                log.error("导出Postman环境失败", ex);
                JOptionPane.showMessageDialog(this,
                        I18nUtil.getMessage(MessageKeys.ENV_DIALOG_EXPORT_POSTMAN_FAIL, ex.getMessage()),
                        I18nUtil.getMessage(MessageKeys.GENERAL_ERROR), JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /**
     * 拖拽后持久化顺序
     */
    private void persistEnvironmentOrder() {
        List<String> idOrder = new ArrayList<>();
        for (int i = 0; i < environmentListModel.size(); i++) {
            idOrder.add(environmentListModel.get(i).getEnvironment().getId());
        }
        EnvironmentService.saveEnvironmentOrder(idOrder);
    }

    /**
     * 拖拽后同步顶部下拉框顺序
     */
    private void syncComboBoxOrder() {
        EnvironmentComboBox comboBox = SingletonFactory.getInstance(TopMenuBar.class).getEnvironmentComboBox();
        if (comboBox != null) {
            List<EnvironmentItem> items = new ArrayList<>();
            for (int i = 0; i < environmentListModel.size(); i++) {
                items.add(environmentListModel.get(i));
            }
            comboBox.setModel(new DefaultComboBoxModel<>(items.toArray(new EnvironmentItem[0])));
        }
    }

    /**
     * 切换到指定工作区的环境数据文件，并刷新UI
     */
    public void switchWorkspaceAndRefreshUI(String envFilePath) {
        EnvironmentService.setDataFilePath(envFilePath);
        this.refreshUI();
        // 同步刷新顶部环境下拉框
        SingletonFactory.getInstance(TopMenuBar.class).getEnvironmentComboBox().reload();
    }

    /**
     * 转移环境到其他工作区
     */
    private void moveEnvironmentToWorkspace() {
        EnvironmentItem selectedItem = environmentList.getSelectedValue();
        if (selectedItem == null) {
            return;
        }

        Environment environment = selectedItem.getEnvironment();

        // 使用工作区转移辅助类（显示成功消息）
        WorkspaceTransferHelper.transferToWorkspace(
                environment.getName(),
                (targetWorkspace, itemName) -> performEnvironmentMove(environment, targetWorkspace)
        );
    }


    /**
     * 执行环境转移操作
     */
    private void performEnvironmentMove(Environment environment, Workspace targetWorkspace) {
        // 1. 深拷贝环境对象
        Environment copiedEnvironment = new Environment(environment.getName());
        copiedEnvironment.setId(environment.getId()); // 保持相同的ID
        // 复制所有变量
        for (String key : environment.getVariables().keySet()) {
            copiedEnvironment.addVariable(key, environment.getVariable(key));
        }

        // 2. 获取目标工作区的环境文件路径
        String targetEnvPath = SystemUtil.getEnvPathForWorkspace(targetWorkspace);

        // 3. 临时切换到目标工作区的环境服务
        String originalDataFilePath = EnvironmentService.getDataFilePath();
        try {
            // 切换到目标工作区
            EnvironmentService.setDataFilePath(targetEnvPath);

            // 4. 将环境保存到目标工作区
            EnvironmentService.saveEnvironment(copiedEnvironment);

            // 5. 切换回原工作区并删除原环境
            EnvironmentService.setDataFilePath(originalDataFilePath);
            EnvironmentService.deleteEnvironment(environment.getId());

            // 6. 刷新当前面板
            refreshUI();

            // 7. 刷新顶部环境下拉框
            SingletonFactory.getInstance(TopMenuBar.class).getEnvironmentComboBox().reload();

            log.info("Successfully moved environment '{}' to workspace '{}'",
                    environment.getName(), targetWorkspace.getName());

        } catch (Exception e) {
            // 如果出现异常，确保恢复原来的数据文件路径
            EnvironmentService.setDataFilePath(originalDataFilePath);
            throw new RuntimeException("转移环境失败: " + e.getMessage(), e);
        }
    }

    /**
     * 过滤表格行，根据搜索框内容筛选显示符合条件的行
     * 搜索范围：Name 列和 Value 列
     */
    private void filterTableRows() {
        String keyword = tableSearchField.getText();
        boolean caseSensitive = tableSearchField.isCaseSensitive();
        boolean wholeWord = tableSearchField.isWholeWord();

        if (keyword == null || keyword.trim().isEmpty()) {
            // 清空搜索时，显示所有行
            variablesTablePanel.getTable().setRowSorter(null);
            return;
        }

        // 使用 TableRowSorter 进行过滤
        TableRowSorter<TableModel> sorter =
                new TableRowSorter<>(variablesTablePanel.getTable().getModel());

        // 转换关键字用于搜索
        final String searchKeyword = caseSensitive ? keyword : keyword.toLowerCase();

        // 创建过滤器
        RowFilter<TableModel, Object> rowFilter = new RowFilter<>() {
            @Override
            public boolean include(Entry<? extends TableModel, ?> entry) {
                // 获取 Name 和 Value 列的值（列索引：1=Name, 2=Value）
                Object nameObj = entry.getValue(1);
                Object valueObj = entry.getValue(2);

                String name = nameObj != null ? nameObj.toString() : "";
                String value = valueObj != null ? valueObj.toString() : "";

                // 转换为搜索文本
                String searchName = caseSensitive ? name : name.toLowerCase();
                String searchValue = caseSensitive ? value : value.toLowerCase();

                // 判断是否匹配
                if (wholeWord) {
                    // 整词匹配
                    return matchesWholeWord(searchName, searchKeyword) ||
                            matchesWholeWord(searchValue, searchKeyword);
                } else {
                    // 包含匹配
                    return searchName.contains(searchKeyword) ||
                            searchValue.contains(searchKeyword);
                }
            }
        };

        sorter.setRowFilter(rowFilter);
        variablesTablePanel.getTable().setRowSorter(sorter);
    }

    /**
     * 判断文本中是否包含整词匹配的关键字
     */
    private boolean matchesWholeWord(String text, String keyword) {
        if (text == null || keyword == null) {
            return false;
        }

        int index = 0;
        while ((index = text.indexOf(keyword, index)) != -1) {
            int start = index;
            int end = index + keyword.length();

            // 检查前一个字符
            if (start > 0) {
                char prevChar = text.charAt(start - 1);
                if (Character.isLetterOrDigit(prevChar) || prevChar == '_') {
                    index++;
                    continue;
                }
            }

            // 检查后一个字符
            if (end < text.length()) {
                char nextChar = text.charAt(end);
                if (Character.isLetterOrDigit(nextChar) || nextChar == '_') {
                    index++;
                    continue;
                }
            }

            return true;
        }

        return false;
    }

    /**
     * 显示批量编辑对话框
     * 支持以 "Key: Value" 格式批量粘贴和编辑环境变量
     */
    private void showBulkEditDialog() {
        if (currentEnvironment == null) {
            NotificationUtil.showWarning("Please select an environment first");
            return;
        }

        // 1. 将当前表格数据转换为文本格式（Key: Value\n）
        StringBuilder text = new StringBuilder();
        List<Variable> currentVariables = variablesTablePanel.getVariableList();
        for (Variable variable : currentVariables) {
            if (!variable.getKey().isEmpty()) {
                text.append(variable.getKey()).append(": ").append(variable.getValue()).append("\n");
            }
        }

        // 2. 创建文本编辑区域
        JTextArea textArea = new JTextArea(text.toString());
        textArea.setLineWrap(false);
        textArea.setTabSize(4);
        // 设置背景色，使其看起来像可编辑区域
        textArea.setBackground(ModernColors.getInputBackgroundColor());
        textArea.setForeground(ModernColors.getTextPrimary());
        textArea.setCaretColor(ModernColors.PRIMARY);

        // 将光标定位到文本末尾
        textArea.setCaretPosition(textArea.getDocument().getLength());

        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setPreferredSize(new Dimension(600, 400));

        // 3. 创建提示标签 - 使用国际化，垂直排列
        JPanel hintPanel = new JPanel();
        hintPanel.setLayout(new BoxLayout(hintPanel, BoxLayout.Y_AXIS));
        hintPanel.setOpaque(false);

        // 主提示文本
        JLabel hintLabel = new JLabel(I18nUtil.getMessage(MessageKeys.ENV_BULK_EDIT_HINT));
        hintLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        hintLabel.setForeground(ModernColors.getTextPrimary());

        // 支持格式说明
        JLabel formatLabel = new JLabel(I18nUtil.getMessage(MessageKeys.ENV_BULK_EDIT_SUPPORTED_FORMATS));
        formatLabel.setForeground(ModernColors.getTextSecondary());
        formatLabel.setFont(FontsUtil.getDefaultFontWithOffset(Font.PLAIN, -2));
        formatLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        hintPanel.add(hintLabel);
        hintPanel.add(Box.createVerticalStrut(6)); // 添加间距
        hintPanel.add(formatLabel);
        hintPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        // 4. 组装内容面板
        JPanel contentPanel = new JPanel(new BorderLayout(0, 5));
        contentPanel.add(hintPanel, BorderLayout.NORTH);
        contentPanel.add(scrollPane, BorderLayout.CENTER);
        contentPanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));

        // 5. 创建按钮面板
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        JButton okButton = new JButton(I18nUtil.getMessage(MessageKeys.GENERAL_OK));
        JButton cancelButton = new JButton(I18nUtil.getMessage(MessageKeys.BUTTON_CANCEL));

        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);

        // 6. 创建自定义对话框
        Window window = SwingUtilities.getWindowAncestor(this);
        JDialog dialog = new JDialog(window, I18nUtil.getMessage(MessageKeys.ENV_BULK_EDIT_VARIABLES), Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setLayout(new BorderLayout());
        dialog.add(contentPanel, BorderLayout.CENTER);
        dialog.add(buttonPanel, BorderLayout.SOUTH);

        // 设置对话框属性
        dialog.setSize(650, 400);
        dialog.setMinimumSize(new Dimension(500, 300));
        dialog.setResizable(true);
        dialog.setLocationRelativeTo(this);

        // 7. 按钮事件处理
        okButton.addActionListener(e -> {
            parseBulkText(textArea.getText());
            dialog.dispose();
        });

        cancelButton.addActionListener(e -> dialog.dispose());

        // 8. 支持 ESC 键关闭对话框
        dialog.getRootPane().registerKeyboardAction(
                e -> dialog.dispose(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW
        );

        // 9. 设置默认按钮
        dialog.getRootPane().setDefaultButton(okButton);

        // 10. 显示对话框
        dialog.setVisible(true);
    }

    /**
     * 解析批量编辑的文本内容
     * 支持格式：
     * - Key: Value
     * - Key:Value
     * - Key = Value
     * - Key=Value
     * 空行和注释会被忽略
     */
    private void parseBulkText(String text) {
        if (text == null || text.trim().isEmpty()) {
            // 如果文本为空，清空所有变量
            variablesTablePanel.setVariableList(new ArrayList<>());
            return;
        }

        List<Variable> variables = new ArrayList<>();
        String[] lines = text.split("\n");

        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("#") || line.startsWith("//")) {
                continue; // 忽略空行和注释
            }

            // 支持 ":" 和 "=" 两种分隔符
            String[] parts = null;
            if (line.contains(":")) {
                parts = line.split(":", 2);
            } else if (line.contains("=")) {
                parts = line.split("=", 2);
            }

            if (parts != null && parts.length == 2) {
                String key = parts[0].trim();
                String value = parts[1].trim();
                if (!key.isEmpty()) {
                    variables.add(new Variable(true, key, value));
                }
            } else if (line.contains(":") || line.contains("=")) {
                // 如果包含分隔符但解析失败，可能是值为空的情况
                String key = line.replaceAll("[=:].*", "").trim();
                if (!key.isEmpty()) {
                    variables.add(new Variable(true, key, ""));
                }
            }
        }

        // 更新表格数据
        variablesTablePanel.setVariableList(variables);
    }
}
