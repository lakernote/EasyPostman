package com.laker.postman.model;

import javax.swing.*;
import java.util.function.Supplier;

// Tab元数据结构，便于维护和扩展
public class TabInfo {
    public String title;
    public Icon icon;
    public Supplier<JPanel> panelSupplier; // 用于懒加载面板
    public JPanel panel;

    public TabInfo(String title, Icon icon, Supplier<JPanel> panelSupplier) {
        this.title = title;
        this.icon = icon;
        this.panelSupplier = panelSupplier;
    }

    public JPanel getPanel() { // 懒加载面板
        if (panel == null) {
            panel = panelSupplier.get();
        }
        return panel;
    }
}