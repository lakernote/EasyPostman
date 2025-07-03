package com.laker.postman.common.table.map;

import com.laker.postman.common.constants.Colors;
import com.laker.postman.common.table.TableRowTransferHandler;
import com.laker.postman.util.FontUtil;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * <pre>
 *  1.通用的表格面板，支持多列
 *  2.支持通过右键菜单添加和删除行。
 *  3.空单元格会高亮显示，便于用户注意到未填写的内容。
 * </pre>
 * <p>
 * 【入参示例】
 * <pre>
 *     // 创建一个三列表格，表头为A、B、C
 *     EasyTablePanel panel = new EasyTablePanel(new String[]{"Name", "Age", "Email"});
 * </pre>
 * <p>
 * 【出参示例】
 * <pre>
 *     // 获取表格内容为 List<Map<String, Object>>
 *     List<Map<String, Object>> rows = panel.getRows();
 * </pre>
 */
@Slf4j
public class EasyTablePanel extends JPanel {
    // 表格的数据模型
    private final DefaultTableModel tableModel;
    // JTable 组件
    private final JTable table;
    // 右键菜单
    private final JPopupMenu popupMenu;
    // 列名数组
    private final String[] columns;

    /**
     * -- GETTER --
     * 获取表格是否可编辑
     */
    @Getter
    private boolean editable = true; // 新增字段，默认可编辑

    /**
     * 控制自动补空行的标志，防止批量操作时触发自动补空行
     */
    private boolean suppressAutoAppendRow = false;

