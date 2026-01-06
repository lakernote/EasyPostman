package com.laker.postman.panel.performance.result;

import com.laker.postman.panel.performance.model.RequestResult;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;
import com.laker.postman.util.TimeDisplayUtil;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class PerformanceReportPanel extends JPanel {
    private static final Color TOTAL_ROW_FOREGROUND = new Color(0, 102, 204);
    private static final Color TOTAL_ROW_BACKGROUND = new Color(230, 240, 255);
    private static final Color SUCCESS_GREEN = new Color(0, 153, 0);
    private static final Color WARNING_BLUE = new Color(51, 153, 255);

    private static final int FAIL_COLUMN_INDEX = 3;
    private static final int SUCCESS_RATE_COLUMN_INDEX = 4;

    private final DefaultTableModel reportTableModel;
    private final String[] columns;
    private final String totalRowName;

    public PerformanceReportPanel() {
        // Initialize internationalized column names
        this.columns = new String[]{
                I18nUtil.getMessage(MessageKeys.PERFORMANCE_REPORT_COLUMN_API_NAME),
                I18nUtil.getMessage(MessageKeys.PERFORMANCE_REPORT_COLUMN_TOTAL),
                I18nUtil.getMessage(MessageKeys.PERFORMANCE_REPORT_COLUMN_SUCCESS),
                I18nUtil.getMessage(MessageKeys.PERFORMANCE_REPORT_COLUMN_FAIL),
                I18nUtil.getMessage(MessageKeys.PERFORMANCE_REPORT_COLUMN_SUCCESS_RATE),
                I18nUtil.getMessage(MessageKeys.PERFORMANCE_REPORT_COLUMN_QPS),
                I18nUtil.getMessage(MessageKeys.PERFORMANCE_REPORT_COLUMN_AVG),
                I18nUtil.getMessage(MessageKeys.PERFORMANCE_REPORT_COLUMN_MIN),
                I18nUtil.getMessage(MessageKeys.PERFORMANCE_REPORT_COLUMN_MAX),
                I18nUtil.getMessage(MessageKeys.PERFORMANCE_REPORT_COLUMN_P90),
                I18nUtil.getMessage(MessageKeys.PERFORMANCE_REPORT_COLUMN_P95),
                I18nUtil.getMessage(MessageKeys.PERFORMANCE_REPORT_COLUMN_P99)
        };
        this.totalRowName = I18nUtil.getMessage(MessageKeys.PERFORMANCE_REPORT_TOTAL_ROW);

        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        reportTableModel = createTableModel();
        JTable reportTable = createReportTable();

        JScrollPane tableScroll = new JScrollPane(reportTable);
        add(tableScroll, BorderLayout.CENTER);
    }

    private DefaultTableModel createTableModel() {
        return new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
    }

    private JTable createReportTable() {
        JTable table = new JTable(reportTableModel);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);

        configureColumnRenderers(table);
        table.getTableHeader().setFont(table.getTableHeader().getFont().deriveFont(Font.BOLD));

        return table;
    }

    private void configureColumnRenderers(JTable table) {
        DefaultTableCellRenderer failRenderer = createFailRenderer();
        DefaultTableCellRenderer rateRenderer = createRateRenderer();
        DefaultTableCellRenderer generalRenderer = createGeneralRenderer();

        // 需要居中的列索引（从第2列到最后一列）
        for (int col = 1; col < columns.length; col++) {
            if (col == FAIL_COLUMN_INDEX) {
                table.getColumnModel().getColumn(col).setCellRenderer(failRenderer);
            } else if (col == SUCCESS_RATE_COLUMN_INDEX) {
                table.getColumnModel().getColumn(col).setCellRenderer(rateRenderer);
            } else {
                table.getColumnModel().getColumn(col).setCellRenderer(generalRenderer);
            }
        }
    }

    private DefaultTableCellRenderer createFailRenderer() {
        return new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                           boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                int modelRow = table.convertRowIndexToModel(row);
                boolean isTotal = isTotalRow(modelRow);

                if (isTotal) {
                    applyTotalRowStyle(c);
                } else {
                    applyFailCellStyle(c, value);
                }

                setHorizontalAlignment(SwingConstants.CENTER);
                return c;
            }
        };
    }

    private DefaultTableCellRenderer createRateRenderer() {
        return new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                           boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                int modelRow = table.convertRowIndexToModel(row);
                boolean isTotal = isTotalRow(modelRow);

                if (isTotal) {
                    applyTotalRowStyle(c);
                } else {
                    applyRateCellStyle(c, value);
                }

                setHorizontalAlignment(SwingConstants.CENTER);
                return c;
            }
        };
    }

    private DefaultTableCellRenderer createGeneralRenderer() {
        return new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                           boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                int modelRow = table.convertRowIndexToModel(row);
                boolean isTotal = isTotalRow(modelRow);

                if (isTotal) {
                    applyTotalRowStyle(c);
                } else {
                    applyDefaultCellStyle(c);
                }

                setHorizontalAlignment(SwingConstants.CENTER);
                return c;
            }
        };
    }

    private boolean isTotalRow(int modelRow) {
        Object firstColumnValue = reportTableModel.getValueAt(modelRow, 0);
        return totalRowName.equals(firstColumnValue);
    }

    private void applyTotalRowStyle(Component c) {
        c.setFont(c.getFont().deriveFont(Font.BOLD));
        c.setForeground(TOTAL_ROW_FOREGROUND);
        c.setBackground(TOTAL_ROW_BACKGROUND);
    }

    private void applyFailCellStyle(Component c, Object value) {
        try {
            int failCount = Integer.parseInt(value == null ? "0" : value.toString());
            c.setForeground(failCount > 0 ? Color.RED : Color.BLACK);
            c.setBackground(Color.WHITE);
        } catch (Exception e) {
            applyDefaultCellStyle(c);
        }
    }

    private void applyRateCellStyle(Component c, Object value) {
        String rateStr = value != null ? value.toString() : "";
        if (rateStr.endsWith("%")) {
            try {
                double rate = Double.parseDouble(rateStr.replace("%", ""));
                if (rate >= 99) {
                    c.setForeground(SUCCESS_GREEN);
                } else if (rate >= 90) {
                    c.setForeground(WARNING_BLUE);
                } else {
                    c.setForeground(Color.RED);
                }
            } catch (Exception e) {
                c.setForeground(Color.BLACK);
            }
        } else {
            c.setForeground(Color.BLACK);
        }
        c.setBackground(Color.WHITE);
    }

    private void applyDefaultCellStyle(Component c) {
        c.setForeground(Color.BLACK);
        c.setBackground(Color.WHITE);
    }


    public void clearReport() {
        reportTableModel.setRowCount(0);
    }

    private void addReportRow(Object[] rowData) {
        if (rowData != null && rowData.length == reportTableModel.getColumnCount()) {
            reportTableModel.addRow(rowData);
        } else {
            throw new IllegalArgumentException("Row data must match the number of columns in the report table.");
        }
    }

    public void updateReport(Map<String, List<Long>> apiCostMap,
                             Map<String, Integer> apiSuccessMap,
                             Map<String, Integer> apiFailMap,
                             List<Long> allRequestStartTimes,
                             List<RequestResult> allRequestResults) {
        clearReport();

        ReportStatistics stats = new ReportStatistics();

        for (Map.Entry<String, List<Long>> entry : apiCostMap.entrySet()) {
            String api = entry.getKey();
            List<Long> costs = entry.getValue();

            ApiMetrics metrics = calculateApiMetrics(api, costs, apiSuccessMap, apiFailMap,
                    allRequestStartTimes, allRequestResults);
            addReportRow(metrics.toRowData());
            stats.accumulate(metrics);
        }

        if (stats.apiCount > 0) {
            ApiMetrics totalMetrics = calculateTotalMetrics(stats, apiCostMap,
                    allRequestStartTimes, allRequestResults);
            addReportRow(totalMetrics.toRowData());
        }
    }

    private ApiMetrics calculateApiMetrics(String api, List<Long> costs,
                                           Map<String, Integer> apiSuccessMap,
                                           Map<String, Integer> apiFailMap,
                                           List<Long> allRequestStartTimes,
                                           List<RequestResult> allRequestResults) {
        int total = costs.size();
        int success = apiSuccessMap.getOrDefault(api, 0);
        int fail = apiFailMap.getOrDefault(api, 0);

        // 优化：一次排序获取所有统计值，避免多次流操作
        PerformanceStats perfStats = calculatePerformanceStats(costs);

        double qps = calculateQps(total, allRequestStartTimes, allRequestResults);
        double rate = total > 0 ? (success * 100.0 / total) : 0;

        return new ApiMetrics(api, total, success, fail, rate, qps,
                perfStats.avg, perfStats.min, perfStats.max,
                perfStats.p90, perfStats.p95, perfStats.p99);
    }

    private ApiMetrics calculateTotalMetrics(ReportStatistics stats,
                                             Map<String, List<Long>> apiCostMap,
                                             List<Long> allRequestStartTimes,
                                             List<RequestResult> allRequestResults) {
        // 避免除零错误
        if (stats.apiCount == 0) {
            return new ApiMetrics(totalRowName, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
        }

        long avgP90 = stats.totalP90 / stats.apiCount;
        long avgP95 = stats.totalP95 / stats.apiCount;
        long avgP99 = stats.totalP99 / stats.apiCount;
        double avgRate = stats.totalRate / stats.apiCount;

        long totalAvg = calculateTotalAverage(apiCostMap, stats.totalApi);
        double totalQps = calculateQps(stats.totalApi, allRequestStartTimes, allRequestResults);
        long totalMin = stats.totalMin == Long.MAX_VALUE ? 0 : stats.totalMin;

        return new ApiMetrics(totalRowName, stats.totalApi, stats.totalSuccess, stats.totalFail,
                avgRate, totalQps, totalAvg, totalMin, stats.totalMax,
                avgP90, avgP95, avgP99);
    }


    private long calculateTotalAverage(Map<String, List<Long>> apiCostMap, int totalApi) {
        if (totalApi == 0) {
            return 0;
        }
        long sum = apiCostMap.values().stream()
                .flatMap(List::stream)
                .mapToLong(Long::longValue)
                .sum();
        return sum / totalApi;
    }

    private double calculateQps(int totalRequests, List<Long> allRequestStartTimes,
                                List<RequestResult> allRequestResults) {
        if (allRequestStartTimes.isEmpty() || allRequestResults.isEmpty()) {
            return 0;
        }

        long minStart = Collections.min(allRequestStartTimes);
        long maxEnd = allRequestResults.stream()
                .mapToLong(result -> result.endTime)
                .max()
                .orElse(minStart);
        long spanMs = Math.max(1, maxEnd - minStart);

        return totalRequests * 1000.0 / spanMs;
    }

    private static class ReportStatistics {
        int totalApi = 0;
        int totalSuccess = 0;
        int totalFail = 0;
        long totalP90 = 0;
        long totalP95 = 0;
        long totalP99 = 0;
        long totalMin = Long.MAX_VALUE;
        long totalMax = 0;
        double totalRate = 0;
        int apiCount = 0;

        void accumulate(ApiMetrics metrics) {
            totalApi += metrics.total;
            totalSuccess += metrics.success;
            totalFail += metrics.fail;
            totalP90 += metrics.p90;
            totalP95 += metrics.p95;
            totalP99 += metrics.p99;
            totalMin = Math.min(totalMin, metrics.min);
            totalMax = Math.max(totalMax, metrics.max);
            totalRate += metrics.rate;
            apiCount++;
        }
    }

    private static class ApiMetrics {
        final String name;
        final int total;
        final int success;
        final int fail;
        final double rate;
        final double qps;
        final long avg;
        final long min;
        final long max;
        final long p90;
        final long p95;
        final long p99;

        ApiMetrics(String name, int total, int success, int fail, double rate, double qps,
                   long avg, long min, long max, long p90, long p95, long p99) {
            this.name = name;
            this.total = total;
            this.success = success;
            this.fail = fail;
            this.rate = rate;
            this.qps = qps;
            this.avg = avg;
            this.min = min;
            this.max = max;
            this.p90 = p90;
            this.p95 = p95;
            this.p99 = p99;
        }

        Object[] toRowData() {
            return new Object[]{
                    name,
                    total,
                    success,
                    fail,
                    String.format("%.2f%%", rate),
                    Math.round(qps),
                    TimeDisplayUtil.formatElapsedTime(avg),
                    TimeDisplayUtil.formatElapsedTime(min),
                    TimeDisplayUtil.formatElapsedTime(max),
                    TimeDisplayUtil.formatElapsedTime(p90),
                    TimeDisplayUtil.formatElapsedTime(p95),
                    TimeDisplayUtil.formatElapsedTime(p99)
            };
        }
    }

    /**
     * 性能统计结果类
     */
    private static class PerformanceStats {
        final long avg;
        final long min;
        final long max;
        final long p90;
        final long p95;
        final long p99;

        PerformanceStats(long avg, long min, long max, long p90, long p95, long p99) {
            this.avg = avg;
            this.min = min;
            this.max = max;
            this.p90 = p90;
            this.p95 = p95;
            this.p99 = p99;
        }
    }

    /**
     * 优化的性能统计计算：一次排序获取所有统计值
     */
    private PerformanceStats calculatePerformanceStats(List<Long> costs) {
        if (costs == null || costs.isEmpty()) {
            return new PerformanceStats(0, 0, 0, 0, 0, 0);
        }

        // 一次排序获取所有值
        List<Long> sorted = new ArrayList<>(costs);
        Collections.sort(sorted);

        int size = sorted.size();
        long sum = sorted.stream().mapToLong(Long::longValue).sum();
        long avg = sum / size;
        long min = sorted.get(0);
        long max = sorted.get(size - 1);
        long p90 = getPercentileFromSorted(sorted, 0.90);
        long p95 = getPercentileFromSorted(sorted, 0.95);
        long p99 = getPercentileFromSorted(sorted, 0.99);

        return new PerformanceStats(avg, min, max, p90, p95, p99);
    }

    /**
     * 从已排序的列表中获取百分位数（修复边界情况）
     */
    private long getPercentileFromSorted(List<Long> sortedCosts, double percentile) {
        int size = sortedCosts.size();
        if (size == 0) {
            return 0;
        }
        if (size == 1) {
            return sortedCosts.get(0);
        }

        // 使用更准确的百分位数计算方法
        double index = percentile * (size - 1);
        int lowerIndex = (int) Math.floor(index);
        int upperIndex = (int) Math.ceil(index);

        if (lowerIndex == upperIndex) {
            return sortedCosts.get(lowerIndex);
        }

        // 线性插值
        long lowerValue = sortedCosts.get(lowerIndex);
        long upperValue = sortedCosts.get(upperIndex);
        double fraction = index - lowerIndex;
        return (long) (lowerValue + (upperValue - lowerValue) * fraction);
    }
}