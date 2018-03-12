package com.neeve.oms.driver.local;

import com.neeve.ci.XRuntime;
import com.neeve.event.IEventHandler;
import com.neeve.pkt.PktPacket;
import com.neeve.sma.MessageView;
import com.neeve.sma.MessageChannel;
import com.neeve.sma.MessageBusDescriptor;
import com.neeve.sma.MessageChannelDescriptor;
import com.neeve.sma.SmaException;
import com.neeve.sma.impl.MessageBusBindingBase;
import com.neeve.stats.Stats.LatencyManager;
import com.neeve.trace.Tracer;
import com.neeve.util.UtlGovernor;
import com.neeve.util.UtlThread;

import com.neeve.oms.driver.NewOrderMessagePopulator;
import com.neeve.oms.messages.NewOrderMessage;

import com.neeve.fix.*;

final public class LocalMessageBusBinding extends MessageBusBindingBase implements Runnable {

    final private int sender = hashCode();

    private LatencyManager latencyManager;
    private volatile Thread sendThread;
    private int sendCount = XRuntime.getValue("oms.driver.sendCount", 10000);
    private int sendRate = XRuntime.getValue("oms.driver.sendRate", 1000);
    private boolean useFix = XRuntime.getValue("oms.driver.useFix", false);
    private int latencySampleSize = XRuntime.getValue("oms.driver.latencySampleSize", 1024 * 1024);
    private boolean printLatencyStats = XRuntime.getValue("oms.driver.printLatencyStats", true);
    private boolean summarizeLatencyStats = XRuntime.getValue("oms.driver.summarizeLatencyStats", true);
    private long sendAffinity = UtlThread.parseAffinityMask(XRuntime.getValue("oms.driver.sendAffinity", "0"));
    private int warmupCount;
    private int totalReceived;
    private int totalSent;

    LocalMessageBusBinding(final String userName,
                           final MessageBusDescriptor descriptor,
                           final IEventHandler eventHandler) throws SmaException {
        super(null, userName, descriptor, eventHandler);
    }

    /**
     * @param w2w Sets the w2w latency manager. 
     */
    public void setW2WLatencyManager(LatencyManager w2w) {
        latencyManager = w2w;
    }

    final private MessageView createNewOrderMessage() {
        if (useFix) {
            final FixMessage message = FixMessage.create();
            NewOrderMessagePopulator.populate(message);
            return message;
        }
        else {
            final NewOrderMessage message = NewOrderMessage.create();
            NewOrderMessagePopulator.populate(message);
            return message;
        }
    }

    final void send(final MessageView view) {
        if (++totalReceived > warmupCount) {
            final long preWireTs = System.nanoTime();
            view.setPreWireTs(preWireTs);
            latencyManager.add(view.getPreWireTs() - view.getPostWireTs());
            if (totalReceived % 10000 == 0) {
                latencyManager.compute();
                StringBuilder sb = new StringBuilder();
                latencyManager.get(sb);
                if (printLatencyStats) {
                    tracer.log(sb.toString(), Tracer.Level.INFO);
                }
            }
            if (summarizeLatencyStats && totalReceived == latencyManager.size()) {

                tracer.log("Writing latencies...", Tracer.Level.INFO);
                try {
                    final java.io.FileWriter writer = new java.io.FileWriter("latencies.csv");
                    latencyManager.get(new com.neeve.stats.IStats.Series.Collector() {
                        private double sum = 0.0;
                        private int count = 0;

                        @Override
                        final public void add(final long sequenceNumber, final double value) {
                            sum += value;
                            count++;
                            if (count == 1000) {
                                try {
                                    writer.write(String.valueOf((long)(sum / count)));
                                    writer.write("\n");
                                }
                                catch (java.io.IOException e1) {
                                    e1.printStackTrace();
                                }
                                count = 0;
                                sum = 0.0;
                            }
                        }
                    }, new cern.colt.list.DoubleArrayList(new double[latencyManager.size()]), 0);
                    writer.close();
                }
                catch (Throwable e) {
                    e.printStackTrace();
                }
                finally {
                    tracer.log("Done", Tracer.Level.INFO);
                }
            }
        }
    }

