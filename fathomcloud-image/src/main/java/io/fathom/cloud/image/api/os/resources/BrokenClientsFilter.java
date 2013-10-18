package io.fathom.cloud.image.api.os.resources;

import java.io.IOException;
import java.net.URI;

import javax.inject.Singleton;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.Provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
@Provider
public class BrokenClientsFilter implements ContainerRequestFilter {
    private static final Logger log = LoggerFactory.getLogger(BrokenClientsFilter.class);

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        UriInfo uriInfo = requestContext.getUriInfo();

        String path = uriInfo.getPath();

        boolean redirect = false;

        if (path.contains("v1/images")) {
            if (path.startsWith("//v1/")) {
                path = path.substring(1);
            }
            if (!path.startsWith("/")) {
                path = "/" + path;
            }
            if (path.startsWith("/v1/images")) {
                // BUG: Glance client doesn't respect relative paths in Keystone

                path = "/openstack/images" + path;

                redirect = true;
            }
        }

        if (redirect) {
            URI uri = uriInfo.getRequestUri();

            URI newUri = UriBuilder.fromUri(uri).replacePath(path).build();
            log.info("Rewriting URI: {} -> {}", uri, newUri);

            requestContext.setRequestUri(newUri);
        }
    }

}
