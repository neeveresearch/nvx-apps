package com.neeve.oms.es.boundary;

import com.neeve.oms.es.domain.Order;
import com.neeve.oms.messages.NewOrderMessage;

import com.neeve.fix.FixMessage;

final public class NewOrderMessageExtractor {
    final public static void extract(final NewOrderMessage message, final Order order) {
        order.extract(message);
    }

    final public static void extract(final FixMessage message, final Order order) {
        order.extract(message);
    }
}
