package com.laker.postman.panel.performance;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.laker.postman.common.SingletonFactory;
import com.laker.postman.common.component.CsvDataPanel;
import com.laker.postman.common.component.MemoryLabel;
import com.laker.postman.common.component.button.RefreshButton;
import com.laker.postman.common.component.button.SegmentedButtonGroupPanel;
import com.laker.postman.common.component.button.SegmentedToggleButton;
import com.laker.postman.common.component.button.StartButton;
import com.laker.postman.common.component.button.StopButton;
import com.laker.postman.common.constants.ModernColors;
import com.laker.postman.model.RequestItemProtocolEnum;
import com.laker.postman.panel.collections.right.request.RequestEditSubPanel;
import com.laker.postman.panel.performance.assertion.AssertionPropertyPanel;
import com.laker.postman.panel.performance.component.JMeterTreeCellRenderer;
import com.laker.postman.panel.performance.component.TreeNodeTransferHandler;
import com.laker.postman.panel.performance.controller.LoopPropertyPanel;
import com.laker.postman.panel.performance.result.PerformanceReportPanel;
import com.laker.postman.panel.performance.result.PerformanceResultTablePanel;
import com.laker.postman.panel.performance.result.PerformanceTrendPanel;
import com.laker.postman.panel.performance.threadgroup.ThreadGroupPropertyPanel;
import com.laker.postman.panel.performance.timer.TimerPropertyPanel;
import com.laker.postman.service.PerformancePersistenceService;
import com.laker.postman.util.FontsUtil;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.IconUtil;
import com.laker.postman.util.MessageKeys;

import javax.swing.*;
import javax.swing.plaf.basic.BasicTabbedPaneUI;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeSelectionModel;
import java.awt.*;
import java.util.function.Consumer;

final class PerformancePanelViewFactory {
    static final int RESULT_TAB_TREND = 0;
    static final int RESULT_TAB_REPORT = 1;
    static final int RESULT_TAB_TABLE = 2;
    private static final String RESULT_CONTEXT_TABLE = "table";
    private static final String RESULT_CONTEXT_REPORT = "report";
    private static final String RESULT_CONTEXT_TREND = "trend";

    TreeSection createTreeSection(DefaultTreeModel treeModel) {
        JTree jmeterTree = new JTree(treeModel);
        jmeterTree.setRootVisible(true);
        jmeterTree.setShowsRootHandles(true);
        jmeterTree.setCellRenderer(new JMeterTreeCellRenderer());
        jmeterTree.getSelectionModel().setSelectionMode(TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION);
        jmeterTree.setDragEnabled(true);
        jmeterTree.setDropMode(DropMode.ON_OR_INSERT);
        jmeterTree.setTransferHandler(new TreeNodeTransferHandler(jmeterTree, treeModel));

        JScrollPane treeScroll = new JScrollPane(jmeterTree);
        treeScroll.setPreferredSize(new Dimension(260, 300));
        return new TreeSection(jmeterTree, treeScroll);
    }