    @Override
    final protected void doOpen() throws SmaException {
        synchronized (this) {
            if (XRuntime.getValue("oms.driver.autoStart", false)) {
                startSender(sendCount, sendRate, useFix);
            }
            else {
                tracer.log("*** Opened Local Binding with autoStart=false", Tracer.Level.INFO);
            }
        }
    }

    @Override
    final protected MessageChannel doGetMessageChannel(final MessageChannelDescriptor descriptor) throws SmaException {
        return new LocalMessageChannel(descriptor, this);
    }

    @Override
    final protected void doStart() throws SmaException {}

    @Override
    final protected void doFlush(final FlushContext flushContext) throws SmaException {
        if (flushContext != null) {
            switch (flushContext.flushMode) {
                case SYNC_BLOCKING:
                    ((SynchronousBlockingFlushContext)flushContext).complete = true;
                    break;

                case SYNC_NON_BLOCKING:
                    ((SynchronousNonBlockingFlushContext)flushContext).complete = true;
                    break;

                case ASYNC:
                    ((AsynchronousFlushContext)flushContext).syncComplete = true;
                    break;

                default:
                    break;
            }
        }
    }

    @Override
    final protected boolean doCanFail() {
        return false;
    }

    @Override
    final protected boolean doAcksRequireFlush() {
        return false;
    }

    @Override
    final protected void doClose() throws SmaException {}

    /**
     * Starts the sender if not already running. 
     * 
     * @param sendCount The number to send. 
     * @param sendRate The rate at which to send. 
     * @param useFix The rate at which to send. 
     */
    final public void startSender(final int sendCount, final int sendRate, final boolean useFix) {
        this.sendCount = sendCount;
        this.sendRate = sendRate;
        this.useFix = useFix;
        sendThread = new Thread(this, "Local Driver Send Thread");
        sendThread.start();
    }

    @Override
    final public void run() {
        try {
            warmupCount = Math.min(50000, (sendCount / 3));
            tracer.log("*** Send Rate=" + sendRate, Tracer.Level.INFO);
            tracer.log("*** Send Count=" + sendCount, Tracer.Level.INFO);
            tracer.log("*** Send Affinity =" + sendAffinity, Tracer.Level.INFO);
            tracer.log("*** Trace Stats =" + printLatencyStats, Tracer.Level.INFO);
            tracer.log("*** Summarize Stats =" + summarizeLatencyStats, Tracer.Level.INFO);
            tracer.log("*** Use FIX =" + useFix, Tracer.Level.INFO);
            UtlThread.setCPUAffinityMask(sendAffinity);
            Thread.sleep(10000);
            final LocalMessageChannel channel = (LocalMessageChannel)getMessageChannel("requests");
            if (latencyManager == null) {
                latencyManager = new LatencyManager("local-w2w", latencySampleSize);
            }
            UtlGovernor.run(sendCount, sendRate, new Runnable() {
                @Override
                final public void run() {
                    try {
                        ++totalSent;
                        final MessageView view = createNewOrderMessage();
                        final PktPacket packet = view.serializeToPacket();
                        final short vfid = view.getVfid();
                        final short type = view.getType();
                        final int encodingType = view.getMessageEncodingType();
                        view.dispose();
                        final long now = System.nanoTime();
                        LocalMessageBusBinding.this.onMessage(channel,
                                                              LocalMessageBusBinding.this.wrap(packet,
                                                                                               vfid,
                                                                                               type,
                                                                                               encodingType,
                                                                                               sender,
                                                                                               0,
                                                                                               0l,
                                                                                               null,
                                                                                               0l,
                                                                                               0l,
                                                                                               now,
                                                                                               now),
                                                              null);
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
        }
        catch (Throwable e) {
            e.printStackTrace();
        }
    }

    public int getTotalSent() {
        return totalSent;
    }

    public int getTotalReceived() {
        return totalReceived;
    }
}
