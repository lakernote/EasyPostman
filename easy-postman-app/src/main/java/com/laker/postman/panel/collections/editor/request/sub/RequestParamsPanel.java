package com.laker.postman.panel.collections.editor.request.sub;

import com.laker.postman.request.model.HttpParam;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * Params tab content with Postman-style Path Variables and Query Params sections.
 */
public class RequestParamsPanel extends JPanel {
    private static final int SECTION_GAP = 8;
    private static final int MIN_SECTION_HEIGHT = 88;

    private final EasyRequestParamsPanel pathVariablesPanel;
    private final EasyRequestParamsPanel queryParamsPanel;
    private final Component pathVariablesGap = Box.createVerticalStrut(SECTION_GAP);
    private final JPanel contentPanel = new JPanel(new GridBagLayout());

    public RequestParamsPanel(EasyRequestParamsPanel pathVariablesPanel,
                              EasyRequestParamsPanel queryParamsPanel) {
        super(new BorderLayout());
        this.pathVariablesPanel = pathVariablesPanel;
        this.queryParamsPanel = queryParamsPanel;
        setOpaque(false);
        buildLayout();
        installLayoutRefreshListeners();
        refreshPathVariablesVisibility();
    }

    public void setPathVariablesList(List<HttpParam> pathVariables) {
        pathVariablesPanel.setParamsList(pathVariables);
        refreshPathVariablesVisibility();
    }

    public List<HttpParam> getPathVariablesList() {
        return pathVariablesPanel.getParamsList();
    }

    public List<HttpParam> getPathVariablesListFromModel() {
        return pathVariablesPanel.getParamsListFromModel();
    }

    public void refreshPathVariablesVisibility() {
        boolean visible = hasContent(pathVariablesPanel.getParamsListFromModel());
        pathVariablesGap.setVisible(visible);
        pathVariablesPanel.setVisible(visible);
        refreshSectionHeights();
        revalidate();
        repaint();
    }

    public EasyRequestParamsPanel getPathVariablesPanel() {
        return pathVariablesPanel;
    }

    public EasyRequestParamsPanel getQueryParamsPanel() {
        return queryParamsPanel;
    }

    private boolean hasContent(List<HttpParam> pathVariables) {
        if (pathVariables == null || pathVariables.isEmpty()) {
            return false;
        }
        return pathVariables.stream()
                .anyMatch(pathVariable -> pathVariable != null
                        && pathVariable.getKey() != null
                        && !pathVariable.getKey().isBlank());
    }

    private void buildLayout() {
        contentPanel.setOpaque(false);

        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.weightx = 1;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.anchor = GridBagConstraints.NORTHWEST;

        constraints.gridy = 0;
        contentPanel.add(queryParamsPanel, constraints);

        constraints.gridy = 1;
        contentPanel.add(pathVariablesGap, constraints);

        constraints.gridy = 2;
        contentPanel.add(pathVariablesPanel, constraints);

        constraints.gridy = 3;
        constraints.weighty = 1;
        constraints.fill = GridBagConstraints.BOTH;
        contentPanel.add(Box.createGlue(), constraints);

        JScrollPane scrollPane = new JScrollPane(contentPanel);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setViewportBorder(BorderFactory.createEmptyBorder());
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setOpaque(false);
        add(scrollPane, BorderLayout.CENTER);
    }

    private void installLayoutRefreshListeners() {
        queryParamsPanel.addTableModelListener(e -> SwingUtilities.invokeLater(this::refreshSectionHeights));
        pathVariablesPanel.addTableModelListener(e -> SwingUtilities.invokeLater(this::refreshPathVariablesVisibility));
    }

    private void refreshSectionHeights() {
        applySectionHeight(queryParamsPanel);
        applySectionHeight(pathVariablesPanel);
        contentPanel.revalidate();
        contentPanel.repaint();
    }

    private void applySectionHeight(EasyRequestParamsPanel section) {
        int height = Math.max(MIN_SECTION_HEIGHT, section.getPreferredSectionHeight());
        section.setPreferredSize(new Dimension(1, height));
        section.setMinimumSize(new Dimension(0, MIN_SECTION_HEIGHT));
    }
}
