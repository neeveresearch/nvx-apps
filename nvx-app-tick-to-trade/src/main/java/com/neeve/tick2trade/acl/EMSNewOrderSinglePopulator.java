package com.neeve.tick2trade.acl;

import java.util.Random;

import com.neeve.util.UtlTime;

import com.neeve.tick2trade.messages.MsgType;
import com.neeve.tick2trade.messages.EMSNewOrderSingle;
import com.neeve.tick2trade.messages.HandlingInstructions;
import com.neeve.tick2trade.messages.DirectedOrderFlags;
import com.neeve.tick2trade.messages.Side;
import com.neeve.tick2trade.messages.SecurityIDSource;
import com.neeve.tick2trade.messages.OrderCapacity;
import com.neeve.tick2trade.messages.TimeInForce;
import com.neeve.tick2trade.messages.SettlType;
import com.neeve.tick2trade.messages.OrdType;

final public class EMSNewOrderSinglePopulator {
    final public static EMSNewOrderSingle populate(final EMSNewOrderSingle message, int orderId, final Random random) {
        final long now = System.currentTimeMillis();
        message.setMsgType(MsgType.NewOrderSingle);

        // FIXRequest.FIXMessageHeader
        message.setSenderCompIDFrom(Constants.LOWTOUCH_SIM);
        message.setSenderSubIDFrom(Constants.LOWTOUCH_SIM);
        message.setTargetCompIdFrom(Constants.TARGET_COMPID);
        message.setSendingTimeAsTimestamp(now);

        // FIXRequest.OrderRoutingFields
        // ... <none>

        // FIXRequest.Instrument
        message.setSymbolFrom(Constants.IBM);
        message.setSecurityIDFrom(Constants.SECURITYID);
        message.setSecurityIDSource(SecurityIDSource.ESMP);
        message.setSecurityAltIDFrom(Constants.IBM);
        message.setSecurityAltIDSource(SecurityIDSource.Blmbrg);

        // FIXRequest
        message.setOrderTs(UtlTime.now());
        message.setClOrdIDFrom(orderId);
        message.setComplianceIDFrom(UtlTime.now());
        message.setTransactTimeAsTimestamp(now);
        message.setSide(Side.BUY);
        message.setClientIDFrom(Constants.FIDELITY);
        message.setTextFrom(Constants.BUYING_ONLY);
        message.setExecutingTraderFrom(Constants.SOR);

        // FIXOrder.FIXOrderInstructions
        message.setSettlType(SettlType.Regular);
        message.setSettlCurrencyFrom(Constants.USD);
        message.setHandlInst(HandlingInstructions.AutoExecPub);
        message.setTimeInForce(TimeInForce.Day);

        // FIXOrder
        message.setOrderCapacity(OrderCapacity.Agency);
        message.setOrdType(OrdType.LIMIT);
        message.setPrice(195.0);
        message.setOrderQty(5000);
        message.setDirectedOrderFlags(DirectedOrderFlags.NotCustomerDirectedOrder);
        message.setCurrencyFrom(Constants.USD);
        message.setPositionAccountFrom(Constants.POSITION_ACCOUNT);

        // EMSNewOrderSingle
        message.lendStrategyParams(Constants.STRATEGY_PARAMS1);
        message.setTargetStrategyFrom(Constants.TARGET_STRATEGY);
        return message;
    }
}
