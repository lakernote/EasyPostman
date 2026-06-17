package com.laker.postman.panel.workspace;

import org.testng.annotations.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class SharedTextInputDialogUsageTest {
    private static final Path ROOT = projectRoot();

    @Test
    public void workspaceRenameShouldUseSharedTextInputDialog() throws IOException {
        String source = Files.readString(ROOT.resolve(
                "easy-postman-app/src/main/java/com/laker/postman/panel/workspace/WorkspacePanel.java"));

        assertFalse(source.contains("JOptionPane.showInputDialog("),
                "Workspace rename should use the shared input dialog instead of JOptionPane.showInputDialog");
        assertTrue(source.contains("TextInputDialog.showRequiredName("),
                "Workspace rename should route through TextInputDialog's shared name label");
        assertFalse(Files.exists(ROOT.resolve(
                        "easy-postman-app/src/main/java/com/laker/postman/panel/workspace/components/WorkspaceNameInputDialog.java")),
                "Workspace should not keep a private input dialog when a shared dialog exists");
    }

    @Test
    public void performanceRenameActionsShouldUseSharedTextInputDialog() throws IOException {
        String panelSource = Files.readString(ROOT.resolve(
                "easy-postman-app/src/main/java/com/laker/postman/panel/performance/PerformancePanel.java"));
        String commandSource = Files.readString(ROOT.resolve(
                "easy-postman-app/src/main/java/com/laker/postman/panel/performance/PerformanceTreeNodeCommandSupport.java"));

        assertFalse(panelSource.contains("JOptionPane.showInputDialog("),
                "Performance plan rename should use the shared TextInputDialog");
        assertFalse(commandSource.contains("JOptionPane.showInputDialog("),
                "Performance tree node rename should use the shared TextInputDialog");
        assertTrue(panelSource.contains("TextInputDialog.showRequiredName("),
                "Performance plan rename should route through TextInputDialog's shared name label");
        assertTrue(commandSource.contains("TextInputDialog.showRequiredName("),
                "Performance tree node rename should route through TextInputDialog's shared name label");
    }

    @Test
    public void sourceCodeShouldUseSharedTextInputDialogForSingleLineInputs() throws IOException {
        List<String> violations = new ArrayList<>();
        List<Path> sourceRoots = new ArrayList<>();
        sourceRoots.add(ROOT.resolve("easy-postman-app/src/main/java"));
        sourceRoots.addAll(pluginSourceRoots());
        for (Path sourceRoot : sourceRoots) {
            try (Stream<Path> paths = Files.walk(sourceRoot)) {
                paths.filter(path -> path.toString().endsWith(".java"))
                        .forEach(path -> collectShowInputDialogViolation(sourceRoot, path, violations));
            }
        }

        assertTrue(violations.isEmpty(),
                "Use TextInputDialog instead of JOptionPane.showInputDialog for single-line inputs: " + violations);
    }

    private static List<Path> pluginSourceRoots() throws IOException {
        List<Path> roots = new ArrayList<>();
        Path pluginsRoot = ROOT.resolve("easy-postman-plugins");
        try (Stream<Path> paths = Files.list(pluginsRoot)) {
            paths.filter(Files::isDirectory)
                    .map(path -> path.resolve("src/main/java"))
                    .filter(Files::exists)
                    .forEach(roots::add);
        }
        return roots;
    }

    private static void collectShowInputDialogViolation(Path sourceRoot, Path path, List<String> violations) {
        try {
            String source = Files.readString(path);
            if (source.contains("JOptionPane.showInputDialog(")) {
                violations.add(sourceRoot.relativize(path).toString());
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read " + path, e);
        }
    }

    private static Path projectRoot() {
        Path current = Path.of("").toAbsolutePath();
        while (current != null && !Files.exists(current.resolve("pom.xml"))) {
            current = current.getParent();
        }
        if (current == null) {
            throw new IllegalStateException("Unable to locate project root");
        }
        if (Files.exists(current.resolve("easy-postman-app/pom.xml"))) {
            return current;
        }
        Path parent = current.getParent();
        if (parent != null && Files.exists(parent.resolve("easy-postman-app/pom.xml"))) {
            return parent;
        }
        return current;
    }
}
