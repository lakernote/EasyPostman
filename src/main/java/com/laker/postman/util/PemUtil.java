package com.laker.postman.util;

import cn.hutool.core.util.StrUtil;

import javax.crypto.Cipher;
import javax.crypto.EncryptedPrivateKeyInfo;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import java.io.ByteArrayInputStream;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.HexFormat;

/**
 * 零依赖 PEM 工具类 (JDK 17+)
 * 支持加载 X.509 证书、PKCS#1/SEC1/PKCS#8 私钥 (含加密)
 */
public class PemUtil {

    // JDK 17 新特性：直接使用 HexFormat 解析 OID，无需手写工具方法
    private static final byte[] OID_EC = HexFormat.of().parseHex("06072A8648CE3D0201");

    /**
     * 1. 加载 X.509 证书
     */
    public static X509Certificate loadCertificate(String certPem) throws Exception {
        // 使用 MIME Decoder 对于某些带有非标换行的 PEM 更宽容，但在去掉头尾后，Standard Decoder 更快
        String content = cleanPem(certPem, "CERTIFICATE");
        byte[] decoded = Base64.getDecoder().decode(content);

        CertificateFactory factory = CertificateFactory.getInstance("X.509");
        return (X509Certificate) factory.generateCertificate(new ByteArrayInputStream(decoded));
    }

    /**
     * 2. 加载私钥 (全能模式)
     * 支持：PKCS#1 (RSA), SEC1 (EC), PKCS#8 (RSA/EC/DSA/EdDSA), Encrypted PKCS#8
     */
    public static PrivateKey loadPrivateKey(String keyPem, String keyPassword) throws Exception {
        String content = keyPem.replaceAll("\\s+", "");

        // 1. 处理加密私钥 (PKCS#8 Encrypted)
        if (content.contains("BEGINENCRYPTEDPRIVATEKEY")) {
            if (keyPassword == null) throw new IllegalArgumentException("Encrypted key requires a password.");
            return decryptPKCS8(content, keyPassword);
        }

        // 2. 处理 PKCS#1 Legacy Encryption (不支持，建议转换)
        if (keyPem.contains("Proc-Type: 4,ENCRYPTED")) {
            throw new UnsupportedOperationException("Legacy PKCS#1 encryption is not supported. Use OpenSSL to convert to PKCS#8.");
        }

        // 3. 格式识别与路由
        if (content.contains("BEGINRSAPRIVATEKEY")) {
            return convertPKCS1ToPrivateKey(content);
        } else if (content.contains("BEGINECPRIVATEKEY")) {
            return convertSEC1ToECPrivateKey(content);
        } else if (content.contains("BEGINPRIVATEKEY")) {
            // PKCS#8 标准格式
            return loadPKCS8PrivateKey(content);
        } else {
            throw new IllegalArgumentException("Unknown or unsupported Private Key format.");
        }
    }

    /**
     * 3. 创建 KeyManagers (用于 SSLContext)
     */
    public static KeyManager[] createKeyManagers(X509Certificate cert, PrivateKey key, String password) throws Exception {
        KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        char[] keyStorePassword = password != null ? password.toCharArray() : new char[0];
        keyStore.load(null, null);
        keyStore.setKeyEntry("default", key, keyStorePassword, new Certificate[]{cert});

        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(keyStore, keyStorePassword);
        return kmf.getKeyManagers();
    }

    /**
     * 3. 创建 KeyManagers (用于 SSLContext)
     */
    public static KeyManager[] createKeyManagers(String certPem, String keyPem, String keyPassword) throws Exception {
        X509Certificate x509Certificate = PemUtil.loadCertificate(certPem);
        PrivateKey privateKey = PemUtil.loadPrivateKey(keyPem, StrUtil.trimToNull(keyPassword));
        return PemUtil.createKeyManagers(x509Certificate, privateKey, StrUtil.trimToNull(keyPassword));
    }

    // ================= 内部核心逻辑 =================

