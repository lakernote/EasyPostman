package com.laker.postman.util;

import lombok.extern.slf4j.Slf4j;

import java.util.*;

/**
 * SQL 格式化工具类
 * 参考 Druid SQLFormatter 实现，提供专业的 SQL 美化和压缩功能
 *
 * @author laker
 */
@Slf4j
public class SqlFormatter {

    // SQL 关键字集合
    private static final Set<String> KEYWORDS = new HashSet<>();
    private static final Set<String> LOGIC_OPERATORS = new HashSet<>();
    private static final Set<String> FUNCTIONS = new HashSet<>();
    private static final Set<String> CASE_KEYWORDS = new HashSet<>();

    static {
        // DML 关键字
        KEYWORDS.addAll(Arrays.asList(
                "SELECT", "FROM", "WHERE", "INSERT", "INTO", "VALUES", "UPDATE", "SET",
                "DELETE", "MERGE", "TRUNCATE"
        ));

        // DDL 关键字
        KEYWORDS.addAll(Arrays.asList(
                "CREATE", "ALTER", "DROP", "TABLE", "INDEX", "VIEW", "DATABASE", "SCHEMA",
                "CONSTRAINT", "PRIMARY", "FOREIGN", "KEY", "REFERENCES", "UNIQUE", "CHECK"
        ));

        // JOIN 关键字
        KEYWORDS.addAll(Arrays.asList(
                "JOIN", "INNER", "LEFT", "RIGHT", "FULL", "OUTER", "CROSS", "ON", "USING"
        ));

        // 子句关键字
        KEYWORDS.addAll(Arrays.asList(
                "GROUP", "BY", "HAVING", "ORDER", "LIMIT", "OFFSET", "UNION", "ALL",
                "INTERSECT", "EXCEPT", "MINUS", "DISTINCT", "AS", "WITH", "RECURSIVE"
        ));

        // 逻辑运算符
        LOGIC_OPERATORS.addAll(Arrays.asList(
                "AND", "OR", "NOT", "IN", "EXISTS", "BETWEEN", "LIKE", "IS", "NULL"
        ));

        // CASE 表达式关键字
        CASE_KEYWORDS.addAll(Arrays.asList(
                "CASE", "WHEN", "THEN", "ELSE", "END"
        ));

        // 聚合函数
        FUNCTIONS.addAll(Arrays.asList(
                "COUNT", "SUM", "AVG", "MAX", "MIN", "FIRST", "LAST",
                "UPPER", "LOWER", "SUBSTR", "CONCAT", "LENGTH", "TRIM",
                "NOW", "DATE", "TIME", "YEAR", "MONTH", "DAY"
        ));
    }

    /**
     * 格式化选项
     */
    public static class FormatOption {
        private String indent = "  ";           // 缩进字符串
        private boolean uppercaseKeywords = true;  // 关键字大写
        private boolean addSemicolon = true;       // 添加分号
        private boolean lineBreakBeforeFrom = true;
        private boolean lineBreakBeforeJoin = true;
        private boolean lineBreakBeforeWhere = true;
        private boolean lineBreakBeforeAnd = true;
        private boolean lineBreakBeforeOr = true;
        private boolean lineBreakAfterComma = true;

        public FormatOption() {
        }

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
    }

    /**
     * 格式化 SQL（标准模式）
     */
    public static String format(String sql) {
        return format(sql, new FormatOption());
    }

    /**
     * 格式化 SQL（自定义选项）
     */
    public static String format(String sql, FormatOption option) {
        if (sql == null || sql.trim().isEmpty()) {
            return sql;
        }

        try {
            // 1. 预处理：移除多余空白，保留字符串和注释
            sql = preprocessSql(sql);

            // 2. 词法分析：分解为 token
            List<Token> tokens = tokenize(sql);

            // 3. 格式化：根据规则重新组织
            String formatted = formatTokens(tokens, option);

            // 4. 后处理：清理多余空行等
            formatted = postprocessSql(formatted);

            // 5. 添加分号
            if (option.addSemicolon && !formatted.trim().endsWith(";")) {
                formatted = formatted.trim() + ";";
            }

            return formatted;
        } catch (Exception e) {
            log.error("SQL format error", e);
            return sql; // 格式化失败时返回原始 SQL
        }
    }


