package com.laker.postman.common.dialog;

import com.laker.postman.common.SingletonFactory;
import com.laker.postman.common.frame.MainFrame;
import com.laker.postman.model.Snippet;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.util.List;

/**
 * 代码片段弹窗，基于 ListModel
 */
public class SnippetDialog extends JDialog {
    private final JList<Snippet> snippetList;
    private final DefaultListModel<Snippet> listModel;
    private final JTextField searchField;
    private Snippet selectedSnippet;
    private static final List<Snippet> snippets = List.of(
            new Snippet("断言-状态码为200", "pm.test('Status code is 200', function () {\n    pm.response.to.have.status(200);\n});", "断言响应状态码为200"),
            new Snippet("断言-Body包含字符串", "pm.test('Body contains string', function () {\n    pm.expect(pm.response.text()).to.include('success');\n});", "断言响应体包含指定字符串"),
            new Snippet("断言-JSON属性值", "pm.test('JSON value check', function () {\n    var jsonData = pm.response.json();\n    pm.expect(jsonData.code).to.eql(0);\n});", "断言JSON属性值等于0"),
            new Snippet("断言-Header存在", "pm.test('Header is present', function () {\n    pm.response.to.have.header('Content-Type');\n});", "断言响应头存在"),
            new Snippet("断言-响应时间<1000ms", "pm.test('Response time is less than 1000ms', function () {\n    pm.expect(pm.response.responseTime).to.be.below(1000);\n});", "断言响应时间小于1000ms"),
            new Snippet("断言-字段存在", "pm.test('字段存在', function () {\n    var jsonData = pm.response.json();\n    pm.expect(jsonData).to.have.property('data');\n});", "断言JSON中存在data字段"),
            new Snippet("断言-数组长度", "pm.test('数组长度为3', function () {\n    var arr = pm.response.json().list;\n    pm.expect(arr.length).to.eql(3);\n});", "断言数组长度为3"),
            new Snippet("断言-正则匹配", "pm.test('Body正则匹配', function () {\n    pm.expect(pm.response.text()).to.match(/success/);\n});", "断言响应体正则匹配"),

            new Snippet("提取JSON字段到环境变量", "var jsonData = pm.response.json();\npm.environment.set('token', jsonData.token);", "提取token到环境变量"),
            new Snippet("提取Header到环境变量", "var token = pm.response.headers.get('X-Token');\npm.environment.set('token', token);", "提取响应头到环境变量"),

            new Snippet("pm.environment.set", "pm.environment.set('key', 'value');", "设置环境变量"),
            new Snippet("pm.environment.get", "pm.environment.get('key');", "获取环境变量"),
            new Snippet("pm.environment.unset", "pm.environment.unset('key');", "删除环境变量"),
            new Snippet("pm.globals.set", "pm.globals.set('key', 'value');", "设置全局变量"),
            new Snippet("pm.globals.get", "pm.globals.get('key');", "获取全局变量"),
            new Snippet("pm.globals.unset", "pm.globals.unset('key');", "删除全局变量"));


    public SnippetDialog() {
        super(SingletonFactory.getInstance(MainFrame.class), "Snippets", true);
        Frame owner = SingletonFactory.getInstance(MainFrame.class);
        setLayout(new BorderLayout());
        listModel = new DefaultListModel<>();
        for (Snippet s : snippets) listModel.addElement(s);
        snippetList = new JList<>(listModel);
        snippetList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        snippetList.setVisibleRowCount(10);
        snippetList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof Snippet) {
                    label.setText(value.toString());
                }
                return label;
            }
        });
        JScrollPane scrollPane = new JScrollPane(snippetList);
        searchField = new JTextField();
        searchField.setToolTipText("搜索片段...");
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) {
                filter();
            }

            public void removeUpdate(DocumentEvent e) {
                filter();
            }

            public void changedUpdate(DocumentEvent e) {
                filter();
            }

            private void filter() {
                String q = searchField.getText().trim().toLowerCase();
                listModel.clear();
                for (Snippet s : snippets) {
                    if (s.title.toLowerCase().contains(q) || (s.desc != null && s.desc.toLowerCase().contains(q))) {
                        listModel.addElement(s);
                    }
                }
            }
        });
        JButton insertBtn = new JButton("插入");
        insertBtn.addActionListener(e -> {
            selectedSnippet = snippetList.getSelectedValue();
            if (selectedSnippet != null) {
                dispose();
            }
        });
        snippetList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                selectedSnippet = snippetList.getSelectedValue();
            }
        });
        snippetList.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                if (evt.getClickCount() == 2) {
                    selectedSnippet = snippetList.getSelectedValue();
                    if (selectedSnippet != null) {
                        dispose();
                    }
                }
            }
        });
        JPanel southPanel = new JPanel(new BorderLayout());
        southPanel.add(insertBtn, BorderLayout.EAST);
        add(searchField, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
        add(southPanel, BorderLayout.SOUTH);
        setSize(400, 300);
        setLocationRelativeTo(owner);
    }

    public Snippet getSelectedSnippet() {
        return selectedSnippet;
    }
}