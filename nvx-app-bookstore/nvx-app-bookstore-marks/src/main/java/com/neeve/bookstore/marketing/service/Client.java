package com.neeve.bookstore.marketing.service;

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
    final protected com.neeve.bookstore.marketing.service.AbstractApp createApp() {
        return new App();
    }
}