    /**
     * 压缩 SQL（移除多余空白）
     */
    public static String compress(String sql) {
        if (sql == null || sql.trim().isEmpty()) {
            return sql;
        }

        try {
            // 移除注释
            sql = removeComments(sql);

            // 移除多余空白（保留字符串内的空白）
            sql = compressWhitespace(sql);

            return sql.trim();
        } catch (Exception e) {
            log.error("SQL compress error", e);
            return sql;
        }
    }

    /**
     * 转换关键字大小写
     */
    public static String convertKeywords(String sql, boolean toUppercase) {
        if (sql == null || sql.trim().isEmpty()) {
            return sql;
        }

        List<Token> tokens = tokenize(sql);
        StringBuilder result = new StringBuilder();

        for (Token token : tokens) {
            if (token.type == TokenType.KEYWORD) {
                result.append(toUppercase ? token.value.toUpperCase() : token.value.toLowerCase());
            } else {
                result.append(token.value);
            }
            if (token.afterSpace) {
                result.append(" ");
            }
        }

        return result.toString();
    }

    // ==================== 内部方法 ====================

    /**
     * 预处理 SQL
     */
    private static String preprocessSql(String sql) {
        // 标准化换行符
        sql = sql.replaceAll("\\r\\n", "\n").replaceAll("\\r", "\n");

        // 移除多余空白（非字符串内）
        sql = sql.replaceAll("[ \\t]+", " ");

        return sql.trim();
    }

    /**
     * 后处理 SQL
     */
    private static String postprocessSql(String sql) {
        // 移除多余空行
        sql = sql.replaceAll("\\n{3,}", "\n\n");

        // 清理每行末尾的空格
        sql = sql.replaceAll("[ \\t]+\\n", "\n");

        return sql.trim();
    }

    /**
     * 词法分析：将 SQL 分解为 token
     */
    private static List<Token> tokenize(String sql) {
        List<Token> tokens = new ArrayList<>();
        int pos = 0;
        int len = sql.length();

        while (pos < len) {
            char ch = sql.charAt(pos);

            // 跳过空白
            if (Character.isWhitespace(ch)) {
                pos++;
                continue;
            }

            // 字符串字面量
            if (ch == '\'' || ch == '"') {
                tokens.add(parseString(sql, pos));
                pos = tokens.get(tokens.size() - 1).endPos;
                continue;
            }

            // 单行注释
            if (ch == '-' && pos + 1 < len && sql.charAt(pos + 1) == '-') {
                tokens.add(parseLineComment(sql, pos));
                pos = tokens.get(tokens.size() - 1).endPos;
                continue;
            }

            // 多行注释
            if (ch == '/' && pos + 1 < len && sql.charAt(pos + 1) == '*') {
                tokens.add(parseBlockComment(sql, pos));
                pos = tokens.get(tokens.size() - 1).endPos;
                continue;
            }

            // 数字
            if (Character.isDigit(ch)) {
                tokens.add(parseNumber(sql, pos));
                pos = tokens.get(tokens.size() - 1).endPos;
                continue;
            }

            // 标识符或关键字
            if (Character.isLetter(ch) || ch == '_') {
                tokens.add(parseIdentifier(sql, pos));
                pos = tokens.get(tokens.size() - 1).endPos;
                continue;
            }

            // 操作符和特殊符号
            tokens.add(parseOperator(sql, pos));
            pos = tokens.get(tokens.size() - 1).endPos;
        }

        return tokens;
    }

