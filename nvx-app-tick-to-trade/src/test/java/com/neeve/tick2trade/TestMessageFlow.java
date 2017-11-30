/**
 * Copyright (c) 2015 Neeve Research & Consulting LLC. All Rights Reserved.
 * Confidential and proprietary information of Neeve Research & Consulting LLC.
 * CopyrightVersion 1.0
 */
package com.neeve.tick2trade;

import org.junit.Test;

import static org.junit.Assert.*;

import com.neeve.ci.XRuntime;
import com.neeve.tick2trade.driver.Client;
import com.neeve.tick2trade.driver.Market;

/**
 * Tests end to end message flow by starting embedded servers and
 * verifying that the client receives all fills.   
 */
public class TestMessageFlow extends AbstractTest {

    @Test
    public void testMessageFlow() throws Throwable {
        Market market = startApp(Market.class, "market", "market");
        App app = startApp(App.class, "ems", "ems1");
        market.getEngine().waitForMessagingToStart();
        Thread.sleep(10000);
        Client client = startApp(Client.class, "client", "client");

        int expectedSendCount = XRuntime.getValue("simulator.sendCount", 0);

        System.out.println("Waiting for Client trades");
        client.waitForTrades(30);

        //Wait for the ems's transaction pipeline to ensure no duplicates:
        waitForTransactionPipelineToEmpty(app.getEngine(), 1000);

        assertEquals("Unexpected number of OMSNewOrderSingles", expectedSendCount, client.waitForTrades(0));
    }
}
