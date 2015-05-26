package org.stasis.pool;

public interface ObjectFactory<A> {

    A create();

    void onBorrow(A object);

    void onRelease(A object);

}
