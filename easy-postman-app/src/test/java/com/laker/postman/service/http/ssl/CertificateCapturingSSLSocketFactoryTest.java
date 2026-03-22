package com.laker.postman.service.http.ssl;

import org.testng.annotations.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotSame;
import static org.testng.Assert.assertTrue;

public class CertificateCapturingSSLSocketFactoryTest {

    @Test
    public void capturedCertificatesShouldBeIsolatedPerThread() throws Exception {
        DummyCertificate threadOneCertificate = new DummyCertificate("thread-one");
        DummyCertificate threadTwoCertificate = new DummyCertificate("thread-two");
        AtomicReference<List<Certificate>> threadOneResult = new AtomicReference<>();
        AtomicReference<List<Certificate>> threadTwoResult = new AtomicReference<>();

        Thread threadOne = new Thread(() -> {
            CertificateCapturingSSLSocketFactory.rememberCapturedCertificates(List.of(threadOneCertificate));
            threadOneResult.set(CertificateCapturingSSLSocketFactory.getLastCapturedCertificates());
            CertificateCapturingSSLSocketFactory.clearLastCapturedCertificates();
        }, "ssl-capture-thread-one");

        Thread threadTwo = new Thread(() -> {
            CertificateCapturingSSLSocketFactory.rememberCapturedCertificates(List.of(threadTwoCertificate));
            threadTwoResult.set(CertificateCapturingSSLSocketFactory.getLastCapturedCertificates());
            CertificateCapturingSSLSocketFactory.clearLastCapturedCertificates();
        }, "ssl-capture-thread-two");

        threadOne.start();
        threadTwo.start();
        threadOne.join();
        threadTwo.join();

        assertEquals(threadOneResult.get(), List.of(threadOneCertificate));
        assertEquals(threadTwoResult.get(), List.of(threadTwoCertificate));
        assertNotSame(threadOneResult.get().get(0), threadTwoResult.get().get(0));
        assertTrue(CertificateCapturingSSLSocketFactory.getLastCapturedCertificates().isEmpty());
    }

    private static final class DummyCertificate extends Certificate {
        private final String value;

        private DummyCertificate(String value) {
            super("X.509");
            this.value = value;
        }

        @Override
        public byte[] getEncoded() throws CertificateEncodingException {
            return value.getBytes(java.nio.charset.StandardCharsets.US_ASCII);
        }

        @Override
        public void verify(PublicKey key) {
        }

        @Override
        public void verify(PublicKey key, String sigProvider) {
        }

        @Override
        public String toString() {
            return value;
        }

        @Override
        public PublicKey getPublicKey() {
            return null;
        }
    }
}
