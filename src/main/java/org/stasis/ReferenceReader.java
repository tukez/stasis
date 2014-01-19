package org.stasis;

public interface ReferenceReader {

    /**
     * Return the object for the given reference.
     */
    Object objectFor(int ref);

    /**
     * Register object to this reader. After registering object,
     * referenceFor(ref) returns an object for the appropriate reference.
     */
    void registerObject(Object object);
}
