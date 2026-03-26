package com.laker.postman.panel.toolbox;

import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.laker.postman.common.SingletonBasePanel;
import com.laker.postman.common.component.SearchTextField;
import com.laker.postman.common.constants.ModernColors;
import com.laker.postman.plugin.api.ToolboxContribution;
import com.laker.postman.plugin.bridge.PluginAccess;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.IconUtil;
import com.laker.postman.util.MessageKeys;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * 工具箱面板 - 左侧导航栏 + 右侧内容区（CardLayout）
 * <p>
 * 交互设计：
 * - 左侧导航支持分组展示、搜索过滤（复用 SearchTextField 组件）
 * - 左右宽度可拖动调整
 * - 选中项高亮使用项目主色 ModernColors.PRIMARY
 * - 图标统一使用 IconUtil 管理
 */
@Slf4j
public class ToolboxPanel extends SingletonBasePanel {

    private static final class ToolEntry {
        private final String id;
        private final String displayName;
        private final String iconPath;
        private final String groupId;
        private final String groupDisplayName;
        private final Supplier<JPanel> panelSupplier;
        private final ClassLoader iconClassLoader;
        private JPanel panel;
        private boolean addedToContentArea;

        private ToolEntry(String id, String displayName, String iconPath,
                          String groupId, String groupDisplayName, Supplier<JPanel> panelSupplier,
                          ClassLoader iconClassLoader) {
            this.id = id;
            this.displayName = displayName;
            this.iconPath = iconPath;
            this.groupId = groupId;
            this.groupDisplayName = groupDisplayName;
            this.panelSupplier = panelSupplier;
            this.iconClassLoader = iconClassLoader;
        }

        private String id() {
            return id;
        }

        private String displayName() {
            return displayName;
        }

        private String iconPath() {
            return iconPath;
        }

        private String groupId() {
            return groupId;
        }

        private String groupDisplayName() {
            return groupDisplayName;
        }

        private ClassLoader iconClassLoader() {
            return iconClassLoader;
        }

        private JPanel getOrCreatePanel() {
            if (panel == null) {
                panel = panelSupplier.get();
            }
            return panel;
        }
    }

    private static JPanel createLoadFailedPanel(String toolDisplayName, Throwable throwable) {
        JPanel panel = new JPanel(new BorderLayout());
        JTextArea errorArea = new JTextArea();
        errorArea.setEditable(false);
        errorArea.setLineWrap(true);
        errorArea.setWrapStyleWord(true);
        errorArea.setText(I18nUtil.getMessage(MessageKeys.ERROR) + ": " + toolDisplayName + "\n\n"
                + throwable.getMessage());
        errorArea.setCaretPosition(0);
        errorArea.setBorder(new EmptyBorder(12, 12, 12, 12));
        panel.add(new JScrollPane(errorArea), BorderLayout.CENTER);
        return panel;
    }

    private final List<ToolEntry> allTools = new ArrayList<>();
    private final List<ToolEntry> filtered = new ArrayList<>();
    private final Map<String, ToolEntry> toolsById = new LinkedHashMap<>();

    private JPanel navPanel;
    private JPanel contentArea;
    private CardLayout cardLayout;
    private SearchTextField searchField;
    private String selectedId = null;

    private static final String GRP_FORMAT = "toolbox.group.format";
    private static final String GRP_CONVERT = "toolbox.group.convert";
    private static final String GRP_GEN = "toolbox.group.generate";
    private static final String GRP_DB = "toolbox.group.database";
    private static final String GRP_DEV = "toolbox.group.dev";
    private static final String GRP_PLUGIN = "plugin";

