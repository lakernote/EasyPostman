package com.laker.postman.panel.performance.result;

import org.jfree.data.xy.XYDataset;

/**
 * 稀疏趋势线的点位显示规则：空窗口会断线，断线两侧的孤立样本必须显示圆点。
 */
final class PerformanceTrendSparsePointPolicy {

    private PerformanceTrendSparsePointPolicy() {
    }

    static boolean shouldShowShape(XYDataset dataset, int series, int item) {
        if (!hasFiniteValue(dataset, series, item)) {
            return false;
        }
        return !hasFiniteValue(dataset, series, item - 1)
                && !hasFiniteValue(dataset, series, item + 1);
    }

    private static boolean hasFiniteValue(XYDataset dataset, int series, int item) {
        if (dataset == null || series < 0 || series >= dataset.getSeriesCount()) {
            return false;
        }
        if (item < 0 || item >= dataset.getItemCount(series)) {
            return false;
        }
        return Double.isFinite(dataset.getYValue(series, item));
    }
}
