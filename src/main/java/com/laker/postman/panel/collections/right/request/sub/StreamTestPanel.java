package com.laker.postman.panel.collections.right.request.sub;

import com.laker.postman.model.StreamTestResult;
import com.laker.postman.model.TestResult;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.List;

public class StreamTestPanel extends JPanel {
    private JTable table;
    private StreamTestTableModel tableModel;

    public StreamTestPanel() {
        setLayout(new BorderLayout());
        tableModel = new StreamTestTableModel();
        table = new JTable(tableModel);
        table.setFillsViewportHeight(true);
        JScrollPane scrollPane = new JScrollPane(table);
        add(scrollPane, BorderLayout.CENTER);
    }

    public void addStreamTestResult(StreamTestResult result) {
        tableModel.addResult(result);
    }

    static class StreamTestTableModel extends AbstractTableModel {
        private List<StreamTestResult> data;
        private final String[] columns = {"时间", "消息内容", "断言通过/总数", "断言详情"};
        private final SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");

        public void setData(List<StreamTestResult> data) {
            this.data = data;
            fireTableDataChanged();
        }

        public void addResult(StreamTestResult result) {
            if (data == null) {
                data = new java.util.ArrayList<>();
            }
            int row = data.size();
            data.add(result);
            fireTableRowsInserted(row, row);
        }

        @Override
        public int getRowCount() {
            return data == null ? 0 : data.size();
        }

        @Override
        public int getColumnCount() {
            return columns.length;
        }

        @Override
        public String getColumnName(int column) {
            return columns[column];
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            StreamTestResult r = data.get(rowIndex);
            switch (columnIndex) {
                case 0:
                    return sdf.format(r.getTimestamp());
                case 1:
                    String msg = r.getMessage();
                    return msg != null && msg.length() > 60 ? msg.substring(0, 60) + "..." : msg;
                case 2:
                    int total = r.getTestResults() == null ? 0 : r.getTestResults().size();
                    int pass = 0;
                    if (r.getTestResults() != null) {
                        for (TestResult tr : r.getTestResults()) if (tr.passed) pass++;
                    }
                    return pass + "/" + total;
                case 3:
                    if (r.getTestResults() == null) return "";
                    StringBuilder sb = new StringBuilder();
                    for (TestResult tr : r.getTestResults()) {
                        sb.append(tr.name).append(": ")
                                .append(tr.passed ? "通过" : "失败");
                        if (!tr.passed && tr.message != null) {
                            sb.append(" (" + tr.message + ")");
                        }
                        sb.append("; ");
                    }
                    return sb.toString();
                default:
                    return "";
            }
        }
    }
}
