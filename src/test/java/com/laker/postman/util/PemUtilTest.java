package com.laker.postman.util;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import org.testng.annotations.Test;

import javax.net.ssl.KeyManager;
import java.nio.charset.StandardCharsets;

import static org.testng.Assert.assertNotNull;

public class PemUtilTest {

    @Test
    public void testCreateKeyManager__with_PKCS1_RSA() throws Exception {
        String certificatePath = "certificate/rsa/rsa.crt";
        String privateKeyPath = "certificate/rsa/pkcs1_rsa.key";
        assertNotNull(this.createKeyManagers(certificatePath, privateKeyPath, null));
    }

    @Test
    public void testCreateKeyManager__with_PKCS8_RSA() throws Exception {
        String certificatePath = "certificate/rsa/rsa.crt";
        String privateKeyPath = "certificate/rsa/pkcs8_rsa.key";
        assertNotNull(this.createKeyManagers(certificatePath, privateKeyPath, null));
    }

    @Test
    public void testCreateKeyManager__with_PKCS8_RSA_Encrypted() throws Exception {
        String certificatePath = "certificate/rsa/rsa.crt";
        String privateKeyPath = "certificate/rsa/pkcs8_rsa_encrypted.key";
        String privateKeyPassword = "123456";
        assertNotNull(this.createKeyManagers(certificatePath, privateKeyPath, privateKeyPassword));
    }

    @Test
    public void testCreateKeyManager__with_PKCS1_EC() throws Exception {
        String certificatePath = "certificate/ec/ec.crt";
        String privateKeyPath = "certificate/ec/pkcs1_ec.key";
        assertNotNull(this.createKeyManagers(certificatePath, privateKeyPath, null));
    }

    @Test
    public void testCreateKeyManager__with_PKCS8_EC() throws Exception {
        String certificatePath = "certificate/ec/ec.crt";
        String privateKeyPath = "certificate/ec/pkcs8_ec.key";
        assertNotNull(this.createKeyManagers(certificatePath, privateKeyPath, null));
    }

    private KeyManager[] createKeyManagers(String certificatePath, String privateKeyPath, String privateKeyPassword) throws Exception {
        String certPem = FileUtil.readString(certificatePath, StandardCharsets.UTF_8);
        String keyPem = FileUtil.readString(privateKeyPath, StandardCharsets.UTF_8);
        return PemUtil.createKeyManagers(certPem, keyPem, StrUtil.trimToNull(privateKeyPassword));
    }
}
