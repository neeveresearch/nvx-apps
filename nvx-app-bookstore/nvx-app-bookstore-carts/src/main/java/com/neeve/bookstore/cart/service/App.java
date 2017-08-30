package com.neeve.bookstore.cart.service;

import java.util.Set;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import com.eaio.uuid.UUID;

import com.google.inject.Inject;
import com.google.inject.name.Names;

import com.neeve.aep.AepEngine;
import com.neeve.aep.annotations.EventHandler;
import com.neeve.aep.event.AepMessagingStartedEvent;
import com.neeve.ci.XRuntime;
import com.neeve.bookstore.cart.service.messages.DoDataMaintenanceRequest;
import com.neeve.service.IMessageScheduler;
import com.neeve.service.messages.MessageHeader;
import com.neeve.trace.Tracer;
import com.neeve.util.UtlProps;

@com.neeve.server.app.annotations.AppVersion(1)
public class App extends AbstractApp {
    /*
     * The do data maintenance executor thread factory
     */
    final private class DoDataMaintenanceExecutorThreadFactory implements ThreadFactory {
        private int num;

        final public Thread newThread(final Runnable r) {
            Thread thread = new Thread(r);
            thread.setName("Carts-DoDataMaintenanceExecutor-" + ++num);
            return thread;
        }
    }

    /*
     * The do data maintenance executor
     */
    final private class DoDataMaintenanceExecutor implements Runnable {
        /**
         * Implementation of {@link Runnable#run}
         */
        final public void run() {
            if (_engine != null && _engine.getState() == AepEngine.State.Started) {
                final DoDataMaintenanceRequest request = DoDataMaintenanceRequest.create();
                request.setHeader(MessageHeader.create());
                request.getHeader().setOrigin(APP_NAME);
                request.getHeader().setSourceId(String.valueOf(APP_PART));
                request.getHeader().setTransactionId(new UUID().toString());
                _messageScheduler.send(request);
            }
        }
    }
    // --- Injectable members
    @Inject
    private com.neeve.service.cdc.main.RunnerController _mainCdcRunnerController;
    @Inject
    private com.neeve.service.cdc.mps.RunnerController _mpsCdcRunnerController;
    @Inject
    private com.neeve.service.cdc.alert.RunnerController _alertCdcRunnerController;
    @Inject
    private IMessageScheduler _messageScheduler;
    // --- Injectable members

    // module level vars
    private ScheduledThreadPoolExecutor _doDataMaintenanceExecutor;

    // --- App version
    final public static int APP_MAJOR_VERSION = 1;
    final public static int APP_MINOR_VERSION = 0;
    // --- App version

    /**
     * Constructor
     */
    public App() {
        super(APP_MAJOR_VERSION, APP_MINOR_VERSION);
        _doDataMaintenanceExecutor = new ScheduledThreadPoolExecutor(1, new DoDataMaintenanceExecutorThreadFactory());
        _doDataMaintenanceExecutor.setExecuteExistingDelayedTasksAfterShutdownPolicy(false);
    }

