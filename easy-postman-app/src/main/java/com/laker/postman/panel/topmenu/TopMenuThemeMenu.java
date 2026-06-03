package com.laker.postman.panel.topmenu;

import com.laker.postman.common.themes.SimpleThemeManager;
import com.laker.postman.common.themes.ThemeDescriptor;
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

        for (ThemeDescriptor theme : SimpleThemeManager.availableThemes()) {
            JRadioButtonMenuItem themeItem = new JRadioButtonMenuItem(I18nUtil.getMessage(theme.nameKey()));
            themeItem.setSelected(theme.id().equals(SimpleThemeManager.currentThemeId()));
            themeItem.addActionListener(e -> SimpleThemeManager.switchTheme(theme.id()));
            themeGroup.add(themeItem);
            themeMenu.add(themeItem);
        }

        return themeMenu;
    }
}
