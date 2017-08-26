package com.neeve.oms.es.domain;

import com.neeve.lang.XString;
import com.neeve.util.UtlPool;

import com.neeve.fix.entities.HandlingInstructions;
import com.neeve.fix.entities.OrdStatus;
import com.neeve.fix.entities.OrdType;
import com.neeve.fix.entities.OrderCapacity;
import com.neeve.fix.entities.SecurityIDSource;
import com.neeve.fix.entities.Side;
import com.neeve.fix.entities.TimeInForce;
import com.neeve.fix.entities.ExecType;
import com.neeve.fix.entities.LastCapacity;
import com.neeve.fix.entities.MsgType;

import com.neeve.fix.FixAppField;
import com.neeve.fix.FixMessage;
import com.neeve.fix.FixMessageFactory;

import com.neeve.oms.Constants;
import com.neeve.oms.messages.NewOrderMessage;
import com.neeve.oms.messages.OrderEvent;

final public class Order implements UtlPool.Item<Order> {
    final public class NewOrderMessageFields {
        @FixAppField(tag=49)
        final private XString senderCompID = XString.create(32, false, true);
        @FixAppField(tag=56)
        final private XString targetCompID = XString.create(32, false, true); 
        @FixAppField(tag=55)
        final private XString symbol = XString.create(4, false, true);
        @FixAppField(tag=11)
        final private XString clOrdId = XString.create(32, false, true);
        @FixAppField(tag=54)
        private char side;
        @FixAppField(tag=40)
        private char ordType;
        @FixAppField(tag=44)
        private double price;
        @FixAppField(tag=38)
        private double orderQty;
        @FixAppField(tag=59)
        private char timeInForce;
        @FixAppField(tag=37)
        private long orderId;
        @FixAppField(tag=14)
        private double cumQuantity;
        @FixAppField(tag=151)
        private double leavesQty;

        /**
         * Extracts fields from an inbound ADM generated order message
         */
        final void extract(final NewOrderMessage message) {
            message.getSenderCompIDTo(senderCompID);
            message.getSymbolTo(symbol);
            message.getClOrdIDTo(clOrdId);
            side = (char)(message.getSide().val);
            ordType = (char)(message.getOrdType().val);
            price = message.getPrice();
            orderQty = message.getOrderQty();
            timeInForce = (char)(message.getTimeInForce().val);
        }

        /**
         * Extracts fields from an inbound FIX message
         */
        final void extract(final FixMessage message) {
            message.extract(this);
        }
    }

    final public class OrderEventFields {
        // ---
        // these fields are only used for FixMessage population. ADM generated
        // message population happens directly from NewOrderMessage and Order 
        // fields - see populate() below
        @FixAppField(tag=35)
        final private XString msgType = XString.create(MsgType.ExecutionReport.getCodeString());
        @FixAppField(tag=49)
        private XString senderCompID;
        @FixAppField(tag=56)
        private XString targetCompID;
        @FixAppField(tag=55)
        private XString symbol;
        @FixAppField(tag=11)
        private XString clOrdId;
        @FixAppField(tag=54)
        private char side;
        @FixAppField(tag=19)
        private char lastCapacity;
        @FixAppField(tag=14)
        private double cumQuantity;
        @FixAppField(tag=75)
        private long tradeDate;
        @FixAppField(tag=17)
        private long execID;
        @FixAppField(tag=17)
        private char execType;
        // ---

        /**
         * Populates an ADM generated outbound order event 
         */
        final void populate(final OrderEvent event) {
            final long now = System.currentTimeMillis();
            event.setMsgType(MsgType.ExecutionReport);
            event.lendSenderCompID(newOrderMessageFields.targetCompID);
            event.lendTargetCompId(newOrderMessageFields.senderCompID);
            event.lendSymbol(newOrderMessageFields.symbol);
            event.lendClOrdID(newOrderMessageFields.clOrdId);
            event.setSide(Side.fromCode(newOrderMessageFields.side));
            event.setLastCapacity(LastCapacity.A);
            event.setCumQty(cumQuantity);
            event.setTradeDateAsTimestamp(now);
            event.setExecIDFrom(now);
            event.setExecType(ExecType.NEW);
        }

        /**
         * Populates an outbound FIX message
         */
        final void populate(final FixMessage message) {
            final long now = System.currentTimeMillis();
            senderCompID = newOrderMessageFields.targetCompID;
            targetCompID = newOrderMessageFields.senderCompID; 
            side = newOrderMessageFields.side;
            symbol = newOrderMessageFields.symbol;
            clOrdId = newOrderMessageFields.clOrdId;
            lastCapacity = (char)LastCapacity.A.val;
            cumQuantity = Order.this.cumQuantity;
            tradeDate = now;
            execID = now;
            execType = (char)ExecType.NEW.val;
            message.populate(FixMessageFactory.ID_FixExecutionReport, this);
        }
    }

    // order id counter
    private static long nextOrderId;
    
    // embedded domain specific representations of messages for optimized serialization/deserialization
    final private NewOrderMessageFields newOrderMessageFields;
    final private OrderEventFields orderEventFields;

    // order state
    private long orderId;
    private double cumQuantity;
    private double leavesQty;
    private OrdStatus ordStatus;

    // order pool
    private UtlPool<Order> pool;

    public Order() {
        // precreate objects that hold inbound and oubound message fields
        newOrderMessageFields = new NewOrderMessageFields();
        orderEventFields = new OrderEventFields();

        // assign order id
        orderId = ++nextOrderId;
    }

    final public void extract(final NewOrderMessage message) {
        // extract new order message fields
        newOrderMessageFields.extract(message);

        // initialize state
        cumQuantity = newOrderMessageFields.orderQty;
        leavesQty = 0.0;
        ordStatus = OrdStatus.NEW;
    }

    final public void extract(final FixMessage message) {
        // extract new order message fields
        newOrderMessageFields.extract(message);

        // initialize state
        cumQuantity = newOrderMessageFields.orderQty;
        leavesQty = 0.0;
        ordStatus = OrdStatus.NEW;
    }

    final public void populate(final OrderEvent event) {
        // populate from contained order event message
        orderEventFields.populate(event);
    }

    final public void populate(final FixMessage event) {
        // populate from contained order event message
        orderEventFields.populate(event);
    }

    final public long getOrderId() {
        return orderId;
    }

    /*
     * Implementation of {@link Item#init()}
     */
    @Override
    final public Order init() {
        return this;
    }

    /*
     * Implementation of {@link Item#setPool}
     */
    @Override
    final public Order setPool(UtlPool<Order> pool) {
        this.pool = pool;
        return this;
    }

    /*
     * Implementation of {@link Item#getPool}
     */
    @Override
    final public UtlPool<Order> getPool() {
        return pool;
    }
}
