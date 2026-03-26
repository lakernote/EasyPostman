package com.laker.postman.panel.performance;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.laker.postman.common.SingletonFactory;
import com.laker.postman.common.component.CsvDataPanel;
import com.laker.postman.common.component.MemoryLabel;
import com.laker.postman.common.component.button.RefreshButton;
import com.laker.postman.common.component.button.StartButton;
import com.laker.postman.common.component.button.StopButton;
import com.laker.postman.common.constants.ModernColors;
import com.laker.postman.model.RequestItemProtocolEnum;
import com.laker.postman.panel.collections.right.request.RequestEditSubPanel;
import com.laker.postman.panel.performance.assertion.AssertionPropertyPanel;
import com.laker.postman.panel.performance.component.JMeterTreeCellRenderer;
import com.laker.postman.panel.performance.component.TreeNodeTransferHandler;
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
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeSelectionModel;
import java.awt.*;
import java.util.function.Consumer;

final class PerformancePanelViewFactory {

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

    ResultSection createResultSection() {
        JTabbedPane resultTabbedPane = new JTabbedPane();
        PerformanceResultTablePanel performanceResultTablePanel = new PerformanceResultTablePanel();
        PerformanceTrendPanel performanceTrendPanel = SingletonFactory.getInstance(PerformanceTrendPanel.class);
        PerformanceReportPanel performanceReportPanel = new PerformanceReportPanel();

        resultTabbedPane.addTab(I18nUtil.getMessage(MessageKeys.PERFORMANCE_TAB_TREND), performanceTrendPanel);
        resultTabbedPane.addTab(I18nUtil.getMessage(MessageKeys.PERFORMANCE_TAB_REPORT), performanceReportPanel);
        resultTabbedPane.addTab(I18nUtil.getMessage(MessageKeys.PERFORMANCE_TAB_RESULT_TREE), performanceResultTablePanel);

        return new ResultSection(
                resultTabbedPane,
                performanceResultTablePanel,
                performanceTrendPanel,
                performanceReportPanel
        );
    }

    ToolbarSection createToolbarSection(Component parentComponent,
                                        boolean efficientMode,
                                        PerformancePersistenceService persistenceService,
                                        Runnable refreshRequestsAction,
                                        Consumer<Boolean> efficientModeSetterAction,
                                        Runnable saveAllPropertyPanelDataAction,
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

        JCheckBox efficientCheckBox = new JCheckBox(I18nUtil.getMessage(MessageKeys.PERFORMANCE_EFFICIENT_MODE));
        efficientCheckBox.setSelected(efficientMode);
        efficientCheckBox.setFont(FontsUtil.getDefaultFont(Font.BOLD));
        efficientCheckBox.setForeground(new Color(0, 128, 0));
        String htmlTooltip = "<html><body style='width: 400px; padding: 10px;'>" +
                "<b style='color: #008000; font-size: 13px;'>" + I18nUtil.getMessage(MessageKeys.PERFORMANCE_EFFICIENT_MODE) + "</b><br><br>" +
                I18nUtil.getMessage(MessageKeys.PERFORMANCE_EFFICIENT_MODE_TOOLTIP_HTML) +
                "</body></html>";
        efficientCheckBox.setToolTipText(htmlTooltip);
        efficientCheckBox.addActionListener(e -> {
            if (!efficientCheckBox.isSelected()) {
                int result = JOptionPane.showConfirmDialog(
                        parentComponent,
                        I18nUtil.getMessage(MessageKeys.PERFORMANCE_EFFICIENT_MODE_DISABLE_WARNING),
                        I18nUtil.getMessage(MessageKeys.PERFORMANCE_EFFICIENT_MODE_WARNING_TITLE),
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
        });
        btnPanel.add(efficientCheckBox);

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

        return new ToolbarSection(topPanel, runBtn, stopBtn, refreshBtn, efficientCheckBox, csvDataPanel, progressLabel);
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

    record ResultSection(JTabbedPane resultTabbedPane,
                         PerformanceResultTablePanel performanceResultTablePanel,
                         PerformanceTrendPanel performanceTrendPanel,
                         PerformanceReportPanel performanceReportPanel) {
    }

    record ToolbarSection(JPanel topPanel,
                          StartButton runBtn,
                          StopButton stopBtn,
                          RefreshButton refreshBtn,
                          JCheckBox efficientCheckBox,
                          CsvDataPanel csvDataPanel,
                          JLabel progressLabel) {
    }

    private record RequestEditorSection(JPanel wrapperPanel, JPanel requestEditorHost) {
    }
}
