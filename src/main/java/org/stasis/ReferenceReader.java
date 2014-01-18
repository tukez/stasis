package org.stasis;

public interface ReferenceReader {

    Object objectFor(int ref);

    void registerObject(Object object);
}
