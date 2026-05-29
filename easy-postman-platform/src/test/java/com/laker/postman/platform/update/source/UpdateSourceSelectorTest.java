package com.laker.postman.platform.update.source;

import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

public class UpdateSourceSelectorTest {

    @Test
    public void shouldUseInjectedGithubPreferenceWithoutAppSettings() {
        UpdateSourceSelector selector = new UpdateSourceSelector(() -> "github");

        UpdateSource source = selector.selectBestSource();

        assertEquals(source.getName(), "GitHub");
    }

    @Test
    public void shouldUseInjectedGiteePreferenceWithoutAppSettings() {
        UpdateSourceSelector selector = new UpdateSourceSelector(() -> "gitee");

        UpdateSource source = selector.selectBestSource();

        assertEquals(source.getName(), "Gitee");
    }
}
