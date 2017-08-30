package com.neeve.bookstore.cart.service.domain;

import com.neeve.service.IdentityInformationProvider;

import com.neeve.bookstore.cart.service.repository.Repository;
import com.neeve.bookstore.cart.service.repository.Partition;

/**
 * Implement unique numeric cart id creation
 */
final public class CartId {
    final private static int PARTITION_OFFSET = 0;
    final private static long PARTITION_MASK = 0x00000000000000ffl;
    final private static int COUNTER_OFFSET = 8;
    final private static long COUNTER_MASK = 0x7fffffffffffff00l;
    /*
     * Create a new cart id.
     *
     * @param partition An instance of a partition
     */
    final public static long create(final Partition partition) {
        if (partition == null) {
            throw new IllegalArgumentException("partition cannot be null");
        }
        partition.setNextCartId(partition.getNextCartId() + 1);
        return ((partition.getPartitionId() << PARTITION_OFFSET) & PARTITION_MASK) |
               ((partition.getNextCartId()<< COUNTER_OFFSET) & COUNTER_MASK);
    }

    /*
     * Create a new cart id.
     *
     * @param repository An instance of the running server's repository.
     */
    final public static long create(final Repository repository, final IdentityInformationProvider identityInformationProvider) {
        if (repository == null) {
            throw new IllegalArgumentException("repository cannot be null");
        }
        final int partitionId = identityInformationProvider.getPartition();
        Partition partition = repository.getPartitions().get((long)partitionId);
        if (partition == null) {
            partition = Partition.create();
            partition.setPartitionId(partitionId);
            repository.getPartitions().put((long)partitionId, partition);
        }
        return create(partition);
    }

    /**
     * Get the partition that owns a cart id.
     *
     * @param cartId The cart id whose partition is to be returned.
     */
    final public static int getPartition(final long cartId) {
        if (cartId <= 0) {
            throw new IllegalArgumentException("invalid cart id '" + cartId + "'");
        }
        return (int)(cartId & PARTITION_MASK);
    }
}

