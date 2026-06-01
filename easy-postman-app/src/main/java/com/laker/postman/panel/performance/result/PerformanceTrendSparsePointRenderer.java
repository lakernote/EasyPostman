package com.laker.postman.panel.performance.result;

import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYDataset;

final class PerformanceTrendSparsePointRenderer extends XYLineAndShapeRenderer {

    PerformanceTrendSparsePointRenderer() {
        super(true, false);
    }

    @Override
    public boolean getItemShapeVisible(int series, int item) {
        XYDataset dataset = getPlot() == null ? null : getPlot().getDataset();
        return PerformanceTrendSparsePointPolicy.shouldShowShape(dataset, series, item);
    }
}
