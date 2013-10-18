package io.fathom.cloud.network;

import io.fathom.cloud.CloudException;
import io.fathom.cloud.lifecycle.LifecycleListener;
import io.fathom.cloud.protobuf.NetworkingModel.NetworkData;
import io.fathom.cloud.protobuf.NetworkingModel.SubnetData;
import io.fathom.cloud.server.auth.Auth;
import io.fathom.cloud.server.auth.Auth.Domain;
import io.fathom.cloud.tasks.TaskScheduler;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response.Status;

import com.google.inject.persist.Transactional;

@Singleton
@Transactional
public class NetworkServiceImpl implements NetworkService, LifecycleListener {

    @Inject
    TaskScheduler scheduler;

    @Inject
    NetworkStore store;

    @Override
    public void start() throws Exception {
        // scheduler.schedule(UpdateClusterTask.class);
    }

    @Override
    public NetworkData updateNetwork(Auth auth, long id, NetworkData.Builder builder) throws CloudException {
        if (id == 0) {
            throw new IllegalArgumentException();
        }

        requireDomainAdmin(auth);

        NetworkData network = store.getSharedNetworks().find(id);
        if (network == null) {
            throw new WebApplicationException(Status.NOT_FOUND);
        }

        NetworkData.Builder merged = NetworkData.newBuilder(network);
        merged.mergeFrom(builder.buildPartial());

        merged.setId(id);

        merged.setProject(auth.getProject().getId());

        NetworkData updated = store.getSharedNetworks().update(merged);
        return updated;
    }

    @Override
    public NetworkData createNetwork(Auth auth, NetworkData.Builder builder) throws CloudException {
        requireDomainAdmin(auth);

        builder.setProject(auth.getProject().getId());

        NetworkData network = store.getSharedNetworks().create(builder);
        return network;
    }

    @Override
    public SubnetData createSubnet(Auth auth, SubnetData.Builder builder) throws CloudException {
        requireDomainAdmin(auth);

        long networkId = builder.getNetwork();
        NetworkData network = findNetwork(auth, networkId);
        if (network == null) {
            throw new WebApplicationException(Status.NOT_FOUND);
        }

        builder.setProject(auth.getProject().getId());

        SubnetData subnet = store.getSharedSubnets().create(builder);
        return subnet;
    }

    private Domain requireDomainAdmin(Auth auth) {
        Domain domain = auth.findDomainWithAdminRole();
        if (domain == null) {
            throw new WebApplicationException(Status.UNAUTHORIZED);
        }
        return domain;
    }

    @Override
    public Status deleteNetwork(Auth auth, long id) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<NetworkData> listNetworks(Auth auth) throws CloudException {
        List<NetworkData> networks = store.getSharedNetworks().list();
        return networks;
    }

    @Override
    public NetworkData findNetwork(Auth auth, long id) throws CloudException {
        NetworkData network = store.getSharedNetworks().find(id);
        return network;
    }

    @Override
    public List<SubnetData> listSubnets(Auth auth) throws CloudException {
        List<SubnetData> networks = store.getSharedSubnets().list();
        return networks;
    }

    @Override
    public SubnetData findSubnet(Auth auth, long id) throws CloudException {
        SubnetData network = store.getSharedSubnets().find(id);
        return network;
    }

}
