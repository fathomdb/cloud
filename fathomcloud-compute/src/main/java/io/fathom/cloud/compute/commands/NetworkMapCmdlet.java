package io.fathom.cloud.compute.commands;

import io.fathom.cloud.CloudException;
import io.fathom.cloud.commands.TypedCmdlet;
import io.fathom.cloud.compute.networks.IpRange;
import io.fathom.cloud.compute.services.NetworkMap;
import io.fathom.cloud.protobuf.CloudModel.HostGroupData;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class NetworkMapCmdlet extends TypedCmdlet {

    private static final Logger log = LoggerFactory.getLogger(NetworkMapCmdlet.class);

    @Inject
    protected NetworkMap networkMap;

    public NetworkMapCmdlet(String command) {
        super(command);
    }

    // protected DatacenterData getDatacenter(Long datacenterKey) throws
    // CloudException {
    // DatacenterData dc;
    // if (datacenterKey == null) {
    // List<DatacenterData> dcs = networkMap.listDatacenters();
    // if (dcs.size() == 0) {
    // throw new
    // IllegalArgumentException("Please create a datacenter first, with create-datacenter");
    // } else if (dcs.size() == 1) {
    // dc = dcs.get(0);
    // } else {
    // throw new
    // IllegalArgumentException("Multiple datacenters found; please specify the datacenter to use");
    // }
    // } else {
    // dc = networkMap.findDatacenter(datacenterKey);
    // if (dc == null) {
    // throw new IllegalArgumentException("Specified datacenter not found");
    // }
    // }
    // return dc;
    // }

    protected HostGroupData findHostGroup(String key) throws CloudException {
        for (HostGroupData hostGroup : networkMap.listHostGroups()) {
            if (key.equals(hostGroup.getKey())) {
                return hostGroup;
            }
        }
        return null;
    }

    protected boolean containsStrict(IpRange parentRange, IpRange range) {
        log.warn("TODO: Implement containsStrict");
        return true;
    }
}
