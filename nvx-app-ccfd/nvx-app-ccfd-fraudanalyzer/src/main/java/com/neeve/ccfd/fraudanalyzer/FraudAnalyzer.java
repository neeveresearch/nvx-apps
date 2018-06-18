/**
 * Copyright (c) 2018 Neeve Research & Consulting LLC. All Rights Reserved.
 * Confidential and proprietary information of Neeve Research & Consulting LLC.
 * CopyrightVersion 1.0
 */
package com.neeve.ccfd.fraudanalyzer;

import com.neeve.ccfd.messages.FraudAnalysisRequestMessage;

/**
 * Defines the fraud analyzer interface. 
 */
public interface FraudAnalyzer {
    public void open() throws Exception;

    /**
     * Closes the fraud analyzer. 
     * 
     * @throws Exception If there is an error closing the fraud analyzer. 
     * @threading This method is not safe for concurrent access by multiple threads. 
     */
    public void close() throws Exception;

    /**
     * Analyzes a fraud analysis request to determin if it appears to be fraudulent. 
     * 
     * @param request The request to analyze 
     * @return True if this request is deemed to be fraudulent. 
     */
    public boolean isFraudulent(FraudAnalysisRequestMessage request);
}
