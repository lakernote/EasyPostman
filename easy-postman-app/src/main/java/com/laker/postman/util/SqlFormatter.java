package com.laker.postman.util;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * SQL 格式化工具类。
 * 不引入 Druid 运行时依赖，保留 Druid 风格的常用格式化规则和更强的本地校验。
 */
@Slf4j
public final class SqlFormatter {

    public enum SqlDialect {
        GENERIC,
        MYSQL,
        POSTGRESQL,
        SQLSERVER,
        ORACLE
    }

    private static final Set<String> KEYWORDS = new HashSet<>();
    private static final Set<String> LOGIC_OPERATORS = new HashSet<>();
    private static final Set<String> FUNCTIONS = new HashSet<>();
    private static final Set<String> CASE_KEYWORDS = new HashSet<>();
    private static final Set<String> CLAUSE_KEYWORDS = new HashSet<>();
    private static final Set<String> STATEMENT_KEYWORDS = new HashSet<>();
    private static final Set<String> INCOMPLETE_ENDINGS = new HashSet<>();

    static {
        KEYWORDS.addAll(Arrays.asList(
                "SELECT", "FROM", "WHERE", "INSERT", "INTO", "VALUES", "UPDATE", "SET",
                "DELETE", "MERGE", "REPLACE", "TRUNCATE", "CREATE", "ALTER", "DROP", "TABLE", "INDEX",
                "VIEW", "DATABASE", "SCHEMA", "CONSTRAINT", "PRIMARY", "FOREIGN", "KEY",
                "REFERENCES", "UNIQUE", "CHECK", "JOIN", "INNER", "LEFT", "RIGHT", "FULL",
                "OUTER", "CROSS", "ON", "USING", "GROUP", "BY", "HAVING", "ORDER", "LIMIT",
                "OFFSET", "UNION", "ALL", "INTERSECT", "EXCEPT", "MINUS", "DISTINCT", "AS",
                "WITH", "RECURSIVE", "DESC", "ASC", "TOP", "RETURNING", "FETCH", "FIRST", "LAST", "NEXT", "ROWS", "ONLY",
                "DUPLICATE", "MATCHED", "NOLOCK", "IGNORE", "STRAIGHT_JOIN",
                "USE", "FOR", "FORCE", "LOCK", "SHARE", "MODE", "NULLS", "OUTPUT",
                "START", "CONNECT", "PRIOR", "SIBLINGS", "CONNECT_BY_ROOT", "FILTER",
                "OVER", "PARTITION", "BETWEEN", "UNBOUNDED", "PRECEDING", "CURRENT", "ROW", "NOCYCLE",
                "RANGE", "GROUPS", "FOLLOWING", "EXCLUDE", "NO", "TIES", "OTHERS"
        ));

        LOGIC_OPERATORS.addAll(Arrays.asList(
                "AND", "OR", "NOT", "IN", "EXISTS", "BETWEEN", "LIKE", "ILIKE", "IS", "NULL"
        ));

        CASE_KEYWORDS.addAll(Arrays.asList(
                "CASE", "WHEN", "THEN", "ELSE", "END"
        ));

        FUNCTIONS.addAll(Arrays.asList(
                "COUNT", "SUM", "AVG", "MAX", "MIN",
                "UPPER", "LOWER", "SUBSTR", "CONCAT", "LENGTH", "TRIM",
                "NOW", "DATE", "TIME", "YEAR", "MONTH", "DAY",
                "CURRENT_TIMESTAMP", "CURRENT_DATE", "CURRENT_TIME"
        ));

        CLAUSE_KEYWORDS.addAll(Arrays.asList(
                "SELECT", "FROM", "WHERE", "GROUP", "ORDER", "HAVING", "LIMIT",
                "OFFSET", "UNION", "INTERSECT", "EXCEPT", "MINUS", "SET", "VALUES", "ON",
                "RETURNING", "FETCH", "START", "CONNECT", "FOR", "LOCK", "OUTPUT"
        ));

        STATEMENT_KEYWORDS.addAll(Arrays.asList(
                "SELECT", "INSERT", "UPDATE", "DELETE", "REPLACE", "CREATE", "ALTER", "DROP",
                "TRUNCATE", "MERGE", "WITH"
        ));

        INCOMPLETE_ENDINGS.addAll(Arrays.asList(
                "SELECT", "FROM", "WHERE", "GROUP", "ORDER", "HAVING", "LIMIT",
                "OFFSET", "UNION", "INTERSECT", "EXCEPT", "MINUS", "AND", "OR",
                "NOT", "JOIN", "INNER", "LEFT", "RIGHT", "FULL", "OUTER",
                "CROSS", "ON", "SET", "VALUES", "WHEN", "THEN", "ELSE", "BY", "INTO",
                "RETURNING", "FETCH", "TOP", "START", "CONNECT", "FOR", "LOCK", "OUTPUT"
        ));
    }

    private SqlFormatter() {
    }

    /**
     * 格式化选项。
     */
    public static class FormatOption {
        private String indent = "  ";
        private boolean uppercaseKeywords = true;
        private boolean addSemicolon = true;
        private boolean lineBreakBeforeFrom = true;
        private boolean lineBreakBeforeJoin = true;
        private boolean lineBreakBeforeWhere = true;
        private boolean lineBreakBeforeAnd = true;
        private boolean lineBreakBeforeOr = true;
        private boolean lineBreakAfterComma = true;
        private SqlDialect dialect = SqlDialect.GENERIC;

        public FormatOption setIndent(int spaces) {
            this.indent = " ".repeat(Math.max(0, spaces));
            return this;
        }

        public FormatOption setUppercaseKeywords(boolean uppercase) {
            this.uppercaseKeywords = uppercase;
            return this;
        }

        public FormatOption setAddSemicolon(boolean add) {
            this.addSemicolon = add;
            return this;
        }

        public FormatOption setLineBreakBeforeAnd(boolean lineBreak) {
            this.lineBreakBeforeAnd = lineBreak;
            return this;
        }

        public FormatOption setLineBreakBeforeOr(boolean lineBreak) {
            this.lineBreakBeforeOr = lineBreak;
            return this;
        }

        public FormatOption setLineBreakAfterComma(boolean lineBreak) {
            this.lineBreakAfterComma = lineBreak;
            return this;
        }

        public FormatOption setDialect(SqlDialect dialect) {
            this.dialect = dialect == null ? SqlDialect.GENERIC : dialect;
            return this;
        }
    }

    public record ValidationResult(boolean valid, List<String> issues, int statementCount) {
        public ValidationResult {
            issues = issues == null ? List.of() : List.copyOf(issues);
        }

        public String firstIssue() {
            return issues.isEmpty() ? "" : issues.get(0);
        }
    }

    /**
     * 标准格式化 SQL。
     */
    public static String format(String sql) {
        return format(sql, new FormatOption());
    }

    public static String format(String sql, SqlDialect dialect) {
        return format(sql, new FormatOption().setDialect(dialect));
    }

