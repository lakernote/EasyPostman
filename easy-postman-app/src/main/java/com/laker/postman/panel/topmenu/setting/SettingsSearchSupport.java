package com.laker.postman.panel.topmenu.setting;

import com.formdev.flatlaf.FlatClientProperties;
import lombok.experimental.UtilityClass;

import javax.accessibility.AccessibleContext;
import javax.swing.AbstractButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPasswordField;
import javax.swing.JTabbedPane;
import javax.swing.text.JTextComponent;
import java.awt.Component;
import java.awt.Container;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

@UtilityClass
class SettingsSearchSupport {

    static List<SettingsSearchPage> filter(Collection<SettingsSearchPage> pages, String query) {
        if (pages == null || pages.isEmpty()) {
            return List.of();
        }
        String normalizedQuery = normalize(query);
        if (normalizedQuery.isEmpty()) {
            return pages.stream()
                    .filter(Objects::nonNull)
                    .toList();
        }

        List<SettingsSearchPage> matches = new ArrayList<>();
        for (SettingsSearchPage page : pages) {
            if (matches(page, normalizedQuery)) {
                matches.add(page);
            }
        }
        return List.copyOf(matches);
    }

    static boolean matches(SettingsSearchPage page, String query) {
        return firstMatch(page, query).isPresent();
    }

    static Optional<SettingsSearchMatch> firstMatch(SettingsSearchPage page, String query) {
        if (page == null) {
            return Optional.empty();
        }
        String normalizedQuery = normalize(query);
        if (normalizedQuery.isEmpty()) {
            return Optional.of(new SettingsSearchMatch(page, page.component()));
        }

        initializePageForSearch(page.component());
        List<SearchableTextEntry> searchableText = new ArrayList<>();
        collectSearchableText(page.component(), searchableText);
        Optional<SettingsSearchMatch> componentMatch = searchableText.stream()
                .filter(entry -> matchesText(entry.text(), normalizedQuery))
                .min(Comparator.comparing(SearchableTextEntry::priority))
                .map(entry -> new SettingsSearchMatch(page, entry.component()));
        if (componentMatch.isPresent()) {
            return componentMatch;
        }
        if (matchesText(page.title(), normalizedQuery)) {
            return Optional.of(new SettingsSearchMatch(page, page.component()));
        }
        return Optional.empty();
    }

    private static void collectSearchableText(Component component, List<SearchableTextEntry> searchableText) {
        if (component == null) {
            return;
        }
        if (component instanceof JComponent jComponent) {
            addText(searchableText, component, jComponent.getToolTipText(), SearchTextPriority.SECONDARY);
            AccessibleContext accessibleContext = jComponent.getAccessibleContext();
            if (accessibleContext != null) {
                addText(searchableText, component, accessibleContext.getAccessibleName(), SearchTextPriority.SECONDARY);
                addText(searchableText, component, accessibleContext.getAccessibleDescription(), SearchTextPriority.SECONDARY);
            }
            addText(
                    searchableText,
                    component,
                    jComponent.getClientProperty(FlatClientProperties.PLACEHOLDER_TEXT),
                    SearchTextPriority.SECONDARY
            );
        }
        if (component instanceof JLabel label) {
            addText(searchableText, component, label.getText(), SearchTextPriority.VISIBLE);
        }
        if (component instanceof AbstractButton button) {
            addText(searchableText, component, button.getText(), SearchTextPriority.VISIBLE);
        }
        if (component instanceof JTextComponent textComponent
                && !(component instanceof JPasswordField)
                && !textComponent.isEditable()) {
            addText(searchableText, component, textComponent.getText(), SearchTextPriority.VISIBLE);
        }
        if (component instanceof JComboBox<?> comboBox) {
            for (int i = 0; i < comboBox.getItemCount(); i++) {
                addText(searchableText, component, comboBox.getItemAt(i), SearchTextPriority.SECONDARY);
            }
        }
        if (component instanceof JTabbedPane tabbedPane) {
            for (int i = 0; i < tabbedPane.getTabCount(); i++) {
                addText(searchableText, component, tabbedPane.getTitleAt(i), SearchTextPriority.VISIBLE);
            }
        }
        if (component instanceof Container container) {
            for (Component child : container.getComponents()) {
                collectSearchableText(child, searchableText);
            }
        }
    }

    private static void initializePageForSearch(JComponent component) {
        if (component instanceof ModernSettingsPanel panel) {
            panel.getPreferredSize();
        }
    }

    private static void addText(List<SearchableTextEntry> searchableText,
                                Component component,
                                Object value,
                                SearchTextPriority priority) {
        if (value == null) {
            return;
        }
        String text = stripHtml(String.valueOf(value));
        if (!normalize(text).isEmpty()) {
            searchableText.add(new SearchableTextEntry(text, component, priority));
        }
    }

    private static boolean matchesText(String text, String normalizedQuery) {
        String normalizedText = normalize(text);
        if (normalizedText.contains(normalizedQuery)) {
            return true;
        }
        String[] terms = normalizedQuery.split(" ");
        for (String term : terms) {
            if (!term.isEmpty() && !normalizedText.contains(term)) {
                return false;
            }
        }
        return terms.length > 0;
    }

    private static String stripHtml(String text) {
        return text == null ? "" : text
                .replaceAll("<[^>]*>", " ")
                .replace("&nbsp;", " ")
                .replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">");
    }

    private static String normalize(String value) {
        return value == null
                ? ""
                : value.toLowerCase(Locale.ROOT).trim().replaceAll("\\s+", " ");
    }

    record SettingsSearchPage(String id, String title, String category, JComponent component) {
        SettingsSearchPage(String title, JComponent component) {
            this(title, title, "", component);
        }

        SettingsSearchPage {
            id = id == null || id.isBlank() ? title : id;
            title = title == null ? "" : title;
            category = category == null ? "" : category;
            component = Objects.requireNonNull(component, "component");
        }
    }

    record SettingsSearchMatch(SettingsSearchPage page, Component component) {
        SettingsSearchMatch {
            page = Objects.requireNonNull(page, "page");
            component = component == null ? page.component() : component;
        }
    }

    private enum SearchTextPriority {
        VISIBLE,
        SECONDARY
    }

    private record SearchableTextEntry(String text, Component component, SearchTextPriority priority) {
    }
}