    /**
     * Guice module configurer
     */
    final protected void configure() {
        // base class configurer
        super.configure();

        // DB config
        bind(String.class).annotatedWith(Names.named("nv.service.db.oracle.url")).toInstance(UtlProps.getValue(XRuntime.getProps(), "bookstore.carts.db.oracle.url", "jdbc:oracle:thin:@testdb.cebh0d49ki9c.us-east-1.rds.amazonaws.com:1521/BOOKSTORE"));
        bind(String.class).annotatedWith(Names.named("nv.service.db.oracle.username")).toInstance(UtlProps.getValue(XRuntime.getProps(), "bookstore.carts.db.oracle.username", "dbuser"));
        bind(String.class).annotatedWith(Names.named("nv.service.db.oracle.password")).toInstance(UtlProps.getValue(XRuntime.getProps(), "bookstore.carts.db.oracle.password", "dbpassword"));
        bind(Boolean.class).annotatedWith(Names.named("nv.service.db.oracle.createtables")).toInstance((Boolean)UtlProps.getValue(XRuntime.getProps(), "bookstore.carts.db.oracle.createtables", true));
        bind(Boolean.class).annotatedWith(Names.named("nv.service.db.oracle.createindexes")).toInstance((Boolean)UtlProps.getValue(XRuntime.getProps(), "bookstore.carts.db.oracle.createindexes", true));
        bind(Integer.class).annotatedWith(Names.named("nv.service.db.oracle.numreaderthreads")).toInstance((int)UtlProps.getValue(com.neeve.ci.XRuntime.getProps(), "bookstore.carts.db.oracle.numreaderthreads", 25));
        bind(Long.class).annotatedWith(Names.named("nv.service.db.oracle.reconnectfrequency")).toInstance((long)UtlProps.getValue(com.neeve.ci.XRuntime.getProps(), "bookstore.carts.db.oracle.reconnectfrequency", 1) * 1000L);
        bind(Long.class).annotatedWith(Names.named("nv.service.db.oracle.reconnectattemptduration")).toInstance((long)UtlProps.getValue(com.neeve.ci.XRuntime.getProps(), "bookstore.carts.db.oracle.reconnectattemptduration", 10 * 60) * 1000L);
        bind(Boolean.class).annotatedWith(Names.named("nv.service.db.influx.enabled")).toInstance(UtlProps.getValue(XRuntime.getProps(), "bookstore.carts.db.influx.enabled", false));
        bind(String.class).annotatedWith(Names.named("nv.service.db.influx.url")).toInstance(UtlProps.getValue(XRuntime.getProps(), "bookstore.carts.db.influx.url", "http://localhost:8086"));
        bind(String.class).annotatedWith(Names.named("nv.service.db.influx.username")).toInstance(UtlProps.getValue(XRuntime.getProps(), "bookstore.carts.db.influx.username", "root"));
        bind(String.class).annotatedWith(Names.named("nv.service.db.influx.password")).toInstance(UtlProps.getValue(XRuntime.getProps(), "bookstore.carts.db.influx.password", "root"));
        bind(String.class).annotatedWith(Names.named("nv.service.db.influx.dbname")).toInstance(UtlProps.getValue(XRuntime.getProps(), "bookstore.carts.db.influx.dbname", "bookstore"));

        // CDC config
        bind(Boolean.class).annotatedWith(Names.named("nv.service.cdc.mps.enabled")).toInstance((Boolean)UtlProps.getValue(XRuntime.getProps(), "bookstore.carts.cdc.mps.enabled", false));
        bind(Boolean.class).annotatedWith(Names.named("nv.service.cdc.mps.disableonfail")).toInstance((Boolean)UtlProps.getValue(XRuntime.getProps(), "bookstore.carts.cdc.mps.disableonfail", false));
        bind(Boolean.class).annotatedWith(Names.named("nv.service.cdc.alert.enabled")).toInstance((Boolean)UtlProps.getValue(XRuntime.getProps(), "bookstore.carts.cdc.alert.enabled", false));
        bind(Boolean.class).annotatedWith(Names.named("nv.service.cdc.alert.disableonfail")).toInstance((Boolean)UtlProps.getValue(XRuntime.getProps(), "bookstore.carts.cdc.alert.disableonfail", false));
        bind(Boolean.class).annotatedWith(Names.named("nv.service.cdc.enabled")).toInstance((Boolean)UtlProps.getValue(XRuntime.getProps(), "bookstore.carts.cdc.enabled", false));
        bind(Boolean.class).annotatedWith(Names.named("nv.service.cdc.disableonfail")).toInstance((Boolean)UtlProps.getValue(XRuntime.getProps(), "bookstore.carts.cdc.disableonfail", false));
    }

    /**
     * Implementation of {@link AbstractApp#doAddCommandHandlerContainers}
     */
    @Override
    final protected void doAddCommandHandlerContainers(final Set<Object> containers) {
        containers.add(_mpsCdcRunnerController);
        containers.add(_alertCdcRunnerController);
        containers.add(_mainCdcRunnerController);
    }

    /**
     * Do service specific initialization
     */
    @Override
    protected void doInitialize() {
        try {
            _mainCdcRunnerController.openCdc(false);
            _mainCdcRunnerController.startCdc();
        }
        catch (Exception ex) {
            _tracer.log("** Failed to open and start CDC: " + ex, Tracer.Level.SEVERE);
        }
    }

    /**
     * Do service specific finalization
     */
    @Override
    protected void doFinalize() {
        try {
            _mainCdcRunnerController.stopCdc(false);
            _mainCdcRunnerController.closeCdc();
        }
        catch (Exception ex) {
            _tracer.log("** Failed to stop and close CDC: " + ex, Tracer.Level.WARNING);
        }
    }

    /**
     * Messaging has started
     */
    @EventHandler
    final public void onMessagingStarted2(final AepMessagingStartedEvent event) {
        if (event.getStatus() == null) {
            if (_doDataMaintenanceExecutor != null) {
                int interval = (int) UtlProps.getValue(XRuntime.getProps(), "bookstore.carts.datamaintenance.interval", 24 * 60);
                _tracer.log("Scheduling data maintenance now and after every " + interval + " minutes.", Tracer.Level.INFO);
                _doDataMaintenanceExecutor.scheduleWithFixedDelay(new DoDataMaintenanceExecutor(), 0, interval, TimeUnit.MINUTES);
            }
        }
    }
}
