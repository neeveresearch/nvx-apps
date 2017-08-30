package com.neeve.bookstore.cart.service.domain.handlers;

import java.util.UUID;

import com.google.inject.*;

import com.neeve.sma.MessageView;
import com.neeve.service.IdentityInformationProvider;
import com.neeve.service.MessageHandler;
import com.neeve.service.MessageSender;
import com.neeve.service.messages.MessageHeader;

import com.neeve.bookstore.cart.service.domain.CartId;
import com.neeve.bookstore.cart.service.messages.CreateCartRequest;
import com.neeve.bookstore.cart.service.messages.CreateCartResponse;
import com.neeve.bookstore.cart.service.messages.CartCreatedEvent;
import com.neeve.bookstore.cart.service.repository.Repository;
import com.neeve.bookstore.cart.service.repository.Cart;

final public class CreateCartRequestHandler implements MessageHandler<CreateCartRequest, CreateCartResponse, Repository> {
    @Inject private IdentityInformationProvider _identityInformationProvider;
    @Inject private MessageSender<com.neeve.bookstore.cart.service.messages.CartCreatedEvent> _cartCreatedEventSender;

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
                                    final CreateCartRequest request, 
                                    final CreateCartResponse response, 
                                    final Repository repository) throws Exception {
        // create a new cart
        final Cart cart = Cart.create();
        cart.setCartId(CartId.create(repository, _identityInformationProvider));
        repository.getCarts().put(cart.getCartId(), cart);
        System.out.println("Created new cart (id=" + cart.getCartId() + ")");

        // populate response
        response.setCartId(cart.getCartId());

        // dispatch event
        CartCreatedEvent event = CartCreatedEvent.create();
        event.setHeader(_cartCreatedEventSender.prepareHeader(request.getHeader(), MessageHeader.create(), null));
        event.setCartId(cart.getCartId());
        _cartCreatedEventSender.send(request.getHeader(), event);

        // done
        return null;
    }
}
