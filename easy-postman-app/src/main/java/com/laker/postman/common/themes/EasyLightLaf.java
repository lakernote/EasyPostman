package com.laker.postman.common.themes;

import com.formdev.flatlaf.FlatLightLaf;

/**
 * Easy Postman 自定义主题（继承自 FlatLightLaf）。
 * <p>
 * 配置文件：com/laker/postman/common/themes/EasyLightLaf.properties
 * <p>
 * 参考：<a href="https://www.formdev.com/flatlaf/how-to-customize/">FlatLaf 自定义文档</a>
 */
public class EasyLightLaf extends FlatLightLaf {
    public static final String NAME = "EasyLightLaf";

    public static boolean setup() {
        return setup(new EasyLightLaf());
    }


    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String getDescription() {
        return "EasyLightLaf Look and Feel";
    }
}
