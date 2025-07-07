package com.laker.postman.panel.collections.edit;

import com.laker.postman.service.http.CookieService;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.List;

/**
 * CookieTablePanel: 高仿Postman，展示和管理所有Cookie（含属性/删除/清空）
 */
public class CookieTablePanel extends JPanel {
    private final JTable table;
    private final DefaultTableModel model;
    private final JButton btnDelete;
    private final JButton btnClear;
    private final Runnable cookieListener = this::loadCookies;

    public CookieTablePanel() {
        setLayout(new BorderLayout());
        String[] columns = {"Name", "Value", "Domain", "Path", "Expires", "Secure", "HttpOnly"};
        model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        table = new JTable(model);
        JScrollPane scrollPane = new JScrollPane(table);
        add(scrollPane, BorderLayout.CENTER);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        btnDelete = new JButton("删除选中");
        btnClear = new JButton("清空所有");
        btnPanel.add(btnDelete);
        btnPanel.add(btnClear);
        add(btnPanel, BorderLayout.SOUTH);

        btnDelete.addActionListener(e -> deleteSelectedCookie());
        btnClear.addActionListener(e -> clearAllCookies());

        CookieService.registerCookieChangeListener(cookieListener);
        loadCookies();
    }

    private void loadCookies() {
        model.setRowCount(0);
        List<CookieService.CookieInfo> cookies = CookieService.getAllCookieInfos();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        for (CookieService.CookieInfo c : cookies) {
            String expires = c.expires > 0 ? sdf.format(c.expires) : "Session";
            model.addRow(new Object[]{
                    c.name, c.value, c.domain, c.path, expires, c.secure, c.httpOnly
            });
        }
    }

    private void deleteSelectedCookie() {
        int row = table.getSelectedRow();
        if (row >= 0) {
            String name = (String) model.getValueAt(row, 0);
            String domain = (String) model.getValueAt(row, 2);
            String path = (String) model.getValueAt(row, 3);
            CookieService.removeCookie(name, domain, path);
            loadCookies();
        }
    }

    private void clearAllCookies() {
        int confirm = JOptionPane.showConfirmDialog(this, "确定要清空所有Cookie吗？", "确认", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            CookieService.clearAllCookies();
            loadCookies();
        }
    }

    @Override
    public void removeNotify() {
        super.removeNotify();
        CookieService.unregisterCookieChangeListener(cookieListener);
    }
}