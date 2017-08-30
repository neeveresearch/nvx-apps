package com.neeve.tick2trade.acl;

import com.neeve.tick2trade.domain.Order;
import com.neeve.tick2trade.messages.SORNewOrderSingle;

final public class SORNewOrderSinglePopulator {
    final public static SORNewOrderSingle populate(final SORNewOrderSingle message, final Order order) {
        message.lendOrderID(order.getOrderId());
        message.lendSymbol(order.getSymbol());
        message.setSide(order.getSide());
        message.setOrderQty(order.getOrderQty());
        message.lendTargetStrategy(Constants.C1012);
        return message;
    }
}
