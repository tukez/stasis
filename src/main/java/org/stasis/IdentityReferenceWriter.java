package org.stasis;

import java.util.IdentityHashMap;

public class IdentityReferenceWriter extends AbstractMapBasedReferenceWriter {

    public IdentityReferenceWriter() {
        super(new IdentityHashMap<Object, Integer>());
    }

    @Override
    public void doClose() {
        // Nothing to do
    }

}
