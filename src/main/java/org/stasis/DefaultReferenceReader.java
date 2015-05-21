package org.stasis;

import java.util.ArrayList;
import java.util.List;

public class DefaultReferenceReader implements ReferenceReader {

    private final List<Object> objects = new ArrayList<>();
    private boolean open = true;

    @Override
    public Object objectFor(int ref) {
        ensureIsOpen();
        return objects.get(ref);
    }

    @Override
    public void registerObject(Object object) {
        ensureIsOpen();
        objects.add(object);
    }

    @Override
    public void close() {
        ensureIsOpen();
        open = false;
    }

    private void ensureIsOpen() {
        if (!open) {
            throw new IllegalStateException("ReferenceReader is closed.");
        }
    }

}