    /**
     * 构造方法，初始化表格和右键菜单。
     *
     * @param columns 表头列名数��
     *                <br>示例：new String[]{"Name", "Age", "Email"}
     */
    public EasyTablePanel(String[] columns) {
        this.columns = columns;
        setLayout(new BorderLayout());
        setBackground(new Color(248, 250, 252));
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 225, 230)),
                BorderFactory.createEmptyBorder(8, 8, 8, 8)));
        // 初始化表格模型
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return editable; // 根据 editable 字段控制
            }
        };
        // 创建表格
        table = new JTable(tableModel);
        // 启用行拖动排序
        enableRowDragAndDrop();
        // 初始化表格UI
        initTableUI();
        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getViewport().setBackground(new Color(248, 250, 252));
        add(scrollPane, BorderLayout.CENTER);
        // 创建右键菜单
        popupMenu = createPopupMenu();
        // 添加鼠标监听器
        addTableListener();

        addAutoAppendRowFeature();
    }

    /**
     * 初始化表格UI样式
     */
    private void initTableUI() {
        table.setFillsViewportHeight(true);
        table.setRowHeight(28);
        table.setFont(FontUtil.getDefaultFont(Font.PLAIN, 12));
        table.getTableHeader().setFont(FontUtil.getDefaultFont(Font.BOLD, 12));
        table.getTableHeader().setBackground(new Color(240, 242, 245));
        table.getTableHeader().setForeground(new Color(33, 33, 33));
        table.setGridColor(new Color(230, 230, 230));
        table.setSelectionBackground(new Color(200, 220, 245));
        table.setSelectionForeground(new Color(33, 150, 243));
        table.setIntercellSpacing(new Dimension(0, 2));
        table.setShowHorizontalLines(false);
        table.setShowVerticalLines(false);
        table.setRowMargin(0);
        table.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 225, 230)),
                BorderFactory.createEmptyBorder(2, 2, 2, 2)));
        table.setOpaque(false);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        table.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
    }

    /**
     * 创建右键菜单，包含添加行和删除行
     *
     * @return JPopupMenu
     */
    private JPopupMenu createPopupMenu() {
        JPopupMenu menu = new JPopupMenu();
        JMenuItem addItem = new JMenuItem("Add");
        // 添加行事件
        addItem.addActionListener(e -> addRowAndScroll());
        JMenuItem delItem = new JMenuItem("Remove");
        // 删除行事件
        delItem.addActionListener(e -> deleteSelectedRow());
        menu.add(addItem);
        menu.add(delItem);
        return menu;
    }

    private void addTableListener() {
        /*
          添加表格的鼠标监听器，右键弹出菜单
         */
        MouseAdapter tableMouseListener = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (e.isPopupTrigger()) showMenu(e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger()) showMenu(e);
            }

            /**
             * 显示右键菜单
             */
            private void showMenu(MouseEvent e) {
                if (!editable) return; // 如果不可编辑，则不显示右键菜单
                int row = table.rowAtPoint(e.getPoint());
                if (row >= 0) {
                    table.setRowSelectionInterval(row, row);
                } else {
                    table.clearSelection();
                }
                popupMenu.show(e.getComponent(), e.getX(), e.getY());
            }
        };
        table.addMouseListener(tableMouseListener);

        // 鼠标悬停高亮
        table.addMouseMotionListener(new MouseMotionAdapter() {
            int lastRow = -1;

            @Override
            public void mouseMoved(java.awt.event.MouseEvent e) {
                int row = table.rowAtPoint(e.getPoint());
                if (row != lastRow) {
                    lastRow = row;
                    table.putClientProperty("hoverRow", row);
                    table.repaint();
                }
            }
        });
        // 设置所有列的默认渲染器,如果单元格内容为空则高亮显示，并兼容斑马纹、选中、悬停
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                int hoverRow = -1;
                Object hoverObj = table.getClientProperty("hoverRow");
                if (hoverObj instanceof Integer) hoverRow = (Integer) hoverObj;
                String cellValue = value == null ? null : value.toString();
                if (isSelected) {
                    c.setBackground(table.getSelectionBackground());
                } else if (row == hoverRow) {
                    c.setBackground(new Color(230, 240, 255));
                } else if (cellValue == null || cellValue.trim().isEmpty()) {
                    c.setBackground(Colors.EMPTY_CELL_YELLOW);
                } else {
                    c.setBackground(row % 2 == 0 ? new Color(250, 252, 255) : Color.WHITE);
                }
                return c;
            }
        });
        tableModel.addTableModelListener(e -> {
            // 当表格内容update，更新边框提示 新增和删除在右键菜单中处理
            if (e.getType() == TableModelEvent.UPDATE
                    && e.getFirstRow() == e.getLastRow()) { // 只处理单行更新
                updateTableBorder(true);
            }
        });
    }

    public void addTableModelListener(TableModelListener l) {
        tableModel.addTableModelListener(l);
    }

    /**
     * 获取所有行数据，每行是一个Map，key为列名，value为单元格内容。
     *
     * @return List<Map < String, Object>>
     * <br>【出参示例】
     * <pre>
     *     List<Map<String, Object>> rows = panel.getRows();
     * </pre>
     */
    public List<Map<String, Object>> getRows() {
        List<Map<String, Object>> list = new ArrayList<>();
        int colCount = tableModel.getColumnCount();
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            Map<String, Object> row = new LinkedHashMap<>();
            for (int j = 0; j < colCount; j++) {
                row.put(tableModel.getColumnName(j), tableModel.getValueAt(i, j));
            }
            list.add(row);
        }
        return list;
    }

    /**
     * 右键menu: 添加一行并滚动到该行
     */
    private void addRowAndScroll() {
        int row = tableModel.getRowCount();
        // 添加空行
        tableModel.addRow(new Object[columns.length]);
        if (row >= 0) {
            // 滚动到新行并选中
            table.scrollRectToVisible(table.getCellRect(row, 0, true)); // 滚动到新行
            table.setRowSelectionInterval(row, row); // 选中该行
        }
    }

    public void scrollRectToVisible() {
        int row = table.getRowCount() - 1; // 滚动到最后一行
        if (row >= 0) {
            table.scrollRectToVisible(table.getCellRect(row, 0, true));
            table.setRowSelectionInterval(row, row); // 选中最后一行
        }
    }

    /**
     * 右键menu: 删除选中的行
     */
    private void deleteSelectedRow() {
        int row = table.getSelectedRow();
        if (row >= 0) {
            boolean isRowEmpty = true;
            for (int col = 0; col < tableModel.getColumnCount(); col++) {
                Object value = tableModel.getValueAt(row, col);
                if (value != null && !value.toString().trim().isEmpty()) {
                    isRowEmpty = false;
                    break;
                }
            }
            tableModel.removeRow(row);
            if (!isRowEmpty) {
                updateTableBorder(true);
            }
        }
    }

    /**
     * 清空表格内容
     * <br>【调用示例】panel.clear();
     */
    public void clear() {
        suppressAutoAppendRow = true;
        tableModel.setRowCount(0);
        suppressAutoAppendRow = false;
        ensureOneEmptyRow();
    }

    /**
     * 用 List<Map<String, Object>> 填充表格
     */
    public void setRows(List<Map<String, Object>> rows) {
        suppressAutoAppendRow = true;
        tableModel.setRowCount(0);
        if (rows != null) {
            for (Map<String, Object> row : rows) {
                Object[] values = new Object[tableModel.getColumnCount()];
                for (int i = 0; i < values.length; i++) {
                    values[i] = row.get(tableModel.getColumnName(i));
                }
                tableModel.addRow(values);
            }
        }
        suppressAutoAppendRow = false;
        ensureOneEmptyRow();
    }

    /**
     * 保证表格底部只有一行空行
     */
    private void ensureOneEmptyRow() {
        int rowCount = tableModel.getRowCount();
        if (rowCount == 0) {
            tableModel.addRow(new Object[columns.length]);
            return;
        }
        // 检查最后一行是否为空
        boolean lastIsEmpty = true;
        for (int col = 0; col < columns.length; col++) {
            Object value = tableModel.getValueAt(rowCount - 1, col);
            if (value != null && !value.toString().trim().isEmpty()) {
                lastIsEmpty = false;
                break;
            }
        }
        if (!lastIsEmpty) {
            tableModel.addRow(new Object[columns.length]);
        }
        // 移除多余的空行
        for (int i = rowCount - 2; i >= 0; i--) {
            boolean empty = true;
            for (int col = 0; col < columns.length; col++) {
                Object value = tableModel.getValueAt(i, col);
                if (value != null && !value.toString().trim().isEmpty()) {
                    empty = false;
                    break;
                }
            }
            if (empty) {
                tableModel.removeRow(i);
            }
        }
    }

    /**
     * 添加一行数据
     *
     * @param values 行数据，顺序与表头一致
     *               <br>【调用示例】panel.addRow("a", "b", "c");
     */
    public void addRow(Object... values) {
        suppressAutoAppendRow = true;
        tableModel.addRow(values);
        suppressAutoAppendRow = false;
        ensureOneEmptyRow();
    }

    /**
     * 设置某一列的自定义渲染器
     *
     * @param columnIndex 列索引
     * @param renderer    TableCellRenderer
     */
    public void setColumnRenderer(int columnIndex, TableCellRenderer renderer) {
        table.getColumnModel().getColumn(columnIndex).setCellRenderer(renderer);
    }

    /**
     * 设置某一列的自定义编辑器
     *
     * @param columnIndex 列索引
     * @param editor      TableCellEditor
     */
    public void setColumnEditor(int columnIndex, TableCellEditor editor) {
        table.getColumnModel().getColumn(columnIndex).setCellEditor(editor);
    }

    /**
     * 获取表格行数
     *
     * @return 行数
     */
    public int getRowCount() {
        return table.getRowCount();
    }

    /**
     * 获取指定单元格的值
     *
     * @param row    行索引
     * @param column 列索引
     *               <br>【调用示例】Object value = panel.getValueAt(0, 1);
     */
    public Object getValueAt(int row, int column) {
        return table.getValueAt(row, column);
    }

    // 设置自适应最后一列，消除右侧空白
    public void setAutoResizeLastColumn() {
        table.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
    }

    // 设置指定列的首选宽度
    public void setColumnPreferredWidth(int columnIndex, int width) {
        table.getColumnModel().getColumn(columnIndex).setPreferredWidth(width);
    }

    // 设置指定列的最小宽度
    public void setColumnMinWidth(int columnIndex, int width) {
        table.getColumnModel().getColumn(columnIndex).setMinWidth(width);
    }

    // 设置指定列的最大宽度
    public void setColumnMaxWidth(int columnIndex, int width) {
        table.getColumnModel().getColumn(columnIndex).setMaxWidth(width);
    }

    // 设置表格可见高度��行数和总宽度）
    public void setPreferredScrollableViewportHeight(int rowCount, int totalWidth) {
        table.setPreferredScrollableViewportSize(new Dimension(totalWidth, table.getRowHeight() * rowCount));
    }

    // 获取当前正在编辑的行
    public int getEditingRow() {
        return table.getEditingRow();
    }

    public void stopCellEditing() {
        if (table.isEditing()) {
            table.getCellEditor().stopCellEditing();
        }
    }

    /**
     * 设置表格是否可编辑
     *
     * @param editable true-可编辑，false-只读
     */
    public void setEditable(boolean editable) {
        this.editable = editable;
        table.repaint();
    }

    // 整体高亮时边框提示
    public void updateTableBorder(boolean modified) {
        if (modified) {
            table.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(255, 140, 0), 3), // 更鲜明橙色且加粗
                    BorderFactory.createEmptyBorder(2, 2, 2, 2)));
        } else {
            table.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(220, 225, 230)),
                    BorderFactory.createEmptyBorder(2, 2, 2, 2)));
        }
        table.repaint();
    }

    /**
     * 启用JTable行拖动排序
     */
    private void enableRowDragAndDrop() {
        table.setDragEnabled(true);
        table.setDropMode(DropMode.INSERT_ROWS);
        table.setTransferHandler(new TableRowTransferHandler(tableModel, this));
    }

    /**
     * 自动在最后一行有内容时添加新空行，实现类似 Postman 的交互体验
     */
    private void addAutoAppendRowFeature() {
        tableModel.addTableModelListener(new TableModelListener() {
            @Override
            public void tableChanged(TableModelEvent e) {
                if (!editable || suppressAutoAppendRow) return;
                int rowCount = tableModel.getRowCount();
                if (rowCount == 0) {
                    tableModel.addRow(new Object[columns.length]);
                    return;
                }
                // 检查最后一行是否有内容
                boolean hasContent = false;
                for (int col = 0; col < columns.length; col++) {
                    Object value = tableModel.getValueAt(rowCount - 1, col);
                    if (value != null && !value.toString().trim().isEmpty()) {
                        hasContent = true;
                        break;
                    }
                }
                // 如果最后一行有内容，则自动添加一行空白行
                if (hasContent) {
                    // 避免重复添加空行
                    boolean needAdd = true;
                    if (rowCount >= 2) {
                        boolean lastIsEmpty = true;
                        for (int col = 0; col < columns.length; col++) {
                            Object value = tableModel.getValueAt(rowCount - 1, col);
                            if (value != null && !value.toString().trim().isEmpty()) {
                                lastIsEmpty = false;
                                break;
                            }
                        }
                        boolean prevIsEmpty = true;
                        for (int col = 0; col < columns.length; col++) {
                            Object value = tableModel.getValueAt(rowCount - 2, col);
                            if (value != null && !value.toString().trim().isEmpty()) {
                                prevIsEmpty = false;
                                break;
                            }
                        }
                        // 如果最后一行和倒数第二行都为空，则不再添加
                        if (lastIsEmpty && prevIsEmpty) needAdd = false;
                    }
                    if (needAdd) {
                        tableModel.addRow(new Object[columns.length]);
                    }
                }
            }
        });
        // 初始化时至少有一行空行
        if (tableModel.getRowCount() == 0) {
            tableModel.addRow(new Object[columns.length]);
        }
    }
}
