package com.laker.postman.service.http.ssl;

import lombok.extern.slf4j.Slf4j;

import javax.net.ssl.*;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.security.cert.Certificate;
import java.util.ArrayList;
import java.util.List;

/**
 * 自定义 SSLSocketFactory，用于捕获SSL握手过程中的证书信息
 */
@Slf4j
public class CertificateCapturingSSLSocketFactory extends SSLSocketFactory {

    private final SSLSocketFactory delegate;

    // 用于传递最近捕获的证书（作为备用方案）
    private static final List<Certificate> lastCapturedCertificates = new ArrayList<>();

    public CertificateCapturingSSLSocketFactory(SSLContext sslContext) {
        this.delegate = sslContext.getSocketFactory();
    }

    /**
     * 获取最近捕获的证书（备用方案）
     */
    public static List<Certificate> getLastCapturedCertificates() {
        return new ArrayList<>(lastCapturedCertificates);
    }

    @Override
    public String[] getDefaultCipherSuites() {
        return delegate.getDefaultCipherSuites();
    }

    @Override
    public String[] getSupportedCipherSuites() {
        return delegate.getSupportedCipherSuites();
    }

    @Override
    public Socket createSocket(Socket s, String host, int port, boolean autoClose) throws IOException {
        SSLSocket socket = (SSLSocket) delegate.createSocket(s, host, port, autoClose);
        return wrapSocket(socket);
    }

    @Override
    public Socket createSocket(String host, int port) throws IOException {
        SSLSocket socket = (SSLSocket) delegate.createSocket(host, port);
        return wrapSocket(socket);
    }

    @Override
    public Socket createSocket(String host, int port, InetAddress localHost, int localPort) throws IOException {
        SSLSocket socket = (SSLSocket) delegate.createSocket(host, port, localHost, localPort);
        return wrapSocket(socket);
    }

    @Override
    public Socket createSocket(InetAddress host, int port) throws IOException {
        SSLSocket socket = (SSLSocket) delegate.createSocket(host, port);
        return wrapSocket(socket);
    }

    @Override
    public Socket createSocket(InetAddress address, int port, InetAddress localAddress, int localPort) throws IOException {
        SSLSocket socket = (SSLSocket) delegate.createSocket(address, port, localAddress, localPort);
        return wrapSocket(socket);
    }

    /**
     * 包装 SSLSocket 以捕获证书信息
     */
    private SSLSocket wrapSocket(SSLSocket socket) {
        socket.setUseClientMode(true);

        // 添加握手监听器来捕获证书信息
        socket.addHandshakeCompletedListener(event -> {
            try {
                captureCertificates(event);
            } catch (Exception e) {
                log.error("Failed to capture certificates: {}", e.getMessage(), e);
            }
        });

        return socket;
    }

    /**
     * 从握手事件中捕获证书信息
     */
    private void captureCertificates(HandshakeCompletedEvent event) {
        try {
            SSLSession session = event.getSession();
            String sessionId = bytesToHex(session.getId());

            log.debug("SSL Handshake completed - Protocol: {}, Cipher: {}, SessionId: {}",
                    session.getProtocol(), session.getCipherSuite(), sessionId);

            // 从 SSLSession 中获取证书
            Certificate[] certs = session.getPeerCertificates();
            if (certs != null && certs.length > 0) {
                List<Certificate> certList = new ArrayList<>(java.util.Arrays.asList(certs));

                lastCapturedCertificates.clear();
                lastCapturedCertificates.addAll(certList);

                log.debug("Captured {} certificates from SSLSession (SessionId: {})",
                        certs.length, sessionId);

                // 记录第一个证书的详情
                if (certs[0] instanceof java.security.cert.X509Certificate x509) {
                    log.debug("Certificate Subject: {}",
                            x509.getSubjectX500Principal().getName());
                    log.debug("Certificate Issuer: {}",
                            x509.getIssuerX500Principal().getName());
                }
            } else {
                log.warn("No certificates in SSLSession");
            }
        } catch (SSLPeerUnverifiedException e) {
            log.debug("Peer unverified: {}", e.getMessage());
        }
    }

    /**
     * 将字节数组转换为十六进制字符串
     */
    private static String bytesToHex(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
