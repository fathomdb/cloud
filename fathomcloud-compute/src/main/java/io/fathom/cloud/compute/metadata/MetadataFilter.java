package io.fathom.cloud.compute.metadata;

import java.io.IOException;
import java.net.URI;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.ext.Provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fathomdb.Configuration;

@Singleton
@Provider
public class MetadataFilter implements ContainerRequestFilter {
    private static final Logger log = LoggerFactory.getLogger(MetadataFilter.class);

    final int port;

    @Inject
    public MetadataFilter(Configuration configuration) {
        this.port = configuration.lookup("metadata.port", 8775);
    }

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        URI uri = requestContext.getUriInfo().getRequestUri();

        // TODO: Cope with fd00:0:0.... etc?
        // TODO: Cope with no square brackets?
        if (uri.getHost().equals("169.254.169.254") || uri.getHost().equals("[fd00::feed]")) {
            String path = uri.getPath();

            path = "/openstack/metadata" + path;
            URI newUri = UriBuilder.fromUri(uri).replacePath(path).build();
            log.info("Rewriting URI: {} -> {}", uri, newUri);
            requestContext.setRequestUri(newUri);
        }
    }

}
