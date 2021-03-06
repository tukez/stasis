package org.stasis;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.lang.reflect.Array;
import java.nio.charset.StandardCharsets;

import org.stasis.Stasis.Reader;
import org.stasis.Stasis.Writer;

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

    private static final Serializer<String> STRING = new Serializer<String>() {

        @Override
        public void write(Writer writer, DataOutput out, String value) throws IOException {
            Varint.writeUnsignedVarInt(value.length(), out); // char length
            if (value.length() > 0) {
                byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
                Varint.writeUnsignedVarInt(bytes.length, out); // byte length
                out.write(bytes); // content
            }
        }

        @Override
        public String read(Reader reader, DataInput in) throws IOException {
            int length = Varint.readUnsignedVarInt(in); // char length
            if (length == 0) {
                return "";
            } else {
                int byteLength = Varint.readUnsignedVarInt(in); // byte length
                byte[] bytes = new byte[byteLength];
                in.readFully(bytes); // content
                return new String(bytes, StandardCharsets.UTF_8);
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
