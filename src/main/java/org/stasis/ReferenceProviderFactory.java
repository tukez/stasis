package org.stasis;

public interface ReferenceProviderFactory {

    ReferenceWriter createWriter();

    ReferenceReader createReader();
}
