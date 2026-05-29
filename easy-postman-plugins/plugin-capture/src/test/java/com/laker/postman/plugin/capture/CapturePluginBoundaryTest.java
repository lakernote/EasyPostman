package com.laker.postman.plugin.capture;

import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.testng.Assert.assertFalse;

public class CapturePluginBoundaryTest {

    private static final List<String> FORBIDDEN_SOURCE_REFERENCES = List.of(
            "com.laker.postman.frame.",
            "com.laker.postman.panel."
    );

    @Test
    public void capturePluginMustNotDependOnHostAppArtifact() throws IOException {
        String pom = Files.readString(moduleDir().resolve("pom.xml"));

        assertFalse(pom.contains("<artifactId>easy-postman</artifactId>"),
                "plugin-capture must use plugin-api/easy-postman-ui APIs instead of depending on the host app artifact");
    }

    @Test
    public void capturePluginMustNotImportHostUiImplementationPackages() throws IOException {
        Path sourceRoot = moduleDir().resolve("src/main/java");
        try (Stream<Path> files = Files.walk(sourceRoot)) {
            List<String> violations = files
                    .filter(path -> path.toString().endsWith(".java"))
                    .flatMap(this::forbiddenReferences)
                    .toList();

            assertFalse(!violations.isEmpty(),
                    "plugin-capture has forbidden host UI references: " + violations);
        }
    }

    private Stream<String> forbiddenReferences(Path sourceFile) {
        try {
            String source = Files.readString(sourceFile);
            return FORBIDDEN_SOURCE_REFERENCES.stream()
                    .filter(source::contains)
                    .map(reference -> moduleDir().relativize(sourceFile) + " -> " + reference);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read " + sourceFile, e);
        }
    }

    private static Path moduleDir() {
        Path cwd = Path.of("").toAbsolutePath().normalize();
        if (Files.isDirectory(cwd.resolve("src/main/java/com/laker/postman/plugin/capture"))) {
            return cwd;
        }
        Path module = cwd.resolve("easy-postman-plugins/plugin-capture");
        if (Files.isDirectory(module.resolve("src/main/java/com/laker/postman/plugin/capture"))) {
            return module;
        }
        throw new IllegalStateException("Cannot locate plugin-capture module from " + cwd);
    }
}
