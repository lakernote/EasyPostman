package com.laker.postman.common.frame;

import com.laker.postman.common.constants.Icons;
import com.laker.postman.common.dialog.ExitDialog;
import com.laker.postman.panel.EasyPostmanMainPanel;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * 主窗口类，继承自 JFrame。
 */
@Slf4j
public class MainFrame extends JFrame {

    private MainFrame() {
        super(); // 调用父类构造函数
        setName("EasyPostman"); // 设置窗口名称
        setTitle("EasyPostman"); // 设置窗口标题
        setIconImage(Icons.LOGO.getImage()); // 设置窗口图标
    }

    public void initComponents() {
        setContentPane(EasyPostmanMainPanel.getInstance()); // 设置主面板为内容面板
        initWindowSize(); // 初始化窗口大小
        initWindowCloseListener(); // 初始化窗口关闭监听器
        pack(); // 调整窗口大小以适应内容
        setLocationRelativeTo(null); // 窗口居中显示
    }

    private void initWindowSize() {
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        if (screenSize.getWidth() > 1280) {
            setPreferredSize(new Dimension(1280, 800));
        } else if (screenSize.getWidth() > 1024) {
            setPreferredSize(new Dimension(1200, 768));
        } else {
            setPreferredSize(new Dimension(960, 640));
        }
        if (screenSize.getWidth() <= 1366) {
            setExtendedState(Frame.MAXIMIZED_BOTH);
        }
    }

    private void initWindowCloseListener() {
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                ExitDialog.show();
            }
        });
    }
}