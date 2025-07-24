package com.laker.postman.panel.jmeter.result;

import com.laker.postman.panel.jmeter.model.RequestResult;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class PerformanceReportPanel extends JPanel {
    private final DefaultTableModel reportTableModel;

    public PerformanceReportPanel() {
        super(new BorderLayout());
        String[] columns = {"API Name", "Total", "Success", "Fail", "QPS", "Avg(ms)", "Min(ms)", "Max(ms)", "P99(ms)", "Total Cost(ms)", "Success Rate"};

        reportTableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        JTable reportTable = new JTable(reportTableModel);
        reportTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        reportTable = new JTable(reportTableModel);
        reportTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        // 设置数据列居中
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        // 失败列红色渲染器（0为黑色，大于0为红色，Total行为蓝色加粗）
        DefaultTableCellRenderer failRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                int modelRow = table.convertRowIndexToModel(row);
                boolean isTotal = "Total".equals(reportTableModel.getValueAt(modelRow, 0));
                if (isTotal) {
                    c.setFont(c.getFont().deriveFont(Font.BOLD));
                    c.setForeground(new Color(0, 102, 204));
                    c.setBackground(new Color(230, 240, 255));
                } else {
                    try {
                        int failCount = Integer.parseInt(value == null ? "0" : value.toString());
                        c.setForeground(failCount > 0 ? Color.RED : Color.BLACK);
                        c.setBackground(Color.WHITE);
                    } catch (Exception e) {
                        c.setForeground(Color.BLACK);
                        c.setBackground(Color.WHITE);
                    }
                }
                setHorizontalAlignment(SwingConstants.CENTER);
                return c;
            }
        };
        // 成功率列绿色渲染器（Total行为蓝色加粗）
        DefaultTableCellRenderer rateRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                int modelRow = table.convertRowIndexToModel(row);
                boolean isTotal = "Total".equals(reportTableModel.getValueAt(modelRow, 0));
                if (isTotal) {
                    c.setFont(c.getFont().deriveFont(Font.BOLD));
                    c.setForeground(new Color(0, 102, 204));
                    c.setBackground(new Color(230, 240, 255));
                } else {
                    String rateStr = value != null ? value.toString() : "";
                    if (rateStr.endsWith("%")) {
                        try {
                            double rate = Double.parseDouble(rateStr.replace("%", ""));
                            if (rate >= 99) {
                                c.setForeground(new Color(0, 153, 0)); // 深绿色
                            } else if (rate >= 90) {
                                c.setForeground(new Color(51, 153, 255)); // 蓝色
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
                setHorizontalAlignment(SwingConstants.CENTER);
                return c;
            }
        };
        // 通用居中渲染器（Total行美化）
        DefaultTableCellRenderer generalRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                int modelRow = table.convertRowIndexToModel(row);
                boolean isTotal = "Total".equals(reportTableModel.getValueAt(modelRow, 0));
                if (isTotal) {
                    c.setFont(c.getFont().deriveFont(Font.BOLD));
                    c.setForeground(new Color(0, 102, 204));
                    c.setBackground(new Color(230, 240, 255));
                } else {
                    c.setForeground(Color.BLACK);
                    c.setBackground(Color.WHITE);
                }
                setHorizontalAlignment(SwingConstants.CENTER);
                return c;
            }
        };
        // 需要居中的列索引
        int[] centerColumns = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10};
        for (int col : centerColumns) {
            if (col == 3) { // 失败列
                reportTable.getColumnModel().getColumn(col).setCellRenderer(failRenderer);
            } else if (col == 10) { // 成功率列
                reportTable.getColumnModel().getColumn(col).setCellRenderer(rateRenderer);
            } else {
                reportTable.getColumnModel().getColumn(col).setCellRenderer(generalRenderer);
            }
        }
        // 设置表头加粗
        reportTable.getTableHeader().setFont(reportTable.getTableHeader().getFont().deriveFont(Font.BOLD));
        JScrollPane tableScroll = new JScrollPane(reportTable);
        add(tableScroll, BorderLayout.CENTER);
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
        int totalApi = 0, totalSuccess = 0, totalFail = 0;
        long totalCost = 0, totalMin = Long.MAX_VALUE, totalMax = 0, totalP99 = 0;
        double totalRate = 0;
        int apiCount = 0;
        for (String api : apiCostMap.keySet()) {
            List<Long> costs = apiCostMap.get(api);
            int apiTotal = costs.size();
            int apiSuccess = apiSuccessMap.getOrDefault(api, 0);
            int apiFail = apiFailMap.getOrDefault(api, 0);
            long apiAvg = apiTotal > 0 ? costs.stream().mapToLong(Long::longValue).sum() / apiTotal : 0;
            long apiMin = costs.stream().mapToLong(Long::longValue).min().orElse(0);
            long apiMax = costs.stream().mapToLong(Long::longValue).max().orElse(0);
            long apiP99 = getP99(costs);
            long apiTotalCost = costs.stream().mapToLong(Long::longValue).sum();
            double apiQps = 0;
            if (!allRequestStartTimes.isEmpty() && !allRequestResults.isEmpty()) {
                long minStart = Collections.min(allRequestStartTimes);
                long maxEnd = Collections.max(allRequestResults.stream().map(result -> result.endTime).toList());
                long spanMs = Math.max(1, maxEnd - minStart);
                apiQps = apiTotal * 1000.0 / spanMs;
            }
            double apiRate = apiTotal > 0 ? (apiSuccess * 100.0 / apiTotal) : 0;
            addReportRow(new Object[]{api, apiTotal, apiSuccess, apiFail, String.format("%.2f", apiQps), apiAvg, apiMin, apiMax, apiP99, apiTotalCost, String.format("%.2f%%", apiRate)});
            totalApi += apiTotal;
            totalSuccess += apiSuccess;
            totalFail += apiFail;
            totalCost += apiTotalCost;
            totalMin = Math.min(totalMin, apiMin);
            totalMax = Math.max(totalMax, apiMax);
            totalP99 += apiP99;
            totalRate += apiRate;
            apiCount++;
        }
        if (apiCount > 0) {
            long avgP99 = totalP99 / apiCount;
            double avgRate = totalRate / apiCount;
            double totalQps = 0;
            if (!allRequestStartTimes.isEmpty() && !allRequestResults.isEmpty()) {
                long minStart = Collections.min(allRequestStartTimes);
                long maxEnd = Collections.max(allRequestResults.stream().map(result -> result.endTime).toList());
                long spanMs = Math.max(1, maxEnd - minStart);
                totalQps = totalApi * 1000.0 / spanMs;
            }
            long avg = totalApi > 0 ? totalCost / totalApi : 0;
            addReportRow(new Object[]{"Total", totalApi, totalSuccess, totalFail, String.format("%.2f", totalQps), avg, totalMin == Long.MAX_VALUE ? 0 : totalMin, totalMax, avgP99, totalCost, String.format("%.2f%%", avgRate)});
        }
    }

    private long getP99(List<Long> costs) {
        if (costs == null || costs.isEmpty()) return 0;
        List<Long> sorted = new ArrayList<>(costs);
        Collections.sort(sorted);
        int idx = (int) Math.ceil(sorted.size() * 0.99) - 1;
        return sorted.get(Math.max(idx, 0));
    }
}