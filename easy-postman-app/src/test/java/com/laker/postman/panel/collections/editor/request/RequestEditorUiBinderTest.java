package com.laker.postman.panel.collections.editor.request;

import com.laker.postman.http.request.AppRequestHeaderDefaults;
import com.laker.postman.panel.collections.editor.request.sub.EasyRequestHttpHeadersPanel;
import com.laker.postman.panel.collections.editor.request.sub.RequestBodyPanel;
import com.laker.postman.request.model.HttpHeader;
import com.laker.postman.request.model.RequestItemProtocolEnum;
import com.laker.postman.test.AbstractSwingUiTest;
import org.testng.annotations.Test;

import javax.swing.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.testng.Assert.assertEquals;

public class RequestEditorUiBinderTest extends AbstractSwingUiTest {

    @Test
    public void binaryBodyShouldSyncContentTypeFromSelectedFile() throws Exception {
        Path file = Files.createTempFile("easy-postman-binary-content-type-", ".png");
        Files.write(file, new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47});
        RequestBodyPanel[] bodyHolder = new RequestBodyPanel[1];
        EasyRequestHttpHeadersPanel[] headersHolder = new EasyRequestHttpHeadersPanel[1];

        SwingUtilities.invokeAndWait(() -> {
            bodyHolder[0] = new RequestBodyPanel(RequestItemProtocolEnum.HTTP);
            headersHolder[0] = new EasyRequestHttpHeadersPanel(AppRequestHeaderDefaults.generatedHeaderPolicy());
            RequestEditorUiBinder.bindBodyTypeHeaderSync(bodyHolder[0], headersHolder[0], () -> false);
            bodyHolder[0].setBinaryFilePath(file.toString());
            bodyHolder[0].getBodyTypeComboBox().setSelectedItem(RequestBodyPanel.BODY_TYPE_BINARY);
        });

        assertEquals(findHeaderValue(headersHolder[0].getHeadersListFromModel(), "Content-Type"), "image/png");
    }

    @Test
    public void binaryFileChangeShouldNotOverwriteCustomContentType() throws Exception {
        Path file = Files.createTempFile("easy-postman-binary-custom-content-type-", ".png");
        Files.write(file, new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47});
        RequestBodyPanel[] bodyHolder = new RequestBodyPanel[1];
        EasyRequestHttpHeadersPanel[] headersHolder = new EasyRequestHttpHeadersPanel[1];

        SwingUtilities.invokeAndWait(() -> {
            bodyHolder[0] = new RequestBodyPanel(RequestItemProtocolEnum.HTTP);
            headersHolder[0] = new EasyRequestHttpHeadersPanel(AppRequestHeaderDefaults.generatedHeaderPolicy());
            RequestEditorUiBinder.bindBodyTypeHeaderSync(bodyHolder[0], headersHolder[0], () -> false);
            bodyHolder[0].getBodyTypeComboBox().setSelectedItem(RequestBodyPanel.BODY_TYPE_BINARY);
            headersHolder[0].setOrUpdateHeader("Content-Type", "application/vnd.custom");
            bodyHolder[0].setBinaryFilePath(file.toString());
        });

        assertEquals(findHeaderValue(headersHolder[0].getHeadersListFromModel(), "Content-Type"), "application/vnd.custom");
    }

    @Test
    public void binaryFileChangeAfterLoadShouldNotOverwriteExistingCustomContentType() throws Exception {
        Path file = Files.createTempFile("easy-postman-binary-loaded-custom-content-type-", ".png");
        Files.write(file, new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47});
        RequestBodyPanel[] bodyHolder = new RequestBodyPanel[1];
        EasyRequestHttpHeadersPanel[] headersHolder = new EasyRequestHttpHeadersPanel[1];
        boolean[] loading = new boolean[]{true};

        SwingUtilities.invokeAndWait(() -> {
            bodyHolder[0] = new RequestBodyPanel(RequestItemProtocolEnum.HTTP);
            headersHolder[0] = new EasyRequestHttpHeadersPanel(AppRequestHeaderDefaults.generatedHeaderPolicy());
            RequestEditorUiBinder.bindBodyTypeHeaderSync(bodyHolder[0], headersHolder[0], () -> loading[0]);
            bodyHolder[0].getBodyTypeComboBox().setSelectedItem(RequestBodyPanel.BODY_TYPE_BINARY);
            headersHolder[0].setOrUpdateHeader("Content-Type", "application/vnd.custom");
            loading[0] = false;
            bodyHolder[0].setBinaryFilePath(file.toString());
        });

        assertEquals(findHeaderValue(headersHolder[0].getHeadersListFromModel(), "Content-Type"), "application/vnd.custom");
    }

    @Test
    public void binaryFileChangeAfterLoadShouldUpdateLoadedAutoContentType() throws Exception {
        Path pngFile = Files.createTempFile("easy-postman-binary-loaded-auto-content-type-", ".png");
        Files.write(pngFile, new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47});
        Path textFile = Files.createTempFile("easy-postman-binary-loaded-auto-content-type-", ".txt");
        Files.writeString(textFile, "hello");
        RequestBodyPanel[] bodyHolder = new RequestBodyPanel[1];
        EasyRequestHttpHeadersPanel[] headersHolder = new EasyRequestHttpHeadersPanel[1];
        boolean[] loading = new boolean[]{true};

        SwingUtilities.invokeAndWait(() -> {
            bodyHolder[0] = new RequestBodyPanel(RequestItemProtocolEnum.HTTP);
            headersHolder[0] = new EasyRequestHttpHeadersPanel(AppRequestHeaderDefaults.generatedHeaderPolicy());
            RequestEditorUiBinder.bindBodyTypeHeaderSync(bodyHolder[0], headersHolder[0], () -> loading[0]);
            headersHolder[0].setOrUpdateHeader("Content-Type", "image/png");
            bodyHolder[0].setBinaryFilePath(pngFile.toString());
            bodyHolder[0].getBodyTypeComboBox().setSelectedItem(RequestBodyPanel.BODY_TYPE_BINARY);

            loading[0] = false;
            bodyHolder[0].setBinaryFilePath(textFile.toString());
        });

        assertEquals(findHeaderValue(headersHolder[0].getHeadersListFromModel(), "Content-Type"), "text/plain");
    }

    private static String findHeaderValue(List<HttpHeader> headers, String key) {
        for (HttpHeader header : headers) {
            if (header.isEnabled() && key.equalsIgnoreCase(header.getKey())) {
                return header.getValue();
            }
        }
        return null;
    }
}
