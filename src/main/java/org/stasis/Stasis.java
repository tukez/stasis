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

    public void setReferenceProviderFactory(ReferenceProviderFactory refsFactory) {
        this.refsFactory = refsFactory;
    }

    public synchronized <A> void register(Class<A> type, Serializer<? super A> serializer) {
        SerializerEntry entry = new SerializerEntry(type, serializer, serializers.size());
        serializers.add(entry);
        typeToSerializer.put(type, entry);
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
                SerializerEntry entry = serializerEntryFor(object.getClass());
                int serializerIndex = entry.index;
                Serializer<Object> serializer = (Serializer<Object>) entry.serializer;

                Varint.writeUnsignedVarInt(toHeader(serializerIndex, OBJECT), out);
                serializer.write(this, out, object);
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
                Varint.writeUnsignedVarInt(toHeader(0, OBJECT), out);
                serializer.write(this, out, object);
            } else {
                writeRef(out, ref);
            }
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
                int ref = readRef(header);
                return refs.objectFor(ref);
            } else {
                int serializerIndex = readSerializerIndex(header);
                Serializer<?> serializer = serializers.get(serializerIndex).serializer;
                return read(in, serializer);
            }
        }

        @SuppressWarnings("unchecked")
        public <A> A readObject(DataInputStream in, Class<? super A> type) throws IOException {
            return readObject(in, (Serializer<A>) serializerFor(type));
        }

        @SuppressWarnings("unchecked")
        public <A> A readObject(DataInputStream in, Serializer<A> serializer) throws IOException {
            int header = Varint.readUnsignedVarInt(in);
            if (isRef(header)) {
                int ref = readRef(header);
                return (A) refs.objectFor(ref);
            } else {
                return read(in, serializer);
            }
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
