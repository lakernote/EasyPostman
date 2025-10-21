package com.laker.postman.panel.toolbox;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.laker.postman.common.SingletonBasePanel;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import java.awt.*;

/**
 * 工具箱面板 - 包含开发常用工具
 */
@Slf4j
public class ToolboxPanel extends SingletonBasePanel {

    private JTabbedPane toolTabs;

    @Override
    protected void initUI() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createMatteBorder(0, 1, 0, 0, Color.LIGHT_GRAY));

        // 创建标签页
        toolTabs = new JTabbedPane(JTabbedPane.TOP);

        // 添加各种工具标签页
        addToolTab(I18nUtil.getMessage(MessageKeys.TOOLBOX_ENCODER), new FlatSVGIcon("icons/code.svg", 16, 16), new EncoderPanel());
        addToolTab(I18nUtil.getMessage(MessageKeys.TOOLBOX_CRYPTO), new FlatSVGIcon("icons/security.svg", 16, 16), new CryptoPanel());
        addToolTab(I18nUtil.getMessage(MessageKeys.TOOLBOX_JSON), new FlatSVGIcon("icons/format.svg", 16, 16), new JsonToolPanel());
        addToolTab(I18nUtil.getMessage(MessageKeys.TOOLBOX_TIMESTAMP), new FlatSVGIcon("icons/time.svg", 16, 16), new TimestampPanel());
        addToolTab(I18nUtil.getMessage(MessageKeys.TOOLBOX_UUID), new FlatSVGIcon("icons/plus.svg", 16, 16), new UuidPanel());
        addToolTab(I18nUtil.getMessage(MessageKeys.TOOLBOX_HASH), new FlatSVGIcon("icons/security.svg", 16, 16), new HashPanel());

        add(toolTabs, BorderLayout.CENTER);
    }

    private void addToolTab(String title, Icon icon, JPanel panel) {
        toolTabs.addTab(title, icon, panel);
    }

    @Override
    protected void registerListeners() {
        // 可以在这里注册监听器
    }
}
