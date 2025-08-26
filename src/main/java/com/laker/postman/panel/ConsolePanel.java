package com.laker.postman.panel;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.laker.postman.common.SingletonFactory;
import com.laker.postman.common.component.SearchTextField;
import com.laker.postman.common.constants.EasyPostManColors;
import com.laker.postman.common.panel.SingletonBasePanel;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.ActionListener;

@Slf4j
public class ConsolePanel extends SingletonBasePanel {
    private JTextPane consoleLogArea;
    private transient StyledDocument consoleDoc;
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
        logScroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        logScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        closeBtn = new JButton();
        closeBtn.setIcon(new FlatSVGIcon("icons/close.svg", 20, 20));
        closeBtn.setBorder(BorderFactory.createEmptyBorder());
        closeBtn.setBackground(EasyPostManColors.PANEL_BACKGROUND);
        // 关闭按钮事件由外部注册

        JButton clearBtn = getClearBtn();

        JTextField searchField = new SearchTextField();
        JButton prevBtn = new JButton();
        prevBtn.setIcon(new FlatSVGIcon("icons/arrow-up.svg", 20, 20));
        JButton nextBtn = new JButton();
        nextBtn.setIcon(new FlatSVGIcon("icons/arrow-down.svg", 20, 20));
        prevBtn.setFocusable(false);
        nextBtn.setFocusable(false);
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
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        btnPanel.setOpaque(false);
        btnPanel.add(clearBtn);
        btnPanel.add(closeBtn);
        topPanel.add(btnPanel, BorderLayout.EAST);
        topPanel.add(searchPanel, BorderLayout.CENTER);
        topPanel.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
        add(topPanel, BorderLayout.NORTH);
        add(logScroll, BorderLayout.CENTER);
    }

    private JButton getClearBtn() {
        JButton clearBtn = new JButton();
        clearBtn.setIcon(new FlatSVGIcon("icons/clear.svg"));
        clearBtn.setBorder(BorderFactory.createEmptyBorder());
        clearBtn.setBackground(EasyPostManColors.PANEL_BACKGROUND);
        clearBtn.addActionListener(e -> {
            try {
                consoleDoc.remove(0, consoleDoc.getLength());
            } catch (BadLocationException ex) {
                // ignore
            }
        });
        return clearBtn;
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
                case INFO:
                    StyleConstants.setForeground(style, new Color(76, 130, 206));
                    StyleConstants.setBold(style, false);
                    break;
                // 默认情况，使用普通文本样式
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