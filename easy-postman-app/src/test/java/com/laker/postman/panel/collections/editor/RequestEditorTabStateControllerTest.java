package com.laker.postman.panel.collections.editor;

import com.laker.postman.panel.collections.editor.request.RequestEditSubPanel;
import com.laker.postman.request.model.RequestItemProtocolEnum;
import org.testng.annotations.Test;

import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertSame;

public class RequestEditorTabStateControllerTest {

    @Test
    public void shouldPinTransientRequestWhenItBecomesDirty() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            JTabbedPane tabbedPane = new JTabbedPane();
            RequestEditSubPanel panel = new RequestEditSubPanel("request-1", RequestItemProtocolEnum.HTTP, true);
            tabbedPane.addTab("Request", panel);
            AtomicReference<Component> pinned = new AtomicReference<>();
            RequestEditorTabStateController controller = new RequestEditorTabStateController(tabbedPane, pinned::set);

            controller.updateRequestDirty(panel, true);

            assertSame(pinned.get(), panel);
        });
    }

    @Test
    public void shouldNotPinTransientRequestWhenItBecomesClean() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            JTabbedPane tabbedPane = new JTabbedPane();
            RequestEditSubPanel panel = new RequestEditSubPanel("request-1", RequestItemProtocolEnum.HTTP, true);
            tabbedPane.addTab("Request", panel);
            AtomicBoolean pinned = new AtomicBoolean();
            RequestEditorTabStateController controller = new RequestEditorTabStateController(
                    tabbedPane,
                    component -> pinned.set(true)
            );

            controller.updateRequestDirty(panel, false);

            assertFalse(pinned.get());
        });
    }
}
