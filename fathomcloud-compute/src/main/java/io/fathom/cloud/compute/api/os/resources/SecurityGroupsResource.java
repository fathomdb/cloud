package io.fathom.cloud.compute.api.os.resources;

import io.fathom.cloud.CloudException;
import io.fathom.cloud.compute.api.os.model.SecurityGroup;
import io.fathom.cloud.compute.api.os.model.SecurityGroupList;
import io.fathom.cloud.compute.api.os.model.WrappedSecurityGroup;
import io.fathom.cloud.compute.services.SecurityGroups;
import io.fathom.cloud.protobuf.CloudModel.SecurityGroupData;
import io.fathom.cloud.protobuf.CloudModel.SecurityGroupRuleData;
import io.fathom.cloud.server.model.Project;

import javax.inject.Inject;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response.Status;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;

@Path("/openstack/compute/{project}/os-security-groups")
public class SecurityGroupsResource extends ComputeResourceBase {

    @Inject
    SecurityGroups securityGroups;

    @GET
    @Produces({ JSON })
    public SecurityGroupList list() throws CloudException {
        SecurityGroupList response = new SecurityGroupList();
        response.securityGroups = Lists.newArrayList();

        for (SecurityGroupData data : securityGroups.list(getProject())) {
            SecurityGroup model = toModel(data, true);
            response.securityGroups.add(model);
        }

        return response;
    }

    @GET
    @Path("{id}")
    @Produces({ JSON })
    public WrappedSecurityGroup getSecurityGroupDetails(@PathParam("id") long id) throws CloudException {
        SecurityGroupData data = securityGroups.find(getProject(), id);
        if (data == null) {
            throw new WebApplicationException(Status.NOT_FOUND);
        }

        WrappedSecurityGroup response = new WrappedSecurityGroup();
        response.securityGroup = toModel(data, true);

        return response;
    }

    @DELETE
    @Path("{id}")
    public void deleteSecurityGroup(@PathParam("id") long id) throws CloudException {
        SecurityGroupData data = securityGroups.delete(getProject(), id);
        if (data == null) {
            throw new WebApplicationException(Status.NOT_FOUND);
        }
    }

    @POST
    @Produces({ JSON })
    public WrappedSecurityGroup create(WrappedSecurityGroup request) throws CloudException {
        WrappedSecurityGroup response = new WrappedSecurityGroup();

        String name = request.securityGroup.name;
        if (Strings.isNullOrEmpty(name)) {
            throw new IllegalArgumentException();
        }

        // TODO: Technically we should lock during this
        for (SecurityGroupData item : securityGroups.list(getProject())) {
            if (name.equals(item.getName())) {
                throw new WebApplicationException(Status.CONFLICT);
            }
        }

        SecurityGroupData created;
        {
            SecurityGroup req = request.securityGroup;

            SecurityGroupData.Builder b = SecurityGroupData.newBuilder();
            b.setName(req.name);
            b.setProjectId(getProject().getId());
            b.setDescription(req.description);

            created = securityGroups.create(getProject(), b);
        }

        response.securityGroup = toModel(created, true);

        return response;
    }

    protected SecurityGroup toModel(SecurityGroupData data, boolean includeRules) throws CloudException {
        Project project = getProject();

        return toModel(project, data, includeRules);
    }

    public static SecurityGroup toModel(Project project, SecurityGroupData data, boolean includeRules)
            throws CloudException {
        SecurityGroup model = new SecurityGroup();

        model.id = data.getId();
        model.description = data.getDescription();
        model.name = data.getName();

        model.tenantId = "" + project.getId();

        if (includeRules) {
            model.rules = Lists.newArrayList();
            for (SecurityGroupRuleData ruleData : data.getRulesList()) {
                model.rules.add(SecurityGroupRulesResource.toModel(data, ruleData));
            }
        }

        return model;
    }

}
