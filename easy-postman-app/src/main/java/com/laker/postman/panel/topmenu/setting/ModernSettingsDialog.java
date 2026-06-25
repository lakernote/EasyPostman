package com.laker.postman.panel.topmenu.setting;

import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.ui.FlatTreeUI;
import com.laker.postman.common.component.SearchTextField;
import com.laker.postman.common.component.ToolWindowSurfaceStyle;
import com.laker.postman.common.constants.ModernColors;
import com.laker.postman.util.FontsUtil;
import com.laker.postman.util.IconUtil;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;

import javax.swing.*;
import javax.swing.border.AbstractBorder;
import javax.swing.border.Border;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;


/**
 * 现代化统一设置对话框
 * 集成所有设置到一个标签页界面中
 */
public class ModernSettingsDialog extends JDialog {

    // UI 常量
    private static final int MIN_WIDTH = 600;
    private static final int MIN_HEIGHT = 300;
    private static final int PREFERRED_WIDTH = 820;
    private static final int PREFERRED_HEIGHT = 640;
    private static final int NAVIGATION_WIDTH = 240;
    private static final int SETTINGS_NAVIGATION_ROW_HEIGHT = 28;
    private static final int SETTINGS_SEARCH_FIELD_HEIGHT = 28;
    private static final int SETTINGS_NAVIGATION_TREE_LEFT_GUTTER = 16;
    private static final int SETTINGS_NAVIGATION_TREE_RIGHT_GUTTER = 8;
    private static final int SETTINGS_NAVIGATION_TEXT_LEFT_INSET = 8;
    private static final int SEARCH_MATCH_HIGHLIGHT_DURATION_MS = 3_000;

    private final SettingsContributionRegistry contributionRegistry;
    private final List<SettingsSearchSupport.SettingsSearchPage> settingsPages = new ArrayList<>();
    private final CardLayout settingsContentLayout = new CardLayout();
    private final JPanel settingsContentPanel = new JPanel(settingsContentLayout);
    private SearchTextField settingsSearchField;
    private JTree settingsNavigationTree;
    private DefaultTreeModel settingsNavigationTreeModel;
    private JComponent noSearchResultsPanel;
    private SettingsSearchSupport.SettingsSearchPage selectedPage;
    private boolean syncingNavigationSelection;
    private JComponent highlightedSearchComponent;
    private Border highlightedSearchOriginalBorder;
    private Timer searchHighlightTimer;


    public ModernSettingsDialog(Window parent) {
        this(parent, SettingsContributionRegistry.defaultRegistry());
    }

    ModernSettingsDialog(Window parent, SettingsContributionRegistry contributionRegistry) {
        super(parent, I18nUtil.getMessage(MessageKeys.SETTINGS_DIALOG_TITLE), ModalityType.APPLICATION_MODAL);
        this.contributionRegistry = Objects.requireNonNull(contributionRegistry, "contributionRegistry");
        initUI();
        pack();
        setLocationRelativeTo(parent);
    }

    private void initUI() {
        configureDialog();
        addSettingTabs();
        setupMainPanel();
        setupIcon();
    }

