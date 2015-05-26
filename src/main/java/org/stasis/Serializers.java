package org.stasis;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CodingErrorAction;

import org.stasis.Stasis.Reader;
import org.stasis.Stasis.Writer;
import org.stasis.pool.ObjectFactory;
import org.stasis.pool.ObjectPool;
import org.stasis.pool.StaticObjectPool;

import sun.nio.cs.ArrayDecoder;
import sun.nio.cs.ArrayEncoder;

@SuppressWarnings("restriction")
public class Serializers {

    private static final Serializer<Void> NULL = new Serializer<Void>() {

        @Override
        public void write(Writer writer, DataOutput out, Void value) throws IOException {
        }

        @Override
        public Void read(Reader reader, DataInput in) throws IOException {
            return null;
        }

    };

    private static final Serializer<Boolean> BOOLEAN = new Serializer<Boolean>() {

        @Override
        public void write(Writer writer, DataOutput out, Boolean value) throws IOException {
            out.write(value ? 1 : 0);
        }

        @Override
        public Boolean read(Reader reader, DataInput in) throws IOException {
            return in.readByte() == 1;
        }

    };

    private static final Serializer<Character> CHAR = new Serializer<Character>() {

        @Override
        public void write(Writer writer, DataOutput out, Character value) throws IOException {
            out.writeChar(value);
        }

        @Override
        public Character read(Reader reader, DataInput in) throws IOException {
            return in.readChar();
        }

    };

    private static final Serializer<Byte> BYTE = new Serializer<Byte>() {

        @Override
        public void write(Writer writer, DataOutput out, Byte value) throws IOException {
            out.writeByte(value);
        }

        @Override
        public Byte read(Reader reader, DataInput in) throws IOException {
            return in.readByte();
        }

    };

    private static final Serializer<Short> SHORT = new Serializer<Short>() {

        @Override
        public void write(Writer writer, DataOutput out, Short value) throws IOException {
            out.writeShort(value);
        }

        @Override
        public Short read(Reader reader, DataInput in) throws IOException {
            return in.readShort();
        }

    };

    private static final Serializer<Integer> INT = new Serializer<Integer>() {

        @Override
        public void write(Writer writer, DataOutput out, Integer value) throws IOException {
            out.writeInt(value);
        }

        @Override
        public Integer read(Reader reader, DataInput in) throws IOException {
            return in.readInt();
        }

    };

    private static final Serializer<Integer> VARINT = new Serializer<Integer>() {

        @Override
        public void write(Writer writer, DataOutput out, Integer value) throws IOException {
            Varint.writeSignedVarInt(value, out);
        }

        @Override
        public Integer read(Reader reader, DataInput in) throws IOException {
            return Varint.readSignedVarInt(in);
        }

    };

    private static final Serializer<Integer> VARUINT = new Serializer<Integer>() {

        @Override
        public void write(Writer writer, DataOutput out, Integer value) throws IOException {
            Varint.writeUnsignedVarInt(value, out);
        }

        @Override
        public Integer read(Reader reader, DataInput in) throws IOException {
            return Varint.readUnsignedVarInt(in);
        }

    };

    private static final Serializer<Long> LONG = new Serializer<Long>() {

        @Override
        public void write(Writer writer, DataOutput out, Long value) throws IOException {
            out.writeLong(value);
        }

        @Override
        public Long read(Reader reader, DataInput in) throws IOException {
            return in.readLong();
        }

    };

    private static final Serializer<Long> VARLONG = new Serializer<Long>() {

        @Override
        public void write(Writer writer, DataOutput out, Long value) throws IOException {
            Varint.writeSignedVarLong(value, out);
        }

        @Override
        public Long read(Reader reader, DataInput in) throws IOException {
            return Varint.readSignedVarLong(in);
        }

    };

    private static final Serializer<Long> VARULONG = new Serializer<Long>() {

        @Override
        public void write(Writer writer, DataOutput out, Long value) throws IOException {
            Varint.writeUnsignedVarLong(value, out);
        }

        @Override
        public Long read(Reader reader, DataInput in) throws IOException {
            return Varint.readUnsignedVarLong(in);
        }

    };

