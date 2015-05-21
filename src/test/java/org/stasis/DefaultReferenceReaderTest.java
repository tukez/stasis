package org.stasis;

import org.junit.Assert;
import org.junit.Test;

public class DefaultReferenceReaderTest {

    private DefaultReferenceReader reader = new DefaultReferenceReader();

    @Test(expected = Exception.class)
    public void tryingToGetObjectBeforeRegisteringThrowsException() {
        reader.objectFor(0);
        reader.close();
    }

    @Test
    public void afterRegisteringObjectTheReferencingIsPossible() {
        reader.registerObject("obj1");
        reader.registerObject("obj2");

        Assert.assertEquals("obj1", reader.objectFor(0));
        Assert.assertEquals("obj2", reader.objectFor(1));

        reader.close();
    }

    @Test(expected = IllegalStateException.class)
    public void usingReaderAfterCloseIsIllegal() {
        reader.registerObject("obj1");
        reader.close();
        reader.objectFor(0);
    }
}
