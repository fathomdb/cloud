package io.fathom.cloud.compute.api.aws.ec2.actions;

import io.fathom.cloud.CloudException;
import io.fathom.cloud.compute.api.aws.ec2.AwsRequestContext;
import io.fathom.cloud.compute.api.aws.ec2.model.Group;
import io.fathom.cloud.compute.api.aws.ec2.model.Instance;
import io.fathom.cloud.compute.api.aws.ec2.model.Instance.Monitoring;
import io.fathom.cloud.compute.api.aws.ec2.model.Instance.Placement;
import io.fathom.cloud.compute.api.aws.ec2.model.Instance.StateReason;
import io.fathom.cloud.compute.api.aws.ec2.model.InstanceState;
import io.fathom.cloud.compute.state.ComputeRepository;
import io.fathom.cloud.protobuf.CloudModel;
import io.fathom.cloud.protobuf.CloudModel.InstanceData;
import io.fathom.cloud.protobuf.CloudModel.InstanceNetworkData;
import io.fathom.cloud.protobuf.CloudModel.NetworkAddressData;
import io.fathom.cloud.protobuf.CloudModel.ReservationData;
import io.fathom.cloud.protobuf.CloudModel.SecurityGroupData;
import io.fathom.cloud.server.model.Project;
import io.fathom.cloud.server.model.User;

import java.util.Date;
import java.util.List;

import javax.inject.Inject;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response.Status;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;

public abstract class AwsActionHandler {
    private static final Logger log = LoggerFactory.getLogger(AwsActionHandler.class);

    @Inject
    ComputeRepository instanceStateStore;

    private MultivaluedMap<String, String> formParameters;
    private AwsRequestContext context;

    public void init(AwsRequestContext context, MultivaluedMap<String, String> formParameters) {
        this.context = context;
        this.formParameters = formParameters;
    }

    public abstract Object go() throws CloudException;

    public int get(String key, int defaultValue) {
        String value = find(key);
        if (value == null) {
            return defaultValue;
        }
        return Integer.valueOf(value);
    }

    public String get(String key) {
        String value = find(key);
        if (value == null) {
            throw new WebApplicationException(Status.BAD_REQUEST);
        }
        return value;
    }

    public List<String> getList(String key) {
        List<String> value = findList(key);
        if (value == null) {
            throw new WebApplicationException(Status.BAD_REQUEST);
        }
        return value;
    }

    public List<String> findList(String key) {
        List<String> values = Lists.newArrayList();
        int i = 1;
        while (true) {
            String value = find(key + "." + i);
            if (value == null) {
                break;
            }
            values.add(value);
            i++;
        }
        return values;
    }

    private String find(String key) {
        String value = formParameters.getFirst(key);
        return value;
    }

    public String getRequestId() {
        return context.getRequestId();
    }

    protected User findUser() throws CloudException {
        throw new UnsupportedOperationException();

        // CredentialInfo credential = getCredential();
        // UserData user = authStore.getUsers().find(credential.getUserId());
        // return user;
    }

    protected User getUser() throws CloudException {
        User user = findUser();
        if (user == null) {
            throw new WebApplicationException(Status.UNAUTHORIZED);
        }
        return user;
    }

    protected Project findProject() throws CloudException {
        throw new UnsupportedOperationException();
        // UserData user = getUser();
        // if (user.getProjectIdsCount() == 0) {
        // return null;
        // }
        //
        // long projectId = user.getProjectIds(0);
        //
        // return authStore.getProjects().find(projectId);
    }

    protected Project getProject() throws CloudException {
        Project project = findProject();
        if (project == null) {
            throw new IllegalStateException();
        }
        return null;
    }

    // protected CredentialInfo getCredential() throws CloudException {
    // String accessKey = get("AWSAccessKeyId");
    //
    // CredentialInfo credential = authStore.findCredential(
    // AuthStore.ACCESS_KEY_ID, accessKey);
    //
    // log.warn("TODO: Verify signature");
    //
    // if (credential == null) {
    // throw new WebApplicationException(Status.FORBIDDEN);
    // }
    //
    // return credential;
    // }

    public static String toEc2ReservationId(long id) {
        String s = toHex8(id);
        return "r-" + s;
    }

    public static long decodeEc2Id(String id) {
        int dashIndex = id.indexOf('-');
        if (dashIndex == -1) {
            throw new IllegalArgumentException();
        }

        String hex = id.substring(dashIndex + 1);
        return Long.parseLong(hex, 16);
    }

