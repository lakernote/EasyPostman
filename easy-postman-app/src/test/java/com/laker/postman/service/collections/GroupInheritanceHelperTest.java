package com.laker.postman.service.collections;

import com.laker.postman.model.HttpRequestItem;
import com.laker.postman.model.RequestGroup;
import org.testng.annotations.Test;

import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class GroupInheritanceHelperTest {

    @Test
    public void shouldPreserveRequestLevelTransportSettingsWhenApplyingInheritanceChain() {
        HttpRequestItem item = new HttpRequestItem();
        item.setId("request-1");
        item.setName("Request 1");
        item.setMethod("GET");
        item.setUrl("https://api.example.com/data");
        item.setFollowRedirects(Boolean.FALSE);
        item.setCookieJarEnabled(Boolean.FALSE);
        item.setHttpVersion(HttpRequestItem.HTTP_VERSION_HTTP_1_1);
        item.setRequestTimeoutMs(4321);

        RequestGroup parent = new RequestGroup("Parent");

        HttpRequestItem merged = GroupInheritanceHelper.mergeGroupSettingsWithChain(item, List.of(parent));

        assertFalse(merged.getFollowRedirects());
        assertFalse(merged.getCookieJarEnabled());
        assertEquals(merged.getHttpVersion(), HttpRequestItem.HTTP_VERSION_HTTP_1_1);
        assertEquals(merged.getRequestTimeoutMs(), Integer.valueOf(4321));
    }
}
