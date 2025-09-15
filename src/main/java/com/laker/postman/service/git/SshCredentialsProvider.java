package com.laker.postman.service.git;

import cn.hutool.core.io.FileUtil;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.sshd.common.util.security.SecurityUtils;
import org.eclipse.jgit.api.TransportConfigCallback;
import org.eclipse.jgit.transport.SshSessionFactory;
import org.eclipse.jgit.transport.SshTransport;
import org.eclipse.jgit.transport.Transport;
import org.eclipse.jgit.transport.sshd.SshdSessionFactoryBuilder;
import org.eclipse.jgit.util.FS;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;

/**
 * SSH认证提供者
 * 使用 Apache MINA SSHD 实现SSH密钥认证
 */
@Slf4j
public class SshCredentialsProvider implements TransportConfigCallback {

    private final String privateKeyPath;
    private final String passphrase;

    public SshCredentialsProvider(String privateKeyPath, String passphrase) {
        this.privateKeyPath = privateKeyPath;
        this.passphrase = passphrase;
    }

    @Override
    public void configure(Transport transport) {
        if (transport instanceof SshTransport sshTransport) {
            sshTransport.setSshSessionFactory(createSshSessionFactory());
        }
    }

    @SneakyThrows
    private SshSessionFactory createSshSessionFactory() {
        String privateKeyContent = FileUtil.readString(new File(privateKeyPath), StandardCharsets.UTF_8);
        Iterable<KeyPair> keyPairs = SecurityUtils.loadKeyPairIdentities(null,
                null,
                new ByteArrayInputStream(privateKeyContent.getBytes()),
                (session, resourceKey, retryIndex) -> passphrase);

        return new SshdSessionFactoryBuilder()
                .setPreferredAuthentications("publickey") // 使用公钥认证
                .setDefaultKeysProvider(ignoredSshDirBecauseWeUseAnInMemorySetOfKeyPairs -> keyPairs)
                .setHomeDirectory(FS.DETECTED.userHome()) //设置用户主目录
                .setSshDirectory(new File(FS.DETECTED.userHome(), ".ssh")) //设置.ssh目录
                .build(null);
    }
}
