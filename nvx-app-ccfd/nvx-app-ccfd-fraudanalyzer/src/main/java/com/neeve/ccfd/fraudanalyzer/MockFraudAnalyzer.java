/**
 * Copyright (c) 2018 Neeve Research & Consulting LLC. All Rights Reserved.
 * Confidential and proprietary information of Neeve Research & Consulting LLC.
 * CopyrightVersion 1.0
 */
package com.neeve.ccfd.fraudanalyzer;

import com.neeve.ccfd.messages.FraudAnalysisRequestMessage;

/**
 * A mock analyzer which simulates some processing time.  
 */
public class MockFraudAnalyzer implements FraudAnalyzer {

    /* (non-Javadoc)
     * @see com.neeve.ccfd.fraudanalyzer.FraudAnalyzer#open()
     */
    @Override
    public final void open() throws Exception {}

    /* (non-Javadoc)
     * @see com.neeve.ccfd.fraudanalyzer.FraudAnalyzer#close()
     */
    @Override
    public final void close() throws Exception {}

    /* (non-Javadoc)
     * @see com.neeve.ccfd.fraudanalyzer.FraudAnalyzer#isFraudulent(com.neeve.ccfd.messages.FraudAnalysisRequestMessage)
     */
    @Override
    public final boolean isFraudulent(FraudAnalysisRequestMessage request) {
        long ts = System.nanoTime();
        while ((System.nanoTime() - ts) < 10000)
            ;
        return request.getNewTransaction().getFlaggedAsFraud();
    }

}
