package com.neeve.bookstore.cart.service;

import com.neeve.bookstore.cart.service.domain.CartId;
import com.neeve.service.EServiceException;
import com.neeve.service.entities.ErrorCode;
import com.neeve.service.entities.ErrorType;

final public class Client extends AbstractClient {
    // ---- Constructors
    public Client() {
        this(null, null);
    }

    public Client(final String name) {
        this(name, null);
    }

    public Client(final String name, final Object handlers) {
        super(name, App.APP_MAJOR_VERSION, App.APP_MINOR_VERSION, handlers);
    }
    // ---- Constructors

    // ---- Overriden implementation of {@link AbstractClient#createApp}
    @Override
    final protected com.neeve.bookstore.cart.service.AbstractApp createApp() {
        return new App();
    }

    @Override
    public int resolvePartitionForAddBook(final com.neeve.bookstore.cart.service.messages.AddBookRequest request) {
        if (request.getCartId() <= 0) {
            throw new EServiceException(ErrorType.Functional, ErrorCode.MalformedRequestDTO, "cart id is required", null);
        }
        return CartId.getPartition(request.getCartId());
    }

    @Override
    public int resolvePartitionForRemoveBook(final com.neeve.bookstore.cart.service.messages.RemoveBookRequest request) {
        if (request.getCartId() <= 0) {
            throw new EServiceException(ErrorType.Functional, ErrorCode.MalformedRequestDTO, "cart id is required", null);
        }
        return CartId.getPartition(request.getCartId());
    }

    @Override
    public int resolvePartitionForGetCart(final com.neeve.bookstore.cart.service.messages.GetCartRequest request) {
        if (request.getCartId() <= 0) {
            throw new EServiceException(ErrorType.Functional, ErrorCode.MalformedRequestDTO, "cart id is required", null);
        }
        return CartId.getPartition(request.getCartId());
    }
}
