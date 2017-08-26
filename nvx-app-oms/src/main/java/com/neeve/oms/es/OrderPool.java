package com.neeve.oms.es;

import com.neeve.util.UtlPool;

import com.neeve.oms.es.domain.Order;

final class OrderPool {
    final private static class OrderFactory implements UtlPool.Factory<Order> {
        final public Order createItem(final Object context) {
            return new Order();
        }

        final public Order[] createItemArray(final int size) {
            return new Order[size];
        }
    }

    final private UtlPool<Order> pool;

    OrderPool(final int orderPreallocateCount) {
        pool = UtlPool.create("order", 
                              "order", 
                              new OrderFactory(), 
                              UtlPool.Params.create().setThreaded(false).setInitialCapacity(orderPreallocateCount).setPreallocate(true));
    }

    final Order get() {
        return pool.get(null);
    }
}
