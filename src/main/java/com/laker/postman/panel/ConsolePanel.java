package com.laker.postman.panel;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.laker.postman.common.SingletonFactory;
import com.laker.postman.common.component.SearchTextField;
import com.laker.postman.common.constants.Colors;
import com.laker.postman.common.panel.SingletonBasePanel;
import jiconfont.icons.font_awesome.FontAwesome;
import jiconfont.swing.IconFontSwing;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.ActionListener;

@Slf4j
public class ConsolePanel extends SingletonBasePanel {
    private JTextPane consoleLogArea;
    private StyledDocument consoleDoc;
    private JButton closeBtn;

    // 日志类型
    public enum LogType {
        INFO, ERROR, SUCCESS, WARN, DEBUG, TRACE, CUSTOM
    }

    @Override
    protected void initUI() {
        setLayout(new BorderLayout());
        createConsolePanel();
    }

    @Override
    protected void registerListeners() {

    }

    private void createConsolePanel() {
        consoleLogArea = new JTextPane();
        consoleLogArea.setEditable(false);
        consoleLogArea.setFocusable(true);
        consoleLogArea.setCursor(Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR));
        consoleDoc = consoleLogArea.getStyledDocument();
        JScrollPane logScroll = new JScrollPane(consoleLogArea);
        logScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        logScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        closeBtn = new JButton();
        closeBtn.setIcon(IconFontSwing.buildIcon(FontAwesome.TIMES, 16, new Color(80, 80, 80)));
        closeBtn.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
        closeBtn.setBackground(Colors.PANEL_BACKGROUND);
        // 关闭按钮事件由外部注册

        JButton clearBtn = new JButton();
        clearBtn.setIcon(new FlatSVGIcon("icons/clear.svg"));
        clearBtn.setBorder(BorderFactory.createEmptyBorder());
        clearBtn.setBackground(Colors.PANEL_BACKGROUND);
        clearBtn.setToolTipText("Clear Log");
        clearBtn.addActionListener(e -> {
            try {
                consoleDoc.remove(0, consoleDoc.getLength());
            } catch (BadLocationException ex) {
                // ignore
            }
        });

        JTextField searchField = new SearchTextField();
        JButton prevBtn = new JButton("Prev");
        JButton nextBtn = new JButton("Next");
        prevBtn.setFocusable(false);
        nextBtn.setFocusable(false);
        searchField.setToolTipText("Search log content");
        searchField.addActionListener(e -> nextBtn.doClick());
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 0));
        searchPanel.setOpaque(false);
        searchPanel.add(searchField);
        searchPanel.add(prevBtn);
        searchPanel.add(nextBtn);

        final int[] lastMatchPos = {-1};
        final String[] lastKeyword = {""};
        nextBtn.addActionListener(e -> {
            String keyword = searchField.getText();
            if (keyword.isEmpty()) return;
            String text = consoleLogArea.getText();
            int start = lastKeyword[0].equals(keyword) ? lastMatchPos[0] + 1 : 0;
            int pos = text.indexOf(keyword, start);
            if (pos == -1 && start > 0) {
                pos = text.indexOf(keyword);
            }
            if (pos != -1) {
                highlightSearchResult(pos, keyword.length());
                lastMatchPos[0] = pos;
                lastKeyword[0] = keyword;
            }
        });
        prevBtn.addActionListener(e -> {
            String keyword = searchField.getText();
            if (keyword.isEmpty()) return;
            String text = consoleLogArea.getText();
            int start = lastKeyword[0].equals(keyword) ? lastMatchPos[0] - 1 : text.length();
            int pos = text.lastIndexOf(keyword, start);
            if (pos == -1 && start < text.length()) {
                pos = text.lastIndexOf(keyword);
            }
            if (pos != -1) {
                highlightSearchResult(pos, keyword.length());
                lastMatchPos[0] = pos;
                lastKeyword[0] = keyword;
            }
        });

        JPanel topPanel = new JPanel(new BorderLayout());
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        btnPanel.setOpaque(false);
        btnPanel.add(clearBtn);
        btnPanel.add(closeBtn);
        topPanel.add(btnPanel, BorderLayout.EAST);
        topPanel.add(searchPanel, BorderLayout.CENTER);
        topPanel.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
        add(topPanel, BorderLayout.NORTH);
        add(logScroll, BorderLayout.CENTER);
    }

    private void highlightSearchResult(int pos, int len) {
        try {
            consoleLogArea.getHighlighter().removeAllHighlights();
            consoleLogArea.getHighlighter().addHighlight(pos, pos + len, new DefaultHighlighter.DefaultHighlightPainter(Color.YELLOW));
            consoleLogArea.setCaretPosition(pos + len);
        } catch (BadLocationException ex) {
            // ignore
        }
    }

    public synchronized void appendConsoleLog(String msg) {
        appendConsoleLog(msg, LogType.INFO);
    }

    public synchronized void appendConsoleLog(String msg, LogType type) {
        SwingUtilities.invokeLater(() -> {
            Style style = consoleLogArea.addStyle("logStyle", null);
            switch (type) {
                case ERROR:
                    StyleConstants.setForeground(style, new Color(220, 53, 69));
                    StyleConstants.setBold(style, true);
                    break;
                case SUCCESS:
                    StyleConstants.setForeground(style, new Color(40, 167, 69));
                    StyleConstants.setBold(style, true);
                    break;
                case WARN:
                    StyleConstants.setForeground(style, new Color(255, 193, 7));
                    StyleConstants.setBold(style, true);
                    break;
                case DEBUG:
                    StyleConstants.setForeground(style, new Color(0, 123, 255));
                    StyleConstants.setBold(style, false);
                    break;
                case TRACE:
                    StyleConstants.setForeground(style, new Color(111, 66, 193));
                    StyleConstants.setBold(style, false);
                    break;
                case CUSTOM:
                    StyleConstants.setForeground(style, new Color(23, 162, 184));
                    StyleConstants.setBold(style, false);
                    break;
                default:
                    StyleConstants.setForeground(style, new Color(33, 37, 41));
                    StyleConstants.setBold(style, false);
            }
            try {
                consoleDoc.insertString(consoleDoc.getLength(), msg + "\n", style);
                consoleLogArea.setCaretPosition(consoleDoc.getLength());
            } catch (BadLocationException e) {
                // ignore
            }
        });
    }

    // 静态代理方法，便于外部调用
    public static void appendLog(String msg) {
        SingletonFactory.getInstance(ConsolePanel.class).appendConsoleLog(msg);
    }

    public static void appendLog(String msg, LogType type) {
        SingletonFactory.getInstance(ConsolePanel.class).appendConsoleLog(msg, type);
    }

    public void setCloseAction(ActionListener listener) {
        closeBtn.addActionListener(listener);
    }
}