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

    private static final int FAIL_COLUMN_INDEX = 3;
    private static final int SUCCESS_RATE_COLUMN_INDEX = 4;

    // 百分位数常量
    private static final double PERCENTILE_90 = 0.90;
    private static final double PERCENTILE_95 = 0.95;
    private static final double PERCENTILE_99 = 0.99;
    private static final int MIN_SAMPLE_SIZE_FOR_INTERPOLATION = 10;

    // 成功率阈值
    private static final double SUCCESS_RATE_EXCELLENT = 99.0;
    private static final double SUCCESS_RATE_GOOD = 90.0;

    private final DefaultTableModel reportTableModel;
    private final String[] columns;
    private final String totalRowName;

    // 单例渲染器，避免重复创建
    private final DefaultTableCellRenderer failRenderer;
    private final DefaultTableCellRenderer rateRenderer;
    private final DefaultTableCellRenderer generalRenderer;

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

        // 创建单例渲染器
        this.failRenderer = createFailRenderer();
        this.rateRenderer = createRateRenderer();
        this.generalRenderer = createGeneralRenderer();

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
        table.setFocusable(false);
        // 使用 SUBSEQUENT_COLUMNS 模式：调整一列时，只影响后续列，不影响前面的列
        table.setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);

        configureColumnRenderers(table);
        configureColumnWidths(table);
        table.getTableHeader().setFont(table.getTableHeader().getFont().deriveFont(Font.BOLD));

        return table;
    }

    private void configureColumnWidths(JTable table) {
        if (table.getColumnModel().getColumnCount() > 0) {
            // API Name 列 - 需要较宽空间显示完整API名称
            table.getColumnModel().getColumn(0).setMinWidth(50);
            table.getColumnModel().getColumn(0).setPreferredWidth(200);

            // Total 列 - 显示 "Total"（5个字符）+ 数字
            table.getColumnModel().getColumn(1).setMinWidth(65);
            table.getColumnModel().getColumn(1).setMaxWidth(85);
            table.getColumnModel().getColumn(1).setPreferredWidth(75);

            // Success 列 - 显示 "Success"（7个字符）+ 数字
            table.getColumnModel().getColumn(2).setMinWidth(75);
            table.getColumnModel().getColumn(2).setMaxWidth(95);
            table.getColumnModel().getColumn(2).setPreferredWidth(85);

            // Fail 列 - 显示 "Fail"（4个字符）+ 数字
            table.getColumnModel().getColumn(3).setMinWidth(60);
            table.getColumnModel().getColumn(3).setMaxWidth(75);
            table.getColumnModel().getColumn(3).setPreferredWidth(65);

            // Success Rate 列 - 显示 "Success Rate"（12个字符）+ 百分比
            table.getColumnModel().getColumn(4).setMinWidth(110);
            table.getColumnModel().getColumn(4).setMaxWidth(130);
            table.getColumnModel().getColumn(4).setPreferredWidth(120);

            // QPS 列 - 显示 "QPS"（3个字符）+ 数字
            table.getColumnModel().getColumn(5).setMinWidth(60);
            table.getColumnModel().getColumn(5).setMaxWidth(80);
            table.getColumnModel().getColumn(5).setPreferredWidth(70);

            // Avg 列 - 显示 "Avg"（3个字符）+ 时间
            table.getColumnModel().getColumn(6).setMinWidth(65);
            table.getColumnModel().getColumn(6).setMaxWidth(85);
            table.getColumnModel().getColumn(6).setPreferredWidth(75);

            // Min 列 - 显示 "Min"（3个字符）+ 时间
            table.getColumnModel().getColumn(7).setMinWidth(65);
            table.getColumnModel().getColumn(7).setMaxWidth(85);
            table.getColumnModel().getColumn(7).setPreferredWidth(75);

            // Max 列 - 显示 "Max"（3个字符）+ 时间
            table.getColumnModel().getColumn(8).setMinWidth(65);
            table.getColumnModel().getColumn(8).setMaxWidth(85);
            table.getColumnModel().getColumn(8).setPreferredWidth(75);

            // P90 列 - 显示 "P90"（3个字符）+ 时间
            table.getColumnModel().getColumn(9).setMinWidth(65);
            table.getColumnModel().getColumn(9).setMaxWidth(85);
            table.getColumnModel().getColumn(9).setPreferredWidth(75);

            // P95 列 - 显示 "P95"（3个字符）+ 时间
            table.getColumnModel().getColumn(10).setMinWidth(65);
            table.getColumnModel().getColumn(10).setMaxWidth(85);
            table.getColumnModel().getColumn(10).setPreferredWidth(75);

            // P99 列 - 显示 "P99"（3个字符）+ 时间
            table.getColumnModel().getColumn(11).setMinWidth(65);
            table.getColumnModel().getColumn(11).setMaxWidth(85);
            table.getColumnModel().getColumn(11).setPreferredWidth(75);
        }
    }

    private void configureColumnRenderers(JTable table) {
        // 使用单例渲染器，避免重复创建
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
        c.setForeground(UIManager.getColor("Performance.report.totalForeground"));
        c.setBackground(UIManager.getColor("Performance.report.totalBackground"));
    }

    private void applyFailCellStyle(Component c, Object value) {
        try {
            int failCount = Integer.parseInt(value == null ? "0" : value.toString());
            c.setForeground(failCount > 0 ? Color.RED : UIManager.getColor("Table.foreground"));
            c.setBackground(UIManager.getColor("Table.background"));
        } catch (Exception e) {
            applyDefaultCellStyle(c);
        }
    }

    private void applyRateCellStyle(Component c, Object value) {
        String rateStr = value != null ? value.toString() : "";
        if (rateStr.endsWith("%")) {
            try {
                double rate = Double.parseDouble(rateStr.replace("%", ""));
                if (rate >= SUCCESS_RATE_EXCELLENT) {
                    c.setForeground(UIManager.getColor("Performance.report.successColor"));
                } else if (rate >= SUCCESS_RATE_GOOD) {
                    c.setForeground(UIManager.getColor("Performance.report.warningColor"));
                } else {
                    c.setForeground(Color.RED);
                }
            } catch (Exception e) {
                c.setForeground(UIManager.getColor("Table.foreground"));
            }
        } else {
            c.setForeground(UIManager.getColor("Table.foreground"));
        }
        c.setBackground(UIManager.getColor("Table.background"));
    }

    private void applyDefaultCellStyle(Component c) {
        c.setForeground(UIManager.getColor("Table.foreground"));
        c.setBackground(UIManager.getColor("Table.background"));
    }


    public void clearReport() {
        reportTableModel.setRowCount(0);
    }

    private void addReportRow(Object[] rowData) {
        if (rowData == null) {
            throw new IllegalArgumentException("Row data cannot be null");
        }
        if (rowData.length != reportTableModel.getColumnCount()) {
            throw new IllegalArgumentException(
                    String.format("Row data must match the number of columns. Expected: %d, Actual: %d",
                            reportTableModel.getColumnCount(), rowData.length));
        }
        reportTableModel.addRow(rowData);
    }

    public void updateReport(Map<String, List<Long>> apiCostMap,
                             Map<String, Integer> apiSuccessMap,
                             Map<String, Integer> apiFailMap,
                             List<RequestResult> allRequestResults) {
        clearReport();

        ReportStatistics stats = new ReportStatistics();

        for (Map.Entry<String, List<Long>> entry : apiCostMap.entrySet()) {
            String api = entry.getKey();
            List<Long> costs = entry.getValue();

            ApiMetrics metrics = calculateApiMetrics(api, costs, apiSuccessMap, apiFailMap,
                    allRequestResults);
            addReportRow(metrics.toRowData());
            stats.accumulate(metrics);
        }

        if (stats.apiCount > 0) {
            ApiMetrics totalMetrics = calculateTotalMetrics(stats, apiCostMap,
                    allRequestResults);
            addReportRow(totalMetrics.toRowData());
        }
    }

    private ApiMetrics calculateApiMetrics(String apiId, List<Long> costs,
                                           Map<String, Integer> apiSuccessMap,
                                           Map<String, Integer> apiFailMap,
                                           List<RequestResult> allRequestResults) {
        int total = costs.size();
        int success = apiSuccessMap.getOrDefault(apiId, 0);
        int fail = apiFailMap.getOrDefault(apiId, 0);

        // 优化：一次排序获取所有统计值，避免多次流操作
        PerformanceStats perfStats = calculatePerformanceStats(costs);

        // 修复：按API ID过滤请求结果，计算该API的实际QPS
        double qps = calculateQpsForApi(apiId, total, allRequestResults);
        double rate = total > 0 ? (success * 100.0 / total) : 0;

        // 从 ApiMetadata 中获取 apiName 用于显示（通过 getApiName() 方法）
        String displayName = allRequestResults.stream()
                .filter(result -> apiId.equals(result.apiId))
                .findFirst()
                .map(RequestResult::getApiName)  // 使用方法而不是字段
                .orElse(apiId);  // 如果找不到，使用 ID 作为备用

        return new ApiMetrics(displayName, total, success, fail, rate, qps,
                perfStats.avg, perfStats.min, perfStats.max,
                perfStats.p90, perfStats.p95, perfStats.p99);
    }

    private ApiMetrics calculateTotalMetrics(ReportStatistics stats,
                                             Map<String, List<Long>> apiCostMap,
                                             List<RequestResult> allRequestResults) {
        // 避免除零错误
        if (stats.apiCount == 0) {
            return new ApiMetrics(totalRowName, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
        }

        double totalRate = stats.totalApi > 0 ? (stats.totalSuccess * 100.0 / stats.totalApi) : 0;

        long totalAvg = calculateTotalAverage(apiCostMap, stats.totalApi);
        // 计算所有API的总QPS
        double totalQps = calculateQpsForAllApis(stats.totalApi, allRequestResults);
        long totalMin = stats.totalMin == Long.MAX_VALUE ? 0 : stats.totalMin;

        PerformanceStats totalPerfStats = calculateTotalPerformanceStats(apiCostMap);

        return new ApiMetrics(totalRowName, stats.totalApi, stats.totalSuccess, stats.totalFail,
                totalRate, totalQps, totalAvg, totalMin, stats.totalMax,
                totalPerfStats.p90, totalPerfStats.p95, totalPerfStats.p99);
    }

    /**
     * 计算所有请求的总体性能统计（合并所有 API）
     */
    private PerformanceStats calculateTotalPerformanceStats(Map<String, List<Long>> apiCostMap) {
        // 合并所有 API 的请求耗时
        List<Long> allCosts = apiCostMap.values().stream()
                .flatMap(List::stream)
                .toList();

        if (allCosts.isEmpty()) {
            return new PerformanceStats(0, 0, 0, 0, 0, 0);
        }

        return calculatePerformanceStats(allCosts);
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

    /**
     * 计算指定API的QPS（使用ID进行过滤，避免重名问题）
     */
    private double calculateQpsForApi(String apiId, int totalRequests, List<RequestResult> allRequestResults) {
        if (totalRequests == 0 || allRequestResults.isEmpty()) {
            return 0;
        }

        // 【关键修复】使用 apiId 而不是 apiName 过滤请求，避免重名问题
        List<RequestResult> apiResults = allRequestResults.stream()
                .filter(result -> apiId.equals(result.apiId))
                .toList();

        if (apiResults.isEmpty()) {
            return 0;
        }

        // 获取该API的最早开始时间和最晚结束时间
        long minStart = apiResults.stream()
                .mapToLong(result -> result.startTime)
                .min()
                .orElse(0);
        long maxEnd = apiResults.stream()
                .mapToLong(result -> result.endTime)
                .max()
                .orElse(minStart);

        long spanMs = Math.max(1, maxEnd - minStart);
        return totalRequests * 1000.0 / spanMs;
    }

    /**
     * 计算所有API的总QPS
     */
    private double calculateQpsForAllApis(int totalRequests, List<RequestResult> allRequestResults) {
        if (totalRequests == 0 || allRequestResults.isEmpty()) {
            return 0;
        }

        // 获取所有请求的最早开始时间和最晚结束时间
        long minStart = allRequestResults.stream()
                .mapToLong(result -> result.startTime)
                .min()
                .orElse(0);
        long maxEnd = allRequestResults.stream()
                .mapToLong(result -> result.endTime)
                .max()
                .orElse(minStart);

        long spanMs = Math.max(1, maxEnd - minStart);

        return totalRequests * 1000.0 / spanMs;
    }

    private static class ReportStatistics {
        int totalApi = 0;        // 总请求数
        int totalSuccess = 0;    // 总成功数
        int totalFail = 0;       // 总失败数
        long totalMin = Long.MAX_VALUE;  // 最小耗时
        long totalMax = 0;       // 最大耗时
        int apiCount = 0;        // API 数量

        void accumulate(ApiMetrics metrics) {
            totalApi += metrics.total;
            totalSuccess += metrics.success;
            totalFail += metrics.fail;
            totalMin = Math.min(totalMin, metrics.min);
            totalMax = Math.max(totalMax, metrics.max);
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
        // 优化：在遍历中同时计算sum和min/max，避免多次遍历
        long sum = 0;
        long min = sorted.get(0);
        long max = sorted.get(size - 1);
        for (Long cost : sorted) {
            sum += cost;
        }
        long avg = sum / size;
        long p90 = getPercentileFromSorted(sorted, PERCENTILE_90);
        long p95 = getPercentileFromSorted(sorted, PERCENTILE_95);
        long p99 = getPercentileFromSorted(sorted, PERCENTILE_99);

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
        // 对于小样本（少于10个），直接取最接近的值，避免插值误导
        if (size < MIN_SAMPLE_SIZE_FOR_INTERPOLATION) {
            int index = Math.min((int) Math.ceil(percentile * size) - 1, size - 1);
            return sortedCosts.get(Math.max(0, index));
        }

        // 使用更准确的百分位数计算方法（线性插值）
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