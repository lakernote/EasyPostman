package com.laker.postman.panel.collections.right.request;

import com.laker.postman.model.HttpRequestItem;
import com.laker.postman.model.RequestItemProtocolEnum;
import com.laker.postman.test.AbstractSwingUiTest;
import org.testng.annotations.Test;

import javax.swing.*;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

public class RequestEditSubPanelLazyInitUiTest extends AbstractSwingUiTest {

    @Test
    public void shouldDelayEditorInitializationUntilActivated() throws Exception {
        HttpRequestItem item = new HttpRequestItem();
        item.setId("lazy-id");
        item.setName("Lazy Request");
        item.setProtocol(RequestItemProtocolEnum.HTTP);

        RequestEditSubPanel panel = createDeferredPanel(item);

        assertFalse(panel.isEditorInitialized());
        assertEquals(panel.getCurrentRequest().getId(), "lazy-id");
        assertEquals(panel.getCurrentRequest().getName(), "Lazy Request");

        SwingUtilities.invokeAndWait(panel::ensureEditorInitialized);

        assertTrue(panel.isEditorInitialized());
        assertNotNull(panel.getRequestLinePanel());
        assertEquals(panel.getCurrentRequest().getId(), "lazy-id");
    }

    private RequestEditSubPanel createDeferredPanel(HttpRequestItem item) throws Exception {
        final RequestEditSubPanel[] holder = new RequestEditSubPanel[1];
        SwingUtilities.invokeAndWait(() -> {
            RequestEditSubPanel panel = new RequestEditSubPanel(item.getId(), item.getProtocol(), true);
            panel.initPanelData(item);
            holder[0] = panel;
        });
        return holder[0];
    }
}
