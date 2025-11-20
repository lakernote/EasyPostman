package com.laker.postman.service.git;

import cn.hutool.core.io.FileUtil;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.sshd.common.util.security.SecurityUtils;
import org.eclipse.jgit.api.TransportConfigCallback;
import org.eclipse.jgit.transport.SshSessionFactory;
import org.eclipse.jgit.transport.SshTransport;
import org.eclipse.jgit.transport.Transport;
import org.eclipse.jgit.transport.sshd.SshdSessionFactory;
import org.eclipse.jgit.transport.sshd.SshdSessionFactoryBuilder;
import org.eclipse.jgit.util.FS;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SSH认证提供者
 * 使用 Apache MINA SSHD 实现SSH密钥认证
 */
@Slf4j
public class SshCredentialsProvider implements TransportConfigCallback {

    private final String privateKeyPath;
    private final String passphrase;

    // 缓存SSH会话工厂，避免每次都创建新的实例
    // Key: privateKeyPath, Value: SshSessionFactory
    private static final Map<String, SshSessionFactory> SESSION_FACTORY_CACHE = new ConcurrentHashMap<>();

    // 用于在应用关闭时清理资源
    static {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("Cleaning up SSH session factories...");
            SESSION_FACTORY_CACHE.values().forEach(factory -> {
                if (factory instanceof SshdSessionFactory sshdFactory) {
                    try {
                        sshdFactory.close();
                    } catch (Exception e) {
                        log.debug("Error closing SSH session factory during shutdown", e);
                    }
                }
            });
            SESSION_FACTORY_CACHE.clear();
        }, "SSH-Cleanup-Thread"));
    }

    public SshCredentialsProvider(String privateKeyPath, String passphrase) {
        this.privateKeyPath = privateKeyPath;
        this.passphrase = passphrase;
    }

    @Override
    public void configure(Transport transport) {
        if (transport instanceof SshTransport sshTransport) {
            sshTransport.setSshSessionFactory(getOrCreateSshSessionFactory());
        }
    }

    /**
     * 获取或创建SSH会话工厂（带缓存）
     * 这样可以避免每次Git操作都创建新的executor线程池
     */
    @SneakyThrows
    private SshSessionFactory getOrCreateSshSessionFactory() {
        return SESSION_FACTORY_CACHE.computeIfAbsent(privateKeyPath, key -> {
            try {
                String privateKeyContent = FileUtil.readString(new File(privateKeyPath), StandardCharsets.UTF_8);
                Iterable<KeyPair> keyPairs = SecurityUtils.loadKeyPairIdentities(null,
                        null,
                        new ByteArrayInputStream(privateKeyContent.getBytes()),
                        (session, resourceKey, retryIndex) -> passphrase);

                return new SshdSessionFactoryBuilder()
//                      .setPreferredAuthentications("publickey") // 可选 使用公钥认证
                        .setDefaultKeysProvider(ignoredSshDirBecauseWeUseAnInMemorySetOfKeyPairs -> keyPairs) // 设置密钥提供者
                        .setHomeDirectory(FS.DETECTED.userHome()) //设置用户主目录 影响.ssh/known_hosts 等
                        .setSshDirectory(new File(FS.DETECTED.userHome(), ".ssh")) //设置.ssh目录
                        .build(null);
            } catch (Exception e) {
                log.error("Failed to create SSH session factory for key: {}", privateKeyPath, e);
                throw new RuntimeException("Failed to create SSH session factory", e);
            }
        });
    }

    /**
     * 清除指定私钥的缓存
     * 当用户更换SSH密钥时可调用此方法
     */
    public static void clearCache(String privateKeyPath) {
        SshSessionFactory factory = SESSION_FACTORY_CACHE.remove(privateKeyPath);
        if (factory instanceof SshdSessionFactory sshdFactory) {
            try {
                sshdFactory.close();
            } catch (Exception e) {
                log.debug("Error closing SSH session factory", e);
            }
        }
    }
}
