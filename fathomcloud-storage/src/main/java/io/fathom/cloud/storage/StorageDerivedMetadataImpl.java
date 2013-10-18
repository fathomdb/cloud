package io.fathom.cloud.storage;

import io.fathom.cloud.CloudException;
import io.fathom.cloud.dns.DnsService;
import io.fathom.cloud.dns.DnsService.DnsRecordsetSpec;
import io.fathom.cloud.loadbalancer.LoadBalanceService;
import io.fathom.cloud.openstack.client.loadbalance.model.LbaasMapping;
import io.fathom.cloud.protobuf.CloudCommons.Attributes;
import io.fathom.cloud.protobuf.CloudCommons.KeyValueData;
import io.fathom.cloud.protobuf.FileModel.BucketData;
import io.fathom.cloud.server.model.Project;

import java.net.URI;
import java.util.List;

import javax.inject.Inject;

import com.google.common.collect.Lists;

public class StorageDerivedMetadataImpl implements StorageDerivedMetadata {
    // Buckets can't have colons in their metadata key names...
    private static final String KEY_HTTP_HOST = "__http_host";

    @Inject
    DnsService dnsService;

    @Inject
    LoadBalanceService loadBalancer;

    private final List<DnsRecordsetSpec> records = Lists.newArrayList();
    private final List<LbaasMapping> loadBalancerRecords = Lists.newArrayList();

    void build(Project project, BucketData bucket) {
        Attributes metadata = bucket.getAttributes();

        for (KeyValueData entry : metadata.getUserAttributesList()) {
            String key = entry.getKey();
            String value = entry.getValue();

            if (key.equals(KEY_HTTP_HOST)) {
                addHttpHost(project, bucket, value);
            }
        }
    }

    // private void addDnsHost(InstanceInfo instance, String fqdn) {
    // List<InetAddress> ips = getIps(instance, true);
    //
    // for (InetAddress ip : ips) {
    // DnsService.Record record = dnsService.buildAddress(fqdn, ip);
    // records.add(record);
    // }
    // }
    //
    // private List<InetAddress> getIps(InstanceInfo instance, boolean
    // findPublic) {
    // List<InetAddress> addresses = Lists.newArrayList();
    // for (NetworkAddressInfo network :
    // instance.getNetwork().getAddressesList()) {
    // if (findPublic != network.getPublicAddress()) {
    // continue;
    // }
    //
    // String ip = network.getIp();
    // InetAddress addr = InetAddresses.forString(ip);
    //
    // addresses.add(addr);
    // }
    // return addresses;
    // }

    private void addHttpHost(Project project, BucketData bucket, String value) {
        String host = value;

        String publicBucketUrl = "https://api-cloud.fathomdb.com/openstack/storage/" + project.getId() + "/"
                + bucket.getKey();
        URI redirect = URI.create(publicBucketUrl);

        LbaasMapping record = new LbaasMapping();

        record.host = host;
        record.forwardUrl = redirect.toString();

        loadBalancerRecords.add(record);
    }

    public List<DnsRecordsetSpec> getDnsRecords() {
        return records;
    }

    public List<LbaasMapping> getLoadBalancerRecords() {
        return loadBalancerRecords;
    }

    @Override
    public void apply(Project project, BucketData bucket, String systemKey) throws CloudException {
        build(project, bucket);

        dnsService.setDnsRecordsets(systemKey, project, records);
        loadBalancer.setMappings(systemKey, project, loadBalancerRecords);
    }

}
