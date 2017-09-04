package com.neeve.tick2trade;

import com.neeve.util.*;
import com.neeve.trace.*;
import com.neeve.aep.annotations.*;
import com.neeve.tick2trade.acl.*;
import com.neeve.tick2trade.messages.*;

/**
 * A skeletal SOR simulator.
 * <p>
 * This not so smart Smart Order Router simply creates one order slice which
 * gets routed to the the single Market venue.
 * <p>
 * Normally, one would leverage market data here, and use the platform's market
 * data replication facilities to draw on real market data to realistic order
 * routing. The intent of this implementation is just to highlight an app structure
 * in which there is a separation of concerns between the EMS and the SOR strategy
 * logic.
 * <p>
 * <b>NOTE:</b> that while in this app the {@link Sor} is embedded in the same
 * process as the EMS, it would be possible to move the {@link Sor} into a
 * separate process or host by moving it into its own application.
 * 
 * @see App
 */
class Sor {
    private final App app;

    Sor(final App app, final Tracer tracer) {
        this.app = app;
    }

    /**
     * Called by the {@link App} to perform any configuration related operations
     * before startup.
     */
    final void configure() {}

    ///////////////////////////////////////////////////////////////////////////////
    // EVENT & MESSAGE HANDLERS                                                  //
    //                                                                           //
    // Event handlers are called by the underlying applications AepEngine.       //
    //                                                                           //
    // NOTE: An Event Sourcing applicaton must be able to identically            //
    // recover its state and generate the *same* outbound messages via replay    //
    // of the its input events at a later time or on a different system.         //
    // Thus, for an application using Event Sourcing, it is crucial that the     //
    // app not make any changes to its state that are based on the local system  //
    // such as System.currentTimeMillis() or interacting with the file system.   //
    //                                                                           //
    // Event handlers are not called concurrently so synchronization is not      //
    // needed.                                                                   // 
    ///////////////////////////////////////////////////////////////////////////////

    /**
     * The {@link Sor} new order single handler.
     * <p>
     * This handler accepts a {@link SORNewOrderSingle} command issued by the
     * {@link Ems} when a new order is received from a client.
     * <p>
     * The {@link Sor} is responsible for slicing and routing the order between
     * liquidity venues and the scheduling of those slices.
     * <p>
     * An actual Sor implementation would tap into market data via a provider
     * like Exegy and would likely trigger a slice later based on that market data
     * For simplicity this implementation just creates a single slice to be dispatched 
     * immediately.
     * 
     * @param message The new order single command from the EMS>
     */
    @EventHandler
    public final void onNewOrderSingle(final SORNewOrderSingle message) {
        final long now = UtlTime.now();
        app.send(EMSSliceCommandPopulator.populate(EMSSliceCommand.create(), message, now));
    }

    ///////////////////////////////////////////////////////////////////////////////
    // COMMAND HANDLERS                                                          //
    //                                                                           //
    // Command handlers can be invoked remotely via management tools such as     //
    // Robin or locally via unit tests.                                          //
    ///////////////////////////////////////////////////////////////////////////////

    /**
     * This is called by the App command handler for reset. 
     */
    final void reset() {}
}