    @Override
    protected void initUI() {
        setLayout(new BorderLayout());
        registerAllTools();

        // ---- 左侧：搜索框 + 导航列表 ----
        navPanel = new JPanel();
        navPanel.setLayout(new BoxLayout(navPanel, BoxLayout.Y_AXIS));

        JScrollPane navScroll = new JScrollPane(navPanel,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        navScroll.setBorder(BorderFactory.createEmptyBorder());
        navScroll.getVerticalScrollBar().setUnitIncrement(16);

        // 复用项目 SearchTextField（带搜索图标、清除按钮、无结果红框）
        // 工具箱搜索不需要大小写/整词按钮，移除 trailing 组件使其更简洁
        searchField = new SearchTextField();
        searchField.putClientProperty(FlatClientProperties.TEXT_FIELD_TRAILING_COMPONENT, null);
        searchField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        searchField.setPreferredSize(new Dimension(Integer.MAX_VALUE, 28));
        searchField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) {
                applyFilter();
            }

            public void removeUpdate(javax.swing.event.DocumentEvent e) {
                applyFilter();
            }

            public void changedUpdate(javax.swing.event.DocumentEvent e) {
                applyFilter();
            }
        });

        JPanel searchBox = new JPanel(new BorderLayout());
        searchBox.setBorder(new EmptyBorder(6, 6, 4, 6));
        searchBox.add(searchField, BorderLayout.CENTER);

        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.setBorder(BorderFactory.createEmptyBorder());
        leftPanel.setMinimumSize(new Dimension(100, 0));
        leftPanel.add(searchBox, BorderLayout.NORTH);
        leftPanel.add(navScroll, BorderLayout.CENTER);

        // ---- 右侧：CardLayout 内容区 ----
        cardLayout = new CardLayout();
        contentArea = new JPanel(cardLayout);
        contentArea.setMinimumSize(new Dimension(200, 0));

        // 可拖动分割线（dividerSize=5，支持拖拽调宽）
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, contentArea);
        split.setDividerSize(3);
        split.setDividerLocation(160);
        split.setContinuousLayout(true);
        split.setBorder(BorderFactory.createEmptyBorder());
        add(split, BorderLayout.CENTER);

        applyFilter(); // 初始渲染并选中第一个
    }

    // ===== 注册所有工具 =====
    private void registerAllTools() {
        // 格式化
        regBuiltIn("json", MessageKeys.TOOLBOX_JSON, "icons/format.svg", GRP_FORMAT, JsonToolPanel::new);
        regBuiltIn("sql", MessageKeys.TOOLBOX_SQL, "icons/database.svg", GRP_FORMAT, SqlToolPanel::new);
        regBuiltIn("markdown", "toolbox.markdown", "icons/edit.svg", GRP_FORMAT, MarkdownToolPanel::new);
        // 转换
        regBuiltIn("encoder", MessageKeys.TOOLBOX_ENCODER, "icons/code.svg", GRP_CONVERT, EncoderPanel::new);
        regBuiltIn("timestamp", MessageKeys.TOOLBOX_TIMESTAMP, "icons/time.svg", GRP_CONVERT, TimestampPanel::new);
        regBuiltIn("crypto", MessageKeys.TOOLBOX_CRYPTO, "icons/security.svg", GRP_CONVERT, CryptoPanel::new);
        // 生成
        regBuiltIn("uuid", MessageKeys.TOOLBOX_UUID, "icons/plus.svg", GRP_GEN, UuidPanel::new);
        regBuiltIn("hash", MessageKeys.TOOLBOX_HASH, "icons/hash.svg", GRP_GEN, HashPanel::new);
        regBuiltIn("cron", MessageKeys.TOOLBOX_CRON, "icons/time.svg", GRP_GEN, CronPanel::new);
        // 数据库
        regBuiltIn("es", MessageKeys.TOOLBOX_ELASTICSEARCH, "icons/elasticsearch.svg", GRP_DB, ElasticsearchPanel::new);
        regBuiltIn("influxdb", MessageKeys.TOOLBOX_INFLUXDB, "icons/influxdb.svg", GRP_DB, InfluxDbPanel::new);
        // 开发
        regBuiltIn("diff", MessageKeys.TOOLBOX_DIFF, "icons/file.svg", GRP_DEV, DiffPanel::new);
        registerPluginTools();
    }

    private void registerPluginTools() {
        for (ToolboxContribution contribution : PluginAccess.getToolboxContributions()) {
            try {
                regPlugin(contribution.id(), contribution.displayName(), contribution.iconPath(),
                        contribution.groupId(), contribution.groupDisplayName(),
                        contribution.panelSupplier(), contribution.iconClassLoader());
            } catch (Throwable t) {
                log.error("Failed to register toolbox contribution: {}", contribution.id(), t);
            }
        }
    }

    private void regBuiltIn(String id, String nameKey, String iconPath, String groupId, Supplier<JPanel> panelSupplier) {
        addTool(new ToolEntry(id, I18nUtil.getMessage(nameKey), iconPath,
                groupId, resolveGroupDisplayName(groupId, null), panelSupplier, null));
    }

    private void regPlugin(String id, String displayName, String iconPath,
                           String groupId, String groupDisplayName, Supplier<JPanel> panelSupplier,
                           ClassLoader iconClassLoader) {
        String normalizedGroupId = (groupId == null || groupId.isBlank()) ? GRP_PLUGIN : groupId;
        addTool(new ToolEntry(id, displayName, iconPath,
                normalizedGroupId, resolveGroupDisplayName(normalizedGroupId, groupDisplayName),
                panelSupplier, iconClassLoader));
    }

    private void addTool(ToolEntry toolEntry) {
        allTools.add(toolEntry);
        toolsById.put(toolEntry.id(), toolEntry);
    }

    private String resolveGroupDisplayName(String groupId, String fallback) {
        if (groupId == null || groupId.isBlank()) {
            return fallback == null || fallback.isBlank() ? GRP_PLUGIN.toUpperCase() : fallback;
        }
        return switch (groupId) {
            case GRP_FORMAT, GRP_CONVERT, GRP_GEN, GRP_DB, GRP_DEV -> I18nUtil.getMessage(groupId).toUpperCase();
            default -> fallback == null || fallback.isBlank() ? groupId.toUpperCase() : fallback;
        };
    }

    // ===== 搜索过滤 =====
    private void applyFilter() {
        String kw = searchField.getText().trim().toLowerCase();
        filtered.clear();
        for (ToolEntry t : allTools) {
            String name = t.displayName().toLowerCase();
            if (kw.isEmpty() || name.contains(kw)) {
                filtered.add(t);
            }
        }
        // 无匹配时红框提示（SearchTextField 已有此能力）
        searchField.setNoResult(!kw.isEmpty() && filtered.isEmpty());
        rebuildNav();
        boolean selVisible = filtered.stream().anyMatch(t -> t.id().equals(selectedId));
        if (!selVisible) {
            if (!filtered.isEmpty()) selectTool(filtered.get(0).id());
            else selectedId = null;
        }
    }

    // ===== 重建左侧导航 =====
    private void rebuildNav() {
        navPanel.removeAll();
        navPanel.add(Box.createVerticalStrut(4));
        Map<String, List<ToolEntry>> groupedTools = new LinkedHashMap<>();
        for (ToolEntry tool : filtered) {
            groupedTools.computeIfAbsent(tool.groupId(), ignored -> new ArrayList<>()).add(tool);
        }

        boolean firstGroup = true;
        for (List<ToolEntry> groupTools : groupedTools.values()) {
            if (groupTools.isEmpty()) {
                continue;
            }
            if (!firstGroup) {
                navPanel.add(Box.createVerticalStrut(2));
                JSeparator sep = new JSeparator();
                sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
                navPanel.add(sep);
                navPanel.add(Box.createVerticalStrut(2));
            }
            firstGroup = false;

            ToolEntry firstTool = groupTools.get(0);
            JLabel grpLbl = new JLabel(firstTool.groupDisplayName());
            grpLbl.setFont(grpLbl.getFont().deriveFont(Font.BOLD, 9.5f));
            grpLbl.setForeground(UIManager.getColor("Label.disabledForeground"));
            grpLbl.setBorder(new EmptyBorder(4, 10, 2, 8));
            grpLbl.setAlignmentX(Component.LEFT_ALIGNMENT);
            navPanel.add(grpLbl);

            for (ToolEntry tool : groupTools) {
                navPanel.add(buildNavItem(tool));
            }
        }
        navPanel.add(Box.createVerticalGlue());
        navPanel.revalidate();
        navPanel.repaint();
    }

    // ===== 构建单个导航项 =====
    private JPanel buildNavItem(ToolEntry t) {
        boolean sel = t.id().equals(selectedId);

        JPanel item = new JPanel(new BorderLayout(7, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                if (t.id().equals(selectedId)) {
                    // 主色调半透明圆角背景（亮暗自适应透明度）
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    int alpha = ModernColors.isDarkTheme() ? 40 : 25;
                    g2.setColor(new Color(ModernColors.PRIMARY.getRed(),
                            ModernColors.PRIMARY.getGreen(), ModernColors.PRIMARY.getBlue(), alpha));
                    g2.fillRoundRect(4, 1, getWidth() - 8, getHeight() - 2, 8, 8);
                    g2.dispose();
                }
                super.paintComponent(g);
            }
        };
        item.setOpaque(false);
        item.setBorder(new EmptyBorder(5, 10, 5, 8));
        item.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        item.setAlignmentX(Component.LEFT_ALIGNMENT);
        item.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        // 选中：PRIMARY 色图标；未选中：主题色图标（IconUtil 统一管理）
        FlatSVGIcon icon = sel
                ? IconUtil.createPrimary(t.iconPath(), IconUtil.SIZE_SMALL, IconUtil.SIZE_SMALL, t.iconClassLoader())
                : IconUtil.createThemed(t.iconPath(), IconUtil.SIZE_SMALL, IconUtil.SIZE_SMALL, t.iconClassLoader());

        JLabel lblIcon = new JLabel(icon);
        JLabel lblName = new JLabel(t.displayName());
        lblName.setFont(lblName.getFont().deriveFont(sel ? Font.BOLD : Font.PLAIN, 12f));
        if (sel) lblName.setForeground(ModernColors.PRIMARY);

        item.add(lblIcon, BorderLayout.WEST);
        item.add(lblName, BorderLayout.CENTER);

        item.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (!t.id().equals(selectedId)) {
                    Color hover = UIManager.getColor("List.hoverBackground");
                    if (hover == null) hover = UIManager.getColor("Button.hoverBackground");
                    item.setBackground(hover);
                    item.setOpaque(true);
                    item.repaint();
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                item.setOpaque(false);
                item.repaint();
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                selectTool(t.id());
            }
        });
        return item;
    }

    // ===== 选中工具 =====
    private void selectTool(String id) {
        ensureToolContentLoaded(id);
        selectedId = id;
        cardLayout.show(contentArea, id);
        rebuildNav();
    }

    private void ensureToolContentLoaded(String id) {
        ToolEntry toolEntry = toolsById.get(id);
        if (toolEntry == null || toolEntry.addedToContentArea) {
            return;
        }
        try {
            contentArea.add(toolEntry.getOrCreatePanel(), toolEntry.id());
        } catch (Throwable t) {
            log.error("Failed to initialize toolbox panel: {}", id, t);
            contentArea.add(createLoadFailedPanel(toolEntry.displayName(), t), toolEntry.id());
        }
        toolEntry.addedToContentArea = true;
        contentArea.revalidate();
        contentArea.repaint();
    }

    @Override
    protected void registerListeners() {
        // 主题切换时刷新导航颜色（图标色、高亮色跟随主题）
        UIManager.addPropertyChangeListener(e -> {
            if ("lookAndFeel".equals(e.getPropertyName())) {
                SwingUtilities.invokeLater(this::rebuildNav);
            }
        });
    }
}
