package org.stasis;

public interface ReferenceWriter {

    int referenceFor(Object object);

    void registerObject(Object object);
}
