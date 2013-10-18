package io.fathom.cloud.identity.state;

import io.fathom.cloud.protobuf.IdentityModel.AttachmentData;
import io.fathom.cloud.protobuf.IdentityModel.ClientAppData;
import io.fathom.cloud.protobuf.IdentityModel.CredentialData;
import io.fathom.cloud.protobuf.IdentityModel.DomainData;
import io.fathom.cloud.protobuf.IdentityModel.GroupData;
import io.fathom.cloud.protobuf.IdentityModel.ProjectData;
import io.fathom.cloud.protobuf.IdentityModel.UserData;
import io.fathom.cloud.state.NamedItemCollection;
import io.fathom.cloud.state.NumberedItemCollection;
import io.fathom.cloud.state.RepositoryBase;
import io.fathom.cloud.state.StateStore.StateNode;

import javax.inject.Singleton;

@Singleton
public class AuthRepository extends RepositoryBase {

    public static final String ACCESS_KEY_ID = "K";
    public static final String USERNAME = "U";

    // public CredentialInfo findCredential(String type, String key)
    // throws CloudException {
    // StateNode root = stateStore.getRoot("credentials");
    // StateNode node = root.child(type + "_" + key);
    //
    // try {
    // return node.deserialize(CredentialInfo.newBuilder());
    // } catch (IOException e) {
    // throw new IllegalStateException("Error reading credential: " + key,
    // e);
    // }
    // }

    // public UserData putUser(UserData.Builder user) throws CloudException {
    // StateNode root = stateStore.getRoot("users");
    //
    // return (UserData) putItem(root, user, UserData.getDescriptor()
    // .findFieldByNumber(UserData.ID_FIELD_NUMBER));
    // }

    // public CredentialInfo createCredential(String type,
    // CredentialInfo.Builder builder) throws CloudException {
    // StateNode parent = stateStore.getRoot("credentials");
    // StateNode node = parent.child(type + "_" + builder.getKey());
    //
    // CredentialInfo built = builder.build();
    //
    // ByteString data = built.toByteString();
    //
    // if (!node.create(data)) {
    // throw new IllegalStateException();
    // }
    //
    // return built;
    // }

    // public UserData addUserToProject(UserData user, ProjectData project)
    // throws CloudException {
    // UserData.Builder userBuilder = UserData.newBuilder(user);
    // List<Long> projectIds = userBuilder.getProjectIdsList();
    // long projectId = project.getId();
    // if (projectIds.contains(projectId)) {
    // return user;
    // }
    //
    // userBuilder.addProjectIds(projectId);
    //
    // return putUser(userBuilder);
    // }

    public NumberedItemCollection<ProjectData> getProjects() {
        StateNode root = stateStore.getRoot("projects");

        return new NumberedItemCollection<ProjectData>(root, ProjectData.newBuilder(), ProjectData.getDescriptor()
                .findFieldByNumber(ProjectData.ID_FIELD_NUMBER));
    }

    public NumberedItemCollection<GroupData> getGroups(long domainId) {
        StateNode groups = stateStore.getRoot("groups");
        StateNode domain = groups.child(Long.toHexString(domainId));

        return new NumberedItemCollection<GroupData>(domain, GroupData.newBuilder(), ProjectData.getDescriptor()
                .findFieldByNumber(GroupData.ID_FIELD_NUMBER));
    }

    public NumberedItemCollection<UserData> getUsers() {
        StateNode node = stateStore.getRoot("users");

        return new NumberedItemCollection<UserData>(node, UserData.newBuilder(), UserData.getDescriptor()
                .findFieldByNumber(UserData.ID_FIELD_NUMBER));
    }

    public NamedItemCollection<CredentialData> getUsernames(DomainData domain) {
        if (domain == null) {
            throw new IllegalArgumentException();
        }

        StateNode root = stateStore.getRoot("credentials");
        StateNode domainNode = root.child(Long.toHexString(domain.getId()));

        return new NamedItemCollection<CredentialData>(domainNode, CredentialData.newBuilder(), CredentialData
                .getDescriptor().findFieldByNumber(CredentialData.KEY_FIELD_NUMBER));
    }

    public DomainStore getDomains() {
        return new DomainStore();
    }

    public RoleStore getRoles() {
        return new RoleStore();
    }

    public NamedItemCollection<CredentialData> getEc2Credentials() {
        StateNode root = stateStore.getRoot("ec2cred");

        return new NamedItemCollection<CredentialData>(root, CredentialData.newBuilder(), CredentialData
                .getDescriptor().findFieldByNumber(CredentialData.KEY_FIELD_NUMBER));
    }

    public NamedItemCollection<AttachmentData> getUserAttachments(long userId) {
        StateNode root = stateStore.getRoot("attach-user");
        StateNode child = root.child(Long.toHexString(userId));
        return new NamedItemCollection<AttachmentData>(child, AttachmentData.newBuilder(), AttachmentData
                .getDescriptor().findFieldByNumber(AttachmentData.KEY_FIELD_NUMBER));
    }

    public NamedItemCollection<AttachmentData> getProjectAttachments(long projectId) {
        StateNode root = stateStore.getRoot("attach-project");
        StateNode child = root.child(Long.toHexString(projectId));
        return new NamedItemCollection<AttachmentData>(child, AttachmentData.newBuilder(), AttachmentData
                .getDescriptor().findFieldByNumber(AttachmentData.KEY_FIELD_NUMBER));
    }

    public NamedItemCollection<ClientAppData> getClientApps(long projectId) {
        StateNode root = stateStore.getRoot("clientapps");
        StateNode projectNode = root.child(Long.toHexString(projectId));

        return new NamedItemCollection<ClientAppData>(projectNode, ClientAppData.newBuilder(), ClientAppData
                .getDescriptor().findFieldByNumber(ClientAppData.KEY_FIELD_NUMBER));
    }

    public NamedItemCollection<CredentialData> getPublicKeyCredentials(long domainId) {
        StateNode root = stateStore.getRoot("publickeys");
        StateNode domain = root.child(Long.toHexString(domainId));

        return NamedItemCollection.builder(domain, CredentialData.class).idField(CredentialData.KEY_FIELD_NUMBER)
                .create();
    }

}
