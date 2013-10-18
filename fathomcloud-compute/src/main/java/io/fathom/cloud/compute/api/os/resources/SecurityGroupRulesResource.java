package io.fathom.cloud.compute.api.os.resources;

import io.fathom.cloud.CloudException;
import io.fathom.cloud.compute.actions.Instance;
import io.fathom.cloud.compute.api.os.model.SecurityGroupRule;
import io.fathom.cloud.compute.api.os.model.WrappedSecurityGroupRule;
import io.fathom.cloud.compute.networks.IpRange;
import io.fathom.cloud.compute.services.SecurityGroups;
import io.fathom.cloud.protobuf.CloudModel.CidrData;
import io.fathom.cloud.protobuf.CloudModel.Protocols;
import io.fathom.cloud.protobuf.CloudModel.SecurityGroupData;
import io.fathom.cloud.protobuf.CloudModel.SecurityGroupRuleData;
import io.fathom.cloud.server.auth.Auth;

import javax.inject.Inject;
import javax.ws.rs.DELETE;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response.Status;

import com.google.common.base.Strings;
import com.google.common.net.InetAddresses;
import com.google.protobuf.ByteString;

@Path("/openstack/compute/{project}/os-security-group-rules")
public class SecurityGroupRulesResource extends ComputeResourceBase {

    @Inject
    SecurityGroups securityGroups;

    @POST
    public WrappedSecurityGroupRule createRule(WrappedSecurityGroupRule request) throws Exception {
        Auth auth = getAuth();

        SecurityGroupRule rule = request.rule;
        long securityGroupId = Long.valueOf(rule.parentGroupId);

        SecurityGroupData securityGroupData = securityGroups.find(getProject(), securityGroupId);
        if (securityGroupData == null) {
            throw new WebApplicationException(Status.NOT_FOUND);
        }

        SecurityGroupRuleData created;
        {
            SecurityGroupRuleData.Builder r = SecurityGroupRuleData.newBuilder();

            if (rule.fromPort > 0) {
                r.setFromPortLow(Integer.valueOf(rule.fromPort));
                r.setFromPortHigh(Integer.valueOf(rule.toPort));
            }

            if (!Strings.isNullOrEmpty(rule.srcGroupId)) {
                throw new UnsupportedOperationException();
            } else if (!Strings.isNullOrEmpty(rule.cidr)) {
                IpRange range = IpRange.parse(rule.cidr);

                CidrData.Builder cidr = r.getFromCidrBuilder();
                cidr.setAddress(ByteString.copyFrom(range.getAddress().getAddress()));
                cidr.setPrefixLength(range.getNetmaskLength());
            } else {
                throw new IllegalArgumentException("Must specify source cidr or group");
            }

            String protocol = rule.protocol;
            if (protocol != null) {
                protocol = protocol.trim().toLowerCase();
            }

            if (!Strings.isNullOrEmpty(protocol)) {
                if (protocol.equals("tcp")) {
                    r.addIpProtocol(Protocols.TCP_VALUE);
                } else if (protocol.equals("udp")) {
                    r.addIpProtocol(Protocols.UDP_VALUE);
                } else if (protocol.equals("icmp")) {
                    r.addIpProtocol(Protocols.ICMP_VALUE);
                } else {
                    throw new IllegalArgumentException("Unknown protocol: " + protocol);
                }
            }

            if (r.hasFromPortLow() && r.getIpProtocolCount() == 0) {
                throw new IllegalArgumentException("Must specify protocol");
            }

            long id = computeStore.getSecurityGroupRuleIdProvider().get();
            r.setId(id);

            created = securityGroups.addRule(auth, getProject(), securityGroupId, r);
        }

        WrappedSecurityGroupRule response = new WrappedSecurityGroupRule();
        response.rule = toModel(securityGroupData, created);
        return response;
    }

    @DELETE
    @Path("{id}")
    public void deleteRule(@PathParam("id") long id) throws CloudException {
        securityGroups.deleteRule(getAuth(), getProject(), id);
    }

    static SecurityGroupRule toModel(SecurityGroupData parent, SecurityGroupRuleData data) throws CloudException {
        SecurityGroupRule model = new SecurityGroupRule();

        model.id = (int) data.getId();

        if (data.getIpProtocolCount() != 0) {
            int ipProtocol = data.getIpProtocol(0);
            switch (ipProtocol) {
            case Protocols.ICMP_VALUE:
                model.protocol = "icmp";
                break;
            case Protocols.UDP_VALUE:
                model.protocol = "udp";
                break;
            case Protocols.TCP_VALUE:
                model.protocol = "tcp";
                break;
            default:
                throw new IllegalStateException();
            }
        } else {
            if (isBrokenClient()) {
                model.protocol = "";
            }
        }

        model.group = new SecurityGroupRule.Group();

        if (data.hasFromSecurityGroup()) {
            model.srcGroupId = "" + data.getFromSecurityGroup();
        }

        model.fromPort = -1;
        if (data.hasFromPortLow()) {
            model.fromPort = data.getFromPortLow();
            if (model.fromPort == 0) {
                model.fromPort = -1;
            }
        }

        model.toPort = -1;
        if (data.hasFromPortHigh()) {
            model.toPort = data.getFromPortHigh();
            if (model.toPort == 0) {
                model.toPort = -1;
            }
        }

        model.parentGroupId = parent.getId() + "";

        // Horizon wants this to be present, even when empty
        model.ipRange = new SecurityGroupRule.IpRange();

        if (data.hasFromCidr()) {
            CidrData cidr = data.getFromCidr();
            IpRange range = Instance.toIpRange(cidr);

            model.ipRange.cidr = InetAddresses.toAddrString(range.getAddress()) + "/" + range.getNetmaskLength();
        }

        return model;
    }

    private static boolean isBrokenClient() {
        return true;
    }

}