    // 解密 PKCS#8 私钥 (JDK 17 增强了对 PBE 算法的支持)
    private static PrivateKey decryptPKCS8(String pem, String password) throws Exception {
        byte[] encoded = decodePemBody(pem);
        EncryptedPrivateKeyInfo epki = new EncryptedPrivateKeyInfo(encoded);

        SecretKeyFactory keyFactory = SecretKeyFactory.getInstance(epki.getAlgName());
        SecretKey pbeKey = keyFactory.generateSecret(new PBEKeySpec(password.toCharArray()));

        Cipher cipher = Cipher.getInstance(epki.getAlgName());
        cipher.init(Cipher.DECRYPT_MODE, pbeKey, epki.getAlgParameters());

        PKCS8EncodedKeySpec keySpec = epki.getKeySpec(cipher);
        return generateKeyTryAllAlgorithms(keySpec);
    }

    // 尝试使用所有支持的算法加载 PKCS#8 Spec
    private static PrivateKey generateKeyTryAllAlgorithms(PKCS8EncodedKeySpec spec) {
        // JDK 17 支持 Ed25519 和 Ed448，这在旧版本中是不行的
        String[] algorithms = {"RSA", "EC", "Ed25519", "Ed448", "DSA", "RSASSA-PSS", "XDH"};

        for (String algo : algorithms) {
            try {
                return KeyFactory.getInstance(algo).generatePrivate(spec);
            } catch (Exception ignored) {
                // Try next
            }
        }
        throw new IllegalArgumentException("Unable to determine key algorithm (tried RSA, EC, EdDSA, etc.)");
    }

    private static PrivateKey loadPKCS8PrivateKey(String pem) {
        byte[] encoded = decodePemBody(pem);
        return generateKeyTryAllAlgorithms(new PKCS8EncodedKeySpec(encoded));
    }

    // 转换 PKCS#1 -> PKCS#8
    private static PrivateKey convertPKCS1ToPrivateKey(String pem) throws Exception {
        byte[] pkcs1Bytes = decodePemBody(pem);

        // 构造 PKCS#8 PrivateKeyInfo
        // AlgorithmIdentifier for RSA: 1.2.840.113549.1.1.1
        byte[] rsaAlgoId = HexFormat.of().parseHex("300D06092A864886F70D0101010500");

        // Wrap PKCS#1 bytes into Octet String
        byte[] pkcs1OctetString = joinBytes(
                new byte[]{0x04}, // OCTET STRING tag
                encodeLength(pkcs1Bytes.length),
                pkcs1Bytes
        );

        // Build PrivateKeyInfo sequence
        byte[] pkcs8Bytes = joinBytes(
                new byte[]{0x30}, // SEQUENCE tag
                encodeLength(3 + rsaAlgoId.length + pkcs1OctetString.length),
                new byte[]{0x02, 0x01, 0x00}, // version
                rsaAlgoId,
                pkcs1OctetString
        );

        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(pkcs8Bytes);
        return KeyFactory.getInstance("RSA").generatePrivate(spec);
    }

    // 转换 SEC1 (EC) -> PKCS#8
    private static PrivateKey convertSEC1ToECPrivateKey(String pem) throws Exception {
        byte[] sec1Bytes = decodePemBody(pem);

        // 提取曲线 OID
        byte[] curveOid = findCurveOID(sec1Bytes);
        if (curveOid == null) {
            throw new IllegalArgumentException("Could not extract Curve OID from EC key.");
        }

        // AlgorithmIdentifier = SEQUENCE { id-ecPublicKey, curveOID }
        byte[] algoId = joinBytes(
                new byte[]{0x30}, // SEQUENCE
                encodeLength(OID_EC.length + curveOid.length),
                OID_EC,
                curveOid
        );

        // PrivateKey = OCTET STRING 包裹 SEC1
        byte[] privateKeyOctet = joinBytes(
                new byte[]{0x04}, // OCTET STRING
                encodeLength(sec1Bytes.length),
                sec1Bytes
        );

        // PrivateKeyInfo = SEQUENCE { version, algoId, privateKeyOctet }
        byte[] pkcs8Bytes = joinBytes(
                new byte[]{0x30}, // SEQUENCE
                encodeLength(3 + algoId.length + privateKeyOctet.length),
                new byte[]{0x02, 0x01, 0x00}, // version
                algoId,
                privateKeyOctet
        );

        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(pkcs8Bytes);
        return KeyFactory.getInstance("EC").generatePrivate(spec);
    }

