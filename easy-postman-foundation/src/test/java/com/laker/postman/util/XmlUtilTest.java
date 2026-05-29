package com.laker.postman.util;

import org.testng.annotations.Test;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class XmlUtilTest {

    @Test
    public void shouldRecognizeValidXml() {
        assertTrue(XmlUtil.isXml("<root><item id=\"1\">value</item></root>"));
    }

    @Test
    public void shouldRejectInvalidXml() {
        assertFalse(XmlUtil.isXml("<root><item></root>"));
    }
}