    /**
     * 格式化 token 列表（改进版，更符合 Druid 标准）
     */
    private static String formatTokens(List<Token> tokens, FormatOption option) {
        StringBuilder result = new StringBuilder();
        int indentLevel = 0;
        boolean needIndent = false;
        int parenLevel = 0; // 括号嵌套级别

        for (int i = 0; i < tokens.size(); i++) {
            Token token = tokens.get(i);
            Token prevToken = i > 0 ? tokens.get(i - 1) : null;
            Token nextToken = i < tokens.size() - 1 ? tokens.get(i + 1) : null;

            String upper = token.value.toUpperCase();

            // 处理左括号
            if ("(".equals(token.value)) {
                parenLevel++;
                result.append(token.value);
                // 括号后可能需要换行（子查询）
                if (nextToken != null && nextToken.type == TokenType.KEYWORD
                        && "SELECT".equals(nextToken.value.toUpperCase())) {
                    result.append("\n");
                    indentLevel++;
                    needIndent = true;
                }
                continue;
            }

            // 处理右括号
            if (")".equals(token.value)) {
                parenLevel--;
                // 如果是子查询的结束，减少缩进
                if (prevToken != null && prevToken.type == TokenType.KEYWORD) {
                    indentLevel = Math.max(0, indentLevel - 1);
                    result.append("\n").append(option.indent.repeat(indentLevel));
                }
                result.append(token.value);
                continue;
            }

            // 决定是否需要换行
            boolean needLineBreak = needsLineBreakBefore(token, prevToken, option);

            if (needLineBreak) {
                result.append("\n");
                needIndent = true;
            }

            // 添加缩进
            if (needIndent) {
                result.append(option.indent.repeat(indentLevel));
                needIndent = false;
            }

            // 输出 token
            String value = token.value;
            if (token.type == TokenType.KEYWORD && option.uppercaseKeywords) {
                value = value.toUpperCase();
            }
            result.append(value);

            // 主要子句后增加缩进（Druid 风格）
            if (token.type == TokenType.KEYWORD) {
                if ("SELECT".equals(upper) || "FROM".equals(upper) ||
                        "WHERE".equals(upper) || "SET".equals(upper)) {
                    if (parenLevel == 0) { // 不在括号内
                        indentLevel++;
                    }
                }
            }

            // 决定是否需要添加空格
            if (needsSpaceAfter(token, nextToken, option)) {
                result.append(" ");
            }

            // 逗号后换行并保持缩进
            if (needsLineBreakAfter(token, nextToken, option)) {
                result.append("\n");
                needIndent = true;
            }

            // 某些关键字后恢复缩进（Druid 风格）
            if (token.type == TokenType.KEYWORD) {
                if (nextToken != null && needsLineBreakBefore(nextToken, token, option)) {
                    indentLevel = Math.max(0, indentLevel - 1);
                }
            }
        }

        return result.toString();
    }

