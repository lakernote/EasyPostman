package com.laker.postman.service.variable;

import com.laker.postman.util.I18nUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 内置函数提供者
 * <p>
 * 负责处理所有内置动态函数，如 $guid, $timestamp, $randomInt 等
 * <ul>
 *   <li>优先级最低</li>
 *   <li>每次调用都会生成新的随机值</li>
 * </ul>
 */
@Slf4j
public class BuiltInFunctionService implements VariableProvider {

    /**
     * 单例实例
     */
    private static final BuiltInFunctionService INSTANCE = new BuiltInFunctionService();

    private static final Random RANDOM = new Random();

    /**
     * 所有支持的内置函数列表
     */
    private static final String[] BUILT_IN_FUNCTIONS = {
            "$guid", "$uuid", "$randomUUID",
            "$timestamp", "$isoTimestamp",
            "$randomInt", "$randomAlphaNumeric", "$randomBoolean", "$randomTF",
            "$randomIP", "$randomEmail",
            "$randomFullName", "$randomFirstName", "$randomLastName",
            "$randomColor", "$randomDate", "$randomTime",
            "$randomUrl", "$randomPhoneNumber",
            "$randomCity", "$randomCountry", "$randomUserAgent",
            "$randomMD5", "$randomBase64"
    };

    private static final Set<String> SIMPLE_FUNCTIONS = Set.of(
            "__uuid",
            "__timestamp",
            "__time",
            "__randomInt",
            "__randomString",
            "__urlEncode",
            "__base64"
    );

    private static final int MAX_RANDOM_STRING_LENGTH = 1024;

    /**
     * 私有构造函数，防止外部实例化
     */
    private BuiltInFunctionService() {
    }

    /**
     * 获取单例实例
     */
    public static BuiltInFunctionService getInstance() {
        return INSTANCE;
    }

    @Override
    public String get(String key) {
        return generate(key);
    }

    @Override
    public boolean has(String key) {
        return isBuiltInFunction(key);
    }

    @Override
    public Map<String, String> getAll() {
        return getAllFunctionsWithDescriptions();
    }

    @Override
    public int getPriority() {
        return VariableType.BUILT_IN.getPriority();
    }

    @Override
    public VariableType getType() {
        return VariableType.BUILT_IN;
    }

    // ==================== 内置函数特有方法 ====================

    // ==================== 内置函数特有方法 ====================

