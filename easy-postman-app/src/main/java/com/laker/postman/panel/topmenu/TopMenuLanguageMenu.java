package com.laker.postman.panel.topmenu;

import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;
import com.laker.postman.common.component.notification.NotificationCenter;
import com.laker.postman.util.FontManager;
import com.laker.postman.util.UIRefreshManager;
import lombok.experimental.UtilityClass;

import javax.swing.ButtonGroup;
import javax.swing.JMenu;
import javax.swing.JRadioButtonMenuItem;

/**
 * 顶部语言菜单。
 */
@UtilityClass
class TopMenuLanguageMenu {

    JMenu create() {
        JMenu languageMenu = new JMenu(I18nUtil.getMessage(MessageKeys.MENU_LANGUAGE));
        ButtonGroup languageGroup = new ButtonGroup();

        JRadioButtonMenuItem englishItem = new JRadioButtonMenuItem("English");
        JRadioButtonMenuItem chineseItem = new JRadioButtonMenuItem("中文");

        languageGroup.add(englishItem);
        languageGroup.add(chineseItem);

        if (I18nUtil.isChinese()) {
            chineseItem.setSelected(true);
        } else {
            englishItem.setSelected(true);
        }

        englishItem.addActionListener(e -> switchLanguage("en"));
        chineseItem.addActionListener(e -> switchLanguage("zh"));

        languageMenu.add(englishItem);
        languageMenu.add(chineseItem);
        return languageMenu;
    }

    private void switchLanguage(String languageCode) {
        I18nUtil.setLocale(languageCode);
        FontManager.applyFontSettings();
        UIRefreshManager.refreshLanguage();
        NotificationCenter.showWarning(I18nUtil.getMessage(MessageKeys.LANGUAGE_CHANGED));
    }
}
