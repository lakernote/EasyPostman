package com.laker.postman.common.component;

import com.laker.postman.test.AbstractSwingUiTest;
import org.fife.ui.rsyntaxtextarea.DocumentRange;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.testng.annotations.Test;

import javax.swing.*;
import javax.swing.text.JTextComponent;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

import static org.testng.Assert.assertEquals;

public class SearchReplacePanelTest extends AbstractSwingUiTest {

    @Test
    public void shouldCountMatchesWithoutTriggeringMarkAllHighlights() throws Exception {
        CountingTextArea[] textAreaHolder = new CountingTextArea[1];
        int[] matchCount = new int[1];

        SwingUtilities.invokeAndWait(() -> {
            CountingTextArea textArea = new CountingTextArea();
            textArea.setText("abc ".repeat(1_000));
            SearchReplacePanel panel = new SearchReplacePanel(textArea, false);
            setSearchText(panel, "a");
            stopSearchDebounceTimer(panel);

            textArea.markAllCalls = 0;
            matchCount[0] = calculateTotalMatches(panel);
            textAreaHolder[0] = textArea;
        });

        assertEquals(matchCount[0], 1_000);
        assertEquals(textAreaHolder[0].markAllCalls, 0);
    }

    @Test
    public void shouldRefreshLiveSearchWithoutTriggeringMarkAllHighlights() throws Exception {
        CountingTextArea[] textAreaHolder = new CountingTextArea[1];
        String[] statusText = new String[1];

        SwingUtilities.invokeAndWait(() -> {
            CountingTextArea textArea = new CountingTextArea();
            textArea.setText("abc ".repeat(1_000));
            SearchReplacePanel panel = new SearchReplacePanel(textArea, false);
            setSearchText(panel, "a");
            stopSearchDebounceTimer(panel);

            textArea.markAllCalls = 0;
            updateSearchStatus(panel);
            textAreaHolder[0] = textArea;
            statusText[0] = getStatusText(panel);
        });

        assertEquals(statusText[0], "1 of 1000");
        assertEquals(textAreaHolder[0].markAllCalls, 0);
    }

    @Test
    public void shouldRespectCaseSensitiveOptionWhenCountingMatches() throws Exception {
        int[] matchCounts = new int[2];

        SwingUtilities.invokeAndWait(() -> {
            SearchReplacePanel panel = createPanel("Alpha alpha ALPHA", "alpha");

            matchCounts[0] = calculateTotalMatches(panel);
            setSearchOption(panel, "caseSensitive", true);
            matchCounts[1] = calculateTotalMatches(panel);
        });

        assertEquals(matchCounts[0], 3);
        assertEquals(matchCounts[1], 1);
    }

    @Test
    public void shouldRespectWholeWordOptionWhenCountingMatches() throws Exception {
        int[] matchCounts = new int[2];

        SwingUtilities.invokeAndWait(() -> {
            SearchReplacePanel panel = createPanel("cat concatenate cat1 cat", "cat");

            matchCounts[0] = calculateTotalMatches(panel);
            setSearchOption(panel, "wholeWord", true);
            matchCounts[1] = calculateTotalMatches(panel);
        });

        assertEquals(matchCounts[0], 4);
        assertEquals(matchCounts[1], 2);
    }

    private SearchReplacePanel createPanel(String body, String searchText) {
        CountingTextArea textArea = new CountingTextArea();
        textArea.setText(body);
        SearchReplacePanel panel = new SearchReplacePanel(textArea, false);
        setSearchText(panel, searchText);
        stopSearchDebounceTimer(panel);
        return panel;
    }

    private void setSearchText(SearchReplacePanel panel, String text) {
        try {
            JTextComponent searchField = getField(panel, "searchField", JTextComponent.class);
            searchField.setText(text);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }

    private void stopSearchDebounceTimer(SearchReplacePanel panel) {
        try {
            Timer timer = getField(panel, "searchDebounceTimer", Timer.class);
            if (timer != null) {
                timer.stop();
            }
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }

    private void setSearchOption(SearchReplacePanel panel, String optionFieldName, boolean value) {
        try {
            SearchTextField searchField = getField(panel, "searchField", SearchTextField.class);
            Field optionField = SearchTextField.class.getDeclaredField(optionFieldName);
            optionField.setAccessible(true);
            optionField.setBoolean(searchField, value);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }

    private int calculateTotalMatches(SearchReplacePanel panel) {
        try {
            Method method = SearchReplacePanel.class.getDeclaredMethod("calculateTotalMatches");
            method.setAccessible(true);
            return (int) method.invoke(panel);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }

    private void updateSearchStatus(SearchReplacePanel panel) {
        try {
            Method method = SearchReplacePanel.class.getDeclaredMethod("updateSearchStatus");
            method.setAccessible(true);
            method.invoke(panel);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }

    private String getStatusText(SearchReplacePanel panel) {
        try {
            return getField(panel, "statusLabel", JLabel.class).getText();
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }

    private <T> T getField(SearchReplacePanel panel, String fieldName, Class<T> fieldType)
            throws ReflectiveOperationException {
        Field field = SearchReplacePanel.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        return fieldType.cast(field.get(panel));
    }

    private static class CountingTextArea extends RSyntaxTextArea {
        private int markAllCalls;

        @Override
        public void markAll(List<DocumentRange> ranges) {
            markAllCalls++;
            super.markAll(ranges);
        }
    }
}
