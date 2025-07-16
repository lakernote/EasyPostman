package com.laker.postman.common.dialog;

import com.laker.postman.common.SingletonFactory;
import com.laker.postman.common.frame.MainFrame;
import com.laker.postman.model.Snippet;
import lombok.Getter;

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
    @Getter
    private Snippet selectedSnippet;
    private static final List<Snippet> snippets = List.of(
            // 前置脚本类别 - 新增
            new Snippet("前置-设置请求变量", "pm.setVariable('requestId', pm.generateUUID());\nconsole.log('已生成请求ID: ' + pm.getVariable('requestId'));", "设置请求级别的变量，仅在当前请求中有效"),
            new Snippet("前置-设置环境变量", "pm.environment.set('timestamp', Date.now());\nconsole.log('已设置时间戳环境变量: ' + pm.environment.get('timestamp'));", "设置环境变量，在所有请求中可用"),
            new Snippet("前置-随机UUID", "pm.environment.set('uuid', pm.generateUUID());\nconsole.log('已生成随机UUID: ' + pm.environment.get('uuid'));", "生成随机UUID并保存到环境变量"),
            new Snippet("前置-动态时间戳", "pm.environment.set('timestamp', pm.getTimestamp());\nconsole.log('已生成时间戳: ' + pm.environment.get('timestamp'));", "生成当前时间戳并保存到环境变量"),
            new Snippet("前置-签名计算", "// 假设需要生成签名\nvar timestamp = Date.now();\nvar appKey = pm.environment.get('appKey');\nvar appSecret = pm.environment.get('appSecret');\n\n// 构建待签名字符串\nvar stringToSign = 'appKey=' + appKey + '&timestamp=' + timestamp;\n\n// 计算签名 (使用SHA256)\nvar signature = SHA256(stringToSign + appSecret).toString();\n\n// 设置到环境变量\npm.environment.set('timestamp', timestamp);\npm.environment.set('signature', signature);\n\nconsole.log('已生成签名: ' + signature);", "计算API签名并保存到环境变量"),
            new Snippet("前置-动态参数处理", "// 获取当前时间\nvar now = new Date();\n\n// 格式化为YYYY-MM-DD\nvar date = now.getFullYear() + '-' + \n    ('0' + (now.getMonth() + 1)).slice(-2) + '-' + \n    ('0' + now.getDate()).slice(-2);\n\n// 保存到环境变量\npm.environment.set('currentDate', date);\nconsole.log('当前日期: ' + date);", "生成格式化的当前日期并保存到环境变量"),
            new Snippet("前置-JWT解析", "// 解析JWT Token\nvar token = pm.environment.get('jwt_token');\nif (token) {\n    // 分割Token\n    var parts = token.split('.');\n    if (parts.length === 3) {\n        // 解码payload部分(base64)\n        var payload = JSON.parse(atob(parts[1]));\n        console.log('Token解析结果:', payload);\n        // 可以提取特定字段\n        if (payload.exp) {\n            console.log('Token过期时间:', new Date(payload.exp * 1000));\n        }\n    }\n}", "解析JWT令牌并提取有用信息"),
            new Snippet("前置-请求数据加密", "// 获取请求体\nvar requestData = JSON.parse(request.body || '{}');\n\n// 假设需要加密某个字段\nif (requestData.password) {\n    // 使用MD5加密\n    requestData.password = MD5(requestData.password).toString();\n    console.log('密码已加密');\n    \n    // 更新请求体\n    pm.setVariable('encryptedBody', JSON.stringify(requestData));\n    // 注意：这里只设置了变量，实际请求体不会改变，需要在请求体中使用{{encryptedBody}}\n}", "加密请求数据中的敏感字段"),
            new Snippet("前置-动态请求头", "// 设置时间戳和签名等动态请求头\nvar timestamp = Date.now();\nvar nonce = Math.random().toString(36).substring(2, 15);\n\n// 设置到环境变量，以便在请求头中使用\npm.environment.set('req_timestamp', timestamp);\npm.environment.set('req_nonce', nonce);\n\nconsole.log('已设置请求时间戳: ' + timestamp);\nconsole.log('已设置请求随机数: ' + nonce);", "生成动态请求头参数并保存到环境变量"),
            new Snippet("前置-条件判断", "// 根据环境判断使用不同的参数\nvar env = pm.environment.get('environment');\n\nif (env === 'production') {\n    pm.environment.set('base_url', 'https://api.example.com');\n    console.log('已切换到生产环境');\n} else if (env === 'staging') {\n    pm.environment.set('base_url', 'https://staging-api.example.com');\n    console.log('已切换到预发布环境');\n} else {\n    pm.environment.set('base_url', 'https://dev-api.example.com');\n    console.log('已切换到开发环境');\n}", "根据条件动态设置不同的环境参数"),

            // 断言类别
            new Snippet("断言-状态码为200", "pm.test('Status code is 200', function () {\n    pm.response.to.have.status(200);\n});", "断言响应状态码为200"),
            new Snippet("断言-Body包含字符串", "pm.test('Body contains string', function () {\n    pm.expect(pm.response.text()).to.include('success');\n});", "断言响应体包含指定字符串"),
            new Snippet("断言-JSON属性值", "pm.test('JSON value check', function () {\n    var jsonData = pm.response.json();\n    pm.expect(jsonData.code).to.eql(0);\n});", "断言JSON属性值等于0"),
            new Snippet("断言-Header存在", "pm.test('Header is present', function () {\n    pm.response.to.have.header('Content-Type');\n});", "断言响应头存在"),
            new Snippet("断言-响应时间<1000ms", "pm.test('Response time is less than 1000ms', function () {\n    pm.expect(pm.response.responseTime).to.be.below(1000);\n});", "断言响应时间小于1000ms"),
            new Snippet("断言-字段存在", "pm.test('字段存在', function () {\n    var jsonData = pm.response.json();\n    pm.expect(jsonData).to.have.property('data');\n});", "断言JSON中存在data字段"),
            new Snippet("断言-数组长度", "pm.test('数组长度为3', function () {\n    var arr = pm.response.json().list;\n    pm.expect(arr.length).to.eql(3);\n});", "断言数组长度为3"),
            new Snippet("断言-正则匹配", "pm.test('Body正则匹配', function () {\n    pm.expect(pm.response.text()).to.match(/success/);\n});", "断言响应体正则匹配"),

            // 提取类别
            new Snippet("提取-JSON字段到环境变量", "var jsonData = pm.response.json();\npm.environment.set('token', jsonData.token);", "提取token到环境变量"),
            new Snippet("提取-Header到环境变量", "var token = pm.response.headers.get('X-Token');\npm.environment.set('token', token);", "提取响应头X-Token到环境变量"),
            new Snippet("提取-正则匹配到环境变量", "var match = pm.response.text().match(/token=(\\w+)/);\nif (match) {\n    pm.environment.set('token', match[1]);\n}", "使用正则提取token到环境变量"),

            // 环境变量管理
            new Snippet("设置环境变量", "pm.environment.set('key', 'value');", "设置环境变量"),
            new Snippet("获取环境变量", "pm.environment.get('key');", "获取环境变量"),
            new Snippet("删除环境变量", "pm.environment.unset('key');", "删除环境变量"),
            new Snippet("清空环境变量", "pm.environment.clear();", "清空所有环境变量"),

            // 其他操作
            new Snippet("循环遍历数组", "var arr = pm.response.json().list;\narr.forEach(function(item) {\n    // 处理每个item\n});", "遍历JSON数组字段list"),
            new Snippet("条件判断", "if (pm.response.code === 200) {\n    // 成功逻辑\n} else {\n    // 失败逻辑\n}", "根据响应状态码进行条件判断"),
            new Snippet("打印日志", "console.log('日志内容');", "打印日志到控制台"),

            // 基本加密解密功能
            new Snippet("Base64编码", "var encoded = btoa('Hello World');\nconsole.log(encoded); // SGVsbG8gV29ybGQ=", "使用btoa进行Base64编码"),
            new Snippet("Base64解码", "var decoded = atob('SGVsbG8gV29ybGQ=');\nconsole.log(decoded); // Hello World", "使用atob进行Base64解码"),
            new Snippet("URL编码", "var encoded = encodeURIComponent('Hello World!');\nconsole.log(encoded); // Hello%20World%21", "使用encodeURIComponent进行URL编码"),
            new Snippet("URL解码", "var decoded = decodeURIComponent('Hello%20World%21');\nconsole.log(decoded); // Hello World!", "使用decodeURIComponent进行URL解码"),

            // 常用字符串操作
            new Snippet("字符串截取", "var str = 'Hello World';\nvar sub = str.substring(0, 5);\nconsole.log(sub); // Hello", "截取字符串的一部分"),
            new Snippet("字符串替换", "var str = 'Hello World';\nvar newStr = str.replace('World', 'JavaScript');\nconsole.log(newStr); // Hello JavaScript", "替换字符串中的内容"),
            new Snippet("字符串分割", "var str = 'a,b,c,d';\nvar arr = str.split(',');\nconsole.log(arr); // ['a', 'b', 'c', 'd']", "将字符串分割为数组"),

            // 日期时间处理
            new Snippet("获取当前时间戳", "var timestamp = Date.now();\nconsole.log(timestamp); // 毫秒时间戳", "获取当前的毫秒时间戳"),
            new Snippet("格式化日期", "var date = new Date();\nvar formatted = date.toISOString();\nconsole.log(formatted); // 如: 2023-01-01T12:00:00.000Z", "格式化日期为ISO字符串"),

            // JSON处理
            new Snippet("JSON字符串转对象", "var jsonString = '{\"name\":\"test\",\"value\":123}';\nvar obj = JSON.parse(jsonString);\nconsole.log(obj.name); // test", "将JSON字符串转换为JavaScript对象"),
            new Snippet("对象转JSON字符串", "var obj = {name: 'test', value: 123};\nvar jsonString = JSON.stringify(obj);\nconsole.log(jsonString); // {\"name\":\"test\",\"value\":123}", "将JavaScript对象转换为JSON字符串"),

            // 数组操作
            new Snippet("数组过滤", "var arr = [1, 2, 3, 4, 5];\nvar filtered = arr.filter(function(item) {\n    return item > 3;\n});\nconsole.log(filtered); // [4, 5]", "过滤数组中的元素"),
            new Snippet("数组映射", "var arr = [1, 2, 3];\nvar mapped = arr.map(function(item) {\n    return item * 2;\n});\nconsole.log(mapped); // [2, 4, 6]", "映射数组中的每个元素"),

            // 正则表达式
            new Snippet("正则匹配提取", "var str = 'My email is test@example.com';\nvar regex = /[\\w._%+-]+@[\\w.-]+\\.[a-zA-Z]{2,4}/;\nvar email = str.match(regex)[0];\nconsole.log(email); // test@example.com", "使用正则表达式提取匹配内容"),

            // 计算与加密
            new Snippet("MD5加密", "var hash = MD5('Message').toString();\nconsole.log(hash);", "计算MD5哈希值"),
            new Snippet("SHA256加密", "var hash = SHA256('Message').toString();\nconsole.log(hash);", "计算SHA256哈希值")
    );


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
        JButton insertBtn = new JButton("Insert");
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
                if (evt.getClickCount() == 2) { // 双击事件
                    selectedSnippet = snippetList.getSelectedValue();
                    if (selectedSnippet != null) {
                        dispose();
                    }
                }
            }
        });
        JPanel southPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        southPanel.add(insertBtn);
        JButton closeBtn = new JButton("Close");
        closeBtn.addActionListener(e -> dispose());
        southPanel.add(closeBtn);
        add(searchField, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
        add(southPanel, BorderLayout.SOUTH);
        setSize(400, 500);
        setLocationRelativeTo(owner);
        setResizable(false); // 禁止窗口大小调整
    }

}