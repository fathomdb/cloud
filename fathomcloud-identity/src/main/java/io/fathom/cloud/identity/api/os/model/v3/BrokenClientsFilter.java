package io.fathom.cloud.identity.api.os.model.v3;

import java.io.IOException;
import java.net.URI;
import java.util.Set;

import javax.inject.Singleton;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.Provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Sets;

@Singleton
@Provider
public class BrokenClientsFilter implements ContainerRequestFilter {
    private static final Logger log = LoggerFactory.getLogger(BrokenClientsFilter.class);

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        boolean redirect = false;

        Set<String> BROKEN_USER_AGENTS = Sets.newHashSet();
        BROKEN_USER_AGENTS.add("python-keystoneclient");

        String userAgent = requestContext.getHeaderString("User-Agent");
        if (userAgent == null || !BROKEN_USER_AGENTS.contains(userAgent)) {
            return;
        }

        UriInfo uriInfo = requestContext.getUriInfo();
        String path = uriInfo.getPath();

        if (path.contains("v2.0/") || path.contains("v3/")) {
            // if (path.startsWith("//v2/") || path.startsWith("//v3/")) {
            // path = path.substring(1);
            // }
            if (!path.startsWith("/")) {
                path = "/" + path;
            }

            if (path.startsWith("/v2.0/") || path.startsWith("/v3/")) {
                path = "/openstack/identity" + path;

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
