package com.laker.postman.service.git;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.TransportConfigCallback;
import org.eclipse.jgit.transport.*;
import org.eclipse.jgit.transport.ssh.jsch.JschConfigSessionFactory;
import org.eclipse.jgit.transport.ssh.jsch.OpenSshConfig;
import org.eclipse.jgit.util.FS;

import java.io.File;

/**
 * SSH认证提供者
 * 使用JSch实现SSH密钥认证
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

    private SshSessionFactory createSshSessionFactory() {
        return new JschConfigSessionFactory() {
            @Override
            protected void configure(OpenSshConfig.Host host, Session session) {
                // 配置SSH会话
                session.setConfig("StrictHostKeyChecking", "no");
                session.setConfig("PreferredAuthentications", "publickey");
            }

            @Override
            protected JSch createDefaultJSch(FS fs) throws JSchException {
                JSch defaultJSch = super.createDefaultJSch(fs);

                try {
                    // 验证私钥文件是否存在
                    File keyFile = new File(privateKeyPath);
                    if (!keyFile.exists()) {
                        throw new JSchException("SSH private key file not found: " + privateKeyPath);
                    }

                    if (!keyFile.canRead()) {
                        throw new JSchException("Cannot read SSH private key file: " + privateKeyPath);
                    }

                    // 添加私钥
                    if (passphrase != null && !passphrase.trim().isEmpty()) {
                        defaultJSch.addIdentity(privateKeyPath, passphrase.getBytes());
                        log.debug("Added SSH identity with passphrase: {}", privateKeyPath);
                    } else {
                        defaultJSch.addIdentity(privateKeyPath);
                        log.debug("Added SSH identity without passphrase: {}", privateKeyPath);
                    }
                } catch (JSchException e) {
                    log.error("Failed to add SSH identity: {}", e.getMessage(), e);
                    throw e;
                }

                return defaultJSch;
            }
        };
    }

    /**
     * 验证SSH私钥文件
     */
    public static void validateSshKey(String privateKeyPath) throws IllegalArgumentException {
        if (privateKeyPath == null || privateKeyPath.trim().isEmpty()) {
            throw new IllegalArgumentException("SSH private key path is required");
        }

        File keyFile = new File(privateKeyPath);
        if (!keyFile.exists()) {
            throw new IllegalArgumentException("SSH private key file not found: " + privateKeyPath);
        }

        if (!keyFile.isFile()) {
            throw new IllegalArgumentException("SSH private key path is not a file: " + privateKeyPath);
        }

        if (!keyFile.canRead()) {
            throw new IllegalArgumentException("Cannot read SSH private key file: " + privateKeyPath);
        }

        // 简单的私钥文件内容验证
        try {
            String fileName = keyFile.getName().toLowerCase();
            // 常见的SSH私钥文件名
            if (!fileName.contains("id_") && !fileName.contains("ssh") && !fileName.endsWith(".pem")) {
                log.warn("SSH private key file name may not be standard: {}", privateKeyPath);
            }
        } catch (Exception e) {
            log.warn("Failed to validate SSH key file name: {}", e.getMessage());
        }
    }
}
