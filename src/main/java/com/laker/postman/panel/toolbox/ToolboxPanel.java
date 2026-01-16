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
        // 添加左右边距，同时保留右侧分割线
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 1, 0, 0, ModernColors.getDividerBorderColor()),
                BorderFactory.createEmptyBorder(0, 10, 0, 10)
        ));

        // 创建标签页容器
        toolTabs = new JTabbedPane(SwingConstants.TOP);
        // 设置标签页布局策略：使用滚动模式，避免标签页过多时换行显示
        toolTabs.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);

        // 添加各种工具标签页 - 按功能分类和使用频率排序
        initToolTabs();

        add(toolTabs, BorderLayout.CENTER);
    }

    /**
     * 初始化所有工具标签页
     * 按使用频率排序，优化用户体验
     */
    private void initToolTabs() {
        // 1. JSON格式化 - API开发必备，使用频率最高
        addToolTab(
                I18nUtil.getMessage(MessageKeys.TOOLBOX_JSON),
                createThemedIcon("icons/format.svg"),
                new JsonToolPanel()
        );

        // 2. SQL格式化 - 数据库开发常用工具
        addToolTab(
                I18nUtil.getMessage(MessageKeys.TOOLBOX_SQL),
                createThemedIcon("icons/database.svg"),
                new SqlToolPanel()
        );

        // 3. 编码解码 - URL/Base64等，日常开发高频工具
        addToolTab(
                I18nUtil.getMessage(MessageKeys.TOOLBOX_ENCODER),
                createThemedIcon("icons/code.svg"),
                new EncoderPanel()
        );

        // 3. 时间戳转换 - 时间格式转换，常用工具
        addToolTab(
                I18nUtil.getMessage(MessageKeys.TOOLBOX_TIMESTAMP),
                createThemedIcon("icons/time.svg"),
                new TimestampPanel()
        );

        // 4. UUID生成器 - 唯一ID生成，频繁使用
        addToolTab(
                I18nUtil.getMessage(MessageKeys.TOOLBOX_UUID),
                createThemedIcon("icons/plus.svg"),
                new UuidPanel()
        );

        // 5. 哈希计算 - MD5/SHA等，安全验证常用
        addToolTab(
                I18nUtil.getMessage(MessageKeys.TOOLBOX_HASH),
                createThemedIcon("icons/hash.svg"),
                new HashPanel()
        );

        // 6. 文本对比 - 差异对比工具，中等频率
        addToolTab(
                I18nUtil.getMessage(MessageKeys.TOOLBOX_DIFF),
                createThemedIcon("icons/file.svg"),
                new DiffPanel()
        );

        // 7. 加密解密 - AES/DES等，特定场景使用
        addToolTab(
                I18nUtil.getMessage(MessageKeys.TOOLBOX_CRYPTO),
                createThemedIcon("icons/security.svg"),
                new CryptoPanel()
        );

        // 8. Cron表达式 - 定时任务表达式，特定场景
        addToolTab(
                I18nUtil.getMessage(MessageKeys.TOOLBOX_CRON),
                createThemedIcon("icons/time.svg"),
                new CronPanel()
        );

        // 9. Java反编译器 - 专业工具，低频但重要
        addToolTab(
                I18nUtil.getMessage(MessageKeys.TOOLBOX_DECOMPILER),
                createThemedIcon("icons/code.svg"),
                new DecompilerPanel()
        );
    }

    /**
     * 创建主题适配的 SVG 图标
     * 使用 ColorFilter 使图标颜色自动跟随主题的按钮前景色
     *
     * @param iconPath SVG 图标路径
     * @return 主题适配的图标
     */
    private Icon createThemedIcon(String iconPath) {
        FlatSVGIcon icon = new FlatSVGIcon(iconPath, 16, 16);
        icon.setColorFilter(new FlatSVGIcon.ColorFilter(color -> UIManager.getColor("Button.foreground")));
        return icon;
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