    // 构建 PKCS#8 ASN.1 结构
    private static PrivateKey generatePKCS8(String algorithm, byte[] keyBytes, byte[] algoIdBlock) throws Exception {
        byte[] pkcs8Bytes = joinBytes(
                new byte[]{0x30}, // Sequence
                encodeLength(1 + 2 + algoIdBlock.length + 2 + encodeLength(keyBytes.length).length + keyBytes.length),
                new byte[]{0x02, 0x01, 0x00}, // Version: 0
                new byte[]{0x30}, // AlgorithmIdentifier Sequence
                encodeLength(algoIdBlock.length),
                algoIdBlock,
                new byte[]{0x04}, // Octet String (PrivateKey)
                encodeLength(keyBytes.length),
                keyBytes
        );
        return KeyFactory.getInstance(algorithm).generatePrivate(new PKCS8EncodedKeySpec(pkcs8Bytes));
    }

    // ================= 辅助工具 =================

    private static String cleanPem(String pem, String type) {
        return pem.replace("-----BEGIN " + type + "-----", "")
                .replace("-----END " + type + "-----", "")
                .replaceAll("\\s+", "");
    }

    private static byte[] decodePemBody(String pem) {
        // 移除所有头部、尾部和空白字符
        String content = pem.replaceAll("-+[A-Z ]+-+", "").replaceAll("\\s+", "");
        return Base64.getDecoder().decode(content);
    }

    // 在 EC 私钥中扫描 Curve OID (ASN.1 Tag 0x06 inside Context Tag 0xA0)
    private static byte[] findCurveOID(byte[] sec1Bytes) {
        for (int i = 0; i < sec1Bytes.length - 10; i++) {
            if (sec1Bytes[i] == (byte) 0xA0) {
                int lenLen = (sec1Bytes[i + 1] & 0x80) == 0x80 ? (sec1Bytes[i + 1] & 0x7F) + 1 : 1;
                int offset = i + 1 + lenLen;
                if (sec1Bytes[offset] == 0x06) {
                    int oidLenLen = (sec1Bytes[offset + 1] & 0x80) == 0x80 ? (sec1Bytes[offset + 1] & 0x7F) + 1 : 1;
                    int oidLen = (oidLenLen == 1) ? sec1Bytes[offset + 1] : sec1Bytes[offset + oidLenLen];
                    byte[] result = new byte[1 + oidLenLen + oidLen];
                    System.arraycopy(sec1Bytes, offset, result, 0, result.length);
                    return result;
                }
            }
        }
        return null;
    }

    private static byte[] encodeLength(int length) {
        if (length < 128) return new byte[]{(byte) length};
        // JDK 17 虽然有 HexFormat，但这里的逻辑是处理 ASN.1 长度编码，直接位运算更直观
        String hex = Integer.toHexString(length);
        if (hex.length() % 2 != 0) hex = "0" + hex;
        byte[] lenBytes = HexFormat.of().parseHex(hex); // 利用 JDK 17 简化
        byte[] res = new byte[1 + lenBytes.length];
        res[0] = (byte) (0x80 | lenBytes.length);
        System.arraycopy(lenBytes, 0, res, 1, lenBytes.length);
        return res;
    }

    private static byte[] joinBytes(byte[]... parts) {
        int total = 0;
        for (byte[] b : parts) total += b.length;
        byte[] res = new byte[total];
        int cur = 0;
        for (byte[] b : parts) {
            System.arraycopy(b, 0, res, cur, b.length);
            cur += b.length;
        }
        return res;
    }
}