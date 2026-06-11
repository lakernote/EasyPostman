package com.laker.postman.panel.performance;

import com.laker.postman.request.model.RequestItemProtocolEnum;


import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.laker.postman.common.component.MemoryLabel;
import com.laker.postman.common.component.ToolWindowActionToolbar;
import com.laker.postman.common.component.ToolWindowSurfaceStyle;
import com.laker.postman.common.component.button.CopyButton;
import com.laker.postman.common.component.button.EditButton;
import com.laker.postman.common.component.button.ExportButton;
import com.laker.postman.common.component.button.HelpButton;
import com.laker.postman.common.component.button.PlusButton;
import com.laker.postman.common.component.button.RefreshButton;
import com.laker.postman.common.component.button.SegmentedButtonGroupPanel;
import com.laker.postman.common.component.button.SegmentedToggleButton;
import com.laker.postman.common.component.button.StartButton;
import com.laker.postman.common.component.button.StopButton;
import com.laker.postman.common.constants.ModernColors;
import com.laker.postman.panel.collections.editor.request.RequestEditSubPanel;
import com.laker.postman.panel.performance.assertion.AssertionPropertyPanel;
import com.laker.postman.panel.performance.component.PerformanceTreeCellRenderer;
import com.laker.postman.panel.performance.component.TreeNodeTransferHandler;
import com.laker.postman.panel.performance.config.CsvDataSetPropertyPanel;
import com.laker.postman.panel.performance.controller.LoopPropertyPanel;
import com.laker.postman.panel.performance.extractor.ExtractorPropertyPanel;
import com.laker.postman.panel.performance.result.PerformanceReportPanel;
import com.laker.postman.panel.performance.result.PerformanceResultTablePanel;
import com.laker.postman.panel.performance.result.LazyPerformanceTrendPanel;
import com.laker.postman.panel.performance.result.PerformanceTrendView;
import com.laker.postman.panel.performance.threadgroup.ThreadGroupPropertyPanel;
import com.laker.postman.panel.performance.timer.TimerPropertyPanel;
import com.laker.postman.performance.core.worker.PerformanceWorkerEndpointParser;
import com.laker.postman.util.FontsUtil;
import com.laker.postman.util.IconUtil;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.plaf.basic.BasicTabbedPaneUI;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeSelectionModel;
import java.awt.*;
import java.util.function.Consumer;

final class PerformancePanelViewFactory {
    static final int RESULT_TAB_TREND = 0;
    static final int RESULT_TAB_REPORT = 1;
    static final int RESULT_TAB_TABLE = 2;
    private static final int TOOLBAR_CONTROL_HEIGHT = 28;
    private static final String RESULT_CONTEXT_TABLE = "table";
    private static final String RESULT_CONTEXT_REPORT = "report";
    private static final String RESULT_CONTEXT_TREND = "trend";

    TreeSection createTreeSection(DefaultTreeModel treeModel) {
        JTree performanceTree = new JTree(treeModel);
        performanceTree.setRootVisible(true);
        performanceTree.setShowsRootHandles(true);
        performanceTree.setCellRenderer(new PerformanceTreeCellRenderer());
        performanceTree.getSelectionModel().setSelectionMode(TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION);
        performanceTree.setDragEnabled(true);
        performanceTree.setDropMode(DropMode.ON_OR_INSERT);
        performanceTree.setTransferHandler(new TreeNodeTransferHandler(performanceTree, treeModel));

        JScrollPane treeScroll = new JScrollPane(performanceTree);
        treeScroll.setPreferredSize(new Dimension(260, 300));
        ToolWindowSurfaceStyle.applyTreeScrollPaneCard(treeScroll, performanceTree);
        return new TreeSection(performanceTree, treeScroll);
    }

