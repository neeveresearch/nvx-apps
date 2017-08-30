/**
 * Copyright (c) 2015 Neeve Research, LLC. All Rights Reserved.
 * Confidential and proprietary information of Neeve Research, LLC.
 * Copyright Version 1.0
 */
package com.neeve.bookstore.cart.db;

import com.google.inject.*;

import com.neeve.root.*;
import com.neeve.trace.*;

import com.neeve.service.cdc.main.*;

/**
 * The Bookstore Cart DB module.
 */
final public class Module extends AbstractModule {
    final private static class DbTracer implements IDbTracer, ICdcTracer {
        final private Tracer _tracer;

        DbTracer(final Tracer tracer) {
            _tracer = tracer;
        }

        final public Tracer getTracer() {
            return _tracer;
        }
    }

    /*
     * Private scope members
     */
    final private static DbTracer _dbTracer;

    /**
     * Static constructor
     */
    static {
        /*
         * DB tracer. The statement below causes default trace output to be controllable
         * via the 'bookstore.bookstore.db.trace' JVM system property or environment variable. Set
         * bookstore.bookstore.db.trace=debug in the environment before launching the service to enable
         * debug level trace in the rds DB tracer.
         */
        _dbTracer = new DbTracer(RootConfig.ObjectConfig.createTracer(RootConfig.ObjectConfig.get("bookstore.bookstore.db")));
    }

    public Module() {
    }

    final protected void configure() {
        bind(com.neeve.service.cdc.main.DbPersister.class).to(com.neeve.bookstore.cart.db.Persister.class);
    }

    /**
     * Provides the DB tracer to guice modules
     */
    @Provides
    final private IDbTracer providesDbTracer() {
        return _dbTracer;
    }

    /**
     * Provides the CDC tracer to guice modules
     */
    @Provides
    final private ICdcTracer providesCdcTracer() {
        return _dbTracer;
    }
}

