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
import com.neeve.ci.XRuntime;
import com.neeve.server.app.annotations.AppFinalizer;
import com.neeve.server.app.annotations.AppHAPolicy;
import com.neeve.server.app.annotations.AppInitializer;
import com.neeve.server.app.annotations.AppInjectionPoint;
import com.neeve.server.app.annotations.AppStat;
import com.neeve.server.app.annotations.AppStateFactoryAccessor;
import com.neeve.sma.MessageView;
import com.neeve.stats.IStats.Counter;
import com.neeve.stats.IStats.Latencies;
import com.neeve.stats.StatsFactory;
import com.neeve.trace.Tracer;
import com.neeve.trace.Tracer.Level;
import com.neeve.util.UtlThrowable;
import com.neeve.util.UtlTime;

@AppHAPolicy(value = AepEngine.HAPolicy.StateReplication)
public class Application {
    private static final Tracer tracer = Tracer.get("ccfd.fraudanalyzer");
    private AepMessageSender _messageSender;
    @AppStat
    private final Counter authorizationRequestCount = StatsFactory.createCounterStat("AuthorizationRequest Received Count");
    @AppStat
    private final Latencies authorizationProcessingLatencies = StatsFactory.createLatencyStat("Authorization Processing Time");

    FraudAnalyzer fraudAnalyzer;

    @AppStateFactoryAccessor
    final public IAepApplicationStateFactory getStateFactory() {
        return new IAepApplicationStateFactory() {
            @Override
            final public Repository createState(MessageView view) {
                return Repository.create();
            }
        };
    }

    @AppInitializer
    final public void init() throws Exception {
        boolean useTensorFlow = XRuntime.getValue("ccfd.useTensorFlow", false);
        if (useTensorFlow) {
            try {
                fraudAnalyzer = new TensorFlowFraudAnalyzer();
                fraudAnalyzer.open();
            }
            catch (Throwable t) {
                useTensorFlow = false;
                tracer.log("Error loading TensorFlow analyzer: " + UtlThrowable.prepareStackTrace(t), Level.WARNING);
                tracer.log("... falling back to Mock Analyzer: " + UtlThrowable.prepareStackTrace(t), Level.WARNING);
                fraudAnalyzer = new MockFraudAnalyzer();
                fraudAnalyzer.open();
            }
        }
        else {
            fraudAnalyzer = new MockFraudAnalyzer();
            fraudAnalyzer.open();
        }

        tracer.log("Tensor Flow fraud analyzer is..." + (useTensorFlow ? "ENABLED" : "DISABLED"), Level.CONFIG);
    }

    @AppFinalizer
    final public void close() throws Exception {
        fraudAnalyzer.close();
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

        if (fraudAnalyzer.isFraudulent(message)) {
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
