package com.laker.postman.panel.sidebar;

import lombok.Getter;
import lombok.Setter;

import javax.swing.*;
import java.util.function.Supplier;

/**
 * 侧边栏 Tab 元数据，面板内容按需懒加载。
 */
@Getter
public class TabInfo {
    @Setter
    private String title;
    private final Icon icon;
    private final Supplier<JPanel> panelSupplier;
    private JPanel panel;

    public TabInfo(String title, Icon icon, Supplier<JPanel> panelSupplier) {
        this.title = title;
        this.icon = icon;
        this.panelSupplier = panelSupplier;
    }

    public JPanel getPanel() {
        if (panel == null) {
            panel = panelSupplier.get();
        }
        return panel;
    }
}
