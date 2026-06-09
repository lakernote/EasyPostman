package com.laker.postman.panel.sidebar;
import lombok.RequiredArgsConstructor;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

/**
 * 创建和刷新侧边栏自定义 tab 组件。
 */
@RequiredArgsConstructor
final class SidebarTabComponentFactory {
    private static final String ICON_LABEL_NAME = "iconLabel";
    private static final String TITLE_LABEL_NAME = "titleLabel";

    private final JTabbedPane tabbedPane;
    private final BooleanSupplier sidebarExpanded;
    private final Function<SidebarTab, Integer> tabIndexResolver;
    private final IntSupplier expandedWidthSupplier;
    private final IntSupplier collapsedWidthSupplier;
    private final Supplier<Font> normalFontSupplier;
    private final Supplier<Font> boldFontSupplier;

    Component create(SidebarTab sidebarTab, String title, Icon icon) {
        int collapsedHeight = SidebarTabMetrics.collapsedHeight(icon != null ? icon.getIconHeight() : 0);
        JPanel panel = new JPanel() {
            @Override
            public Dimension getPreferredSize() {
                Dimension size = super.getPreferredSize();
                if (sidebarExpanded.getAsBoolean()) {
                    size.width = expandedWidthSupplier.getAsInt();
                } else {
                    size.width = collapsedWidthSupplier.getAsInt();
                    size.height = collapsedHeight;
                }
                return size;
            }
        };
        panel.setOpaque(false);

        boolean expanded = sidebarExpanded.getAsBoolean();
        if (expanded) {
            addExpandedContent(panel, sidebarTab, title, icon);
        } else {
            addCollapsedContent(panel, sidebarTab, title, icon);
        }

        panel.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                int tabIndex = tabIndexResolver.apply(sidebarTab);
                if (tabIndex >= 0) {
                    tabbedPane.setSelectedIndex(tabIndex);
                }
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                panel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                panel.setCursor(Cursor.getDefaultCursor());
            }
        });

        return panel;
    }

    void updateSelection(Component tabComponent, SidebarTab sidebarTab, boolean selected) {
        if (!(tabComponent instanceof JPanel panel)) {
            return;
        }
        updateTabIcon(panel, sidebarTab, selected);
        updateTabTitle(panel, selected);
    }

    private void addExpandedContent(JPanel panel, SidebarTab sidebarTab, String title, Icon icon) {
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        JLabel iconLabel = createIconLabel(initialIcon(sidebarTab, icon));

        JLabel titleLabel = createTitleLabel(title);
        applyTitleSelection(titleLabel, isCurrentlySelected(sidebarTab));

        panel.add(Box.createVerticalStrut(SidebarTabMetrics.EXPANDED_TAB_SPACING_TOP));
        panel.add(iconLabel);
        panel.add(Box.createVerticalStrut(SidebarTabMetrics.EXPANDED_TAB_SPACING_MIDDLE));
        panel.add(titleLabel);
        panel.add(Box.createVerticalStrut(SidebarTabMetrics.EXPANDED_TAB_SPACING_BOTTOM));
        panel.setBorder(BorderFactory.createEmptyBorder(
                SidebarTabMetrics.EXPANDED_TAB_PADDING_VERTICAL,
                SidebarTabMetrics.EXPANDED_TAB_PADDING_HORIZONTAL,
                SidebarTabMetrics.EXPANDED_TAB_PADDING_VERTICAL,
                SidebarTabMetrics.EXPANDED_TAB_PADDING_HORIZONTAL
        ));
    }

    private void addCollapsedContent(JPanel panel, SidebarTab sidebarTab, String title, Icon icon) {
        panel.setLayout(new BorderLayout());
        panel.setToolTipText(title);
        panel.add(createIconLabel(initialIcon(sidebarTab, icon)), BorderLayout.CENTER);
        panel.setBorder(BorderFactory.createEmptyBorder(
                SidebarTabMetrics.COLLAPSED_TAB_PADDING_VERTICAL,
                SidebarTabMetrics.collapsedIconPaddingLeft(),
                SidebarTabMetrics.COLLAPSED_TAB_PADDING_VERTICAL,
                SidebarTabMetrics.collapsedIconPaddingRight()
        ));
    }

    private JLabel createIconLabel(Icon icon) {
        JLabel iconLabel = new JLabel(icon);
        iconLabel.setHorizontalAlignment(SwingConstants.CENTER);
        iconLabel.setVerticalAlignment(SwingConstants.CENTER);
        iconLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        iconLabel.setName(ICON_LABEL_NAME);
        return iconLabel;
    }

    private JLabel createTitleLabel(String title) {
        JLabel titleLabel = new JLabel(title);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        titleLabel.setName(TITLE_LABEL_NAME);
        return titleLabel;
    }

    private Icon initialIcon(SidebarTab sidebarTab, Icon fallbackIcon) {
        boolean selected = isCurrentlySelected(sidebarTab);
        return selected ? selectedIcon(sidebarTab) : fallbackIcon;
    }

    private boolean isCurrentlySelected(SidebarTab sidebarTab) {
        int currentIndex = tabIndexResolver.apply(sidebarTab);
        return currentIndex >= 0 && tabbedPane.getSelectedIndex() == currentIndex;
    }

    private void updateTabIcon(JPanel panel, SidebarTab sidebarTab, boolean selected) {
        JLabel iconLabel = findNamedLabel(panel, ICON_LABEL_NAME);
        if (iconLabel != null) {
            iconLabel.setIcon(selected ? selectedIcon(sidebarTab) : sidebarTab.getIcon());
        }
    }

    private Icon selectedIcon(SidebarTab sidebarTab) {
        return sidebarExpanded.getAsBoolean()
                ? sidebarTab.getSelectedIcon()
                : sidebarTab.getSelectedOnPrimaryIcon();
    }

    private void updateTabTitle(JPanel panel, boolean selected) {
        JLabel titleLabel = findNamedLabel(panel, TITLE_LABEL_NAME);
        if (titleLabel != null) {
            applyTitleSelection(titleLabel, selected);
        }
    }

    private JLabel findNamedLabel(JPanel panel, String name) {
        for (Component comp : panel.getComponents()) {
            if (comp instanceof JLabel label && name.equals(label.getName())) {
                return label;
            }
        }
        return null;
    }

    private void applyTitleSelection(JLabel label, boolean selected) {
        if (selected) {
            label.setForeground(SidebarTheme.selectedTabTitleForeground());
            label.setFont(boldFontSupplier.get());
        } else {
            label.setForeground(SidebarTheme.inactiveTabTitleForeground());
            label.setFont(normalFontSupplier.get());
        }
    }
}
