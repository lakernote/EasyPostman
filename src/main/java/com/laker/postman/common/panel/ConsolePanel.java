package com.laker.postman.common.panel;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.laker.postman.common.constants.Colors;
import com.laker.postman.util.FontUtil;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * 控制台日志面板
 * 用于显示应用程序的日志信息，支持展开和收起功能。
 * 当面板展开时，显示日志内容；当收起时，仅显示标题。
 * 日志内容可以通过 appendConsoleLog 方法追加。
 * 日志面板默认收起状态，点击标题可以展开。
 * 日志内容使用 JTextArea 显示，支持自动换行和滚动条。
 * 日志面板的样式和交互设计旨在提供清晰的用户体验，便于查看和管理日志信息。
 */
@Slf4j
public class ConsolePanel extends BasePanel {
    private JPanel consoleContainer;
    private JLabel consoleLabel;
    private JPanel consolePanel;
    private JTextArea consoleLogArea;
    private boolean expanded = false;


    @Override
    protected void initUI() {
        setLayout(new BorderLayout());
        consoleContainer = new JPanel(new BorderLayout());
        consoleContainer.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Color.LIGHT_GRAY));
        consoleContainer.setOpaque(false);
        createConsoleLabel();
        createConsolePanel();
        setConsoleExpanded(false);
    }

    @Override
    protected void registerListeners() {

    }

    private void createConsolePanel() {
        consolePanel = new JPanel(new BorderLayout());
        JLabel title = new JLabel("Console");
        consoleLogArea = new JTextArea(8,20);
        consoleLogArea.setEditable(false);
        consoleLogArea.setLineWrap(true);
        consoleLogArea.setWrapStyleWord(true);
        consoleLogArea.setFocusable(true);
        consoleLogArea.setCursor(Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR));
        JScrollPane logScroll = new JScrollPane(consoleLogArea);
        logScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        logScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        JButton toggleBtn = new JButton();
        toggleBtn.setIcon(new FlatSVGIcon("icons/clear.svg")); // 默认收起图标
        toggleBtn.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
        toggleBtn.setBackground(Colors.PANEL_BACKGROUND);
        toggleBtn.setToolTipText("收起控制台");
        toggleBtn.addActionListener(e -> setConsoleExpanded(false));

        JButton clearBtn = new JButton();
        clearBtn.setIcon(new FlatSVGIcon("icons/clear.svg"));
        clearBtn.setBorder(BorderFactory.createEmptyBorder());
        clearBtn.setBackground(Colors.PANEL_BACKGROUND);
        clearBtn.setToolTipText("清空日志");
        clearBtn.addActionListener(e -> consoleLogArea.setText(""));

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(title, BorderLayout.WEST);
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        btnPanel.setOpaque(false);
        btnPanel.add(clearBtn);
        btnPanel.add(toggleBtn);
        topPanel.add(btnPanel, BorderLayout.EAST);
        topPanel.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
        consolePanel.add(topPanel, BorderLayout.NORTH);
        consolePanel.add(logScroll, BorderLayout.CENTER);
    }

    private void setConsoleExpanded(boolean expanded) {
        this.expanded = expanded;
        removeAll();
        if (expanded) {
            consoleContainer.removeAll();
            consoleContainer.add(consolePanel, BorderLayout.CENTER);
            add(consoleContainer, BorderLayout.CENTER);
        } else {
            consoleContainer.removeAll();
            consoleContainer.add(consoleLabel, BorderLayout.CENTER);
            add(consoleContainer, BorderLayout.SOUTH);
        }
        revalidate();
        repaint();
    }

    private void createConsoleLabel() {
        consoleLabel = new JLabel("Console");
        consoleLabel.setIcon(new FlatSVGIcon("icons/console.svg", 16, 16));
        consoleLabel.setFont(FontUtil.getDefaultFont(Font.BOLD, 12));
        consoleLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        consoleLabel.setBorder(BorderFactory.createEmptyBorder(4, 12, 4, 12));
        consoleLabel.setFocusable(true);
        consoleLabel.setEnabled(true);
        consoleLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                setConsoleExpanded(true);
            }
        });
    }

    public void appendConsoleLog(String msg) {
        if (consoleLogArea != null) {
            SwingUtilities.invokeLater(() -> {
                consoleLogArea.append(msg + "\n");
                consoleLogArea.setCaretPosition(consoleLogArea.getText().length());
            });
        }
    }

    public boolean isExpanded() {
        return expanded;
    }
}

