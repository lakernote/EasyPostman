package com.laker.postman.panel.collections.editor.request;

import com.laker.postman.request.model.HttpRequestItem;
import com.laker.postman.request.model.RequestBodyTypes;
import com.laker.postman.request.model.RequestItemProtocolEnum;
import com.laker.postman.test.AbstractSwingUiTest;
import org.testng.annotations.Test;

import javax.swing.*;

import static org.testng.Assert.assertEquals;

public class RequestEditorBinderTest extends AbstractSwingUiTest {

    @Test
    public void populateRawBodyShouldNotLeakIntoBinaryFilePath() throws Exception {
        RequestViewComponents[] viewHolder = new RequestViewComponents[1];
        String[] rawBody = new String[1];
        String[] binaryFilePath = new String[1];

        SwingUtilities.invokeAndWait(() -> {
            viewHolder[0] = RequestViewFactory.create(RequestItemProtocolEnum.HTTP, RequestEditSubPanelType.NORMAL, e -> {
            });
            HttpRequestItem item = new HttpRequestItem();
            item.setMethod("POST");
            item.setUrl("https://example.com");
            item.setBodyType(RequestBodyTypes.BODY_TYPE_RAW);
            item.setBody("{\"hello\":\"world\"}");

            new RequestEditorBinder(viewHolder[0]).populate(item);
            rawBody[0] = viewHolder[0].requestBodyPanel.getRawBody();
            binaryFilePath[0] = viewHolder[0].requestBodyPanel.getBinaryFilePath();
        });

        assertEquals(rawBody[0], "{\"hello\":\"world\"}");
        assertEquals(binaryFilePath[0], "");
    }

    @Test
    public void populateBinaryBodyShouldNotLeakFilePathIntoRawBody() throws Exception {
        RequestViewComponents[] viewHolder = new RequestViewComponents[1];
        String[] rawBody = new String[1];
        String[] binaryFilePath = new String[1];

        SwingUtilities.invokeAndWait(() -> {
            viewHolder[0] = RequestViewFactory.create(RequestItemProtocolEnum.HTTP, RequestEditSubPanelType.NORMAL, e -> {
            });
            HttpRequestItem item = new HttpRequestItem();
            item.setMethod("POST");
            item.setUrl("https://example.com");
            item.setBodyType(RequestBodyTypes.BODY_TYPE_BINARY);
            item.setBody("/tmp/upload.bin");

            new RequestEditorBinder(viewHolder[0]).populate(item);
            rawBody[0] = viewHolder[0].requestBodyPanel.getRawBody();
            binaryFilePath[0] = viewHolder[0].requestBodyPanel.getBinaryFilePath();
        });

        assertEquals(rawBody[0], "");
        assertEquals(binaryFilePath[0], "/tmp/upload.bin");
    }

    @Test
    public void collectCurrentDraftShouldIgnoreHiddenRawTextForNoneBodyType() throws Exception {
        RequestViewComponents[] viewHolder = new RequestViewComponents[1];
        String[] collectedBody = new String[1];

        SwingUtilities.invokeAndWait(() -> {
            viewHolder[0] = RequestViewFactory.create(RequestItemProtocolEnum.HTTP, RequestEditSubPanelType.NORMAL, e -> {
            });
            viewHolder[0].requestBodyPanel.setRawBodyText("{\"stale\":true}");
            viewHolder[0].requestBodyPanel.getBodyTypeComboBox().setSelectedItem(RequestBodyTypes.BODY_TYPE_NONE);

            collectedBody[0] = new RequestEditorBinder(viewHolder[0])
                    .collectCurrentDraft("id", "name", RequestItemProtocolEnum.HTTP, null, true)
                    .getBody();
        });

        assertEquals(collectedBody[0], "");
    }
}
