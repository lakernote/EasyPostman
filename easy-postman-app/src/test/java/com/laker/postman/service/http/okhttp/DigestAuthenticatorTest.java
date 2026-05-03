package com.laker.postman.service.http.okhttp;

import okhttp3.Request;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.Map;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class DigestAuthenticatorTest {

    @Test
    public void shouldCreateRfc7616DigestAuthorizationHeader() {
        DigestAuthenticator.DigestChallenge challenge = DigestAuthenticator.DigestChallenge.parse(
                "Digest realm=\"testrealm@host.com\", " +
                        "qop=\"auth,auth-int\", " +
                        "nonce=\"dcd98b7102dd2f0e8b11d0f600bfb0c093\", " +
                        "opaque=\"5ccc069c403ebaf9f0171e9517f40e41\""
        );
        Request request = new Request.Builder()
                .url("http://www.example.com/dir/index.html")
                .get()
                .build();

        String authorization = DigestAuthenticator.createAuthorizationHeader(
                challenge,
                "Mufasa",
                "Circle Of Life",
                request,
                "0a4f113b",
                1
        );

        Map<String, String> attributes = parseDigestAttributes(authorization);
        assertEquals(attributes.get("username"), "Mufasa");
        assertEquals(attributes.get("realm"), "testrealm@host.com");
        assertEquals(attributes.get("nonce"), "dcd98b7102dd2f0e8b11d0f600bfb0c093");
        assertEquals(attributes.get("uri"), "/dir/index.html");
        assertEquals(attributes.get("qop"), "auth");
        assertEquals(attributes.get("nc"), "00000001");
        assertEquals(attributes.get("cnonce"), "0a4f113b");
        assertEquals(attributes.get("opaque"), "5ccc069c403ebaf9f0171e9517f40e41");
        assertEquals(attributes.get("response"), "6629fae49393a05397450978507c4ef1");
    }

    @Test
    public void shouldParseDigestChallengeWithCommasInsideQuotedValues() {
        DigestAuthenticator.DigestChallenge challenge = DigestAuthenticator.DigestChallenge.parse(
                "Digest realm=\"api,internal\", nonce=\"nonce-value\", qop=\"auth\""
        );

        assertEquals(challenge.realm(), "api,internal");
        assertEquals(challenge.nonce(), "nonce-value");
        assertTrue(challenge.supportsQopAuth());
    }

    private static Map<String, String> parseDigestAttributes(String authorization) {
        assertTrue(authorization.startsWith("Digest "));
        Map<String, String> attributes = new HashMap<>();
        for (String token : authorization.substring("Digest ".length()).split(", ")) {
            String[] pair = token.split("=", 2);
            String value = pair[1];
            if (value.startsWith("\"") && value.endsWith("\"")) {
                value = value.substring(1, value.length() - 1);
            }
            attributes.put(pair[0], value);
        }
        return attributes;
    }
}
