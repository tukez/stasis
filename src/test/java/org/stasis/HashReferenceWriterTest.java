package org.stasis;

import org.junit.Assert;
import org.junit.Test;

public class HashReferenceWriterTest {

    private HashReferenceWriter writer = new HashReferenceWriter();

    @Test
    public void refsAreAssignedCorrectly() {
        int ref1 = writer.referenceFor("obj1");
        Assert.assertTrue("reference not found", ref1 < 0);

        writer.registerObject("obj1");
        int ref2 = writer.referenceFor("obj1");
        Assert.assertEquals("reference found", 0, ref2);

        int ref3 = writer.referenceFor("obj2");
        Assert.assertTrue("reference not found", ref3 < 0);

        writer.registerObject("obj2");
        int ref4 = writer.referenceFor("obj2");
        Assert.assertEquals("reference found", 1, ref4);

        int ref5 = writer.referenceFor("obj1");
        Assert.assertEquals("reference found", 0, ref5);
    }

    @Test
    public void objectsAreRecognizedByHashcodeAndEquals() {
        Integer obj1 = new Integer(0);
        Integer obj2 = new Integer(0);

        int ref1 = writer.referenceFor(obj1);
        Assert.assertTrue("reference not found", ref1 < 0);

        writer.registerObject(obj1);
        int ref2 = writer.referenceFor(obj2);
        Assert.assertEquals("reference found", 0, ref2);
    }

    @Test(expected = IllegalStateException.class)
    public void usingWriterAfterCloseIsIllegal() {
        writer.close();
        writer.referenceFor("obj");
    }
}
