package com.neeve.bookstore.cart.webservice.resources;

import javax.ws.rs.Path;
import io.swagger.annotations.*;

import com.sun.jersey.spi.resource.Singleton;

import com.neeve.ci.XRuntime;
import com.neeve.util.UtlProps;

import com.neeve.bookstore.cart.webservice.resources.AbstractWebApp;

@Singleton
@Path("/bookstore-carts")
@Api(value="bookstore-carts")
final public class WebApp extends AbstractWebApp {

    public WebApp() {
        super((int)UtlProps.getValue(XRuntime.getProps(), "bookstore.carts.webservice.port", com.neeve.webservice.AbstractApp.DEFAULT_PORT));
    }

}
