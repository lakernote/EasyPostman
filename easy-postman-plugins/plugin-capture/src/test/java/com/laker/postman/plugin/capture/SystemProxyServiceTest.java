package com.laker.postman.plugin.capture;

import com.laker.postman.plugin.api.PluginStorage;
import org.testng.annotations.Test;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

public class SystemProxyServiceTest {

    @Test
    public void shouldSkipWindowsRestoreWhenNoSnapshotWasCaptured() throws Exception {
        SystemProxyService service = new SystemProxyService();
        Method restoreWindowsSnapshot = SystemProxyService.class.getDeclaredMethod("restoreWindowsSnapshot");
        restoreWindowsSnapshot.setAccessible(true);

        restoreWindowsSnapshot.invoke(service);

        assertFalse(readBooleanField(service, "active"));
        assertNull(readObjectField(service, "windowsSnapshot"));
    }

    @Test
    public void shouldApplyWindowsProxyThroughWinInetBeforeRegistryFallback() throws Exception {
        List<List<String>> commands = new ArrayList<>();
        SystemProxyService service = new SystemProxyService(command -> {
            commands.add(command);
            if (command.size() > 2 && "reg".equals(command.get(0)) && "query".equals(command.get(1))) {
                return new SystemProxyService.CommandResult(1, List.of());
            }
            return new SystemProxyService.CommandResult(0, List.of());
        }, "Windows 11", false);

        service.enable("127.0.0.1", 8888);

        assertTrue(commands.stream().anyMatch(command -> command.stream()
                        .anyMatch(part -> part.contains("InternetSetOption")
                                && part.contains("INTERNET_OPTION_PER_CONNECTION_OPTION")
                                && part.contains("PROXY_TYPE_DIRECT | PROXY_TYPE_PROXY")
                                && part.contains("127.0.0.1:8888")
                                && part.contains("localhost;127.0.0.1;::1;<local>"))),
                "Windows proxy changes should be applied through WinINet per-connection options first");
        assertFalse(commands.stream().anyMatch(command -> command.equals(List.of(
                "reg", "add",
                "HKEY_CURRENT_USER\\Software\\Microsoft\\Windows\\CurrentVersion\\Internet Settings",
                "/v", "ProxyServer", "/t", "REG_SZ", "/d", "127.0.0.1:8888", "/f"
        ))), "Registry writes should be skipped when WinINet applies the proxy successfully");
    }

    @Test
    public void shouldNotFailProxyEnableWhenWinInetRefreshCommandIsUnavailable() throws Exception {
        List<List<String>> commands = new ArrayList<>();
        SystemProxyService service = new SystemProxyService(command -> {
            commands.add(command);
            if ("powershell".equals(command.get(0))) {
                throw new IOException("powershell missing");
            }
            if (command.size() > 2 && "reg".equals(command.get(0)) && "query".equals(command.get(1))) {
                return new SystemProxyService.CommandResult(1, List.of());
            }
            return new SystemProxyService.CommandResult(0, List.of());
        }, "Windows 11", false);

        service.enable("127.0.0.1", 8888);

        assertTrue(readBooleanField(service, "active"));
        assertTrue(commands.stream().anyMatch(command -> command.equals(List.of(
                "reg", "add",
                "HKEY_CURRENT_USER\\Software\\Microsoft\\Windows\\CurrentVersion\\Internet Settings",
                "/v", "ProxyServer", "/t", "REG_SZ", "/d", "127.0.0.1:8888", "/f"
        ))), "Registry fallback should apply ProxyServer when WinInet is unavailable");
        assertTrue(commands.stream().anyMatch(command -> command.equals(List.of(
                "cmd", "/c", "RUNDLL32.EXE", "USER32.DLL,UpdatePerUserSystemParameters", "1", "True"
        ))), "Legacy Windows settings refresh should still be attempted when WinINet notification fails");
    }

    @Test
    public void shouldRestoreWindowsProxyThroughWinInetBeforeRegistryFallback() throws Exception {
        List<List<String>> commands = new ArrayList<>();
        SystemProxyService service = new SystemProxyService(command -> {
            commands.add(command);
            if (command.size() > 2 && "reg".equals(command.get(0)) && "query".equals(command.get(1))) {
                return new SystemProxyService.CommandResult(1, List.of());
            }
            return new SystemProxyService.CommandResult(0, List.of());
        }, "Windows 11", false);

        service.enable("127.0.0.1", 8888);
        commands.clear();

        service.disable();

        assertTrue(commands.stream().anyMatch(command -> command.stream()
                        .anyMatch(part -> part.contains("InternetSetOption")
                                && part.contains("INTERNET_OPTION_PER_CONNECTION_OPTION")
                                && part.contains("$proxyFlags=1"))),
                "Windows proxy restore should use WinINet per-connection options first");
        assertFalse(commands.stream().anyMatch(command -> command.size() > 1
                        && "reg".equals(command.get(0))
                        && ("add".equals(command.get(1)) || "delete".equals(command.get(1)))),
                "Registry writes should be skipped when WinINet restores the proxy successfully");
    }

