/**
 * Copyright (c) 2018 Neeve Research & Consulting LLC. All Rights Reserved.
 * Confidential and proprietary information of Neeve Research & Consulting LLC.
 * CopyrightVersion 1.0
 */
package com.neeve.ccfd.fraudanalyzer;

import com.neeve.ccfd.messages.FraudAnalysisRequestMessage;
import com.neeve.ci.XRuntime;

/**
 * A mock analyzer which simulates some processing time.  
 */
public class MockFraudAnalyzer implements FraudAnalyzer {
    long mockProcessingTime = XRuntime.getValue("ccfd.mockProcessingTime", 90000l);

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
        //Simulate TensorFlow processing time:
        long ts = System.nanoTime();
        while ((System.nanoTime() - ts) < mockProcessingTime) {}
        return request.getNewTransaction().getFlaggedAsFraud();
    }

}
