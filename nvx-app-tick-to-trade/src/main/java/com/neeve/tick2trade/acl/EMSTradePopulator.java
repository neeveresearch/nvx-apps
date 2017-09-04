package com.neeve.tick2trade.acl;

import com.neeve.tick2trade.domain.Order;
import com.neeve.tick2trade.messages.ExecType;
import com.neeve.tick2trade.messages.LastCapacity;
import com.neeve.tick2trade.messages.MsgType;
import com.neeve.tick2trade.messages.EMSTrade;
import com.neeve.tick2trade.messages.OrdStatus;

final public class EMSTradePopulator {
    final public static EMSTrade populate(final EMSTrade message, final Order order) {
        final long now = System.currentTimeMillis();

        // FIXMessageHeader
        message.setMsgType(MsgType.ExecutionReport);
        message.lendSenderCompID(Constants.CMA);
        message.lendSenderSubID(Constants.SOR);
        message.lendTargetCompId(Constants.TARGET_COMPID);
        message.lendTargetSubID(Constants.TARGET_SUBID);
        message.setSendingTimeAsTimestamp(now);

        // FIXExecutionStatus
        message.lendClOrdID(order.getClOrdId());
        message.lendOrderID(order.getOrderId());
        message.setSide(order.getSide());
        message.setOrdStatus(OrdStatus.FILLED);
        message.setTransactTimeAsTimestamp(now);
        message.setLastCapacity(LastCapacity.A);
        message.setTradeDateAsTimestamp(now);

        // FIXExecutionReport
        message.lendComplianceID(order.getComplianceId());
        message.setExecType(ExecType.TRADE);
        message.setExecIDFrom(now);

        // done
        return message;
    }
}
