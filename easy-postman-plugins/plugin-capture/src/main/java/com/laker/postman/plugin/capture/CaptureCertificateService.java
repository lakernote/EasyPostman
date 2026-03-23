package com.laker.postman.plugin.capture;

import com.laker.postman.util.SystemUtil;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.util.NetUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.asn1.x509.KeyPurposeId;
import org.bouncycastle.asn1.x509.KeyUsage;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509ExtensionUtils;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.bouncycastle.pkcs.PKCS10CertificationRequestBuilder;
import org.bouncycastle.pkcs.jcajce.JcaPKCS10CertificationRequestBuilder;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;

import javax.net.ssl.SSLException;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.RandomAccessFile;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.util.HexFormat;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

final class CaptureCertificateService {
    private static final String BC = BouncyCastleProvider.PROVIDER_NAME;
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Logger log = LoggerFactory.getLogger(CaptureCertificateService.class);

    private final File caDirectory;
    private final File caCertFile;
    private final File caKeyFile;
    private final File caLockFile;
    private final Map<String, SslContext> serverContextCache = new ConcurrentHashMap<>();

    private volatile KeyPair rootKeyPair;
    private volatile X509Certificate rootCertificate;

    CaptureCertificateService() {
        if (Security.getProvider(BC) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
        caDirectory = new File(SystemUtil.getEasyPostmanPath(), "capture-ca");
        caCertFile = new File(caDirectory, "easy-postman-capture-root-ca.crt");
        caKeyFile = new File(caDirectory, "easy-postman-capture-root-ca.key");
        caLockFile = new File(caDirectory, ".capture-ca.lock");
    }

    String rootCertificatePath() throws Exception {
        ensureRootCa();
        return caCertFile.getAbsolutePath();
    }

    SslContext buildServerSslContext(String host) throws Exception {
        ensureRootCa();
        String normalizedHost = normalizeHost(host);
        log.info("Building MITM SSL context for host {} (normalized: {})", host, normalizedHost);
        return serverContextCache.computeIfAbsent(normalizedHost, this::createServerContextUnchecked);
    }

    SslContext buildClientSslContext() throws SSLException {
        return SslContextBuilder.forClient().build();
    }

    private SslContext createServerContextUnchecked(String host) {
        try {
            KeyPair leafKeyPair = generateRsaKeyPair();
            X509Certificate certificate = issueLeafCertificate(host, leafKeyPair);
            log.info("Issued MITM leaf certificate for host {}", host);
            return SslContextBuilder.forServer(leafKeyPair.getPrivate(), certificate, rootCertificate).build();
        } catch (Exception ex) {
            log.error("Failed to build MITM certificate for host {}", host, ex);
            throw new IllegalStateException("Failed to build MITM certificate for host " + host, ex);
        }
    }

    private synchronized void ensureRootCa() throws Exception {
        if (rootCertificate != null && rootKeyPair != null) {
            return;
        }
        if (!caDirectory.exists() && !caDirectory.mkdirs()) {
            throw new IllegalStateException("Failed to create CA directory: " + caDirectory);
        }
        try (RandomAccessFile lockHandle = new RandomAccessFile(caLockFile, "rw");
             FileChannel lockChannel = lockHandle.getChannel();
             FileLock ignored = lockChannel.lock()) {
            loadOrCreateRootCa();
        }
    }

    private void loadOrCreateRootCa() throws Exception {
        if (caCertFile.exists() && caKeyFile.exists()) {
            log.info("Loading existing capture root CA from {}", caCertFile.getAbsolutePath());
            X509Certificate loadedCertificate = readCertificate(caCertFile);
            KeyPair loadedKeyPair = new KeyPair(loadedCertificate.getPublicKey(), readPrivateKey(caKeyFile));
            try {
                validateRootCaMaterial(loadedCertificate, loadedKeyPair);
                rootCertificate = loadedCertificate;
                rootKeyPair = loadedKeyPair;
                log.info("Capture root CA loaded successfully: {}", fingerprint(loadedCertificate));
                return;
            } catch (Exception ex) {
                log.warn("Existing capture root CA is invalid, regenerating certificate chain", ex);
                serverContextCache.clear();
            }
        }
        regenerateRootCa();
    }

    private KeyPair generateRsaKeyPair() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048, RANDOM);
        return generator.generateKeyPair();
    }

    private X509Certificate createRootCertificate(KeyPair keyPair) throws Exception {
        Instant now = Instant.now();
        X500Name issuer = new X500Name("CN=EasyPostman Capture Root CA, O=EasyPostman, C=CN");
        BigInteger serial = new BigInteger(160, RANDOM).abs();
        X509v3CertificateBuilder builder = new JcaX509v3CertificateBuilder(
                issuer,
                serial,
                Date.from(now.minus(1, ChronoUnit.DAYS)),
                Date.from(now.plus(3650, ChronoUnit.DAYS)),
                issuer,
                keyPair.getPublic()
        );
        JcaX509ExtensionUtils extensionUtils = new JcaX509ExtensionUtils();
        builder.addExtension(Extension.basicConstraints, true, new BasicConstraints(true));
        builder.addExtension(Extension.keyUsage, true, new KeyUsage(KeyUsage.keyCertSign | KeyUsage.cRLSign | KeyUsage.digitalSignature));
        builder.addExtension(Extension.subjectKeyIdentifier, false, extensionUtils.createSubjectKeyIdentifier(keyPair.getPublic()));
        builder.addExtension(Extension.authorityKeyIdentifier, false, extensionUtils.createAuthorityKeyIdentifier(keyPair.getPublic()));
        return signCertificate(builder, keyPair.getPrivate());
    }

    private X509Certificate issueLeafCertificate(String host, KeyPair leafKeyPair) throws Exception {
        Instant now = Instant.now();
        X500Name issuer = X500Name.getInstance(rootCertificate.getSubjectX500Principal().getEncoded());
        X500Name subject = new X500Name("CN=" + host);
        BigInteger serial = new BigInteger(160, RANDOM).abs();

        PKCS10CertificationRequest csr = createCsr(host, leafKeyPair);
        SubjectPublicKeyInfo publicKeyInfo = csr.getSubjectPublicKeyInfo();
        X509v3CertificateBuilder builder = new X509v3CertificateBuilder(
                issuer,
                serial,
                Date.from(now.minus(1, ChronoUnit.DAYS)),
                Date.from(now.plus(365, ChronoUnit.DAYS)),
                subject,
                publicKeyInfo
        );
        JcaX509ExtensionUtils extensionUtils = new JcaX509ExtensionUtils();
        builder.addExtension(Extension.basicConstraints, true, new BasicConstraints(false));
        builder.addExtension(Extension.keyUsage, true, new KeyUsage(KeyUsage.digitalSignature | KeyUsage.keyEncipherment));
        builder.addExtension(Extension.extendedKeyUsage, false, new org.bouncycastle.asn1.x509.ExtendedKeyUsage(KeyPurposeId.id_kp_serverAuth));
        builder.addExtension(Extension.subjectKeyIdentifier, false, extensionUtils.createSubjectKeyIdentifier(leafKeyPair.getPublic()));
        builder.addExtension(Extension.authorityKeyIdentifier, false, extensionUtils.createAuthorityKeyIdentifier(rootCertificate));
        builder.addExtension(Extension.subjectAlternativeName, false, buildSubjectAlternativeNames(host));
        return signCertificate(builder, rootKeyPair.getPrivate());
    }

    private PKCS10CertificationRequest createCsr(String host, KeyPair keyPair) throws Exception {
        X500Name subject = new X500Name("CN=" + host);
        PKCS10CertificationRequestBuilder csrBuilder = new JcaPKCS10CertificationRequestBuilder(subject, keyPair.getPublic());
        ContentSigner signer = new JcaContentSignerBuilder("SHA256withRSA").setProvider(BC).build(keyPair.getPrivate());
        return csrBuilder.build(signer);
    }

    private X509Certificate signCertificate(X509v3CertificateBuilder builder, PrivateKey signerKey) throws Exception {
        ContentSigner signer = new JcaContentSignerBuilder("SHA256withRSA").setProvider(BC).build(signerKey);
        X509CertificateHolder holder = builder.build(signer);
        return new JcaX509CertificateConverter().setProvider(BC).getCertificate(holder);
    }

    private ASN1Encodable buildSubjectAlternativeNames(String host) {
        String normalizedHost = normalizeHost(host);
        String ipCandidate = stripIpv6ZoneId(normalizedHost);
        if (isIpAddress(ipCandidate)) {
            try {
                return new GeneralNames(new GeneralName(GeneralName.iPAddress, ipCandidate));
            } catch (IllegalArgumentException ex) {
                log.warn("Falling back to DNS SAN for host {} after IP SAN build failed", normalizedHost, ex);
            }
        }
        return new GeneralNames(new GeneralName(GeneralName.dNSName, normalizedHost));
    }

    private boolean isIpAddress(String host) {
        return NetUtil.isValidIpV4Address(host) || NetUtil.isValidIpV6Address(host);
    }

    private String normalizeHost(String host) {
        if (host == null) {
            throw new IllegalArgumentException("Host must not be null");
        }
        String normalized = host.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("Host must not be blank");
        }
        if (normalized.startsWith("[") && normalized.endsWith("]") && normalized.length() > 2) {
            normalized = normalized.substring(1, normalized.length() - 1);
        }
        int firstColon = normalized.indexOf(':');
        int lastColon = normalized.lastIndexOf(':');
        if (firstColon > 0 && firstColon == lastColon) {
            String portCandidate = normalized.substring(lastColon + 1);
            if (!portCandidate.isEmpty() && portCandidate.chars().allMatch(Character::isDigit)) {
                normalized = normalized.substring(0, lastColon);
            }
        }
        while (normalized.endsWith(".")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("Host must not be blank");
        }
        return normalized;
    }

    private String stripIpv6ZoneId(String host) {
        int zoneIndex = host.indexOf('%');
        return zoneIndex >= 0 ? host.substring(0, zoneIndex) : host;
    }

    private void regenerateRootCa() throws Exception {
        log.info("Generating new capture root CA in {}", caDirectory.getAbsolutePath());
        rootKeyPair = generateRsaKeyPair();
        rootCertificate = createRootCertificate(rootKeyPair);
        writePemAtomically(caCertFile.toPath(), rootCertificate);
        writePemAtomically(caKeyFile.toPath(), rootKeyPair.getPrivate());
        serverContextCache.clear();
        log.info("Capture root CA generated: {} ({})", caCertFile.getAbsolutePath(), fingerprint(rootCertificate));
    }

    private void validateRootCaMaterial(X509Certificate certificate, KeyPair keyPair) throws Exception {
        certificate.checkValidity(new Date());
        if (certificate.getBasicConstraints() < 0) {
            throw new IllegalStateException("Root certificate is not a CA certificate");
        }
        certificate.verify(certificate.getPublicKey());

        KeyPair leafKeyPair = generateRsaKeyPair();
        X509Certificate issuedLeaf = issueLeafCertificate("easy-postman.local", leafKeyPair, certificate, keyPair);
        issuedLeaf.checkValidity(new Date());
        issuedLeaf.verify(certificate.getPublicKey());
        SslContextBuilder.forServer(leafKeyPair.getPrivate(), issuedLeaf, certificate).build();
    }

    private X509Certificate issueLeafCertificate(String host, KeyPair leafKeyPair, X509Certificate issuerCertificate, KeyPair issuerKeyPair) throws Exception {
        Instant now = Instant.now();
        X500Name issuer = X500Name.getInstance(issuerCertificate.getSubjectX500Principal().getEncoded());
        X500Name subject = new X500Name("CN=" + host);
        BigInteger serial = new BigInteger(160, RANDOM).abs();

        PKCS10CertificationRequest csr = createCsr(host, leafKeyPair);
        SubjectPublicKeyInfo publicKeyInfo = csr.getSubjectPublicKeyInfo();
        X509v3CertificateBuilder builder = new X509v3CertificateBuilder(
                issuer,
                serial,
                Date.from(now.minus(1, ChronoUnit.DAYS)),
                Date.from(now.plus(365, ChronoUnit.DAYS)),
                subject,
                publicKeyInfo
        );
        JcaX509ExtensionUtils extensionUtils = new JcaX509ExtensionUtils();
        builder.addExtension(Extension.basicConstraints, true, new BasicConstraints(false));
        builder.addExtension(Extension.keyUsage, true, new KeyUsage(KeyUsage.digitalSignature | KeyUsage.keyEncipherment));
        builder.addExtension(Extension.extendedKeyUsage, false, new org.bouncycastle.asn1.x509.ExtendedKeyUsage(KeyPurposeId.id_kp_serverAuth));
        builder.addExtension(Extension.subjectKeyIdentifier, false, extensionUtils.createSubjectKeyIdentifier(leafKeyPair.getPublic()));
        builder.addExtension(Extension.authorityKeyIdentifier, false, extensionUtils.createAuthorityKeyIdentifier(issuerCertificate));
        builder.addExtension(Extension.subjectAlternativeName, false, buildSubjectAlternativeNames(host));
        return signCertificate(builder, issuerKeyPair.getPrivate());
    }

    private X509Certificate readCertificate(File file) throws Exception {
        try (PEMParser parser = new PEMParser(new FileReader(file, StandardCharsets.UTF_8))) {
            Object object = parser.readObject();
            if (!(object instanceof X509CertificateHolder holder)) {
                throw new IllegalStateException("Unexpected certificate format: " + file);
            }
            return new JcaX509CertificateConverter().setProvider(BC).getCertificate(holder);
        }
    }

    private PrivateKey readPrivateKey(File file) throws Exception {
        try (PEMParser parser = new PEMParser(new FileReader(file, StandardCharsets.UTF_8))) {
            Object object = parser.readObject();
            JcaPEMKeyConverter converter = new JcaPEMKeyConverter().setProvider(BC);
            if (object instanceof PrivateKeyInfo privateKeyInfo) {
                return converter.getPrivateKey(privateKeyInfo);
            }
            if (object instanceof PEMKeyPair keyPair) {
                return converter.getPrivateKey(keyPair.getPrivateKeyInfo());
            }
            throw new IllegalStateException("Unexpected private key format: " + file);
        }
    }

    private void writePem(Path path, Object object) throws Exception {
        try (JcaPEMWriter writer = new JcaPEMWriter(new FileWriter(path.toFile(), StandardCharsets.UTF_8))) {
            writer.writeObject(object);
        }
    }

    private void writePemAtomically(Path target, Object object) throws Exception {
        Path tempFile = Files.createTempFile(caDirectory.toPath(), target.getFileName().toString(), ".tmp");
        try {
            writePem(tempFile, object);
            moveAtomically(tempFile, target);
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    private void moveAtomically(Path source, Path target) throws Exception {
        try {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException ex) {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private String fingerprint(X509Certificate certificate) throws Exception {
        return HexFormat.ofDelimiter(":").withUpperCase().formatHex(
                java.security.MessageDigest.getInstance("SHA-256").digest(certificate.getEncoded()));
    }
}
