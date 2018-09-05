package com.neeve.tick2trade.acl;

import com.neeve.tick2trade.messages.MsgType;
import com.neeve.tick2trade.messages.MarketOrderNew;
import com.neeve.tick2trade.messages.MarketNewOrderSingle;
import com.neeve.tick2trade.messages.ExecType;
import com.neeve.tick2trade.messages.OrdStatus;

final public class MarketOrderNewPopulator {
    final public static MarketOrderNew populate(final MarketOrderNew message, final MarketNewOrderSingle marketNewOrderSingle, final long timestamp) {
        // MarketOrderNew.FIXExecutionReport.FIXMessageHeader
        message.setMsgType(MsgType.ExecutionReport);
        message.lendSenderCompID(Constants.CMA);
        message.lendTargetCompId(Constants.SOR_COLLAPSED_STATE);
        message.setSendingTimeAsTimestamp(timestamp);

        // MarketOrderNew.FIXExecutionReport.FIXExecutionStatus
        message.setClOrdIDFrom(marketNewOrderSingle.getClOrdIDUnsafe());
        message.setOrderIDFrom(marketNewOrderSingle.getClOrdIDUnsafe());
        message.setOrdStatus(OrdStatus.NEW);
        message.setTransactTimeAsTimestamp(timestamp);

        // MarketOrderNew.FIXExecutionReport
        message.setComplianceIDFrom(marketNewOrderSingle.getComplianceIDUnsafe());
        message.setExecType(ExecType.NEW);
        message.setExecIDFrom(timestamp);

        // done
        return message;
    }
}