    PropertySection createPropertySection(String emptyCard,
                                          String threadGroupCard,
                                          String csvDataSetCard,
                                          String loopCard,
                                          String requestCard,
                                          String assertionCard,
                                          String extractorCard,
                                          String timerCard,
                                          String sseConnectCard,
                                          String sseReadCard,
                                          String wsConnectCard,
                                          String wsSendCard,
                                          String wsReadCard,
                                          String wsCloseCard) {
        CardLayout propertyCardLayout = new CardLayout();
        JPanel propertyPanel = new JPanel(propertyCardLayout);
        ToolWindowSurfaceStyle.applyCard(propertyPanel);
        JLabel emptyLabel = new JLabel(I18nUtil.getMessage(MessageKeys.PERFORMANCE_PROPERTY_SELECT_NODE), SwingConstants.CENTER);
        emptyLabel.setForeground(ModernColors.getTextHint());
        propertyPanel.add(emptyLabel, emptyCard);

        ThreadGroupPropertyPanel threadGroupPanel = new ThreadGroupPropertyPanel();
        propertyPanel.add(threadGroupPanel, threadGroupCard);

        CsvDataSetPropertyPanel csvDataSetPanel = new CsvDataSetPropertyPanel();
        propertyPanel.add(csvDataSetPanel, csvDataSetCard);

        LoopPropertyPanel loopPanel = new LoopPropertyPanel();
        propertyPanel.add(loopPanel, loopCard);

        RequestEditSubPanel requestEditSubPanel = RequestEditSubPanel.performanceSnapshot("", RequestItemProtocolEnum.HTTP, true);
        RequestEditorSection requestEditorSection = createRequestEditorSection(requestEditSubPanel);
        propertyPanel.add(requestEditorSection.wrapperPanel(), requestCard);

        AssertionPropertyPanel assertionPanel = new AssertionPropertyPanel();
        propertyPanel.add(assertionPanel, assertionCard);
        ExtractorPropertyPanel extractorPanel = new ExtractorPropertyPanel();
        propertyPanel.add(extractorPanel, extractorCard);
        TimerPropertyPanel timerPanel = new TimerPropertyPanel();
        propertyPanel.add(timerPanel, timerCard);

        SseStagePropertyPanel sseConnectPanel = new SseStagePropertyPanel(SseStagePropertyPanel.Stage.CONNECT);
        propertyPanel.add(sseConnectPanel, sseConnectCard);
        SseStagePropertyPanel sseReadPanel = new SseStagePropertyPanel(SseStagePropertyPanel.Stage.READ);
        propertyPanel.add(sseReadPanel, sseReadCard);

        WebSocketStagePropertyPanel wsConnectPanel = new WebSocketStagePropertyPanel(WebSocketStagePropertyPanel.Stage.CONNECT);
        propertyPanel.add(wsConnectPanel, wsConnectCard);
        WebSocketStagePropertyPanel wsSendPanel = new WebSocketStagePropertyPanel(WebSocketStagePropertyPanel.Stage.SEND);
        propertyPanel.add(wsSendPanel, wsSendCard);
        WebSocketStagePropertyPanel wsReadPanel = new WebSocketStagePropertyPanel(WebSocketStagePropertyPanel.Stage.READ);
        propertyPanel.add(wsReadPanel, wsReadCard);
        WebSocketStagePropertyPanel wsClosePanel = new WebSocketStagePropertyPanel(WebSocketStagePropertyPanel.Stage.CLOSE);
        propertyPanel.add(wsClosePanel, wsCloseCard);

        propertyCardLayout.show(propertyPanel, emptyCard);
        return new PropertySection(
                propertyPanel,
                propertyCardLayout,
                threadGroupPanel,
                csvDataSetPanel,
                loopPanel,
                assertionPanel,
                extractorPanel,
                timerPanel,
                sseConnectPanel,
                sseReadPanel,
                wsConnectPanel,
                wsSendPanel,
                wsReadPanel,
                wsClosePanel,
                requestEditSubPanel,
                requestEditorSection.requestEditorHost()
        );
    }

    ResultSection createResultSection(boolean trendEnabled,
                                      boolean reportRealtimeEnabled,
                                      boolean efficientMode,
                                      Component parentComponent,
                                      Consumer<Boolean> efficientModeSetterAction,
                                      Consumer<Boolean> trendEnabledSetterAction,
                                      Consumer<Boolean> reportRefreshModeSetterAction,
                                      Runnable reportRefreshAction,
                                      Runnable saveAllPropertyPanelDataAction,
                                      Runnable saveConfigAction) {
        JTabbedPane resultTabbedPane = new HiddenResultTabsTabbedPane();
        PerformanceResultTablePanel performanceResultTablePanel = new PerformanceResultTablePanel();
        LazyPerformanceTrendPanel performanceTrendPanel = new LazyPerformanceTrendPanel();
        PerformanceReportPanel performanceReportPanel = new PerformanceReportPanel();

        resultTabbedPane.addTab(I18nUtil.getMessage(MessageKeys.PERFORMANCE_TAB_TREND), performanceTrendPanel);
        resultTabbedPane.addTab(I18nUtil.getMessage(MessageKeys.PERFORMANCE_TAB_REPORT), performanceReportPanel);
        resultTabbedPane.addTab(I18nUtil.getMessage(MessageKeys.PERFORMANCE_TAB_RESULT_TREE), performanceResultTablePanel);
        resultTabbedPane.setSelectedIndex(RESULT_TAB_TREND);
        ToolWindowSurfaceStyle.applyTabbedPaneCard(resultTabbedPane);

        ResultToolbar resultToolbar = createResultToolbar(
                resultTabbedPane,
                trendEnabled,
                reportRealtimeEnabled,
                efficientMode,
                parentComponent,
                efficientModeSetterAction,
                trendEnabledSetterAction,
                reportRefreshModeSetterAction,
                reportRefreshAction,
                saveAllPropertyPanelDataAction,
                saveConfigAction
        );

        JPanel resultPanel = new JPanel(new BorderLayout());
        ToolWindowSurfaceStyle.applyCard(resultPanel);
        resultPanel.add(resultToolbar.panel(), BorderLayout.NORTH);
        resultPanel.add(resultTabbedPane, BorderLayout.CENTER);

        return new ResultSection(
                resultPanel,
                resultTabbedPane,
                resultToolbar.resultTableButton(),
                resultToolbar.reportButton(),
                resultToolbar.trendButton(),
                resultToolbar.efficientCheckBox(),
                resultToolbar.trendCheckBox(),
                resultToolbar.reportRefreshModeBox(),
                performanceResultTablePanel,
                performanceTrendPanel,
                performanceReportPanel
        );
    }

