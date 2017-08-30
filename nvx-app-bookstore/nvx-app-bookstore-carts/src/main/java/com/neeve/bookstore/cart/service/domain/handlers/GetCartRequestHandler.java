package com.neeve.bookstore.cart.service.domain.handlers;

import com.neeve.bookstore.cart.service.messages.Book;

import com.neeve.sma.MessageView;
import com.neeve.service.MessageHandler;

import com.neeve.bookstore.cart.service.messages.GetCartRequest;
import com.neeve.bookstore.cart.service.messages.GetCartResponse;
import com.neeve.bookstore.cart.service.repository.Cart;
import com.neeve.bookstore.cart.service.repository.Item;
import com.neeve.bookstore.cart.service.repository.Repository;

final public class GetCartRequestHandler implements MessageHandler<GetCartRequest, GetCartResponse, Repository> {
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
                                    final GetCartRequest request,
                                    final GetCartResponse response,
                                    final Repository repository) throws Exception {
        final Cart cart = repository.getCarts().get(request.getCartId());
        if (cart != null) {
            final Book[] books = new Book[cart.getItems().size()];
            int i = 0;
            for (Item item : cart.getItems().values()) {
                final Book book = Book.create();
                book.setIsbn(item.getIsbn());
                book.setTitle(item.getTitle());
                books[i++] = book;
            }
            response.setCartId(cart.getCartId());
            response.setBooks(books);
            return null;
        }
        else {
            throw new IllegalArgumentException("Invalid cart id. Cart id '" + request.getCartId() + "' not found!");
        }
    }
}
