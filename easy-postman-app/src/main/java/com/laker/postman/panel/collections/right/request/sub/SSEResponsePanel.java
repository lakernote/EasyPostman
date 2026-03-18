package com.laker.postman.panel.collections.right.request.sub;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.json.JSONUtil;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.laker.postman.common.SingletonFactory;
import com.laker.postman.common.component.SearchTextField;
import com.laker.postman.common.component.button.ClearButton;
import com.laker.postman.frame.MainFrame;
import com.laker.postman.model.MessageType;
import com.laker.postman.model.script.TestResult;
import com.laker.postman.service.render.HttpHtmlRenderer;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.JsonUtil;
import com.laker.postman.util.MessageKeys;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * SSE响应体面板，展示事件时间线以及 event/id/retry 等元信息。
 */
public class SSEResponsePanel extends JPanel {
    private final JTable table;
    private final DefaultTableModel tableModel;
    private final JComboBox<String> typeFilterBox;
    private final SearchTextField searchField;
    private final ClearButton clearButton;
    private final List<MessageRow> allRows = new ArrayList<>();
    private final List<MessageRow> visibleRows = new ArrayList<>();

    private static final int COLUMN_TYPE = 0;
    private static final int COLUMN_TIME = 1;
    private static final int COLUMN_EVENT_ID = 2;
    private static final int COLUMN_EVENT_TYPE = 3;
    private static final int COLUMN_RETRY = 4;
    private static final int COLUMN_CONTENT = 5;
    private static final int COLUMN_ASSERTION = 6;

    private static final String[] COLUMN_NAMES = {
            I18nUtil.getMessage(MessageKeys.WEBSOCKET_COLUMN_TYPE),
            I18nUtil.getMessage(MessageKeys.WEBSOCKET_COLUMN_TIME),
            I18nUtil.getMessage(MessageKeys.SSE_COLUMN_EVENT_ID),
            I18nUtil.getMessage(MessageKeys.SSE_COLUMN_EVENT_TYPE),
            I18nUtil.getMessage(MessageKeys.SSE_COLUMN_RETRY),
            I18nUtil.getMessage(MessageKeys.WEBSOCKET_COLUMN_CONTENT),
            I18nUtil.getMessage(MessageKeys.FUNCTIONAL_TABLE_ASSERTION)
    };

    private static final String[] TYPE_FILTERS = {
            I18nUtil.getMessage(MessageKeys.WEBSOCKET_TYPE_ALL),
            I18nUtil.getMessage(MessageKeys.WEBSOCKET_TYPE_CONNECTED),
            I18nUtil.getMessage(MessageKeys.WEBSOCKET_TYPE_RECEIVED),
            I18nUtil.getMessage(MessageKeys.WEBSOCKET_TYPE_CLOSED),
            I18nUtil.getMessage(MessageKeys.WEBSOCKET_TYPE_WARNING),
            I18nUtil.getMessage(MessageKeys.WEBSOCKET_TYPE_INFO)
    };

