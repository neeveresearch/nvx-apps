package com.neeve.ccfd.cardmaster.driver;

import java.util.Random;

import com.neeve.ccfd.messages.NewCardMessage;
import com.neeve.ccfd.messages.PaymentTransactionDTO;
import com.neeve.ccfd.messages.AuthorizationRequestMessage;
import com.neeve.ccfd.util.TestDataGenerator;
import com.neeve.aep.AepMessageSender;
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
    private final XLinkedList<XString> addedCardNumbers = new XLinkedList<XString>();
    private volatile AepMessageSender messageSender;

    @AppInjectionPoint
    final public void setMessageSender(AepMessageSender messageSender) {
        this.messageSender = messageSender;
    }

    @Command(name = "addCards", description = "Instructs the driver to add cards to the card master")
    public final void addCards(@Argument(name = "count", position = 1, required = true, description = "The number of cards to add") int count) {
        final XString idGen = XString.create(32, true, true);
        for (int i = 0; i < count; i++) {
            final XString cardNumber = TestDataGenerator.generateIdTo(XString.create(32, true, true));
            addedCardNumbers.add(cardNumber);
            NewCardMessage newCardMessage = NewCardMessage.create();
            newCardMessage.setRequestIdFrom(TestDataGenerator.generateIdTo(idGen));
            newCardMessage.setCardNumberFrom(cardNumber);
            newCardMessage.setCardHolderIdFrom(TestDataGenerator.generateIdTo(idGen));
            messageSender.sendMessage("card-events", newCardMessage);
        }
    }

    @Command(name = "sendAuths", description = "Instructs the driver to send authorization requests")
    public final void sendAuthorizationRequests(@Argument(name = "count", position = 1, required = true, description = "The number of messages to send") int count,
                                                @Argument(name = "rate", position = 2, required = true, description = "The rate at which to send") int rate) {
        final XString merchangeIdGen = XString.create(32, true, true);
        final XString merchantStoreIdGen = XString.create(32, true, true);
        final XString requestIdGen = XString.create(32, true, true);

        UtlGovernor.run(count, rate, new Runnable() {
            @Override
            public void run() {
                PaymentTransactionDTO transaction = testDataGenerator.generateTransactionMessage(addedCardNumbers.get(random.nextInt(addedCardNumbers.size())),
                                                                                                 TestDataGenerator.generateIdTo(merchangeIdGen),
                                                                                                 TestDataGenerator.generateIdTo(merchantStoreIdGen));

                AuthorizationRequestMessage message = AuthorizationRequestMessage.create();
                message.setFlowStartTs(UtlTime.now());
                message.setRequestIdFrom(TestDataGenerator.generateIdTo(requestIdGen));
                message.setCardNumberFrom(transaction.getCardNumberUnsafe());
                message.setMerchantIdFrom(transaction.getMerchantIdUnsafe());
                message.setNewTransaction(transaction);
                messageSender.sendMessage("authreq", message);
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