    /**
     * 判断 token 前是否需要换行
     */
    private static boolean needsLineBreakBefore(Token token, Token prevToken, FormatOption option) {
        if (prevToken == null) return false;

        String upper = token.value.toUpperCase();
        String prevUpper = prevToken.value.toUpperCase();

        // 如果前一个词是 LEFT/RIGHT/FULL/INNER/OUTER/CROSS，当前词是 JOIN，不换行
        if ("JOIN".equals(upper)) {
            if ("LEFT".equals(prevUpper) || "RIGHT".equals(prevUpper) ||
                    "FULL".equals(prevUpper) || "INNER".equals(prevUpper) ||
                    "OUTER".equals(prevUpper) || "CROSS".equals(prevUpper)) {
                return false; // 保持 LEFT JOIN 在同一行
            }
        }

        // 主要子句前换行
        if (option.lineBreakBeforeFrom && "FROM".equals(upper)) return true;

        // JOIN 前换行（但要排除上面的组合情况）
        if (option.lineBreakBeforeJoin && "JOIN".equals(upper)) return true;

        // LEFT/RIGHT/FULL/INNER 等修饰词前换行（它们通常在 JOIN 前）
        if (option.lineBreakBeforeJoin &&
                ("LEFT".equals(upper) || "RIGHT".equals(upper) ||
                        "FULL".equals(upper) || "INNER".equals(upper))) {
            return true;
        }

        if (option.lineBreakBeforeWhere && "WHERE".equals(upper)) return true;
        if ("GROUP".equals(upper) || "ORDER".equals(upper) || "HAVING".equals(upper)) return true;
        if ("LIMIT".equals(upper) || "OFFSET".equals(upper)) return true;
        if ("UNION".equals(upper) || "INTERSECT".equals(upper) || "EXCEPT".equals(upper)) return true;

        // SET 子句换行 (UPDATE ... SET)
        if ("SET".equals(upper)) return true;

        // VALUES 子句换行 (INSERT INTO ... VALUES)
        if ("VALUES".equals(upper)) return true;

        // AND/OR 前换行
        if (option.lineBreakBeforeAnd && "AND".equals(upper)) return true;
        if (option.lineBreakBeforeOr && "OR".equals(upper)) return true;

        return false;
    }

    /**
     * 判断 token 后是否需要换行
     */
    private static boolean needsLineBreakAfter(Token token, Token nextToken, FormatOption option) {
        if (nextToken == null) return false;

        // 逗号后换行（在 SELECT 列表等）
        if (option.lineBreakAfterComma && ",".equals(token.value)) {
            String nextUpper = nextToken.value.toUpperCase();
            // 不在 VALUES() 或函数参数中换行
            return !("FROM".equals(nextUpper) || "WHERE".equals(nextUpper) || ")".equals(nextToken.value));
        }

        return false;
    }

    /**
     * 判断 token 后是否需要空格
     */
    private static boolean needsSpaceAfter(Token token, Token nextToken, FormatOption option) {
        if (nextToken == null) return false;

        // 关键字后需要空格（除非下一个是操作符）
        if (token.type == TokenType.KEYWORD) {
            // 特殊关键字后不需要空格的情况
            if (nextToken.type == TokenType.OPERATOR) {
                // 只有左括号例外
                return "(".equals(nextToken.value);
            }
            return true;
        }

        // 标识符后需要空格
        if (token.type == TokenType.IDENTIFIER) {
            if (nextToken.type == TokenType.OPERATOR) {
                // 操作符前需要空格，除了左括号和点号
                return !"(".equals(nextToken.value) && !".".equals(nextToken.value);
            }
            return true;
        }

        // 操作符后需要空格
        if (token.type == TokenType.OPERATOR) {
            String op = token.value;

            // 这些操作符后面不需要空格
            if ("(".equals(op) || ".".equals(op)) {
                return false;
            }

            // 逗号后如果换行了就不需要空格
            if (",".equals(op) && option.lineBreakAfterComma) {
                return false;
            }

            // 其他操作符后需要空格（比如 =, >, <, >=, <=, !=）
            return true;
        }

        // 数字和字符串后，如果下一个是操作符，需要空格
        if (token.type == TokenType.NUMBER || token.type == TokenType.STRING) {
            if (nextToken.type == TokenType.OPERATOR) {
                return !")".equals(nextToken.value) && !",".equals(nextToken.value);
            }
            return true;
        }

        return false;
    }


    // ==================== Token 解析方法 ====================

    private static Token parseString(String sql, int start) {
        char quote = sql.charAt(start);
        int pos = start + 1;
        StringBuilder value = new StringBuilder().append(quote);

        while (pos < sql.length()) {
            char ch = sql.charAt(pos);
            value.append(ch);
            if (ch == quote) {
                // 检查是否是转义的引号
                if (pos + 1 < sql.length() && sql.charAt(pos + 1) == quote) {
                    value.append(quote);
                    pos += 2;
                    continue;
                }
                pos++;
                break;
            }
            pos++;
        }

        return new Token(TokenType.STRING, value.toString(), start, pos);
    }

