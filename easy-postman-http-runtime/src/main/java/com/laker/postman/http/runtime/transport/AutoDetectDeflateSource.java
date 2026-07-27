package com.laker.postman.http.runtime.transport;

import okio.Buffer;
import okio.BufferedSource;
import okio.InflaterSource;
import okio.Source;
import okio.Timeout;

import java.io.EOFException;
import java.io.IOException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

/**
 * Lazily detects whether an HTTP {@code deflate} response uses the RFC 1950
 * zlib wrapper or a raw RFC 1951 DEFLATE stream.
 */
final class AutoDetectDeflateSource implements Source {

    private static final int ZLIB_HEADER_LENGTH = 2;
    private static final int ZLIB_COMPRESSION_METHOD_MASK = 0x0f;
    private static final int ZLIB_COMPRESSION_INFO_SHIFT = 4;
    private static final int ZLIB_MAX_COMPRESSION_INFO = 7;
    private static final int ZLIB_HEADER_CHECK_DIVISOR = 31;

    private final BufferedSource compressedSource;
    private InflaterSource inflaterSource;

    AutoDetectDeflateSource(BufferedSource compressedSource) {
        this.compressedSource = compressedSource;
    }

    @Override
    public long read(Buffer sink, long byteCount) throws IOException {
        return inflaterSource().read(sink, byteCount);
    }

    @Override
    public Timeout timeout() {
        return compressedSource.timeout();
    }

    @Override
    public void close() throws IOException {
        if (inflaterSource != null) {
            inflaterSource.close();
        } else {
            compressedSource.close();
        }
    }

    private InflaterSource inflaterSource() throws IOException {
        if (inflaterSource == null) {
            boolean rawDeflate = !hasZlibHeader();
            inflaterSource = new InflaterSource(compressedSource, new Inflater(rawDeflate));
        }
        return inflaterSource;
    }

    private boolean hasZlibHeader() throws IOException {
        if (!compressedSource.request(ZLIB_HEADER_LENGTH)) {
            throw new EOFException("Unexpected end of deflate stream");
        }

        int compressionMethodAndInfo = compressedSource.getBuffer().getByte(0) & 0xff;
        int flags = compressedSource.getBuffer().getByte(1) & 0xff;
        int compressionMethod = compressionMethodAndInfo & ZLIB_COMPRESSION_METHOD_MASK;
        int compressionInfo = compressionMethodAndInfo >> ZLIB_COMPRESSION_INFO_SHIFT;
        int header = (compressionMethodAndInfo << Byte.SIZE) | flags;

        return compressionMethod == Deflater.DEFLATED
                && compressionInfo <= ZLIB_MAX_COMPRESSION_INFO
                && header % ZLIB_HEADER_CHECK_DIVISOR == 0;
    }
}
