package com.laker.postman.service.http.ssl;

import org.testng.annotations.Test;

import java.math.BigInteger;
import java.security.Principal;
import java.security.PublicKey;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Set;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class SSLCertificateValidatorTest {

    @Test
    public void hostnameMatchShouldFailWhenCertificateHostnameInspectionThrows() throws Exception {
        X509Certificate certificate = new BrokenAltNamesCertificate(TestTrustMaterialFixtures.loadCertificate());

        boolean matches = SSLCertificateValidator.isHostnameMatchPublic(certificate, "api.localhost");

        assertFalse(matches);
    }

    @Test
    public void validateCertificatesShouldReportHostnameMismatchWhenCertificateHostnameInspectionThrows() throws Exception {
        X509Certificate certificate = new BrokenAltNamesCertificate(TestTrustMaterialFixtures.loadCertificate());

        SSLValidationResult result = SSLCertificateValidator.validateCertificates(List.of(certificate), "api.localhost");

        assertTrue(result.hasErrors());
        assertTrue(result.getSummary().contains("Hostname mismatch"));
    }

    private static final class BrokenAltNamesCertificate extends X509Certificate {
        private final X509Certificate delegate;

        private BrokenAltNamesCertificate(X509Certificate delegate) {
            this.delegate = delegate;
        }

        @Override
        public Collection<List<?>> getSubjectAlternativeNames() throws CertificateParsingException {
            throw new CertificateParsingException("broken SAN extension");
        }

        @Override
        public void checkValidity() throws CertificateExpiredException, CertificateNotYetValidException {
            delegate.checkValidity();
        }

        @Override
        public void checkValidity(Date date) throws CertificateExpiredException, CertificateNotYetValidException {
            delegate.checkValidity(date);
        }

        @Override
        public int getVersion() {
            return delegate.getVersion();
        }

        @Override
        public BigInteger getSerialNumber() {
            return delegate.getSerialNumber();
        }

        @Override
        public Principal getIssuerDN() {
            return delegate.getIssuerDN();
        }

        @Override
        public Principal getSubjectDN() {
            return delegate.getSubjectDN();
        }

        @Override
        public Date getNotBefore() {
            return delegate.getNotBefore();
        }

        @Override
        public Date getNotAfter() {
            return delegate.getNotAfter();
        }

        @Override
        public byte[] getTBSCertificate() throws CertificateEncodingException {
            return delegate.getTBSCertificate();
        }

        @Override
        public byte[] getSignature() {
            return delegate.getSignature();
        }

        @Override
        public String getSigAlgName() {
            return delegate.getSigAlgName();
        }

        @Override
        public String getSigAlgOID() {
            return delegate.getSigAlgOID();
        }

        @Override
        public byte[] getSigAlgParams() {
            return delegate.getSigAlgParams();
        }

        @Override
        public boolean[] getIssuerUniqueID() {
            return delegate.getIssuerUniqueID();
        }

        @Override
        public boolean[] getSubjectUniqueID() {
            return delegate.getSubjectUniqueID();
        }

        @Override
        public boolean[] getKeyUsage() {
            return delegate.getKeyUsage();
        }

        @Override
        public int getBasicConstraints() {
            return delegate.getBasicConstraints();
        }

        @Override
        public byte[] getEncoded() throws CertificateEncodingException {
            return delegate.getEncoded();
        }

        @Override
        public void verify(PublicKey key) throws CertificateException {
            try {
                delegate.verify(key);
            } catch (CertificateException e) {
                throw e;
            } catch (Exception e) {
                throw new CertificateException(e);
            }
        }

        @Override
        public void verify(PublicKey key, String sigProvider) throws CertificateException {
            try {
                delegate.verify(key, sigProvider);
            } catch (CertificateException e) {
                throw e;
            } catch (Exception e) {
                throw new CertificateException(e);
            }
        }

        @Override
        public String toString() {
            return delegate.toString();
        }

        @Override
        public PublicKey getPublicKey() {
            return delegate.getPublicKey();
        }

        @Override
        public Set<String> getCriticalExtensionOIDs() {
            return delegate.getCriticalExtensionOIDs();
        }

        @Override
        public Set<String> getNonCriticalExtensionOIDs() {
            return delegate.getNonCriticalExtensionOIDs();
        }

        @Override
        public byte[] getExtensionValue(String oid) {
            return delegate.getExtensionValue(oid);
        }

        @Override
        public boolean hasUnsupportedCriticalExtension() {
            return delegate.hasUnsupportedCriticalExtension();
        }

        @Override
        public Collection<List<?>> getIssuerAlternativeNames() throws CertificateParsingException {
            return delegate.getIssuerAlternativeNames();
        }

        @Override
        public javax.security.auth.x500.X500Principal getSubjectX500Principal() {
            return delegate.getSubjectX500Principal();
        }

        @Override
        public javax.security.auth.x500.X500Principal getIssuerX500Principal() {
            return delegate.getIssuerX500Principal();
        }
    }
}
