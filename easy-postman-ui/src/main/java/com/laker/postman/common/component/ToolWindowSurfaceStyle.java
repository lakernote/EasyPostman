package com.laker.postman.common.component;

import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.util.SystemInfo;
import com.laker.postman.common.constants.ModernColors;

import javax.swing.AbstractButton;
import javax.swing.JComponent;
import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTree;
import javax.swing.JPopupMenu;
import javax.swing.RootPaneContainer;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.JViewport;
import javax.swing.SwingUtilities;
import javax.swing.text.JTextComponent;
import javax.swing.plaf.basic.BasicSplitPaneDivider;
import javax.swing.plaf.basic.BasicSplitPaneUI;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import java.awt.AWTEvent;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeListener;
import java.lang.ref.WeakReference;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * Shared surface helpers for IDEA-like tool-window content.
 */
public final class ToolWindowSurfaceStyle {
    private static final String THEME_REFRESH_LISTENER = "EasyPostman.toolWindowSurfaceStyle.themeRefreshListener";
    private static final String SCROLL_PANE_CARD_VARIANT =
            "EasyPostman.toolWindowSurfaceStyle.scrollPaneCardVariant";
    private static final String DIALOG_WINDOW_CHROME_APPLIED =
            "EasyPostman.toolWindowSurfaceStyle.dialogWindowChromeApplied";
    private static boolean globalDialogWindowChromeInstalled;

    private ToolWindowSurfaceStyle() {
    }

    public static synchronized void installGlobalDialogWindowChrome() {
        if (globalDialogWindowChromeInstalled) {
            return;
        }
        globalDialogWindowChromeInstalled = true;

        Toolkit.getDefaultToolkit().addAWTEventListener(event -> {
            if (event instanceof HierarchyEvent hierarchyEvent) {
                long flags = hierarchyEvent.getChangeFlags();
                if ((flags & (HierarchyEvent.DISPLAYABILITY_CHANGED
                        | HierarchyEvent.PARENT_CHANGED
                        | HierarchyEvent.SHOWING_CHANGED)) != 0) {
                    applyDialogWindowChromeIfNeeded(hierarchyEvent.getComponent());
                }
            } else if (event instanceof WindowEvent windowEvent
                    && windowEvent.getID() == WindowEvent.WINDOW_OPENED) {
                applyDialogWindowChromeIfNeeded(windowEvent.getWindow());
            }
        }, AWTEvent.HIERARCHY_EVENT_MASK | AWTEvent.WINDOW_EVENT_MASK);

        for (Window window : Window.getWindows()) {
            applyDialogWindowChromeIfNeeded(window);
        }
    }

    public static void applyCard(JComponent component) {
        setCard(component, true);
        installThemeRefresh(component, ToolWindowSurfaceStyle::setOpaqueCard);
    }

    public static void applySectionHeader(JComponent component) {
        setCard(component, true);
        component.setBorder(BorderFactory.createEmptyBorder());
        installThemeRefresh(component, ToolWindowSurfaceStyle::setOpaqueCard);
    }

    public static void applySectionHeader(JComponent component, int top, int left, int bottom, int right) {
        setCard(component, true);
        component.setBorder(BorderFactory.createEmptyBorder(top, left, bottom, right));
        installThemeRefresh(component, ToolWindowSurfaceStyle::setOpaqueCard);
    }

    public static void applyToolWindowToolbarSeparator(JComponent component, int top, int left, int bottom, int right) {
        setToolWindowToolbarSeparator(component, top, left, bottom, right);
        installThemeRefresh(component, target -> setToolWindowToolbarSeparator(target, top, left, bottom, right));
    }

    public static void applySectionCard(JComponent component) {
        setSectionCard(component);
        installThemeRefresh(component, ToolWindowSurfaceStyle::setSectionCard);
    }

    public static void applyDialogFooter(JComponent component) {
        setDialogFooter(component);
        installThemeRefresh(component, ToolWindowSurfaceStyle::setDialogFooter);
    }

