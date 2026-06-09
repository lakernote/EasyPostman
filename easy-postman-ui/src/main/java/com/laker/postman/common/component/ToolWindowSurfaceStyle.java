package com.laker.postman.common.component;

import com.laker.postman.common.constants.ModernColors;

import javax.swing.AbstractButton;
import javax.swing.JComponent;
import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTree;
import javax.swing.JPopupMenu;
import javax.swing.UIManager;
import javax.swing.JViewport;
import javax.swing.SwingUtilities;
import javax.swing.text.JTextComponent;
import javax.swing.plaf.basic.BasicSplitPaneDivider;
import javax.swing.plaf.basic.BasicSplitPaneUI;
import javax.swing.table.JTableHeader;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.beans.PropertyChangeListener;
import java.lang.ref.WeakReference;
import java.util.function.Consumer;

/**
 * Shared surface helpers for IDEA-like tool-window content.
 */
public final class ToolWindowSurfaceStyle {
    private static final String THEME_REFRESH_LISTENER = "EasyPostman.toolWindowSurfaceStyle.themeRefreshListener";

    private ToolWindowSurfaceStyle() {
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

    public static void applyBackground(JComponent component) {
        setBackground(component, true);
        installThemeRefresh(component, ToolWindowSurfaceStyle::setOpaqueBackground);
    }

    public static void applyScrollPaneCard(JScrollPane scrollPane) {
        applyCard(scrollPane);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setViewportBorder(BorderFactory.createEmptyBorder());
        applyViewportCard(scrollPane.getViewport());
        applyScrollBarCard(scrollPane.getVerticalScrollBar());
        applyScrollBarCard(scrollPane.getHorizontalScrollBar());
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
        applyScrollPaneCard(scrollPane);
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

    public static void applyPanelTreeCard(Component component) {
        if (component == null || isControl(component)) {
            return;
        }

        if (component instanceof JScrollPane scrollPane) {
            applyScrollPaneCard(scrollPane);
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

    private static void setTableCard(JTable table) {
        Color surface = ModernColors.getCardBackgroundColor();
        table.setOpaque(true);
        table.setBackground(surface);
        table.setGridColor(ModernColors.getBorderLightColor());
        JTableHeader header = table.getTableHeader();
        if (header != null) {
            header.setOpaque(true);
            header.setBackground(ModernColors.getInputBackgroundColor());
            header.setForeground(ModernColors.getTextPrimary());
        }
    }

    private static void setListCard(JList<?> list) {
        list.setOpaque(true);
        list.setBackground(ModernColors.getCardBackgroundColor());
        list.setForeground(ModernColors.getTextPrimary());
        list.setSelectionBackground(ModernColors.getSelectionBackgroundColor());
        list.setSelectionForeground(ModernColors.getTextPrimary());
    }

    private static void setTreeCard(JTree tree) {
        tree.setOpaque(true);
        tree.setBackground(ModernColors.getCardBackgroundColor());
        tree.setForeground(ModernColors.getTextPrimary());
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
            component.removePropertyChangeListener("UI", registration.componentListener());
            UIManager.removePropertyChangeListener(registration.lookAndFeelListener());
        }
        PropertyChangeListener componentListener = event -> {
            if ("UI".equals(event.getPropertyName())) {
                scheduleRefresh(component, updater);
            }
        };
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
        component.putClientProperty(THEME_REFRESH_LISTENER,
                new ThemeRefreshRegistration(componentListener, lookAndFeelListener));
        component.addPropertyChangeListener("UI", componentListener);
        UIManager.addPropertyChangeListener(lookAndFeelListener);
    }

    private static void scheduleRefresh(JComponent component, Consumer<JComponent> updater) {
        if (SwingUtilities.isEventDispatchThread()) {
            updater.accept(component);
            return;
        }
        SwingUtilities.invokeLater(() -> updater.accept(component));
    }

    private record ThemeRefreshRegistration(PropertyChangeListener componentListener,
                                            PropertyChangeListener lookAndFeelListener) {
    }
}
