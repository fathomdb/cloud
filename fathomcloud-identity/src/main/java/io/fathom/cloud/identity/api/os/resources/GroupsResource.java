package io.fathom.cloud.identity.api.os.resources;

import io.fathom.cloud.CloudException;
import io.fathom.cloud.identity.api.os.model.v3.Group;
import io.fathom.cloud.identity.api.os.model.v3.GroupList;
import io.fathom.cloud.identity.model.AuthenticatedUser;
import io.fathom.cloud.identity.services.IdentityService;
import io.fathom.cloud.protobuf.IdentityModel.GroupData;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;

@Path("/openstack/identity/v3/groups")
@Produces({ "application/json" })
public class GroupsResource extends IdentityResourceBase {
    private static final Logger log = LoggerFactory.getLogger(GroupsResource.class);

    @Inject
    IdentityService identityService;

    @GET
    @Produces({ JSON })
    public GroupList listGroup() throws CloudException {
        AuthenticatedUser user = getAuthenticatedUser();

        GroupList response = new GroupList();
        response.groups = Lists.newArrayList();

        for (GroupData data : identityService.listGroups(user)) {
            Group domain = toModel(data);
            response.groups.add(domain);
        }

        return response;
    }

    private Group toModel(GroupData data) {
        Group model = new Group();

        model.id = "" + data.getId();
        model.name = data.getName();

        model.description = data.getDescription();

        model.domain_id = "" + data.getDomainId();

        // if (data.hasEnabled()) {
        // model.enabled = data.getEnabled();
        // } else {
        // model.enabled = true;
        // }

        return model;
    }
}
