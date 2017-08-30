package com.neeve.bookstore.cart.webservice.resources;

import javax.ws.rs.Path;

import com.neeve.ci.XRuntime;
import com.sun.jersey.spi.resource.Singleton;

import io.swagger.annotations.Api;

@Singleton
@Path("/bookstore-carts")
@Api(value = "bookstore-carts")
final public class WebApp extends AbstractWebApp {

    public WebApp() {
        super(XRuntime.getValue("bookstore.carts.webservice.port", com.neeve.webservice.AbstractApp.DEFAULT_PORT));
    }

}
