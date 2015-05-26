package org.stasis.pool;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class StaticObjectPool<A> implements ObjectPool<A> {

    private final BlockingQueue<A> pool;
    private final ObjectFactory<A> factory;

    public StaticObjectPool(int size, ObjectFactory<A> factory) {
        this.pool = new ArrayBlockingQueue<>(size);
        this.factory = factory;

        for (int i = 0; i < size; i++) {
            if (!pool.offer(factory.create())) {
                throw new IllegalStateException("Could not add initial objects to pool.");
            }
        }
    }

    @Override
    public A borrow() {
        try {
            A object = pool.take();
            factory.onBorrow(object);
            return object;
        } catch (InterruptedException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public boolean release(A object) {
        factory.onRelease(object);
        return pool.offer(object);
    }

}
