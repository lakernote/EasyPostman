package com.laker.postman.service.http.okhttp;

import okhttp3.Authenticator;
import okhttp3.HttpUrl;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.Route;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public final class DigestAuthenticator implements Authenticator {
    private static final String AUTHORIZATION = "Authorization";
    private static final String WWW_AUTHENTICATE = "WWW-Authenticate";
    private static final String DIGEST_SCHEME = "Digest";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final String username;
    private final String password;
    private final CnonceGenerator cnonceGenerator;
    private final AtomicInteger nonceCount = new AtomicInteger();

    public DigestAuthenticator(String username, String password) {
        this(username, password, DigestAuthenticator::randomCnonce);
    }

    DigestAuthenticator(String username, String password, CnonceGenerator cnonceGenerator) {
        this.username = username;
        this.password = password;
        this.cnonceGenerator = cnonceGenerator;
    }

    @Override
    public Request authenticate(Route route, Response response) {
        if (response == null) {
            return null;
        }

        DigestChallenge challenge = findDigestChallenge(response);
        if (challenge == null || isBlank(username)) {
            return null;
        }

        String existingAuthorization = response.request().header(AUTHORIZATION);
        if (existingAuthorization != null && !shouldRetryStaleNonce(challenge, existingAuthorization, response)) {
            return null;
        }

        try {
            String authorization = createAuthorizationHeader(
                    challenge,
                    username,
                    password == null ? "" : password,
                    response.request(),
                    cnonceGenerator.create(),
                    nonceCount.incrementAndGet()
            );
            if (authorization == null) {
                return null;
            }
            return response.request().newBuilder()
                    .header(AUTHORIZATION, authorization)
                    .build();
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private static boolean shouldRetryStaleNonce(DigestChallenge challenge, String existingAuthorization, Response response) {
        if (!challenge.stale() || responseCount(response) > 2) {
            return false;
        }
        String previousNonce = DigestChallenge.attributeValue(existingAuthorization, "nonce");
        return isBlank(previousNonce) || !challenge.nonce().equals(previousNonce);
    }

    private static int responseCount(Response response) {
        int count = 1;
        while ((response = response.priorResponse()) != null) {
            count++;
        }
        return count;
    }

    static String createAuthorizationHeader(DigestChallenge challenge,
                                            String username,
                                            String password,
                                            Request request,
                                            String cnonce,
                                            int nonceCount) {
        if (challenge == null || request == null || isBlank(username)
                || isBlank(challenge.realm()) || isBlank(challenge.nonce())) {
            return null;
        }

        String qop = challenge.selectedQop();
        if (challenge.hasQop() && qop == null) {
            return null;
        }

        String nc = String.format("%08x", nonceCount);
        String uri = digestUri(request.url());
        String algorithm = challenge.algorithm();
        String ha1 = hash(algorithm, username + ":" + challenge.realm() + ":" + (password == null ? "" : password));
        if (isSessionAlgorithm(algorithm)) {
            ha1 = hash(algorithm, ha1 + ":" + challenge.nonce() + ":" + cnonce);
        }

        String ha2 = hash(algorithm, request.method() + ":" + uri);
        String responseDigest = qop == null
                ? hash(algorithm, ha1 + ":" + challenge.nonce() + ":" + ha2)
                : hash(algorithm, ha1 + ":" + challenge.nonce() + ":" + nc + ":" + cnonce + ":" + qop + ":" + ha2);

        List<String> parts = new ArrayList<>();
        parts.add("username=" + quote(username));
        parts.add("realm=" + quote(challenge.realm()));
        parts.add("nonce=" + quote(challenge.nonce()));
        parts.add("uri=" + quote(uri));
        parts.add("response=" + quote(responseDigest));
        if (challenge.algorithmProvided()) {
            parts.add("algorithm=" + challenge.algorithm());
        }
        if (!isBlank(challenge.opaque())) {
            parts.add("opaque=" + quote(challenge.opaque()));
        }
        if (qop != null) {
            parts.add("qop=" + qop);
            parts.add("nc=" + nc);
            parts.add("cnonce=" + quote(cnonce));
        }
        return DIGEST_SCHEME + " " + String.join(", ", parts);
    }

    private static DigestChallenge findDigestChallenge(Response response) {
        for (String header : response.headers(WWW_AUTHENTICATE)) {
            DigestChallenge challenge = DigestChallenge.parse(header);
            if (challenge != null) {
                return challenge;
            }
        }
        return null;
    }

    private static String digestUri(HttpUrl url) {
        String uri = url.encodedPath();
        String query = url.encodedQuery();
        return query == null ? uri : uri + "?" + query;
    }

    private static String hash(String algorithm, String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance(messageDigestAlgorithm(algorithm));
            return toHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalArgumentException("Unsupported digest algorithm: " + algorithm, ex);
        }
    }

    private static String messageDigestAlgorithm(String algorithm) {
        String base = baseAlgorithm(algorithm).toUpperCase(Locale.ROOT);
        return switch (base) {
            case "MD5" -> "MD5";
            case "SHA-256" -> "SHA-256";
            case "SHA-512-256" -> "SHA-512/256";
            default -> throw new IllegalArgumentException("Unsupported digest algorithm: " + algorithm);
        };
    }

    private static String baseAlgorithm(String algorithm) {
        String normalized = isBlank(algorithm) ? "MD5" : algorithm.trim();
        return isSessionAlgorithm(normalized)
                ? normalized.substring(0, normalized.length() - "-sess".length())
                : normalized;
    }

    private static boolean isSessionAlgorithm(String algorithm) {
        return algorithm != null && algorithm.toLowerCase(Locale.ROOT).endsWith("-sess");
    }

    private static String quote(String value) {
        return "\"" + (value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"")) + "\"";
    }

    private static String randomCnonce() {
        byte[] bytes = new byte[16];
        SECURE_RANDOM.nextBytes(bytes);
        return toHex(bytes);
    }

    private static String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b & 0xff));
        }
        return sb.toString();
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    @FunctionalInterface
    interface CnonceGenerator {
        String create();
    }

    record DigestChallenge(String realm,
                           String nonce,
                           String qop,
                           String algorithm,
                           String opaque,
                           boolean stale,
                           boolean algorithmProvided) {
        static DigestChallenge parse(String header) {
            if (header == null) {
                return null;
            }

            String trimmed = header.trim();
            int digestIndex = indexOfDigestScheme(trimmed);
            if (digestIndex < 0) {
                return null;
            }

            String params = trimmed.substring(digestIndex + DIGEST_SCHEME.length()).trim();
            if (params.startsWith(",")) {
                params = params.substring(1).trim();
            }
            Map<String, String> attributes = parseAuthParams(params);
            String realm = attributes.get("realm");
            String nonce = attributes.get("nonce");
            if (isBlank(realm) || isBlank(nonce)) {
                return null;
            }

            String rawAlgorithm = attributes.get("algorithm");
            boolean algorithmProvided = !isBlank(rawAlgorithm);
            String algorithm = algorithmProvided ? rawAlgorithm.trim() : "MD5";
            return new DigestChallenge(
                    realm,
                    nonce,
                    attributes.get("qop"),
                    algorithm,
                    attributes.get("opaque"),
                    "true".equalsIgnoreCase(attributes.get("stale")),
                    algorithmProvided
            );
        }

        boolean hasQop() {
            return !isBlank(qop);
        }

        boolean supportsQopAuth() {
            return selectedQop() != null;
        }

        String selectedQop() {
            if (isBlank(qop)) {
                return null;
            }
            for (String candidate : qop.split(",")) {
                if ("auth".equalsIgnoreCase(candidate.trim())) {
                    return "auth";
                }
            }
            return null;
        }

        static String attributeValue(String header, String name) {
            if (header == null || name == null) {
                return null;
            }
            String trimmed = header.trim();
            int digestIndex = indexOfDigestScheme(trimmed);
            if (digestIndex < 0) {
                return null;
            }
            String params = trimmed.substring(digestIndex + DIGEST_SCHEME.length()).trim();
            if (params.startsWith(",")) {
                params = params.substring(1).trim();
            }
            return parseAuthParams(params).get(name.toLowerCase(Locale.ROOT));
        }

        private static int indexOfDigestScheme(String header) {
            if (header.regionMatches(true, 0, DIGEST_SCHEME, 0, DIGEST_SCHEME.length())) {
                return 0;
            }
            String lower = header.toLowerCase(Locale.ROOT);
            int index = lower.indexOf(" " + DIGEST_SCHEME.toLowerCase(Locale.ROOT) + " ");
            if (index >= 0) {
                return index + 1;
            }
            index = lower.indexOf("," + DIGEST_SCHEME.toLowerCase(Locale.ROOT) + " ");
            return index >= 0 ? index + 1 : -1;
        }

        private static Map<String, String> parseAuthParams(String params) {
            Map<String, String> attributes = new HashMap<>();
            for (String token : splitAuthParams(params)) {
                int eq = token.indexOf('=');
                if (eq <= 0) {
                    continue;
                }
                String key = token.substring(0, eq).trim().toLowerCase(Locale.ROOT);
                String value = unquote(token.substring(eq + 1).trim());
                attributes.put(key, value);
            }
            return attributes;
        }

        private static List<String> splitAuthParams(String params) {
            List<String> tokens = new ArrayList<>();
            StringBuilder current = new StringBuilder();
            boolean inQuotes = false;
            boolean escaping = false;
            for (int i = 0; i < params.length(); i++) {
                char c = params.charAt(i);
                if (escaping) {
                    current.append(c);
                    escaping = false;
                    continue;
                }
                if (c == '\\' && inQuotes) {
                    current.append(c);
                    escaping = true;
                    continue;
                }
                if (c == '"') {
                    inQuotes = !inQuotes;
                    current.append(c);
                    continue;
                }
                if (c == ',' && !inQuotes) {
                    addToken(tokens, current);
                    continue;
                }
                current.append(c);
            }
            addToken(tokens, current);
            return tokens;
        }

        private static void addToken(List<String> tokens, StringBuilder current) {
            String token = current.toString().trim();
            if (!token.isEmpty()) {
                tokens.add(token);
            }
            current.setLength(0);
        }

        private static String unquote(String value) {
            if (value.length() < 2 || value.charAt(0) != '"' || value.charAt(value.length() - 1) != '"') {
                return value;
            }
            String quoted = value.substring(1, value.length() - 1);
            StringBuilder sb = new StringBuilder(quoted.length());
            boolean escaping = false;
            for (int i = 0; i < quoted.length(); i++) {
                char c = quoted.charAt(i);
                if (escaping) {
                    sb.append(c);
                    escaping = false;
                } else if (c == '\\') {
                    escaping = true;
                } else {
                    sb.append(c);
                }
            }
            if (escaping) {
                sb.append('\\');
            }
            return sb.toString();
        }
    }
}
