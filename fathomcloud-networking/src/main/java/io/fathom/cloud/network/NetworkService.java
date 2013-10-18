package io.fathom.cloud.network;

import io.fathom.cloud.CloudException;
import io.fathom.cloud.protobuf.NetworkingModel.NetworkData;
import io.fathom.cloud.protobuf.NetworkingModel.SubnetData;
import io.fathom.cloud.server.auth.Auth;

import java.util.List;

import javax.ws.rs.core.Response.Status;

public interface NetworkService {

    NetworkData updateNetwork(Auth auth, long id, NetworkData.Builder builder) throws CloudException;

    NetworkData createNetwork(Auth auth, NetworkData.Builder builder) throws CloudException;

    List<NetworkData> listNetworks(Auth auth) throws CloudException;

    NetworkData findNetwork(Auth auth, long id) throws CloudException;

    Status deleteNetwork(Auth auth, long id);

    SubnetData findSubnet(Auth auth, long id) throws CloudException;

    List<SubnetData> listSubnets(Auth auth) throws CloudException;

    SubnetData createSubnet(Auth auth, SubnetData.Builder b) throws CloudException;

}
