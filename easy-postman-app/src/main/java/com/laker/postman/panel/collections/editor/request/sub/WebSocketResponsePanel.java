package com.laker.postman.panel.collections.editor.request.sub;

import cn.hutool.json.JSONUtil;
import com.laker.postman.common.component.SearchTextField;
import com.laker.postman.common.component.ToolWindowSurfaceStyle;
import com.laker.postman.common.component.button.ClearButton;
import com.laker.postman.common.constants.ModernColors;
import com.laker.postman.stream.MessageType;
import com.laker.postman.panel.collections.editor.request.StreamMessageUiMetadata;
import com.laker.postman.script.model.TestResult;
import com.laker.postman.util.FontsUtil;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.JsonUtil;
import com.laker.postman.util.MessageKeys;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * WebSocket响应体面板，展示消息类型、时间、内容和断言结果，支持搜索、清除、类型过滤。
 */
public class WebSocketResponsePanel extends JPanel {
    private final JTable table;
    private final StreamMessageTableModel<MessageRow> tableModel;
    private final JComboBox<String> typeFilterBox;
    private final SearchTextField searchField;
    private final ClearButton clearButton;
    private final JLabel retentionLabel;
    private final StreamMessageLogBuffer<MessageRow> logBuffer;
    private final ConcurrentLinkedQueue<MessageRow> pendingRows = new ConcurrentLinkedQueue<>();
    private final AtomicBoolean pendingFlushQueued = new AtomicBoolean();
    private final JScrollPane tableScrollPane;
    private final JSplitPane assertionSplitPane;
    private final StreamAssertionDetailsPanel assertionDetailsPanel;
    private boolean refreshQueued;

    private static final int COLUMN_TYPE = 0;
    private static final int COLUMN_TIME = 1;
    private static final int COLUMN_CONTENT = 2;
    private static final int COLUMN_ASSERTION = 3;

    private static final String[] COLUMN_NAMES = {
            I18nUtil.getMessage(MessageKeys.WEBSOCKET_COLUMN_TYPE),
            I18nUtil.getMessage(MessageKeys.WEBSOCKET_COLUMN_TIME),
            I18nUtil.getMessage(MessageKeys.WEBSOCKET_COLUMN_CONTENT),
            I18nUtil.getMessage(MessageKeys.FUNCTIONAL_TABLE_ASSERTION)
    };
    private static final String[] TYPE_FILTERS = {
            I18nUtil.getMessage(MessageKeys.WEBSOCKET_TYPE_ALL),
            I18nUtil.getMessage(MessageKeys.WEBSOCKET_TYPE_SENT),
            I18nUtil.getMessage(MessageKeys.WEBSOCKET_TYPE_RECEIVED),
            I18nUtil.getMessage(MessageKeys.WEBSOCKET_TYPE_CONNECTED),
            I18nUtil.getMessage(MessageKeys.WEBSOCKET_TYPE_CLOSED),
            I18nUtil.getMessage(MessageKeys.WEBSOCKET_TYPE_WARNING),
            I18nUtil.getMessage(MessageKeys.WEBSOCKET_TYPE_INFO),
            I18nUtil.getMessage(MessageKeys.WEBSOCKET_TYPE_BINARY)
    };

    public WebSocketResponsePanel() {
        this(StreamMessageLogBuffer.DEFAULT_MAX_ROWS);
    }

