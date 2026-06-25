package com.laker.postman.plugin.capture;

import org.testng.annotations.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.testng.Assert.assertFalse;

public class CaptureLoggingPolicyTest {

    @Test
    public void highVolumeTlsPathsShouldNotLogAtInfo() throws Exception {
        assertFalse(source("HttpsMitmFrontendHandler").contains("log.info"),
                "HTTPS request handling is high-volume and should use debug for success logs");
    }

    @Test
    public void perHostCertificatePathsShouldNotLogAtInfo() throws Exception {
        String source = source("CaptureCertificateService");

        assertFalse(source.contains("log.info(\"Building MITM SSL context"),
                "MITM SSL context construction runs per CONNECT and should not be info");
        assertFalse(source.contains("log.info(\"Issued MITM leaf certificate"),
                "Leaf certificate issuance can happen for many hosts and should not be info");
    }

    private String source(String className) throws Exception {
        Path path = Path.of("src/main/java/com/laker/postman/plugin/capture/" + className + ".java");
        return Files.readString(path);
    }
}
