package org.stasis;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
 
public interface Serializer<A> {

    void write(Stasis.Writer writer, DataOutput out, A value) throws IOException;

    A read(Stasis.Reader reader, DataInput in) throws IOException;
}
