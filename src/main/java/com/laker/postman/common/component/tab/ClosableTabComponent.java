package com.laker.postman.common.component.tab;


import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.laker.postman.common.SingletonFactory;
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

/**
 * 通用可关闭Tab组件，支持右上角红点脏标记
 * 支持亮色和暗色主题自适应
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
    @Getter
    private boolean previewMode = false; // 预览模式：单击打开的临时 tab
    private final JTabbedPane tabbedPane;

    private boolean hoverClose = false; // 鼠标是否悬浮在关闭按钮区域
    private static final int CLOSE_DIAMETER = 10; // 关闭按钮直径
    private static final int CLOSE_MARGIN = 0; // 关闭按钮距离顶部和右侧的距离
    private static final int CLOSE_TEXT_SPACING = 0; // 关闭按钮与文字之间的间距
    private static final int LABEL_HORIZONTAL_PADDING = 4; // Label 左右内边距

    /**
     * 检查当前是否为暗色主题
     */
    private boolean isDarkTheme() {
        return FlatLaf.isLafDark();
    }

    /**
     * 获取主题适配的关闭按钮背景色
     */
    private Color getCloseButtonBackground() {
        Color tabBg = tabbedPane.getBackground();
        // 使用 tab 背景色，添加透明度
        return new Color(tabBg.getRed(), tabBg.getGreen(), tabBg.getBlue(),
                isDarkTheme() ? 200 : 180);
    }

    /**
     * 获取主题适配的关闭按钮线条颜色
     */
    private Color getCloseButtonLineColor() {
        // 暗色主题使用白色，亮色主题使用黑色
        return isDarkTheme() ? Color.WHITE : Color.BLACK;
    }

    /**
     * 获取主题适配的新请求标记颜色
     */
    private Color getNewRequestColor() {
        // 黄色在两种主题下都适用
        return new Color(255, 204, 0, 180);
    }

    /**
     * 获取主题适配的脏标记颜色
     */
    private Color getDirtyColor() {
        // 红色在两种主题下都适用，但暗色主题可以稍微亮一点
        if (isDarkTheme()) {
            return new Color(239, 83, 80, 180); // 稍微亮一点的红色
        } else {
            return new Color(209, 47, 47, 131); // 原来的红色
        }
    }

    public ClosableTabComponent(String title, RequestItemProtocolEnum protocol) {
        setOpaque(false);
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
        setToolTipText(title); // 设置完整标题为tooltip
        this.tabbedPane = SingletonFactory.getInstance(RequestEditPanel.class).getTabbedPane();

        // 动态计算宽度，最大不超过MAX_TAB_WIDTH
        FontMetrics fm = getFontMetrics(getFont());
        // 计算关闭按钮占用的空间：按钮直径 + 间距 + 右边距 + 左侧图标空间
        int closeButtonSpace = CLOSE_DIAMETER + CLOSE_TEXT_SPACING + CLOSE_MARGIN; // 按钮直径 + 文字间距 + 右边距
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
        label.setFont(FontsUtil.getDefaultFontWithOffset(Font.PLAIN, -1)); // 使用用户设置的字体大小，略小一点
        if (protocol != null) {
            label.setIcon(protocol.getIcon());
        } else {
            // 分组编辑tab
            label.setIcon(new FlatSVGIcon("icons/group.svg", 16, 16));
        }
        label.setBorder(BorderFactory.createEmptyBorder(0, LABEL_HORIZONTAL_PADDING, 0, closeButtonSpace + CLOSE_TEXT_SPACING)); // 右侧预留关闭按钮空间
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
                            // 关闭按钮点击：先选中再关闭
                            tabbedPane.setSelectedIndex(idx);
                            SingletonFactory.getInstance(RequestEditPanel.class).closeCurrentTab();
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
            // 绘制关闭按钮（主题适配）
            g2.setColor(getCloseButtonBackground());
            g2.fillOval(x, y, r, r);
            g2.setColor(getCloseButtonLineColor());
            int pad = 2;
            g2.setStroke(new BasicStroke(1.5f));
            g2.drawLine(x + pad, y + pad, x + r - pad, y + r - pad);
            g2.drawLine(x + r - pad, y + pad, x + pad, y + r - pad);
        } else if (newRequest) {
            // 新请求标记（主题适配）
            g2.setColor(getNewRequestColor());
            g2.fillOval(x, y, r, r);
        } else if (dirty) {
            // 脏标记（主题适配）
            g2.setColor(getDirtyColor());
            g2.fillOval(x, y, r, r);
        }
        g2.dispose();
    }

    private JPopupMenu getPopupMenu() {
        JPopupMenu menu = new JPopupMenu();
        JMenuItem closeCurrent = new JMenuItem(I18nUtil.getMessage(MessageKeys.TAB_CLOSE_CURRENT));
        JMenuItem closeOthers = new JMenuItem(I18nUtil.getMessage(MessageKeys.TAB_CLOSE_OTHERS));
        JMenuItem closeAll = new JMenuItem(I18nUtil.getMessage(MessageKeys.TAB_CLOSE_ALL));

        RequestEditPanel editPanel = SingletonFactory.getInstance(RequestEditPanel.class);

        // 关闭当前标签
        closeCurrent.addActionListener(e -> {
            int idx = tabbedPane.indexOfTabComponent(this);
            if (idx >= 0) {
                tabbedPane.setSelectedIndex(idx); // 先选中
                editPanel.closeCurrentTab();
            }
        });

        // 关闭其他标签
        closeOthers.addActionListener(e -> {
            int idx = tabbedPane.indexOfTabComponent(this);
            if (idx >= 0) {
                tabbedPane.setSelectedIndex(idx); // 先选中
                editPanel.closeOtherTabs();
            }
        });

        // 关闭所有标签
        closeAll.addActionListener(e -> editPanel.closeAllTabs());

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

    public void setNewRequest(boolean newRequest) {
        this.newRequest = newRequest;
        label.setText(rawTitle);
        repaint();
    }

    /**
     * 设置预览模式
     * 预览模式的 tab 使用斜体字体，提示用户这是临时的
     */
    public void setPreviewMode(boolean previewMode) {
        this.previewMode = previewMode;
        if (previewMode) {
            label.setFont(FontsUtil.getDefaultFontWithOffset(Font.ITALIC, -1)); // 使用用户设置的字体大小
        } else {
            label.setFont(FontsUtil.getDefaultFontWithOffset(Font.PLAIN, -1)); // 使用用户设置的字体大小
        }
        repaint();
    }
}
