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

    /**
     * Generates String id field from UUID.
     */
    public static final String generateId() {
        String retVal = UUID.randomUUID().toString();
        return retVal;
    }
}
