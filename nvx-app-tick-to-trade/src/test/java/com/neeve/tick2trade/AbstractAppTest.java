/**
 * Copyright (c) 2015 Neeve Research & Consulting LLC. All Rights Reserved.
 * Confidential and proprietary information of Neeve Research & Consulting LLC.
 * CopyrightVersion 1.0
 */
package com.neeve.tick2trade;

import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.util.Properties;

import org.junit.Before;
import org.junit.BeforeClass;

import com.neeve.aep.AepEngine;
import com.neeve.server.embedded.EmbeddedServer;
import com.neeve.server.embedded.EmbeddedXVM;
import com.neeve.test.UnitTest;
import com.neeve.tick2trade.driver.Client;
import com.neeve.tick2trade.driver.Market;

/**
 * Base class with some helper methods for creating embedded vms.
 */
public class AbstractAppTest extends UnitTest {
    private static final String desktopConf = "conf/profiles/desktop/application.conf";
    private URL confidDDL;
    private Properties profileProps;

    @BeforeClass
    public static void unitTestIntialize() throws IOException {
        UnitTest.unitTestIntialize();
    }

    @Before
    public void beforeTestcase() throws FileNotFoundException, IOException {
        confidDDL = new File(getProjectBaseDirectory(), "conf/config.xml").toURI().toURL();
        profileProps = new Properties();
        profileProps.load(new FileInputStream(new File(getProjectBaseDirectory(), desktopConf)));
    }

    @Before
    public void afterTestcase() {}

    public App startEmsPrimary() throws Throwable {
        EmbeddedXVM server = EmbeddedServer.create(confidDDL, "ems1", profileProps);
        server.start();
        return (App)server.getApplication("ems");
    }

    public App startEmsBackup() throws Throwable {
        EmbeddedXVM server = EmbeddedServer.create(confidDDL, "ems2", profileProps);
        server.start();
        return (App)server.getApplication("ems");
    }

    public Market startMarket() throws Throwable {
        EmbeddedXVM server = EmbeddedServer.create(confidDDL, "market", profileProps);
        server.start();
        return (Market)server.getApplication("market");
    }

    public Client startClient() throws Throwable {
        EmbeddedXVM server = EmbeddedServer.create(confidDDL, "client", profileProps);
        server.start();
        return (Client)server.getApplication("client");
    }

    final protected void waitForTransactionPipelineToEmpty(final AepEngine engine, long timeout) throws Exception {
        timeout = System.currentTimeMillis() + timeout;
        while (true) {
            final long numCommitsPending = (engine.getStats().getNumCommitsStarted() - engine.getStats().getNumCommitsCompleted());
            if (numCommitsPending == 0l) {
                break;
            }
            else {
                if (timeout < System.currentTimeMillis()) {
                    System.out.println("Waiting for transaction pipeline to empty, remaining: " + numCommitsPending);
                    Thread.sleep(1000l);
                }
                else {
                    fail("Timed out waiting for transaction pipeline to empty, remaining: " + numCommitsPending);
                }
            }
        }

    }
}
