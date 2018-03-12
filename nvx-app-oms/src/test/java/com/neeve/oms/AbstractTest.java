package com.neeve.oms;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.HashSet;
import java.util.Properties;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.rules.TestName;

import com.neeve.server.embedded.EmbeddedXVM;
import com.neeve.util.UtlFile;

public class AbstractTest {
    @Rule
    public TestName testcaseName = new TestName();

    protected static final HashSet<EmbeddedXVM> xvms = new HashSet<EmbeddedXVM>();

    @BeforeClass
    public static void unitTestIntialize() throws IOException {
        File testRoot = getTestbedRoot();
        System.setProperty("NVROOT", testRoot.getCanonicalPath());

        File rdat = new File(testRoot, "rdat");
        if (rdat.exists()) {
            UtlFile.deleteDirectory(rdat);
        }

        if (!testRoot.exists()) {
            testRoot.mkdirs();
        }
    }

    protected static File getProjectBaseDirectory() {
        final String basedir = System.getProperty("basedir");
        if (basedir != null) {
            return new File(basedir);
        }
        return new File(".");
    }

    protected static File getTestbedRoot() {
        return new File(getProjectBaseDirectory(), "target/testbed");
    }

    @AfterClass
    public static void cleanup() throws Throwable {
        Throwable error = null;
        for (EmbeddedXVM xvm : xvms) {
            try {
                xvm.shutdown();
            }
            catch (Throwable thrown) {
                if (error != null) {
                    error = thrown;
                }
                thrown.printStackTrace();
            }
        }

        System.clearProperty("NVROOT");

        if (error != null) {
            throw error;
        }
    }

    @SuppressWarnings("unchecked")
    public <T> T startApp(Class<T> appClass, String appName, String xvmName, Properties env) throws Throwable {
        URL ddlConfig = new File(getProjectBaseDirectory(), "/conf/config.xml").toURI().toURL();
        env.setProperty("x.env.nv.data.directory", "rdat/" + xvmName);
        EmbeddedXVM xvm = EmbeddedXVM.create(ddlConfig, xvmName, env);
        xvms.add(xvm);
        xvm.start();
        return (T)xvm.getApplication(appName);
    }
}
