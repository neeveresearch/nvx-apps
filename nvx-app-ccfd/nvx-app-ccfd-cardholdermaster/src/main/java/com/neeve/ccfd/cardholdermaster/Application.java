package com.neeve.ccfd.cardholdermaster;

import com.neeve.aep.AepEngine;
import com.neeve.aep.AepMessageSender;
import com.neeve.aep.IAepApplicationStateFactory;
import com.neeve.aep.annotations.EventHandler;
import com.neeve.ccfd.cardholdermaster.state.CardHolder;
import com.neeve.ccfd.cardholdermaster.state.PaymentTransaction;
import com.neeve.ccfd.cardholdermaster.state.Repository;
import com.neeve.ccfd.messages.AuthorizationRequestMessage;
import com.neeve.ccfd.messages.AuthorizationResponseMessage;
import com.neeve.ccfd.messages.FraudAnalysisRequestMessage;
import com.neeve.ccfd.messages.NewCardHolderMessage;
import com.neeve.ccfd.messages.PaymentTransactionDTO;
import com.neeve.ccfd.util.TestDataGenerator;
import com.neeve.ccfd.util.UtlCommon;
import com.neeve.cli.annotations.Configured;
import com.neeve.server.app.annotations.AppHAPolicy;
import com.neeve.server.app.annotations.AppInjectionPoint;
import com.neeve.server.app.annotations.AppStat;
import com.neeve.server.app.annotations.AppStateFactoryAccessor;
import com.neeve.sma.MessageView;
import com.neeve.stats.IStats.Counter;
import com.neeve.stats.IStats.Latencies;
import com.neeve.stats.StatsFactory;
import com.neeve.util.UtlTime;

@AppHAPolicy(value = AepEngine.HAPolicy.StateReplication)
public class Application {

    private final TestDataGenerator testDataGenerator = new TestDataGenerator(100);
    private AepMessageSender _messageSender;

    @AppStat
    private final Counter authorizationRequestCount = StatsFactory.createCounterStat("Authorization Request Received Count");
    @AppStat
    private final Counter newCardHolderRequestCount = StatsFactory.createCounterStat("New CardHolder Request Received Count");
    @AppStat
    private final Latencies authorizationProcessingLatencies = StatsFactory.createLatencyStat("Authorization Processing Time");

    @Configured(property = "fraudanalyzer.numShards")
    private int fraudanalyzerNumShards;

    private boolean simulateSoftwareFraudCheck() {
        long ts = System.nanoTime();
        while ((System.nanoTime() - ts) < 100000)
            ;
        return false;
    }

    @AppStateFactoryAccessor
    final public IAepApplicationStateFactory getStateFactory() {
        return new IAepApplicationStateFactory() {
            @Override
            final public Repository createState(MessageView view) {
                return Repository.create();
            }
        };
    }

    @AppInjectionPoint
    final public void setMessageSender(AepMessageSender messageSender) {
        _messageSender = messageSender;
    }

    @EventHandler
    final public void handleNewCardHolder(NewCardHolderMessage message, Repository repository) throws Exception {
        // stats
        newCardHolderRequestCount.increment();

        // add card holder
        CardHolder cardHolder = CardHolder.create();
        cardHolder.setCardHolderId(message.getCardHolderId());
        for (PaymentTransactionDTO dto : message.getHistory()) {
            PaymentTransaction transaction = PaymentTransaction.create();
            transaction.setCardNumberFrom(dto.getCardNumberUnsafe());
            transaction.setPaymentTransactionIdFrom(dto.getTransactionIdUnsafe());
            transaction.setMerchantIdFrom(dto.getMerchantIdUnsafe());
            transaction.setMerchantStoreIdFrom(dto.getMerchantStoreIdUnsafe());
            cardHolder.getHistory().add(transaction);
        }
        repository.getCardHolders().put(cardHolder.getCardHolderId(), cardHolder);
    }

    @EventHandler
    final public void onAuthorizationRequest(AuthorizationRequestMessage message, Repository repository) {
        // stats
        authorizationRequestCount.increment();
        long start = UtlTime.now();

        /****
         * This is where one would do the non-hardware accelerated fraud checks. 
         * In the code here, we simulate the fraud check by sleeping for 100 microseconds.  
         ****/
        if (!simulateSoftwareFraudCheck()) {
            FraudAnalysisRequestMessage outboundMessage = FraudAnalysisRequestMessage.create();
            outboundMessage.setRequestIdFrom(message.getRequestIdUnsafe());
            outboundMessage.setFlowStartTs(message.getFlowStartTs());
            outboundMessage.setNewTransaction(testDataGenerator.generateTransformedTransactionMessage(message.getNewTransaction()));
            outboundMessage.setCardHolderIdFrom(message.getCardHolderIdUnsafe());
            outboundMessage.setMerchantStoreCountryCodeFrom(message.getMerchantStoreCountryCodeUnsafe());
            outboundMessage.setMerchantStorePostcodeFrom(message.getMerchantStorePostcodeUnsafe());

            // Fraud Analyzer has no partitioned state. We know that in our demo it will have same number of shards for 
            // cardholdermaster and fraudanalyzer, so we will use cardholder ID to make shard key for fraud analyzer. 
            // We could choose anything to make fraudanalyzer shard key, including generating with RNG,
            // as long as it will result in spreading the message load evenly across fraud analyzer instances. 
            _messageSender.sendMessage("authreq4", outboundMessage, UtlCommon.getShardKey(message.getCardHolderId(), fraudanalyzerNumShards));
            authorizationProcessingLatencies.add(UtlTime.now() - start);
        }
        else {
            AuthorizationResponseMessage authorizationResponseMessage = AuthorizationResponseMessage.create();
            authorizationResponseMessage.setFlowStartTs(message.getFlowStartTs());
            authorizationResponseMessage.setRequestIdFrom(message.getRequestIdUnsafe());
            authorizationResponseMessage.setDecision(false);
            authorizationResponseMessage.setDecisionScore(0);
            _messageSender.sendMessage("authresp", authorizationResponseMessage);
            authorizationProcessingLatencies.add(UtlTime.now() - start);
        }
    }
}