package com.neeve.bookstore.cart.service.domain.handlers;

import com.google.inject.*;

import com.neeve.sma.MessageView;
import com.neeve.service.MessageHandler;
import com.neeve.service.MessageSender;

import com.neeve.bookstore.cart.service.messages.AddBookRequest;
import com.neeve.bookstore.cart.service.messages.AddBookResponse;
import com.neeve.bookstore.cart.service.messages.BookAddedToCartEvent;
import com.neeve.bookstore.cart.service.repository.Cart;
import com.neeve.bookstore.cart.service.repository.Item;
import com.neeve.bookstore.cart.service.repository.Repository;
import com.neeve.service.messages.MessageHeader;

final public class AddBookRequestHandler implements MessageHandler<AddBookRequest, AddBookResponse, Repository> {
    @Inject private MessageSender<BookAddedToCartEvent> _bookAddedToCartEventSender;

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
                                    final AddBookRequest request,
                                    final AddBookResponse response,
                                    final Repository repository) throws Exception {
        final Cart cart = repository.getCarts().get(request.getCartId());
        if (cart != null) {
            final Item item = Item.create();
            item.setCartId(request.getCartId());
            item.setTitle(request.getTitle());
            item.setIsbn(String.valueOf(System.currentTimeMillis()));

            cart.getItems().put(item.getIsbn(), item);

            // dispatch event
            final BookAddedToCartEvent event = BookAddedToCartEvent.create();
            event.setHeader(_bookAddedToCartEventSender.prepareHeader(request.getHeader(), MessageHeader.create(), null));
            event.setCartId(cart.getCartId());
            event.setIsbn(item.getIsbn());
            _bookAddedToCartEventSender.send(request.getHeader(), event);

            response.setIsbn(item.getIsbn());

            // done
            return null;
        }
        else {
            throw new IllegalArgumentException("Invalid cart id. Cart id '" + request.getCartId() + "' not found!");
        }
    }
}
