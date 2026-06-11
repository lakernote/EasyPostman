package com.laker.postman.common.component;

import com.laker.postman.util.I18nUtil;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.testng.annotations.Test;

import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.KeyStroke;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
import javax.swing.event.PopupMenuEvent;
import javax.swing.text.DefaultEditorKit;
import java.awt.Component;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.lang.reflect.Field;
import java.util.Locale;
import java.util.concurrent.ConcurrentMap;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class SearchableTextAreaTest {

    @Test
    public void shouldBindLineSelectionShortcutsAcrossPlatforms() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            RSyntaxTextArea textArea = new RSyntaxTextArea();

            new SearchableTextArea(textArea);

            assertEditorAction(textArea, KeyEvent.VK_HOME, 0, DefaultEditorKit.beginLineAction);
            assertEditorAction(textArea, KeyEvent.VK_END, 0, DefaultEditorKit.endLineAction);
            assertEditorAction(textArea, KeyEvent.VK_HOME, InputEvent.SHIFT_DOWN_MASK,
                    DefaultEditorKit.selectionBeginLineAction);
            assertEditorAction(textArea, KeyEvent.VK_END, InputEvent.SHIFT_DOWN_MASK,
                    DefaultEditorKit.selectionEndLineAction);
            assertEditorAction(textArea, KeyEvent.VK_LEFT,
                    InputEvent.META_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK,
                    DefaultEditorKit.selectionBeginLineAction);
            assertEditorAction(textArea, KeyEvent.VK_RIGHT,
                    InputEvent.META_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK,
                    DefaultEditorKit.selectionEndLineAction);
        });
    }

    @Test
    public void shouldLocalizeDefaultEditorPopupMenu() throws Exception {
        Locale originalLocale = forceCurrentLocale(Locale.CHINESE);
        try {
            SwingUtilities.invokeAndWait(() -> {
                RSyntaxTextArea textArea = new RSyntaxTextArea();

                new SearchableTextArea(textArea);

                JPopupMenu popupMenu = textArea.getPopupMenu();
                assertTrue(hasMenuTextContaining(popupMenu, "全选"));
                assertTrue(hasMenuTextContaining(popupMenu, "折叠"));
                assertFalse(hasMenuTextContaining(popupMenu, "Select All"));
                assertFalse(hasMenuTextContaining(popupMenu, "Folding"));
            });
        } finally {
            forceCurrentLocale(originalLocale);
        }
    }

    @Test
    public void shouldExposeCompactCurrentPlatformShortcutHintInEditorPopupMenu() throws Exception {
        Locale originalLocale = forceCurrentLocale(Locale.CHINESE);
        try {
            SwingUtilities.invokeAndWait(() -> {
                RSyntaxTextArea textArea = new RSyntaxTextArea();

                new SearchableTextArea(textArea);

                JPopupMenu popupMenu = textArea.getPopupMenu();
                assertEquals(countMenuTextContaining(popupMenu, "选中到行首/尾"), 1);
                assertFalse(hasMenuTextContaining(popupMenu, "Windows/Linux"));
                assertFalse(hasMenuTextContaining(popupMenu, "macOS"));
                if (isMac()) {
                    assertTrue(hasMenuTextContaining(popupMenu, "⇧⌘←/⇧⌘→"));
                } else {
                    assertTrue(hasMenuTextContaining(popupMenu, "Shift+Home/End"));
                }
            });
        } finally {
            forceCurrentLocale(originalLocale);
        }
    }

    @Test
    public void shouldExposeJsonCopyActionsInSharedEditorPopupMenu() throws Exception {
        Locale originalLocale = forceCurrentLocale(Locale.CHINESE);
        try {
            SwingUtilities.invokeAndWait(() -> {
                RSyntaxTextArea textArea = new RSyntaxTextArea();

                new SearchableTextArea(textArea);

                JPopupMenu popupMenu = textArea.getPopupMenu();
                assertTrue(hasMenuTextContaining(popupMenu, "复制键"));
                assertTrue(hasMenuTextContaining(popupMenu, "复制值"));
                assertTrue(hasMenuTextContaining(popupMenu, "复制选中"));
                assertTrue(hasMenuTextContaining(popupMenu, "复制全部"));
            });
        } finally {
            forceCurrentLocale(originalLocale);
        }
    }

    @Test
    public void shouldEnableJsonCopyActionsForJsonPropertyAtCaret() throws Exception {
        Locale originalLocale = forceCurrentLocale(Locale.CHINESE);
        try {
            SwingUtilities.invokeAndWait(() -> {
                RSyntaxTextArea textArea = new RSyntaxTextArea();
                SearchableTextArea searchableTextArea = new SearchableTextArea(textArea);
                textArea.setText("{\"data\":\"value\"}");
                searchableTextArea.rememberJsonPopupOffset(2);

                firePopupWillBecomeVisible(textArea.getPopupMenu());

                assertTrue(findMenuItem(textArea.getPopupMenu(), "复制键").isEnabled());
                assertTrue(findMenuItem(textArea.getPopupMenu(), "复制值").isEnabled());
            });
        } finally {
            forceCurrentLocale(originalLocale);
        }
    }

    @Test
    public void shouldKeepSelectionWhenSharedPopupOpensInsideSelection() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            RSyntaxTextArea textArea = new RSyntaxTextArea();
            SearchableTextArea searchableTextArea = new SearchableTextArea(textArea);
            textArea.setText("{\"data\":\"value\"}");
            textArea.select(1, 7);

            searchableTextArea.rememberJsonPopupOffset(3);

            assertEquals(textArea.getSelectionStart(), 1);
            assertEquals(textArea.getSelectionEnd(), 7);
        });
    }

    @Test
    public void shouldMoveCaretWhenSharedPopupOpensOutsideSelection() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            RSyntaxTextArea textArea = new RSyntaxTextArea();
            SearchableTextArea searchableTextArea = new SearchableTextArea(textArea);
            textArea.setText("{\"data\":\"value\"}");
            textArea.select(1, 7);

            searchableTextArea.rememberJsonPopupOffset(7);

            assertEquals(textArea.getSelectionStart(), 7);
            assertEquals(textArea.getSelectionEnd(), 7);
        });
    }

    private void assertEditorAction(RSyntaxTextArea textArea, int keyCode, int modifiers, String expectedAction) {
        Object action = textArea.getInputMap(JComponent.WHEN_FOCUSED)
                .get(KeyStroke.getKeyStroke(keyCode, modifiers));

        assertEquals(action, expectedAction);
    }

    private boolean hasMenuTextContaining(JPopupMenu popupMenu, String text) {
        return countMenuTextContaining(popupMenu, text) > 0;
    }

    private int countMenuTextContaining(JPopupMenu popupMenu, String text) {
        int count = 0;
        for (Component component : popupMenu.getComponents()) {
            if (component instanceof JMenu menu) {
                count += countMenuTextContaining(menu, text);
            }
            if (component instanceof JMenuItem menuItem && menuItem.getText() != null
                    && menuItem.getText().contains(text)) {
                count++;
            }
        }
        return count;
    }

    private JMenuItem findMenuItem(JPopupMenu popupMenu, String text) {
        for (Component component : popupMenu.getComponents()) {
            if (component instanceof JMenuItem menuItem && text.equals(menuItem.getText())) {
                return menuItem;
            }
        }
        throw new AssertionError("Menu item not found: " + text);
    }

    private void firePopupWillBecomeVisible(JPopupMenu popupMenu) {
        PopupMenuEvent event = new PopupMenuEvent(popupMenu);
        for (var listener : popupMenu.getPopupMenuListeners()) {
            listener.popupMenuWillBecomeVisible(event);
        }
    }

    private int countMenuTextContaining(JMenu menu, String text) {
        int count = 0;
        if (menu.getText() != null && menu.getText().contains(text)) {
            count++;
        }
        for (Component component : menu.getMenuComponents()) {
            if (component instanceof JMenu childMenu) {
                count += countMenuTextContaining(childMenu, text);
            }
            if (component instanceof JMenuItem menuItem && menuItem.getText() != null
                    && menuItem.getText().contains(text)) {
                count++;
            }
        }
        return count;
    }

    private Locale forceCurrentLocale(Locale locale) throws Exception {
        Field currentLocaleField = I18nUtil.class.getDeclaredField("currentLocale");
        currentLocaleField.setAccessible(true);
        Locale original = (Locale) currentLocaleField.get(null);
        currentLocaleField.set(null, locale);

        Field cacheField = I18nUtil.class.getDeclaredField("BUNDLE_CACHE");
        cacheField.setAccessible(true);
        ((ConcurrentMap<?, ?>) cacheField.get(null)).clear();
        return original;
    }

    private boolean isMac() {
        return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("mac");
    }
}
