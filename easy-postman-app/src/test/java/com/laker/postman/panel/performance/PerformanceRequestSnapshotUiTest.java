package com.laker.postman.panel.performance;

import com.laker.postman.model.HttpRequestItem;
import com.laker.postman.panel.collections.editor.request.sub.RequestBodyPanel;
import com.laker.postman.panel.collections.editor.request.RequestEditSubPanel;
import com.laker.postman.panel.collections.editor.request.sub.RequestLinePanel;
import com.laker.postman.panel.collections.editor.request.sub.ResponsePanel;
import com.laker.postman.test.AbstractSwingUiTest;
import org.testng.annotations.Test;

import java.awt.*;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;

public class PerformanceRequestSnapshotUiTest extends AbstractSwingUiTest {

    @Test
    public void requestPropertyShouldUseReadOnlySnapshotWithoutResponsePanel() {
        PerformancePanelViewFactory viewFactory = new PerformancePanelViewFactory();

        PerformancePanelViewFactory.PropertySection propertySection = viewFactory.createPropertySection(
                "empty",
                "threadGroup",
                "loop",
                "request",
                "assertion",
                "timer",
                "sseConnect",
                "sseAwait",
                "wsConnect",
                "wsSend",
                "wsAwait",
                "wsClose"
        );

        RequestEditSubPanel requestPanel = propertySection.requestEditSubPanel();
        requestPanel.ensureEditorInitialized();
        RequestLinePanel requestLinePanel = requestPanel.getRequestLinePanel();

        Container requestWrapper = propertySection.requestEditorHost().getParent();
        assertNotNull(requestWrapper);
        assertNull(((BorderLayout) requestWrapper.getLayout()).getLayoutComponent(BorderLayout.NORTH));
        assertFalse(containsComponentOfType(requestPanel, ResponsePanel.class));
        assertFalse(requestLinePanel.getSendButton().isVisible());
        assertFalse(requestLinePanel.getSaveButton().isVisible());
        assertFalse(requestLinePanel.getUrlField().isEditable());
        assertFalse(requestLinePanel.getMethodBox().isEnabled());

        HttpRequestItem requestItem = new HttpRequestItem();
        requestItem.setId("perf-snapshot-request");
        requestItem.setName("Snapshot request");
        requestItem.setMethod("POST");
        requestItem.setUrl("http://localhost:18080/http");
        requestItem.setBodyType(RequestBodyPanel.BODY_TYPE_RAW);
        requestItem.setBody("{\"ok\":true}");

        requestPanel.initPanelData(requestItem);
        RequestBodyPanel bodyPanel = findComponentOfType(requestPanel, RequestBodyPanel.class);
        assertNotNull(bodyPanel);
        assertFalse(bodyPanel.getBodyArea().isEditable());
        assertFalse(bodyPanel.getBodyTypeComboBox().isEnabled());
    }

    private static boolean containsComponentOfType(Component component, Class<?> type) {
        if (type.isInstance(component)) {
            return true;
        }
        if (component instanceof Container container) {
            for (Component child : container.getComponents()) {
                if (containsComponentOfType(child, type)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static <T> T findComponentOfType(Component component, Class<T> type) {
        if (type.isInstance(component)) {
            return type.cast(component);
        }
        if (component instanceof Container container) {
            for (Component child : container.getComponents()) {
                T found = findComponentOfType(child, type);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }
}