    ToolbarSection createToolbarSection(Runnable exportRunPlanAction,
                                        Runnable usageHelpAction,
                                        Runnable refreshRequestsAction,
                                        boolean remoteExecutionEnabled,
                                        String workerEndpoints,
                                        Consumer<Boolean> remoteExecutionEnabledAction,
                                        Consumer<String> workerEndpointsAction) {
        JPanel topPanel = new JPanel(new MigLayout(
                "insets 6 10 6 10, fillx, novisualpadding, gap 0",
                "[]8[1!]10[]10[1!]10[280:380:520,fill]push[]",
                "[]"
        ));
        ToolWindowSurfaceStyle.applyCard(topPanel);
        topPanel.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));

        StartButton runBtn = new StartButton();
        StopButton stopBtn = new StopButton();
        stopBtn.setEnabled(false);

        ExportButton exportBtn = new ExportButton();
        exportBtn.addActionListener(e -> exportRunPlanAction.run());

        RefreshButton refreshBtn = new RefreshButton();
        refreshBtn.addActionListener(e -> refreshRequestsAction.run());

        HelpButton usageHelpBtn = new HelpButton();
        usageHelpBtn.setToolTipText(I18nUtil.getMessage(MessageKeys.PERFORMANCE_USAGE_HELP_TOOLTIP));
        usageHelpBtn.addActionListener(e -> usageHelpAction.run());

        JPanel btnPanel = ToolWindowActionToolbar.inlineLeft(runBtn, stopBtn, exportBtn, refreshBtn, usageHelpBtn);
        topPanel.add(btnPanel);
        topPanel.add(createToolbarSeparator());

        JPanel planPanel = createToolbarGroupPanel("[]4[140:160:180,fill]4[]2[]2[]2[]");
        JLabel planLabel = new JLabel(I18nUtil.getMessage(MessageKeys.PERFORMANCE_PLAN_LABEL));
        planLabel.setFont(FontsUtil.getDefaultFontWithOffset(Font.PLAIN, -1));
        planLabel.setForeground(ModernColors.getTextSecondary());
        JComboBox<String> planSelector = new JComboBox<>();
        planSelector.setFocusable(false);
        planSelector.setPreferredSize(new Dimension(160, TOOLBAR_CONTROL_HEIGHT));

        PlusButton addPlanButton = new PlusButton(IconUtil.SIZE_SMALL);
        addPlanButton.setToolTipText(I18nUtil.getMessage(MessageKeys.PERFORMANCE_PLAN_ADD_TOOLTIP));
        CopyButton duplicatePlanButton = new CopyButton();
        duplicatePlanButton.setToolTipText(I18nUtil.getMessage(MessageKeys.PERFORMANCE_PLAN_DUPLICATE_TOOLTIP));
        EditButton renamePlanButton = new EditButton(IconUtil.SIZE_SMALL);
        renamePlanButton.setToolTipText(I18nUtil.getMessage(MessageKeys.PERFORMANCE_PLAN_RENAME_TOOLTIP));
        JButton deletePlanButton = createDeletePlanButton();

        planPanel.add(planLabel);
        planPanel.add(planSelector);
        planPanel.add(addPlanButton);
        planPanel.add(duplicatePlanButton);
        planPanel.add(renamePlanButton);
        planPanel.add(deletePlanButton);
        topPanel.add(planPanel);
        topPanel.add(createToolbarSeparator());

        JPanel remotePanel = createToolbarGroupPanel("[]6[160:260:360,fill]6[]");
        JCheckBox remoteModeCheckBox = new JCheckBox(I18nUtil.getMessage(MessageKeys.PERFORMANCE_REMOTE_MODE));
        remoteModeCheckBox.setSelected(remoteExecutionEnabled);
        remoteModeCheckBox.setToolTipText(I18nUtil.getMessage(MessageKeys.PERFORMANCE_REMOTE_MODE_TOOLTIP));
        JTextField workerEndpointsField = new JTextField(workerEndpoints == null ? "" : workerEndpoints);
        workerEndpointsField.setPreferredSize(new Dimension(260, TOOLBAR_CONTROL_HEIGHT));
        workerEndpointsField.putClientProperty(
                FlatClientProperties.PLACEHOLDER_TEXT,
                I18nUtil.getMessage(MessageKeys.PERFORMANCE_REMOTE_WORKERS_PLACEHOLDER)
        );
        workerEndpointsField.setToolTipText(I18nUtil.getMessage(MessageKeys.PERFORMANCE_REMOTE_WORKERS_TOOLTIP));
        JLabel workerStatusLabel = createWorkerStatusLabel();
        remoteModeCheckBox.addActionListener(e -> {
            if (remoteExecutionEnabledAction != null) {
                remoteExecutionEnabledAction.accept(remoteModeCheckBox.isSelected());
            }
            syncWorkerEndpointsState(remoteModeCheckBox, workerEndpointsField, workerStatusLabel);
        });
        addWorkerEndpointsListener(
                workerEndpointsField,
                workerEndpointsAction,
                () -> syncWorkerEndpointsState(remoteModeCheckBox, workerEndpointsField, workerStatusLabel)
        );
        syncWorkerEndpointsState(remoteModeCheckBox, workerEndpointsField, workerStatusLabel);
        remotePanel.add(remoteModeCheckBox);
        remotePanel.add(workerEndpointsField);
        remotePanel.add(workerStatusLabel);
        topPanel.add(remotePanel);

        JPanel progressPanel = createToolbarGroupPanel("[]5[]5[]", ", hidemode 3");
        JLabel progressLabel = createRunStatusLabel("0/0", "icons/users.svg");
        JLabel limitLabel = createRunStatusLabel("", null);
        limitLabel.setVisible(false);
        MemoryLabel memoryLabel = new MemoryLabel(1000, true);
        memoryLabel.setFont(runStatusFont());
        memoryLabel.setToolTipText("JVM memory usage. Double-click to run GC.");
        progressPanel.setToolTipText(I18nUtil.getMessage(MessageKeys.PERFORMANCE_PROGRESS_TOOLTIP));
        progressPanel.add(progressLabel);
        progressPanel.add(limitLabel);
        progressPanel.add(memoryLabel);
        topPanel.add(progressPanel);

        return new ToolbarSection(
                topPanel,
                runBtn,
                stopBtn,
                exportBtn,
                refreshBtn,
                usageHelpBtn,
                planSelector,
                addPlanButton,
                duplicatePlanButton,
                renamePlanButton,
                deletePlanButton,
                remoteModeCheckBox,
                workerEndpointsField,
                progressLabel,
                limitLabel
        );
    }

    private JLabel createRunStatusLabel(String text, String iconPath) {
        JLabel label = new JLabel(text);
        label.setFont(runStatusFont());
        if (iconPath != null && !iconPath.isBlank()) {
            label.setIcon(new FlatSVGIcon(iconPath, 18, 18)
                    .setColorFilter(new FlatSVGIcon.ColorFilter(color -> ModernColors.getTextPrimary())));
        }
        label.setHorizontalTextPosition(SwingConstants.RIGHT);
        label.setIconTextGap(4);
        label.setHorizontalAlignment(SwingConstants.LEFT);
        return label;
    }

    private Font runStatusFont() {
        return FontsUtil.getMonospacedFontWithOffset(Font.BOLD, 0);
    }

    private JPanel createToolbarGroupPanel(String columnConstraints) {
        return createToolbarGroupPanel(columnConstraints, "");
    }

    private JPanel createToolbarGroupPanel(String columnConstraints, String layoutSuffix) {
        JPanel panel = new JPanel(new MigLayout(
                "insets 0, fillx, novisualpadding, gap 0" + layoutSuffix,
                columnConstraints,
                "[]"
        ));
        panel.setOpaque(false);
        return panel;
    }

    private JComponent createToolbarSeparator() {
        JSeparator separator = new JSeparator(SwingConstants.VERTICAL);
        separator.setForeground(ModernColors.getDividerBorderColor());
        separator.setPreferredSize(new Dimension(1, 22));
        return separator;
    }

    private JLabel createWorkerStatusLabel() {
        JLabel label = new JLabel();
        label.setFont(FontsUtil.getDefaultFontWithOffset(Font.PLAIN, -2));
        label.setHorizontalAlignment(SwingConstants.LEFT);
        return label;
    }

    private void syncWorkerEndpointsState(JCheckBox remoteModeCheckBox,
                                          JTextField workerEndpointsField,
                                          JLabel workerStatusLabel) {
        boolean remoteEnabled = remoteModeCheckBox.isSelected();
        workerEndpointsField.setEditable(remoteEnabled);
        workerEndpointsField.setForeground(remoteEnabled
                ? ModernColors.getTextPrimary()
                : ModernColors.getTextDisabled());
        workerEndpointsField.setBackground(remoteEnabled
                ? ModernColors.getInputBackgroundColor()
                : ModernColors.getCardBackgroundColor());
        workerEndpointsField.putClientProperty(FlatClientProperties.OUTLINE, null);
        if (!remoteEnabled) {
            workerStatusLabel.setText(I18nUtil.getMessage(MessageKeys.PERFORMANCE_REMOTE_WORKERS_STATUS_LOCAL));
            workerStatusLabel.setForeground(ModernColors.getTextHint());
            workerStatusLabel.setToolTipText(I18nUtil.getMessage(MessageKeys.PERFORMANCE_REMOTE_MODE_TOOLTIP));
            workerEndpointsField.setToolTipText(I18nUtil.getMessage(MessageKeys.PERFORMANCE_REMOTE_WORKERS_TOOLTIP));
            return;
        }

        String text = workerEndpointsField.getText();
        if (text == null || text.isBlank()) {
            workerStatusLabel.setText(I18nUtil.getMessage(MessageKeys.PERFORMANCE_REMOTE_WORKERS_STATUS_REQUIRED));
            workerStatusLabel.setForeground(ModernColors.getWarning());
            workerStatusLabel.setToolTipText(I18nUtil.getMessage(MessageKeys.PERFORMANCE_REMOTE_WORKERS_REQUIRED));
            workerEndpointsField.setToolTipText(I18nUtil.getMessage(MessageKeys.PERFORMANCE_REMOTE_WORKERS_REQUIRED));
            workerEndpointsField.putClientProperty(FlatClientProperties.OUTLINE, FlatClientProperties.OUTLINE_WARNING);
            return;
        }

        try {
            int workerCount = PerformanceWorkerEndpointParser.parse(text).size();
            workerStatusLabel.setText(I18nUtil.getMessage(MessageKeys.PERFORMANCE_REMOTE_WORKERS_STATUS_COUNT, workerCount));
            workerStatusLabel.setForeground(ModernColors.getSuccess());
            workerStatusLabel.setToolTipText(I18nUtil.getMessage(MessageKeys.PERFORMANCE_REMOTE_WORKERS_TOOLTIP));
            workerEndpointsField.setToolTipText(I18nUtil.getMessage(MessageKeys.PERFORMANCE_REMOTE_WORKERS_TOOLTIP));
        } catch (IllegalArgumentException ex) {
            workerStatusLabel.setText(I18nUtil.getMessage(MessageKeys.PERFORMANCE_REMOTE_WORKERS_STATUS_INVALID));
            workerStatusLabel.setForeground(ModernColors.getError());
            workerStatusLabel.setToolTipText(ex.getMessage());
            workerEndpointsField.setToolTipText(ex.getMessage());
            workerEndpointsField.putClientProperty(FlatClientProperties.OUTLINE, FlatClientProperties.OUTLINE_ERROR);
        }
    }

    private JButton createDeletePlanButton() {
        JButton button = new JButton(IconUtil.createThemed("icons/delete.svg", IconUtil.SIZE_SMALL, IconUtil.SIZE_SMALL));
        button.setToolTipText(I18nUtil.getMessage(MessageKeys.PERFORMANCE_PLAN_DELETE_TOOLTIP));
        button.setFocusable(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.putClientProperty(FlatClientProperties.BUTTON_TYPE, FlatClientProperties.BUTTON_TYPE_TOOLBAR_BUTTON);
        return button;
    }

    private void addWorkerEndpointsListener(JTextField workerEndpointsField,
                                            Consumer<String> workerEndpointsAction,
                                            Runnable afterUpdateAction) {
        if (workerEndpointsAction == null && afterUpdateAction == null) {
            return;
        }
        workerEndpointsField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                update();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                update();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                update();
            }

            private void update() {
                if (workerEndpointsAction != null) {
                    workerEndpointsAction.accept(workerEndpointsField.getText());
                }
                if (afterUpdateAction != null) {
                    afterUpdateAction.run();
                }
            }
        });
    }

    private ResultToolbar createResultToolbar(JTabbedPane resultTabbedPane,
                                              boolean trendEnabled,
                                              boolean reportRealtimeEnabled,
                                              boolean efficientMode,
                                              Component parentComponent,
                                              Consumer<Boolean> efficientModeSetterAction,
                                              Consumer<Boolean> trendEnabledSetterAction,
                                              Consumer<Boolean> reportRefreshModeSetterAction,
                                              Runnable reportRefreshAction,
                                              Runnable saveAllPropertyPanelDataAction,
                                              Runnable saveConfigAction) {
        JPanel toolbar = new JPanel(new BorderLayout(8, 0));
        ToolWindowSurfaceStyle.applyCard(toolbar);
        toolbar.setBorder(BorderFactory.createEmptyBorder(5, 8, 5, 8));

        JToggleButton resultTableButton = new SegmentedToggleButton(
                I18nUtil.getMessage(MessageKeys.PERFORMANCE_TAB_RESULT_TREE),
                true
        );
        resultTableButton.setToolTipText(I18nUtil.getMessage(MessageKeys.PERFORMANCE_RESULT_TABLE_TOOLTIP));
        JToggleButton reportButton = new SegmentedToggleButton(
                I18nUtil.getMessage(MessageKeys.PERFORMANCE_TAB_REPORT),
                false
        );
        reportButton.setToolTipText(I18nUtil.getMessage(MessageKeys.PERFORMANCE_RESULT_REPORT_TOOLTIP));
        JToggleButton trendButton = new SegmentedToggleButton(
                I18nUtil.getMessage(MessageKeys.PERFORMANCE_TAB_TREND),
                false
        );
        trendButton.setToolTipText(I18nUtil.getMessage(MessageKeys.PERFORMANCE_TREND_ENABLED_TOOLTIP));

        ButtonGroup viewGroup = new ButtonGroup();
        viewGroup.add(resultTableButton);
        viewGroup.add(reportButton);
        viewGroup.add(trendButton);

        JPanel switcher = new SegmentedButtonGroupPanel(FlowLayout.LEFT);
        switcher.add(trendButton);
        switcher.add(reportButton);
        switcher.add(resultTableButton);

        JCheckBox efficientCheckBox = createCompactDetailsCheckBox(
                parentComponent,
                efficientMode,
                efficientModeSetterAction,
                saveAllPropertyPanelDataAction,
                saveConfigAction
        );

        JCheckBox trendCheckBox = new JCheckBox(I18nUtil.getMessage(MessageKeys.PERFORMANCE_TREND_ENABLED));
        trendCheckBox.setSelected(trendEnabled);
        trendCheckBox.setToolTipText(I18nUtil.getMessage(MessageKeys.PERFORMANCE_TREND_ENABLED_TOOLTIP));
        trendCheckBox.addActionListener(e -> {
            boolean selected = trendCheckBox.isSelected();
            trendEnabledSetterAction.accept(selected);
            selectResultTab(resultTabbedPane, selected ? RESULT_TAB_TREND : RESULT_TAB_TABLE, reportRefreshAction);
            saveConfigAction.run();
        });

        JComboBox<String> reportRefreshModeBox = new JComboBox<>(new String[]{
                I18nUtil.getMessage(MessageKeys.PERFORMANCE_REPORT_REFRESH_END),
                I18nUtil.getMessage(MessageKeys.PERFORMANCE_REPORT_REFRESH_REALTIME)
        });
        reportRefreshModeBox.setSelectedIndex(reportRealtimeEnabled ? 1 : 0);
        reportRefreshModeBox.setToolTipText(I18nUtil.getMessage(MessageKeys.PERFORMANCE_REPORT_REFRESH_TOOLTIP));
        reportRefreshModeBox.setFocusable(false);
        reportRefreshModeBox.addActionListener(e -> {
            reportRefreshModeSetterAction.accept(reportRefreshModeBox.getSelectedIndex() == 1);
            selectResultTab(resultTabbedPane, RESULT_TAB_REPORT, reportRefreshAction);
            saveConfigAction.run();
        });

        JPanel contextCards = new JPanel(new CardLayout());
        contextCards.setOpaque(false);
        contextCards.add(createTableContextPanel(efficientCheckBox), RESULT_CONTEXT_TABLE);
        contextCards.add(createReportContextPanel(reportRefreshModeBox), RESULT_CONTEXT_REPORT);
        contextCards.add(createTrendContextPanel(trendCheckBox), RESULT_CONTEXT_TREND);
        toolbar.add(createResultToolbarLeftPanel(switcher, contextCards), BorderLayout.WEST);

        resultTableButton.addActionListener(e -> selectResultTab(resultTabbedPane, RESULT_TAB_TABLE, reportRefreshAction));
        reportButton.addActionListener(e -> selectResultTab(resultTabbedPane, RESULT_TAB_REPORT, reportRefreshAction));
        trendButton.addActionListener(e -> selectResultTab(resultTabbedPane, RESULT_TAB_TREND, reportRefreshAction));

        resultTabbedPane.addChangeListener(e -> {
            syncResultToolbarState(resultTabbedPane, resultTableButton, reportButton, trendButton, contextCards);
            if (resultTabbedPane.getSelectedIndex() == RESULT_TAB_REPORT && reportRefreshAction != null) {
                reportRefreshAction.run();
            }
        });
        syncResultToolbarState(resultTabbedPane, resultTableButton, reportButton, trendButton, contextCards);

        return new ResultToolbar(
                toolbar,
                resultTableButton,
                reportButton,
                trendButton,
                efficientCheckBox,
                trendCheckBox,
                reportRefreshModeBox
        );
    }

    private JCheckBox createCompactDetailsCheckBox(Component parentComponent,
                                                   boolean efficientMode,
                                                   Consumer<Boolean> efficientModeSetterAction,
                                                   Runnable saveAllPropertyPanelDataAction,
                                                   Runnable saveConfigAction) {
        JCheckBox efficientCheckBox = new JCheckBox(I18nUtil.getMessage(MessageKeys.PERFORMANCE_RESULT_DETAIL_COMPACT));
        efficientCheckBox.setSelected(efficientMode);
        efficientCheckBox.setToolTipText(I18nUtil.getMessage(MessageKeys.PERFORMANCE_RESULT_DETAIL_COMPACT_TOOLTIP));
        efficientCheckBox.addActionListener(e -> {
            if (!efficientCheckBox.isSelected()) {
                int result = JOptionPane.showConfirmDialog(
                        parentComponent,
                        I18nUtil.getMessage(MessageKeys.PERFORMANCE_RESULT_DETAIL_COMPACT_DISABLE_WARNING),
                        I18nUtil.getMessage(MessageKeys.PERFORMANCE_RESULT_DETAIL_COMPACT_WARNING_TITLE),
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.WARNING_MESSAGE
                );
                if (result != JOptionPane.YES_OPTION) {
                    efficientCheckBox.setSelected(true);
                    return;
                }
            }
            efficientModeSetterAction.accept(efficientCheckBox.isSelected());
            saveAllPropertyPanelDataAction.run();
            saveConfigAction.run();
        });
        return efficientCheckBox;
    }

    private JPanel createTableContextPanel(JCheckBox efficientCheckBox) {
        return ToolWindowActionToolbar.inlineRight(efficientCheckBox);
    }

    private JPanel createReportContextPanel(JComboBox<String> reportRefreshModeBox) {
        JLabel label = new JLabel(I18nUtil.getMessage(MessageKeys.PERFORMANCE_REPORT_REFRESH_MODE));
        label.setFont(FontsUtil.getDefaultFontWithOffset(Font.PLAIN, -1));
        label.setForeground(ModernColors.getTextSecondary());
        return ToolWindowActionToolbar.inlineRight(label, reportRefreshModeBox);
    }

    private JPanel createTrendContextPanel(JCheckBox trendCheckBox) {
        return ToolWindowActionToolbar.inlineRight(trendCheckBox);
    }

    private JPanel createResultToolbarLeftPanel(JPanel switcher, JPanel contextCards) {
        JPanel panel = new JPanel(new MigLayout(
                "insets 0, fillx, novisualpadding, gap 0",
                "[]10[]",
                "[]"
        ));
        panel.setOpaque(false);
        panel.add(switcher);
        panel.add(contextCards);
        return panel;
    }

    private void selectResultTab(JTabbedPane resultTabbedPane, int index, Runnable reportRefreshAction) {
        if (index < 0 || index >= resultTabbedPane.getTabCount()) {
            return;
        }
        if (index == resultTabbedPane.getSelectedIndex()) {
            if (index == RESULT_TAB_REPORT && reportRefreshAction != null) {
                reportRefreshAction.run();
            }
            return;
        }
        resultTabbedPane.setSelectedIndex(index);
    }

    private void syncResultToolbarState(JTabbedPane resultTabbedPane,
                                        JToggleButton resultTableButton,
                                        JToggleButton reportButton,
                                        JToggleButton trendButton,
                                        JPanel contextCards) {
        int selectedIndex = resultTabbedPane.getSelectedIndex();
        resultTableButton.setSelected(selectedIndex == RESULT_TAB_TABLE);
        reportButton.setSelected(selectedIndex == RESULT_TAB_REPORT);
        trendButton.setSelected(selectedIndex == RESULT_TAB_TREND);

        CardLayout contextLayout = (CardLayout) contextCards.getLayout();
        if (selectedIndex == RESULT_TAB_REPORT) {
            contextLayout.show(contextCards, RESULT_CONTEXT_REPORT);
        } else if (selectedIndex == RESULT_TAB_TREND) {
            contextLayout.show(contextCards, RESULT_CONTEXT_TREND);
        } else {
            contextLayout.show(contextCards, RESULT_CONTEXT_TABLE);
        }
    }

    private RequestEditorSection createRequestEditorSection(RequestEditSubPanel requestEditSubPanel) {
        JPanel wrapper = new JPanel(new BorderLayout());
        JPanel requestEditorHost = new JPanel(new BorderLayout());
        ToolWindowSurfaceStyle.applyCard(wrapper);
        ToolWindowSurfaceStyle.applyCard(requestEditorHost);
        requestEditorHost.add(requestEditSubPanel, BorderLayout.CENTER);
        wrapper.add(requestEditorHost, BorderLayout.CENTER);
        return new RequestEditorSection(wrapper, requestEditorHost);
    }

    record TreeSection(JTree tree, JScrollPane scrollPane) {
    }

    record PropertySection(JPanel propertyPanel,
                           CardLayout propertyCardLayout,
                           ThreadGroupPropertyPanel threadGroupPanel,
                           CsvDataSetPropertyPanel csvDataSetPanel,
                           LoopPropertyPanel loopPanel,
                           AssertionPropertyPanel assertionPanel,
                           ExtractorPropertyPanel extractorPanel,
                           TimerPropertyPanel timerPanel,
                           SseStagePropertyPanel sseConnectPanel,
                           SseStagePropertyPanel sseReadPanel,
                           WebSocketStagePropertyPanel wsConnectPanel,
                           WebSocketStagePropertyPanel wsSendPanel,
                           WebSocketStagePropertyPanel wsReadPanel,
                           WebSocketStagePropertyPanel wsClosePanel,
                           RequestEditSubPanel requestEditSubPanel,
                           JPanel requestEditorHost) {
    }

    record ResultSection(JPanel resultPanel,
                         JTabbedPane resultTabbedPane,
                         JToggleButton resultTableButton,
                         JToggleButton reportButton,
                         JToggleButton trendButton,
                         JCheckBox efficientCheckBox,
                         JCheckBox trendCheckBox,
                         JComboBox<String> reportRefreshModeBox,
                         PerformanceResultTablePanel performanceResultTablePanel,
                         PerformanceTrendView performanceTrendPanel,
                         PerformanceReportPanel performanceReportPanel) {
    }

    record ToolbarSection(JPanel topPanel,
                          StartButton runBtn,
                          StopButton stopBtn,
                          ExportButton exportBtn,
                          RefreshButton refreshBtn,
                          HelpButton usageHelpBtn,
                          JComboBox<String> planSelector,
                          JButton addPlanButton,
                          JButton duplicatePlanButton,
                          JButton renamePlanButton,
                          JButton deletePlanButton,
                          JCheckBox remoteModeCheckBox,
                          JTextField workerEndpointsField,
                          JLabel progressLabel,
                          JLabel limitLabel) {
    }

    private record ResultToolbar(JPanel panel,
                                 JToggleButton resultTableButton,
                                 JToggleButton reportButton,
                                 JToggleButton trendButton,
                                 JCheckBox efficientCheckBox,
                                 JCheckBox trendCheckBox,
                                 JComboBox<String> reportRefreshModeBox) {
    }

    private static final class HiddenResultTabsTabbedPane extends JTabbedPane {
        @Override
        public void updateUI() {
            setUI(new HiddenResultTabsUi());
        }
    }

    private static final class HiddenResultTabsUi extends BasicTabbedPaneUI {
        @Override
        protected int calculateTabAreaHeight(int tabPlacement, int horizRunCount, int maxTabHeight) {
            return 0;
        }

        @Override
        protected void paintTabArea(Graphics g, int tabPlacement, int selectedIndex) {
            // Result view switching is provided by the toolbar above this content pane.
        }

        @Override
        protected Insets getContentBorderInsets(int tabPlacement) {
            return new Insets(0, 0, 0, 0);
        }
    }

    private record RequestEditorSection(JPanel wrapperPanel, JPanel requestEditorHost) {
    }
}
