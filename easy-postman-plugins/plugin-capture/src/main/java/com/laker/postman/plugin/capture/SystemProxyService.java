package com.laker.postman.plugin.capture;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class SystemProxyService {
    private static final Logger log = LoggerFactory.getLogger(SystemProxyService.class);
    private static final String NETWORKSETUP = "/usr/sbin/networksetup";
    private static final String REG = "reg";
    private static final String CMD = "cmd";
    private static final String POWERSHELL = "powershell";
    private static final String WINDOWS_PROXY_KEY = "HKEY_CURRENT_USER\\Software\\Microsoft\\Windows\\CurrentVersion\\Internet Settings";
    private static final List<String> REQUIRED_BYPASS_DOMAINS = List.of("localhost", "127.0.0.1", "::1");
    private static final String EMPTY = "Empty";
    private static final int WINDOWS_PROXY_TYPE_DIRECT = 1;
    private static final int WINDOWS_PROXY_TYPE_PROXY = 2;
    private static final int WINDOWS_PROXY_TYPE_AUTO_PROXY_URL = 4;
    private static final int WINDOWS_PROXY_TYPE_AUTO_DETECT = 8;
    private static final String WININET_REFRESH_SCRIPT = """
            $ErrorActionPreference='Stop';
            Add-Type -Namespace EasyPostman -Name WinInet -MemberDefinition '[System.Runtime.InteropServices.DllImport("wininet.dll", SetLastError=true)] public static extern bool InternetSetOption(System.IntPtr hInternet, int dwOption, System.IntPtr lpBuffer, int dwBufferLength);';
            [EasyPostman.WinInet]::InternetSetOption([System.IntPtr]::Zero, 39, [System.IntPtr]::Zero, 0) | Out-Null;
            [EasyPostman.WinInet]::InternetSetOption([System.IntPtr]::Zero, 95, [System.IntPtr]::Zero, 0) | Out-Null;
            [EasyPostman.WinInet]::InternetSetOption([System.IntPtr]::Zero, 37, [System.IntPtr]::Zero, 0) | Out-Null;
            """;
    private static final String WININET_SET_PROXY_SCRIPT = """
            Add-Type -Namespace EasyPostman -Name WinInetProxy -MemberDefinition @'
            [System.Runtime.InteropServices.StructLayout(System.Runtime.InteropServices.LayoutKind.Sequential)]
            public struct INTERNET_PER_CONN_OPTION_LIST
            {
                public int Size;
                public System.IntPtr Connection;
                public int OptionCount;
                public int OptionError;
                public System.IntPtr Options;
            }

            [System.Runtime.InteropServices.StructLayout(System.Runtime.InteropServices.LayoutKind.Sequential)]
            public struct INTERNET_PER_CONN_OPTION
            {
                public int Option;
                public System.IntPtr Value;
            }

            private const int INTERNET_OPTION_PER_CONNECTION_OPTION = 75;
            private const int INTERNET_OPTION_SETTINGS_CHANGED = 39;
            private const int INTERNET_OPTION_REFRESH = 37;
            private const int INTERNET_OPTION_PROXY_SETTINGS_CHANGED = 95;
            private const int INTERNET_PER_CONN_FLAGS = 1;
            private const int INTERNET_PER_CONN_PROXY_SERVER = 2;
            private const int INTERNET_PER_CONN_PROXY_BYPASS = 3;
            private const int INTERNET_PER_CONN_AUTOCONFIG_URL = 4;
            private const int PROXY_TYPE_DIRECT = 1;
            private const int PROXY_TYPE_PROXY = 2;
            private const int PROXY_TYPE_AUTO_PROXY_URL = 4;
            private const int PROXY_TYPE_AUTO_DETECT = 8;

            [System.Runtime.InteropServices.DllImport("wininet.dll", SetLastError=true, CharSet=System.Runtime.InteropServices.CharSet.Unicode)]
            private static extern bool InternetSetOption(System.IntPtr hInternet, int option, System.IntPtr buffer, int bufferLength);

            public static void SetProxy(string proxyServer, string proxyBypass)
            {
                SetInternetOptions(PROXY_TYPE_DIRECT | PROXY_TYPE_PROXY, proxyServer, proxyBypass, "");
            }

            public static void SetInternetOptions(int proxyFlags, string proxyServer, string proxyBypass, string autoConfigUrl)
            {
                INTERNET_PER_CONN_OPTION[] optionValues = new INTERNET_PER_CONN_OPTION[4];
                System.IntPtr proxyPtr = System.Runtime.InteropServices.Marshal.StringToHGlobalUni(proxyServer);
                System.IntPtr bypassPtr = System.Runtime.InteropServices.Marshal.StringToHGlobalUni(proxyBypass);
                System.IntPtr autoConfigPtr = System.Runtime.InteropServices.Marshal.StringToHGlobalUni(autoConfigUrl);
                System.IntPtr optionsPtr = System.IntPtr.Zero;
                System.IntPtr listPtr = System.IntPtr.Zero;
                try
                {
                    optionValues[0].Option = INTERNET_PER_CONN_FLAGS;
                    optionValues[0].Value = new System.IntPtr(proxyFlags);
                    optionValues[1].Option = INTERNET_PER_CONN_PROXY_SERVER;
                    optionValues[1].Value = proxyPtr;
                    optionValues[2].Option = INTERNET_PER_CONN_PROXY_BYPASS;
                    optionValues[2].Value = bypassPtr;
                    optionValues[3].Option = INTERNET_PER_CONN_AUTOCONFIG_URL;
                    optionValues[3].Value = autoConfigPtr;

                    int optionSize = System.Runtime.InteropServices.Marshal.SizeOf(typeof(INTERNET_PER_CONN_OPTION));
                    optionsPtr = System.Runtime.InteropServices.Marshal.AllocHGlobal(optionSize * optionValues.Length);
                    for (int i = 0; i < optionValues.Length; i++)
                    {
                        System.Runtime.InteropServices.Marshal.StructureToPtr(optionValues[i], System.IntPtr.Add(optionsPtr, i * optionSize), false);
                    }

                    INTERNET_PER_CONN_OPTION_LIST list = new INTERNET_PER_CONN_OPTION_LIST();
                    list.Size = System.Runtime.InteropServices.Marshal.SizeOf(typeof(INTERNET_PER_CONN_OPTION_LIST));
                    list.Connection = System.IntPtr.Zero;
                    list.OptionCount = optionValues.Length;
                    list.OptionError = 0;
                    list.Options = optionsPtr;

                    listPtr = System.Runtime.InteropServices.Marshal.AllocHGlobal(list.Size);
                    System.Runtime.InteropServices.Marshal.StructureToPtr(list, listPtr, false);
                    if (!InternetSetOption(System.IntPtr.Zero, INTERNET_OPTION_PER_CONNECTION_OPTION, listPtr, list.Size))
                    {
                        throw new System.ComponentModel.Win32Exception(System.Runtime.InteropServices.Marshal.GetLastWin32Error());
                    }
                    InternetSetOption(System.IntPtr.Zero, INTERNET_OPTION_SETTINGS_CHANGED, System.IntPtr.Zero, 0);
                    InternetSetOption(System.IntPtr.Zero, INTERNET_OPTION_PROXY_SETTINGS_CHANGED, System.IntPtr.Zero, 0);
                    InternetSetOption(System.IntPtr.Zero, INTERNET_OPTION_REFRESH, System.IntPtr.Zero, 0);
                }
                finally
                {
                    if (proxyPtr != System.IntPtr.Zero)
                    {
                        System.Runtime.InteropServices.Marshal.FreeHGlobal(proxyPtr);
                    }
                    if (bypassPtr != System.IntPtr.Zero)
                    {
                        System.Runtime.InteropServices.Marshal.FreeHGlobal(bypassPtr);
                    }
                    if (autoConfigPtr != System.IntPtr.Zero)
                    {
                        System.Runtime.InteropServices.Marshal.FreeHGlobal(autoConfigPtr);
                    }
                    if (optionsPtr != System.IntPtr.Zero)
                    {
                        System.Runtime.InteropServices.Marshal.FreeHGlobal(optionsPtr);
                    }
                    if (listPtr != System.IntPtr.Zero)
                    {
                        System.Runtime.InteropServices.Marshal.FreeHGlobal(listPtr);
                    }
                }
            }
            '@;
            [EasyPostman.WinInetProxy]::SetInternetOptions($proxyFlags, $proxyServer, $proxyBypass, $autoConfigUrl);
            """;

    private final Thread shutdownHook;
    private final CommandRunner commandRunner;
    private final String osName;

    private volatile Map<String, ProxyServiceSnapshot> snapshots = Map.of();
    private volatile WindowsProxySnapshot windowsSnapshot;
    private volatile boolean active;
    private volatile String activeHost = "";
    private volatile int activePort;

    SystemProxyService() {
        this(new ProcessCommandRunner(), System.getProperty("os.name", ""), true);
    }

    SystemProxyService(CommandRunner commandRunner, String osName, boolean registerShutdownHook) {
        this.commandRunner = commandRunner == null ? new ProcessCommandRunner() : commandRunner;
        this.osName = osName == null ? "" : osName;
        shutdownHook = new Thread(this::restoreQuietly, "capture-system-proxy-restore");
        if (registerShutdownHook) {
            Runtime.getRuntime().addShutdownHook(shutdownHook);
        }
    }

    boolean isSupported() {
        String normalizedOsName = osName.toLowerCase(Locale.ROOT);
        return normalizedOsName.contains("mac") || normalizedOsName.contains("win");
    }

    boolean isActive() {
        return active;
    }

    synchronized void enable(String host, int port) throws Exception {
        ensureSupported();
        if (isWindows()) {
            enableWindows(host, port);
            return;
        }
        String proxyHost = normalizeProxyHost(host);
        if (active) {
            if (proxyHost.equals(activeHost) && port == activePort) {
                return;
            }
            restoreSnapshots();
        }

        List<String> services = listEnabledNetworkServices();
        if (services.isEmpty()) {
            throw new IllegalStateException("No enabled macOS network services found");
        }

        Map<String, ProxyServiceSnapshot> captured = new LinkedHashMap<>();
        try {
            for (String service : services) {
                ProxyServiceSnapshot snapshot = readSnapshot(service);
                applyProxy(service, proxyHost, port, snapshot.bypassDomains());
                captured.put(service, snapshot);
            }
            snapshots = Map.copyOf(captured);
            activeHost = proxyHost;
            activePort = port;
            active = true;
        } catch (Exception ex) {
            snapshots = captured;
            restoreQuietly();
            throw ex;
        }
    }

    synchronized void disable() throws Exception {
        if (!active) {
            return;
        }
        if (isWindows()) {
            restoreWindowsSnapshot();
            return;
        }
        restoreSnapshots();
    }

    String statusSummary() {
        if (!isSupported()) {
            return "System proxy: unsupported";
        }
        if (!active) {
            return "System proxy: manual";
        }
        return "System proxy: synced to " + activeHost + ":" + activePort;
    }

    private void ensureSupported() {
        if (!isSupported()) {
            throw new IllegalStateException("System proxy sync is only supported on macOS and Windows");
        }
    }

    private synchronized void restoreSnapshots() throws Exception {
        Map<String, ProxyServiceSnapshot> currentSnapshots = snapshots;
        try {
            for (Map.Entry<String, ProxyServiceSnapshot> entry : currentSnapshots.entrySet()) {
                restoreSnapshot(entry.getKey(), entry.getValue());
            }
        } finally {
            active = false;
            activeHost = "";
            activePort = 0;
            snapshots = Map.of();
        }
    }

    private void restoreQuietly() {
        try {
            if (isWindows()) {
                restoreWindowsSnapshot();
            } else {
                restoreSnapshots();
            }
        } catch (Exception ex) {
            log.warn("Failed to restore system proxy settings", ex);
        }
    }

    private boolean isWindows() {
        return osName.toLowerCase(Locale.ROOT).contains("win");
    }

    private void enableWindows(String host, int port) throws Exception {
        String proxyHost = normalizeProxyHost(host);
        if (active) {
            if (proxyHost.equals(activeHost) && port == activePort) {
                return;
            }
            restoreWindowsSnapshot();
        }

        WindowsProxySnapshot snapshot = readWindowsSnapshot();
        try {
            applyWindowsProxy(proxyHost, port, snapshot.proxyOverrideData());
            windowsSnapshot = snapshot;
            activeHost = proxyHost;
            activePort = port;
            active = true;
        } catch (Exception ex) {
            windowsSnapshot = snapshot;
            restoreQuietly();
            throw ex;
        }
    }

    private void applyProxy(String service, String host, int port, List<String> originalBypassDomains) throws Exception {
        runCommand(NETWORKSETUP, "-setproxyautodiscovery", service, "off");
        runCommand(NETWORKSETUP, "-setautoproxystate", service, "off");
        runCommand(NETWORKSETUP, "-setwebproxy", service, host, String.valueOf(port));
        runCommand(NETWORKSETUP, "-setsecurewebproxy", service, host, String.valueOf(port));

        LinkedHashSet<String> bypassDomains = new LinkedHashSet<>(originalBypassDomains);
        bypassDomains.addAll(REQUIRED_BYPASS_DOMAINS);
        setBypassDomains(service, new ArrayList<>(bypassDomains));

        runCommand(NETWORKSETUP, "-setwebproxystate", service, "on");
        runCommand(NETWORKSETUP, "-setsecurewebproxystate", service, "on");
    }

    private void applyWindowsProxy(String host, int port, String originalBypass) throws Exception {
        try {
            applyWindowsProxyWithWinInet(host, port, originalBypass);
            return;
        } catch (Exception ex) {
            log.debug("Failed to apply Windows proxy through WinINet; falling back to registry", ex);
        }
        applyWindowsProxyWithRegistry(host, port, originalBypass);
    }

    private void applyWindowsProxyWithWinInet(String host, int port, String originalBypass) throws Exception {
        applyWindowsInternetOptionsWithWinInet(
                WINDOWS_PROXY_TYPE_DIRECT | WINDOWS_PROXY_TYPE_PROXY,
                host + ":" + port,
                mergeWindowsBypass(originalBypass),
                ""
        );
    }

    private void applyWindowsProxyWithRegistry(String host, int port, String originalBypass) throws Exception {
        writeWindowsRegistryValue("ProxyServer", "REG_SZ", host + ":" + port);
        writeWindowsRegistryValue("ProxyOverride", "REG_SZ", mergeWindowsBypass(originalBypass));
        writeWindowsRegistryValue("ProxyEnable", "REG_DWORD", "1");
        deleteWindowsRegistryValue("AutoConfigURL");
        writeWindowsRegistryValue("AutoDetect", "REG_DWORD", "0");
        refreshWindowsInternetOptions();
    }

    private void restoreSnapshot(String service, ProxyServiceSnapshot snapshot) throws Exception {
        restoreProxy(service, false, snapshot.webProxy());
        restoreProxy(service, true, snapshot.secureWebProxy());
        setBypassDomains(service, snapshot.bypassDomains());

        if (snapshot.autoProxyUrl() != null && !snapshot.autoProxyUrl().isBlank()) {
            runCommand(NETWORKSETUP, "-setautoproxyurl", service, snapshot.autoProxyUrl());
        }
        runCommand(NETWORKSETUP, "-setautoproxystate", service, snapshot.autoProxyEnabled() ? "on" : "off");
        runCommand(NETWORKSETUP, "-setproxyautodiscovery", service, snapshot.autoDiscoveryEnabled() ? "on" : "off");
    }

    private void restoreProxy(String service, boolean secure, ProxyEndpoint endpoint) throws Exception {
        if (endpoint.server() != null && !endpoint.server().isBlank() && endpoint.port() > 0) {
            runCommand(
                    NETWORKSETUP,
                    secure ? "-setsecurewebproxy" : "-setwebproxy",
                    service,
                    endpoint.server(),
                    String.valueOf(endpoint.port())
            );
        }
        runCommand(
                NETWORKSETUP,
                secure ? "-setsecurewebproxystate" : "-setwebproxystate",
                service,
                endpoint.enabled() ? "on" : "off"
        );
    }

    private ProxyServiceSnapshot readSnapshot(String service) throws Exception {
        ProxyEndpoint webProxy = readProxyEndpoint(service, false);
        ProxyEndpoint secureWebProxy = readProxyEndpoint(service, true);
        boolean autoDiscoveryEnabled = parseEnabled(readLines(NETWORKSETUP, "-getproxyautodiscovery", service));
        AutoProxyConfig autoProxyConfig = readAutoProxyConfig(service);
        List<String> bypassDomains = readBypassDomains(service);
        return new ProxyServiceSnapshot(
                webProxy,
                secureWebProxy,
                bypassDomains,
                autoDiscoveryEnabled,
                autoProxyConfig.enabled(),
                autoProxyConfig.url()
        );
    }

    private ProxyEndpoint readProxyEndpoint(String service, boolean secure) throws Exception {
        List<String> lines = readLines(NETWORKSETUP, secure ? "-getsecurewebproxy" : "-getwebproxy", service);
        Map<String, String> values = parseKeyValueLines(lines);
        boolean enabled = "yes".equalsIgnoreCase(values.getOrDefault("Enabled", "No"));
        String server = values.getOrDefault("Server", "");
        int port = parsePort(values.get("Port"));
        return new ProxyEndpoint(enabled, server, port);
    }

    private AutoProxyConfig readAutoProxyConfig(String service) throws Exception {
        Map<String, String> values = parseKeyValueLines(readLines(NETWORKSETUP, "-getautoproxyurl", service));
        boolean enabled = "yes".equalsIgnoreCase(values.getOrDefault("Enabled", "No"));
        return new AutoProxyConfig(enabled, values.getOrDefault("URL", ""));
    }

    private List<String> readBypassDomains(String service) throws Exception {
        List<String> lines = readLines(NETWORKSETUP, "-getproxybypassdomains", service);
        List<String> domains = new ArrayList<>();
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            if (trimmed.toLowerCase(Locale.ROOT).contains("there aren't any bypass domains")) {
                return List.of();
            }
            domains.add(trimmed);
        }
        return domains;
    }

    private List<String> listEnabledNetworkServices() throws Exception {
        List<String> lines = readLines(NETWORKSETUP, "-listallnetworkservices");
        List<String> services = new ArrayList<>();
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("An asterisk")) {
                continue;
            }
            if (trimmed.startsWith("*")) {
                continue;
            }
            services.add(trimmed);
        }
        return services;
    }

    private void setBypassDomains(String service, List<String> domains) throws Exception {
        List<String> command = new ArrayList<>();
        command.add(NETWORKSETUP);
        command.add("-setproxybypassdomains");
        command.add(service);
        if (domains == null || domains.isEmpty()) {
            command.add(EMPTY);
        } else {
            command.addAll(domains);
        }
        runCommand(command);
    }

    private boolean parseEnabled(List<String> lines) {
        Map<String, String> values = parseKeyValueLines(lines);
        return values.values().stream().findFirst()
                .map(value -> "yes".equalsIgnoreCase(value) || "on".equalsIgnoreCase(value))
                .orElse(false);
    }

    private Map<String, String> parseKeyValueLines(List<String> lines) {
        Map<String, String> values = new LinkedHashMap<>();
        for (String line : lines) {
            int separator = line.indexOf(':');
            if (separator <= 0) {
                continue;
            }
            String key = line.substring(0, separator).trim();
            String value = line.substring(separator + 1).trim();
            values.put(key, value);
        }
        return values;
    }

    private WindowsProxySnapshot readWindowsSnapshot() throws Exception {
        return new WindowsProxySnapshot(
                queryWindowsRegistryValue("ProxyEnable"),
                queryWindowsRegistryValue("ProxyServer"),
                queryWindowsRegistryValue("ProxyOverride"),
                queryWindowsRegistryValue("AutoConfigURL"),
                queryWindowsRegistryValue("AutoDetect")
        );
    }

    private synchronized void restoreWindowsSnapshot() throws Exception {
        WindowsProxySnapshot snapshot = windowsSnapshot;
        if (snapshot == null) {
            active = false;
            activeHost = "";
            activePort = 0;
            return;
        }
        try {
            try {
                restoreWindowsSnapshotWithWinInet(snapshot);
            } catch (Exception ex) {
                log.debug("Failed to restore Windows proxy through WinINet; falling back to registry", ex);
                restoreWindowsSnapshotWithRegistry(snapshot);
            }
        } finally {
            active = false;
            activeHost = "";
            activePort = 0;
            windowsSnapshot = null;
        }
    }

    private void restoreWindowsSnapshotWithWinInet(WindowsProxySnapshot snapshot) throws Exception {
        applyWindowsInternetOptionsWithWinInet(
                windowsProxyFlags(snapshot),
                registryData(snapshot.proxyServer()),
                registryData(snapshot.proxyOverride()),
                registryData(snapshot.autoConfigUrl())
        );
    }

    private void restoreWindowsSnapshotWithRegistry(WindowsProxySnapshot snapshot) throws Exception {
        restoreWindowsValue("ProxyEnable", snapshot.proxyEnable());
        restoreWindowsValue("ProxyServer", snapshot.proxyServer());
        restoreWindowsValue("ProxyOverride", snapshot.proxyOverride());
        restoreWindowsValue("AutoConfigURL", snapshot.autoConfigUrl());
        restoreWindowsValue("AutoDetect", snapshot.autoDetect());
        refreshWindowsInternetOptions();
    }

    private void restoreWindowsValue(String name, WindowsRegistryValue value) throws Exception {
        if (value == null || !value.present()) {
            deleteWindowsRegistryValue(name);
            return;
        }
        writeWindowsRegistryValue(name, value.type(), value.data());
    }

    private WindowsRegistryValue queryWindowsRegistryValue(String name) throws Exception {
        CommandResult result = runCommandAllowFailure(List.of(REG, "query", WINDOWS_PROXY_KEY, "/v", name));
        if (result.exitCode() != 0) {
            return new WindowsRegistryValue(false, "", "");
        }
        for (String line : result.lines()) {
            String trimmed = line.trim();
            if (!trimmed.startsWith(name)) {
                continue;
            }
            String remainder = trimmed.substring(name.length()).trim();
            String[] parts = remainder.split("\\s+", 2);
            if (parts.length == 2) {
                return new WindowsRegistryValue(true, parts[0], parts[1].trim());
            }
        }
        return new WindowsRegistryValue(false, "", "");
    }

    private void writeWindowsRegistryValue(String name, String type, String data) throws Exception {
        runCommand(REG, "add", WINDOWS_PROXY_KEY, "/v", name, "/t", type, "/d", data, "/f");
    }

    private void deleteWindowsRegistryValue(String name) throws Exception {
        runCommandAllowFailure(List.of(REG, "delete", WINDOWS_PROXY_KEY, "/v", name, "/f"));
    }

    private String mergeWindowsBypass(String originalBypass) {
        LinkedHashSet<String> values = new LinkedHashSet<>();
        if (originalBypass != null && !originalBypass.isBlank()) {
            for (String value : originalBypass.split(";")) {
                String trimmed = value.trim();
                if (!trimmed.isEmpty()) {
                    values.add(trimmed);
                }
            }
        }
        values.add("localhost");
        values.add("127.0.0.1");
        values.add("::1");
        values.add("<local>");
        return String.join(";", values);
    }

    private void applyWindowsInternetOptionsWithWinInet(
            int proxyFlags,
            String proxyServer,
            String proxyBypass,
            String autoConfigUrl
    ) throws Exception {
        String script = "$ErrorActionPreference='Stop';" + System.lineSeparator()
                + "$proxyFlags=" + proxyFlags + ";" + System.lineSeparator()
                + "$proxyServer=" + powerShellSingleQuoted(proxyServer) + ";" + System.lineSeparator()
                + "$proxyBypass=" + powerShellSingleQuoted(proxyBypass) + ";" + System.lineSeparator()
                + "$autoConfigUrl=" + powerShellSingleQuoted(autoConfigUrl) + ";" + System.lineSeparator()
                + WININET_SET_PROXY_SCRIPT;
        runCommand(List.of(
                POWERSHELL,
                "-NoProfile",
                "-NonInteractive",
                "-ExecutionPolicy",
                "Bypass",
                "-Command",
                script
        ));
    }

    private int windowsProxyFlags(WindowsProxySnapshot snapshot) {
        int flags = WINDOWS_PROXY_TYPE_DIRECT;
        if (isRegistryDwordEnabled(snapshot.proxyEnable()) && !registryData(snapshot.proxyServer()).isBlank()) {
            flags |= WINDOWS_PROXY_TYPE_PROXY;
        }
        if (!registryData(snapshot.autoConfigUrl()).isBlank()) {
            flags |= WINDOWS_PROXY_TYPE_AUTO_PROXY_URL;
        }
        if (isRegistryDwordEnabled(snapshot.autoDetect())) {
            flags |= WINDOWS_PROXY_TYPE_AUTO_DETECT;
        }
        return flags;
    }

    private boolean isRegistryDwordEnabled(WindowsRegistryValue value) {
        if (value == null || !value.present() || value.data() == null || value.data().isBlank()) {
            return false;
        }
        String data = value.data().trim();
        try {
            return Integer.decode(data) != 0;
        } catch (NumberFormatException ex) {
            return !"0".equals(data);
        }
    }

    private String registryData(WindowsRegistryValue value) {
        if (value == null || !value.present() || value.data() == null) {
            return "";
        }
        return value.data();
    }

    private String powerShellSingleQuoted(String value) {
        String safeValue = value == null ? "" : value;
        return "'" + safeValue.replace("'", "''") + "'";
    }

    private void refreshWindowsInternetOptions() throws Exception {
        try {
            runCommandAllowFailure(List.of(
                    POWERSHELL,
                    "-NoProfile",
                    "-NonInteractive",
                    "-ExecutionPolicy",
                    "Bypass",
                    "-Command",
                    WININET_REFRESH_SCRIPT
            ));
        } catch (Exception ex) {
            log.debug("Failed to notify WinINet proxy settings refresh", ex);
        }
        try {
            runCommandAllowFailure(List.of(CMD, "/c", "RUNDLL32.EXE", "USER32.DLL,UpdatePerUserSystemParameters", "1", "True"));
        } catch (Exception ex) {
            log.debug("Failed to run legacy Windows proxy settings refresh", ex);
        }
    }

    private int parsePort(String value) {
        if (value == null || value.isBlank()) {
            return 0;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    private List<String> readLines(String... command) throws Exception {
        return readLines(List.of(command));
    }

    private List<String> readLines(List<String> command) throws Exception {
        return runCommand(command).lines();
    }

    private CommandResult runCommand(String... command) throws Exception {
        return runCommand(List.of(command));
    }

    private CommandResult runCommand(List<String> command) throws Exception {
        CommandResult result = runCommandAllowFailure(command);
        if (result.exitCode() != 0) {
            throw new IllegalStateException(String.join(System.lineSeparator(), result.lines()));
        }
        return result;
    }

    private CommandResult runCommandAllowFailure(List<String> command) throws Exception {
        return commandRunner.run(command);
    }

    private String normalizeProxyHost(String host) {
        if (host == null || host.isBlank()) {
            return "127.0.0.1";
        }
        String trimmed = host.trim();
        if ("0.0.0.0".equals(trimmed) || "::".equals(trimmed) || "::0".equals(trimmed)) {
            return "127.0.0.1";
        }
        return trimmed;
    }

    @FunctionalInterface
    interface CommandRunner {
        CommandResult run(List<String> command) throws Exception;
    }

    record CommandResult(int exitCode, List<String> lines) {
    }

    private static final class ProcessCommandRunner implements CommandRunner {
        @Override
        public CommandResult run(List<String> command) throws Exception {
            ProcessBuilder processBuilder = new ProcessBuilder(command);
            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();
            List<String> lines = new ArrayList<>();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), Charset.defaultCharset()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    lines.add(line);
                }
            }
            int exitCode = process.waitFor();
            return new CommandResult(exitCode, lines);
        }
    }

    private record ProxyServiceSnapshot(
            ProxyEndpoint webProxy,
            ProxyEndpoint secureWebProxy,
            List<String> bypassDomains,
            boolean autoDiscoveryEnabled,
            boolean autoProxyEnabled,
            String autoProxyUrl
    ) {
    }

    private record ProxyEndpoint(boolean enabled, String server, int port) {
    }

    private record AutoProxyConfig(boolean enabled, String url) {
    }

    private record WindowsRegistryValue(boolean present, String type, String data) {
    }

    private record WindowsProxySnapshot(
            WindowsRegistryValue proxyEnable,
            WindowsRegistryValue proxyServer,
            WindowsRegistryValue proxyOverride,
            WindowsRegistryValue autoConfigUrl,
            WindowsRegistryValue autoDetect
    ) {
        String proxyOverrideData() {
            return proxyOverride.present() ? proxyOverride.data() : "";
        }
    }
}
