package com.laker.postman.util;

import org.testng.annotations.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNotEquals;

public class EditorSyntaxThemeResourceTest {

    @Test
    public void lightThemeShouldUseBalancedJsonSyntaxColors() throws Exception {
        SyntaxTheme theme = loadTheme("/themes/easypostman-light.xml");

        assertEquals(theme.color("currentLineHighlight"), "F5F7FA");
        assertEquals(theme.color("gutterBorder"), "E5E7EB");
        assertEquals(theme.style("lineNumbers").get("fg"), "8A8F98");

        assertToken(theme, "VARIABLE", "6F2A8E", false);
        assertToken(theme, "LITERAL_STRING_DOUBLE_QUOTE", "067D17", false);
        assertToken(theme, "LITERAL_NUMBER_DECIMAL_INT", "1750EB", false);
        assertToken(theme, "LITERAL_NUMBER_FLOAT", "1750EB", false);
        assertToken(theme, "LITERAL_BOOLEAN", "B45309", false);
        assertToken(theme, "RESERVED_WORD", "5B3FD6", false);
        assertToken(theme, "SEPARATOR", "1F2937", false);
        assertToken(theme, "OPERATOR", "374151", false);
        assertDistinctJsonLiteralColors(theme);
    }

    @Test
    public void darkThemeShouldKeepReadableJsonSyntaxWithoutExtraWeight() throws Exception {
        SyntaxTheme theme = loadTheme("/themes/easypostman-dark.xml");

        assertEquals(theme.color("background"), "1E1F22");
        assertEquals(theme.color("currentLineHighlight"), "25282D");

        assertToken(theme, "VARIABLE", "C77DBB", false);
        assertToken(theme, "LITERAL_STRING_DOUBLE_QUOTE", "6AAB73", false);
        assertToken(theme, "LITERAL_NUMBER_DECIMAL_INT", "2AACB8", false);
        assertToken(theme, "LITERAL_NUMBER_FLOAT", "2AACB8", false);
        assertToken(theme, "LITERAL_BOOLEAN", "CF8E6D", false);
        assertToken(theme, "RESERVED_WORD", "B392F0", false);
        assertDistinctJsonLiteralColors(theme);
    }

    private static void assertToken(SyntaxTheme theme, String token, String expectedColor, boolean expectedBold) {
        Map<String, String> style = theme.tokenStyle(token);
        assertEquals(style.get("fg"), expectedColor, token + " foreground");
        if (expectedBold) {
            assertEquals(style.get("bold"), "true", token + " bold");
        } else {
            assertFalse(Boolean.parseBoolean(style.getOrDefault("bold", "false")), token + " should not be bold");
        }
    }

    private static void assertDistinctJsonLiteralColors(SyntaxTheme theme) {
        String number = theme.tokenStyle("LITERAL_NUMBER_DECIMAL_INT").get("fg");
        String bool = theme.tokenStyle("LITERAL_BOOLEAN").get("fg");
        String keywordOrNull = theme.tokenStyle("RESERVED_WORD").get("fg");

        assertNotEquals(bool, number, "JSON boolean should not read as number");
        assertNotEquals(keywordOrNull, number, "JSON null/keyword should not read as number");
        assertNotEquals(keywordOrNull, bool, "JSON null/keyword should not read as boolean");
    }

    private static SyntaxTheme loadTheme(String resourcePath) throws Exception {
        try (InputStream stream = EditorThemeUtil.class.getResourceAsStream(resourcePath)) {
            assertNotNull(stream, "Missing editor theme resource: " + resourcePath);

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setExpandEntityReferences(false);
            DocumentBuilder builder = factory.newDocumentBuilder();
            builder.setEntityResolver((publicId, systemId) -> new InputSource(new StringReader("")));
            Document document = builder.parse(new InputSource(stream));
            document.getDocumentElement().normalize();
            return new SyntaxTheme(document);
        }
    }

    private record SyntaxTheme(Document document) {
        String color(String elementName) {
            return style(elementName).get("color");
        }

        Map<String, String> tokenStyle(String token) {
            Element root = document.getDocumentElement();
            for (int i = 0; i < root.getElementsByTagName("style").getLength(); i++) {
                Element style = (Element) root.getElementsByTagName("style").item(i);
                if (token.equals(style.getAttribute("token"))) {
                    return attributes(style);
                }
            }
            throw new AssertionError("Missing token style: " + token);
        }

        Map<String, String> style(String elementName) {
            Element element = (Element) document.getElementsByTagName(elementName).item(0);
            if (element == null) {
                throw new AssertionError("Missing editor theme element: " + elementName);
            }
            return attributes(element);
        }

        private static Map<String, String> attributes(Element element) {
            Map<String, String> values = new HashMap<>();
            for (int i = 0; i < element.getAttributes().getLength(); i++) {
                values.put(
                        element.getAttributes().item(i).getNodeName(),
                        element.getAttributes().item(i).getNodeValue()
                );
            }
            return values;
        }
    }
}
