package org.stasis;

import java.util.Map;

public abstract class AbstractMapBasedReferenceWriter implements
        ReferenceWriter {

    private final Map<Object, Integer> refs;
    private boolean open = true;

    public AbstractMapBasedReferenceWriter(Map<Object, Integer> refs) {
        this.refs = refs;
    }

    @Override
    public final int referenceFor(Object object) {
        ensureIsOpen();
        Integer ref = refs.get(object);
        if (ref == null) {
            return -1;
        } else {
            return ref;
        }
    }

    @Override
    public final void registerObject(Object object) {
        ensureIsOpen();
        refs.put(object, refs.size());
    }

    @Override
    public final void close() {
        ensureIsOpen();
        doClose();
        open = false;
    }

    private void ensureIsOpen() {
        if (!open) {
            throw new IllegalStateException("ReferenceWriter is closed.");
        }
    }

    protected abstract void doClose();
}
