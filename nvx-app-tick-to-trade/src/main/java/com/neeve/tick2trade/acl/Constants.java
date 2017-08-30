package com.neeve.tick2trade.acl;

import com.neeve.lang.XString;
import com.neeve.tick2trade.messages.StrategyParams;

final public class Constants {
    final public static XString SOR_COLLAPSED_STATE = XString.create("SOR-COLLAPSED-STATE", false, true);
    final public static XString CMA = XString.create("CMA", false, true);
    final public static XString BATS = XString.create("BATS", false, true);
    final public static XString C1012 = XString.create("1012", false, true);
    final public static XString C1013 = XString.create("1013", false, true);
    final public static XString LOWTOUCH_SIM = XString.create("LowTouchSim", false, true);
    final public static XString SOR = XString.create("SOR", false, true);
    final public static XString USD = XString.create("USD", false, true);
    final public static XString IBM = XString.create("IBM", false, true);
    final public static XString FIDELITY = XString.create("FIDELITY", false, true);
    final public static XString BUYING_ONLY = XString.create("Buying Only", false, true);
    final public static XString SELLING_ONLY = XString.create("Selling Only", false, true);
    final public static XString POSITION_ACCOUNT = XString.create("9301471810099", false, true);
    final public static XString INITIATING_TRADER = XString.create("INITIATING_TRADER", false, true);
    final public static XString LOCATE_SYSTEM = XString.create("LOCATE_SYSTEM", false, true);
    final public static XString ORDER_ORIG_SYSTEM = XString.create("INPROC", false, true);
    final public static XString PUBLISHING_SYSTEM = XString.create("XSIMEMS", false, true);
    final public static XString PUBLISHING_SYSTEM_ID = XString.create("1", false, true);
    final public static XString DESKID = XString.create("_NONE_", false, true);
    final public static XString ENTERING_TRADER = XString.create("ENTERING_TRADER", false, true);
    final public static XString TEXT = XString.create("TEXT", false, true);
    final public static XString EXECUTING_FIRM = XString.create("LEHM", false, true);
    final public static XString EX_DESTINATION = XString.create("EX_DESTINATION", false, true);
    final public static XString CLIENT_ID = XString.create("CLIENT_ID", false, true);
    final public static XString MARKET_DATA_SNAPSHOT = XString.create("MARKET_DATA_SNAPSHOT", false, true);
    final public static XString EX_TRADING_ACC = XString.create("EX_TRADING_ACC", false, true);
    final public static XString EX_PARAMS = XString.create("EX_PARAMS", false, true);
    final public static XString EXEC_INST = XString.create("EXEC_INST", false, true);
    final public static XString ENTERING_SYSTEM_INSTANCE = XString.create("SOR01", false, true);
    final public static XString EXECUTING_SYSTEM_INSTANCE = XString.create("SOR01", false, true);
    final public static XString MARKET_SECTOR_DESC = XString.create("EQUITY", false, true);
    final public static XString BLOOMBERG_SYMBOL = XString.create("IBM", false, true);
    final public static XString EXCH_CL_ORDID = XString.create("EXCH_CL_ORDID", false, true);
    final public static XString EXCH_ORDID = XString.create("EXCH_ORDID", false, true);
    final public static XString EXCH_ORIG_CL_ORDID = XString.create("EXCH_ORIG_CL_ORDID", false, true);
    final public static XString EXCH_ORIG_ORDID = XString.create("EXCH_ORIG_ORDID", false, true);
    final public static XString EXCH_PARTICIPANT_ID = XString.create("EXCH_PARTICIPANT_ID", false, true);
    final public static XString EXCH_SENDER_ID = XString.create("EXCH_SENDER_ID", false, true);
    final public static XString COUNTRY = XString.create("US", false, true);
    final public static XString CITY = XString.create("NEW_YORK", false, true);
    final public static XString TIMEZONE = XString.create("America/New York", false, true);
    final public static XString BROKER = XString.create("XSIM", false, true);
    final public static XString TARGET_COMPID = XString.create("XSIMCLIENT", false, true);
    final public static XString TARGET_SUBID = XString.create("1", false, true);
    final public static XString SECURITYID = XString.create("1527057", false, true);
    final public static XString INPROC_SIMULATOR = XString.create("INPROC", false, true);
    final public static XString ARCX = XString.create("ARCX", false, true);
    final public static XString ESMP = XString.create("1527057", false, true);
    final public static XString BOOK_SHORTNAME = XString.create("T14718", false, true);
    final public static XString CLIENT_ACRONYM = XString.create("", false, true);

    final public static StrategyParams STRATEGY_PARAMS1 = StrategyParams.create();
    final public static StrategyParams STRATEGY_PARAMS2 = StrategyParams.create();

    static {
        STRATEGY_PARAMS1.setTargetFrom(C1012);
        STRATEGY_PARAMS1.setParamsFrom(BUYING_ONLY);
        STRATEGY_PARAMS2.setTargetFrom(C1013);
        STRATEGY_PARAMS2.setParamsFrom(SELLING_ONLY);
    }

    final public static XString TARGET_STRATEGY = XString.create("8000", false, true);
}
