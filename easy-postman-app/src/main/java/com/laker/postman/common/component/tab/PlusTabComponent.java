package com.laker.postman.common.component.tab;

import com.laker.postman.util.IconUtil;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import static com.laker.postman.util.IconUtil.SIZE_SMALL;

/**
 * PlusTabComponent 用于显示一个加号图标的标签组件
 */
public class PlusTabComponent extends JPanel {
    private static final int BUTTON_WIDTH = 34;
    private static final int BUTTON_HEIGHT = 30;
    private static final int BUTTON_ARC = 8;
    private boolean hover;

    public PlusTabComponent() {
        setOpaque(false);
        setFocusable(false);
        setPreferredSize(new Dimension(BUTTON_WIDTH, BUTTON_HEIGHT));
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setLayout(new BorderLayout());

        JLabel plusLabel = new JLabel();
        plusLabel.setIcon(IconUtil.createThemed("icons/plus.svg", SIZE_SMALL, SIZE_SMALL));
        plusLabel.setHorizontalAlignment(SwingConstants.CENTER);
        plusLabel.setVerticalAlignment(SwingConstants.CENTER);
        add(plusLabel, BorderLayout.CENTER);

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                setHover(true);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                setHover(false);
            }
        });
    }

    private void setHover(boolean hover) {
        if (this.hover == hover) {
            return;
        }
        this.hover = hover;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        if (hover) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(RequestEditorTabTheme.hoverTabBackground());
            g2.fillRoundRect(1, 2, getWidth() - 2, getHeight() - 4, BUTTON_ARC, BUTTON_ARC);
            g2.dispose();
        }
        super.paintComponent(g);
    }
}
