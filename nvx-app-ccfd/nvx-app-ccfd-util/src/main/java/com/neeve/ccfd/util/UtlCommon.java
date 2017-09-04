/**
 * Copyright (c) 2015 Neeve Research, LLC. All Rights Reserved.
 * Confidential and proprietary information of Neeve Research, LLC.
 * CopyrightVersion 1.0
 */
package com.neeve.ccfd.util;

import java.util.UUID;

/**
 * Common utility methods used in Fraud Detection Application
 *
 */
public class UtlCommon {

    private static String shardKeys[] = null;

    /**
     * Generates String id field from UUID.
     */
    public static final String generateId() {
        String retVal = UUID.randomUUID().toString();
        return retVal;
    }

    /**
     * Converts numerical key into shard id
     * @param key Key to map to shard id.
     * @param numShards Number of shards.
     * @return Shard ID.
     */
    public static final String getShardKey(final String key, final int numShards) {
        if (key == null || key.isEmpty()) {
            throw new IllegalArgumentException("'key' cannot be null or empty string.");
        }
        if (numShards < 1) {
            throw new IllegalArgumentException("'numShards' must be positive integer.");
        }

        // we will cache shard keys so not to generate new String instances every time converting integer to string
        if (shardKeys == null) {
            shardKeys = new String[50];
            for (int i = 0; i < 50; i++) {
                shardKeys[i] = Integer.toString(i);
            }
        }

        return shardKeys[Math.abs(key.hashCode()) % numShards];
    }
}
