package io.fathom.cloud.compute.services;

import io.fathom.cloud.CloudException;
import io.fathom.cloud.dns.DnsService;
import io.fathom.cloud.dns.DnsService.DnsRecordsetSpec;
import io.fathom.cloud.loadbalancer.LoadBalanceService;
import io.fathom.cloud.openstack.client.loadbalance.model.LbaasMapping;
import io.fathom.cloud.openstack.client.loadbalance.model.LbaasServer;
import io.fathom.cloud.protobuf.CloudModel.InstanceData;
import io.fathom.cloud.server.model.Project;

import java.net.InetAddress;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import com.google.common.net.InetAddresses;

@Singleton
public class ComputeDerivedMetadataImpl implements ComputeDerivedMetadata {
    private static final Logger log = LoggerFactory.getLogger(ComputeDerivedMetadataImpl.class);

    public static final String ROLE_LBAAS = "lbaas";

    @Inject
    DnsService dnsService;

    @Inject
    LoadBalanceService loadBalanceService;

    @Inject
    Provider<DerivedMetadata> derivedMetadataProvider;

    @Override
    public void instanceUpdated(Project project, InstanceData instance) throws CloudException {
        DerivedMetadata newMetadata = derivedMetadataProvider.get();
        newMetadata.build(instance);

        String systemKey = "instance/" + instance.getId() + "/metadata";

        List<DnsRecordsetSpec> dnsRecordsets = newMetadata.getDnsRecordsets();
        List<LbaasMapping> loadbalanceRecords = newMetadata.getLbaasMappings();
        List<String> roles = newMetadata.getRoles();

        dnsService.setDnsRecordsets(systemKey, project, dnsRecordsets);

        loadBalanceService.setMappings(systemKey, project, loadbalanceRecords);

        // We do this so that the LBAAS service can configure DNS
        // This is risky though, as the load balancers will then start
        // taking traffic, even if they're not fully booted up
        // Better might be to have the load balancers register with DNS
        // themselves
        // (they can do that using the per-instance token)
        // Or we could just put a delay in here
        // Or we might need another layer (i.e. a layer 4 load balancer in front
        // of the layer 7 load balancers)
        List<LbaasServer> lbaasServers = Lists.newArrayList();
        for (String role : roles) {
            if (role.equals(ROLE_LBAAS)) {
                List<InetAddress> publicIps = newMetadata.getPublicIps();
                if (publicIps.size() == 0) {
                    log.warn("No public IPs found for load balancer instance: {}", instance);
                } else {
                    if (publicIps.size() != 1) {
                        log.warn("Multiple public IPs found for load balancer instance, will only use first: {}",
                                instance);
                    }
                    LbaasServer lbaasServer = new LbaasServer();
                    lbaasServer.ip = InetAddresses.toAddrString(publicIps.get(0));
                    lbaasServers.add(lbaasServer);
                }
            } else {
                log.info("Ignoring unknown role: " + role);
            }
        }

        loadBalanceService.setServers(systemKey, project, lbaasServers);
    }
}