    /**
     * 格式化 SQL。
     */
    public static String format(String sql, FormatOption option) {
        if (sql == null || sql.trim().isEmpty()) {
            return sql;
        }

        try {
            FormatOption actualOption = option == null ? new FormatOption() : option;
            List<Token> tokens = tokenize(preprocessSql(sql), actualOption.dialect);
            List<List<Token>> statements = splitStatements(tokens);
            if (statements.isEmpty()) {
                return sql;
            }

            List<String> formattedStatements = new ArrayList<>();
            for (List<Token> statement : statements) {
                if (hasRenderableTokens(statement)) {
                    formattedStatements.add(postprocessSql(formatTokens(statement, actualOption)));
                }
            }

            if (formattedStatements.isEmpty()) {
                return sql.trim();
            }

            String separator = actualOption.addSemicolon ? ";\n\n" : "\n\n";
            String formatted = String.join(separator, formattedStatements);
            if (actualOption.addSemicolon && !formatted.endsWith(";")) {
                formatted += ";";
            }
            return formatted;
        } catch (ValidationException ex) {
            log.debug("SQL format validation error: {}", ex.getMessage());
            return sql;
        } catch (Exception ex) {
            log.error("SQL format error", ex);
            return sql;
        }
    }

    /**
     * 压缩 SQL 为单行。
     */
    public static String compress(String sql) {
        return compress(sql, SqlDialect.GENERIC);
    }

    public static String compress(String sql, SqlDialect dialect) {
        if (sql == null || sql.trim().isEmpty()) {
            return sql;
        }

        try {
            List<Token> tokens = tokenize(normalizeLineSeparators(sql), dialect);
            StringBuilder result = new StringBuilder();
            Token previousToken = null;

            for (int i = 0; i < tokens.size(); i++) {
                Token token = tokens.get(i);
                if (token.type == TokenType.COMMENT) {
                    continue;
                }

                if (";".equals(token.value)) {
                    trimTrailingSpace(result);
                    if (result.length() > 0 && result.charAt(result.length() - 1) != ';') {
                        result.append(';');
                    }
                    Token nextToken = nextRenderableToken(tokens, i + 1);
                    if (nextToken != null) {
                        result.append(' ');
                    }
                    previousToken = null;
                    continue;
                }

                if (previousToken != null && needsSpaceBetween(previousToken, token, false)) {
                    result.append(' ');
                }

                result.append(token.value);
                previousToken = token;
            }

            return result.toString().trim();
        } catch (Exception ex) {
            log.error("SQL compress error", ex);
            return sql.trim();
        }
    }

    /**
     * 转换关键字大小写。
     */
    public static String convertKeywords(String sql, boolean toUppercase) {
        return convertKeywords(sql, toUppercase, SqlDialect.GENERIC);
    }

    public static String convertKeywords(String sql, boolean toUppercase, SqlDialect dialect) {
        if (sql == null || sql.trim().isEmpty()) {
            return sql;
        }

        try {
            List<Token> tokens = tokenize(normalizeLineSeparators(sql), dialect);
            StringBuilder result = new StringBuilder();
            Token previousToken = null;

            for (Token token : tokens) {
                if (token.type == TokenType.COMMENT) {
                    if (result.length() > 0 && result.charAt(result.length() - 1) != '\n') {
                        result.append('\n');
                    }
                    result.append(token.value);
                    previousToken = null;
                    continue;
                }

                if (previousToken != null && needsSpaceBetween(previousToken, token, false)) {
                    result.append(' ');
                }

                if (token.type == TokenType.KEYWORD) {
                    result.append(toUppercase
                            ? token.value.toUpperCase(Locale.ROOT)
                            : token.value.toLowerCase(Locale.ROOT));
                } else {
                    result.append(token.value);
                }

                previousToken = token;
            }

            return result.toString();
        } catch (Exception ex) {
            log.error("SQL keyword conversion error", ex);
            return sql;
        }
    }

    /**
     * SQL 校验。
     */
    public static ValidationResult validate(String sql) {
        return validate(sql, SqlDialect.GENERIC);
    }

    public static ValidationResult validate(String sql, SqlDialect dialect) {
        if (sql == null || sql.trim().isEmpty()) {
            return new ValidationResult(false, List.of(), 0);
        }

        try {
            List<Token> tokens = tokenize(normalizeLineSeparators(sql), dialect);
            List<String> issues = new ArrayList<>(collectParenthesisIssues(tokens));
            List<List<Token>> statements = splitStatements(tokens);
            int statementCount = 0;

            for (List<Token> statement : statements) {
                List<Token> significantTokens = filterComments(statement);
                if (significantTokens.isEmpty()) {
                    continue;
                }
                statementCount++;
                validateStatement(significantTokens, issues, dialect);
            }

            if (statementCount == 0) {
                issues.add(I18nUtil.getMessage(MessageKeys.TOOLBOX_SQL_VALIDATION_NO_KEYWORDS));
            }

            return new ValidationResult(issues.isEmpty(), issues, statementCount);
        } catch (ValidationException ex) {
            return new ValidationResult(false, List.of(ex.getMessage()), 0);
        } catch (Exception ex) {
            log.error("SQL validation error", ex);
            return new ValidationResult(false, List.of(ex.getMessage()), 0);
        }
    }

    private static List<String> collectParenthesisIssues(List<Token> tokens) {
        int openParen = 0;
        int closeParen = 0;
        for (Token token : tokens) {
            if ("(".equals(token.value)) {
                openParen++;
            } else if (")".equals(token.value)) {
                closeParen++;
            }
        }

        if (openParen == closeParen) {
            return List.of();
        }

        return List.of(I18nUtil.getMessage(MessageKeys.TOOLBOX_SQL_VALIDATION_PAREN_MISMATCH,
                String.valueOf(openParen), String.valueOf(closeParen)));
    }

    private static String preprocessSql(String sql) {
        return normalizeLineSeparators(sql).trim();
    }

    private static String postprocessSql(String sql) {
        return sql.replaceAll("\\n{3,}", "\n\n")
                .replaceAll("[ \\t]+\\n", "\n")
                .trim();
    }

    private static String normalizeLineSeparators(String sql) {
        return sql.replace("\r\n", "\n").replace('\r', '\n');
    }

