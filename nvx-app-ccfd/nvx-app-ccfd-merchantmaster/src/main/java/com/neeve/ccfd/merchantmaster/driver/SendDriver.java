package com.neeve.ccfd.merchantmaster.driver;

import java.util.Random;

import com.neeve.ccfd.messages.NewMerchantMessage;
import com.neeve.ccfd.messages.PaymentTransactionDTO;
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
    private final XLinkedList<String> addedMerchantIds = new XLinkedList<String>();
    private final XLinkedList<String> addedMerchantStores = new XLinkedList<String>();
    private volatile AepMessageSender messageSender;

    @AppInjectionPoint
    final public void setMessageSender(AepMessageSender messageSender) {
        this.messageSender = messageSender;
    }

    @Command(name = "addMerchants", description = "Instructs the driver to add merchants to the merchant master")
    public final void addMerchants(@Argument(name = "count", position = 1, required = true, description = "The number of merchants to add") int count) {
        for (int i = 0; i < count; i++) {
            NewMerchantMessage newMerchantMessage = testDataGenerator.generateNewMerchantMessage(1);
            addedMerchantIds.add(newMerchantMessage.getMerchantId());
            addedMerchantStores.add(newMerchantMessage.getStoresIterator().next().getStoreId());
            messageSender.sendMessage("authreq2", newMerchantMessage);
        }
    }

    @Command(name = "sendAuths", description = "Instructs the driver to send authorization requests")
    public final void sendAuthorizationRequests(@Argument(name = "count", position = 1, required = true, description = "The number of messages to send") int count,
                                                @Argument(name = "rate", position = 2, required = true, description = "The rate at which to send") int rate) {

        UtlGovernor.run(count, rate, new Runnable() {
            @Override
            public void run() {
                int merchantIndex = random.nextInt(addedMerchantIds.size());
                PaymentTransactionDTO newTransaction = testDataGenerator.generateTransactionMessage(TestDataGenerator.generateId(),
                                                                                                    addedMerchantIds.get(merchantIndex),
                                                                                                    addedMerchantStores.get(merchantIndex));
                AuthorizationRequestMessage message = AuthorizationRequestMessage.create();
                message.setFlowStartTs(UtlTime.now());
                message.setRequestId(TestDataGenerator.generateId());
                message.setCardHolderId(UtlCommon.generateId());
                message.setCardNumber(newTransaction.getCardNumber());
                message.setMerchantId(newTransaction.getMerchantId());
                message.setNewTransaction(newTransaction);

                messageSender.sendMessage("authreq2", message);
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
        addMerchants(100);
        sendAuthorizationRequests(sendCount, sendRate);
    }
}
