package com.laker.postman.performance.core.controller;

import java.util.ArrayList;
import java.util.List;

public final class ConditionExpressionEvaluator {

    private ConditionExpressionEvaluator() {
    }

    public interface VariableLookup {
        VariableLookup EMPTY = new VariableLookup() {
            @Override
            public String resolve(String variableName) {
                return null;
            }

            @Override
            public boolean isDefined(String variableName) {
                return false;
            }
        };

        String resolve(String variableName);

        boolean isDefined(String variableName);
    }

    public static boolean evaluate(String expression, VariableLookup variableLookup) {
        if (expression == null || expression.isBlank()) {
            return false;
        }
        try {
            VariableLookup safeLookup = variableLookup == null ? VariableLookup.EMPTY : variableLookup;
            return new Parser(new Tokenizer(expression).tokens(), safeLookup).parse();
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    private enum TokenType {
        VARIABLE,
        STRING,
        NUMBER,
        BOOLEAN,
        IDENTIFIER,
        LPAREN,
        RPAREN,
        COMMA,
        AND,
        OR,
        NOT,
        EQ,
        NE,
        GT,
        LT,
        GE,
        LE,
        EOF
    }

    private record Token(TokenType type, String text) {
    }

    private record ConditionValue(String text, Boolean booleanValue) {
        static ConditionValue text(String value) {
            return new ConditionValue(value == null ? "" : value, null);
        }

        static ConditionValue bool(boolean value) {
            return new ConditionValue(Boolean.toString(value), value);
        }

        boolean asBoolean() {
            if (booleanValue != null) {
                return booleanValue;
            }
            return "true".equalsIgnoreCase(text.trim());
        }
    }

    private static final class Parser {
        private final List<Token> tokens;
        private final VariableLookup variableLookup;
        private int index;

        private Parser(List<Token> tokens, VariableLookup variableLookup) {
            this.tokens = tokens;
            this.variableLookup = variableLookup;
        }

        private boolean parse() {
            boolean result = parseOr();
            expect(TokenType.EOF);
            return result;
        }

        private boolean parseOr() {
            boolean result = parseAnd();
            while (match(TokenType.OR)) {
                boolean right = parseAnd();
                result = result || right;
            }
            return result;
        }

        private boolean parseAnd() {
            boolean result = parseUnary();
            while (match(TokenType.AND)) {
                boolean right = parseUnary();
                result = result && right;
            }
            return result;
        }

        private boolean parseUnary() {
            if (match(TokenType.NOT)) {
                return !parseUnary();
            }
            return parseComparison();
        }

        private boolean parseComparison() {
            ConditionValue left = parsePrimary();
            Token operator = peek();
            if (operator.type() != TokenType.EQ
                    && operator.type() != TokenType.NE
                    && operator.type() != TokenType.GT
                    && operator.type() != TokenType.LT
                    && operator.type() != TokenType.GE
                    && operator.type() != TokenType.LE) {
                return left.asBoolean();
            }
            index++;
            ConditionValue right = parsePrimary();
            return compare(left, operator.type(), right);
        }

        private ConditionValue parsePrimary() {
            Token token = peek();
            index++;
            return switch (token.type()) {
                case VARIABLE -> ConditionValue.text(resolveVariable(token.text()));
                case STRING, NUMBER -> ConditionValue.text(token.text());
                case BOOLEAN -> ConditionValue.bool(Boolean.parseBoolean(token.text()));
                case IDENTIFIER -> parseIdentifier(token);
                case LPAREN -> {
                    boolean value = parseOr();
                    expect(TokenType.RPAREN);
                    yield ConditionValue.bool(value);
                }
                default -> throw new IllegalArgumentException("Unexpected token: " + token.type());
            };
        }

        private ConditionValue parseIdentifier(Token token) {
            if ("defined".equalsIgnoreCase(token.text()) && match(TokenType.LPAREN)) {
                String variableName = parseVariableNameArgument();
                expect(TokenType.RPAREN);
                return ConditionValue.bool(variableLookup.isDefined(variableName));
            }
            return ConditionValue.text(token.text());
        }

        private String parseVariableNameArgument() {
            Token argument = peek();
            index++;
            return switch (argument.type()) {
                case STRING, IDENTIFIER, VARIABLE -> argument.text();
                default -> throw new IllegalArgumentException("Invalid function argument: " + argument.type());
            };
        }

        private boolean compare(ConditionValue left, TokenType operator, ConditionValue right) {
            if (operator == TokenType.EQ || operator == TokenType.NE) {
                boolean equals = left.text().equals(right.text());
                return operator == TokenType.EQ ? equals : !equals;
            }
            Double leftNumber = toNumber(left.text());
            Double rightNumber = toNumber(right.text());
            if (leftNumber == null || rightNumber == null) {
                return false;
            }
            return switch (operator) {
                case GT -> leftNumber > rightNumber;
                case LT -> leftNumber < rightNumber;
                case GE -> leftNumber >= rightNumber;
                case LE -> leftNumber <= rightNumber;
                default -> false;
            };
        }

        private String resolveVariable(String variableName) {
            String value = variableLookup.resolve(variableName);
            return value == null ? "" : value;
        }

        private Double toNumber(String value) {
            if (value == null || value.isBlank()) {
                return null;
            }
            try {
                return Double.parseDouble(value.trim());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }

        private boolean match(TokenType type) {
            if (peek().type() != type) {
                return false;
            }
            index++;
            return true;
        }

        private void expect(TokenType type) {
            if (!match(type)) {
                throw new IllegalArgumentException("Expected token: " + type);
            }
        }

        private Token peek() {
            return tokens.get(index);
        }
    }

    private static final class Tokenizer {
        private final String expression;
        private final List<Token> tokens = new ArrayList<>();
        private int index;

        private Tokenizer(String expression) {
            this.expression = expression;
        }

        private List<Token> tokens() {
            while (index < expression.length()) {
                char current = expression.charAt(index);
                if (Character.isWhitespace(current)) {
                    index++;
                } else if (startsWith("{{")) {
                    readVariable();
                } else if (current == '\'' || current == '"') {
                    readString(current);
                } else if (isNumberStart(current)) {
                    readNumber();
                } else if (isIdentifierStart(current)) {
                    readIdentifier();
                } else {
                    readOperator();
                }
            }
            tokens.add(new Token(TokenType.EOF, ""));
            return tokens;
        }

        private void readVariable() {
            int end = expression.indexOf("}}", index + 2);
            if (end < 0) {
                throw new IllegalArgumentException("Unclosed variable");
            }
            String variableName = expression.substring(index + 2, end).trim();
            tokens.add(new Token(TokenType.VARIABLE, variableName));
            index = end + 2;
        }

        private void readString(char quote) {
            index++;
            StringBuilder value = new StringBuilder();
            while (index < expression.length()) {
                char current = expression.charAt(index++);
                if (current == quote) {
                    tokens.add(new Token(TokenType.STRING, value.toString()));
                    return;
                }
                if (current == '\\' && index < expression.length()) {
                    value.append(unescape(expression.charAt(index++)));
                } else {
                    value.append(current);
                }
            }
            throw new IllegalArgumentException("Unclosed string");
        }

        private char unescape(char value) {
            return switch (value) {
                case 'n' -> '\n';
                case 'r' -> '\r';
                case 't' -> '\t';
                default -> value;
            };
        }

        private void readNumber() {
            int start = index;
            if (expression.charAt(index) == '-') {
                index++;
            }
            while (index < expression.length()
                    && (Character.isDigit(expression.charAt(index)) || expression.charAt(index) == '.')) {
                index++;
            }
            tokens.add(new Token(TokenType.NUMBER, expression.substring(start, index)));
        }

        private void readIdentifier() {
            int start = index;
            index++;
            while (index < expression.length() && isIdentifierPart(expression.charAt(index))) {
                index++;
            }
            String text = expression.substring(start, index);
            if ("true".equalsIgnoreCase(text) || "false".equalsIgnoreCase(text)) {
                tokens.add(new Token(TokenType.BOOLEAN, text.toLowerCase()));
            } else {
                tokens.add(new Token(TokenType.IDENTIFIER, text));
            }
        }

        private void readOperator() {
            if (startsWith("&&")) {
                tokens.add(new Token(TokenType.AND, "&&"));
                index += 2;
            } else if (startsWith("||")) {
                tokens.add(new Token(TokenType.OR, "||"));
                index += 2;
            } else if (startsWith("==")) {
                tokens.add(new Token(TokenType.EQ, "=="));
                index += 2;
            } else if (startsWith("!=")) {
                tokens.add(new Token(TokenType.NE, "!="));
                index += 2;
            } else if (startsWith(">=")) {
                tokens.add(new Token(TokenType.GE, ">="));
                index += 2;
            } else if (startsWith("<=")) {
                tokens.add(new Token(TokenType.LE, "<="));
                index += 2;
            } else {
                readSingleOperator();
            }
        }

        private void readSingleOperator() {
            char current = expression.charAt(index++);
            switch (current) {
                case '=' -> tokens.add(new Token(TokenType.EQ, "="));
                case '!' -> tokens.add(new Token(TokenType.NOT, "!"));
                case '>' -> tokens.add(new Token(TokenType.GT, ">"));
                case '<' -> tokens.add(new Token(TokenType.LT, "<"));
                case '(' -> tokens.add(new Token(TokenType.LPAREN, "("));
                case ')' -> tokens.add(new Token(TokenType.RPAREN, ")"));
                case ',' -> tokens.add(new Token(TokenType.COMMA, ","));
                default -> throw new IllegalArgumentException("Unexpected char: " + current);
            }
        }

        private boolean startsWith(String value) {
            return expression.startsWith(value, index);
        }

        private boolean isNumberStart(char value) {
            return Character.isDigit(value)
                    || (value == '-' && index + 1 < expression.length() && Character.isDigit(expression.charAt(index + 1)));
        }

        private boolean isIdentifierStart(char value) {
            return Character.isLetter(value) || value == '_';
        }

        private boolean isIdentifierPart(char value) {
            return Character.isLetterOrDigit(value) || value == '_' || value == '.' || value == '-';
        }
    }
}
