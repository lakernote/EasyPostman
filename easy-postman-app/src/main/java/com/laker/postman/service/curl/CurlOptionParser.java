package com.laker.postman.service.curl;

import com.laker.postman.request.model.AuthType;
import com.laker.postman.request.model.HttpFormData;
import lombok.experimental.UtilityClass;

import java.util.List;
import java.util.Locale;

@UtilityClass
class CurlOptionParser {

    static CurlCommandOptions parse(List<String> tokens) {
        CurlCommandOptions options = new CurlCommandOptions();
        if (tokens == null || tokens.isEmpty()) {
            return options;
        }

        for (int i = 0; i < tokens.size(); i++) {
            String token = tokens.get(i);
            if (isCurlExecutableToken(token) || "\"".equals(token)) {
                continue;
            }

            if ("--".equals(token)) {
                for (int j = i + 1; j < tokens.size(); j++) {
                    applyPositionalToken(options, tokens.get(j));
                }
                break;
            }

            if (token.startsWith("--") && token.length() > 2) {
                i = parseLongOption(tokens, i, options);
                continue;
            }

            if (token.startsWith("-") && token.length() > 1 && !isUrlToken(token)) {
                i = parseShortOptions(tokens, i, options);
                continue;
            }

            applyPositionalToken(options, token);
        }

        return options;
    }

    private static int parseLongOption(List<String> tokens, int index, CurlCommandOptions options) {
        String token = tokens.get(index);
        String optionToken = token.substring(2);
        int separator = optionToken.indexOf('=');
        String name = separator >= 0 ? optionToken.substring(0, separator) : optionToken;
        String inlineValue = separator >= 0 ? optionToken.substring(separator + 1) : null;
        CurlOptionSpec spec = CurlOptionRegistry.findLong(name);

        if (spec == null) {
            options.addWarning("curl.option.unknown", "Unsupported cURL option ignored: --" + name);
            return index;
        }

        OptionValueResult valueResult = resolveOptionValue(tokens, index, inlineValue, spec, options);
        if (valueResult.missingRequiredValue()) {
            return index;
        }
        applyOption(options, spec, valueResult.value());
        return valueResult.nextIndex();
    }

    private static int parseShortOptions(List<String> tokens, int index, CurlCommandOptions options) {
        String token = tokens.get(index);
        int i = 1;
        while (i < token.length()) {
            char optionName = token.charAt(i);
            CurlOptionSpec spec = CurlOptionRegistry.findShort(optionName);
            if (spec == null) {
                options.addWarning("curl.option.unknown", "Unsupported cURL option ignored: -" + optionName);
                i++;
                continue;
            }

            if (spec.requiresValue() || spec.acceptsOptionalValue()) {
                String attachedValue = i + 1 < token.length() ? token.substring(i + 1) : null;
                OptionValueResult valueResult = resolveOptionValue(tokens, index, attachedValue, spec, options);
                if (!valueResult.missingRequiredValue()) {
                    applyOption(options, spec, valueResult.value());
                }
                return valueResult.nextIndex();
            }

            applyOption(options, spec, null);
            i++;
        }
        return index;
    }

    private static OptionValueResult resolveOptionValue(
            List<String> tokens,
            int index,
            String inlineValue,
            CurlOptionSpec spec,
            CurlCommandOptions options
    ) {
        if (inlineValue != null) {
            if (spec.valueMode() == CurlOptionValueMode.NONE) {
                options.addWarning("curl.option.unexpected_value", "Unexpected inline value for cURL option: " + spec.displayName());
            }
            return new OptionValueResult(inlineValue, index, false);
        }
        if (spec.valueMode() == CurlOptionValueMode.NONE) {
            return new OptionValueResult(null, index, false);
        }
        if (index + 1 < tokens.size()) {
            return new OptionValueResult(tokens.get(index + 1), index + 1, false);
        }
        if (spec.requiresValue()) {
            options.addWarning("curl.option.missing_value", "Missing value for cURL option: " + spec.displayName());
            return new OptionValueResult(null, index, true);
        }
        return new OptionValueResult(null, index, false);
    }

    private static void applyOption(CurlCommandOptions options, CurlOptionSpec spec, String value) {
        switch (spec.action()) {
            case URL -> options.url = CurlUrlSupport.normalizeCurlUrlToken(value);
            case REQUEST -> {
                if (value != null) {
                    options.method = value.toUpperCase(Locale.ROOT);
                }
            }
            case HEADER -> options.addHeader(CurlHeaderSupport.parseHeaderOption(value));
            case COOKIE -> {
                if (value != null) {
                    options.addCookieHeader(value);
                }
            }
            case DATA -> {
                if (value != null) {
                    options.dataParams.add(value);
                    if ("json".equals(spec.longName())) {
                        options.setHeader("Content-Type", "application/json");
                        options.setHeader("Accept", "application/json");
                    }
                }
            }
            case DATA_BINARY -> {
                if (value != null) {
                    if (value.startsWith("@") && value.length() > 1) {
                        options.binaryDataFilePath = value.substring(1);
                    } else {
                        options.dataParams.add(value);
                    }
                }
            }
            case DATA_URLENCODE -> {
                if (value != null) {
                    options.dataUrlencodeParams.add(value);
                }
            }
            case FORM -> {
                HttpFormData formData = CurlFormDataParser.parseOption(value);
                if (formData != null) {
                    options.formDataList.add(formData);
                }
            }
            case GET -> options.forceGet = true;
            case LOCATION -> options.followRedirects = true;
            case USER -> options.applyUserOption(value);
            case DIGEST -> options.authType = AuthType.DIGEST.getConstant();
            case USER_AGENT -> {
                if (value != null) {
                    options.setHeader("User-Agent", value);
                }
            }
            case REFERER -> {
                if (value != null) {
                    options.setHeader("Referer", value);
                }
            }
            case HEAD -> options.method = "HEAD";
            case OAUTH2_BEARER -> {
                if (value != null) {
                    options.setHeader("Authorization", "Bearer " + value);
                }
            }
            case IGNORE -> {
                if (spec.requiresValue() || spec.acceptsOptionalValue()) {
                    options.addWarning("curl.option.ignored", "cURL option is not part of the imported request model and was ignored: " + spec.displayName());
                }
            }
        }
    }

    private static boolean isUrlToken(String token) {
        return token.startsWith("http://")
                || token.startsWith("https://")
                || token.startsWith("ws://")
                || token.startsWith("wss://");
    }

    private static void applyPositionalToken(CurlCommandOptions options, String token) {
        if (isUrlToken(token)) {
            options.url = CurlUrlSupport.normalizeCurlUrlToken(token);
        }
    }

    private static boolean isCurlExecutableToken(String token) {
        if (token == null) {
            return false;
        }
        int slashIndex = Math.max(token.lastIndexOf('/'), token.lastIndexOf('\\'));
        String executableName = slashIndex >= 0 ? token.substring(slashIndex + 1) : token;
        return "curl".equalsIgnoreCase(executableName) || "curl.exe".equalsIgnoreCase(executableName);
    }

    private record OptionValueResult(String value, int nextIndex, boolean missingRequiredValue) {
    }
}
