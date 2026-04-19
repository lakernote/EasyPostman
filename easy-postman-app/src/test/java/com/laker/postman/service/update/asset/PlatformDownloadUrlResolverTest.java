package com.laker.postman.service.update.asset;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.testng.Assert.assertEquals;

public class PlatformDownloadUrlResolverTest {

    @AfterMethod
    public void tearDown() {
        System.clearProperty("os.name");
        System.clearProperty("os.arch");
        System.clearProperty("easyPostman.update.osReleasePath");
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
    public void shouldPreferCompatibilityDebOnUosLikeArm64() throws IOException {
        useLinux("aarch64", "ID=uos\nID_LIKE=debian deepin\nNAME=\"UnionTech OS\"\n");

        String url = new PlatformDownloadUrlResolver().resolveDownloadUrl(sampleAssets());

        assertEquals(url, "https://example.com/EasyPostman-5.4.15-linux-arm64-compat.deb");
    }

    @Test
    public void shouldResolveRpmOnRpmBasedLinux() throws IOException {
        useLinux("x86_64", "ID=fedora\nID_LIKE=\"fedora rhel\"\n");

        String url = new PlatformDownloadUrlResolver().resolveDownloadUrl(sampleAssets());

        assertEquals(url, "https://example.com/EasyPostman-5.4.15-1.x86_64.rpm");
    }

    @Test
    public void shouldFallbackToLegacyArm64DebName() throws IOException {
        useLinux("aarch64", "ID=ubuntu\nID_LIKE=debian\n");

        JSONArray assets = new JSONArray()
                .put(asset("easypostman_5.4.15_arm64.deb"))
                .put(asset("EasyPostman-5.4.15-1.x86_64.rpm"));

        String url = new PlatformDownloadUrlResolver().resolveDownloadUrl(assets);

        assertEquals(url, "https://example.com/easypostman_5.4.15_arm64.deb");
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
                .put(asset("EasyPostman-5.4.15-linux-arm64-compat.deb"))
                .put(asset("EasyPostman-5.4.15-1.x86_64.rpm"))
                .put(asset("EasyPostman-5.4.15-1.aarch64.rpm"));
    }

    private static JSONObject asset(String name) {
        return new JSONObject()
                .set("name", name)
                .set("browser_download_url", "https://example.com/" + name);
    }
}
