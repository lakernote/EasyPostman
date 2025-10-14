package com.laker.postman.util;

import com.laker.postman.model.Environment;
import com.laker.postman.model.VariableSegment;
import com.laker.postman.service.EnvironmentService;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.security.MessageDigest;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EasyPostmanVariableUtil {
    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\{\\{([^}]+)}}");
    private static final Random RANDOM = new Random();

    // 内置函数列表
    private static final String[] BUILT_IN_FUNCTIONS = {
            "$guid",
            "$uuid",
            "$randomUUID",
            "$timestamp",
            "$isoTimestamp",
            "$randomInt",
            "$randomAlphaNumeric",
            "$randomBoolean",
            "$randomIP",
            "$randomEmail",
            "$randomFullName",
            "$randomFirstName",
            "$randomLastName",
            "$randomColor",
            "$randomDate",
            "$randomTime",
            "$randomUrl",
            "$randomPhoneNumber",
            "$randomCity",
            "$randomCountry",
            "$randomUserAgent",
            "$randomMD5",
            "$randomBase64"
    };

    public static List<VariableSegment> getVariableSegments(String value) {
        List<VariableSegment> segments = new ArrayList<>();
        if (value == null) return segments;
        Matcher m = VARIABLE_PATTERN.matcher(value);
        while (m.find()) {
            segments.add(new VariableSegment(m.start(), m.end(), m.group(1)));
        }
        return segments;
    }

    public static boolean isVariableDefined(String varName) {
        if (varName == null) return false;
        if (EnvironmentService.getTemporaryVariable(varName) != null) {
            return true;
        }
        Environment activeEnv = EnvironmentService.getActiveEnvironment();
        return activeEnv != null && activeEnv.getVariable(varName) != null;
    }

    public static String getVariableValue(String varName) {
        if (varName == null) return null;
        Object temp = EnvironmentService.getTemporaryVariable(varName);
        if (temp != null) return temp.toString();
        Environment activeEnv = EnvironmentService.getActiveEnvironment();
        if (activeEnv != null && activeEnv.getVariable(varName) != null) {
            Object v = activeEnv.getVariable(varName);
            return v == null ? null : v.toString();
        }
        return null;
    }

    /**
     * 获取所有可用的变量（包括环境变量和内置函数）
     * 返回 Map<变量名, 变量值或描述>
     */
    public static Map<String, String> getAllAvailableVariables() {
        Map<String, String> allVars = new LinkedHashMap<>();

        // 添加当前激活环境的变量
        Environment activeEnv = EnvironmentService.getActiveEnvironment();
        if (activeEnv != null && activeEnv.getVariables() != null) {
            for (Map.Entry<String, String> entry : activeEnv.getVariables().entrySet()) {
                String value = entry.getValue();
                // 如果值太长，截断显示
                if (value != null && value.length() > 50) {
                    value = value.substring(0, 47) + "...";
                }
                allVars.put(entry.getKey(), value);
            }
        }
        // 添加内置函数
        for (String func : BUILT_IN_FUNCTIONS) {
            allVars.put(func, getBuiltInFunctionDescription(func));
        }
        return allVars;
    }

    /**
     * 获取内置函数的描述
     */
    private static String getBuiltInFunctionDescription(String func) {
        try {
            return I18nUtil.getMessage("builtin.var." + func.substring(1));
        } catch (Exception e) {
            // 如果没有找到国际化文本，返回默认描述
            return "Built-in function: " + func;
        }
    }

    /**
     * 根据前缀过滤变量列表
     */
    public static Map<String, String> filterVariables(String prefix) {
        Map<String, String> allVars = getAllAvailableVariables();
        if (prefix == null || prefix.isEmpty()) {
            return allVars;
        }

        Map<String, String> filtered = new LinkedHashMap<>();
        String lowerPrefix = prefix.toLowerCase();
        for (Map.Entry<String, String> entry : allVars.entrySet()) {
            if (entry.getKey().toLowerCase().startsWith(lowerPrefix)) {
                filtered.put(entry.getKey(), entry.getValue());
            }
        }
        return filtered;
    }

    /**
     * 判断是否是内置函数
     */
    public static boolean isBuiltInFunction(String name) {
        if (name == null) return false;
        for (String func : BUILT_IN_FUNCTIONS) {
            if (func.equals(name)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 生成内置函数的值
     */
    public static String generateBuiltInFunctionValue(String funcName) {
        if (funcName == null) return null;

        switch (funcName) {
            case "$guid":
            case "$uuid":
            case "$randomUUID":
                return UUID.randomUUID().toString();

            case "$timestamp":
                return String.valueOf(System.currentTimeMillis() / 1000);

            case "$isoTimestamp":
                return Instant.now().atZone(ZoneId.systemDefault())
                        .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);

            case "$randomInt":
                return String.valueOf(RANDOM.nextInt(1001)); // 0-1000

            case "$randomAlphaNumeric":
                return generateRandomAlphaNumeric(10);

            case "$randomBoolean":
                return String.valueOf(RANDOM.nextBoolean());

            case "$randomIP":
                return RANDOM.nextInt(256) + "." + RANDOM.nextInt(256) + "." +
                        RANDOM.nextInt(256) + "." + RANDOM.nextInt(256);

            case "$randomEmail":
                return generateRandomAlphaNumeric(8).toLowerCase() + "@example.com";

            case "$randomFullName":
                return getRandomFirstName() + " " + getRandomLastName();

            case "$randomFirstName":
                return getRandomFirstName();

            case "$randomLastName":
                return getRandomLastName();

            case "$randomColor":
                return String.format("#%06x", RANDOM.nextInt(0xffffff + 1));

            case "$randomDate":
                LocalDate date = LocalDate.now().minusDays(RANDOM.nextInt(365));
                return date.format(DateTimeFormatter.ISO_LOCAL_DATE);

            case "$randomTime":
                LocalTime time = LocalTime.of(RANDOM.nextInt(24), RANDOM.nextInt(60), RANDOM.nextInt(60));
                return time.format(DateTimeFormatter.ISO_LOCAL_TIME);

            case "$randomUrl":
                return "https://example" + RANDOM.nextInt(100) + ".com/" + generateRandomAlphaNumeric(8).toLowerCase();

            case "$randomPhoneNumber":
                return String.format("+1-%03d-%03d-%04d",
                        RANDOM.nextInt(1000), RANDOM.nextInt(1000), RANDOM.nextInt(10000));

            case "$randomCity":
                return getRandomCity();

            case "$randomCountry":
                return getRandomCountry();

            case "$randomUserAgent":
                return getRandomUserAgent();

            case "$randomMD5":
                return generateMD5Hash(generateRandomAlphaNumeric(16));

            case "$randomBase64":
                return Base64.getEncoder().encodeToString(generateRandomAlphaNumeric(12).getBytes());

            default:
                return null;
        }
    }

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
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hash = md.digest(input.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            return UUID.randomUUID().toString().replace("-", "");
        }
    }
}