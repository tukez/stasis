package org.stasis;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.stasis.Stasis.Reader;
import org.stasis.Stasis.Writer;

public class StasisTest {

    private ByteArrayOutputStream baos = new ByteArrayOutputStream();
    private DataOutputStream out = new DataOutputStream(baos);
    private Stasis stasis = Stasis.create().registerNull().registerPrimitives().registerBoxedPrimitives().registerString()
                                  .registerPrimitiveArrays().registerBoxedPrimitiveArrays().registerStringArray().registerObjectArray()
                                  .register(List.class, new ListSerializer());
    private Stasis.Writer writer = stasis.newWriter();
    private Stasis.Reader reader = stasis.newReader();

    @After
    public void after() {
        writer.close();
        reader.close();
    }
    
    @Test
    public void primitives() throws IOException {
        writer.writeObject(true, out, boolean.class);
        writer.writeObject('a', out, char.class);
        writer.writeObject((byte) 1, out, byte.class);
        writer.writeObject((short) 2, out, short.class);
        writer.writeObject(3, out, int.class);
        writer.writeObject(4l, out, long.class);
        writer.writeObject(5f, out, float.class);
        writer.writeObject(6.0, out, double.class);
        writer.writeObject("string", out, String.class);

        DataInputStream in = in();
        Assert.assertEquals(true, reader.readObject(in, boolean.class));
        Assert.assertEquals((Character) 'a', reader.readObject(in, char.class));
        Assert.assertEquals((Byte) (byte) 1, reader.readObject(in, byte.class));
        Assert.assertEquals((Short) (short) 2, reader.readObject(in, short.class));
        Assert.assertEquals((Integer) 3, reader.readObject(in, int.class));
        Assert.assertEquals((Long) 4l, reader.readObject(in, long.class));
        Assert.assertEquals((Float) 5f, reader.readObject(in, float.class));
        Assert.assertEquals((Double) 6.0, reader.readObject(in, double.class));
        Assert.assertEquals("string", reader.readObject(in, String.class));
    }
    
    @Test
    public void longString() throws IOException {
    	StringBuilder strBuilder = new StringBuilder();
    	for (int i=0; i<Short.MAX_VALUE * 2 + 1; i++) {
    		strBuilder.append("v\u65e5\u672c");
    	}
    	String str = strBuilder.toString();
    	
    	writer.writeObject(str, out, String.class);
    	DataInputStream in = in();
        Assert.assertEquals(str, reader.readObject(in, String.class));
    }

    @Test
    public void boxedPrimitives() throws IOException {
        writer.writeObject(true, out, Boolean.class);
        writer.writeObject('a', out, Character.class);
        writer.writeObject((byte) 1, out, Byte.class);
        writer.writeObject((short) 2, out, Short.class);
        writer.writeObject(3, out, Integer.class);
        writer.writeObject(4l, out, Long.class);
        writer.writeObject(5f, out, Float.class);
        writer.writeObject(6.0, out, Double.class);

        DataInputStream in = in();
        Assert.assertEquals(true, reader.readObject(in, Boolean.class));
        Assert.assertEquals((Character) 'a', reader.readObject(in, Character.class));
        Assert.assertEquals((Byte) (byte) 1, reader.readObject(in, Byte.class));
        Assert.assertEquals((Short) (short) 2, reader.readObject(in, Short.class));
        Assert.assertEquals((Integer) 3, reader.readObject(in, Integer.class));
        Assert.assertEquals((Long) 4l, reader.readObject(in, Long.class));
        Assert.assertEquals((Float) 5f, reader.readObject(in, Float.class));
        Assert.assertEquals((Double) 6.0, reader.readObject(in, Double.class));
    }

    @Test
    public void enums() throws IOException {
        Serializer<TestEnum> serializer = Serializers.forEnum(TestEnum.class);
        writer.writeObject(TestEnum.VAL1, out, serializer);
        writer.writeObject(TestEnum.VAL2, out, serializer);

        DataInputStream in = in();
        Assert.assertEquals(TestEnum.VAL1, reader.readObject(in, serializer));
        Assert.assertEquals(TestEnum.VAL2, reader.readObject(in, serializer));
    }