    private static List<Token> tokenize(String sql, SqlDialect dialect) {
        List<Token> tokens = new ArrayList<>();
        int pos = 0;

        while (pos < sql.length()) {
            char ch = sql.charAt(pos);

            if (Character.isWhitespace(ch)) {
                pos++;
                continue;
            }

            if (ch == '\'') {
                Token token = parseQuotedToken(sql, pos, ch, TokenType.STRING);
                tokens.add(token);
                pos = token.endPos;
                continue;
            }

            if (ch == '"') {
                TokenType quoteTokenType = supportsDoubleQuotedIdentifiers(dialect)
                        ? TokenType.IDENTIFIER : TokenType.STRING;
                Token token = parseQuotedToken(sql, pos, ch, quoteTokenType);
                tokens.add(token);
                pos = token.endPos;
                continue;
            }

            if (ch == '`') {
                Token token = parseQuotedToken(sql, pos, ch, TokenType.IDENTIFIER);
                tokens.add(token);
                pos = token.endPos;
                continue;
            }

            if (ch == '[') {
                Token token = parseBracketIdentifier(sql, pos);
                tokens.add(token);
                pos = token.endPos;
                continue;
            }

            if (ch == '-' && pos + 1 < sql.length() && sql.charAt(pos + 1) == '-') {
                Token token = parseLineComment(sql, pos);
                tokens.add(token);
                pos = token.endPos;
                continue;
            }

            if (ch == '/' && pos + 1 < sql.length() && sql.charAt(pos + 1) == '*') {
                Token token = parseBlockComment(sql, pos);
                tokens.add(token);
                pos = token.endPos;
                continue;
            }

            if (ch == '#' && supportsHashComments(dialect)) {
                Token token = parseLineComment(sql, pos);
                tokens.add(token);
                pos = token.endPos;
                continue;
            }

            if (Character.isDigit(ch)) {
                Token token = parseNumber(sql, pos);
                tokens.add(token);
                pos = token.endPos;
                continue;
            }

            if (isIdentifierStart(ch)) {
                Token token = parseIdentifier(sql, pos);
                tokens.add(token);
                pos = token.endPos;
                continue;
            }

            if (ch == ':' && pos + 1 < sql.length() && isIdentifierStart(sql.charAt(pos + 1))) {
                Token token = parseNamedParameter(sql, pos);
                tokens.add(token);
                pos = token.endPos;
                continue;
            }

            Token token = parseOperator(sql, pos);
            tokens.add(token);
            pos = token.endPos;
        }

        return tokens;
    }

    private static boolean isIdentifierStart(char ch) {
        return Character.isLetter(ch) || ch == '_' || ch == '$';
    }

    private static List<List<Token>> splitStatements(List<Token> tokens) {
        List<List<Token>> statements = new ArrayList<>();
        List<Token> current = new ArrayList<>();
        int parenLevel = 0;

        for (Token token : tokens) {
            if ("(".equals(token.value)) {
                parenLevel++;
            } else if (")".equals(token.value)) {
                parenLevel = Math.max(0, parenLevel - 1);
            }

            if (";".equals(token.value) && parenLevel == 0) {
                if (!current.isEmpty()) {
                    statements.add(current);
                    current = new ArrayList<>();
                }
                continue;
            }

            current.add(token);
        }

        if (!current.isEmpty()) {
            statements.add(current);
        }

        return statements;
    }

    private static boolean hasRenderableTokens(List<Token> tokens) {
        for (Token token : tokens) {
            if (token.type != TokenType.COMMENT) {
                return true;
            }
        }
        return false;
    }

    private static List<Token> filterComments(List<Token> tokens) {
        List<Token> filtered = new ArrayList<>();
        for (Token token : tokens) {
            if (token.type != TokenType.COMMENT) {
                filtered.add(token);
            }
        }
        return filtered;
    }

    private static String formatTokens(List<Token> tokens, FormatOption option) {
        StringBuilder result = new StringBuilder();
        int indentLevel = 0;
        boolean lineStart = true;
        boolean inCaseExpression = false;
        int caseLevel = 0;
        int parenLevel = 0;
        String statementType = detectStatementType(tokens);
        boolean cascadeTopLevelSelectClauses = "SELECT".equals(statementType) || "WITH".equals(statementType);
        List<ParenBlockType> parenBlockStack = new ArrayList<>();

        for (int i = 0; i < tokens.size(); i++) {
            Token token = tokens.get(i);
            Token previousToken = previousRenderableToken(tokens, i - 1);
            Token nextToken = nextRenderableToken(tokens, i + 1);
            String upper = token.upperValue();
            SpecialClause specialClause = detectSpecialClause(tokens, i, option.dialect, statementType);
            ParenBlockType activeParenBlockType = activeParenBlockType(parenBlockStack);
            boolean structuredFormattingContext = isStructuredFormattingContext(parenLevel, activeParenBlockType);
            boolean setOperatorSelectStart = "SELECT".equals(upper)
                    && previousToken != null
                    && isSetOperator(previousToken.upperValue());
            ParenBlockType closingParenBlockType = ")".equals(token.value) && !parenBlockStack.isEmpty()
                    ? parenBlockStack.get(parenBlockStack.size() - 1)
                    : ParenBlockType.NONE;
            boolean closingStatementParen = ")".equals(token.value)
                    && closingParenBlockType != ParenBlockType.NONE;

            if (token.type == TokenType.COMMENT) {
                Token nextCommentTarget = nextRenderableToken(tokens, i + 1);
                int commentIndentLevel = indentLevel;
                if (nextCommentTarget != null
                        && shouldDecreaseIndentBeforeToken(nextCommentTarget.upperValue(),
                        structuredFormattingContext, activeParenBlockType,
                        cascadeTopLevelSelectClauses, parenLevel, option.dialect)) {
                    commentIndentLevel = Math.max(0, commentIndentLevel - 1);
                }
                if (!lineStart) {
                    result.append('\n');
                }
                appendIndent(result, commentIndentLevel, option.indent);
                result.append(token.value.trim());
                result.append('\n');
                lineStart = true;
                continue;
            }

            if ("END".equals(upper)) {
                indentLevel = Math.max(0, indentLevel - 1);
            }

            if (closingStatementParen) {
                indentLevel = Math.max(0, indentLevel - closingParenBlockType.indentReduction());
                if (!lineStart) {
                    result.append('\n');
                }
                lineStart = true;
            }

            if (shouldDecreaseIndentBeforeToken(upper,
                    structuredFormattingContext, activeParenBlockType,
                    cascadeTopLevelSelectClauses, parenLevel, option.dialect)) {
                indentLevel = Math.max(0, indentLevel - 1);
            }
            if (setOperatorSelectStart) {
                indentLevel = Math.max(0, indentLevel - 1);
            }
            if (specialClause.decreaseIndentBefore) {
                indentLevel = Math.max(0, indentLevel - 1);
            }

            if (specialClause.forceLineBreakBefore
                    || needsLineBreakBefore(token, previousToken, option, inCaseExpression,
                    structuredFormattingContext, activeParenBlockType)) {
                if (!lineStart) {
                    result.append('\n');
                }
                lineStart = true;
            }

            if (lineStart) {
                appendIndent(result, indentLevel, option.indent);
                lineStart = false;
            } else if (previousToken != null && needsSpaceBetween(previousToken, token, option.lineBreakAfterComma)) {
                result.append(' ');
            }

            result.append(formatTokenValue(token, option.uppercaseKeywords));

            if ("CASE".equals(upper)) {
                inCaseExpression = true;
                caseLevel++;
                indentLevel++;
            }

            if ("(".equals(token.value)) {
                ParenBlockType parenBlockType = detectParenBlockType(previousToken, nextToken);
                parenBlockStack.add(parenBlockType);
                if (parenBlockType != ParenBlockType.NONE) {
                    indentLevel++;
                    result.append('\n');
                    lineStart = true;
                }
                parenLevel++;
            } else if (")".equals(token.value)) {
                if (!parenBlockStack.isEmpty()) {
                    parenBlockStack.remove(parenBlockStack.size() - 1);
                }
                parenLevel = Math.max(0, parenLevel - 1);
            }

            if (shouldIncreaseIndentAfterToken(upper, cascadeTopLevelSelectClauses, parenLevel, activeParenBlockType)) {
                indentLevel++;
            }
            if (specialClause.increaseIndentAfter) {
                indentLevel++;
            }

            if (needsLineBreakAfter(token, nextToken, option,
                    structuredFormattingContext, activeParenBlockType)) {
                result.append('\n');
                lineStart = true;
            }

            if ("END".equals(upper)) {
                caseLevel = Math.max(0, caseLevel - 1);
                if (caseLevel == 0) {
                    inCaseExpression = false;
                }
            }
        }

        return result.toString();
    }

