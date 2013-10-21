package io.fathom.cloud.compute.commands;

import io.fathom.cloud.commands.TypedCmdlet;
import io.fathom.cloud.compute.services.ComputeSecrets;
import io.fathom.cloud.compute.services.NetworkMap;
import io.fathom.cloud.protobuf.CloudCommons.SecretData;
import io.fathom.cloud.protobuf.CloudModel.HostGroupData;
import io.fathom.cloud.protobuf.CloudModel.HostGroupSecretData;
import io.fathom.cloud.protobuf.CloudModel.HostGroupType;

import javax.inject.Inject;

import org.kohsuke.args4j.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HostGroupCreateCmdlet extends TypedCmdlet {
    private static final Logger log = LoggerFactory.getLogger(HostGroupCreateCmdlet.class);

    @Option(name = "-label", usage = "label", required = false)
    public String label;

    @Option(name = "-key", usage = "cidr", required = true)
    public String key;

    // TODO: We could probably auto-find the smallest CIDR
    @Option(name = "-parent", usage = "parent key", required = false)
    public String parentKey = null;

    @Option(name = "-type", usage = "type", required = true)
    public String type;

    // @Option(name = "-username", usage = "AWS username", required = false)
    // public String username = null;
    //
    // @Option(name = "-password", usage = "AWS password", required = false)
    // public String password = null;

    @Inject
    ComputeSecrets computeSecrets;

    @Inject
    NetworkMap networkMap;

    public HostGroupCreateCmdlet() {
        super("hostgroup-create");
    }

    @Override
    protected HostGroupData run0() throws Exception {
        // IpRange range = IpRange.parse(cidr);
        // if (!range.isIpv6()) {
        // throw new IllegalArgumentException("Only IPV6 is supported");
        // }
        //
        // if (range.getNetmaskLength() > 120) {
        // // No real reason, just to keep things sensible
        // throw new IllegalArgumentException("Must allocate at least a /120");
        // }

        if (networkMap.findHostGroupByKey(key) != null) {
            throw new IllegalArgumentException("Host group already exists");
        }

        HostGroupData parent = null;
        if (parentKey != null) {
            parent = networkMap.findHostGroupByKey(parentKey);
            if (parent == null) {
                throw new IllegalArgumentException("Specified parent not found");
            }
        }

        HostGroupData.Builder b = HostGroupData.newBuilder();

        if (label != null) {
            b.setLabel(label);
        }
        b.setKey(key);

        type = type.toLowerCase().trim();
        if (type.equals("ec2")) {
            b.setHostGroupType(HostGroupType.HOST_GROUP_TYPE_AMAZON_EC2);
        } else if (type.equals("bare")) {
            b.setHostGroupType(HostGroupType.HOST_GROUP_TYPE_RAW);
        } else {
            throw new IllegalArgumentException("Expected type to be 'bare' or 'ec2'");
        }

        {
            HostGroupSecretData.Builder sb = HostGroupSecretData.newBuilder();

            // We're going to rely on IAM ... so much easier + more secure
            // if (username != null) {
            // sb.setUsername(username);
            // }
            //
            // if (password != null) {
            // sb.setPassword(password);
            // }

            SecretData secretData = computeSecrets.encrypt(sb.build());
            b.setSecretData(secretData);
        }

        if (parent != null) {
            b.setParent(parent.getId());

            // IpRange parentRange = IpRange.parse(parent.getCidr());
            // if (!containsStrict(parentRange, range)) {
            // throw new
            // IllegalArgumentException("Child CIDR must be a sub-range of the parent range");
            // }
        }

        HostGroupData created = networkMap.createHostGroup(b);
        return created;
    }
}
