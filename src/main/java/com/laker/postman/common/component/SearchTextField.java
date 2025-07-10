package com.laker.postman.common.component;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.formdev.flatlaf.extras.components.FlatTextField;

import java.awt.*;

/**
 * 通用搜索输入框组件，带搜索图标、占位符、清除按钮和固定宽度。
 */
public class SearchTextField extends FlatTextField {
    public SearchTextField() {
        super();
        setLeadingIcon(new FlatSVGIcon("icons/search.svg", 16, 16));
        setPlaceholderText("Search...");
        setShowClearButton(true);
        setPreferredSize(new Dimension(180, 28));
        setMaximumSize(new Dimension(180, 28));
    }
}