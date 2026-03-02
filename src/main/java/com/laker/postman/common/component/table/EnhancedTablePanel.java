package com.laker.postman.common.component.table;

import com.formdev.flatlaf.FlatClientProperties;
import com.laker.postman.common.component.SearchTextField;
import com.laker.postman.common.component.SearchableTextArea;
import com.laker.postman.util.*;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.*;
import java.util.*;
import java.util.List;

/**
 * 增强型通用表格组件
 * <ul>
 *   <li>自动铺满视口宽度（内容窄时拉伸，超宽时出横向滚动条）</li>
 *   <li>顶部工具栏：实时搜索过滤 + 列多选过滤 + 总行数显示</li>
 *   <li>分页：每页条数下拉 20/50/100/All、翻页、页码跳转</li>
 *   <li>列标题单击排序（升/降，带箭头指示）</li>
 *   <li>行悬停高亮 + 斑马纹</li>
 *   <li>右键菜单：复制单元格、复制整行</li>
 *   <li>支持运行时动态重置列结构（{@link #resetAndSetData}）</li>
 * </ul>
 */
@Slf4j
public class EnhancedTablePanel extends JPanel {

    // ── 列名（可变，支持动态重置）───────────────────────────────────────────
    private String[] currentColumns;

    // ── 数据 ──────────────────────────────────────────────────────────────
    private final List<Object[]> allRows = new ArrayList<>();
    private final List<Object[]> filteredRows = new ArrayList<>();

    // ── 分页 ──────────────────────────────────────────────────────────────
    private int pageSize = 20;
    private int currentPage = 0;

    // ── 排序 ──────────────────────────────────────────────────────────────
    private int sortCol = -1;
    private boolean sortAsc = true;

    // ── 过滤 ─────────────────────────────────────────────────────────────
    private String filterText = "";
    /**
     * null = 搜索全列；否则只在这些列索引中搜索
     */
    private Set<Integer> filterCols = null;

    // ── Hover ─────────────────────────────────────────────────────────────
    private int hoveredRow = -1;

    // ── UI 常量 ───────────────────────────────────────────────────────────
    private static final String SEPARATOR_FG = "Separator.foreground";
    private static final String LABEL_DISABLED = "Label.disabledForeground";

    /**
     * 超过此字符数时，单元格显示截断文字 + 省略号，并启用 tooltip
     */
    private static final int LARGE_CELL_THRESHOLD = 100;
    /**
     * Tooltip 最多显示的字符数
     */
    private static final int TOOLTIP_MAX_LEN = 300;

    // ── 分页大小选项 ───────────────────────────────────────────────────────
    private static final int[] PAGE_SIZES = {20, 50, 100, 0};
    private static final String[] SIZE_LABELS = {"20", "50", "100", "All"};

    // ── Swing 组件 ────────────────────────────────────────────────────────
    @Getter
    private JTable table;
    private DefaultTableModel tableModel;
    private SearchTextField searchField;
    private JButton colFilterBtn;
    private JLabel hintLabel;
    private JLabel pageInfoLabel;
    private JButton btnPrev;
    private JButton btnNext;

    // ── 搜索选项（与 SearchTextField 联动）────────────────────────────────
    private boolean caseSensitive = false;
    private boolean wholeWord = false;

    // ── 空状态覆盖层 ─────────────────────────────────────────────────────
    private JLabel emptyLabel;

    // ─────────────────────────────────────────────────────────────────────

    public EnhancedTablePanel(String[] columns) {
        this.currentColumns = columns.clone();
        buildUI();
    }

    // ═══════════════════ Public API ══════════════════════════════════════

    /**
     * 动态重置列结构并替换全量数据。
     */
    public void resetAndSetData(String[] newColumns, List<Object[]> rows) {
        boolean colsChanged = !Arrays.equals(currentColumns, newColumns);
        this.currentColumns = newColumns.clone();
        this.sortCol = -1;
        this.currentPage = 0;
        this.filterText = "";
        searchField.setText("");

        if (colsChanged) {
            // 列结构变化：清除超出新列数的无效索引；若清后覆盖全部列则置 null（全选）
            if (filterCols != null && !filterCols.isEmpty()) {
                filterCols.removeIf(ci -> ci >= newColumns.length);
                if (filterCols.size() >= newColumns.length) filterCols = null;
            }
            updateColFilterBtnLabel();
        }

        // 重置 tableModel 列
        tableModel.setRowCount(0);
        tableModel.setColumnCount(0);
        for (String col : currentColumns) tableModel.addColumn(col);

        allRows.clear();
        if (rows != null) allRows.addAll(rows);

        updateHintLabel();
        applyFilterAndSort();
    }

    /**
     * 保持列不变，仅替换数据
     */
    public void setData(List<Object[]> rows) {
        allRows.clear();
        if (rows != null) allRows.addAll(rows);
        currentPage = 0;
        sortCol = -1;
        updateHintLabel();
        applyFilterAndSort();
    }

