package com.laker.postman.panel.collections.editor.request.sub;

import com.laker.postman.common.component.FallbackAwareRSyntaxTextArea;
import com.laker.postman.common.component.button.WrapToggleButton;
import com.laker.postman.http.runtime.model.HttpResponse;
import com.laker.postman.test.AbstractSwingUiTest;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;
import com.laker.postman.util.UiI18n;
import com.laker.postman.util.UiMessageKeys;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.testng.annotations.Test;

import javax.swing.*;
import javax.swing.event.PopupMenuEvent;
import java.awt.*;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
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

        assertTrue(panel.getResponseBodyPane() instanceof FallbackAwareRSyntaxTextArea);
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

        assertTrue(hasMenuItem(popupMenu, UiI18n.get(UiMessageKeys.EDITOR_POPUP_COPY_KEY)));
        assertTrue(hasMenuItem(popupMenu, UiI18n.get(UiMessageKeys.EDITOR_POPUP_COPY_VALUE)));
    }

    @Test
    public void shouldUseCompactLocalizedResponseEditorPopupMenu() throws Exception {
        ResponseBodyPanel panel = createPanelWithResponse(responseWithBody("{\"data\":\"value\"}"));

        JPopupMenu popupMenu = panel.getResponseBodyPane().getPopupMenu();

        assertFalse(hasAnyMenuItem(popupMenu, "Undo", "Can't Redo", "Cut", "Paste", "Delete",
                "撤销", "无法恢复", "剪切", "粘贴", "删除"));
        assertTrue(hasMenuItem(popupMenu, UiI18n.get(UiMessageKeys.EDITOR_POPUP_COPY_SELECTED)));
        assertTrue(hasMenuItem(popupMenu, UiI18n.get(UiMessageKeys.EDITOR_POPUP_COPY_ALL)));
        assertTrue(hasMenuItem(popupMenu, UiI18n.get(UiMessageKeys.EDITOR_POPUP_SELECT_ALL)));
        assertTrue(hasMenuItem(popupMenu, UiI18n.get(UiMessageKeys.EDITOR_POPUP_FOLDING)));
    }

    @Test
    public void shouldKeepResponseEditorCopyAllMenuEnabledWithoutSelection() throws Exception {
        ResponseBodyPanel panel = createPanelWithResponse(responseWithBody("{\"data\":\"value\"}"));
        JPopupMenu popupMenu = panel.getResponseBodyPane().getPopupMenu();

        firePopupWillBecomeVisible(popupMenu);

        JMenuItem copyItem = findMenuItem(popupMenu, UiI18n.get(UiMessageKeys.EDITOR_POPUP_COPY_ALL));
        assertNotNull(copyItem);
        assertTrue(copyItem.isEnabled());
    }

    @Test
    public void shouldKeepResponseEditorCopySelectedMenuEnabledWithSelection() throws Exception {
        ResponseBodyPanel panel = createPanelWithResponse(responseWithBody("{\"data\":\"value\"}"));
        JPopupMenu popupMenu = panel.getResponseBodyPane().getPopupMenu();

        SwingUtilities.invokeAndWait(() -> panel.getResponseBodyPane().select(1, 7));
        firePopupWillBecomeVisible(popupMenu);

        JMenuItem copyItem = findMenuItem(popupMenu, UiI18n.get(UiMessageKeys.EDITOR_POPUP_COPY_SELECTED));
        assertNotNull(copyItem);
        assertTrue(copyItem.isEnabled());
    }

    @Test
    public void shouldShowAudioPlayerForAudioResponseWithoutLoadingCodecOnEdt() throws Exception {
        Path audioFile = Files.createTempFile("easy-postman-audio-preview-", ".wav");
        try {
            HttpResponse response = new HttpResponse();
            response.body = "[Binary response saved to a temporary file]";
            response.bodySize = 128;
            response.filePath = audioFile.toString();
            response.fileName = "sample.wav";
            response.headers = Map.of("Content-Type", List.of("audio/wav"));

            ResponseBodyPanel panel = createPanelWithResponse(response);
            MediaResponsePanel mediaPanel = findComponent(panel, MediaResponsePanel.class);

            assertNotNull(mediaPanel);
            assertTrue(mediaPanel.isVisible());
        } finally {
            Files.deleteIfExists(audioFile);
        }
    }

    @Test
    public void shouldReturnToTextCardAfterAudioResponse() throws Exception {
        Path audioFile = Files.createTempFile("easy-postman-audio-preview-", ".wav");
        try {
            HttpResponse audioResponse = new HttpResponse();
            audioResponse.bodySize = 128;
            audioResponse.filePath = audioFile.toString();
            audioResponse.fileName = "sample.wav";
            audioResponse.headers = Map.of("Content-Type", List.of("audio/wav"));
            ResponseBodyPanel panel = createPanelWithResponse(audioResponse);

            SwingUtilities.invokeAndWait(() -> panel.setBodyText(responseWithBody("{\"ok\":true}")));

            MediaResponsePanel mediaPanel = findComponent(panel, MediaResponsePanel.class);
            assertNotNull(mediaPanel);
            assertFalse(mediaPanel.isVisible());
            assertTrue(panel.getResponseBodyPane().isVisible());
        } finally {
            Files.deleteIfExists(audioFile);
        }
    }

    @Test
    public void shouldShowSystemPlayerCardForVideoResponse() throws Exception {
        Path videoFile = Files.createTempFile("easy-postman-video-preview-", ".mp4");
        try {
            HttpResponse response = new HttpResponse();
            response.body = "[Binary response saved to a temporary file]";
            response.bodySize = 256;
            response.filePath = videoFile.toString();
            response.fileName = "sample.mp4";
            response.headers = Map.of("content-type", List.of("video/mp4; charset=binary"));

            ResponseBodyPanel panel = createPanelWithResponse(response);
            MediaResponsePanel mediaPanel = findComponent(panel, MediaResponsePanel.class);

            assertNotNull(mediaPanel);
            assertTrue(mediaPanel.isVisible());
            assertTrue(hasLabel(mediaPanel, I18nUtil.getMessage(MessageKeys.RESPONSE_VIDEO_TITLE)));
            JLabel videoStatusLabel = (JLabel) mediaPanel.getComponent(3);
            assertFalse(videoStatusLabel.isVisible());
            assertEquals(videoStatusLabel.getText(), "");
        } finally {
            Files.deleteIfExists(videoFile);
        }
    }

    @Test
    public void shouldKeepMediaStatusVisibleWhenResponseAreaIsNarrow() throws Exception {
        Path audioFile = Files.createTempFile("easy-postman-audio-layout-", ".wav");
        JLabel[] summaryHolder = new JLabel[1];
        JLabel[] statusHolder = new JLabel[1];

        try {
            SwingUtilities.invokeAndWait(() -> {
                MediaResponsePanel mediaPanel = new MediaResponsePanel();
                mediaPanel.setAudioSource(audioFile.toString(), "sample.wav", 128);
                mediaPanel.setSize(360, 220);
                mediaPanel.doLayout();
                summaryHolder[0] = (JLabel) mediaPanel.getComponent(1);
                statusHolder[0] = (JLabel) mediaPanel.getComponent(3);
            });
        } finally {
            Files.deleteIfExists(audioFile);
        }

        assertNotNull(summaryHolder[0]);
        assertNotNull(statusHolder[0]);
        assertTrue(summaryHolder[0].getWidth() > 0);
        assertTrue(summaryHolder[0].getHeight() > 0);
        assertTrue(statusHolder[0].getWidth() > 0);
        assertTrue(statusHolder[0].getHeight() > 0);
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

    private <T extends Component> T findComponent(Container container, Class<T> type) {
        for (Component component : container.getComponents()) {
            if (type.isInstance(component)) {
                return type.cast(component);
            }
            if (component instanceof Container child) {
                T found = findComponent(child, type);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private boolean hasLabel(Container container, String text) {
        return findLabel(container, text) != null;
    }

    private JLabel findLabel(Container container, String text) {
        for (Component component : container.getComponents()) {
            if (component instanceof JLabel label && text.equals(label.getText())) {
                return label;
            }
            if (component instanceof Container child) {
                JLabel found = findLabel(child, text);
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

    private void firePopupWillBecomeVisible(JPopupMenu popupMenu) {
        PopupMenuEvent event = new PopupMenuEvent(popupMenu);
        for (var listener : popupMenu.getPopupMenuListeners()) {
            listener.popupMenuWillBecomeVisible(event);
        }
    }
}
