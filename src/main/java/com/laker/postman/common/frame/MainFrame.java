package com.laker.postman.common.frame;

import com.laker.postman.common.constants.Icons;
import com.laker.postman.common.dialog.ExitDialog;
import com.laker.postman.panel.EasyPostmanMainPanel;
import com.laker.postman.util.SingletonProvider;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

@Slf4j
public class MainFrame extends JFrame {

    private MainFrame() {
        super();
        setName("EasyPostman");
        setTitle("EasyPostman");
        setIconImage(Icons.LOGO.getImage());
        // 启动时先设置为透明，等内容加载好后再渐变显示
        try {
            this.setOpacity(0f);
        } catch (Exception ignore) {}
    }

    public static MainFrame getInstance() {
        return SingletonProvider.getInstance(MainFrame.class, MainFrame::new);
    }

    public void initComponents() {
        setContentPane(EasyPostmanMainPanel.getInstance());
        initWindowSize();
        initWindowCloseListener();
        pack();
        setLocationRelativeTo(null);
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