    /**
     * 清空数据（保持列结构）
     */
    public void clearData() {
        allRows.clear();
        filteredRows.clear();
        currentPage = 0;
        sortCol = -1;
        updateHintLabel();
        tableModel.setRowCount(0);
        updatePaginationControls();
        updateEmptyState();
    }

    /**
     * 返回全量数据行数（不受分页/过滤影响）
     */
    public int getTotalRowCount() {
        return allRows.size();
    }

    // ═══════════════════ UI Build ════════════════════════════════════════

    private void buildUI() {
        setLayout(new BorderLayout(0, 0));
        setBorder(BorderFactory.createEmptyBorder());
        add(buildToolBar(), BorderLayout.NORTH);
        add(buildTableArea(), BorderLayout.CENTER);
        add(buildPagination(), BorderLayout.SOUTH);
    }

    // ── 工具栏：搜索框 + 列筛选按钮 + 行数提示 ────────────────────────────
    private JPanel buildToolBar() {
        JPanel bar = new JPanel(new BorderLayout(4, 0));
        bar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0,
                        UIManager.getColor(SEPARATOR_FG)),
                BorderFactory.createEmptyBorder(4, 6, 4, 6)));

        searchField = new SearchTextField();
        // 保留 SearchTextField 内置的大小写/整词匹配按钮（不覆盖 trailing component）
        searchField.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT,
                I18nUtil.getMessage(MessageKeys.TABLE_SEARCH_PLACEHOLDER_ALL));
        searchField.setPreferredSize(new Dimension(220, 28));
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) {
                onSearch();
            }

            public void removeUpdate(DocumentEvent e) {
                onSearch();
            }

            public void changedUpdate(DocumentEvent e) {
                onSearch();
            }
        });

        // 监听大小写 / 整词切换 → 重新过滤
        searchField.addPropertyChangeListener("caseSensitive", e -> {
            caseSensitive = searchField.isCaseSensitive();
            if (!filterText.isEmpty()) {
                currentPage = 0;
                applyFilterAndSort();
            }
        });
        searchField.addPropertyChangeListener("wholeWord", e -> {
            wholeWord = searchField.isWholeWord();
            if (!filterText.isEmpty()) {
                currentPage = 0;
                applyFilterAndSort();
            }
        });

        // Escape 清空搜索框
        searchField.getInputMap(WHEN_FOCUSED).put(
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "clearSearch");
        searchField.getActionMap().put("clearSearch", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (!searchField.getText().isEmpty()) searchField.setText("");
            }
        });

        colFilterBtn = new JButton(I18nUtil.getMessage(MessageKeys.TABLE_COL_FILTER_BTN));
        colFilterBtn.setFont(colFilterBtn.getFont().deriveFont(Font.PLAIN, 11f));
        colFilterBtn.setFocusPainted(false);
        colFilterBtn.setToolTipText(I18nUtil.getMessage(MessageKeys.TABLE_COL_FILTER_TOOLTIP));
        colFilterBtn.addActionListener(e -> showColFilterPopup());

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        left.setOpaque(false);
        left.add(searchField);
        left.add(colFilterBtn);

        hintLabel = new JLabel("0" + I18nUtil.getMessage(MessageKeys.TABLE_ROWS_SUFFIX));
        hintLabel.setForeground(UIManager.getColor(LABEL_DISABLED));
        hintLabel.setFont(hintLabel.getFont().deriveFont(Font.PLAIN, 11f));
        hintLabel.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 0));

        bar.add(left, BorderLayout.WEST);
        bar.add(hintLabel, BorderLayout.EAST);
        return bar;
    }

    // ── 列筛选 popup（持久化，避免每次重建导致状态丢失）──────────────────────
    private JPopupMenu colFilterPopup = null;
    private JCheckBox[] colFilterBoxes = null;
    private String[] colFilterPopupColumns = null;

    private void showColFilterPopup() {
        if (currentColumns.length == 0) return;

        if (colFilterPopup == null || !Arrays.equals(colFilterPopupColumns, currentColumns)) {
            colFilterPopup = buildColFilterPopup();
            colFilterPopupColumns = currentColumns.clone();
        } else {
            syncCheckboxesToFilterCols();
        }

        colFilterPopup.show(colFilterBtn, 0, colFilterBtn.getHeight());
    }

    private JPopupMenu buildColFilterPopup() {
        JPopupMenu popup = new JPopupMenu();
        popup.setLayout(new BorderLayout(0, 0));

        // ── 标题栏 ──────────────────────────────────────────────────────
        JLabel titleLabel = new JLabel(I18nUtil.getMessage(MessageKeys.TABLE_COL_FILTER_TITLE));
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 12f));
        titleLabel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, UIManager.getColor(SEPARATOR_FG)),
                BorderFactory.createEmptyBorder(6, 10, 6, 10)));
        popup.add(titleLabel, BorderLayout.NORTH);

        // ── 复选框列表 ───────────────────────────────────────────────────
        JPanel checkPanel = new JPanel(new GridLayout(0, 1, 0, 0));
        checkPanel.setBorder(BorderFactory.createEmptyBorder(4, 10, 4, 10));
        checkPanel.setOpaque(false);
        colFilterBoxes = new JCheckBox[currentColumns.length];
        for (int i = 0; i < currentColumns.length; i++) {
            boolean checked = (filterCols == null || filterCols.contains(i));
            colFilterBoxes[i] = new JCheckBox(currentColumns[i], checked);
            colFilterBoxes[i].setFont(colFilterBoxes[i].getFont().deriveFont(Font.PLAIN, 12f));
            colFilterBoxes[i].setOpaque(false);
            colFilterBoxes[i].setBorder(BorderFactory.createEmptyBorder(2, 0, 2, 0));
            checkPanel.add(colFilterBoxes[i]);
        }
        JScrollPane checkScroll = new JScrollPane(checkPanel,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        checkScroll.setBorder(BorderFactory.createEmptyBorder());
        checkScroll.setPreferredSize(new Dimension(200, Math.min(currentColumns.length * 28 + 8, 250)));
        popup.add(checkScroll, BorderLayout.CENTER);

        // ── 底部操作栏 ───────────────────────────────────────────────────
        JPanel footer = new JPanel(new BorderLayout(0, 0));
        footer.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, UIManager.getColor(SEPARATOR_FG)),
                BorderFactory.createEmptyBorder(6, 10, 6, 10)));

        JPanel linkRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        linkRow.setOpaque(false);
        JButton btnAll = makeLinkBtn(I18nUtil.getMessage(MessageKeys.TABLE_COL_FILTER_SELECT_ALL));
        JLabel sep = new JLabel(" | ");
        sep.setForeground(UIManager.getColor(LABEL_DISABLED));
        sep.setFont(sep.getFont().deriveFont(Font.PLAIN, 11f));
        JButton btnNone = makeLinkBtn(I18nUtil.getMessage(MessageKeys.TABLE_COL_FILTER_DESELECT_ALL));
        linkRow.add(btnAll);
        linkRow.add(sep);
        linkRow.add(btnNone);

        JButton btnOk = new JButton(I18nUtil.getMessage(MessageKeys.TABLE_COL_FILTER_OK));
        btnOk.setFont(btnOk.getFont().deriveFont(Font.PLAIN, 11f));
        btnOk.setFocusPainted(false);

        footer.add(linkRow, BorderLayout.WEST);
        footer.add(btnOk, BorderLayout.EAST);
        popup.add(footer, BorderLayout.SOUTH);

        btnAll.addActionListener(e -> {
            for (JCheckBox cb : colFilterBoxes) cb.setSelected(true);
        });
        btnNone.addActionListener(e -> {
            for (JCheckBox cb : colFilterBoxes) cb.setSelected(false);
        });
        btnOk.addActionListener(e -> applyColFilter(popup));

        return popup;
    }

    /**
     * 链接风格的小按钮（无边框、无背景、字体 11f）
     */
    private static JButton makeLinkBtn(String text) {
        JButton b = new JButton(text);
        b.setFont(b.getFont().deriveFont(Font.PLAIN, 11f));
        b.setFocusPainted(false);
        b.setBorderPainted(false);
        b.setContentAreaFilled(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.putClientProperty(FlatClientProperties.BUTTON_TYPE,
                FlatClientProperties.BUTTON_TYPE_BORDERLESS);
        return b;
    }

    private void syncCheckboxesToFilterCols() {
        if (colFilterBoxes == null) return;
        for (int i = 0; i < colFilterBoxes.length; i++) {
            colFilterBoxes[i].setSelected(filterCols == null || filterCols.contains(i));
        }
    }

    private void applyColFilter(JPopupMenu popup) {
        if (colFilterBoxes == null) return;
        Set<Integer> sel = new HashSet<>();
        boolean allChecked = true;
        for (int i = 0; i < colFilterBoxes.length; i++) {
            if (colFilterBoxes[i].isSelected()) sel.add(i);
            else allChecked = false;
        }
        filterCols = allChecked ? null : sel;
        updateColFilterBtnLabel();
        currentPage = 0;
        applyFilterAndSort();
        popup.setVisible(false);
    }

    /**
     * 更新列筛选按钮文字 + 搜索框 placeholder
     */
    private void updateColFilterBtnLabel() {
        if (filterCols == null) {
            // 默认：搜索全部列
            colFilterBtn.setText(I18nUtil.getMessage(MessageKeys.TABLE_COL_FILTER_BTN));
            colFilterBtn.setForeground(null);
            searchField.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT,
                    I18nUtil.getMessage(MessageKeys.TABLE_SEARCH_PLACEHOLDER_ALL));
        } else if (filterCols.isEmpty()) {
            // 全不选：置灰提示用户未指定范围
            colFilterBtn.setText(I18nUtil.getMessage(MessageKeys.TABLE_COL_FILTER_BTN_N, "0"));
            colFilterBtn.setForeground(UIManager.getColor(LABEL_DISABLED));
            searchField.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT,
                    I18nUtil.getMessage(MessageKeys.TABLE_SEARCH_PLACEHOLDER_NONE));
        } else {
            // 已选部分列：高亮显示数量，placeholder 显示列名
            colFilterBtn.setText(I18nUtil.getMessage(MessageKeys.TABLE_COL_FILTER_BTN_N,
                    String.valueOf(filterCols.size())));
            colFilterBtn.setForeground(UIManager.getColor("Component.accentColor"));
            searchField.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT,
                    I18nUtil.getMessage(MessageKeys.TABLE_SEARCH_PLACEHOLDER_COLS,
                            buildFilterColNames()));
        }
        searchField.repaint();
    }

    private String buildFilterColNames() {
        List<String> names = new ArrayList<>();
        for (int ci : filterCols) {
            if (ci < currentColumns.length) {
                names.add(currentColumns[ci]);
                if (names.size() >= 3) break;
            }
        }
        String joined = String.join(", ", names);
        return (names.size() >= 3 && filterCols.size() > 3) ? joined + "..." : joined;
    }

    // ── 表格 ─────────────────────────────────────────────────────────────
    private JComponent buildTableArea() {
        initTableModel();
        initTableWidget();
        JScrollPane scroll = buildTableScroll();
        emptyLabel = buildEmptyLabel();
        return buildLayeredPane(scroll, emptyLabel);
    }

    private void initTableModel() {
        tableModel = new DefaultTableModel(currentColumns, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
    }

    private void initTableWidget() {
        table = new JTable(tableModel) {
            @Override
            public boolean getScrollableTracksViewportWidth() {
                return getPreferredSize().width < getParent().getWidth();
            }
        };
        table.setFillsViewportHeight(true);
        table.setRowHeight(26);
        table.setFont(FontsUtil.getDefaultFontWithOffset(Font.PLAIN, -1));
        table.getTableHeader().setFont(FontsUtil.getDefaultFontWithOffset(Font.BOLD, -1));
        table.setShowHorizontalLines(true);
        table.setShowVerticalLines(false);
        table.setIntercellSpacing(new Dimension(0, 1));
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
        table.setDefaultRenderer(Object.class, new HoverRenderer());
        table.getTableHeader().addMouseListener(buildHeaderMouseListener());
        table.addMouseMotionListener(buildTableMotionListener());
        table.addMouseListener(buildTableMouseListener());
        // 双击单元格 → 大内容弹出详情对话框
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e)) {
                    int row = table.rowAtPoint(e.getPoint());
                    int col = table.columnAtPoint(e.getPoint());
                    if (row >= 0 && col >= 0) {
                        Object val = table.getValueAt(row, col);
                        String text = val == null ? "" : val.toString();
                        if (text.length() > LARGE_CELL_THRESHOLD) {
                            String colName = table.getColumnName(col);
                            showCellDetailDialog(colName, text);
                        }
                    }
                }
            }
        });
    }

    private JScrollPane buildTableScroll() {
        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        return scroll;
    }

    private JLabel buildEmptyLabel() {
        JLabel label = new JLabel("", SwingConstants.CENTER);
        label.setFont(label.getFont().deriveFont(Font.PLAIN, 13f));
        label.setForeground(UIManager.getColor(LABEL_DISABLED));
        label.setOpaque(false);
        label.setVisible(false);
        return label;
    }

    private static JLayeredPane buildLayeredPane(JScrollPane scroll, JLabel overlay) {
        JLayeredPane layered = new JLayeredPane() {
            @Override
            public void doLayout() {
                for (Component c : getComponents()) c.setBounds(0, 0, getWidth(), getHeight());
            }
        };
        layered.add(scroll, JLayeredPane.DEFAULT_LAYER);
        layered.add(overlay, JLayeredPane.PALETTE_LAYER);
        return layered;
    }

    private MouseAdapter buildHeaderMouseListener() {
        return new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int col = table.columnAtPoint(e.getPoint());
                if (col < 0) return;
                if (e.getClickCount() == 2 && isOnColumnResizeBorder(e)) {
                    autoResizeSingleColumn(col);
                    return;
                }
                if (e.getClickCount() == 1) {
                    if (sortCol == col) sortAsc = !sortAsc;
                    else {
                        sortCol = col;
                        sortAsc = true;
                    }
                    applyFilterAndSort();
                    updateSortHeaders();
                }
            }
        };
    }

    private MouseMotionAdapter buildTableMotionListener() {
        return new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                int row = table.rowAtPoint(e.getPoint());
                if (row != hoveredRow) {
                    hoveredRow = row;
                    table.repaint();
                }
            }
        };
    }

    private MouseAdapter buildTableMouseListener() {
        return new MouseAdapter() {
            @Override
            public void mouseExited(MouseEvent e) {
                if (hoveredRow != -1) {
                    hoveredRow = -1;
                    table.repaint();
                }
            }

            @Override
            public void mousePressed(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    int row = table.rowAtPoint(e.getPoint());
                    if (row >= 0) table.setRowSelectionInterval(row, row);
                    showContextMenu(e);
                }
            }
        };
    }

    // ── 分页栏 ────────────────────────────────────────────────────────────
    private JPanel buildPagination() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 3));
        bar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0,
                        UIManager.getColor(SEPARATOR_FG)),
                BorderFactory.createEmptyBorder(2, 4, 2, 4)));

        JComboBox<String> pageSizeCombo = new JComboBox<>(SIZE_LABELS);
        pageSizeCombo.setPreferredSize(new Dimension(65, 24));
        pageSizeCombo.setFont(pageSizeCombo.getFont().deriveFont(Font.PLAIN, 11f));
        pageSizeCombo.addActionListener(e -> {
            pageSize = PAGE_SIZES[pageSizeCombo.getSelectedIndex()];
            currentPage = 0;
            refreshView();
        });

        btnPrev = navBtn("‹");
        btnNext = navBtn("›");
        btnPrev.addActionListener(e -> {
            if (currentPage > 0) {
                currentPage--;
                refreshView();
            }
        });
        btnNext.addActionListener(e -> {
            if (currentPage < totalPages() - 1) {
                currentPage++;
                refreshView();
            }
        });

        pageInfoLabel = new JLabel("0 / 0");
        pageInfoLabel.setFont(pageInfoLabel.getFont().deriveFont(Font.PLAIN, 11f));
        pageInfoLabel.setPreferredSize(new Dimension(120, 20));
        pageInfoLabel.setHorizontalAlignment(SwingConstants.CENTER);

        JTextField pageJumpField = new JTextField(3);
        pageJumpField.setFont(pageJumpField.getFont().deriveFont(Font.PLAIN, 11f));
        pageJumpField.setHorizontalAlignment(SwingConstants.CENTER);
        pageJumpField.setPreferredSize(new Dimension(40, 24));
        pageJumpField.addActionListener(e -> {
            try {
                int pg = Integer.parseInt(pageJumpField.getText().trim()) - 1;
                if (pg >= 0 && pg < totalPages()) {
                    currentPage = pg;
                    refreshView();
                }
            } catch (NumberFormatException ex) {
                log.debug("invalid page jump input");
            }
            pageJumpField.setText("");
        });

        bar.add(tiny(I18nUtil.getMessage(MessageKeys.TABLE_PAGE_SIZE_LABEL)));
        bar.add(pageSizeCombo);
        bar.add(btnPrev);
        bar.add(pageInfoLabel);
        bar.add(btnNext);
        bar.add(tiny(I18nUtil.getMessage(MessageKeys.TABLE_PAGE_JUMP_LABEL)));
        bar.add(pageJumpField);
        return bar;
    }

    // ═══════════════════ 核心逻辑 ════════════════════════════════════════

    private void onSearch() {
        filterText = searchField.getText().trim();
        if (!caseSensitive) filterText = filterText.toLowerCase();
        currentPage = 0;
        applyFilterAndSort();
    }

    private void applyFilterAndSort() {
        filteredRows.clear();
        for (Object[] row : allRows) {
            if (matches(row)) filteredRows.add(row);
        }
        searchField.setNoResult(!filterText.isEmpty() && filteredRows.isEmpty());

        if (sortCol >= 0 && sortCol < tableModel.getColumnCount()) {
            final int col = sortCol;
            final boolean asc = sortAsc;
            filteredRows.sort((x, y) -> cmp(
                    col < x.length ? x[col] : null,
                    col < y.length ? y[col] : null, asc));
        }

        updateHintLabel();
        refreshView();
    }

    private void refreshView() {
        tableModel.setRowCount(0);
        int total = filteredRows.size();
        int pages = totalPages();
        if (currentPage >= pages) currentPage = Math.max(0, pages - 1);

        int from = getPageOffset();
        int to = (pageSize == 0) ? total : Math.min(from + pageSize, total);
        for (int i = from; i < to; i++) {
            tableModel.addRow(filteredRows.get(i));
        }

        updatePaginationControls();
        updateEmptyState();
        SwingUtilities.invokeLater(this::autoResizeColumns);
    }

    private void updatePaginationControls() {
        int total = filteredRows.size();
        int pages = totalPages();
        String info;
        if (total == 0) {
            info = I18nUtil.getMessage(MessageKeys.TABLE_PAGE_INFO_EMPTY);
        } else {
            int from = getPageOffset() + 1;
            int to = (pageSize == 0) ? total : Math.min(getPageOffset() + pageSize, total);
            info = I18nUtil.getMessage(MessageKeys.TABLE_PAGE_INFO,
                    String.valueOf(from),
                    String.valueOf(to),
                    String.valueOf(currentPage + 1),
                    String.valueOf(pages));
        }
        pageInfoLabel.setText(info);
        btnPrev.setEnabled(currentPage > 0);
        btnNext.setEnabled(currentPage < pages - 1);
    }

    /**
     * 更新顶部行数提示（过滤时显示过滤比例）
     */
    private void updateHintLabel() {
        int total = allRows.size();
        int filtered = filteredRows.size();
        String suffix = I18nUtil.getMessage(MessageKeys.TABLE_ROWS_SUFFIX);
        if (!filterText.isEmpty() && filtered != total) {
            hintLabel.setText(I18nUtil.getMessage(MessageKeys.TABLE_ROWS_FILTERED,
                    String.valueOf(filtered), String.valueOf(total)));
        } else {
            hintLabel.setText(total + suffix);
        }
    }

    /**
     * 更新空状态覆盖层文字和可见性
     */
    private void updateEmptyState() {
        boolean hasData = !filteredRows.isEmpty();
        boolean hasFilter = !filterText.isEmpty();
        if (hasData) {
            emptyLabel.setVisible(false);
        } else if (hasFilter) {
            emptyLabel.setText(I18nUtil.getMessage(MessageKeys.TABLE_EMPTY_NO_MATCH));
            emptyLabel.setVisible(true);
        } else if (allRows.isEmpty()) {
            emptyLabel.setText(I18nUtil.getMessage(MessageKeys.TABLE_EMPTY_NO_DATA));
            emptyLabel.setVisible(true);
        } else {
            emptyLabel.setVisible(false);
        }
    }

    // ...existing code...

    private void showContextMenu(MouseEvent e) {
        JPopupMenu menu = new JPopupMenu();
        int r = table.getSelectedRow();
        int c = table.getSelectedColumn();

        // "查看单元格内容" 仅在单元格内容超过阈值时显示
        if (r >= 0 && c >= 0) {
            Object val = table.getValueAt(r, c);
            String text = val == null ? "" : val.toString();
            if (text.length() > LARGE_CELL_THRESHOLD) {
                JMenuItem viewCell = new JMenuItem(I18nUtil.getMessage(MessageKeys.TABLE_CONTEXT_VIEW_CELL));
                viewCell.addActionListener(ev -> showCellDetailDialog(table.getColumnName(c), text));
                menu.add(viewCell);
                menu.addSeparator();
            }
        }

        JMenuItem copyCell = new JMenuItem(I18nUtil.getMessage(MessageKeys.TABLE_CONTEXT_COPY_CELL));
        copyCell.addActionListener(ev -> {
            copySelectedCell();
            NotificationUtil.showSuccess(I18nUtil.getMessage(MessageKeys.TABLE_CONTEXT_COPIED));
        });
        JMenuItem copyRow = new JMenuItem(I18nUtil.getMessage(MessageKeys.TABLE_CONTEXT_COPY_ROW));
        copyRow.addActionListener(ev -> {
            copySelectedRow();
            NotificationUtil.showSuccess(I18nUtil.getMessage(MessageKeys.TABLE_CONTEXT_COPIED));
        });
        menu.add(copyCell);
        menu.add(copyRow);
        menu.show(table, e.getX(), e.getY());
    }

    private void copySelectedCell() {
        int r = table.getSelectedRow();
        int c = table.getSelectedColumn();
        if (r >= 0 && c >= 0) {
            Object v = table.getValueAt(r, c);
            clip(v == null ? "" : v.toString());
        }
    }

    private void copySelectedRow() {
        int r = table.getSelectedRow();
        if (r < 0) return;
        StringBuilder sb = new StringBuilder();
        for (int c = 0; c < table.getColumnCount(); c++) {
            if (c > 0) sb.append('\t');
            Object v = table.getValueAt(r, c);
            if (v != null) sb.append(v);
        }
        clip(sb.toString());
    }

    // ═══════════════════ 工具方法 ════════════════════════════════════════

    private int totalPages() {
        if (pageSize == 0) return 1;
        int n = filteredRows.size();
        return n == 0 ? 1 : (int) Math.ceil((double) n / pageSize);
    }

    private int getPageOffset() {
        return pageSize == 0 ? 0 : currentPage * pageSize;
    }

    private boolean matches(Object[] row) {
        if (filterText.isEmpty()) return true;
        if (filterCols == null) return matchesAnyCell(row);
        return matchesSelectedCols(row);
    }

    private boolean matchesAnyCell(Object[] row) {
        for (Object cell : row) {
            if (cellContains(cell)) return true;
        }
        return false;
    }

    private boolean matchesSelectedCols(Object[] row) {
        for (int ci : filterCols) {
            if (ci < row.length && cellContains(row[ci])) return true;
        }
        return false;
    }

    private boolean cellContains(Object cell) {
        if (cell == null) return false;
        String text = caseSensitive ? cell.toString() : cell.toString().toLowerCase();
        if (wholeWord) {
            for (String word : filterText.split("\\s+")) {
                if (text.startsWith(word) || text.endsWith(word) ||
                        text.contains(" " + word) || text.contains(word + " ")) {
                    return true;
                }
            }
            return false;
        }
        return text.contains(filterText);
    }

    /**
     * 在列头上追加排序箭头指示
     */
    private void updateSortHeaders() {
        TableColumnModel cm = table.getColumnModel();
        for (int i = 0; i < cm.getColumnCount(); i++) {
            TableColumn tc = cm.getColumn(i);
            String name = tableModel.getColumnName(i);
            if (i == sortCol) {
                tc.setHeaderValue(name + (sortAsc ? " ▲" : " ▼"));
            } else {
                tc.setHeaderValue(name);
            }
        }
        table.getTableHeader().repaint();
    }

    /**
     * 自动适配所有列宽
     */
    private void autoResizeColumns() {
        if (table.getColumnModel().getColumnCount() == 0) return;
        for (int col = 0; col < table.getColumnModel().getColumnCount(); col++) {
            table.getColumnModel().getColumn(col).setPreferredWidth(calcColWidth(col));
        }
    }

    /**
     * 计算单列合适宽度（扫描最多 100 行，上限 320px）
     */
    private int calcColWidth(int col) {
        TableCellRenderer hr = table.getTableHeader().getDefaultRenderer();
        Component hc = hr.getTableCellRendererComponent(
                table, table.getColumnName(col), false, false, -1, col);
        int w = hc.getPreferredSize().width + 20;
        int rowCount = Math.min(table.getRowCount(), 100);
        for (int row = 0; row < rowCount; row++) {
            Component cc = table.prepareRenderer(table.getCellRenderer(row, col), row, col);
            w = Math.max(w, cc.getPreferredSize().width + 16);
        }
        return Math.min(w, 320);
    }

    /**
     * 双击列分隔线时自适应该列宽
     */
    private void autoResizeSingleColumn(int col) {
        int count = table.getColumnModel().getColumnCount();
        if (col >= 0 && col < count) {
            table.getColumnModel().getColumn(col).setPreferredWidth(calcColWidth(col));
        }
    }

    /**
     * 判断鼠标是否在列分隔线上（距分隔线 3px 内）
     */
    private boolean isOnColumnResizeBorder(MouseEvent e) {
        int x = e.getX();
        TableColumnModel cm = table.getColumnModel();
        int cumWidth = 0;
        for (int i = 0; i < cm.getColumnCount(); i++) {
            cumWidth += cm.getColumn(i).getWidth();
            if (Math.abs(x - cumWidth) <= 3) return true;
        }
        return false;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static int cmp(Object a, Object b, boolean asc) {
        if (a == null && b == null) return 0;
        if (a == null) return asc ? -1 : 1;
        if (b == null) return asc ? 1 : -1;
        int r;
        if (a instanceof Number na && b instanceof Number nb) {
            r = Double.compare(na.doubleValue(), nb.doubleValue());
        } else if (a instanceof Comparable ca) {
            try {
                r = ca.compareTo(b);
            } catch (ClassCastException ex) {
                r = a.toString().compareToIgnoreCase(b.toString());
            }
        } else {
            r = a.toString().compareToIgnoreCase(b.toString());
        }
        return asc ? r : -r;
    }

    private static void clip(String s) {
        Toolkit.getDefaultToolkit().getSystemClipboard()
                .setContents(new StringSelection(s), null);
    }

    private static JButton navBtn(String t) {
        JButton b = new JButton(t);
        b.setFont(b.getFont().deriveFont(Font.PLAIN, 14f));
        b.setPreferredSize(new Dimension(28, 24));
        b.setFocusPainted(false);
        b.setMargin(new Insets(0, 2, 0, 2));
        return b;
    }

    private static JLabel tiny(String t) {
        JLabel l = new JLabel(t);
        l.setFont(l.getFont().deriveFont(Font.PLAIN, 11f));
        return l;
    }

    // ═══════════════════ 单元格详情对话框 ═════════════════════════════════

    /**
     * 弹出单元格详情对话框（支持语法高亮、JSON 格式化、Cmd/Ctrl+F 搜索、复制）
     */
    private void showCellDetailDialog(String colName, String text) {
        Window owner = SwingUtilities.getWindowAncestor(this);
        JDialog dialog = (owner instanceof Frame f)
                ? new JDialog(f, I18nUtil.getMessage(MessageKeys.TABLE_CELL_DETAIL_TITLE) + " — " + colName, true)
                : new JDialog((Dialog) owner, I18nUtil.getMessage(MessageKeys.TABLE_CELL_DETAIL_TITLE) + " — " + colName, true);
        dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        dialog.setLayout(new BorderLayout(0, 0));

        // ── RSyntaxTextArea ─────────────────────────────────────────────
        RSyntaxTextArea textArea = new RSyntaxTextArea();
        textArea.setEditable(false);
        textArea.setCodeFoldingEnabled(true);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        textArea.setHighlightCurrentLine(false);
        EditorThemeUtil.loadTheme(textArea);
        textArea.setFont(FontsUtil.getDefaultFont(Font.PLAIN));

        // 判断是否是 JSON
        boolean isJson = JsonUtil.isTypeJSON(text);
        textArea.setSyntaxEditingStyle(isJson
                ? SyntaxConstants.SYNTAX_STYLE_JSON
                : SyntaxConstants.SYNTAX_STYLE_NONE);

        // ── SearchableTextArea 包装（仅搜索，禁用替换）──────────────────
        SearchableTextArea searchableTextArea = new SearchableTextArea(textArea, false);

        // ── 顶部信息栏 ──────────────────────────────────────────────────
        JLabel lenLabel = new JLabel(I18nUtil.getMessage(MessageKeys.TABLE_CELL_DETAIL_LENGTH,
                String.valueOf(text.length())));
        lenLabel.setForeground(UIManager.getColor(LABEL_DISABLED));
        lenLabel.setFont(lenLabel.getFont().deriveFont(Font.PLAIN, 11f));
        lenLabel.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));

        // ── 底部按钮栏 ──────────────────────────────────────────────────
        JButton btnFormatJson = new JButton(I18nUtil.getMessage(MessageKeys.TABLE_CELL_DETAIL_FORMAT_JSON));
        JButton btnCopy = new JButton(I18nUtil.getMessage(MessageKeys.TABLE_CELL_DETAIL_COPY));
        JButton btnClose = new JButton(I18nUtil.getMessage(MessageKeys.TABLE_CELL_DETAIL_CLOSE));

        btnFormatJson.setEnabled(isJson);
        if (!isJson) btnFormatJson.setToolTipText("Content is not valid JSON");

        // 格式化 / 还原 JSON 切换
        final boolean[] formatted = {false};
        btnFormatJson.addActionListener(ev -> {
            if (!formatted[0]) {
                try {
                    String pretty = JsonUtil.toJsonPrettyStr(text);
                    textArea.setText(pretty);
                    textArea.setCaretPosition(0);
                    formatted[0] = true;
                    btnFormatJson.setText("⟲ " + I18nUtil.getMessage(MessageKeys.TABLE_CELL_DETAIL_FORMAT_JSON));
                } catch (Exception ex) {
                    log.warn("JSON format failed", ex);
                }
            } else {
                textArea.setText(text);
                textArea.setCaretPosition(0);
                formatted[0] = false;
                btnFormatJson.setText(I18nUtil.getMessage(MessageKeys.TABLE_CELL_DETAIL_FORMAT_JSON));
            }
        });

        btnCopy.addActionListener(ev -> {
            String selected = textArea.getSelectedText();
            clip(selected != null && !selected.isEmpty() ? selected : textArea.getText());
            NotificationUtil.showSuccess(I18nUtil.getMessage(MessageKeys.TABLE_CONTEXT_COPIED));
        });
        btnClose.addActionListener(ev -> dialog.dispose());

        // JSON 自动格式化展示
        if (isJson) {
            try {
                String pretty = JsonUtil.toJsonPrettyStr(text);
                textArea.setText(pretty);
                textArea.setCaretPosition(0);
                formatted[0] = true;
                btnFormatJson.setText("⟲ " + I18nUtil.getMessage(MessageKeys.TABLE_CELL_DETAIL_FORMAT_JSON));
            } catch (Exception ex) {
                log.warn("auto JSON format failed", ex);
                textArea.setText(text);
                textArea.setCaretPosition(0);
            }
        } else {
            textArea.setText(text);
            textArea.setCaretPosition(0);
        }

        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 4));
        footer.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, UIManager.getColor(SEPARATOR_FG)),
                BorderFactory.createEmptyBorder(2, 4, 2, 4)));
        footer.add(btnFormatJson);
        footer.add(btnCopy);
        footer.add(btnClose);

        dialog.add(lenLabel, BorderLayout.NORTH);
        dialog.add(searchableTextArea, BorderLayout.CENTER);
        dialog.add(footer, BorderLayout.SOUTH);

        // Escape 关闭
        dialog.getRootPane().registerKeyboardAction(
                ev -> dialog.dispose(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW);

        dialog.setSize(720, 500);
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    // ═══════════════════ Renderer ═════════════════════════════════════════

    private class HoverRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(
                JTable t, Object value, boolean sel, boolean focus, int row, int col) {
            // 对大内容进行截断展示
            String display = value == null ? "" : value.toString();
            String tooltip = null;
            if (display.length() > LARGE_CELL_THRESHOLD) {
                tooltip = "<html><body style='width:380px;font-family:monospace;font-size:11px;white-space:pre-wrap'>"
                        + escapeHtml(display.length() > TOOLTIP_MAX_LEN
                        ? display.substring(0, TOOLTIP_MAX_LEN) + "…" : display)
                        + "<br><br><i style='color:gray'>"
                        + I18nUtil.getMessage(MessageKeys.TABLE_CELL_DETAIL_LENGTH, String.valueOf(display.length()))
                        + " — double-click to view</i></body></html>";
                display = display.substring(0, LARGE_CELL_THRESHOLD) + " …";
            }
            super.getTableCellRendererComponent(t, display, sel, focus, row, col);
            setToolTipText(tooltip);
            setBorder(BorderFactory.createEmptyBorder(0, 6, 0, 6));
            if (!sel) {
                if (row == hoveredRow) {
                    Color hv = UIManager.getColor("Table.hoverBackground");
                    setBackground(hv != null ? hv
                            : UIManager.getColor("Table.selectionBackground").brighter());
                } else {
                    Color alt = UIManager.getColor("Table.alternateRowColor");
                    setBackground(row % 2 == 0 || alt == null
                            ? UIManager.getColor("Table.background") : alt);
                }
            }
            return this;
        }
    }

    /**
     * 转义 HTML 特殊字符，用于 tooltip
     */
    private static String escapeHtml(String text) {
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("\n", "<br>")
                .replace(" ", "&nbsp;");
    }
}

