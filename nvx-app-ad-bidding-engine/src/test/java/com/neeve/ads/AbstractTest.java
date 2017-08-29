/**
 * Copyright (c) 2015 Neeve Research & Consulting LLC. All Rights Reserved.
 * Confidential and proprietary information of Neeve Research & Consulting LLC.
 * CopyrightVersion 1.0
 */
package com.neeve.ads;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.net.URL;
import java.util.HashSet;
import java.util.Properties;

import org.junit.After;
import org.junit.Before;

import com.neeve.aep.AepEngine;
import com.neeve.ci.XRuntime;
import com.neeve.config.VMConfigurer;
import com.neeve.server.Configurer;
import com.neeve.server.embedded.EmbeddedXVM;
import com.neeve.test.UnitTest;
import com.neeve.util.UtlTailoring;
import com.neeve.util.UtlTailoring.PropertySource;

public class AbstractTest extends UnitTest {
    private static volatile boolean vmConfigured = false;
    protected final HashSet<EmbeddedXVM> servers = new HashSet<EmbeddedXVM>();

    @Before
    public void beforeTestcase() {
        vmConfigured = false;
    }

    @Before
    public void afterTestcase() {
        vmConfigured = false;
    }

    @After
    public void cleanup() throws Throwable {
        Throwable error = null;
        for (EmbeddedXVM server : servers) {
            try {
                server.shutdown();
            }
            catch (Throwable thrown) {
                if (error != null) {
                    error = thrown;
                }
                thrown.printStackTrace();
            }
        }

        if (error != null) {
            throw error;
        }
    }

    private static class TestConfigurer implements Configurer, PropertySource {
        private final String serverName;
        private final PropertySource envResolver = UtlTailoring.ENV_SUBSTITUTION_RESOLVER;

        TestConfigurer(final String serverName, Properties env) {
            this.serverName = serverName;
            XRuntime.updateProps(env);
        }

        /* (non-Javadoc)
         * @see com.neeve.server.Configurer#configure(java.lang.String[])
         */
        @Override
        public String[] configure(String[] args) throws Exception {
            if (!vmConfigured) {
                URL overlayUrl = new File(getProjectBaseDirectory(), "conf/config.xml").toURI().toURL();
                File overlayConfig = new File(overlayUrl.toURI());
                VMConfigurer.configure(overlayConfig, this);
                vmConfigured = true;
            }
            return new String[] { "--name", serverName };
        }

        /* (non-Javadoc)
         * @see com.neeve.util.UtlTailoring.PropertySource#getValue(java.lang.String, java.lang.String)
         */
        @Override
        public String getValue(String key, String defaultValue) {
            return envResolver.getValue(key, defaultValue);
        }
    }

    @SuppressWarnings("unchecked")
    public <T> T startApp(Class<T> appClass, String appName, String xvmName, Properties env) throws Throwable {
        TestConfigurer configurer = new TestConfigurer(xvmName, env);
        EmbeddedXVM server = EmbeddedXVM.create(configurer);
        servers.add(server);
        server.start();
        return (T)server.getApplication(appName);
    }

    final protected void waitForTransactionPipelineToEmpty(final AepEngine engine) throws Exception {
        int i;
        for (i = 0; i < 100; i++) {
            final long numCommitsPending = (engine.getStats().getNumCommitsStarted() - engine.getStats().getNumCommitsCompleted());
            if (numCommitsPending == 0l) {
                break;
            }
            else {
                System.out.println("Waiting for transaction pipeline to empty remaining: " + numCommitsPending);
                Thread.sleep(100l);
            }
        }
        assertTrue(i < 100);
    }
}
