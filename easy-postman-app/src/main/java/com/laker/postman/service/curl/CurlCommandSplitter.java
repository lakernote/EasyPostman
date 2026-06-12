package com.laker.postman.service.curl;

import lombok.experimental.UtilityClass;

import java.util.ArrayList;
import java.util.List;

@UtilityClass
class CurlCommandSplitter {

    static List<String> split(String curlText) {
        List<String> commands = new ArrayList<>();
        if (curlText == null || curlText.trim().isEmpty()) {
            return commands;
        }

        StringBuilder current = new StringBuilder();
        boolean inSingleQuote = false;
        boolean inDoubleQuote = false;
        boolean inDollarQuote = false;

        for (int i = 0; i < curlText.length(); i++) {
            char c = curlText.charAt(i);

            if (inDoubleQuote || inDollarQuote) {
                current.append(c);
                if (c == '\\' && i + 1 < curlText.length()) {
                    current.append(curlText.charAt(++i));
                    continue;
                }
                if (inDoubleQuote && c == '"') {
                    inDoubleQuote = false;
                } else if (inDollarQuote && c == '\'') {
                    inDollarQuote = false;
                }
                continue;
            }

            if (inSingleQuote) {
                current.append(c);
                if (c == '\'') {
                    inSingleQuote = false;
                }
                continue;
            }

            if (c == '$' && i + 1 < curlText.length() && curlText.charAt(i + 1) == '\'') {
                inDollarQuote = true;
                current.append(c).append(curlText.charAt(++i));
                continue;
            }
            if (c == '\'') {
                inSingleQuote = true;
                current.append(c);
                continue;
            }
            if (c == '"') {
                inDoubleQuote = true;
                current.append(c);
                continue;
            }

            if (c == ';') {
                addCurlCommand(commands, current);
                current.setLength(0);
                continue;
            }

            if (isLineBreak(c)) {
                int nextIndex = c == '\r' && i + 1 < curlText.length() && curlText.charAt(i + 1) == '\n'
                        ? i + 2
                        : i + 1;
                if (!endsWithLineContinuation(current) && startsWithCurlCommandAt(curlText, nextIndex)) {
                    addCurlCommand(commands, current);
                    current.setLength(0);
                    if (nextIndex == i + 2) {
                        i++;
                    }
                    continue;
                }
            }

            current.append(c);
        }

        addCurlCommand(commands, current);
        return commands;
    }

    private static void addCurlCommand(List<String> commands, StringBuilder command) {
        String value = command.toString().trim();
        if (startsWithCurlCommand(value)) {
            commands.add(value);
        }
    }

    private static boolean startsWithCurlCommandAt(String text, int index) {
        int i = index;
        while (i < text.length() && Character.isWhitespace(text.charAt(i))) {
            i++;
        }
        return startsWithCurlCommand(text.substring(i));
    }

    private static boolean startsWithCurlCommand(String text) {
        if (text == null) {
            return false;
        }
        String trimmed = text.trim();
        if (trimmed.length() < 4 || !trimmed.regionMatches(true, 0, "curl", 0, 4)) {
            return false;
        }
        return trimmed.length() == 4 || Character.isWhitespace(trimmed.charAt(4));
    }

    private static boolean isLineBreak(char c) {
        return c == '\n' || c == '\r';
    }

    private static boolean endsWithLineContinuation(CharSequence value) {
        for (int i = value.length() - 1; i >= 0; i--) {
            char c = value.charAt(i);
            if (c == '\n' || c == '\r') {
                return false;
            }
            if (!Character.isWhitespace(c)) {
                return c == '\\';
            }
        }
        return false;
    }
}
