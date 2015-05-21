package org.stasis;

import java.util.HashMap;

public class HashReferenceWriter extends AbstractMapBasedReferenceWriter {

    public HashReferenceWriter() {
        super(new HashMap<Object, Integer>());
    }

    @Override
    public void doClose() {
        // Nothing to do
    }

}