    private static boolean needsLineBreakBefore(Token token, Token previousToken, FormatOption option,
                                                boolean inCaseExpression, boolean structuredFormattingContext,
                                                ParenBlockType activeParenBlockType) {
        if (previousToken == null) {
            return false;
        }

        String upper = token.upperValue();
        String previousUpper = previousToken.upperValue();

        if (inCaseExpression && ("WHEN".equals(upper) || "ELSE".equals(upper) || "END".equals(upper))) {
            return true;
        }

        if ("SELECT".equals(upper) && isSetOperator(previousUpper)) {
            return true;
        }

        if (isJoinKeyword(upper) && isJoinModifier(previousUpper)) {
            return false;
        }
        if ("AND".equals(upper) && "MATCHED".equals(previousUpper)) {
            return false;
        }
        if ("DELETE".equals(upper) && "THEN".equals(previousUpper)) {
            return false;
        }
        if ("WHERE".equals(upper) && "DELETE".equals(previousUpper)) {
            return false;
        }

        if (activeParenBlockType.shouldSuppressLineBreakBefore(upper, previousUpper)) {
            return false;
        }
        if (activeParenBlockType.shouldForceLineBreakBefore(upper, previousUpper)) {
            return true;
        }

        if (structuredFormattingContext) {
            if ("SELECT".equals(upper) && ")".equals(previousToken.value)) {
                return true;
            }
            if (option.lineBreakBeforeFrom && "FROM".equals(upper)) {
                return true;
            }
            if (option.lineBreakBeforeWhere && "WHERE".equals(upper)) {
                return true;
            }
            if ("GROUP".equals(upper) || "ORDER".equals(upper) || "HAVING".equals(upper)
                    || "LIMIT".equals(upper) || "OFFSET".equals(upper)
                    || "UNION".equals(upper) || "INTERSECT".equals(upper)
                    || "EXCEPT".equals(upper) || "MINUS".equals(upper)
                    || "RETURNING".equals(upper) || "FETCH".equals(upper)
                    || "SET".equals(upper) || "VALUES".equals(upper)
                    || "DELETE".equals(upper)
                    || "START".equals(upper) || "CONNECT".equals(upper)
                    || "FOR".equals(upper) || "LOCK".equals(upper)
                    || "OUTPUT".equals(upper)) {
                return true;
            }
            if (option.lineBreakBeforeJoin && (isJoinKeyword(upper) || isJoinModifier(upper))) {
                return true;
            }
            if (option.lineBreakBeforeAnd && "AND".equals(upper) && !"BETWEEN".equals(previousUpper)) {
                return true;
            }
            if (option.lineBreakBeforeOr && "OR".equals(upper)) {
                return true;
            }
        }

        return false;
    }

    private static boolean needsLineBreakAfter(Token token, Token nextToken, FormatOption option,
                                               boolean structuredFormattingContext,
                                               ParenBlockType activeParenBlockType) {
        if (nextToken == null || !option.lineBreakAfterComma) {
            return false;
        }
        if (!",".equals(token.value)) {
            return false;
        }

        String nextUpper = nextToken.upperValue();
        if (structuredFormattingContext) {
            if (activeParenBlockType.shouldBreakAfterCommaBefore(nextUpper)) {
                return true;
            }
            return !"FROM".equals(nextUpper) && !"WHERE".equals(nextUpper) && !")".equals(nextToken.value);
        }
        return false;
    }

    private static boolean isStructuredFormattingContext(int parenLevel, ParenBlockType activeParenBlockType) {
        return parenLevel == 0 || activeParenBlockType != ParenBlockType.NONE;
    }

    private static boolean shouldDecreaseIndentBeforeToken(String upper, boolean structuredFormattingContext,
                                                           ParenBlockType activeParenBlockType,
                                                           boolean cascadeTopLevelSelectClauses, int parenLevel,
                                                           SqlDialect dialect) {
        if (!structuredFormattingContext) {
            return false;
        }

        if (activeParenBlockType.shouldKeepIndentFor(upper)) {
            return false;
        }

        if (cascadeTopLevelSelectClauses && parenLevel == 0) {
            if ((dialect == SqlDialect.SQLSERVER || dialect == SqlDialect.ORACLE)
                    && ("OFFSET".equals(upper) || "FETCH".equals(upper))) {
                return false;
            }
            return "LIMIT".equals(upper) || "OFFSET".equals(upper) || "FOR".equals(upper)
                    || "LOCK".equals(upper)
                    || "UNION".equals(upper) || "INTERSECT".equals(upper)
                    || "EXCEPT".equals(upper) || "MINUS".equals(upper)
                    || "RETURNING".equals(upper) || "FETCH".equals(upper)
                    || "DELETE".equals(upper)
                    || "OUTPUT".equals(upper);
        }

        return "FROM".equals(upper) || "WHERE".equals(upper) || "GROUP".equals(upper)
                || "HAVING".equals(upper) || "ORDER".equals(upper) || "LIMIT".equals(upper)
                || "OFFSET".equals(upper) || "UNION".equals(upper) || "INTERSECT".equals(upper)
                || "EXCEPT".equals(upper) || "MINUS".equals(upper) || "SET".equals(upper)
                || "DELETE".equals(upper)
                || "VALUES".equals(upper) || "RETURNING".equals(upper) || "FETCH".equals(upper)
                || "START".equals(upper) || "CONNECT".equals(upper) || "FOR".equals(upper)
                || "LOCK".equals(upper) || "OUTPUT".equals(upper);
    }

    private static boolean shouldIncreaseIndentAfterToken(String upper, boolean cascadeTopLevelSelectClauses,
                                                          int parenLevel, ParenBlockType activeParenBlockType) {
        if (activeParenBlockType.shouldSkipIndentAfter(upper)) {
            return false;
        }

        if (cascadeTopLevelSelectClauses && parenLevel == 0) {
            return "SELECT".equals(upper) || "FROM".equals(upper) || "WHERE".equals(upper)
                    || "START".equals(upper) || "CONNECT".equals(upper);
        }

        return "SELECT".equals(upper) || "FROM".equals(upper) || "WHERE".equals(upper)
                || "GROUP".equals(upper) || "HAVING".equals(upper) || "ORDER".equals(upper)
                || "SET".equals(upper) || "VALUES".equals(upper)
                || "RETURNING".equals(upper) || "FETCH".equals(upper)
                || "START".equals(upper) || "CONNECT".equals(upper)
                || "OUTPUT".equals(upper);
    }

