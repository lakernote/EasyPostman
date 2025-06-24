package com.laker.postman.panel.env;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.laker.postman.common.SingletonFactory;
import com.laker.postman.common.combobox.EnvironmentComboBox;
import com.laker.postman.common.list.EnvironmentListCellRenderer;
import com.laker.postman.common.panel.BasePanel;
import com.laker.postman.common.panel.TopMenuBarPanel;
import com.laker.postman.common.table.map.EasyNameValueTablePanel;
import com.laker.postman.model.Environment;
import com.laker.postman.model.EnvironmentItem;
import com.laker.postman.service.EnvironmentService;
import com.laker.postman.util.PostmanImport;
import jiconfont.icons.font_awesome.FontAwesome;
import jiconfont.swing.IconFontSwing;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 环境变量管理面板
 */
@Slf4j
public class EnvironmentPanel extends BasePanel {
    private EasyNameValueTablePanel variablesTablePanel;
    private Environment currentEnvironment;
    private JList<EnvironmentItem> environmentList;
    private DefaultListModel<EnvironmentItem> environmentListModel;
    private JTextField searchField;
    private JButton addEnvButton;

    private String originalVariablesSnapshot; // 原始变量快照，直接用json字符串

    @Override
    protected void initUI() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createMatteBorder(0, 1, 0, 0, Color.LIGHT_GRAY));
        setPreferredSize(new Dimension(700, 400));

        // 左侧环境列表面板
        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.setPreferredSize(new Dimension(250, 400));
        // 顶部搜索+新增
        JPanel searchPanel = new JPanel();
        searchPanel.setLayout(new BorderLayout(5, 5));
        searchPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        searchField = new JTextField();
        searchField.setToolTipText("搜索环境");

        addEnvButton = new JButton(new FlatSVGIcon("icons/plus.svg", 20, 20));
        addEnvButton.setToolTipText("新增环境");
        addEnvButton.setFocusable(false);

        searchPanel.add(searchField, BorderLayout.CENTER);
        searchPanel.add(addEnvButton, BorderLayout.EAST);
        leftPanel.add(searchPanel, BorderLayout.NORTH);

        // 环境列表
        environmentListModel = new DefaultListModel<>();
        environmentList = new JList<>(environmentListModel);
        environmentList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        environmentList.setCellRenderer(new EnvironmentListCellRenderer());
        environmentList.setFixedCellWidth(0); // 让JList自适应宽度
        environmentList.setVisibleRowCount(-1); // 让JList显示所有行
        JScrollPane envListScroll = new JScrollPane(environmentList);
        envListScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER); // 禁用横向滚动条
        leftPanel.add(envListScroll, BorderLayout.CENTER);
        add(leftPanel, BorderLayout.WEST);

        // 右侧 导入 导出 变量表格及操作
        JPanel rightPanel = new JPanel(new BorderLayout());
        JPanel importExportPanel = getImportExportPanel();
        rightPanel.add(importExportPanel, BorderLayout.NORTH);

        // 变量表格
        variablesTablePanel = new EasyNameValueTablePanel();
        JScrollPane tableScrollPane = new JScrollPane(variablesTablePanel);
        tableScrollPane.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 5));
        rightPanel.add(tableScrollPane, BorderLayout.CENTER);

        // 变量操作按钮
        JPanel varButtonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        varButtonPanel.setBorder(BorderFactory.createEmptyBorder(0, 10, 5, 5));
        JButton saveVarButton = new JButton("Save");
        saveVarButton.setIcon(IconFontSwing.buildIcon(FontAwesome.FLOPPY_O, 14, new Color(0, 0, 150)));
        saveVarButton.addActionListener(e -> saveVariables());
        varButtonPanel.add(saveVarButton);
        rightPanel.add(varButtonPanel, BorderLayout.SOUTH);

        add(rightPanel, BorderLayout.CENTER);
    }

    @NotNull
    private JPanel getImportExportPanel() {
        JPanel importExportPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        importExportPanel.setBorder(BorderFactory.createEmptyBorder(0, 5, 5, 5));
        JButton importBtn = new JButton(new FlatSVGIcon("icons/upload.svg", 20, 20));
        importBtn.setText("Import");
        importBtn.setToolTipText("导入环境变量");
        importBtn.setFocusPainted(false);
        importBtn.setBackground(Color.WHITE);
        importBtn.setIconTextGap(6);
        JPopupMenu importMenu = new JPopupMenu();
        JMenuItem importEasyToolsItem = new JMenuItem("Import from EasyPostman", new FlatSVGIcon("icons/easy.svg", 20, 20));
        importEasyToolsItem.addActionListener(e -> importEnvironments());
        JMenuItem importPostmanItem = new JMenuItem("Import from Postman", new FlatSVGIcon("icons/postman.svg", 20, 20));
        importPostmanItem.addActionListener(e -> importPostmanEnvironments());
        importMenu.add(importEasyToolsItem);
        importMenu.add(importPostmanItem);
        importBtn.addActionListener(e -> importMenu.show(importBtn, 0, importBtn.getHeight()));
        importExportPanel.add(importBtn);

        JButton exportBtn = new JButton(new FlatSVGIcon("icons/download.svg", 20, 20));
        exportBtn.setText("Export");
        exportBtn.setFocusPainted(false);
        exportBtn.setBackground(Color.WHITE);
        exportBtn.setIconTextGap(6);
        exportBtn.addActionListener(e -> exportEnvironments());
        importExportPanel.add(exportBtn);
        return importExportPanel;
    }

    @Override
    protected void registerListeners() {
        addEnvButton.addActionListener(e -> addEnvironment());
        // 联动菜单栏右上角下拉框
        EnvironmentComboBox topComboBox = SingletonFactory.getInstance(TopMenuBarPanel.class).getEnvironmentComboBox();
        if (topComboBox != null) {
            topComboBox.setOnEnvironmentChange(env -> {
                environmentListModel.clear();
                java.util.List<Environment> envs = EnvironmentService.getAllEnvironments();
                for (Environment envItem : envs) {
                    environmentListModel.addElement(new EnvironmentItem(envItem));
                }
                if (!environmentListModel.isEmpty()) {
                    environmentList.setSelectedIndex(topComboBox.getSelectedIndex()); // 设置选中当前激活环境
                }
                loadActiveEnvironmentVariables();
            });
        }
        searchField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) {
                reloadEnvironmentList(searchField.getText());
            }

            public void removeUpdate(javax.swing.event.DocumentEvent e) {
                reloadEnvironmentList(searchField.getText());
            }

            public void changedUpdate(javax.swing.event.DocumentEvent e) {
                reloadEnvironmentList(searchField.getText());
            }
        });
        environmentList.addListSelectionListener(e -> { // 监听环境列表左键
            if (!e.getValueIsAdjusting()) {
                EnvironmentItem item = environmentList.getSelectedValue();
                if (item == null || item.getEnvironment() == currentEnvironment) {
                    return; // 没有切换环境，不处理
                }
                if (isVariablesChanged()) {
                    int option = JOptionPane.showConfirmDialog(this, "存在变量修改，是否保存？", "提示", JOptionPane.YES_NO_OPTION);
                    if (option == JOptionPane.YES_OPTION) {
                        saveVariables();
                    } else {
                        loadVariables(currentEnvironment);
                    }
                }
                currentEnvironment = item.getEnvironment();
                loadVariables(currentEnvironment);
                variablesTablePanel.updateTableBorder(false);
            }
        });
        // 环境列表右键菜单
        addRightMenuList();

        addSaveKeyStroke();

        // 默认加载当前激活环境变量
        loadActiveEnvironmentVariables();

        // 环境列表加载与搜索
        reloadEnvironmentList("");

    }

    private void addSaveKeyStroke() {
        // 右键菜单由EasyTablePanel自带，无需再注册
        // 增加 Command+S 保存快捷键（兼容 Mac 和 Windows Ctrl+S）
        KeyStroke saveKeyStroke = KeyStroke.getKeyStroke("meta S"); // Mac Command+S
        KeyStroke saveKeyStroke2 = KeyStroke.getKeyStroke("control S"); // Windows/Linux Ctrl+S
        this.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(saveKeyStroke, "saveVariables");
        this.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(saveKeyStroke2, "saveVariables");
        this.getActionMap().put("saveVariables", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                saveVariables();
            }
        });
    }

    private void addRightMenuList() {
        JPopupMenu envListMenu = new JPopupMenu();
        JMenuItem renameItem = new JMenuItem("Rename");
        JMenuItem copyItem = new JMenuItem("Copy"); // 复制菜单项
        JMenuItem deleteItem = new JMenuItem("Delete");
        JMenuItem exportPostmanItem = new JMenuItem("Export as Postman");
        exportPostmanItem.addActionListener(e -> exportSelectedEnvironmentAsPostman());
        renameItem.addActionListener(e -> renameSelectedEnvironment());
        copyItem.addActionListener(e -> copySelectedEnvironment()); // 复制事件
        deleteItem.addActionListener(e -> deleteSelectedEnvironment());
        envListMenu.add(renameItem);
        envListMenu.add(copyItem);
        envListMenu.add(deleteItem);
        envListMenu.addSeparator();
        envListMenu.add(exportPostmanItem);
        environmentList.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                if (e.isPopupTrigger() || e.getButton() == MouseEvent.BUTTON3) { // 右键菜单
                    int idx = environmentList.locationToIndex(e.getPoint());
                    if (idx >= 0) {
                        environmentList.setSelectedIndex(idx);
                        envListMenu.show(environmentList, e.getX(), e.getY());
                    }
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
                            EnvironmentComboBox comboBox = SingletonFactory.getInstance(TopMenuBarPanel.class).getEnvironmentComboBox();
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
    }

    /**
     * 只加载当前激活环境变量
     */
    public void loadActiveEnvironmentVariables() {
        Environment env = EnvironmentService.getActiveEnvironment();
        if (env != null) {
            currentEnvironment = env;
            loadVariables(env);
        }
    }

    private void loadVariables(Environment env) {
        variablesTablePanel.stopCellEditing();
        currentEnvironment = env;
        variablesTablePanel.clear();
        if (env != null) {
            java.util.List<Map<String, Object>> rows = new ArrayList<>();
            for (String key : env.getVariables().keySet()) {
                String value = env.getVariable(key);
                java.util.Map<String, Object> row = new LinkedHashMap<>();
                row.put("Name", key);
                row.put("Value", value);
                rows.add(row);
            }
            variablesTablePanel.setRows(rows);
            originalVariablesSnapshot = JSONUtil.toJsonStr(rows); // 用rows做快照，保证同步
        } else {
            variablesTablePanel.clear();
            originalVariablesSnapshot = JSONUtil.toJsonStr(new ArrayList<>()); // 空快照
        }
    }

    /**
     * 保存表格中的变量到当前环境
     */
    public void saveVariables() {
        if (currentEnvironment == null) return;
        variablesTablePanel.stopCellEditing();
        currentEnvironment.getVariables().clear();
        java.util.List<Map<String, Object>> rows = variablesTablePanel.getRows();
        for (Map<String, Object> row : rows) {
            String key = row.get("Name") == null ? null : row.get("Name").toString();
            String value = row.get("Value") == null ? "" : row.get("Value").toString();
            if (key != null && !key.trim().isEmpty()) {
                currentEnvironment.addVariable(key.trim(), value);
            }
        }
        EnvironmentService.saveEnvironment(currentEnvironment);
        variablesTablePanel.updateTableBorder(false);
        // 保存后更新快照为json字符串
        originalVariablesSnapshot = JSONUtil.toJsonStr(rows);
        JOptionPane.showMessageDialog(this, "保存成功！", "提示", JOptionPane.INFORMATION_MESSAGE);
    }

    /**
     * 导出所有环境变量为JSON文件
     */
    private void exportEnvironments() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("导出环境变量");
        fileChooser.setSelectedFile(new java.io.File("EasyPostman-Environments.json"));
        int userSelection = fileChooser.showSaveDialog(this);
        if (userSelection == JFileChooser.APPROVE_OPTION) {
            java.io.File fileToSave = fileChooser.getSelectedFile();
            try (java.io.Writer writer = new java.io.OutputStreamWriter(new java.io.FileOutputStream(fileToSave), java.nio.charset.StandardCharsets.UTF_8)) {
                java.util.List<Environment> envs = EnvironmentService.getAllEnvironments();
                writer.write(JSONUtil.toJsonPrettyStr(envs));
                JOptionPane.showMessageDialog(this, "导出成功！", "提示", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                log.error("导出失败", ex);
                JOptionPane.showMessageDialog(this, "导出失败: " + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /**
     * 导入环境变量JSON文件
     */
    private void importEnvironments() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Import EasyPostman Environments");
        int userSelection = fileChooser.showOpenDialog(this);
        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File fileToOpen = fileChooser.getSelectedFile();
            try {
                java.util.List<Environment> envs = JSONUtil.toList(JSONUtil.readJSONArray(fileToOpen, StandardCharsets.UTF_8), Environment.class);
                // 直接导入，不清空原有环境
                for (Environment env : envs) {
                    EnvironmentService.saveEnvironment(env);
                }
                refreshUI(); // 刷新UI
                JOptionPane.showMessageDialog(this, "导入成功！", "提示", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                log.error("Import Error", ex);
                JOptionPane.showMessageDialog(this, "导入失败: " + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /**
     * 导入Postman环境变量JSON文件
     */
    private void importPostmanEnvironments() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Import Postman Environments");
        int userSelection = fileChooser.showOpenDialog(this);
        if (userSelection == JFileChooser.APPROVE_OPTION) {
            java.io.File fileToOpen = fileChooser.getSelectedFile();
            try {
                String json = FileUtil.readString(fileToOpen, StandardCharsets.UTF_8);
                java.util.List<Environment> envs = PostmanImport.parsePostmanEnvironments(json);
                if (!envs.isEmpty()) {
                    // 导入新环境
                    for (Environment env : envs) {
                        EnvironmentService.saveEnvironment(env);
                    }
                    refreshUI(); // 刷新UI
                    JOptionPane.showMessageDialog(this, "导入成功！", "提示", JOptionPane.INFORMATION_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(this, "未解析到有效环境", "提示", JOptionPane.WARNING_MESSAGE);
                }
            } catch (Exception ex) {
                log.error("Postman环境导入失败", ex);
                JOptionPane.showMessageDialog(this, "导入失败: " + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    // 新增环境
    private void addEnvironment() {
        String name = JOptionPane.showInputDialog(this, "请输入环境名称:", "新增环境", JOptionPane.PLAIN_MESSAGE);
        if (name != null && !name.trim().isEmpty()) {
            Environment env = new Environment(name.trim());
            env.setId("env-" + IdUtil.simpleUUID());
            EnvironmentService.saveEnvironment(env);
            environmentListModel.addElement(new EnvironmentItem(env));
            environmentList.setSelectedValue(new EnvironmentItem(env), true);
            EnvironmentComboBox environmentComboBox = SingletonFactory.getInstance(TopMenuBarPanel.class).getEnvironmentComboBox();
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
        Object result = JOptionPane.showInputDialog(this, "请输入新的环境名称:", "重命名环境", JOptionPane.PLAIN_MESSAGE, null, null, env.getName());
        if (result != null) {
            String newName = result.toString().trim();
            if (!newName.isEmpty() && !newName.equals(env.getName())) {
                env.setName(newName);
                EnvironmentService.saveEnvironment(env);
                environmentListModel.setElementAt(new EnvironmentItem(env), environmentList.getSelectedIndex());
                JOptionPane.showMessageDialog(this, "环境名称已更新", "提示", JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this, "环境名称不能为空或未更改", "提示", JOptionPane.WARNING_MESSAGE);
            }
        }
    }

    private void deleteSelectedEnvironment() {
        EnvironmentItem item = environmentList.getSelectedValue();
        if (item == null) return;
        Environment env = item.getEnvironment();
        int confirm = JOptionPane.showConfirmDialog(this,
                "确定要删除环境 \"" + env.getName() + "\" 吗?\n此操作不可恢复。",
                "删除环境",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);
        if (confirm == JOptionPane.YES_OPTION) {
            environmentListModel.removeElement(new EnvironmentItem(env));
            EnvironmentService.deleteEnvironment(env.getId());
            SingletonFactory.getInstance(TopMenuBarPanel.class).getEnvironmentComboBox().reload(); // 刷新顶部下拉框
        }
    }

    // 复制环境方法
    private void copySelectedEnvironment() {
        EnvironmentItem item = environmentList.getSelectedValue();
        if (item == null) return;
        Environment env = item.getEnvironment();
        try {
            Environment copy = new Environment(env.getName() + " Copy");
            copy.setId("env-" + IdUtil.simpleUUID());
            // 复制变量
            for (String key : env.getVariables().keySet()) {
                copy.addVariable(key, env.getVariable(key));
            }
            EnvironmentService.saveEnvironment(copy);
            EnvironmentItem copyItem = new EnvironmentItem(copy);
            environmentListModel.addElement(copyItem);
            EnvironmentComboBox environmentComboBox = SingletonFactory.getInstance(TopMenuBarPanel.class).getEnvironmentComboBox();
            if (environmentComboBox != null) {
                environmentComboBox.addItem(copyItem);
            }
            environmentList.setSelectedValue(copyItem, true);
        } catch (Exception ex) {
            log.error("复制环境失败", ex);
            JOptionPane.showMessageDialog(this, "复制失败: " + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
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
        String curJson = JSONUtil.toJsonStr(variablesTablePanel.getRows());
        boolean isVariablesChanged = !StrUtil.equals(curJson, originalVariablesSnapshot);
        if (isVariablesChanged) {
            log.info("env name: {}", currentEnvironment.getName());
            log.info("current  variables: {}", curJson);
            log.info("original variables: {}", originalVariablesSnapshot);
        }
        return isVariablesChanged;
    }

    // 新增：导出选中环境为Postman格式
    private void exportSelectedEnvironmentAsPostman() {
        EnvironmentItem item = environmentList.getSelectedValue();
        if (item == null) return;
        Environment env = item.getEnvironment();
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("导出为Postman环境变量");
        fileChooser.setSelectedFile(new File(env.getName() + "-postman-env.json"));
        int userSelection = fileChooser.showSaveDialog(this);
        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File fileToSave = fileChooser.getSelectedFile();
            try {
                // 只导出当前环境为Postman格式
                String postmanEnvJson = PostmanImport.toPostmanEnvironmentJson(env);
                FileUtil.writeUtf8String(postmanEnvJson, fileToSave);
                JOptionPane.showMessageDialog(this, "导出成功！", "提示", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                log.error("导出Postman环境失败", ex);
                JOptionPane.showMessageDialog(this, "导出失败: " + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}