    private static Token parseLineComment(String sql, int start) {
        int pos = start + 2;
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
                break;
            }
            pos++;
        }
        return new Token(TokenType.COMMENT, sql.substring(start, pos), start, pos);
    }

    private static Token parseNumber(String sql, int start) {
        int pos = start;
        while (pos < sql.length() && (Character.isDigit(sql.charAt(pos)) || sql.charAt(pos) == '.')) {
            pos++;
        }
        return new Token(TokenType.NUMBER, sql.substring(start, pos), start, pos);
    }

    private static Token parseIdentifier(String sql, int start) {
        int pos = start;
        while (pos < sql.length()) {
            char ch = sql.charAt(pos);
            if (!Character.isLetterOrDigit(ch) && ch != '_') {
                break;
            }
            pos++;
        }

        String value = sql.substring(start, pos);
        String upper = value.toUpperCase();

        // 判断是否为关键字
        TokenType type = TokenType.IDENTIFIER;
        if (KEYWORDS.contains(upper) || LOGIC_OPERATORS.contains(upper) ||
                FUNCTIONS.contains(upper) || CASE_KEYWORDS.contains(upper)) {
            type = TokenType.KEYWORD;
        }

        return new Token(type, value, start, pos);
    }

    private static Token parseOperator(String sql, int start) {
        char ch = sql.charAt(start);
        int pos = start + 1;

        // 处理多字符操作符
        if (pos < sql.length()) {
            char nextCh = sql.charAt(pos);
            // >=, <=, !=, <>, ||, &&
            if ((ch == '>' || ch == '<' || ch == '!' || ch == '|' || ch == '&') &&
                    (nextCh == '=' || nextCh == '>' || (ch == '<' && nextCh == '>') ||
                            (ch == '|' && nextCh == '|') || (ch == '&' && nextCh == '&'))) {
                pos++;
            }
        }

        String value = sql.substring(start, pos);
        return new Token(TokenType.OPERATOR, value, start, pos);
    }

    /**
     * 移除注释
     */
    private static String removeComments(String sql) {
        // 移除单行注释
        sql = sql.replaceAll("--[^\n]*", "");
        // 移除多行注释
        sql = sql.replaceAll("/\\*.*?\\*/", "");
        return sql;
    }

    /**
     * 压缩空白字符
     */
    private static String compressWhitespace(String sql) {
        StringBuilder result = new StringBuilder();
        boolean inString = false;
        char stringChar = 0;
        boolean lastWasSpace = false;

        for (int i = 0; i < sql.length(); i++) {
            char ch = sql.charAt(i);

            // 处理字符串
            if ((ch == '\'' || ch == '"') && (i == 0 || sql.charAt(i - 1) != '\\')) {
                if (!inString) {
                    inString = true;
                    stringChar = ch;
                } else if (ch == stringChar) {
                    inString = false;
                }
                result.append(ch);
                lastWasSpace = false;
                continue;
            }

            // 字符串内保留所有字符
            if (inString) {
                result.append(ch);
                continue;
            }

            // 压缩空白
            if (Character.isWhitespace(ch)) {
                if (!lastWasSpace) {
                    result.append(' ');
                    lastWasSpace = true;
                }
            } else {
                result.append(ch);
                lastWasSpace = false;
            }
        }

        return result.toString();
    }

    // ==================== Token 类 ====================

    private enum TokenType {
        KEYWORD, IDENTIFIER, STRING, NUMBER, OPERATOR, COMMENT
    }

    private static class Token {
        TokenType type;
        String value;
        int startPos;
        int endPos;
        boolean afterSpace;

        Token(TokenType type, String value, int startPos, int endPos) {
            this.type = type;
            this.value = value;
            this.startPos = startPos;
            this.endPos = endPos;
        }
    }
}

