package com.laker.postman.panel.performance.config;

import com.laker.postman.common.component.CsvDataPanel;
import com.laker.postman.common.component.button.ModernButtonFactory;
import com.laker.postman.common.constants.ModernColors;
import com.laker.postman.performance.model.PerformanceTreeNode;
import com.laker.postman.panel.performance.tree.PerformanceTreeNodeTitleFormatter;
import com.laker.postman.performance.core.config.CsvDataSetData;
import com.laker.postman.util.FontsUtil;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.IconUtil;
import com.laker.postman.util.MessageKeys;
import lombok.Setter;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CsvDataSetPropertyPanel extends JPanel {
    private static final int PREVIEW_ROW_LIMIT = 8;
    private static final int PREVIEW_COLUMN_WIDTH = 140;
    private static final int PREVIEW_MIN_HEIGHT = 72;
    private static final int PREVIEW_MAX_HEIGHT = 220;

    private final CsvDataPanel csvDataPanel = new CsvDataPanel();
    private final JLabel statusLabel = new JLabel();
    private final JLabel sourceLabel = new JLabel();
    private final JButton manageButton;
    private final JButton clearButton;
    private final JTable previewTable;
    private final JScrollPane previewScrollPane;
    private final DefaultTableModel previewModel;
    private PerformanceTreeNode currentNode;
    @Setter
    private Runnable changeListener;
    private boolean restoring;

    public CsvDataSetPropertyPanel() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));

        csvDataPanel.setContextHelpText(I18nUtil.getMessage(MessageKeys.PERFORMANCE_CSV_DATA_SET_SCOPE_NOTE));
        csvDataPanel.setChangeListener(this::handleCsvDataChanged);

        JPanel contentPanel = new JPanel();
        contentPanel.setOpaque(false);
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));

        JPanel headerPanel = new JPanel(new BorderLayout(12, 0));
        headerPanel.setOpaque(false);
        headerPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel titlePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        titlePanel.setOpaque(false);
        JLabel titleLabel = new JLabel(I18nUtil.getMessage(MessageKeys.PERFORMANCE_CSV_DATA_SET_TITLE));
        titleLabel.setIcon(IconUtil.createThemed("icons/csv.svg", 18, 18));
        titleLabel.setFont(FontsUtil.getDefaultFontWithOffset(Font.BOLD, 1));
        titlePanel.add(titleLabel);

        statusLabel.setFont(FontsUtil.getDefaultFontWithOffset(Font.PLAIN, -1));
        statusLabel.setForeground(ModernColors.getTextSecondary());
        titlePanel.add(statusLabel);
        headerPanel.add(titlePanel, BorderLayout.WEST);

        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actionPanel.setOpaque(false);

        JButton importButton = createActionButton(MessageKeys.CSV_MENU_IMPORT_FILE, "icons/import.svg", true);
        importButton.addActionListener(e -> csvDataPanel.importCsvFile());
        actionPanel.add(importButton);

        JButton manualButton = createActionButton(MessageKeys.CSV_MENU_CREATE_MANUAL, "icons/plus.svg", false);
        manualButton.addActionListener(e -> csvDataPanel.showManualCreateDialog());
        actionPanel.add(manualButton);

        manageButton = createActionButton(MessageKeys.CSV_MENU_MANAGE_DATA, "icons/code.svg", false);
        manageButton.addActionListener(e -> csvDataPanel.showCsvDataManageDialog());
        actionPanel.add(manageButton);

        clearButton = createActionButton(MessageKeys.CSV_MENU_CLEAR_DATA, "icons/clear.svg", false);
        clearButton.addActionListener(e -> csvDataPanel.clearCsvData());
        actionPanel.add(clearButton);

        headerPanel.add(actionPanel, BorderLayout.EAST);
        contentPanel.add(headerPanel);

        contentPanel.add(Box.createVerticalStrut(8));

        sourceLabel.setFont(FontsUtil.getDefaultFontWithOffset(Font.PLAIN, -1));
        sourceLabel.setForeground(ModernColors.getTextSecondary());
        sourceLabel.setToolTipText(I18nUtil.getMessage(MessageKeys.PERFORMANCE_CSV_DATA_SET_SCOPE_NOTE));
        sourceLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        contentPanel.add(sourceLabel);

        contentPanel.add(Box.createVerticalStrut(10));

        previewModel = new DefaultTableModel() {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        previewTable = new JTable(previewModel);
        previewTable.setFont(FontsUtil.getDefaultFont(Font.PLAIN));
        previewTable.setRowHeight(26);
        previewTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        previewTable.setFillsViewportHeight(true);
        previewTable.setShowVerticalLines(false);
        previewTable.setGridColor(ModernColors.getBorderLightColor());

        JTableHeader header = previewTable.getTableHeader();
        header.setFont(FontsUtil.getDefaultFontWithOffset(Font.BOLD, -1));
        header.setReorderingAllowed(false);

        previewScrollPane = new JScrollPane(previewTable,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        previewScrollPane.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0,
                ModernColors.getDividerBorderColor()));
        previewScrollPane.setPreferredSize(new Dimension(0, PREVIEW_MIN_HEIGHT));
        previewScrollPane.setAlignmentX(Component.LEFT_ALIGNMENT);
        contentPanel.add(previewScrollPane);

        add(contentPanel, BorderLayout.NORTH);
        refreshStateView();
    }

    public void setNode(PerformanceTreeNode node) {
        currentNode = node;
        restoring = true;
        try {
            csvDataPanel.restoreState(toCsvState(node != null ? node.csvDataSetData : null));
        } finally {
            restoring = false;
        }
        refreshStateView();
    }

    public void saveCsvDataSetData() {
        if (currentNode == null) {
            return;
        }
        currentNode.csvDataSetData = fromCsvState(csvDataPanel.exportState());
        currentNode.name = PerformanceTreeNodeTitleFormatter.csvDataSetTitle(currentNode.csvDataSetData);
    }

    private void handleCsvDataChanged() {
        if (restoring) {
            return;
        }
        saveCsvDataSetData();
        refreshStateView();
        if (changeListener != null) {
            changeListener.run();
        }
    }

    private JButton createActionButton(String messageKey, String iconPath, boolean primary) {
        String text = I18nUtil.getMessage(messageKey);
        JButton button = ModernButtonFactory.createButton(text, primary, iconPath, 16);
        button.setHorizontalAlignment(SwingConstants.CENTER);

        int width = button.getFontMetrics(button.getFont()).stringWidth(text) + 54;
        button.setPreferredSize(new Dimension(Math.max(96, width), 32));
        button.setMinimumSize(new Dimension(84, 32));
        return button;
    }

    private void refreshStateView() {
        CsvDataPanel.CsvState state = csvDataPanel.exportState();
        boolean hasData = state != null && !state.getRows().isEmpty();

        manageButton.setEnabled(hasData);
        clearButton.setEnabled(hasData);
        previewScrollPane.setVisible(hasData);

        if (!hasData) {
            statusLabel.setIcon(IconUtil.createThemed("icons/warning.svg", 16, 16));
            statusLabel.setText(I18nUtil.getMessage(MessageKeys.CSV_STATUS_NO_DATA));
            statusLabel.setForeground(ModernColors.getTextHint());
            sourceLabel.setText(I18nUtil.getMessage(MessageKeys.PERFORMANCE_CSV_DATA_SET_SCOPE_NOTE));
            previewModel.setColumnCount(0);
            previewModel.setRowCount(0);
            revalidate();
            repaint();
            return;
        }

        List<String> headers = resolveHeaders(state);
        List<Map<String, String>> rows = state.getRows();
        statusLabel.setIcon(IconUtil.createThemed("icons/check.svg", 16, 16));
        statusLabel.setText(I18nUtil.getMessage(MessageKeys.CSV_STATUS_LOADED, rows.size()));
        statusLabel.setForeground(ModernColors.getTextSecondary());
        sourceLabel.setText(I18nUtil.getMessage(MessageKeys.CSV_DATA_SOURCE_INFO,
                resolveSourceDisplayName(state),
                rows.size()));

        previewModel.setRowCount(0);
        previewModel.setColumnIdentifiers(headers.toArray());
        int rowLimit = Math.min(rows.size(), PREVIEW_ROW_LIMIT);
        for (int i = 0; i < rowLimit; i++) {
            Map<String, String> row = rows.get(i);
            Object[] values = new Object[headers.size()];
            for (int col = 0; col < headers.size(); col++) {
                values[col] = row.getOrDefault(headers.get(col), "");
            }
            previewModel.addRow(values);
        }
        for (int i = 0; i < previewTable.getColumnModel().getColumnCount(); i++) {
            TableColumn column = previewTable.getColumnModel().getColumn(i);
            column.setPreferredWidth(PREVIEW_COLUMN_WIDTH);
        }
        updatePreviewHeight(rowLimit);

        revalidate();
        repaint();
    }

    private void updatePreviewHeight(int visibleRowCount) {
        int headerHeight = previewTable.getTableHeader().getPreferredSize().height;
        int contentHeight = headerHeight + visibleRowCount * previewTable.getRowHeight() + 8;
        int height = Math.max(PREVIEW_MIN_HEIGHT, Math.min(PREVIEW_MAX_HEIGHT, contentHeight));
        previewScrollPane.setPreferredSize(new Dimension(0, height));
    }

    private static List<String> resolveHeaders(CsvDataPanel.CsvState state) {
        List<String> headers = state.getHeaders();
        if (!headers.isEmpty()) {
            return headers;
        }

        List<Map<String, String>> rows = state.getRows();
        return rows.isEmpty() ? List.of() : new ArrayList<>(rows.get(0).keySet());
    }

    private static String resolveSourceDisplayName(CsvDataPanel.CsvState state) {
        String sourceName = state.getSourceName();
        return sourceName == null || sourceName.isBlank()
                ? I18nUtil.getMessage(MessageKeys.CSV_MANUAL_CREATED)
                : sourceName;
    }

    private static CsvDataPanel.CsvState toCsvState(CsvDataSetData data) {
        if (data == null || !data.hasRows()) {
            return null;
        }
        return new CsvDataPanel.CsvState(data.getSourceName(), data.getHeaders(), data.getRows());
    }

    private static CsvDataSetData fromCsvState(CsvDataPanel.CsvState state) {
        if (state == null || state.getRows().isEmpty()) {
            return null;
        }
        return new CsvDataSetData(state.getSourceName(), state.getHeaders(), state.getRows());
    }
}