    private static boolean needsSpaceBetween(Token previousToken, Token currentToken, boolean lineBreakAfterComma) {
        if (previousToken == null || currentToken == null) {
            return false;
        }

        if (".".equals(previousToken.value) || ".".equals(currentToken.value)) {
            return false;
        }

        if ("::".equals(previousToken.value) || "::".equals(currentToken.value)) {
            return false;
        }

        if ("(".equals(currentToken.value) && previousToken.type == TokenType.FUNCTION) {
            return false;
        }

        if ("(".equals(currentToken.value) && previousToken.type == TokenType.KEYWORD) {
            String previousUpper = previousToken.upperValue();
            if ("AND".equals(previousUpper) || "OR".equals(previousUpper) || "NOT".equals(previousUpper)) {
                return false;
            }
        }

        if ("(".equals(previousToken.value)) {
            return false;
        }

        if (")".equals(currentToken.value) || ",".equals(currentToken.value) || ";".equals(currentToken.value)) {
            return false;
        }

        if (",".equals(previousToken.value)) {
            return true;
        }

        return true;
    }

    private static boolean isMysqlDuplicateKeyUpdateStart(List<Token> tokens, int index, SqlDialect dialect) {
        if (dialect != SqlDialect.MYSQL || index < 0 || index >= tokens.size()) {
            return false;
        }
        Token token = tokens.get(index);
        if (!"ON".equals(token.upperValue())) {
            return false;
        }

        Token first = nextRenderableToken(tokens, index + 1);
        Token second = first == null ? null : nextRenderableToken(tokens, tokens.indexOf(first) + 1);
        Token third = second == null ? null : nextRenderableToken(tokens, tokens.indexOf(second) + 1);
        return first != null && second != null && third != null
                && "DUPLICATE".equals(first.upperValue())
                && "KEY".equals(second.upperValue())
                && "UPDATE".equals(third.upperValue());
    }

    private static boolean isMergeClauseStart(String statementType, List<Token> tokens, int index) {
        if (!"MERGE".equals(statementType) || index < 0 || index >= tokens.size()) {
            return false;
        }
        String upper = tokens.get(index).upperValue();
        return "USING".equals(upper) || "ON".equals(upper) || "WHEN".equals(upper);
    }

    private static boolean isOrderSiblingsClauseStart(List<Token> tokens, int index) {
        if (index < 0 || index >= tokens.size()) {
            return false;
        }
        Token token = tokens.get(index);
        if (!"ORDER".equals(token.upperValue())) {
            return false;
        }
        Token next = nextRenderableToken(tokens, index + 1);
        return next != null && "SIBLINGS".equals(next.upperValue());
    }

    private static SpecialClause detectSpecialClause(List<Token> tokens, int index, SqlDialect dialect,
                                                     String statementType) {
        if (isMysqlDuplicateKeyUpdateStart(tokens, index, dialect)) {
            return SpecialClause.MYSQL_DUPLICATE_KEY_UPDATE;
        }
        if (isOutputIntoClauseStart(tokens, index)) {
            return SpecialClause.OUTPUT_INTO;
        }
        if (isMergeClauseStart(statementType, tokens, index)) {
            return SpecialClause.MERGE_BLOCK;
        }
        if (isOrderSiblingsClauseStart(tokens, index)) {
            return SpecialClause.ORDER_SIBLINGS_BY;
        }
        return SpecialClause.NONE;
    }

    private enum SpecialClause {
        NONE(false, false, false),
        MYSQL_DUPLICATE_KEY_UPDATE(true, true, true),
        OUTPUT_INTO(true, true, false),
        MERGE_BLOCK(true, true, false),
        ORDER_SIBLINGS_BY(true, true, false);

        private final boolean forceLineBreakBefore;
        private final boolean decreaseIndentBefore;
        private final boolean increaseIndentAfter;

        SpecialClause(boolean forceLineBreakBefore, boolean decreaseIndentBefore, boolean increaseIndentAfter) {
            this.forceLineBreakBefore = forceLineBreakBefore;
            this.decreaseIndentBefore = decreaseIndentBefore;
            this.increaseIndentAfter = increaseIndentAfter;
        }
    }

    private enum ParenBlockType {
        NONE(0),
        SUBQUERY(2),
        FILTER_CLAUSE(1) {
            @Override
            boolean shouldKeepIndentFor(String upper) {
                return "WHERE".equals(upper);
            }

            @Override
            boolean shouldSkipIndentAfter(String upper) {
                return "WHERE".equals(upper);
            }
        },
        WINDOW_CLAUSE(1) {
            @Override
            boolean shouldForceLineBreakBefore(String upper, String previousUpper) {
                return isWindowClauseBoundary(upper);
            }

            @Override
            boolean shouldSuppressLineBreakBefore(String upper, String previousUpper) {
                return ("AND".equals(upper) && isWindowFrameContinuation(previousUpper))
                        || ("EXCLUDE".equals(previousUpper) && isWindowExcludeTarget(upper));
            }

            @Override
            boolean shouldBreakAfterCommaBefore(String upper) {
                return isWindowClauseBoundary(upper);
            }

            @Override
            boolean shouldKeepIndentFor(String upper) {
                return isWindowClauseBoundary(upper);
            }

            @Override
            boolean shouldSkipIndentAfter(String upper) {
                return isWindowClauseBoundary(upper);
            }
        };

        private final int indentReduction;

        ParenBlockType(int indentReduction) {
            this.indentReduction = indentReduction;
        }

        private int indentReduction() {
            return indentReduction;
        }

        boolean shouldForceLineBreakBefore(String upper, String previousUpper) {
            return false;
        }

        boolean shouldSuppressLineBreakBefore(String upper, String previousUpper) {
            return false;
        }

        boolean shouldBreakAfterCommaBefore(String upper) {
            return false;
        }

        boolean shouldKeepIndentFor(String upper) {
            return false;
        }

        boolean shouldSkipIndentAfter(String upper) {
            return false;
        }
    }

    private static String formatTokenValue(Token token, boolean uppercaseKeywords) {
        if (token.type == TokenType.KEYWORD) {
            return uppercaseKeywords
                    ? token.value.toUpperCase(Locale.ROOT)
                    : token.value.toLowerCase(Locale.ROOT);
        }
        if (token.type == TokenType.FUNCTION) {
            return token.value.toLowerCase(Locale.ROOT);
        }
        return token.value;
    }

    private static void appendIndent(StringBuilder result, int indentLevel, String indent) {
        if (indentLevel <= 0) {
            return;
        }
        result.append(indent.repeat(indentLevel));
    }

