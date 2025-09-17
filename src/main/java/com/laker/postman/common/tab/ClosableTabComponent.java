package com.laker.postman.common.tab;


import com.laker.postman.common.SingletonFactory;
import com.laker.postman.common.constants.EasyPostManColors;
import com.laker.postman.panel.collections.right.RequestEditPanel;
import com.laker.postman.panel.collections.right.request.RequestEditSubPanel;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;
import lombok.Getter;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.util.ArrayList;
import java.util.List;

/**
 * 通用可关闭Tab组件，支持右上角红点脏标记
 */
public class ClosableTabComponent extends JPanel {
    private static final int MAX_TAB_WIDTH = 100; // 最大Tab宽度
    private static final int TAB_HEIGHT = 20; // Tab高度
    private final JLabel label;
    private final String rawTitle;
    @Getter
    private boolean dirty = false;
    @Getter
    private boolean newRequest = false;
    private final JTabbedPane tabbedPane;

    private boolean hoverClose = false; // 鼠标是否悬浮在关闭按钮区域
    private static final int CLOSE_DIAMETER = 10; // 关闭按钮直径
    private static final int CLOSE_MARGIN = 0; // 关闭按钮距离顶部和右侧的距离

    public ClosableTabComponent(String title) {
        setOpaque(false);
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(2, 0, 2, 0));
        this.tabbedPane = SingletonFactory.getInstance(RequestEditPanel.class).getTabbedPane();
        // 动态计算宽度，最大不超过MAX_TAB_WIDTH
        FontMetrics fm = getFontMetrics(getFont());
        int textWidth = fm.stringWidth(title) + 32;
        int tabWidth = Math.min(textWidth, MAX_TAB_WIDTH);
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
        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                if (!hoverClose) {
                    hoverClose = true;
                    repaint();
                }
            }
        });
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseExited(MouseEvent e) {
                if (hoverClose) {
                    hoverClose = false;
                    repaint();
                }
            }

            @Override
            public void mousePressed(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e)) {  // 左键点击
                    int idx = tabbedPane.indexOfTabComponent(ClosableTabComponent.this); // 获取当前Tab的索引
                    if (idx != -1) { // 确保索引有效
                        if (isInCloseButton(e.getX(), e.getY())) {
                            closeCurrent();
                            return;
                        }
                        tabbedPane.setSelectedIndex(idx); // 选中当前Tab
                    }
                }
            }
        });
        rawTitle = title;
    }

    private Rectangle getCloseButtonBounds() {
        int r = CLOSE_DIAMETER;
        int x = getWidth() - r - CLOSE_MARGIN;
        int y = CLOSE_MARGIN;
        return new Rectangle(x, y, r, r);
    }

    private boolean isInCloseButton(int x, int y) {
        return getCloseButtonBounds().contains(x, y);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON); // 抗锯齿
        Rectangle closeRect = getCloseButtonBounds();
        int r = CLOSE_DIAMETER;
        int x = closeRect.x;
        int y = closeRect.y;
        if (hoverClose) {
            // 绘制关闭按钮
            g2.setColor(EasyPostManColors.TAB_SELECTED_BACKGROUND);
            g2.fillOval(x, y, r, r);
            g2.setColor(Color.BLACK);
            int pad = 2;
            g2.setStroke(new BasicStroke(1.5f));
            g2.drawLine(x + pad, y + pad, x + r - pad, y + r - pad);
            g2.drawLine(x + r - pad, y + pad, x + pad, y + r - pad);
        } else if (newRequest) {
            g2.setColor(new Color(255, 204, 0, 180)); // yellow
            g2.fillOval(x, y, r, r);
        } else if (dirty) {
            g2.setColor(new Color(209, 47, 47, 131)); // red
            g2.fillOval(x, y, r, r);
        }
        g2.dispose();
    }

    private JPopupMenu getPopupMenu() {
        JPopupMenu menu = new JPopupMenu();
        JMenuItem closeCurrent = new JMenuItem(I18nUtil.getMessage(MessageKeys.TAB_CLOSE_CURRENT));
        JMenuItem closeOthers = new JMenuItem(I18nUtil.getMessage(MessageKeys.TAB_CLOSE_OTHERS));
        JMenuItem closeAll = new JMenuItem(I18nUtil.getMessage(MessageKeys.TAB_CLOSE_ALL));
        closeCurrent.addActionListener(e -> closeCurrent());
        closeOthers.addActionListener(e -> {
            int thisIdx = tabbedPane.indexOfTabComponent(this);
            List<Component> toRemove = new ArrayList<>();
            int firstDirtyIdx = -1;
            for (int i = 0; i < tabbedPane.getTabCount(); i++) {
                Component comp = tabbedPane.getComponentAt(i);
                if (comp instanceof RequestEditSubPanel && i != thisIdx) {
                    toRemove.add(comp);
                }
            }
            for (Component comp : toRemove) {
                if (comp instanceof RequestEditSubPanel subPanel && subPanel.isModified()) {
                    int idx = tabbedPane.indexOfComponent(comp);
                    if (firstDirtyIdx == -1) firstDirtyIdx = idx;
                    int result = JOptionPane.showConfirmDialog(tabbedPane,
                            I18nUtil.getMessage(MessageKeys.TAB_UNSAVED_CHANGES_SAVE_OTHERS),
                            I18nUtil.getMessage(MessageKeys.TAB_UNSAVED_CHANGES_TITLE),
                            JOptionPane.YES_NO_CANCEL_OPTION,
                            JOptionPane.WARNING_MESSAGE);
                    if (result == JOptionPane.CANCEL_OPTION) {
                        tabbedPane.setSelectedIndex(idx);
                        return;
                    }
                    if (result == JOptionPane.YES_OPTION) {
                        tabbedPane.setSelectedIndex(idx);
                        SingletonFactory.getInstance(RequestEditPanel.class).saveCurrentRequest();
                    }
                }
                tabbedPane.remove(comp);
            }
            // 操作完成后，定位到当前tab
            int idx = tabbedPane.indexOfTabComponent(this);
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
                if (comp instanceof RequestEditSubPanel subPanel && subPanel.isModified()) {
                    int idx = tabbedPane.indexOfComponent(comp);
                    if (firstDirtyIdx == -1) firstDirtyIdx = idx;
                    int result = JOptionPane.showConfirmDialog(tabbedPane,
                            I18nUtil.getMessage(MessageKeys.TAB_UNSAVED_CHANGES_SAVE_ALL),
                            I18nUtil.getMessage(MessageKeys.TAB_UNSAVED_CHANGES_TITLE),
                            JOptionPane.YES_NO_CANCEL_OPTION,
                            JOptionPane.WARNING_MESSAGE);
                    if (result == JOptionPane.CANCEL_OPTION) {
                        tabbedPane.setSelectedIndex(idx);
                        return;
                    }
                    if (result == JOptionPane.YES_OPTION) {
                        tabbedPane.setSelectedIndex(idx);
                        SingletonFactory.getInstance(RequestEditPanel.class).saveCurrentRequest();
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

    private void closeCurrent() {
        int idx = tabbedPane.indexOfTabComponent(this);
        if (idx >= 0) {
            RequestEditSubPanel editSubPanel = (RequestEditSubPanel) tabbedPane.getComponentAt(idx);
            if (editSubPanel.isModified()) {
                int result = JOptionPane.showConfirmDialog(tabbedPane,
                        I18nUtil.getMessage(MessageKeys.TAB_UNSAVED_CHANGES_SAVE_CURRENT),
                        I18nUtil.getMessage(MessageKeys.TAB_UNSAVED_CHANGES_TITLE),
                        JOptionPane.YES_NO_CANCEL_OPTION,
                        JOptionPane.WARNING_MESSAGE);
                if (result == JOptionPane.CANCEL_OPTION) return;
                if (result == JOptionPane.YES_OPTION) {
                    SingletonFactory.getInstance(RequestEditPanel.class).saveCurrentRequest();
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
    }

    public void setDirty(boolean dirty) {
        this.dirty = dirty;
        label.setText(rawTitle);
        repaint();
    }

    public void setNewRequest(boolean newRequest) {
        this.newRequest = newRequest;
        label.setText(rawTitle);
        repaint();
    }
}