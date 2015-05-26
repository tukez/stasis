package org.stasis;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;

public class StreamEncoder {

    private volatile boolean isOpen = true;

    private void ensureOpen() throws IOException {
        if (!isOpen) {
            throw new IOException("Stream closed");
        }
    }

    public void flushBuffer() throws IOException {
        if (isOpen()) {
            implFlushBuffer();
        } else {
            throw new IOException("Stream closed");
        }
    }

    public void write(char cbuf[], int off, int len) throws IOException {
        ensureOpen();
        if ((off < 0) || (off > cbuf.length) || (len < 0) || ((off + len) > cbuf.length) || ((off + len) < 0)) {
            throw new IndexOutOfBoundsException();
        } else if (len == 0) {
            return;
        }
        implWrite(cbuf, off, len);
    }

    public void flush() throws IOException {
        ensureOpen();
        implFlush();
    }

    public void close() throws IOException {
        if (!isOpen) {
            return;
        }
        implClose();
        isOpen = false;
    }

    private boolean isOpen() {
        return isOpen;
    }

    private final CharsetEncoder encoder;
    private final ByteBuffer bb;

    private final OutputStream out;

    private boolean haveLeftoverChar = false;
    private char leftoverChar;
    private CharBuffer lcb = null;

    public StreamEncoder(OutputStream out, ByteBuffer outBuffer, CharsetEncoder enc) {
        this.out = out;
        this.encoder = enc;
        this.bb = outBuffer;
    }

    private void writeBytes() throws IOException {
        bb.flip();
        int lim = bb.limit();
        int pos = bb.position();
        assert (pos <= lim);
        int rem = (pos <= lim ? lim - pos : 0);

        if (rem > 0) {
            out.write(bb.array(), bb.arrayOffset() + pos, rem);
        }
        bb.clear();
    }

    private void flushLeftoverChar(CharBuffer cb, boolean endOfInput) throws IOException {
        if (!haveLeftoverChar && !endOfInput) {
            return;
        }
        if (lcb == null) {
            lcb = CharBuffer.allocate(2);
        } else {
            lcb.clear();
        }
        if (haveLeftoverChar) {
            lcb.put(leftoverChar);
        }
        if ((cb != null) && cb.hasRemaining()) {
            lcb.put(cb.get());
        }
        lcb.flip();
        while (lcb.hasRemaining() || endOfInput) {
            CoderResult cr = encoder.encode(lcb, bb, endOfInput);
            if (cr.isUnderflow()) {
                if (lcb.hasRemaining()) {
                    leftoverChar = lcb.get();
                    if (cb != null && cb.hasRemaining()) {
                        flushLeftoverChar(cb, endOfInput);
                    }
                    return;
                }
                break;
            }
            if (cr.isOverflow()) {
                assert bb.position() > 0;
                writeBytes();
                continue;
            }
            cr.throwException();
        }
        haveLeftoverChar = false;
    }

    void implWrite(char cbuf[], int off, int len) throws IOException {
        CharBuffer cb = CharBuffer.wrap(cbuf, off, len);

        if (haveLeftoverChar) {
            flushLeftoverChar(cb, false);
        }

        while (cb.hasRemaining()) {
            CoderResult cr = encoder.encode(cb, bb, false);
            if (cr.isUnderflow()) {
                assert (cb.remaining() <= 1) : cb.remaining();
                if (cb.remaining() == 1) {
                    haveLeftoverChar = true;
                    leftoverChar = cb.get();
                }
                break;
            }
            if (cr.isOverflow()) {
                assert bb.position() > 0;
                writeBytes();
                continue;
            }
            cr.throwException();
        }
    }

    void implFlushBuffer() throws IOException {
        if (bb.position() > 0) {
            writeBytes();
        }
    }

    void implFlush() throws IOException {
        implFlushBuffer();
        out.flush();
    }

    void implClose() throws IOException {
        flushLeftoverChar(null, true);
        try {
            for (;;) {
                CoderResult cr = encoder.flush(bb);
                if (cr.isUnderflow()) {
                    break;
                }
                if (cr.isOverflow()) {
                    assert bb.position() > 0;
                    writeBytes();
                    continue;
                }
                cr.throwException();
            }

            if (bb.position() > 0) {
                writeBytes();
            }
            out.close();
        } catch (IOException x) {
            encoder.reset();
            throw x;
        }
    }

}
