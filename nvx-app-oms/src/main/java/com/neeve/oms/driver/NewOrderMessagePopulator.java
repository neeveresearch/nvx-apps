package com.neeve.oms.driver;

import java.util.Random;

import com.neeve.lang.XString;
import com.neeve.util.UtlTime;

import com.neeve.fix.entities.MsgType;
import com.neeve.fix.entities.HandlingInstructions;
import com.neeve.fix.entities.Side;
import com.neeve.fix.entities.SecurityIDSource;
import com.neeve.fix.entities.OrderCapacity;
import com.neeve.fix.entities.TimeInForce;
import com.neeve.fix.entities.SettlType;
import com.neeve.fix.entities.OrdType;

import com.neeve.oms.Constants;
import com.neeve.oms.messages.NewOrderMessage;

import com.neeve.fix.FixAppField;
import com.neeve.fix.FixMessage;
import com.neeve.fix.FixMessageFactory;

final public class NewOrderMessagePopulator {
    final public static class NewOrderMessageFields {
        @FixAppField(tag=35)
        final private XString msgType = XString.create(MsgType.NewOrderSingle.getCodeString());
        @FixAppField(tag=49)
        final private XString senderCompID = Constants.COMPANY;
        @FixAppField(tag=56)
        final private XString targetCompID = Constants.EXCH;
        @FixAppField(tag=55)
        final private XString symbol = Constants.SYMBOL;
        @FixAppField(tag=11)
        final private XString clOrdId = XString.create("1002");
        @FixAppField(tag=54)
        private char side = '1';
        @FixAppField(tag=40)
        private char ordType = '2';
        @FixAppField(tag=44)
        private double price = 10.01;
        @FixAppField(tag=38)
        private double orderQty = 1000;
        @FixAppField(tag=59)
        private char timeInForce = '0';
    }

    final private static NewOrderMessageFields newOrderMessageFields = new NewOrderMessageFields();

    final public static void populate(final NewOrderMessage message) {
        final long now = System.currentTimeMillis();
        final long nowInMicros = UtlTime.now();
        message.setMsgType(MsgType.NewOrderSingle);
        message.setSenderCompIDFrom(newOrderMessageFields.senderCompID);
        message.setTargetCompIdFrom(newOrderMessageFields.targetCompID);
        message.setSymbolFrom(newOrderMessageFields.symbol);
        message.setClOrdIDFrom(newOrderMessageFields.clOrdId);
        message.setSide(Side.fromCode(newOrderMessageFields.side));
        message.setTimeInForce(TimeInForce.fromCode(newOrderMessageFields.timeInForce));
        message.setOrdType(OrdType.fromCode(newOrderMessageFields.ordType));
        message.setPrice(newOrderMessageFields.price);
        message.setOrderQty(newOrderMessageFields.orderQty);
    }

    final public static void populate(final FixMessage message) {
        message.populate(FixMessageFactory.ID_NewOrderSingle, newOrderMessageFields);
    }
}
