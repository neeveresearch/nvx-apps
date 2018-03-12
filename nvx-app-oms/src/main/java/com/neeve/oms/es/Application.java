package com.neeve.oms.es;

import java.util.Set;

import com.neeve.aep.AepEngine;
import com.neeve.aep.AepMessageSender;
import com.neeve.aep.annotations.EventHandler;
import com.neeve.cli.annotations.Configured;
import com.neeve.fix.FixMessage;
import com.neeve.lang.XLongLinkedHashMap;
import com.neeve.oms.driver.local.LocalDriver;
import com.neeve.oms.es.boundary.NewOrderMessageExtractor;
import com.neeve.oms.es.boundary.OrderEventPopulator;
import com.neeve.oms.es.domain.Order;
import com.neeve.oms.messages.NewOrderMessage;
import com.neeve.oms.messages.OrderEvent;
import com.neeve.server.app.annotations.AppCommandHandlerContainersAccessor;
import com.neeve.server.app.annotations.AppEventHandlerContainersAccessor;
import com.neeve.server.app.annotations.AppHAPolicy;
import com.neeve.server.app.annotations.AppInitializer;
import com.neeve.server.app.annotations.AppInjectionPoint;
import com.neeve.server.app.annotations.AppStatContainersAccessor;

@AppHAPolicy(value = AepEngine.HAPolicy.EventSourcing)
final public class Application {
    final private LocalDriver localDriver = new LocalDriver();

    @Configured(property = "oms.orderPreallocateCount", defaultValue = "1000")
    private int _orderPreallocateCount;
    private AepMessageSender _messageSender;
    private OrderPool _orderPool;
    private XLongLinkedHashMap<Order> _orders;

    @AppInitializer
    final public void init() {
        _orders = new XLongLinkedHashMap<Order>(_orderPreallocateCount);
        _orderPool = new OrderPool(_orderPreallocateCount);
        for (int i = 0; i < 1; i++) {
            System.gc();
        }
    }

    @AppInjectionPoint
    final public void setMessageSender(AepMessageSender messageSender) {
        _messageSender = messageSender;
    }

    /**
     * Handler for ADM generated NewOrderMessage message
     */
    @EventHandler
    final public void onNewOrder(final NewOrderMessage message) {
        // instantiate a new order
        final Order order = _orderPool.get();

        // extract from message and populate order
        NewOrderMessageExtractor.extract(message, order);

        // add to the application's order collection
        _orders.put(order.getOrderId(), order);

        // create a new order event
        final OrderEvent event = OrderEvent.create();

        // populate the event
        OrderEventPopulator.populate(event, order);
        event.setPostWireTs(message.getPostWireTs());

        // send the event
        _messageSender.sendMessage(2, event);
    }

    /**
     * Handlers for FIX message
     */
    @EventHandler
    final public void onMessage(final FixMessage message) {
        // instantiate a new order
        final Order order = _orderPool.get();

        // extract from message and populate order
        NewOrderMessageExtractor.extract(message, order);

        // add to the application's order collection
        _orders.put(order.getOrderId(), order);

        // create a new order event (FIX message)
        final FixMessage event = FixMessage.create();

        // populate the event
        OrderEventPopulator.populate(event, order);
        event.setPostWireTs(message.getPostWireTs());

        // send the event
        _messageSender.sendMessage(2, event);
    }

    /**
     * Hook for adding command handler used when running in 'local' driver mode.
     * @param commandHandlers The command handlers
     */
    @AppCommandHandlerContainersAccessor
    public void getCommandHandlers(Set<Object> commandHandlers) {
        commandHandlers.add(localDriver);
    }

    /**
     * Hook for adding stats used when running in 'local' driver mode.
     * @param statsContainers The objects containing stats.
     */
    @AppStatContainersAccessor
    public void getStatsContainers(Set<Object> statsContainers) {
        statsContainers.add(localDriver);
    }

    /**
     * Hook for adding event handler used when running in 'local' driver mode.
     * @param eventHandlers The event handlers
     */
    @AppEventHandlerContainersAccessor
    public void getEventHandlers(Set<Object> eventHandlers) {
        eventHandlers.add(localDriver);
    }

    /**
     * Returns the local driver. 
     * 
     * @return The local driver. 
     */
    public final LocalDriver getLocalDriver() {
        return localDriver;
    }
}
