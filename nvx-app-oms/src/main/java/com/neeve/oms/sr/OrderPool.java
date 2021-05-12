package com.neeve.oms.sr;

import com.neeve.util.UtlPool;

import com.neeve.oms.state.Order;

final class OrderPool {
    final private static class PoolItem implements UtlPool.Item<PoolItem> {
        final Order order;
        private UtlPool<PoolItem> pool;

        public PoolItem(Order order) {
            this.order = order;
        }

        /*
         * Implementation of {@link Item#init()}
         */
        @Override
        final public PoolItem init() {
            return this;
        }

        /*
         * Implementation of {@link Item#setPool}
         */
        @Override
        final public PoolItem setPool(UtlPool<PoolItem> pool) {
            this.pool = pool;
            return this;
        }

        /*
         * Implementation of {@link Item#getPool}
         */
        @Override
        final public UtlPool<PoolItem> getPool() {
            return pool;
        }
    }

    final private static class PoolItemFactory implements UtlPool.Factory<PoolItem> {
        final public PoolItem createItem(final Object context) {
            return new PoolItem(Order.create());
        }

        final public PoolItem[] createItemArray(final int size) {
            return new PoolItem[size];
        }
    }

    final private UtlPool<PoolItem> pool;

    OrderPool(final int orderPreallocateCount) {
        pool = UtlPool.create("order",
                              "order",
                              new PoolItemFactory(),
                              UtlPool.Params.create().setThreaded(false).setInitialCapacity(orderPreallocateCount).setPreallocate(true));
    }

    final Order get() {
        return pool.get(null).order;
    }
}
