package com.laker.postman.common.component.table;

import com.laker.postman.model.EnvironmentVariable;
import com.laker.postman.util.FontsUtil;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * Postman 环境变量表格面板
 * 专门用于处理 Postman 环境变量数据
 * 支持 Drag+Enable、Key、Value 和 Delete 列结构
 */
@Slf4j
public class EasyPostmanEnvironmentTablePanel extends AbstractEasyPostmanTablePanel<EnvironmentVariable> {

    // Column indices - 此表格有特殊的 Drag+Enable 合并列
    private static final int COL_DRAG_ENABLE = 0;  // 合并拖动手柄和启用复选框
    private static final int COL_KEY = 1;
    private static final int COL_VALUE = 2;
    private static final int COL_DELETE = 3;

    /**
     * Flag to track if dragging is in progress
     */
    private boolean isDragging = false;

    /**
     * Flag to suppress auto-append during drag operations
     */
    private boolean suppressAutoDuringDrag = false;

    /**
     * 构造函数，创建默认的环境变量表格面板
     */
    public EasyPostmanEnvironmentTablePanel() {
        this("Name", "Value", true, true);
    }

    /**
     * 构造函数，支持自定义配置
     *
     * @param nameCol              Key列名称
     * @param valueCol             Value列名称
     * @param popupMenuEnabled     是否启用右键菜单
     * @param autoAppendRowEnabled 是否启用自动补空行
     */
    public EasyPostmanEnvironmentTablePanel(String nameCol, String valueCol, boolean popupMenuEnabled, boolean autoAppendRowEnabled) {
        super(new String[]{"", nameCol, valueCol, ""});
        initializeComponents();
        initializeTableUI();
        setupCellRenderersAndEditors();
        if (popupMenuEnabled) {
            setupTableListeners();
        }
        if (autoAppendRowEnabled) {
            addAutoAppendRowFeature();
        }

        enableRowDragAndDrop();

        // Add initial empty row
        addRow();
    }

    // ========== 实现抽象方法 ==========

    @Override
    protected int getEnabledColumnIndex() {
        return COL_DRAG_ENABLE;  // Drag+Enable 合并列
    }

    @Override
    protected int getDeleteColumnIndex() {
        return COL_DELETE;
    }

    @Override
    protected int getFirstEditableColumnIndex() {
        return COL_KEY;
    }

    @Override
    protected int getLastEditableColumnIndex() {
        return COL_VALUE;
    }

    @Override
    protected boolean isCellEditableForNavigation(int row, int column) {
        return column == COL_KEY || column == COL_VALUE;
    }

    @Override
    protected boolean hasContentInRow(int row) {
        String key = getStringValue(row, COL_KEY);
        String value = getStringValue(row, COL_VALUE);
        return !key.isEmpty() || !value.isEmpty();
    }

    // ========== 拖拽功能 ==========

    /**
     * 启用JTable行拖动排序
     */
    private void enableRowDragAndDrop() {
        table.setDragEnabled(true);
        table.setDropMode(DropMode.INSERT_ROWS);

        // 传递回调函数，在拖拽期间控制自动补空行和编辑状态
        table.setTransferHandler(new ImprovedTableRowTransferHandler(tableModel, dragging -> {
            suppressAutoDuringDrag = dragging;
            isDragging = dragging;
            log.debug("Drag state changed: {}", dragging);
        }));
    }

