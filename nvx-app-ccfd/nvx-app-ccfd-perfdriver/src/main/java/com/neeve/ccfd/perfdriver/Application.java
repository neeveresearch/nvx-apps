package com.neeve.ccfd.perfdriver;

import java.util.concurrent.atomic.AtomicBoolean;

import com.neeve.cli.annotations.Command;
import com.neeve.cli.annotations.Option;
import com.neeve.cli.annotations.Configured;
import com.neeve.aep.AepEngine;
import com.neeve.aep.AepMessageSender;
import com.neeve.aep.annotations.EventHandler;
import com.neeve.lang.XIterator;
import com.neeve.lang.XLinkedList;
import com.neeve.server.app.annotations.AppInjectionPoint;
import com.neeve.server.app.annotations.AppHAPolicy;
import com.neeve.server.app.annotations.AppMain;
import com.neeve.server.app.annotations.AppStat;
import com.neeve.stats.IStats.Counter;
import com.neeve.stats.IStats.Latencies;
import com.neeve.stats.StatsFactory;
import com.neeve.util.UtlGovernor;
import com.neeve.util.UtlTime;

import com.neeve.ccfd.messages.AuthorizationRequestMessage;
import com.neeve.ccfd.messages.AuthorizationResponseMessage;
import com.neeve.ccfd.messages.NewCardHolderMessage;
import com.neeve.ccfd.messages.NewCardMessage;
import com.neeve.ccfd.messages.NewMerchantMessage;
import com.neeve.ccfd.util.TestDataGenerator;
import com.neeve.ccfd.util.UtlCommon;

@AppHAPolicy(value = AepEngine.HAPolicy.StateReplication)
public class Application {
    private final class AuthSendRunner {
        private final AtomicBoolean running = new AtomicBoolean();
        private volatile boolean stopRequested = false;

        AuthSendRunner() {
            running.set(false);
        }

        public final boolean isRunning() {
            return running.get();
        }

        public synchronized final void stop() throws InterruptedException {
            while (isRunning()) {
                stopRequested = true;
                wait();
            }
            stopRequested = false;
        }

        public void sendAuthorizationRequests(final int count, final int rate, boolean async) {
            if (isRunning()) {
                throw new IllegalStateException("Sender is already running");
            }

            if (existingCardNumbers.size() == 0) {
                throw new IllegalStateException("There are no payment cards in the system.");
            }
            if (async) {
                new Thread(new Runnable() {
                    public void run() {
                        sendAuthorizationRequests(count, rate, false);
                    }
                }, "Performance Driver Auth Send Thread").start();
            }
            else {
                if (!running.compareAndSet(false, true)) {
                    throw new IllegalStateException("Sender is already running");
                }
                try {
                    UtlGovernor governer = new UtlGovernor(rate);
                    int numSent = 0;
                    XIterator<String> cardIterator = existingCardNumbers.iterator();
                    XIterator<String> merchantIterator = existingMerchantIds.iterator();
                    XIterator<String> merchantStoreIterator = existingMerchantStoreIds.iterator();
                    while (numSent++ < count && !stopRequested) {
                        if (!cardIterator.hasNext()) {
                            cardIterator = cardIterator.toFirst();
                        }
                        if (!merchantIterator.hasNext()) {
                            merchantIterator = merchantIterator.toFirst();
                            merchantStoreIterator = merchantStoreIterator.toFirst();
                        }
                        governer.blockToNext();
                        AuthorizationRequestMessage message = AuthorizationRequestMessage.create();
                        message.setFlowStartTs(UtlTime.now());
                        message.setRequestId(TestDataGenerator.generateId());
                        message.setNewTransaction(dataGenerator.generateTransactionMessage(cardIterator.next(),
                                                                                           merchantIterator.next(),
                                                                                           merchantStoreIterator.next()));

                        _messageSender.sendMessage("authreq",
                                                   message,
                                                   UtlCommon.getShardKey(message.getNewTransaction().getCardNumber(), cardMasterNumShards));
                        authorizationRequestCount.increment();
                    }
                }
                finally {
                    running.set(false);
                    synchronized (this) {
                        notifyAll();
                    }
                }
            }
        }
    }

    private AepMessageSender _messageSender;
    private final XLinkedList<String> existingCardNumbers = new XLinkedList<String>();
    private final XLinkedList<String> existingMerchantIds = new XLinkedList<String>();
    private final XLinkedList<String> existingMerchantStoreIds = new XLinkedList<String>();
    @Configured(property = "driver.sendCount")
    private int sendCount;
    @Configured(property = "driver.sendRate")
    private int sendRate;

    @Configured(property = "cardholdermaster.numShards")
    private int cardholderMasterNumShards;

    @Configured(property = "merchantmaster.numShards")
    private int merchantMasterNumShards;

    @Configured(property = "cardmaster.numShards")
    private int cardMasterNumShards;

    @AppStat
    private final Counter newCardRequestCount = StatsFactory.createCounterStat("NewCardRequest Sent Count");
    @AppStat
    private final Counter newCardHolderRequestCount = StatsFactory.createCounterStat("NewCardHolderRequest Sent Count");
    @AppStat
    private final Counter newMerchantRequestCount = StatsFactory.createCounterStat("NewMerchantRequest Sent Count");
    @AppStat
    private final Counter authorizationRequestCount = StatsFactory.createCounterStat("AuthorizationRequest Sent Count");
    @AppStat
    private final Counter authorizationResponseCount = StatsFactory.createCounterStat("AuthorizationResponse Received Count");
    @AppStat
    private final Latencies authorizationServeLatencies = StatsFactory.createLatencyStat("Payment Authorization Time");
    private final AuthSendRunner authSendRunner = new AuthSendRunner();
    private final TestDataGenerator dataGenerator = new TestDataGenerator(100);

