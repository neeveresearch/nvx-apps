package com.neeve.tick2trade.acl;

import com.neeve.tick2trade.messages.SORNewOrderSingle;
import com.neeve.tick2trade.messages.EMSSliceCommand;

final public class EMSSliceCommandPopulator {
    final public static EMSSliceCommand populate(final EMSSliceCommand message, final SORNewOrderSingle nos, final long tickTs) {
        message.lendOrderID(nos.getOrderIDUnsafe());
        message.lendSymbol(nos.getSymbolUnsafe());
        message.setSide(nos.getSide());
        message.setOrderQty(nos.getOrderQty());
        message.setTickTs(tickTs);
        return message;
    }
}