    public static void applyDialogSurface(JComponent component) {
        setDialogSurface(component);
        installThemeRefresh(component, ToolWindowSurfaceStyle::setDialogSurface);
    }

    public static void applyDialogSection(JComponent component) {
        setDialogSection(component);
        installThemeRefresh(component, ToolWindowSurfaceStyle::setDialogSection);
    }

    public static void applyDialogHeader(JComponent component, int top, int left, int bottom, int right) {
        setDialogHeader(component, top, left, bottom, right);
        installThemeRefresh(component, target -> setDialogHeader(target, top, left, bottom, right));
    }

    public static void applyDialogBottomSeparator(JComponent component) {
        setDialogBottomSeparator(component);
        installThemeRefresh(component, ToolWindowSurfaceStyle::setDialogBottomSeparator);
    }

    public static void applyDialogRightSeparator(JComponent component) {
        setDialogRightSeparator(component);
        installThemeRefresh(component, ToolWindowSurfaceStyle::setDialogRightSeparator);
    }

    public static void applyDialogTopSeparator(JComponent component, int top, int left, int bottom, int right) {
        setDialogTopSeparator(component, top, left, bottom, right);
        installThemeRefresh(component, target -> setDialogTopSeparator(target, top, left, bottom, right));
    }

    public static void applyDialogFrame(JComponent component) {
        setDialogFrame(component);
        installThemeRefresh(component, ToolWindowSurfaceStyle::setDialogFrame);
    }

    public static void applyDialogInputBorder(JComponent component, boolean focused) {
        setDialogInputBorder(component, focused);
    }

    public static void applyDialogWindowChrome(Window window) {
        if (window == null) {
            return;
        }

        Color chrome = ModernColors.getDialogChromeBackgroundColor();
        window.setBackground(chrome);
        if (window instanceof RootPaneContainer rootPaneContainer) {
            rootPaneContainer.getRootPane().putClientProperty(DIALOG_WINDOW_CHROME_APPLIED, Boolean.TRUE);
            applyDialogWindowChrome(rootPaneContainer.getRootPane());
            applyDialogWindowChrome((JComponent) rootPaneContainer.getLayeredPane());
            applyDialogWindowChrome(rootPaneContainer.getContentPane());
            if (rootPaneContainer.getGlassPane() instanceof JComponent glassPane) {
                glassPane.setOpaque(false);
                glassPane.setBackground(chrome);
            }
        }
        if (window.isDisplayable()) {
            window.validate();
            window.repaint();
        }
    }

    public static void skipDialogWindowChrome(Window window) {
        if (window instanceof RootPaneContainer rootPaneContainer) {
            rootPaneContainer.getRootPane().putClientProperty(DIALOG_WINDOW_CHROME_APPLIED, Boolean.TRUE);
        }
    }

    private static void applyDialogWindowChromeIfNeeded(Component component) {
        if (component == null) {
            return;
        }
        Window window = component instanceof Window sourceWindow
                ? sourceWindow
                : SwingUtilities.getWindowAncestor(component);
        if (window instanceof JDialog dialog) {
            if (Boolean.TRUE.equals(dialog.getRootPane().getClientProperty(DIALOG_WINDOW_CHROME_APPLIED))) {
                return;
            }
            applyDialogWindowChrome(dialog);
        }
    }

    public static void applyDialogWindowChrome(JRootPane rootPane) {
        if (rootPane == null) {
            return;
        }
        setDialogChromeBackground(rootPane, true);
        rootPane.putClientProperty(
                FlatClientProperties.TITLE_BAR_BACKGROUND,
                ModernColors.getDialogChromeBackgroundColor()
        );
        rootPane.putClientProperty(FlatClientProperties.TITLE_BAR_FOREGROUND, UIManager.getColor("Menu.foreground"));
        applyMacDialogWindowChrome(rootPane);
        installThemeRefresh(rootPane, component -> applyDialogWindowChrome((JRootPane) component));
        rootPane.revalidate();
        rootPane.repaint();
    }

