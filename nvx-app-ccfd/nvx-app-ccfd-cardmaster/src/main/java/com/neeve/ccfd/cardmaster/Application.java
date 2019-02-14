package com.neeve.ccfd.cardmaster;

import com.neeve.aep.AepEngine;
import com.neeve.aep.AepMessageSender;
import com.neeve.aep.IAepApplicationStateFactory;
import com.neeve.aep.annotations.EventHandler;
import com.neeve.ccfd.cardmaster.state.PaymentCard;
import com.neeve.ccfd.cardmaster.state.Repository;
import com.neeve.ccfd.messages.AuthorizationDeclinedMessage;
import com.neeve.ccfd.messages.AuthorizationRequestMessage;
import com.neeve.ccfd.messages.NewCardMessage;
import com.neeve.lang.XString;
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

    @AppStat
    private final Counter authorizationRequestCount = StatsFactory.createCounterStat("Authorization Request Received Count");
    @AppStat
    private final Counter newCardRequestCount = StatsFactory.createCounterStat("New Card Received Count");
    @AppStat
    private final Latencies authorizationProcessingLatencies = StatsFactory.createLatencyStat("Authorization Processing Time");

    private AepMessageSender _messageSender;

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
    final public void handleNewCard(NewCardMessage message, Repository repository) {
        // stats
        newCardRequestCount.increment();

        // add card
        PaymentCard card = PaymentCard.create();
        card.setCardHolderIdFrom(message.getCardHolderIdUnsafe());
        XString cardNumer = message.getCardNumberUnsafe();
        card.setCardNumberFrom(cardNumer);
        // TODO will this retain reference to value?
        repository.getCards().put(cardNumer.getValue(), card);
    }

    @EventHandler
    final public void handleAuthRequest(AuthorizationRequestMessage message, Repository repository) {
        // stats
        authorizationRequestCount.increment();
        long start = UtlTime.now();

        // lookup cardholder
        final PaymentCard card = repository.getCards().get(message.getNewTransaction().getCardNumber());
        if (card == null) {
            AuthorizationDeclinedMessage rejection = AuthorizationDeclinedMessage.create();
            rejection.setFlowStartTs(message.getFlowStartTs());
            rejection.setRequestIdFrom(message.getRequestIdUnsafe());
            rejection.setDecisionScore(0);
            rejection.setCardHolderIdFrom(message.getCardHolderIdUnsafe());
            rejection.setNewTransaction(message.getNewTransaction().copy());
            _messageSender.sendMessage("authresp", rejection);
            return;
        }

        // validate that a card holder id is associated with the card. 
        if (!card.hasCardHolderId()) {
            throw new IllegalStateException("Card '" + card.getCardNumber() + "' has no associated card holder id!");
        }

        // send message to continue authorization with card holder resolved
        final AuthorizationRequestMessage outMessage = message.copy();
        outMessage.setCardHolderIdFrom(card.getCardHolderIdUnsafe());
        _messageSender.sendMessage("authreq2", outMessage);

        authorizationProcessingLatencies.add(UtlTime.now() - start);
    }
}
