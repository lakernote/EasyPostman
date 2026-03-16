package com.laker.postman.plugin.clientcert;

import cn.hutool.core.io.FileUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONUtil;
import com.laker.postman.common.constants.ConfigPathConstants;
import com.laker.postman.model.ClientCertificate;
import com.laker.postman.plugin.bridge.ClientCertificatePluginService;
import com.laker.postman.plugin.clientcert.internal.ClientCertificateLoader;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;
import lombok.extern.slf4j.Slf4j;

import javax.net.ssl.KeyManager;
import java.io.File;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

@Slf4j
public class ClientCertificatePluginServiceImpl implements ClientCertificatePluginService {

    private static final String CERT_CONFIG_FILE = ConfigPathConstants.CLIENT_CERTIFICATES;

    private final List<ClientCertificate> certificates = new CopyOnWriteArrayList<>();

    public ClientCertificatePluginServiceImpl() {
        load();
    }

    @Override
    public List<ClientCertificate> getAllCertificates() {
        return new ArrayList<>(certificates);
    }

    @Override
    public void addCertificate(ClientCertificate certificate) {
        String id = certificate.getId();
        if (id == null || id.isEmpty()) {
            certificate.setId(UUID.randomUUID().toString());
        }
        certificate.setCreatedAt(System.currentTimeMillis());
        certificate.setUpdatedAt(System.currentTimeMillis());
        certificates.add(certificate);
        save();
        log.info("Added client certificate: {} for host: {}", certificate.getId(), certificate.getHost());
    }

    @Override
    public void updateCertificate(ClientCertificate certificate) {
        for (int i = 0; i < certificates.size(); i++) {
            if (certificates.get(i).getId().equals(certificate.getId())) {
                certificate.setUpdatedAt(System.currentTimeMillis());
                certificates.set(i, certificate);
                save();
                log.info("Updated client certificate: {}", certificate.getId());
                return;
            }
        }
    }

    @Override
    public void deleteCertificate(String id) {
        certificates.removeIf(certificate -> certificate.getId().equals(id));
        save();
        log.info("Deleted client certificate: {}", id);
    }

    @Override
    public boolean validateCertificatePaths(ClientCertificate certificate) {
        if (certificate.getCertPath() == null || certificate.getCertPath().trim().isEmpty()) {
            String message = MessageFormat.format(
                    I18nUtil.getMessage(MessageKeys.CERT_CONSOLE_VALIDATION_FAILED),
                    certificate.getName() != null ? certificate.getName() : "Unknown"
            );
            logWarn(message);
            return false;
        }

        File certFile = new File(certificate.getCertPath());
        if (!certFile.exists() || !certFile.canRead()) {
            log.warn("Certificate file not found or not readable: {}", certificate.getCertPath());
            String message = MessageFormat.format(
                    I18nUtil.getMessage(MessageKeys.CERT_CONSOLE_FILE_NOT_FOUND),
                    certificate.getCertPath()
            );
            logError(message);
            return false;
        }

        if (ClientCertificate.CERT_TYPE_PEM.equals(certificate.getCertType())) {
            if (certificate.getKeyPath() == null || certificate.getKeyPath().trim().isEmpty()) {
                String message = MessageFormat.format(
                        I18nUtil.getMessage(MessageKeys.CERT_CONSOLE_VALIDATION_FAILED),
                        certificate.getName() != null ? certificate.getName() : "Unknown"
                );
                logWarn(message);
                return false;
            }
            File keyFile = new File(certificate.getKeyPath());
            if (!keyFile.exists() || !keyFile.canRead()) {
                log.warn("Private key file not found or not readable: {}", certificate.getKeyPath());
                String message = MessageFormat.format(
                        I18nUtil.getMessage(MessageKeys.CERT_CONSOLE_FILE_NOT_FOUND),
                        certificate.getKeyPath()
                );
                logError(message);
                return false;
            }
        }

        return true;
    }

    @Override
    public KeyManager[] loadClientCertificateKeyManagers(String host, int port) {
        if (host == null || host.isBlank()) {
            return new KeyManager[0];
        }

        ClientCertificate clientCertificate = findMatchingCertificate(host, port);
        if (clientCertificate == null || !validateCertificatePaths(clientCertificate)) {
            return new KeyManager[0];
        }

        try {
            KeyManager[] keyManagers = ClientCertificateLoader.createKeyManagers(clientCertificate);
            log.info("Using client certificate for host: {} ({})", host, clientCertificate.getName());

            String certificateName = clientCertificate.getName() != null && !clientCertificate.getName().isEmpty()
                    ? clientCertificate.getName()
                    : clientCertificate.getCertPath();
            String message = MessageFormat.format(
                    I18nUtil.getMessage(MessageKeys.CERT_CONSOLE_LOADED),
                    certificateName
            );
            logInfo(message);
            return keyManagers;
        } catch (Exception e) {
            log.error("Failed to load client certificate for host: {}", host, e);
            String certificateName = clientCertificate.getName() != null && !clientCertificate.getName().isEmpty()
                    ? clientCertificate.getName()
                    : clientCertificate.getCertPath();
            String message = MessageFormat.format(
                    I18nUtil.getMessage(MessageKeys.CERT_CONSOLE_LOAD_FAILED),
                    certificateName, e.getMessage()
            );
            logError(message);
            return new KeyManager[0];
        }
    }

    private void load() {
        File file = new File(CERT_CONFIG_FILE);
        if (!file.exists()) {
            log.info("Client certificate config file not found, creating new one");
            return;
        }

        try {
            String jsonContent = FileUtil.readUtf8String(file);
            JSONArray jsonArray = JSONUtil.parseArray(jsonContent);
            certificates.clear();
            for (int i = 0; i < jsonArray.size(); i++) {
                ClientCertificate certificate = jsonArray.getBean(i, ClientCertificate.class);
                certificates.add(certificate);
            }
            log.info("Loaded {} client certificate configurations", certificates.size());
        } catch (Exception e) {
            log.error("Failed to load client certificates", e);
        }
    }

    private void save() {
        try {
            File file = new File(CERT_CONFIG_FILE);
            File parent = file.getParentFile();
            if (parent != null && !parent.exists() && !parent.mkdirs()) {
                log.warn("Failed to create directory: {}", parent.getAbsolutePath());
            }
            String jsonContent = JSONUtil.toJsonPrettyStr(certificates);
            FileUtil.writeUtf8String(jsonContent, file);
            log.info("Saved {} client certificate configurations", certificates.size());
        } catch (Exception e) {
            log.error("Failed to save client certificates", e);
        }
    }

    private ClientCertificate findMatchingCertificate(String host, int port) {
        for (ClientCertificate certificate : certificates) {
            if (certificate.matches(host, port)) {
                log.debug("Found matching certificate for {}:{} - {}", host, port, certificate.getName());
                String certificateName = certificate.getName() != null && !certificate.getName().isEmpty()
                        ? certificate.getName()
                        : certificate.getCertPath();
                String message = MessageFormat.format(
                        I18nUtil.getMessage(MessageKeys.CERT_CONSOLE_MATCHED),
                        host, port, certificate.getCertType(), certificateName
                );
                logInfo(message);
                return certificate;
            }
        }
        return null;
    }

    private static void logInfo(String message) {
        log.info(message);
    }

    private static void logWarn(String message) {
        log.warn(message);
    }

    private static void logError(String message) {
        log.error(message);
    }
}
