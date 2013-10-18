package io.fathom.cloud.identity.api.os.resources;

import io.fathom.cloud.CloudException;
import io.fathom.cloud.PATCH;
import io.fathom.cloud.identity.api.os.model.Projects;
import io.fathom.cloud.identity.api.os.model.User;
import io.fathom.cloud.identity.api.os.model.Users;
import io.fathom.cloud.identity.api.os.model.WrappedUser;
import io.fathom.cloud.identity.model.AuthenticatedUser;
import io.fathom.cloud.identity.secrets.Secrets;
import io.fathom.cloud.identity.services.IdentityService.UserCreationData;
import io.fathom.cloud.identity.state.AuthRepository;
import io.fathom.cloud.protobuf.IdentityModel.DomainData;
import io.fathom.cloud.protobuf.IdentityModel.ProjectData;
import io.fathom.cloud.protobuf.IdentityModel.ProjectRoles;
import io.fathom.cloud.protobuf.IdentityModel.UserData;
import io.fathom.cloud.server.auth.Auth;
import io.fathom.cloud.state.NumberedItemCollection;

import javax.inject.Inject;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
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

@Path("/openstack/identity/v3/users")
@Transactional
@Produces({ "application/json" })
public class UsersResource extends IdentityResourceBase {
    private static final Logger log = LoggerFactory.getLogger(UsersResource.class);

    // Should use identity service instead
    @Deprecated
    @Inject
    AuthRepository authRepository;

    @GET
    @Produces({ JSON })
    public Users listUsers() throws CloudException {
        Auth.Domain domain = findDomainWithAdminRole();
        if (domain == null) {
            throw new WebApplicationException(Status.FORBIDDEN);
        }

        Users response = new Users();
        response.users = Lists.newArrayList();

        for (UserData data : authRepository.getUsers().list()) {
            if (data.getDomainId() != domain.getId()) {
                continue;
            }

            User user = toModel(data);
            response.users.add(user);
        }

        return response;
    }

    @PATCH
    @Path("{id}")
    @Produces({ JSON })
    public WrappedUser patchUser(@PathParam("id") long id, WrappedUser wrapped) throws CloudException {
        AuthenticatedUser user = getAuthenticatedUser();

        NumberedItemCollection<UserData> users = authRepository.getUsers();

        // UserData user = users.find(id);
        // if (user == null) {
        // throw new WebApplicationException(Status.NOT_FOUND);
        // }

        if (id != user.getUserId()) {
            // requireAdmin();
            // Tricky to change password
            // TODO: Support changing non-encrypted fields?
            throw new UnsupportedOperationException();
        }

        User req = wrapped.user;

        UserData updated;

        {
            UserData.Builder b = UserData.newBuilder(user.getUserData());

            if (!Strings.isNullOrEmpty(req.description)) {
                b.setDescription(req.description);
            }

            if (req.enabled != null) {
                b.setEnabled(req.enabled);
            }

            if (req.defaultProjectId != null) {
                b.setDefaultProjectId(Long.valueOf(req.defaultProjectId));
            }

            if (!Strings.isNullOrEmpty(req.password)) {
                Secrets.setPassword(b.getSecretStoreBuilder(), req.password, user.getKeys());
                // DomainData domain =
                // authRepository.getDomains().find(user.getDomainId());
                // NamedItemCollection<CredentialData> usernames =
                // authRepository.getUsernames(domain);
                //
                // CredentialData credential = usernames.find(user.getName());
                //
                // if (credential.getUserId() != user.getId()) {
                // throw new IllegalStateException();
                // }
                //
                // CredentialData.Builder b =
                // CredentialData.newBuilder(credential);
                //
                // PasswordHashData passwordHash = hasher.hash(req.password);
                // b.setPasswordHash(passwordHash);
                //
                // usernames.update(b);
            }

            updated = users.update(b);
        }

        WrappedUser response = new WrappedUser();
        response.user = toModel(updated);
        return response;
    }

    @POST
    @Produces({ JSON })
    public WrappedUser createUser(WrappedUser wrappedUser) throws CloudException {
        Auth.Domain domain = findDomainWithAdminRole();
        if (domain == null) {
            // TODO: Allow foreign domain creation?
            throw new WebApplicationException(Status.FORBIDDEN);
        }

        User req = wrappedUser.user;

        DomainData domainData = authRepository.getDomains().find(domain.getId());
        if (domainData == null) {
            throw new IllegalStateException();
        }

        if (Strings.isNullOrEmpty(req.domainId)) {
            // domain = getDomainFromToken();
        } else {
            if (Long.valueOf(req.domainId) != domainData.getId()) {
                // TODO: Allow this?
                throw new UnsupportedOperationException();
            }
            // domain = getDomain(Long.valueOf(req.domainId));
        }

        ProjectData project = getProject(Long.valueOf(req.defaultProjectId));

        UserData.Builder b = UserData.newBuilder();

        if (!Strings.isNullOrEmpty(req.description)) {
            b.setDescription(req.description);
        }

        b.setName(req.name);

        b.setDomainId(domain.getId());

        if (req.enabled != null) {
            b.setEnabled(req.enabled);
        } else {
            b.setEnabled(true);
        }

        if (project != null) {
            b.setDefaultProjectId(project.getId());
        }

        b.setEmail(req.email);

        UserData user = identityService.createUser(new UserCreationData(domainData, b, req.password));

        WrappedUser response = new WrappedUser();
        response.user = toModel(user);
        return response;
    }

    @GET
    @Path("{id}")
    @Produces({ JSON })
    public WrappedUser getUserDetails(@PathParam("id") long id) throws CloudException {
        UserData user = getUser(id);

        WrappedUser response = new WrappedUser();
        response.user = toModel(user);

        return response;
    }

    @DELETE
    @Path("{id}")
    public Response deleteUser(@PathParam("id") long id) throws Exception {
        UserData user = getUser(id);

        if (user.getId() == getUser().getId()) {
            // Prevent users from shooting themselves in the foot
            throw new IllegalArgumentException();
        }

        identityService.deleteUser(user);

        ResponseBuilder response = Response.noContent();
        return response.build();
    }

    @GET
    @Path("{id}/projects")
    @Produces({ JSON })
    public Projects getUserProjects(@PathParam("id") long id) throws CloudException {
        UserData user = getUser(id);

        Projects response = new Projects();
        response.projects = Lists.newArrayList();

        for (ProjectRoles projectRole : user.getProjectRolesList()) {
            long projectId = projectRole.getProject();

            ProjectData project = authRepository.getProjects().find(projectId);
            if (project == null) {
                log.warn("Cannot find project {}", projectId);
                continue;
            }

            response.projects.add(toModel(project));
        }

        return response;
    }

    private User toModel(UserData data) {
        User user = new User();

        user.id = "" + data.getId();
        user.name = data.getName();

        if (data.hasEmail()) {
            user.email = data.getEmail();
        } else {
            user.email = user.name;
        }

        if (data.hasDescription()) {
            user.description = data.getDescription();
        }

        if (data.hasEnabled()) {
            user.enabled = data.getEnabled();
        } else {
            user.enabled = true;
        }

        user.domainId = "" + data.getDomainId();

        if (data.hasDefaultProjectId()) {
            user.defaultProjectId = "" + data.getDefaultProjectId();
        }

        return user;
    }
}
