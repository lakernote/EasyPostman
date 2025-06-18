package com.laker.postman.common.tab;

import com.laker.postman.panel.collections.edit.RequestEditSubPanel;

import javax.swing.*;
import java.awt.*;

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
            }
        });
        closeOthers.addActionListener(e -> {
            int thisIdx = tabbedPane.indexOfComponent(panel);
            for (int i = tabbedPane.getTabCount() - 1; i >= 0; i--) {
                if (tabbedPane.getComponentAt(i) instanceof RequestEditSubPanel subPanel && i != thisIdx) {
                    if (subPanel.isModified()) {
                        int result = JOptionPane.showConfirmDialog(tabbedPane,
                                "有未保存的更改，是否保存？",
                                "未保存的更改",
                                JOptionPane.YES_NO_CANCEL_OPTION,
                                JOptionPane.WARNING_MESSAGE);
                        if (result == JOptionPane.CANCEL_OPTION) continue;
                        if (result == JOptionPane.YES_OPTION && saveCallback != null) {
                            saveCallback.run();
                        }
                    }
                    tabbedPane.remove(i);
                }
            }
        });
        closeAll.addActionListener(e -> {
            for (int i = tabbedPane.getTabCount() - 1; i >= 0; i--) {
                if (tabbedPane.getComponentAt(i) instanceof RequestEditSubPanel subPanel) {
                    if (subPanel.isModified()) {
                        int result = JOptionPane.showConfirmDialog(tabbedPane,
                                "有未保存的更改，是否保存？",
                                "未保存的更改",
                                JOptionPane.YES_NO_CANCEL_OPTION,
                                JOptionPane.WARNING_MESSAGE);
                        if (result == JOptionPane.CANCEL_OPTION) continue;
                        if (result == JOptionPane.YES_OPTION && saveCallback != null) {
                            saveCallback.run();
                        }
                    }
                    tabbedPane.remove(i);
                }
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