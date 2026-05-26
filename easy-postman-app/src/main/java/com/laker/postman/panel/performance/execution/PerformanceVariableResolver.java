package com.laker.postman.panel.performance.execution;

import com.laker.postman.service.variable.VariableResolver;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@UtilityClass
class PerformanceVariableResolver {

    private static final Pattern VAR_PATTERN = Pattern.compile("\\{\\{(.+?)}}");
    private static final Set<String> LIGHTWEIGHT_FUNCTIONS = Set.of(
            "__uuid",
            "__timestamp",
            "__time",
            "__randomInt",
            "__randomString",
            "__urlEncode",
            "__base64"
    );
    private static final int MAX_ITERATIONS = 10;
    private static final int MAX_RANDOM_STRING_LENGTH = 1024;

    String resolve(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }

        String result = text;
        for (int i = 0; i < MAX_ITERATIONS; i++) {
            String before = result;
            result = resolveOnce(result);
            if (before.equals(result)) {
                break;
            }
        }
        return result;
    }

    private String resolveOnce(String text) {
        Matcher matcher = VAR_PATTERN.matcher(text);
        StringBuilder result = new StringBuilder();
        while (matcher.find()) {
            String expression = matcher.group(1);
            String value = evaluateLightweightFunction(expression.trim());
            if (value == null) {
                value = VariableResolver.resolveVariable(expression);
            }
            if (value == null && !expression.equals(expression.trim())) {
                value = VariableResolver.resolveVariable(expression.trim());
            }

            if (value == null) {
                matcher.appendReplacement(result, Matcher.quoteReplacement(matcher.group(0)));
            } else {
                matcher.appendReplacement(result, Matcher.quoteReplacement(value));
            }
        }
        matcher.appendTail(result);
        return result.toString();
    }

    private String evaluateLightweightFunction(String expression) {
        SimpleFunctionCall functionCall = parseFunctionCall(expression);
        if (functionCall == null) {
            return null;
        }
        try {
            List<String> args = functionCall.arguments();
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
            log.warn("Failed to evaluate performance function {}: {}", functionCall.name(), e.getMessage());
            return null;
        }
    }

    private SimpleFunctionCall parseFunctionCall(String expression) {
        if (expression == null || !expression.startsWith("__") || !expression.endsWith(")")) {
            return null;
        }
        int openParen = expression.indexOf('(');
        if (openParen <= 0) {
            return null;
        }
        String name = expression.substring(0, openParen).trim();
        if (!LIGHTWEIGHT_FUNCTIONS.contains(name)) {
            return null;
        }
        String rawArgs = expression.substring(openParen + 1, expression.length() - 1);
        return new SimpleFunctionCall(name, parseArguments(rawArgs));
    }

    private List<String> parseArguments(String rawArgs) {
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

    private String resolveArgument(String raw) {
        String value = raw == null ? "" : raw.trim();
        if (value.isEmpty()) {
            return "";
        }
        String resolved = VariableResolver.resolveVariable(value);
        return resolved != null ? resolved : value;
    }

    private String formatCurrentTime(List<String> args) {
        String pattern = args.isEmpty() || args.get(0).isBlank()
                ? "yyyy-MM-dd'T'HH:mm:ss.SSSXXX"
                : args.get(0);
        return Instant.now()
                .atZone(ZoneId.systemDefault())
                .format(DateTimeFormatter.ofPattern(pattern));
    }

    private String randomInt(List<String> args) {
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

    private String randomString(List<String> args) {
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

    private int parseInt(String value, int defaultValue) {
        try {
            return Integer.parseInt(value.trim());
        } catch (Exception ignored) {
            return defaultValue;
        }
    }

    private String firstArg(List<String> args) {
        return args.isEmpty() ? "" : args.get(0);
    }

    private record SimpleFunctionCall(String name, List<String> arguments) {
    }
}
