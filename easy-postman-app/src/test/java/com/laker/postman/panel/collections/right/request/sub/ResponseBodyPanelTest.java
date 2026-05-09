package com.laker.postman.panel.collections.right.request.sub;

import com.laker.postman.common.component.button.WrapToggleButton;
import com.laker.postman.model.HttpResponse;
import com.laker.postman.test.AbstractSwingUiTest;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.testng.annotations.Test;

import javax.swing.*;
import java.awt.*;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

public class ResponseBodyPanelTest extends AbstractSwingUiTest {

    @Test
    public void shouldNotAutoEnableLineWrapForSmallJsonResponseWithVeryLongLine() throws Exception {
        HttpResponse response = responseWithBody(longLineJson());

        ResponseBodyPanel panel = createPanelWithResponse(response);

        assertFalse(panel.getResponseBodyPane().getLineWrap());
    }

    @Test
    public void shouldKeepManualLineWrapSelectionAcrossResponses() throws Exception {
        ResponseBodyPanel panel = createPanelWithResponse(responseWithBody("{\"ok\":true}"));
        WrapToggleButton wrapButton = findWrapButton(panel);
        assertNotNull(wrapButton);

        SwingUtilities.invokeAndWait(() -> {
            wrapButton.doClick();
            panel.setBodyText(responseWithBody(longLineJson()));
            panel.setBodyText(responseWithBody("{\"ok\":true}"));
        });

        assertTrue(panel.getResponseBodyPane().getLineWrap());
    }

    @Test
    public void shouldUseViewportClippedTokenPainterForResponseRendering() throws Exception {
        ResponseBodyPanel panel = createPanelWithResponse(responseWithBody(longLineJson()));

        Object tokenPainter = getTokenPainter(panel);

        assertEquals(tokenPainter.getClass().getName(),
                "com.laker.postman.common.component.ViewportClippedTokenPainter");
    }

    @Test
    public void shouldDisableMatchedBracketPopupForResponseEditor() throws Exception {
        ResponseBodyPanel panel = createPanelWithResponse(responseWithBody(longLineJson()));

        assertFalse(panel.getResponseBodyPane().getShowMatchedBracketPopup());
    }

    @Test
    public void shouldExposeJsonCopyActionsInResponseEditorPopupMenu() throws Exception {
        ResponseBodyPanel panel = createPanelWithResponse(responseWithBody("{\"data\":\"value\"}"));

        JPopupMenu popupMenu = panel.getResponseBodyPane().getPopupMenu();

        assertTrue(hasMenuItem(popupMenu, I18nUtil.getMessage(MessageKeys.RESPONSE_BODY_COPY_JSON_KEY)));
        assertTrue(hasMenuItem(popupMenu, I18nUtil.getMessage(MessageKeys.RESPONSE_BODY_COPY_JSON_VALUE)));
    }

    @Test
    public void shouldUseCompactLocalizedResponseEditorPopupMenu() throws Exception {
        ResponseBodyPanel panel = createPanelWithResponse(responseWithBody("{\"data\":\"value\"}"));

        JPopupMenu popupMenu = panel.getResponseBodyPane().getPopupMenu();

        assertFalse(hasAnyMenuItem(popupMenu, "Undo", "Can't Redo", "Cut", "Paste", "Delete",
                "撤销", "无法恢复", "剪切", "粘贴", "删除"));
        assertTrue(hasMenuItem(popupMenu, I18nUtil.getMessage(MessageKeys.RESPONSE_BODY_CONTEXT_COPY_SELECTED)));
        assertTrue(hasMenuItem(popupMenu, I18nUtil.getMessage(MessageKeys.RESPONSE_BODY_CONTEXT_COPY_ALL)));
        assertTrue(hasMenuItem(popupMenu, I18nUtil.getMessage(MessageKeys.RESPONSE_BODY_CONTEXT_SELECT_ALL)));
        assertTrue(hasMenuItem(popupMenu, I18nUtil.getMessage(MessageKeys.RESPONSE_BODY_CONTEXT_FOLDING)));
    }

    @Test
    public void shouldKeepResponseEditorCopyAllMenuEnabledWithoutSelection() throws Exception {
        ResponseBodyPanel panel = createPanelWithResponse(responseWithBody("{\"data\":\"value\"}"));
        JPopupMenu popupMenu = panel.getResponseBodyPane().getPopupMenu();

        configurePopupMenu(panel.getResponseBodyPane(), popupMenu);

        JMenuItem copyItem = findMenuItem(popupMenu, I18nUtil.getMessage(MessageKeys.RESPONSE_BODY_CONTEXT_COPY_ALL));
        assertNotNull(copyItem);
        assertTrue(copyItem.isEnabled());
    }

