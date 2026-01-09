package com.laker.postman.panel.toolbox;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.laker.postman.common.SingletonBasePanel;
import com.laker.postman.common.constants.ModernColors;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import java.awt.*;

/**
 * 工具箱面板 - 包含开发常用工具
 * Toolbox Panel - Contains common development tools
 */
@Slf4j
public class ToolboxPanel extends SingletonBasePanel {

    private JTabbedPane toolTabs;

    @Override
    protected void initUI() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createMatteBorder(0, 1, 0, 0, ModernColors.getDividerBorderColor()));

        // 创建标签页容器
        toolTabs = new JTabbedPane(SwingConstants.TOP);

        // 添加各种工具标签页 - 按功能分类和使用频率排序
        initToolTabs();

        add(toolTabs, BorderLayout.CENTER);
    }

    /**
     * 初始化所有工具标签页
     */
    private void initToolTabs() {
        // 1. 数据格式化工具
        addToolTab(
                I18nUtil.getMessage(MessageKeys.TOOLBOX_JSON),
                new FlatSVGIcon("icons/format.svg", 16, 16),
                new JsonToolPanel()
        );

        // 2. 编码解码工具
        addToolTab(
                I18nUtil.getMessage(MessageKeys.TOOLBOX_ENCODER),
                new FlatSVGIcon("icons/code.svg", 16, 16),
                new EncoderPanel()
        );

        // 3. 哈希计算工具（单向加密）
        addToolTab(
                I18nUtil.getMessage(MessageKeys.TOOLBOX_HASH),
                new FlatSVGIcon("icons/hash.svg", 16, 16),
                new HashPanel()
        );

        // 4. 加密解密工具（双向加密）
        addToolTab(
                I18nUtil.getMessage(MessageKeys.TOOLBOX_CRYPTO),
                new FlatSVGIcon("icons/security.svg", 16, 16),
                new CryptoPanel()
        );

        // 5. 时间戳转换工具
        addToolTab(
                I18nUtil.getMessage(MessageKeys.TOOLBOX_TIMESTAMP),
                new FlatSVGIcon("icons/time.svg", 16, 16),
                new TimestampPanel()
        );

        // 6. UUID生成器
        addToolTab(
                I18nUtil.getMessage(MessageKeys.TOOLBOX_UUID),
                new FlatSVGIcon("icons/plus.svg", 16, 16),
                new UuidPanel()
        );

        // 7. 文本对比工具
        addToolTab(
                I18nUtil.getMessage(MessageKeys.TOOLBOX_DIFF),
                new FlatSVGIcon("icons/file.svg", 16, 16),
                new DiffPanel()
        );

        // 8. Cron表达式工具
        addToolTab(
                I18nUtil.getMessage(MessageKeys.TOOLBOX_CRON),
                new FlatSVGIcon("icons/time.svg", 16, 16),
                new CronPanel()
        );
    }

    /**
     * 添加工具标签页
     *
     * @param title 标签页标题
     * @param icon  标签页图标
     * @param panel 标签页面板内容
     */
    private void addToolTab(String title, Icon icon, JPanel panel) {
        toolTabs.addTab(title, icon, panel);
    }

    @Override
    protected void registerListeners() {
        // 可以在这里注册监听器，如标签页切换事件等
    }
}
