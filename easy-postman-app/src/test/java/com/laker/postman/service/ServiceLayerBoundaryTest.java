package com.laker.postman.service;

import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.testng.Assert.assertFalse;

public class ServiceLayerBoundaryTest {

    @Test
    public void serviceLayerShouldNotImportRequestEditorPanelsForConstants() throws IOException {
        Path serviceDir = moduleDir().resolve("src/main/java/com/laker/postman/service");
        try (var paths = Files.walk(serviceDir)) {
            for (Path sourceFile : paths.filter(path -> path.toString().endsWith(".java")).toList()) {
                String source = Files.readString(sourceFile);
                assertFalse(source.contains("panel.collections.editor.request.sub.AuthTabPanel"),
                        sourceFile + " must use domain auth constants, not Swing panel constants");
                assertFalse(source.contains("panel.collections.editor.request.sub.RequestBodyPanel"),
                        sourceFile + " must use domain body constants, not Swing panel constants");
                assertFalse(source.contains("panel.functional.table.RunnerRowData"),
                        sourceFile + " must use functional runner model data, not Swing table package data");
            }
        }
    }

    @Test
    public void exitUiCoordinationShouldNotLiveInServicePackage() {
        assertFalse(Files.exists(moduleDir().resolve("src/main/java/com/laker/postman/service/ExitService.java")),
                "exit UI coordination should live in panel/lifecycle layer");
    }

    @Test
    public void workspaceTransferUiCoordinationShouldNotLiveInServicePackage() {
        assertFalse(Files.exists(moduleDir().resolve(
                        "src/main/java/com/laker/postman/service/workspace/WorkspaceTransferHelper.java")),
                "workspace transfer UI coordination should live in panel/workspace layer");
    }

    @Test
    public void httpExecutionLayerShouldNotWriteNetworkLogsThroughRequestEditorUi() throws IOException {
        for (String relativePath : new String[]{
                "src/main/java/com/laker/postman/http/runtime/redirect/HttpRedirectExecutor.java",
                "src/main/java/com/laker/postman/http/runtime/okhttp/OkHttpExchangeEventListener.java"
        }) {
            Path sourceFile = moduleDir().resolve(relativePath);
            String source = Files.readString(sourceFile);
            assertFalse(source.contains("com.laker.postman.common.UiSingletonFactory"),
                    sourceFile + " must publish network log events instead of looking up UI singletons");
            assertFalse(source.contains("com.laker.postman.panel.collections.editor.RequestEditorPanel"),
                    sourceFile + " must not depend on the request editor UI");
            assertFalse(source.contains("com.laker.postman.panel.collections.editor.request.sub.NetworkLogPanel"),
                    sourceFile + " must not depend on the Swing network log panel");
            assertFalse(source.contains("com.laker.postman.panel.collections.editor.request.sub.NetworkLogStage"),
                    sourceFile + " must not use UI log stage styling from the service layer");
        }
    }

    private Path moduleDir() {
        Path moduleDir = Path.of(System.getProperty("user.dir"));
        if (!Files.exists(moduleDir.resolve("src/main/java"))) {
            moduleDir = moduleDir.resolve("easy-postman-app");
        }
        return moduleDir;
    }
}