    @AppInjectionPoint
    final public void setMessageSender(AepMessageSender messageSender) {
        _messageSender = messageSender;
    }

    @Command(displayName = "Seed Card Holders", description = "Seeds card holders with their transaction history.")
    public final void seedCardHolders(@Option(shortForm = 'c', longForm = "count", defaultValue = "100", description = "The number of card holders to send") int count,
                                      @Option(shortForm = 'r', longForm = "rate", defaultValue = "100", description = "The rate at which to send in card holders") int rate) {
        if (authSendRunner.isRunning()) {
            throw new IllegalStateException("Can't add card holders while authorization sender is running!");
        }
        final XIterator<String> merchantIterator = existingMerchantIds.iterator();
        final XIterator<String> merchantStoreIterator = existingMerchantStoreIds.iterator();
        if (!merchantIterator.hasNext()) {
            throw new IllegalStateException("Can't seed card holders before seeding merchants");
        }

        UtlGovernor.run(count, rate, new Runnable() {
            @Override
            public void run() {
                try {
                    NewCardHolderMessage newCardHolderMessage = dataGenerator.generateCardHolderMessage(350, 2, merchantIterator.next(), merchantStoreIterator.next());
                    for (String cardNumber : newCardHolderMessage.getCardNumbers()) {
                        existingCardNumbers.add(cardNumber);
                        NewCardMessage newCardMessage = NewCardMessage.create();
                        newCardMessage.setRequestId(TestDataGenerator.generateId());
                        newCardMessage.setCardNumber(cardNumber);
                        newCardMessage.setCardHolderId(newCardHolderMessage.getCardHolderId());
                        _messageSender.sendMessage("authreq", newCardMessage, UtlCommon.getShardKey(newCardMessage.getCardNumber(), cardMasterNumShards));
                        newCardRequestCount.increment();
                    }
                    _messageSender.sendMessage("authreq3", newCardHolderMessage, UtlCommon.getShardKey(newCardHolderMessage.getCardHolderId(), cardholderMasterNumShards));
                    newCardHolderRequestCount.increment();
                }
                catch (Exception e) {
                    e.printStackTrace();
                    throw new IllegalStateException("This should never happen. Added for UtlReflector.");
                }
            }
        });
    }

    @Command(displayName = "Seed Merchants", description = "Seeds merchants with their stores.")
    public final void seedMerchants(@Option(shortForm = 'c', longForm = "count", defaultValue = "100", description = "The number of merchants to send") int count,
                                    @Option(shortForm = 'r', longForm = "rate", defaultValue = "100", description = "The rate at which to send in merchants") int rate) {
        if (authSendRunner.isRunning()) {
            throw new IllegalStateException("Can't add merchants while authorization sender is running!");
        }

        UtlGovernor.run(count, rate, new Runnable() {
            @Override
            public void run() {
                try {
                    NewMerchantMessage newMerchantMessage = dataGenerator.generateNewMerchantMessage(1);
                    existingMerchantIds.add(newMerchantMessage.getMerchantId());
                    existingMerchantStoreIds.add(newMerchantMessage.getStoresIterator().next().getStoreId());
                    _messageSender.sendMessage("authreq2", newMerchantMessage, UtlCommon.getShardKey(newMerchantMessage.getMerchantId(), merchantMasterNumShards));
                    newMerchantRequestCount.increment();
                }
                catch (Exception e) {
                    e.printStackTrace();
                    throw new IllegalStateException("This should never happen. Added for UtlReflector.");
                }
            }
        });
    }

    @Command(displayName = "Send Authorization Requests", description = "Drives Authorization Request traffic")
    public final void sendAuthorizationRequests(@Option(shortForm = 'c', longForm = "count", defaultValue = "10000", description = "The rate at which to send requests") int count,
                                                @Option(shortForm = 'r', longForm = "rate", defaultValue = "1000", description = "The rate at which to send requests") int rate,
                                                @Option(shortForm = 'a', longForm = "async", defaultValue = "true", description = "Whether or not to spin up a background thread to do the sends") boolean async) {
        try {
            authSendRunner.stop();
        }
        catch (InterruptedException e) {
            throw new RuntimeException("Couldn't stop current send in progress (interupted)");
        }
        authSendRunner.sendAuthorizationRequests(count, rate, async);
    }

    @Command(displayName = "Stop Sending", description = "Halts Requests being sent to driven app")
    public void stopAuthorizationRequests() throws InterruptedException {
        authSendRunner.stop();
    }

    @Command(displayName = "Get Authorization Request Count", description = "Gets the number of authorizations requested")
    public long getAuthorizationRequestCount() {
        return authorizationRequestCount.getCount();
    }

    @Command(displayName = "Get Authorization Response Count", description = "Gets the number of authorizations received")
    public long getAuthorizationResponseCount() {
        return authorizationResponseCount.getCount();
    }

    @EventHandler
    final public void onAuthorizationResponseMessage(AuthorizationResponseMessage message) {
        authorizationResponseCount.increment();
        authorizationServeLatencies.add(UtlTime.now() - message.getFlowStartTs());
    }

    @AppMain
    public void run(String[] args) {
        seedMerchants(100, 100);
        seedCardHolders(100, 100);
        sendAuthorizationRequests(sendCount, sendRate, false);
    }
}
