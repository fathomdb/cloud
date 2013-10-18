package io.fathom.cloud.compute.services;

import io.fathom.cloud.dns.DnsService;
import io.fathom.cloud.dns.DnsService.DnsRecordSpec;
import io.fathom.cloud.dns.DnsService.DnsRecordsetSpec;
import io.fathom.cloud.openstack.client.loadbalance.model.LbaasMapping;
import io.fathom.cloud.protobuf.CloudModel.InstanceData;
import io.fathom.cloud.protobuf.CloudModel.MetadataData;
import io.fathom.cloud.protobuf.CloudModel.MetadataEntryData;
import io.fathom.cloud.protobuf.CloudModel.NetworkAddressData;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.util.List;

import javax.inject.Inject;

import com.google.common.collect.Lists;
import com.google.common.net.InetAddresses;

public class DerivedMetadata {
    public static final String KEY_DNS_HOST = "__dns:host";
    public static final String KEY_LOADBALANCE_HOST = "__loadbalance:host";
    public static final String KEY_ROLE = "__role";

    @Inject
    DnsService dnsService;

    private final List<DnsRecordsetSpec> dnsRecordsets = Lists.newArrayList();
    private final List<LbaasMapping> lbaasMappings = Lists.newArrayList();
    private final List<String> roles = Lists.newArrayList();
    private List<InetAddress> publicIps;

    public void build(InstanceData instance) {
        MetadataData metadata = instance.getMetadata();

        this.publicIps = getIps(instance, true);

        for (MetadataEntryData entry : metadata.getEntryList()) {
            String key = entry.getKey();
            String value = entry.getValue();

            if (key.equals(KEY_DNS_HOST)) {
                addDnsHost(instance, value);
            }

            if (key.equals(KEY_LOADBALANCE_HOST)) {
                addLbaasMapping(instance, value);
            }

            if (key.equals(KEY_ROLE)) {
                roles.add(value);
            }
        }
    }

    private void addDnsHost(InstanceData instance, String fqdn) {
        for (InetAddress ip : publicIps) {
            DnsRecordsetSpec recordset = new DnsRecordsetSpec();
            recordset.fqdn = fqdn;

            recordset.records = Lists.newArrayList();

            DnsRecordSpec record = new DnsRecordSpec();
            record.address = InetAddresses.toAddrString(ip);
            recordset.records.add(record);

            String type = (ip instanceof Inet6Address ? DnsService.TYPE_AAAA : DnsService.TYPE_A);
            recordset.type = type;

            dnsRecordsets.add(recordset);
        }
    }

    private void addLbaasMapping(InstanceData instance, String fqdn) {

        for (InetAddress ip : publicIps) {
            LbaasMapping lbaasMapping = new LbaasMapping();
            lbaasMapping.host = fqdn;

            String address = InetAddresses.toAddrString(ip);
            lbaasMapping.ip = address;

            lbaasMappings.add(lbaasMapping);
        }
    }

    private List<InetAddress> getIps(InstanceData instance, boolean findPublic) {
        List<InetAddress> addresses = Lists.newArrayList();
        for (NetworkAddressData network : instance.getNetwork().getAddressesList()) {
            if (findPublic != network.getPublicAddress()) {
                continue;
            }

            String ip = network.getIp();
            InetAddress addr = InetAddresses.forString(ip);

            addresses.add(addr);
        }
        return addresses;
    }

    public List<DnsRecordsetSpec> getDnsRecordsets() {
        return dnsRecordsets;
    }

    public List<LbaasMapping> getLbaasMappings() {
        return lbaasMappings;
    }

    public List<String> getRoles() {
        return roles;
    }

    public List<InetAddress> getPublicIps() {
        return publicIps;
    }

}
