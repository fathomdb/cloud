package io.fathom.cloud.identity.api.os.resources;

import io.fathom.cloud.CloudException;
import io.fathom.cloud.identity.api.os.model.ApiVersion;
import io.fathom.cloud.identity.api.os.model.ApiVersions;

import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;

@Path("/openstack/identity")
@Produces({ "application/json" })
public class IdentityVersionsResource extends IdentityResourceBase {
    private static final Logger log = LoggerFactory.getLogger(IdentityVersionsResource.class);

    @GET
    @Produces({ JSON })
    public ApiVersions listVersions() throws CloudException {
        ApiVersions response = new ApiVersions();
        response.versions = new ApiVersions.VersionList();
        List<ApiVersion> list = response.versions.versions = Lists.newArrayList();

        {
            ApiVersion v = new ApiVersion();
            v.id = "v2.0";
            v.status = "CURRENT";
            v.updated = "2012-01-19T22:30:00.25Z";

            list.add(v);
        }

        // {"version":[{"id":"v1.0","status":"DEPRECATED","updated":"2011-07-19T22:30:00Z","link":{"href":"https://identity.api.rackspacecloud.com/v1.0","rel":"self"}},
        // {"id":"v1.1","status":"CURRENT","updated":"2012-01-19T22:30:00.25Z","link":{"href":"https://identity.api.rackspacecloud.com/v1.1/","rel":"self"},"link":{"href":"http://docs.rackspacecloud.com/auth/api/v1.1/auth-client-devguide-latest.pdf","rel":"describedby","type":"application/pdf"},"link":{"href":"http://docs.rackspacecloud.com/auth/api/v1.1/auth.wadl","rel":"describedby","type":"application/vnd.sun.wadl+xml"}},
        // {"id":"v2.0","status":"CURRENT","updated":"2012-01-19T22:30:00.25Z","link":{"href":"https://identity.api.rackspacecloud.com/v2.0/","rel":"self"},"link":{"href":"http://docs.rackspacecloud.com/auth/api/v2.0/auth-client-devguide-latest.pdf","rel":"describedby","type":"application/pdf"},"link":{"href":"http://docs.rackspacecloud.com/auth/api/v2.0/auth.wadl","rel":"describedby","type":"application/vnd.sun.wadl+xml"}}]}}

        return response;

    }

}
