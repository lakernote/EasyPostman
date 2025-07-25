package com.laker.postman.common.tab;


import com.laker.postman.panel.collections.right.request.RequestEditSubPanel;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * 通用可关闭Tab组件，支持右上角红点脏标记
 */
public class ClosableTabComponent extends JPanel {
    private final JLabel label;
    private final String rawTitle;
    private boolean dirty = false;
    private final JTabbedPane tabbedPane;
    private final RequestEditSubPanel panel;
    private final Runnable saveCallback;

    public ClosableTabComponent(String title, RequestEditSubPanel panel, JTabbedPane tabbedPane, Runnable saveCallback) {
        setOpaque(false);
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(2, 8, 2, 2));
        this.tabbedPane = tabbedPane;
        this.panel = panel;
        this.saveCallback = saveCallback;
        // 动态计算宽度，最大不超过MAX_TAB_WIDTH
        FontMetrics fm = getFontMetrics(getFont());
        int textWidth = fm.stringWidth(title) + 32;
        int MAX_TAB_WIDTH = 80;
        int tabWidth = Math.min(textWidth, MAX_TAB_WIDTH);
        int TAB_HEIGHT = 28;
        setPreferredSize(new Dimension(tabWidth, TAB_HEIGHT));
        // 截断文本并设置tooltip
        String displayTitle = title;
        int maxLabelWidth = tabWidth - 10;
        String ellipsis = "...";
        if (fm.stringWidth(title) > maxLabelWidth) {
            int len = title.length();
            while (len > 0 && fm.stringWidth(title.substring(0, len) + ellipsis) > maxLabelWidth) {
                len--;
            }
            displayTitle = title.substring(0, len) + ellipsis;
        }
        label = new JLabel(displayTitle) {
            @Override
            public boolean contains(int x, int y) {
                return false;
            }
        };
        label.setToolTipText(title);
        label.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        label.setHorizontalAlignment(SwingConstants.CENTER);
        label.setVerticalAlignment(SwingConstants.CENTER);
        add(label, BorderLayout.CENTER);
        // 添加右键菜单
        JPopupMenu menu = getPopupMenu();
        this.setComponentPopupMenu(menu);
        addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mousePressed(java.awt.event.MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e)) {
                    int idx = tabbedPane.indexOfTabComponent(ClosableTabComponent.this);
                    if (idx != -1) {
                        tabbedPane.setSelectedIndex(idx);
                    }
                }
            }
        });
        rawTitle = title;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (dirty) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int r = 8;
            int x = getWidth() - r - 4;
            int y = 4;
            g2.setColor(new Color(209, 47, 47, 131));
            g2.fillOval(x, y, r, r);
            g2.dispose();
        }
    }

    private JPopupMenu getPopupMenu() {
        JPopupMenu menu = new JPopupMenu();
        JMenuItem closeCurrent = new JMenuItem("关闭当前");
        JMenuItem closeOthers = new JMenuItem("关闭其他");
        JMenuItem closeAll = new JMenuItem("关闭所有");
        closeCurrent.addActionListener(e -> {
            int idx = tabbedPane.indexOfComponent(panel);
            if (idx >= 0) {
                if (panel.isModified()) {
                    int result = JOptionPane.showConfirmDialog(tabbedPane,
                            "当前请求有未保存的更改，是否保存？",
                            "未保存的更改",
                            JOptionPane.YES_NO_CANCEL_OPTION,
                            JOptionPane.WARNING_MESSAGE);
                    if (result == JOptionPane.CANCEL_OPTION) return;
                    if (result == JOptionPane.YES_OPTION && saveCallback != null) {
                        saveCallback.run();
                    }
                }
                tabbedPane.remove(idx);
                // 如果还有请求Tab，且没有选中Tab，则选中最后一个请求Tab
                int count = tabbedPane.getTabCount();
                if (count > 1) { // 还有请求Tab和+Tab
                    int selected = tabbedPane.getSelectedIndex();
                    if (selected == -1 || selected == count - 1) { // 没有选中或选中的是+Tab
                        tabbedPane.setSelectedIndex(count - 2); // 选中最后一个请求Tab
                    }
                }
            }
        });
        closeOthers.addActionListener(e -> {
            int thisIdx = tabbedPane.indexOfComponent(panel);
            List<Component> toRemove = new ArrayList<>();
            int firstDirtyIdx = -1;
            for (int i = 0; i < tabbedPane.getTabCount(); i++) {
                Component comp = tabbedPane.getComponentAt(i);
                if (comp instanceof RequestEditSubPanel && i != thisIdx) {
                    toRemove.add(comp);
                }
            }
            for (Component comp : toRemove) {
                if (comp instanceof RequestEditSubPanel subPanel) {
                    if (subPanel.isModified()) {
                        int idx = tabbedPane.indexOfComponent(comp);
                        if (firstDirtyIdx == -1) firstDirtyIdx = idx;
                        int result = JOptionPane.showConfirmDialog(tabbedPane,
                                "有未保存的更改，是否保存？\n将定位到该Tab。",
                                "未保存的更改",
                                JOptionPane.YES_NO_CANCEL_OPTION,
                                JOptionPane.WARNING_MESSAGE);
                        if (result == JOptionPane.CANCEL_OPTION) {
                            tabbedPane.setSelectedIndex(idx);
                            return;
                        }
                        if (result == JOptionPane.YES_OPTION && saveCallback != null) {
                            tabbedPane.setSelectedIndex(idx);
                            saveCallback.run();
                        }
                    }
                }
                tabbedPane.remove(comp);
            }
            // 操作完成后，定位到当前tab
            int idx = tabbedPane.indexOfComponent(panel);
            if (idx >= 0) tabbedPane.setSelectedIndex(idx);
        });
        closeAll.addActionListener(e -> {
            List<Component> toRemove = new ArrayList<>();
            int firstDirtyIdx = -1;
            for (int i = 0; i < tabbedPane.getTabCount(); i++) {
                Component comp = tabbedPane.getComponentAt(i);
                if (comp instanceof RequestEditSubPanel) {
                    toRemove.add(comp);
                }
            }
            for (Component comp : toRemove) {
                if (comp instanceof RequestEditSubPanel subPanel) {
                    if (subPanel.isModified()) {
                        int idx = tabbedPane.indexOfComponent(comp);
                        if (firstDirtyIdx == -1) firstDirtyIdx = idx;
                        int result = JOptionPane.showConfirmDialog(tabbedPane,
                                "有未保存的更改，是否保存？\n将定位到该Tab。",
                                "未保存的更改",
                                JOptionPane.YES_NO_CANCEL_OPTION,
                                JOptionPane.WARNING_MESSAGE);
                        if (result == JOptionPane.CANCEL_OPTION) {
                            tabbedPane.setSelectedIndex(idx);
                            return;
                        }
                        if (result == JOptionPane.YES_OPTION && saveCallback != null) {
                            tabbedPane.setSelectedIndex(idx);
                            saveCallback.run();
                        }
                    }
                }
                tabbedPane.remove(comp);
            }
        });
        menu.add(closeCurrent);
        menu.add(closeOthers);
        menu.add(closeAll);
        return menu;
    }

    public void setDirty(boolean dirty) {
        this.dirty = dirty;
        label.setText(rawTitle);
        repaint();
    }
}