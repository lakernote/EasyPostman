package com.laker.postman.http.runtime.okhttp;

import com.laker.postman.http.runtime.config.HttpRuntimeSettings;
import com.laker.postman.http.runtime.config.HttpRuntimeSettingsProvider;
import lombok.RequiredArgsConstructor;
import lombok.experimental.UtilityClass;

import java.net.Authenticator;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.PasswordAuthentication;
import java.net.Proxy;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@UtilityClass
class SocksProxyAuthenticatorSupport {
    private final Object INSTALL_LOCK = new Object();

    void configureFor(Proxy proxy, boolean allowWhenGlobalProxyDisabled) {
        ProxyEndpoint endpoint = ProxyEndpoint.from(proxy);
        if (endpoint == null || !hasConfiguredUsername()) {
            return;
        }
        EasyPostmanSocksAuthenticator authenticator = installIfNecessary();
        authenticator.allow(endpoint, EndpointPermission.of(allowWhenGlobalProxyDisabled));
    }

    void clearAllowedEndpoints() {
        Authenticator current = Authenticator.getDefault();
        if (current instanceof EasyPostmanSocksAuthenticator easyPostmanAuthenticator) {
            easyPostmanAuthenticator.clearAllowedEndpoints();
        }
    }

    private EasyPostmanSocksAuthenticator installIfNecessary() {
        synchronized (INSTALL_LOCK) {
            Authenticator current = Authenticator.getDefault();
            if (current instanceof EasyPostmanSocksAuthenticator easyPostmanAuthenticator) {
                return easyPostmanAuthenticator;
            }

            EasyPostmanSocksAuthenticator authenticator = new EasyPostmanSocksAuthenticator(current);
            Authenticator.setDefault(authenticator);
            return authenticator;
        }
    }

    private boolean hasConfiguredUsername() {
        return hasConfiguredUsername(settings());
    }

    private boolean hasConfiguredUsername(HttpRuntimeSettings runtimeSettings) {
        String username = runtimeSettings.getProxyUsername();
        return username != null && !username.trim().isEmpty();
    }

    private boolean isSocksProxyConfiguration(HttpRuntimeSettings runtimeSettings) {
        return runtimeSettings.isSystemProxyMode()
                || HttpRuntimeSettings.PROXY_TYPE_SOCKS.equalsIgnoreCase(runtimeSettings.getProxyType());
    }

    private PasswordAuthentication configuredCredentials(HttpRuntimeSettings runtimeSettings) {
        String password = runtimeSettings.getProxyPassword();
        return new PasswordAuthentication(
                runtimeSettings.getProxyUsername().trim(),
                (password == null ? "" : password).toCharArray()
        );
    }

    private HttpRuntimeSettings settings() {
        return HttpRuntimeSettingsProvider.get();
    }

    private record ProxyEndpoint(String host, String address, int port) {
        static ProxyEndpoint from(Proxy proxy) {
            if (proxy == null || proxy.type() != Proxy.Type.SOCKS || !(proxy.address() instanceof InetSocketAddress socketAddress)) {
                return null;
            }
            String host = normalizeHost(socketAddress.getHostString());
            String address = socketAddress.getAddress() == null
                    ? ""
                    : normalizeHost(socketAddress.getAddress().getHostAddress());
            return new ProxyEndpoint(host, address, socketAddress.getPort());
        }

        boolean matches(String requestingHost, InetAddress requestingSite, int requestingPort) {
            if (port != requestingPort) {
                return false;
            }
            String normalizedRequestingHost = normalizeHost(requestingHost);
            if (!normalizedRequestingHost.isEmpty() && (normalizedRequestingHost.equals(host) || normalizedRequestingHost.equals(address))) {
                return true;
            }
            if (requestingSite == null) {
                return false;
            }
            String siteAddress = normalizeHost(requestingSite.getHostAddress());
            String siteHost = normalizeHost(requestingSite.getHostName());
            return (!siteAddress.isEmpty() && (siteAddress.equals(address) || siteAddress.equals(host)))
                    || (!siteHost.isEmpty() && (siteHost.equals(host) || siteHost.equals(address)));
        }

        private static String normalizeHost(String value) {
            return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        }
    }

    private record EndpointPermission(boolean allowWhenGlobalProxyDisabled) {
        static EndpointPermission of(boolean allowWhenGlobalProxyDisabled) {
            return new EndpointPermission(allowWhenGlobalProxyDisabled);
        }

        EndpointPermission merge(EndpointPermission other) {
            return new EndpointPermission(allowWhenGlobalProxyDisabled || other.allowWhenGlobalProxyDisabled);
        }

        boolean canUseCredentials(HttpRuntimeSettings runtimeSettings) {
            return runtimeSettings.isProxyEnabled() || allowWhenGlobalProxyDisabled;
        }
    }

    private record AuthenticationRequest(String protocol, String host, InetAddress site, int port) {
        boolean isSocks() {
            return protocol != null && protocol.toUpperCase(Locale.ROOT).startsWith("SOCKS");
        }

        boolean matches(ProxyEndpoint endpoint) {
            return endpoint.matches(host, site, port);
        }
    }

    @RequiredArgsConstructor
    private static final class EasyPostmanSocksAuthenticator extends Authenticator {
        private final Authenticator delegate;
        private final Map<ProxyEndpoint, EndpointPermission> endpointPermissions = new ConcurrentHashMap<>();

        private void allow(ProxyEndpoint endpoint, EndpointPermission permission) {
            endpointPermissions.merge(endpoint, permission, EndpointPermission::merge);
        }

        private void clearAllowedEndpoints() {
            endpointPermissions.clear();
        }

        @Override
        protected PasswordAuthentication getPasswordAuthentication() {
            HttpRuntimeSettings runtimeSettings = settings();
            if (shouldHandleSocksAuthentication(runtimeSettings, currentRequest())) {
                return configuredCredentials(runtimeSettings);
            }
            return delegatePasswordAuthentication();
        }

        private AuthenticationRequest currentRequest() {
            return new AuthenticationRequest(
                    getRequestingProtocol(),
                    getRequestingHost(),
                    getRequestingSite(),
                    getRequestingPort()
            );
        }

        private boolean shouldHandleSocksAuthentication(HttpRuntimeSettings runtimeSettings, AuthenticationRequest request) {
            if (!request.isSocks()) {
                return false;
            }

            if (!isSocksProxyConfiguration(runtimeSettings) || !hasConfiguredUsername(runtimeSettings)) {
                return false;
            }

            for (Map.Entry<ProxyEndpoint, EndpointPermission> entry : endpointPermissions.entrySet()) {
                if (request.matches(entry.getKey()) && entry.getValue().canUseCredentials(runtimeSettings)) {
                    return true;
                }
            }
            return false;
        }

        private PasswordAuthentication delegatePasswordAuthentication() {
            if (delegate == null) {
                return null;
            }
            return delegate.requestPasswordAuthenticationInstance(
                    getRequestingHost(),
                    getRequestingSite(),
                    getRequestingPort(),
                    getRequestingProtocol(),
                    getRequestingPrompt(),
                    getRequestingScheme(),
                    getRequestingURL(),
                    getRequestorType()
            );
        }
    }
}
