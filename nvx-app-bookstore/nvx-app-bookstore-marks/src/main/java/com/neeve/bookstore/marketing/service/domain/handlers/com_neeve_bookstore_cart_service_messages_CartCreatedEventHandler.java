package com.neeve.bookstore.marketing.service.domain.handlers;

import com.neeve.sma.MessageView;
import com.neeve.service.MessageHandler;

import com.neeve.bookstore.cart.service.messages.CartCreatedEvent;
import com.neeve.service.messages.NullMessage;
import com.neeve.bookstore.marketing.service.repository.Repository;

final public class com_neeve_bookstore_cart_service_messages_CartCreatedEventHandler implements MessageHandler<CartCreatedEvent, NullMessage, Repository> {
    /**
     * Implementation of {@link MessageHandler#getType}
     */
    final public Type getType() {
        return Type.Local;
    }

    /**
     * Implementation of {@link MessageHandler#handle}
     */
    final public MessageView handle(final String origin,
                                    final CartCreatedEvent request,
                                    final NullMessage response,
                                    final Repository repository) throws Exception {
        System.out.println("Received new cart created event (cart id=" + request.getCartId() + ")");
        return null;
    }
}