    private static final Serializer<Float> FLOAT = new Serializer<Float>() {

        @Override
        public void write(Writer writer, DataOutput out, Float value) throws IOException {
            out.writeInt(Float.floatToRawIntBits(value));
        }

        @Override
        public Float read(Reader reader, DataInput in) throws IOException {
            return Float.intBitsToFloat(in.readInt());
        }

    };

    private static final Serializer<Double> DOUBLE = new Serializer<Double>() {

        @Override
        public void write(Writer writer, DataOutput out, Double value) throws IOException {
            out.writeLong(Double.doubleToRawLongBits(value));
        }

        @Override
        public Double read(Reader reader, DataInput in) throws IOException {
            return Double.longBitsToDouble(in.readLong());
        }

    };

    private static final Charset UTF8_CHARSET = Charset.forName("UTF-8");

    private static final Field STRING_CHARS;
    private static final Constructor<String> STRING_CONSTRUCTOR;
    static {
        try {
            STRING_CHARS = String.class.getDeclaredField("value");
            STRING_CHARS.setAccessible(true);

            STRING_CONSTRUCTOR = String.class.getDeclaredConstructor(char[].class, boolean.class);
            STRING_CONSTRUCTOR.setAccessible(true);
        } catch (NoSuchFieldException | SecurityException | NoSuchMethodException e) {
            throw new IllegalStateException(e);
        }
    }

    private static final class EncoderEntry {
        final CharsetEncoder encoder;
        final CharsetDecoder decoder;
        final byte[] buffer;

        public EncoderEntry(CharsetEncoder encoder, CharsetDecoder decoder, byte[] buffer) {
            this.encoder = encoder;
            this.decoder = decoder;
            this.buffer = buffer;
        }
    }

    private static final Serializer<String> STRING = new Serializer<String>() {

        private final ObjectPool<EncoderEntry> ENCODERS = new StaticObjectPool<EncoderEntry>(8,
                new ObjectFactory<EncoderEntry>() {

                    @Override
                    public EncoderEntry create() {
                        return new EncoderEntry(UTF8_CHARSET.newEncoder().onMalformedInput(CodingErrorAction.REPLACE)
                                .onUnmappableCharacter(CodingErrorAction.REPLACE), UTF8_CHARSET.newDecoder()
                                .onMalformedInput(CodingErrorAction.REPLACE)
                                .onUnmappableCharacter(CodingErrorAction.REPLACE), new byte[4096]);
                    }

                    @Override
                    public void onBorrow(EncoderEntry object) {
                    }

                    @Override
                    public void onRelease(EncoderEntry object) {
                    }

                });

        private EncoderEntry borrowEncoder() {
            return ENCODERS.borrow();
        }

        private void release(EncoderEntry out) {
            if (!ENCODERS.release(out)) {
                throw new IllegalStateException("Could not release " + out.getClass().getName());
            }
        }

        @Override
        public void write(Writer writer, DataOutput out, String value) throws IOException {
            EncoderEntry encoderEntry = borrowEncoder();
            try {
                byte[] buffer = encoderEntry.buffer;
                char[] chars = (char[]) STRING_CHARS.get(value);
                Varint.writeUnsignedVarInt(chars.length, out); // char length
                if (chars.length > 0) {
                    CharsetEncoder encoder = encoderEntry.encoder;
                    encoder.reset();

                    int maxBytes = (int) (chars.length * (double) encoder.maxBytesPerChar());
                    if (buffer.length < maxBytes) {
                        buffer = new byte[maxBytes];
                        encoderEntry = new EncoderEntry(encoder, encoderEntry.decoder, buffer);
                    }

                    int encodedBytes = ((ArrayEncoder) encoder).encode(chars, 0, chars.length, buffer);

                    Varint.writeUnsignedVarInt(encodedBytes, out); // byte length
                    out.write(buffer, 0, encodedBytes); // content
                }
            } catch (IllegalArgumentException | IllegalAccessException e) {
                throw new IllegalStateException(e);
            } finally {
                release(encoderEntry);
            }
        }

        @Override
        public String read(Reader reader, DataInput in) throws IOException {
            int length = Varint.readUnsignedVarInt(in);
            if (length == 0) {
                return "";
            } else {
                char[] chars = new char[length];
                int byteLength = Varint.readUnsignedVarInt(in);

                EncoderEntry encoderEntry = borrowEncoder();
                try {
                    CharsetDecoder decoder = encoderEntry.decoder;
                    decoder.reset();

                    byte[] buffer = encoderEntry.buffer;
                    if (buffer.length < byteLength) {
                        buffer = new byte[byteLength];
                        encoderEntry = new EncoderEntry(encoderEntry.encoder, decoder, buffer);
                    }
                    in.readFully(buffer, 0, byteLength);

                    int decodedChars = ((ArrayDecoder) decoder).decode(buffer, 0, byteLength, chars);
                    if (length != decodedChars) {
                        throw new IllegalStateException("Decoded chars does not match with length read from stream. "
                                + decodedChars + " != " + length);
                    }
                } finally {
                    release(encoderEntry);
                }

                try {
                    return STRING_CONSTRUCTOR.newInstance(chars, true);
                } catch (InstantiationException | IllegalAccessException | IllegalArgumentException
                        | InvocationTargetException e) {
                    throw new IllegalStateException(e);
                }
            }
        }

    };

