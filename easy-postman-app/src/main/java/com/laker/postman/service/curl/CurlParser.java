package com.laker.postman.service.curl;

import com.laker.postman.http.runtime.model.PreparedRequest;
import lombok.experimental.UtilityClass;

import java.util.ArrayList;
import java.util.List;

/**
 * Compatibility facade for cURL import/export.
 */
@UtilityClass
public class CurlParser {

    /**
     * 解析 cURL 命令字符串为 CurlRequest 对象。
     *
     * @param curl cURL 命令字符串
     * @return 解析后的 CurlRequest 对象
     */
    public static CurlRequest parse(String curl) {
        if (curl == null || curl.trim().isEmpty()) {
            CurlRequest req = new CurlRequest();
            req.method = "GET";
            return req;
        }

        List<String> commands = splitCommands(curl);
        String command = commands.isEmpty() ? curl : commands.get(0);
        ShellCommand shellCommand = CurlCommandTokenizer.parseCommand(command);
        CurlCommandOptions options = CurlOptionParser.parse(shellCommand.argv());
        options.warnings.addAll(0, shellCommand.warnings());
        return CurlRequestAssembler.assemble(options);
    }

    /**
     * 解析包含一条或多条 cURL 命令的文本。
     *
     * @param curlText cURL 命令文本
     * @return 逐条解析后的请求列表
     */
    public static List<CurlRequest> parseMany(String curlText) {
        List<CurlRequest> requests = new ArrayList<>();
        for (String command : splitCommands(curlText)) {
            requests.add(parse(command));
        }
        return requests;
    }

    /**
     * 将批量粘贴的 Bash cURL 文本拆成独立命令。
     *
     * @param curlText cURL 命令文本
     * @return 独立 cURL 命令列表
     */
    public static List<String> splitCommands(String curlText) {
        return CurlCommandSplitter.split(curlText);
    }

    /**
     * 将 PreparedRequest 转换为 cURL 命令字符串。
     */
    public static String toCurl(PreparedRequest preparedRequest) {
        return CurlRequestExporter.toCurl(preparedRequest);
    }

    /**
     * 将网络层实际发送的 PreparedRequest 快照转换为 cURL 命令字符串。
     */
    public static String toActualCurl(PreparedRequest preparedRequest) {
        return CurlRequestExporter.toActualCurl(preparedRequest);
    }

    /**
     * 判断 PreparedRequest 是否已有网络层实际发送快照。
     */
    public static boolean canExportActualCurl(PreparedRequest preparedRequest) {
        return CurlRequestExporter.hasSentSnapshot(preparedRequest);
    }
}
