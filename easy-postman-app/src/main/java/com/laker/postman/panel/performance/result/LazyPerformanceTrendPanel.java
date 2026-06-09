package com.laker.postman.panel.performance.result;

import com.laker.postman.common.UiSingletonFactory;
import com.laker.postman.common.component.ToolWindowSurfaceStyle;
import com.laker.postman.common.component.placeholder.PerformanceTrendPlaceholderPanel;
import com.laker.postman.performance.core.model.PerformanceTrendSnapshot;
import org.jfree.data.time.RegularTimePeriod;

import javax.swing.*;
import java.awt.*;
import java.util.function.Supplier;

public class LazyPerformanceTrendPanel extends JPanel implements PerformanceTrendView {
    private final Supplier<PerformanceTrendPanel> trendPanelSupplier;
    private PerformanceTrendPanel trendPanel;

    public LazyPerformanceTrendPanel() {
        this(() -> UiSingletonFactory.getInstance(PerformanceTrendPanel.class));
    }

    LazyPerformanceTrendPanel(Supplier<PerformanceTrendPanel> trendPanelSupplier) {
        super(new BorderLayout());
        ToolWindowSurfaceStyle.applyCard(this);
        this.trendPanelSupplier = trendPanelSupplier == null
                ? () -> UiSingletonFactory.getInstance(PerformanceTrendPanel.class)
                : trendPanelSupplier;
        add(new PerformanceTrendPlaceholderPanel(), BorderLayout.CENTER);
    }

    public boolean isTrendPanelCreated() {
        return trendPanel != null;
    }

    @Override
    public void clearTrendDataset() {
        if (trendPanel != null) {
            trendPanel.clearTrendDataset();
        }
    }

    @Override
    public void addOrUpdate(RegularTimePeriod period, PerformanceTrendSnapshot snapshot) {
        if (period == null || snapshot == null) {
            return;
        }
        getOrCreateTrendPanel().addOrUpdate(period, snapshot);
    }

    private PerformanceTrendPanel getOrCreateTrendPanel() {
        if (trendPanel != null) {
            return trendPanel;
        }

        trendPanel = trendPanelSupplier.get();
        removeAll();
        add(trendPanel, BorderLayout.CENTER);
        revalidate();
        repaint();
        return trendPanel;
    }
}