    private static void applyDialogWindowChrome(Container container) {
        if (container instanceof JComponent component) {
            applyDialogWindowChrome(component);
        }
    }

    private static void applyDialogWindowChrome(JComponent component) {
        if (component == null) {
            return;
        }
        setDialogChromeBackground(component, true);
        installThemeRefresh(component, ToolWindowSurfaceStyle::setOpaqueDialogChromeBackground);
    }

    private static void applyMacDialogWindowChrome(JRootPane rootPane) {
        if (!SystemInfo.isMacFullWindowContentSupported) {
            return;
        }
        rootPane.putClientProperty("apple.awt.fullWindowContent", true);
        rootPane.putClientProperty("apple.awt.transparentTitleBar", true);
        rootPane.putClientProperty(
                "apple.awt.windowAppearance",
                ModernColors.isDarkTheme() ? "NSAppearanceNameVibrantDark" : "NSAppearanceNameVibrantLight"
        );
        rootPane.setBorder(BorderFactory.createEmptyBorder(getMacDialogTitleBarInset(), 0, 0, 0));
    }

    private static int getMacDialogTitleBarInset() {
        int titlePaneHeight = UIManager.getInt("TitlePane.height");
        return titlePaneHeight > 0 ? titlePaneHeight : 28;
    }

    public static void applyDialogScrollPane(JScrollPane scrollPane) {
        applyDialogSurface(scrollPane);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setViewportBorder(BorderFactory.createEmptyBorder());
        applyViewportWindowChrome(scrollPane.getViewport());
        applyScrollBarWindowChrome(scrollPane.getVerticalScrollBar());
        applyScrollBarWindowChrome(scrollPane.getHorizontalScrollBar());
    }

    public static void applyDialogListScrollPane(JScrollPane scrollPane, JList<?> list) {
        applyDialogScrollPane(scrollPane);
        applyDialogList(list);
    }

    public static void applyDialogSplitPane(JSplitPane splitPane) {
        setDialogSplitPane(splitPane);
        installThemeRefresh(splitPane, component -> setDialogSplitPane((JSplitPane) component));
    }

    public static void applyBackground(JComponent component) {
        setBackground(component, true);
        installThemeRefresh(component, ToolWindowSurfaceStyle::setOpaqueBackground);
    }

    public static void applyScrollPaneCard(JScrollPane scrollPane) {
        scrollPane.putClientProperty(SCROLL_PANE_CARD_VARIANT, ScrollPaneCardVariant.PLAIN);
        applyCard(scrollPane);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setViewportBorder(BorderFactory.createEmptyBorder());
        applyViewportCard(scrollPane.getViewport());
        applyScrollBarCard(scrollPane.getVerticalScrollBar());
        applyScrollBarCard(scrollPane.getHorizontalScrollBar());
    }

    public static void applyFramedScrollPaneCard(JScrollPane scrollPane) {
        scrollPane.putClientProperty(SCROLL_PANE_CARD_VARIANT, ScrollPaneCardVariant.FRAMED);
        setFramedScrollPaneCard(scrollPane);
        installThemeRefresh(scrollPane, component -> setFramedScrollPaneCard((JScrollPane) component));
    }

    public static void applyTabbedPaneCard(JTabbedPane tabbedPane) {
        setTabbedPaneCard(tabbedPane);
        tabbedPane.setForeground(ModernColors.getTextPrimary());
        installThemeRefresh(tabbedPane, component -> {
            setTabbedPaneCard((JTabbedPane) component);
            ((JTabbedPane) component).setForeground(ModernColors.getTextPrimary());
        });
    }

    public static void applyTableCard(JTable table) {
        setTableCard(table);
        installThemeRefresh(table, component -> setTableCard((JTable) component));
    }

