package com.neeve.tick2trade.acl;

import com.neeve.tick2trade.domain.Order;
import com.neeve.tick2trade.messages.ExecType;
import com.neeve.tick2trade.messages.LastCapacity;
import com.neeve.tick2trade.messages.MsgType;
import com.neeve.tick2trade.messages.EMSOrderNew;
import com.neeve.tick2trade.messages.OrdStatus;

final public class EMSOrderNewPopulator {
    final public static EMSOrderNew populate(final EMSOrderNew message, final Order order) {
        final long now = System.currentTimeMillis();

        // EMSOrderNew.FIXExecutionReport.FIXMessageHeader
        message.setMsgType(MsgType.ExecutionReport);
        message.lendSenderCompID(Constants.CMA);
        message.lendSenderSubID(Constants.SOR);
        message.lendTargetCompId(Constants.TARGET_COMPID);
        message.lendTargetSubID(Constants.TARGET_SUBID);
        message.setSendingTimeAsTimestamp(now);

        // EMSOrderNew.FIXExecutionReport.FIXExecutionStatus
        message.lendClOrdID(order.getClOrdId());
        message.lendOrderID(order.getOrderId());
        message.setSide(order.getSide());
        message.setOrdStatus(OrdStatus.NEW);
        message.setTransactTimeAsTimestamp(now);
        message.setLastCapacity(LastCapacity.A);
        message.setCumQty(0.0);
        message.setTradeDateAsTimestamp(now);

        // EMSOrderNew.FIXExecutionReport
        message.lendComplianceID(order.getComplianceId());
        message.setExecIDFrom(now);
        message.setExecType(ExecType.NEW);

        // done
        return message;
    }
}
