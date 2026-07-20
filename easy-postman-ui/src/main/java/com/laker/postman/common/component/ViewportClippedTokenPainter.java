package com.laker.postman.common.component;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.Token;

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
    boolean requiresCustomPainting(Token token, RSyntaxTextArea host) {
        return token.length() > LONG_TOKEN_THRESHOLD || super.requiresCustomPainting(token, host);
    }

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
        int preferredEnd = Math.min(chunkStart + LONG_TOKEN_CHUNK_SIZE, tokenEnd);
        if (preferredEnd == tokenEnd) {
            return tokenEnd;
        }

        int clusterStart = chunkStart;
        while (clusterStart < preferredEnd) {
            int clusterEnd = FallbackFontTextRenderer.nextClusterEnd(text, clusterStart, tokenEnd);
            if (clusterEnd > preferredEnd) {
                break;
            }
            clusterStart = clusterEnd;
        }

        // A single grapheme can be longer than the target chunk size. Keep it intact even then.
        return clusterStart > chunkStart
                ? clusterStart
                : FallbackFontTextRenderer.nextClusterEnd(text, chunkStart, tokenEnd);
    }
}
