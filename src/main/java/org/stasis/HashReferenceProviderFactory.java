package org.stasis;

public class HashReferenceProviderFactory implements ReferenceProviderFactory {

    @Override
    public ReferenceWriter createWriter() {
        return new HashReferenceWriter();
    }

    @Override
    public ReferenceReader createReader() {
        return new DefaultReferenceReader();
    }
}
