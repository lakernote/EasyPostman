package com.laker.postman.util;

import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

public class FileSizeDisplayUtilTest {

    @Test
    public void shouldFormatByteAndLargerUnits() {
        assertEquals(FileSizeDisplayUtil.formatSize(512), "512 B");
        assertEquals(FileSizeDisplayUtil.formatSize(1024), "1.00 KB");
        assertEquals(FileSizeDisplayUtil.formatSize(1024 * 1024), "1.00 MB");
    }

    @Test
    public void shouldFormatDownloadProgressWithOrWithoutKnownTotal() {
        assertEquals(FileSizeDisplayUtil.formatDownloadSize(1024, 2048), "Downloaded: 1.00 KB / 2.00 KB");
        assertEquals(FileSizeDisplayUtil.formatDownloadSize(1024, -1), "Downloaded: 1.00 KB");
    }
}
