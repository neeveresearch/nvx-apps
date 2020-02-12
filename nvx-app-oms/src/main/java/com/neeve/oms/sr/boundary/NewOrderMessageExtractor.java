package com.neeve.oms.sr.boundary;

import com.neeve.oms.messages.NewOrderMessage;
import com.neeve.oms.state.Order;

final public class NewOrderMessageExtractor {
    final public static void extract(final NewOrderMessage message, final Order order) {
        order.setSenderCompIDFrom(message.getSenderCompIDUnsafe());
        order.setSymbolFrom(message.getSymbolUnsafe());
        order.setClOrdIDFrom(message.getClOrdIDUnsafe());
        order.setSide(message.getSide());
        order.setOrdType(message.getOrdType());
        order.setPrice(message.getPrice());
        order.setOrderQty(message.getOrderQty());
        order.setTimeInForce(message.getTimeInForce());
    }
}
