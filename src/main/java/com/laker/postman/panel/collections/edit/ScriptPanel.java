package com.laker.postman.panel.collections.edit;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.util.function.Consumer;

/**
 * 脚本面板，包含 PreScript、PostScript、Help 三个 Tab，并支持脏监听
 */
public class ScriptPanel extends JPanel {
    private final JTextArea prescriptArea;
    private final JTextArea postscriptArea;

    public ScriptPanel() {
        setLayout(new BorderLayout());
        prescriptArea = new JTextArea(6, 40);
        postscriptArea = new JTextArea(6, 40);
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.addTab("PreScript", new JScrollPane(prescriptArea));
        tabbedPane.addTab("PostScript", new JScrollPane(postscriptArea));

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
            public void insertUpdate(DocumentEvent e) { dirtyCallback.run(); }
            public void removeUpdate(DocumentEvent e) { dirtyCallback.run(); }
            public void changedUpdate(DocumentEvent e) { dirtyCallback.run(); }
        };
        prescriptArea.getDocument().addDocumentListener(listener);
        postscriptArea.getDocument().addDocumentListener(listener);
    }
}