    PropertySection createPropertySection(Runnable refreshCurrentRequestAction,
                                          String emptyCard,
                                          String threadGroupCard,
                                          String loopCard,
                                          String requestCard,
                                          String assertionCard,
                                          String timerCard,
                                          String sseConnectCard,
                                          String sseAwaitCard,
                                          String wsConnectCard,
                                          String wsSendCard,
                                          String wsAwaitCard,
                                          String wsCloseCard) {
        CardLayout propertyCardLayout = new CardLayout();
        JPanel propertyPanel = new JPanel(propertyCardLayout);
        propertyPanel.add(new JLabel(I18nUtil.getMessage(MessageKeys.PERFORMANCE_PROPERTY_SELECT_NODE)), emptyCard);

        ThreadGroupPropertyPanel threadGroupPanel = new ThreadGroupPropertyPanel();
        propertyPanel.add(threadGroupPanel, threadGroupCard);

        LoopPropertyPanel loopPanel = new LoopPropertyPanel();
        propertyPanel.add(loopPanel, loopCard);

        RequestEditSubPanel requestEditSubPanel = new RequestEditSubPanel("", RequestItemProtocolEnum.HTTP, true);
        RequestEditorSection requestEditorSection = createRequestEditorSection(requestEditSubPanel, refreshCurrentRequestAction);
        propertyPanel.add(requestEditorSection.wrapperPanel(), requestCard);

        AssertionPropertyPanel assertionPanel = new AssertionPropertyPanel();
        propertyPanel.add(assertionPanel, assertionCard);
        TimerPropertyPanel timerPanel = new TimerPropertyPanel();
        propertyPanel.add(timerPanel, timerCard);

        SseStagePropertyPanel sseConnectPanel = new SseStagePropertyPanel(SseStagePropertyPanel.Stage.CONNECT);
        propertyPanel.add(sseConnectPanel, sseConnectCard);
        SseStagePropertyPanel sseAwaitPanel = new SseStagePropertyPanel(SseStagePropertyPanel.Stage.AWAIT);
        propertyPanel.add(sseAwaitPanel, sseAwaitCard);

        WebSocketStagePropertyPanel wsConnectPanel = new WebSocketStagePropertyPanel(WebSocketStagePropertyPanel.Stage.CONNECT);
        propertyPanel.add(wsConnectPanel, wsConnectCard);
        WebSocketStagePropertyPanel wsSendPanel = new WebSocketStagePropertyPanel(WebSocketStagePropertyPanel.Stage.SEND);
        propertyPanel.add(wsSendPanel, wsSendCard);
        WebSocketStagePropertyPanel wsAwaitPanel = new WebSocketStagePropertyPanel(WebSocketStagePropertyPanel.Stage.AWAIT);
        propertyPanel.add(wsAwaitPanel, wsAwaitCard);
        WebSocketStagePropertyPanel wsClosePanel = new WebSocketStagePropertyPanel(WebSocketStagePropertyPanel.Stage.CLOSE);
        propertyPanel.add(wsClosePanel, wsCloseCard);

        propertyCardLayout.show(propertyPanel, emptyCard);
        return new PropertySection(
                propertyPanel,
                propertyCardLayout,
                threadGroupPanel,
                loopPanel,
                assertionPanel,
                timerPanel,
                sseConnectPanel,
                sseAwaitPanel,
                wsConnectPanel,
                wsSendPanel,
                wsAwaitPanel,
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
        PerformanceTrendPanel performanceTrendPanel = SingletonFactory.getInstance(PerformanceTrendPanel.class);
        PerformanceReportPanel performanceReportPanel = new PerformanceReportPanel();

        resultTabbedPane.addTab(I18nUtil.getMessage(MessageKeys.PERFORMANCE_TAB_TREND), performanceTrendPanel);
        resultTabbedPane.addTab(I18nUtil.getMessage(MessageKeys.PERFORMANCE_TAB_REPORT), performanceReportPanel);
        resultTabbedPane.addTab(I18nUtil.getMessage(MessageKeys.PERFORMANCE_TAB_RESULT_TREE), performanceResultTablePanel);
        resultTabbedPane.setSelectedIndex(RESULT_TAB_TREND);

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

    ToolbarSection createToolbarSection(
                                        PerformancePersistenceService persistenceService,
                                        Runnable refreshRequestsAction,
                                        Runnable saveConfigAction) {
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, ModernColors.getDividerBorderColor()));

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 3));
        StartButton runBtn = new StartButton();
        StopButton stopBtn = new StopButton();
        stopBtn.setEnabled(false);
        btnPanel.add(runBtn);
        btnPanel.add(stopBtn);

        RefreshButton refreshBtn = new RefreshButton();
        refreshBtn.addActionListener(e -> refreshRequestsAction.run());
        btnPanel.add(refreshBtn);

        CsvDataPanel csvDataPanel = new CsvDataPanel();
        csvDataPanel.setContextHelpText(I18nUtil.getMessage(MessageKeys.PERFORMANCE_CSV_USAGE_NOTE));
        csvDataPanel.setChangeListener(saveConfigAction);
        csvDataPanel.restoreState(persistenceService.loadCsvState());
        btnPanel.add(csvDataPanel);
        topPanel.add(btnPanel, BorderLayout.WEST);

        JPanel progressPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 5));
        JLabel progressLabel = new JLabel("0/0");
        progressLabel.setFont(progressLabel.getFont().deriveFont(Font.BOLD));
        progressLabel.setIcon(new FlatSVGIcon("icons/users.svg", 20, 20)
                .setColorFilter(new FlatSVGIcon.ColorFilter(color -> UIManager.getColor("Button.foreground"))));
        progressLabel.setHorizontalTextPosition(SwingConstants.RIGHT);
        progressPanel.setToolTipText(I18nUtil.getMessage(MessageKeys.PERFORMANCE_PROGRESS_TOOLTIP));
        progressPanel.add(progressLabel);
        progressPanel.add(new MemoryLabel());
        topPanel.add(progressPanel, BorderLayout.EAST);

        return new ToolbarSection(topPanel, runBtn, stopBtn, refreshBtn, csvDataPanel, progressLabel);
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
        toolbar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 1, 0, ModernColors.getDividerBorderColor()),
                BorderFactory.createEmptyBorder(5, 8, 5, 8)
        ));

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

        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        leftPanel.setOpaque(false);
        leftPanel.add(switcher);
        toolbar.add(leftPanel, BorderLayout.WEST);

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
        toolbar.add(contextCards, BorderLayout.EAST);

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
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        panel.setOpaque(false);
        panel.add(efficientCheckBox);
        return panel;
    }

    private JPanel createReportContextPanel(JComboBox<String> reportRefreshModeBox) {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        panel.setOpaque(false);
        JLabel label = new JLabel(I18nUtil.getMessage(MessageKeys.PERFORMANCE_REPORT_REFRESH_MODE));
        label.setFont(FontsUtil.getDefaultFontWithOffset(Font.PLAIN, -1));
        label.setForeground(ModernColors.getTextSecondary());
        panel.add(label);
        panel.add(reportRefreshModeBox);
        return panel;
    }

    private JPanel createTrendContextPanel(JCheckBox trendCheckBox) {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        panel.setOpaque(false);
        panel.add(trendCheckBox);
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

    private RequestEditorSection createRequestEditorSection(RequestEditSubPanel requestEditSubPanel,
                                                            Runnable refreshCurrentRequestAction) {
        JPanel wrapper = new JPanel(new BorderLayout());
        JPanel infoBar = new JPanel(new BorderLayout());
        infoBar.setBackground(new Color(255, 250, 205));
        infoBar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(230, 220, 170)),
                BorderFactory.createEmptyBorder(4, 4, 4, 4)
        ));

        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        leftPanel.setOpaque(false);
        leftPanel.add(new JLabel(IconUtil.create("icons/info.svg", 14, 14)));

        JLabel infoText = new JLabel(I18nUtil.getMessage(MessageKeys.PERFORMANCE_REQUEST_COPY_INFO));
        infoText.setFont(FontsUtil.getDefaultFontWithOffset(Font.PLAIN, -1));
        infoText.setForeground(new Color(102, 85, 0));
        leftPanel.add(infoText);

        JButton refreshCurrentBtn = new JButton();
        refreshCurrentBtn.setIcon(IconUtil.createThemed("icons/refresh.svg", 14, 14));
        refreshCurrentBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        refreshCurrentBtn.setFocusable(false);
        refreshCurrentBtn.addActionListener(e -> refreshCurrentRequestAction.run());
        leftPanel.add(refreshCurrentBtn);
        infoBar.add(leftPanel, BorderLayout.CENTER);

        wrapper.add(infoBar, BorderLayout.NORTH);
        JPanel requestEditorHost = new JPanel(new BorderLayout());
        requestEditorHost.add(requestEditSubPanel, BorderLayout.CENTER);
        wrapper.add(requestEditorHost, BorderLayout.CENTER);
        return new RequestEditorSection(wrapper, requestEditorHost);
    }

    record TreeSection(JTree tree, JScrollPane scrollPane) {
    }

    record PropertySection(JPanel propertyPanel,
                           CardLayout propertyCardLayout,
                           ThreadGroupPropertyPanel threadGroupPanel,
                           LoopPropertyPanel loopPanel,
                           AssertionPropertyPanel assertionPanel,
                           TimerPropertyPanel timerPanel,
                           SseStagePropertyPanel sseConnectPanel,
                           SseStagePropertyPanel sseAwaitPanel,
                           WebSocketStagePropertyPanel wsConnectPanel,
                           WebSocketStagePropertyPanel wsSendPanel,
                           WebSocketStagePropertyPanel wsAwaitPanel,
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
                         PerformanceTrendPanel performanceTrendPanel,
                         PerformanceReportPanel performanceReportPanel) {
    }

    record ToolbarSection(JPanel topPanel,
                          StartButton runBtn,
                          StopButton stopBtn,
                          RefreshButton refreshBtn,
                          CsvDataPanel csvDataPanel,
                          JLabel progressLabel) {
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
