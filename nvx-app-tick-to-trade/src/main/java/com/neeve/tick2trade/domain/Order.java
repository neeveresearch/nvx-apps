package com.neeve.tick2trade.domain;

import com.neeve.lang.XString;
import com.neeve.tick2trade.messages.HandlingInstructions;
import com.neeve.tick2trade.messages.OrdStatus;
import com.neeve.tick2trade.messages.OrdType;
import com.neeve.tick2trade.messages.OrderCapacity;
import com.neeve.tick2trade.messages.SecurityIDSource;
import com.neeve.tick2trade.messages.Side;
import com.neeve.tick2trade.messages.TimeInForce;
import com.neeve.util.UtlPool;
import com.neeve.util.UtlPool.Item;

final public class Order implements Item<Order> {

    final private static class OrderFactory implements UtlPool.Factory<Order> {
        final public Order createItem(final Object context) {
            return Order.create();
        }

        final public Order[] createItemArray(final int size) {
            return new Order[size];
        }
    }

    // order pool
    private UtlPool<Order> pool;

    // poc fields
    long orderTs;

    // domain fields
    final private XString orderId = XString.create(32, false, true);
    final private XString complianceId = XString.create(32, false, true);
    final private XString securityId = XString.create(4, false, true);
    final private XString securityAltId = XString.create(4, false, true);
    final private XString positionAccount = XString.create(13, false, true);
    final private XString symbol = XString.create(4, false, true);
    final private XString routeId = XString.create(3, false, true);
    final private XString clientAcronym = XString.create(4, false, true);
    final private XString targetStategy = XString.create(4, false, true);
    final private XString targetStrategyParameters = XString.create(10, false, true);
    final private XString outboundSystem = XString.create(13, false, true);
    final private XString lastText = XString.create(10, false, true);
    final private XString executingTrader = XString.create(1, false, true);

    private SecurityIDSource securityIdSource;
    private OrdStatus ordStatus;
    private OrderCapacity orderCapacity;
    private OrdType ordType;
    private double price;
    private Side side;
    private TimeInForce timeInForce;
    private HandlingInstructions handlInst;
    private double orderQty;
    private double maxFloor;
    private long transactTime;
    private double leavesQty;
    private double cumQuantity;

    // other fields (specific to POC for w2w measurement)
    private long nosPostWireTs;

    /**
     * Creates a new order pool with the provided number of preallocated orders.
     * 
     * @param orderPreallocateCount
     *            The number of orders to preallocate in the pool.
     * 
     * @return A new order pool.
     */
    public static UtlPool<Order> createPool(int orderPreallocateCount) {
        final UtlPool<Order> orderPool = UtlPool.create("order", "order", new OrderFactory(), UtlPool.Params.create().setThreaded(false).setInitialCapacity(orderPreallocateCount));
        System.out.println("Seeding order state pool with " + (orderPreallocateCount - orderPool.size()) + " entries");
        while (orderPool.size() < orderPreallocateCount) {
            final Order state = Order.create();
            state.setPool(orderPool);
            orderPool.put(state);
        }
        System.out.println("order state pool seeded size=" + orderPool.size() + " entries");
        return orderPool;
    }

    final public static Order create() {
        return new Order();
    }

    final public Order extract(com.neeve.tick2trade.messages.EMSNewOrderSingle nos) {
        nos.getClOrdIDTo(orderId);
        nos.getComplianceIDTo(complianceId);
        nos.getPositionAccountTo(positionAccount);
        securityIdSource = nos.getSecurityIDSource();
        nos.getSecurityIDTo(securityId);
        nos.getSecurityAltIDTo(securityAltId);
        nos.getSymbolTo(symbol);
        ordType = nos.getOrdType();
        price = nos.getPrice();
        orderQty = nos.getOrderQty();
        maxFloor = nos.getMaxFloor();
        side = nos.getSide();
        orderTs = nos.getOrderTs();
        nos.getClientAcronymTo(clientAcronym);
        transactTime = nos.getTransactTimeAsTimestamp();
        timeInForce = nos.getTimeInForce();
        nos.getTargetStrategyTo(targetStategy);
        nos.getTargetStrategyParametersTo(targetStrategyParameters);
        ordStatus = OrdStatus.NEW;
        orderCapacity = nos.getOrderCapacity();
        nos.getTextTo(lastText);
        leavesQty = 0.0;
        nos.getExecutingTraderTo(executingTrader);
        cumQuantity = nos.getOrderQty();
        handlInst = nos.getHandlInst();
        return this;
    }

    final public void setNosPostWireTs(final long ts) {
        nosPostWireTs = ts;
    }

    final public long getNosPostWireTs() {
        return nosPostWireTs;
    }

    final public XString getComplianceId() {
        return complianceId;
    }

    final public XString getOrderId() {
        return orderId;
    }

    final public XString getClOrdId() {
        return orderId;
    }

    final public XString getPositionAccount() {
        return positionAccount;
    }

    final public XString getSecurityId() {
        return securityId;
    }

    final public XString getSecurityAldID() {
        return securityAltId;
    }

    final public XString getSymbol() {
        return symbol;
    }

    final public XString getRouteId() {
        return routeId;
    }

    final public XString getClientAcronym() {
        return clientAcronym;
    }

    final public XString getTargetStrategy() {
        return targetStategy;
    }

    final public XString getTargetStrategyParameters() {
        return targetStrategyParameters;
    }

    final public XString getOutboundSystem() {
        return outboundSystem;
    }

    final public XString getExecutingTrader() {
        return executingTrader;
    }

    final public XString getLastText() {
        return lastText;
    }

    final public SecurityIDSource getSecurityIDSource() {
        return securityIdSource;
    }

    final public OrdType getOrderType() {
        return ordType;
    }

    final public double getPrice() {
        return price;
    }

    final public TimeInForce getTimeInForce() {
        return timeInForce;
    }

    final public OrdStatus getOrderStatus() {
        return ordStatus;
    }

    final public OrderCapacity getOrderCapacity() {
        return orderCapacity;
    }

    final public Side getSide() {
        return side;
    }

    final public HandlingInstructions getHandlInst() {
        return handlInst;
    }

    final public double getOrderQty() {
        return orderQty;
    }

    final public double getMaxFloor() {
        return maxFloor;
    }

    final public long getOriginTs() {
        return orderTs;
    }

    final public long getTransactTime() {
        return transactTime;
    }

    final public double getLeavesQty() {
        return leavesQty;
    }

    final public double getCumQuantity() {
        return cumQuantity;
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
