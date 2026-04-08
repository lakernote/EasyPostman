package com.laker.postman.service;

import com.laker.postman.model.Variable;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.testng.Assert.assertEquals;

public class GlobalVariablesServiceTest {

    private String originalGlobalDataFilePath;
    private Path tempGlobalFile;

    @BeforeMethod
    public void setUp() {
        originalGlobalDataFilePath = GlobalVariablesService.getInstance().getDataFilePath();
        try {
            tempGlobalFile = Files.createTempFile("easy-postman-global-service-test-", ".json");
            Files.writeString(tempGlobalFile, "{}");
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize temporary global variables file", e);
        }
        GlobalVariablesService.getInstance().setDataFilePath(tempGlobalFile.toString());
        GlobalVariablesService.getInstance().replaceGlobalVariables(List.of());
    }

    @AfterMethod
    public void tearDown() {
        try {
            GlobalVariablesService.getInstance().replaceGlobalVariables(List.of());
        } finally {
            if (originalGlobalDataFilePath != null && !originalGlobalDataFilePath.isBlank()) {
                GlobalVariablesService.getInstance().setDataFilePath(originalGlobalDataFilePath);
            }
            if (tempGlobalFile != null) {
                try {
                    Files.deleteIfExists(tempGlobalFile);
                } catch (Exception ignored) {
                    // ignore cleanup failures
                }
            }
        }
    }

    @Test
    public void shouldMergeUiEditsWithScriptChanges() {
        GlobalVariablesService service = GlobalVariablesService.getInstance();
        service.replaceGlobalVariables(List.of(
                new Variable(true, "sharedKey", "original"),
                new Variable(true, "uiStableKey", "stable")
        ));

        List<Variable> originalVariables = List.of(
                new Variable(true, "sharedKey", "original"),
                new Variable(true, "uiStableKey", "stable")
        );

        service.getGlobalVariables().set("sharedKey", "from-script");
        service.getGlobalVariables().set("scriptOnlyKey", "from-script");

        List<Variable> editedVariables = List.of(
                new Variable(true, "sharedKey", "original"),
                new Variable(true, "uiStableKey", "from-ui"),
                new Variable(true, "uiOnlyKey", "from-ui")
        );

        List<Variable> mergedVariables = service.mergeVariables(originalVariables, editedVariables);
        Map<String, Variable> mergedMap = toMap(mergedVariables);

        assertEquals(mergedMap.get("sharedKey").getValue(), "from-script");
        assertEquals(mergedMap.get("uiStableKey").getValue(), "from-ui");
        assertEquals(mergedMap.get("uiOnlyKey").getValue(), "from-ui");
        assertEquals(mergedMap.get("scriptOnlyKey").getValue(), "from-script");
    }

    private Map<String, Variable> toMap(List<Variable> variables) {
        Map<String, Variable> result = new LinkedHashMap<>();
        for (Variable variable : variables) {
            result.put(variable.getKey(), variable);
        }
        return result;
    }
}
