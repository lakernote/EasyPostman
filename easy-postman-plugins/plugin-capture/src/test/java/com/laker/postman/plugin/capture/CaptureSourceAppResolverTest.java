package com.laker.postman.plugin.capture;

import org.testng.annotations.Test;

import java.util.Optional;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class CaptureSourceAppResolverTest {

    @Test
    public void shouldParseMatchingLsofRecord() {
        CaptureSourceInfo sourceInfo = CaptureSourceInfo.network("127.0.0.1", 53421, "127.0.0.1", 8888);
        String output = """
                p999
                cEasyPostman
                n127.0.0.1:8888->127.0.0.1:53421
                p12345
                cCisco Agent
                n127.0.0.1:53421->127.0.0.1:8888
                """;

        Optional<CaptureSourceAppResolver.ProcessMatch> match =
                CaptureSourceAppResolver.parseLsofOutput(sourceInfo, output, 999);

        assertTrue(match.isPresent());
        assertEquals(match.get().processId(), "12345");
        assertEquals(match.get().processName(), "Cisco Agent");
    }

    @Test
    public void shouldParseMatchingWindowsNetstatRecord() {
        CaptureSourceInfo sourceInfo = CaptureSourceInfo.network("127.0.0.1", 53421, "127.0.0.1", 8888);
        String output = """
                  TCP    127.0.0.1:8888         127.0.0.1:53421        ESTABLISHED     999
                  TCP    127.0.0.1:53421        127.0.0.1:8888         ESTABLISHED     12345
                """;

        Optional<String> pid = CaptureSourceAppResolver.parseWindowsNetstatPid(sourceInfo, output, 999);

        assertTrue(pid.isPresent());
        assertEquals(pid.get(), "12345");
    }
}
