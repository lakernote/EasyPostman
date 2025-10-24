package com.laker.postman.common.component.table;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.laker.postman.model.EnvironmentVariable;
import com.laker.postman.util.FontsUtil;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import javax.swing.event.TableModelListener;
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
 * 支持 Enable、Key、Value 和 Delete 列结构
 */
@Slf4j
public class EasyPostmanEnvironmentTablePanel extends JPanel {

    // Table components
    private DefaultTableModel tableModel;
    @Getter
    private JTable table;
    private final String[] columns;

    @Getter
    private boolean editable = true;

    /**
     * Flag to suppress auto-append row during batch operations
     */
    private boolean suppressAutoAppendRow = false;

    // Column indices
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
        this.columns = new String[]{"", nameCol, valueCol, ""};
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

    private void initializeTableUI() {
        table.setFillsViewportHeight(true);
        table.setRowHeight(28);
        table.setFont(FontsUtil.getDefaultFont(Font.PLAIN, 11));
        table.getTableHeader().setFont(FontsUtil.getDefaultFont(Font.BOLD, 11));
        table.getTableHeader().setBackground(new Color(240, 242, 245));
        table.getTableHeader().setForeground(new Color(33, 33, 33));
        table.setGridColor(new Color(237, 237, 237));
        table.setShowHorizontalLines(true);
        table.setShowVerticalLines(true);
        table.setIntercellSpacing(new Dimension(2, 2));
        table.setRowMargin(2);
        table.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(237, 237, 237)),
                BorderFactory.createEmptyBorder(2, 2, 2, 2)));
        table.setOpaque(false);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        table.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);

        // Configure Tab key behavior to move between columns
        table.setSurrendersFocusOnKeystroke(true);

        // Set column widths for drag/enable column
        table.getColumnModel().getColumn(COL_DRAG_ENABLE).setPreferredWidth(40);
        table.getColumnModel().getColumn(COL_DRAG_ENABLE).setMaxWidth(40);
        table.getColumnModel().getColumn(COL_DRAG_ENABLE).setMinWidth(40);

        // Set column widths for delete button
        table.getColumnModel().getColumn(COL_DELETE).setPreferredWidth(40);
        table.getColumnModel().getColumn(COL_DELETE).setMaxWidth(40);
        table.getColumnModel().getColumn(COL_DELETE).setMinWidth(40);

        // Setup Tab key navigation to move between columns in the same row
        setupTabKeyNavigation();
    }

    /**
     * Setup Tab key navigation to move between columns instead of rows
     */
    private void setupTabKeyNavigation() {
        // Override Tab and Shift+Tab behavior for proper column-by-column navigation
        InputMap inputMap = table.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        ActionMap actionMap = table.getActionMap();

        // Tab key - move to next editable cell in the same row
        inputMap.put(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_TAB, 0), "nextCell");
        actionMap.put("nextCell", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                moveToNextEditableCell(false);
            }
        });

        // Shift+Tab - move to previous editable cell in the same row
        inputMap.put(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_TAB, java.awt.event.InputEvent.SHIFT_DOWN_MASK), "previousCell");
        actionMap.put("previousCell", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                moveToNextEditableCell(true);
            }
        });
    }

    /**
     * Move to the next (or previous) editable cell in the current row
     */
    private void moveToNextEditableCell(boolean reverse) {
        int currentRow = table.getSelectedRow();
        int currentColumn = table.getSelectedColumn();

        if (currentRow < 0 || currentColumn < 0) {
            table.changeSelection(0, COL_KEY, false, false);
            table.editCellAt(0, COL_KEY);
            return;
        }

        if (table.isEditing()) {
            table.getCellEditor().stopCellEditing();
        }

        int nextColumn = currentColumn;
        int columnCount = table.getColumnCount();

        if (reverse) {
            do {
                nextColumn--;
                if (nextColumn < 0) {
                    if (currentRow > 0) {
                        currentRow--;
                        nextColumn = COL_VALUE;
                    } else {
                        nextColumn = 0;
                    }
                    break;
                }
            } while (nextColumn >= 0 && !isCellEditable(currentRow, nextColumn));
        } else {
            do {
                nextColumn++;
                if (nextColumn >= columnCount) {
                    if (currentRow < table.getRowCount() - 1) {
                        currentRow++;
                        nextColumn = COL_KEY;
                    } else {
                        nextColumn = columnCount - 1;
                    }
                    break;
                }
            } while (nextColumn < columnCount && !isCellEditable(currentRow, nextColumn));
        }

        if (nextColumn >= 0 && nextColumn < columnCount && currentRow >= 0 && currentRow < table.getRowCount()) {
            table.changeSelection(currentRow, nextColumn, false, false);
            if (isCellEditable(currentRow, nextColumn)) {
                table.editCellAt(currentRow, nextColumn);
                Component editor = table.getEditorComponent();
                if (editor instanceof JTextField) {
                    editor.requestFocusInWindow();
                    ((JTextField) editor).selectAll();
                }
            }
        }
    }

    /**
     * Check if a cell is editable
     */
    private boolean isCellEditable(int row, int column) {
        if (!editable) {
            return false;
        }
        return column == COL_KEY || column == COL_VALUE;
    }

    private void setupCellRenderersAndEditors() {
        setEmptyCellWhiteBackgroundRenderer();

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
     * Custom renderer for delete button column
     */
    private class DeleteButtonRenderer extends JLabel implements TableCellRenderer {
        private final Icon deleteIcon;

        public DeleteButtonRenderer() {
            setHorizontalAlignment(SwingConstants.CENTER);
            setOpaque(true);
            deleteIcon = new FlatSVGIcon("icons/close.svg", 16, 16);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus, int row, int column) {
            if (isSelected) {
                setBackground(table.getSelectionBackground());
                setForeground(table.getSelectionForeground());
            } else {
                setBackground(table.getBackground());
                setForeground(table.getForeground());
            }

            setIcon(null);
            setCursor(Cursor.getDefaultCursor());

            int modelRow = row;
            if (table.getRowSorter() != null) {
                modelRow = table.getRowSorter().convertRowIndexToModel(row);
            }

            if (modelRow >= 0 && modelRow < tableModel.getRowCount()) {
                int rowCount = tableModel.getRowCount();
                boolean isLastRow = (modelRow == rowCount - 1);

                Object keyObj = tableModel.getValueAt(modelRow, COL_KEY);
                Object valueObj = tableModel.getValueAt(modelRow, COL_VALUE);
                String keyStr = keyObj == null ? "" : keyObj.toString().trim();
                String valueStr = valueObj == null ? "" : valueObj.toString().trim();
                boolean isEmpty = keyStr.isEmpty() && valueStr.isEmpty();

                boolean shouldShowIcon = false;
                if (!isLastRow) {
                    shouldShowIcon = true;
                } else {
                    shouldShowIcon = !isEmpty && rowCount > 1;
                }

                if (shouldShowIcon && editable) {
                    setIcon(deleteIcon);
                    setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                }
            }

            return this;
        }
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
    private void addAutoAppendRowFeature() {
        tableModel.addTableModelListener(e -> {
            // 在拖拽期间、批量操作期间或不可编辑时，禁用自动补空行
            if (suppressAutoAppendRow || suppressAutoDuringDrag || !editable) {
                return;
            }

            SwingUtilities.invokeLater(() -> {
                try {
                    int rowCount = tableModel.getRowCount();
                    if (rowCount == 0) {
                        return;
                    }

                    int lastRow = rowCount - 1;
                    boolean lastRowHasContent = false;

                    for (int col = COL_KEY; col <= COL_VALUE; col++) {
                        Object value = tableModel.getValueAt(lastRow, col);
                        if (value != null && !value.toString().trim().isEmpty()) {
                            lastRowHasContent = true;
                            break;
                        }
                    }

                    if (lastRowHasContent) {
                        suppressAutoAppendRow = true;
                        try {
                            tableModel.addRow(new Object[]{true, "", "", ""});
                        } finally {
                            suppressAutoAppendRow = false;
                        }
                    }
                } catch (Exception ex) {
                    log.warn("Error in auto-append row feature", ex);
                }
            });
        });
    }

    /**
     * 添加表格模型监听器
     */
    public void addTableModelListener(TableModelListener listener) {
        if (tableModel != null) {
            tableModel.addTableModelListener(listener);
        }
    }

    /**
     * 清空表格数据
     */
    public void clear() {
        stopCellEditing();

        suppressAutoAppendRow = true;
        try {
            tableModel.setRowCount(0);
            tableModel.addRow(new Object[]{true, "", "", ""});
        } finally {
            suppressAutoAppendRow = false;
        }
    }

    /**
     * 添加一行数据 (内部使用)
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
     * Scroll to last row
     */
    private void scrollToLastRow() {
        SwingUtilities.invokeLater(() -> {
            int rowCount = table.getRowCount();
            if (rowCount > 0) {
                Rectangle rect = table.getCellRect(rowCount - 1, 0, true);
                table.scrollRectToVisible(rect);
            }
        });
    }

    /**
     * 获取环境变量列表（新格式）
     */
    public List<EnvironmentVariable> getVariableList() {
        List<EnvironmentVariable> dataList = new ArrayList<>();
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            Object enabledObj = tableModel.getValueAt(i, COL_DRAG_ENABLE);
            Object keyObj = tableModel.getValueAt(i, COL_KEY);
            Object valueObj = tableModel.getValueAt(i, COL_VALUE);

            boolean enabled = enabledObj instanceof Boolean ? (Boolean) enabledObj : true;
            String key = keyObj == null ? "" : keyObj.toString().trim();
            String value = valueObj == null ? "" : valueObj.toString().trim();

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
     * Check if the last row has any content
     */
    private boolean hasContentInLastRow() {
        int rowCount = tableModel.getRowCount();
        if (rowCount == 0) {
            return false;
        }

        int lastRow = rowCount - 1;
        Object keyObj = tableModel.getValueAt(lastRow, COL_KEY);
        Object valueObj = tableModel.getValueAt(lastRow, COL_VALUE);

        String key = keyObj == null ? "" : keyObj.toString().trim();
        String value = valueObj == null ? "" : valueObj.toString().trim();

        return !key.isEmpty() || !value.isEmpty();
    }

    /**
     * Ensure there's always an empty row at the end of the table
     */
    private void ensureEmptyLastRow() {
        suppressAutoAppendRow = true;
        try {
            if (tableModel.getRowCount() == 0 || hasContentInLastRow()) {
                tableModel.addRow(new Object[]{true, "", "", ""});
            }
        } finally {
            suppressAutoAppendRow = false;
        }
    }

    /**
     * Safely stop cell editing
     */
    public void stopCellEditing() {
        if (table.isEditing()) {
            TableCellEditor editor = table.getCellEditor();
            if (editor != null) {
                editor.stopCellEditing();
            }
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