    public static void applyTableScrollPaneCard(JScrollPane scrollPane, JTable table) {
        if (scrollPane.getColumnHeader() == null && table.getTableHeader() != null) {
            scrollPane.setColumnHeaderView(table.getTableHeader());
        }
        setTableScrollPaneCard(scrollPane);
        installThemeRefresh(scrollPane, component -> setTableScrollPaneCard((JScrollPane) component));
        applyTableCard(table);
    }

    public static void applyListScrollPaneCard(JScrollPane scrollPane, JList<?> list) {
        applyScrollPaneCard(scrollPane);
        applyListCard(list);
    }

    public static void applyTreeScrollPaneCard(JScrollPane scrollPane, JTree tree) {
        applyScrollPaneCard(scrollPane);
        applyTreeCard(tree);
    }

    public static void applyPopupMenuCard(JPopupMenu popupMenu) {
        setCard(popupMenu, true);
        popupMenu.setBorder(BorderFactory.createLineBorder(ModernColors.getBorderLightColor()));
        installThemeRefresh(popupMenu, component -> {
            setCard(component, true);
            ((JPopupMenu) component).setBorder(BorderFactory.createLineBorder(ModernColors.getBorderLightColor()));
        });
    }

    public static void applyListCard(JList<?> list) {
        setListCard(list);
        installThemeRefresh(list, component -> setListCard((JList<?>) component));
    }

    public static void applyDialogList(JList<?> list) {
        setDialogList(list);
        installThemeRefresh(list, component -> setDialogList((JList<?>) component));
    }

    public static void applyTreeCard(JTree tree) {
        setTreeCard(tree);
        installThemeRefresh(tree, component -> setTreeCard((JTree) component));
    }

    public static void applyTextComponentCard(JTextComponent textComponent) {
        setTextComponentCard(textComponent);
        installThemeRefresh(textComponent, component -> setTextComponentCard((JTextComponent) component));
    }

    public static void applyTextComponentInput(JTextComponent textComponent) {
        setTextComponentInput(textComponent);
        installThemeRefresh(textComponent, component -> setTextComponentInput((JTextComponent) component));
    }

    public static void applyTextComponentDialogSurface(JTextComponent textComponent) {
        setTextComponentDialogSurface(textComponent);
        installThemeRefresh(textComponent, component -> setTextComponentDialogSurface((JTextComponent) component));
    }

    public static void applyPanelTreeCard(Component component) {
        if (component == null || isControl(component)) {
            return;
        }

        if (component instanceof JScrollPane scrollPane) {
            if (scrollPane.getClientProperty(SCROLL_PANE_CARD_VARIANT) == ScrollPaneCardVariant.FRAMED) {
                applyFramedScrollPaneCard(scrollPane);
            } else {
                applyScrollPaneCard(scrollPane);
            }
        } else if (component instanceof JPopupMenu popupMenu) {
            applyPopupMenuCard(popupMenu);
        } else if (component instanceof JTable table) {
            applyTableCard(table);
        } else if (component instanceof JList<?> list) {
            applyListCard(list);
        } else if (component instanceof JTree tree) {
            applyTreeCard(tree);
        } else if (component instanceof JTabbedPane tabbedPane) {
            applyTabbedPaneCard(tabbedPane);
        } else if (isToolWindowChromeContainer(component)) {
            // Keep outer gutters, split gaps, and rounded masks on the main background color.
        } else if (component instanceof JSplitPane splitPane) {
            applySplitPaneCard(splitPane);
        } else if (component instanceof JPanel panel) {
            applyPanelCardPreservingOpacity(panel);
        } else if (component instanceof JComponent jComponent) {
            setCard(jComponent, jComponent.isOpaque());
            installThemeRefresh(jComponent, target -> setCard(target, target.isOpaque()));
        }

        if (component instanceof Container container) {
            for (Component child : container.getComponents()) {
                applyPanelTreeCard(child);
            }
        }
    }

    private static void applyViewportCard(JViewport viewport) {
        if (viewport == null) {
            return;
        }
        setCard(viewport, true);
        installThemeRefresh(viewport, ToolWindowSurfaceStyle::setOpaqueCard);
    }

