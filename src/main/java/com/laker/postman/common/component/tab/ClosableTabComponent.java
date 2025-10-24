package com.laker.postman.common.component.tab;


import com.laker.postman.common.SingletonFactory;
import com.laker.postman.common.constants.EasyPostManColors;
import com.laker.postman.model.HttpRequestItem;
import com.laker.postman.model.RequestItemProtocolEnum;
import com.laker.postman.panel.collections.left.RequestCollectionsLeftPanel;
import com.laker.postman.panel.collections.right.RequestEditPanel;
import com.laker.postman.panel.collections.right.request.RequestEditSubPanel;
import com.laker.postman.util.FontsUtil;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

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
@Slf4j
public class ClosableTabComponent extends JPanel {
    public static final String ELLIPSIS = "...";
    private static final int MAX_TAB_WIDTH = 160; // 最大Tab宽度
    private static final int MIN_TAB_WIDTH = 80;  // 最小Tab宽度
    private static final int TAB_HEIGHT = 28; // Tab高度
    private final JLabel label;
    private final String rawTitle;
    @Getter
    private boolean dirty = false;
    @Getter
    private boolean newRequest = false;
    private final JTabbedPane tabbedPane;

    private boolean hoverClose = false; // 鼠标是否悬浮在关闭按钮区域
    private static final int CLOSE_DIAMETER = 12; // 关闭按钮直径
    private static final int CLOSE_MARGIN = 0; // 关闭按钮距离顶部和右侧的距离

    public ClosableTabComponent(String title, RequestItemProtocolEnum protocol) {
        setOpaque(false);
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
        setToolTipText(title); // 设置完整标题为tooltip
        this.tabbedPane = SingletonFactory.getInstance(RequestEditPanel.class).getTabbedPane();

        // 动态计算宽度，最大不超过MAX_TAB_WIDTH
        FontMetrics fm = getFontMetrics(getFont());
        // 计算关闭按钮占用的空间：按钮直径 + 间距 + 右边距 + 左侧图标空间
        int closeButtonSpace = CLOSE_DIAMETER + 4 + CLOSE_MARGIN; // 8px为文本与按钮之间的间距
        int iconSpace = 20; // 为协议图标预留空间
        int padding = 20; // 左右内边距

        // 计算合理的Tab宽度
        int idealTextWidth = fm.stringWidth(title);
        int totalRequiredWidth = idealTextWidth + iconSpace + closeButtonSpace + padding;
        int tabWidth = Math.max(Math.min(totalRequiredWidth, MAX_TAB_WIDTH), MIN_TAB_WIDTH);
        setPreferredSize(new Dimension(tabWidth, TAB_HEIGHT));

        // 截断文本并设置tooltip
        String displayTitle = title;
        int maxLabelWidth = tabWidth - iconSpace - closeButtonSpace - padding; // 为图标和关闭按钮预留空间
        if (fm.stringWidth(title) > maxLabelWidth) { // 需要截断
            int len = title.length(); // 从完整标题开始
            while (len > 0 && fm.stringWidth(title.substring(0, len) + ELLIPSIS) > maxLabelWidth) { // 逐渐减少长度
                len--; // 减少一个字符
            }
            displayTitle = title.substring(0, len) + ELLIPSIS; // 截断并添加省略号
        }

        label = new JLabel(displayTitle) {
            @Override
            public boolean contains(int x, int y) { // 重写contains方法，避免遮挡关闭按钮的鼠标事件
                return false;
            }
        };
        label.setFont(FontsUtil.getDefaultFont(Font.PLAIN, 12));
        label.setIcon(protocol.getIcon());
        label.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, closeButtonSpace + 4)); // 右侧预留关闭按钮空间
        label.setHorizontalAlignment(SwingConstants.LEFT); // 改为左对齐，避免文本居中与按钮重叠
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
                        // 反向定位到左侧树节点
                        locateRequestTreeNode(idx);
                    }
                }
            }
        });
        rawTitle = title;
    }

    private void locateRequestTreeNode(int idx) {
        Component selectedComponent = tabbedPane.getComponentAt(idx);
        if (selectedComponent instanceof RequestEditSubPanel subPanel) {
            HttpRequestItem currentRequest = subPanel.getCurrentRequest();
            if (currentRequest != null && currentRequest.getId() != null) {
                SwingUtilities.invokeLater(() -> {
                    try {
                        RequestCollectionsLeftPanel collectionPanel = SingletonFactory.getInstance(RequestCollectionsLeftPanel.class);
                        collectionPanel.locateAndSelectRequest(currentRequest.getId());
                    } catch (Exception ex) {
                        log.error("定位请求节点时出错", ex);
                    }
                });
            }
        }
    }

    private Rectangle getCloseButtonBounds() {
        int r = CLOSE_DIAMETER;
        int x = getWidth() - r - CLOSE_MARGIN;
        return new Rectangle(x, (getHeight() - r) / 2, r, r);
    }

    private boolean isInCloseButton(int x, int y) {
        return getCloseButtonBounds().contains(x, y);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON); // 抗锯齿
        int r = CLOSE_DIAMETER;
        int x = getWidth() - r - CLOSE_MARGIN;
        int y = (getHeight() - r) / 2;
        if (hoverClose) {
            // 绘制关闭按钮
            Color base = EasyPostManColors.TAB_SELECTED_BACKGROUND;
            Color transparent = new Color(base.getRed(), base.getGreen(), base.getBlue(), 180); // 180透明度代表半透明
            g2.setColor(transparent);
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