    private void configureDialog() {
        ToolWindowSurfaceStyle.applyDialogWindowChrome(this);
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        setResizable(false);
        setMinimumSize(new Dimension(MIN_WIDTH, MIN_HEIGHT));
        setPreferredSize(new Dimension(PREFERRED_WIDTH, PREFERRED_HEIGHT));
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowOpened(WindowEvent e) {
                resetSelectedTabScrollPosition();
            }

            @Override
            public void windowClosing(WindowEvent e) {
                handleWindowClosing();
            }
        });

        // Add ESC key handling to close dialog
        JRootPane rootPane = getRootPane();
        rootPane.registerKeyboardAction(
                e -> handleWindowClosing(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW
        );
    }

    private void addSettingTabs() {
        SettingsContributionContext context = new SettingsContributionContext(this);
        settingsPages.clear();
        ToolWindowSurfaceStyle.applyDialogSurface(settingsContentPanel);
        for (SettingsContribution contribution : contributionRegistry.contributions()) {
            String title = contribution.resolveTitle();
            JComponent panel = contribution.createPanel(context);
            SettingsSearchSupport.SettingsSearchPage page = new SettingsSearchSupport.SettingsSearchPage(
                    contribution.id(),
                    title,
                    contribution.category(),
                    panel
            );
            settingsPages.add(page);
            settingsContentPanel.add(panel, page.id());
        }
    }

    private void setupMainPanel() {
        JPanel mainPanel = new JPanel(new BorderLayout());
        ToolWindowSurfaceStyle.applyDialogSurface(mainPanel);
        settingsContentPanel.add(getNoSearchResultsPanel(), MessageKeys.SETTINGS_SEARCH_NO_RESULTS);
        mainPanel.add(createSettingsNavigationPanel(), BorderLayout.WEST);
        mainPanel.add(settingsContentPanel, BorderLayout.CENTER);
        setContentPane(mainPanel);
        rebuildNavigationTree(settingsPages);
        selectPage(settingsPages.isEmpty() ? null : settingsPages.get(0), false);
    }

    private JComponent createSettingsNavigationPanel() {
        JPanel navigationPanel = new JPanel(new BorderLayout());
        ToolWindowSurfaceStyle.applyDialogSurface(navigationPanel);
        navigationPanel.setPreferredSize(new Dimension(NAVIGATION_WIDTH, PREFERRED_HEIGHT));
        ToolWindowSurfaceStyle.applyDialogRightSeparator(navigationPanel);

        JPanel searchBar = new JPanel(new BorderLayout());
        ToolWindowSurfaceStyle.applyDialogSurface(searchBar);
        searchBar.setBorder(BorderFactory.createEmptyBorder(8, 10, 6, 10));

        settingsSearchField = new SearchTextField();
        settingsSearchField.setLeadingIcon(IconUtil.createThemed("icons/search.svg", 16, 16));
        settingsSearchField.setPlaceholderText(I18nUtil.getMessage(MessageKeys.SETTINGS_SEARCH_PLACEHOLDER));
        settingsSearchField.putClientProperty(FlatClientProperties.TEXT_FIELD_TRAILING_COMPONENT, null);
        settingsSearchField.setFont(FontsUtil.getDefaultFont(Font.PLAIN));
        settingsSearchField.setPreferredSize(new Dimension(NAVIGATION_WIDTH - 20, SETTINGS_SEARCH_FIELD_HEIGHT));
        settingsSearchField.setMinimumSize(new Dimension(180, SETTINGS_SEARCH_FIELD_HEIGHT));
        settingsSearchField.setMaximumSize(new Dimension(NAVIGATION_WIDTH - 20, SETTINGS_SEARCH_FIELD_HEIGHT));
        settingsSearchField.installUserActivatedFocus();
        settingsSearchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                applySettingsSearch();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                applySettingsSearch();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                applySettingsSearch();
            }
        });

        searchBar.add(settingsSearchField, BorderLayout.WEST);
        navigationPanel.add(searchBar, BorderLayout.NORTH);

        settingsNavigationTree = createSettingsNavigationTree();
        JScrollPane scrollPane = new JScrollPane(settingsNavigationTree);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        ToolWindowSurfaceStyle.applyDialogScrollPane(scrollPane);
        navigationPanel.add(scrollPane, BorderLayout.CENTER);
        return navigationPanel;
    }

    private void applySettingsSearch() {
        if (settingsSearchField == null) {
            return;
        }
        String query = settingsSearchField.getText();
        boolean hasQuery = query != null && !query.trim().isEmpty();
        List<SettingsSearchSupport.SettingsSearchPage> matchedPages =
                SettingsSearchSupport.filter(settingsPages, query);

        settingsSearchField.setNoResult(hasQuery && matchedPages.isEmpty());
        if (hasQuery && matchedPages.isEmpty()) {
            rebuildNavigationTree(List.of());
            settingsContentLayout.show(settingsContentPanel, MessageKeys.SETTINGS_SEARCH_NO_RESULTS);
            selectedPage = null;
            clearSearchHighlight();
            return;
        }

        rebuildNavigationTree(matchedPages);
        SettingsSearchSupport.SettingsSearchPage pageToSelect =
                selectedPage != null && matchedPages.contains(selectedPage)
                        ? selectedPage
                        : matchedPages.stream().findFirst().orElse(null);
        selectPage(pageToSelect, hasQuery);
    }

    private JTree createSettingsNavigationTree() {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("settings");
        settingsNavigationTreeModel = new DefaultTreeModel(root);
        JTree tree = new SettingsNavigationTree(settingsNavigationTreeModel);
        tree.setOpaque(false);
        tree.setBackground(ModernColors.getDialogChromeBackgroundColor());
        tree.setRootVisible(false);
        tree.setShowsRootHandles(true);
        tree.setBorder(BorderFactory.createEmptyBorder(
                2,
                SETTINGS_NAVIGATION_TREE_LEFT_GUTTER,
                8,
                SETTINGS_NAVIGATION_TREE_RIGHT_GUTTER
        ));
        tree.setFont(FontsUtil.getDefaultFont(Font.PLAIN));
        tree.setRowHeight(SETTINGS_NAVIGATION_ROW_HEIGHT);
        tree.setToggleClickCount(0);
        tree.setUI(new SettingsNavigationTreeUi());
        tree.putClientProperty("JTree.lineStyle", "None");
        tree.putClientProperty(
                "FlatLaf.style",
                "wideCellRenderer: true; paintSelection: false; showCellFocusIndicator: false"
        );
        tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        tree.setCellRenderer(new SettingsNavigationTreeCellRenderer());
        tree.addTreeSelectionListener(e -> {
            if (syncingNavigationSelection) {
                return;
            }
            Object node = tree.getLastSelectedPathComponent();
            if (node instanceof SettingsPageTreeNode pageNode) {
                selectPage(pageNode.page(), isSearchActive());
            }
        });
        return tree;
    }

    private void rebuildNavigationTree(List<SettingsSearchSupport.SettingsSearchPage> pages) {
        if (settingsNavigationTreeModel == null) {
            return;
        }
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("settings");
        Map<String, DefaultMutableTreeNode> categoryNodes = new LinkedHashMap<>();
        for (SettingsSearchSupport.SettingsSearchPage page : pages) {
            String category = page.category();
            DefaultMutableTreeNode categoryNode = categoryNodes.computeIfAbsent(category, key -> {
                DefaultMutableTreeNode node = new SettingsCategoryTreeNode(resolveCategoryTitle(key));
                root.add(node);
                return node;
            });
            categoryNode.add(new SettingsPageTreeNode(page));
        }
        settingsNavigationTreeModel.setRoot(root);
        expandAllNavigationRows();
    }

    private void expandAllNavigationRows() {
        if (settingsNavigationTree == null) {
            return;
        }
        for (int i = 0; i < settingsNavigationTree.getRowCount(); i++) {
            settingsNavigationTree.expandRow(i);
        }
    }

    private void selectPage(SettingsSearchSupport.SettingsSearchPage page, boolean locateSearchMatch) {
        if (page == null) {
            return;
        }
        selectedPage = page;
        settingsContentLayout.show(settingsContentPanel, page.id());
        selectNavigationNode(page);
        if (locateSearchMatch) {
            locateSearchMatch(page);
        } else {
            clearSearchHighlight();
            resetSelectedTabScrollPosition();
        }
    }

    private void selectNavigationNode(SettingsSearchSupport.SettingsSearchPage page) {
        if (settingsNavigationTree == null || page == null) {
            return;
        }
        DefaultMutableTreeNode root = (DefaultMutableTreeNode) settingsNavigationTreeModel.getRoot();
        for (int categoryIndex = 0; categoryIndex < root.getChildCount(); categoryIndex++) {
            DefaultMutableTreeNode categoryNode = (DefaultMutableTreeNode) root.getChildAt(categoryIndex);
            for (int pageIndex = 0; pageIndex < categoryNode.getChildCount(); pageIndex++) {
                Object child = categoryNode.getChildAt(pageIndex);
                if (child instanceof SettingsPageTreeNode pageNode && pageNode.page() == page) {
                    TreePath categoryPath = new TreePath(categoryNode.getPath());
                    TreePath path = new TreePath(pageNode.getPath());
                    syncingNavigationSelection = true;
                    try {
                        settingsNavigationTree.expandPath(categoryPath);
                        settingsNavigationTree.setSelectionPath(path);
                        settingsNavigationTree.scrollPathToVisible(path);
                    } finally {
                        syncingNavigationSelection = false;
                    }
                    return;
                }
            }
        }
    }

    private void locateSearchMatch(SettingsSearchSupport.SettingsSearchPage page) {
        String query = settingsSearchField == null ? "" : settingsSearchField.getText();
        // Mirrors IntelliJ settings search: open the page, then run a page-local search action.
        SettingsSearchSupport.firstMatch(page, query)
                .map(SettingsSearchSupport.SettingsSearchMatch::component)
                .ifPresentOrElse(this::scrollToAndHighlightMatch, this::resetSelectedTabScrollPosition);
    }

    private void scrollToAndHighlightMatch(Component component) {
        Component target = findHighlightTarget(component);
        if (target == null) {
            resetSelectedTabScrollPosition();
            return;
        }
        SwingUtilities.invokeLater(() -> {
            if (target instanceof JComponent jComponent) {
                jComponent.scrollRectToVisible(new Rectangle(
                        0,
                        0,
                        Math.max(1, jComponent.getWidth()),
                        Math.max(1, jComponent.getHeight())
                ));
                highlightSearchMatch(jComponent);
            }
        });
    }

    private Component findHighlightTarget(Component component) {
        Component current = component;
        while (current != null && current != selectedPage.component()) {
            String simpleName = current.getClass().getSimpleName();
            if ("SettingsFieldRow".equals(simpleName) || "SettingsCheckBoxRow".equals(simpleName)) {
                return current;
            }
            current = current.getParent();
        }
        return component;
    }

    private void highlightSearchMatch(JComponent component) {
        clearSearchHighlight();
        highlightedSearchComponent = component;
        highlightedSearchOriginalBorder = component.getBorder();
        component.setBorder(new SearchMatchHighlightBorder(highlightedSearchOriginalBorder));
        component.revalidate();
        component.repaint();

        searchHighlightTimer = new Timer(SEARCH_MATCH_HIGHLIGHT_DURATION_MS, e -> clearSearchHighlight());
        searchHighlightTimer.setRepeats(false);
        searchHighlightTimer.start();
    }

    private void clearSearchHighlight() {
        if (searchHighlightTimer != null) {
            searchHighlightTimer.stop();
            searchHighlightTimer = null;
        }
        if (highlightedSearchComponent != null) {
            highlightedSearchComponent.setBorder(highlightedSearchOriginalBorder);
            highlightedSearchComponent.revalidate();
            highlightedSearchComponent.repaint();
        }
        highlightedSearchComponent = null;
        highlightedSearchOriginalBorder = null;
    }

    private boolean isSearchActive() {
        return settingsSearchField != null && !settingsSearchField.getText().trim().isEmpty();
    }

    private String resolveCategoryTitle(String category) {
        if (SettingsContributionRegistry.CATEGORY_APPLICATION.equals(category)) {
            return I18nUtil.getMessage(MessageKeys.SETTINGS_CATEGORY_APPLICATION);
        }
        if (SettingsContributionRegistry.CATEGORY_NETWORK.equals(category)) {
            return I18nUtil.getMessage(MessageKeys.SETTINGS_CATEGORY_NETWORK);
        }
        if (SettingsContributionRegistry.CATEGORY_RUNTIME.equals(category)) {
            return I18nUtil.getMessage(MessageKeys.SETTINGS_CATEGORY_RUNTIME);
        }
        if (SettingsContributionRegistry.CATEGORY_EXTENSIONS.equals(category)) {
            return I18nUtil.getMessage(MessageKeys.SETTINGS_CATEGORY_EXTENSIONS);
        }
        return category == null || category.isBlank() ? I18nUtil.getMessage(MessageKeys.SETTINGS_CATEGORY_OTHER) : category;
    }

    private JComponent getNoSearchResultsPanel() {
        if (noSearchResultsPanel == null) {
            noSearchResultsPanel = createNoSearchResultsPanel();
        }
        return noSearchResultsPanel;
    }

    private JComponent createNoSearchResultsPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        ToolWindowSurfaceStyle.applyDialogSurface(panel);

        JLabel label = new JLabel(I18nUtil.getMessage(MessageKeys.SETTINGS_SEARCH_NO_RESULTS));
        label.setForeground(ModernColors.getTextSecondary());
        label.setFont(FontsUtil.getDefaultFontWithOffset(Font.PLAIN, -1));
        panel.add(label);
        return panel;
    }

    private void setupIcon() {
        try {
            setIconImage(Toolkit.getDefaultToolkit()
                    .getImage(getClass().getResource("/icons/icon.png")));
        } catch (Exception e) {
            // Icon loading failed, continue without icon
        }
    }

    /**
     * 处理窗口关闭事件
     */
    private void handleWindowClosing() {
        if (checkCurrentPanelUnsavedChanges() && checkAllPanelsUnsavedChanges()) {
            dispose();
        }
    }

    /**
     * 检查当前选中面板的未保存更改
     */
    private boolean checkCurrentPanelUnsavedChanges() {
        Component selectedComponent = selectedPage == null ? null : selectedPage.component();
        if (selectedComponent instanceof ModernSettingsPanel panel) {
            return !panel.hasUnsavedChanges() || panel.confirmDiscardChanges();
        }
        return true;
    }

    /**
     * 检查所有面板的未保存更改
     */
    private boolean checkAllPanelsUnsavedChanges() {
        for (SettingsSearchSupport.SettingsSearchPage page : settingsPages) {
            Component component = page.component();
            if (component instanceof ModernSettingsPanel panel && panel.hasUnsavedChanges()) {
                revealAndSelectPage(page);
                if (!panel.confirmDiscardChanges()) {
                    return false;
                }
            }
        }
        return true;
    }

    private void revealAndSelectPage(SettingsSearchSupport.SettingsSearchPage page) {
        if (page == null) {
            return;
        }
        if (settingsSearchField != null && !settingsSearchField.getText().isBlank()
                && !SettingsSearchSupport.filter(settingsPages, settingsSearchField.getText()).contains(page)) {
            settingsSearchField.setText("");
        }
        selectPage(page, false);
    }

    /**
     * 显示设置对话框
     *
     * @param parent 父窗口
     */
    public static void showSettings(Window parent) {
        showSettings(parent, 0);
    }

    /**
     * 显示设置对话框并打开指定的标签页
     *
     * @param parent   父窗口
     * @param tabIndex 要打开的标签页索引
     */
    public static void showSettings(Window parent, int tabIndex) {
        ModernSettingsDialog dialog = new ModernSettingsDialog(parent);
        dialog.selectTab(tabIndex);
        dialog.setVisible(true);
    }

    /**
     * 选择指定的标签页
     *
     * @param tabIndex 标签页索引
     */
    private void selectTab(int tabIndex) {
        if (tabIndex >= 0 && tabIndex < settingsPages.size()) {
            if (settingsSearchField != null && !settingsSearchField.getText().isBlank()) {
                settingsSearchField.setText("");
            }
            selectPage(settingsPages.get(tabIndex), false);
        }
    }

    private void resetSelectedTabScrollPosition() {
        Component component = selectedPage == null ? null : selectedPage.component();
        if (component instanceof ModernSettingsPanel panel) {
            panel.resetScrollPositionToTop();
        }
    }

    private static final class SettingsCategoryTreeNode extends DefaultMutableTreeNode {
        private SettingsCategoryTreeNode(String title) {
            super(title);
        }

        @Override
        public boolean isLeaf() {
            return false;
        }
    }

    private static final class SettingsPageTreeNode extends DefaultMutableTreeNode {
        private final SettingsSearchSupport.SettingsSearchPage page;

        private SettingsPageTreeNode(SettingsSearchSupport.SettingsSearchPage page) {
            super(page.title());
            this.page = page;
        }

        private SettingsSearchSupport.SettingsSearchPage page() {
            return page;
        }
    }

    private static final class SettingsNavigationTree extends JTree {
        private static final int ROW_BACKGROUND_HORIZONTAL_INSET = 12;
        private static final int ROW_BACKGROUND_VERTICAL_INSET = 1;
        private static final int ROW_BACKGROUND_ARC = 7;

        private int hoverRow = -1;

        private SettingsNavigationTree(DefaultTreeModel model) {
            super(model);
            addMouseMotionListener(new MouseMotionAdapter() {
                @Override
                public void mouseMoved(MouseEvent e) {
                    updateHoverRow(getRowForLocation(e.getX(), e.getY()));
                }
            });
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseExited(MouseEvent e) {
                    updateHoverRow(-1);
                }
            });
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            return getParent() instanceof JViewport || super.getScrollableTracksViewportWidth();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setColor(ModernColors.getDialogChromeBackgroundColor());
                g2.fillRect(0, 0, getWidth(), getHeight());
                if (hoverRow >= 0 && !isRowSelected(hoverRow)) {
                    paintRowBackground(g2, hoverRow, navigationHoverBackgroundColor());
                }
                int[] selectedRows = getSelectionRows();
                if (selectedRows != null) {
                    for (int selectedRow : selectedRows) {
                        paintRowBackground(g2, selectedRow, navigationSelectionBackgroundColor());
                    }
                }
            } finally {
                g2.dispose();
            }
            super.paintComponent(g);
        }

        @Override
        protected void processMouseEvent(MouseEvent e) {
            TreePath categoryPath = categoryPathAt(e);
            if (categoryPath != null && SwingUtilities.isLeftMouseButton(e)) {
                if (e.getID() == MouseEvent.MOUSE_PRESSED) {
                    toggleCategoryPath(categoryPath);
                }
                if (e.getID() == MouseEvent.MOUSE_PRESSED || e.getID() == MouseEvent.MOUSE_CLICKED) {
                    e.consume();
                    return;
                }
            }
            super.processMouseEvent(e);
        }

        private TreePath categoryPathAt(MouseEvent e) {
            int row = getClosestRowForLocation(e.getX(), e.getY());
            if (row < 0) {
                return null;
            }
            Rectangle rowBounds = getRowBounds(row);
            if (rowBounds == null || e.getY() < rowBounds.y || e.getY() >= rowBounds.y + rowBounds.height) {
                return null;
            }
            TreePath path = getPathForRow(row);
            return path != null && path.getLastPathComponent() instanceof SettingsCategoryTreeNode ? path : null;
        }

        private void toggleCategoryPath(TreePath path) {
            if (isExpanded(path)) {
                collapsePath(path);
            } else {
                expandPath(path);
            }
        }

        private void updateHoverRow(int row) {
            if (hoverRow == row) {
                return;
            }
            int previousRow = hoverRow;
            hoverRow = row;
            repaintRow(previousRow);
            repaintRow(hoverRow);
        }

        private void repaintRow(int row) {
            if (row < 0) {
                return;
            }
            Rectangle rowBounds = getRowBounds(row);
            if (rowBounds != null) {
                repaint(0, rowBounds.y, getWidth(), rowBounds.height);
            }
        }

        private void paintRowBackground(Graphics2D g2, int row, Color color) {
            Rectangle rowBounds = getRowBounds(row);
            if (rowBounds == null) {
                return;
            }
            g2.setColor(color);
            g2.fillRoundRect(
                    ROW_BACKGROUND_HORIZONTAL_INSET,
                    rowBounds.y + ROW_BACKGROUND_VERTICAL_INSET,
                    Math.max(0, getWidth() - ROW_BACKGROUND_HORIZONTAL_INSET * 2),
                    Math.max(0, rowBounds.height - ROW_BACKGROUND_VERTICAL_INSET * 2),
                    ROW_BACKGROUND_ARC,
                    ROW_BACKGROUND_ARC
            );
        }

        static Color navigationSelectionBackgroundColor() {
            return ModernColors.getSelectionBackgroundColor();
        }

        static Color navigationHoverBackgroundColor() {
            return ModernColors.getHoverBackgroundColor();
        }
    }

    private static final class SettingsNavigationTreeUi extends FlatTreeUI {
        @Override
        protected void installDefaults() {
            super.installDefaults();
            setLeftChildIndent(2);
            setRightChildIndent(6);
        }
    }

    private static final class SettingsNavigationTreeCellRenderer extends JLabel implements TreeCellRenderer {
        private SettingsNavigationTreeCellRenderer() {
            setOpaque(false);
            setIcon(null);
            setBorder(BorderFactory.createEmptyBorder(0, SETTINGS_NAVIGATION_TEXT_LEFT_INSET, 0, 8));
        }

        @Override
        public Component getTreeCellRendererComponent(JTree tree,
                                                      Object value,
                                                      boolean selected,
                                                      boolean expanded,
                                                      boolean leaf,
                                                      int row,
                                                      boolean hasFocus) {
            setText(String.valueOf(value));
            setFont(value instanceof SettingsCategoryTreeNode
                    ? FontsUtil.getDefaultFontWithOffset(Font.BOLD, -1)
                    : FontsUtil.getDefaultFont(Font.PLAIN));
            if (selected) {
                setForeground(ModernColors.getTextPrimary());
            } else {
                setForeground(value instanceof SettingsCategoryTreeNode
                        ? ModernColors.getTextSecondary()
                        : ModernColors.getTextPrimary());
            }
            return this;
        }
    }

    private static final class SearchMatchHighlightBorder extends AbstractBorder {
        private static final int ARC = 7;
        private static final float STROKE_WIDTH = 1f;
        private final Border delegate;

        private SearchMatchHighlightBorder(Border delegate) {
            this.delegate = delegate;
        }

        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(searchMatchFillColor());
                g2.fillRoundRect(x, y, Math.max(0, width - 1), Math.max(0, height - 1), ARC, ARC);
            } finally {
                g2.dispose();
            }
            if (delegate != null) {
                delegate.paintBorder(c, g, x, y, width, height);
            }
            g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setStroke(new BasicStroke(STROKE_WIDTH));
                g2.setColor(searchMatchBorderColor());
                g2.drawRoundRect(x, y, Math.max(0, width - 1), Math.max(0, height - 1), ARC, ARC);
            } finally {
                g2.dispose();
            }
        }

        @Override
        public Insets getBorderInsets(Component c, Insets insets) {
            if (delegate != null) {
                Insets delegateInsets = delegate.getBorderInsets(c);
                insets.set(delegateInsets.top, delegateInsets.left, delegateInsets.bottom, delegateInsets.right);
                return insets;
            }
            insets.set(0, 0, 0, 0);
            return insets;
        }

        private static Color searchMatchBorderColor() {
            return ModernColors.withAlpha(
                    ModernColors.isDarkTheme() ? ModernColors.getWarning() : ModernColors.getWarningDark(),
                    ModernColors.isDarkTheme() ? 150 : 135
            );
        }

        private static Color searchMatchFillColor() {
            return ModernColors.withAlpha(
                    ModernColors.getWarning(),
                    ModernColors.isDarkTheme() ? 28 : 24
            );
        }
    }
}