    /**
     * 判断是否是内置函数
     *
     * @param name 函数名
     * @return 是否是内置函数
     */
    public boolean isBuiltInFunction(String name) {
        if (name == null) {
            return false;
        }
        SimpleFunctionCall simpleFunctionCall = parseSimpleFunctionCall(name);
        if (simpleFunctionCall != null) {
            return true;
        }
        for (String func : BUILT_IN_FUNCTIONS) {
            if (func.equals(name)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 生成内置函数的值
     *
     * @param funcName 函数名
     * @return 生成的值，如果函数不存在则返回 null
     */
    public String generate(String funcName) {
        if (funcName == null) {
            return null;
        }

        SimpleFunctionCall simpleFunctionCall = parseSimpleFunctionCall(funcName);
        if (simpleFunctionCall != null) {
            String result = generateSimpleFunction(simpleFunctionCall);
            log.debug("Generated value for {}: {}", funcName, result);
            return result;
        }

        String result;
        switch (funcName) {
            case "$guid":
            case "$uuid":
            case "$randomUUID":
                result = UUID.randomUUID().toString();
                break;

            case "$timestamp":
                result = String.valueOf(System.currentTimeMillis() / 1000);
                break;

            case "$isoTimestamp":
                result = Instant.now().atZone(ZoneId.systemDefault())
                        .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
                break;

            case "$randomInt":
                result = String.valueOf(RANDOM.nextInt(1001)); // 0-1000
                break;

            case "$randomAlphaNumeric":
                result = generateRandomAlphaNumeric(10);
                break;

            case "$randomBoolean":
                result = String.valueOf(RANDOM.nextBoolean());
                break;

            case "$randomTF":
                result = RANDOM.nextBoolean() ? "T" : "F";
                break;

            case "$randomIP":
                result = RANDOM.nextInt(256) + "." + RANDOM.nextInt(256) + "." +
                        RANDOM.nextInt(256) + "." + RANDOM.nextInt(256);
                break;

            case "$randomEmail":
                result = generateRandomAlphaNumeric(8).toLowerCase() + "@example.com";
                break;

            case "$randomFullName":
                result = getRandomFirstName() + " " + getRandomLastName();
                break;

            case "$randomFirstName":
                result = getRandomFirstName();
                break;

            case "$randomLastName":
                result = getRandomLastName();
                break;

            case "$randomColor":
                result = String.format("#%06x", RANDOM.nextInt(0xffffff + 1));
                break;

            case "$randomDate":
                LocalDate date = LocalDate.now().minusDays(RANDOM.nextInt(365));
                result = date.format(DateTimeFormatter.ISO_LOCAL_DATE);
                break;

            case "$randomTime":
                LocalTime time = LocalTime.of(RANDOM.nextInt(24), RANDOM.nextInt(60), RANDOM.nextInt(60));
                result = time.format(DateTimeFormatter.ISO_LOCAL_TIME);
                break;

            case "$randomUrl":
                result = "https://example" + RANDOM.nextInt(100) + ".com/" +
                        generateRandomAlphaNumeric(8).toLowerCase();
                break;

            case "$randomPhoneNumber":
                result = String.format("+1-%03d-%03d-%04d",
                        RANDOM.nextInt(1000), RANDOM.nextInt(1000), RANDOM.nextInt(10000));
                break;

            case "$randomCity":
                result = getRandomCity();
                break;

            case "$randomCountry":
                result = getRandomCountry();
                break;

            case "$randomUserAgent":
                result = getRandomUserAgent();
                break;

            case "$randomMD5":
                result = generateMD5Hash(generateRandomAlphaNumeric(16));
                break;

            case "$randomBase64":
                result = Base64.getEncoder().encodeToString(generateRandomAlphaNumeric(12).getBytes());
                break;

            default:
                log.warn("Unknown built-in function: {}", funcName);
                return null;
        }

        log.debug("Generated value for {}: {}", funcName, result);
        return result;
    }

    private String generateSimpleFunction(SimpleFunctionCall functionCall) {
        List<String> args = functionCall.arguments();
        try {
            return switch (functionCall.name()) {
                case "__uuid" -> UUID.randomUUID().toString();
                case "__timestamp" -> String.valueOf(System.currentTimeMillis());
                case "__time" -> formatCurrentTime(args);
                case "__randomInt" -> randomInt(args);
                case "__randomString" -> randomString(args);
                case "__urlEncode" -> URLEncoder.encode(firstArg(args), StandardCharsets.UTF_8);
                case "__base64" -> Base64.getEncoder().encodeToString(firstArg(args).getBytes(StandardCharsets.UTF_8));
                default -> null;
            };
        } catch (Exception e) {
            log.warn("Failed to evaluate built-in function {}: {}", functionCall.name(), e.getMessage());
            return null;
        }
    }

    private static String formatCurrentTime(List<String> args) {
        String pattern = args.isEmpty() || args.get(0).isBlank()
                ? "yyyy-MM-dd'T'HH:mm:ss.SSSXXX"
                : args.get(0);
        return Instant.now()
                .atZone(ZoneId.systemDefault())
                .format(DateTimeFormatter.ofPattern(pattern));
    }

    private static String randomInt(List<String> args) {
        int min = args.isEmpty() ? 0 : parseInt(args.get(0), 0);
        int max = args.size() < 2 ? 1000 : parseInt(args.get(1), 1000);
        if (max < min) {
            int tmp = min;
            min = max;
            max = tmp;
        }
        if (min == max) {
            return String.valueOf(min);
        }
        return String.valueOf(ThreadLocalRandom.current().nextLong(min, (long) max + 1L));
    }

    private static String randomString(List<String> args) {
        int length = args.isEmpty() ? 10 : parseInt(args.get(0), 10);
        length = Math.max(0, Math.min(MAX_RANDOM_STRING_LENGTH, length));
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder sb = new StringBuilder(length);
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }

    private static int parseInt(String value, int defaultValue) {
        try {
            return Integer.parseInt(value.trim());
        } catch (Exception ignored) {
            return defaultValue;
        }
    }

    private static String firstArg(List<String> args) {
        return args.isEmpty() ? "" : args.get(0);
    }

    private static SimpleFunctionCall parseSimpleFunctionCall(String expression) {
        if (expression == null || !expression.startsWith("__") || !expression.endsWith(")")) {
            return null;
        }
        int openParen = expression.indexOf('(');
        if (openParen <= 0) {
            return null;
        }
        String name = expression.substring(0, openParen).trim();
        if (!SIMPLE_FUNCTIONS.contains(name)) {
            return null;
        }
        String rawArgs = expression.substring(openParen + 1, expression.length() - 1);
        return new SimpleFunctionCall(name, parseArguments(rawArgs));
    }

    private static List<String> parseArguments(String rawArgs) {
        if (rawArgs == null || rawArgs.isBlank()) {
            return List.of();
        }
        List<String> args = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean escaped = false;
        char quote = 0;
        for (int i = 0; i < rawArgs.length(); i++) {
            char ch = rawArgs.charAt(i);
            if (escaped) {
                current.append(ch);
                escaped = false;
                continue;
            }
            if (ch == '\\' && quote != 0) {
                escaped = true;
                continue;
            }
            if (quote != 0) {
                if (ch == quote) {
                    quote = 0;
                } else {
                    current.append(ch);
                }
                continue;
            }
            if (ch == '\'' || ch == '"') {
                quote = ch;
                continue;
            }
            if (ch == ',') {
                args.add(resolveArgument(current.toString()));
                current.setLength(0);
                continue;
            }
            current.append(ch);
        }
        args.add(resolveArgument(current.toString()));
        return args;
    }

    private static String resolveArgument(String raw) {
        String value = raw == null ? "" : raw.trim();
        if (value.isEmpty()) {
            return "";
        }
        String resolved = VariableResolver.resolveVariable(value);
        return resolved != null ? resolved : value;
    }

    /**
     * 获取所有内置函数及其描述
     *
     * @return Map<函数名, 描述>
     */
    public Map<String, String> getAllFunctionsWithDescriptions() {
        Map<String, String> functions = new LinkedHashMap<>();
        for (String func : BUILT_IN_FUNCTIONS) {
            functions.put(func, getDescription(func));
        }
        return functions;
    }

    /**
     * 获取内置函数的描述
     *
     * @param funcName 函数名
     * @return 函数描述
     */
    private static String getDescription(String funcName) {
        try {
            return I18nUtil.getMessage("builtin.var." + funcName.substring(1));
        } catch (Exception e) {
            // 如果没有找到国际化文本，返回默认描述
            return "Built-in function: " + funcName;
        }
    }

    // ==================== 辅助方法 ====================

    private static String generateRandomAlphaNumeric(int length) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(RANDOM.nextInt(chars.length())));
        }
        return sb.toString();
    }

