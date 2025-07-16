package com.laker.postman.common.dialog;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.laker.postman.common.SingletonFactory;
import com.laker.postman.common.frame.MainFrame;
import com.laker.postman.model.Snippet;
import lombok.Getter;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 代码片段弹窗，基于 ListModel
 */
public class SnippetDialog extends JDialog {
    private final JList<Snippet> snippetList;
    private final DefaultListModel<Snippet> listModel;
    private final JTextField searchField;
    private final JTextArea previewArea;
    private final JLabel descriptionLabel;
    private final JComboBox<String> categoryCombo;
    private final Map<String, List<Snippet>> snippetCategories = new LinkedHashMap<>();
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
        setLayout(new BorderLayout(10, 10));

        // 初始化分类
        initCategories();

        // 创建北部面板：搜索框和分类选择器
        JPanel northPanel = new JPanel(new BorderLayout(5, 0));
        northPanel.setBorder(new EmptyBorder(10, 10, 5, 10));

        // 搜索框带图标和提示
        JPanel searchPanel = new JPanel(new BorderLayout());
        searchField = new JTextField();
        searchField.setToolTipText("搜索片段...");

        // 添加搜索图标
        JLabel searchIcon = new JLabel(new FlatSVGIcon("icons/search.svg"));
        searchIcon.setBorder(new EmptyBorder(0, 5, 0, 5));
        searchPanel.add(searchIcon, BorderLayout.WEST);
        searchPanel.add(searchField, BorderLayout.CENTER);

        // 下拉分类选择器
        String[] categories = snippetCategories.keySet().toArray(new String[0]);
        categoryCombo = new JComboBox<>(categories);
        categoryCombo.setPreferredSize(new Dimension(150, 30));

        northPanel.add(searchPanel, BorderLayout.CENTER);
        northPanel.add(categoryCombo, BorderLayout.EAST);

        // 创建中部面板：片段列表
        listModel = new DefaultListModel<>();
        loadSnippets(snippets);

        snippetList = new JList<>(listModel);
        snippetList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        snippetList.setVisibleRowCount(8);

