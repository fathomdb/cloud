package io.fathom.cloud.compute.api.os.resources;

import io.fathom.cloud.CloudException;
import io.fathom.cloud.compute.api.os.model.FloatingIp;
import io.fathom.cloud.compute.api.os.model.FloatingIps;
import io.fathom.cloud.compute.api.os.model.WrappedFloatingIp;
import io.fathom.cloud.compute.networks.VirtualIp;
import io.fathom.cloud.compute.services.IpPools;
import io.fathom.cloud.protobuf.CloudModel.VirtualIpData;
import io.fathom.cloud.protobuf.CloudModel.VirtualIpPoolData;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;

import javax.inject.Inject;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response.Status;

import com.fathomdb.utils.Hex;
import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.net.InetAddresses;

@Path("/openstack/compute/{project}/os-floating-ips")
public class FloatingIpsResource extends ComputeResourceBase {
    @Inject
    IpPools ipPools;

    @GET
    public FloatingIps list() throws CloudException {
        FloatingIps response = new FloatingIps();
        response.floatingIps = Lists.newArrayList();

        for (VirtualIp vip : ipPools.listVirtualIps(getProject())) {
            response.floatingIps.add(toModel(vip));
        }

        return response;
    }

    @GET
    @Path("{id}")
    public WrappedFloatingIp find(@PathParam("id") String id) throws CloudException {
        String address = toAddress(id);

        VirtualIp vip = ipPools.findVirtualIp(getProject(), address);
        if (vip == null) {
            throw new WebApplicationException(Status.NOT_FOUND);
        }

        WrappedFloatingIp response = new WrappedFloatingIp();
        response.floatingIp = toModel(vip);
        return response;
    }

    @DELETE
    @Path("{id}")
    public void delete(@PathParam("id") String id) throws CloudException {
        String address = toAddress(id);
        VirtualIp vip = ipPools.findVirtualIp(getProject(), address);
        if (vip == null) {
            throw new WebApplicationException(Status.NOT_FOUND);
        }

        ipPools.deallocateFloatingIp(getProject(), vip.getData().getIp());
    }

    @POST
    public WrappedFloatingIp allocate(FloatingIp template) throws CloudException {
        VirtualIpPoolData pool = findPool(template.pool);
        if (pool == null) {
            throw new IllegalArgumentException();
        }

        VirtualIp vip = ipPools.allocateFloatingIp(getProject(), pool);

        WrappedFloatingIp response = new WrappedFloatingIp();
        response.floatingIp = toModel(vip);
        return response;
    }

    private VirtualIpPoolData findPool(String label) throws CloudException {
        if (label == null) {
            throw new IllegalArgumentException();
        }

        for (VirtualIpPoolData poolData : ipPools.listVirtualIpPools(getProject())) {
            if (label.equals(getLabel(poolData))) {
                return poolData;
            }
        }
        return null;
    }

    private FloatingIp toModel(VirtualIp vip) {
        VirtualIpData data = vip.getData();
        FloatingIp model = new FloatingIp();
        // model.id = vip.getId();
        model.id = toId(vip.getData());
        if (data.hasInstanceId()) {
            model.instanceId = Long.toString(data.getInstanceId());
        }

        InetAddress address = InetAddresses.forString(data.getIp());
        String ip;
        if (address instanceof Inet4Address) {
            ip = InetAddresses.toAddrString(address);
        } else {
            throw new UnsupportedOperationException();
            // ip = InetAddresses.toAddrString(address) + "/112";
        }
        model.ip = ip;
        model.pool = getLabel(vip.getPoolData());
        return model;
    }

    public static String toId(VirtualIpData data) {
        // We make it look like a UUID

        InetAddress address = InetAddresses.forString(data.getIp());
        if (address instanceof Inet4Address) {
            Inet4Address inet4 = (Inet4Address) address;

            byte[] b = new byte[16];
            System.arraycopy(inet4.getAddress(), 0, b, 12, 4);

            return asUuid(b);
        } else {
            Inet6Address inet6 = (Inet6Address) address;

            byte[] b = new byte[16];
            System.arraycopy(inet6.getAddress(), 0, b, 0, 16);

            return asUuid(b);
        }
    }

    private String toAddress(String id) {
        String s = id.replace("-", "");
        byte[] b = Hex.fromHex(s);
        if (b.length != 16) {
            throw new IllegalArgumentException();
        }

        boolean ipv4 = true;

        for (int i = 0; i < 12; i++) {
            if (b[i] != 0) {
                ipv4 = false;
                break;
            }
        }

        if (ipv4) {
            byte[] data = new byte[4];
            System.arraycopy(b, 12, data, 0, 4);
            Inet4Address addr;
            try {
                addr = (Inet4Address) InetAddress.getByAddress(data);
            } catch (UnknownHostException e) {
                throw new IllegalArgumentException();
            }
            return addr.getHostAddress();
        } else {
            Inet6Address addr;
            try {
                addr = (Inet6Address) InetAddress.getByAddress(b);
            } catch (UnknownHostException e) {
                throw new IllegalArgumentException();
            }
            return addr.getHostAddress();
        }
    }

    private static String asUuid(byte[] b) {
        return Hex.toHex(b, 0, 4) + "-" + Hex.toHex(b, 4, 2) + "-" + Hex.toHex(b, 6, 2) + "-" + Hex.toHex(b, 8, 2)
                + "-" + Hex.toHex(b, 10, 6);
    }

    public static String getLabel(VirtualIpPoolData pool) {
        String name = pool.getLabel();
        if (Strings.isNullOrEmpty(name)) {
            if (pool.getCidrCount() != 0) {
                name = "CIDR " + Joiner.on(",").join(pool.getCidrList());
            } else if (pool.hasType()) {
                switch (pool.getType()) {
                case AMAZON_EC2:
                    name = "AWS Elastic IPs";
                    break;
                default:
                    name = pool.getType().name().toLowerCase();
                    break;
                }
            } else {
                throw new IllegalStateException();
            }
        }
        return name;
    }

}