    private static final Serializer<byte[]> BYTE_ARRAY = new Serializer<byte[]>() {

        @Override
        public void write(Writer writer, DataOutput out, byte[] value) throws IOException {
            forVarUInt().write(writer, out, value.length);
            out.write(value);
        }

        @Override
        public byte[] read(Reader reader, DataInput in) throws IOException {
            int size = forVarUInt().read(reader, in);
            byte[] value = new byte[size];
            in.readFully(value);
            return value;
        }

    };

    private static abstract class PrimitiveArraySerializer<A> implements Serializer<A> {

        private final Serializer<Object> serializer;

        @SuppressWarnings("unchecked")
        public PrimitiveArraySerializer(Serializer<?> serializer) {
            this.serializer = (Serializer<Object>) serializer;
        }

        @Override
        public final void write(Writer writer, DataOutput out, A array) throws IOException {
            int size = Array.getLength(array);
            Serializers.forVarUInt().write(writer, out, size);
            for (int i = 0; i < size; i++) {
                serializer.write(writer, out, Array.get(array, i));
            }
        }

        @Override
        public final A read(Reader reader, DataInput in) throws IOException {
            int size = Serializers.forVarUInt().read(reader, in);
            A array = newArray(size);
            for (int i = 0; i < size; i++) {
                Array.set(array, i, serializer.read(reader, in));
            }
            return array;
        }

        protected abstract A newArray(int size);
    }

    private static final Serializer<char[]> CHAR_ARRAY = new PrimitiveArraySerializer<char[]>(forChar()) {

        @Override
        protected char[] newArray(int size) {
            return new char[size];
        }

    };

    private static final Serializer<short[]> SHORT_ARRAY = new PrimitiveArraySerializer<short[]>(forShort()) {

        @Override
        protected short[] newArray(int size) {
            return new short[size];
        }

    };

    private static final Serializer<int[]> INT_ARRAY = new PrimitiveArraySerializer<int[]>(forVarInt()) {

        @Override
        protected int[] newArray(int size) {
            return new int[size];
        }

    };

    private static final Serializer<long[]> LONG_ARRAY = new PrimitiveArraySerializer<long[]>(forVarLong()) {

        @Override
        protected long[] newArray(int size) {
            return new long[size];
        }

    };

    private static final Serializer<float[]> FLOAT_ARRAY = new PrimitiveArraySerializer<float[]>(forFloat()) {

        @Override
        protected float[] newArray(int size) {
            return new float[size];
        }

    };

    private static final Serializer<double[]> DOUBLE_ARRAY = new PrimitiveArraySerializer<double[]>(forDouble()) {

        @Override
        protected double[] newArray(int size) {
            return new double[size];
        }

    };

    private static class ArraySerializer<A> implements Serializer<A[]> {

