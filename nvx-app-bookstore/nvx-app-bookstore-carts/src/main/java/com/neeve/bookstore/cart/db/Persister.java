/**
 * Copyright (c) 2015 Neeve Research, LLC. All Rights Reserved.
 * Confidential and proprietary information of Neeve Research, LLC.
 * Copyright Version 1.0
 */
package com.neeve.bookstore.cart.db;

import com.google.inject.*;

import com.eaio.uuid.UUID;

import com.neeve.service.cdc.main.DbPersister;
import com.neeve.service.cdc.main.IDbTracer;
import com.neeve.service.cdc.IDbReaderCallback;

import com.neeve.bookstore.cart.service.repository.Cart;
import com.neeve.bookstore.cart.service.repository.Item;
import com.neeve.bookstore.cart.service.repository.Partition;

/**
 * The cart service persister
 */
final public class Persister extends DbPersister {
    private PartitionPersister _partitionPersister;
    private CartPersister _cartPersister;
    private ItemPersister _itemPersister;

    @Inject
    public Persister(final IDbTracer dbTracer) {
        super(dbTracer);
        _partitionPersister = new PartitionPersister(this);
        _cartPersister = new CartPersister(this);
        _itemPersister = new ItemPersister(this);
    }

    final public PartitionPersister getPartitionPersister() {
        return _partitionPersister;
    }

    final public CartPersister getCartPersister() {
        return _cartPersister;
    }

    final public ItemPersister getItemPersister() {
        return _itemPersister;
    }

    @Override
    final protected void doOpen() throws Exception {
        _partitionPersister.open(_rdbmsConnection, _rdbmsCreateTables, _rdbmsCreateIndexes);
        _cartPersister.open(_rdbmsConnection, _rdbmsCreateTables, _rdbmsCreateIndexes);
        _itemPersister.open(_rdbmsConnection, _rdbmsCreateTables, _rdbmsCreateIndexes);
    }

    @Override
    final public void read(final UUID parentId, final IDbReaderCallback cb) throws Exception {
    }

    @Override
    final protected void doUpdate(Object object) throws Exception {
        if (object instanceof Partition) {
            _partitionPersister.update((Partition)object);
        }
        else if (object instanceof Cart) {
            _cartPersister.update((Cart)object);
        }
        else if (object instanceof Item) {
            _itemPersister.update((Item)object);
        }
    }

    @Override
    final protected void doDelete(final UUID id) throws Exception {
        if (_partitionPersister.delete(id.toString()) > 0) {
            return;
        }
        if (_cartPersister.delete(id.toString()) > 0) {
            return;
        }
        if (_itemPersister.delete(id.toString()) > 0) {
            return;
        }
    }

    @Override
    final protected void doClose() {
        _cartPersister.close();
    }
}
