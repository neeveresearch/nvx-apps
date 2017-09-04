package com.neeve.tick2trade.acl;

import com.neeve.tick2trade.domain.Order;
import com.neeve.tick2trade.messages.DirectedOrderFlags;
import com.neeve.tick2trade.messages.MarketNewOrderSingle;
import com.neeve.tick2trade.messages.HandlingInstructions;
import com.neeve.tick2trade.messages.MsgType;
import com.neeve.tick2trade.messages.OrderCapacity;
import com.neeve.tick2trade.messages.TimeInForce;
import com.neeve.tick2trade.messages.SettlType;

final public class MarketNewOrderSinglePopulator {
    final public static MarketNewOrderSingle populate(final MarketNewOrderSingle message, final Order order) {
        final long now = System.currentTimeMillis();

        // FIXRequest.FIXMessageHeader
        message.setMsgType(MsgType.NewOrderSingle);
        message.lendSenderCompID(Constants.SOR_COLLAPSED_STATE);
        message.lendSenderSubID(Constants.CMA);
        message.lendTargetCompId(Constants.CMA);
        message.setSendingTimeAsTimestamp(now);

        // FIXRequest.OrderRoutingFields
        // ... <none>

        // FIXRequest.Instrument
        message.lendSymbol(order.getSymbol());
        message.lendSecurityID(order.getSecurityId());
        message.setSecurityIDSource(order.getSecurityIDSource());
        message.lendSecurityAltID(order.getSecurityAldID());
        message.setSecurityAltIDSource(order.getSecurityIDSource());

        // FIXRequest
        message.setOrderTs(order.getOriginTs());
        message.lendClOrdID(order.getClOrdId());
        message.lendComplianceID(order.getComplianceId());
        message.setTransactTimeAsTimestamp(now);
        message.setSide(order.getSide());
        message.lendExecutingTrader(Constants.SOR_COLLAPSED_STATE);

        // FIXOrder.FIXOrderInstructions
        message.setSettlType(SettlType.Regular);
        message.setOrderQty(order.getOrderQty());
        message.lendPositionAccount(order.getPositionAccount());
        message.lendTargetStrategy(Constants.C1012);
        message.setHandlInst(HandlingInstructions.AutoExecPub);
        message.setMaxFloor(order.getMaxFloor());
        message.setTimeInForce(TimeInForce.Day);

        // FIXOrder
        message.setOrderCapacity(OrderCapacity.Agency);
        message.setOrdType(order.getOrderType());
        message.setPrice(order.getPrice());
        message.setDirectedOrderFlags(DirectedOrderFlags.NotCustomerDirectedOrder);
        message.lendExDestination(Constants.ARCX);

        // other
        message.setAsPriority();

        // done
        return message;
    }
}
