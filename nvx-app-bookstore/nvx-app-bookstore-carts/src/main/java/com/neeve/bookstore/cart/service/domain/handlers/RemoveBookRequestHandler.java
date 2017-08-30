package com.neeve.bookstore.cart.service.domain.handlers;

import com.google.inject.*;
import com.neeve.bookstore.cart.service.messages.BookRemovedFromCartEvent;

import com.neeve.sma.MessageView;
import com.neeve.service.MessageHandler;
import com.neeve.service.MessageSender;

import com.neeve.bookstore.cart.service.messages.RemoveBookRequest;
import com.neeve.bookstore.cart.service.messages.RemoveBookResponse;
import com.neeve.bookstore.cart.service.repository.Cart;
import com.neeve.bookstore.cart.service.repository.Item;
import com.neeve.bookstore.cart.service.repository.Repository;
import com.neeve.service.EServiceException;
import com.neeve.service.entities.ErrorCode;
import com.neeve.service.entities.ErrorType;
import com.neeve.service.messages.MessageHeader;

final public class RemoveBookRequestHandler implements MessageHandler<RemoveBookRequest, RemoveBookResponse, Repository> {
    @Inject private MessageSender<BookRemovedFromCartEvent> _bookRemovedFromCartEventSender;

    final private void validate(final RemoveBookRequest request) {
        if (request.getCartId() <= 0) {
            throw new EServiceException(ErrorType.Functional,
                                        ErrorCode.MalformedRequestDTO,
                                        "cart id is required",
                                        null);
        }
        if (request.getIsbn() == null) {
            throw new EServiceException(ErrorType.Functional,
                                        ErrorCode.MalformedRequestDTO,
                                        "book isbn is required",
                                        null);
        }
    }

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
                                    final RemoveBookRequest request,
                                    final RemoveBookResponse response,
                                    final Repository repository) throws Exception {
        validate(request);

        final Cart cart = repository.getCarts().get(request.getCartId());
        if (cart != null) {
            final Item item = cart.getItems().get(request.getIsbn());
            if (item != null) {
                cart.getItems().remove(request.getIsbn());

                // dispatch event
                final BookRemovedFromCartEvent event = BookRemovedFromCartEvent.create();
                event.setHeader(_bookRemovedFromCartEventSender.prepareHeader(request.getHeader(), MessageHeader.create(), null));
                event.setCartId(cart.getCartId());
                event.setIsbn(item.getIsbn());
                _bookRemovedFromCartEventSender.send(request.getHeader(), event);

                // done
                return null;
            }
            else {
                throw new IllegalArgumentException("Invalid book isbn. Isbn '" + request.getIsbn() + "' not found in cart!");
            }
        }
        else {
            throw new IllegalArgumentException("Invalid cart id. Cart id '" + request.getCartId() + "' not found!");
        }
    }
}
