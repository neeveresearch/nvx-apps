package com.neeve.bookstore.marketing.webservice.resources;

import javax.ws.rs.Path;

import com.neeve.ci.XRuntime;
import com.sun.jersey.spi.resource.Singleton;

import io.swagger.annotations.Api;

@Singleton
@Path("/bookstore-marks")
@Api(value = "bookstore-marks")
final public class WebApp extends AbstractWebApp {

    public WebApp() {
        super(XRuntime.getValue("bookstore.marks.webservice.port", com.neeve.webservice.AbstractApp.DEFAULT_PORT));
    }

}