        private final Class<A> type;
        private final Serializer<? super A> serializer;

        public ArraySerializer(Class<A> type, Serializer<A> serializer) {
            this.type = type;
            this.serializer = serializer;
        }

        @Override
        public void write(Writer writer, DataOutput out, A[] array) throws IOException {
            Serializers.forVarUInt().write(writer, out, array.length);
            for (A value : array) {
                serializer.write(writer, out, value);
            }
        }

        @SuppressWarnings("unchecked")
        @Override
        public A[] read(Reader reader, DataInput in) throws IOException {
            int size = Serializers.forVarUInt().read(reader, in);
            A[] array = (A[]) Array.newInstance(type, size);
            for (int i = 0; i < size; i++) {
                array[i] = (A) serializer.read(reader, in);
            }
            return array;
        }

    }

    private static final Serializer<Object[]> OBJECT_ARRAY = new Serializer<Object[]>() {

        @Override
        public void write(Writer writer, DataOutput out, Object[] array) throws IOException {
            Serializers.forVarUInt().write(writer, out, array.length);
            for (Object value : array) {
                writer.writeTypeAndObject(value, out);
            }
        }

        @Override
        public Object[] read(Reader reader, DataInput in) throws IOException {
            int size = Serializers.forVarUInt().read(reader, in);
            Object[] array = new Object[size];
            for (int i = 0; i < size; i++) {
                array[i] = reader.readTypeAndObject(in);
            }
            return array;
        }

    };

    private static class EnumSerializer<A extends Enum<A>> implements Serializer<A> {

        private final Class<A> type;

        public EnumSerializer(Class<A> type) {
            this.type = type;
        }

        @Override
        public void write(Writer writer, DataOutput out, A value) throws IOException {
            forVarUInt().write(writer, out, value.ordinal());
        }

        @Override
        public A read(Reader reader, DataInput in) throws IOException {
            return type.getEnumConstants()[forVarUInt().read(reader, in)];
        }

    };

    public static Serializer<Void> forNull() {
        return NULL;
    }

    public static Serializer<Boolean> forBoolean() {
        return BOOLEAN;
    }

    public static Serializer<Character> forChar() {
        return CHAR;
    }

    public static Serializer<Byte> forByte() {
        return BYTE;
    }

    public static Serializer<Short> forShort() {
        return SHORT;
    }

    public static Serializer<Integer> forInt() {
        return INT;
    }

    public static Serializer<Integer> forVarInt() {
        return VARINT;
    }

    public static Serializer<Integer> forVarUInt() {
        return VARUINT;
    }

    public static Serializer<Long> forLong() {
        return LONG;
    }

    public static Serializer<Long> forVarLong() {
        return VARLONG;
    }

    public static Serializer<Long> forVarULong() {
        return VARULONG;
    }

    public static Serializer<Float> forFloat() {
        return FLOAT;
    }

    public static Serializer<Double> forDouble() {
        return DOUBLE;
    }

    public static Serializer<String> forString() {
        return STRING;
    }

    public static Serializer<byte[]> forByteArray() {
        return BYTE_ARRAY;
    }

    public static Serializer<char[]> forCharArray() {
        return CHAR_ARRAY;
    }

    public static Serializer<short[]> forShortArray() {
        return SHORT_ARRAY;
    }

    public static Serializer<int[]> forIntArray() {
        return INT_ARRAY;
    }

    public static Serializer<long[]> forLongArray() {
        return LONG_ARRAY;
    }

    public static Serializer<float[]> forFloatArray() {
        return FLOAT_ARRAY;
    }

    public static Serializer<double[]> forDoubleArray() {
        return DOUBLE_ARRAY;
    }

    public static Serializer<Object[]> forObjectArray() {
        return OBJECT_ARRAY;
    }

    public static <A> Serializer<A[]> forArray(Class<A> type, Serializer<A> serializer) {
        return new ArraySerializer<>(type, serializer);
    }

    public static <A extends Enum<A>> Serializer<A> forEnum(Class<A> type) {
        return new EnumSerializer<>(type);
    }

}
