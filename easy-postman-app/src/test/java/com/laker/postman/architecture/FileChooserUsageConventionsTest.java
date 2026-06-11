package com.laker.postman.architecture;

import org.testng.annotations.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.testng.Assert.assertTrue;

public class FileChooserUsageConventionsTest {
    private static final Path ROOT = findProjectRoot();

    @Test
    public void productionCodeShouldUseSharedFileChooserUtility() throws Exception {
        List<String> violations = new ArrayList<>();
        for (Path sourceRoot : productionSourceRoots()) {
            if (!Files.exists(sourceRoot)) {
                continue;
            }
            try (Stream<Path> files = Files.walk(sourceRoot)) {
                files.filter(path -> path.toString().endsWith(".java"))
                        .filter(path -> !path.endsWith("FileChooserUtil.java"))
                        .forEach(path -> collectViolations(sourceRoot, path, violations));
            }
        }

        assertTrue(violations.isEmpty(),
                "Production code should use FileChooserUtil instead of raw JFileChooser/SystemFileChooser creation:\n"
                        + String.join("\n", violations));
    }

    private static List<Path> productionSourceRoots() {
        return List.of(
                Path.of("easy-postman-app/src/main/java"),
                Path.of("easy-postman-ui/src/main/java"),
                Path.of("easy-postman-plugins")
        ).stream().map(ROOT::resolve).toList();
    }

    private static void collectViolations(Path sourceRoot, Path path, List<String> violations) {
        String normalizedPath = path.toString().replace('\\', '/');
        if (!path.startsWith(sourceRoot) || !normalizedPath.contains("src/main/java/")) {
            return;
        }
        try {
            String source = Files.readString(path);
            if (source.contains("new JFileChooser")
                    || source.contains("import javax.swing.JFileChooser")
                    || source.contains("javax.swing.filechooser.FileNameExtensionFilter")
                    || source.contains("import javax.swing.filechooser.FileFilter")
                    || source.contains("new SystemFileChooser")) {
                violations.add(path.toString());
            }
        } catch (Exception e) {
            violations.add(path + " (failed to inspect: " + e.getMessage() + ")");
        }
    }

    private static Path findProjectRoot() {
        Path current = Path.of(System.getProperty("user.dir")).toAbsolutePath();
        while (current != null) {
            if (Files.isDirectory(current.resolve("easy-postman-app"))
                    && Files.isDirectory(current.resolve("easy-postman-ui"))) {
                return current;
            }
            current = current.getParent();
        }
        throw new IllegalStateException("Cannot find easy-postman project root");
    }
}
