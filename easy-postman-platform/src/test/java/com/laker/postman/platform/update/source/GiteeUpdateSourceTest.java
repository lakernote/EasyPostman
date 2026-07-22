package com.laker.postman.platform.update.source;

import org.testng.annotations.Test;

import static org.testng.Assert.assertTrue;

public class GiteeUpdateSourceTest {

    @Test
    public void shouldRequestReleasesNewestFirst() {
        String releasesApiUrl = new GiteeUpdateSource().getAllReleasesApiUrl();

        assertTrue(releasesApiUrl.contains("direction=desc"));
    }
}
