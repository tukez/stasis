package org.stasis.pool;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicInteger;

public class DynamicObjectPool<A> implements ObjectPool<A> {

    private final BlockingQueue<A> pool;
    private final ObjectFactory<A> factory;

    private final int maxSize;
    private final AtomicInteger size = new AtomicInteger(0);

    public DynamicObjectPool(int maxSize, ObjectFactory<A> factory) {
        this.maxSize = maxSize;
        this.pool = new LinkedBlockingDeque<>();
        this.factory = factory;
    }

    @Override
    public A borrow() {
        A object = pool.poll();
        if (object == null) {
            if (size.get() < maxSize && this.size.incrementAndGet() <= maxSize) {
                object = factory.create();
            } else {
                try {
                    object = pool.take();
                } catch (InterruptedException e) {
                    throw new IllegalStateException(e);
                }
            }
        }
        factory.onBorrow(object);
        return object;
    }

    @Override
    public boolean release(A object) {
        factory.onRelease(object);
        return pool.offer(object);
    }

}
