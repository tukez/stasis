package org.stasis.pool;

public interface ObjectPool<A> {

    A borrow();

    boolean release(A object);

}
