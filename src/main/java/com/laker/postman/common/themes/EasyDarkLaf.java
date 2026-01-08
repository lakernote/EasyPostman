package com.laker.postman.common.themes;

import com.formdev.flatlaf.FlatDarkLaf;

/**
 * Easy Postman 自定义暗色主题（继承自 FlatDarkLaf）。
 * <p>
 * 配置文件：com/laker/postman/common/themes/EasyDarkLaf.properties
 * <p>
 * 参考：<a href="https://www.formdev.com/flatlaf/how-to-customize/">FlatLaf 自定义文档</a>
 */
public class EasyDarkLaf extends FlatDarkLaf {
    public static final String NAME = "EasyDarkLaf";

    public static boolean setup() {
        return setup(new EasyDarkLaf());
    }


    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String getDescription() {
        return "EasyDarkLaf Look and Feel";
    }
}
