package com.laker.postman.model;

import org.testng.annotations.Test;

import java.util.List;
import java.util.Set;

import static org.testng.Assert.assertEquals;

public class SidebarTabTest {

    @Test(description = "侧边栏排序配置应忽略未知和重复项，并补齐遗漏项")
    public void testResolveOrderedTabs_ShouldNormalizeConfiguredOrder() {
        List<SidebarTab> tabs = SidebarTab.resolveOrderedTabs(List.of(
                "TOOLBOX",
                "COLLECTIONS",
                "unknown",
                "TOOLBOX"
        ));

        assertEquals(tabs.get(0), SidebarTab.TOOLBOX);
        assertEquals(tabs.get(1), SidebarTab.COLLECTIONS);
        assertEquals(tabs.size(), SidebarTab.values().length);
    }

    @Test(description = "当隐藏配置把所有菜单都隐藏时，应回退到默认可见列表")
    public void testResolveVisibleTabs_ShouldFallbackWhenAllTabsHidden() {
        Set<String> hiddenTabs = Set.of(
                "COLLECTIONS",
                "ENVIRONMENTS",
                "WORKSPACES",
                "FUNCTIONAL",
                "PERFORMANCE",
                "TOOLBOX",
                "HISTORY"
        );

        List<SidebarTab> tabs = SidebarTab.resolveVisibleTabs(List.of("HISTORY", "TOOLBOX"), hiddenTabs);

        assertEquals(tabs, List.of(SidebarTab.values()));
    }
}
