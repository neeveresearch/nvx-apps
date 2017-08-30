package com.neeve.tick2trade.acl;

import com.neeve.tick2trade.messages.MsgType;
import com.neeve.tick2trade.messages.MarketTrade;
import com.neeve.tick2trade.messages.ExecType;
import com.neeve.tick2trade.messages.MarketNewOrderSingle;
import com.neeve.tick2trade.messages.OrdStatus;

final public class MarketTradePopulator {
    final public static MarketTrade populate(final MarketTrade message, final MarketNewOrderSingle marketNewOrderSingle, final long timestamp) {
        // MarketTrade.FIXExecutionReport.FIXMessageHeader
        message.setMsgType(MsgType.ExecutionReport);
        message.lendSenderCompID(Constants.CMA);
        message.lendTargetCompId(Constants.SOR_COLLAPSED_STATE);
        message.setSendingTimeAsTimestamp(timestamp);

        // MarketTrade.FIXExecutionReport.FIXExecutionStatus
        message.getClOrdIDField().setValueFrom(marketNewOrderSingle.getClOrdIDField());
        message.getOrderIDField().setValueFrom(marketNewOrderSingle.getClOrdIDField());
        message.setOrdStatus(OrdStatus.FILLED);
        message.setTransactTimeAsTimestamp(timestamp);

        // MarketTrade.FIXExecutionReport
        message.getComplianceIDField().setValueFrom(marketNewOrderSingle.getComplianceIDField());
        message.setExecType(ExecType.TRADE);
        message.setExecIDFrom(timestamp);

        // done
        return message;
    }
}
