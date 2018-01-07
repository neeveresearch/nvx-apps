package com.neeve.ccfd.cardmaster.driver;

import java.util.Random;

import com.neeve.ccfd.messages.NewCardMessage;
import com.neeve.ccfd.messages.AuthorizationRequestMessage;
import com.neeve.ccfd.util.TestDataGenerator;
import com.neeve.ccfd.util.UtlCommon;
import com.neeve.aep.AepMessageSender;
import com.neeve.cli.annotations.Argument;
import com.neeve.cli.annotations.Command;
import com.neeve.cli.annotations.Configured;
import com.neeve.lang.XLinkedList;
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
    private final XLinkedList<String> addedCardNumbers = new XLinkedList<String>();
    private volatile AepMessageSender messageSender;

    @Configured(property = "cardmaster.numShards")
    private int cardMasterNumShards;

    @AppInjectionPoint
    final public void setMessageSender(AepMessageSender messageSender) {
        this.messageSender = messageSender;
    }

    @Command(name = "addCards", description = "Instructs the driver to add cards to the card master")
    public final void addCards(@Argument(name = "count", position = 1, required = true, description = "The number of cards to add") int count) {
        for (int i = 0; i < count; i++) {
            final String cardNumber = TestDataGenerator.generateId();
            addedCardNumbers.add(cardNumber);
            NewCardMessage newCardMessage = NewCardMessage.create();
            newCardMessage.setRequestId(TestDataGenerator.generateId());
            newCardMessage.setCardNumber(cardNumber);
            newCardMessage.setCardHolderId(TestDataGenerator.generateId());
            messageSender.sendMessage("card-events", newCardMessage, UtlCommon.getShardKey(cardNumber, cardMasterNumShards));
        }
    }

    @Command(name = "sendAuths", description = "Instructs the driver to send authorization requests")
    public final void sendAuthorizationRequests(@Argument(name = "count", position = 1, required = true, description = "The number of messages to send") int count,
                                                @Argument(name = "rate", position = 2, required = true, description = "The rate at which to send") int rate) {
        UtlGovernor.run(count, rate, new Runnable() {
            @Override
            public void run() {
                AuthorizationRequestMessage message = AuthorizationRequestMessage.create();
                message.setFlowStartTs(UtlTime.now());
                message.setRequestId(TestDataGenerator.generateId());
                message.setNewTransaction(testDataGenerator.generateTransactionMessage(addedCardNumbers.get(random.nextInt(addedCardNumbers.size())),
                                                                                       TestDataGenerator.generateId(),
                                                                                       TestDataGenerator.generateId()));
                messageSender.sendMessage("authreq", message, UtlCommon.getShardKey(message.getNewTransaction().getCardNumber(), cardMasterNumShards));
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
    public void run(String[] args) {
        addCards(100);
        sendAuthorizationRequests(sendCount, sendRate);
    }
}
