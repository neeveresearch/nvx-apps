package com.neeve.ccfd.fraudanalyzer;

import com.neeve.aep.AepEngine;
import com.neeve.aep.AepMessageSender;
import com.neeve.aep.IAepApplicationStateFactory;
import com.neeve.aep.annotations.EventHandler;
import com.neeve.ccfd.fraudanalyzer.state.Repository;
import com.neeve.ccfd.messages.AuthorizationApprovedMessage;
import com.neeve.ccfd.messages.AuthorizationDeclinedMessage;
import com.neeve.ccfd.messages.FraudAnalysisRequestMessage;
import com.neeve.ccfd.messages.PaymentTransactionDTO;
import com.neeve.ccfd.messages.TransformedPaymentTransactionDTO;
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
    private AepMessageSender _messageSender;
    @AppStat
    private final Counter authorizationRequestCount = StatsFactory.createCounterStat("AuthorizationRequest Received Count");
    @AppStat
    private final Latencies authorizationProcessingLatencies = StatsFactory.createLatencyStat("Authorization Processing Time");

    private boolean simulateHardwareAcceleratedFraudCheck() {
        long ts = System.nanoTime();
        while ((System.nanoTime() - ts) < 100000)
            ;
        return true;
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
    final public void handleFraudAnalysisRequest(FraudAnalysisRequestMessage message, Repository repository) {
        // stats
        authorizationRequestCount.increment();
        long start = UtlTime.now();

        /****
         * This is where one would do the test with the statistical model accelerated by hardware. 
         * In the code here, we simulate the fraud check by sleeping for 100 microseconds.  
         ****/
        if (!simulateHardwareAcceleratedFraudCheck()) {
            final PaymentTransactionDTO transactionDetails = PaymentTransactionDTO.create();
            final TransformedPaymentTransactionDTO sourceDetails = message.getNewTransaction();
            transactionDetails.setCardNumberFrom(sourceDetails.getCardNumberUnsafe());
            transactionDetails.setMerchantIdFrom(sourceDetails.getMerchantIdUnsafe());
            transactionDetails.setMerchantStoreIdFrom(sourceDetails.getMerchantStoreIdUnsafe());
            transactionDetails.setTransactionIdFrom(sourceDetails.getTransactionIdUnsafe());
            transactionDetails.setAmount(sourceDetails.getAmount());
            transactionDetails.setCardTransactionSequenceNumber(sourceDetails.getCardTransactionSequenceNumber());

            AuthorizationDeclinedMessage authorizationResponseMessage = AuthorizationDeclinedMessage.create();
            authorizationResponseMessage.setFlowStartTs(message.getFlowStartTs());
            authorizationResponseMessage.setRequestIdFrom(message.getRequestIdUnsafe());
            authorizationResponseMessage.setDecisionScore(0);
            authorizationResponseMessage.setCardHolderIdFrom(message.getCardHolderIdUnsafe());
            authorizationResponseMessage.setCardNumberFrom(transactionDetails.getCardNumberUnsafe());
            authorizationResponseMessage.setNewTransaction(transactionDetails);
            _messageSender.sendMessage("authresp", authorizationResponseMessage);
        }
        else {

            final PaymentTransactionDTO transactionDetails = PaymentTransactionDTO.create();
            final TransformedPaymentTransactionDTO sourceDetails = message.getNewTransaction();
            transactionDetails.setCardNumberFrom(sourceDetails.getCardNumberUnsafe());
            transactionDetails.setMerchantIdFrom(sourceDetails.getMerchantIdUnsafe());
            transactionDetails.setMerchantStoreIdFrom(sourceDetails.getMerchantStoreIdUnsafe());
            transactionDetails.setTransactionIdFrom(sourceDetails.getTransactionIdUnsafe());
            transactionDetails.setAmount(sourceDetails.getAmount());
            transactionDetails.setCardTransactionSequenceNumber(sourceDetails.getCardTransactionSequenceNumber());

            AuthorizationApprovedMessage authorizationResponseMessage = AuthorizationApprovedMessage.create();
            authorizationResponseMessage.setFlowStartTs(message.getFlowStartTs());
            authorizationResponseMessage.setRequestIdFrom(message.getRequestIdUnsafe());
            authorizationResponseMessage.setDecisionScore(0);
            authorizationResponseMessage.setCardHolderIdFrom(message.getCardHolderIdUnsafe());
            authorizationResponseMessage.setCardNumberFrom(transactionDetails.getCardNumberUnsafe());
            authorizationResponseMessage.setNewTransaction(transactionDetails);
            _messageSender.sendMessage("authresp", authorizationResponseMessage);
        }

        authorizationProcessingLatencies.add(UtlTime.now() - start);
    }
}
