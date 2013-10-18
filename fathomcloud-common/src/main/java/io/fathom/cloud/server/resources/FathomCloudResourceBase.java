package io.fathom.cloud.server.resources;

import io.fathom.cloud.server.auth.Auth;
import io.fathom.cloud.server.auth.AuthProvider;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response.Status;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fathomdb.Configuration;

public abstract class FathomCloudResourceBase {
    private static final Logger log = LoggerFactory.getLogger(FathomCloudResourceBase.class);

    public static final String JSON = javax.ws.rs.core.MediaType.APPLICATION_JSON;
    public static final String XML = javax.ws.rs.core.MediaType.APPLICATION_XML;
    public static final String TEXT_PLAIN = javax.ws.rs.core.MediaType.TEXT_PLAIN;

    @Context
    protected HttpServletRequest httpRequest;

    @Inject
    Configuration configuration;

    @Inject
    AuthProvider authProvider;

    protected void warnStub() {
        log.warn("Stub implementation in " + getClass().getSimpleName());
    }

    private String baseUrl;

    protected String getBaseUrl() {
        if (baseUrl == null) {
            baseUrl = Urls.getRequestUrl(httpRequest);
        }
        return baseUrl;
    }

    protected Auth getAuth() {
        Auth auth = authProvider.get();
        if (auth == null) {
            throw new WebApplicationException(Status.UNAUTHORIZED);
        }
        return auth;
    }

    protected void notFoundIfNull(Object o) {
        if (o == null) {
            throw new WebApplicationException(Status.NOT_FOUND);
        }
    }

}
