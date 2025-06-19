package com.laker.postman.panel.collections.edit;

import lombok.extern.slf4j.Slf4j;
import org.fife.ui.autocomplete.AutoCompletion;
import org.fife.ui.autocomplete.BasicCompletion;
import org.fife.ui.autocomplete.DefaultCompletionProvider;
import org.fife.ui.autocomplete.ShorthandCompletion;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rsyntaxtextarea.Theme;
import org.fife.ui.rtextarea.RTextScrollPane;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.io.InputStream;

/**
 * 脚本面板，包含 PreScript、PostScript、Help 三个 Tab，并支持脏监听
 */
@Slf4j
public class ScriptPanel extends JPanel {
    private final RSyntaxTextArea prescriptArea;
    private final RSyntaxTextArea postscriptArea;

    public ScriptPanel() {
        setLayout(new BorderLayout());
        prescriptArea = new RSyntaxTextArea(6, 40);
        prescriptArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JAVASCRIPT); // 设置语法高亮为 JavaScript
        prescriptArea.setCodeFoldingEnabled(true); // 启用代码折叠
        loadEditorTheme(prescriptArea);
        addAutoCompletion(prescriptArea);
        postscriptArea = new RSyntaxTextArea(6, 40);
        postscriptArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JAVASCRIPT);
        postscriptArea.setCodeFoldingEnabled(true); // 启用代码折叠
        loadEditorTheme(postscriptArea);
        addAutoCompletion(postscriptArea);
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.addTab("PreScript", new RTextScrollPane(prescriptArea)); // 用RTextScrollPane显示行号和代码折叠
        tabbedPane.addTab("PostScript", new RTextScrollPane(postscriptArea));

        JTextArea helpArea = new JTextArea();
        helpArea.setEditable(false);
        helpArea.setLineWrap(true);
        helpArea.setWrapStyleWord(true);
        helpArea.setText("PreScript/PostScript 可用变量：\nrequest, env, postman/pm, responseBody, responseHeaders, status, statusCode 等。\n可在脚本中通过 pm.environment.set('key', 'value') 设置环境变量。\n详细用法请参考文档或悬停提示。");
        tabbedPane.addTab("Help", new JScrollPane(helpArea));

        add(tabbedPane, BorderLayout.CENTER);
    }

    public void setPrescript(String text) {
        prescriptArea.setText(text);
    }

    public void setPostscript(String text) {
        postscriptArea.setText(text);
    }

    public String getPrescript() {
        return prescriptArea.getText();
    }

    public String getPostscript() {
        return postscriptArea.getText();
    }

    /**
     * 添加脏监听，内容变更时回调
     */
    public void addDirtyListeners(Runnable dirtyCallback) {
        DocumentListener listener = new DocumentListener() {
            public void insertUpdate(DocumentEvent e) {
                dirtyCallback.run();
            }

            public void removeUpdate(DocumentEvent e) {
                dirtyCallback.run();
            }

            public void changedUpdate(DocumentEvent e) {
                dirtyCallback.run();
            }
        };
        prescriptArea.getDocument().addDocumentListener(listener);
        postscriptArea.getDocument().addDocumentListener(listener);
    }


    private void loadEditorTheme(RSyntaxTextArea area) {
        // 尝试加载主题文件
        // 这里使用 IntelliJ IDEA 风格的主题
        // dark.xml
        // vs.xml
        try (InputStream in = getClass().getResourceAsStream("/org/fife/ui/rsyntaxtextarea/themes/vs.xml")) {
            if (in != null) {
                Theme theme = Theme.load(in);
                theme.apply(area);
            }
        } catch (IOException ignored) {
            log.error(ignored.getMessage(), ignored);
        }
    }

    /**
     * 为 RSyntaxTextArea 添加自动补全、悬浮提示和代码片段
     */
    private void addAutoCompletion(RSyntaxTextArea area) {
        DefaultCompletionProvider provider = new DefaultCompletionProvider();
        // 常用 JS/pm/postman 相关补全
        provider.addCompletion(new BasicCompletion(provider, "pm", "Postman pm 对象，常用断言/环境操作"));
        provider.addCompletion(new BasicCompletion(provider, "postman", "Postman 旧版对象"));
        provider.addCompletion(new BasicCompletion(provider, "request", "请求对象"));
        provider.addCompletion(new BasicCompletion(provider, "response", "响应对象"));
        provider.addCompletion(new BasicCompletion(provider, "env", "环境变量对象"));
        provider.addCompletion(new BasicCompletion(provider, "responseBody", "响应体字符串"));
        provider.addCompletion(new BasicCompletion(provider, "responseHeaders", "响应头对象"));
        provider.addCompletion(new BasicCompletion(provider, "status", "响应状态"));
        provider.addCompletion(new BasicCompletion(provider, "statusCode", "响应状态码"));
        provider.addCompletion(new BasicCompletion(provider, "console", "控制台对象"));
        provider.addCompletion(new BasicCompletion(provider, "console.log", "打印日志到控制台"));
        provider.addCompletion(new BasicCompletion(provider, "setEnvironmentVariable", "设置环境变量"));
        provider.addCompletion(new BasicCompletion(provider, "getEnvironmentVariable", "获取环境变量"));
        provider.addCompletion(new BasicCompletion(provider, "if", "条件语句"));
        provider.addCompletion(new BasicCompletion(provider, "else", "条件语句"));
        provider.addCompletion(new BasicCompletion(provider, "for", "循环语句"));
        provider.addCompletion(new BasicCompletion(provider, "while", "循环语句"));
        provider.addCompletion(new BasicCompletion(provider, "function", "函数定义"));
        provider.addCompletion(new BasicCompletion(provider, "return", "返回语句"));
        // 代码片段
        provider.addCompletion(new ShorthandCompletion(provider, "pm.environment.set", "pm.environment.set('key', 'value');", "设置环境变量"));
        provider.addCompletion(new ShorthandCompletion(provider, "pm.environment.get", "pm.environment.get('key');", "获取环境变量"));
        provider.addCompletion(new ShorthandCompletion(provider, "console.log", "console.log('内容');", "打印日志"));
        provider.addCompletion(new ShorthandCompletion(provider, "JSON.parse(responseBody)", "JSON.parse(responseBody);", "解析响应体为 JSON 对象"));
        provider.addCompletion(new ShorthandCompletion(provider, "JSON.stringify", "JSON.stringify(obj);", "将对象转换为 JSON 字符串"));
        AutoCompletion ac = new AutoCompletion(provider);
        ac.setAutoCompleteEnabled(true); // 启用自动补全
        ac.setAutoActivationEnabled(true); // 启用自动激活
        ac.setAutoActivationDelay(200); // 200ms后触发自动补全
        ac.setTriggerKey(KeyStroke.getKeyStroke(KeyEvent.VK_TAB, 0)); // 使用Tab键触发补全
        ac.setShowDescWindow(true); // 显示悬浮说明
        ac.install(area);
    }
}
