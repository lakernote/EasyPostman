package com.laker.postman.platform.instance;

import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

public class SingleInstanceCoordinatorTest {

    @Test
    public void shouldNotifyExistingOwnerAndAllowReplacementAfterClose() throws Exception {
        Path dataRoot = Files.createTempDirectory("easy-postman-instance-test-");
        CountDownLatch activationReceived = new CountDownLatch(1);
        SingleInstanceCoordinator.LaunchResult primary = null;
        SingleInstanceCoordinator.LaunchResult replacement = null;
        try {
            primary = SingleInstanceCoordinator.acquireOrNotify(dataRoot, activationReceived::countDown);
            assertEquals(primary.status(), SingleInstanceCoordinator.LaunchStatus.PRIMARY);
            assertNotNull(primary.coordinator());

            SingleInstanceCoordinator.LaunchResult secondary =
                    SingleInstanceCoordinator.acquireOrNotify(dataRoot, () -> {
                    });

            assertEquals(
                    secondary.status(),
                    SingleInstanceCoordinator.LaunchStatus.EXISTING_INSTANCE_NOTIFIED
            );
            assertTrue(activationReceived.await(2, TimeUnit.SECONDS));

            primary.coordinator().close();
            primary = null;

            replacement = SingleInstanceCoordinator.acquireOrNotify(dataRoot, () -> {
            });
            assertEquals(replacement.status(), SingleInstanceCoordinator.LaunchStatus.PRIMARY);
        } finally {
            close(primary);
            close(replacement);
            deleteRecursively(dataRoot);
        }
    }

    @Test
    public void shouldRecoverOwnershipAfterLockHolderIsForciblyTerminated() throws Exception {
        Path dataRoot = Files.createTempDirectory("easy-postman-instance-crash-test-");
        Path readyFile = dataRoot.resolve("holder-ready");
        Process process = startLockHolder(dataRoot, readyFile);
        SingleInstanceCoordinator.LaunchResult recovered = null;
        try {
            assertTrue(waitForFile(readyFile, process, 8, TimeUnit.SECONDS), readProcessOutput(process));
            process.destroyForcibly();
            assertTrue(process.waitFor(5, TimeUnit.SECONDS), "Lock-holder process did not terminate");

            recovered = SingleInstanceCoordinator.acquireOrNotify(dataRoot, () -> {
            });

            assertEquals(recovered.status(), SingleInstanceCoordinator.LaunchStatus.PRIMARY);
        } finally {
            if (process.isAlive()) {
                process.destroyForcibly();
                process.waitFor(5, TimeUnit.SECONDS);
            }
            close(recovered);
            deleteRecursively(dataRoot);
        }
    }

    private static Process startLockHolder(Path dataRoot, Path readyFile) throws IOException {
        String javaExecutableName = System.getProperty("os.name").startsWith("Windows") ? "java.exe" : "java";
        Path javaExecutable = Path.of(System.getProperty("java.home"), "bin", javaExecutableName);
        String classPath = System.getProperty(
                "surefire.test.class.path",
                System.getProperty("java.class.path")
        );
        return new ProcessBuilder(
                javaExecutable.toString(),
                "-cp",
                classPath,
                LockHolderProcess.class.getName(),
                dataRoot.toString(),
                readyFile.toString()
        ).redirectErrorStream(true).start();
    }

    private static boolean waitForFile(Path readyFile,
                                       Process process,
                                       long timeout,
                                       TimeUnit timeUnit) throws InterruptedException {
        long deadline = System.nanoTime() + timeUnit.toNanos(timeout);
        while (System.nanoTime() < deadline) {
            if (Files.isRegularFile(readyFile)) {
                return true;
            }
            if (!process.isAlive()) {
                return false;
            }
            Thread.sleep(25);
        }
        return Files.isRegularFile(readyFile);
    }

    private static String readProcessOutput(Process process) {
        if (process.isAlive()) {
            return "Lock-holder process did not become ready";
        }
        try {
            return new String(process.getInputStream().readAllBytes());
        } catch (IOException exception) {
            return "Unable to read lock-holder output: " + exception.getMessage();
        }
    }

    private static void close(SingleInstanceCoordinator.LaunchResult result) {
        if (result != null && result.coordinator() != null) {
            result.coordinator().close();
        }
    }

    private static void deleteRecursively(Path root) throws IOException {
        if (!Files.exists(root)) {
            return;
        }
        try (var paths = Files.walk(root)) {
            for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(path);
            }
        }
    }

    public static final class LockHolderProcess {
        private LockHolderProcess() {
        }

        public static void main(String[] args) throws Exception {
            Path dataRoot = Path.of(args[0]);
            Path readyFile = Path.of(args[1]);
            SingleInstanceCoordinator.LaunchResult result =
                    SingleInstanceCoordinator.acquireOrNotify(dataRoot, () -> {
                    });
            if (!result.isPrimary()) {
                throw new IllegalStateException("Failed to acquire primary instance lock");
            }
            Files.writeString(readyFile, "ready");
            new CountDownLatch(1).await();
        }
    }
}
