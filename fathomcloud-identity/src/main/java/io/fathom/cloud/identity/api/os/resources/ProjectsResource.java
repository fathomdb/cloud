package io.fathom.cloud.identity.api.os.resources;

import io.fathom.cloud.CloudException;
import io.fathom.cloud.PATCH;
import io.fathom.cloud.WellKnownRoles;
import io.fathom.cloud.identity.Users;
import io.fathom.cloud.identity.api.os.model.Project;
import io.fathom.cloud.identity.api.os.model.Projects;
import io.fathom.cloud.identity.api.os.model.Roles;
import io.fathom.cloud.identity.api.os.model.WrappedProject;
import io.fathom.cloud.identity.api.os.model.v2.Role;
import io.fathom.cloud.identity.model.AuthenticatedProject;
import io.fathom.cloud.identity.model.AuthenticatedUser;
import io.fathom.cloud.identity.state.AuthRepository;
import io.fathom.cloud.protobuf.IdentityModel.ProjectData;
import io.fathom.cloud.protobuf.IdentityModel.ProjectRoles;
import io.fathom.cloud.protobuf.IdentityModel.RoleData;
import io.fathom.cloud.protobuf.IdentityModel.UserData;
import io.fathom.cloud.server.auth.Auth;

import javax.inject.Inject;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.inject.persist.Transactional;

@Path("/openstack/identity/v3/projects")
@Produces({ "application/json" })
public class ProjectsResource extends IdentityResourceBase {
    private static final Logger log = LoggerFactory.getLogger(ProjectsResource.class);

    // Should use identity service instead
    @Deprecated
    @Inject
    AuthRepository authRepository;

    @GET
    @Transactional
    public Projects listProjects() throws CloudException {
        Auth.Domain domain = findDomainWithAdminRole();
        if (domain == null) {
            // TODO: Should we allow a non-admin to list their own projects??
            throw new WebApplicationException(Status.FORBIDDEN);
        }

        Projects response = new Projects();
        response.projects = Lists.newArrayList();

        for (ProjectData data : authRepository.getProjects().list()) {
            if (data.getDomainId() != domain.getId()) {
                continue;
            }
            Project user = toModel(data);
            response.projects.add(user);
        }

        return response;
    }

    @DELETE
    @Path("{id}")
    @Transactional
    public Response deleteProject(@PathParam("id") long userId) throws CloudException {
        UserData user = getUser(userId);

        if (user.getId() == getAuth().getUser().getId()) {
            // Don't let people delete themselves
            // TODO: Only protect admin account??
            throw new IllegalArgumentException();
        }

        // TODO: Mark as deleted?
        // TODO: Deleted related things e.g. credentials?
        // TODO: Block delete if "in use"
        authRepository.getProjects().delete(user.getId());

        ResponseBuilder response = Response.noContent();
        return response.build();
    }

    @PATCH
    @Path("{id}")
    @Produces({ JSON })
    @Transactional
    public WrappedProject patchProject(@PathParam("id") long projectId, WrappedProject wrappedProject)
            throws CloudException {
        ProjectData project = getProject(projectId);

        ProjectData.Builder b = ProjectData.newBuilder(project);

        Project req = wrappedProject.project;
        if (!Strings.isNullOrEmpty(req.description)) {
            b.setDescription(req.description);
        }

        if (req.enabled != null) {
            b.setEnabled(req.enabled);
        }

        ProjectData created = authRepository.getProjects().update(b);

        WrappedProject response = new WrappedProject();
        response.project = toModel(created);
        return response;
    }

    @PUT
    @Path("{project_id}/users/{user_id}/roles/{role_id}")
    public Response grantRoleToUserOnProject(@PathParam("project_id") long projectId,
            @PathParam("user_id") long userId, @PathParam("role_id") long roleId) throws CloudException {
        AuthenticatedUser currentUser = getAuthenticatedUser();
        UserData grantee = getUser(userId);

        AuthenticatedProject authenticatedProject = identityService.authenticateToProject(currentUser, projectId);
        if (authenticatedProject == null) {
            // Forbidden?
            log.info("Cannot authenticate to project: {} as user: {}", projectId, currentUser);
            throw new WebApplicationException(Status.NOT_FOUND);
        }

        identityService.grantRoleToUserOnProject(authenticatedProject, grantee.getId(), roleId);

        return Response.noContent().build();
    }

    @POST
    public WrappedProject createProject(WrappedProject wrappedProject) throws CloudException {
        AuthenticatedUser owner = getAuthenticatedUser();

        Project req = wrappedProject.project;

        ProjectData.Builder b = ProjectData.newBuilder();

        if (!Strings.isNullOrEmpty(req.description)) {
            b.setDescription(req.description);
        }

        b.setName(req.name);

        if (!Strings.isNullOrEmpty(req.domainId)) {
            // Not sure what good can come of this...
            throw new UnsupportedOperationException();
        }
        b.setDomainId(owner.getDomainId());

        if (req.enabled != null) {
            b.setEnabled(req.enabled);
        } else {
            b.setEnabled(true);
        }

        ProjectData created = identityService.createProject(b, owner, WellKnownRoles.ROLE_ID_ADMIN);

        WrappedProject response = new WrappedProject();
        response.project = toModel(created);
        return response;
    }

    @GET
    @Path("{projectId}")
    public WrappedProject getProjectDetails(@PathParam("projectId") long projectId) throws CloudException {
        ProjectData project = getProject(projectId);

        WrappedProject response = new WrappedProject();
        response.project = toModel(project);

        return response;
    }

    @GET
    @Path("{projectId}/users/{userId}/roles")
    public Roles getProjectDetails(@PathParam("projectId") long projectId, @PathParam("userId") long userId)
            throws CloudException {
        UserData user = getUser(userId);
        ProjectData project = getProject(projectId);

        Roles response = new Roles();
        response.roles = Lists.newArrayList();

        ProjectRoles projectRoles = Users.findProjectRoles(user, project.getId());
        if (projectRoles != null) {
            for (long roleId : projectRoles.getRoleList()) {
                RoleData role = identityService.findRole(roleId);
                if (role == null) {
                    log.warn("Role not found: {}", roleId);
                } else {
                    response.roles.add(toModel(role));
                }
            }
        }

        return response;
    }

    private Role toModel(RoleData data) {
        Role role = new Role();
        role.id = "" + data.getId();
        role.name = data.getName();
        return role;
    }
}
