package com.neeve.ccfd.cardholdermaster.driver;

import java.util.Random;

import com.neeve.aep.AepMessageSender;
import com.neeve.ccfd.messages.AuthorizationRequestMessage;
import com.neeve.ccfd.messages.NewCardHolderMessage;
import com.neeve.ccfd.util.TestDataGenerator;
import com.neeve.cli.annotations.Argument;
import com.neeve.cli.annotations.Command;
import com.neeve.cli.annotations.Configured;
import com.neeve.lang.XLinkedList;
import com.neeve.lang.XString;
import com.neeve.server.app.annotations.AppInjectionPoint;
import com.neeve.server.app.annotations.AppMain;
import com.neeve.server.app.annotations.AppStat;
import com.neeve.stats.IStats.Counter;
import com.neeve.stats.StatsFactory;
import com.neeve.util.UtlGovernor;
import com.neeve.util.UtlTime;

/**
 * A test driver app for the Application.
 */
public class SendDriver {
    @Configured(property = "driver.sendCount")
    private int sendCount;
    @Configured(property = "driver.sendRate")
    private int sendRate;

    @AppStat
    private final Counter sentCount = StatsFactory.createCounterStat("SendDriver Count");
    private final TestDataGenerator testDataGenerator = new TestDataGenerator(100);
    private final Random random = new Random(System.currentTimeMillis());
    private final XLinkedList<XString> addedCardHolderIds = new XLinkedList<XString>();
    private volatile AepMessageSender messageSender;

    @AppInjectionPoint
    final public void setMessageSender(AepMessageSender messageSender) {
        this.messageSender = messageSender;
    }

    @Command(name = "addCardHolders", description = "Instructs the driver to add card holders to the card holder master")
    public final void addCardHolders(@Argument(name = "count", position = 1, required = true, description = "The number of card holders to add") int count) throws Exception {
        final XString merchantIdGen = XString.create(32, true, true);
        final XString merchantStoreIdGen = XString.create(32, true, true);
        for (int i = 0; i < count; i++) {
            NewCardHolderMessage newCardHolderMessage = testDataGenerator.generateCardHolderMessage(350, 2, TestDataGenerator.generateIdTo(merchantIdGen), TestDataGenerator.generateIdTo(merchantStoreIdGen));
            addedCardHolderIds.add(XString.create(newCardHolderMessage.getCardHolderId()));
            messageSender.sendMessage("authreq3", newCardHolderMessage);
        }
    }

    @Command(name = "sendAuths", description = "Instructs the driver to send authorization requests")
    public final void sendAuthorizationRequests(@Argument(name = "count", position = 1, required = true, description = "The number of messages to send") int count,
                                                @Argument(name = "rate", position = 2, required = true, description = "The rate at which to send") int rate) {

        final XString requestIdGen = XString.create(32, true, true);
        final XString cardNumberGen = XString.create(32, true, true);
        final XString merchantIdGen = XString.create(32, true, true);
        final XString merchantStoreIdGen = XString.create(32, true, true);
        UtlGovernor.run(count, rate, new Runnable() {
            @Override
            public void run() {
                AuthorizationRequestMessage message = AuthorizationRequestMessage.create();
                message.setFlowStartTs(UtlTime.now());
                message.setRequestIdFrom(TestDataGenerator.generateIdTo(requestIdGen));
                message.setNewTransaction(testDataGenerator.generateTransactionMessage(TestDataGenerator.generateIdTo(cardNumberGen),
                                                                                       TestDataGenerator.generateIdTo(merchantIdGen),
                                                                                       TestDataGenerator.generateIdTo(merchantStoreIdGen)));
                message.setCardHolderIdFrom(addedCardHolderIds.get(random.nextInt(addedCardHolderIds.size())));
                messageSender.sendMessage("authreq3", message);
                sentCount.increment();
            }
        });
    }

    /**
     * Gets the number of messages sent by the sender. 
     * 
     * @return The number of messages sent by this sender.
     */
    @Command
    public long getSentCount() {
        return sentCount.getCount();
    }

    @AppMain
    public void run(String[] args) throws Exception {
        addCardHolders(100);
        sendAuthorizationRequests(sendCount, sendRate);
    }
}
