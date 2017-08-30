package com.neeve.bookstore.cart.service.domain.handlers;

import com.google.inject.*;
import com.neeve.ci.XRuntime;

import com.neeve.sma.MessageView;
import com.neeve.service.MessageHandler;
import com.neeve.service.MessageSender;

import com.neeve.bookstore.cart.service.messages.DoDataMaintenanceRequest;
import com.neeve.bookstore.cart.service.messages.DataMaintenanceDoneEvent;
import com.neeve.bookstore.cart.service.repository.Repository;
import com.neeve.trace.Tracer;
import com.neeve.util.UtlProps;

final public class DoDataMaintenanceRequestHandler implements MessageHandler<DoDataMaintenanceRequest, DataMaintenanceDoneEvent, Repository> {
    @Inject
    private Tracer _tracer;
    final static private long _maintenanceTime = (int) UtlProps.getValue(XRuntime.getProps(), "bookstore.carts.datamaintenance.duration", 1) * 1000L;

    /**
     * Implementation of {@link MessageHandler#getType}
     */
    final public Type getType() {
        return Type.Local;
    }

    /**
     * Implementation of {@link MessageHandler#handle}
     */
    final public MessageView handle(final String origin,
                                    final DoDataMaintenanceRequest request,
                                    final DataMaintenanceDoneEvent response,
                                    final Repository repository) throws Exception {
        // do some data maintenance work
        _tracer.log("Running periodic data maintenance...", Tracer.Level.INFO);
        Thread.sleep(_maintenanceTime);
        _tracer.log("...periodic data maintenance - done", Tracer.Level.INFO);

        return null;
    }
}
