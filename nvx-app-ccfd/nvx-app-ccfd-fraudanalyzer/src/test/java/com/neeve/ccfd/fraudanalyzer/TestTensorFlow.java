/**
 * Copyright (c) 2018 Neeve Research & Consulting LLC. All Rights Reserved.
 * Confidential and proprietary information of Neeve Research & Consulting LLC.
 * CopyrightVersion 1.0
 */
package com.neeve.ccfd.fraudanalyzer;

import static com.neeve.ccfd.util.TestDataGenerator.DEFAULT_COUNTRY_CODE;
import static com.neeve.ccfd.util.TestDataGenerator.DEFAULT_POSTAL_CODE;
import static com.neeve.ccfd.util.TestDataGenerator.generateIdTo;
import static com.neeve.ccfd.util.TestDataGenerator.tempIdHolder;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeNoException;

import java.text.DecimalFormat;

import org.junit.Test;

import com.neeve.ccfd.messages.FraudAnalysisRequestMessage;
import com.neeve.ccfd.util.TestDataGenerator;
import com.neeve.stats.IStats.Latencies;
import com.neeve.stats.StatsFactory;
import com.neeve.test.UnitTest;
import com.neeve.util.UtlTime;

/**
 * Testcase for running with Tesnsorflow
 */
public class TestTensorFlow extends UnitTest {

    private final TestDataGenerator testDataGenerator = new TestDataGenerator(100);

    @Test
    public void testTensorFlow() throws Exception {
        TensorFlowFraudAnalyzer analyzer = null;
        try {
            analyzer = new TensorFlowFraudAnalyzer();
        }
        catch (UnsatisfiedLinkError e) {
            assumeNoException("Tensor flow not available on this platform", e);
        }
        catch (UnsupportedClassVersionError e) {
            assumeNoException("Tensor flow requires at least Java 7", e);
        }

        analyzer.open();
        try {
            int analyzed = 0;
            int incorrectPredictions = 0;
            Latencies latencies = StatsFactory.createLatencyStat("proc", 100000);
            StringBuilder sb = new StringBuilder();
            System.out.println(sb);
            for (; analyzed < 100000; analyzed++) {
                FraudAnalysisRequestMessage message = FraudAnalysisRequestMessage.create();
                message.setRequestIdFrom(generateIdTo(tempIdHolder()));
                message.setFlowStartTs(UtlTime.now());
                message.setNewTransaction(testDataGenerator.generateTransformedTransactionMessage());
                message.setCardHolderIdFrom(generateIdTo(tempIdHolder()));
                message.setMerchantStoreCountryCode(DEFAULT_COUNTRY_CODE);
                message.setMerchantStorePostcode(DEFAULT_POSTAL_CODE);

                boolean actuallyFraudulent = message.getNewTransaction().getFlaggedAsFraud();
                long now = System.nanoTime();
                boolean predictedFraudulent = analyzer.isFraudulent(message);
                latencies.add(System.nanoTime() - now);
                if (actuallyFraudulent != predictedFraudulent) {
                    incorrectPredictions++;
                }
                message.dispose();
            }

            float accuracy = (analyzed - incorrectPredictions) / ((float)analyzed);
            System.out.println("Accuracy: " + accuracy);
            sb.setLength(0);
            latencies.compute();
            latencies.get(sb, DecimalFormat.getNumberInstance());
            System.out.println(sb);
            assertTrue("Expected accuracy greater than 99.5% but was " + accuracy * 100, accuracy >= .995f);

        }
        finally {
            analyzer.close();
        }

    }
}