    private static void validateStatement(List<Token> tokens, List<String> issues, SqlDialect dialect) {
        String statementType = detectStatementType(tokens);
        if (statementType == null) {
            issues.add(I18nUtil.getMessage(MessageKeys.TOOLBOX_SQL_VALIDATION_NO_KEYWORDS));
            return;
        }

        Token lastToken = tokens.get(tokens.size() - 1);
        if (isIncompleteEnding(lastToken)) {
            issues.add(I18nUtil.getMessage(MessageKeys.TOOLBOX_SQL_VALIDATION_INCOMPLETE_STATEMENT, lastToken.value));
        }

        validateKeywordFollower(tokens, "WHERE", issues);
        validateKeywordFollower(tokens, "HAVING", issues);
        validateKeywordFollower(tokens, "ON", issues);
        if (dialect != SqlDialect.ORACLE) {
            validateKeywordFollower(tokens, "LIMIT", issues);
        }
        validateKeywordFollower(tokens, "OFFSET", issues);

        validateKeywordPair(tokens, "GROUP", "BY", issues);
        validateKeywordPair(tokens, "ORDER", "BY", issues);
        validateDialectSpecificKeywords(tokens, issues, dialect);

        switch (statementType) {
            case "SELECT", "WITH" -> validateSelect(tokens, issues, dialect);
            case "INSERT" -> validateInsert(tokens, issues);
            case "UPDATE" -> validateUpdate(tokens, issues);
            case "DELETE" -> validateDelete(tokens, issues);
            case "CREATE", "ALTER", "DROP", "TRUNCATE", "MERGE" -> validateDdl(tokens, issues);
            default -> {
            }
        }
    }

    private static void validateSelect(List<Token> tokens, List<String> issues, SqlDialect dialect) {
        int selectIndex = findTopLevelKeyword(tokens, "SELECT");
        if (selectIndex < 0) {
            return;
        }

        Token afterSelect = nextNonCommentToken(tokens, selectIndex + 1);
        if (dialect == SqlDialect.SQLSERVER && afterSelect != null && "TOP".equals(afterSelect.upperValue())) {
            afterSelect = nextNonCommentToken(tokens, selectIndex + 2);
        }
        if (!isExpressionStart(afterSelect)) {
            issues.add(I18nUtil.getMessage(MessageKeys.TOOLBOX_SQL_VALIDATION_EXPECTED_AFTER, "SELECT"));
        }

        int fromIndex = findTopLevelKeyword(tokens, "FROM");
        if (fromIndex >= 0) {
            Token afterFrom = nextNonCommentToken(tokens, fromIndex + 1);
            if (!isTableStart(afterFrom)) {
                issues.add(I18nUtil.getMessage(MessageKeys.TOOLBOX_SQL_VALIDATION_EXPECTED_AFTER, "FROM"));
            }
        }
    }

    private static void validateInsert(List<Token> tokens, List<String> issues) {
        int insertIndex = findTopLevelKeyword(tokens, "INSERT");
        if (insertIndex < 0) {
            return;
        }

        int intoIndex = findTopLevelKeyword(tokens, "INTO");
        int tableIndex = intoIndex >= 0 ? intoIndex + 1 : insertIndex + 1;
        Token tableToken = nextNonCommentToken(tokens, tableIndex);
        if (!isTableStart(tableToken)) {
            issues.add(I18nUtil.getMessage(MessageKeys.TOOLBOX_SQL_VALIDATION_EXPECTED_AFTER, intoIndex >= 0 ? "INTO" : "INSERT"));
        }

        int valuesIndex = findTopLevelKeyword(tokens, "VALUES");
        int selectIndex = findTopLevelKeyword(tokens, "SELECT");
        if (valuesIndex < 0 && selectIndex < 0) {
            issues.add(I18nUtil.getMessage(MessageKeys.TOOLBOX_SQL_VALIDATION_MISSING_KEYWORD, "VALUES/SELECT"));
            return;
        }

        if (valuesIndex >= 0) {
            Token afterValues = nextNonCommentToken(tokens, valuesIndex + 1);
            if (!isExpressionStart(afterValues)) {
                issues.add(I18nUtil.getMessage(MessageKeys.TOOLBOX_SQL_VALIDATION_EXPECTED_AFTER, "VALUES"));
            }
        }
    }

    private static void validateUpdate(List<Token> tokens, List<String> issues) {
        int updateIndex = findTopLevelKeyword(tokens, "UPDATE");
        if (updateIndex < 0) {
            return;
        }

        Token tableToken = nextNonCommentToken(tokens, updateIndex + 1);
        if (!isTableStart(tableToken)) {
            issues.add(I18nUtil.getMessage(MessageKeys.TOOLBOX_SQL_VALIDATION_EXPECTED_AFTER, "UPDATE"));
        }

        int setIndex = findTopLevelKeyword(tokens, "SET");
        if (setIndex < 0) {
            issues.add(I18nUtil.getMessage(MessageKeys.TOOLBOX_SQL_VALIDATION_MISSING_KEYWORD, "SET"));
            return;
        }

        Token afterSet = nextNonCommentToken(tokens, setIndex + 1);
        if (!isExpressionStart(afterSet)) {
            issues.add(I18nUtil.getMessage(MessageKeys.TOOLBOX_SQL_VALIDATION_EXPECTED_AFTER, "SET"));
        }
    }

    private static void validateDelete(List<Token> tokens, List<String> issues) {
        int deleteIndex = findTopLevelKeyword(tokens, "DELETE");
        if (deleteIndex < 0) {
            return;
        }

        Token afterDelete = nextNonCommentToken(tokens, deleteIndex + 1);
        if (afterDelete == null || !"FROM".equals(afterDelete.upperValue())) {
            issues.add(I18nUtil.getMessage(MessageKeys.TOOLBOX_SQL_VALIDATION_EXPECTED_KEYWORD_AFTER, "FROM", "DELETE"));
            return;
        }

        int fromIndex = findTopLevelKeyword(tokens, "FROM");
        Token afterFrom = nextNonCommentToken(tokens, fromIndex + 1);
        if (!isTableStart(afterFrom)) {
            issues.add(I18nUtil.getMessage(MessageKeys.TOOLBOX_SQL_VALIDATION_EXPECTED_AFTER, "FROM"));
        }
    }

    private static void validateDdl(List<Token> tokens, List<String> issues) {
        if (tokens.size() < 2) {
            issues.add(I18nUtil.getMessage(MessageKeys.TOOLBOX_SQL_VALIDATION_INCOMPLETE_STATEMENT, tokens.get(0).value));
            return;
        }

        Token nextToken = nextNonCommentToken(tokens, 1);
        if (!isExpressionStart(nextToken)) {
            issues.add(I18nUtil.getMessage(MessageKeys.TOOLBOX_SQL_VALIDATION_EXPECTED_AFTER, tokens.get(0).upperValue()));
        }
    }

    private static void validateKeywordFollower(List<Token> tokens, String keyword, List<String> issues) {
        int index = findTopLevelKeyword(tokens, keyword);
        if (index < 0) {
            return;
        }

        Token nextToken = nextNonCommentToken(tokens, index + 1);
        if (!isExpressionStart(nextToken)) {
            issues.add(I18nUtil.getMessage(MessageKeys.TOOLBOX_SQL_VALIDATION_EXPECTED_AFTER, keyword));
        }
    }

