package io.fathom.cloud.identity.services;

import io.fathom.cloud.CloudException;
import io.fathom.cloud.identity.model.AuthenticatedProject;
import io.fathom.cloud.identity.model.AuthenticatedUser;
import io.fathom.cloud.lifecycle.LifecycleListener;
import io.fathom.cloud.protobuf.IdentityModel.DomainData;
import io.fathom.cloud.protobuf.IdentityModel.GroupData;
import io.fathom.cloud.protobuf.IdentityModel.ProjectData;
import io.fathom.cloud.protobuf.IdentityModel.RoleData;
import io.fathom.cloud.protobuf.IdentityModel.UserData;

import java.util.List;

import com.google.inject.ImplementedBy;
import com.google.protobuf.ByteString;

@ImplementedBy(IdentityServiceImpl.class)
public interface IdentityService extends LifecycleListener {

    public static class UserCreationData {
        public DomainData domain;
        public UserData.Builder user;
        public String password;

        public ByteString publicKeySha1;
        public ByteString publicKeyChallengeRequest;
        public ByteString publicKeyChallengeResponse;

        public UserCreationData(DomainData domain, UserData.Builder user, String password) {
            this.domain = domain;
            this.user = user;
            this.password = password;
        }
    }

    UserData createUser(UserCreationData data) throws CloudException;

    DomainData getDefaultDomain() throws CloudException;

    // @AutoRetry(ConcurrentUpdateException.class)
    // void deleteUser(long id) throws StateStoreException,
    // ConcurrentUpdateException;

    void grantRoleToUserOnProject(AuthenticatedProject authenticatedProject, long granteeUserId, long roleId)
            throws CloudException;

    ProjectData createProject(ProjectData.Builder b, AuthenticatedUser owner, long ownerRoleId) throws CloudException;

    ProjectData findProject(AuthenticatedUser user, long projectId) throws CloudException;

    void deleteUser(UserData user) throws CloudException;

    void sweep() throws CloudException;

    RoleData findRole(long roleId);

    AuthenticatedProject authenticateToProject(AuthenticatedUser user, long projectId) throws CloudException;

    UserData findUser(long id) throws CloudException;

    List<DomainData> listDomains(UserData user) throws CloudException;

    List<GroupData> listGroups(AuthenticatedUser user) throws CloudException;

    List<RoleData> listRoles() throws CloudException;

    void grantDomainRoleToUser(long domainId, long granteeUserId, long roleId) throws CloudException;

    UserData findUserByName(long domainId, String userName) throws CloudException;

    void fixupProject(AuthenticatedUser user, long projectId) throws CloudException;

    List<ProjectData> listProjects() throws CloudException;

}
