package com.neeve.oms.es.boundary;

import com.neeve.oms.es.domain.Order;
import com.neeve.oms.messages.OrderEvent;

import com.neeve.fix.FixMessage;

final public class OrderEventPopulator {
    final public static void populate(final OrderEvent event, final Order order) {
        order.populate(event);
    }

    final public static void populate(final FixMessage message, final Order order) {
        order.populate(message);
    }
}