    @Test
    public void primitiveArrays() throws IOException {
        writer.writeObject(new char[] { 'a', 'b' }, out, char[].class);
        writer.writeObject(new byte[] { 1, 2 }, out, byte[].class);
        writer.writeObject(new short[] { 3, 4 }, out, short[].class);
        writer.writeObject(new int[] { 5, 6 }, out, int[].class);
        writer.writeObject(new long[] { 7l, 8l }, out, long[].class);
        writer.writeObject(new float[] { 9f, 10f }, out, float[].class);
        writer.writeObject(new double[] { 11.0, 12.0 }, out, double[].class);
        writer.writeObject(new String[] { "string1", "string2" }, out, String[].class);

        DataInputStream in = in();
        Assert.assertArrayEquals(new char[] { 'a', 'b' }, reader.readObject(in, char[].class));
        Assert.assertArrayEquals(new byte[] { 1, 2 }, reader.readObject(in, byte[].class));
        Assert.assertArrayEquals(new short[] { 3, 4 }, reader.readObject(in, short[].class));
        Assert.assertArrayEquals(new int[] { 5, 6 }, reader.readObject(in, int[].class));
        Assert.assertArrayEquals(new long[] { 7l, 8l }, reader.readObject(in, long[].class));
        Assert.assertArrayEquals(new float[] { 9f, 10f }, reader.readObject(in, float[].class), 0.0001f);
        Assert.assertArrayEquals(new double[] { 11.0, 12.0 }, reader.readObject(in, double[].class), 0.0001);
        Assert.assertArrayEquals(new String[] { "string1", "string2" }, reader.readObject(in, String[].class));
    }

    @Test
    public void boxedPrimitiveArrays() throws IOException {
        writer.writeObject(new Character[] { 'a', 'b' }, out, Character[].class);
        writer.writeObject(new Byte[] { 1, 2 }, out, Byte[].class);
        writer.writeObject(new Short[] { 3, 4 }, out, Short[].class);
        writer.writeObject(new Integer[] { 5, 6 }, out, Integer[].class);
        writer.writeObject(new Long[] { 7l, 8l }, out, Long[].class);
        writer.writeObject(new Float[] { 9f, 10f }, out, Float[].class);
        writer.writeObject(new Double[] { 11.0, 12.0 }, out, Double[].class);

        DataInputStream in = in();
        Assert.assertArrayEquals(new Character[] { 'a', 'b' }, reader.readObject(in, Character[].class));
        Assert.assertArrayEquals(new Byte[] { 1, 2 }, reader.readObject(in, Byte[].class));
        Assert.assertArrayEquals(new Short[] { 3, 4 }, reader.readObject(in, Short[].class));
        Assert.assertArrayEquals(new Integer[] { 5, 6 }, reader.readObject(in, Integer[].class));
        Assert.assertArrayEquals(new Long[] { 7l, 8l }, reader.readObject(in, Long[].class));
        Assert.assertArrayEquals(new Float[] { 9f, 10f }, reader.readObject(in, Float[].class));
        Assert.assertArrayEquals(new Double[] { 11.0, 12.0 }, reader.readObject(in, Double[].class));
    }

    @Test
    public void nullValuesWorkWhenSavingType() throws IOException {
        writer.writeTypeAndObject(Arrays.asList("obj1", null, "obj2"), out);
        writer.writeTypeAndObject(null, out);
        writer.writeTypeAndObject(Arrays.asList(null, null, "obj2"), out);

        DataInputStream in = in();

        Assert.assertEquals(Arrays.asList("obj1", null, "obj2"), reader.readTypeAndObject(in));
        Assert.assertEquals(null, reader.readTypeAndObject(in));
        Assert.assertEquals(Arrays.asList(null, null, "obj2"), reader.readTypeAndObject(in));
    }

