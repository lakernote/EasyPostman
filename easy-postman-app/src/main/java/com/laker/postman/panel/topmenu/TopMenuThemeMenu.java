package com.laker.postman.panel.topmenu;

import com.laker.postman.common.themes.SimpleThemeManager;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;
import lombok.experimental.UtilityClass;

import javax.swing.ButtonGroup;
import javax.swing.JMenu;
import javax.swing.JRadioButtonMenuItem;

/**
 * 顶部主题菜单。
 */
@UtilityClass
class TopMenuThemeMenu {

    JMenu create() {
        JMenu themeMenu = new JMenu(I18nUtil.getMessage(MessageKeys.MENU_THEME));
        ButtonGroup themeGroup = new ButtonGroup();

        JRadioButtonMenuItem lightItem = new JRadioButtonMenuItem(I18nUtil.getMessage(MessageKeys.MENU_THEME_LIGHT));
        JRadioButtonMenuItem darkItem = new JRadioButtonMenuItem(I18nUtil.getMessage(MessageKeys.MENU_THEME_DARK));

        themeGroup.add(lightItem);
        themeGroup.add(darkItem);

        if (SimpleThemeManager.isLightTheme()) {
            lightItem.setSelected(true);
        } else {
            darkItem.setSelected(true);
        }

        lightItem.addActionListener(e -> SimpleThemeManager.switchToLightTheme());
        darkItem.addActionListener(e -> SimpleThemeManager.switchToDarkTheme());

        themeMenu.add(lightItem);
        themeMenu.add(darkItem);
        return themeMenu;
    }
}