    WebSocketResponsePanel(int maxRows) {
        logBuffer = new StreamMessageLogBuffer<>(maxRows);
        setLayout(new BorderLayout());
        ToolWindowSurfaceStyle.applyCard(this);
        setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        // 顶部工具栏
        JPanel toolBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 2)); // 左对齐，水平间距5，垂直间距2
        ToolWindowSurfaceStyle.applyCard(toolBar);
        typeFilterBox = new JComboBox<>(TYPE_FILTERS);
        searchField = new SearchTextField();
        clearButton = new ClearButton();
        retentionLabel = new JLabel();
        retentionLabel.setFont(FontsUtil.getDefaultFontWithOffset(Font.PLAIN, -1));
        retentionLabel.setForeground(ModernColors.getTextSecondary());
        retentionLabel.setVisible(false);
        toolBar.add(typeFilterBox);
        toolBar.add(searchField);
        toolBar.add(clearButton);
        toolBar.add(retentionLabel);
        add(toolBar, BorderLayout.NORTH);

        // 表格
        tableModel = new StreamMessageTableModel<>(COLUMN_NAMES, this::messageValueAt);
        table = new JTable(tableModel);
        table.setRowHeight(26);
        // Type 列（类型图标）：显示 "Type"（4个字符）+ 图标
        table.getColumnModel().getColumn(COLUMN_TYPE).setMinWidth(100);
        table.getColumnModel().getColumn(COLUMN_TYPE).setPreferredWidth(118);
        table.getColumnModel().getColumn(COLUMN_TYPE).setMaxWidth(150);
        table.getColumnModel().getColumn(COLUMN_TYPE).setCellRenderer(new StreamMessageTypeCellRenderer());
        // Time 列（时间）：显示 "Time"（4个字符）+ 时间戳
        table.getColumnModel().getColumn(COLUMN_TIME).setMinWidth(90);
        table.getColumnModel().getColumn(COLUMN_TIME).setPreferredWidth(110);
        table.getColumnModel().getColumn(COLUMN_TIME).setMaxWidth(150);
        // 设置 Time 列居中
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        table.getColumnModel().getColumn(COLUMN_TIME).setCellRenderer(centerRenderer);
        // Content 列（内容）：显示 "Content"（7个字符）+ 消息内容，可自由调整
        table.getColumnModel().getColumn(COLUMN_CONTENT).setMinWidth(150);
        table.getColumnModel().getColumn(COLUMN_CONTENT).setPreferredWidth(400);
        // Assertion 列（断言结果图标）：显示 "Assertion"（9个字符）+ 图标
        table.getColumnModel().getColumn(COLUMN_ASSERTION).setMinWidth(90);
        table.getColumnModel().getColumn(COLUMN_ASSERTION).setPreferredWidth(100);
        table.getColumnModel().getColumn(COLUMN_ASSERTION).setMaxWidth(120);
        table.getColumnModel().getColumn(COLUMN_ASSERTION).setCellRenderer(new StreamAssertionSummaryCellRenderer());
        table.setCellSelectionEnabled(true);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        // 鼠标监听：内容列右键菜单，断言列单击查看结果，内容列双击查看详情。
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
            public void mouseExited(MouseEvent e) {
                table.setCursor(Cursor.getDefaultCursor());
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                int row = table.rowAtPoint(e.getPoint());
                int viewCol = table.columnAtPoint(e.getPoint());
                if (row < 0 || viewCol < 0) {
                    return;
                }
                int col = table.convertColumnIndexToModel(viewCol);
                MessageRow messageRow = getVisibleRow(row);
                if (messageRow == null) {
                    return;
                }
                if (col == COLUMN_ASSERTION && e.getClickCount() == 1) {
                    if (!(table.getValueAt(row, viewCol) instanceof StreamAssertionSummary)) {
                        return;
                    }
                    table.setRowSelectionInterval(row, row);
                    table.setColumnSelectionInterval(viewCol, viewCol);
                    showAssertionDetails(messageRow);
                } else if (col == COLUMN_CONTENT && e.getClickCount() == 2) {
                    showContentDialog(messageRow.content);
                }
            }

            private void maybeShowPopup(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    int row = table.rowAtPoint(e.getPoint());
                    int viewCol = table.columnAtPoint(e.getPoint());
                    if (row < 0 || viewCol < 0) {
                        return;
                    }
                    int col = table.convertColumnIndexToModel(viewCol);
                    if (col == COLUMN_CONTENT) {
                        MessageRow messageRow = getVisibleRow(row);
                        if (messageRow == null) {
                            return;
                        }
                        table.setRowSelectionInterval(row, row);
                        String content = messageRow.content;
                        JPopupMenu popupMenu = new JPopupMenu();
                        ToolWindowSurfaceStyle.applyPopupMenuCard(popupMenu);
                        JMenuItem copyItem = new JMenuItem(I18nUtil.getMessage(MessageKeys.BUTTON_COPY));
                        JMenuItem detailItem = new JMenuItem(I18nUtil.getMessage(MessageKeys.BUTTON_DETAIL));
                        copyItem.addActionListener(ev -> {
                            // 复制内容到剪贴板
                            StringSelection selection = new StringSelection(content);
                            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, null);
                        });
                        detailItem.addActionListener(ev -> showContentDialog(content));
                        popupMenu.add(copyItem);
                        popupMenu.add(detailItem);
                        popupMenu.show(e.getComponent(), e.getX(), e.getY());
                    }
                }
            }
        });
        table.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                updateTableCursor(e.getPoint());
            }
        });
        tableScrollPane = new JScrollPane(table);
        ToolWindowSurfaceStyle.applyTableScrollPaneCard(tableScrollPane, table);
        tableScrollPane.setBorder(BorderFactory.createEmptyBorder(5, 0, 0, 0));
        assertionDetailsPanel = new StreamAssertionDetailsPanel();
        assertionDetailsPanel.setVisibilityChangeListener(this::updateAssertionSplitPane);
        assertionSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, tableScrollPane, assertionDetailsPanel);
        assertionSplitPane.setResizeWeight(1.0);
        assertionSplitPane.setContinuousLayout(true);
        assertionSplitPane.setBorder(BorderFactory.createEmptyBorder());
        assertionSplitPane.setDividerSize(0);
        assertionSplitPane.setOpaque(false);
        add(assertionSplitPane, BorderLayout.CENTER);

        // 美化表格
        JTableHeader header = table.getTableHeader();
        header.setFont(header.getFont().deriveFont(Font.BOLD));
        table.setGridColor(ModernColors.getTableGridColor());
        table.setShowGrid(true);
        table.setRowHeight(24);
        table.setBorder(BorderFactory.createEmptyBorder());
        // 选中行加粗
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (isSelected) {
                    c.setFont(c.getFont().deriveFont(Font.BOLD));
                } else {
                    c.setFont(c.getFont().deriveFont(Font.PLAIN));
                }
                return c;
            }
        });

        // 事件
        clearButton.addActionListener(e -> clearMessages());
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) {
                requestFilterAndShow();
            }

            public void removeUpdate(DocumentEvent e) {
                requestFilterAndShow();
            }

            public void changedUpdate(DocumentEvent e) {
                requestFilterAndShow();
            }
        });
        typeFilterBox.addActionListener(e -> requestFilterAndShow());
    }

    public void addMessage(MessageType type, String time, String content, List<TestResult> testResults) {
        MessageRow row = new MessageRow(type, time, content, testResults);
        pendingRows.add(row);
        requestPendingRowsFlush();
    }

    private void requestPendingRowsFlush() {
        if (SwingUtilities.isEventDispatchThread()) {
            flushPendingRows();
            return;
        }
        if (pendingFlushQueued.compareAndSet(false, true)) {
            SwingUtilities.invokeLater(this::flushPendingRows);
        }
    }

    private void flushPendingRows() {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(this::flushPendingRows);
            return;
        }
        List<MessageRow> rowsToAppend = new ArrayList<>();
        MessageRow row;
        while ((row = pendingRows.poll()) != null) {
            rowsToAppend.add(row);
        }
        pendingFlushQueued.set(false);
        if (rowsToAppend.isEmpty()) {
            return;
        }
        appendRows(rowsToAppend);
        if (!pendingRows.isEmpty() && pendingFlushQueued.compareAndSet(false, true)) {
            SwingUtilities.invokeLater(this::flushPendingRows);
        }
    }

    private void appendRows(List<MessageRow> rows) {
        boolean shouldScrollToBottom = isScrolledNearBottom();
        String search = currentSearchText();
        String typeFilter = currentTypeFilter();
        List<MessageRow> droppedRows = logBuffer.appendAndTrim(rows);
        if (droppedRows.isEmpty()) {
            List<MessageRow> visibleRowsToAppend = rows.stream()
                    .filter(row -> matchesFilter(row, search, typeFilter))
                    .toList();
            tableModel.appendRows(visibleRowsToAppend);
        } else {
            tableModel.setRows(logBuffer.filtered(row -> matchesFilter(row, search, typeFilter)));
        }
        searchField.setNoResult(!search.isEmpty() && tableModel.getRowCount() == 0);
        updateRetentionLabel();
        if (shouldScrollToBottom && tableModel.getRowCount() > 0) {
            SwingUtilities.invokeLater(this::scrollToBottom);
        }
    }

    public void clearMessages() {
        runOnEdt(() -> {
            pendingRows.clear();
            logBuffer.clear();
            tableModel.clear();
            assertionDetailsPanel.hideDetails();
            updateAssertionSplitPane();
            searchField.setNoResult(false);
            updateRetentionLabel();
        });
    }

    private void filterAndShow() {
        // 确保在 EDT 内执行
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(this::filterAndShow);
            return;
        }
        String search = currentSearchText();
        String typeFilter = currentTypeFilter();
        boolean shouldScrollToBottom = isScrolledNearBottom();
        List<MessageRow> filtered = logBuffer.filtered(row -> matchesFilter(row, search, typeFilter));
        tableModel.setRows(filtered);
        searchField.setNoResult(!search.isEmpty() && tableModel.getRowCount() == 0);
        updateRetentionLabel();
        if (shouldScrollToBottom && tableModel.getRowCount() > 0) {
            SwingUtilities.invokeLater(this::scrollToBottom);
        }
    }

    private Object messageValueAt(MessageRow row, int column) {
        return switch (column) {
            case COLUMN_TYPE -> row.type;
            case COLUMN_TIME -> row.time;
            case COLUMN_CONTENT -> row.content;
            case COLUMN_ASSERTION -> StreamAssertionSummary.from(row.testResults);
            default -> null;
        };
    }

    private boolean matchesFilter(MessageRow row, String search, String typeFilter) {
        return (I18nUtil.getMessage(MessageKeys.WEBSOCKET_TYPE_ALL).equals(typeFilter)
                || StreamMessageUiMetadata.display(row.type).equals(typeFilter))
                && (search.isEmpty() || safeLower(row.content).contains(search));
    }

    private String currentSearchText() {
        return searchField.getText().trim().toLowerCase();
    }

    private String currentTypeFilter() {
        return (String) typeFilterBox.getSelectedItem();
    }

    private void requestFilterAndShow() {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(this::requestFilterAndShow);
            return;
        }
        if (refreshQueued) {
            return;
        }
        refreshQueued = true;
        SwingUtilities.invokeLater(() -> {
            refreshQueued = false;
            filterAndShow();
        });
    }

    private void runOnEdt(Runnable task) {
        if (SwingUtilities.isEventDispatchThread()) {
            task.run();
        } else {
            SwingUtilities.invokeLater(task);
        }
    }

    private boolean isScrolledNearBottom() {
        JScrollBar verticalBar = tableScrollPane.getVerticalScrollBar();
        int bottom = verticalBar.getValue() + verticalBar.getVisibleAmount();
        return bottom >= verticalBar.getMaximum() - Math.max(table.getRowHeight() * 2, 24);
    }

    private void scrollToBottom() {
        int lastRow = tableModel.getRowCount() - 1;
        if (lastRow < 0) {
            return;
        }
        Rectangle rect = table.getCellRect(lastRow, COLUMN_TYPE, true);
        table.scrollRectToVisible(rect);
        JScrollBar verticalBar = tableScrollPane.getVerticalScrollBar();
        verticalBar.setValue(verticalBar.getMaximum());
    }

    private static String safeLower(String value) {
        return value == null ? "" : value.toLowerCase();
    }

    MessageRow getVisibleRow(int viewRow) {
        if (viewRow < 0) {
            return null;
        }
        int modelRow = table.convertRowIndexToModel(viewRow);
        if (modelRow >= tableModel.getRowCount()) {
            return null;
        }
        return tableModel.getRow(modelRow);
    }

    JTable getTable() {
        return table;
    }

    SearchTextField getSearchField() {
        return searchField;
    }

    void showAssertionDetails(MessageRow row) {
        assertionDetailsPanel.showResults(
                row.testResults,
                StreamAssertionSummary.from(row.testResults),
                StreamMessageUiMetadata.display(row.type),
                row.time
        );
        updateAssertionSplitPane();
    }

    StreamAssertionDetailsPanel getAssertionDetailsPanel() {
        return assertionDetailsPanel;
    }

    JSplitPane getAssertionSplitPane() {
        return assertionSplitPane;
    }

    JScrollPane getTableScrollPane() {
        return tableScrollPane;
    }

    // 行数据
    public static class MessageRow {
        public final MessageType type;
        public final String time;
        public final String content;
        public final List<TestResult> testResults;

        public MessageRow(MessageType type, String time, String content, List<TestResult> testResults) {
            this.type = type;
            this.time = time;
            this.content = content;
            this.testResults = testResults;
        }
    }

    private void updateTableCursor(Point point) {
        int row = table.rowAtPoint(point);
        int col = table.columnAtPoint(point);
        boolean clickableAssertion = row >= 0 && col >= 0
                && table.convertColumnIndexToModel(col) == COLUMN_ASSERTION
                && table.getValueAt(row, col) instanceof StreamAssertionSummary;
        table.setCursor(Cursor.getPredefinedCursor(clickableAssertion ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
    }

    private void updateRetentionLabel() {
        long droppedCount = logBuffer.droppedCount();
        boolean visible = droppedCount > 0;
        boolean visibilityChanged = retentionLabel.isVisible() != visible;
        retentionLabel.setVisible(visible);
        if (droppedCount > 0) {
            retentionLabel.setText(I18nUtil.getMessage(MessageKeys.STREAM_LOG_DROPPED_COUNT, droppedCount));
        } else {
            retentionLabel.setText("");
        }
        if (visibilityChanged && retentionLabel.getParent() != null) {
            retentionLabel.getParent().revalidate();
            retentionLabel.getParent().repaint();
        }
    }

    private void updateAssertionSplitPane() {
        if (assertionDetailsPanel.isVisible()) {
            assertionSplitPane.setDividerSize(6);
            SwingUtilities.invokeLater(() -> {
                int height = assertionSplitPane.getHeight();
                if (height <= 0) {
                    return;
                }
                int targetLocation = Math.max(120, height - assertionDetailsPanel.getPreferredSize().height);
                if (assertionSplitPane.getDividerLocation() > targetLocation || assertionSplitPane.getDividerLocation() <= 0) {
                    assertionSplitPane.setDividerLocation(targetLocation);
                }
            });
        } else {
            assertionSplitPane.setDividerSize(0);
            assertionSplitPane.setDividerLocation(1.0);
        }
        assertionSplitPane.revalidate();
        assertionSplitPane.repaint();
    }

    private void showContentDialog(String content) {
        boolean isJson = JSONUtil.isTypeJSON(content);
        StreamMessageContentDialog.show(
                this,
                I18nUtil.getMessage(MessageKeys.WEBSOCKET_DIALOG_TITLE),
                content,
                isJson,
                () -> formatJson(content)
        );
    }


    // 简单格式化JSON
    private String formatJson(String str) {
        if (JSONUtil.isTypeJSON(str)) {
            return JsonUtil.toJsonPrettyStr(str);
        }
        return str;

    }
}
