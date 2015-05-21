package org.stasis;

public interface ReferenceWriter {

    /**
     * Get reference for the given object or -1 if no reference was found.
     */
    int referenceFor(Object object);

    /**
     * Register object to this writer. After registering object,
     * referenceFor(object) returns a reference for this object.
     */
    void registerObject(Object object);
    
    /**
     * Close this writer to free up reserved resources.
     * 
     * @throws IllegalStateException
     *             if this writer is used after call to close().
     */
    void close();
}
