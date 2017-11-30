/**
 * Copyright (c) 2015 Neeve Research & Consulting LLC. All Rights Reserved.
 * Confidential and proprietary information of Neeve Research & Consulting LLC.
 * CopyrightVersion 1.0
 */
package com.neeve.tick2trade;

import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.util.HashSet;
import java.util.Properties;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;

import com.neeve.aep.AepEngine;
import com.neeve.server.embedded.EmbeddedXVM;
import com.neeve.test.UnitTest;

/**
 * Base class with some helper methods for creating embedded vms.
 */
public class AbstractTest extends UnitTest {
    protected final HashSet<EmbeddedXVM> xvms = new HashSet<EmbeddedXVM>();
    private URL confidDDL;
    protected Properties profileProps;

    @BeforeClass
    public static void unitTestIntialize() throws IOException {
        UnitTest.unitTestIntialize();
    }

    @Before
    public void beforeTestcase() throws FileNotFoundException, IOException {
        confidDDL = new File(getProjectBaseDirectory(), "conf/config.xml").toURI().toURL();
        profileProps = new Properties();
        profileProps.setProperty("nv.ddl.profiles", "desktop");
    }

    @After
    public void afterTestCase() throws IOException {
        for (EmbeddedXVM xvm : xvms) {
            xvm.shutdown(true);
        }
    }

    @SuppressWarnings("unchecked")
    public <T> T startApp(Class<T> appClass, String appName, String xvmName) throws Throwable {
        EmbeddedXVM server = EmbeddedXVM.create(confidDDL, xvmName, profileProps);
        xvms.add(server);
        server.start();
        return (T)server.getApplication(appName);
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