    private static String getRandomFirstName() {
        String[] names = {"John", "Jane", "Michael", "Emily", "David", "Sarah",
                "James", "Emma", "Robert", "Olivia", "William", "Sophia",
                "Daniel", "Isabella", "Matthew", "Mia"};
        return names[RANDOM.nextInt(names.length)];
    }

    private static String getRandomLastName() {
        String[] names = {"Smith", "Johnson", "Williams", "Brown", "Jones",
                "Garcia", "Miller", "Davis", "Rodriguez", "Martinez",
                "Anderson", "Taylor", "Thomas", "Moore", "Jackson"};
        return names[RANDOM.nextInt(names.length)];
    }

    private static String getRandomCity() {
        String[] cities = {"New York", "Los Angeles", "Chicago", "Houston", "Phoenix",
                "Philadelphia", "San Antonio", "San Diego", "Dallas", "Austin",
                "London", "Paris", "Tokyo", "Beijing", "Shanghai"};
        return cities[RANDOM.nextInt(cities.length)];
    }

    private static String getRandomCountry() {
        String[] countries = {"United States", "United Kingdom", "Canada", "Australia", "Germany",
                "France", "Japan", "China", "Brazil", "India",
                "Mexico", "Spain", "Italy", "South Korea", "Netherlands"};
        return countries[RANDOM.nextInt(countries.length)];
    }

    private static String getRandomUserAgent() {
        String[] userAgents = {
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
                "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:121.0) Gecko/20100101 Firefox/121.0",
                "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.1 Safari/605.1.15",
                "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
        };
        return userAgents[RANDOM.nextInt(userAgents.length)];
    }

    private static String generateMD5Hash(String input) {
        return DigestUtils.md5Hex(input);
    }

    private record SimpleFunctionCall(String name, List<String> arguments) {
    }
}
