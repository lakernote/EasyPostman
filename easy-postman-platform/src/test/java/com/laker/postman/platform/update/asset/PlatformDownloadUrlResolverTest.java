package com.laker.postman.platform.update.asset;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.testng.Assert.assertEquals;

public class PlatformDownloadUrlResolverTest {
    private final String originalOsName = System.getProperty("os.name");
    private final String originalOsArch = System.getProperty("os.arch");
    private final String originalOsReleasePath = System.getProperty("easyPostman.update.osReleasePath");

    @AfterMethod
    public void tearDown() {
        restoreProperty("os.name", originalOsName);
        restoreProperty("os.arch", originalOsArch);
        restoreProperty("easyPostman.update.osReleasePath", originalOsReleasePath);
    }

    @Test
    public void shouldResolveLinuxAmd64DebByStableReleaseName() throws IOException {
        useLinux("amd64", "ID=ubuntu\nID_LIKE=debian\n");

        String url = new PlatformDownloadUrlResolver().resolveDownloadUrl(sampleAssets());

        assertEquals(url, "https://example.com/EasyPostman-5.4.15-linux-amd64.deb");
    }

    @Test
    public void shouldResolveLinuxArm64DebByStableReleaseName() throws IOException {
        useLinux("aarch64", "ID=ubuntu\nID_LIKE=debian\n");

        String url = new PlatformDownloadUrlResolver().resolveDownloadUrl(sampleAssets());

        assertEquals(url, "https://example.com/EasyPostman-5.4.15-linux-arm64.deb");
    }

    @Test
    public void shouldResolveRpmOnRpmBasedLinux() throws IOException {
        useLinux("x86_64", "ID=fedora\nID_LIKE=\"fedora rhel\"\n");

        String url = new PlatformDownloadUrlResolver().resolveDownloadUrl(sampleAssets());

        assertEquals(url, "https://example.com/EasyPostman-5.4.15-1.x86_64.rpm");
    }

    private static void useLinux(String arch, String osReleaseContent) throws IOException {
        System.setProperty("os.name", "Linux");
        System.setProperty("os.arch", arch);

        Path osRelease = Files.createTempFile("os-release", ".txt");
        Files.writeString(osRelease, osReleaseContent);
        System.setProperty("easyPostman.update.osReleasePath", osRelease.toString());
    }

    private static JSONArray sampleAssets() {
        return new JSONArray()
                .put(asset("EasyPostman-5.4.15-linux-amd64.deb"))
                .put(asset("EasyPostman-5.4.15-linux-arm64.deb"))
                .put(asset("EasyPostman-5.4.15-1.x86_64.rpm"))
                .put(asset("EasyPostman-5.4.15-1.aarch64.rpm"));
    }

    private static JSONObject asset(String name) {
        return new JSONObject()
                .set("name", name)
                .set("browser_download_url", "https://example.com/" + name);
    }

    private static void restoreProperty(String key, String value) {
        if (value == null) {
            System.clearProperty(key);
            return;
        }
        System.setProperty(key, value);
    }
}