    public SSEResponsePanel() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        JPanel toolBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 2));
        typeFilterBox = new JComboBox<>(TYPE_FILTERS);
        searchField = new SearchTextField();
        clearButton = new ClearButton();
        toolBar.add(typeFilterBox);
        toolBar.add(searchField);
        toolBar.add(clearButton);
        add(toolBar, BorderLayout.NORTH);

        tableModel = new DefaultTableModel(COLUMN_NAMES, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        table = new JTable(tableModel);
        table.setRowHeight(26);
        table.setCellSelectionEnabled(true);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        table.getColumnModel().getColumn(COLUMN_TYPE).setMinWidth(60);
        table.getColumnModel().getColumn(COLUMN_TYPE).setPreferredWidth(70);
        table.getColumnModel().getColumn(COLUMN_TYPE).setMaxWidth(85);
        table.getColumnModel().getColumn(COLUMN_TYPE).setCellRenderer(new IconCellRenderer());

        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        table.getColumnModel().getColumn(COLUMN_TIME).setMinWidth(90);
        table.getColumnModel().getColumn(COLUMN_TIME).setPreferredWidth(110);
        table.getColumnModel().getColumn(COLUMN_TIME).setMaxWidth(150);
        table.getColumnModel().getColumn(COLUMN_TIME).setCellRenderer(centerRenderer);

        table.getColumnModel().getColumn(COLUMN_EVENT_ID).setMinWidth(100);
        table.getColumnModel().getColumn(COLUMN_EVENT_ID).setPreferredWidth(120);
        table.getColumnModel().getColumn(COLUMN_EVENT_ID).setMaxWidth(180);

        table.getColumnModel().getColumn(COLUMN_EVENT_TYPE).setMinWidth(90);
        table.getColumnModel().getColumn(COLUMN_EVENT_TYPE).setPreferredWidth(110);
        table.getColumnModel().getColumn(COLUMN_EVENT_TYPE).setMaxWidth(160);

        table.getColumnModel().getColumn(COLUMN_RETRY).setMinWidth(90);
        table.getColumnModel().getColumn(COLUMN_RETRY).setPreferredWidth(100);
        table.getColumnModel().getColumn(COLUMN_RETRY).setMaxWidth(120);
        table.getColumnModel().getColumn(COLUMN_RETRY).setCellRenderer(centerRenderer);

        table.getColumnModel().getColumn(COLUMN_CONTENT).setMinWidth(150);
        table.getColumnModel().getColumn(COLUMN_CONTENT).setPreferredWidth(360);

        table.getColumnModel().getColumn(COLUMN_ASSERTION).setMinWidth(90);
        table.getColumnModel().getColumn(COLUMN_ASSERTION).setPreferredWidth(100);
        table.getColumnModel().getColumn(COLUMN_ASSERTION).setMaxWidth(120);
        table.getColumnModel().getColumn(COLUMN_ASSERTION).setCellRenderer(new IconCellRenderer());

        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                maybeShowPopup(e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                maybeShowPopup(e);
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() != 2) {
                    return;
                }
                int row = table.rowAtPoint(e.getPoint());
                int col = table.columnAtPoint(e.getPoint());
                MessageRow messageRow = getVisibleRow(row);
                if (messageRow == null) {
                    return;
                }
                if (col >= COLUMN_EVENT_ID && col <= COLUMN_CONTENT) {
                    showContentDialog(messageRow);
                } else if (col == COLUMN_ASSERTION) {
                    showAssertionDialog(messageRow.testResults);
                }
            }

            private void maybeShowPopup(MouseEvent e) {
                if (!e.isPopupTrigger()) {
                    return;
                }
                int row = table.rowAtPoint(e.getPoint());
                int col = table.columnAtPoint(e.getPoint());
                if (row < 0 || col < COLUMN_EVENT_ID || col > COLUMN_CONTENT) {
                    return;
                }
                MessageRow messageRow = getVisibleRow(row);
                if (messageRow == null) {
                    return;
                }
                table.setRowSelectionInterval(row, row);
                JPopupMenu popupMenu = new JPopupMenu();
                JMenuItem copyItem = new JMenuItem(I18nUtil.getMessage(MessageKeys.BUTTON_COPY));
                JMenuItem detailItem = new JMenuItem(I18nUtil.getMessage(MessageKeys.BUTTON_DETAIL));
                copyItem.addActionListener(ev -> {
                    StringSelection selection = new StringSelection(buildDetailContent(messageRow));
                    Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, null);
                });
                detailItem.addActionListener(ev -> showContentDialog(messageRow));
                popupMenu.add(copyItem);
                popupMenu.add(detailItem);
                popupMenu.show(e.getComponent(), e.getX(), e.getY());
            }
        });

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createEmptyBorder(5, 0, 0, 0));
        add(scrollPane, BorderLayout.CENTER);

        JTableHeader header = table.getTableHeader();
        header.setFont(header.getFont().deriveFont(Font.BOLD));
        table.setGridColor(new Color(220, 225, 235));
        table.setShowGrid(true);
        table.setRowHeight(24);
        table.setBorder(BorderFactory.createLineBorder(new Color(200, 210, 230), 1));
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                           boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                c.setFont(c.getFont().deriveFont(isSelected ? Font.BOLD : Font.PLAIN));
                return c;
            }
        });

        clearButton.addActionListener(e -> clearMessages());
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                filterAndShow();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                filterAndShow();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                filterAndShow();
            }
        });
        typeFilterBox.addActionListener(e -> filterAndShow());
    }

    public void addMessage(MessageType messageType, String time, String content, List<TestResult> testResults) {
        addMessage(messageType, time, null, null, null, content, testResults);
    }

    public void addMessage(MessageType messageType, String time, String eventId, String eventType, Long retryMs,
                           String content, List<TestResult> testResults) {
        allRows.add(new MessageRow(messageType, time, eventId, eventType, retryMs, content, testResults));
        SwingUtilities.invokeLater(this::filterAndShow);
    }

    public void clearMessages() {
        allRows.clear();
        visibleRows.clear();
        SwingUtilities.invokeLater(this::filterAndShow);
    }

    private static Icon getSummaryIcon(List<TestResult> testResults) {
        if (CollUtil.isEmpty(testResults)) {
            return null;
        }
        boolean hasFail = testResults.stream().anyMatch(tr -> !tr.passed);
        return hasFail ? new FlatSVGIcon("icons/fail.svg", 16, 16) : new FlatSVGIcon("icons/pass.svg", 16, 16);
    }

    private void filterAndShow() {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(this::filterAndShow);
            return;
        }
        String search = searchField.getText().trim().toLowerCase();
        String typeFilter = (String) typeFilterBox.getSelectedItem();
        List<MessageRow> filtered = allRows.stream()
                .filter(row -> I18nUtil.getMessage(MessageKeys.WEBSOCKET_TYPE_ALL).equals(typeFilter)
                        || row.messageType.display.equals(typeFilter))
                .filter(row -> search.isEmpty()
                        || safeLower(row.content).contains(search)
                        || safeLower(row.eventId).contains(search)
                        || safeLower(row.eventType).contains(search))
                .toList();

        visibleRows.clear();
        visibleRows.addAll(filtered);
        tableModel.setRowCount(0);
        for (MessageRow row : filtered) {
            tableModel.addRow(new Object[]{
                    row.messageType.icon,
                    row.time,
                    blankToDash(row.eventId),
                    blankToDash(row.eventType),
                    row.retryMs != null ? row.retryMs : blankToDash(null),
                    row.content,
                    getSummaryIcon(row.testResults)
            });
        }
        searchField.setNoResult(!search.isEmpty() && filtered.isEmpty());
    }

    private MessageRow getVisibleRow(int row) {
        if (row < 0 || row >= visibleRows.size()) {
            return null;
        }
        return visibleRows.get(row);
    }

    public static class MessageRow {
        public final String time;
        public final String eventId;
        public final String eventType;
        public final Long retryMs;
        public final String content;
        public final List<TestResult> testResults;
        public final MessageType messageType;

        public MessageRow(MessageType messageType, String time, String eventId, String eventType, Long retryMs,
                          String content, List<TestResult> testResults) {
            this.messageType = messageType;
            this.time = time;
            this.eventId = eventId;
            this.eventType = eventType;
            this.retryMs = retryMs;
            this.content = content;
            this.testResults = testResults;
        }
    }

    private static class IconCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus, int row, int column) {
            if (value instanceof Icon icon) {
                setIcon(icon);
            } else {
                setIcon(null);
            }
            setText("");
            setHorizontalAlignment(CENTER);
            return this;
        }
    }

    private void showContentDialog(MessageRow row) {
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(this),
                I18nUtil.getMessage(MessageKeys.SSE_DETAIL_TITLE), Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setSize(600, 400);
        dialog.setLocationRelativeTo(this);

        JPanel panel = new JPanel(new BorderLayout(5, 5));
        JTextArea textArea = new JTextArea();
        textArea.setEditable(false);
        textArea.setLineWrap(true);
        JScrollPane scrollPane = new JScrollPane(textArea);
        panel.add(scrollPane, BorderLayout.CENTER);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton copyBtn = new JButton(I18nUtil.getMessage(MessageKeys.BUTTON_COPY));
        JButton formatBtn = new JButton(I18nUtil.getMessage(MessageKeys.BUTTON_FORMAT));
        JButton rawBtn = new JButton(I18nUtil.getMessage(MessageKeys.BUTTON_RAW));
        JButton cancelBtn = new JButton(I18nUtil.getMessage(MessageKeys.BUTTON_CANCEL));
        btnPanel.add(copyBtn);
        btnPanel.add(formatBtn);
        btnPanel.add(rawBtn);
        btnPanel.add(cancelBtn);
        panel.add(btnPanel, BorderLayout.SOUTH);
        dialog.setContentPane(panel);

        String rawContent = buildDetailContent(row);
        textArea.setText(rawContent);

        boolean isJson = JSONUtil.isTypeJSON(row.content);
        formatBtn.setEnabled(isJson);
        rawBtn.setEnabled(false);
        formatBtn.addActionListener(e -> {
            textArea.setText(buildFormattedDetailContent(row));
            formatBtn.setEnabled(false);
            rawBtn.setEnabled(true);
        });
        rawBtn.addActionListener(e -> {
            textArea.setText(rawContent);
            formatBtn.setEnabled(isJson);
            rawBtn.setEnabled(false);
        });
        copyBtn.addActionListener(e -> {
            textArea.selectAll();
            textArea.copy();
        });
        cancelBtn.addActionListener(e -> dialog.dispose());

        dialog.getRootPane().registerKeyboardAction(e -> dialog.dispose(),
                KeyStroke.getKeyStroke("ESCAPE"),
                JComponent.WHEN_IN_FOCUSED_WINDOW);
        dialog.getRootPane().setDefaultButton(cancelBtn);
        cancelBtn.requestFocusInWindow();
        dialog.setVisible(true);
    }

    private void showAssertionDialog(List<TestResult> testResults) {
        if (CollUtil.isEmpty(testResults)) {
            return;
        }
        String html = HttpHtmlRenderer.renderTestResults(testResults);
        JEditorPane editorPane = new JEditorPane();
        editorPane.setContentType("text/html");
        editorPane.setText(html);
        editorPane.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(editorPane);
        scrollPane.setPreferredSize(new Dimension(600, 400));
        JOptionPane.showMessageDialog(SingletonFactory.getInstance(MainFrame.class),
                scrollPane, I18nUtil.getMessage(MessageKeys.FUNCTIONAL_TABLE_ASSERTION), JOptionPane.PLAIN_MESSAGE);
    }

    private String buildDetailContent(MessageRow row) {
        String none = I18nUtil.getMessage(MessageKeys.SSE_VALUE_NONE);
        return I18nUtil.getMessage(MessageKeys.SSE_DETAIL_TYPE) + ": " + row.messageType.display + "\n"
                + I18nUtil.getMessage(MessageKeys.SSE_DETAIL_TIME) + ": " + blankToDash(row.time) + "\n"
                + I18nUtil.getMessage(MessageKeys.SSE_DETAIL_EVENT_ID) + ": " + blankToDash(row.eventId) + "\n"
                + I18nUtil.getMessage(MessageKeys.SSE_DETAIL_EVENT_TYPE) + ": " + blankToDash(row.eventType) + "\n"
                + I18nUtil.getMessage(MessageKeys.SSE_DETAIL_RETRY) + ": "
                + (row.retryMs != null ? row.retryMs : none) + "\n\n"
                + I18nUtil.getMessage(MessageKeys.SSE_DETAIL_CONTENT) + ":\n"
                + (row.content == null ? none : row.content);
    }

    private String buildFormattedDetailContent(MessageRow row) {
        String formattedContent = row.content == null ? null : formatJson(row.content);
        MessageRow formatted = new MessageRow(row.messageType, row.time, row.eventId, row.eventType,
                row.retryMs, formattedContent, row.testResults);
        return buildDetailContent(formatted);
    }

    private String formatJson(String str) {
        if (JSONUtil.isTypeJSON(str)) {
            return JsonUtil.toJsonPrettyStr(str);
        }
        return str;
    }

    private String safeLower(String value) {
        return value == null ? "" : value.toLowerCase();
    }

    private String blankToDash(String value) {
        return value == null || value.isBlank() ? I18nUtil.getMessage(MessageKeys.SSE_VALUE_NONE) : value;
    }
}
