package com.neeve.ccfd.fraudanalyzer.driver;

import static com.neeve.ccfd.util.TestDataGenerator.*;

import com.neeve.aep.AepMessageSender;
import com.neeve.ccfd.messages.FraudAnalysisRequestMessage;
import com.neeve.ccfd.util.TestDataGenerator;
import com.neeve.cli.annotations.Argument;
import com.neeve.cli.annotations.Command;
import com.neeve.cli.annotations.Configured;
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
    private volatile AepMessageSender messageSender;

    @AppInjectionPoint
    final public void setMessageSender(AepMessageSender messageSender) {
        this.messageSender = messageSender;
    }

    @Command(name = "sendAuths", description = "Instructs the driver to send fraud analysis authorization requests")
    public final void sendAuthorizationRequests(@Argument(name = "count", position = 1, required = true, description = "The number of messages to send") int count,
                                                @Argument(name = "rate", position = 2, required = true, description = "The rate at which to send") int rate) {
        UtlGovernor.run(count, rate, new Runnable() {
            @Override
            public void run() {
                FraudAnalysisRequestMessage message = FraudAnalysisRequestMessage.create();
                message.setRequestIdFrom(generateIdTo(tempIdHolder()));
                message.setFlowStartTs(UtlTime.now());

                message.setNewTransaction(testDataGenerator.generateTransformedTransactionMessage());

                message.setCardHolderIdFrom(generateIdTo(tempIdHolder()));
                message.setMerchantStoreCountryCode(DEFAULT_COUNTRY_CODE);
                message.setMerchantStorePostcode(DEFAULT_POSTAL_CODE);
                messageSender.sendMessage("authreq4", message);
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
        sendAuthorizationRequests(sendCount, sendRate);
    }
}