    public static long decodeEc2Id(String prefix, String id) {
        if (!id.startsWith(prefix)) {
            throw new IllegalArgumentException();
        }
        String hex = id.substring(prefix.length());
        return Long.parseLong(hex, 16);
    }

    public static String toEc2InstanceId(long id) {
        String s = toHex8(id);
        return "i-" + s;
    }

    public static String toEc2ImageId(long id) {
        String s = toHex8(id);
        return "ami-" + s;
    }

    public static String toEc2SecurityGroupId(long id) {
        String s = toHex8(id);
        return "sg-" + s;
    }

    public static String toEc2Owner(long user) {
        return Long.toString(user);
    }

    private static String toHex8(long id) {
        String s = Long.toHexString(id);
        int length = s.length();
        if (length > 8) {
            throw new IllegalStateException();
        }
        if (s.length() != 8) {
            s = "00000000".substring(s.length()) + s;
        }
        return s;
    }

    protected Instance buildRunningInstanceXml(ReservationData reservationInfo, InstanceData instanceInfo) {
        Instance instance = new Instance();
        instance.instanceId = toEc2InstanceId(instanceInfo.getId());
        instance.imageId = toEc2ImageId(reservationInfo.getImageId());

        instance.instanceState = buildInstanceState(instanceInfo);

        switch (instanceInfo.getInstanceState()) {
        case PENDING:
            instance.stateReason = new StateReason();
            instance.stateReason.code = "pending";
            instance.stateReason.message = "pending";
            break;

        case RUNNING:
            break;

        case TERMINATED:
            break;

        default:
            throw new IllegalStateException();
        }

        instance.instanceType = "m1.small";
        Date launchTime = new Date(instanceInfo.getLaunchTime());
        instance.launchTime = launchTime;

        instance.placement = new Placement();
        instance.placement.availabilityZone = "main";
        instance.placement.tenancy = "default";

        instance.monitoring = new Monitoring();
        instance.monitoring.state = "disabled";

        instance.architecture = "x86_64";
        instance.rootDeviceType = "instance-store";
        instance.virtualizationType = "paravirtual";
        instance.hypervisor = "xen";
        instance.ebsOptimized = false;

        if (instanceInfo.hasNetwork()) {
            InstanceNetworkData network = instanceInfo.getNetwork();

            NetworkAddressData bestPublic = null;
            NetworkAddressData bestPrivate = null;

            for (NetworkAddressData address : network.getAddressesList()) {
                if (address.getPublicAddress()) {
                    if (bestPublic == null) {
                        bestPublic = address;
                    } else {
                        log.warn("Selection between public addresses is primitive");
                    }
                } else {
                    if (bestPrivate == null) {
                        bestPrivate = address;
                    } else {
                        log.warn("Selection between private addresses is primitive");
                    }
                }
            }

            if (bestPublic != null) {
                instance.ipAddress = bestPublic.getIp();
            }

            if (bestPrivate != null) {
                instance.privateIpAddress = bestPrivate.getIp();
            }
        }
        return instance;
    }

    protected InstanceState buildInstanceState(InstanceData instanceInfo) {
        CloudModel.InstanceState state = instanceInfo.getInstanceState();
        return buildInstanceState(state);
    }

    protected InstanceState buildInstanceState(CloudModel.InstanceState state) {
        InstanceState instanceState = new InstanceState();
        switch (state) {
        case PENDING:
            instanceState.code = 0;
            instanceState.name = "pending";

            break;

        case RUNNING:
            instanceState.code = 16;
            instanceState.name = "running";
            break;

        case SHUTTING_DOWN:
            instanceState.code = 32;
            instanceState.name = "shutting-down";
            break;

        case TERMINATED:
            instanceState.code = 48;
            instanceState.name = "terminated";
            break;

        default:
            throw new IllegalStateException();
        }
        return instanceState;
    }

    protected List<Group> buildGroupsXml(List<SecurityGroupData> groups) {
        List<Group> xml = Lists.newArrayList();

        for (SecurityGroupData group : groups) {
            Group groupXml = new Group();
            xml.add(groupXml);

            groupXml.groupId = toEc2SecurityGroupId(group.getId());
            groupXml.groupName = group.getName();
        }

        return xml;
    }

    protected List<SecurityGroupData> getSecurityGroups() {
        throw new UnsupportedOperationException();
        // List<SecurityGroupData> groups = Lists.newArrayList();
        //
        // {
        // SecurityGroupData.Builder g = SecurityGroupData.newBuilder();
        // g.setId(1234);
        // g.setName("default");
        // groups.add(g.build());
        // }
        // return groups;
    }
}
