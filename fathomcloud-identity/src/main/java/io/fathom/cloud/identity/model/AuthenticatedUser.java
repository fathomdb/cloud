package io.fathom.cloud.identity.model;

import io.fathom.cloud.WellKnownRoles;
import io.fathom.cloud.identity.secrets.AuthenticatedUserKeys;
import io.fathom.cloud.identity.secrets.UserWithSecret;
import io.fathom.cloud.protobuf.CloudCommons.TokenScope;
import io.fathom.cloud.protobuf.IdentityModel.DomainData;
import io.fathom.cloud.protobuf.IdentityModel.DomainRoles;
import io.fathom.cloud.protobuf.IdentityModel.ProjectData;
import io.fathom.cloud.protobuf.IdentityModel.ProjectRoles;
import io.fathom.cloud.protobuf.IdentityModel.UserData;

import java.util.Collections;
import java.util.List;

public class AuthenticatedUser {
    private final UserData userData;
    private final ProjectData project;
    private final ProjectRoles projectRoles;
    private final DomainData domain;
    private final TokenScope scope;
    final AuthenticatedUserKeys keys;

    public AuthenticatedUser(TokenScope scope, UserWithSecret user, ProjectData project, ProjectRoles projectRoles,
            DomainData domain) {
        super();
        this.scope = scope;
        this.userData = user.getUserData();
        this.project = project;
        this.projectRoles = projectRoles;
        this.domain = domain;
        this.keys = new AuthenticatedUserKeys(user);

        if ((projectRoles == null) != (project == null)) {
            throw new IllegalStateException();
        }
    }

    public UserData getUserData() {
        return userData;
    }

    // public List<RoleData> getProjectRoles() {
    // if (projectRoles == null) {
    // return null;
    // }
    //
    // List<RoleData> ret = Lists.newArrayList();
    // for (long roleId : projectRoles.getRoleList()) {
    // RoleData role = authRepository.getRoles().find(roleId);
    // if (role == null) {
    // continue;
    // }
    // ret.add(role);
    // }
    // return ret;
    // }

    public List<Long> getProjectRoleIds() {
        if (projectRoles == null) {
            return null;
        }

        return projectRoles.getRoleList();
    }

    public ProjectData getProject() {
        return project;
    }

    public long getUserId() {
        return userData.getId();
    }

    public TokenScope getScope() {
        return scope;
    }

    public long getDomainId() {
        return domain.getId();
    }

    public List<Long> getDomainRoleIds(long domainId) {
        for (DomainRoles domainRoles : userData.getDomainRolesList()) {
            if (domainRoles.getDomain() != domainId) {
                continue;
            }

            return domainRoles.getRoleList();
        }

        return Collections.emptyList();
    }

    @Override
    public String toString() {
        return "AuthenticatedUser [name=" + userData.getName() + "]";
    }

    public boolean isDomainAdmin(long domainId) {
        return getDomainRoleIds(domainId).contains(WellKnownRoles.ROLE_ID_ADMIN);
    }

    public AuthenticatedUserKeys getKeys() {
        return keys;
    }

    public boolean isInProjectRole(long projectId, long roleId) {
        if (project == null || project.getId() != projectId) {
            throw new IllegalStateException();
        }

        for (long projectRoleId : getProjectRoleIds()) {
            if (roleId == projectRoleId) {
                return true;
            }
        }
        return false;
    }

}
