package org.stasis;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Stasis {

    private static final int NON_NULL_HEADER_MASK = 1;
    private static final int OBJECT = 0;
    private static final int REFERENCE = 1;

    private static final class SerializerEntry {

        private final Class<?> type;
        private final Serializer<?> serializer;
        private final int index;

        public SerializerEntry(Class<?> type, Serializer<?> serializer, int index) {
            this.type = type;
            this.serializer = serializer;
            this.index = index;
        }
    }

    private final List<SerializerEntry> serializers = Collections.synchronizedList(new ArrayList<SerializerEntry>());
    private final Map<Class<?>, SerializerEntry> typeToSerializer = new ConcurrentHashMap<>();

    private volatile ReferenceProviderFactory refsFactory = new IdentityReferenceProviderFactory();

    private Stasis() {
    }

    public static Stasis create() {
        return new Stasis();
    }

    public Stasis registerNull() {
        register(Void.class, Serializers.forNull());
        return this;
    }

    public Stasis registerPrimitives() {
        register(boolean.class, Serializers.forBoolean());
        register(char.class, Serializers.forChar());
        register(byte.class, Serializers.forByte());
        register(short.class, Serializers.forShort());
        register(int.class, Serializers.forInt());
        register(long.class, Serializers.forLong());
        register(float.class, Serializers.forFloat());
        register(double.class, Serializers.forDouble());
        return this;
    }

    public Stasis registerBoxedPrimitives() {
        register(Boolean.class, Serializers.forBoolean());
        register(Character.class, Serializers.forChar());
        register(Byte.class, Serializers.forByte());
        register(Short.class, Serializers.forShort());
        register(Integer.class, Serializers.forInt());
        register(Long.class, Serializers.forLong());
        register(Float.class, Serializers.forFloat());
        register(Double.class, Serializers.forDouble());
        return this;
    }

    public Stasis registerString() {
        register(String.class, Serializers.forString());
        return this;
    }

    public Stasis registerPrimitiveArrays() {
        register(char[].class, Serializers.forCharArray());
        register(byte[].class, Serializers.forByteArray());
        register(short[].class, Serializers.forShortArray());
        register(int[].class, Serializers.forIntArray());
        register(long[].class, Serializers.forLongArray());
        register(float[].class, Serializers.forFloatArray());
        register(double[].class, Serializers.forDoubleArray());
        return this;
    }

    public Stasis registerBoxedPrimitiveArrays() {
        register(Character[].class, Serializers.forArray(Character.class, Serializers.forChar()));
        register(Byte[].class, Serializers.forArray(Byte.class, Serializers.forByte()));
        register(Short[].class, Serializers.forArray(Short.class, Serializers.forShort()));
        register(Integer[].class, Serializers.forArray(Integer.class, Serializers.forInt()));
        register(Long[].class, Serializers.forArray(Long.class, Serializers.forLong()));
        register(Float[].class, Serializers.forArray(Float.class, Serializers.forFloat()));
        register(Double[].class, Serializers.forArray(Double.class, Serializers.forDouble()));
        return this;
    }

    public Stasis registerStringArray() {
        register(String[].class, Serializers.forArray(String.class, Serializers.forString()));
        return this;
    }

    public Stasis registerObjectArray() {
        register(Object[].class, Serializers.forObjectArray());
        return this;
    }

    public Stasis setReferenceProviderFactory(ReferenceProviderFactory refsFactory) {
        this.refsFactory = refsFactory;
        return this;
    }

    public synchronized <A> Stasis register(Class<A> type, Serializer<? super A> serializer) {
        SerializerEntry existingEntry = typeToSerializer.get(type);
        SerializerEntry entry = new SerializerEntry(type, serializer, existingEntry == null ? serializers.size() : existingEntry.index);
        if (existingEntry == null) {
            serializers.add(entry);
        } else {
            serializers.set(entry.index, entry);
        }
        typeToSerializer.put(type, entry);
        return this;
    }

    @SuppressWarnings("unchecked")
    public <A> Serializer<? super A> serializerFor(Class<A> type) {
        return (Serializer<? super A>) serializerEntryFor(type).serializer;
    }

    public Writer newWriter() {
        return new Writer(refsFactory.createWriter());
    }

    public Reader newReader() {
        return new Reader(refsFactory.createReader());
    }

    public class Writer {

        private final ReferenceWriter refs;

        private Writer(ReferenceWriter refs) {
            this.refs = refs;
        }

        @SuppressWarnings("unchecked")
        public void writeTypeAndObject(Object object, DataOutputStream out) throws IOException {
            int ref = refs.referenceFor(object);
            if (noRefFound(ref)) {
                SerializerEntry entry = serializerEntryFor(classOf(object));
                int serializerIndex = entry.index;
                Serializer<Object> serializer = (Serializer<Object>) entry.serializer;
                writeObject(object, out, serializerIndex, serializer);
            } else {
                writeRef(out, ref);
            }
        }

        public <A> void writeObject(A object, DataOutputStream out, Class<? super A> type) throws IOException {
            writeObject(object, out, serializerFor(type));
        }

        public <A> void writeObject(A object, DataOutputStream out, Serializer<? super A> serializer) throws IOException {
            int ref = refs.referenceFor(object);
            if (noRefFound(ref)) {
                writeObject(object, out, 0, serializer);
            } else {
                writeRef(out, ref);
            }
        }

        private Class<? extends Object> classOf(Object object) {
            return object == null ? Void.class : object.getClass();
        }

        private <A> void writeObject(A object, DataOutputStream out, int headerData, Serializer<A> serializer) throws IOException {
            Varint.writeUnsignedVarInt(toHeader(headerData, OBJECT), out);
            serializer.write(this, out, object);
            refs.registerObject(object);
        }

        private void writeRef(DataOutputStream out, int ref) throws IOException {
            Varint.writeUnsignedVarInt(toHeader(ref, REFERENCE), out);
        }

    }

    public class Reader {

        private final ReferenceReader refs;

        private Reader(ReferenceReader refs) {
            this.refs = refs;
        }

        public Object readTypeAndObject(DataInputStream in) throws IOException {
            int header = Varint.readUnsignedVarInt(in);
            if (isRef(header)) {
                return readFromRef(header);
            } else {
                int serializerIndex = readSerializerIndex(header);
                Serializer<?> serializer = serializerFor(serializerIndex);
                return read(in, serializer);
            }
        }

        @SuppressWarnings("unchecked")
        public <A> A readObject(DataInputStream in, Class<? super A> type) throws IOException {
            return readObject(in, (Serializer<A>) serializerFor(type));
        }

        public <A> A readObject(DataInputStream in, Serializer<A> serializer) throws IOException {
            int header = Varint.readUnsignedVarInt(in);
            if (isRef(header)) {
                return readFromRef(header);
            } else {
                return read(in, serializer);
            }
        }

        @SuppressWarnings("unchecked")
        private <A> A readFromRef(int header) {
            int ref = readRef(header);
            return (A) refs.objectFor(ref);
        }

        private <A> A read(DataInputStream in, Serializer<A> serializer) throws IOException {
            A object = serializer.read(this, in);
            refs.registerObject(object);
            return object;
        }
    }

    private SerializerEntry serializerEntryFor(Class<?> type) {
        SerializerEntry entry = typeToSerializer.get(type);
        if (entry == null) {
            for (SerializerEntry e : serializers) {
                if (e.type.isAssignableFrom(type)) {
                    typeToSerializer.put(type, e);
                    return e;
                }
            }
            throw new IllegalStateException("Serializer for " + type.getName() + " not found.");
        } else {
            return entry;
        }
    }

    private Serializer<?> serializerFor(int serializerIndex) {
        if (serializerIndex >= serializers.size()) {
            throw new IllegalStateException("Serializer for index " + serializerIndex + " not found.");
        }
        return serializers.get(serializerIndex).serializer;
    }

    private boolean noRefFound(int ref) {
        return ref < 0;
    }

    private int toHeader(int data, int headerType) {
        return (data << 1) | headerType;
    }

    private boolean isRef(int header) {
        return (header & NON_NULL_HEADER_MASK) == REFERENCE;
    }

    private int readRef(int header) {
        return readHeaderData(header);
    }

    private int readSerializerIndex(int header) {
        return readHeaderData(header);
    }

    private int readHeaderData(int header) {
        return header >>> 1;
    }

}
