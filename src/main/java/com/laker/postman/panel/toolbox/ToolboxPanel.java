package com.laker.postman.panel.toolbox;

import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.laker.postman.common.SingletonBasePanel;
import com.laker.postman.common.component.SearchTextField;
import com.laker.postman.common.constants.ModernColors;
import com.laker.postman.panel.toolbox.kafka.KafkaPanel;
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
import java.util.List;

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

    private record ToolEntry(String id, String nameKey, String iconPath,
                             String groupKey, JPanel panel) {
    }

    private final List<ToolEntry> allTools = new ArrayList<>();
    private final List<ToolEntry> filtered = new ArrayList<>();

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
        for (ToolEntry t : allTools) {
            contentArea.add(t.panel(), t.id());
        }

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
        reg("json", MessageKeys.TOOLBOX_JSON, "icons/format.svg", GRP_FORMAT, new JsonToolPanel());
        reg("sql", MessageKeys.TOOLBOX_SQL, "icons/database.svg", GRP_FORMAT, new SqlToolPanel());
        reg("markdown", "toolbox.markdown", "icons/edit.svg", GRP_FORMAT, new MarkdownToolPanel());
        // 转换
        reg("encoder", MessageKeys.TOOLBOX_ENCODER, "icons/code.svg", GRP_CONVERT, new EncoderPanel());
        reg("timestamp", MessageKeys.TOOLBOX_TIMESTAMP, "icons/time.svg", GRP_CONVERT, new TimestampPanel());
        reg("crypto", MessageKeys.TOOLBOX_CRYPTO, "icons/security.svg", GRP_CONVERT, new CryptoPanel());
        // 生成
        reg("uuid", MessageKeys.TOOLBOX_UUID, "icons/plus.svg", GRP_GEN, new UuidPanel());
        reg("hash", MessageKeys.TOOLBOX_HASH, "icons/hash.svg", GRP_GEN, new HashPanel());
        reg("cron", MessageKeys.TOOLBOX_CRON, "icons/time.svg", GRP_GEN, new CronPanel());
        // 数据库
        reg("es", MessageKeys.TOOLBOX_ELASTICSEARCH, "icons/elasticsearch.svg", GRP_DB, new ElasticsearchPanel());
        reg("redis", MessageKeys.TOOLBOX_REDIS, "icons/redis.svg", GRP_DB, new RedisPanel());
        reg("influxdb", MessageKeys.TOOLBOX_INFLUXDB, "icons/influxdb.svg", GRP_DB, new InfluxDbPanel());
        reg("kafka", MessageKeys.TOOLBOX_KAFKA, "icons/kafka.svg", GRP_DB, new KafkaPanel());
        // 开发
        reg("diff", MessageKeys.TOOLBOX_DIFF, "icons/file.svg", GRP_DEV, new DiffPanel());
        reg("decompiler", MessageKeys.TOOLBOX_DECOMPILER, "icons/decompile.svg", GRP_DEV, new DecompilerPanel());
    }

    private void reg(String id, String nameKey, String iconPath, String group, JPanel panel) {
        allTools.add(new ToolEntry(id, nameKey, iconPath, group, panel));
    }

    // ===== 搜索过滤 =====
    private void applyFilter() {
        String kw = searchField.getText().trim().toLowerCase();
        filtered.clear();
        for (ToolEntry t : allTools) {
            String name = I18nUtil.getMessage(t.nameKey()).toLowerCase();
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
        String curGroup = null;
        for (ToolEntry t : filtered) {
            if (!t.groupKey().equals(curGroup)) {
                curGroup = t.groupKey();
                if (navPanel.getComponentCount() > 1) {
                    navPanel.add(Box.createVerticalStrut(2));
                    JSeparator sep = new JSeparator();
                    sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
                    navPanel.add(sep);
                    navPanel.add(Box.createVerticalStrut(2));
                }
                JLabel grpLbl = new JLabel(I18nUtil.getMessage(curGroup).toUpperCase());
                grpLbl.setFont(grpLbl.getFont().deriveFont(Font.BOLD, 9.5f));
                grpLbl.setForeground(UIManager.getColor("Label.disabledForeground"));
                grpLbl.setBorder(new EmptyBorder(4, 10, 2, 8));
                grpLbl.setAlignmentX(Component.LEFT_ALIGNMENT);
                navPanel.add(grpLbl);
            }
            navPanel.add(buildNavItem(t));
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
                ? IconUtil.createPrimary(t.iconPath(), IconUtil.SIZE_SMALL, IconUtil.SIZE_SMALL)
                : IconUtil.createThemed(t.iconPath(), IconUtil.SIZE_SMALL, IconUtil.SIZE_SMALL);

        JLabel lblIcon = new JLabel(icon);
        JLabel lblName = new JLabel(I18nUtil.getMessage(t.nameKey()));
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
        selectedId = id;
        cardLayout.show(contentArea, id);
        rebuildNav();
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
