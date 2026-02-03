package com.laker.postman.util;

import com.laker.postman.model.VariableSegment;
import lombok.experimental.UtilityClass;
import org.apache.commons.codec.digest.DigestUtils;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@UtilityClass
public class VariableUtil {
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

    /**
     * 获取所有内置函数（返回函数名和描述）
     */
    public static Map<String, String> getAllBuiltInFunctions() {
        Map<String, String> functions = new LinkedHashMap<>();
        for (String func : BUILT_IN_FUNCTIONS) {
            functions.put(func, getBuiltInFunctionDescription(func));
        }
        return functions;
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
        return DigestUtils.md5Hex(input);
    }
}