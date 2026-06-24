package com.laker.postman.service.curl;

import lombok.experimental.UtilityClass;

import java.util.HashMap;
import java.util.Map;

@UtilityClass
class CurlOptionRegistry {

    private static final Map<String, CurlOptionSpec> LONG_OPTIONS = new HashMap<>();
    private static final Map<Character, CurlOptionSpec> SHORT_OPTIONS = new HashMap<>();

    static {
        register("url", null, CurlOptionValueMode.REQUIRED, CurlOptionAction.URL);
        register("request", 'X', CurlOptionValueMode.REQUIRED, CurlOptionAction.REQUEST);
        register("header", 'H', CurlOptionValueMode.REQUIRED, CurlOptionAction.HEADER);
        register("cookie", 'b', CurlOptionValueMode.REQUIRED, CurlOptionAction.COOKIE);
        register("data", 'd', CurlOptionValueMode.REQUIRED, CurlOptionAction.DATA);
        register("data-ascii", null, CurlOptionValueMode.REQUIRED, CurlOptionAction.DATA);
        register("data-raw", null, CurlOptionValueMode.REQUIRED, CurlOptionAction.DATA);
        register("data-binary", null, CurlOptionValueMode.REQUIRED, CurlOptionAction.DATA_BINARY);
        register("data-urlencode", null, CurlOptionValueMode.REQUIRED, CurlOptionAction.DATA_URLENCODE);
        register("json", null, CurlOptionValueMode.REQUIRED, CurlOptionAction.DATA);
        register("form", 'F', CurlOptionValueMode.REQUIRED, CurlOptionAction.FORM);
        register("form-string", null, CurlOptionValueMode.REQUIRED, CurlOptionAction.FORM);
        register("get", 'G', CurlOptionValueMode.NONE, CurlOptionAction.GET);
        register("location", 'L', CurlOptionValueMode.NONE, CurlOptionAction.LOCATION);
        register("location-trusted", null, CurlOptionValueMode.NONE, CurlOptionAction.LOCATION);
        register("user", 'u', CurlOptionValueMode.REQUIRED, CurlOptionAction.USER);
        register("digest", null, CurlOptionValueMode.NONE, CurlOptionAction.DIGEST);
        register("user-agent", 'A', CurlOptionValueMode.REQUIRED, CurlOptionAction.USER_AGENT);
        register("referer", 'e', CurlOptionValueMode.REQUIRED, CurlOptionAction.REFERER);
        register("head", 'I', CurlOptionValueMode.NONE, CurlOptionAction.HEAD);
        register("oauth2-bearer", null, CurlOptionValueMode.REQUIRED, CurlOptionAction.OAUTH2_BEARER);

        registerIgnoredFlags(
                "anyauth", "basic", "cert-status", "compressed", "compressed-ssh", "create-dirs",
                "crlf", "disable", "disable-epsv", "disable-eprt", "disallow-username-in-url",
                "doh-cert-status", "doh-insecure", "fail", "fail-early", "fail-with-body",
                "false-start", "ftp-create-dirs", "ftp-pasv", "ftp-pret", "ftp-skip-pasv-ip",
                "ftp-ssl", "ftp-ssl-ccc", "ftp-ssl-control", "globoff", "haproxy-protocol",
                "http0.9", "http1.0", "http1.1", "http2", "http2-prior-knowledge", "http3",
                "ignore-content-length", "include", "insecure", "ipv4", "ipv6", "list-only",
                "mail-rcpt-allowfails", "manual", "negotiate", "netrc", "netrc-optional",
                "no-alpn", "no-buffer", "no-clobber", "no-keepalive", "no-npn", "no-progress-meter",
                "parallel", "parallel-immediate", "path-as-is", "post301", "post302", "post303",
                "progress-bar", "proxy-anyauth", "proxy-basic", "proxy-digest",
                "proxy-insecure", "proxy-negotiate", "proxy-ntlm", "proxy-ssl-allow-beast",
                "raw", "remote-header-name", "remote-name", "remote-name-all", "remote-time",
                "retry-all-errors", "retry-connrefused", "sasl-ir", "show-error", "silent",
                "socks5-basic", "socks5-gssapi", "socks5-gssapi-nec", "ssl", "ssl-allow-beast",
                "ssl-no-revoke", "ssl-reqd", "ssl-revoke-best-effort", "styled-output",
                "tcp-fastopen", "tcp-nodelay", "tlsv1", "tlsv1.0", "tlsv1.1", "tlsv1.2",
                "tlsv1.3", "tr-encoding", "verbose", "version", "xattr"
        );
        registerIgnoredShortFlags('0', '1', '2', '3', '4', '6', 'f', 'g', 'i', 'j', 'k', 'l', 'n', 'N', 'O',
                'p', 'q', 'R', 's', 'S', 'v', 'V');

        registerIgnoredValues(
                "abstract-unix-socket", "alt-svc", "aws-sigv4", "cacert", "capath", "cert",
                "cert-type", "ciphers", "config", "connect-timeout", "connect-to", "cookie-jar",
                "curves", "delegation", "dns-interface", "dns-ipv4-addr", "dns-ipv6-addr",
                "dns-servers", "doh-url", "dump-header", "egd-file", "engine", "etag-compare",
                "etag-save", "expect100-timeout", "ftp-account", "ftp-alternative-to-user",
                "ftp-method", "ftp-ssl-ccc-mode", "haproxy-clientip", "hostpubmd5",
                "hostpubsha256", "interface", "key", "key-type", "krb", "limit-rate",
                "local-port", "login-options", "mail-auth", "mail-from", "mail-rcpt",
                "max-filesize", "max-redirs", "max-time", "netrc-file", "output", "parallel-max",
                "pass", "pinnedpubkey", "preproxy", "proto", "proto-default", "proto-redir", "proxy",
                "proxy-cacert", "proxy-capath", "proxy-cert", "proxy-cert-type", "proxy-ciphers",
                "proxy-header", "proxy-key", "proxy-key-type", "proxy-pass", "proxy-service-name",
                "proxy-tls13-ciphers", "proxy-tlsauthtype", "proxy-tlspassword", "proxy-tlsuser",
                "proxy-user", "proxy1.0", "pubkey", "quote", "random-file", "range", "rate",
                "request-target", "resolve", "retry", "retry-delay", "retry-max-time",
                "sasl-authzid", "service-name", "socks4", "socks4a", "socks5", "socks5-gssapi-service",
                "socks5-hostname", "speed-limit", "speed-time", "stderr", "telnet-option",
                "tftp-blksize", "time-cond", "tls-max", "tls13-ciphers", "tlsauthtype",
                "tlspassword", "tlsuser", "trace", "trace-ascii", "unix-socket", "upload-file", "url-query",
                "variable", "write-out"
        );
        registerIgnoredShortValues('c', 'C', 'D', 'E', 'K', 'm', 'o', 'P', 'Q', 'r', 'T', 'U', 'w', 'x', 'Y', 'y', 'z');
    }

