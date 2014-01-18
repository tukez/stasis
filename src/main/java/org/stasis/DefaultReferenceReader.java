package org.stasis;

import java.util.ArrayList;
import java.util.List;

public class DefaultReferenceReader implements ReferenceReader {

    private final List<Object> objects = new ArrayList<>();

    @Override
    public Object objectFor(int ref) {
        return objects.get(ref);
    }

    @Override
    public void registerObject(Object object) {
        objects.add(object);
    }

}
