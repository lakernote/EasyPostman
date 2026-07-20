package com.laker.postman.common.component;

/**
 * Standard fallback-aware editor painter with sub-token viewport clipping for very long tokens.
 *
 * <p>Use this specialization for editors that commonly display large JSON strings, base64, model
 * output, or compressed payloads. Ordinary editors should use {@link StandardEditorTokenPainter}.</p>
 */
public class ViewportClippedTokenPainter extends StandardEditorTokenPainter {

    private static final int LONG_TOKEN_THRESHOLD = 512;
    private static final int LONG_TOKEN_CHUNK_SIZE = 256;

    @Override
    float paintToken(TokenPaintContext context) {
        if (context.end() - context.start() <= LONG_TOKEN_THRESHOLD) {
            return super.paintToken(context);
        }

        float currentX = context.x();
        for (int chunkStart = context.start(); chunkStart < context.end(); ) {
            int chunkEnd = resolveChunkEnd(context.text(), chunkStart, context.end());
            float nextX = measureRange(context, chunkStart, chunkEnd, currentX);

            // The caller still needs the token's full end position for subsequent tokens.
            if (currentX > context.clipRange().right()) {
                return measureRange(context, chunkStart, context.end(), currentX);
            }

            if (isRangeVisible(currentX, nextX, context.clipRange())) {
                paintRange(context, chunkStart, chunkEnd, currentX);
            }
            currentX = nextX;
            chunkStart = chunkEnd;
        }
        return currentX;
    }

    private int resolveChunkEnd(char[] text, int chunkStart, int tokenEnd) {
        int chunkEnd = Math.min(chunkStart + LONG_TOKEN_CHUNK_SIZE, tokenEnd);
        if (chunkEnd < tokenEnd
                && Character.isHighSurrogate(text[chunkEnd - 1])
                && Character.isLowSurrogate(text[chunkEnd])) {
            chunkEnd--;
        }
        return chunkEnd;
    }
}
