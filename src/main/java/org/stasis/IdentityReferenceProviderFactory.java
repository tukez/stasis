package org.stasis;

public class IdentityReferenceProviderFactory implements ReferenceProviderFactory {

    @Override
    public ReferenceWriter createWriter() {
        return new IdentityReferenceWriter();
    }

    @Override
    public ReferenceReader createReader() {
        return new DefaultReferenceReader();
    }

}