        // 自定义渲染器，让列表项更美观
        snippetList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof Snippet snippet) {
                    label.setText(snippet.title);
                    label.setBorder(new EmptyBorder(5, 10, 5, 10));

                    // 设置图标 - 根据不同类型使用不同图标
                    String title = snippet.title;
                    if (title.startsWith("前置-")) {
                        label.setIcon(new FlatSVGIcon("icons/arrow-up.svg", 16, 16));
                    } else if (title.startsWith("断言-")) {
                        label.setIcon(new FlatSVGIcon("icons/check.svg", 16, 16));
                    } else if (title.startsWith("提取-")) {
                        label.setIcon(new FlatSVGIcon("icons/arrow-down.svg", 16, 16));
                    } else if (title.contains("环境变量")) {
                        label.setIcon(new FlatSVGIcon("icons/environments.svg", 16, 16));
                    } else if (title.contains("编码") || title.contains("解码")) {
                        label.setIcon(new FlatSVGIcon("icons/format.svg", 16, 16));
                    } else if (title.contains("加密") || title.contains("MD5") || title.contains("SHA")) {
                        label.setIcon(new FlatSVGIcon("icons/security.svg", 16, 16)); // 如果没有security.svg可能需要添加
                    } else if (title.contains("字符串")) {
                        label.setIcon(new FlatSVGIcon("icons/format.svg", 16, 16));
                    } else if (title.contains("数组") || title.contains("遍历")) {
                        label.setIcon(new FlatSVGIcon("icons/functional.svg", 16, 16));
                    } else if (title.contains("JSON")) {
                        label.setIcon(new FlatSVGIcon("icons/http.svg", 16, 16));
                    } else if (title.contains("日期") || title.contains("时间")) {
                        label.setIcon(new FlatSVGIcon("icons/time.svg", 16, 16));
                    } else if (title.contains("打印") || title.contains("日志")) {
                        label.setIcon(new FlatSVGIcon("icons/console.svg", 16, 16));
                    } else if (title.contains("正则")) {
                        label.setIcon(new FlatSVGIcon("icons/search.svg", 16, 16));
                    } else {
                        label.setIcon(new FlatSVGIcon("icons/format.svg", 16, 16));
                    }
                }
                return label;
            }
        });

        JScrollPane listScrollPane = new JScrollPane(snippetList);
        listScrollPane.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 0));

        // 创建南部面板：预览区域和按钮
        JPanel southPanel = new JPanel(new BorderLayout(5, 5));
        southPanel.setBorder(new EmptyBorder(5, 10, 10, 10));

        // 预览区域
        JPanel previewPanel = new JPanel(new BorderLayout(5, 5));
        previewPanel.setBorder(BorderFactory.createTitledBorder("代码预览"));

        previewArea = new JTextArea(8, 40);
        previewArea.setEditable(false);
        previewArea.setLineWrap(true);
        previewArea.setWrapStyleWord(true);
        previewArea.setBackground(new Color(245, 245, 245));
        JScrollPane previewScrollPane = new JScrollPane(previewArea);
        previewPanel.add(previewScrollPane, BorderLayout.CENTER);

        // 描述标签
        descriptionLabel = new JLabel(" ");
        descriptionLabel.setBorder(new EmptyBorder(5, 0, 0, 0));
        previewPanel.add(descriptionLabel, BorderLayout.SOUTH);

        // 按钮面板
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton insertBtn = new JButton("插入");
        insertBtn.setPreferredSize(new Dimension(100, 30));

        JButton closeBtn = new JButton("关闭");
        closeBtn.setPreferredSize(new Dimension(100, 30));

        buttonPanel.add(insertBtn);
        buttonPanel.add(closeBtn);

        southPanel.add(previewPanel, BorderLayout.CENTER);
        southPanel.add(buttonPanel, BorderLayout.SOUTH);

        // 将分割面板添加到主面板
        JSplitPane splitPane = new JSplitPane(
                JSplitPane.HORIZONTAL_SPLIT,
                listScrollPane,
                southPanel
        );
        splitPane.setResizeWeight(0.3); // 设置左右比例
        splitPane.setDividerLocation(200);

        // 添加到对话框
        add(northPanel, BorderLayout.NORTH);
        add(splitPane, BorderLayout.CENTER);

        // 绑定事件监听器

        // 搜索框事件
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) {
                filterSnippets();
            }

            public void removeUpdate(DocumentEvent e) {
                filterSnippets();
            }

            public void changedUpdate(DocumentEvent e) {
                filterSnippets();
            }
        });

        // 分类选择器事件
        categoryCombo.addActionListener(e -> {
            String category = (String) categoryCombo.getSelectedItem();
            if (category != null) {
                if (category.equals("全部分类")) {
                    loadSnippets(snippets);
                } else {
                    loadSnippets(snippetCategories.get(category));
                }

                // 如果有搜索关键词，则还需要过滤
                if (!searchField.getText().trim().isEmpty()) {
                    filterSnippets();
                }
            }
        });

        // 列表选择事件
        snippetList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                selectedSnippet = snippetList.getSelectedValue();
                if (selectedSnippet != null) {
                    previewArea.setText(selectedSnippet.code);
                    previewArea.setCaretPosition(0);
                    descriptionLabel.setText(selectedSnippet.desc);
                }
            }
        });

        // 列表双击事件
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

        // 键盘事件
        searchField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_DOWN) {
                    // 按下方向键时，转移焦点到列表并选择第一项
                    snippetList.requestFocusInWindow();
                    if (listModel.getSize() > 0 && snippetList.getSelectedIndex() == -1) {
                        snippetList.setSelectedIndex(0);
                    }
                }
            }
        });

        snippetList.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    // 按下回车键时，如果有选中项，则选择并关闭对话框
                    selectedSnippet = snippetList.getSelectedValue();
                    if (selectedSnippet != null) {
                        dispose();
                    }
                }
            }
        });

        // 按钮事件
        insertBtn.addActionListener(e -> {
            selectedSnippet = snippetList.getSelectedValue();
            if (selectedSnippet != null) {
                dispose();
            } else {
                JOptionPane.showMessageDialog(this, "请先选择一个代码片段", "提示", JOptionPane.INFORMATION_MESSAGE);
            }
        });

        closeBtn.addActionListener(e -> dispose());

        // 初始化状态
        if (listModel.getSize() > 0) {
            snippetList.setSelectedIndex(0);
        }

        // 设置对话框属性
        setSize(800, 600);
        setLocationRelativeTo(owner);
        setMinimumSize(new Dimension(600, 400));
    }

    // 初始化代码片段分类
    private void initCategories() {
        snippetCategories.put("全部分类", snippets);

        // 按前缀和内容分类，更细致的分类
        Map<String, List<Snippet>> categorized = snippets.stream()
                .collect(Collectors.groupingBy(snippet -> {
                    String title = snippet.title;
                    // 主要功能类别
                    if (title.startsWith("前置-")) return "前置脚本";
                    if (title.startsWith("断言-")) return "断言脚本";
                    if (title.startsWith("提取-")) return "提取脚本";

                    // 工具类别
                    if (title.contains("环境变量")) return "环境变量";
                    if (title.contains("加密") || title.contains("MD5") || title.contains("SHA")) return "加密与安全";
                    if (title.contains("编码") || title.contains("解码")) return "编码与解码";
                    if (title.contains("Base64")) return "编码与解码";
                    if (title.contains("URL编码") || title.contains("URL解码")) return "编码与解码";

                    // 数据处理
                    if (title.contains("字符串")) return "字符串操作";
                    if (title.contains("数组") || title.contains("遍历") || title.contains("过滤") || title.contains("映射")) return "数组操作";
                    if (title.contains("JSON") || title.contains("对象")) return "JSON处理";

                    // 特殊功能
                    if (title.contains("日期") || title.contains("时间")) return "日期时间";
                    if (title.contains("正则")) return "正则表达式";
                    if (title.contains("条件") || title.contains("判断")) return "流程控制";
                    if (title.contains("打印") || title.contains("日志")) return "日志调试";
                    if (title.contains("JWT")) return "令牌处理";

                    // 默认类别
                    return "其他工具";
                }));

        // 按特定顺序添加分类，调整了分类顺序并增加了新分类
        String[] orderedCategories = {
                "前置脚本", "断言脚本", "提取脚本", "环境变量",
                "加密与安全", "编码与解码", "字符串操作", "数组操作",
                "JSON处理", "日期时间", "正则表达式", "流程控制",
                "日志调试", "令牌处理", "其他工具"
        };

        for (String category : orderedCategories) {
            List<Snippet> categorySnippets = categorized.get(category);
            if (categorySnippets != null && !categorySnippets.isEmpty()) {
                snippetCategories.put(category, categorySnippets);
            }
        }
    }

    // 加载片段到列表
    private void loadSnippets(List<Snippet> snippetsToLoad) {
        listModel.clear();
        for (Snippet s : snippetsToLoad) {
            listModel.addElement(s);
        }
    }

    // 根据搜索词过滤片段
    private void filterSnippets() {
        String query = searchField.getText().trim().toLowerCase();

        // 获取当前选择的分类下拉框中的选项
        String currentCategory = (String) categoryCombo.getSelectedItem();

        // 确定搜索范围
        List<Snippet> searchSource;
        if (currentCategory != null && !currentCategory.equals("全部分类")) {
            searchSource = snippetCategories.get(currentCategory);
        } else {
            searchSource = snippets;
        }

        // 搜索框为空时，显示当前分类的所有片段
        if (query.isEmpty()) {
            loadSnippets(searchSource);
            return;
        }

        // 在当前选中分类中搜索
        DefaultListModel<Snippet> filteredModel = new DefaultListModel<>();
        for (Snippet s : searchSource) {
            if (s.title.toLowerCase().contains(query) ||
                    (s.desc != null && s.desc.toLowerCase().contains(query)) ||
                    (s.code != null && s.code.toLowerCase().contains(query))) {
                filteredModel.addElement(s);
            }
        }

        // 更新列表模型
        listModel.clear();
        for (int i = 0; i < filteredModel.getSize(); i++) {
            listModel.addElement(filteredModel.getElementAt(i));
        }

        // 如果有结果，选择第一个
        if (listModel.getSize() > 0) {
            snippetList.setSelectedIndex(0);
            // 显示预览
            selectedSnippet = snippetList.getSelectedValue();
            previewArea.setText(selectedSnippet.code);
            previewArea.setCaretPosition(0);
            descriptionLabel.setText(selectedSnippet.desc);
        } else {
            // 没有结果时清空预览
            previewArea.setText("");
            descriptionLabel.setText("未找到匹配的代码片段");
        }
    }
}

