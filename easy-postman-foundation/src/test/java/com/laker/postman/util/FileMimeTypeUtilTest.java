package com.laker.postman.util;

import org.testng.annotations.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.testng.Assert.assertEquals;

public class FileMimeTypeUtilTest {

    @Test
    public void shouldDetectCommonMimeTypeFromFileName() throws Exception {
        Path file = Files.createTempFile("easy-postman-mime-", ".png");
        Files.write(file, new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47});

        assertEquals(FileMimeTypeUtil.detectMimeType(file.toFile()), "image/png");
    }

    @Test
    public void shouldFallbackToOctetStreamForMissingFile() {
        assertEquals(FileMimeTypeUtil.detectMimeType(Path.of("/tmp/easy-postman-missing-file.unknown").toFile()),
                FileMimeTypeUtil.DEFAULT_MIME_TYPE);
    }

    @Test
    public void shouldIdentifyReadableRegularFilePath() throws Exception {
        Path file = Files.createTempFile("easy-postman-mime-regular-", ".txt");
        Files.writeString(file, "hello");

        assertEquals(FileMimeTypeUtil.isReadableRegularFile(file.toString()), true);
        assertEquals(FileMimeTypeUtil.isReadableRegularFile(file.resolve("missing").toString()), false);
        assertEquals(FileMimeTypeUtil.isReadableRegularFile("  "), false);
    }
}
