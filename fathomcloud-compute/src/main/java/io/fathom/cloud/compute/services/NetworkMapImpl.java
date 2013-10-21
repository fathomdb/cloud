package io.fathom.cloud.compute.services;

import io.fathom.cloud.CloudException;
import io.fathom.cloud.compute.scheduler.InstanceScheduler;
import io.fathom.cloud.compute.state.HostStore;
import io.fathom.cloud.protobuf.CloudModel.HostData;
import io.fathom.cloud.protobuf.CloudModel.HostGroupData;
import io.fathom.cloud.protobuf.CloudModel.HostGroupData.Builder;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.google.inject.persist.Transactional;

@Singleton
@Transactional
public class NetworkMapImpl implements NetworkMap {

    @Inject
    HostStore hostStore;

    @Inject
    InstanceScheduler instanceScheduler;

    @Override
    public List<HostGroupData> listHostGroups() throws CloudException {
        List<HostGroupData> dcs = hostStore.getHostGroups().list();
        return dcs;
    }

    @Override
    public HostGroupData findHostGroup(long key) throws CloudException {
        return hostStore.getHostGroups().find(key);
    }

    @Override
    public HostData createHost(HostData.Builder b) throws CloudException {
        long parent = b.getHostGroup();
        if (parent == 0) {
            throw new IllegalArgumentException();
        }

        instanceScheduler.refreshHosts();

        return hostStore.getHosts().create(b);
    }

    @Override
    public List<HostData> listHosts() throws CloudException {
        return hostStore.getHosts().list();
    }

    @Override
    public HostGroupData createHostGroup(Builder b) throws CloudException {
        return hostStore.getHostGroups().create(b);
    }

    @Override
    public HostData updateHost(long hostId, io.fathom.cloud.protobuf.CloudModel.HostData.Builder b)
            throws CloudException {
        HostData host = hostStore.getHosts().find(hostId);
        if (host == null) {
            throw new IllegalArgumentException();
        }

        HostData.Builder hb = HostData.newBuilder(host);
        hb.mergeFrom(b.buildPartial());
        hb.setId(host.getId());

        return hostStore.getHosts().update(hb);
    }

    @Override
    public HostData findHost(String cidr) throws CloudException {
        for (HostData host : listHosts()) {
            if (host.getCidr().equals(cidr)) {
                return host;
            }
        }
        return null;
    }

    @Override
    public HostGroupData findHostGroupByKey(String findKey) throws CloudException {
        for (HostGroupData hostGroup : listHostGroups()) {
            if (findKey.equals(hostGroup.getKey())) {
                return hostGroup;
            }
        }
        return null;
    }

}
