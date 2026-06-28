package com.laker.postman.plugin.capture;

import org.testng.annotations.Test;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

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
}
