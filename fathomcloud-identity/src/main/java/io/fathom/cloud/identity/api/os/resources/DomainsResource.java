package io.fathom.cloud.identity.api.os.resources;

import io.fathom.cloud.CloudException;
import io.fathom.cloud.identity.api.os.model.v3.Domain;
import io.fathom.cloud.identity.api.os.model.v3.DomainList;
import io.fathom.cloud.identity.api.os.model.v3.DomainWrapper;
import io.fathom.cloud.identity.services.IdentityService;
import io.fathom.cloud.protobuf.IdentityModel.DomainData;
import io.fathom.cloud.protobuf.IdentityModel.UserData;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;

@Path("/openstack/identity/v3/domains")
@Produces({ "application/json" })
public class DomainsResource extends IdentityResourceBase {
    private static final Logger log = LoggerFactory.getLogger(DomainsResource.class);

    @Inject
    IdentityService identityService;

    @GET
    public DomainList listDomains() throws CloudException {
        UserData user = getUser();

        DomainList response = new DomainList();
        response.domains = Lists.newArrayList();

        for (DomainData data : identityService.listDomains(user)) {
            Domain domain = toModel(data);
            response.domains.add(domain);
        }

        return response;
    }

    @GET
    @Path("{id}")
    public DomainWrapper getDomain(@PathParam("id") String id) throws CloudException {
        UserData user = getUser();

        DomainData data = identityService.findDomain(user, id);
        notFoundIfNull(data);

        DomainWrapper response = new DomainWrapper();
        response.domain = toModel(data);
        return response;
    }

    private Domain toModel(DomainData data) {
        Domain model = new Domain();

        model.id = "" + data.getId();
        model.name = data.getName();

        model.description = data.getDescription();

        if (data.hasEnabled()) {
            model.enabled = data.getEnabled();
        } else {
            model.enabled = true;
        }

        return model;
    }
}
