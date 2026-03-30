package com.laker.postman.service.http;

import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class NetworkErrorMessageResolverTest {

    @Test
    public void shouldTranslateMalformedSocksReplyIntoActionableHint() {
        String message = NetworkErrorMessageResolver.toUserFriendlyMessage("Malformed reply from SOCKS server");

        assertTrue(message.contains("SOCKS"));
        assertTrue(message.contains("HTTP/SOCKS"));
        assertTrue(message.contains("Malformed reply from SOCKS server"));
    }

    @Test
    public void shouldKeepUnknownMessagesUnchanged() {
        String rawMessage = "Connection reset by peer";

        assertEquals(NetworkErrorMessageResolver.toUserFriendlyMessage(rawMessage), rawMessage);
    }
}