    @Test
    public void objectArray() throws IOException {
        writer.writeTypeAndObject(new Object[] { "string", null, new Object[] { 2, new int[] { 3, 4 } } }, out);
        DataInputStream in = in();
        Assert.assertArrayEquals(new Object[] { "string", null, new Object[] { 2, new int[] { 3, 4 } } },
                                 (Object[]) reader.readTypeAndObject(in));
    }

    @Test
    public void objectsAreSerializedOnlyOnceAndReferencedAfterwards() throws IOException {
        writer.writeObject("string", out, String.class);
        int size1 = baos.toByteArray().length;
        writer.writeObject("string", out, String.class);
        int size2 = baos.toByteArray().length;

        Assert.assertEquals("reference size is only 1 byte in this case", size1 + 1, size2);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void writeObjectWorksWithSupertypeSerializer() throws IOException {
        ArrayList<Object> list1 = new ArrayList<>(Arrays.asList(1, "2", 3));
        writer.writeTypeAndObject(list1, out);

        DataInputStream in = in();
        List<Object> list2 = (List<Object>) reader.readTypeAndObject(in);

        Assert.assertEquals("lists are same after serialization", list1, list2);
        Assert.assertEquals("deserialized object is not same type", LinkedList.class, list2.getClass());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void referencesWorksInNestedObjects() throws IOException {
        stasis.setReferenceProviderFactory(new HashReferenceProviderFactory());
        writer = stasis.newWriter();

        List<List<String>> list1 = new ArrayList<>();
        list1.add(Arrays.asList("obj1", "obj2"));
        list1.add(Arrays.asList("obj1", "obj2"));
        list1.add(Arrays.asList("obj1", "obj3", "obj4"));

        writer.writeTypeAndObject(list1, out);

        DataInputStream in = in();
        List<List<String>> list2 = (List<List<String>>) reader.readTypeAndObject(in);

        Assert.assertEquals("lists are equal", list1, list2);

        Assert.assertSame("inner objects are same instances", list2.get(0), list2.get(1));
        Assert.assertSame("inner objects are same instances", list2.get(0).get(0), list2.get(2).get(0));
    }

    @Test
    public void referencesWorkWithoutSerializingType() throws IOException {
        writer.writeObject("obj", out, String.class);
        writer.writeObject("obj", out, String.class);

        DataInputStream in = in();
        String obj1 = reader.readObject(in, String.class);
        String obj2 = reader.readObject(in, String.class);

        Assert.assertSame("obj1 and obj2 are same instance", obj1, obj2);
    }

    @Test
    public void registeringSameTypeAgainOverridesTheOldRegistration() throws IOException {
        stasis.register(String.class, new Serializer<String>() {

            @Override
            public void write(Writer writer, DataOutput out, String value) throws IOException {
                // Do nothing
            }

            @Override
            public String read(Reader reader, DataInput in) throws IOException {
                return "dummy";
            }

        });

        writer.writeTypeAndObject("object", out);
        DataInputStream in = in();
        Object object = reader.readTypeAndObject(in);

        Assert.assertEquals("String serializer was overridden", "dummy", object);
    }

    private DataInputStream in() {
        return new DataInputStream(new ByteArrayInputStream(baos.toByteArray()));
    }

    private enum TestEnum {
        VAL1, VAL2
    }

    @SuppressWarnings("rawtypes")
    private class ListSerializer implements Serializer<List> {

        @Override
        public void write(Writer writer, DataOutput out, List value) throws IOException {
            out.writeInt(value.size());
            for (Object object : value) {
                writer.writeTypeAndObject(object, out);
            }
        }

        @Override
        public List read(Reader reader, DataInput in) throws IOException {
            int size = in.readInt();
            LinkedList<Object> list = new LinkedList<>();
            for (int i = 0; i < size; i++) {
                list.add(reader.readTypeAndObject(in));
            }
            return list;
        }

    }
}