    private static void applyScrollBarCard(JScrollBar scrollBar) {
        if (scrollBar == null) {
            return;
        }
        setCard(scrollBar, true);
        installThemeRefresh(scrollBar, ToolWindowSurfaceStyle::setOpaqueCard);
    }

    private static void applyViewportWindowChrome(JViewport viewport) {
        if (viewport == null) {
            return;
        }
        setDialogChromeBackground(viewport, true);
        installThemeRefresh(viewport, ToolWindowSurfaceStyle::setOpaqueDialogChromeBackground);
    }

    private static void applyScrollBarWindowChrome(JScrollBar scrollBar) {
        if (scrollBar == null) {
            return;
        }
        setDialogChromeBackground(scrollBar, true);
        installThemeRefresh(scrollBar, ToolWindowSurfaceStyle::setOpaqueDialogChromeBackground);
    }

    private static void applyPanelCardPreservingOpacity(JPanel panel) {
        boolean opaque = panel.isOpaque();
        setCard(panel, opaque);
        installThemeRefresh(panel, component -> setCard(component, opaque));
    }

    private static void applySplitPaneCard(JSplitPane splitPane) {
        setCard(splitPane, true);
        splitPane.setBorder(BorderFactory.createEmptyBorder());
        BasicSplitPaneUI ui = splitPane.getUI() instanceof BasicSplitPaneUI basicUi ? basicUi : null;
        if (ui != null) {
            BasicSplitPaneDivider divider = ui.getDivider();
            if (divider != null) {
                divider.setBorder(BorderFactory.createEmptyBorder());
                divider.setBackground(ModernColors.getCardBackgroundColor());
            }
        }
        installThemeRefresh(splitPane, component -> applySplitPaneCard((JSplitPane) component));
    }

    private static void setOpaqueCard(JComponent component) {
        setCard(component, true);
    }

    private static void setOpaqueBackground(JComponent component) {
        setBackground(component, true);
    }

    private static void setOpaqueDialogChromeBackground(JComponent component) {
        setDialogChromeBackground(component, true);
    }