    private static void validateKeywordPair(List<Token> tokens, String firstKeyword, String secondKeyword, List<String> issues) {
        int index = findTopLevelKeyword(tokens, firstKeyword);
        if (index < 0) {
            return;
        }

        Token nextToken = nextNonCommentToken(tokens, index + 1);
        if (nextToken == null || !secondKeyword.equals(nextToken.upperValue())) {
            issues.add(I18nUtil.getMessage(MessageKeys.TOOLBOX_SQL_VALIDATION_EXPECTED_KEYWORD_AFTER, secondKeyword, firstKeyword));
            return;
        }

        Token valueToken = nextNonCommentToken(tokens, index + 2);
        if (!isExpressionStart(valueToken)) {
            issues.add(I18nUtil.getMessage(MessageKeys.TOOLBOX_SQL_VALIDATION_EXPECTED_AFTER, firstKeyword + " " + secondKeyword));
        }
    }

    private static String detectStatementType(List<Token> tokens) {
        int parenLevel = 0;
        for (Token token : tokens) {
            if ("(".equals(token.value)) {
                parenLevel++;
                continue;
            }
            if (")".equals(token.value)) {
                parenLevel = Math.max(0, parenLevel - 1);
                continue;
            }
            if (parenLevel > 0 || token.type != TokenType.KEYWORD) {
                continue;
            }

            String upper = token.upperValue();
            if (STATEMENT_KEYWORDS.contains(upper)) {
                return upper;
            }
        }
        return null;
    }

    private static int findTopLevelKeyword(List<Token> tokens, String keyword) {
        int parenLevel = 0;
        for (int i = 0; i < tokens.size(); i++) {
            Token token = tokens.get(i);
            if ("(".equals(token.value)) {
                parenLevel++;
                continue;
            }
            if (")".equals(token.value)) {
                parenLevel = Math.max(0, parenLevel - 1);
                continue;
            }
            if (parenLevel == 0 && token.type == TokenType.KEYWORD && keyword.equals(token.upperValue())) {
                return i;
            }
        }
        return -1;
    }

    private static boolean isExpressionStart(Token token) {
        if (token == null) {
            return false;
        }
        if ("(".equals(token.value) || "?".equals(token.value)) {
            return true;
        }
        if (token.type == TokenType.IDENTIFIER || token.type == TokenType.STRING || token.type == TokenType.NUMBER
                || token.type == TokenType.FUNCTION) {
            return true;
        }
        if (token.type == TokenType.KEYWORD) {
            String upper = token.upperValue();
            return "SELECT".equals(upper) || "CASE".equals(upper) || "EXISTS".equals(upper)
                    || "NOT".equals(upper) || "NULL".equals(upper) || "DISTINCT".equals(upper);
        }
        return false;
    }

    private static boolean isTableStart(Token token) {
        if (token == null) {
            return false;
        }
        if ("(".equals(token.value)) {
            return true;
        }
        if (token.type == TokenType.IDENTIFIER || token.type == TokenType.STRING) {
            return true;
        }
        return token.type == TokenType.KEYWORD && "SELECT".equals(token.upperValue());
    }

    private static boolean isIncompleteEnding(Token token) {
        if (token == null) {
            return false;
        }
        if (token.type == TokenType.KEYWORD) {
            return INCOMPLETE_ENDINGS.contains(token.upperValue());
        }
        return token.type == TokenType.OPERATOR && !")".equals(token.value);
    }

    private static boolean isJoinModifier(String upperKeyword) {
        return "LEFT".equals(upperKeyword) || "RIGHT".equals(upperKeyword)
                || "FULL".equals(upperKeyword) || "INNER".equals(upperKeyword)
                || "OUTER".equals(upperKeyword) || "CROSS".equals(upperKeyword);
    }

    private static boolean isJoinKeyword(String upperKeyword) {
        return "JOIN".equals(upperKeyword) || "STRAIGHT_JOIN".equals(upperKeyword);
    }

    private static boolean isSetOperator(String upperKeyword) {
        return "UNION".equals(upperKeyword) || "INTERSECT".equals(upperKeyword)
                || "EXCEPT".equals(upperKeyword) || "MINUS".equals(upperKeyword);
    }

    private static void validateDialectSpecificKeywords(List<Token> tokens, List<String> issues, SqlDialect dialect) {
        switch (dialect) {
            case SQLSERVER -> validateKeywordFollower(tokens, "TOP", issues);
            case POSTGRESQL, ORACLE -> validateKeywordFollower(tokens, "RETURNING", issues);
            default -> {
            }
        }
    }

    private static boolean supportsHashComments(SqlDialect dialect) {
        return dialect == SqlDialect.MYSQL || dialect == SqlDialect.GENERIC;
    }

    private static boolean supportsDoubleQuotedIdentifiers(SqlDialect dialect) {
        return dialect == SqlDialect.POSTGRESQL
                || dialect == SqlDialect.ORACLE
                || dialect == SqlDialect.SQLSERVER
                || dialect == SqlDialect.GENERIC;
    }

    private static boolean isStatementKeyword(String upperKeyword) {
        return STATEMENT_KEYWORDS.contains(upperKeyword) || CLAUSE_KEYWORDS.contains(upperKeyword);
    }

    private static ParenBlockType activeParenBlockType(List<ParenBlockType> parenBlockStack) {
        for (int i = parenBlockStack.size() - 1; i >= 0; i--) {
            ParenBlockType parenBlockType = parenBlockStack.get(i);
            if (parenBlockType != ParenBlockType.NONE) {
                return parenBlockType;
            }
        }
        return ParenBlockType.NONE;
    }

    private static ParenBlockType detectParenBlockType(Token previousToken, Token nextToken) {
        if (nextToken == null) {
            return ParenBlockType.NONE;
        }

        String nextUpper = nextToken.upperValue();
        if ("SELECT".equals(nextUpper) || "WITH".equals(nextUpper)) {
            return ParenBlockType.SUBQUERY;
        }

        if (previousToken == null || previousToken.type != TokenType.KEYWORD) {
            return ParenBlockType.NONE;
        }

        String previousUpper = previousToken.upperValue();
        if ("FILTER".equals(previousUpper)) {
            return ParenBlockType.FILTER_CLAUSE;
        }
        if ("OVER".equals(previousUpper)) {
            return ParenBlockType.WINDOW_CLAUSE;
        }

        return ParenBlockType.NONE;
    }

    private static boolean isOutputIntoClauseStart(List<Token> tokens, int index) {
        if (index < 0 || index >= tokens.size()) {
            return false;
        }
        Token token = tokens.get(index);
        if (!"INTO".equals(token.upperValue())) {
            return false;
        }

        int parenLevel = 0;
        for (int i = 0; i < index; i++) {
            Token candidate = tokens.get(i);
            if ("(".equals(candidate.value)) {
                parenLevel++;
                continue;
            }
            if (")".equals(candidate.value)) {
                parenLevel = Math.max(0, parenLevel - 1);
                continue;
            }
            if (parenLevel == 0 && candidate.type == TokenType.KEYWORD && "OUTPUT".equals(candidate.upperValue())) {
                return true;
            }
        }
        return false;
    }

