package com.neeve.ccfd.perfdriver;

import static com.neeve.ccfd.util.TestDataGenerator.*;
import java.util.concurrent.atomic.AtomicBoolean;

import com.neeve.aep.AepEngine;
import com.neeve.aep.AepMessageSender;
import com.neeve.aep.annotations.EventHandler;
import com.neeve.ccfd.messages.AuthorizationApprovedMessage;
import com.neeve.ccfd.messages.AuthorizationDeclinedMessage;
import com.neeve.ccfd.messages.AuthorizationRequestMessage;
import com.neeve.ccfd.messages.NewCardHolderMessage;
import com.neeve.ccfd.messages.NewCardMessage;
import com.neeve.ccfd.messages.NewMerchantMessage;
import com.neeve.ccfd.messages.PaymentTransactionDTO;
import com.neeve.ccfd.util.TestDataGenerator;
import com.neeve.cli.annotations.Command;
import com.neeve.cli.annotations.Configured;
import com.neeve.cli.annotations.Option;
import com.neeve.lang.XIterator;
import com.neeve.lang.XLinkedList;
import com.neeve.lang.XString;
import com.neeve.server.app.annotations.AppHAPolicy;
import com.neeve.server.app.annotations.AppInjectionPoint;
import com.neeve.server.app.annotations.AppMain;
import com.neeve.server.app.annotations.AppStat;
import com.neeve.stats.IStats.Counter;
import com.neeve.stats.IStats.Latencies;
import com.neeve.stats.StatsFactory;
import com.neeve.util.UtlGovernor;
import com.neeve.util.UtlTime;

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
                    XIterator<XString> cardIterator = existingCardNumbers.iterator();
                    XIterator<XString> merchantIterator = existingMerchantIds.iterator();
                    XIterator<XString> merchantStoreIterator = existingMerchantStoreIds.iterator();
                    while (numSent++ < count && !stopRequested) {
                        if (!cardIterator.hasNext()) {
                            cardIterator = cardIterator.toFirst();
                        }
                        if (!merchantIterator.hasNext()) {
                            merchantIterator = merchantIterator.toFirst();
                            merchantStoreIterator = merchantStoreIterator.toFirst();
                        }
                        governer.blockToNext();
                        final PaymentTransactionDTO newTransaction = dataGenerator.generateTransactionMessage(cardIterator.next(),
                                                                                                              merchantIterator.next(),
                                                                                                              merchantStoreIterator.next());

                        AuthorizationRequestMessage message = AuthorizationRequestMessage.create();
                        message.setFlowStartTs(UtlTime.now());
                        message.setRequestIdFrom(generateIdTo(tempIdHolder()));
                        message.setCardNumberFrom(newTransaction.getCardNumberUnsafe());
                        message.setMerchantIdFrom(newTransaction.getMerchantIdUnsafe());
                        message.setNewTransaction(newTransaction);

                        _messageSender.sendMessage("authreq", message);
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
    private final XLinkedList<XString> existingCardNumbers = new XLinkedList<XString>();
    private final XLinkedList<XString> existingMerchantIds = new XLinkedList<XString>();
    private final XLinkedList<XString> existingMerchantStoreIds = new XLinkedList<XString>();
    @Configured(property = "driver.autoStart", defaultValue = "true")
    private boolean autoStart;
    @Configured(property = "driver.sendCount")
    private int sendCount;
    @Configured(property = "driver.sendRate")
    private int sendRate;

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

    @Command(name = "seedCardHolders", displayName = "Seed Card Holders", description = "Seeds card holders with their transaction history.")
    public final void seedCardHolders(@Option(shortForm = 'c', longForm = "count", defaultValue = "100", description = "The number of card holders to seed") final int count,
                                      @Option(shortForm = 'r', longForm = "rate", defaultValue = "100", description = "The rate at which to send in card holders") final int rate,
                                      @Option(shortForm = 'a', longForm = "async", defaultValue = "true", description = "Whether or not to spin up a background thread to do the sends") final boolean async) {
        if (authSendRunner.isRunning()) {
            throw new IllegalStateException("Can't add card holders while authorization sender is running!");
        }
        final XIterator<XString> merchantIterator = existingMerchantIds.iterator();
        final XIterator<XString> merchantStoreIterator = existingMerchantStoreIds.iterator();
        if (!merchantIterator.hasNext()) {
            throw new IllegalStateException("Can't seed card holders before seeding merchants");
        }

        final Runnable seedOperation = new Runnable() {
            @Override
            public void run() {
                try {
                    NewCardHolderMessage newCardHolderMessage = dataGenerator.generateCardHolderMessage(350, 2, merchantIterator.next(), merchantStoreIterator.next());
                    for (String cardNumber : newCardHolderMessage.getCardNumbers()) {
                        existingCardNumbers.add(XString.create(cardNumber));
                        NewCardMessage newCardMessage = NewCardMessage.create();
                        newCardMessage.setRequestIdFrom(generateIdTo(tempIdHolder()));
                        newCardMessage.setCardNumber(cardNumber);
                        newCardMessage.setCardHolderIdFrom(newCardHolderMessage.getCardHolderIdUnsafe());
                        _messageSender.sendMessage("card-events", newCardMessage);
                        newCardRequestCount.increment();
                    }
                    _messageSender.sendMessage("cardholder-events", newCardHolderMessage);
                    newCardHolderRequestCount.increment();
                }
                catch (Exception e) {
                    e.printStackTrace();
                    throw new IllegalStateException("This should never happen. Added for UtlReflector.");
                }
            }
        };

        if (async) {
            Thread seederThread = new Thread(new Runnable() {
                public void run() {
                    UtlGovernor.run(count, rate, seedOperation);
                }
            }, "Card Holder Seeder");
            seederThread.start();
        }
        else {
            UtlGovernor.run(count, rate, seedOperation);
        }
    }

    @Command(name = "seedMerchants", displayName = "Seed Merchants", description = "Seeds merchants with their stores.")
    public final void seedMerchants(@Option(shortForm = 'c', longForm = "count", defaultValue = "100", description = "The number of merchants to seed") int count,
                                    @Option(shortForm = 'r', longForm = "rate", defaultValue = "100", description = "The rate at which to send in merchants") int rate) {
        if (authSendRunner.isRunning()) {
            throw new IllegalStateException("Can't add merchants while authorization sender is running!");
        }

        UtlGovernor.run(count, rate, new Runnable() {
            @Override
            public void run() {
                try {
                    NewMerchantMessage newMerchantMessage = dataGenerator.generateNewMerchantMessage(1);
                    existingMerchantIds.add(XString.create(newMerchantMessage.getMerchantId()));
                    existingMerchantStoreIds.add(XString.create(newMerchantMessage.getStoresIterator().next().getStoreId()));
                    _messageSender.sendMessage("merchant-events", newMerchantMessage);
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
    final public void onAuthorizationApproved(AuthorizationApprovedMessage message) {
        authorizationResponseCount.increment();
        authorizationServeLatencies.add(UtlTime.now() - message.getFlowStartTs());
    }

    @EventHandler
    final public void onAuthorizationDeclined(AuthorizationDeclinedMessage message) {
        authorizationResponseCount.increment();
        authorizationServeLatencies.add(UtlTime.now() - message.getFlowStartTs());
    }

    @AppMain
    public void run(String[] args) {
        if (autoStart) {
            seedMerchants(100, 100);
            seedCardHolders(100, 100, false);
            sendAuthorizationRequests(sendCount, sendRate, false);
        }
    }
}
