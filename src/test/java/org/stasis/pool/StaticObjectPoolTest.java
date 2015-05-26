package org.stasis.pool;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Assert;
import org.junit.Test;

public class StaticObjectPoolTest {

    @Test
    public void sizeIsStatic() {
        final AtomicInteger created = new AtomicInteger(0);
        StaticObjectPool<Object> pool = new StaticObjectPool<>(3, new ObjectFactory<Object>() {

            @Override
            public Object create() {
                created.incrementAndGet();
                return new Object();
            }

            @Override
            public void onBorrow(Object object) {
            }

            @Override
            public void onRelease(Object object) {
            }

        });

        Assert.assertEquals(3, created.get());

        Object object1 = pool.borrow();
        @SuppressWarnings("unused")
        Object object2 = pool.borrow();
        @SuppressWarnings("unused")
        Object object3 = pool.borrow();

        pool.release(object1);

        @SuppressWarnings("unused")
        Object object4 = pool.borrow();
        Assert.assertEquals(3, created.get());
    }

    @Test
    public void onBorrowAndOnReleaseAreCalled() {
        final List<Object> borrows = new ArrayList<>();
        final List<Object> releases = new ArrayList<>();
        StaticObjectPool<Object> pool = new StaticObjectPool<>(3, new ObjectFactory<Object>() {

            @Override
            public Object create() {
                return new Object();
            }

            @Override
            public void onBorrow(Object object) {
                borrows.add(object);
            }

            @Override
            public void onRelease(Object object) {
                releases.add(object);
            }

        });

        Assert.assertEquals(Arrays.asList(), borrows);
        Assert.assertEquals(Arrays.asList(), releases);

        Object object1 = pool.borrow();
        Object object2 = pool.borrow();

        Assert.assertEquals(Arrays.asList(object1, object2), borrows);
        Assert.assertEquals(Arrays.asList(), releases);

        pool.release(object2);
        pool.release(object1);

        Assert.assertEquals(Arrays.asList(object1, object2), borrows);
        Assert.assertEquals(Arrays.asList(object2, object1), releases);
    }
}
