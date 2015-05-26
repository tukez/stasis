package org.stasis;

import java.io.DataInput;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CoderResult;

public class StreamDecoder {

    private boolean haveLeftoverChar = false;
    private char leftoverChar;

    private int read0() throws IOException {
        // Return the leftover char, if there is one
        if (haveLeftoverChar) {
            haveLeftoverChar = false;
            return leftoverChar;
        }

        // Convert more bytes
        char cb[] = new char[2];
        int n = read(cb, 0, 2);
        switch (n) {
        case -1:
            return -1;
        case 2:
            leftoverChar = cb[1];
            haveLeftoverChar = true;
            // FALL THROUGH
        case 1:
            return cb[0];
        default:
            assert false : n;
            return -1;
        }
    }

    public int read(char cbuf[], int offset, int length) throws IOException {
        int off = offset;
        int len = length;

        if ((off < 0) || (off > cbuf.length) || (len < 0) || ((off + len) > cbuf.length) || ((off + len) < 0)) {
            throw new IndexOutOfBoundsException();
        }
        if (len == 0) {
            return 0;
        }

        int n = 0;

        if (haveLeftoverChar) {
            // Copy the leftover char into the buffer
            cbuf[off] = leftoverChar;
            off++;
            len--;
            haveLeftoverChar = false;
            n = 1;
            if ((len == 0) || !implReady()) {
                // Return now if this is all we can produce w/o blocking
                return n;
            }
        }

        if (len == 1) {
            // Treat single-character array reads just like read()
            int c = read0();
            if (c == -1) {
                return (n == 0) ? -1 : n;
            }
            cbuf[off] = (char) c;
            return n + 1;
        }

        return n + implRead(cbuf, off, off + len);
    }

    private final DataInput in;
    private final int inLimit;
    private int inRead;

    private final ByteBuffer bb;

    private final CharsetDecoder decoder;

    public StreamDecoder(DataInput in, int inLimit, ByteBuffer inBuffer, CharsetDecoder dec) {
        this.in = in;
        this.inLimit = inLimit;
        this.inRead = 0;
        this.bb = inBuffer;
        this.decoder = dec;
        bb.flip();
    }

    private int readBytes() throws IOException {
        bb.compact();
        try {
            // Read from the input stream, and then update the buffer
            int lim = bb.limit();
            int pos = bb.position();
            assert (pos <= lim);
            int rem = (pos <= lim ? lim - pos : 0);
            assert rem > 0;
            rem = Math.min(rem, inLimit - inRead);
            in.readFully(bb.array(), bb.arrayOffset() + pos, rem);
            int n = rem;
            if (n < 0) {
                return n;
            }
            if (n == 0) {
                throw new IOException("Underlying input stream returned zero bytes");
            }
            assert (n <= rem) : "n = " + n + ", rem = " + rem;
            inRead += n;
            bb.position(pos + n);
        } finally {
            // Flip even when an IOException is thrown,
            // otherwise the stream will stutter
            bb.flip();
        }

        int rem = bb.remaining();
        assert (rem != 0) : rem;
        return rem;
    }

    private int implRead(char[] cbuf, int off, int end) throws IOException {

        // In order to handle surrogate pairs, this method requires that
        // the invoker attempt to read at least two characters. Saving the
        // extra character, if any, at a higher level is easier than trying
        // to deal with it here.
        assert (end - off > 1);

        CharBuffer cb = CharBuffer.wrap(cbuf, off, end - off);
        if (cb.position() != 0) {
            // Ensure that cb[0] == cbuf[off]
            cb = cb.slice();
        }

        boolean eof = false;
        for (;;) {
            CoderResult cr = decoder.decode(bb, cb, eof);
            if (cr.isUnderflow()) {
                if (eof) {
                    break;
                }
                if (!cb.hasRemaining()) {
                    break;
                }
                if ((cb.position() > 0) && !inReady()) {
                    break; // Block at most once
                }
                int n = readBytes();
                if (n < 0) {
                    eof = true;
                    if ((cb.position() == 0) && (!bb.hasRemaining())) {
                        break;
                    }
                    decoder.reset();
                }
                continue;
            }
            if (cr.isOverflow()) {
                assert cb.position() > 0;
                break;
            }
            cr.throwException();
        }

        if (eof) {
            // ## Need to flush decoder
            decoder.reset();
        }

        if (cb.position() == 0) {
            if (eof) {
                return -1;
            }
            assert false;
        }
        return cb.position();
    }

    private boolean inReady() {
        return inRead < inLimit;
    }

    private boolean implReady() {
        return bb.hasRemaining() || inReady();
    }

}