    private void initializeComponents() {
        setLayout(new BorderLayout());
        setBackground(new Color(248, 250, 252));
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(237, 237, 237)),
                BorderFactory.createEmptyBorder(4, 4, 4, 4)));

        // Initialize table model with custom cell editing logic
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public Class<?> getColumnClass(int columnIndex) {
                if (columnIndex == COL_DRAG_ENABLE) {
                    return Boolean.class;
                }
                return Object.class;
            }

            @Override
            public boolean isCellEditable(int row, int column) {
                if (!editable || isDragging) {
                    return false;
                }

                // Drag/Enable column - checkbox part is editable
                if (column == COL_DRAG_ENABLE) {
                    return true;
                }

                // Delete column is not editable (uses custom renderer)
                if (column == COL_DELETE) {
                    return false;
                }

                // Key and Value columns are editable
                return column == COL_KEY || column == COL_VALUE;
            }
        };

        // Create table
        table = new JTable(tableModel);

        // Wrap table in JScrollPane to show headers
        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        add(scrollPane, BorderLayout.CENTER);
    }

    @Override
    protected void initializeTableUI() {
        // 调用父类的通用UI配置
        super.initializeTableUI();

        // 设置 Drag+Enable 合并列的宽度
        setEnabledColumnWidth(40);
        setDeleteColumnWidth(40);

        // Setup Tab key navigation to move between columns in the same row
        setupTabKeyNavigation();
    }

    private void setupCellRenderersAndEditors() {
        // Set custom renderer and editor for drag/enable column
        setColumnRenderer(COL_DRAG_ENABLE, new DragEnableRenderer());
        setColumnEditor(COL_DRAG_ENABLE, new DragEnableEditor());

        // Set editors for Key and Value columns
        setColumnEditor(COL_KEY, new EasyPostmanTextFieldCellEditor());
        setColumnEditor(COL_VALUE, new EasyPostmanTextFieldCellEditor());
        setColumnRenderer(COL_KEY, new EasyPostmanTextFieldCellRenderer());
        setColumnRenderer(COL_VALUE, new EasyPostmanTextFieldCellRenderer());

        // Set custom renderer for delete column
        setColumnRenderer(COL_DELETE, new DeleteButtonRenderer());
    }

    /**
     * Custom renderer for drag/enable column that combines drag handle and checkbox
     */
    private class DragEnableRenderer extends JPanel implements TableCellRenderer {
        private final JLabel dragLabel;
        private final JCheckBox checkBox;

        public DragEnableRenderer() {
            setLayout(new BorderLayout(0, 0)); // 减少间距从 2 到 0
            setOpaque(true);

            // Drag handle label - 加粗的拖拽图标
            dragLabel = new JLabel("⋮⋮");
            dragLabel.setFont(FontsUtil.getDefaultFont(Font.BOLD, 16)); // 增大字号并加粗
            dragLabel.setForeground(new Color(100, 100, 100)); // 颜色稍深一点更明显
            dragLabel.setHorizontalAlignment(SwingConstants.CENTER);
            dragLabel.setPreferredSize(new Dimension(20, 28)); // 宽度从25减到20
            dragLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

            // Checkbox
            checkBox = new JCheckBox();
            checkBox.setHorizontalAlignment(SwingConstants.CENTER);
            checkBox.setOpaque(false);

            add(dragLabel, BorderLayout.WEST);
            add(checkBox, BorderLayout.CENTER);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus, int row, int column) {
            if (isSelected) {
                setBackground(table.getSelectionBackground());
                // 选中时拖拽手柄颜色更深
                dragLabel.setForeground(new Color(60, 60, 60));
            } else {
                setBackground(table.getBackground());
                dragLabel.setForeground(new Color(100, 100, 100));
            }

            // Set checkbox state
            checkBox.setSelected(value instanceof Boolean && (Boolean) value);

            return this;
        }
    }

    /**
     * Custom editor for drag/enable column
     */
    private class DragEnableEditor extends DefaultCellEditor {
        private final JPanel panel;
        private final JLabel dragLabel;
        private final JCheckBox checkBox;

        public DragEnableEditor() {
            super(new JCheckBox());

            panel = new JPanel(new BorderLayout(0, 0)); // 减少间距从 2 到 0
            panel.setOpaque(true);

            // Drag handle label - 加粗的拖拽图标
            dragLabel = new JLabel("⋮⋮");
            dragLabel.setFont(FontsUtil.getDefaultFont(Font.BOLD, 16)); // 增大字号并加粗
            dragLabel.setForeground(new Color(100, 100, 100)); // 颜色稍深一点更明显
            dragLabel.setHorizontalAlignment(SwingConstants.CENTER);
            dragLabel.setPreferredSize(new Dimension(20, 28)); // 宽度从25减到20
            dragLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

            // Checkbox
            checkBox = new JCheckBox();
            checkBox.setHorizontalAlignment(SwingConstants.CENTER);
            checkBox.setOpaque(false);

            // Add ActionListener to immediately commit checkbox changes
            checkBox.addActionListener(e -> {
                fireEditingStopped();
            });

            panel.add(dragLabel, BorderLayout.WEST);
            panel.add(checkBox, BorderLayout.CENTER);

            // Add mouse listener to handle clicks on drag area vs checkbox area
            panel.addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    Rectangle checkBoxBounds = checkBox.getBounds();
                    Point panelPoint = e.getPoint();

                    // If click is on checkbox area, toggle it
                    if (checkBoxBounds.contains(panelPoint)) {
                        checkBox.setSelected(!checkBox.isSelected());
                        fireEditingStopped();
                    }
                }
            });
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value,
                                                     boolean isSelected, int row, int column) {
            panel.setBackground(table.getSelectionBackground());
            checkBox.setSelected(value instanceof Boolean && (Boolean) value);
            dragLabel.setForeground(new Color(60, 60, 60));
            return panel;
        }

        @Override
        public Object getCellEditorValue() {
            return checkBox.isSelected();
        }
    }

    private void setupTableListeners() {
        addTableRightMouseListener();
        addDeleteButtonListener();
        addDragHandleMouseListener();
    }

    /**
     * Add mouse listener for delete button clicks
     */
    private void addDeleteButtonListener() {
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (!editable || !SwingUtilities.isLeftMouseButton(e)) {
                    return;
                }

                int column = table.columnAtPoint(e.getPoint());
                int row = table.rowAtPoint(e.getPoint());

                if (column == COL_DELETE && row >= 0) {
                    int modelRow = row;
                    if (table.getRowSorter() != null) {
                        modelRow = table.getRowSorter().convertRowIndexToModel(row);
                    }

                    if (modelRow < 0 || modelRow >= tableModel.getRowCount()) {
                        return;
                    }

                    int rowCount = tableModel.getRowCount();
                    if (modelRow == rowCount - 1 && rowCount == 1) {
                        return;
                    }

                    stopCellEditing();
                    tableModel.removeRow(modelRow);
                    ensureEmptyLastRow();
                }
            }
        });
    }

    /**
     * Add mouse listener to show hand cursor when hovering over drag handle area
     */
    private void addDragHandleMouseListener() {
        // 使用简单的鼠标监听器来显示光标
        table.addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                int column = table.columnAtPoint(e.getPoint());
                int row = table.rowAtPoint(e.getPoint());

                // 在第一列显示手型光标
                if (column == COL_DRAG_ENABLE && row >= 0) {
                    Rectangle cellRect = table.getCellRect(row, column, false);
                    int relativeX = e.getX() - cellRect.x;

                    // 左侧拖拽区域显示小手指光标，提示用户可以拖拽
                    if (relativeX < 25) {
                        table.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                    } else {
                        table.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                    }
                } else {
                    table.setCursor(Cursor.getDefaultCursor());
                }
            }
        });

        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseExited(MouseEvent e) {
                table.setCursor(Cursor.getDefaultCursor());
            }

            @Override
            public void mousePressed(MouseEvent e) {
                int column = table.columnAtPoint(e.getPoint());
                int row = table.rowAtPoint(e.getPoint());

                // 在第一列的拖拽手柄区域按下时，选中行并立即启动拖拽
                if (column == COL_DRAG_ENABLE && row >= 0) {
                    Rectangle cellRect = table.getCellRect(row, column, false);
                    int relativeX = e.getX() - cellRect.x;

                    // 左侧25px是拖拽区域
                    if (relativeX < 25) {
                        // 选中该行
                        table.setRowSelectionInterval(row, row);

                        // 停止编辑
                        if (table.isEditing()) {
                            table.getCellEditor().stopCellEditing();
                        }

                        // 标记为拖拽状态，防止意外编辑
                        isDragging = true;

                        // 立即触发拖拽操作
                        TransferHandler handler = table.getTransferHandler();
                        if (handler != null) {
                            handler.exportAsDrag(table, e, TransferHandler.MOVE);
                        }
                    }
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                // 释放鼠标后重置拖拽状态
                SwingUtilities.invokeLater(() -> isDragging = false);
            }
        });
    }

    /**
     * Set empty cell white background renderer
     */
    private void setEmptyCellWhiteBackgroundRenderer() {
        DefaultTableCellRenderer renderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                                                           boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

                if (!isSelected) {
                    c.setBackground(Color.WHITE);
                }

                return c;
            }
        };

        table.setDefaultRenderer(Object.class, renderer);
    }

    /**
     * Set column editor
     */
    public void setColumnEditor(int column, TableCellEditor editor) {
        if (column >= 0 && column < table.getColumnCount()) {
            table.getColumnModel().getColumn(column).setCellEditor(editor);
        }
    }

    /**
     * Set column renderer
     */
    public void setColumnRenderer(int column, TableCellRenderer renderer) {
        if (column >= 0 && column < table.getColumnCount()) {
            table.getColumnModel().getColumn(column).setCellRenderer(renderer);
        }
    }

    /**
     * Add right-click menu listener
     */
    private void addTableRightMouseListener() {
        MouseAdapter tableMouseListener = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    showPopupMenu(e);
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    showPopupMenu(e);
                }
            }

            private void showPopupMenu(MouseEvent e) {
                if (!editable) {
                    return;
                }

                int viewRow = table.rowAtPoint(e.getPoint());
                if (viewRow >= 0) {
                    table.setRowSelectionInterval(viewRow, viewRow);
                }

                JPopupMenu contextMenu = createContextPopupMenu();
                contextMenu.show(e.getComponent(), e.getX(), e.getY());
            }
        };

        table.addMouseListener(tableMouseListener);
    }

    /**
     * Create context menu
     */
    private JPopupMenu createContextPopupMenu() {
        JPopupMenu menu = new JPopupMenu();

        JMenuItem addItem = new JMenuItem("Add");
        addItem.addActionListener(e -> addRowAndScroll());
        menu.add(addItem);

        JMenuItem removeItem = new JMenuItem("Remove");
        removeItem.addActionListener(e -> deleteSelectedRow());
        menu.add(removeItem);

        return menu;
    }

    /**
     * Add auto-append row feature when editing the last row
     */
    private void addRow(Object... values) {
        if (values == null || values.length == 0) {
            tableModel.addRow(new Object[]{true, "", "", ""});
        } else if (values.length == 2) {
            // Legacy support: if 2 values provided, treat as Key, Value
            tableModel.addRow(new Object[]{true, values[0], values[1], ""});
        } else if (values.length == 3) {
            // New format: enabled, key, value
            tableModel.addRow(new Object[]{values[0], values[1], values[2], ""});
        } else {
            Object[] row = new Object[4];
            row[0] = true; // enabled
            for (int i = 0; i < Math.min(values.length, 2); i++) {
                row[i + 1] = values[i];
            }
            for (int i = values.length; i < 2; i++) {
                row[i + 1] = "";
            }
            row[3] = ""; // delete
            tableModel.addRow(row);
        }
    }

    /**
     * Add a new row and scroll to it
     */
    public void addRowAndScroll() {
        addRow();
        scrollToLastRow();
    }

    /**
     * Delete the currently selected row
     */
    public void deleteSelectedRow() {
        stopCellEditing();

        int selectedRow = table.getSelectedRow();
        if (selectedRow >= 0) {
            int modelRow = selectedRow;
            if (table.getRowSorter() != null) {
                modelRow = table.getRowSorter().convertRowIndexToModel(selectedRow);
            }

            if (modelRow >= 0 && modelRow < tableModel.getRowCount()) {
                int rowCount = tableModel.getRowCount();

                if (rowCount <= 1) {
                    return;
                }

                tableModel.removeRow(modelRow);
                ensureEmptyLastRow();
            }
        }
    }


    /**
     * 获取环境变量列表（新格式）
     */
    public List<EnvironmentVariable> getVariableList() {
        // Stop cell editing to ensure any in-progress edits are committed
        // Use parent class method with recursion protection
        stopCellEditingWithProtection();

        List<EnvironmentVariable> dataList = new ArrayList<>();
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            boolean enabled = getBooleanValue(i, COL_DRAG_ENABLE);
            String key = getStringValue(i, COL_KEY);
            String value = getStringValue(i, COL_VALUE);

            if (!key.isEmpty()) {
                dataList.add(new EnvironmentVariable(enabled, key, value));
            }
        }
        return dataList;
    }

    /**
     * 设置环境变量列表（新格式）
     */
    public void setVariableList(List<EnvironmentVariable> dataList) {
        stopCellEditing();

        suppressAutoAppendRow = true;
        try {
            tableModel.setRowCount(0);
            if (dataList != null) {
                for (EnvironmentVariable var : dataList) {
                    tableModel.addRow(new Object[]{var.isEnabled(), var.getKey(), var.getValue(), ""});
                }
            }

            if (tableModel.getRowCount() == 0 || hasContentInLastRow()) {
                tableModel.addRow(new Object[]{true, "", "", ""});
            }
        } finally {
            suppressAutoAppendRow = false;
        }
    }


    /**
     * Check if table is currently in dragging state
     *
     * @return true if dragging is in progress
     */
    public boolean isDragging() {
        return isDragging;
    }
}
