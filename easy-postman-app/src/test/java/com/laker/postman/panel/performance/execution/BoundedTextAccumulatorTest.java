package com.laker.postman.panel.performance.execution;

import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class BoundedTextAccumulatorTest {

    @Test
    public void shouldRetainOnlyConfiguredPreviewWhileCountingFullUtf8Size() {
        BoundedTextAccumulator accumulator = new BoundedTextAccumulator(16);

        accumulator.append("0123456789");
        accumulator.append("abcdef");
        accumulator.append("XYZ");

        assertEquals(accumulator.totalUtf8Bytes(), 19);
        assertTrue(accumulator.isTruncated());
        assertTrue(accumulator.value().startsWith("0123456789abcdef"));
        assertTrue(accumulator.value().contains("truncated"));
        assertTrue(accumulator.value().length() < 128);
    }

    @Test
    public void shouldCountMultibyteCharactersWithoutAllocatingEncodedCopy() {
        BoundedTextAccumulator accumulator = new BoundedTextAccumulator(8);

        accumulator.append("你好");

        assertEquals(accumulator.totalUtf8Bytes(), 6);
        assertFalse(accumulator.isTruncated());
        assertEquals(accumulator.value(), "你好");
    }

    @Test
    public void shouldLimitRetainedPreviewByUtf8BytesInsteadOfCharacters() {
        BoundedTextAccumulator accumulator = new BoundedTextAccumulator(4);

        accumulator.append("你好abc");

        assertEquals(accumulator.totalUtf8Bytes(), 9);
        assertTrue(accumulator.isTruncated());
        assertTrue(accumulator.value().startsWith("你"));
        assertFalse(accumulator.value().startsWith("你好"));
    }
}
