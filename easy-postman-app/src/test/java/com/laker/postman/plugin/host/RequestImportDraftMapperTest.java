package com.laker.postman.plugin.host;

import com.laker.postman.model.HttpRequestItem;
import com.laker.postman.model.RequestBodyTypes;
import com.laker.postman.model.RequestImportBodyTypes;
import com.laker.postman.model.RequestImportDraft;
import com.laker.postman.model.RequestImportHeader;
import com.laker.postman.model.RequestImportProtocol;
import com.laker.postman.model.RequestItemProtocolEnum;
import org.testng.annotations.Test;

import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;

public class RequestImportDraftMapperTest {

    @Test
    public void shouldMapImportDraftToHttpRequestItem() {
        RequestImportDraft draft = new RequestImportDraft(
                "request-1",
                "Captured API",
                "https://example.com/api",
                "POST",
                RequestImportProtocol.WEBSOCKET,
                List.of(new RequestImportHeader(true, "Accept", "application/json")),
                "Imported from test",
                RequestImportBodyTypes.RAW,
                "{\"ok\":true}"
        );

        HttpRequestItem item = RequestImportDraftMapper.toHttpRequestItem(draft);

        assertEquals(item.getId(), "request-1");
        assertEquals(item.getName(), "Captured API");
        assertEquals(item.getUrl(), "https://example.com/api");
        assertEquals(item.getMethod(), "POST");
        assertEquals(item.getProtocol(), RequestItemProtocolEnum.WEBSOCKET);
        assertEquals(item.getDescription(), "Imported from test");
        assertEquals(item.getBodyType(), RequestBodyTypes.BODY_TYPE_RAW);
        assertEquals(item.getBody(), "{\"ok\":true}");
        assertEquals(item.getHeadersList().size(), 1);
        assertEquals(item.getHeadersList().get(0).getKey(), "Accept");
        assertEquals(item.getHeadersList().get(0).getValue(), "application/json");
    }

    @Test
    public void shouldGenerateIdForBlankImportDraftId() {
        RequestImportDraft draft = new RequestImportDraft(
                "",
                "Generated",
                "https://example.com",
                "GET",
                RequestImportProtocol.HTTP,
                List.of(),
                "",
                RequestImportBodyTypes.NONE,
                ""
        );

        HttpRequestItem item = RequestImportDraftMapper.toHttpRequestItem(draft);

        assertFalse(item.getId().isBlank());
        assertEquals(item.getProtocol(), RequestItemProtocolEnum.HTTP);
        assertEquals(item.getBodyType(), RequestBodyTypes.BODY_TYPE_NONE);
    }
}
