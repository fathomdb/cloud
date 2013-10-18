package io.fathom.cloud.compute.scheduler;

import io.fathom.cloud.CloudException;
import io.fathom.cloud.compute.networks.VirtualIp;
import io.fathom.cloud.protobuf.CloudModel.InstanceData;
import io.fathom.cloud.protobuf.CloudModel.SecurityGroupData;

import java.io.Closeable;
import java.util.List;
import java.util.Set;

public interface ConfigurationOperation extends Closeable {

    void configureFirewall(InstanceData instance, List<SecurityGroupData> securityGroups) throws CloudException;

    void removeFirewallConfig(InstanceData instance) throws CloudException;

    boolean applyChanges() throws CloudException;

    void attachVip(InstanceData instance, VirtualIp vip) throws CloudException;

    void detachVip(InstanceData instance, VirtualIp vip) throws CloudException;

    void configureIpset(long securityGroupId, Set<String> ips) throws CloudException;

}
