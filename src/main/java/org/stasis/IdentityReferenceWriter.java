package org.stasis;

import java.util.IdentityHashMap;

public class IdentityReferenceWriter implements ReferenceWriter {

    private final IdentityHashMap<Object, Integer> refs = new IdentityHashMap<>();

    @Override
    public int referenceFor(Object object) {
        Integer ref = refs.get(object);
        if (ref == null) {
            refs.put(object, refs.size());
            return -1;
        } else {
            return ref;
        }
    }

}
