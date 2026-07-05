package com.laker.postman.panel.topmenu.setting;

import com.formdev.flatlaf.FlatClientProperties;
import com.laker.postman.panel.topmenu.setting.SettingsSearchSupport.SettingsSearchMatch;
import com.laker.postman.panel.topmenu.setting.SettingsSearchSupport.SettingsSearchPage;
import org.testng.annotations.Test;

import javax.swing.AbstractButton;
import javax.swing.Box;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class SettingsSearchSupportTest {

    @Test
    public void blankQueryShouldReturnAllPagesInOriginalOrder() {
        List<SettingsSearchPage> pages = List.of(
                page("General", new JPanel()),
                page("Request", new JPanel()),
                page("Network Proxy", new JPanel())
        );

        List<SettingsSearchPage> filtered = SettingsSearchSupport.filter(pages, "  ");

        assertEquals(filtered.stream().map(SettingsSearchPage::title).toList(),
                List.of("General", "Request", "Network Proxy"));
    }

    @Test
    public void queryShouldMatchPageTitleAndNestedVisibleText() {
        JPanel requestPanel = new JPanel();
        requestPanel.add(new JLabel("Default protocol"));
        requestPanel.add(new JCheckBox("Disable SSL certificate validation"));

        List<SettingsSearchPage> pages = List.of(
                page("General", new JPanel()),
                page("Request", requestPanel)
        );

        assertEquals(SettingsSearchSupport.filter(pages, "request").stream()
                        .map(SettingsSearchPage::title).toList(),
                List.of("Request"));
        assertEquals(SettingsSearchSupport.filter(pages, "ssl certificate").stream()
                        .map(SettingsSearchPage::title).toList(),
                List.of("Request"));
    }

    @Test
    public void queryShouldMatchTooltipsComboItemsAndPlaceholders() {
        JPanel panel = new JPanel();
        AbstractButton button = new JCheckBox("Show progress dialog");
        button.setToolTipText("Display progress before downloading large files");
        panel.add(button);
        panel.add(new JComboBox<>(new String[]{"Top Right", "Bottom Center"}));
        JTextField field = new JTextField();
        field.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Git Diff large file threshold");
        panel.add(field);

        SettingsSearchPage page = page("General", panel);

        assertTrue(SettingsSearchSupport.matches(page, "large files"));
        assertTrue(SettingsSearchSupport.matches(page, "bottom center"));
        assertTrue(SettingsSearchSupport.matches(page, "git diff"));
    }

    @Test
    public void queryShouldReturnFirstMatchingComponentForPageLocation() {
        JPanel panel = new JPanel();
        JLabel first = new JLabel("Maximum history count");
        JCheckBox second = new JCheckBox("Show request tabs in multiple rows");
        panel.add(first);
        panel.add(second);

        SettingsSearchMatch match =
                SettingsSearchSupport.firstMatch(page("General", panel), "request rows").orElseThrow();

        assertEquals(match.component(), second);
    }

    @Test
    public void visibleTextMatchShouldBePreferredOverEarlierTooltipMatchForPageLocation() {
        JPanel panel = new JPanel();
        JCheckBox requestTabs = new JCheckBox("请求标签页多行显示");
        requestTabs.setToolTipText("开启后，请求编辑器顶部打开的请求标签页会自动换行显示");
        JLabel editorFontSection = new JLabel("编辑器字体");
        panel.add(requestTabs);
        panel.add(editorFontSection);

        SettingsSearchMatch match =
                SettingsSearchSupport.firstMatch(page("通用设置", panel), "编辑器").orElseThrow();

        assertEquals(match.component(), editorFontSection);
    }

    @Test
    public void queryTermsShouldMatchWhenTheyAppearInTheSameSettingText() {
        JPanel panel = new JPanel();
        JTextField field = new JTextField();
        field.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Git Diff large file threshold");
        panel.add(field);

        assertTrue(SettingsSearchSupport.matches(page("General", panel), "git threshold"));
        assertEquals(SettingsSearchSupport.firstMatch(page("General", panel), "git threshold").orElseThrow().component(),
                field);
    }

    @Test
    public void searchShouldInitializeLazySettingsPanelsBeforeScanning() {
        LazySearchPanel panel = new LazySearchPanel();

        List<SettingsSearchPage> filtered =
                SettingsSearchSupport.filter(List.of(page("Lazy", panel)), "lazy option");

        assertEquals(filtered.size(), 1);
        assertTrue(panel.buildContentCalled);
    }

    private static SettingsSearchPage page(String title, JComponent component) {
        return new SettingsSearchPage(title, component);
    }

    private static final class LazySearchPanel extends ModernSettingsPanel {
        private boolean buildContentCalled;

        @Override
        protected void buildContent(JPanel contentPanel) {
            buildContentCalled = true;
            contentPanel.add(new JLabel("Lazy option"));
        }

        @Override
        protected void registerListeners() {
            // No listeners needed for search indexing.
        }
    }
}
