package com.laker.postman.panel.collections.editor.request.sub;

import com.laker.postman.util.JsonUtil;

import java.util.Optional;

final class JsonCopyTargetResolver {

    private JsonCopyTargetResolver() {
    }

    static Optional<CopyTarget> resolve(String json, int offset) {
        if (json == null || json.isBlank() || offset < 0 || offset > json.length()) {
            return Optional.empty();
        }
        try {
            Parser parser = new Parser(json, offset);
            parser.parseDocument();
            return Optional.ofNullable(parser.bestTarget);
        } catch (IllegalArgumentException ex) {
            return Optional.empty();
        }
    }

    record CopyTarget(String key, String value) {
    }

    private static final class Parser {
        private final String text;
        private final int offset;
        private CopyTarget bestTarget;
        private int bestSpan = Integer.MAX_VALUE;

        private Parser(String text, int offset) {
            this.text = text;
            this.offset = offset;
        }

        private void parseDocument() {
            int end = parseValue(skipWhitespace(0)).end();
            if (skipWhitespace(end) != text.length()) {
                throw new IllegalArgumentException("Unexpected trailing content");
            }
        }

        private ValueRange parseValue(int index) {
            index = skipWhitespace(index);
            if (index >= text.length()) {
                throw new IllegalArgumentException("Unexpected end of JSON");
            }

            char ch = text.charAt(index);
            if (ch == '{') {
                int end = parseObject(index);
                return new ValueRange(index, end, text.substring(index, end));
            }
            if (ch == '[') {
                int end = parseArray(index);
                return new ValueRange(index, end, text.substring(index, end));
            }
            if (ch == '"') {
                StringRange stringRange = parseString(index);
                return new ValueRange(index, stringRange.end(), stringRange.unescapedValue());
            }
            return parseLiteral(index);
        }

        private int parseObject(int index) {
            int cursor = skipWhitespace(index + 1);
            if (cursor < text.length() && text.charAt(cursor) == '}') {
                return cursor + 1;
            }

            while (cursor < text.length()) {
                StringRange keyRange = parseString(cursor);
                cursor = skipWhitespace(keyRange.end());
                expect(cursor, ':');
                cursor = skipWhitespace(cursor + 1);

                ValueRange valueRange = parseValue(cursor);
                consider(new Candidate(
                        keyRange.start(),
                        keyRange.end(),
                        valueRange.start(),
                        valueRange.end(),
                        keyRange.unescapedValue(),
                        valueRange.copyValue()));

                cursor = skipWhitespace(valueRange.end());
                if (cursor < text.length() && text.charAt(cursor) == ',') {
                    cursor = skipWhitespace(cursor + 1);
                    continue;
                }
                expect(cursor, '}');
                return cursor + 1;
            }
            throw new IllegalArgumentException("Unclosed object");
        }

        private int parseArray(int index) {
            int cursor = skipWhitespace(index + 1);
            if (cursor < text.length() && text.charAt(cursor) == ']') {
                return cursor + 1;
            }

            while (cursor < text.length()) {
                ValueRange valueRange = parseValue(cursor);
                consider(new Candidate(-1, -1, valueRange.start(), valueRange.end(), null, valueRange.copyValue()));

                cursor = skipWhitespace(valueRange.end());
                if (cursor < text.length() && text.charAt(cursor) == ',') {
                    cursor = skipWhitespace(cursor + 1);
                    continue;
                }
                expect(cursor, ']');
                return cursor + 1;
            }
            throw new IllegalArgumentException("Unclosed array");
        }

        private StringRange parseString(int start) {
            expect(start, '"');
            StringBuilder rawContent = new StringBuilder();
            int cursor = start + 1;
            while (cursor < text.length()) {
                char ch = text.charAt(cursor);
                if (ch == '"') {
                    return new StringRange(start, cursor + 1, JsonUtil.unescapeJsonStringContent(rawContent.toString()));
                }
                if (ch == '\\') {
                    if (cursor + 1 >= text.length()) {
                        throw new IllegalArgumentException("Unclosed escape sequence");
                    }
                    rawContent.append(ch).append(text.charAt(cursor + 1));
                    cursor += 2;
                    continue;
                }
                rawContent.append(ch);
                cursor++;
            }
            throw new IllegalArgumentException("Unclosed string");
        }

        private ValueRange parseLiteral(int start) {
            int cursor = start;
            while (cursor < text.length()) {
                char ch = text.charAt(cursor);
                if (Character.isWhitespace(ch) || ch == ',' || ch == '}' || ch == ']') {
                    break;
                }
                cursor++;
            }
            if (cursor == start) {
                throw new IllegalArgumentException("Expected literal");
            }
            String literal = text.substring(start, cursor);
            if (!isSupportedLiteral(literal)) {
                throw new IllegalArgumentException("Unsupported literal");
            }
            return new ValueRange(start, cursor, literal);
        }

        private boolean isSupportedLiteral(String literal) {
            return "true".equals(literal)
                    || "false".equals(literal)
                    || "null".equals(literal)
                    || literal.matches("-?(0|[1-9]\\d*)(\\.\\d+)?([eE][+-]?\\d+)?");
        }

        private void consider(Candidate candidate) {
            int span = matchingSpan(candidate);
            if (span >= 0 && span < bestSpan) {
                bestSpan = span;
                bestTarget = new CopyTarget(candidate.key(), candidate.value());
            }
        }

        private int matchingSpan(Candidate candidate) {
            if (candidate.keyStart() >= 0 && contains(candidate.keyStart(), candidate.keyEnd())) {
                return candidate.keyEnd() - candidate.keyStart();
            }
            if (contains(candidate.valueStart(), candidate.valueEnd())) {
                return candidate.valueEnd() - candidate.valueStart();
            }
            return -1;
        }

        private boolean contains(int start, int end) {
            return offset >= start && offset < end;
        }

        private int skipWhitespace(int index) {
            while (index < text.length() && Character.isWhitespace(text.charAt(index))) {
                index++;
            }
            return index;
        }

        private void expect(int index, char expected) {
            if (index >= text.length() || text.charAt(index) != expected) {
                throw new IllegalArgumentException("Expected " + expected);
            }
        }
    }

    private record StringRange(int start, int end, String unescapedValue) {
    }

    private record ValueRange(int start, int end, String copyValue) {
    }

    private record Candidate(int keyStart, int keyEnd, int valueStart, int valueEnd, String key, String value) {
    }
}
