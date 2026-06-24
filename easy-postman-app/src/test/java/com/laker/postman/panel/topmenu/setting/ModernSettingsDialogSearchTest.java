package com.laker.postman.panel.topmenu.setting;

import org.testng.annotations.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class ModernSettingsDialogSearchTest {

    @Test
    public void settingsDialogShouldWireSearchFieldToFilterVisibleSettingsPages() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/com/laker/postman/panel/topmenu/setting/ModernSettingsDialog.java"
        ));

        assertTrue(source.contains("settingsSearchField"));
        assertTrue(source.contains("createSettingsNavigationPanel()"));
        assertTrue(source.contains("mainPanel.add(createSettingsNavigationPanel(), BorderLayout.WEST)"));
        assertTrue(source.contains("CardLayout"));
        assertTrue(source.contains("settingsNavigationTree"));
        assertTrue(source.contains("SettingsSearchSupport.filter(settingsPages"));
        assertTrue(source.contains("locateSearchMatch"));
        assertTrue(source.contains("SettingsSearchSupport.firstMatch"));
        assertTrue(source.contains("settingsSearchField.setNoResult"));
        assertTrue(source.contains("selectedPage != null && matchedPages.contains(selectedPage)"));
        assertTrue(source.contains("SEARCH_MATCH_HIGHLIGHT_DURATION_MS"));
        assertTrue(source.contains("SearchMatchHighlightBorder"));
        assertTrue(source.contains("searchMatchFillColor()"));
        assertTrue(source.contains("ModernColors.getWarning()"));
        assertTrue(source.contains("STROKE_WIDTH = 1f"));
        assertFalse(source.contains("ModernColors.getPrimary()"));
        assertFalse(source.contains("BorderFactory.createLineBorder(SEARCH_MATCH_BORDER_COLOR, 2, true)"));
    }

    @Test
    public void settingsNavigationShouldUseDedicatedSidebarInteractionStyle() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/com/laker/postman/panel/topmenu/setting/ModernSettingsDialog.java"
        ));

        assertTrue(source.contains("new SettingsNavigationTree(settingsNavigationTreeModel)"));
        assertTrue(source.contains("SETTINGS_NAVIGATION_ROW_HEIGHT"));
        assertTrue(source.contains("tree.setToggleClickCount(0)"));
        assertTrue(source.contains("paintSelection: false"));
        assertTrue(source.contains("new SettingsNavigationTreeUi()"));
        assertTrue(source.contains("SETTINGS_NAVIGATION_TREE_LEFT_GUTTER = 16"));
        assertTrue(source.contains("SETTINGS_NAVIGATION_TEXT_LEFT_INSET = 8"));
        assertTrue(source.contains("ROW_BACKGROUND_HORIZONTAL_INSET = 12"));
        assertTrue(source.contains("setLeftChildIndent(2)"));
        assertTrue(source.contains("setRightChildIndent(6)"));
        assertFalse(source.contains("leftChildIndent:"));
        assertFalse(source.contains("rightChildIndent:"));
        assertFalse(source.contains("iconTextGap:"));
        assertTrue(source.contains("navigationSelectionBackgroundColor()"));
        assertTrue(source.contains("navigationHoverBackgroundColor()"));
        assertTrue(source.contains("setOpaque(false)"));
    }

    @Test
    public void settingsNavigationShouldUseModernColorsWithoutDefaultTreeSelectionPaint() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/com/laker/postman/panel/topmenu/setting/ModernSettingsDialog.java"
        ));

        assertTrue(source.contains("implements TreeCellRenderer"));
        assertFalse(source.contains("extends DefaultTreeCellRenderer"));
        assertTrue(source.contains("ModernColors.getSelectionBackgroundColor()"));
        assertTrue(source.contains("ModernColors.getHoverBackgroundColor()"));
    }

    @Test
    public void settingsNavigationCategoryClickShouldToggleWithoutSelectingFirstChildPage() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/com/laker/postman/panel/topmenu/setting/ModernSettingsDialog.java"
        ));

        assertTrue(source.contains("toggleCategoryPath"));
        assertTrue(source.contains("expandAllNavigationRows()"));
        assertTrue(source.contains("processMouseEvent"));
        assertTrue(source.contains("getClosestRowForLocation"));
        assertTrue(source.contains("e.consume()"));
        assertFalse(source.contains("collapseOtherCategoriesExcept"));
        assertFalse(source.contains("expandSelectedCategoryOnly"));
        assertFalse(source.contains("categoryNode.getFirstChild() instanceof SettingsPageTreeNode pageNode"));
    }
}
