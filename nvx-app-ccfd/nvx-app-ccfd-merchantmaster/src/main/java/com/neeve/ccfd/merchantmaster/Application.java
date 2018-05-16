package com.neeve.ccfd.merchantmaster;

import com.neeve.aep.AepEngine;
import com.neeve.aep.AepMessageSender;
import com.neeve.aep.IAepApplicationStateFactory;
import com.neeve.aep.annotations.EventHandler;
import com.neeve.ccfd.merchantmaster.state.Merchant;
import com.neeve.ccfd.merchantmaster.state.MerchantStore;
import com.neeve.ccfd.merchantmaster.state.Repository;
import com.neeve.ccfd.messages.AuthorizationRequestMessage;
import com.neeve.ccfd.messages.NewMerchantMessage;
import com.neeve.ccfd.messages.NewMerchantStoreDTO;
import com.neeve.lang.XIterator;
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
    private final Counter newMerchantRequestCount = StatsFactory.createCounterStat("New Merchant Received Count");
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
    final public void handleNewMerchant(NewMerchantMessage message, Repository repository) {
        // stats
        newMerchantRequestCount.increment();

        // add new merchant
        Merchant merchant = Merchant.create();
        merchant.setMerchantIdFrom(message.getMerchantIdUnsafe());
        merchant.setNameFrom(message.getNameUnsafe());
        merchant.setAddressFrom(message.getAddressUnsafe());
        merchant.setPostOrZipFrom(message.getPostOrZipUnsafe());
        merchant.setCountryCodeFrom(message.getCountryCodeUnsafe());
        XIterator<NewMerchantStoreDTO> newStoreIterator = message.getStoresIterator();
        while (newStoreIterator.hasNext()) {
            final NewMerchantStoreDTO newStoreDTO = newStoreIterator.next();
            MerchantStore store = MerchantStore.create();
            store.setStoreIdFrom(newStoreDTO.getStoreIdUnsafe());
            store.setNameFrom(newStoreDTO.getNameUnsafe());
            store.setAddressFrom(newStoreDTO.getAddressUnsafe());
            store.setPostOrZipFrom(newStoreDTO.getPostOrZipUnsafe());
            store.setCountryCodeFrom(newStoreDTO.getCountryCodeUnsafe());
            merchant.getStores().put(store.getStoreId(), store);
        }
        repository.getMerchants().put(merchant.getMerchantId(), merchant);
    }

    @EventHandler
    final public void handleAuthRequest(AuthorizationRequestMessage message, Repository repository) {
        // stats
        authorizationRequestCount.increment();
        long start = UtlTime.now();

        // lookup merchant and merchant store
        final String merchantId = message.getNewTransaction().getMerchantId();
        final Merchant merchant = repository.getMerchants().get(merchantId);
        final String merchantStoreId = message.getNewTransaction().getMerchantStoreId();
        final MerchantStore store = merchantStoreId != null ? merchant.getStores().get(merchantStoreId) : null;

        // send message to continue authorization with merchant resolved
        AuthorizationRequestMessage outMessage = message.copy();
        if (store != null) {
            outMessage.setMerchantStoreCountryCodeFrom(store.getCountryCodeUnsafe());
            outMessage.setMerchantStorePostcodeFrom(store.getPostOrZipUnsafe());
        }
        else {
            outMessage.setMerchantStoreCountryCodeFrom(merchant.getCountryCodeUnsafe());
            outMessage.setMerchantStorePostcodeFrom(merchant.getPostOrZipUnsafe());
        }
        _messageSender.sendMessage("authreq3", outMessage);
        authorizationProcessingLatencies.add(UtlTime.now() - start);
    }
}
