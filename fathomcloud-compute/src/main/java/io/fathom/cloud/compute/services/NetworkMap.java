package io.fathom.cloud.compute.services;

import io.fathom.cloud.CloudException;
import io.fathom.cloud.protobuf.CloudModel.HostData;
import io.fathom.cloud.protobuf.CloudModel.HostGroupData;

import java.util.List;

public interface NetworkMap {

    HostGroupData createHostGroup(HostGroupData.Builder b) throws CloudException;

    List<HostGroupData> listHostGroups() throws CloudException;

    HostGroupData findHostGroup(long id) throws CloudException;

    HostGroupData findHostGroupByKey(String parentKey) throws CloudException;

    HostData createHost(HostData.Builder b) throws CloudException;

    List<HostData> listHosts() throws CloudException;

    HostData updateHost(long hostId, HostData.Builder b) throws CloudException;

    HostData findHost(String cidr) throws CloudException;

}