    static CurlOptionSpec findLong(String name) {
        if (name == null) {
            return null;
        }
        return LONG_OPTIONS.get(normalizeLongName(name));
    }

    static CurlOptionSpec findShort(char name) {
        return SHORT_OPTIONS.get(name);
    }

    private static void register(String longName, Character shortName, CurlOptionValueMode valueMode, CurlOptionAction action) {
        CurlOptionSpec spec = new CurlOptionSpec(longName, shortName, valueMode, action);
        if (longName != null) {
            LONG_OPTIONS.put(normalizeLongName(longName), spec);
        }
        if (shortName != null) {
            SHORT_OPTIONS.put(shortName, spec);
        }
    }

    private static void registerIgnoredFlags(String... longNames) {
        for (String longName : longNames) {
            register(longName, null, CurlOptionValueMode.NONE, CurlOptionAction.IGNORE);
        }
    }

    private static void registerIgnoredShortFlags(char... shortNames) {
        for (char shortName : shortNames) {
            register(null, shortName, CurlOptionValueMode.NONE, CurlOptionAction.IGNORE);
        }
    }

    private static void registerIgnoredValues(String... longNames) {
        for (String longName : longNames) {
            register(longName, null, CurlOptionValueMode.REQUIRED, CurlOptionAction.IGNORE);
        }
    }

    private static void registerIgnoredShortValues(char... shortNames) {
        for (char shortName : shortNames) {
            register(null, shortName, CurlOptionValueMode.REQUIRED, CurlOptionAction.IGNORE);
        }
    }

    private static String normalizeLongName(String longName) {
        String normalized = longName;
        while (normalized.startsWith("-")) {
            normalized = normalized.substring(1);
        }
        return normalized.toLowerCase();
    }
}
