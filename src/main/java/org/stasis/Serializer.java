package org.stasis;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public interface Serializer<A> {

    void write(Stasis.Writer writer, DataOutputStream out, A value) throws IOException;

    A read(Stasis.Reader reader, DataInputStream in) throws IOException;
}
