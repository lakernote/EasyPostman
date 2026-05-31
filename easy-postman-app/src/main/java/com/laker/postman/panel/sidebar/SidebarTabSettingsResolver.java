package com.laker.postman.panel.sidebar;

import com.laker.postman.service.setting.SettingManager;
import lombok.experimental.UtilityClass;

import java.util.List;

@UtilityClass
public class SidebarTabSettingsResolver {

    public List<SidebarTab> getOrderedSidebarTabs() {
        return SidebarTab.resolveOrderedTabs(SettingManager.getSidebarTabOrder());
    }

    public List<SidebarTab> getVisibleSidebarTabs() {
        return SidebarTab.resolveVisibleTabs(
                SettingManager.getSidebarTabOrder(),
                SettingManager.getHiddenSidebarTabs()
        );
    }
}