    @Test
    public void shouldKeepResponseEditorCopySelectedMenuEnabledWithSelection() throws Exception {
        ResponseBodyPanel panel = createPanelWithResponse(responseWithBody("{\"data\":\"value\"}"));
        JPopupMenu popupMenu = panel.getResponseBodyPane().getPopupMenu();

        SwingUtilities.invokeAndWait(() -> panel.getResponseBodyPane().select(1, 7));
        configurePopupMenu(panel.getResponseBodyPane(), popupMenu);

        JMenuItem copyItem = findMenuItem(popupMenu, I18nUtil.getMessage(MessageKeys.RESPONSE_BODY_CONTEXT_COPY_SELECTED));
        assertNotNull(copyItem);
        assertTrue(copyItem.isEnabled());
    }

    @Test
    public void shouldKeepSelectionWhenContextPopupOpensInsideSelection() throws Exception {
        ResponseBodyPanel panel = createPanelWithResponse(responseWithBody("{\"data\":\"value\"}"));
        RSyntaxTextArea textArea = panel.getResponseBodyPane();

        SwingUtilities.invokeAndWait(() -> {
            textArea.select(1, 7);
            panel.rememberJsonPopupOffset(3);
        });

        assertEquals(textArea.getSelectionStart(), 1);
        assertEquals(textArea.getSelectionEnd(), 7);
    }

    @Test
    public void shouldMoveCaretWhenContextPopupOpensOutsideSelection() throws Exception {
        ResponseBodyPanel panel = createPanelWithResponse(responseWithBody("{\"data\":\"value\"}"));
        RSyntaxTextArea textArea = panel.getResponseBodyPane();

        SwingUtilities.invokeAndWait(() -> {
            textArea.select(1, 7);
            panel.rememberJsonPopupOffset(7);
        });

        assertEquals(textArea.getSelectionStart(), 7);
        assertEquals(textArea.getSelectionEnd(), 7);
    }

    private ResponseBodyPanel createPanelWithResponse(HttpResponse response) throws Exception {
        ResponseBodyPanel[] holder = new ResponseBodyPanel[1];
        SwingUtilities.invokeAndWait(() -> {
            ResponseBodyPanel panel = new ResponseBodyPanel(false);
            panel.setBodyText(response);
            holder[0] = panel;
        });
        return holder[0];
    }

    private HttpResponse responseWithBody(String body) {
        HttpResponse response = new HttpResponse();
        response.body = body;
        response.bodySize = response.body.getBytes(StandardCharsets.UTF_8).length;
        response.headers = Map.of("Content-Type", List.of("application/json"));
        return response;
    }

    private String longLineJson() {
        return """
                {
                    "choices": [
                        {
                            "message": {
                                "content": "%s"
                            }
                        }
                    ]
                }
                """.formatted("a".repeat(4_500));
    }

    private WrapToggleButton findWrapButton(Container container) {
        for (Component component : container.getComponents()) {
            if (component instanceof WrapToggleButton wrapToggleButton) {
                return wrapToggleButton;
            }
            if (component instanceof Container child) {
                WrapToggleButton found = findWrapButton(child);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private Object getTokenPainter(ResponseBodyPanel panel) {
        try {
            Field field = RSyntaxTextArea.class.getDeclaredField("tokenPainter");
            field.setAccessible(true);
            return field.get(panel.getResponseBodyPane());
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }

    private boolean hasMenuItem(JPopupMenu popupMenu, String text) {
        return findMenuItem(popupMenu, text) != null;
    }

    private JMenuItem findMenuItem(JPopupMenu popupMenu, String text) {
        for (Component component : popupMenu.getComponents()) {
            if (component instanceof JMenuItem menuItem && text.equals(menuItem.getText())) {
                return menuItem;
            }
        }
        return null;
    }

    private boolean hasAnyMenuItem(JPopupMenu popupMenu, String... texts) {
        for (String text : texts) {
            if (hasMenuItem(popupMenu, text)) {
                return true;
            }
        }
        return false;
    }

    private void configurePopupMenu(RSyntaxTextArea textArea, JPopupMenu popupMenu) {
        try {
            Method method = RSyntaxTextArea.class.getDeclaredMethod("configurePopupMenu", JPopupMenu.class);
            method.setAccessible(true);
            method.invoke(textArea, popupMenu);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }
}
