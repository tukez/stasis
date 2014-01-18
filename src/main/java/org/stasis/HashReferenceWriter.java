package org.stasis;

import java.util.HashMap;

public class HashReferenceWriter implements ReferenceWriter {

    private final HashMap<Object, Integer> refs = new HashMap<>();

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