    @Test
    public void shouldRecoverPersistedWindowsProxySnapshotWhenCurrentProxyIsOwned() throws Exception {
        MemoryPluginStorage storage = new MemoryPluginStorage();
        SystemProxyService original = new SystemProxyService(command -> {
            if (command.size() > 2 && "reg".equals(command.get(0)) && "query".equals(command.get(1))) {
                return new SystemProxyService.CommandResult(1, List.of());
            }
            return new SystemProxyService.CommandResult(0, List.of());
        }, "Windows 11", false);
        original.configureStorage(storage);

        original.enable("127.0.0.1", 8888);

        assertTrue(storage.files.containsKey(SystemProxyService.RECOVERY_STORAGE_FILE));

        List<List<String>> recoveryCommands = new ArrayList<>();
        SystemProxyService recovered = new SystemProxyService(command -> {
            recoveryCommands.add(command);
            if (command.equals(List.of(
                    "reg",
                    "query",
                    "HKEY_CURRENT_USER\\Software\\Microsoft\\Windows\\CurrentVersion\\Internet Settings",
                    "/v",
                    "ProxyEnable"
            ))) {
                return new SystemProxyService.CommandResult(0, List.of("    ProxyEnable    REG_DWORD    0x1"));
            }
            if (command.equals(List.of(
                    "reg",
                    "query",
                    "HKEY_CURRENT_USER\\Software\\Microsoft\\Windows\\CurrentVersion\\Internet Settings",
                    "/v",
                    "ProxyServer"
            ))) {
                return new SystemProxyService.CommandResult(0, List.of("    ProxyServer    REG_SZ    127.0.0.1:8888"));
            }
            if (command.size() > 2 && "reg".equals(command.get(0)) && "query".equals(command.get(1))) {
                return new SystemProxyService.CommandResult(1, List.of());
            }
            return new SystemProxyService.CommandResult(0, List.of());
        }, "Windows 11", false);
        recovered.configureStorage(storage);

        SystemProxyService.SystemProxyRecoveryResult result = recovered.restorePersistedSnapshotIfOwned();

        assertTrue(result.attempted());
        assertTrue(result.restored());
        assertFalse(result.stale());
        assertFalse(storage.files.containsKey(SystemProxyService.RECOVERY_STORAGE_FILE));
        assertTrue(recoveryCommands.stream().anyMatch(command -> command.stream()
                        .anyMatch(part -> part.contains("InternetSetOption")
                                && part.contains("INTERNET_OPTION_PER_CONNECTION_OPTION")
                                && part.contains("$proxyFlags=1"))),
                "Persisted Windows proxy snapshot should be restored through WinINet");
    }

    @Test
    public void shouldDropPersistedWindowsProxySnapshotWhenCurrentProxyWasChangedByUser() throws Exception {
        MemoryPluginStorage storage = new MemoryPluginStorage();
        SystemProxyService original = new SystemProxyService(command -> {
            if (command.size() > 2 && "reg".equals(command.get(0)) && "query".equals(command.get(1))) {
                return new SystemProxyService.CommandResult(1, List.of());
            }
            return new SystemProxyService.CommandResult(0, List.of());
        }, "Windows 11", false);
        original.configureStorage(storage);
        original.enable("127.0.0.1", 8888);

        List<List<String>> recoveryCommands = new ArrayList<>();
        SystemProxyService recovered = new SystemProxyService(command -> {
            recoveryCommands.add(command);
            if (command.equals(List.of(
                    "reg",
                    "query",
                    "HKEY_CURRENT_USER\\Software\\Microsoft\\Windows\\CurrentVersion\\Internet Settings",
                    "/v",
                    "ProxyEnable"
            ))) {
                return new SystemProxyService.CommandResult(0, List.of("    ProxyEnable    REG_DWORD    0x1"));
            }
            if (command.equals(List.of(
                    "reg",
                    "query",
                    "HKEY_CURRENT_USER\\Software\\Microsoft\\Windows\\CurrentVersion\\Internet Settings",
                    "/v",
                    "ProxyServer"
            ))) {
                return new SystemProxyService.CommandResult(0, List.of("    ProxyServer    REG_SZ    127.0.0.1:9000"));
            }
            if (command.size() > 2 && "reg".equals(command.get(0)) && "query".equals(command.get(1))) {
                return new SystemProxyService.CommandResult(1, List.of());
            }
            return new SystemProxyService.CommandResult(0, List.of());
        }, "Windows 11", false);
        recovered.configureStorage(storage);

        SystemProxyService.SystemProxyRecoveryResult result = recovered.restorePersistedSnapshotIfOwned();

        assertTrue(result.attempted());
        assertFalse(result.restored());
        assertTrue(result.stale());
        assertFalse(storage.files.containsKey(SystemProxyService.RECOVERY_STORAGE_FILE));
        assertFalse(recoveryCommands.stream().anyMatch(command -> command.stream()
                        .anyMatch(part -> part.contains("INTERNET_OPTION_PER_CONNECTION_OPTION"))),
                "Recovery should not restore a snapshot after the user changes the system proxy");
    }

    private static boolean readBooleanField(Object target, String fieldName) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.getBoolean(target);
    }

    private static Object readObjectField(Object target, String fieldName) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(target);
    }

    private static final class MemoryPluginStorage implements PluginStorage {
        private final Map<String, String> files = new HashMap<>();

        @Override
        public Optional<String> readString(String relativePath) {
            return Optional.ofNullable(files.get(relativePath));
        }

        @Override
        public void writeString(String relativePath, String content) {
            files.put(relativePath, content);
        }

        @Override
        public void delete(String relativePath) {
            files.remove(relativePath);
        }

        @Override
        public Path dataDirectory() {
            return Path.of("");
        }
    }
}
