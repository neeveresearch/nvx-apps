package com.neeve.tick2trade.acl;

import com.neeve.tick2trade.domain.Order;
import com.neeve.tick2trade.messages.EMSNewOrderSingle;

final public class EMSNewOrderSingleExtractor {
    final public static Order extract(final EMSNewOrderSingle message, final Order order) {
        order.extract(message);
        return order;
    }
}
