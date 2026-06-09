package com.laker.postman.panel.performance.config;

import com.laker.postman.common.component.CsvDataPanel;
import com.laker.postman.common.component.ToolWindowActionToolbar;
import com.laker.postman.common.component.ToolWindowSurfaceStyle;
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
import javax.swing.table.TableColumn;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CsvDataSetPropertyPanel extends JPanel {
    private static final int PREVIEW_VISIBLE_ROW_LIMIT = 12;
    private static final int PREVIEW_MIN_COLUMN_WIDTH = 140;
    private static final int PREVIEW_MIN_HEIGHT = 180;
    private static final int PREVIEW_VIEWPORT_PADDING = 18;
    private static final String BODY_EMPTY = "empty";
    private static final String BODY_PREVIEW = "preview";

    private final CsvDataPanel csvDataPanel = new CsvDataPanel();
    private final JLabel statusLabel = new JLabel();
    private final JButton manageButton;
    private final JButton clearButton;
    private final JTable previewTable;
    private final JScrollPane previewScrollPane;
    private final DefaultTableModel previewModel;
    private final JPanel bodyPanel;
    private final CardLayout bodyCardLayout;
    private PerformanceTreeNode currentNode;
    @Setter
    private Runnable changeListener;
    private boolean restoring;

    public CsvDataSetPropertyPanel() {
        setLayout(new BorderLayout(0, 12));
        ToolWindowSurfaceStyle.applyCard(this);
        setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));

        csvDataPanel.setContextHelpText(I18nUtil.getMessage(MessageKeys.PERFORMANCE_CSV_DATA_SET_SCOPE_NOTE));
        csvDataPanel.setChangeListener(this::handleCsvDataChanged);

        JPanel contentPanel = new JPanel();
        contentPanel.setOpaque(false);
        contentPanel.setLayout(new BorderLayout(0, 10));

        JPanel headerPanel = new JPanel(new BorderLayout(0, 6));
        headerPanel.setOpaque(false);

        JPanel topRow = new JPanel(new BorderLayout(12, 0));
        topRow.setOpaque(false);

        JPanel titlePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        titlePanel.setOpaque(false);
        JLabel titleLabel = createTitleLabel();
        titlePanel.add(titleLabel);
        statusLabel.setFont(FontsUtil.getDefaultFontWithOffset(Font.PLAIN, -1));
        statusLabel.setForeground(ModernColors.getTextSecondary());
        titlePanel.add(statusLabel);
        topRow.add(titlePanel, BorderLayout.WEST);

        JButton importButton = createActionButton(
                MessageKeys.CSV_ACTION_IMPORT,
                MessageKeys.CSV_MENU_IMPORT_FILE,
                "icons/import.svg",
                true);
        importButton.addActionListener(e -> csvDataPanel.importCsvFile());

        JButton manualButton = createActionButton(
                MessageKeys.CSV_ACTION_CREATE,
                MessageKeys.CSV_MENU_CREATE_MANUAL,
                "icons/plus.svg",
                false);
        manualButton.addActionListener(e -> csvDataPanel.showManualCreateDialog());

        manageButton = createActionButton(
                MessageKeys.CSV_ACTION_MANAGE,
                MessageKeys.CSV_MENU_MANAGE_DATA,
                "icons/code.svg",
                false);
        manageButton.addActionListener(e -> csvDataPanel.showCsvDataManageDialog());

        clearButton = createActionButton(
                MessageKeys.CSV_ACTION_CLEAR,
                MessageKeys.CSV_MENU_CLEAR_DATA,
                "icons/clear.svg",
                false);
        clearButton.addActionListener(e -> csvDataPanel.clearCsvData());
        JPanel actionPanel = ToolWindowActionToolbar.inlineRight(
                importButton,
                manualButton,
                manageButton,
                clearButton
        );

        topRow.add(actionPanel, BorderLayout.EAST);
        headerPanel.add(topRow, BorderLayout.NORTH);

        contentPanel.add(headerPanel, BorderLayout.NORTH);

        previewModel = new DefaultTableModel() {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        previewTable = new JTable(previewModel);
        CsvDataPanel.configureCsvTableAppearance(previewTable);

        previewScrollPane = new JScrollPane(previewTable,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        ToolWindowSurfaceStyle.applyTableScrollPaneCard(previewScrollPane, previewTable);
        previewScrollPane.setPreferredSize(new Dimension(0, PREVIEW_MIN_HEIGHT));
        previewScrollPane.getViewport().addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                updatePreviewColumnWidths();
            }
        });

        bodyCardLayout = new CardLayout();
        bodyPanel = new JPanel(bodyCardLayout);
        bodyPanel.setOpaque(false);
        bodyPanel.add(createEmptyStatePanel(), BODY_EMPTY);
        bodyPanel.add(previewScrollPane, BODY_PREVIEW);
        contentPanel.add(bodyPanel, BorderLayout.CENTER);

        add(contentPanel, BorderLayout.CENTER);
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

    private JButton createActionButton(String messageKey, String tooltipKey, String iconPath, boolean primary) {
        String text = I18nUtil.getMessage(messageKey);
        JButton button = ModernButtonFactory.createButton(text, primary, iconPath, 16);
        button.setHorizontalAlignment(SwingConstants.CENTER);
        button.setToolTipText(I18nUtil.getMessage(tooltipKey));

        int width = button.getFontMetrics(button.getFont()).stringWidth(text) + 54;
        button.setPreferredSize(new Dimension(Math.max(82, width), 32));
        button.setMinimumSize(new Dimension(76, 32));
        return button;
    }

    private JLabel createTitleLabel() {
        JLabel titleLabel = new JLabel(I18nUtil.getMessage(MessageKeys.PERFORMANCE_CSV_DATA_SET_TITLE));
        titleLabel.setIcon(IconUtil.createThemed("icons/csv.svg", 18, 18));
        titleLabel.setFont(FontsUtil.getDefaultFontWithOffset(Font.BOLD, 1));
        return titleLabel;
    }

    private JPanel createEmptyStatePanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        ToolWindowSurfaceStyle.applyCard(panel);
        panel.setBorder(BorderFactory.createEmptyBorder(28, 24, 28, 24));

        JPanel content = new JPanel();
        content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));

        JLabel titleLabel = new JLabel(I18nUtil.getMessage(MessageKeys.CSV_STATUS_NO_DATA));
        titleLabel.setIcon(IconUtil.createThemed("icons/warning.svg", 18, 18));
        titleLabel.setFont(FontsUtil.getDefaultFontWithOffset(Font.BOLD, 1));
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        content.add(titleLabel);

        content.add(Box.createVerticalStrut(8));

        JLabel scopeLabel = new JLabel("<html><body style='width:560px;text-align:center'>"
                + I18nUtil.getMessage(MessageKeys.PERFORMANCE_CSV_DATA_SET_SCOPE_NOTE)
                + "</body></html>");
        scopeLabel.setFont(FontsUtil.getDefaultFontWithOffset(Font.PLAIN, -1));
        scopeLabel.setForeground(ModernColors.getTextSecondary());
        scopeLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        content.add(scopeLabel);

        content.add(Box.createVerticalStrut(16));

        JButton importButton = createActionButton(
                MessageKeys.CSV_ACTION_IMPORT,
                MessageKeys.CSV_MENU_IMPORT_FILE,
                "icons/import.svg",
                true);
        importButton.addActionListener(e -> csvDataPanel.importCsvFile());
        JButton manualButton = createActionButton(
                MessageKeys.CSV_ACTION_CREATE,
                MessageKeys.CSV_MENU_CREATE_MANUAL,
                "icons/plus.svg",
                false);
        manualButton.addActionListener(e -> csvDataPanel.showManualCreateDialog());
        JPanel actions = ToolWindowActionToolbar.inlineLeft(importButton, manualButton);
        actions.setAlignmentX(Component.CENTER_ALIGNMENT);
        content.add(actions);

        panel.add(content);
        return panel;
    }

    private void refreshStateView() {
        CsvDataPanel.CsvState state = csvDataPanel.exportState();
        boolean hasData = state != null && !state.getRows().isEmpty();

        manageButton.setEnabled(hasData);
        clearButton.setEnabled(hasData);

        if (!hasData) {
            statusLabel.setIcon(IconUtil.createThemed("icons/warning.svg", 16, 16));
            statusLabel.setText(I18nUtil.getMessage(MessageKeys.CSV_STATUS_NO_DATA));
            statusLabel.setForeground(ModernColors.getTextHint());
            previewModel.setColumnCount(0);
            previewModel.setRowCount(0);
            bodyCardLayout.show(bodyPanel, BODY_EMPTY);
            revalidate();
            repaint();
            return;
        }

        List<String> headers = resolveHeaders(state);
        List<Map<String, String>> rows = state.getRows();
        statusLabel.setIcon(IconUtil.createThemed("icons/check.svg", 16, 16));
        statusLabel.setText(I18nUtil.getMessage(MessageKeys.CSV_STATUS_LOADED, rows.size()));
        statusLabel.setForeground(ModernColors.getTextSecondary());

        previewModel.setRowCount(0);
        previewModel.setColumnIdentifiers(headers.toArray());
        for (Map<String, String> row : rows) {
            Object[] values = new Object[headers.size()];
            for (int col = 0; col < headers.size(); col++) {
                values[col] = row.getOrDefault(headers.get(col), "");
            }
            previewModel.addRow(values);
        }
        updatePreviewHeight(Math.min(rows.size(), PREVIEW_VISIBLE_ROW_LIMIT));
        updatePreviewColumnWidths();
        SwingUtilities.invokeLater(this::updatePreviewColumnWidths);
        bodyCardLayout.show(bodyPanel, BODY_PREVIEW);

        revalidate();
        repaint();
    }

    private void updatePreviewHeight(int visibleRowCount) {
        int headerHeight = previewTable.getTableHeader().getPreferredSize().height;
        int contentHeight = headerHeight + visibleRowCount * previewTable.getRowHeight() + 8;
        int height = Math.max(PREVIEW_MIN_HEIGHT, contentHeight);
        previewScrollPane.setPreferredSize(new Dimension(0, height));
    }

    private void updatePreviewColumnWidths() {
        int columnCount = previewTable.getColumnModel().getColumnCount();
        if (columnCount <= 0) {
            return;
        }

        int viewportWidth = previewScrollPane.getViewport().getExtentSize().width;
        if (viewportWidth <= 0) {
            viewportWidth = previewScrollPane.getViewport().getWidth();
        }
        if (viewportWidth <= 0) {
            viewportWidth = previewScrollPane.getWidth();
        }

        int usableViewportWidth = Math.max(viewportWidth - PREVIEW_VIEWPORT_PADDING, 0);
        int minTotalWidth = PREVIEW_MIN_COLUMN_WIDTH * columnCount;
        if (usableViewportWidth >= minTotalWidth) {
            previewTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
            int baseWidth = usableViewportWidth / columnCount;
            int remainder = usableViewportWidth % columnCount;
            for (int i = 0; i < columnCount; i++) {
                TableColumn column = previewTable.getColumnModel().getColumn(i);
                column.setPreferredWidth(baseWidth + (i < remainder ? 1 : 0));
                column.setMinWidth(PREVIEW_MIN_COLUMN_WIDTH);
            }
        } else {
            previewTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
            for (int i = 0; i < columnCount; i++) {
                TableColumn column = previewTable.getColumnModel().getColumn(i);
                column.setPreferredWidth(PREVIEW_MIN_COLUMN_WIDTH);
                column.setMinWidth(PREVIEW_MIN_COLUMN_WIDTH);
            }
        }
    }

    private static List<String> resolveHeaders(CsvDataPanel.CsvState state) {
        List<String> headers = state.getHeaders();
        if (!headers.isEmpty()) {
            return headers;
        }

        List<Map<String, String>> rows = state.getRows();
        return rows.isEmpty() ? List.of() : new ArrayList<>(rows.get(0).keySet());
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
