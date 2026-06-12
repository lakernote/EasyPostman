package com.laker.postman.service.curl;

import org.testng.annotations.Test;

import java.util.List;

import static org.testng.Assert.assertEquals;

public class CurlCommandTokenizerTest {

    @Test(description = "tokenizer preserves whitespace inside quoted arguments")
    public void testTokenizePreservesQuotedWhitespace() {
        String body = "line  one\nline\tthree";

        List<String> tokens = CurlCommandTokenizer.tokenize(
                "curl --data-raw '" + body + "' https://example.com"
        );

        assertEquals(tokens.get(2), body);
    }

    @Test(description = "tokenizer handles Windows CMD caret escapes")
    public void testTokenizeWindowsCmdCaretEscapes() {
        List<String> tokens = CurlCommandTokenizer.tokenize(
                "curl \"https://example.com/api?first=1^&second=2\" -H \"X-Token: a^&b\""
        );

        assertEquals(tokens.get(1), "https://example.com/api?first=1&second=2");
        assertEquals(tokens.get(3), "X-Token: a&b");
    }

    @Test(description = "tokenizer reads exported POSIX single-quote escaping")
    public void testTokenizeExportedSingleQuoteEscaping() {
        String body = "prefix 'quoted' suffix";

        List<String> tokens = CurlCommandTokenizer.tokenize(
                "curl --data " + ShellArgumentEscaper.escape(body) + " https://example.com"
        );

        assertEquals(tokens.get(2), body);
    }

    @Test(description = "double quotes keep Bash backslash semantics instead of ANSI-C decoding")
    public void testDoubleQuotedBackslashNIsLiteral() {
        List<String> tokens = CurlCommandTokenizer.tokenize(
                "curl --data \"{\\\"msg\\\":\\\"line\\nnext\\\"}\" https://example.com"
        );

        assertEquals(tokens.get(2), "{\"msg\":\"line\\nnext\"}");
    }

    @Test(description = "ANSI-C quotes decode escape sequences")
    public void testAnsiCQuoteDecodesEscapes() {
        List<String> tokens = CurlCommandTokenizer.tokenize(
                "curl --data $'line1\\nline2\\tindent' https://example.com"
        );

        assertEquals(tokens.get(2), "line1\nline2\tindent");
    }

    @Test(description = "quoted empty arguments are preserved")
    public void testQuotedEmptyArgumentIsPreserved() {
        List<String> tokens = CurlCommandTokenizer.tokenize(
                "curl --data '' https://example.com"
        );

        assertEquals(tokens.get(2), "");
        assertEquals(tokens.get(3), "https://example.com");
    }
}