    private static void setDialogFooter(JComponent component) {
        setDialogChromeBackground(component, true);
        component.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, ModernColors.getTabSeparatorColor()),
                BorderFactory.createEmptyBorder(10, 16, 10, 16)
        ));
    }

    private static void setToolWindowToolbarSeparator(JComponent component, int top, int left, int bottom, int right) {
        setCard(component, true);
        component.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, ModernColors.getTabSeparatorColor()),
                BorderFactory.createEmptyBorder(top, left, bottom, right)
        ));
    }

    private static void setDialogSurface(JComponent component) {
        setDialogChromeBackground(component, true);
    }

    private static void setDialogSection(JComponent component) {
        setDialogChromeBackground(component, true);
        component.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ModernColors.getTabSeparatorColor()),
                BorderFactory.createEmptyBorder(8, 10, 10, 10)
        ));
    }

    private static void setDialogHeader(JComponent component, int top, int left, int bottom, int right) {
        setDialogChromeBackground(component, true);
        component.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, ModernColors.getTabSeparatorColor()),
                BorderFactory.createEmptyBorder(top, left, bottom, right)
        ));
    }

    private static void setDialogBottomSeparator(JComponent component) {
        setDialogChromeBackground(component, true);
        component.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, ModernColors.getTabSeparatorColor()));
    }

    private static void setDialogRightSeparator(JComponent component) {
        setDialogChromeBackground(component, true);
        component.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, ModernColors.getTabSeparatorColor()));
    }

    private static void setDialogTopSeparator(JComponent component, int top, int left, int bottom, int right) {
        setDialogChromeBackground(component, true);
        component.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, ModernColors.getTabSeparatorColor()),
                BorderFactory.createEmptyBorder(top, left, bottom, right)
        ));
    }

    private static void setDialogFrame(JComponent component) {
        setDialogChromeBackground(component, true);
        component.setBorder(BorderFactory.createLineBorder(ModernColors.getTabSeparatorColor()));
    }

    private static void setDialogInputBorder(JComponent component, boolean focused) {
        Color borderColor = focused ? ModernColors.getPrimary() : ModernColors.getBorderMediumColor();
        component.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(borderColor),
                BorderFactory.createEmptyBorder(1, 1, 1, 1)
        ));
    }

    private static void setSectionCard(JComponent component) {
        setCard(component, true);
        component.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ModernColors.getBorderLightColor()),
                BorderFactory.createEmptyBorder(8, 10, 10, 10)
        ));
    }

    private static void setDialogList(JList<?> list) {
        setDialogChromeBackground(list, true);
        list.setForeground(ModernColors.getTextPrimary());
        list.setSelectionBackground(ModernColors.getTabSelectedBackgroundColor());
        list.setSelectionForeground(ModernColors.getTextPrimary());
    }

    private static void setDialogSplitPane(JSplitPane splitPane) {
        setDialogChromeBackground(splitPane, true);
        splitPane.setBorder(BorderFactory.createEmptyBorder());
        BasicSplitPaneUI ui = splitPane.getUI() instanceof BasicSplitPaneUI basicUi ? basicUi : null;
        if (ui != null) {
            BasicSplitPaneDivider divider = ui.getDivider();
            if (divider != null) {
                divider.setBorder(BorderFactory.createEmptyBorder());
                divider.setBackground(ModernColors.getDialogChromeBackgroundColor());
            }
        }
    }

    private static void setTabbedPaneCard(JTabbedPane tabbedPane) {
        tabbedPane.setOpaque(true);
        tabbedPane.setBackground(ModernColors.getTabBackgroundColor());
    }

    private static void setCard(JComponent component, boolean opaque) {
        component.setOpaque(opaque);
        component.setBackground(ModernColors.getCardBackgroundColor());
    }

    private static void setBackground(JComponent component, boolean opaque) {
        component.setOpaque(opaque);
        component.setBackground(ModernColors.getBackgroundColor());
    }

    private static void setDialogChromeBackground(JComponent component, boolean opaque) {
        component.setOpaque(opaque);
        component.setBackground(ModernColors.getDialogChromeBackgroundColor());
    }

    private static void setTableCard(JTable table) {
        Color surface = ModernColors.getTableBackgroundColor();
        Color grid = ModernColors.getTableGridColor();
        table.setOpaque(true);
        table.setBackground(surface);
        table.setForeground(ModernColors.getTextPrimary());
        table.setSelectionBackground(ModernColors.getTableSelectionBackgroundColor());
        table.setSelectionForeground(ModernColors.getTableSelectionForegroundColor());
        table.setGridColor(grid);
        table.setShowHorizontalLines(true);
        table.setShowVerticalLines(true);
        table.setIntercellSpacing(new Dimension(1, 1));
        table.setRowMargin(1);
        if (table.getRowHeight() < 28) {
            table.setRowHeight(28);
        }
        JTableHeader header = table.getTableHeader();
        if (header != null) {
            header.setOpaque(true);
            header.setBackground(ModernColors.getTableHeaderBackgroundColor());
            header.setForeground(ModernColors.getTextPrimary());
            header.setReorderingAllowed(false);
            int headerHeight = Math.max(32, header.getPreferredSize().height);
            header.setPreferredSize(new Dimension(header.getPreferredSize().width, headerHeight));
            installTableHeaderRenderer(header);
        }
    }

    private static void setTableScrollPaneCard(JScrollPane scrollPane) {
        setCard(scrollPane, true);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setViewportBorder(BorderFactory.createEmptyBorder());
        applyViewportCard(scrollPane.getViewport());
        applyScrollBarCard(scrollPane.getVerticalScrollBar());
        applyScrollBarCard(scrollPane.getHorizontalScrollBar());

        JViewport columnHeader = scrollPane.getColumnHeader();
        if (columnHeader != null) {
            columnHeader.setOpaque(true);
            columnHeader.setBackground(ModernColors.getTableHeaderBackgroundColor());
        }
        setTableCorner(scrollPane, JScrollPane.UPPER_RIGHT_CORNER);
        setTableCorner(scrollPane, JScrollPane.UPPER_LEFT_CORNER);
    }

    private static void setFramedScrollPaneCard(JScrollPane scrollPane) {
        setCard(scrollPane, true);
        scrollPane.setBorder(BorderFactory.createLineBorder(ModernColors.getBorderLightColor()));
        scrollPane.setViewportBorder(BorderFactory.createEmptyBorder());
        applyViewportCard(scrollPane.getViewport());
        applyScrollBarCard(scrollPane.getVerticalScrollBar());
        applyScrollBarCard(scrollPane.getHorizontalScrollBar());
    }

    private static void setTableCorner(JScrollPane scrollPane, String cornerKey) {
        Component corner = scrollPane.getCorner(cornerKey);
        if (corner instanceof JComponent component) {
            component.setOpaque(true);
            component.setBackground(ModernColors.getTableHeaderBackgroundColor());
        }
    }

    private static void installTableHeaderRenderer(JTableHeader header) {
        TableCellRenderer renderer = header.getDefaultRenderer();
        if (renderer instanceof IdeaTableHeaderRenderer) {
            return;
        }
        header.setDefaultRenderer(new IdeaTableHeaderRenderer(renderer));
    }

    private static final class IdeaTableHeaderRenderer implements TableCellRenderer {
        private final TableCellRenderer delegate;

        private IdeaTableHeaderRenderer(TableCellRenderer delegate) {
            this.delegate = delegate;
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
                                                       int row, int column) {
            Component component = delegate.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            if (component instanceof JComponent jComponent) {
                jComponent.setOpaque(true);
                jComponent.setBackground(ModernColors.getTableHeaderBackgroundColor());
                jComponent.setForeground(ModernColors.getTextPrimary());
                jComponent.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(0, 0, 1, 1, ModernColors.getTableGridColor()),
                        BorderFactory.createEmptyBorder(0, 10, 0, 10)
                ));
            }
            if (component instanceof JLabel label) {
                label.setFont(table.getTableHeader().getFont());
                boolean blankHeader = value == null || value.toString().isBlank();
                label.setHorizontalAlignment(blankHeader ? SwingConstants.CENTER : SwingConstants.LEADING);
            }
            return component;
        }
    }

    private static void setListCard(JList<?> list) {
        list.setOpaque(true);
        list.setBackground(ModernColors.getCardBackgroundColor());
        list.setForeground(uiColor("List.foreground", ModernColors.getTextPrimary()));
        list.setSelectionBackground(ModernColors.getSelectionBackgroundColor());
        list.setSelectionForeground(uiColor("List.selectionForeground", ModernColors.getTextPrimary()));
    }

    private static void setTreeCard(JTree tree) {
        tree.setOpaque(true);
        tree.setBackground(ModernColors.getCardBackgroundColor());
        tree.setForeground(uiColor("Tree.foreground", ModernColors.getTextPrimary()));
    }

    private static Color uiColor(String key, Color fallback) {
        Color color = UIManager.getColor(key);
        return color != null ? color : fallback;
    }

    private static void setTextComponentCard(JTextComponent textComponent) {
        textComponent.setOpaque(true);
        textComponent.setBackground(ModernColors.getCardBackgroundColor());
        textComponent.setForeground(ModernColors.getTextPrimary());
    }

    private static void setTextComponentInput(JTextComponent textComponent) {
        textComponent.setOpaque(true);
        textComponent.setBackground(ModernColors.getInputBackgroundColor());
        textComponent.setForeground(ModernColors.getTextPrimary());
    }

    private static void setTextComponentDialogSurface(JTextComponent textComponent) {
        setDialogChromeBackground(textComponent, true);
        textComponent.setForeground(ModernColors.getTextPrimary());
    }

    private static boolean isControl(Component component) {
        return component instanceof AbstractButton
                || component instanceof JTextComponent
                || component instanceof JComboBox<?>
                || component instanceof JSpinner;
    }

    private static boolean isToolWindowChromeContainer(Component component) {
        if (!(component instanceof JComponent jComponent)) {
            return false;
        }
        return Boolean.TRUE.equals(jComponent.getClientProperty(ToolWindowChrome.CHROME_BACKGROUND_PROPERTY))
                || Boolean.TRUE.equals(jComponent.getClientProperty(ToolWindowChrome.CHROME_ROUNDED_PROPERTY))
                || Boolean.TRUE.equals(jComponent.getClientProperty(ToolWindowChrome.CHROME_SPLIT_PROPERTY));
    }

    private static void installThemeRefresh(JComponent component, Consumer<JComponent> updater) {
        Object previous = component.getClientProperty(THEME_REFRESH_LISTENER);
        if (previous instanceof ThemeRefreshRegistration registration) {
            registration.uninstall(component);
        }
        PropertyChangeListener componentListener = event -> {
            if ("UI".equals(event.getPropertyName())) {
                scheduleRefresh(component, updater);
            }
        };
        AtomicReference<ThemeRefreshRegistration> registrationRef = new AtomicReference<>();
        PropertyChangeListener lookAndFeelListener = new PropertyChangeListener() {
            private final WeakReference<JComponent> componentRef = new WeakReference<>(component);

            @Override
            public void propertyChange(java.beans.PropertyChangeEvent event) {
                if (!"lookAndFeel".equals(event.getPropertyName())) {
                    return;
                }
                JComponent target = componentRef.get();
                if (target == null) {
                    UIManager.removePropertyChangeListener(this);
                    return;
                }
                scheduleRefresh(target, updater);
            }
        };
        HierarchyListener hierarchyListener = event -> {
            if ((event.getChangeFlags() & HierarchyEvent.DISPLAYABILITY_CHANGED) == 0 || component.isDisplayable()) {
                return;
            }
            ThemeRefreshRegistration registration = registrationRef.get();
            if (registration != null) {
                scheduleThemeRefreshCleanup(component, registration);
            }
        };

        ThemeRefreshRegistration registration =
                new ThemeRefreshRegistration(componentListener, lookAndFeelListener, hierarchyListener);
        registrationRef.set(registration);
        component.putClientProperty(THEME_REFRESH_LISTENER, registration);
        component.addPropertyChangeListener("UI", componentListener);
        component.addHierarchyListener(hierarchyListener);
        UIManager.addPropertyChangeListener(lookAndFeelListener);
    }

    private static void scheduleRefresh(JComponent component, Consumer<JComponent> updater) {
        if (SwingUtilities.isEventDispatchThread()) {
            updater.accept(component);
            return;
        }
        SwingUtilities.invokeLater(() -> updater.accept(component));
    }

    private static void scheduleThemeRefreshCleanup(JComponent component, ThemeRefreshRegistration registration) {
        SwingUtilities.invokeLater(() -> {
            if (component.isDisplayable() || component.getClientProperty(THEME_REFRESH_LISTENER) != registration) {
                return;
            }
            registration.uninstall(component);
            component.putClientProperty(THEME_REFRESH_LISTENER, null);
        });
    }

    private record ThemeRefreshRegistration(PropertyChangeListener componentListener,
                                            PropertyChangeListener lookAndFeelListener,
                                            HierarchyListener hierarchyListener) {
        private void uninstall(JComponent component) {
            component.removePropertyChangeListener("UI", componentListener);
            component.removeHierarchyListener(hierarchyListener);
            UIManager.removePropertyChangeListener(lookAndFeelListener);
        }
    }

    private enum ScrollPaneCardVariant {
        PLAIN,
        FRAMED
    }
}
