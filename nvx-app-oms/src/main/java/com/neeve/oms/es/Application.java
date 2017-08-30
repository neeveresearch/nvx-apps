package com.neeve.oms.es;

import com.neeve.aep.AepEngine;
import com.neeve.aep.AepMessageSender;
import com.neeve.aep.annotations.EventHandler;
import com.neeve.cli.annotations.Configured;
import com.neeve.config.ConfigRepositoryFactory;
import com.neeve.lang.XLongLinkedHashMap;
import com.neeve.server.app.annotations.AppInitializer;
import com.neeve.server.app.annotations.AppInjectionPoint;
import com.neeve.server.app.annotations.AppHAPolicy;
import com.neeve.sma.MessagingProviderDescriptor;
import com.neeve.xbuf.XbufDesyncPolicy;

import com.neeve.oms.messages.NewOrderMessage;
import com.neeve.oms.messages.OrderEvent;
import com.neeve.oms.es.boundary.NewOrderMessageExtractor;
import com.neeve.oms.es.boundary.OrderEventPopulator;
import com.neeve.oms.es.domain.Order;

import com.neeve.fix.FixMessage;

@AppHAPolicy(value = AepEngine.HAPolicy.EventSourcing)
final public class Application {
    @Configured(property = "oms.orderPreallocateCount", defaultValue = "1000")
    private int _orderPreallocateCount;
    private AepMessageSender _messageSender;
    private OrderPool _orderPool;
    private XLongLinkedHashMap<Order> _orders;

    static {
        try {
            MessagingProviderDescriptor.create("local", "com.neeve.oms.driver.local.LocalProvider").save(ConfigRepositoryFactory.getInstance().getLocalRepository());
        }
        catch (Throwable e) {
            throw new RuntimeException(e);
        }
        NewOrderMessage.setDesyncPolicy(XbufDesyncPolicy.FrameFields);
    }

    public Application() {
        _orders = new XLongLinkedHashMap<Order>(_orderPreallocateCount);
    }

    @AppInitializer
    final public void init() {
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
}
