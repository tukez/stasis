package org.stasis;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.reflect.Array;

import org.stasis.Stasis.Reader;
import org.stasis.Stasis.Writer;

public class Serializers {

    private static final Serializer<Boolean> BOOLEAN = new Serializer<Boolean>() {

        @Override
        public void write(Writer writer, DataOutputStream out, Boolean value) throws IOException {
            out.write(value ? 1 : 0);
        }

        @Override
        public Boolean read(Reader reader, DataInputStream in) throws IOException {
            return in.read() == 1;
        }

    };

    private static final Serializer<Character> CHAR = new Serializer<Character>() {

        @Override
        public void write(Writer writer, DataOutputStream out, Character value) throws IOException {
            out.writeChar(value);
        }

        @Override
        public Character read(Reader reader, DataInputStream in) throws IOException {
            return in.readChar();
        }

    };

    private static final Serializer<Byte> BYTE = new Serializer<Byte>() {

        @Override
        public void write(Writer writer, DataOutputStream out, Byte value) throws IOException {
            out.writeByte(value);
        }

        @Override
        public Byte read(Reader reader, DataInputStream in) throws IOException {
            return in.readByte();
        }

    };

    private static final Serializer<Short> SHORT = new Serializer<Short>() {

        @Override
        public void write(Writer writer, DataOutputStream out, Short value) throws IOException {
            out.writeShort(value);
        }

        @Override
        public Short read(Reader reader, DataInputStream in) throws IOException {
            return in.readShort();
        }

    };

    private static final Serializer<Integer> INT = new Serializer<Integer>() {

        @Override
        public void write(Writer writer, DataOutputStream out, Integer value) throws IOException {
            Varint.writeSignedVarInt(value, out);
        }

        @Override
        public Integer read(Reader reader, DataInputStream in) throws IOException {
            return Varint.readSignedVarInt(in);
        }

    };

    private static final Serializer<Long> LONG = new Serializer<Long>() {

        @Override
        public void write(Writer writer, DataOutputStream out, Long value) throws IOException {
            Varint.writeSignedVarLong(value, out);
        }

        @Override
        public Long read(Reader reader, DataInputStream in) throws IOException {
            return Varint.readSignedVarLong(in);
        }

    };

    private static final Serializer<Float> FLOAT = new Serializer<Float>() {

        @Override
        public void write(Writer writer, DataOutputStream out, Float value) throws IOException {
            out.writeInt(Float.floatToRawIntBits(value));
        }

        @Override
        public Float read(Reader reader, DataInputStream in) throws IOException {
            return Float.intBitsToFloat(in.readInt());
        }

    };

    private static final Serializer<Double> DOUBLE = new Serializer<Double>() {

        @Override
        public void write(Writer writer, DataOutputStream out, Double value) throws IOException {
            out.writeLong(Double.doubleToRawLongBits(value));
        }

        @Override
        public Double read(Reader reader, DataInputStream in) throws IOException {
            return Double.longBitsToDouble(in.readLong());
        }

    };

    private static final Serializer<String> STRING = new Serializer<String>() {

        @Override
        public void write(Writer writer, DataOutputStream out, String value) throws IOException {
            out.writeUTF(value);
        }

        @Override
        public String read(Reader reader, DataInputStream in) throws IOException {
            return in.readUTF();
        }

    };

    private static final Serializer<byte[]> BYTE_ARRAY = new Serializer<byte[]>() {

        @Override
        public void write(Writer writer, DataOutputStream out, byte[] value) throws IOException {
            forInt().write(writer, out, value.length);
            out.write(value);
        }

        @Override
        public byte[] read(Reader reader, DataInputStream in) throws IOException {
            int size = forInt().read(reader, in);
            byte[] value = new byte[size];
            in.read(value);
            return value;
        }

    };

    private static class PrimitiveArraySerializer<A, B> implements Serializer<A> {

        private final Class<B> type;
        private final Serializer<B> serializer;

        public PrimitiveArraySerializer(Class<B> type, Serializer<B> serializer) {
            this.type = type;
            this.serializer = serializer;
        }

        @SuppressWarnings("unchecked")
        @Override
        public void write(Writer writer, DataOutputStream out, A array) throws IOException {
            int size = Array.getLength(array);
            Serializers.forInt().write(writer, out, size);
            for (int i = 0; i < size; i++) {
                serializer.write(writer, out, (B) Array.get(array, i));
            }
        }

        @SuppressWarnings("unchecked")
        @Override
        public A read(Reader reader, DataInputStream in) throws IOException {
            int size = Serializers.forInt().read(reader, in);
            B[] array = (B[]) Array.newInstance(type, size);
            for (int i = 0; i < size; i++) {
                array[i] = serializer.read(reader, in);
            }
            return (A) array;
        }

    }

    private static class ArraySerializer<A> implements Serializer<A[]> {

        private final Class<A> type;
        private final Serializer<? super A> serializer;

        public ArraySerializer(Class<A> type, Serializer<A> serializer) {
            this.type = type;
            this.serializer = serializer;
        }

        @Override
        public void write(Writer writer, DataOutputStream out, A[] array) throws IOException {
            Serializers.forInt().write(writer, out, array.length);
            for (A value : array) {
                serializer.write(writer, out, value);
            }
        }

        @SuppressWarnings("unchecked")
        @Override
        public A[] read(Reader reader, DataInputStream in) throws IOException {
            int size = Serializers.forInt().read(reader, in);
            A[] array = (A[]) Array.newInstance(type, size);
            for (int i = 0; i < size; i++) {
                array[i] = (A) serializer.read(reader, in);
            }
            return array;
        }

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

    public static Serializer<Long> forLong() {
        return LONG;
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
        return forPrimitiveArray(char.class, forChar());
    }

    public static Serializer<short[]> forShortArray() {
        return forPrimitiveArray(short.class, forShort());
    }

    public static Serializer<int[]> forIntArray() {
        return forPrimitiveArray(int.class, forInt());
    }

    public static Serializer<long[]> forLongArray() {
        return forPrimitiveArray(long.class, forLong());
    }

    public static Serializer<float[]> forFloatArray() {
        return forPrimitiveArray(float.class, forFloat());
    }

    public static Serializer<double[]> forDoubleArray() {
        return forPrimitiveArray(double.class, forDouble());
    }

    public static <A> Serializer<A[]> forArray(Class<A> type, Serializer<A> serializer) {
        return new ArraySerializer<>(type, serializer);
    }

    private static <A, B> Serializer<A> forPrimitiveArray(Class<B> type, Serializer<B> serializer) {
        return new PrimitiveArraySerializer<>(type, serializer);
    }
}