    private static boolean isWindowClauseBoundary(String upperKeyword) {
        return "PARTITION".equals(upperKeyword) || "ORDER".equals(upperKeyword)
                || "ROWS".equals(upperKeyword) || "RANGE".equals(upperKeyword)
                || "GROUPS".equals(upperKeyword) || "EXCLUDE".equals(upperKeyword);
    }

    private static boolean isWindowFrameContinuation(String previousUpper) {
        return "BETWEEN".equals(previousUpper) || "PRECEDING".equals(previousUpper)
                || "FOLLOWING".equals(previousUpper) || "CURRENT".equals(previousUpper)
                || "ROW".equals(previousUpper);
    }

    private static boolean isWindowExcludeTarget(String upperKeyword) {
        return "CURRENT".equals(upperKeyword) || "GROUP".equals(upperKeyword)
                || "TIES".equals(upperKeyword) || "OTHERS".equals(upperKeyword);
    }

    private static void trimTrailingSpace(StringBuilder builder) {
        while (builder.length() > 0 && Character.isWhitespace(builder.charAt(builder.length() - 1))) {
            builder.setLength(builder.length() - 1);
        }
    }

    private static Token nextNonCommentToken(List<Token> tokens, int start) {
        for (int i = start; i < tokens.size(); i++) {
            if (tokens.get(i).type != TokenType.COMMENT) {
                return tokens.get(i);
            }
        }
        return null;
    }

    private static Token previousRenderableToken(List<Token> tokens, int start) {
        for (int i = start; i >= 0; i--) {
            if (tokens.get(i).type != TokenType.COMMENT) {
                return tokens.get(i);
            }
        }
        return null;
    }

    private static Token nextRenderableToken(List<Token> tokens, int start) {
        for (int i = start; i < tokens.size(); i++) {
            if (tokens.get(i).type != TokenType.COMMENT) {
                return tokens.get(i);
            }
        }
        return null;
    }

    private static Token parseQuotedToken(String sql, int start, char quote, TokenType tokenType) {
        int pos = start + 1;
        StringBuilder value = new StringBuilder().append(quote);
        boolean terminated = false;

        while (pos < sql.length()) {
            char ch = sql.charAt(pos);
            value.append(ch);
            if (ch == quote) {
                if (quote == '\'' || quote == '"') {
                    if (pos + 1 < sql.length() && sql.charAt(pos + 1) == quote) {
                        value.append(quote);
                        pos += 2;
                        continue;
                    }
                }
                pos++;
                terminated = true;
                break;
            }
            pos++;
        }

        if (!terminated) {
            throw new ValidationException(I18nUtil.getMessage(MessageKeys.TOOLBOX_SQL_VALIDATION_UNTERMINATED_STRING));
        }

        return new Token(tokenType, value.toString(), start, pos);
    }

    private static Token parseBracketIdentifier(String sql, int start) {
        int pos = start + 1;
        while (pos < sql.length() && sql.charAt(pos) != ']') {
            pos++;
        }
        if (pos >= sql.length()) {
            throw new ValidationException(I18nUtil.getMessage(MessageKeys.TOOLBOX_SQL_VALIDATION_UNTERMINATED_STRING));
        }
        pos++;
        return new Token(TokenType.IDENTIFIER, sql.substring(start, pos), start, pos);
    }

    private static Token parseLineComment(String sql, int start) {
        int pos = start + 1;
        if (sql.charAt(start) == '-') {
            pos = start + 2;
        }
        while (pos < sql.length() && sql.charAt(pos) != '\n') {
            pos++;
        }
        return new Token(TokenType.COMMENT, sql.substring(start, pos), start, pos);
    }

    private static Token parseBlockComment(String sql, int start) {
        int pos = start + 2;
        while (pos < sql.length() - 1) {
            if (sql.charAt(pos) == '*' && sql.charAt(pos + 1) == '/') {
                pos += 2;
                return new Token(TokenType.COMMENT, sql.substring(start, pos), start, pos);
            }
            pos++;
        }
        throw new ValidationException(I18nUtil.getMessage(MessageKeys.TOOLBOX_SQL_VALIDATION_UNTERMINATED_COMMENT));
    }

    private static Token parseNumber(String sql, int start) {
        int pos = start;
        while (pos < sql.length()) {
            char ch = sql.charAt(pos);
            if (!Character.isDigit(ch) && ch != '.') {
                break;
            }
            pos++;
        }
        return new Token(TokenType.NUMBER, sql.substring(start, pos), start, pos);
    }

    private static Token parseIdentifier(String sql, int start) {
        int pos = start;
        while (pos < sql.length()) {
            char ch = sql.charAt(pos);
            if (!Character.isLetterOrDigit(ch) && ch != '_' && ch != '$') {
                break;
            }
            pos++;
        }

        String value = sql.substring(start, pos);
        String upper = value.toUpperCase(Locale.ROOT);
        TokenType type = TokenType.IDENTIFIER;
        if (FUNCTIONS.contains(upper)) {
            type = TokenType.FUNCTION;
        } else if (KEYWORDS.contains(upper) || LOGIC_OPERATORS.contains(upper) || CASE_KEYWORDS.contains(upper)) {
            type = TokenType.KEYWORD;
        }
        return new Token(type, value, start, pos);
    }

    private static Token parseNamedParameter(String sql, int start) {
        int pos = start + 1;
        while (pos < sql.length()) {
            char ch = sql.charAt(pos);
            if (!Character.isLetterOrDigit(ch) && ch != '_' && ch != '$') {
                break;
            }
            pos++;
        }
        return new Token(TokenType.IDENTIFIER, sql.substring(start, pos), start, pos);
    }

    private static Token parseOperator(String sql, int start) {
        int pos = start + 1;
        char ch = sql.charAt(start);

        if (pos < sql.length()) {
            char next = sql.charAt(pos);
            if ((ch == '>' || ch == '<' || ch == '!' || ch == '=' || ch == '|' || ch == '&')
                    && (next == '=' || next == '>' || (ch == '<' && next == '>')
                    || (ch == '|' && next == '|') || (ch == '&' && next == '&'))) {
                pos++;
            } else if (ch == ':' && next == ':') {
                pos++;
            }
        }

        return new Token(TokenType.OPERATOR, sql.substring(start, pos), start, pos);
    }

    private enum TokenType {
        KEYWORD, IDENTIFIER, STRING, NUMBER, OPERATOR, COMMENT, FUNCTION
    }

    private static class Token {
        private final TokenType type;
        private final String value;
        private final int startPos;
        private final int endPos;

        private Token(TokenType type, String value, int startPos, int endPos) {
            this.type = type;
            this.value = value;
            this.startPos = startPos;
            this.endPos = endPos;
        }

        private String upperValue() {
            return value.toUpperCase(Locale.ROOT);
        }
    }

    private static class ValidationException extends RuntimeException {
        private ValidationException(String message) {
            super(message);
        }
    }
}
