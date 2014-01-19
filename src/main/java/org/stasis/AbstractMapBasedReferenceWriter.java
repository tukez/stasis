package org.stasis;

import java.util.Map;

public abstract class AbstractMapBasedReferenceWriter implements ReferenceWriter {

    private final Map<Object, Integer> refs;

    public AbstractMapBasedReferenceWriter(Map<Object, Integer> refs) {
        this.refs = refs;
    }

    @Override
    public final int referenceFor(Object object) {
        Integer ref = refs.get(object);
        if (ref == null) {
            return -1;
        } else {
            return ref;
        }
    }

    @Override
    public final void registerObject(Object object) {
        refs.put(object, refs.size());
    }
}
