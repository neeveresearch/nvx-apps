package com.neeve.oms.sr.boundary;

import com.neeve.fix.entities.ExecType;
import com.neeve.fix.entities.MsgType;
import com.neeve.fix.entities.LastCapacity;

import com.neeve.oms.messages.OrderEvent;
import com.neeve.oms.state.Order;

final public class OrderEventPopulator {
    final public static void populate(final OrderEvent event, final Order order) {
        final long now = System.currentTimeMillis();
        event.setMsgType(MsgType.ExecutionReport);
        event.setSenderCompIDFrom(order.getTargetCompIDUnsafe());
        event.setTargetCompIdFrom(order.getSenderCompIDUnsafe());
        event.setSymbolFrom(order.getSymbolUnsafe());
        event.setClOrdIDFrom(order.getClOrdIDUnsafe());
        event.setSide(order.getSide());
        event.setLastCapacity(LastCapacity.A);
        event.setCumQty(order.getCumQuantity());
        event.setTradeDateAsTimestamp(now);
        event.setExecIDFrom(now);
        event.setExecType(ExecType.NEW);
    }
}
