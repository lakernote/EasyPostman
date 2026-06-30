package com.laker.postman.panel.collections.editor.request;

import com.laker.postman.request.model.HttpRequestItem;
import com.laker.postman.request.model.RequestItemProtocolEnum;
import com.laker.postman.test.AbstractSwingUiTest;
import org.testng.annotations.Test;

import javax.swing.SwingUtilities;

import static org.testng.Assert.assertSame;

public class RequestEditorDefaultTabSelectorTest extends AbstractSwingUiTest {

    @Test
    public void shouldSelectBodyTabForSseRequestEvenWithoutBodyContent() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            RequestViewComponents view = RequestViewFactory.create(RequestItemProtocolEnum.SSE, RequestEditSubPanelType.NORMAL, e -> {
            });
            HttpRequestItem item = new HttpRequestItem();
            item.setProtocol(RequestItemProtocolEnum.SSE);
            item.setMethod("POST");

            new RequestEditorBinder(view).populate(item);
            new RequestEditorDefaultTabSelector(view).selectByRequestType(RequestItemProtocolEnum.SSE, item);

            assertSame(view.reqTabs.getSelectedComponent(), view.requestBodyPanel);
        });
    }

    @Test
    public void shouldSelectBodyTabForSseInitialProtocolUi() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            RequestViewComponents view = RequestViewFactory.create(RequestItemProtocolEnum.SSE, RequestEditSubPanelType.NORMAL, e -> {
            });

            RequestEditorUiBinder.applyInitialProtocolUi(
                    RequestItemProtocolEnum.SSE,
                    view.reqTabs,
                    view.requestBodyPanel,
                    view.paramsTabPanel,
                    view.authTabPanel,
                    e -> {
                    }
            );

            assertSame(view.reqTabs.getSelectedComponent(), view.requestBodyPanel);
        });
    }
}
