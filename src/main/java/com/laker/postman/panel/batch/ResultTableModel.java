package com.laker.postman.panel.batch;

import com.laker.postman.common.table.generic.BaseTableModel;

// 结果表格模型，继承通用BaseTableModel
public class ResultTableModel extends BaseTableModel<ResultRow> {
    private final String[] columns = {"请求名称", "状态码", "耗时(ms)", "响应大小", "错误信息"};

    @Override
    public int getColumnCount() {
        return columns.length;
    }

    @Override
    public String getColumnName(int col) {
        return columns[col];
    }

    @Override
    protected Object getValueAtRow(ResultRow r, int col) {
        return switch (col) {
            case 0 -> r.name;
            case 1 -> r.code == 0 ? "-" : r.code;
            case 2 -> r.cost;
            case 3 -> r.size;
            case 4 -> r.error == null ? "" : r.error;
            default -> "";
        };
    }
}